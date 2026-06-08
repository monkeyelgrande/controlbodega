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
import modelos.ProductoBodegaRango;

/**
 * Acceso a datos de productos_bodega_rangos: prioridad de bodega por rangos de
 * cantidad, por producto.
 *
 * @author Monkeyelgrande
 */
public class DBproductoBodegaRango {

    /**
     * Lista los rangos configurados para un producto, ordenados por cantidad
     * minima ascendente.
     *
     * @param idProducto ID del producto
     * @return lista de rangos (vacia si el producto no tiene configuracion)
     */
    public static List<ProductoBodegaRango> listarPorProducto(int idProducto) {
        List<ProductoBodegaRango> lista = new ArrayList<>();
        String sql = "SELECT id, id_producto, cantidad_min, cantidad_max, id_bodega "
                + "FROM productos_bodega_rangos WHERE id_producto = " + idProducto + " "
                + "ORDER BY cantidad_min ASC";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                ProductoBodegaRango r = new ProductoBodegaRango();
                r.setId(rs.getInt("id"));
                r.setId_producto(rs.getInt("id_producto"));
                r.setCantidad_min(rs.getDouble("cantidad_min"));
                double max = rs.getDouble("cantidad_max");
                r.setCantidad_max(rs.wasNull() ? null : max);
                r.setId_bodega(rs.getInt("id_bodega"));
                lista.add(r);
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error listando rangos de bodega: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Reemplaza por completo los rangos de un producto: borra los existentes e
     * inserta los nuevos, todo en una transaccion.
     *
     * @param idProducto ID del producto
     * @param rangos lista de rangos a guardar (puede venir vacia para limpiar)
     * @return true si se guardo correctamente
     */
    public static boolean guardarRangos(int idProducto, List<ProductoBodegaRango> rangos) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            // 1. Borrar configuracion anterior del producto
            try (PreparedStatement psDel = con.prepareStatement(
                    "DELETE FROM productos_bodega_rangos WHERE id_producto = ?")) {
                psDel.setInt(1, idProducto);
                psDel.executeUpdate();
            }

            // 2. Insertar los nuevos rangos (id por serial)
            String sqlIns = "INSERT INTO productos_bodega_rangos "
                    + "(id_producto, cantidad_min, cantidad_max, id_bodega) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psIns = con.prepareStatement(sqlIns)) {
                for (ProductoBodegaRango r : rangos) {
                    psIns.setInt(1, idProducto);
                    psIns.setDouble(2, r.getCantidad_min());
                    if (r.getCantidad_max() == null) {
                        psIns.setNull(3, java.sql.Types.DOUBLE);
                    } else {
                        psIns.setDouble(3, r.getCantidad_max());
                    }
                    psIns.setInt(4, r.getId_bodega());
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
                    System.err.println("Error en rollback de rangos: " + ex.getMessage());
                }
            }
            JOptionPane.showMessageDialog(null, "Error al guardar la configuración de bodegas por cantidad:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ex) {
                    System.err.println("Error cerrando conexion de rangos: " + ex.getMessage());
                }
            }
        }
    }
}
