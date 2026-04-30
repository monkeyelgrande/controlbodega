-- ============================================================================
-- Migración: trigger que notifica facturas que son SOLO novedades.
--
-- Cuando wo-printer procesa una factura donde TODOS los ítems son novedades
-- (códigos inválidos / no existen / inactivos / cantidad cero) NO inserta
-- ninguna fila en facturas_cabeceras. Por lo tanto el trigger normal
-- (trg_notify_orden_nueva) nunca dispara y el listener no se entera.
--
-- Este trigger nuevo se dispara en facturas_impresas y emite NOTIFY sólo
-- cuando no existe ninguna cabecera para esa numero_factura.
-- Idempotente.
-- ============================================================================

CREATE OR REPLACE FUNCTION fn_notify_factura_solo_novedad()
RETURNS TRIGGER AS $$
BEGIN
    -- Sólo notifica si NO hay cabeceras para esta numero_factura.
    -- Como la transacción de wo-printer inserta primero las cabeceras y
    -- luego facturas_impresas, en este punto ya están visibles (misma TX).
    IF NOT EXISTS (
        SELECT 1 FROM facturas_cabeceras
        WHERE codigo = NEW.numero_factura
    ) THEN
        PERFORM pg_notify(
            'orden_nueva',
            'NOVEDAD|' || NEW.id::text || '|' || COALESCE(NEW.numero_factura, '')
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_notify_factura_solo_novedad ON facturas_impresas;
CREATE TRIGGER trg_notify_factura_solo_novedad
AFTER INSERT ON facturas_impresas
FOR EACH ROW EXECUTE PROCEDURE fn_notify_factura_solo_novedad();
