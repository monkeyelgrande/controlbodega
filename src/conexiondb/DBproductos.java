/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Productos;

/**
 *
 * @author Monkeyelgrande
 */
public class DBproductos {

    public int Guardar(Productos producto) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO productos (id,codigo_barras,descripcion,stock_minimo,stock_ideal, id_unidad, id_padre, cant_paquete, precio_costo, precio_venta, precio_venta2, precio_venta3, tipo, estado) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, producto.getId());
            psql.setString(2, producto.getCodigo_barras());
            psql.setString(3, producto.getDescripcion());
            psql.setDouble(4, producto.getStock_minimo());
            psql.setDouble(5, producto.getStock_ideal());
            psql.setInt(6, producto.getId_unidad());
            psql.setInt(7, producto.getId_padre());
            psql.setInt(8, producto.getCant_paquete());
            psql.setDouble(9, producto.getPrecio_costo());
            psql.setDouble(10, producto.getPrecio_venta());
            psql.setDouble(11, producto.getPrecio_venta2());
            psql.setDouble(12, producto.getPrecio_venta3());
            psql.setInt(13, producto.getTipo());
            psql.setBoolean(14, true); // Producto nuevo siempre habilitado

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

    public int Actualizar(Productos producto) {
        int resultado = 0;
        Connection con = null;

        String SQL = "UPDATE productos set "
                + "codigo_barras='" + producto.getCodigo_barras() + "', "
                + "descripcion='" + producto.getDescripcion() + "', "
                + "stock_minimo=" + producto.getStock_minimo() + ", "
                + "stock_ideal=" + producto.getStock_ideal() + ", "
                + "id_padre=" + producto.getId_padre() + ", "
                + "cant_paquete=" + producto.getCant_paquete() + ", "
                + "precio_costo=" + producto.getPrecio_costo() + ", "
                + "precio_venta=" + producto.getPrecio_venta() + ", "
                + "precio_venta2=" + producto.getPrecio_venta2() + ", "
                + "precio_venta3=" + producto.getPrecio_venta3() + ", "
                + "tipo=" + producto.getTipo()+ ", "
                + "id_unidad=" + producto.getId_unidad() + ", "
                + "estado=true "
                + "where id=" + producto.getId();
        System.out.println(SQL);
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

    public static int traer_stock(String codigo_barras) {
        String cadena = "select stock from productos where codigo_barras = '" + codigo_barras + "'";
        ResultSet rs = DB_consultas_R_D.getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return Integer.parseInt(rs.getString("stock"));
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }
}
