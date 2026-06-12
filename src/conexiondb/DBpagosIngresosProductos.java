package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 * Pagos a proveedores de los ingresos del modulo Precios (tabla
 * pagos_ingresos_productos — distinta de pagos_ingresos, que pertenece a los
 * ingresos de mercancia clasicos).
 *
 * @author Monkeyelgrande
 */
public class DBpagosIngresosProductos {

    public static boolean guardar(int idIngreso, double total, String fecha, String hora, String codPago) {
        String sql = "INSERT INTO pagos_ingresos_productos (id, id_ingreso_productos_cabecera, total, fecha, hora, cod_pago) "
                + "VALUES ((select COALESCE(max(id),0)+1 from pagos_ingresos_productos), ?, ?, ?::date, ?, ?)";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idIngreso);
            ps.setDouble(2, total);
            ps.setString(3, fecha);
            ps.setString(4, hora);
            ps.setString(5, codPago);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al registrar el pago:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public static boolean eliminar(int idPago) {
        String sql = "DELETE FROM pagos_ingresos_productos WHERE id = ?";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPago);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al eliminar el pago:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
