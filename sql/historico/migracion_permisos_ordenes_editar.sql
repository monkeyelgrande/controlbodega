-- =============================================================================
-- PERMISOS: EDITAR ORDENES (modulo Ordenes) - bodega_nuevo
-- Incremento sobre migracion_permisos.sql (que ya debe estar aplicada).
-- Idempotente (re-ejecutable). Compatible PostgreSQL 9.4.
--
-- Agrega la opcion 'ordenes_editar' que gobierna el boton "Editar" del
-- listado de ordenes (frm_Ordenes). Asi el permiso de editar puede
-- entregarse a la persona o perfil que se necesite, desde la pantalla de
-- administracion de permisos.
--
-- Sembrado para conservar el comportamiento actual: hoy el boton Editar esta
-- habilitado para todos los perfiles MENOS el bodeguero (perfil 2). Se concede
-- entonces a todos los perfiles excepto el 2. Corre UNA SOLA VEZ (cuando la
-- opcion aun no existe): re-ejecutar nunca pisa cambios hechos desde la
-- administracion.
--
-- Nota: las reglas de negocio NO cambian: el vendedor (perfil 3) sigue sin
-- poder editar una orden ya impresa, y ninguna orden con entregas se edita.
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM opciones WHERE clave = 'ordenes_editar') THEN
        INSERT INTO opciones (clave, nombre, modulo, componente, orden) VALUES
        ('ordenes_editar', 'Editar ordenes', 'Ordenes', 'btn_editar', 45);

        INSERT INTO perfil_opciones (id_perfil, id_opcion)
        SELECT p.id, o.id
        FROM perfiles p, opciones o
        WHERE o.clave = 'ordenes_editar'
          AND p.id <> 2;
    END IF;
END $$;

COMMIT;
