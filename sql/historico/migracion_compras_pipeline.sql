-- ============================================================================
-- Migración: PIPELINE DE COMPRAS (Especificación Funcional del cliente)
--
-- Amplía el módulo de compras a las etapas:
--   RF-01/02  Sugerido de pedidos (+ selección y amarre de proveedores)
--   RF-03     Solicitud de cotización (RFQ)
--   RF-04     Comparativo de cotizaciones (matriz N proveedores)
--   RF-05     Autorización de Gerencia
--   RF-06     Orden de compra final (IVA, condiciones, entrega)
--
-- Complementa a migracion_ordenes_compra.sql (no la reemplaza).
-- Compatible con Postgres 9.4. Idempotente.
-- ============================================================================

-- ===================== FASE 0: cimientos de datos ==========================

-- Peso unitario del producto (kg) para el comparativo y la planificación de carga.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'productos' AND column_name = 'peso_unitario'
    ) THEN
        ALTER TABLE productos ADD COLUMN peso_unitario numeric(18,4);
    END IF;
END$$;

-- Amarre N:M producto <-> proveedor (un producto puede tener varios proveedores).
-- El celular/datos del proveedor salen de contactos.
CREATE TABLE IF NOT EXISTS producto_proveedores (
    id            serial PRIMARY KEY,
    id_producto   integer NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    id_proveedor  integer NOT NULL REFERENCES contactos(id) ON DELETE CASCADE
);
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'uq_producto_proveedor'
    ) THEN
        CREATE UNIQUE INDEX uq_producto_proveedor
            ON producto_proveedores (id_producto, id_proveedor);
    END IF;
END$$;

-- ===================== FASE 1/2: Sugerido de pedidos =======================
-- estado: 0=ABIERTO (editable), 1=BLOQUEADO (cerrado), 2=PROCESADO (ya generó RFQ/comparativo)
CREATE TABLE IF NOT EXISTS sugeridos_cabecera (
    id            integer PRIMARY KEY,
    numero        varchar(50),
    id_user_crea  integer NOT NULL REFERENCES users(id),
    fecha         date,
    hora          time,
    estado        integer NOT NULL DEFAULT 0,
    observacion   text,
    id_bodega     integer REFERENCES bodegas(id),
    meses_cobertura numeric(6,2) DEFAULT 1
);

CREATE TABLE IF NOT EXISTS sugeridos_detalle (
    id                integer PRIMARY KEY,
    id_sugerido_cab   integer NOT NULL REFERENCES sugeridos_cabecera(id) ON DELETE CASCADE,
    id_producto       integer NOT NULL REFERENCES productos(id),
    cantidad_sugerida numeric(18,4) NOT NULL,
    existencia        numeric(18,4),   -- snapshot al momento del recorrido
    rotacion_mensual  numeric(18,4),   -- snapshot
    ultima_compra     numeric(18,4),   -- snapshot (cantidad última compra)
    seleccionado      boolean NOT NULL DEFAULT TRUE,  -- RF-02: qué se pide realmente
    cantidad_final    numeric(18,4),                  -- RF-02: cantidad ajustada
    observacion       text
);
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_sugeridos_detalle_cab') THEN
        CREATE INDEX idx_sugeridos_detalle_cab ON sugeridos_detalle (id_sugerido_cab);
    END IF;
END$$;

-- ===================== FASE 3: Solicitud de cotización (RFQ) ================
-- estado: 0=BORRADOR, 1=ENVIADA, 2=RESPONDIDA, 3=SIN RESPUESTA
CREATE TABLE IF NOT EXISTS cotizaciones_compra_cabecera (
    id            integer PRIMARY KEY,
    numero        varchar(50),
    id_sugerido   integer REFERENCES sugeridos_cabecera(id) ON DELETE SET NULL,
    id_proveedor  integer NOT NULL REFERENCES contactos(id),
    id_user       integer REFERENCES users(id),
    fecha         date,
    hora          time,
    estado        integer NOT NULL DEFAULT 0,
    fecha_envio   date,
    fecha_limite  date,
    condicion_pago varchar(120),
    validez       varchar(120),
    observacion   text
);

