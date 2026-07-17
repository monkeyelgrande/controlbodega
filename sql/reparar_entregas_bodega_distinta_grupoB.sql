-- =============================================================================
-- REPARACIÓN — Grupo B: entregas POST-CUTOVER con bodega distinta a la de la orden
-- Base: producción (bodega_nuevo). PostgreSQL 9.4.
--
-- Contexto (informe):
--   * movimientos_inventario / stock_productos se cargaron en el cutover
--     (MIN(movimientos_inventario.fecha), ~2026-04-20). Solo las entregas
--     >= cutover afectan el stock actual.
--   * Grupo A (entregas PRE-cutover, ~14.5k) NO se toca: no tiene movimientos y
--     el stock ya está baselado. (Decisión del negocio: dejarlas como están.)
--   * Este script corrige SOLO el Grupo B (post-cutover), y dentro de él SOLO
--     las cabeceras 100% sanas: aquellas cuyas líneas TODAVÍA muestran la firma
--     del error (la bodega de la orden conserva los 'pendientes' sin consumir).
--     Las cabeceras "arrastradas" (parcialmente reconciliadas por actividad
--     posterior) se EXCLUYEN y se listan para revisión manual.
--
-- Dirección de la corrección (confirmada): mover a la bodega de la orden.
--   - Bodega de entrega (equivocada):  cantidad += q, pendientes += q  (revierte)
--   - Bodega de la orden (correcta):   cantidad -= q, pendientes -= q  (aplica)
--   - entregas_productos_cabecera.id_bodega := bodega de la orden
--   - Se insertan movimientos de auditoría tipo 'CORRECCION_BODEGA'.
--
-- CÓMO USARLO (IMPORTANTE):
--   1) Corre PARTE 0 y PARTE 1 (solo lectura) y revisa el preview.
--   2) Corre la PARTE 2 (transacción). Deja el COMMIT comentado.
--   3) Revisa los SELECT de verificación (PARTE 3, dentro de la misma transacción).
--   4) Si todo cuadra, ejecuta COMMIT;  si no, ROLLBACK;
--   Ejecutar de forma interactiva (psql/pgAdmin) para poder decidir el COMMIT.
-- =============================================================================


-- ============================ PARTE 0 — panorama (solo lectura) ==============
SELECT (SELECT MIN(fecha) FROM movimientos_inventario) AS cutover,
       COUNT(*) AS entregas_descuadradas_post_cutover
FROM entregas_productos_cabecera epc
JOIN facturas_cabeceras fc ON fc.id = epc.id_factura
WHERE epc.id_bodega IS DISTINCT FROM fc.id_bodega
  AND epc.fecha_entrega >= (SELECT MIN(fecha) FROM movimientos_inventario);


-- ============================ PARTE 2 — corrección (transacción) =============
BEGIN;

-- --- Conjunto objetivo: líneas post-cutover con bodega entrega != bodega orden ---
CREATE TEMP TABLE _mal ON COMMIT DROP AS
SELECT epc.id            AS id_cab,
       epc.id_factura    AS id_orden,
       epc.id_bodega     AS bod_mal,
       fc.id_bodega      AS bod_ok,
       ep.id_producto    AS id_producto,
       ep.cantidad::numeric AS q
FROM entregas_productos_cabecera epc
JOIN facturas_cabeceras fc  ON fc.id = epc.id_factura
JOIN entregas_productos ep  ON ep.id_cabecera = epc.id
WHERE epc.id_bodega IS DISTINCT FROM fc.id_bodega
  AND epc.fecha_entrega >= (SELECT MIN(fecha) FROM movimientos_inventario);

-- Demanda total por (producto, bodega de la orden) sobre TODO el objetivo.
CREATE TEMP TABLE _demanda ON COMMIT DROP AS
SELECT id_producto, bod_ok, SUM(q) AS q_total
FROM _mal GROUP BY id_producto, bod_ok;

-- Cabeceras 100% sanas: TODAS sus líneas tienen pendientes suficientes en la
-- bodega de la orden (firma del error aún presente = seguro de corregir).
CREATE TEMP TABLE _cab_ok ON COMMIT DROP AS
SELECT m.id_cab
FROM _mal m
JOIN _demanda d       ON d.id_producto = m.id_producto AND d.bod_ok = m.bod_ok
JOIN stock_productos s ON s.id_producto = m.id_producto AND s.id_bodega = m.bod_ok
GROUP BY m.id_cab
HAVING bool_and( s.pendientes >= d.q_total );

