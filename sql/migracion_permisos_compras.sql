-- =============================================================================
-- PERMISOS: MODULO COMPRAS (bodega_nuevo)
-- Incremento sobre migracion_permisos.sql (que ya debe estar aplicada).
-- Idempotente (re-ejecutable). Compatible PostgreSQL 9.4.
--
-- Agrega al catalogo de permisos el menu Compras y sus acciones:
--   menu_compras    visibilidad del menu Compras (ordenes de compra)
--   compras_crear   boton Nueva orden
--   compras_editar  boton Editar orden
--
-- Se crean y conceden a TODOS los perfiles UNA SOLA VEZ (hoy el menu Compras
-- es visible para todos los perfiles: el comportamiento del dia 1 no cambia).
-- Re-ejecutar este script nunca pisa cambios hechos desde la pantalla de
-- administracion de permisos.
--
-- El permiso de analizar/aprobar ordenes NO cambia: sigue siendo la casilla
-- por usuario users.aprueba_compras.
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM opciones WHERE clave = 'menu_compras') THEN
        INSERT INTO opciones (clave, nombre, modulo, componente, orden) VALUES
        ('menu_compras',   'Menu Compras (ordenes de compra)', 'Compras', 'menuCompras', 10),
        ('compras_crear',  'Crear orden de compra',            'Compras', 'btn_nueva',   20),
        ('compras_editar', 'Editar orden de compra',           'Compras', 'btn_editar',  30);

        INSERT INTO perfil_opciones (id_perfil, id_opcion)
        SELECT p.id, o.id
        FROM perfiles p, opciones o
        WHERE o.clave IN ('menu_compras', 'compras_crear', 'compras_editar');
    END IF;
END $$;

COMMIT;
