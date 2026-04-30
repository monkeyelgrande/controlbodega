/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Recortes_cabeceras;

/**
 *
 * @author Monkeyelgrande
 */
public class DB_Recortes_cabecera {

    public int Guardar(Recortes_cabeceras rc) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO recortes_cabecera (id, id_producto, id_user, fecha, hora, observacion, cantidad) "
                + "VALUES (" + rc.getId() + ", " + rc.getId_producto() + "," + rc.getId_user() + ",'" + rc.getFecha() + "',"
                + "'" + rc.getHora() + "','" + rc.getObservacion() + "'," + rc.getCantidad() + ")";
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

    public int Actualizar(Recortes_cabeceras rc) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "update recortes_cabecera set "
                + "id_producto = " + rc.getId_producto() + ","
                + "id_user = " + rc.getId_user() + ","
                + "fecha = '" + rc.getFecha() + "',"
                + "hora = '" + rc.getHora() + "',"
                + "observacion = '" + rc.getObservacion() + "',"
                + "cantidad= " + rc.getCantidad() + " "
                + " where id = " + rc.getId();
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
