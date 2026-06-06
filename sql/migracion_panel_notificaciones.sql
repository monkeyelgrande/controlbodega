-- ============================================================================
-- Migración: flag por usuario para mostrar, dentro del JDesktopPane, el panel
-- de productos agotados o en negativo (users.panel_notificaciones).
-- Compatible con Postgres < 9.6 (sin ADD COLUMN IF NOT EXISTS).
-- Idempotente: se puede correr varias veces sin error.
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'panel_notificaciones'
    ) THEN
        ALTER TABLE users
        ADD COLUMN panel_notificaciones BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END$$;

-- Activar el panel para los usuarios que correspondan, por ejemplo:
--   UPDATE users SET panel_notificaciones = TRUE WHERE id = 1;
