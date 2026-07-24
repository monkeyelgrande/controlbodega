/*
 * Modulo Caja: persistencia de fondos.
 * Portado desde cajadiaria con rediseno: id serial (sin id en el INSERT),
 * PreparedStatement con parametros y correccion del bug que pisaba
 * fisico_digital cuando predeterminado==1.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Fondos;

/**
 *
 * @author Monkeyelgrande
 */
public class DB_Fondos {

    public int Guardar(Fondos obj) {
        int resultado = 0;
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();

            // Si el nuevo fondo es el predeterminado, se quita la marca de los demas.
            if (obj.getPredeterminado() == 1) {
                PreparedStatement preset = con.prepareStatement("update fondos set predeterminado=0");
                preset.executeUpdate();
                preset.close();
            }

            PreparedStatement psql = con.prepareStatement(
                    "INSERT INTO fondos (nombre, predeterminado, fisico_digital) VALUES (?, ?, ?)");
            psql.setString(1, obj.getNombre());
            psql.setInt(2, obj.getPredeterminado());
            psql.setInt(3, obj.getFisico_digital());

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

    public int Actualizar(Fondos cuenta) {
        int resultado = 0;
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();

            if (cuenta.getPredeterminado() == 1) {
                PreparedStatement preset = con.prepareStatement("update fondos set predeterminado=0");
                preset.executeUpdate();
                preset.close();
            }

            PreparedStatement psql = con.prepareStatement(
                    "UPDATE fondos set nombre=?, predeterminado=?, fisico_digital=? where id=?");
            psql.setString(1, cuenta.getNombre());
            psql.setInt(2, cuenta.getPredeterminado());
            psql.setInt(3, cuenta.getFisico_digital());
            psql.setInt(4, cuenta.getId());

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
}
