/*
 * Modulo Caja: persistencia de cuentas de ingresos.
 * Portado desde cajadiaria con rediseno: id serial (sin id en el INSERT)
 * y PreparedStatement con parametros.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Cuentas_Ingresos;

/**
 *
 * @author Monkeyelgrande
 */
public class DBcuentas_Ingresos {

    public int Guardar(Cuentas_Ingresos cuenta) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO cuentas_ingresos (nombre, predeterminado, id_caja, abono_a_credito) "
                + "VALUES (?, ?, ?, ?)";
        try {
            con = DB_consultas_R_D.getConexion();
            // Solo una cuenta puede recibir los abonos a credito: si esta se
            // marca, la anterior se desmarca (hay indice unico en la base).
            if (cuenta.getAbono_a_credito() == 1) {
                PreparedStatement limpiar = con.prepareStatement(
                        "update cuentas_ingresos set abono_a_credito=0 where abono_a_credito=1");
                limpiar.executeUpdate();
                limpiar.close();
            }
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setString(1, cuenta.getNombre());
            psql.setInt(2, cuenta.getPredeterminado());
            psql.setInt(3, cuenta.getId_caja());
            psql.setInt(4, cuenta.getAbono_a_credito());

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

    public int Actualizar(Cuentas_Ingresos cuenta) {
        int resultado = 0;
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();

            // El original siempre reiniciaba el predeterminado antes de actualizar
            // (acotado a la caja de la cuenta: cada caja tiene el suyo).
            PreparedStatement preset = con.prepareStatement("update cuentas_ingresos set predeterminado=0 where id_caja=?");
            preset.setInt(1, cuenta.getId_caja());
            preset.executeUpdate();
            preset.close();

            if (cuenta.getAbono_a_credito() == 1) {
                PreparedStatement limpiar = con.prepareStatement(
                        "update cuentas_ingresos set abono_a_credito=0 where abono_a_credito=1 and id<>?");
                limpiar.setInt(1, cuenta.getId());
                limpiar.executeUpdate();
                limpiar.close();
            }

            PreparedStatement psql = con.prepareStatement(
                    "UPDATE cuentas_ingresos set nombre=?, predeterminado=?, abono_a_credito=? where id=?");
            psql.setString(1, cuenta.getNombre());
            psql.setInt(2, cuenta.getPredeterminado());
            psql.setInt(3, cuenta.getAbono_a_credito());
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
