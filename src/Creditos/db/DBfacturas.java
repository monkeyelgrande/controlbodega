/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos.db;

import conexiondb.DB_consultas_R_D;
import java.io.FileInputStream;
import java.sql.*;
import javax.swing.JOptionPane;
import Creditos.modelos.Facturas;

/**
 *
 * @author Monkeyelgrande
 */
public class DBfacturas {

    public static int Actualizar(Facturas factura) {
        int resultado = 0;
        Connection con = null;

        String SQL = "UPDATE creditos set "
                + "codigo='" + factura.getCodigo() + "',"
                + "descripcion='" + factura.getDescripcion() + "',"
                + "fecha_creacion='" + factura.getFecha_creacion() + "',"
                + "fecha_vencimiento='" + factura.getFecha_vencimiento() + "',"
                + "foto='" + factura.getFoto() + "',"
                + "pdf='" + factura.getPDF() + "',"
                + "id_contacto=" + factura.getId_contacto() + ","
                + "id_cuenta=" + factura.getId_cuenta() + ","
                + "total=" + factura.getTotal() + ","
                + "interes=" + factura.getInteres() + ","
                + "id_empleado=" + (factura.getId_empleado() > 0 ? String.valueOf(factura.getId_empleado()) : "null") + ","
                + "comisionable=" + factura.isComisionable() + ","
                + "id_user=" + factura.getId_user() + " "
                + "where id=" + factura.getId();
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

    public int Guardar(Facturas factura) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO creditos (id,id_contacto,id_user,total,fecha_creacion,fecha_vencimiento, estado, codigo,descripcion,interes,id_cuenta,foto,pdf, hora, id_empleado, comisionable) "
                + "VALUES (?,?,?,?,'" + factura.getFecha_creacion() + "','" + factura.getFecha_vencimiento() + "',?,?,?,?,?,?,?, '" + factura.getHora() + "', ?, ?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);

            psql.setInt(1, factura.getId());
            psql.setInt(2, factura.getId_contacto());
            psql.setInt(3, factura.getId_user());
            psql.setDouble(4, factura.getTotal());
            psql.setInt(5, factura.getEstado());
            psql.setString(6, factura.getCodigo());
            psql.setString(7, factura.getDescripcion());
            psql.setDouble(8, factura.getInteres());
            psql.setInt(9, factura.getId_cuenta());
            psql.setString(10, factura.getFoto());
            psql.setString(11, factura.getPDF());
            // Sin vendedor se guarda NULL: id_empleado es llave foranea a contactos.
            if (factura.getId_empleado() > 0) {
                psql.setInt(12, factura.getId_empleado());
            } else {
                psql.setNull(12, java.sql.Types.INTEGER);
            }
            psql.setBoolean(13, factura.isComisionable());

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
