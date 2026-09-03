-- =============================================================================
-- UPDATE CONTROLBODEGA   -   actualizador acumulativo e idempotente
--
-- ESTE ES EL UNICO ARCHIVO DE ACTUALIZACION DEL SISTEMA.
-- Se corre tal cual sobre una base de controlbodega que YA este en la linea
-- base del 2026-08-29 y la deja al dia. Es RE-EJECUTABLE: correrlo dos veces
-- no hace dano y no duplica nada.
--
--     psql -U postgres -d <base> -f sql/update_controlbodega.sql
--
-- -----------------------------------------------------------------------------
-- LINEA BASE
--
-- Todo lo anterior al 2026-08-30 se archivo en
--     sql/historico/update_controlbodega_20260829.sql
-- Ese archivo construye la linea base y es lo unico de sql/historico/ que se
-- corre. Si una base se quedo atras, corrilo UNA vez y despues segui con este.
-- La PARTE A de aqui abajo verifica la linea base y frena el script con un
-- mensaje claro si falta algo, para no aplicar cambios sobre una base a medias.
--
-- -----------------------------------------------------------------------------
-- REGLA DE TRABAJO (para cada cambio nuevo del sistema)
--
--   1. El cambio de esquema o de catalogo se agrega al final, en la PARTE B,
--      como un incremento nuevo con su fecha (B.2, B.3, ...), con su propio
--      BEGIN;/COMMIT; y una nota de que agrega y por que. NO se crea un
--      archivo de migracion aparte.
--   2. El MISMO cambio se refleja en sql/controlbodega.sql, que es el script
--      de construccion desde cero para una empresa nueva. Los dos archivos
--      viajan siempre juntos, en el mismo commit.
--   3. Aqui solo entra lo que es seguro correr en cualquier base:
--          tablas, columnas, indices, llaves, funciones, triggers, vistas
--          catalogos del sistema (perfiles, modulos, opciones de permisos)
--          semillas minimas, y solo cuando la tabla esta vacia
--      NO entra nada que toque datos del negocio de un cliente: cargas de
--      inventario, deduplicaciones, unificaciones de usuarios, correcciones
--      puntuales. Eso sigue siendo un script propio en sql/, se corre a mano
--      una sola vez y se documenta en la PARTE C.
--   4. Motor destino PostgreSQL 9.4: no hay ADD COLUMN IF NOT EXISTS ni
--      CREATE INDEX IF NOT EXISTS. Se verifica a mano con DO $$ ... END $$;
--      contra information_schema / pg_class / pg_constraint.
--
-- -----------------------------------------------------------------------------
-- COMO ESTA ORGANIZADO
--
--   PARTE A   Guarda de linea base. Frena si la base viene atrasada.
--   PARTE B   Incrementos. Cada uno con su fecha y su transaccion.
--   PARTE C   Pasos que requieren decision. Fuera de transaccion y comentados.
--   PARTE D   Verificacion de solo lectura.
-- =============================================================================

\set ON_ERROR_STOP on


-- #############################################################################
-- ##  PARTE A   GUARDA DE LINEA BASE                                         ##
-- #############################################################################

-- Comprueba que la base traiga la linea base del 2026-08-29. Si falta algo,
-- corta aca y dice exactamente que: aplicar la PARTE B sobre una base a medias
-- deja el sistema en un estado que nadie puede diagnosticar despues.

DO $$
DECLARE
    faltan text := '';
    v_tmp  text;
