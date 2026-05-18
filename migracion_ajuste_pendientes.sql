-- ============================================================================
-- Migracion: agregar columnas de pendientes a ajustes_inventario_detalle
-- Permite que un ajuste de inventario tambien modifique la cantidad pendiente
-- (reservada contra ordenes) ademas de la cantidad fisica.
-- Compatible con PostgreSQL 9.4+.
-- ============================================================================

BEGIN;

ALTER TABLE ajustes_inventario_detalle
  ADD COLUMN IF NOT EXISTS pendientes_anterior    double precision NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS pendientes_nuevo       double precision NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS diferencia_pendientes  double precision NOT NULL DEFAULT 0;

COMMIT;