-- Líneas a corregir y delta agregado por (producto, bod_mal, bod_ok).
CREATE TEMP TABLE _lineas ON COMMIT DROP AS
SELECT * FROM _mal WHERE id_cab IN (SELECT id_cab FROM _cab_ok);

CREATE TEMP TABLE _delta ON COMMIT DROP AS
SELECT id_producto, bod_mal, bod_ok, SUM(q) AS delta
FROM _lineas GROUP BY id_producto, bod_mal, bod_ok;

-- Foto del stock ANTES (para auditoría y backup).
CREATE TEMP TABLE _snap ON COMMIT DROP AS
SELECT s.id_producto, s.id_bodega, s.cantidad, s.pendientes
FROM stock_productos s
JOIN (SELECT id_producto, bod_mal AS bodega FROM _delta
      UNION
      SELECT id_producto, bod_ok  FROM _delta) a
  ON a.id_producto = s.id_producto AND a.bodega = s.id_bodega;


-- ---------------- PARTE 1 — PREVIEW (revisar ANTES de continuar) -------------
-- (a) Cabeceras que SÍ se van a corregir:
SELECT epc.id AS id_cab, epc.id_factura AS orden, epc.fecha_entrega,
       epc.id_bodega AS bod_entrega, fc.id_bodega AS bod_orden
FROM entregas_productos_cabecera epc
JOIN facturas_cabeceras fc ON fc.id = epc.id_factura
WHERE epc.id IN (SELECT id_cab FROM _cab_ok)
ORDER BY epc.fecha_entrega, epc.id;

-- (b) Movimiento de stock por producto (antes -> después):
SELECT d.id_producto, d.bod_mal, d.bod_ok, d.delta,
       sm.cantidad AS bmal_cant_antes,  sm.cantidad + d.delta AS bmal_cant_desp,
       sm.pendientes AS bmal_pend_antes, sm.pendientes + d.delta AS bmal_pend_desp,
       so.cantidad AS bok_cant_antes,   so.cantidad - d.delta AS bok_cant_desp,
       so.pendientes AS bok_pend_antes, so.pendientes - d.delta AS bok_pend_desp
FROM _delta d
JOIN _snap sm ON sm.id_producto = d.id_producto AND sm.id_bodega = d.bod_mal
JOIN _snap so ON so.id_producto = d.id_producto AND so.id_bodega = d.bod_ok
ORDER BY d.id_producto;

-- (c) EXCLUIDAS (post-cutover descuadradas pero NO 100% sanas) -> revisión manual:
SELECT epc.id AS id_cab, epc.id_factura AS orden, epc.fecha_entrega,
       epc.id_bodega AS bod_entrega, fc.id_bodega AS bod_orden
FROM entregas_productos_cabecera epc
JOIN facturas_cabeceras fc ON fc.id = epc.id_factura
WHERE epc.id_bodega IS DISTINCT FROM fc.id_bodega
  AND epc.fecha_entrega >= (SELECT MIN(fecha) FROM movimientos_inventario)
  AND epc.id NOT IN (SELECT id_cab FROM _cab_ok)
ORDER BY epc.fecha_entrega, epc.id;


-- ------------------------------ BACKUPS --------------------------------------
-- (renombra el sufijo de fecha si lo necesitas)
CREATE TABLE bak_stock_grupoB_20260702        AS SELECT * FROM _snap;
CREATE TABLE bak_entregas_cab_grupoB_20260702 AS
  SELECT * FROM entregas_productos_cabecera WHERE id IN (SELECT id_cab FROM _cab_ok);


-- ---------------------- MOVIMIENTOS DE AUDITORÍA -----------------------------
-- Reversión en la bodega equivocada (+cantidad, +pendientes)
INSERT INTO movimientos_inventario
  (id_producto, id_bodega, id_user, tipo, afecta_cantidad, afecta_pendientes, valor,
   cantidad_anterior, cantidad_nueva, pendientes_anterior, pendientes_nuevo,
   id_referencia, tabla_referencia, fecha, hora, observacion)
