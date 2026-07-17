-- =============================================================================
-- DIAGNÓSTICO: entregas cuya bodega difiere de la bodega de la orden.
-- Solo lectura. Sirve para medir el alcance del problema (entregas históricas
-- registradas en una bodega distinta a la de creación de la orden, típicamente
-- por el bug del combo que caía en la bodega 1).
--
-- NO corrige nada. La corrección de stock histórico debe hacerse con cuidado
-- (revertir el movimiento en la bodega equivocada y aplicarlo en la correcta),
-- caso por caso; consultar antes de ejecutar cualquier UPDATE.
-- =============================================================================

SELECT
    epc.id                AS id_entrega_cabecera,
    epc.id_factura        AS id_orden,
    fc.codigo             AS codigo_orden,
    fc.tipo_factura       AS tipo,
    epc.id_bodega         AS bodega_entrega_id,
    be.nombre             AS bodega_entrega,
    fc.id_bodega          AS bodega_orden_id,
    bo.nombre             AS bodega_orden,
    epc.fecha_entrega,
    epc.hora_entrega,
    epc.id_user
FROM entregas_productos_cabecera epc
JOIN facturas_cabeceras fc ON fc.id = epc.id_factura
LEFT JOIN bodegas be ON be.id = epc.id_bodega
LEFT JOIN bodegas bo ON bo.id = fc.id_bodega
WHERE epc.id_bodega IS DISTINCT FROM fc.id_bodega
ORDER BY epc.fecha_entrega DESC, epc.id DESC;

-- Resumen por par (bodega_entrega -> bodega_orden):
-- SELECT be.nombre AS bodega_entrega, bo.nombre AS bodega_orden, COUNT(*) AS entregas
-- FROM entregas_productos_cabecera epc
-- JOIN facturas_cabeceras fc ON fc.id = epc.id_factura
-- LEFT JOIN bodegas be ON be.id = epc.id_bodega
-- LEFT JOIN bodegas bo ON bo.id = fc.id_bodega
-- WHERE epc.id_bodega IS DISTINCT FROM fc.id_bodega
-- GROUP BY be.nombre, bo.nombre
-- ORDER BY entregas DESC;
