package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import modelos.Configuraciones;

public class DBconfiguraciones {

    public int Guardar(Configuraciones config) {
        boolean existe = DB_consultas_R_D.consultarId("1", "configuraciones") == 1;

        String SQL = existe
                ? "UPDATE configuraciones SET "
                +   "nombre_negocio=?, nit_negocio=?, contacto_negocio=?, contacto2_negocio=?, "
                +   "direccion=?, utilidad_venta=?, utilidad_mayorista=?, utilidad_credito=?, "
                +   "iva=?, nombre_impresora=?, impresora_venta_80mm=?, pie_legal=?, "
                +   "servicios=?, imprimir_factura=?, productos_repetidos=? "
                + "WHERE id=1"
                : "INSERT INTO configuraciones ("
                +   "nombre_negocio, nit_negocio, contacto_negocio, contacto2_negocio, "
                +   "direccion, utilidad_venta, utilidad_mayorista, utilidad_credito, "
                +   "iva, nombre_impresora, impresora_venta_80mm, pie_legal, "
                +   "servicios, imprimir_factura, productos_repetidos"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int resultado = 0;
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement ps = con.prepareStatement(SQL);
            ps.setString(1,  config.getNombre_negocio());
            ps.setString(2,  config.getNit_negocio());
            ps.setString(3,  config.getContacto_Negocio());
            ps.setString(4,  config.getContacto2_Negocio());
            ps.setString(5,  config.getDireccion());
            ps.setInt   (6,  config.getUtilida_venta());
            ps.setInt   (7,  config.getUtilida_mayorista());
            ps.setInt   (8,  config.getUtilida_credito());
            ps.setInt   (9,  config.getIva());
            ps.setString(10, config.getNombre_impresota());
            ps.setString(11, config.getImpresora_venta_80mm());
            ps.setString(12, config.getPie_legal());
            ps.setString(13, config.getServicios());
            ps.setInt   (14, config.getImprimir_factura());
            ps.setInt   (15, config.getProductos_repetidos());

            resultado = ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al intentar almacenar la información:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (con != null) {
                try { con.close(); }
                catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null,
                            "Error al intentar cerrar la conexión:\n" + ex,
                            "Error en la operación", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return resultado;
    }

    public int Actualizar_Ruta(String ruta) {
        int resultado = 0;
        Connection con = null;
        String SQL = "UPDATE configuraciones SET ruta_backup=? WHERE id=1";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement ps = con.prepareStatement(SQL);
            ps.setString(1, ruta);
            resultado = ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al intentar almacenar la información:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (con != null) {
                try { con.close(); }
                catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null,
                            "Error al intentar cerrar la conexión:\n" + ex,
                            "Error en la operación", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return resultado;
    }
}
