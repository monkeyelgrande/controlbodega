-- ============================================================================
-- Migración: flag por bodega para generar orden de entrega automática
-- al facturar Venta desde frm_facturacion_ventas.
-- Compatible con Postgres < 9.6 (sin ADD COLUMN IF NOT EXISTS).
-- Idempotente: se puede correr varias veces sin error.
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'bodegas' AND column_name = 'genera_orden_automatica'
    ) THEN
        ALTER TABLE bodegas
        ADD COLUMN genera_orden_automatica BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END$$;

-- Configuración inicial (ajustar a las bodegas que aplican):
--   - ALMACEN PISO 1 (id 3) → FALSE (entrega inmediata, sin orden)
--   - BODEGA PRINCIPAL (id 2), ALMACEN PISO 2 (id 4) → TRUE
--
-- UPDATE bodegas SET genera_orden_automatica = TRUE WHERE id IN (2, 4);
