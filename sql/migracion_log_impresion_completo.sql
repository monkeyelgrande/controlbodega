-- ============================================================================
-- Migración: bitácora por usuario de tickets completos auto-impresos.
-- Necesaria para deduplicar el ticket completo cuando un usuario tiene
-- imp_ticket_bodega_asignada = true y wo-printer parte una factura en
-- varias cabeceras (cada una dispara NOTIFY, pero el ticket completo se
-- debe imprimir una sola vez por usuario y por numero_factura).
-- Idempotente.
-- ============================================================================

CREATE TABLE IF NOT EXISTS auto_impresion_log_completo (
    id_user        INTEGER NOT NULL,
    numero_factura VARCHAR(200) NOT NULL,
    impreso_at     TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id_user, numero_factura)
);

-- Índice por fecha para limpieza programada (opcional)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = 'public' AND indexname = 'idx_auto_impresion_log_fecha'
    ) THEN
        CREATE INDEX idx_auto_impresion_log_fecha
            ON auto_impresion_log_completo (impreso_at);
    END IF;
END$$;
