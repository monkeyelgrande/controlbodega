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
import modelos.Ordenes_compra_cabecera;
import modelos.Ordenes_compra_detalle;

/**
 * Acceso a datos del módulo de Órdenes de Compra.
 *
 * El guardado de cabecera + detalles se hace en una sola transacción
 * (setAutoCommit(false) + commit/rollback), siguiendo el patrón de
 * {@link DBfacturas_cabeceras}.
 *
 * @author Monkeyelgrande
 */
public class DBordenes_compra {

    /**
     * Calcula el siguiente id (COALESCE(MAX(id),0)+1) de una tabla usando la
     * conexión de la transacción en curso.
     */
    private int siguienteId(Connection con, String tabla) throws SQLException {
        try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id),0)+1 AS id FROM " + tabla)) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return 1;
    }

    private void setCabeceraParams(PreparedStatement ps, Ordenes_compra_cabecera oc) throws SQLException {
        ps.setInt(1, oc.getId());
        ps.setString(2, oc.getNumero());
        ps.setInt(3, oc.getId_user_crea());
        ps.setDate(4, oc.getFecha() != null ? java.sql.Date.valueOf(oc.getFecha()) : null);
        ps.setTime(5, oc.getHora() != null ? java.sql.Time.valueOf(oc.getHora()) : null);
        ps.setInt(6, oc.getEstado());
        ps.setString(7, oc.getObservacion());
        if (oc.getId_bodega() > 0) {
            ps.setInt(8, oc.getId_bodega());
        } else {
            ps.setNull(8, java.sql.Types.INTEGER);
        }
    }

    private void insertarDetalle(Connection con, Ordenes_compra_detalle d) throws SQLException {
        String sql = "INSERT INTO ordenes_compra_detalle "
                + "(id, id_orden_cabecera, id_producto, cantidad, id_proveedor, precio_unitario, observacion) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, d.getId());
            ps.setInt(2, d.getId_orden_cabecera());
            ps.setInt(3, d.getId_producto());
            ps.setDouble(4, d.getCantidad());
            if (d.getId_proveedor() != null && d.getId_proveedor() > 0) {
                ps.setInt(5, d.getId_proveedor());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            if (d.getPrecio_unitario() != null) {
                ps.setDouble(6, d.getPrecio_unitario());
            } else {
                ps.setNull(6, java.sql.Types.NUMERIC);
            }
            ps.setString(7, d.getObservacion());
            ps.executeUpdate();
        }
    }

    /**
     * Guarda una orden nueva (cabecera + detalles) en una sola transacción.
     * Asigna a {@code oc} el id generado.
     *
     * @return true si se guardó correctamente.
     */
    public boolean GuardarOrdenCompleta(Ordenes_compra_cabecera oc, List<Ordenes_compra_detalle> detalles) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            int idCabecera = siguienteId(con, "ordenes_compra_cabecera");
            oc.setId(idCabecera);
            if (oc.getNumero() == null || oc.getNumero().trim().isEmpty()) {
                oc.setNumero("OC-" + idCabecera);
            }

            String sqlCab = "INSERT INTO ordenes_compra_cabecera "
                    + "(id, numero, id_user_crea, fecha, hora, estado, observacion, id_bodega) "
                    + "VALUES (?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sqlCab)) {
                setCabeceraParams(ps, oc);
                ps.executeUpdate();
            }

            int idDetalle = siguienteId(con, "ordenes_compra_detalle");
            for (Ordenes_compra_detalle d : detalles) {
                d.setId(idDetalle++);
                d.setId_orden_cabecera(idCabecera);
                insertarDetalle(con, d);
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            rollback(con);
            JOptionPane.showMessageDialog(null, "Error al guardar la orden de compra:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            cerrar(con);
        }
    }

    /**
     * Actualiza una orden existente (cabecera + detalles). Borra los detalles
     * previos y los reinserta, todo en una transacción. Solo debería usarse
     * mientras la orden esté en BORRADOR o PENDIENTE.
     */
    public boolean ActualizarOrdenCompleta(Ordenes_compra_cabecera oc, List<Ordenes_compra_detalle> detalles) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            String sqlCab = "UPDATE ordenes_compra_cabecera SET "
                    + "fecha=?, hora=?, estado=?, observacion=?, id_bodega=? WHERE id=?";
            try (PreparedStatement ps = con.prepareStatement(sqlCab)) {
                ps.setDate(1, oc.getFecha() != null ? java.sql.Date.valueOf(oc.getFecha()) : null);
                ps.setTime(2, oc.getHora() != null ? java.sql.Time.valueOf(oc.getHora()) : null);
                ps.setInt(3, oc.getEstado());
                ps.setString(4, oc.getObservacion());
                if (oc.getId_bodega() > 0) {
                    ps.setInt(5, oc.getId_bodega());
                } else {
                    ps.setNull(5, java.sql.Types.INTEGER);
                }
                ps.setInt(6, oc.getId());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM ordenes_compra_detalle WHERE id_orden_cabecera=?")) {
                ps.setInt(1, oc.getId());
                ps.executeUpdate();
            }

            int idDetalle = siguienteId(con, "ordenes_compra_detalle");
            for (Ordenes_compra_detalle d : detalles) {
                d.setId(idDetalle++);
                d.setId_orden_cabecera(oc.getId());
                insertarDetalle(con, d);
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            rollback(con);
            JOptionPane.showMessageDialog(null, "Error al actualizar la orden de compra:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            cerrar(con);
        }
    }

    /** Cambia el estado de una orden (p. ej. BORRADOR -> PENDIENTE). */
    public int ActualizarEstado(int idOrden, int nuevoEstado) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE ordenes_compra_cabecera SET estado=? WHERE id=?")) {
                ps.setInt(1, nuevoEstado);
                ps.setInt(2, idOrden);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar el estado:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return 0;
        } finally {
            cerrar(con);
        }
    }

    /** Marca la orden como RECHAZADA registrando quién y cuándo. */
    public int Rechazar(int idOrden, int idUserAprueba) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE ordenes_compra_cabecera SET estado=" + Ordenes_compra_cabecera.ESTADO_RECHAZADA
                    + ", id_user_aprueba=?, fecha_aprobacion=now() WHERE id=?")) {
                ps.setInt(1, idUserAprueba);
                ps.setInt(2, idOrden);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al rechazar la orden:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return 0;
        } finally {
            cerrar(con);
        }
    }

    /**
     * Aprueba la orden: actualiza proveedor y precio de cada línea y marca la
     * cabecera como APROBADA, todo en una transacción.
     */
    public boolean AprobarConLineas(int idOrden, int idUserAprueba, List<Ordenes_compra_detalle> lineas) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            String sqlLinea = "UPDATE ordenes_compra_detalle SET id_proveedor=?, precio_unitario=? WHERE id=?";
            try (PreparedStatement ps = con.prepareStatement(sqlLinea)) {
                for (Ordenes_compra_detalle d : lineas) {
                    if (d.getId_proveedor() != null && d.getId_proveedor() > 0) {
                        ps.setInt(1, d.getId_proveedor());
                    } else {
                        ps.setNull(1, java.sql.Types.INTEGER);
                    }
                    if (d.getPrecio_unitario() != null) {
                        ps.setDouble(2, d.getPrecio_unitario());
                    } else {
                        ps.setNull(2, java.sql.Types.NUMERIC);
                    }
                    ps.setInt(3, d.getId());
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE ordenes_compra_cabecera SET estado=" + Ordenes_compra_cabecera.ESTADO_APROBADA
                    + ", id_user_aprueba=?, fecha_aprobacion=now() WHERE id=?")) {
                ps.setInt(1, idUserAprueba);
                ps.setInt(2, idOrden);
                ps.executeUpdate();
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            rollback(con);
            JOptionPane.showMessageDialog(null, "Error al aprobar la orden:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            cerrar(con);
        }
    }

    /**
     * Lista las órdenes de compra. {@code filtroEstado} = -1 trae todas; de lo
     * contrario filtra por ese estado.
     */
    public static ResultSet listar(int filtroEstado) {
        String sql = "SELECT oc.id, oc.numero, oc.fecha, oc.estado, "
                + "COALESCE(u.nombre, '') AS creador, COALESCE(b.nombre, '') AS bodega, "
                + "(SELECT COUNT(*) FROM ordenes_compra_detalle d WHERE d.id_orden_cabecera = oc.id) AS items "
                + "FROM ordenes_compra_cabecera oc "
                + "LEFT JOIN users u ON u.id = oc.id_user_crea "
                + "LEFT JOIN bodegas b ON b.id = oc.id_bodega ";
        if (filtroEstado >= 0) {
            sql += "WHERE oc.estado = " + filtroEstado + " ";
        }
        sql += "ORDER BY oc.id DESC";
        return DB_consultas_R_D.getTabla(sql);
    }

    /** Detalles de una orden, con datos del producto y nombre del proveedor. */
    public static ResultSet cargarDetalles(int idOrden) {
        String sql = "SELECT d.id, d.id_producto, p.codigo_barras, p.descripcion, d.cantidad, "
                + "d.id_proveedor, COALESCE(c.nombre, '') AS proveedor, d.precio_unitario, d.observacion "
                + "FROM ordenes_compra_detalle d "
                + "JOIN productos p ON p.id = d.id_producto "
                + "LEFT JOIN contactos c ON c.id = d.id_proveedor "
                + "WHERE d.id_orden_cabecera = " + idOrden + " ORDER BY d.id";
        return DB_consultas_R_D.getTabla(sql);
    }

    /** Cabecera de una orden (para mostrar en el análisis, la vista y el PDF). */
    public static ResultSet cargarCabecera(int idOrden) {
        String sql = "SELECT oc.id, oc.numero, oc.fecha, oc.hora, oc.estado, oc.observacion, "
                + "COALESCE(u.nombre, '') AS creador, COALESCE(b.nombre, '') AS bodega, "
                + "COALESCE(ua.nombre, '') AS aprobador, oc.fecha_aprobacion "
                + "FROM ordenes_compra_cabecera oc "
                + "LEFT JOIN users u ON u.id = oc.id_user_crea "
                + "LEFT JOIN bodegas b ON b.id = oc.id_bodega "
                + "LEFT JOIN users ua ON ua.id = oc.id_user_aprueba "
                + "WHERE oc.id = " + idOrden;
        return DB_consultas_R_D.getTabla(sql);
    }

    /**
     * Histórico de compras de un producto: a qué proveedores se le compró y a
     * qué precio, tomado de los ingresos de mercancía. Ordenado de lo más
     * reciente a lo más antiguo. Es la base para decidir a quién comprar.
     */
    public static ResultSet historicoComprasProducto(int idProducto) {
        String sql = "SELECT imc.fecha, imc.no_factura, c.id AS id_proveedor, c.nombre AS proveedor, "
                + "imd.cantidad, imd.precio_costo "
                + "FROM ingresos_mercancias_detalle imd "
                + "JOIN ingresos_mercancias_cabecera imc ON imd.id_ingreso_cabecera = imc.id "
                + "JOIN contactos c ON imc.id_proveedor = c.id "
                + "WHERE imd.id_producto = " + idProducto + " "
                + "ORDER BY imc.fecha DESC, imc.id DESC";
        return DB_consultas_R_D.getTabla(sql);
    }

    // ----------------------------------------------------------------
    private void rollback(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.err.println("Error en rollback: " + ex.getMessage());
            }
        }
    }

    private void cerrar(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
                System.err.println("Error al cerrar conexión: " + ex.getMessage());
            }
        }
    }
}
