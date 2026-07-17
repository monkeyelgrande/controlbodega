package Metodos;

import conexiondb.DB_consultas_R_D;
import conexiondb.DBstock_productos;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logica de negocio del modulo Entregas Rapidas (lectura QR + tactil).
 *
 * - Parsea el contenido del QR generado en ImprimirFacturaPDF ("ORDEN-<id>").
 * - Resuelve la orden, valida bodega y estado.
 * - Calcula productos pendientes.
 * - Registra cada lectura en escaneos_qr_ordenes.
 * - Ejecuta entrega completa o parcial reusando entregas_productos / entregas_productos_cabecera
 *   y DBstock_productos.entrega() para el movimiento de stock.
 */
public class EntregaQRService {

    private static final Pattern QR_PATTERN = Pattern.compile("^ORDEN-(\\d+)$");

    public enum Resultado {
        OK,
        QR_INVALIDO,
        ORDEN_NO_EXISTE,
        OTRA_BODEGA,
        ANULADA,
        YA_ENTREGADA
    }

    public enum Accion {
        NINGUNA,
        ENTREGA_COMPLETA,
        ENTREGA_PARCIAL
    }

    // =====================================================================
    // Estructuras
    // =====================================================================

    public static class OrdenInfo {
        public int idFactura;
        public String codigoFactura;     // codigo legible (ORD-...)
        public int idCliente;
        public String nombreCliente;
        public String cedulaCliente;
        public String fecha;
        public String tipoFactura;
        public int idBodegaOrden;
        public String nombreBodegaOrden;
        public String observacion;       // facturas_cabeceras.observacion
        public boolean anulada;          // true si la orden esta anulada
        public List<ProductoPendiente> productos = new ArrayList<>();

        public double getTotalPendiente() {
            double t = 0.0;
            for (ProductoPendiente p : productos) t += p.pendiente;
            return t;
        }
    }

    public static class ProductoPendiente {
        public int idProducto;
        public String codigo;
        public String descripcion;
        public double pedido;
        public double entregado;
        public double pendiente;
        public double stockBodega;       // stock actual en la bodega de la orden
    }

    public static class ResultadoEscaneo {
        public Resultado resultado;
        public OrdenInfo orden;          // null si la lectura no resolvio una orden valida
        public int idEscaneo;            // id en escaneos_qr_ordenes
        public String mensaje;
    }

    // =====================================================================
    // API publica
    // =====================================================================

    /**
     * Procesa la lectura cruda del lector QR. Registra siempre el escaneo.
     */
    public static ResultadoEscaneo procesarLectura(String qrCrudo, int idUser, int idBodegaUsuario) {
        ResultadoEscaneo r = new ResultadoEscaneo();
        String texto = qrCrudo == null ? "" : qrCrudo.trim();

        Integer idFactura = parsearIdOrden(texto);
        if (idFactura == null) {
            r.resultado = Resultado.QR_INVALIDO;
            r.mensaje = "QR no reconocido. Esperado formato 'ORDEN-<id>'.";
            r.idEscaneo = registrarEscaneo(null, idUser, idBodegaUsuario, texto, r.resultado, Accion.NINGUNA, null);
            return r;
        }

        OrdenInfo info = cargarOrden(idFactura);
        if (info == null) {
            r.resultado = Resultado.ORDEN_NO_EXISTE;
            r.mensaje = "La orden " + idFactura + " no existe.";
            r.idEscaneo = registrarEscaneo(idFactura, idUser, idBodegaUsuario, texto, r.resultado, Accion.NINGUNA, null);
            return r;
        }

        if (info.anulada) {
            r.resultado = Resultado.ANULADA;
            r.orden = info;
            r.mensaje = "La orden " + info.codigoFactura + " esta anulada.";
            r.idEscaneo = registrarEscaneo(idFactura, idUser, idBodegaUsuario, texto, r.resultado, Accion.NINGUNA, null);
            return r;
        }

        if (info.idBodegaOrden != idBodegaUsuario) {
            r.resultado = Resultado.OTRA_BODEGA;
            r.orden = info;
            r.mensaje = "La orden pertenece a la bodega '" + info.nombreBodegaOrden
                    + "'. Tu sesion esta en otra bodega.";
            r.idEscaneo = registrarEscaneo(idFactura, idUser, idBodegaUsuario, texto, r.resultado, Accion.NINGUNA, null);
            return r;
        }

        if (info.getTotalPendiente() <= 0) {
            r.resultado = Resultado.YA_ENTREGADA;
            r.orden = info;
            r.mensaje = "La orden ya fue entregada por completo.";
            r.idEscaneo = registrarEscaneo(idFactura, idUser, idBodegaUsuario, texto, r.resultado, Accion.NINGUNA, null);
            return r;
        }

        r.resultado = Resultado.OK;
        r.orden = info;
        r.idEscaneo = registrarEscaneo(idFactura, idUser, idBodegaUsuario, texto, r.resultado, Accion.NINGUNA, null);
        return r;
    }

