-- ============================================================================
-- Migración: prioridad de bodega por rangos de cantidad, por producto.
--
-- Permite que un producto se entregue de una bodega distinta según la cantidad
-- solicitada (ej: 1-5 -> bodega A, 6-20 -> bodega B, 20+ -> bodega C).
-- Es opcional por producto: si un producto no tiene filas en esta tabla, se
-- sigue usando la selección automática normal (las 4 reglas de
-- DBstock_productos.seleccionarBodegaDescarga).
--
-- cantidad_max NULL = "en adelante" (sin tope superior).
--
-- Compatible con Postgres < 9.6. Idempotente: se puede correr varias veces.
-- ============================================================================

CREATE TABLE IF NOT EXISTS productos_bodega_rangos (
    id            serial PRIMARY KEY,
    id_producto   integer NOT NULL,
    cantidad_min  double precision NOT NULL,
    cantidad_max  double precision,            -- NULL = sin tope superior ("en adelante")
    id_bodega     integer NOT NULL,
    CONSTRAINT fk_pbr_producto FOREIGN KEY (id_producto)
        REFERENCES productos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_pbr_bodega FOREIGN KEY (id_bodega)
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
        SELECT 1 FROM pg_class WHERE relname = 'idx_pbr_producto'
    ) THEN
        CREATE INDEX idx_pbr_producto ON productos_bodega_rangos (id_producto);
    END IF;
END$$;
