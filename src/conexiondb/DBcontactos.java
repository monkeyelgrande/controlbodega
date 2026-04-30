/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Contactos;

/**
 *
 * @author Monkeyelgrande
 */
public class DBcontactos {

    public int Guardar(Contactos contacto) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO contactos (id,nombre, cedula, direccion,ciudad, contacto, contacto2, descuento, email,forma_pago,cuenta,tipo_cuenta,numero_cuenta,observaciones,proveedor) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, contacto.getId());
            psql.setString(2, contacto.getNombre());
            psql.setString(3, contacto.getCedula());
            psql.setString(4, contacto.getDireccion());
            psql.setString(5, contacto.getCiudad());
            psql.setString(6, contacto.getContacto());
            psql.setString(7, contacto.getContacto2());
            psql.setInt(8, contacto.getDescuento());
            psql.setString(9, contacto.getEmail());
            psql.setString(10, contacto.getFormapago());
            psql.setString(11, contacto.getCuenta());
            psql.setString(12, contacto.getTipo_cuenta());
            psql.setString(13, contacto.getNumero_cuenta());
            psql.setString(14, contacto.getObservaciones());
            psql.setInt(15, contacto.getProveedor());

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

    public static boolean existeCedula(String cedula) {
        Connection con = null;
        boolean existe = false;

        String sql = "SELECT 1 FROM contactos WHERE cedula = ? LIMIT 1";

        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, cedula);

            ResultSet rs = ps.executeQuery();
            existe = rs.next();

            rs.close();
            ps.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error validando cédula:\n" + e,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ignored) {
            }
        }
        return existe;
    }

    public int GuardarFactura(Contactos contacto) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO contactos (id,nombre, cedula, direccion, contacto) "
                + "VALUES ((select COALESCE(max(id),0)+1 from contactos),'" + contacto.getNombre() + "','" + contacto.getCedula() + "','" + contacto.getDireccion() + "','" + contacto.getContacto() + "')";
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

    public int Actualizar(Contactos contacto) {
        int resultado = 0;
        Connection con = null;

        String SQL = "UPDATE contactos set "
                + "nombre='" + contacto.getNombre() + "', "
                + "cedula='" + contacto.getCedula() + "', "
                + "direccion='" + contacto.getDireccion() + "', "
                + "ciudad='" + contacto.getCiudad() + "', "
                + "contacto='" + contacto.getContacto() + "', "
                + "contacto2='" + contacto.getContacto2() + "', "
                + "descuento=" + contacto.getDescuento() + ", "
                + "email='" + contacto.getEmail() + "', "
                + "forma_pago='" + contacto.getFormapago() + "', "
                + "cuenta='" + contacto.getCuenta() + "', "
                + "tipo_cuenta='" + contacto.getTipo_cuenta() + "', "
                + "numero_cuenta='" + contacto.getNumero_cuenta() + "', "
                + "observaciones='" + contacto.getObservaciones() + "', "
                + "proveedor=" + contacto.getProveedor() + " "
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
