/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import java.sql.*;
import javax.swing.JOptionPane;

/**
 *
 * @author Monkeyelgrande
 */
public class DB_Stock {

    public int CargarStock() {
        int resultado = 0;
        Connection con = null;

        String SSQL = "delete from stock;\n"
                + "\n"
                + "WITH consulta AS (\n"
                + "WITH ventas_referenciadas AS (\n"
                + "    -- Identifica qué productos de qué ventas están siendo referenciados por órdenes\n"
                + "    SELECT DISTINCT\n"
                + "        fd_ref.id_factura       AS id_factura_venta,\n"
                + "        fd_ref.id_producto\n"
                + "    FROM facturas_detalles fd_ref\n"
                + "    JOIN facturas_cabeceras fc_ref\n"
                + "        ON fd_ref.id_cabecera = fc_ref.id\n"
                + "    WHERE fc_ref.tipo_factura <> 'Venta'\n"
                + "      AND fd_ref.id_factura > 0  -- Solo órdenes que referencian una venta\n"
                + "),\n"
                + "\n"
                + "movimientos AS (\n"
                + "    -- 1) ENTRADAS: ingresos de mercancía (solo estado = 1 = recibido)\n"
                + "    SELECT\n"
                + "        imd.id_producto,\n"
                + "        imc.id_bodega,\n"
                + "        imd.cantidad::double precision AS qty\n"
                + "    FROM ingresos_mercancias_detalle imd\n"
                + "    JOIN ingresos_mercancias_cabecera imc\n"
                + "        ON imd.id_ingreso_cabecera = imc.id\n"
                + "    WHERE imc.estado = 1\n"
                + "\n"
                + "    UNION ALL\n"
                + "\n"
                + "    -- 2) ENTRADAS: devoluciones a bodega\n"
                + "    SELECT\n"
                + "        dd.id_producto,\n"
                + "        d.id_bodega,\n"
                + "        dd.cantidad::double precision AS qty\n"
                + "    FROM devoluciones_detalles dd\n"
                + "    JOIN devoluciones d\n"
                + "        ON dd.id_cabecera_devolucion = d.id\n"
                + "\n"
                + "    UNION ALL\n"
                + "\n"
                + "    -- 3) ENTRADAS: traslados (bodega destino)\n"
                + "    SELECT\n"
                + "        t.id_producto,\n"
                + "        t.id_bodega_destino::integer AS id_bodega,\n"
                + "        t.cantidad::double precision AS qty\n"
                + "    FROM traslados_productos t\n"
                + "\n"
                + "    UNION ALL\n"
                + "\n"
                + "    -- 4) SALIDAS: facturas tipo \"Venta\" (solo líneas NO referenciadas por órdenes)\n"
                + "    SELECT\n"
                + "        fd.id_producto,\n"
                + "        fc.id_bodega,\n"
                + "        -fd.cantidad::double precision AS qty\n"
                + "    FROM facturas_detalles fd\n"
                + "    JOIN facturas_cabeceras fc\n"
                + "        ON fd.id_cabecera = fc.id\n"
                + "    LEFT JOIN ventas_referenciadas vr\n"
                + "        ON vr.id_factura_venta = fc.id\n"
                + "       AND vr.id_producto      = fd.id_producto\n"
                + "    WHERE fc.anulado = 1\n"
                + "      AND fc.tipo_factura = 'Venta'\n"
                + "      AND vr.id_factura_venta IS NULL  -- Excluye líneas referenciadas por órdenes\n"
                + "\n"
                + "    UNION ALL\n"
                + "\n"
                + "    -- 5) SALIDAS: entregas de facturas tipo \"Salida\", \"Prestamo\", \"Eliminacion\"\n"
                + "    --    La bodega real de salida es la de entregas_productos_cabecera\n"
                + "    SELECT\n"
                + "        ep.id_producto,\n"
                + "        epc.id_bodega,\n"
                + "        -ep.cantidad::double precision AS qty\n"
                + "    FROM entregas_productos ep\n"
                + "    JOIN entregas_productos_cabecera epc\n"
                + "        ON ep.id_cabecera = epc.id\n"
                + "    JOIN facturas_cabeceras fc\n"
                + "        ON epc.id_factura = fc.id\n"
                + "    WHERE fc.anulado = 1\n"
                + "      AND fc.tipo_factura <> 'Venta'\n"
                + "\n"
                + "    UNION ALL\n"
                + "\n"
                + "    -- 6) SALIDAS: traslados (bodega origen)\n"
                + "    SELECT\n"
                + "        t.id_producto,\n"
                + "        t.id_bodega_origen::integer AS id_bodega,\n"
                + "        -t.cantidad::double precision AS qty\n"
                + "    FROM traslados_productos t\n"
                + "),\n"
                + "\n"
                + "-- Stock por producto y bodega\n"
                + "stock_por_bodega AS (\n"
                + "    SELECT\n"
                + "        id_producto,\n"
                + "        id_bodega,\n"
                + "        SUM(qty) AS cantidad\n"
                + "    FROM movimientos\n"
                + "    GROUP BY id_producto, id_bodega\n"
                + "),\n"
                + "\n"
                + "-- Stock total por producto (sumando todas las bodegas)\n"
                + "stock_total AS (\n"
                + "    SELECT\n"
                + "        id_producto,\n"
                + "        SUM(cantidad) AS cantidad_total\n"
                + "    FROM stock_por_bodega\n"
                + "    GROUP BY id_producto\n"
                + "),\n"
                + "\n"
                + "-- Cantidad facturada (NO venta) por producto\n"
                + "facturado_no_venta AS (\n"
                + "    SELECT\n"
                + "        fd.id_producto,\n"
                + "        SUM(fd.cantidad) AS qty_facturada\n"
                + "    FROM facturas_detalles fd\n"
                + "    JOIN facturas_cabeceras fc\n"
                + "        ON fd.id_cabecera = fc.id\n"
                + "    WHERE fc.anulado = 1\n"
                + "      AND fc.tipo_factura <> 'Venta'\n"
                + "    GROUP BY fd.id_producto\n"
                + "),\n"
                + "\n"
                + "-- Cantidad realmente entregada de esas facturas (según entregas_productos)\n"
                + "entregado_no_venta AS (\n"
                + "    SELECT\n"
                + "        ep.id_producto,\n"
                + "        SUM(ep.cantidad) AS qty_entregada\n"
                + "    FROM entregas_productos ep\n"
                + "    JOIN entregas_productos_cabecera epc\n"
                + "        ON ep.id_cabecera = epc.id\n"
                + "    JOIN facturas_cabeceras fc\n"
                + "        ON epc.id_factura = fc.id\n"
                + "    WHERE fc.anulado = 1\n"
                + "      AND fc.tipo_factura <> 'Venta'\n"
                + "    GROUP BY ep.id_producto\n"
                + "),\n"
                + "\n"
                + "-- Pendientes por producto (solo si facturado > entregado)\n"
                + "pendientes AS (\n"
                + "    SELECT\n"
                + "        f.id_producto,\n"
                + "        GREATEST(\n"
                + "            f.qty_facturada - COALESCE(e.qty_entregada, 0),\n"
                + "            0\n"
                + "        ) AS pendiente_por_entregar\n"
                + "    FROM facturado_no_venta f\n"
                + "    LEFT JOIN entregado_no_venta e\n"
                + "        ON e.id_producto = f.id_producto\n"
                + ")\n"
                + "\n"
                + "SELECT\n"
                + "    p.id AS id_producto,\n"
                + "    p.codigo_barras,\n"
                + "    p.descripcion,\n"
                + "    COALESCE(st.cantidad_total, 0) AS cantidad_total,\n"
                + "    COALESCE(pen.pendiente_por_entregar, 0) AS pendiente_por_entregar,\n"
                + "    COALESCE(st.cantidad_total, 0) - COALESCE(pen.pendiente_por_entregar, 0)\n"
                + "        AS cantidad_disponible_para_venta\n"
                + "FROM productos p\n"
                + "LEFT JOIN stock_total st\n"
                + "    ON st.id_producto = p.id\n"
                + "LEFT JOIN pendientes pen\n"
                + "    ON pen.id_producto = p.id\n"
                + "ORDER BY p.codigo_barras\n"
                + ")\n"
                + "insert into stock (codigo_barras,stock, pendientes_entregas) select codigo_barras, round(avg(cantidad_disponible_para_venta)::numeric,3), "
                + "pendiente_por_entregar as total from consulta group by codigo_barras, pendiente_por_entregar";
        System.out.println(SSQL);
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);

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

}
