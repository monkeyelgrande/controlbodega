-- =============================================================================
-- Migración: modo de cálculo de precios por instalación (empresa).
-- Fecha:     2026-08-27
-- Motor:     PostgreSQL 9.4 (sin ADD COLUMN IF NOT EXISTS -> DO-blocks).
-- Idempotente: re-ejecutable sin efectos secundarios. NO modifica datos.
--
-- Contexto: controlbodega es el único desarrollo para varias empresas; cada
-- una corre con su propia base. Lo único que cambia entre empresas es CÓMO se
-- calculan los precios de venta en el módulo Precios:
--
--   'AGRO'  (default) = 1 margen (porcentaje_utilidad) + descuentos
--           escalonados (tabla descuentos) + valor S&T + precio de crédito.
--           Comportamiento actual: las instalaciones existentes no cambian.
--   'TECNI' = 3 márgenes independientes -> Precio 1 / Precio 2 / Precio 3,
--           todos con margen sobre el precio de venta:
--             precio = costo / ((100 - %) / 100)
--           Sin descuentos escalonados y sin precio de crédito.
--
-- En modo TECNI se REUTILIZAN las columnas existentes (mismo esquema para
-- ambas empresas, distinta interpretación):
--   productos.venta         = Precio 1     (margen porcentaje_utilidad)
--   productos.valor_desc_1  = Precio 2     (margen porcentaje_utilidad2)
--   productos.valor_desc_2  = Precio 3     (margen porcentaje_utilidad3)
--   ingresos_productos_detalle.desc_n_1/desc_n_2:
--       AGRO  -> utilidad monetaria descontada (venta - desc N)
--       TECNI -> porcentajes 2 y 3 de la línea (para retomar la edición)
--
-- Requiere haber corrido antes sql/migracion_fusion_agro.sql (columnas del
-- módulo Precios).
-- =============================================================================

BEGIN;

-- 1. Modo de precios de la instalación (configuraciones es de una sola fila).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'configuraciones' AND column_name = 'modo_precios') THEN
        ALTER TABLE configuraciones ADD COLUMN modo_precios character varying(10) DEFAULT 'AGRO';
    END IF;
END $$;

-- La fila existente queda explícitamente en AGRO si el default no la cubrió.
UPDATE configuraciones SET modo_precios = 'AGRO' WHERE id = 1 AND modo_precios IS NULL;

-- 2. Márgenes 2 y 3 del modo TECNI (nullables: el modo AGRO no los usa).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'productos' AND column_name = 'porcentaje_utilidad2') THEN
        ALTER TABLE productos ADD COLUMN porcentaje_utilidad2 double precision;
        ALTER TABLE productos ADD COLUMN porcentaje_utilidad3 double precision;
    END IF;
END $$;

COMMIT;

-- Para activar el modo TECNI en la instalación de esa empresa (una sola vez):
--   UPDATE configuraciones SET modo_precios = 'TECNI' WHERE id = 1;
