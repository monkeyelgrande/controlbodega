/*
 * Modulo Caja: persistencia de fotos de ingresos/egresos (fotos_registros).
 * Portado desde cajadiaria con rediseno: id serial (sin id en el INSERT)
 * y PreparedStatement con parametros.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Fotos_registros;

/**
 *
 * @author Monkeyelgrande
 */
public class DB_Fotos_servicios {

    public int Guardar(Fotos_registros obj) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO fotos_registros(nombre, id_registro, tipo_registro) "
                + "VALUES (?, ?, ?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setString(1, obj.getNombre());
            psql.setInt(2, obj.getId_registro());
            psql.setInt(3, obj.getTipo_registro());

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
