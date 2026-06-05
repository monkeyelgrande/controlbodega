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
        String SSQL = "INSERT INTO bodegas (id, nombre, imprime, genera_orden_automatica) "
                + "VALUES (?, ?, ?, ?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, bodega.getId());
            psql.setString(2, bodega.getNombre());
            psql.setInt(3, bodega.getImprime());
            psql.setBoolean(4, bodega.isGeneraOrdenAutomatica());

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

        String SQL = "UPDATE bodegas SET "
                + "nombre = ?, "
                + "imprime = ?, "
                + "genera_orden_automatica = ? "
                + "WHERE id = ?";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SQL);
            psql.setString(1, obj.getNombre());
            psql.setInt(2, obj.getImprime());
            psql.setBoolean(3, obj.isGeneraOrdenAutomatica());
            psql.setInt(4, obj.getId());
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

    /**
     * Indica si la bodega genera orden de entrega automática al facturar venta.
     * Si la bodega no existe o hay error de consulta, devuelve false (comportamiento
     * conservador: descuento inmediato sin orden).
     */
    public static boolean generaOrdenAutomatica(int idBodega) {
        Connection con = null;
        boolean resultado = false;
        String SQL = "SELECT genera_orden_automatica FROM bodegas WHERE id = ?";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SQL);
            psql.setInt(1, idBodega);
            try (ResultSet rs = psql.executeQuery()) {
                if (rs.next()) {
                    resultado = rs.getBoolean(1);
                }
            }
            psql.close();
        } catch (SQLException e) {
            System.err.println("Error consultando genera_orden_automatica bodega " + idBodega + ": " + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignored) {
                }
            }
        }
        return resultado;
    }
}
