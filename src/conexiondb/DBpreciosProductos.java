package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 * Actualizaciones de las columnas de precios estilo agro sobre la tabla
 * unificada productos (modulo Precios). SOLO escribe las columnas del modulo
 * (venta, valor_desc_1/2, valor_s_y_t, valor_credito, iva,
 * porcentaje_utilidad) y el costo real de compra (precio_costo, compartido con
 * el resto de controlbodega por decision del negocio). Nunca toca
 * precio_venta/2/3.
 *
 * @author Monkeyelgrande
 */
public class DBpreciosProductos {

    /** Actualiza los precios de venta del modulo Precios (flujo rol 4). */
    public static boolean actualizarPrecios(String codigoBarras, double venta, double valorDesc1,
            double valorDesc2, double valorSyT, double valorCredito, double porcentajeUtilidad, double iva) {
        String sql = "update productos set venta=?, valor_desc_1=?, valor_desc_2=?, valor_s_y_t=?, "
                + "valor_credito=?, porcentaje_utilidad=?, iva=? where codigo_barras=?";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, venta);
            ps.setDouble(2, valorDesc1);
            ps.setDouble(3, valorDesc2);
            ps.setDouble(4, valorSyT);
            ps.setDouble(5, valorCredito);
            ps.setDouble(6, porcentajeUtilidad);
            ps.setDouble(7, iva);
            ps.setString(8, codigoBarras);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar precios del producto:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Actualiza el costo real de compra + iva (flujo rol 3 / Contadora). */
    public static boolean actualizarCosto(String codigoBarras, double precioCosto, double iva) {
        String sql = "update productos set precio_costo=?, iva=? where codigo_barras=?";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, precioCosto);
            ps.setDouble(2, iva);
            ps.setString(3, codigoBarras);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar costo del producto:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** @return el nombre de la unidad de medida del producto (codigo de barras). */
    public static String traerUnidad(String codigoBarras) {
        String unidad = "";
        String sql = "select u.nombre as unidad from productos p "
                + "inner join unidades_medidas u on p.id_unidad=u.id where p.codigo_barras=?";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoBarras);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    unidad = rs.getString("unidad");
                }
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return unidad;
    }

    /** Habilita un producto deshabilitado (estado=true). */
    public static boolean habilitarProducto(String codigoBarras) {
        String sql = "update productos set estado=true where codigo_barras=?";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigoBarras);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al habilitar el producto:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
