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
import modelos.Sugerido_cabecera;
import modelos.Sugerido_detalle;

/**
 * Acceso a datos del Sugerido de pedidos (RF-01/02).
 *
 * @author Monkeyelgrande
 */
public class DBsugeridos {

    private int siguienteId(Connection con, String tabla) throws SQLException {
        try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id),0)+1 AS id FROM " + tabla)) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return 1;
    }

    public boolean GuardarCompleto(Sugerido_cabecera cab, List<Sugerido_detalle> detalles) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);
            int id = siguienteId(con, "sugeridos_cabecera");
            cab.setId(id);
            if (cab.getNumero() == null || cab.getNumero().trim().isEmpty()) {
                cab.setNumero("SUG-" + id);
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO sugeridos_cabecera (id,numero,id_user_crea,fecha,hora,estado,observacion,id_bodega,meses_cobertura) "
                    + "VALUES (?,?,?,?,?,?,?,?,?)")) {
                ps.setInt(1, cab.getId());
                ps.setString(2, cab.getNumero());
                ps.setInt(3, cab.getId_user_crea());
                ps.setDate(4, cab.getFecha() != null ? java.sql.Date.valueOf(cab.getFecha()) : null);
                ps.setTime(5, cab.getHora() != null ? java.sql.Time.valueOf(cab.getHora()) : null);
                ps.setInt(6, cab.getEstado());
                ps.setString(7, cab.getObservacion());
                if (cab.getId_bodega() > 0) {
                    ps.setInt(8, cab.getId_bodega());
                } else {
                    ps.setNull(8, java.sql.Types.INTEGER);
                }
                ps.setDouble(9, cab.getMeses_cobertura());
                ps.executeUpdate();
            }
            int idDet = siguienteId(con, "sugeridos_detalle");
            for (Sugerido_detalle d : detalles) {
                d.setId(idDet++);
                d.setId_sugerido_cab(id);
                insertarDetalle(con, d);
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            rollback(con);
            JOptionPane.showMessageDialog(null, "Error al guardar el sugerido:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            cerrar(con);
        }
    }

    private void insertarDetalle(Connection con, Sugerido_detalle d) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO sugeridos_detalle (id,id_sugerido_cab,id_producto,cantidad_sugerida,existencia,"
                + "rotacion_mensual,ultima_compra,seleccionado,cantidad_final,observacion) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, d.getId());
            ps.setInt(2, d.getId_sugerido_cab());
            ps.setInt(3, d.getId_producto());
            ps.setDouble(4, d.getCantidad_sugerida());
            ps.setDouble(5, d.getExistencia());
            ps.setDouble(6, d.getRotacion_mensual());
            ps.setDouble(7, d.getUltima_compra());
            ps.setBoolean(8, d.isSeleccionado());
            if (d.getCantidad_final() != null) {
                ps.setDouble(9, d.getCantidad_final());
            } else {
                ps.setNull(9, java.sql.Types.NUMERIC);
            }
            ps.setString(10, d.getObservacion());
            ps.executeUpdate();
        }
    }

    public boolean ActualizarCompleto(Sugerido_cabecera cab, List<Sugerido_detalle> detalles) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE sugeridos_cabecera SET observacion=?, id_bodega=?, meses_cobertura=? WHERE id=?")) {
                ps.setString(1, cab.getObservacion());
                if (cab.getId_bodega() > 0) {
                    ps.setInt(2, cab.getId_bodega());
                } else {
                    ps.setNull(2, java.sql.Types.INTEGER);
                }
                ps.setDouble(3, cab.getMeses_cobertura());
                ps.setInt(4, cab.getId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM sugeridos_detalle WHERE id_sugerido_cab=?")) {
                ps.setInt(1, cab.getId());
                ps.executeUpdate();
            }
            int idDet = siguienteId(con, "sugeridos_detalle");
            for (Sugerido_detalle d : detalles) {
                d.setId(idDet++);
                d.setId_sugerido_cab(cab.getId());
                insertarDetalle(con, d);
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            rollback(con);
            JOptionPane.showMessageDialog(null, "Error al actualizar el sugerido:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            cerrar(con);
        }
    }

    /** Guarda la selección/cantidad final de cada línea (RF-02). */
    public boolean GuardarSeleccion(List<Sugerido_detalle> detalles) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE sugeridos_detalle SET seleccionado=?, cantidad_final=? WHERE id=?")) {
                for (Sugerido_detalle d : detalles) {
                    ps.setBoolean(1, d.isSeleccionado());
                    if (d.getCantidad_final() != null) {
                        ps.setDouble(2, d.getCantidad_final());
                    } else {
                        ps.setNull(2, java.sql.Types.NUMERIC);
                    }
                    ps.setInt(3, d.getId());
                    ps.executeUpdate();
                }
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            rollback(con);
            JOptionPane.showMessageDialog(null, "Error al guardar la selección:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            cerrar(con);
        }
    }

    public int ActualizarEstado(int idSugerido, int estado) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE sugeridos_cabecera SET estado=? WHERE id=?")) {
                ps.setInt(1, estado);
                ps.setInt(2, idSugerido);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al cambiar estado:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return 0;
        } finally {
            cerrar(con);
        }
    }

    public static ResultSet listar(int filtroEstado) {
        String sql = "SELECT s.id, s.numero, s.fecha, s.estado, COALESCE(u.nombre,'') AS creador, "
                + "COALESCE(b.nombre,'') AS bodega, "
                + "(SELECT COUNT(*) FROM sugeridos_detalle d WHERE d.id_sugerido_cab=s.id) AS items "
                + "FROM sugeridos_cabecera s "
                + "LEFT JOIN users u ON u.id=s.id_user_crea "
                + "LEFT JOIN bodegas b ON b.id=s.id_bodega ";
        if (filtroEstado >= 0) {
            sql += "WHERE s.estado=" + filtroEstado + " ";
        }
        sql += "ORDER BY s.id DESC";
        return DB_consultas_R_D.getTabla(sql);
    }

    public static ResultSet cargarCabecera(int idSugerido) {
        return DB_consultas_R_D.getTabla(
                "SELECT s.id, s.numero, s.fecha, s.estado, s.observacion, s.id_bodega, s.meses_cobertura, "
                + "COALESCE(u.nombre,'') AS creador, COALESCE(b.nombre,'') AS bodega "
                + "FROM sugeridos_cabecera s "
                + "LEFT JOIN users u ON u.id=s.id_user_crea "
                + "LEFT JOIN bodegas b ON b.id=s.id_bodega WHERE s.id=" + idSugerido);
    }

    public static ResultSet cargarDetalles(int idSugerido) {
        return DB_consultas_R_D.getTabla(
                "SELECT d.id, d.id_producto, p.codigo_barras, p.descripcion, d.cantidad_sugerida, "
                + "d.existencia, d.rotacion_mensual, d.ultima_compra, d.seleccionado, d.cantidad_final "
                + "FROM sugeridos_detalle d JOIN productos p ON p.id=d.id_producto "
                + "WHERE d.id_sugerido_cab=" + idSugerido + " ORDER BY d.id");
    }

    private void rollback(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.err.println("rollback: " + ex.getMessage());
            }
        }
    }

    private void cerrar(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
                System.err.println("close: " + ex.getMessage());
            }
        }
    }
}
