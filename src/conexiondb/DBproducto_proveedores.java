/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 * Amarre N:M producto &lt;-&gt; proveedor. El celular y demás datos del proveedor
 * salen de la tabla contactos.
 *
 * @author Monkeyelgrande
 */
public class DBproducto_proveedores {

    /** Proveedores asociados a un producto (con celular tomado de contactos). */
    public static ResultSet proveedoresDeProducto(int idProducto) {
        String sql = "SELECT pp.id, c.id AS id_proveedor, c.nombre, "
                + "COALESCE(c.contacto,'') AS celular, COALESCE(c.contacto2,'') AS telefono "
                + "FROM producto_proveedores pp "
                + "JOIN contactos c ON c.id = pp.id_proveedor "
                + "WHERE pp.id_producto = " + idProducto + " ORDER BY c.nombre";
        return DB_consultas_R_D.getTabla(sql);
    }

    /** Texto corto con los proveedores de un producto (para mostrar en tablas). */
    public static String resumenProveedores(int idProducto) {
        StringBuilder sb = new StringBuilder();
        ResultSet rs = proveedoresDeProducto(idProducto);
        try {
            int n = 0;
            while (rs.next()) {
                if (n > 0) {
                    sb.append(", ");
                }
                sb.append(rs.getString("nombre"));
                n++;
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return sb.toString();
    }

    public static boolean agregar(int idProducto, int idProveedor) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO producto_proveedores (id_producto, id_proveedor) "
                    + "SELECT ?, ? WHERE NOT EXISTS ("
                    + "SELECT 1 FROM producto_proveedores WHERE id_producto=? AND id_proveedor=?)")) {
                ps.setInt(1, idProducto);
                ps.setInt(2, idProveedor);
                ps.setInt(3, idProducto);
                ps.setInt(4, idProveedor);
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al asociar proveedor:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            cerrar(con);
        }
    }

    public static boolean quitar(int idAmarre) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM producto_proveedores WHERE id = ?")) {
                ps.setInt(1, idAmarre);
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al quitar proveedor:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            cerrar(con);
        }
    }

    private static void cerrar(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ex) {
                System.err.println("Error al cerrar conexión: " + ex.getMessage());
            }
        }
    }
}
