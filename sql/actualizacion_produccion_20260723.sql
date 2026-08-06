-- =============================================================================
-- ACTUALIZACION DE PRODUCCION  ->  modulos Creditos y Caja (+ tabla modulos)
-- Objetivo:  llevar una copia de produccion (p.ej. bodega_nuevo2) al dia con
--            los cambios de estructura mas recientes, SIN TOCAR NINGUN DATO
--            EXISTENTE.
--
-- Generado: 2026-07-23   |   Motor destino: PostgreSQL 9.4
--
-- QUE HACE (todo aditivo e idempotente / re-ejecutable):
--   * Crea 13 tablas nuevas:
--       modulos
--       creditos, abonos_cabeceras, abonos, tipos_abonos, cuentas   (mod. Creditos)
--       fondos, cuentas_ingresos, cuentas_egresos, ingresos, egresos,
--       transferencias, fotos_registros                             (mod. Caja)
--   * Agrega 8 columnas NUEVAS y NULLABLES a tablas compartidas:
--       contactos       + cupo, interes, empleado, antiguo, predeterminado
--       configuraciones + ruta_imagenes, tipo_impresora, ingreso_dinero
--   * Siembra filas de CONFIGURACION (no datos de negocio) con guardas
--     "WHERE NOT EXISTS" / "IF NOT EXISTS":
--       - catalogo de modulos (Compras, Precios, Creditos, Caja)
--       - opciones de permisos de los menus Creditos y Caja
--
-- QUE NO HACE (garantia de "no tocar datos existentes"):
--   * NO ejecuta ningun UPDATE ni DELETE sobre filas existentes.
--   * NO reescribe filas: las columnas nuevas son NULLABLES y sin DEFAULT que
--     obligue a backfill, asi que las filas actuales de contactos/configuraciones
--     quedan intactas (el valor nuevo queda en NULL).
--   * NO incluye los scripts de datos (dedup_contactos_*, reparar_entregas_*,
--     carga_inicial_*): esos si modifican filas y quedan deliberadamente fuera.
--   * Si una tabla/columna/fila ya existe, se omite (por eso es re-ejecutable).
--
-- TODO EN UNA SOLA TRANSACCION: si algo falla, no se aplica nada (rollback).
--
-- VALIDACION PREVIA:
--   Aplicado y verificado el 2026-07-24 sobre bodega_nuevo2, una copia exacta
--   de los datos de produccion (5082 contactos / 29 users). Resultado: 13/13
--   tablas creadas, 8/8 columnas agregadas, 0 filas existentes modificadas.
--
-- COMO EJECUTAR EN PRODUCCION:
--   El nombre de la base y el servidor son los que usa la instalacion; se leen
--   de los archivos  src\database_name.txt  y  src\ip.txt  junto al ejecutable.
--
--   "C:\Program Files\PostgreSQL\9.4\bin\psql.exe" -h <IP> -p 5432 ^
--       -U postgres -d <BASE> -v ON_ERROR_STOP=1 ^
--       -f sql\actualizacion_produccion_20260723.sql
--
--   ON_ERROR_STOP=1 es importante: aborta al primer error.
--   >> HAZ UN BACKUP (pg_dump) DE LA BASE ANTES DE CORRERLO. <<
--   >> CORRELO CON LOS USUARIOS FUERA DE LA APP (crea tablas y toma locks). <<
--
-- Al terminar imprime, via RAISE NOTICE, los conteos ANTES de las tablas
-- compartidas para que verifiques que no cambiaron. La verificacion posterior
-- (solo lectura) esta al final de este archivo, fuera de la transaccion.
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

-- -----------------------------------------------------------------------------
-- 0. PRE-VUELO: confirmar que es la base correcta y dejar traza de los conteos
--    actuales (para comparar despues). Aborta si faltan tablas compartidas
--    (base equivocada o migracion de permisos no aplicada).
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    faltantes text;
    n_contactos       bigint;
    n_configuraciones bigint;
    n_opciones        bigint;
    n_users           bigint;
