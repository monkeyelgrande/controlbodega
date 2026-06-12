package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import modelos.Descuentos;

/**
 * DAO de descuentos escalonados del modulo Precios (portado de
 * productos-agroinsumos).
 *
 * @author Monkeyelgrande
 */
public class DB_Descuentos {

    public int Guardar(Descuentos obj) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO descuentos (id, tipo, utilidad, descuento) "
                + "VALUES (?, ?, ?, ?)";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, obj.getId());
            psql.setInt(2, obj.getTipo());
            psql.setDouble(3, obj.getUtilidad());
            psql.setDouble(4, obj.getDescuento());
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

    public int Actualizar(Descuentos obj) {
        int resultado = 0;
        Connection con = null;
        String SQL = "UPDATE descuentos set tipo=?, utilidad=?, descuento=? where id=?";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SQL);
            psql.setInt(1, obj.getTipo());
            psql.setDouble(2, obj.getUtilidad());
            psql.setDouble(3, obj.getDescuento());
            psql.setInt(4, obj.getId());
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
