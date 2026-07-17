-- ============================================================================
-- Deduplicacion de contactos - Paso 00: Preparacion
-- ----------------------------------------------------------------------------
-- Crea extensiones, funciones de normalizacion y la tabla de auditoria.
-- Es idempotente: se puede ejecutar varias veces sin error.
-- Base destino: bodega_nuevo (copia de produccion). NO ejecutar primero en prod.
-- ============================================================================

-- Extensiones para matching difuso y normalizacion de acentos.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- ----------------------------------------------------------------------------
-- normaliza_cedula: deja solo digitos. IMMUTABLE para poder usarse en indices.
--   '1051674606-1' -> '1051674606'   ''/NULL -> ''
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION normaliza_cedula(p_ced text)
RETURNS text
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT regexp_replace(COALESCE(p_ced, ''), '[^0-9]', '', 'g');
$$;

-- ----------------------------------------------------------------------------
-- normaliza_nombre: mayusculas, sin acentos, espacios colapsados, sin bordes.
--   STABLE (depende del diccionario unaccent). Uso solo en agrupaciones/reportes.
--   '  José   Pérez ' -> 'JOSE PEREZ'
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION normaliza_nombre(p_nom text)
RETURNS text
LANGUAGE sql
STABLE
AS $$
    SELECT upper(btrim(regexp_replace(unaccent(COALESCE(p_nom, '')), '\s+', ' ', 'g')));
$$;

-- ----------------------------------------------------------------------------
-- Tabla de auditoria: una fila por cada contacto absorbido (eliminado).
--   Permite rastrear que se fusiono con que, y revertir si fuese necesario.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dedup_contactos_log (
    id                SERIAL PRIMARY KEY,
    id_sobreviviente  integer     NOT NULL,
    id_eliminado      integer     NOT NULL,
    nombre_eliminado  text,
    cedula_eliminado  text,
    motivo            text,
    fase              text,
    fecha             timestamp   NOT NULL DEFAULT now()
);

-- Verificacion rapida.
SELECT 'preparacion OK' AS estado,
       normaliza_cedula('1051674606-1') AS ej_cedula,
       normaliza_nombre('  José   Pérez ') AS ej_nombre;
