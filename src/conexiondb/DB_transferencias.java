/*
 * Modulo Caja (portado de cajadiaria): persistencia de traslados entre fondos.
 * Rediseño: ids serial (INSERT ... RETURNING id), sin tablas de abonos, y el
 * par ingreso/egreso del traslado se crea aqui en UNA transaccion.
 */
package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javax.swing.JOptionPane;
import modelos.Transferencias;

/**
 *
 * @author Monkeyelgrande
 */
public class DB_transferencias {

    /**
     * Crea el traslado completo en una sola transaccion:
     * 1) egreso en el fondo origen (transferencia=1),
     * 2) ingreso en el fondo destino (transferencia=1),
     * 3) fila en transferencias enlazando ambos.
     * Los pares llevan la cuenta de ingresos/egresos indicada (la
     * predeterminada de cada catalogo) y el contacto predeterminado si existe.
     *
     * @return id de la transferencia creada, o 0 si fallo (con rollback).
     */
    public int GuardarTraslado(Transferencias t, int idCuentaIngreso, int idCuentaEgreso) {
        if (idCuentaIngreso <= 0 || idCuentaEgreso <= 0) {
            JOptionPane.showMessageDialog(null, "No hay cuenta de ingresos/egresos predeterminada.\n"
                    + "Configure una cuenta predeterminada en cada catálogo antes de crear traslados.",
                    "Traslado no guardado", JOptionPane.WARNING_MESSAGE);
            return 0;
        }

        int idTransferencia = 0;
        Connection con = null;
        // La fecha llega como yyyy-MM-dd (se toleran comillas sobrantes del formato viejo)
        java.sql.Date fecha = java.sql.Date.valueOf(t.getFecha().replace("'", ""));
        String descripcionPar = "TRASLADO - " + t.getDescripcion();

        String sqlEgreso = "INSERT INTO egresos (id_user, id_cuenta, id_cliente, id_fondo, descripcion, total, fecha, hora, factura_remision, transferencia) "
                + "VALUES (?,?,?,?,?,?,?,?,?,1) RETURNING id";
        String sqlIngreso = "INSERT INTO ingresos (id_user, id_cuenta, id_cliente, id_fondo, descripcion, total, fecha, hora, factura_remision, transferencia) "
                + "VALUES (?,?,?,?,?,?,?,?,?,1) RETURNING id";
        String sqlTransferencia = "INSERT INTO transferencias (id_user, id_fondo_origen, id_fondo_destino, descripcion, total, fecha, hora, id_ingreso, id_egreso) "
                + "VALUES (?,?,?,?,?,?,?,?,?) RETURNING id";

        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            Integer idCliente = traerContactoPredeterminado(con);

            // 1) egreso en el fondo origen
            int idEgreso = insertarPar(con, sqlEgreso, t, idCuentaEgreso, idCliente,
                    t.getId_fondo_origen(), descripcionPar, fecha);

            // 2) ingreso en el fondo destino
            int idIngreso = insertarPar(con, sqlIngreso, t, idCuentaIngreso, idCliente,
                    t.getId_fondo_destino(), descripcionPar, fecha);

            // 3) la transferencia que enlaza el par
            PreparedStatement ps = con.prepareStatement(sqlTransferencia);
            ps.setInt(1, t.getId_user());
            ps.setInt(2, t.getId_fondo_origen());
            ps.setInt(3, t.getId_fondo_destino());
            ps.setString(4, t.getDescripcion());
            ps.setDouble(5, t.getTotal());
            ps.setDate(6, fecha);
            ps.setString(7, t.getHora());
            ps.setInt(8, idIngreso);
            ps.setInt(9, idEgreso);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idTransferencia = rs.getInt(1);
            }
            rs.close();
            ps.close();

            con.commit();

            t.setId(idTransferencia);
            t.setId_ingreso(idIngreso);
            t.setId_egreso(idEgreso);

        } catch (SQLException e) {
            idTransferencia = 0;
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ex) {
                System.out.println(ex);
            }
            JOptionPane.showMessageDialog(null, "Error al intentar almacenar la información:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error al intentar cerrar la conexión:\n"
                        + ex, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            }
        }
        return idTransferencia;
    }

    /** INSERT de un lado del par (ingreso o egreso) devolviendo su id serial. */
    private int insertarPar(Connection con, String sql, Transferencias t, int idCuenta,
            Integer idCliente, int idFondo, String descripcion, java.sql.Date fecha) throws SQLException {
        int id = 0;
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, t.getId_user());
        ps.setInt(2, idCuenta);
        if (idCliente == null) {
            ps.setNull(3, Types.INTEGER);
        } else {
            ps.setInt(3, idCliente);
        }
        ps.setInt(4, idFondo);
        ps.setString(5, descripcion);
        ps.setDouble(6, t.getTotal());
        ps.setDate(7, fecha);
        ps.setString(8, t.getHora());
        ps.setInt(9, t.getFactura_remision());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            id = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return id;
    }

    /** Contacto predeterminado (contactos.predeterminado=1) o null si no hay. */
    private Integer traerContactoPredeterminado(Connection con) throws SQLException {
        Integer id = null;
        PreparedStatement ps = con.prepareStatement("SELECT id FROM contactos WHERE predeterminado = 1 LIMIT 1");
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            id = rs.getInt(1);
        }
        rs.close();
        ps.close();
        return id;
    }

    /**
     * Inserta solo la fila de transferencias (sin el par ingreso/egreso).
     * Se conserva por compatibilidad con la firma del original; el flujo del
     * formulario usa GuardarTraslado.
     */
    public int Guardar(Transferencias trans) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO transferencias (id_user, id_fondo_origen, id_fondo_destino, descripcion, total, fecha, hora, id_ingreso, id_egreso) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";

        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, trans.getId_user());
            psql.setInt(2, trans.getId_fondo_origen());
            psql.setInt(3, trans.getId_fondo_destino());
            psql.setString(4, trans.getDescripcion());
            psql.setDouble(5, trans.getTotal());
            psql.setDate(6, java.sql.Date.valueOf(trans.getFecha().replace("'", "")));
            psql.setString(7, trans.getHora());
            psql.setInt(8, trans.getId_ingreso());
            psql.setInt(9, trans.getId_egreso());
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

    /**
     * Actualiza los datos de la transferencia. Nota: el original actualizaba
     * por error la tabla ingresos; aqui se corrige a transferencias.
     */
    public int Actualizar(Transferencias trans) {
        int resultado = 0;
        Connection con = null;
        String SQL = "UPDATE transferencias SET "
                + "id_user=?, id_fondo_origen=?, id_fondo_destino=?, "
                + "descripcion=?, total=?, fecha=?, hora=? "
                + "WHERE id=?";

        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SQL);
            psql.setInt(1, trans.getId_user());
            psql.setInt(2, trans.getId_fondo_origen());
            psql.setInt(3, trans.getId_fondo_destino());
            psql.setString(4, trans.getDescripcion());
            psql.setDouble(5, trans.getTotal());
            psql.setDate(6, java.sql.Date.valueOf(trans.getFecha().replace("'", "")));
            psql.setString(7, trans.getHora());
            psql.setInt(8, trans.getId());
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
