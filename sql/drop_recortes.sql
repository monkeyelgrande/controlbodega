-- ============================================================================
-- drop_recortes.sql
-- ----------------------------------------------------------------------------
-- Elimina el modulo "Recortes" de la base de datos de produccion.
--
-- El modulo Recortes nunca se uso; su codigo (formularios, clases y reportes)
-- fue removido de la aplicacion. Estas dos tablas son las unicas huellas que
-- quedan en la base de datos.
--
-- IMPORTANTE:
--   * Este script BORRA de forma permanente las tablas y TODOS sus datos.
--   * Ejecutalo TU manualmente en produccion cuando lo decidas, despues de
--     confirmar que no hay datos que valga la pena conservar:
--         SELECT count(*) FROM recortes_cabecera;
--         SELECT count(*) FROM recortes_detalle;
--   * Se recomienda un respaldo (pg_dump) antes de ejecutar.
--
-- Orden: primero el detalle (depende de la cabecera por FK), luego la cabecera.
-- Se usa IF EXISTS para que el script sea idempotente.
-- ============================================================================

BEGIN;

DROP TABLE IF EXISTS recortes_detalle;
DROP TABLE IF EXISTS recortes_cabecera;

COMMIT;
