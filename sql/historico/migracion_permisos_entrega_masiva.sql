-- =============================================================================
-- PERMISOS: ENTREGA MASIVA POR BODEGA (modulo Ordenes)
-- Incremento sobre migracion_permisos.sql (que ya debe estar aplicada).
-- Idempotente (re-ejecutable). Compatible PostgreSQL 9.4.
--
-- Agrega al catalogo de permisos la opcion que gobierna el boton "Entregar
-- todo" de frm_Ordenes, que entrega por completo todas las ordenes pendientes
-- de las bodegas marcadas con entrega_automatica = true:
--   ordenes_entrega_masiva   boton btn_entregar_todo en frm_Ordenes
--
-- Igual que 'ordenes_unir', NO se concede a ningun perfil: es una propiedad por
-- usuario. Por defecto solo el admin (perfil 1) ve el boton; a los demas se les
-- concede individualmente desde la pantalla de administracion de permisos
-- (tabla usuario_opciones).
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM opciones WHERE clave = 'ordenes_entrega_masiva') THEN
        INSERT INTO opciones (clave, nombre, modulo, componente, orden) VALUES
        ('ordenes_entrega_masiva', 'Entrega masiva por bodega', 'Ordenes', 'btn_entregar_todo', 11);
        -- SIN INSERT en perfil_opciones: se concede por usuario.
    END IF;
END $$;

COMMIT;
