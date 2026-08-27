-- =============================================================================
-- UNIFICACION DE USUARIOS DUPLICADOS (importados de otros sistemas)
-- Base: bodega_nuevo. Compatible PostgreSQL 9.4.
--
-- QUE HACE
--   1. Reasigna TODAS las referencias de los usuarios duplicados al usuario
--      canonico (27 columnas en 25 tablas, verificadas contra el esquema real).
--   2. Fusiona los permisos en el usuario canonico:
--        - users.rol_precios        -> el mayor del grupo
--        - users.aprueba_compras    -> OR del grupo
--        - usuario_opciones         -> union de las opciones concedidas
--        - usuario_roles_precios    -> union de los roles
--   3. Verifica que no quede NINGUNA referencia a los ids viejos.
--   4. Elimina los usuarios duplicados.
--
-- MAPA DE UNIFICACION (confirmado con el cliente el 2026-08-26)
--   Juan Carlos:  "Jan Carlos" (7)  y "JUANCA" (30)        ->  FLECHAS (6)
--   Niyi:         NIYI BODEGA (27), NIYI ADMINISTRADORA/
--                 NIYI CONTADOR (28) y NIYI PRECIOS (29)   ->  NIYI (14)
--
--   El canonico conserva su login, password, perfil, bodega y configuracion
--   de impresion; solo ABSORBE permisos, nunca los pierde.
--
-- NOTAS
--   - auditoria_ingresos.id_usuario NO se toca: es rastro historico inmutable
--     (y hoy no contiene filas de los ids viejos; el nombre queda congelado en
--     sus columnas de texto, asi que el panel de historial no se ve afectado).
--   - facturas_impresas.vendedor y sucursales.responsable guardan nombres de
--     texto ajenos a users (nombres completos de vendedores): no se tocan.
--   - facturas_cabeceras.impreso_vendedor es una bandera 0/1, no un id de user.
--   - El trigger de auditoria de ingresos_productos_cabecera se deshabilita
--     durante la reasignacion para no generar registros de auditoria falsos.
--
-- COMO EJECUTAR (con la aplicacion CERRADA en todos los equipos):
--   psql -h localhost -p 5432 -U postgres -d bodega_nuevo -f sql/unificacion_usuarios.sql
--
--   Todo corre en UNA transaccion: si algo falla, no cambia nada.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 0. Mapa de unificacion (id_viejo -> id_nuevo). Editar aqui si cambia algo.
-- -----------------------------------------------------------------------------
CREATE TEMP TABLE mapa_usuarios (
    id_viejo integer PRIMARY KEY,
    id_nuevo integer NOT NULL
) ON COMMIT DROP;

INSERT INTO mapa_usuarios (id_viejo, id_nuevo) VALUES
    ( 7,  6),   -- Jan Carlos           -> FLECHAS
    (30,  6),   -- JUANCA               -> FLECHAS
    (27, 14),   -- NIYI BODEGA          -> NIYI
    (28, 14),   -- NIYI ADMINISTRADORA  -> NIYI
    (29, 14);   -- NIYI PRECIOS         -> NIYI

-- Sanidad del mapa: ambos ids existen y ningun destino es a la vez un viejo.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM mapa_usuarios m
               WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = m.id_viejo)) THEN
        RAISE EXCEPTION 'El mapa referencia un id_viejo que no existe en users';
    END IF;
    IF EXISTS (SELECT 1 FROM mapa_usuarios m
               WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = m.id_nuevo)) THEN
        RAISE EXCEPTION 'El mapa referencia un id_nuevo que no existe en users';
    END IF;
    IF EXISTS (SELECT 1 FROM mapa_usuarios m
               WHERE m.id_nuevo IN (SELECT id_viejo FROM mapa_usuarios)) THEN
        RAISE EXCEPTION 'Un id_nuevo del mapa figura tambien como id_viejo';
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 1. Reasignar todas las referencias a los ids viejos
-- -----------------------------------------------------------------------------

