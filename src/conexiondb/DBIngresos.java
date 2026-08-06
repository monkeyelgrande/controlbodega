/*
 * Modulo Caja: persistencia de ingresos de dinero.
 * Rediseno: id serial (INSERT ... RETURNING id), sin abonos_ingresos;
 * el fondo va directo en ingresos.id_fondo (NULL = pendiente).
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Ingresos;

/**
 *
 * @author Monkeyelgrande
 */
public class DBIngresos {

    /**
     * Inserta el ingreso. Si seleccionado (dinero recibido) guarda el fondo del
     * objeto; si no, id_fondo queda NULL (estado Pendiente). El id generado por
     * la secuencia queda en obj (obj.getId()).
     *
     * @return 1 si inserto, 0 si fallo.
     */
    public int Guardar(Ingresos obj, boolean seleccionado) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO ingresos (id_user, id_cuenta, descripcion, total, fecha, hora, id_cliente, factura_remision, transferencia, id_fondo, recibo_caja, id_caja) "
                + "VALUES (?,?,?,?,cast(? as date),?,?,?,0,?,?,?) RETURNING id";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, obj.getId_user());
            psql.setInt(2, obj.getId_cuenta());
            psql.setString(3, obj.getDescripcion());
            psql.setDouble(4, obj.getTotal());
            psql.setString(5, obj.getFecha().replace("'", ""));
            psql.setString(6, obj.getHora());
            psql.setInt(7, obj.getId_cliente());
            psql.setInt(8, obj.getFactura_remision());
            if (seleccionado) {
                psql.setInt(9, obj.getId_fondo());
            } else {
                psql.setNull(9, Types.INTEGER);
            }
            psql.setInt(10, obj.getRecibo_caja());
            psql.setInt(11, obj.getId_caja());
            ResultSet rs = psql.executeQuery();
            if (rs.next()) {
                obj.setId(rs.getInt(1));
                resultado = 1;
            }
            rs.close();
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

    /**
     * Actualiza la fila del ingreso. Si dinero_recibido asigna el fondo del
     * objeto; si no, deja id_fondo en NULL (Pendiente).
     */
    public int Actualizar(Ingresos ingreso, boolean dinero_recibido) {
        int resultado = 0;
        Connection con = null;
        String SQL = "UPDATE ingresos set id_user=?, id_cuenta=?, id_cliente=?, descripcion=?, total=?, "
                + "fecha=cast(? as date), factura_remision=?, hora=?, id_fondo=?, recibo_caja=? where id=?";

        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SQL);
            psql.setInt(1, ingreso.getId_user());
            psql.setInt(2, ingreso.getId_cuenta());
            psql.setInt(3, ingreso.getId_cliente());
            psql.setString(4, ingreso.getDescripcion());
            psql.setDouble(5, ingreso.getTotal());
            psql.setString(6, ingreso.getFecha().replace("'", ""));
            psql.setInt(7, ingreso.getFactura_remision());
            psql.setString(8, ingreso.getHora());
            if (dinero_recibido) {
                psql.setInt(9, ingreso.getId_fondo());
            } else {
                psql.setNull(9, Types.INTEGER);
            }
            psql.setInt(10, ingreso.getRecibo_caja());
            psql.setInt(11, ingreso.getId());
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
