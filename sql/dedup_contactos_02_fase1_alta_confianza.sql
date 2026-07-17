-- ============================================================================
-- Deduplicacion de contactos - Paso 02: FASE 1 (alta confianza)
-- ----------------------------------------------------------------------------
-- Fusiona automaticamente los dos casos seguros:
--   (A) Pares "agro": en cada grupo de mismo nombre normalizado que mezcla
--       un original (origen<>'agro') con copias de la migracion (origen='agro'),
--       las copias agro se absorben en el original.
--   (B) Cedula normalizada (solo digitos) repetida: ej. '1051674606' vs
--       '1051674606-1'. Sobrevive el original (no-agro) de menor id.
--
-- Sobreviviente: no-agro, preferentemente con cedula, menor id.
-- Requiere: pasos 00 y 01 ya ejecutados.
--
-- USO:
--   1) Revisar primero la seccion DRY-RUN (solo SELECT, no modifica nada).
--   2) Ejecutar la seccion APLICAR. Para una prueba en seco, cambiar el
--      COMMIT final por ROLLBACK.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- DRY-RUN A: pares agro que se fusionaran (sobreviviente + cuantas copias agro).
-- ----------------------------------------------------------------------------
SELECT s.id AS id_sobreviviente, s.nombre, s.cedula,
       g.copias_agro,
       (SELECT count(*) FROM facturas_cabeceras f
         JOIN contactos c ON f.id_contacto = c.id
        WHERE normaliza_nombre(c.nombre) = g.n AND COALESCE(c.origen,'') = 'agro') AS facturas_a_mover,
       (SELECT count(*) FROM ingresos_mercancias_cabecera im
         JOIN contactos c ON im.id_proveedor = c.id OR im.id_transportador = c.id
        WHERE normaliza_nombre(c.nombre) = g.n AND COALESCE(c.origen,'') = 'agro') AS ingresos_a_mover
FROM (
    SELECT normaliza_nombre(nombre) AS n,
           count(*) FILTER (WHERE COALESCE(origen,'') = 'agro') AS copias_agro
    FROM contactos
    GROUP BY normaliza_nombre(nombre)
    HAVING count(*) FILTER (WHERE origen = 'agro') > 0
       AND count(*) FILTER (WHERE COALESCE(origen,'') <> 'agro') > 0
) g
JOIN LATERAL (
    SELECT id, nombre, cedula FROM contactos
    WHERE normaliza_nombre(nombre) = g.n AND COALESCE(origen,'') <> 'agro'
    ORDER BY (CASE WHEN cedula IS NOT NULL AND btrim(cedula) <> '' THEN 0 ELSE 1 END), id
    LIMIT 1
) s ON true
ORDER BY g.copias_agro DESC, s.nombre;


-- ----------------------------------------------------------------------------
-- DRY-RUN B: grupos por cedula normalizada repetida.
-- ----------------------------------------------------------------------------
SELECT normaliza_cedula(cedula) AS cedula_norm,
       count(*) AS miembros,
       string_agg(id || ':' || nombre || ' (' || COALESCE(cedula,'') || ')', ' | ' ORDER BY id) AS detalle
FROM contactos
WHERE normaliza_cedula(cedula) <> ''
GROUP BY normaliza_cedula(cedula)
HAVING count(*) > 1
ORDER BY 1;


-- ============================================================================
-- APLICAR FASE 1
-- ============================================================================
BEGIN;

DO $$
DECLARE
    r       record;
    v_super integer;
BEGIN
    -- (A) Absorber copias agro en su original homonimo.
    FOR r IN
        SELECT normaliza_nombre(nombre) AS n
        FROM contactos
        GROUP BY normaliza_nombre(nombre)
        HAVING count(*) FILTER (WHERE origen = 'agro') > 0
           AND count(*) FILTER (WHERE COALESCE(origen,'') <> 'agro') > 0
    LOOP
        SELECT id INTO v_super
        FROM contactos
        WHERE normaliza_nombre(nombre) = r.n AND COALESCE(origen,'') <> 'agro'
        ORDER BY (CASE WHEN cedula IS NOT NULL AND btrim(cedula) <> '' THEN 0 ELSE 1 END), id
        LIMIT 1;

        PERFORM fusionar_contacto(v_super, d.id, 'fase1-agro')
        FROM contactos d
        WHERE normaliza_nombre(d.nombre) = r.n AND COALESCE(d.origen,'') = 'agro';
    END LOOP;

    -- (B) Unificar por cedula normalizada (recalculado sobre el estado ya fusionado).
    FOR r IN
        SELECT normaliza_cedula(cedula) AS c
        FROM contactos
        WHERE normaliza_cedula(cedula) <> ''
        GROUP BY normaliza_cedula(cedula)
        HAVING count(*) > 1
    LOOP
        SELECT id INTO v_super
        FROM contactos
        WHERE normaliza_cedula(cedula) = r.c
        ORDER BY (CASE WHEN COALESCE(origen,'') <> 'agro' THEN 0 ELSE 1 END), id
        LIMIT 1;

        PERFORM fusionar_contacto(v_super, d.id, 'fase1-cedula')
        FROM contactos d
        WHERE normaliza_cedula(d.cedula) = r.c AND d.id <> v_super;
    END LOOP;
END $$;

-- Resumen de lo aplicado en esta transaccion.
SELECT fase, count(*) AS contactos_eliminados
FROM dedup_contactos_log
WHERE fase IN ('fase1-agro', 'fase1-cedula')
GROUP BY fase ORDER BY fase;

SELECT count(*) AS total_contactos_restantes FROM contactos;

-- Cambiar a ROLLBACK para una prueba en seco; COMMIT para aplicar definitivo.
COMMIT;
