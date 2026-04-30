/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Recortes_detallle;

/**
 *
 * @author Monkeyelgrande
 */
public class DB_Recortes_detalle {

    public int Guardar(Recortes_detallle obj) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO recortes_detalle (id,id_cabecera,id_user,cantidad,fecha, hora, observacion, codigo, id_contacto, estado) "
                + "VALUES (" + obj.getId() + "," + obj.getId_cabecera() + "," + obj.getId_user() + "," + obj.getCantidad() + ","
                + "'" + obj.getFecha() + "','" + obj.getHora() + "','" + obj.getObservacion() + "','" + obj.getCodigo() + "'," + obj.getId_contacto() + ", " + obj.getEstado() + ")";
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
