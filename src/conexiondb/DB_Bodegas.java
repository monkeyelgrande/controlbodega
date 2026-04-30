/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Bodegas;

/**
 *
 * @author Monkeyelgrande
 */
public class DB_Bodegas {


    public int Guardar(Bodegas bodega) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO bodegas (id,nombre, imprime) "
                + "VALUES (?, ?, ?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, bodega.getId());
            psql.setString(2, bodega.getNombre());
            psql.setInt(3, bodega.getImprime());


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
    
    public int Actualizar(Bodegas obj) {
        int resultado = 0;
        Connection con = null;
        
        String SQL = "UPDATE bodegas set "
                + "nombre='"+obj.getNombre()+"', "
                + "imprime="+obj.getImprime()+" "
                + "where id="+obj.getId();
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
}