-- La auditoria de ingresos registra cualquier UPDATE sobre la cabecera; esta
-- correccion masiva no es un evento del negocio, asi que se apaga el trigger.
ALTER TABLE ingresos_productos_cabecera DISABLE TRIGGER trg_auditoria_ingresos_productos;

UPDATE abonos_cabeceras             t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE ajustes_inventario_cabecera  t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE auto_impresion_log_completo  t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE comparativos_cabecera        t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE comparativos_cabecera        t SET id_user_autoriza = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user_autoriza = m.id_viejo;
UPDATE cotizaciones_cabeceras       t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE cotizaciones_cabeceras       t SET id_user_edita    = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user_edita    = m.id_viejo;
UPDATE cotizaciones_compra_cabecera t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE creditos                     t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE devoluciones                 t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE egresos                      t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE entregas_productos_cabecera  t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE escaneos_qr_ordenes          t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE facturas_cabeceras           t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE facturas_cabeceras           t SET id_user_edita    = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user_edita    = m.id_viejo;
UPDATE ingresos                     t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE ingresos_mercancias_cabecera t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE ingresos_productos_cabecera  t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE movimientos_inventario       t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE novedades_facturas           t SET revisado_por     = m.id_nuevo FROM mapa_usuarios m WHERE t.revisado_por     = m.id_viejo;
UPDATE ordenes_compra_cabecera      t SET id_user_aprueba  = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user_aprueba  = m.id_viejo;
UPDATE ordenes_compra_cabecera      t SET id_user_crea     = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user_crea     = m.id_viejo;
UPDATE recortes_cabecera            t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE recortes_detalle             t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE sugeridos_cabecera           t SET id_user_crea     = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user_crea     = m.id_viejo;
UPDATE transferencias               t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;
UPDATE traslados_productos          t SET id_user          = m.id_nuevo FROM mapa_usuarios m WHERE t.id_user          = m.id_viejo;

ALTER TABLE ingresos_productos_cabecera ENABLE TRIGGER trg_auditoria_ingresos_productos;

-- -----------------------------------------------------------------------------
-- 2. Fusionar permisos en el usuario canonico (solo suma, nunca resta)
-- -----------------------------------------------------------------------------

-- 2a. Opciones de menu concedidas: si el canonico ya tiene la opcion pero
--     revocada y algun duplicado la tenia concedida, se concede.
UPDATE usuario_opciones uo
SET concedido = true
FROM (SELECT DISTINCT m.id_nuevo, o.id_opcion
      FROM usuario_opciones o
      JOIN mapa_usuarios m ON m.id_viejo = o.id_user
      WHERE o.concedido) s
WHERE uo.id_user = s.id_nuevo
  AND uo.id_opcion = s.id_opcion
  AND NOT uo.concedido;

--     Opciones concedidas que el canonico no tiene: se copian.
--     (Las revocaciones -concedido=false- de los duplicados NO se copian.)
INSERT INTO usuario_opciones (id_user, id_opcion, concedido)
SELECT DISTINCT m.id_nuevo, o.id_opcion, true
FROM usuario_opciones o
JOIN mapa_usuarios m ON m.id_viejo = o.id_user
WHERE o.concedido
  AND NOT EXISTS (SELECT 1 FROM usuario_opciones x
                  WHERE x.id_user = m.id_nuevo AND x.id_opcion = o.id_opcion);

-- 2b. Roles del modulo Precios: union.
INSERT INTO usuario_roles_precios (id_user, rol)
SELECT DISTINCT m.id_nuevo, r.rol
FROM usuario_roles_precios r
JOIN mapa_usuarios m ON m.id_viejo = r.id_user
WHERE NOT EXISTS (SELECT 1 FROM usuario_roles_precios x
                  WHERE x.id_user = m.id_nuevo AND x.rol = r.rol);

-- 2c. Permisos directos en users: rol_precios al mayor, aprueba_compras con OR.
UPDATE users u
SET rol_precios     = GREATEST(u.rol_precios, s.rol_precios),
    aprueba_compras = u.aprueba_compras OR s.aprueba_compras
