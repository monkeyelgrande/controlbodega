-- =============================================================================
-- MIGRACION: ingresos.recibo_caja
-- Motor: PostgreSQL 9.4  |  Fecha: 2026-07-24
--
-- Marca informativa para distinguir los ingresos que corresponden a un
-- "recibo de caja". Por defecto TODOS los ingresos son 0 (no es recibo de
-- caja); se guarda 1 solo cuando el usuario marca la casilla en el
-- formulario de Ingresos. Por ahora es solo informativo: se usara mas
-- adelante para reportes.
--
-- Es ADITIVA e IDEMPOTENTE (re-ejecutable, no toca datos existentes salvo
-- para rellenar el valor por defecto 0 en las filas ya creadas).
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'ingresos' AND column_name = 'recibo_caja') THEN
        ALTER TABLE ingresos ADD COLUMN recibo_caja integer DEFAULT 0;
    END IF;
END $$;

-- Filas historicas: sin recibo de caja
UPDATE ingresos SET recibo_caja = 0 WHERE recibo_caja IS NULL;

COMMIT;

-- Verificacion rapida:
--   SELECT recibo_caja, count(*) FROM ingresos GROUP BY recibo_caja;
