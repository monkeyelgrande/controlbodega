-- ============================================================================
-- Deduplicacion de contactos - Paso 05: Prevencion (endurecer integridad)
-- ----------------------------------------------------------------------------
-- Ejecutar SOLO cuando las fusiones (fases 1-3 aprobadas) esten completas.
--   1) Indice unico sobre la cedula NORMALIZADA (solo digitos) -> impide volver
--      a tener '1051674606' y '1051674606-1' como contactos distintos.
--   2) Elimina la columna muerta contacto_maestro (sin uso en el codigo).
--
-- Requiere: paso 00 ejecutado (funcion normaliza_cedula IMMUTABLE).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) PRE-CHECK: no debe quedar ningun duplicado por cedula normalizada.
--    Si esta consulta devuelve filas, resolverlas antes de crear el indice.
-- ----------------------------------------------------------------------------
SELECT normaliza_cedula(cedula) AS cedula_norm, count(*) AS miembros,
       string_agg(id::text, ', ' ORDER BY id) AS ids
FROM contactos
WHERE normaliza_cedula(cedula) <> ''
GROUP BY normaliza_cedula(cedula)
HAVING count(*) > 1
ORDER BY 2 DESC;

-- ----------------------------------------------------------------------------
-- 2) Indice unico sobre cedula normalizada.
--    La condicion <> '' excluye las cedulas que ahora son nombres (sin digitos);
--    esas quedan protegidas por el indice ya existente contactos_cedula_unique.
-- ----------------------------------------------------------------------------
-- El indice cubre SOLO cedulas con formato numerico real (digitos, con guion y
-- digito de verificacion opcional). Las cedulas basadas en nombre (regla
-- cedula=nombre del paso 06, que pueden contener letras o digitos sueltos) NO
-- entran aqui: solo las ampara el indice contactos_cedula_unique sobre la cruda.
--
-- Nota: PostgreSQL 9.4 no soporta CREATE INDEX IF NOT EXISTS (9.5+), por eso
-- el guard va con un bloque DO que verifica pg_indexes.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'contactos_cedula_norm_unique') THEN
        CREATE UNIQUE INDEX contactos_cedula_norm_unique
            ON contactos (normaliza_cedula(cedula))
            WHERE cedula ~ '^[0-9]+(-[0-9]+)?$';
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 3) Eliminar columna muerta contacto_maestro (residuo de migracion; sin uso
--    en el codigo Java, apuntaba a ids inexistentes).
-- ----------------------------------------------------------------------------
ALTER TABLE contactos DROP COLUMN IF EXISTS contacto_maestro;

SELECT 'prevencion aplicada' AS estado;
