-- =============================================================================
-- MODULOS LICENCIABLES POR INSTALACION (bodega_nuevo)
-- Incremento sobre la migracion de permisos (que ya debe estar aplicada).
-- Idempotente (re-ejecutable). Compatible PostgreSQL 9.4.
--
-- Crea la tabla modulos: interruptor por cliente/instalacion de conjuntos
-- completos de funcionalidad. Es un nivel DISTINTO al de permisos:
--
--   modulos.activo        que compro este cliente (se apaga para todos,
--                         incluso Admin; se administra por instalacion)
--   opciones/perfil_...   que ve cada usuario DENTRO de un modulo activo
--
-- Reglas:
--   * modulos.clave se corresponde con opciones.modulo ('Compras', 'Precios',
--     y a futuro 'Creditos', ...).
--   * Un modulo SIN fila en esta tabla es NUCLEO: siempre activo. Solo se
--     siembran los modulos genuinamente opcionales.
--   * activo = false apaga el modulo completo: sus opciones desaparecen de
--     los permisos efectivos y la app oculta sus menus para todos.
--   * En BD sin esta migracion la app degrada a "todo activo" (comportamiento
--     anterior), igual que hace el sistema de permisos.
--
-- Para apagar un modulo en la instalacion de un cliente:
--   UPDATE modulos SET activo = false WHERE clave = 'Compras';
-- =============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS modulos (
    id     serial       PRIMARY KEY,
    clave  varchar(50)  NOT NULL UNIQUE,
    nombre varchar(100) NOT NULL,
    activo boolean      NOT NULL DEFAULT true
);

-- Siembra de los modulos opcionales existentes, encendidos: el dia 1 no
-- cambia nada. Re-ejecutar nunca pisa el activo elegido por instalacion.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM modulos WHERE clave = 'Compras') THEN
        INSERT INTO modulos (clave, nombre, activo)
        VALUES ('Compras', 'Compras (pipeline de ordenes de compra)', true);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM modulos WHERE clave = 'Precios') THEN
        INSERT INTO modulos (clave, nombre, activo)
        VALUES ('Precios', 'Precios (agroinsumos y comisiones)', true);
    END IF;
END $$;

COMMIT;
