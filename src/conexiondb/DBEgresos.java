/*
 * DBEgresos del modulo Caja (portado desde cajadiaria).
 * Rediseño: id serial (INSERT sin id, RETURNING id), sin abonos_egresos:
 * el fondo del pago va directo en egresos.id_fondo (NULL = pendiente).
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;
import modelos.Egresos;

/**
 *
 * @author Monkeyelgrande
 */
public class DBEgresos {

    /**
     * Inserta el egreso. Si seleccionado (dinero entregado) guarda el fondo
     * directo en id_fondo; si no, queda NULL (Pendiente).
     *
     * @return el id generado por la secuencia (0 si falló). Además queda
     * asignado en obj via setId().
     */
    public int Guardar(Egresos obj, boolean seleccionado) {
        int idGenerado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO egresos (id_user, id_cuenta, descripcion, total, fecha, hora, id_cliente, factura_remision, transferencia, id_fondo, id_caja) "
                + "VALUES (?,?,?,?,cast(? as date),?,?,?,0,?,?) RETURNING id";

        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, obj.getId_user());
            psql.setInt(2, obj.getId_cuenta());
            psql.setString(3, obj.getDescripcion());
            psql.setDouble(4, obj.getTotal());
            psql.setString(5, limpiarFecha(obj.getFecha()));
            psql.setString(6, obj.getHora());
            psql.setInt(7, obj.getId_cliente());
            psql.setInt(8, obj.getFactura_remision());
            if (seleccionado) {
                psql.setInt(9, obj.getId_fondo());
            } else {
                psql.setNull(9, Types.INTEGER);
            }
            psql.setInt(10, obj.getId_caja());

            ResultSet rs = psql.executeQuery();
            if (rs.next()) {
                idGenerado = rs.getInt(1);
                obj.setId(idGenerado);
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
        return idGenerado;
    }

    /**
     * Actualiza la cabecera. Si dinero_recibido asigna el fondo en id_fondo;
     * si no, lo deja NULL (Pendiente). Ya no hay abonos que borrar/insertar.
     */
    public int Actualizar(Egresos egreso, boolean dinero_recibido) {
        int resultado = 0;
        Connection con = null;
        String SQL = "UPDATE egresos set "
                + "id_user=?, "
                + "id_cuenta=?, "
                + "id_cliente=?, "
                + "factura_remision=?, "
                + "descripcion=?, "
                + "total=?, "
                + "fecha=cast(? as date), "
                + "hora=?, "
                + "id_fondo=? "
                + "where id=?";

        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SQL);
            psql.setInt(1, egreso.getId_user());
            psql.setInt(2, egreso.getId_cuenta());
            psql.setInt(3, egreso.getId_cliente());
            psql.setInt(4, egreso.getFactura_remision());
            psql.setString(5, egreso.getDescripcion());
            psql.setDouble(6, egreso.getTotal());
            psql.setString(7, limpiarFecha(egreso.getFecha()));
            psql.setString(8, egreso.getHora());
            if (dinero_recibido) {
                psql.setInt(9, egreso.getId_fondo());
            } else {
                psql.setNull(9, Types.INTEGER);
            }
            psql.setInt(10, egreso.getId());

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

    // La fecha llega como texto ('yyyy-M-d'); se quitan comillas heredadas del
    // formato viejo por si algún llamador aún las trae.
    private static String limpiarFecha(String fecha) {
        return fecha == null ? null : fecha.replace("'", "").trim();
    }
}
