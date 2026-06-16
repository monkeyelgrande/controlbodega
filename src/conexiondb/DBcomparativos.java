/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * Acceso a datos del Comparativo de cotizaciones (RF-04). La matriz se compone
 * de: cabecera (parámetros), productos (filas), proveedores (columnas) y precios
 * (celdas precio de lista sin IVA).
 *
 * @author Monkeyelgrande
 */
public class DBcomparativos {

    public static class Proveedor {
        public int idCompProv;
        public int idProveedor;
        public String nombre;
        public double descuento;
        public double flete;
        public String condicion;
    }

    private static int sigId(Connection con, String tabla) throws SQLException {
        try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id),0)+1 AS id FROM " + tabla)) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return 1;
    }

    /** Crea un comparativo a partir de los ítems seleccionados de un sugerido. */
    public int crearDesdeSugerido(int idSugerido, int idUser) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);
            int id = sigId(con, "comparativos_cabecera");
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO comparativos_cabecera (id,numero,id_sugerido,id_user,fecha,hora,iva_pct,capacidad_camion_ton,estado) "
                    + "VALUES (?,?,?,?,current_date,current_time,0.19,30,0)")) {
                ps.setInt(1, id);
                ps.setString(2, "COMP-" + id);
                ps.setInt(3, idSugerido);
                ps.setInt(4, idUser);
                ps.executeUpdate();
            }
            int idProd = sigId(con, "comparativos_productos");
            String sel = "SELECT d.id_producto, COALESCE(d.cantidad_final, d.cantidad_sugerida) AS cant, "
                    + "p.peso_unitario FROM sugeridos_detalle d JOIN productos p ON p.id=d.id_producto "
                    + "WHERE d.id_sugerido_cab=" + idSugerido + " AND d.seleccionado=true ORDER BY d.id";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sel)) {
                int pos = 1;
                while (rs.next()) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO comparativos_productos (id,id_comparativo,id_producto,cantidad,peso_unitario,posicion) "
                            + "VALUES (?,?,?,?,?,?)")) {
                        ps.setInt(1, idProd++);
                        ps.setInt(2, id);
                        ps.setInt(3, rs.getInt("id_producto"));
                        ps.setDouble(4, rs.getDouble("cant"));
                        double peso = rs.getDouble("peso_unitario");
                        if (rs.wasNull()) {
                            ps.setNull(5, java.sql.Types.NUMERIC);
                        } else {
                            ps.setDouble(5, peso);
                        }
                        ps.setInt(6, pos++);
                        ps.executeUpdate();
                    }
                }
            }
            // marca el sugerido como procesado
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE sugeridos_cabecera SET estado=2 WHERE id=?")) {
                ps.setInt(1, idSugerido);
                ps.executeUpdate();
            }
            con.commit();
            return id;
        } catch (SQLException e) {
            rollback(con);
            JOptionPane.showMessageDialog(null, "Error al generar el comparativo:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        } finally {
            cerrar(con);
        }
    }

    public static ResultSet listar(int filtroEstado) {
        String sql = "SELECT c.id, c.numero, c.fecha, c.estado, COALESCE(u.nombre,'') AS creador, "
                + "(SELECT COUNT(*) FROM comparativos_productos p WHERE p.id_comparativo=c.id) AS items, "
                + "(SELECT COUNT(*) FROM comparativos_proveedores v WHERE v.id_comparativo=c.id) AS provs "
                + "FROM comparativos_cabecera c LEFT JOIN users u ON u.id=c.id_user ";
        if (filtroEstado >= 0) {
            sql += "WHERE c.estado=" + filtroEstado + " ";
        }
        sql += "ORDER BY c.id DESC";
        return DB_consultas_R_D.getTabla(sql);
    }

    public static ResultSet cargarCabecera(int id) {
        return DB_consultas_R_D.getTabla(
                "SELECT c.*, COALESCE(u.nombre,'') AS creador, COALESCE(ua.nombre,'') AS autoriza, "
                + "COALESCE(pu.nombre,'') AS proveedor_unico "
                + "FROM comparativos_cabecera c "
                + "LEFT JOIN users u ON u.id=c.id_user "
                + "LEFT JOIN users ua ON ua.id=c.id_user_autoriza "
                + "LEFT JOIN contactos pu ON pu.id=c.id_proveedor_unico WHERE c.id=" + id);
    }

    public static ResultSet cargarProductos(int idComparativo) {
        return DB_consultas_R_D.getTabla(
                "SELECT cp.id, cp.id_producto, p.codigo_barras, p.descripcion, cp.cantidad, "
                + "COALESCE(cp.peso_unitario,0) AS peso_unitario "
                + "FROM comparativos_productos cp JOIN productos p ON p.id=cp.id_producto "
                + "WHERE cp.id_comparativo=" + idComparativo + " ORDER BY cp.posicion, cp.id");
    }

    public static ResultSet cargarProveedores(int idComparativo) {
        return DB_consultas_R_D.getTabla(
                "SELECT cv.id, cv.id_proveedor, c.nombre, cv.descuento_pronto_pago, cv.flete, "
                + "COALESCE(cv.condicion_pago,'') AS condicion_pago "
                + "FROM comparativos_proveedores cv JOIN contactos c ON c.id=cv.id_proveedor "
                + "WHERE cv.id_comparativo=" + idComparativo + " ORDER BY cv.posicion, cv.id");
    }

    public static ResultSet cargarPrecios(int idComparativo) {
        return DB_consultas_R_D.getTabla(
                "SELECT id_comp_producto, id_comp_proveedor, precio_lista "
                + "FROM comparativos_precios WHERE id_comparativo=" + idComparativo);
    }

    public int agregarProveedor(int idComparativo, int idProveedor) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            int id = sigId(con, "comparativos_proveedores");
            int pos = id;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO comparativos_proveedores (id,id_comparativo,id_proveedor,descuento_pronto_pago,flete,posicion) "
                    + "VALUES (?,?,?,0,0,?)")) {
                ps.setInt(1, id);
                ps.setInt(2, idComparativo);
                ps.setInt(3, idProveedor);
                ps.setInt(4, pos);
                ps.executeUpdate();
            }
            return id;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al agregar proveedor:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        } finally {
            cerrar(con);
        }
    }

    public boolean quitarProveedor(int idCompProv) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM comparativos_precios WHERE id_comp_proveedor=?")) {
                ps.setInt(1, idCompProv);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM comparativos_proveedores WHERE id=?")) {
                ps.setInt(1, idCompProv);
                ps.executeUpdate();
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            rollback(con);
            return false;
        } finally {
            cerrar(con);
        }
    }

    /**
     * Guarda los parámetros de cada proveedor (descuento, flete, condición),
     * la cabecera (IVA, capacidad camión) y todas las celdas de precio.
     */
    public boolean guardarMatriz(int idComparativo, double ivaPct, double capacidadCamion,
            List<Proveedor> proveedores, int[][] precioKeys, double[] precioVals) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE comparativos_cabecera SET iva_pct=?, capacidad_camion_ton=? WHERE id=?")) {
                ps.setDouble(1, ivaPct);
                ps.setDouble(2, capacidadCamion);
                ps.setInt(3, idComparativo);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE comparativos_proveedores SET descuento_pronto_pago=?, flete=?, condicion_pago=? WHERE id=?")) {
                for (Proveedor pv : proveedores) {
                    ps.setDouble(1, pv.descuento);
                    ps.setDouble(2, pv.flete);
                    ps.setString(3, pv.condicion);
                    ps.setInt(4, pv.idCompProv);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM comparativos_precios WHERE id_comparativo=?")) {
                ps.setInt(1, idComparativo);
                ps.executeUpdate();
            }
            int idCelda = sigId(con, "comparativos_precios");
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO comparativos_precios (id,id_comparativo,id_comp_producto,id_comp_proveedor,precio_lista) "
                    + "VALUES (?,?,?,?,?)")) {
                for (int i = 0; i < precioVals.length; i++) {
                    if (Double.isNaN(precioVals[i])) {
                        continue;
                    }
                    ps.setInt(1, idCelda++);
                    ps.setInt(2, idComparativo);
                    ps.setInt(3, precioKeys[i][0]);
                    ps.setInt(4, precioKeys[i][1]);
                    ps.setDouble(5, precioVals[i]);
                    ps.executeUpdate();
                }
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            rollback(con);
            JOptionPane.showMessageDialog(null, "Error al guardar el comparativo:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            cerrar(con);
        }
    }

    public int decidirYAutorizar(int idComparativo, String decision, Integer idProveedorUnico, int idUserAutoriza) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE comparativos_cabecera SET decision=?, id_proveedor_unico=?, estado=2, "
                    + "id_user_autoriza=?, fecha_autorizacion=now() WHERE id=?")) {
                ps.setString(1, decision);
                if (idProveedorUnico != null && idProveedorUnico > 0) {
                    ps.setInt(2, idProveedorUnico);
                } else {
                    ps.setNull(2, java.sql.Types.INTEGER);
                }
                ps.setInt(3, idUserAutoriza);
                ps.setInt(4, idComparativo);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al autorizar:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return 0;
        } finally {
            cerrar(con);
        }
    }

    private void rollback(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ex) {
            }
        }
    }

    private void cerrar(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
            }
        }
    }
}