BEGIN
    SELECT string_agg(t.nombre, ', ') INTO v_tmp
      FROM (VALUES ('opciones'), ('perfil_opciones'), ('usuario_opciones'),
                   ('modulos'), ('ordenes_compra_cabecera'), ('descuentos'),
                   ('creditos'), ('porcentajes_comision'), ('ingresos'),
                   ('auditoria_caja'), ('auditoria_ingresos'),
                   ('escaneos_qr_ordenes'), ('productos_unidades_entrega')
           ) AS t(nombre)
     WHERE NOT EXISTS (SELECT 1 FROM information_schema.tables
                        WHERE table_schema = 'public' AND table_name = t.nombre);
    IF v_tmp IS NOT NULL THEN
        faltan := faltan || E'\n  tablas:    ' || v_tmp;
    END IF;

    SELECT string_agg(t.nombre, ', ') INTO v_tmp
      FROM (VALUES ('v_auditoria_caja'), ('v_auditoria_creditos'),
                   ('v_auditoria_ingresos')
           ) AS t(nombre)
     WHERE NOT EXISTS (SELECT 1 FROM information_schema.views
                        WHERE table_schema = 'public' AND table_name = t.nombre);
    IF v_tmp IS NOT NULL THEN
        faltan := faltan || E'\n  vistas:    ' || v_tmp;
    END IF;

    SELECT string_agg(t.nombre, ', ') INTO v_tmp
      FROM (VALUES ('fn_auditoria_caja'), ('asignar_bodegas_entrega'),
                   ('seleccionar_bodega_descarga'), ('fn_notify_orden_nueva')
           ) AS t(nombre)
     WHERE NOT EXISTS (SELECT 1 FROM pg_proc WHERE proname = t.nombre);
    IF v_tmp IS NOT NULL THEN
        faltan := faltan || E'\n  funciones: ' || v_tmp;
    END IF;

    SELECT string_agg(t.tabla || '.' || t.col, ', ') INTO v_tmp
      FROM (VALUES ('ingresos', 'id_caja'), ('configuraciones', 'modo_precios'),
                   ('bodegas', 'entrega_automatica'),
                   ('facturas_impresas', 'nit_cliente')
           ) AS t(tabla, col)
     WHERE NOT EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = t.tabla AND column_name = t.col);
    IF v_tmp IS NOT NULL THEN
        faltan := faltan || E'\n  columnas:  ' || v_tmp;
    END IF;

    -- Catalogo: las cinco opciones con que cerro la linea base.
    IF EXISTS (SELECT 1 FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = 'opciones') THEN
        SELECT string_agg(t.clave, ', ') INTO v_tmp
          FROM (VALUES ('creditos_porcentajes_comision'), ('creditos_comisiones'),
                       ('creditos_cruzar_saldo'), ('creditos_auditoria'),
                       ('ordenes_eliminar_entrega')
               ) AS t(clave)
         WHERE NOT EXISTS (SELECT 1 FROM opciones o WHERE o.clave = t.clave);
        IF v_tmp IS NOT NULL THEN
            faltan := faltan || E'\n  opciones:  ' || v_tmp;
        END IF;
    END IF;

    IF faltan <> '' THEN
        RAISE EXCEPTION E'Esta base NO tiene la linea base del 2026-08-29.\nFalta:%\n\nCorre UNA vez sql/historico/update_controlbodega_20260829.sql\ny volve a ejecutar este archivo.', faltan;
    END IF;
END $$;


-- #############################################################################
-- ##  PARTE B   INCREMENTOS                                                  ##
-- #############################################################################

-- =============================================================================
-- B.1  (2026-08-30)  PERMISOS FINOS DEL MENU PRODUCTOS
--      Hasta ahora el menu Productos era todo o nada: con
--      'jMenu_productos_principal' concedido el usuario veia (y podia usar)
--      las cinco entradas del menu -- Productos, Ingreso de mercancia,
--      Consulta, Traslados entre bodegas y Ajustes de inventario -- sin que
--      se pudiera separar quien entra a cada una ni que puede hacer adentro.
--
--      Esta parte agrega 18 opciones: una por entrada del menu (abrir) y
--      una por accion dentro de cada pantalla (crear / editar / eliminar).
--
--      Reparto por perfil, calculado desde lo que cada perfil YA tenia para
--      que el dia 1 nadie pierda accesos:
--        - quien tenia el menu o el boton de Productos      -> ver/crear/editar/deshabilitar productos
--        - quien tenia el menu o el boton Ingreso productos -> ver/crear/editar ingresos de mercancia
--        - quien tenia el menu Productos                    -> traslados y ajustes (ver/crear/editar)
--        - todos los perfiles                               -> 'productos_consulta' (el boton
--          "Consultar" del escritorio nunca estuvo restringido)
--
--      EXCEPCION DELIBERADA: las cinco acciones destructivas -- eliminar
--      producto, eliminar ingreso de mercancia, pasarlo a RECIBIDO, eliminar
--      traslado y anular ajuste -- NO se conceden a ningun perfil, porque
--      todas reversan stock. Solo el perfil 1 (Admin) las tiene, por codigo.
--      Antes dos de ellas ya pedian clave de administrador; las otras tres
--      estaban abiertas y ahora quedan cerradas. Para devolverselas a un
--      perfil o a una persona, usar la pantalla de permisos, o ver C.1.
-- =============================================================================

BEGIN;

