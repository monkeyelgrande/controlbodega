/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Devoluciones_Cabeceras;

/**
 *
 * @author Monkeyelgrande
 */
public class DBdevoluciones_cabeceras {

    public int Guardar(Devoluciones_Cabeceras dc) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO devoluciones (id,id_factura,id_user,total,fecha,hora, id_bodega) "
                + "VALUES (" + dc.getId() + ", " + dc.getId_factura() + "," + dc.getId_user() + "," + dc.getTotal() + ",'" + dc.getFecha() + "','" + dc.getHora() + "', " + dc.getId_bodega() + ")";
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

}
