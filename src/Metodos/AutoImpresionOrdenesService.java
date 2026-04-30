package Metodos;

import Formularios.frm_main;
import conexiondb.DB_consultas_R_D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

/**
 * Escucha el canal Postgres "orden_nueva" y manda imprimir las órdenes
 * según los flags del usuario logueado.
 *
 * Tabla de casos:
 *   1. imprime_ordenes=false                                  -> nada
 *   2. usuario logueado creó la orden                          -> nada
 *   3. B=false, orden de OTRA bodega                           -> nada
 *   4. B=false, orden de SU bodega                             -> ticket completo
 *   5. B=true,  orden de OTRA bodega                           -> ticket completo
 *   6. B=true,  orden de SU bodega                             -> completo + filtrado
 *
 * Donde B = imp_ticket_bodega_asignada.
 *
 * Dedupe:
 *   - Ticket completo para B=true: tabla auto_impresion_log_completo
 *     (PK por id_user + numero_factura) — evita reimprimir cuando wo-printer
 *     parte una factura en varias cabeceras.
 *   - Ticket filtrado / caso 4: facturas_cabeceras.impreso_auto.
 *
 * Defensa en profundidad:
 *   - Conexión dedicada con LISTEN.
 *   - Reconexión automática con backoff si la conexión se cae.
 *   - Polling de respaldo cada 60s.
 */
public class AutoImpresionOrdenesService {

    private static final String CANAL = "orden_nueva";
    private static final long POLL_MS = 60_000L;
    private static final long BACKOFF_INICIAL_MS = 5_000L;
    private static final long BACKOFF_MAX_MS = 60_000L;

    private static AutoImpresionOrdenesService instancia;

    private volatile boolean activo = false;
    private Thread hilo;
    private Connection conn;

    private AutoImpresionOrdenesService() {
    }

    public static synchronized AutoImpresionOrdenesService getInstance() {
        if (instancia == null) {
            instancia = new AutoImpresionOrdenesService();
        }
        return instancia;
    }

    public synchronized void iniciar() {
        if (activo) {
            return;
        }
        activo = true;
        hilo = new Thread(this::loop, "auto-impresion-ordenes");
        hilo.setDaemon(true);
        hilo.start();
        System.out.println("[AutoImpresion] Servicio iniciado para usuario " + frm_main.id_user);
    }

    public synchronized void detener() {
        activo = false;
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (Exception ignored) {
        }
        if (hilo != null) {
            hilo.interrupt();
        }
        System.out.println("[AutoImpresion] Servicio detenido");
    }

