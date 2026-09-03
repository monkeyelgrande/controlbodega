package Creditos.db;

import Creditos.modelos.Porcentajes_comision;
import conexiondb.DB_consultas_R_D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * Capa de datos de la escala de comisiones (porcentajes_comision).
 *
 * @author Monkeyelgrande
 */
public class DB_Porcentajes_comision {

    /**
     * La escala completa, ordenada por dias ascendente. Ese orden es el que
     * espera {@link #porcentajePorDias}: se toma el primer escalón que cubra
     * los dias de cobro.
     */
    public static List<Porcentajes_comision> listar() {
        List<Porcentajes_comision> lista = new ArrayList<>();
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT id, dias, porcentaje FROM porcentajes_comision ORDER BY dias");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Porcentajes_comision(rs.getInt("id"), rs.getInt("dias"),
                        rs.getDouble("porcentaje")));
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return lista;
    }

    /**
     * Porcentaje que corresponde a un cobro que tardó {@code dias} días.
     *
     * @param escala la lista devuelta por {@link #listar()} (ordenada por dias)
     * @param dias dias transcurridos entre el credito y el abono
     * @param esAnticipo un anticipo siempre toma el primer escalón: es plata que
     * entró antes de que existiera la deuda, se premia con el mejor porcentaje
     * @return el porcentaje, o 0 si la escala está vacía
     */
    public static double porcentajePorDias(List<Porcentajes_comision> escala, int dias, boolean esAnticipo) {
        if (escala == null || escala.isEmpty()) {
            return 0;
        }
        if (esAnticipo) {
            return escala.get(0).getPorcentaje();
        }
        for (Porcentajes_comision pc : escala) {
            if (dias <= pc.getDias()) {
                return pc.getPorcentaje();
            }
        }
        // Cobro mas lento que el ultimo escalón: se queda con ese ultimo.
        return escala.get(escala.size() - 1).getPorcentaje();
    }

    public int Guardar(Porcentajes_comision obj) {
        String sql = "INSERT INTO porcentajes_comision (dias, porcentaje) VALUES (?,?)";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, obj.getDias());
            ps.setDouble(2, obj.getPorcentaje());
            return ps.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al intentar almacenar la información:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return 0;
        }
    }

    public int Actualizar(Porcentajes_comision obj) {
        String sql = "UPDATE porcentajes_comision SET dias=?, porcentaje=? WHERE id=?";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, obj.getDias());
            ps.setDouble(2, obj.getPorcentaje());
            ps.setInt(3, obj.getId());
            return ps.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al intentar actualizar la información:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return 0;
        }
    }

    public boolean Eliminar(int id) {
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM porcentajes_comision WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al intentar eliminar la información:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
