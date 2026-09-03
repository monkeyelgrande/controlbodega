-- =============================================================================
-- CAJA DOS: segunda caja independiente sobre el mismo modulo Caja.
-- Incremento sobre sql/migracion_modulo_caja.sql (y opcionalmente
-- sql/migracion_auditoria_caja.sql). Idempotente (re-ejecutable).
-- Compatible PostgreSQL 9.4.
--
-- IDEA
--   El modulo Caja pasa de tener UNA caja a soportar N cajas con los MISMOS
--   formularios y el MISMO codigo. Una columna id_caja marca a que caja
--   pertenece cada fondo, cuenta, ingreso, egreso y traslado:
--       id_caja = 1  -> "Caja"      (la de siempre; datos historicos)
--       id_caja = 2  -> "Caja Dos"  (nueva, arranca de cero)
--   Cada caja tiene sus propios fondos, cuentas, movimientos, saldos y
--   reportes. Los contactos (clientes/proveedores) y las configuraciones se
--   comparten a proposito: son los mismos del negocio.
--
--   El menu "Caja Dos" abre los mismos formularios que "Caja" pero pasandoles
--   idCaja = 2; toda consulta a estas tablas filtra por id_caja.
--
-- PERMISOS
--   Caja Dos es un modulo licenciable propio ('Caja Dos') con sus 8 opciones,
--   independientes de las de 'Caja'. Asi se puede dar acceso a una caja y no a
--   la otra. Cerrado por defecto (es dinero): solo Admin la ve hasta asignar.
-- =============================================================================

BEGIN;

-- 1. Columna id_caja en las tablas de dinero y catalogos --------------------
--    NOT NULL DEFAULT 1: las filas existentes quedan en la Caja 1
--    automaticamente; los INSERT nuevos que no la indiquen tambien.
--    (PG 9.4 no soporta ADD COLUMN IF NOT EXISTS: se verifica a mano.)

DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['fondos', 'cuentas_ingresos', 'cuentas_egresos',
                             'ingresos', 'egresos', 'transferencias'] LOOP
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = t AND column_name = 'id_caja') THEN
            EXECUTE format('ALTER TABLE %I ADD COLUMN id_caja integer NOT NULL DEFAULT 1', t);
        END IF;
    END LOOP;
END $$;

-- Indices por caja: casi todas las consultas del modulo filtran por id_caja.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_fondos_caja') THEN
        CREATE INDEX idx_fondos_caja ON fondos(id_caja);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_cuentas_ingresos_caja') THEN
        CREATE INDEX idx_cuentas_ingresos_caja ON cuentas_ingresos(id_caja);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_cuentas_egresos_caja') THEN
        CREATE INDEX idx_cuentas_egresos_caja ON cuentas_egresos(id_caja);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_ingresos_caja') THEN
        CREATE INDEX idx_ingresos_caja ON ingresos(id_caja);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_egresos_caja') THEN
        CREATE INDEX idx_egresos_caja ON egresos(id_caja);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_transferencias_caja') THEN
        CREATE INDEX idx_transferencias_caja ON transferencias(id_caja);
    END IF;
END $$;

-- 2. Semilla minima de Caja Dos ---------------------------------------------
--    Para que Caja Dos funcione de una vez (crear ingresos/egresos y traslados
--    necesita al menos un fondo y una cuenta predeterminada de cada tipo).
--    El usuario luego crea/renombra el resto desde las pantallas de Caja Dos.
--    Solo se siembra si la caja 2 aun no tiene NADA de ese catalogo.

INSERT INTO fondos (nombre, predeterminado, fisico_digital, id_caja)
SELECT 'Caja general', 1, 0, 2
WHERE NOT EXISTS (SELECT 1 FROM fondos WHERE id_caja = 2);

INSERT INTO cuentas_ingresos (nombre, predeterminado, id_caja)
SELECT 'Ingreso general', 1, 2
WHERE NOT EXISTS (SELECT 1 FROM cuentas_ingresos WHERE id_caja = 2);

INSERT INTO cuentas_egresos (nombre, predeterminado, id_caja)
SELECT 'Egreso general', 1, 2
WHERE NOT EXISTS (SELECT 1 FROM cuentas_egresos WHERE id_caja = 2);

-- 3. Modulo licenciable propio de Caja Dos ----------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM modulos WHERE clave = 'Caja Dos') THEN
        INSERT INTO modulos (clave, nombre, activo)
        VALUES ('Caja Dos', 'Caja Dos (segunda caja independiente)', true);
    END IF;
END $$;

-- 4. Opciones de permiso de Caja Dos (modulo = 'Caja Dos').
--    Mismas 8 opciones que 'Caja' pero con clave/componente propios. No se
--    conceden a ningun perfil: el Admin decide desde la pantalla de permisos.
INSERT INTO opciones (clave, nombre, modulo, componente, orden)
SELECT v.clave, v.nombre, v.modulo, v.componente, v.orden
FROM (
    VALUES
    ('menu_caja2',             'Menu Caja Dos (completo)',   'Caja Dos', 'menuCaja2',             10),
    ('caja2_ingresos',         'Ingresos de dinero',         'Caja Dos', 'itemCaja2Ingresos',     20),
    ('caja2_egresos',          'Egresos de dinero',          'Caja Dos', 'itemCaja2Egresos',      30),
    ('caja2_traslados',        'Traslados entre fondos',     'Caja Dos', 'itemCaja2Traslados',    40),
    ('caja2_fondos',           'Fondos (cajas y bancos)',    'Caja Dos', 'itemCaja2Fondos',       50),
    ('caja2_cuentas_ingresos', 'Cuentas de ingresos',        'Caja Dos', 'itemCaja2CtasIngresos', 60),
    ('caja2_cuentas_egresos',  'Cuentas de egresos',         'Caja Dos', 'itemCaja2CtasEgresos',  70),
    ('caja2_reportes',         'Reportes de caja',           'Caja Dos', 'itemCaja2Reportes',     80)
) AS v (clave, nombre, modulo, componente, orden)
WHERE NOT EXISTS (SELECT 1 FROM opciones o WHERE o.clave = v.clave);

COMMIT;
