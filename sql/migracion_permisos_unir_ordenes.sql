-- =============================================================================
-- PERMISOS: UNIR ORDENES DE ENTREGA (bodega_nuevo)
-- Incremento sobre migracion_permisos.sql (que ya debe estar aplicada).
-- Idempotente (re-ejecutable). Compatible PostgreSQL 9.4.
--
-- Agrega al catalogo de permisos la opcion de unir (fusionar) ordenes de
-- entrega desde la pantalla de Ordenes:
--   ordenes_unir   boton Unir ordenes en frm_Ordenes
--
-- A diferencia de otras migraciones, NO se concede a ningun perfil: es una
-- propiedad por usuario. Por defecto solo el admin (perfil 1) puede unir;
-- a los demas usuarios se les concede individualmente desde la pantalla de
-- administracion de permisos (tabla usuario_opciones).
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM opciones WHERE clave = 'ordenes_unir') THEN
        INSERT INTO opciones (clave, nombre, modulo, componente, orden) VALUES
        ('ordenes_unir', 'Unir ordenes de entrega', 'Ordenes', 'btn_unir', 10);
        -- SIN INSERT en perfil_opciones: se concede por usuario.
    END IF;
END $$;

COMMIT;