SELECT d.id_producto, d.bod_mal, (SELECT MIN(id) FROM users), 'CORRECCION_BODEGA', 1, 1, d.delta,
       sm.cantidad, sm.cantidad + d.delta, sm.pendientes, sm.pendientes + d.delta,
       NULL, 'stock_productos', current_date, to_char(now(),'HH24:MI:SS'),
       'Correccion bodega entrega (Grupo B): revierte en bodega '||d.bod_mal||' -> orden '||d.bod_ok
FROM _delta d
JOIN _snap sm ON sm.id_producto = d.id_producto AND sm.id_bodega = d.bod_mal;

-- Aplicación en la bodega de la orden (-cantidad, -pendientes)
INSERT INTO movimientos_inventario
  (id_producto, id_bodega, id_user, tipo, afecta_cantidad, afecta_pendientes, valor,
   cantidad_anterior, cantidad_nueva, pendientes_anterior, pendientes_nuevo,
   id_referencia, tabla_referencia, fecha, hora, observacion)
SELECT d.id_producto, d.bod_ok, (SELECT MIN(id) FROM users), 'CORRECCION_BODEGA', -1, -1, d.delta,
       so.cantidad, so.cantidad - d.delta, so.pendientes, so.pendientes - d.delta,
       NULL, 'stock_productos', current_date, to_char(now(),'HH24:MI:SS'),
       'Correccion bodega entrega (Grupo B): aplica en bodega de la orden '||d.bod_ok
FROM _delta d
JOIN _snap so ON so.id_producto = d.id_producto AND so.id_bodega = d.bod_ok;


-- ------------------------------- STOCK ---------------------------------------
UPDATE stock_productos s
SET cantidad = s.cantidad + d.delta, pendientes = s.pendientes + d.delta, updated_at = now()
FROM _delta d
WHERE s.id_producto = d.id_producto AND s.id_bodega = d.bod_mal;

UPDATE stock_productos s
SET cantidad = s.cantidad - d.delta, pendientes = s.pendientes - d.delta, updated_at = now()
FROM _delta d
WHERE s.id_producto = d.id_producto AND s.id_bodega = d.bod_ok;


-- ----------------------------- ENTREGAS --------------------------------------
UPDATE entregas_productos_cabecera epc
SET id_bodega = fc.id_bodega
FROM facturas_cabeceras fc
WHERE fc.id = epc.id_factura
  AND epc.id IN (SELECT id_cab FROM _cab_ok);


-- ----------------- POSTCONDICIÓN (aborta sola si algo quedó mal) -------------
DO $$
DECLARE v_neg int; v_mis int;
BEGIN
  -- ningún pendiente negativo en la bodega de la orden tras aplicar
  SELECT count(*) INTO v_neg
  FROM stock_productos s JOIN _delta d
    ON d.id_producto = s.id_producto AND d.bod_ok = s.id_bodega
  WHERE s.pendientes < 0;
  IF v_neg > 0 THEN
    RAISE EXCEPTION 'ABORTA: % producto(s) con pendientes negativos en la bodega de la orden', v_neg;
  END IF;

  -- ninguna cabecera corregida debe seguir descuadrada
  SELECT count(*) INTO v_mis
  FROM entregas_productos_cabecera epc JOIN facturas_cabeceras fc ON fc.id = epc.id_factura
  WHERE epc.id IN (SELECT id FROM bak_entregas_cab_grupoB_20260702)
    AND epc.id_bodega IS DISTINCT FROM fc.id_bodega;
  IF v_mis > 0 THEN
    RAISE EXCEPTION 'ABORTA: % cabecera(s) corregida(s) siguen descuadradas', v_mis;
  END IF;

  RAISE NOTICE 'OK: correccion consistente. Revisa PARTE 3 y haz COMMIT.';
END $$;


-- ============================ PARTE 3 — verificación =========================
-- Descuadradas post-cutover restantes (debería quedar solo lo EXCLUIDO manual):
SELECT COUNT(*) AS descuadradas_post_cutover_restantes
FROM entregas_productos_cabecera epc
JOIN facturas_cabeceras fc ON fc.id = epc.id_factura
WHERE epc.id_bodega IS DISTINCT FROM fc.id_bodega
  AND epc.fecha_entrega >= (SELECT MIN(fecha) FROM movimientos_inventario);


-- Si todo está correcto:
-- COMMIT;
-- Si algo no cuadra:
-- ROLLBACK;
