package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Acceso a datos de los modulos licenciables (tabla modulos): conjuntos
 * completos de funcionalidad que se encienden o apagan por instalacion
 * (cliente), a diferencia de los permisos, que gobiernan que ve cada usuario
 * dentro de un modulo activo.
 *
 * Reglas (ver sql/migracion_modulos.sql):
 *   * modulos.clave se corresponde con opciones.modulo.
 *   * Un modulo sin fila en la tabla es nucleo: siempre activo.
 *   * activo = false apaga el modulo para todos, incluso el Admin.
 *
 * Si la consulta falla (BD sin la migracion de modulos o sin conexion) se
 * devuelven conjuntos vacios: todo activo, comportamiento anterior.
 *
 * @author Monkeyelgrande
 */
public class DBmodulos {

    /** Claves de los modulos apagados en esta instalacion (activo = false). */
    public static Set<String> modulosInactivos() {
        Set<String> claves = new HashSet<>();
        String sql = "select clave from modulos where not activo";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                claves.add(rs.getString("clave"));
            }
        } catch (SQLException e) {
            // BD sin la migracion de modulos: todo activo.
            System.out.println("DBmodulos.modulosInactivos: " + e);
        }
        return claves;
    }

    /** Claves de las opciones (opciones.clave) cuyo modulo esta apagado. */
    public static Set<String> opcionesDeModulosInactivos() {
        Set<String> claves = new HashSet<>();
        String sql = "select o.clave from opciones o "
                + "join modulos m on m.clave = o.modulo "
                + "where not m.activo";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                claves.add(rs.getString("clave"));
            }
        } catch (SQLException e) {
            // BD sin la migracion de modulos: ninguna opcion apagada.
            System.out.println("DBmodulos.opcionesDeModulosInactivos: " + e);
        }
        return claves;
    }
}
