package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;

/**
 * Operaciones CRUD para ajustes_inventario_cabecera y ajustes_inventario_detalle.
 *
 * Cada fila en el detalle puede ajustar:
 *   - la cantidad fisica (cantidad_anterior/cantidad_nueva/diferencia), y/o
 *   - la cantidad pendiente (pendientes_anterior/pendientes_nuevo/diferencia_pendientes).
 *
 * @author M-Work
 */
public class DBajustes_inventario {

    /**
     * Guarda un ajuste completo (cabecera + detalles) y aplica los movimientos de stock.
     *
     * @param idUser       ID del usuario que realiza el ajuste
     * @param idBodega     ID de la bodega
     * @param observacion  Observacion general del ajuste
     * @param productos    Array de 7 columnas por fila:
     *                       [idProducto, cantAnterior, cantNueva, difCant,
     *                        pendAnterior, pendNueva, difPend]
     * @param obsProductos Array de observaciones por producto (puede ser null)
     * @return ID de la cabecera creada, o -1 si falla
     */
    public int guardar(int idUser, int idBodega, String observacion,
                       double[][] productos, String[] obsProductos) {
        Connection con = null;
        int idCabecera = -1;

        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            // 1. Insertar cabecera
            String sqlCab = "INSERT INTO ajustes_inventario_cabecera "
                    + "(fecha, hora, id_user, id_bodega, observacion, estado) "
                    + "VALUES (CURRENT_DATE, TO_CHAR(NOW(), 'HH24:MI:SS'), ?, ?, ?, 1)";

            PreparedStatement psCab = con.prepareStatement(sqlCab, Statement.RETURN_GENERATED_KEYS);
            psCab.setInt(1, idUser);
            psCab.setInt(2, idBodega);
            psCab.setString(3, observacion);
            psCab.executeUpdate();

            ResultSet keys = psCab.getGeneratedKeys();
            if (keys.next()) {
                idCabecera = keys.getInt(1);
            }
            keys.close();
            psCab.close();

            if (idCabecera <= 0) {
                con.rollback();
                return -1;
            }

            // 2. Insertar detalles (cantidad + pendientes)
            String sqlDet = "INSERT INTO ajustes_inventario_detalle "
                    + "(id_ajuste_cabecera, id_producto, "
                    + "cantidad_anterior, cantidad_nueva, diferencia, "
                    + "pendientes_anterior, pendientes_nuevo, diferencia_pendientes, "
                    + "observacion) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement psDet = con.prepareStatement(sqlDet);

            for (int i = 0; i < productos.length; i++) {
                int idProducto = (int) productos[i][0];
                double cantAnterior = productos[i][1];
                double cantNueva    = productos[i][2];
                double difCant      = productos[i][3];
                double pendAnterior = productos[i][4];
                double pendNueva    = productos[i][5];
                double difPend      = productos[i][6];

                psDet.setInt(1, idCabecera);
                psDet.setInt(2, idProducto);
                psDet.setDouble(3, cantAnterior);
                psDet.setDouble(4, cantNueva);
                psDet.setDouble(5, difCant);
                psDet.setDouble(6, pendAnterior);
                psDet.setDouble(7, pendNueva);
                psDet.setDouble(8, difPend);
                psDet.setString(9, obsProductos != null && i < obsProductos.length ? obsProductos[i] : null);
                psDet.addBatch();
            }
            psDet.executeBatch();
            psDet.close();

            con.commit();

            // 3. Aplicar movimientos de stock (fuera de la transaccion principal)
            DBstock_productos dbStock = new DBstock_productos();
            for (int i = 0; i < productos.length; i++) {
                int idProducto = (int) productos[i][0];
                double difCant = productos[i][3];
                double difPend = productos[i][6];

                String obsBase = "Ajuste #" + idCabecera;
                if (obsProductos != null && i < obsProductos.length && obsProductos[i] != null
                        && !obsProductos[i].trim().isEmpty()) {
                    obsBase += " - " + obsProductos[i];
                }

                if (difCant != 0) {
                    dbStock.ajuste(idProducto, idBodega, idUser,
                            Math.abs(difCant), difCant > 0, obsBase);
                }
                if (difPend != 0) {
                    dbStock.ajustePendientes(idProducto, idBodega, idUser,
                            Math.abs(difPend), difPend > 0, obsBase + " (pendientes)");
                }
            }

            return idCabecera;

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (SQLException ex) {}
            JOptionPane.showMessageDialog(null,
                    "Error al guardar ajuste de inventario:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return -1;
        } finally {
            try { if (con != null) { con.setAutoCommit(true); con.close(); } } catch (SQLException ex) {}
        }
    }

    /**
     * Elimina (anula) un ajuste de inventario, revirtiendo los movimientos de stock
     * (tanto cantidad como pendientes).
     */
    public boolean eliminar(int idCabecera, int idUser) {
        Connection con = null;

        try {
            con = DB_consultas_R_D.getConexion();

            // 1. Obtener bodega y verificar estado
            PreparedStatement psCab = con.prepareStatement(
                    "SELECT id_bodega, estado FROM ajustes_inventario_cabecera WHERE id = ?");
            psCab.setInt(1, idCabecera);
            ResultSet rsCab = psCab.executeQuery();

            if (!rsCab.next()) {
                JOptionPane.showMessageDialog(null, "Ajuste no encontrado.");
                return false;
            }

            int estado = rsCab.getInt("estado");
            int idBodega = rsCab.getInt("id_bodega");
            rsCab.close();
            psCab.close();

            if (estado == 0) {
                JOptionPane.showMessageDialog(null, "Este ajuste ya fue anulado.");
                return false;
            }

            // 2. Obtener detalles para revertir
            PreparedStatement psDet = con.prepareStatement(
                    "SELECT id_producto, diferencia, diferencia_pendientes "
                    + "FROM ajustes_inventario_detalle "
                    + "WHERE id_ajuste_cabecera = ?");
            psDet.setInt(1, idCabecera);
            ResultSet rsDet = psDet.executeQuery();

            // 3. Revertir stock (invertir las diferencias)
            DBstock_productos dbStock = new DBstock_productos();
            while (rsDet.next()) {
                int idProducto = rsDet.getInt("id_producto");
                double difCant = rsDet.getDouble("diferencia");
                double difPend = rsDet.getDouble("diferencia_pendientes");

                String obsBase = "Anulacion ajuste #" + idCabecera;

                if (difCant != 0) {
                    // Revertir: si fue +5, ahora es -5
                    dbStock.ajuste(idProducto, idBodega, idUser,
                            Math.abs(difCant), difCant < 0, obsBase);
                }
                if (difPend != 0) {
                    dbStock.ajustePendientes(idProducto, idBodega, idUser,
                            Math.abs(difPend), difPend < 0, obsBase + " (pendientes)");
                }
            }
            rsDet.close();
            psDet.close();

            // 4. Marcar cabecera como anulada (estado = 0)
            PreparedStatement psAnular = con.prepareStatement(
                    "UPDATE ajustes_inventario_cabecera SET estado = 0 WHERE id = ?");
            psAnular.setInt(1, idCabecera);
            psAnular.executeUpdate();
            psAnular.close();

            con.close();
            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error al anular ajuste:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (SQLException ex) {}
        }
    }
}
