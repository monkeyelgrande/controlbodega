/*
 * Lectura del historial de auditoria de un ingreso del modulo Precios.
 *
 * La auditoria la escriben los triggers de PostgreSQL (ver
 * sql/migracion_auditoria_ingresos.sql); esta clase solo la CONSULTA para
 * pintarla en el panel de historial del listado de ingresos.
 *
 * El contexto de usuario (quien esta trabajando) ya se publica en cada conexion
 * desde conexiondb.AuditoriaCaja, aplicado en DB_consultas_R_D.getConexion(),
 * asi que no hace falta nada extra aqui para que el trigger sepa el usuario.
 */
package conexiondb;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Monkeyelgrande
 */
public class AuditoriaIngresos {

    /**
     * Devuelve el historial de un ingreso como modelo de tabla listo para
     * pintar, con las columnas: Fecha / Hora | Usuario | Cambio. Lo mas antiguo
     * arriba (orden cronologico del flujo). Si la migracion de auditoria aun no
     * se ha aplicado, devuelve un modelo vacio sin reventar.
     */
    public static DefaultTableModel historial(int idIngreso) {
        DefaultTableModel modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        modelo.setColumnIdentifiers(new Object[]{"Fecha / Hora", "Usuario", "Cambio"});

        String consulta = "SELECT to_char(fecha_hora, 'YYYY-MM-DD HH24:MI:SS') AS cuando, "
                + "COALESCE(usuario_visible, '-') AS quien, "
                + "COALESCE(descripcion, '-') AS que "
                + "FROM v_auditoria_ingresos "
                + "WHERE id_registro = " + idIngreso + " ORDER BY id";

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        if (rs == null) {
            return modelo;
        }
        try {
            while (rs.next()) {
                modelo.addRow(new Object[]{rs.getString("cuando"), rs.getString("quien"), rs.getString("que")});
            }
            rs.close();
        } catch (SQLException ex) {
            // La tabla/vista puede no existir aun (migracion sin aplicar): se
            // deja el historial vacio en vez de interrumpir el trabajo.
            System.out.println("Auditoria de ingresos no disponible: " + ex.getMessage());
        }
        return modelo;
    }
}
