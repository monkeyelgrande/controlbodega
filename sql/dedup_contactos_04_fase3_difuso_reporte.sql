-- ============================================================================
-- Deduplicacion de contactos - Paso 04: FASE 3 (similitud difusa) - SOLO REPORTE
-- ----------------------------------------------------------------------------
-- Detecta posibles duplicados con variaciones de nombre que NO son identicas
-- (ej. "ALEXANDER MARIN" vs "ALEXANDER MARIN PEÑA") usando trigramas (pg_trgm).
--
-- ESTE SCRIPT NO MODIFICA NADA. Entrega pares candidatos para revision manual.
-- Una vez confirmados, fusionar con la OPCION 1 del paso 03:
--     SELECT fusionar_contacto(<sobreviviente>, <duplicado>, 'fase3');
--
-- Requiere: paso 00 ejecutado (pg_trgm + normaliza_nombre).
-- Recomendado correrlo despues de fases 1 y 2 para reducir ruido.
-- ============================================================================

-- Umbral de similitud (0..1). Subir para menos ruido, bajar para mas candidatos.
SELECT set_limit(0.6);

WITH base AS (
    SELECT id, nombre, cedula, proveedor, ciudad,
           normaliza_nombre(nombre) AS n
    FROM contactos
)
SELECT
    round(similarity(a.n, b.n)::numeric, 3) AS similitud,
    a.id AS id_a, a.nombre AS nombre_a, a.cedula AS cedula_a,
    b.id AS id_b, b.nombre AS nombre_b, b.cedula AS cedula_b,
    (SELECT count(*) FROM facturas_cabeceras f WHERE f.id_contacto = a.id) AS facturas_a,
    (SELECT count(*) FROM facturas_cabeceras f WHERE f.id_contacto = b.id) AS facturas_b
FROM base a
JOIN base b
  ON a.id < b.id
 AND left(a.n, 3) = left(b.n, 3)   -- prefiltro por prefijo: acota el costo
 AND a.n <> b.n                    -- los identicos ya los cubre la fase 2
 AND a.n % b.n                     -- operador de similitud trigram (usa set_limit)
ORDER BY similitud DESC, a.nombre
LIMIT 500;
