/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos.db;

import conexiondb.DB_consultas_R_D;
import java.sql.*;
import java.util.List;
import javax.swing.JOptionPane;
import Creditos.modelos.Abonos;
import Creditos.modelos.AbonosCabecera;

/**
 * Capa de datos del modelo cabecera + detalle de abonos.
 *
 * abonos_cabeceras: el pago como evento (total, fecha, tipo, soportes).
 * abonos (detalle): cada aplicación del pago a una factura.
 * El saldo a favor es implícito: cabecera.total - SUM(detalle).
 *
 * @author Monkeyelgrande
 */
public class DBabonos {

    /**
     * Guarda un pago completo (cabecera + su reparto en facturas) en UNA sola
     * transacción. Los detalles pueden venir vacíos (anticipo puro: todo el
     * pago queda como saldo a favor). Devuelve el id de la cabecera generado
     * por la BD, o 0 si falló (en cuyo caso no queda nada guardado).
     */
    public int GuardarPago(AbonosCabecera cab, List<Abonos> detalles) {
        String sqlCabecera = "INSERT INTO abonos_cabeceras "
                + "(id_contacto, id_user, id_tipo_abono, total, fecha, hora, observacion, foto, pdf) "
                + "VALUES (?,?,?,?,?::date,?,?,?,?) RETURNING id";
        String sqlDetalle = "INSERT INTO abonos "
                + "(id_cabecera, id_credito, abono, fecha, hora) "
                + "VALUES (?,?,?,?::date,?)";

        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            int idCabecera;
            try (PreparedStatement ps = con.prepareStatement(sqlCabecera)) {
                ps.setInt(1, cab.getId_contacto());
                ps.setInt(2, cab.getId_user());
                ps.setInt(3, cab.getId_tipo_abono());
                ps.setDouble(4, cab.getTotal());
                ps.setString(5, cab.getFecha());
                ps.setString(6, cab.getHora());
                ps.setString(7, cab.getObservacion() == null ? "" : cab.getObservacion());
                ps.setString(8, cab.getFoto() == null ? "" : cab.getFoto());
                ps.setString(9, cab.getPDF() == null ? "" : cab.getPDF());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    idCabecera = rs.getInt(1);
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
                for (Abonos d : detalles) {
                    ps.setInt(1, idCabecera);
                    ps.setInt(2, d.getId_credito());
                    ps.setDouble(3, d.getAbono());
                    ps.setString(4, d.getFecha());
                    ps.setString(5, d.getHora());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();
            cab.setId(idCabecera);
            return idCabecera;

        } catch (SQLException e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ignore) {
            }
            JOptionPane.showMessageDialog(null, "Error al intentar almacenar el abono (no se guardó nada):\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return 0;
        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException ignore) {
            }
        }
    }

    /**
     * Actualiza los datos de la cabecera de un pago (fecha, tipo, total,
     * observación y soportes). No toca el detalle.
     */
    public int ActualizarCabecera(AbonosCabecera cab) {
        String sql = "UPDATE abonos_cabeceras SET "
                + "id_tipo_abono=?, total=?, fecha=?::date, hora=?, observacion=?, foto=?, pdf=? "
                + "WHERE id=?";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cab.getId_tipo_abono());
            ps.setDouble(2, cab.getTotal());
            ps.setString(3, cab.getFecha());
            ps.setString(4, cab.getHora());
            ps.setString(5, cab.getObservacion() == null ? "" : cab.getObservacion());
            ps.setString(6, cab.getFoto() == null ? "" : cab.getFoto());
            ps.setString(7, cab.getPDF() == null ? "" : cab.getPDF());
            ps.setInt(8, cab.getId());
            return ps.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al intentar actualizar la información:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return 0;
        }
    }

    /**
     * Actualiza un abono (detalle): monto, fecha y hora, ajustando el total de
     * su cabecera por la diferencia (así el saldo a favor de la cabecera no se
     * altera), y actualiza tipo/observación/soportes de la cabecera. Todo en
     * una sola transacción.
     */
    public int ActualizarDetalleYCabecera(int idDetalle, double nuevoAbono, String fecha, String hora,
            int idTipoAbono, String observacion, String foto, String pdf) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE abonos_cabeceras c SET "
                    + "total = c.total + (? - a.abono), id_tipo_abono=?, observacion=?, foto=?, pdf=? "
                    + "FROM abonos a WHERE a.id=? AND c.id=a.id_cabecera")) {
                ps.setDouble(1, nuevoAbono);
                ps.setInt(2, idTipoAbono);
                ps.setString(3, observacion == null ? "" : observacion);
                ps.setString(4, foto == null ? "" : foto);
                ps.setString(5, pdf == null ? "" : pdf);
                ps.setInt(6, idDetalle);
                ps.executeUpdate();
            }
            int filas;
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE abonos SET abono=?, fecha=?::date, hora=? WHERE id=?")) {
                ps.setDouble(1, nuevoAbono);
                ps.setString(2, fecha);
                ps.setString(3, hora);
                ps.setInt(4, idDetalle);
                filas = ps.executeUpdate();
            }
            con.commit();
            return filas;

        } catch (SQLException e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ignore) {
            }
            JOptionPane.showMessageDialog(null, "Error al intentar actualizar el abono:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return 0;
        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException ignore) {
            }
        }
    }

    /**
     * Elimina un pago completo: la cabecera y, por ON DELETE CASCADE, todo su
     * detalle. Devuelve true si se eliminó.
     */
    public boolean EliminarPago(int idCabecera) {
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM abonos_cabeceras WHERE id=?")) {
            ps.setInt(1, idCabecera);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al intentar eliminar el abono:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Saldo a favor de un cliente: suma sobre sus cabeceras de
     * (total - detalle aplicado).
     */
    public static double SaldoAFavor(int idContacto) {
        String sql = "SELECT COALESCE(SUM(c.total),0) - COALESCE((SELECT SUM(a.abono) "
                + "FROM abonos a JOIN abonos_cabeceras c2 ON a.id_cabecera=c2.id "
                + "WHERE c2.id_contacto=?),0) AS saldo_a_favor "
                + "FROM abonos_cabeceras c WHERE c.id_contacto=?";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idContacto);
            ps.setInt(2, idContacto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("saldo_a_favor");
                }
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return 0;
    }
}
