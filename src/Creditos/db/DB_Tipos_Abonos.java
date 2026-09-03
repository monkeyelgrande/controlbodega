/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos.db;

import conexiondb.DB_consultas_R_D;
import java.sql.*;
import javax.swing.JOptionPane;
import Creditos.modelos.Tipos_abonos;

/**
 *
 * @author Monkeyelgrande
 */
public class DB_Tipos_Abonos {

    public int Guardar(Tipos_abonos obj) {
        int resultado = 0;
        Connection con = null;

        
        String SSQL = "INSERT INTO tipos_abonos (id,nombre, color, anticipo, agregar_a_ingreso, comisionable) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, obj.getId());
            psql.setString(2, obj.getNombre());
            psql.setString(3, obj.getColor());
            psql.setInt(4, obj.getAnticipo());
            psql.setInt(5, obj.getAgregar_a_ingreso());
            psql.setInt(6, obj.getComisionable());

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

    public int Actualizar(Tipos_abonos obj) {
        int resultado = 0;
        Connection con = null;

        String SQL = "UPDATE tipos_abonos set "
                + "nombre='" + obj.getNombre() + "', "
                + "color='" + obj.getColor()+ "', "
                + "anticipo=" + obj.getAnticipo() + ", "
                + "agregar_a_ingreso=" + obj.getAgregar_a_ingreso() + ", "
                + "comisionable=" + obj.getComisionable() + " "
                + "where id=" + obj.getId();
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
