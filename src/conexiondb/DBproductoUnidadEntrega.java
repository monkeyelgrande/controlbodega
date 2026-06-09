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
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import modelos.ProductoUnidadEntrega;

/**
 * Acceso a datos de productos_unidades_entrega: unidades de entrega por producto
 * (partición de cantidad entre bodegas por tamaño de paquete).
 *
 * @author Monkeyelgrande
 */
public class DBproductoUnidadEntrega {

    /**
     * Lista las unidades de entrega de un producto, de menor a mayor paquete
     * (el renglón de paquete = 1, bodega por unidad, queda primero).
     *
     * @param idProducto ID del producto
     * @return lista de unidades (vacia si el producto no tiene configuracion)
     */
    public static List<ProductoUnidadEntrega> listarPorProducto(int idProducto) {
        List<ProductoUnidadEntrega> lista = new ArrayList<>();
        String sql = "SELECT id, id_producto, nombre, cantidad_paquete, id_bodega "
                + "FROM productos_unidades_entrega WHERE id_producto = " + idProducto + " "
                + "ORDER BY cantidad_paquete ASC";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                ProductoUnidadEntrega u = new ProductoUnidadEntrega();
                u.setId(rs.getInt("id"));
                u.setId_producto(rs.getInt("id_producto"));
                u.setNombre(rs.getString("nombre"));
                u.setCantidad_paquete(rs.getDouble("cantidad_paquete"));
                u.setId_bodega(rs.getInt("id_bodega"));
                lista.add(u);
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error listando unidades de entrega: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Reemplaza por completo las unidades de entrega de un producto: borra las
     * existentes e inserta las nuevas, todo en una transaccion.
     *
     * @param idProducto ID del producto
     * @param unidades lista de unidades a guardar (puede venir vacia para limpiar)
     * @return true si se guardo correctamente
     */
    public static boolean guardarUnidades(int idProducto, List<ProductoUnidadEntrega> unidades) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            // 1. Borrar configuracion anterior del producto
            try (PreparedStatement psDel = con.prepareStatement(
                    "DELETE FROM productos_unidades_entrega WHERE id_producto = ?")) {
                psDel.setInt(1, idProducto);
                psDel.executeUpdate();
            }

            // 2. Insertar las nuevas unidades (id por serial)
            String sqlIns = "INSERT INTO productos_unidades_entrega "
                    + "(id_producto, nombre, cantidad_paquete, id_bodega) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psIns = con.prepareStatement(sqlIns)) {
                for (ProductoUnidadEntrega u : unidades) {
                    psIns.setInt(1, idProducto);
                    if (u.getNombre() == null) {
                        psIns.setNull(2, java.sql.Types.VARCHAR);
                    } else {
                        psIns.setString(2, u.getNombre());
                    }
                    psIns.setDouble(3, u.getCantidad_paquete());
                    psIns.setInt(4, u.getId_bodega());
                    psIns.executeUpdate();
                }
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback de unidades de entrega: " + ex.getMessage());
                }
            }
            JOptionPane.showMessageDialog(null, "Error al guardar las unidades de entrega:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ex) {
                    System.err.println("Error cerrando conexion de unidades de entrega: " + ex.getMessage());
                }
            }
        }
    }
}