FROM (SELECT m.id_nuevo,
             max(v.rol_precios)       AS rol_precios,
             bool_or(v.aprueba_compras) AS aprueba_compras
      FROM mapa_usuarios m
      JOIN users v ON v.id = m.id_viejo
      GROUP BY m.id_nuevo) s
WHERE u.id = s.id_nuevo;

-- -----------------------------------------------------------------------------
-- 3. Verificacion: no debe quedar NINGUNA referencia a los ids viejos
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    par text;
    v_tabla text;
    v_col text;
    v_n bigint;
BEGIN
    FOREACH par IN ARRAY ARRAY[
        'abonos_cabeceras:id_user',
        'ajustes_inventario_cabecera:id_user',
        'auto_impresion_log_completo:id_user',
        'comparativos_cabecera:id_user',
        'comparativos_cabecera:id_user_autoriza',
        'cotizaciones_cabeceras:id_user',
        'cotizaciones_cabeceras:id_user_edita',
        'cotizaciones_compra_cabecera:id_user',
        'creditos:id_user',
        'devoluciones:id_user',
        'egresos:id_user',
        'entregas_productos_cabecera:id_user',
        'escaneos_qr_ordenes:id_user',
        'facturas_cabeceras:id_user',
        'facturas_cabeceras:id_user_edita',
        'ingresos:id_user',
        'ingresos_mercancias_cabecera:id_user',
        'ingresos_productos_cabecera:id_user',
        'movimientos_inventario:id_user',
        'novedades_facturas:revisado_por',
        'ordenes_compra_cabecera:id_user_aprueba',
        'ordenes_compra_cabecera:id_user_crea',
        'recortes_cabecera:id_user',
        'recortes_detalle:id_user',
        'sugeridos_cabecera:id_user_crea',
        'transferencias:id_user',
        'traslados_productos:id_user'
    ] LOOP
        v_tabla := split_part(par, ':', 1);
        v_col   := split_part(par, ':', 2);
        EXECUTE format(
            'SELECT count(*) FROM %I WHERE %I IN (SELECT id_viejo FROM mapa_usuarios)',
            v_tabla, v_col) INTO v_n;
        IF v_n > 0 THEN
            RAISE EXCEPTION 'Quedan % referencia(s) en %.% a usuarios viejos: se cancela todo',
                v_n, v_tabla, v_col;
        END IF;
    END LOOP;
END $$;

-- -----------------------------------------------------------------------------
-- 4. Eliminar los usuarios duplicados
--    (sus filas en usuario_opciones y usuario_roles_precios caen por CASCADE)
-- -----------------------------------------------------------------------------
DELETE FROM users WHERE id IN (SELECT id_viejo FROM mapa_usuarios);

-- -----------------------------------------------------------------------------
-- 5. Resumen final (revisar antes de que termine la transaccion)
-- -----------------------------------------------------------------------------
SELECT u.id, u.nombre, u.user_name, u.estado, u.id_perfil, u.id_bodega,
       u.rol_precios, u.aprueba_compras,
       (SELECT count(*) FROM usuario_opciones uo
         WHERE uo.id_user = u.id AND uo.concedido)      AS opciones_concedidas,
       (SELECT string_agg(r.rol::text, ',' ORDER BY r.rol)
          FROM usuario_roles_precios r
         WHERE r.id_user = u.id)                        AS roles_precios
FROM users u
WHERE u.id IN (6, 14)
ORDER BY u.id;

COMMIT;

-- =============================================================================
-- COMPROBACION POSTERIOR (opcional)
--   -- Ya no deben existir los duplicados:
--   SELECT id, nombre, user_name FROM users WHERE id IN (7, 27, 28, 29, 30);
--   -- Historial reasignado (ejemplos):
--   SELECT count(*) FROM facturas_cabeceras            WHERE id_user = 6;   -- incluye las 297 de "Jan Carlos"
--   SELECT count(*) FROM ingresos_productos_cabecera   WHERE id_user = 6;   -- incluye los 1251 de "JUANCA"
--   SELECT count(*) FROM ingresos                      WHERE id_user = 14;  -- incluye los de NIYI ADMINISTRADORA
-- =============================================================================
