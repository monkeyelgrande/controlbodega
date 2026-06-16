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
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 * Acceso a datos de la Solicitud de Cotización / RFQ (RF-03).
 *
 * @author Monkeyelgrande
 */
public class DBcotizaciones {

    private int sigId(Connection con, String tabla) throws SQLException {
        try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id),0)+1 AS id FROM " + tabla)) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return 1;
    }

    /** Crea una RFQ para un proveedor a partir de los ítems seleccionados de un sugerido. */
    public int crearDesdeSugerido(int idSugerido, int idProveedor, int idUser) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);
            int id = sigId(con, "cotizaciones_compra_cabecera");
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO cotizaciones_compra_cabecera (id,numero,id_sugerido,id_proveedor,id_user,fecha,hora,estado) "
                    + "VALUES (?,?,?,?,?,current_date,current_time,0)")) {
                ps.setInt(1, id);
                ps.setString(2, "RFQ-" + id);
                ps.setInt(3, idSugerido);
                ps.setInt(4, idProveedor);
                ps.setInt(5, idUser);
                ps.executeUpdate();
            }
            int idDet = sigId(con, "cotizaciones_compra_detalle");
            String sel = "SELECT id_producto, COALESCE(cantidad_final,cantidad_sugerida) AS cant "
                    + "FROM sugeridos_detalle WHERE id_sugerido_cab=" + idSugerido + " AND seleccionado=true ORDER BY id";
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sel)) {
                while (rs.next()) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO cotizaciones_compra_detalle (id,id_cotiz_cab,id_producto,cantidad) VALUES (?,?,?,?)")) {
                        ps.setInt(1, idDet++);
                        ps.setInt(2, id);
                        ps.setInt(3, rs.getInt("id_producto"));
                        ps.setDouble(4, rs.getDouble("cant"));
                        ps.executeUpdate();
                    }
                }
            }
            con.commit();
            return id;
        } catch (SQLException e) {
            rollback(con);
            JOptionPane.showMessageDialog(null, "Error al generar la solicitud de cotización:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        } finally {
            cerrar(con);
        }
    }

    public static ResultSet listar(int filtroEstado) {
        String sql = "SELECT c.id, c.numero, c.fecha, c.estado, COALESCE(ct.nombre,'') AS proveedor, "
                + "(SELECT COUNT(*) FROM cotizaciones_compra_detalle d WHERE d.id_cotiz_cab=c.id) AS items "
                + "FROM cotizaciones_compra_cabecera c LEFT JOIN contactos ct ON ct.id=c.id_proveedor ";
        if (filtroEstado >= 0) {
            sql += "WHERE c.estado=" + filtroEstado + " ";
        }
        sql += "ORDER BY c.id DESC";
        return DB_consultas_R_D.getTabla(sql);
    }

    public static ResultSet cargarCabecera(int id) {
        return DB_consultas_R_D.getTabla(
                "SELECT c.*, COALESCE(ct.nombre,'') AS proveedor, COALESCE(ct.contacto,'') AS celular "
                + "FROM cotizaciones_compra_cabecera c LEFT JOIN contactos ct ON ct.id=c.id_proveedor WHERE c.id=" + id);
    }

    public static ResultSet cargarDetalles(int id) {
        return DB_consultas_R_D.getTabla(
                "SELECT d.id, d.id_producto, p.codigo_barras, p.descripcion, d.cantidad, "
                + "d.precio_unitario, d.iva_pct, COALESCE(d.plazo_entrega,'') AS plazo_entrega "
                + "FROM cotizaciones_compra_detalle d JOIN productos p ON p.id=d.id_producto "
                + "WHERE d.id_cotiz_cab=" + id + " ORDER BY d.id");
    }

    /** Guarda condiciones de cabecera y precios/iva/plazo capturados por línea. */
    public boolean guardarRespuesta(int id, String condicion, String validez, String fechaLimite,
            String observacion, DefaultTableModel detalle, int colId, int colPrecio, int colIva, int colPlazo) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE cotizaciones_compra_cabecera SET condicion_pago=?, validez=?, fecha_limite=?, observacion=? WHERE id=?")) {
                ps.setString(1, condicion);
                ps.setString(2, validez);
                if (fechaLimite != null && !fechaLimite.trim().isEmpty()) {
                    ps.setDate(3, java.sql.Date.valueOf(fechaLimite));
                } else {
                    ps.setNull(3, java.sql.Types.DATE);
                }
                ps.setString(4, observacion);
                ps.setInt(5, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE cotizaciones_compra_detalle SET precio_unitario=?, iva_pct=?, plazo_entrega=? WHERE id=?")) {
                for (int i = 0; i < detalle.getRowCount(); i++) {
                    int idDet = Integer.parseInt(detalle.getValueAt(i, colId).toString());
                    Double precio = parseNum(detalle.getValueAt(i, colPrecio));
                    Double iva = parseNum(detalle.getValueAt(i, colIva));
                    String plazo = String.valueOf(detalle.getValueAt(i, colPlazo));
                    if (precio != null) {
                        ps.setDouble(1, precio);
                    } else {
                        ps.setNull(1, java.sql.Types.NUMERIC);
                    }
                    if (iva != null) {
                        ps.setDouble(2, iva);
                    } else {
                        ps.setNull(2, java.sql.Types.NUMERIC);
                    }
                    ps.setString(3, plazo);
                    ps.setInt(4, idDet);
                    ps.executeUpdate();
                }
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            rollback(con);
            JOptionPane.showMessageDialog(null, "Error al guardar la cotización:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            cerrar(con);
        }
    }

    public int actualizarEstado(int id, int estado) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            String extra = estado == 1 ? ", fecha_envio=current_date" : "";
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE cotizaciones_compra_cabecera SET estado=?" + extra + " WHERE id=?")) {
                ps.setInt(1, estado);
                ps.setInt(2, id);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            return 0;
        } finally {
            cerrar(con);
        }
    }

    private Double parseNum(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(s.replace(".", "").replace(",", "."));
        } catch (Exception e) {
            try {
                return Double.parseDouble(s);
            } catch (Exception e2) {
                return null;
            }
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

    public static String nombreEstado(int e) {
        switch (e) {
            case 0:
                return "Borrador";
            case 1:
                return "Enviada";
            case 2:
                return "Respondida";
            case 3:
                return "Sin respuesta";
            default:
                return "?";
        }
    }
}
