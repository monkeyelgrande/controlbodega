/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Devoluciones_detalles;
import modelos.Facturas_detalles;

/**
 *
 * @author Monkeyelgrande
 */
public class DBdevoluciones_detalles {




    public int Guardar(Devoluciones_detalles dd) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO devoluciones_detalles (id_cabecera_devolucion,id_producto,cantidad,valor_unitario,total) "
                + "VALUES ("+dd.getId_cabecera()+",'"+dd.getId_producto()+"',"+dd.getCantidad()+","+dd.getValor_unitario()+","+dd.getTotal()+")";
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
    
}
