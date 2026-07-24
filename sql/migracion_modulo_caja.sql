-- =============================================================================
-- MODULO CAJA (importado de cajadiaria / ContaMonkey V2.0, rediseñado)
-- Incremento sobre las migraciones de permisos y modulos (deben estar
-- aplicadas). Idempotente (re-ejecutable). Compatible PostgreSQL 9.4.
--
-- Rediseño acordado respecto a cajadiaria:
--   * SIN abonos_ingresos / abonos_egresos: un ingreso o egreso tiene UN solo
--     pago, asi que id_fondo va directo en la tabla ingresos / egresos.
--   * SIN base_diaria y SIN cajas: los historicos de dinero se derivan por
--     consulta (saldo de fondo = SUM(ingresos) - SUM(egresos) por fondo).
--   * IDs por serial (cajadiaria insertaba MAX(id)+1 a mano).
--   * Sin migracion de datos: todo arranca de cero.
--
-- Tablas nuevas (ninguna toca inventarios ni ordenes):
--   fondos            a donde entra/sale el dinero (caja fisica o digital).
--                     Distinto de "cuentas" del modulo Creditos: se mantienen
--                     separados a proposito.
--   cuentas_ingresos  catalogo de conceptos de ingreso
--   cuentas_egresos   catalogo de conceptos de egreso
--   ingresos          documento de entrada de dinero (con id_fondo directo)
--   egresos           documento de salida de dinero (con id_fondo directo)
--   transferencias    traslado entre fondos; genera un par egreso (origen) +
--                     ingreso (destino) marcados con transferencia=1
--   fotos_registros   soportes fotograficos de ingresos/egresos
--
-- Nota de nombres: en bodega "ingresos" de mercancia vive en
-- ingresos_mercancias_cabecera/detalle; la tabla "ingresos" estaba libre y
-- aqui significa ingreso de DINERO.
--
-- Tablas compartidas con bodega (se EXTIENDEN, no se duplican):
--   contactos         + predeterminado (contacto por defecto en ingresos)
--   configuraciones   + tipo_impresora, ingreso_dinero
--   users             se reutiliza tal cual (login unico)
--
-- Interruptor comercial: fila 'Caja' en modulos. Para un cliente que no
-- licencia el modulo:
--   UPDATE modulos SET activo = false WHERE clave = 'Caja';
-- =============================================================================

BEGIN;

-- 1. Catalogos ---------------------------------------------------------------

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

-- 2. Campos de caja en tablas compartidas (PG 9.4: sin IF NOT EXISTS
--    para ADD COLUMN, se verifica a mano) -----------------------------------

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

-- 3. Ingresos, egresos y transferencias --------------------------------------

-- factura_remision: 1=Factura, 0=Remision (filtro de reportes)
-- transferencia:    1 cuando el registro lo genero un traslado entre fondos

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
    -- pk_ingresos ya lo usa ingresos_mercancias_cabecera; nombre distinto
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

-- 4. Opciones de permisos del modulo (modulo = 'Caja' = modulos.clave).
--    No se conceden a ningun perfil: el Admin siempre puede y decide desde la
--    pantalla de permisos quien mas entra (es dinero: cerrado por defecto).
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

-- 5. Interruptor comercial. Se siembra ENCENDIDO aqui (instalacion de
--    desarrollo); en la instalacion de cada cliente se deja segun lo
--    licenciado. Re-ejecutar nunca pisa el valor elegido.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM modulos WHERE clave = 'Caja') THEN
        INSERT INTO modulos (clave, nombre, activo)
        VALUES ('Caja', 'Caja (ingresos, egresos y fondos)', true);
    END IF;
END $$;

COMMIT;
