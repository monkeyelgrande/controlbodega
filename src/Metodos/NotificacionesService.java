package Metodos;

import Formularios.frm_main;
import conexiondb.DB_consultas_R_D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.SwingUtilities;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

/**
 * Listener Postgres dedicado para alimentar la barra lateral de notificaciones.
 * Escucha el mismo canal "orden_nueva" que AutoImpresionOrdenesService — ambos
 * servicios pueden coexistir porque cada uno mantiene su propia conexion LISTEN.
 *
 * Reglas:
 *   - Solo dispara para tipo_factura distinto de 'Venta' (igual que el trigger).
 *   - Ignora ordenes creadas por el usuario logueado (evita ruido visual).
 *   - Para payloads "NOVEDAD|..." (factura sin cabeceras), tambien notifica.
 *   - Reconecta con backoff si la conexion se cae.
 */
public class NotificacionesService {

    private static final String CANAL = "orden_nueva";
    private static final long POLL_MS = 30_000L;
    private static final long BACKOFF_INICIAL_MS = 5_000L;
    private static final long BACKOFF_MAX_MS = 60_000L;

    private static NotificacionesService instancia;

    private volatile boolean activo = false;
    private Thread hilo;
    private Connection conn;

    private NotificacionesService() {
    }

    public static synchronized NotificacionesService getInstance() {
        if (instancia == null) {
            instancia = new NotificacionesService();
        }
        return instancia;
    }

    public synchronized void iniciar() {
        if (activo) {
            return;
        }
        activo = true;
        hilo = new Thread(this::loop, "barra-notificaciones");
        hilo.setDaemon(true);
        hilo.start();
        System.out.println("[Notificaciones] Servicio iniciado para usuario " + frm_main.id_user);
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
        System.out.println("[Notificaciones] Servicio detenido");
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
                System.out.println("[Notificaciones] Error: " + ex.getMessage()
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
            throw new IllegalStateException("No se obtuvo conexion");
        }
        try (Statement st = conn.createStatement()) {
            st.execute("LISTEN " + CANAL);
        }

        PGConnection pgConn = conn.unwrap(PGConnection.class);
        while (activo) {
            PGNotification[] notifs = pgConn.getNotifications((int) POLL_MS);
            if (!activo) {
                return;
            }
            if (notifs != null) {
                for (PGNotification n : notifs) {
                    procesarPayload(n.getParameter());
                }
            }
            if (conn.isClosed()) {
                throw new IllegalStateException("Conexion cerrada");
            }
        }
    }

    private void procesarPayload(String payload) {
        if (payload == null) {
            return;
        }

        if (payload.startsWith("NOVEDAD|")) {
            String[] partes = payload.split("\\|", -1);
            if (partes.length < 3) return;
            String numeroFactura = partes[2];
            if (numeroFactura == null || numeroFactura.isEmpty()) return;
            // Sin cabecera real: pedimos los datos por numero_factura
            buscarYEnviarPorNumero(numeroFactura);
            return;
        }

        // Formato: id|id_bodega|id_user|tipo_factura
        String[] partes = payload.split("\\|", -1);
        if (partes.length < 4) return;
        int idFactura, idUserCreador;
        try {
            idFactura = Integer.parseInt(partes[0]);
            idUserCreador = partes[2].isEmpty() ? -1 : Integer.parseInt(partes[2]);
        } catch (NumberFormatException nfe) {
            return;
        }
        String tipo = partes[3];
        if ("Venta".equals(tipo)) return;
        if (idUserCreador == frm_main.id_user) return;  // no avisar de mis propias ordenes

        buscarYEnviarPorId(idFactura);
    }

    private void buscarYEnviarPorId(int idFactura) {
        Connection c = null;
        try {
            c = DB_consultas_R_D.getConexion();
            String sql = "SELECT fc.id, fc.codigo, "
                    + "       COALESCE(co.nombre, '-') AS cliente, "
                    + "       COALESCE(b.nombre, '-')  AS bodega "
                    + "FROM facturas_cabeceras fc "
                    + "LEFT JOIN contactos co ON co.id = fc.id_contacto "
                    + "LEFT JOIN bodegas   b  ON b.id  = fc.id_bodega "
                    + "WHERE fc.id = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, idFactura);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        empujar(rs.getInt("id"), rs.getString("codigo"),
                                rs.getString("cliente"), rs.getString("bodega"));
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("[Notificaciones] Error consultando orden " + idFactura
                    + ": " + ex.getMessage());
        } finally {
            try { if (c != null) c.close(); } catch (Exception ignored) {}
        }
    }

    private void buscarYEnviarPorNumero(String numeroFactura) {
        Connection c = null;
        try {
            c = DB_consultas_R_D.getConexion();
            String sql = "SELECT numero_factura, COALESCE(cliente, '-') AS cliente "
                    + "FROM facturas_impresas "
                    + "WHERE numero_factura = ? "
                    + "ORDER BY id DESC LIMIT 1";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, numeroFactura);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        empujar(0, rs.getString("numero_factura"),
                                rs.getString("cliente"), "-");
                    } else {
                        empujar(0, numeroFactura, "-", "-");
                    }
                }
            }
        } catch (Exception ex) {
            // facturas_impresas puede no existir en algunos entornos
            empujar(0, numeroFactura, "-", "-");
        } finally {
            try { if (c != null) c.close(); } catch (Exception ignored) {}
        }
    }

    private void empujar(final int idOrden, final String codigo,
                         final String cliente, final String bodega) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (frm_main.barraNotif != null) {
                    frm_main.barraNotif.agregarNotificacion(idOrden, codigo, cliente, bodega);
                }
            }
        });
    }
}
