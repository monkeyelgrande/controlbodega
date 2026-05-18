-- =====================================================================
-- Migracion: tabla escaneos_qr_ordenes
-- Modulo: Entregas Rapidas (lector QR + pantalla tactil)
-- Motor: PostgreSQL (compatible con versiones anteriores a 9.5)
-- =====================================================================
-- Registra cada lectura de codigo QR realizada desde el modulo
-- "Entregas Rapidas". Permite generar reportes historicos de la orden
-- incluyendo todas sus lecturas (validas, invalidas, fallidas).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Tabla principal
-- ---------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name   = 'escaneos_qr_ordenes'
    ) THEN
        CREATE TABLE escaneos_qr_ordenes (
            id              INTEGER       NOT NULL,
            id_factura      INTEGER       NULL,
            id_user         INTEGER       NOT NULL,
            id_bodega       INTEGER       NOT NULL,
            fecha_escaneo   DATE          NOT NULL,
            hora_escaneo    TIME          NOT NULL,
            qr_leido        VARCHAR(150)  NULL,
            resultado       VARCHAR(30)   NOT NULL,
            accion          VARCHAR(30)   NOT NULL,
            id_entrega_cab  INTEGER       NULL,
            pc_origen       VARCHAR(80)   NULL,
            CONSTRAINT pk_escaneos_qr_ordenes PRIMARY KEY (id)
        );
    END IF;
END
$$;

-- ---------------------------------------------------------------------
-- Indices (cada uno verificado individualmente)
-- ---------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = current_schema()
          AND indexname  = 'idx_escaneos_qr_id_factura'
    ) THEN
        CREATE INDEX idx_escaneos_qr_id_factura
            ON escaneos_qr_ordenes (id_factura);
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = current_schema()
          AND indexname  = 'idx_escaneos_qr_fecha'
    ) THEN
        CREATE INDEX idx_escaneos_qr_fecha
            ON escaneos_qr_ordenes (fecha_escaneo);
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = current_schema()
          AND indexname  = 'idx_escaneos_qr_user'
    ) THEN
        CREATE INDEX idx_escaneos_qr_user
            ON escaneos_qr_ordenes (id_user);
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = current_schema()
          AND indexname  = 'idx_escaneos_qr_bodega'
    ) THEN
        CREATE INDEX idx_escaneos_qr_bodega
            ON escaneos_qr_ordenes (id_bodega);
    END IF;
END
$$;

-- ---------------------------------------------------------------------
-- Comentarios (opcional; auto-documentacion en pgAdmin)
-- ---------------------------------------------------------------------
COMMENT ON TABLE  escaneos_qr_ordenes              IS 'Log de lecturas QR del modulo Entregas Rapidas';
COMMENT ON COLUMN escaneos_qr_ordenes.id_factura   IS 'NULL cuando el QR es invalido o no se pudo resolver la orden';
COMMENT ON COLUMN escaneos_qr_ordenes.qr_leido     IS 'Texto crudo recibido del lector, util para auditar lecturas defectuosas';
COMMENT ON COLUMN escaneos_qr_ordenes.resultado    IS 'OK | QR_INVALIDO | ORDEN_NO_EXISTE | OTRA_BODEGA | ANULADA | YA_ENTREGADA';
COMMENT ON COLUMN escaneos_qr_ordenes.accion       IS 'NINGUNA | ENTREGA_COMPLETA | ENTREGA_PARCIAL';
COMMENT ON COLUMN escaneos_qr_ordenes.id_entrega_cab IS 'Vincula el escaneo con la cabecera de entrega que genero (si la genero)';
COMMENT ON COLUMN escaneos_qr_ordenes.pc_origen    IS 'Nombre de host del equipo tactil que ejecuto la lectura';
