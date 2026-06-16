-- =============================================================================
-- Color identificador por bodega.
-- Agrega la columna bodegas.color (hex "#RRGGBB") para diferenciar las bodegas
-- en tablas, reportes y graficos. Nullable: una bodega sin color asignado se
-- muestra con el fondo normal.
--
-- Idempotente y compatible con PostgreSQL 9.4 (no usa ADD COLUMN IF NOT EXISTS,
-- que solo existe desde 9.6).
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'bodegas' AND column_name = 'color'
    ) THEN
        ALTER TABLE bodegas ADD COLUMN color varchar(7);
    END IF;
END $$;

COMMIT;
