-- ============================================================================
-- Migración: auto-impresión de órdenes vía LISTEN/NOTIFY
-- Compatible con Postgres < 9.6 (sin ADD COLUMN IF NOT EXISTS).
-- Idempotente: se puede correr varias veces sin error.
-- ============================================================================

-- 1. Flags por usuario
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'imprime_ordenes'
    ) THEN
        ALTER TABLE users ADD COLUMN imprime_ordenes BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'nombre_impresora'
    ) THEN
        ALTER TABLE users ADD COLUMN nombre_impresora VARCHAR(150);
    END IF;
END$$;

-- 2. Marca de impresión automática (idempotencia frente a reinicios)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'facturas_cabeceras' AND column_name = 'impreso_auto'
    ) THEN
        ALTER TABLE facturas_cabeceras ADD COLUMN impreso_auto BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END$$;

-- 3. Función disparada por trigger
CREATE OR REPLACE FUNCTION fn_notify_orden_nueva()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.tipo_factura IS DISTINCT FROM 'Venta' THEN
        PERFORM pg_notify(
            'orden_nueva',
            NEW.id::text                              || '|' ||
            COALESCE(NEW.id_bodega::text, '')         || '|' ||
            COALESCE(NEW.id_user::text, '')           || '|' ||
            COALESCE(NEW.tipo_factura, '')
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 4. Trigger AFTER INSERT
DROP TRIGGER IF EXISTS trg_notify_orden_nueva ON facturas_cabeceras;
CREATE TRIGGER trg_notify_orden_nueva
AFTER INSERT ON facturas_cabeceras
FOR EACH ROW EXECUTE PROCEDURE fn_notify_orden_nueva();
