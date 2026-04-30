package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import modelos.Traslados_productos;

/**
 *
 * Clase de conexión para la tabla traslados_productos Similar a DBproductos
 * pero adaptada a la clase traslados_productos
 */
public class DB_Traslados_productos {

    // Método para insertar un nuevo traslado
    public int Guardar(Traslados_productos traslado) {
        int resultado = 0;
        Connection con = null;

        String SQL = "INSERT INTO traslados_productos "
                + "(id_producto, cantidad, id_bodega_origen, id_bodega_destino, hora, observacion, id_user, fecha) "
                + "VALUES (?,?,?,?,?,?,?," + traslado.getFecha() + ")";

        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SQL);
            psql.setInt(1, traslado.getId_producto());
            psql.setDouble(2, traslado.getCantidad());
            psql.setInt(3, traslado.getId_bodega_origen());
            psql.setInt(4, traslado.getId_bodega_destino());

            psql.setString(5, traslado.getHora());
            psql.setString(6, traslado.getObservacion());
            psql.setInt(7, traslado.getId_user());

            resultado = psql.executeUpdate();
            psql.close();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al intentar almacenar la información del traslado:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null,
                        "Error al intentar cerrar la conexión:\n" + ex,
                        "Error en la operación", JOptionPane.ERROR_MESSAGE);
            }
        }
        return resultado;
    }

    // Método para actualizar un traslado existente
    public int Actualizar(Traslados_productos traslado) {
        int resultado = 0;
        Connection con = null;
        String SQL = "UPDATE traslados_productos SET "
                + "id_producto = ?, "
                + "cantidad = ?, "
                + "id_bodega_origen = ?, "
                + "id_bodega_destino = ?, "
                + "fecha = ?::date, " // Cast explícito a date
                + "hora = ?, "
                + "observacion = ?, "
                + "id_user = ? "
                + "WHERE id = ?";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SQL);
            psql.setInt(1, traslado.getId_producto());
            psql.setDouble(2, traslado.getCantidad());
            psql.setInt(3, traslado.getId_bodega_origen());
            psql.setInt(4, traslado.getId_bodega_destino());

            // Limpiar comillas si las tiene
            String fecha = traslado.getFecha().replace("'", "");
            psql.setString(5, fecha);

            String hora = traslado.getHora().replace("'", "");
            psql.setString(6, hora);

            psql.setString(7, traslado.getObservacion());
            psql.setInt(8, traslado.getId_user());
            psql.setInt(9, traslado.getId());
            resultado = psql.executeUpdate();
            psql.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al intentar actualizar la información del traslado:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null,
                        "Error al intentar cerrar la conexión:\n" + ex,
                        "Error en la operación", JOptionPane.ERROR_MESSAGE);
            }
        }
        return resultado;
    }
}
