-- ============================================================================
-- Migración: unidades de entrega por producto (partición de cantidad entre
-- bodegas según el tamaño de paquete).
--
-- Por producto se definen pares (cantidad_paquete, id_bodega):
--   - cantidad_paquete = 1  -> bodega para entregas por UNIDAD (absorbe el
--                              sobrante de la descomposición).
--   - cantidad_paquete > 1  -> paquete (ej. caja de 50, docena de 12) con su
--                              bodega.
-- Al vender/ordenar una cantidad, se descompone greedy de mayor a menor
-- paquete; cada paquete completo sale de su bodega y el sobrante de la bodega
-- por unidad. Si el producto no tiene filas aquí, se usa la selección
-- automática normal (4 reglas).
--
-- REEMPLAZA al modelo de rangos (productos_bodega_rangos), que se elimina.
--
-- Compatible con Postgres < 9.6. Idempotente: se puede correr varias veces.
-- ============================================================================

-- 1. Eliminar el modelo anterior de rangos (sin datos productivos).
DROP TABLE IF EXISTS productos_bodega_rangos;

-- 2. Nueva tabla de unidades de entrega.
CREATE TABLE IF NOT EXISTS productos_unidades_entrega (
    id               serial PRIMARY KEY,
    id_producto      integer NOT NULL,
    nombre           varchar(60),                 -- etiqueta opcional ("Caja x50", "Unidad")
    cantidad_paquete double precision NOT NULL,    -- 1 = bodega por unidad; > 1 = paquete
    id_bodega        integer NOT NULL,
    CONSTRAINT fk_pue_producto FOREIGN KEY (id_producto)
        REFERENCES productos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_pue_bodega FOREIGN KEY (id_bodega)
        REFERENCES bodegas (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

-- CREATE INDEX IF NOT EXISTS no existe en Postgres < 9.5: se usa guarda.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class WHERE relname = 'idx_pue_producto'
    ) THEN
        CREATE INDEX idx_pue_producto ON productos_unidades_entrega (id_producto);
    END IF;
END$$;