    /**
     * Ejecuta una entrega (completa o parcial). Para cada producto se entrega
     * la cantidad indicada en {@code cantidadesPorProducto} (mapa idProducto -> cantidad).
     * Si la cantidad es 0 se omite la fila. La orden y el contexto provienen de un
     * escaneo previo, cuyo idEscaneo se actualiza con la accion realizada.
     *
     * Devuelve el id de la cabecera de entrega creada, o -1 si no se hizo nada.
     */
    public static int ejecutarEntrega(OrdenInfo orden,
                                      List<ItemEntrega> items,
                                      int idUser,
                                      int idBodegaEntrega,
                                      Accion accion,
                                      int idEscaneo) {

        if (items == null || items.isEmpty()) return -1;

        // filtra items con cantidad > 0
        List<ItemEntrega> efectivos = new ArrayList<>();
        for (ItemEntrega it : items) {
            if (it.cantidad > 0) efectivos.add(it);
        }
        if (efectivos.isEmpty()) return -1;

        // ESTRICTO: nunca registrar una entrega en una bodega distinta a la de la
        // orden. Es una barrera de seguridad: los llamadores ya deberían pasar la
        // bodega de la orden, pero si por cualquier motivo no coincide, se rechaza.
        if (orden == null || idBodegaEntrega != orden.idBodegaOrden) {
            System.out.println("EntregaQRService.ejecutarEntrega: bodega de entrega ("
                    + idBodegaEntrega + ") distinta a la de la orden #"
                    + (orden == null ? "?" : orden.idFactura + " (bodega " + orden.idBodegaOrden + ")")
                    + ". Entrega rechazada.");
            return -1;
        }

        Connection con = null;
        boolean autoCommitPrev = true;
        try {
            con = DB_consultas_R_D.getConexion();
            autoCommitPrev = con.getAutoCommit();
            con.setAutoCommit(false);

            int idCab = Integer.parseInt(DB_consultas_R_D.cargarId("entregas_productos_cabecera"));
            long now = System.currentTimeMillis();
            java.sql.Date fechaSql = new java.sql.Date(now);
            String horaStr = DB_consultas_R_D.obtener_hora(); // hora_entrega es varchar(8)

            try (PreparedStatement ps = con.prepareStatement(
                    "insert into entregas_productos_cabecera "
                    + "(id, id_factura, id_user, fecha_entrega, hora_entrega, id_bodega) "
                    + "values (?, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, idCab);
                ps.setInt(2, orden.idFactura);
                ps.setInt(3, idUser);
                ps.setDate(4, fechaSql);
                ps.setString(5, horaStr);
                ps.setInt(6, idBodegaEntrega);
                ps.executeUpdate();
            }

            // detalles
            int idDetSeq = Integer.parseInt(DB_consultas_R_D.cargarId("entregas_productos"));
            for (ItemEntrega it : efectivos) {
                try (PreparedStatement ps = con.prepareStatement(
                        "insert into entregas_productos "
                        + "(id, id_cabecera, id_producto, cantidad, id_factura) "
                        + "values (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, idDetSeq++);
                    ps.setInt(2, idCab);
                    ps.setInt(3, it.idProducto);
                    ps.setDouble(4, it.cantidad);
                    ps.setInt(5, orden.idFactura);
                    ps.executeUpdate();
                }
            }

            con.commit();

            // Movimiento de stock (no transaccional con esta conexion porque
            // DBstock_productos abre su propia conexion). Se hace despues del commit
            // para que la cabecera de entrega exista al ser referenciada.
            DBstock_productos dbStock = new DBstock_productos();
            for (ItemEntrega it : efectivos) {
                String obs = (accion == Accion.ENTREGA_COMPLETA)
                        ? "Entrega completa QR - Orden #" + orden.idFactura
                        : "Entrega parcial QR - Orden #" + orden.idFactura;
                dbStock.entrega(it.idProducto, idBodegaEntrega, idUser, it.cantidad, idCab, obs);
            }

            // Marcar escaneo
            actualizarAccionEscaneo(idEscaneo, accion, idCab);

            return idCab;
        } catch (Exception ex) {
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            System.out.println("EntregaQRService.ejecutarEntrega error: " + ex);
            return -1;
        } finally {
            try { if (con != null) con.setAutoCommit(autoCommitPrev); } catch (Exception ignore) {}
        }
    }

    /**
     * Entrega por completo una orden (todos sus pendientes) sin pasar por un
     * escaneo QR. Pensada para la entrega masiva por bodega desde frm_Ordenes.
     *
     * La entrega se acredita al usuario indicado y se registra en la bodega de
     * la propia orden. Entrega aunque el stock quede negativo (mismo criterio
     * que la entrega manual). Reusa {@link #ejecutarEntrega} para mover el
     * stock exactamente igual que la entrega manual o por QR.
     *
     * @return id de la cabecera de entrega creada, o -1 si la orden no existe,
     * está anulada o no tiene pendientes.
     */
    public static int entregarOrdenCompleta(int idFactura, int idUser) {
        OrdenInfo info = cargarOrden(idFactura);
        if (info == null || info.anulada) {
            return -1;
        }
        if (info.getTotalPendiente() <= 0) {
            return -1;
        }

        List<ItemEntrega> items = new ArrayList<>();
        for (ProductoPendiente p : info.productos) {
            if (p.pendiente > 0) {
                items.add(new ItemEntrega(p.idProducto, p.codigo, p.descripcion, p.pendiente));
            }
        }
        if (items.isEmpty()) {
            return -1;
        }

        // idEscaneo = -1: no proviene de un escaneo, ejecutarEntrega lo ignora.
        return ejecutarEntrega(info, items, idUser, info.idBodegaOrden,
                Accion.ENTREGA_COMPLETA, -1);
    }

    /** Item simple para construir la entrega. */
    public static class ItemEntrega {
        public final int idProducto;
        public final String codigo;
        public final String descripcion;
        public final double cantidad;

        public ItemEntrega(int idProducto, String codigo, String descripcion, double cantidad) {
            this.idProducto = idProducto;
            this.codigo = codigo;
            this.descripcion = descripcion;
            this.cantidad = cantidad;
        }
    }

    // =====================================================================
    // Internos
    // =====================================================================

    private static Integer parsearIdOrden(String texto) {
        if (texto == null) return null;
        Matcher m = QR_PATTERN.matcher(texto);
        if (!m.matches()) return null;
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static OrdenInfo cargarOrden(int idFactura) {
        OrdenInfo info = null;

        // Cabecera + cliente + bodega
        String sqlCab =
                "SELECT fc.id, fc.codigo, fc.fecha, fc.tipo_factura, fc.anulado, "
                + "       fc.observacion, "
                + "       fc.id_bodega, b.nombre AS bodega, "
                + "       c.id AS id_cliente, c.nombre AS cliente, c.cedula AS cedula "
                + "FROM facturas_cabeceras fc "
                + "LEFT JOIN bodegas b ON b.id = fc.id_bodega "
                + "LEFT JOIN contactos c ON c.id = fc.id_contacto "
                + "WHERE fc.id = " + idFactura;

        try (ResultSet rs = DB_consultas_R_D.getTabla(sqlCab)) {
            if (rs != null && rs.next()) {
                info = new OrdenInfo();
                info.idFactura = rs.getInt("id");
                info.codigoFactura = rs.getString("codigo");
                info.fecha = rs.getString("fecha");
                info.tipoFactura = rs.getString("tipo_factura");
                int anulado = rs.getInt("anulado");
                // En el codigo existente: anulado = 1 significa "activa".
                info.anulada = (anulado != 1);
                info.idBodegaOrden = rs.getInt("id_bodega");
                info.nombreBodegaOrden = rs.getString("bodega");
                info.idCliente = rs.getInt("id_cliente");
                info.nombreCliente = rs.getString("cliente");
                info.cedulaCliente = rs.getString("cedula");
                info.observacion = rs.getString("observacion");
            }
        } catch (Exception e) {
            System.out.println("EntregaQRService.cargarOrden error: " + e);
        }

        if (info == null) return null;

        // Productos pendientes
        String sqlDet =
                "WITH entregas AS ("
                + "  SELECT id_producto, SUM(cantidad) AS total_entregado "
                + "  FROM entregas_productos WHERE id_factura = " + idFactura
                + "  GROUP BY id_producto "
                + ") "
                + "SELECT fd.id_producto, p.codigo_barras AS codigo, p.descripcion, "
                + "       SUM(fd.cantidad) AS pedido, "
                + "       COALESCE(MAX(e.total_entregado), 0) AS entregado "
                + "FROM facturas_detalles fd "
                + "JOIN productos p ON p.id = fd.id_producto "
                + "LEFT JOIN entregas e ON e.id_producto = fd.id_producto "
                + "WHERE fd.id_cabecera = " + idFactura
                + " GROUP BY fd.id_producto, p.codigo_barras, p.descripcion "
                + " ORDER BY p.descripcion";

        try (ResultSet rs = DB_consultas_R_D.getTabla(sqlDet)) {
            DBstock_productos dbStock = new DBstock_productos();
            while (rs != null && rs.next()) {
                ProductoPendiente pp = new ProductoPendiente();
                pp.idProducto = rs.getInt("id_producto");
                pp.codigo = rs.getString("codigo");
                pp.descripcion = rs.getString("descripcion");
                pp.pedido = rs.getDouble("pedido");
                pp.entregado = rs.getDouble("entregado");
                pp.pendiente = Math.max(0.0, pp.pedido - pp.entregado);

                try {
                    double[] stock = dbStock.getStock(pp.idProducto, info.idBodegaOrden);
                    pp.stockBodega = (stock != null && stock.length > 0) ? stock[0] : 0.0;
                } catch (Exception ignore) {
                    pp.stockBodega = 0.0;
                }

                info.productos.add(pp);
            }
        } catch (Exception e) {
            System.out.println("EntregaQRService.cargarOrden detalle error: " + e);
        }

        return info;
    }

    /** Inserta una fila en escaneos_qr_ordenes y devuelve el id generado. */
    public static int registrarEscaneo(Integer idFactura, int idUser, int idBodega,
                                       String qrLeido, Resultado resultado,
                                       Accion accion, Integer idEntregaCab) {
        try {
            int id = Integer.parseInt(DB_consultas_R_D.cargarId("escaneos_qr_ordenes"));
            long now = System.currentTimeMillis();
            java.sql.Date fechaSql = new java.sql.Date(now);
            java.sql.Time horaSql  = new java.sql.Time(now);
            String pc = hostname();

            Connection con = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = con.prepareStatement(
                    "insert into escaneos_qr_ordenes "
                    + "(id, id_factura, id_user, id_bodega, fecha_escaneo, hora_escaneo, "
                    + " qr_leido, resultado, accion, id_entrega_cab, pc_origen) "
                    + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, id);
                if (idFactura == null) ps.setNull(2, java.sql.Types.INTEGER);
                else ps.setInt(2, idFactura);
                ps.setInt(3, idUser);
                ps.setInt(4, idBodega);
                ps.setDate(5, fechaSql);
                ps.setTime(6, horaSql);
                ps.setString(7, qrLeido != null ? truncate(qrLeido, 150) : null);
                ps.setString(8, resultado.name());
                ps.setString(9, accion.name());
                if (idEntregaCab == null) ps.setNull(10, java.sql.Types.INTEGER);
                else ps.setInt(10, idEntregaCab);
                ps.setString(11, pc);
                ps.executeUpdate();
            }
            return id;
        } catch (Exception ex) {
            System.out.println("EntregaQRService.registrarEscaneo error: " + ex);
            return -1;
        }
    }

    private static void actualizarAccionEscaneo(int idEscaneo, Accion accion, Integer idEntregaCab) {
        if (idEscaneo <= 0) return;
        try {
            Connection con = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = con.prepareStatement(
                    "update escaneos_qr_ordenes set accion = ?, id_entrega_cab = ? where id = ?")) {
                ps.setString(1, accion.name());
                if (idEntregaCab == null) ps.setNull(2, java.sql.Types.INTEGER);
                else ps.setInt(2, idEntregaCab);
                ps.setInt(3, idEscaneo);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            System.out.println("EntregaQRService.actualizarAccionEscaneo error: " + ex);
        }
    }

    private static String fechaHoy() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
