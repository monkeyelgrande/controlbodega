-- =============================================================================
-- Entrega automática (masiva) por bodega.
-- Agrega la columna bodegas.entrega_automatica (boolean). Las bodegas marcadas
-- con TRUE son las que el botón "Entregar todo" de frm_Ordenes barre: entrega
-- por completo, de una sola vez, todas sus órdenes pendientes (caso Almacén
-- Piso Uno, donde los productos se entregan siempre en el acto).
--
-- Por defecto FALSE: ninguna bodega cambia su comportamiento hasta marcarla.
-- Marque la bodega objetivo manualmente, por ejemplo:
--   UPDATE bodegas SET entrega_automatica = true WHERE nombre = 'Almacén Piso Uno';
--
-- Idempotente y compatible con PostgreSQL 9.4 (no usa ADD COLUMN IF NOT EXISTS,
-- que solo existe desde 9.6).
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'bodegas' AND column_name = 'entrega_automatica'
    ) THEN
        ALTER TABLE bodegas ADD COLUMN entrega_automatica boolean NOT NULL DEFAULT false;
    END IF;
END $$;

COMMIT;
