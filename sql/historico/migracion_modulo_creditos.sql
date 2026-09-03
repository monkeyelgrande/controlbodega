-- =============================================================================
-- MODULO CREDITOS (importado de control_creditos)
-- Incremento sobre las migraciones de permisos y modulos (deben estar
-- aplicadas). Idempotente (re-ejecutable). Compatible PostgreSQL 9.4.
--
-- Tablas nuevas (solo del modulo; ninguna toca inventarios ni ordenes):
--   creditos          el credito otorgado (en control_creditos se llamaba
--                     "facturas"; se renombra para no confundir con
--                     facturas_cabeceras de bodega)
--   abonos_cabeceras  el pago como evento (total pagado + soportes foto/pdf)
--   abonos            detalle: aplicacion de un pago a un credito
--   tipos_abonos      catalogo de tipos de pago (con color y flag anticipo)
--   cuentas           catalogo de cuentas destino del credito
--
-- Tablas compartidas con bodega (se EXTIENDEN, no se duplican):
--   contactos         + cupo, interes, empleado, antiguo (campos del cliente
--                     de credito; nullables, invisibles para el resto)
--   configuraciones   + ruta_imagenes (carpeta de fotos/PDF de soportes)
--   users             se reutiliza tal cual (login unico)
--
-- Interruptor comercial: fila 'Creditos' en modulos. Para un cliente que no
-- licencia el modulo:
--   UPDATE modulos SET activo = false WHERE clave = 'Creditos';
--
-- Datos: esta migracion solo crea estructura. El traslado de los datos del
-- control_creditos en produccion sera un script aparte cuando se defina el
-- corte con el cliente.
-- =============================================================================

BEGIN;

-- 1. Catalogos ---------------------------------------------------------------

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

-- 2. Campos de credito en tablas compartidas (PG 9.4: sin IF NOT EXISTS
--    para ADD COLUMN, se verifica a mano) -----------------------------------

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

-- 3. Creditos y abonos --------------------------------------------------------

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

-- Modelo cabecera + detalle:
--   abonos_cabeceras : el pago como evento (guarda el TOTAL pagado y soportes)
--   abonos (detalle) : cada aplicacion del pago a un credito
--   saldo a favor    : implicito = cabecera.total - SUM(detalle) (no se guarda)
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

-- 4. Opciones de permisos del modulo (modulo = 'Creditos' = modulos.clave).
--    No se conceden a ningun perfil: el Admin siempre puede y decide desde la
--    pantalla de permisos quien mas entra (es cartera: mejor cerrado por
--    defecto).
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

-- 5. Interruptor comercial. Se siembra ENCENDIDO aqui (instalacion de
--    desarrollo); en la instalacion de cada cliente se deja segun lo
--    licenciado. Re-ejecutar nunca pisa el valor elegido.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM modulos WHERE clave = 'Creditos') THEN
        INSERT INTO modulos (clave, nombre, activo)
        VALUES ('Creditos', 'Creditos (cartera y abonos)', true);
    END IF;
END $$;

-- 6. Catalogo minimo de tipos de abono. Sin al menos una fila el combo de
--    "tipo de abono" de las pantallas de pago queda vacio y no se puede
--    registrar ningun abono. Solo se siembra si la tabla esta vacia: nunca
--    pisa el catalogo que el cliente ya haya definido.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM tipos_abonos) THEN
        INSERT INTO tipos_abonos (id, nombre, color, anticipo) VALUES
            (1, 'EFECTIVO',      'VERDE',   0),
            (2, 'TRANSFERENCIA', 'AZUL',    0),
            (3, 'CHEQUE',        'AMARILLO', 0),
            (4, 'ANTICIPO',      'NARANJA', 1);
        PERFORM setval('tipos_abonos_id_seq', (SELECT MAX(id) FROM tipos_abonos));
    END IF;
END $$;

COMMIT;