BEGIN
    SELECT string_agg(t, ', ')
      INTO faltantes
      FROM (VALUES ('opciones'),('contactos'),('configuraciones'),('users')) v(t)
     WHERE NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'public' AND table_name = v.t);

    IF faltantes IS NOT NULL THEN
        RAISE EXCEPTION
          'Base incorrecta o incompleta: faltan tablas compartidas (%). Abortando.',
          faltantes;
    END IF;

    SELECT count(*) INTO n_contactos       FROM contactos;
    SELECT count(*) INTO n_configuraciones FROM configuraciones;
    SELECT count(*) INTO n_opciones        FROM opciones;
    SELECT count(*) INTO n_users           FROM users;

    RAISE NOTICE '--- PRE-VUELO (conteos actuales, no deben cambiar salvo opciones) ---';
    RAISE NOTICE 'contactos       = % filas (se conservan; solo se AGREGAN columnas)', n_contactos;
    RAISE NOTICE 'configuraciones = % filas (se conservan; solo se AGREGAN columnas)', n_configuraciones;
    RAISE NOTICE 'users           = % filas (intactas)', n_users;
    RAISE NOTICE 'opciones        = % filas (se AGREGAN ~14 de config Creditos/Caja)', n_opciones;
END $$;


-- =============================================================================
-- 1. TABLA modulos  (interruptor comercial por instalacion)
-- =============================================================================
CREATE TABLE IF NOT EXISTS modulos (
    id     serial       PRIMARY KEY,
    clave  varchar(50)  NOT NULL UNIQUE,
    nombre varchar(100) NOT NULL,
    activo boolean      NOT NULL DEFAULT true
);

-- Siembra de los modulos opcionales existentes, ENCENDIDOS. Re-ejecutar nunca
-- pisa el 'activo' elegido por instalacion. Ajusta con:
--   UPDATE modulos SET activo = false WHERE clave = '<Modulo>';
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM modulos WHERE clave = 'Compras') THEN
        INSERT INTO modulos (clave, nombre, activo)
        VALUES ('Compras', 'Compras (pipeline de ordenes de compra)', true);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM modulos WHERE clave = 'Precios') THEN
        INSERT INTO modulos (clave, nombre, activo)
        VALUES ('Precios', 'Precios (agroinsumos y comisiones)', true);
    END IF;
END $$;


-- =============================================================================
-- 2. MODULO CREDITOS  (cartera y abonos)
-- =============================================================================

-- 2.1 Catalogos
CREATE TABLE IF NOT EXISTS cuentas (
    id serial NOT NULL,
    nombre character varying,
    CONSTRAINT cuentas_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS tipos_abonos (
    id serial NOT NULL,
    nombre character varying,
    color character varying,
    anticipo integer,
    CONSTRAINT tipos_abonos_pkey PRIMARY KEY (id)
);

-- 2.2 Columnas nuevas en tablas compartidas (PG 9.4: verificar a mano)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'contactos' AND column_name = 'cupo') THEN
        ALTER TABLE contactos ADD COLUMN cupo double precision;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'contactos' AND column_name = 'interes') THEN
        ALTER TABLE contactos ADD COLUMN interes double precision;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'contactos' AND column_name = 'empleado') THEN
        ALTER TABLE contactos ADD COLUMN empleado integer;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'contactos' AND column_name = 'antiguo') THEN
        ALTER TABLE contactos ADD COLUMN antiguo integer;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'configuraciones' AND column_name = 'ruta_imagenes') THEN
        ALTER TABLE configuraciones ADD COLUMN ruta_imagenes character varying;
    END IF;
END $$;

