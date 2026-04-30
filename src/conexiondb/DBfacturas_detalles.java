/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import static conexiondb.DB_consultas_R_D.getTabla;
import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Facturas_detalles;

/**
 *
 * @author Monkeyelgrande
 */
public class DBfacturas_detalles {

    public int Guardar(Facturas_detalles fd) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO facturas_detalles (id_cabecera,id_producto,cantidad,valor_unitario,subtotal) "
                + "VALUES (" + fd.getId_cabecera() + ",'" + fd.getId_producto() + "'," + fd.getCantidad() + ")";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            resultado = psql.executeUpdate();
            psql.close();

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(null, "Error al intentar almacenar la información factura detallas:\n"
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

    public static Facturas_detalles TraerFacturaDetalleUnSoloItem(String id) {
        Facturas_detalles fd = new Facturas_detalles();
        
        String cadena = "select *, u.nombre from facturas_detalles f, users u where f.id_user=u.id and f.id=" + id + ";";
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                fd.setNombre_usuario(rs.getString("nombre"));
                fd.setHora_entrega(rs.getString("hora_entrega"));
                fd.setFecha_entrega(rs.getString("fecha_entrega"));
            }
            rs.close();

        } catch (SQLException e) {
            System.out.println(e);
            return fd;
        }
        return fd;
    }

}