INSERT INTO opciones (clave, nombre, modulo, componente, orden)
SELECT v.clave, v.nombre, v.modulo, v.componente, v.orden
FROM (VALUES
    -- Productos (catalogo)
    ('productos_ver',              'Productos: abrir y ver',                'Productos', 'jmenu_productos',            100),
    ('productos_crear',            'Productos: crear',                      'Productos', 'btn_crear',                  110),
    ('productos_editar',           'Productos: editar',                     'Productos', 'btn_editar',                 120),
    ('productos_eliminar',         'Productos: eliminar',                   'Productos', 'btn_eliminar',               130),
    ('productos_deshabilitar',     'Productos: habilitar / deshabilitar',   'Productos', 'btn_deshabilitar',           140),
    ('productos_consulta',         'Consulta de productos y existencias',   'Productos', 'jMenuItem3',                 150),
    -- Ingreso de mercancia
    ('ingresos_mercancia_ver',     'Ingreso de mercancia: abrir y ver',     'Productos', 'jMenuItem2',                 200),
    ('ingresos_mercancia_crear',   'Ingreso de mercancia: crear',           'Productos', 'btn_crear',                  210),
    ('ingresos_mercancia_editar',  'Ingreso de mercancia: editar',          'Productos', 'btn_editar',                 220),
    ('ingresos_mercancia_eliminar','Ingreso de mercancia: eliminar',        'Productos', 'btn_eliminar',               230),
    ('ingresos_mercancia_recibir', 'Ingreso de mercancia: pasar a recibido','Productos', 'jtabla',                     240),
    -- Traslados entre bodegas
    ('traslados_ver',              'Traslados entre bodegas: abrir y ver',  'Productos', 'jmenu_mover_productos',      300),
    ('traslados_crear',            'Traslados entre bodegas: crear',        'Productos', 'btn_crear',                  310),
    ('traslados_editar',           'Traslados entre bodegas: editar',       'Productos', 'btn_editar',                 320),
    ('traslados_eliminar',         'Traslados entre bodegas: eliminar',     'Productos', 'btn_eliminar',               330),
    -- Ajustes de inventario
    ('ajustes_inventario_ver',     'Ajustes de inventario: abrir y ver',    'Productos', 'jMenu_verificar_inventario', 400),
    ('ajustes_inventario_crear',   'Ajustes de inventario: crear',          'Productos', 'btn_crear',                  410),
    ('ajustes_inventario_anular',  'Ajustes de inventario: anular',         'Productos', 'btn_eliminar',               420)
) AS v (clave, nombre, modulo, componente, orden)
WHERE NOT EXISTS (SELECT 1 FROM opciones o WHERE o.clave = v.clave);

-- Reparto: el perfil que ya entraba a una pantalla conserva la pantalla y su
-- trabajo normal (crear/editar). Se resuelve contra perfil_opciones de ESTA
-- base, no contra la semilla, para respetar los perfiles que el cliente haya
-- ajustado a mano.
INSERT INTO perfil_opciones (id_perfil, id_opcion)
SELECT p.id, o.id
FROM perfiles p
JOIN opciones o ON o.clave IN ('productos_ver', 'productos_crear',
                               'productos_editar', 'productos_deshabilitar')
WHERE EXISTS (SELECT 1 FROM perfil_opciones po JOIN opciones o2 ON o2.id = po.id_opcion
               WHERE po.id_perfil = p.id
                 AND o2.clave IN ('jMenu_productos_principal', 'btn_productos'))
  AND NOT EXISTS (SELECT 1 FROM perfil_opciones po
                   WHERE po.id_perfil = p.id AND po.id_opcion = o.id);

INSERT INTO perfil_opciones (id_perfil, id_opcion)
SELECT p.id, o.id
FROM perfiles p
JOIN opciones o ON o.clave IN ('ingresos_mercancia_ver', 'ingresos_mercancia_crear',
                               'ingresos_mercancia_editar')
WHERE EXISTS (SELECT 1 FROM perfil_opciones po JOIN opciones o2 ON o2.id = po.id_opcion
               WHERE po.id_perfil = p.id
                 AND o2.clave IN ('jMenu_productos_principal', 'btn_ingreso_productos'))
  AND NOT EXISTS (SELECT 1 FROM perfil_opciones po
                   WHERE po.id_perfil = p.id AND po.id_opcion = o.id);

INSERT INTO perfil_opciones (id_perfil, id_opcion)
SELECT p.id, o.id
FROM perfiles p
JOIN opciones o ON o.clave IN ('traslados_ver', 'traslados_crear', 'traslados_editar',
                               'ajustes_inventario_ver', 'ajustes_inventario_crear')
