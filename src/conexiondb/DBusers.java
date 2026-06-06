/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Contactos;
import modelos.Users;

/**
 *
 * @author Monkeyelgrande
 */
public class DBusers {


    public int Guardar(Users user) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO users (id,nombre, password, user_name, direccion, telefono, telefono2, estado, email, id_perfil, id_bodega, imprime_ordenes, nombre_impresora, imp_ticket_bodega_asignada, barra_notificaciones, panel_notificaciones, aprueba_compras) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, user.getId());
            psql.setString(2, user.getNombre());
            psql.setString(3, user.getPassword());
            psql.setString(4, user.getUser_name());
            psql.setString(5, user.getDireccion());
            psql.setString(6, user.getTelefono());
            psql.setString(7, user.getTelefono2());
            psql.setString(8, user.getEstado());
            psql.setString(9, user.getEmail());
            psql.setInt(10, user.getId_perfil());
            psql.setInt(11, user.getId_bodega());
            psql.setBoolean(12, user.isImprime_ordenes());
            psql.setString(13, user.getNombre_impresora());
            psql.setBoolean(14, user.isImp_ticket_bodega_asignada());
            psql.setBoolean(15, user.isBarra_notificaciones());
            psql.setBoolean(16, user.isPanel_notificaciones());
            psql.setBoolean(17, user.isAprueba_compras());

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
    
    public int Actualizar(Users user) {
        int resultado = 0;
        Connection con = null;
        String pas="password='"+user.getPassword()+"', ";
        if (user.getPassword().equals("")) {
            pas = "";
        }
        String impresoraVal = user.getNombre_impresora();
        String impresoraSql = (impresoraVal == null)
                ? "NULL"
                : "'" + impresoraVal.replace("'", "''") + "'";
        String SQL = "UPDATE users set "
                + "nombre='"+user.getNombre()+"', "
                + pas
                + "user_name='"+user.getUser_name()+"', "
                + "direccion='"+user.getDireccion()+"', "
                + "telefono='"+user.getTelefono()+"', "
                + "telefono2='"+user.getTelefono2()+"', "
                + "estado='"+user.getEstado()+"', "
                + "email='"+user.getEmail()+"', "
                + "id_perfil="+user.getId_perfil()+", "
                + "id_bodega="+user.getId_bodega()+", "
                + "imprime_ordenes="+user.isImprime_ordenes()+", "
                + "nombre_impresora="+impresoraSql+", "
                + "imp_ticket_bodega_asignada="+user.isImp_ticket_bodega_asignada()+", "
                + "barra_notificaciones="+user.isBarra_notificaciones()+", "
                + "panel_notificaciones="+user.isPanel_notificaciones()+", "
                + "aprueba_compras="+user.isAprueba_compras()+" "
                + "where id="+user.getId();
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
