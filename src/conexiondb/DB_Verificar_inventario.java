/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.verificar_inventario;

/**
 *
 * @author Monkeyelgrande
 */
public class DB_Verificar_inventario {

    public int Guardar(verificar_inventario inv ){
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO verificar_inventario (id,id_producto,codigo_barras,descripcion,cantidad_actual,cantidad_real,observaciones1,observaciones2,observaciones3) "
                + "VALUES ((select COALESCE(max(id),0)+1 from verificar_inventario),?,?,?,?,?,?,?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, inv.getId_producto());
            psql.setString(2, inv.getCodigo_barras());
            psql.setString(3, inv.getDescripcion());
            psql.setDouble(4, inv.getCantidad_actual());
            psql.setDouble(5, inv.getCantidad_real());
            psql.setString(6, inv.getObservaciones());
            psql.setString(7, inv.getObservaciones2());
            psql.setString(8, inv.getObservaciones3());

            resultado = psql.executeUpdate();
            psql.close();

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(null, "Error al intentar almacenar la veridicaion del inventario:\n"
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
