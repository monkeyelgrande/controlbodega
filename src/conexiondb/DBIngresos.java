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
     * Id de la cuenta de ingresos marcada como destino de los abonos a credito
     * (cuentas_ingresos.abono_a_credito = 1). Devuelve 0 si no hay ninguna: en
     * ese caso el abono no puede entrar a Caja y hay que avisarlo, no fallar
     * con un error de base de datos.
     */
    public static int cuentaAbonoACredito() {
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement psql = con.prepareStatement(
                        "select id from cuentas_ingresos where abono_a_credito = 1 order by id limit 1");
                ResultSet rs = psql.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return 0;
    }

    /**
     * Registra en Caja el ingreso que genera un abono a credito. Queda ligado a
     * la cabecera del pago por ingresos.id_abono_credito: por ahi lo encuentra
     * DBabonos.EliminarPago para borrarlo si el pago se elimina.
     *
     * La cuenta destino no se elige a mano: es la marcada como
     * abono_a_credito en el catalogo de cuentas de ingresos.
     *
     * @return id del ingreso creado, o 0 si no se pudo (el abono en si ya
     * quedo guardado; el llamador debe avisar que no entro a caja).
     */
    public int Guardar_desde_abono_credito(Ingresos obj, int id_abono_credito) {
        int idCuenta = cuentaAbonoACredito();
        if (idCuenta <= 0) {
            JOptionPane.showMessageDialog(null,
                    "El abono se guardó, pero NO entró a Caja: no hay ninguna cuenta de ingresos\n"
                    + "marcada como \"abono a crédito\".\n\n"
                    + "Marque una en Caja > Cuentas de ingresos y vuelva a intentarlo.",
                    "Sin cuenta de abonos", JOptionPane.WARNING_MESSAGE);
            return 0;
        }

        String SSQL = "INSERT INTO ingresos (id_user, id_cuenta, descripcion, total, fecha, hora, "
                + "id_cliente, factura_remision, transferencia, id_fondo, id_vendedor, "
                + "id_abono_credito, recibo_caja, id_caja) "
                + "VALUES (?,?,?,?,cast(? as date),?,?,?,0,?,?,?,0,?) RETURNING id";
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, obj.getId_user());
            psql.setInt(2, idCuenta);
            psql.setString(3, obj.getDescripcion() == null ? "" : obj.getDescripcion());
            psql.setDouble(4, obj.getTotal());
            psql.setString(5, obj.getFecha().replace("'", ""));
            psql.setString(6, obj.getHora());
            psql.setInt(7, obj.getId_cliente());
            psql.setInt(8, obj.getFactura_remision());
            if (obj.getId_fondo() > 0) {
                psql.setInt(9, obj.getId_fondo());
            } else {
                psql.setNull(9, Types.INTEGER);
            }
            if (obj.getId_vendedor() > 0) {
                psql.setInt(10, obj.getId_vendedor());
            } else {
                psql.setNull(10, Types.INTEGER);
            }
            psql.setInt(11, id_abono_credito);
            psql.setInt(12, obj.getId_caja());

            int idGenerado = 0;
            ResultSet rs = psql.executeQuery();
            if (rs.next()) {
                idGenerado = rs.getInt(1);
                obj.setId(idGenerado);
            }
            rs.close();
            psql.close();
            return idGenerado;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "El abono se guardó, pero no se pudo registrar en Caja:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return 0;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex);
            }
        }
    }

    /**
     * Deja el ingreso de Caja en linea con el abono despues de editarlo.
     *
     * Cubre los tres casos que se dan al guardar una edicion:
     * <ul>
     * <li>el tipo de abono dejo de entrar a Caja: se borra el ingreso;</li>
     * <li>ya habia ingreso: se le corrigen valor, fecha, fondo y descripcion;</li>
     * <li>antes no entraba a Caja y ahora si: se crea.</li>
     * </ul>
     * Sin esto, cambiar el valor de un abono descuadraba el arqueo.
     *
     * @param debeEntrarACaja lo que dice tipos_abonos.agregar_a_ingreso del
     * tipo de abono con el que quedo guardado el pago
     * @return true si la sincronizacion se hizo sin errores
     */
    public boolean Sincronizar_ingreso_de_abono(Ingresos obj, int id_abono_credito, boolean debeEntrarACaja) {
        if (!debeEntrarACaja) {
            try (Connection con = DB_consultas_R_D.getConexion();
                    PreparedStatement ps = con.prepareStatement(
                            "DELETE FROM ingresos WHERE id_abono_credito = ?")) {
                ps.setInt(1, id_abono_credito);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "No se pudo quitar de Caja el ingreso del abono:\n"
                        + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        String sql = "UPDATE ingresos SET total=?, fecha=cast(? as date), hora=?, descripcion=?, "
                + "id_cliente=?, id_fondo=?, id_vendedor=? WHERE id_abono_credito=?";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, obj.getTotal());
            ps.setString(2, obj.getFecha().replace("'", ""));
            ps.setString(3, obj.getHora());
            ps.setString(4, obj.getDescripcion() == null ? "" : obj.getDescripcion());
            ps.setInt(5, obj.getId_cliente());
            if (obj.getId_fondo() > 0) {
                ps.setInt(6, obj.getId_fondo());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            if (obj.getId_vendedor() > 0) {
                ps.setInt(7, obj.getId_vendedor());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setInt(8, id_abono_credito);

            if (ps.executeUpdate() > 0) {
                return true;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "No se pudo actualizar en Caja el ingreso del abono:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // No habia ingreso todavia (el abono no entraba a Caja cuando se creo).
        return Guardar_desde_abono_credito(obj, id_abono_credito) > 0;
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
