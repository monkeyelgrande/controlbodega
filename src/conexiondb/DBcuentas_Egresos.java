/*
 * Modulo Caja: persistencia de cuentas de egresos.
 * Portado desde cajadiaria con rediseno: id serial (sin id en el INSERT)
 * y PreparedStatement con parametros.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Cuentas_Egresos;

/**
 *
 * @author Monkeyelgrande
 */
public class DBcuentas_Egresos {

    public int Guardar(Cuentas_Egresos cuenta) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO cuentas_egresos (nombre, predeterminado) "
                + "VALUES (?, ?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setString(1, cuenta.getNombre());
            psql.setInt(2, cuenta.getPredeterminado());

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

    public int Actualizar(Cuentas_Egresos cuenta) {
        int resultado = 0;
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();

            // El original siempre reiniciaba el predeterminado antes de actualizar.
            PreparedStatement preset = con.prepareStatement("update cuentas_egresos set predeterminado=0");
            preset.executeUpdate();
            preset.close();

            PreparedStatement psql = con.prepareStatement(
                    "UPDATE cuentas_egresos set nombre=?, predeterminado=? where id=?");
            psql.setString(1, cuenta.getNombre());
            psql.setInt(2, cuenta.getPredeterminado());
            psql.setInt(3, cuenta.getId());

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
