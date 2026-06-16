-- =============================================================================
-- Opcion de permisos para el modulo "Analizar comisiones" (menu Precios).
-- Idempotente. Sigue el patron de las demas opciones menu_precios_*.
-- =============================================================================

BEGIN;

-- 1. Sembrar la opcion (clave unica). Si ya existe, se omite.
INSERT INTO opciones (clave, nombre, modulo, componente, orden)
SELECT 'menu_precios_comisiones', 'Analizar comisiones', 'Precios', 'itemComisionesPrecios', 45
WHERE NOT EXISTS (SELECT 1 FROM opciones WHERE clave = 'menu_precios_comisiones');

-- 2. Darla a los mismos perfiles que ya tienen "Imprimir etiquetas"
--    (menu_precios_etiquetas), evitando duplicados.
INSERT INTO perfil_opciones (id_perfil, id_opcion)
SELECT po.id_perfil, nueva.id
FROM perfil_opciones po
JOIN opciones eti   ON eti.id = po.id_opcion AND eti.clave = 'menu_precios_etiquetas'
JOIN opciones nueva ON nueva.clave = 'menu_precios_comisiones'
WHERE NOT EXISTS (
    SELECT 1 FROM perfil_opciones x
    WHERE x.id_perfil = po.id_perfil AND x.id_opcion = nueva.id
);

COMMIT;