WHERE EXISTS (SELECT 1 FROM perfil_opciones po JOIN opciones o2 ON o2.id = po.id_opcion
               WHERE po.id_perfil = p.id
                 AND o2.clave = 'jMenu_productos_principal')
  AND NOT EXISTS (SELECT 1 FROM perfil_opciones po
                   WHERE po.id_perfil = p.id AND po.id_opcion = o.id);

-- La consulta de existencias es de solo lectura y su boton del escritorio
-- ("Consultar") nunca estuvo restringido: se concede a todos los perfiles.
INSERT INTO perfil_opciones (id_perfil, id_opcion)
SELECT p.id, o.id
FROM perfiles p
JOIN opciones o ON o.clave = 'productos_consulta'
WHERE NOT EXISTS (SELECT 1 FROM perfil_opciones po
                   WHERE po.id_perfil = p.id AND po.id_opcion = o.id);

COMMIT;



-- #############################################################################
-- ##  PARTE C   PASOS QUE REQUIEREN DECISION                                 ##
-- ##  Fuera de transaccion y TODOS COMENTADOS. Descomenta y ejecuta a mano   ##
-- ##  solo los que apliquen a esta instalacion.                              ##
-- #############################################################################
--
-- Las decisiones de montaje de una empresa nueva (modo de precios, modulos a
-- apagar, descuentos escalonados, escala de comisiones de cartera, dedup de
-- contactos y limpiezas viejas) estan en sql/controlbodega.sql y en la PARTE C
-- de sql/historico/update_controlbodega_20260829.sql.

-- -----------------------------------------------------------------------------
-- C.1 ACCIONES DESTRUCTIVAS DEL MENU PRODUCTOS (ver B.1)
--     Las cinco acciones que reversan stock quedaron cerradas: solo el Admin
--     (perfil 1) las tiene. Normalmente se reparten desde la pantalla de
--     permisos, por usuario. Para abrirlas a un perfil completo, descomenta la
--     linea que corresponda y ajusta el id del perfil:
--
--     INSERT INTO perfil_opciones (id_perfil, id_opcion)
--     SELECT 5, o.id FROM opciones o
--      WHERE o.clave IN ('productos_eliminar', 'ingresos_mercancia_eliminar',
--                        'ingresos_mercancia_recibir', 'traslados_eliminar',
--                        'ajustes_inventario_anular')
--        AND NOT EXISTS (SELECT 1 FROM perfil_opciones po
--                         WHERE po.id_perfil = 5 AND po.id_opcion = o.id);



-- #############################################################################
-- ##  PARTE D   VERIFICACION (solo lectura)                                  ##
-- #############################################################################

-- D.1 Lo que garantiza este archivo. Las 2 filas deben salir en t.
SELECT 'opciones del menu Productos (21)' AS objeto,
       (SELECT count(*) FROM opciones WHERE modulo = 'Productos') = 21 AS ok
UNION ALL
SELECT 'linea base + este archivo (83 opciones)',
       (SELECT count(*) FROM opciones) >= 83;

-- D.2 Reparto de las opciones nuevas por perfil. Los cinco permisos
--     destructivos NO deben aparecer aca (solo el Admin los tiene, por
--     codigo), salvo que se hayan concedido a proposito desde C.1.
SELECT p.perfil,
       string_agg(o.clave, ', ' ORDER BY o.orden) AS opciones_nuevas
  FROM perfiles p
  JOIN perfil_opciones po ON po.id_perfil = p.id
  JOIN opciones o         ON o.id = po.id_opcion
 WHERE o.modulo = 'Productos' AND o.orden >= 100
 GROUP BY p.id, p.perfil
 ORDER BY p.id;

-- D.3 Los datos del negocio no se tocaron: estos conteos deben ser IDENTICOS
--     a los de antes de correr el script.
SELECT 'productos' AS tabla, count(*) FROM productos
UNION ALL SELECT 'contactos',              count(*) FROM contactos
UNION ALL SELECT 'users',                  count(*) FROM users
UNION ALL SELECT 'facturas_cabeceras',     count(*) FROM facturas_cabeceras
UNION ALL SELECT 'movimientos_inventario', count(*) FROM movimientos_inventario;

-- D.4 Permisos por usuario concedidos en esta base (el script no los toca).
-- SELECT u.user_name, o.clave, uo.concedido
--   FROM usuario_opciones uo
--   JOIN users u    ON u.id = uo.id_user
--   JOIN opciones o ON o.id = uo.id_opcion
--  ORDER BY 1, 2;
