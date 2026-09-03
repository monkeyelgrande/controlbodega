-- =============================================================================
-- PERMISOS: ANULAR ORDEN (modulo Ordenes)
-- Incremento sobre migracion_permisos.sql (que ya debe estar aplicada).
-- Idempotente (re-ejecutable). Compatible PostgreSQL 9.4.
--
-- Agrega al catalogo de permisos la opcion que gobierna el boton "Anular"
-- (btn_eliminar en frm_Ordenes):
--   ordenes_anular   boton btn_eliminar en frm_Ordenes
--
-- Antes anular una orden exigia clave de administrador. Ahora es una opcion
-- gobernable: el usuario o perfil al que se le conceda puede anular directamente
-- sin pedir clave. Si la BD no tiene los permisos cargados, se conserva el
-- comportamiento anterior (clave de administrador).
--
-- A diferencia de 'ordenes_unir'/'ordenes_entrega_masiva', esta opcion SI se
-- puede conceder tanto a un perfil (perfil_opciones) como a un usuario
-- (usuario_opciones), segun se necesite, desde la pantalla de administracion de
-- permisos. Aqui solo se agrega al catalogo, sin concederla a ningun perfil por
-- defecto (solo el admin la tiene por codigo).
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM opciones WHERE clave = 'ordenes_anular') THEN
        INSERT INTO opciones (clave, nombre, modulo, componente, orden) VALUES
        ('ordenes_anular', 'Anular ordenes', 'Ordenes', 'btn_eliminar', 13);
        -- SIN INSERT en perfil_opciones: se concede a perfil o usuario a demanda.
    END IF;
END $$;

COMMIT;
