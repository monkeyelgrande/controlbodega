-- =============================================================================
-- FUSION productos-agroinsumos -> controlbodega (bodega_nuevo)
-- FASE 1: esquema. Idempotente (re-ejecutable). Compatible PostgreSQL 9.4:
-- sin ADD COLUMN IF NOT EXISTS ni CREATE INDEX IF NOT EXISTS -> DO-blocks.
-- =============================================================================

BEGIN;

-- 1. Columnas de precios estilo agro en productos (NO toca las existentes:
--    precio_costo, precio_venta, precio_venta2/3, cant_paquete, tipo, estado)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'productos' AND column_name = 'venta') THEN
        ALTER TABLE productos ADD COLUMN venta double precision;
        ALTER TABLE productos ADD COLUMN valor_desc_1 double precision;
        ALTER TABLE productos ADD COLUMN valor_desc_2 double precision;
        ALTER TABLE productos ADD COLUMN valor_s_y_t double precision;
        ALTER TABLE productos ADD COLUMN valor_credito double precision;
        ALTER TABLE productos ADD COLUMN iva double precision;
        ALTER TABLE productos ADD COLUMN porcentaje_utilidad double precision;
    END IF;
END $$;

-- 2. Traza de origen en contactos + ensanchar "contacto": agro guarda hasta
--    95 caracteres en ese campo y en bodega era varchar(20)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'contactos' AND column_name = 'origen') THEN
        ALTER TABLE contactos ADD COLUMN origen character varying(20);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'contactos' AND column_name = 'contacto'
                 AND character_maximum_length < 200) THEN
        ALTER TABLE contactos ALTER COLUMN contacto TYPE character varying(200);
    END IF;
END $$;

-- 3. Permiso del modulo Precios en users:
--    0=sin acceso, 2=captura cantidades (ex Bodega), 3=costos (ex Contadora),
--    4=precios (ex Precios / Admin)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'users' AND column_name = 'rol_precios') THEN
        ALTER TABLE users ADD COLUMN rol_precios integer NOT NULL DEFAULT 0;
    END IF;
END $$;

-- 4. Porcentajes de la configuracion de precios (los que el codigo vivo de
--    agro usa; las columnas porcentaje_d1_*/d2_* de agro eran codigo muerto)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'configuraciones' AND column_name = 'porcentaje_operacion') THEN
        ALTER TABLE configuraciones ADD COLUMN porcentaje_operacion double precision;
        ALTER TABLE configuraciones ADD COLUMN porcentaje_s_y_t double precision;
        ALTER TABLE configuraciones ADD COLUMN porcentaje_credito double precision;
    END IF;
END $$;

-- 5. Descuentos escalonados (estructura del descuentos VIVO de agro)
CREATE TABLE IF NOT EXISTS descuentos (
    id serial NOT NULL,
    tipo integer,
    utilidad double precision,
    descuento double precision,
    CONSTRAINT pk_descuentos_precios PRIMARY KEY (id)
);

-- 6. Historico de ingresos de agro en tablas propias (nombres libres en
--    bodega_nuevo). id_bodega es dato historico de agro: sin FK.
CREATE TABLE IF NOT EXISTS ingresos_productos_cabecera (
    id serial NOT NULL,
    no_factura character varying(100),
    id_proveedor integer,
    id_transportador integer,
    id_user integer,
    total double precision,
    fecha date,
    hora character varying(8),
    estado integer,                          -- 0=Recibido 1=Ingresado 2=Precios
    observacion character varying,
    id_bodega integer,
    fecha_vencimiento date,
    enviado_control_bodega boolean DEFAULT false,
    CONSTRAINT pk_ingresos_productos_cabecera PRIMARY KEY (id),
    CONSTRAINT fk_ipc_proveedor FOREIGN KEY (id_proveedor) REFERENCES contactos (id),
    CONSTRAINT fk_ipc_transportador FOREIGN KEY (id_transportador) REFERENCES contactos (id),
    CONSTRAINT fk_ipc_user FOREIGN KEY (id_user) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS ingresos_productos_detalle (
    id serial NOT NULL,
    id_ingreso_cabecera integer,
    id_producto integer,
    cantidad double precision,
    iva double precision,
    precio_costo double precision,
    venta double precision,
    valor_desc_1 double precision,
    valor_desc_2 double precision,
    valor_s_y_t double precision,
    valor_credito double precision,
    descuento double precision,
    porcentaje_utilidad double precision,
    desc_n_1 double precision,
    desc_n_2 double precision,
    etiquetas character varying,
    id_bodega_control integer,               -- bodega elegida por fila (rol 2) antes de enviar a bodega
    CONSTRAINT pk_ingresos_productos_detalle PRIMARY KEY (id),
    CONSTRAINT fk_ipd_cabecera FOREIGN KEY (id_ingreso_cabecera)
        REFERENCES ingresos_productos_cabecera (id) ON DELETE CASCADE,
    CONSTRAINT fk_ipd_producto FOREIGN KEY (id_producto) REFERENCES productos (id)
);

-- 6b. Si la tabla ya existia sin id_bodega_control (corridas previas del script)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'ingresos_productos_detalle'
                     AND column_name = 'id_bodega_control') THEN
        ALTER TABLE ingresos_productos_detalle ADD COLUMN id_bodega_control integer;
    END IF;
END $$;

-- 7. Pagos a proveedores de los ingresos de agro. NOMBRE NUEVO:
--    pagos_ingresos ya existe en bodega_nuevo (es de ingresos de mercancia).
CREATE TABLE IF NOT EXISTS pagos_ingresos_productos (
    id serial NOT NULL,
    id_ingreso_productos_cabecera integer,
    total double precision,
    fecha date,
    hora character varying(8),
    cod_pago character varying,
    CONSTRAINT pk_pagos_ingresos_productos PRIMARY KEY (id),
    CONSTRAINT fk_pip_cabecera FOREIGN KEY (id_ingreso_productos_cabecera)
        REFERENCES ingresos_productos_cabecera (id) ON DELETE CASCADE
);

-- 8. Tabla de mapeo de la migracion: trazabilidad, idempotencia e insumo de
--    la futura unificacion de contactos.
CREATE TABLE IF NOT EXISTS migracion_mapeo (
    tabla_origen character varying(60) NOT NULL,
    id_viejo integer NOT NULL,
    id_nuevo integer NOT NULL,
    accion character varying(20) NOT NULL,   -- 'creado' | 'fusionado' | 'mapeado'
    fecha timestamp NOT NULL DEFAULT now(),
    CONSTRAINT pk_migracion_mapeo PRIMARY KEY (tabla_origen, id_viejo)
);

-- 9. Indices (PG 9.4: sin IF NOT EXISTS -> chequeo en pg_class)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_ipd_cabecera') THEN
        CREATE INDEX idx_ipd_cabecera ON ingresos_productos_detalle (id_ingreso_cabecera);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_ipd_producto') THEN
        CREATE INDEX idx_ipd_producto ON ingresos_productos_detalle (id_producto);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_ipc_proveedor') THEN
        CREATE INDEX idx_ipc_proveedor ON ingresos_productos_cabecera (id_proveedor);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_ipc_fecha') THEN
        CREATE INDEX idx_ipc_fecha ON ingresos_productos_cabecera (fecha);
    END IF;
END $$;

COMMIT;