    private void loop() {
        long backoff = BACKOFF_INICIAL_MS;
        while (activo) {
            try {
                conectarYEscuchar();
                backoff = BACKOFF_INICIAL_MS;
            } catch (Exception ex) {
                if (!activo) {
                    return;
                }
                System.out.println("[AutoImpresion] Error en listener: " + ex.getMessage()
                        + " — reintentando en " + (backoff / 1000) + "s");
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    return;
                }
                backoff = Math.min(backoff * 2, BACKOFF_MAX_MS);
            }
        }
    }

    private void conectarYEscuchar() throws Exception {
        conn = DB_consultas_R_D.getConexion();
        if (conn == null) {
            throw new IllegalStateException("No se obtuvo conexión");
        }

        // ORDEN IMPORTANTE:
        // 1) Marcar lo existente como ya procesado ANTES de LISTEN.
        //    Si lo hiciéramos después, una NOTIFY que llegue en la microventana
        //    entre LISTEN y UPDATE quedaría encolada pero su cabecera sería
        //    marcada impresa por el UPDATE -> al leer la notificación se
        //    descartaría como "ya impresa". Resultado: ticket perdido.
        marcarExistentesComoProcesadas();

        // 2) Ahora sí, LISTEN. Las NOTIFY que lleguen a partir de aquí
        //    corresponden a cabeceras nuevas (creadas después del UPDATE),
        //    así que no estarán marcadas impresas y se procesarán normal.
        try (Statement st = conn.createStatement()) {
            st.execute("LISTEN " + CANAL);
        }

        // 3) Pendientes (defensa en profundidad ante caídas de conexión).
        procesarPendientes();

        PGConnection pgConn = conn.unwrap(PGConnection.class);
        long ultimoPoll = System.currentTimeMillis();

        while (activo) {
            // Bloquea hasta POLL_MS o hasta que llegue un NOTIFY.
            PGNotification[] notifs = pgConn.getNotifications((int) POLL_MS);
            if (!activo) {
                return;
            }
            if (notifs != null) {
                for (PGNotification n : notifs) {
                    procesarPayload(n.getParameter());
                }
            }
            // Polling de respaldo
            if (System.currentTimeMillis() - ultimoPoll >= POLL_MS) {
                procesarPendientes();
                ultimoPoll = System.currentTimeMillis();
            }
            // Sanity check de la conexión
            if (conn.isClosed()) {
                throw new IllegalStateException("Conexión cerrada");
            }
        }
    }

    /**
     * Procesa un NOTIFY entrante. Filtros previos al despacho:
     *   - tipo == 'Venta'                 -> ignorar
     *   - usuario logueado creó la orden  -> ignorar (caso 2)
     *   - imprime_ordenes == false        -> ignorar (caso 1)
     */
    private void procesarPayload(String payload) {
        if (payload == null) {
            return;
        }

        if (!frm_main.imprime_ordenes) {
            return; // caso 1
        }

        // Formato 1 (cabecera nueva): id|id_bodega|id_user|tipo_factura
        // Formato 2 (factura solo novedad): NOVEDAD|id_impresa|numero_factura
        if (payload.startsWith("NOVEDAD|")) {
            procesarNovedadSinBodega(payload);
            return;
        }

        String[] partes = payload.split("\\|", -1);
        if (partes.length < 4) {
            return;
        }
        int idFactura, idBodega, idUserCreador;
        try {
            idFactura = Integer.parseInt(partes[0]);
            idBodega = partes[1].isEmpty() ? -1 : Integer.parseInt(partes[1]);
            idUserCreador = partes[2].isEmpty() ? -1 : Integer.parseInt(partes[2]);
        } catch (NumberFormatException nfe) {
            return;
        }
        String tipo = partes[3];

        if ("Venta".equals(tipo)) {
            return;
        }
        if (idUserCreador == frm_main.id_user) {
            return; // caso 2
        }

        imprimirOrden(idFactura, idBodega);
    }

    /**
     * Maneja una factura donde TODOS los ítems son novedades (no hay cabeceras).
     * Sólo los usuarios B=true reciben este ticket — para B=false no aplica
     * (no hay bodega involucrada que matchear).
     */
    private void procesarNovedadSinBodega(String payload) {
        if (!frm_main.imp_ticket_bodega_asignada) {
            return; // sólo aplica caso 5/6 con bodega vacía
        }
        String[] partes = payload.split("\\|", -1);
        if (partes.length < 3) {
            return;
        }
        String numeroFactura = partes[2];
        if (numeroFactura == null || numeroFactura.isEmpty()) {
            return;
        }

        // Dedupe por (id_user, numero_factura)
        if (!tryInsertLogCompleto(numeroFactura)) {
            return;
        }

        try {
            new ver_factura_impresion().imprimir_termica_80mm_silencioso_solo_novedad(
                    numeroFactura, frm_main.print_service_user);
            System.out.println("[AutoImpresion] Full ticket (solo novedad) impreso: " + numeroFactura);
        } catch (Exception ex) {
            System.out.println("[AutoImpresion] Error imprimiendo solo-novedad "
                    + numeroFactura + ": " + ex.getMessage());
        }
    }

    /**
     * Pendientes no procesados al reconectar. La query depende del flag B:
     *   - B=false: solo cabeceras de su bodega no impresas hoy.
     *   - B=true:  cualquier cabecera de hoy donde falte algo (full no logged,
     *              o filtrada no impresa).
     */
    private void procesarPendientes() {
        String sql;
        if (frm_main.imp_ticket_bodega_asignada) {
            sql = "SELECT fc.id, fc.id_bodega "
                    + "FROM facturas_cabeceras fc "
                    + "WHERE fc.tipo_factura <> 'Venta' "
                    + "AND fc.id_user <> " + frm_main.id_user + " "
                    + "AND fc.fecha = CURRENT_DATE "
                    + "AND ( "
                    + "  (fc.id_bodega = " + frm_main.id_bodega + " AND fc.impreso_auto = false) "
                    + "  OR NOT EXISTS ( "
                    + "    SELECT 1 FROM auto_impresion_log_completo l "
                    + "    WHERE l.id_user = " + frm_main.id_user + " "
                    + "    AND l.numero_factura = fc.codigo "
                    + "  ) "
                    + ")";
        } else {
            sql = "SELECT fc.id, fc.id_bodega "
                    + "FROM facturas_cabeceras fc "
                    + "WHERE fc.impreso_auto = false "
                    + "AND fc.tipo_factura <> 'Venta' "
                    + "AND fc.id_bodega = " + frm_main.id_bodega + " "
                    + "AND fc.id_user <> " + frm_main.id_user + " "
                    + "AND fc.fecha = CURRENT_DATE";
        }

        List<int[]> filas = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                filas.add(new int[] { rs.getInt("id"), rs.getInt("id_bodega") });
            }
        } catch (Exception ex) {
            System.out.println("[AutoImpresion] Error leyendo pendientes: " + ex.getMessage());
            return;
        }
        for (int[] f : filas) {
            imprimirOrden(f[0], f[1]);
        }

        // Pendientes adicionales: facturas solo-novedad (sin cabeceras) — sólo B=true
        if (frm_main.imp_ticket_bodega_asignada) {
            String sqlNov = "SELECT fi.numero_factura "
                    + "FROM facturas_impresas fi "
                    + "WHERE fi.fecha_factura = CURRENT_DATE "
                    + "AND fi.numero_factura IS NOT NULL "
                    + "AND NOT EXISTS ( "
                    + "  SELECT 1 FROM facturas_cabeceras WHERE codigo = fi.numero_factura "
                    + ") "
                    + "AND NOT EXISTS ( "
                    + "  SELECT 1 FROM auto_impresion_log_completo l "
                    + "  WHERE l.id_user = " + frm_main.id_user + " "
                    + "  AND l.numero_factura = fi.numero_factura "
                    + ")";
            List<String> nums = new ArrayList<>();
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlNov)) {
                while (rs.next()) {
                    nums.add(rs.getString("numero_factura"));
                }
            } catch (Exception ex) {
                // facturas_impresas puede no existir en entornos sin wo-printer — ignorar.
                return;
            }
            for (String n : nums) {
                procesarNovedadSinBodega("NOVEDAD|0|" + n);
            }
        }
    }

    private final Set<Integer> enProceso = new HashSet<>();

    /**
     * Implementa la tabla de casos. Asume que ya se filtraron los casos 1 y 2.
     */
    private void imprimirOrden(int idFactura, int idBodega) {
        synchronized (enProceso) {
            if (!enProceso.add(idFactura)) {
                return; // ya en curso en otro hilo (defensivo)
            }
        }
        try {
            boolean esMiBodega = (idBodega == frm_main.id_bodega);

            if (frm_main.imp_ticket_bodega_asignada) {
                // ----- Casos 5 y 6 -----
                // Full ticket: dedupe por (user, numero_factura)
                String numeroFactura = lookupNumeroFactura(idFactura);
                if (numeroFactura != null && tryInsertLogCompleto(numeroFactura)) {
                    imprimirFull(idFactura);
                }
                // Caso 6: además ticket filtrado (dedupe via impreso_auto)
                if (esMiBodega && !cabeceraEstaImpresa(idFactura)) {
                    imprimirFiltrado(idFactura, frm_main.id_bodega);
                    marcarImpreso(idFactura);
                }
            } else {
                // ----- Casos 3 y 4 -----
                if (!esMiBodega) {
                    return; // caso 3
                }
                if (cabeceraEstaImpresa(idFactura)) {
                    return; // ya impresa
                }
                imprimirFull(idFactura);
                marcarImpreso(idFactura);
            }
        } finally {
            synchronized (enProceso) {
                enProceso.remove(idFactura);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Helpers de impresión
    // ------------------------------------------------------------------------

    private void imprimirFull(int idFactura) {
        try {
            new ver_factura_impresion().imprimir_termica_80mm_silencioso(
                    String.valueOf(idFactura), frm_main.print_service_user);
            System.out.println("[AutoImpresion] Full ticket impreso para orden " + idFactura);
        } catch (Exception ex) {
            System.out.println("[AutoImpresion] Error imprimiendo full ticket "
                    + idFactura + ": " + ex.getMessage());
        }
    }

    private void imprimirFiltrado(int idFactura, int idBodega) {
        try {
            new ver_factura_impresion().imprimir_termica_80mm_silencioso(
                    String.valueOf(idFactura), frm_main.print_service_user, Integer.valueOf(idBodega));
            System.out.println("[AutoImpresion] Filtrado bodega " + idBodega
                    + " impreso para orden " + idFactura);
        } catch (Exception ex) {
            System.out.println("[AutoImpresion] Error imprimiendo filtrado "
                    + idFactura + ": " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // Helpers de BD
    // ------------------------------------------------------------------------

    private String lookupNumeroFactura(int idFactura) {
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT codigo FROM facturas_cabeceras WHERE id = " + idFactura)) {
            return rs.next() ? rs.getString("codigo") : null;
        } catch (Exception ex) {
            System.out.println("[AutoImpresion] Error obteniendo numero_factura: " + ex.getMessage());
            return null;
        }
    }

    private boolean cabeceraEstaImpresa(int idFactura) {
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT impreso_auto FROM facturas_cabeceras WHERE id = " + idFactura)) {
            return rs.next() && rs.getBoolean("impreso_auto");
        } catch (Exception ex) {
            // En caso de error asumimos impresa para evitar duplicar.
            return true;
        }
    }

    /**
     * Inserta una entrada (id_user, numero_factura) en la bitácora.
     * @return true si efectivamente insertó (primera vez); false si ya existía.
     */
    private boolean tryInsertLogCompleto(String numeroFactura) {
        Connection c = null;
        try {
            c = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO auto_impresion_log_completo (id_user, numero_factura) VALUES (?, ?)")) {
                ps.setInt(1, frm_main.id_user);
                ps.setString(2, numeroFactura);
                ps.executeUpdate();
                return true;
            } catch (SQLException ex) {
                // 23505 = unique_violation -> ya estaba
                if ("23505".equals(ex.getSQLState())) {
                    return false;
                }
                throw ex;
            }
        } catch (Exception ex) {
            System.out.println("[AutoImpresion] Error insertando log_completo: " + ex.getMessage());
            return false; // por seguridad, no reimprimir si falla
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Al arrancar el servicio: marca todas las cabeceras del día de MI bodega
     * como ya impresas (para que no se reimpriman en pendientes), y si soy B=true
     * también pre-popula la bitácora de tickets completos con todas las facturas
     * del día para no replayar el ticket completo.
     *
     * Sólo toca DATOS DE MI BODEGA / MI USUARIO. No afecta a otros usuarios.
     */
    private void marcarExistentesComoProcesadas() {
        // 1) Cabeceras de mi bodega -> impreso_auto = true
        String sqlUpd = "UPDATE facturas_cabeceras "
                + "SET impreso_auto = true "
                + "WHERE impreso_auto = false "
                + "AND id_bodega = " + frm_main.id_bodega + " "
                + "AND fecha = CURRENT_DATE "
                + "AND tipo_factura <> 'Venta'";
        try (Statement st = conn.createStatement()) {
            int rows = st.executeUpdate(sqlUpd);
            if (rows > 0) {
                System.out.println("[AutoImpresion] " + rows
                        + " cabeceras existentes marcadas como impresas (bodega del usuario)");
            }
        } catch (Exception ex) {
            System.out.println("[AutoImpresion] Error marcando existentes: " + ex.getMessage());
        }

        // 2) Si soy B=true, pre-popular log de tickets completos con TODAS las
        //    facturas del día (para no reimprimir el full al hacer login).
        if (frm_main.imp_ticket_bodega_asignada) {
            // 2a. Facturas que SÍ tienen cabeceras (flujo normal)
            String sqlIns = "INSERT INTO auto_impresion_log_completo (id_user, numero_factura) "
                    + "SELECT DISTINCT " + frm_main.id_user + ", fc.codigo "
                    + "FROM facturas_cabeceras fc "
                    + "WHERE fc.fecha = CURRENT_DATE "
                    + "AND fc.codigo IS NOT NULL "
                    + "AND fc.tipo_factura <> 'Venta' "
                    + "AND NOT EXISTS ( "
                    + "  SELECT 1 FROM auto_impresion_log_completo l "
                    + "  WHERE l.id_user = " + frm_main.id_user + " "
                    + "  AND l.numero_factura = fc.codigo "
                    + ")";
            try (Statement st = conn.createStatement()) {
                int rows = st.executeUpdate(sqlIns);
                if (rows > 0) {
                    System.out.println("[AutoImpresion] " + rows
                            + " facturas existentes registradas en log_completo");
                }
            } catch (Exception ex) {
                System.out.println("[AutoImpresion] Error pre-populando log: " + ex.getMessage());
            }

            // 2b. Facturas SIN cabeceras (solo novedades) — wo-printer
            String sqlInsNov = "INSERT INTO auto_impresion_log_completo (id_user, numero_factura) "
                    + "SELECT " + frm_main.id_user + ", fi.numero_factura "
                    + "FROM facturas_impresas fi "
                    + "WHERE fi.fecha_factura = CURRENT_DATE "
                    + "AND fi.numero_factura IS NOT NULL "
                    + "AND NOT EXISTS ( "
                    + "  SELECT 1 FROM facturas_cabeceras WHERE codigo = fi.numero_factura "
                    + ") "
                    + "AND NOT EXISTS ( "
                    + "  SELECT 1 FROM auto_impresion_log_completo l "
                    + "  WHERE l.id_user = " + frm_main.id_user + " "
                    + "  AND l.numero_factura = fi.numero_factura "
                    + ")";
            try (Statement st = conn.createStatement()) {
                int rows = st.executeUpdate(sqlInsNov);
                if (rows > 0) {
                    System.out.println("[AutoImpresion] " + rows
                            + " facturas solo-novedad existentes registradas en log");
                }
            } catch (Exception ex) {
                // Si la tabla facturas_impresas no existe (entorno sin wo-printer), ignorar.
                System.out.println("[AutoImpresion] (omitiendo log solo-novedad: " + ex.getMessage() + ")");
            }
        }
    }

    private void marcarImpreso(int idFactura) {
        Connection c = null;
        try {
            c = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE facturas_cabeceras SET impreso_auto = true WHERE id = ?")) {
                ps.setInt(1, idFactura);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            System.out.println("[AutoImpresion] Error marcando impreso_auto: " + ex.getMessage());
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