CREATE TABLE IF NOT EXISTS cotizaciones_compra_detalle (
    id              integer PRIMARY KEY,
    id_cotiz_cab    integer NOT NULL REFERENCES cotizaciones_compra_cabecera(id) ON DELETE CASCADE,
    id_producto     integer NOT NULL REFERENCES productos(id),
    cantidad        numeric(18,4) NOT NULL,
    precio_unitario numeric(18,4),   -- lo diligencia/recibe del proveedor
    iva_pct         numeric(6,4),
    plazo_entrega   varchar(80),
    observacion     text
);
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_cotiz_detalle_cab') THEN
        CREATE INDEX idx_cotiz_detalle_cab ON cotizaciones_compra_detalle (id_cotiz_cab);
    END IF;
END$$;

-- ===================== FASE 4: Comparativo de cotizaciones =================
-- estado: 0=ABIERTO, 1=DECIDIDO, 2=AUTORIZADO
-- decision: 'UNICO' (todo a un proveedor) | 'POR_PRODUCTO' (el más barato de cada uno)
CREATE TABLE IF NOT EXISTS comparativos_cabecera (
    id                  integer PRIMARY KEY,
    numero              varchar(50),
    id_sugerido         integer REFERENCES sugeridos_cabecera(id) ON DELETE SET NULL,
    id_user             integer REFERENCES users(id),
    fecha               date,
    hora                time,
    iva_pct             numeric(6,4) NOT NULL DEFAULT 0.19,
    capacidad_camion_ton numeric(10,2) DEFAULT 30,
    estado              integer NOT NULL DEFAULT 0,
    decision            varchar(20),
    id_proveedor_unico  integer REFERENCES contactos(id),
    id_user_autoriza    integer REFERENCES users(id),
    fecha_autorizacion  timestamp,
    observacion         text
);

-- Filas del comparativo (productos a cotizar)
CREATE TABLE IF NOT EXISTS comparativos_productos (
    id              integer PRIMARY KEY,
    id_comparativo  integer NOT NULL REFERENCES comparativos_cabecera(id) ON DELETE CASCADE,
    id_producto     integer NOT NULL REFERENCES productos(id),
    cantidad        numeric(18,4) NOT NULL DEFAULT 0,
    peso_unitario   numeric(18,4),
    posicion        integer
);

-- Columnas del comparativo (proveedores, hasta 10)
CREATE TABLE IF NOT EXISTS comparativos_proveedores (
    id                   integer PRIMARY KEY,
    id_comparativo       integer NOT NULL REFERENCES comparativos_cabecera(id) ON DELETE CASCADE,
    id_proveedor         integer NOT NULL REFERENCES contactos(id),
    descuento_pronto_pago numeric(6,4) NOT NULL DEFAULT 0,
    condicion_pago       varchar(120),
    flete                numeric(18,4) NOT NULL DEFAULT 0,
    posicion             integer
);

-- Celdas: precio de lista (sin IVA) de cada producto por cada proveedor
CREATE TABLE IF NOT EXISTS comparativos_precios (
    id              integer PRIMARY KEY,
    id_comparativo  integer NOT NULL REFERENCES comparativos_cabecera(id) ON DELETE CASCADE,
    id_comp_producto integer NOT NULL REFERENCES comparativos_productos(id) ON DELETE CASCADE,
    id_comp_proveedor integer NOT NULL REFERENCES comparativos_proveedores(id) ON DELETE CASCADE,
    precio_lista    numeric(18,4)
);
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_comp_precios_comp') THEN
        CREATE INDEX idx_comp_precios_comp ON comparativos_precios (id_comparativo);
    END IF;
END$$;

-- ===================== FASE 5: Orden de compra final =======================
-- Campos adicionales sobre la tabla existente ordenes_compra_cabecera.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name='ordenes_compra_cabecera' AND column_name='id_comparativo') THEN
        ALTER TABLE ordenes_compra_cabecera ADD COLUMN id_comparativo integer;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name='ordenes_compra_cabecera' AND column_name='iva_pct') THEN
        ALTER TABLE ordenes_compra_cabecera ADD COLUMN iva_pct numeric(6,4) DEFAULT 0.19;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name='ordenes_compra_cabecera' AND column_name='condicion_pago') THEN
        ALTER TABLE ordenes_compra_cabecera ADD COLUMN condicion_pago varchar(120);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name='ordenes_compra_cabecera' AND column_name='fecha_entrega') THEN
        ALTER TABLE ordenes_compra_cabecera ADD COLUMN fecha_entrega date;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name='ordenes_compra_cabecera' AND column_name='lugar_entrega') THEN
        ALTER TABLE ordenes_compra_cabecera ADD COLUMN lugar_entrega varchar(200);
    END IF;
END$$;
