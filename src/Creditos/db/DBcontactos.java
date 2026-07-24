/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos.db;

import conexiondb.DB_consultas_R_D;
import java.sql.*;
import javax.swing.JOptionPane;
import Creditos.modelos.Contactos;

/**
 *
 * @author Monkeyelgrande
 */
public class DBcontactos {

    public int Guardar(Contactos contacto) {
        int resultado = 0;
        Connection con = null;
        // En bodega los telefonos del contacto son las columnas contacto (celular)
        // y contacto2 (fijo); cupo, empleado, antiguo e interes las agrega la
        // migracion del modulo Creditos.
        String SSQL = "INSERT INTO contactos (id,nombre, cedula, direccion,ciudad, contacto, contacto2, email, observaciones, cupo, empleado, antiguo, interes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, contacto.getId());
            psql.setString(2, contacto.getNombre());
            psql.setString(3, contacto.getCedula());
            psql.setString(4, contacto.getDireccion());
            psql.setString(5, contacto.getCiudad());
            psql.setString(6, contacto.getCelular());
            psql.setString(7, contacto.getTelefono());
            psql.setString(8, contacto.getEmail());
            psql.setString(9, contacto.getObservaciones());
            psql.setDouble(10, contacto.getCupo());
            psql.setInt(11, contacto.getEmpleado());
            psql.setInt(12, contacto.getAntiguo());
            psql.setDouble(13, contacto.getInteres());

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

    public int Actualizar(Contactos contacto) {
        int resultado = 0;
        Connection con = null;

        String SQL = "UPDATE contactos set "
                + "nombre='" + contacto.getNombre() + "', "
                + "cedula='" + contacto.getCedula() + "', "
                + "direccion='" + contacto.getDireccion() + "', "
                + "ciudad='" + contacto.getCiudad() + "', "
                + "contacto='" + contacto.getCelular() + "', "
                + "contacto2='" + contacto.getTelefono() + "', "
                + "email='" + contacto.getEmail() + "', "
                + "cupo=" + contacto.getCupo() + ", "
                + "observaciones='" + contacto.getObservaciones() + "', "
                + "empleado=" + contacto.getEmpleado() + ", "
                + "antiguo=" + contacto.getAntiguo()+ ", "
                + "interes=" + contacto.getInteres()+ " "
                + "where id=" + contacto.getId();
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
