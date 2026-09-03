-- =============================================================================
-- PERMISOS: REIMPRIMIR ORDEN SIN LIMITE (modulo Ordenes)
-- Incremento sobre migracion_permisos.sql (que ya debe estar aplicada).
-- Idempotente (re-ejecutable). Compatible PostgreSQL 9.4.
--
-- Agrega al catalogo de permisos la opcion que libera el candado de "una sola
-- impresion" del boton "Imprimir Orden" (btn_verFactura en frm_ver_orden):
--   ordenes_reimprimir   boton btn_verFactura en frm_ver_orden
--
-- Por defecto el vendedor (perfil 3) solo puede imprimir una vez cada orden
-- (columna facturas_cabeceras.impreso_vendedor). El usuario al que se le conceda
-- esta opcion puede imprimir la orden todas las veces que quiera, sin pedir clave
-- de administrador.
--
-- Igual que 'ordenes_unir' y 'ordenes_entrega_masiva', NO se concede a ningun
-- perfil: es una propiedad por usuario. Se concede individualmente desde la
-- pantalla de administracion de permisos (tabla usuario_opciones).
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM opciones WHERE clave = 'ordenes_reimprimir') THEN
        INSERT INTO opciones (clave, nombre, modulo, componente, orden) VALUES
        ('ordenes_reimprimir', 'Reimprimir orden sin limite', 'Ordenes', 'btn_verFactura', 12);
        -- SIN INSERT en perfil_opciones: se concede por usuario.
    END IF;
END $$;

COMMIT;
