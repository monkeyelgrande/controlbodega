/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos.db;

import conexiondb.DB_consultas_R_D;
import java.sql.*;
import javax.swing.JOptionPane;
import Creditos.modelos.Cuentas;
/**
 *
 * @author Monkeyelgrande
 */
public class DBcuentas {


    public int Guardar(Cuentas cuenta) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO cuentas (id,nombre) "
                + "VALUES (?, ?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, cuenta.getId());
            psql.setString(2, cuenta.getNombre());


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
    
    public int Actualizar(Cuentas cuenta) {
        int resultado = 0;
        Connection con = null;
        
        String SQL = "UPDATE cuentas set "
                + "nombre='"+cuenta.getNombre()+"' "
                + "where id="+cuenta.getId();
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
