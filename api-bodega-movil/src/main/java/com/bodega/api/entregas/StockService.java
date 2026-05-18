package com.bodega.api.entregas;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Movimiento de inventario para ENTREGA, portado fielmente de
 * {@code DBstock_productos.registrarMovimiento(... TIPO_ENTREGA ...)} de la
 * app de escritorio:
 *
 *   - resta de stock_productos.cantidad   (afecta_cantidad = -1)
 *   - resta de stock_productos.pendientes (afecta_pendientes = -1)
 *   - no cambia el costo promedio
 *   - deja traza en movimientos_inventario
 *
 * Cada entrega va en su PROPIA transaccion (REQUIRES_NEW), igual que en el
 * escritorio donde DBstock_productos abria y confirmaba su propia conexion.
 */
@Service
public class StockService {

    private static final String TIPO_ENTREGA = "ENTREGA";
    private static final String TABLA_REF = "entregas_productos_cabecera";

    private final JdbcTemplate jdbc;

    public StockService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Cantidad fisica actual del producto en una bodega (0 si no hay fila). */
    public double cantidadEnBodega(int idProducto, int idBodega) {
        List<Map<String, Object>> r = jdbc.queryForList(
                "SELECT cantidad FROM stock_productos "
                + "WHERE id_producto = ? AND id_bodega = ?",
                idProducto, idBodega);
        if (r.isEmpty() || r.get(0).get("cantidad") == null) {
            return 0.0;
        }
        return ((Number) r.get(0).get("cantidad")).doubleValue();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void entrega(int idProducto, int idBodega, int idUser,
                        double cantidad, int idReferencia, String observacion) {

        // PASO 1: leer (o crear) la fila de stock con bloqueo
        Double[] actual = leerStockConBloqueo(idProducto, idBodega);
        boolean existe = actual != null;
        double cantidadAnterior = existe ? actual[0] : 0.0;
        double pendientesAnterior = existe ? actual[1] : 0.0;
        double costoPromedioAnterior = existe ? actual[2] : 0.0;

        if (!existe) {
            jdbc.update(
                    "INSERT INTO stock_productos "
                    + "(id_producto, id_bodega, cantidad, pendientes, costo_promedio) "
                    + "VALUES (?, ?, 0, 0, 0)",
                    idProducto, idBodega);
        }

        // PASO 2: nuevos valores (afecta_cantidad = -1, afecta_pendientes = -1)
        double cantidadNueva = cantidadAnterior - cantidad;
        double pendientesNuevo = pendientesAnterior - cantidad;
        // ENTREGA no modifica el costo promedio
        double costoPromedioNuevo = costoPromedioAnterior;

        // PASO 3: actualizar stock_productos
        jdbc.update(
                "UPDATE stock_productos SET cantidad = ?, pendientes = ?, "
                + "costo_promedio = ?, updated_at = now() "
                + "WHERE id_producto = ? AND id_bodega = ?",
                cantidadNueva, pendientesNuevo, costoPromedioNuevo,
                idProducto, idBodega);

        // PASO 4: histórico en movimientos_inventario
        final String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
        final double cpa = costoPromedioAnterior;
        final double cpn = costoPromedioNuevo;
        final double ca = cantidadAnterior;
        final double cn = cantidadNueva;
        final double pa = pendientesAnterior;
        final double pn = pendientesNuevo;

        jdbc.update(con -> {
            java.sql.PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO movimientos_inventario ("
                    + "id_producto, id_bodega, id_user, tipo, afecta_cantidad, "
                    + "afecta_pendientes, valor, valor_anterior, valor_nuevo, "
                    + "costo_unitario, costo_promedio_anterior, costo_promedio_nuevo, "
                    + "cantidad_anterior, cantidad_nueva, pendientes_anterior, "
                    + "pendientes_nuevo, id_referencia, tabla_referencia, fecha, "
                    + "hora, observacion) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                    + "('now'::text)::date, ?, ?)");
            int i = 1;
            ps.setInt(i++, idProducto);
            ps.setInt(i++, idBodega);
            ps.setInt(i++, idUser);
            ps.setString(i++, TIPO_ENTREGA);
            ps.setInt(i++, -1);                 // afecta_cantidad
            ps.setInt(i++, -1);                 // afecta_pendientes
            ps.setDouble(i++, cantidad);        // valor
            ps.setNull(i++, Types.DOUBLE);      // valor_anterior
            ps.setNull(i++, Types.DOUBLE);      // valor_nuevo
            ps.setNull(i++, Types.DOUBLE);      // costo_unitario
            ps.setDouble(i++, cpa);
            ps.setDouble(i++, cpn);
            ps.setDouble(i++, ca);
            ps.setDouble(i++, cn);
            ps.setDouble(i++, pa);
            ps.setDouble(i++, pn);
            ps.setInt(i++, idReferencia);
            ps.setString(i++, TABLA_REF);
            ps.setString(i++, hora);
            if (observacion != null) {
                ps.setString(i++, observacion);
            } else {
                ps.setNull(i++, Types.VARCHAR);
            }
            return ps;
        });
    }

    /** SELECT ... FOR UPDATE; devuelve [cantidad, pendientes, costo] o null. */
    private Double[] leerStockConBloqueo(int idProducto, int idBodega) {
        List<Double[]> filas = jdbc.query(
                "SELECT cantidad, pendientes, costo_promedio FROM stock_productos "
                + "WHERE id_producto = ? AND id_bodega = ? FOR UPDATE",
                new Object[]{idProducto, idBodega},
                (rs, n) -> new Double[]{
                        rs.getDouble("cantidad"),
                        rs.getDouble("pendientes"),
                        rs.getDouble("costo_promedio")});
        return filas.isEmpty() ? null : filas.get(0);
    }
}
