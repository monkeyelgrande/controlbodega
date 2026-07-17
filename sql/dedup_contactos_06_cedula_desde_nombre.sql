-- ============================================================================
-- Deduplicacion de contactos - Paso 06: cedula = nombre para los sin cedula
-- ----------------------------------------------------------------------------
-- Regla de negocio: todo contacto sin cedula debe quedar con cedula = su nombre.
-- Como existe el indice unico de cedula (raw), los pocos casos donde el nombre
-- colisionaria (nombres repetidos sin cedula, o un nombre que ya es la cedula
-- de otro contacto) reciben el nombre con el id como sufijo para garantizar
-- unicidad: 'NOMBRE (id)'.
--
-- Ejecutar idealmente al final, tras las fusiones. Idempotente.
-- ============================================================================

BEGIN;

-- 1) Caso comun: asignar el nombre tal cual cuando NO genera colision
--    (ni con una cedula existente, ni con otro contacto sin cedula del mismo nombre).
UPDATE contactos a
   SET cedula = a.nombre
 WHERE (a.cedula IS NULL OR btrim(a.cedula) = '')
   AND NOT EXISTS (SELECT 1 FROM contactos b
                   WHERE b.id <> a.id AND b.cedula = a.nombre)
   AND NOT EXISTS (SELECT 1 FROM contactos c
                   WHERE c.id <> a.id AND (c.cedula IS NULL OR btrim(c.cedula) = '')
                     AND c.nombre = a.nombre);

-- 2) Casos en colision (los que quedaron en blanco): nombre + id para unicidad.
UPDATE contactos a
   SET cedula = a.nombre || ' (' || a.id || ')'
 WHERE (a.cedula IS NULL OR btrim(a.cedula) = '');

COMMIT;

-- Verificacion: no debe quedar ninguno en blanco.
SELECT count(*) AS sin_cedula_restantes FROM contactos
WHERE cedula IS NULL OR btrim(cedula) = '';
