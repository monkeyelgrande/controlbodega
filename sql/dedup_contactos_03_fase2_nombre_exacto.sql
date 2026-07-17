-- ============================================================================
-- Deduplicacion de contactos - Paso 03: FASE 2 (nombre exacto legacy)
-- ----------------------------------------------------------------------------
-- Tras la fase 1 (ya sin copias agro), quedan grupos de contactos con el MISMO
-- nombre normalizado que son duplicados legacy genuinos. Como aqui ya no hay
-- el marcador 'agro' que los distinga, requieren REVISION antes de fusionar.
--
-- Este script:
--   1) Genera un reporte para revisar cada grupo (uso real de cada miembro).
--   2) Ofrece dos formas de aplicar la fusion, SOLO sobre lo aprobado.
--
-- Requiere: pasos 00 y 01 ejecutados, y fase 1 (paso 02) ya aplicada.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- REPORTE: grupos de nombre duplicado con el uso de cada miembro.
-- Revisar para decidir cuales son realmente la misma persona/empresa.
-- ----------------------------------------------------------------------------
WITH grupos AS (
    SELECT normaliza_nombre(nombre) AS n
    FROM contactos
    GROUP BY normaliza_nombre(nombre)
    HAVING count(*) > 1
)
SELECT
    g.n AS nombre_norm,
    c.id, c.nombre, c.cedula, c.proveedor, c.ciudad, c.contacto, c.email,
    (SELECT count(*) FROM facturas_cabeceras f           WHERE f.id_contacto = c.id) AS facturas,
    (SELECT count(*) FROM cotizaciones_cabeceras q       WHERE q.id_contacto = c.id) AS cotizaciones,
    (SELECT count(*) FROM ingresos_mercancias_cabecera i WHERE i.id_proveedor = c.id OR i.id_transportador = c.id) AS ingresos,
    (SELECT count(*) FROM productos p                    WHERE p.id_proveedor = c.id) AS productos,
    (SELECT count(*) FROM ordenes_compra_detalle o       WHERE o.id_proveedor = c.id) AS ordenes_compra
FROM grupos g
JOIN contactos c ON normaliza_nombre(c.nombre) = g.n
ORDER BY g.n, c.id;


-- ============================================================================
-- APLICAR FASE 2 - OPCION 0 (AUTO, alta confianza)
-- ----------------------------------------------------------------------------
-- Fusiona los grupos de mismo nombre donde TODAS las cedulas no vacias se
-- reducen a una misma "base" (digitos antes del guion / dígito verificador),
-- tratando las vacias como comodin. Es decir: misma entidad con la cedula
-- escrita de formas distintas (37816926 vs 37816926-2) o stubs sin cedula.
--
-- Los grupos con 2+ cedulas base distintas (posibles homonimos reales, cedulas
-- mal digitadas, o "CONSUMIDOR FINAL") NO se tocan aqui: van a revision manual
-- (OPCION 1 / OPCION 2).
-- ============================================================================
BEGIN;

DO $$
DECLARE
    r       record;
    v_super integer;
BEGIN
    FOR r IN
        SELECT normaliza_nombre(nombre) AS n
        FROM contactos
        GROUP BY normaliza_nombre(nombre)
        HAVING count(*) > 1
           AND count(DISTINCT NULLIF(
                   regexp_replace(regexp_replace(cedula, '-.*$', ''), '[^0-9]', '', 'g'), '')
               ) <= 1
    LOOP
        SELECT id INTO v_super
        FROM contactos
        WHERE normaliza_nombre(nombre) = r.n
        ORDER BY (CASE WHEN cedula IS NOT NULL AND btrim(cedula) <> '' THEN 0 ELSE 1 END), id
        LIMIT 1;

        PERFORM fusionar_contacto(v_super, d.id, 'fase2-auto')
        FROM contactos d
        WHERE normaliza_nombre(d.nombre) = r.n AND d.id <> v_super;
    END LOOP;
END $$;

SELECT count(*) AS eliminados_fase2_auto FROM dedup_contactos_log WHERE fase = 'fase2-auto';
SELECT count(*) AS total_contactos_restantes FROM contactos;

-- Cambiar a ROLLBACK para prueba en seco; COMMIT para aplicar.
COMMIT;


-- ============================================================================
-- APLICAR FASE 2 - revision MANUAL (grupos con cedulas base distintas)
-- ============================================================================
--
-- OPCION 1 (puntual): llamadas explicitas sobreviviente/duplicado.
--   Util cuando hay que escoger manualmente quien gana en un grupo concreto.
--
--   BEGIN;
--     SELECT fusionar_contacto(843, 3485, 'fase2');   -- ejemplo: 3485 -> 843
--     SELECT fusionar_contacto(843, 2437, 'fase2');
--   COMMIT;
--
-- ----------------------------------------------------------------------------
-- OPCION 2 (por grupo): aprobar nombres normalizados completos. Cada grupo se
-- fusiona en su mejor sobreviviente (con cedula, no-agro, menor id).
-- Rellenar la lista v_aprobados con los nombres del reporte que se confirmen.
-- Con la lista vacia este bloque no hace nada.
-- ----------------------------------------------------------------------------
BEGIN;

DO $$
DECLARE
    v_aprobados text[] := ARRAY[
        -- 'ALEXANDER MARIN',
        -- 'ALGOFER SAS'
    ]::text[];
    v_nombre text;
    v_super  integer;
BEGIN
    FOREACH v_nombre IN ARRAY v_aprobados
    LOOP
        SELECT id INTO v_super
        FROM contactos
        WHERE normaliza_nombre(nombre) = v_nombre
        ORDER BY (CASE WHEN cedula IS NOT NULL AND btrim(cedula) <> '' THEN 0 ELSE 1 END), id
        LIMIT 1;

        IF v_super IS NULL THEN
            RAISE NOTICE 'Sin coincidencias para nombre normalizado: %', v_nombre;
            CONTINUE;
        END IF;

        PERFORM fusionar_contacto(v_super, d.id, 'fase2')
        FROM contactos d
        WHERE normaliza_nombre(d.nombre) = v_nombre AND d.id <> v_super;
    END LOOP;
END $$;

SELECT count(*) AS eliminados_fase2 FROM dedup_contactos_log WHERE fase = 'fase2';

-- Cambiar a ROLLBACK para prueba en seco; COMMIT para aplicar.
COMMIT;
