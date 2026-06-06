-- ============================================================================
-- Migración: Módulo de ÓRDENES DE COMPRA (solicitudes de pedido por escalas).
--
-- Cualquier usuario crea una orden agregando productos y cantidades, SIN
-- proveedor ni precios. Luego un usuario con privilegios (users.aprueba_compras)
-- analiza el histórico de compras y APRUEBA o RECHAZA asignando proveedor y
-- precio POR LÍNEA de producto.
--
-- Estados de la cabecera (ordenes_compra_cabecera.estado):
--   0 = BORRADOR    (la creó alguien, aún editable)
--   1 = PENDIENTE   (enviada a revisión)
--   2 = APROBADA    (revisada; con proveedor y precio por línea)
--   3 = RECHAZADA
--
-- Compatible con Postgres < 9.6 (sin ADD COLUMN IF NOT EXISTS).
-- Idempotente: se puede correr varias veces sin error.
-- ============================================================================

-- 1) Flag por usuario: permite analizar el histórico y aprobar/rechazar.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'aprueba_compras'
    ) THEN
        ALTER TABLE users
        ADD COLUMN aprueba_compras BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END$$;

-- 2) Cabecera de la orden de compra.
CREATE TABLE IF NOT EXISTS ordenes_compra_cabecera (
    id               integer PRIMARY KEY,
    numero           varchar(50),
    id_user_crea     integer NOT NULL REFERENCES users(id),
    fecha            date,
    hora             time,
    estado           integer NOT NULL DEFAULT 0,  -- 0=BORRADOR,1=PENDIENTE,2=APROBADA,3=RECHAZADA
    observacion      text,
    id_bodega        integer REFERENCES bodegas(id),
    id_user_aprueba  integer REFERENCES users(id),
    fecha_aprobacion timestamp
);

-- 3) Detalle: una fila por producto solicitado. Proveedor y precio quedan
--    nulos al crear y se asignan durante el análisis/aprobación.
CREATE TABLE IF NOT EXISTS ordenes_compra_detalle (
    id                integer PRIMARY KEY,
    id_orden_cabecera integer NOT NULL REFERENCES ordenes_compra_cabecera(id) ON DELETE CASCADE,
    id_producto       integer NOT NULL REFERENCES productos(id),
    cantidad          numeric(18,4) NOT NULL,
    id_proveedor      integer REFERENCES contactos(id),
    precio_unitario   numeric(18,4),
    observacion       text
);

-- Índice para acelerar la carga de detalles por orden.
-- (Postgres 9.4 no soporta CREATE INDEX IF NOT EXISTS; se usa guarda.)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_ordenes_compra_detalle_cabecera'
    ) THEN
        CREATE INDEX idx_ordenes_compra_detalle_cabecera
            ON ordenes_compra_detalle (id_orden_cabecera);
    END IF;
END$$;

-- Activar el permiso para los usuarios que correspondan, por ejemplo:
--   UPDATE users SET aprueba_compras = TRUE WHERE id = 1;
