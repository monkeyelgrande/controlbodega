package conexiondb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Puente local del modulo Precios hacia los ingresos de mercancia clasicos:
 * al "enviar a bodega" un ingreso de productos, crea ingreso(s) de mercancia
 * en estado PENDIENTE (uno por bodega) para que el inventario entre por el
 * flujo normal de controlbodega.
 *
 * Version local del antiguo DBreplicaBodega de productos-agroinsumos: como las
 * tablas de productos y contactos ya estan unificadas, se referencian los ids
 * directamente y no se crea nada.
 */
public class ReplicaIngresoOrden {

    private static final int ID_TIPO_FACTURA = 3;  // tipo_ingreso = FACTURA
    private static final int ESTADO_PENDIENTE = 0; // no afecta inventario hasta recibirse

    /** Una linea del ingreso a enviar. */
    public static class Linea {

        public final int idProducto;
        public final double cantidad;
        public final int idBodega;

        public Linea(int idProducto, double cantidad, int idBodega) {
            this.idProducto = idProducto;
            this.cantidad = cantidad;
            this.idBodega = idBodega;
        }
    }

    /** Resumen del resultado del envio. */
    public static class Resultado {

        public int cabecerasCreadas = 0;
        public boolean ok = true;
        public List<String> errores = new ArrayList<>();
    }

    /**
     * Crea ingreso(s) de mercancia PENDIENTES agrupando las lineas por bodega.
     *
     * @param lineas         lineas (id_producto, cantidad, id_bodega)
     * @param idProveedor    id en contactos, o 0 si no hay proveedor
     * @param noFactura      numero de factura del proveedor
     * @param fecha          'YYYY-MM-DD' (sin comillas)
     * @param hora           'HH:MM:SS' (sin comillas)
     * @param nombreUsuario  usuario creador (trazabilidad en la descripcion)
     * @param observacion    observacion del ingreso (puede ser null/"")
     * @param idUser         id del usuario en users
     */
    public Resultado enviar(List<Linea> lineas, int idProveedor, String noFactura,
            String fecha, String hora, String nombreUsuario, String observacion, int idUser) {

        Resultado res = new Resultado();
        if (lineas == null || lineas.isEmpty()) {
            res.ok = false;
            res.errores.add("No hay líneas para enviar.");
            return res;
        }

        String descripcion = "Creado por: " + (nombreUsuario == null ? "" : nombreUsuario)
                + " (módulo Precios)";
        if (observacion != null && !observacion.trim().isEmpty()) {
            descripcion += " | " + observacion.trim();
        }

        // Agrupar por bodega conservando el orden
        Map<Integer, List<Linea>> porBodega = new LinkedHashMap<>();
        for (Linea l : lineas) {
            List<Linea> lista = porBodega.get(l.idBodega);
            if (lista == null) {
                lista = new ArrayList<>();
                porBodega.put(l.idBodega, lista);
            }
            lista.add(l);
        }

        for (Map.Entry<Integer, List<Linea>> entry : porBodega.entrySet()) {
            int idBodega = entry.getKey();
            Connection con = null;
            try {
                con = DB_consultas_R_D.getConexion();
                con.setAutoCommit(false);

                int idCabecera = siguienteId(con, "ingresos_mercancias_cabecera");
                String sqlCab = "INSERT INTO ingresos_mercancias_cabecera "
                        + "(id, id_proveedor, id_transportador, id_user, id_tipo, fecha, hora, no_factura, estado, id_bodega, fecha_vencimiento, descripcion) "
                        + "VALUES (" + idCabecera + ", "
                        + (idProveedor > 0 ? idProveedor : "NULL") + ", NULL, "
                        + idUser + ", " + ID_TIPO_FACTURA + ", "
                        + "'" + fecha + "', '" + hora + "', '" + q(noFactura) + "', "
                        + ESTADO_PENDIENTE + ", " + idBodega + ", "
                        + "'" + fecha + "', '" + q(descripcion) + "')";
                ejecutar(con, sqlCab);

                for (Linea l : entry.getValue()) {
                    String sqlDet = "INSERT INTO ingresos_mercancias_detalle "
                            + "(id, id_producto, cantidad, id_ingreso_cabecera, precio_costo) "
                            + "VALUES ((SELECT COALESCE(MAX(id),0)+1 FROM ingresos_mercancias_detalle), "
                            + l.idProducto + ", " + l.cantidad + ", " + idCabecera + ", 0)";
                    ejecutar(con, sqlDet);
                }

                con.commit();
                res.cabecerasCreadas++;
            } catch (SQLException e) {
                res.ok = false;
                res.errores.add("Bodega " + idBodega + ": " + e.getMessage());
                if (con != null) {
                    try {
                        con.rollback();
                    } catch (SQLException ex) {
                        System.err.println("Error en rollback: " + ex.getMessage());
                    }
                }
            } finally {
                if (con != null) {
                    try {
                        con.setAutoCommit(true);
                        con.close();
                    } catch (SQLException ex) {
                        System.err.println("Error al cerrar conexión: " + ex.getMessage());
                    }
                }
            }
        }
        return res;
    }

    private int siguienteId(Connection con, String tabla) throws SQLException {
        try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id),0)+1 AS id FROM " + tabla)) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return 1;
    }

    private void ejecutar(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private String q(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("'", "''");
    }
}
