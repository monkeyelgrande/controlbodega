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
import Metodos.metodos;

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
                + "(id_cabecera, id_credito, abono, fecha, hora, comision_pagada) "
                + "VALUES (?,?,?,?::date,?,?)";

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
                    ps.setInt(6, d.getComision_pagada());
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
     * detalle. Si el pago habia entrado a Caja como ingreso, ese ingreso se
     * elimina en la misma transacción: de lo contrario quedaria plata en el
     * arqueo sin respaldo en cartera. Devuelve true si se eliminó.
     */
    public boolean EliminarPago(int idCabecera) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM ingresos WHERE id_abono_credito=?")) {
                ps.setInt(1, idCabecera);
                ps.executeUpdate();
            }
            int filas;
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM abonos_cabeceras WHERE id=?")) {
                ps.setInt(1, idCabecera);
                filas = ps.executeUpdate();
            }
            con.commit();
            return filas > 0;

        } catch (SQLException e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ignore) {
            }
            JOptionPane.showMessageDialog(null, "Error al intentar eliminar el abono:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
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

    /**
     * Saldo a favor de UN pago concreto: cabecera.total - SUM(detalle). Es lo
     * que queda disponible de ese abono para cruzar contra créditos.
     */
    public static double SaldoAFavorCabecera(int idCabecera) {
        String sql = "SELECT ca.total - COALESCE((SELECT SUM(a.abono) FROM abonos a "
                + "WHERE a.id_cabecera = ca.id),0) AS saldo_favor "
                + "FROM abonos_cabeceras ca WHERE ca.id = ?";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCabecera);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("saldo_favor");
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return 0;
    }

    /**
     * Cruza el saldo a favor de un pago contra créditos pendientes del mismo
     * cliente: por cada crédito inserta un detalle en abonos.
     *
     * Todo ocurre en UNA sola transacción y se revalida contra la base con las
     * filas bloqueadas (FOR UPDATE), de modo que no se pueda sobregirar ni el
     * saldo del pago ni el saldo de un crédito aunque otro usuario esté
     * abonando al mismo tiempo. Si algo falla no queda nada guardado.
     *
     * @param idCabecera pago del que sale el dinero
     * @param aplicaciones id de crédito -> monto a aplicar
     * @param fecha fecha del cruce en formato yyyy-MM-dd
     * @param hora hora del cruce
     * @return null si el cruce se guardó bien, o el mensaje de error si no se
     * guardó nada.
     */
    public String CruzarSaldoAFavor(int idCabecera, java.util.Map<Integer, Double> aplicaciones,
            String fecha, String hora) {

        if (aplicaciones == null || aplicaciones.isEmpty()) {
            return "No se seleccionó ningún crédito para cruzar.";
        }

        String sqlCabecera = "SELECT ca.id_contacto, ca.total, "
                + "COALESCE((SELECT SUM(a.abono) FROM abonos a WHERE a.id_cabecera = ca.id),0) AS aplicado "
                + "FROM abonos_cabeceras ca WHERE ca.id = ? FOR UPDATE";
        String sqlCredito = "SELECT f.id_contacto, f.codigo, f.total, "
                + "COALESCE((SELECT SUM(a.abono) FROM abonos a WHERE a.id_credito = f.id),0) AS aplicado "
                + "FROM creditos f WHERE f.id = ? FOR UPDATE";
        String sqlDetalle = "INSERT INTO abonos (id_cabecera, id_credito, abono, fecha, hora, comision_pagada) "
                + "VALUES (?,?,?,?::date,?,0)";

        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            int idContacto;
            double saldoFavor;
            try (PreparedStatement ps = con.prepareStatement(sqlCabecera)) {
                ps.setInt(1, idCabecera);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return "El pago #" + idCabecera + " ya no existe.";
                    }
                    idContacto = rs.getInt("id_contacto");
                    saldoFavor = rs.getDouble("total") - rs.getDouble("aplicado");
                }
            }

            double totalACruzar = 0;
            for (Double monto : aplicaciones.values()) {
                totalACruzar += (monto == null ? 0 : monto);
            }

            if (totalACruzar <= 0.009) {
                con.rollback();
                return "El monto a cruzar debe ser mayor que cero.";
            }
            if (totalACruzar > saldoFavor + 0.009) {
                con.rollback();
                return "El pago #" + idCabecera + " solo tiene $ "
                        + metodos.formateador_dinero().format(saldoFavor)
                        + " disponibles y se intentó cruzar $ "
                        + metodos.formateador_dinero().format(totalACruzar) + ".";
            }

            // Cada crédito se revalida: debe ser del mismo cliente y tener saldo
            // suficiente. Asi el cruce nunca deja un crédito sobrepagado.
            for (java.util.Map.Entry<Integer, Double> e : aplicaciones.entrySet()) {
                int idCredito = e.getKey();
                double monto = e.getValue() == null ? 0 : e.getValue();
                if (monto <= 0.009) {
                    continue;
                }
                try (PreparedStatement ps = con.prepareStatement(sqlCredito)) {
                    ps.setInt(1, idCredito);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return "El crédito " + idCredito + " ya no existe.";
                        }
                        if (rs.getInt("id_contacto") != idContacto) {
                            con.rollback();
                            return "El crédito " + idCredito + " no pertenece al mismo cliente del pago.";
                        }
                        double saldoCredito = rs.getDouble("total") - rs.getDouble("aplicado");
                        if (monto > saldoCredito + 0.009) {
                            con.rollback();
                            return "El crédito " + rs.getString("codigo") + " (id " + idCredito + ") solo debe $ "
                                    + metodos.formateador_dinero().format(saldoCredito)
                                    + " y se intentó aplicar $ "
                                    + metodos.formateador_dinero().format(monto) + ".";
                        }
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
                for (java.util.Map.Entry<Integer, Double> e : aplicaciones.entrySet()) {
                    double monto = e.getValue() == null ? 0 : e.getValue();
                    if (monto <= 0.009) {
                        continue;
                    }
                    ps.setInt(1, idCabecera);
                    ps.setInt(2, e.getKey());
                    ps.setDouble(3, monto);
                    ps.setString(4, fecha);
                    ps.setString(5, hora);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();
            return null;

        } catch (Exception e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ignore) {
            }
            return "No se pudo guardar el cruce (no quedó nada guardado):\n" + e;
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
     * Marca como liquidadas las comisiones de los abonos indicados, en una sola
     * transacción: o se marcan todas o no se marca ninguna.
     *
     * @param idsDetalle ids de abonos (aplicaciones a un crédito)
     * @param idsCabecera ids de abonos_cabeceras (anticipos puros, que no
     * tienen detalle donde marcar)
     * @return cuántas filas quedaron marcadas, o -1 si falló
     */
    public int MarcarComisionesPagadas(java.util.List<Integer> idsDetalle,
            java.util.List<Integer> idsCabecera) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);
            int filas = 0;

            if (idsDetalle != null && !idsDetalle.isEmpty()) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE abonos SET comision_pagada = 1 WHERE id = ? AND comision_pagada = 0")) {
                    for (Integer id : idsDetalle) {
                        ps.setInt(1, id);
                        ps.addBatch();
                    }
                    for (int r : ps.executeBatch()) {
                        filas += Math.max(r, 0);
                    }
                }
            }
            if (idsCabecera != null && !idsCabecera.isEmpty()) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE abonos_cabeceras SET comision_pagada = 1 WHERE id = ? AND comision_pagada = 0")) {
                    for (Integer id : idsCabecera) {
                        ps.setInt(1, id);
                        ps.addBatch();
                    }
                    for (int r : ps.executeBatch()) {
                        filas += Math.max(r, 0);
                    }
                }
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
            JOptionPane.showMessageDialog(null, "No se pudieron marcar las comisiones (no se marcó ninguna):\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return -1;
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
     * ¿Un pago de este tipo de abono debe entrar a Caja como ingreso? Lo decide
     * el catálogo (tipos_abonos.agregar_a_ingreso).
     */
    public static boolean agregar_a_ingreso(int id_tipo_abono) {
        boolean resultado = true;

        ResultSet rs = DB_consultas_R_D.getTabla(
                "select agregar_a_ingreso from tipos_abonos where id=" + id_tipo_abono);
        try {
            while (rs.next()) {
                if (rs.getInt("agregar_a_ingreso") == 0) {
                    resultado = false;
                }
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return resultado;
    }
}
