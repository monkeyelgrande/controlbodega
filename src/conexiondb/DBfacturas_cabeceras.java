/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Facturas_cabeceras;

/**
 *
 * @author Monkeyelgrande
 */
public class DBfacturas_cabeceras {

    public static int ActualizaAnulado(String id, int estado) {
        int resultado = 0;
        Connection con = null;

        String SQL = "UPDATE facturas_cabeceras set "
                + "anulado=" + estado
                + "where id=" + id;
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SQL);
            resultado = psql.executeUpdate();
            psql.close();

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(null, "Error al intentar actualizar la información:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error al intentar cerrar la conexión:\n"
                        + ex, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            }
        }
        return resultado;
    }

    public int Guardar(Facturas_cabeceras fc) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO facturas_cabeceras (id,id_contacto,id_user,fecha,hora,tipo_factura,codigo,observacion, observacion_entrega, anulado, tipo_pago, id_bodega) "
                + "VALUES (" + fc.getId() + ", " + fc.getId_cliente() + "," + fc.getId_user() + ",'" + fc.getFecha() + "',"
                + "'" + fc.getHora() + "','" + fc.getTipo() + "','" + fc.getCodigo() + "','" + fc.getObservacion() + "','" + fc.getObservacion_entrega() + "',"
                + fc.getAnulado() + "," + fc.getTipo_pago() + " ," + fc.getId_bodega()+ " )";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            resultado = psql.executeUpdate();
            psql.close();

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(null, "Error al intentar almacenar la información:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error al intentar cerrar la conexión:\n"
                        + ex, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            }
        }
        return resultado;
    }

    public int Actualizar(Facturas_cabeceras fc) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "update facturas_cabeceras set "
                + "id_contacto = " + fc.getId_cliente() + ","
                + "id_user_edita = " + fc.getId_user() + ","
                + "fecha = '" + fc.getFecha() + "',"
                + "hora = '" + fc.getHora() + "',"
                + "tipo_factura = '" + fc.getTipo() + "',"
                + "tipo_pago= " + fc.getTipo_pago() + ","
                + "codigo = '" + fc.getCodigo() + "',"
                + "observacion = '" + fc.getObservacion() + "',"
                + "observacion_entrega= '" + fc.getObservacion_entrega() + "',"
                + "anulado = " + fc.getAnulado() + ", "
                + "id_bodega= " + fc.getId_bodega()+ " "
                + " where id = " + fc.getId();
        System.out.println(SSQL);
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            resultado = psql.executeUpdate();
            psql.close();

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(null, "Error al intentar almacenar la información:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error al intentar cerrar la conexión:\n"
                        + ex, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            }
        }
        return resultado;
    }

    public int Guardar_Cotizacion(Facturas_cabeceras fc) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO cotizaciones_cabeceras (id,id_contacto,id_user,fecha,hora,tipo_factura,codigo,observacion, observacion_entrega, anulado, tipo_pago) "
                + "VALUES (" + fc.getId() + ", " + fc.getId_cliente() + "," + fc.getId_user() + ",'" + fc.getFecha() + "',"
                + "'" + fc.getHora() + "','" + fc.getTipo() + "','" + fc.getCodigo() + "','" + fc.getObservacion() + "','" + fc.getObservacion_entrega() + "'," + fc.getAnulado() + "," + fc.getTipo_pago() + " )";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            resultado = psql.executeUpdate();
            psql.close();

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(null, "Error al intentar almacenar la información:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error al intentar cerrar la conexión:\n"
                        + ex, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            }
        }
        return resultado;
    }

    public int Actualizar_Cotizacion(Facturas_cabeceras fc) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "update cotizaciones_cabeceras set "
                + "id_contacto = " + fc.getId_cliente() + ","
                + "id_user_edita = " + fc.getId_user() + ","
                + "fecha = '" + fc.getFecha() + "',"
                + "hora = '" + fc.getHora() + "',"
                + "tipo_factura = '" + fc.getTipo() + "',"
                + "tipo_pago= " + fc.getTipo_pago() + ","
                + "codigo = '" + fc.getCodigo() + "',"
                + "observacion = '" + fc.getObservacion() + "',"
                + "observacion_entrega= '" + fc.getObservacion_entrega() + "',"
                + "anulado = " + fc.getAnulado() + " "
                + " where id = " + fc.getId();
        System.out.println(SSQL);
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            resultado = psql.executeUpdate();
            psql.close();

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(null, "Error al intentar almacenar la información:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error al intentar cerrar la conexión:\n"
                        + ex, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            }
        }
        return resultado;
    }

}