-- 2.3 Creditos y abonos
CREATE TABLE IF NOT EXISTS creditos (
    id serial NOT NULL,
    codigo character varying,
    id_contacto integer,
    id_user integer,
    total double precision,
    fecha_creacion date,
    fecha_vencimiento date,
    estado integer,
    interes double precision,
    hora character varying(8),
    descripcion character varying,
    monto_descuento double precision,
    id_cuenta integer,
    foto character varying,
    pdf character varying,
    CONSTRAINT pk_creditos PRIMARY KEY (id),
    CONSTRAINT fk_creditos_user FOREIGN KEY (id_user)
        REFERENCES users (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_creditos_contacto FOREIGN KEY (id_contacto)
        REFERENCES contactos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_creditos_cuenta FOREIGN KEY (id_cuenta)
        REFERENCES cuentas (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS abonos_cabeceras (
    id serial PRIMARY KEY,
    id_contacto integer NOT NULL REFERENCES contactos (id),
    id_user integer REFERENCES users (id),
    id_tipo_abono integer REFERENCES tipos_abonos (id),
    total double precision NOT NULL,
    fecha date NOT NULL,
    hora character varying DEFAULT '',
    observacion character varying DEFAULT '',
    foto character varying DEFAULT '',
    pdf character varying DEFAULT ''
);

CREATE TABLE IF NOT EXISTS abonos (
    id serial PRIMARY KEY,
    id_cabecera integer NOT NULL REFERENCES abonos_cabeceras (id) ON DELETE CASCADE,
    id_credito integer NOT NULL REFERENCES creditos (id),
    abono double precision NOT NULL,
    fecha date NOT NULL,
    hora character varying DEFAULT ''
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_abonos_id_cabecera') THEN
        CREATE INDEX idx_abonos_id_cabecera ON abonos(id_cabecera);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_abonos_id_credito') THEN
        CREATE INDEX idx_abonos_id_credito ON abonos(id_credito);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_abonos_cabeceras_contacto') THEN
        CREATE INDEX idx_abonos_cabeceras_contacto ON abonos_cabeceras(id_contacto);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_abonos_cabeceras_fecha') THEN
        CREATE INDEX idx_abonos_cabeceras_fecha ON abonos_cabeceras(fecha);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_creditos_contacto') THEN
        CREATE INDEX idx_creditos_contacto ON creditos(id_contacto);
    END IF;
END $$;

-- 2.4 Opciones de permisos del modulo Creditos (guarda WHERE NOT EXISTS)
INSERT INTO opciones (clave, nombre, modulo, componente, orden)
SELECT v.clave, v.nombre, v.modulo, v.componente, v.orden
FROM (
    VALUES
    ('menu_creditos',         'Menu Creditos (completo)', 'Creditos', 'menuCreditos',        10),
    ('creditos_ver',          'Creditos (cartera)',       'Creditos', 'itemCreditosVer',     20),
    ('creditos_clientes',     'Clientes de credito',      'Creditos', 'itemCreditosClientes', 30),
    ('creditos_cuentas',      'Cuentas',                  'Creditos', 'itemCreditosCuentas', 40),
    ('creditos_tipos_abonos', 'Tipos de abonos',          'Creditos', 'itemCreditosTipos',   50),
    ('creditos_reportes',     'Reportes de creditos',     'Creditos', 'itemCreditosReportes', 60)
) AS v (clave, nombre, modulo, componente, orden)
WHERE NOT EXISTS (SELECT 1 FROM opciones o WHERE o.clave = v.clave);

-- 2.5 Interruptor comercial Creditos (encendido; ajustable por cliente)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM modulos WHERE clave = 'Creditos') THEN
        INSERT INTO modulos (clave, nombre, activo)
        VALUES ('Creditos', 'Creditos (cartera y abonos)', true);
    END IF;
END $$;


-- =============================================================================
-- 3. MODULO CAJA  (ingresos, egresos, fondos y transferencias)
-- =============================================================================

-- 3.1 Catalogos
CREATE TABLE IF NOT EXISTS fondos (
    id serial NOT NULL,
    nombre character varying(100),
    predeterminado integer,
    fisico_digital integer,          -- 0=fisico (efectivo), 1=digital (banco)
    CONSTRAINT pk_fondos PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS cuentas_ingresos (
    id serial NOT NULL,
    nombre character varying(50),
    predeterminado integer,
    CONSTRAINT pk_cuentas_ingresos PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS cuentas_egresos (
    id serial NOT NULL,
    nombre character varying(50),
    predeterminado integer,
    CONSTRAINT pk_cuentas_egresos PRIMARY KEY (id)
);

-- 3.2 Columnas nuevas en tablas compartidas
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'contactos' AND column_name = 'predeterminado') THEN
        ALTER TABLE contactos ADD COLUMN predeterminado integer;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'configuraciones' AND column_name = 'tipo_impresora') THEN
        ALTER TABLE configuraciones ADD COLUMN tipo_impresora character varying;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'configuraciones' AND column_name = 'ingreso_dinero') THEN
        ALTER TABLE configuraciones ADD COLUMN ingreso_dinero integer;
    END IF;
END $$;

-- 3.3 Ingresos, egresos y transferencias
CREATE TABLE IF NOT EXISTS ingresos (
    id serial NOT NULL,
    id_user integer,
    id_cuenta integer NOT NULL,
    id_cliente integer,
    id_fondo integer,
    descripcion character varying,
    total double precision,
    fecha date,
    hora character varying(8),
    factura_remision integer,
    transferencia integer,
    CONSTRAINT pk_caja_ingresos PRIMARY KEY (id),
    CONSTRAINT fk_ingreso_cuenta FOREIGN KEY (id_cuenta)
        REFERENCES cuentas_ingresos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_ingreso_cliente FOREIGN KEY (id_cliente)
        REFERENCES contactos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_ingreso_fondo FOREIGN KEY (id_fondo)
        REFERENCES fondos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_ingreso_user FOREIGN KEY (id_user)
        REFERENCES users (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS egresos (
    id serial NOT NULL,
    id_user integer,
    id_cuenta integer NOT NULL,
    id_cliente integer,
    id_fondo integer,
    descripcion character varying,
    total double precision,
    fecha date,
    hora character varying(8),
    factura_remision integer,
    transferencia integer,
    CONSTRAINT pk_egresos PRIMARY KEY (id),
    CONSTRAINT fk_egreso_cuenta FOREIGN KEY (id_cuenta)
        REFERENCES cuentas_egresos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_egreso_cliente FOREIGN KEY (id_cliente)
        REFERENCES contactos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_egreso_fondo FOREIGN KEY (id_fondo)
        REFERENCES fondos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_egreso_user FOREIGN KEY (id_user)
        REFERENCES users (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS transferencias (
    id serial NOT NULL,
    id_user integer,
    id_fondo_origen integer,
    id_fondo_destino integer,
    descripcion character varying,
    total double precision,
    fecha date,
    hora character varying(8),
    id_ingreso integer,              -- ingreso generado en el fondo destino
    id_egreso integer,               -- egreso generado en el fondo origen
    CONSTRAINT pk_transferencias PRIMARY KEY (id),
    CONSTRAINT fk_transferencia_fondo_origen FOREIGN KEY (id_fondo_origen)
        REFERENCES fondos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_transferencia_fondo_destino FOREIGN KEY (id_fondo_destino)
        REFERENCES fondos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_transferencia_ingreso FOREIGN KEY (id_ingreso)
        REFERENCES ingresos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_transferencia_egreso FOREIGN KEY (id_egreso)
        REFERENCES egresos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_transferencia_user FOREIGN KEY (id_user)
        REFERENCES users (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- tipo_registro: 1=ingreso, 2=egreso (id_registro apunta a esa tabla)
CREATE TABLE IF NOT EXISTS fotos_registros (
    id serial NOT NULL,
    nombre character varying,
    id_registro integer,
    tipo_registro integer,
    CONSTRAINT pk_fotos_registros PRIMARY KEY (id)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_ingresos_fecha') THEN
        CREATE INDEX idx_ingresos_fecha ON ingresos(fecha);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_ingresos_fondo') THEN
        CREATE INDEX idx_ingresos_fondo ON ingresos(id_fondo);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_ingresos_cliente') THEN
        CREATE INDEX idx_ingresos_cliente ON ingresos(id_cliente);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_egresos_fecha') THEN
        CREATE INDEX idx_egresos_fecha ON egresos(fecha);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_egresos_fondo') THEN
        CREATE INDEX idx_egresos_fondo ON egresos(id_fondo);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_egresos_cliente') THEN
        CREATE INDEX idx_egresos_cliente ON egresos(id_cliente);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_transferencias_fecha') THEN
        CREATE INDEX idx_transferencias_fecha ON transferencias(fecha);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_fotos_registros_registro') THEN
        CREATE INDEX idx_fotos_registros_registro ON fotos_registros(id_registro, tipo_registro);
    END IF;
END $$;

-- 3.4 Opciones de permisos del modulo Caja (guarda WHERE NOT EXISTS)
INSERT INTO opciones (clave, nombre, modulo, componente, orden)
SELECT v.clave, v.nombre, v.modulo, v.componente, v.orden
FROM (
    VALUES
    ('menu_caja',            'Menu Caja (completo)',       'Caja', 'menuCaja',             10),
    ('caja_ingresos',        'Ingresos de dinero',         'Caja', 'itemCajaIngresos',     20),
    ('caja_egresos',         'Egresos de dinero',          'Caja', 'itemCajaEgresos',      30),
    ('caja_traslados',       'Traslados entre fondos',     'Caja', 'itemCajaTraslados',    40),
    ('caja_fondos',          'Fondos (cajas y bancos)',    'Caja', 'itemCajaFondos',       50),
    ('caja_cuentas_ingresos','Cuentas de ingresos',        'Caja', 'itemCajaCtasIngresos', 60),
    ('caja_cuentas_egresos', 'Cuentas de egresos',         'Caja', 'itemCajaCtasEgresos',  70),
    ('caja_reportes',        'Reportes de caja',           'Caja', 'itemCajaReportes',     80)
) AS v (clave, nombre, modulo, componente, orden)
WHERE NOT EXISTS (SELECT 1 FROM opciones o WHERE o.clave = v.clave);

-- 3.5 Interruptor comercial Caja (encendido; ajustable por cliente)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM modulos WHERE clave = 'Caja') THEN
        INSERT INTO modulos (clave, nombre, activo)
        VALUES ('Caja', 'Caja (ingresos, egresos y fondos)', true);
    END IF;
END $$;


-- -----------------------------------------------------------------------------
-- 4. RESUMEN POST (dentro de la transaccion, antes del COMMIT)
-- -----------------------------------------------------------------------------
DO $$
DECLARE
    n_tablas_nuevas int;
BEGIN
    SELECT count(*) INTO n_tablas_nuevas
      FROM information_schema.tables
     WHERE table_schema = 'public'
       AND table_name IN ('modulos','creditos','abonos_cabeceras','abonos',
             'tipos_abonos','cuentas','fondos','cuentas_ingresos',
             'cuentas_egresos','ingresos','egresos','transferencias',
             'fotos_registros');
    RAISE NOTICE '--- POST: % de 13 tablas nuevas presentes ---', n_tablas_nuevas;
    RAISE NOTICE 'contactos=% configuraciones=% opciones=% (opciones subio por las de Creditos/Caja)',
        (SELECT count(*) FROM contactos),
        (SELECT count(*) FROM configuraciones),
        (SELECT count(*) FROM opciones);
END $$;

COMMIT;

-- =============================================================================
-- VERIFICACION (SOLO LECTURA) -- correr aparte despues del COMMIT si se desea.
-- Nada de esto modifica datos.
-- =============================================================================
--
-- -- a) Las 13 tablas nuevas deben existir y estar VACIAS (recien creadas):
-- SELECT relname AS tabla, n_live_tup AS filas
--   FROM pg_stat_user_tables
--  WHERE relname IN ('modulos','creditos','abonos_cabeceras','abonos',
--        'tipos_abonos','cuentas','fondos','cuentas_ingresos','cuentas_egresos',
--        'ingresos','egresos','transferencias','fotos_registros')
--  ORDER BY relname;
--
-- -- b) Las 8 columnas nuevas deben existir (y estar en NULL en las filas viejas):
-- SELECT table_name, column_name
--   FROM information_schema.columns
--  WHERE (table_name='contactos'       AND column_name IN ('cupo','interes','empleado','antiguo','predeterminado'))
--     OR (table_name='configuraciones' AND column_name IN ('ruta_imagenes','tipo_impresora','ingreso_dinero'))
--  ORDER BY table_name, column_name;
--
-- -- c) Catalogo de modulos sembrado:
-- SELECT clave, nombre, activo FROM modulos ORDER BY id;
--
-- -- d) Confirmar que NO se toco ningun dato: los conteos de las tablas
-- --    existentes deben ser IGUALES a los de antes de correr el script
-- --    (opciones es la unica que sube, por filas de configuracion de permisos):
-- SELECT 'contactos' t, count(*) FROM contactos
-- UNION ALL SELECT 'configuraciones', count(*) FROM configuraciones
-- UNION ALL SELECT 'users', count(*) FROM users
-- UNION ALL SELECT 'opciones', count(*) FROM opciones;
-- =============================================================================

-- =============================================================================
-- SIGUIENTE PASO OBLIGATORIO EN PRODUCCION (posterior a este script):
--
--   psql -h <host> -U postgres -d <base> -f sql/migracion_auditoria_caja.sql
--
-- Crea la auditoria del modulo Caja (auditoria_caja + auditoria_caja_campos y
-- los triggers sobre ingresos, egresos y transferencias). Es aditiva e
-- idempotente igual que este script, pero necesita que las tablas de Caja ya
-- existan, por eso va DESPUES.
-- =============================================================================
