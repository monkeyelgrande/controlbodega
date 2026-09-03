-- =============================================================================
-- MODULO CREDITOS - FASE 2: cruce de saldos, caja, comisiones y auditoria
--
-- Incremento sobre sql/migracion_modulo_creditos.sql y sql/migracion_modulo_caja.sql
-- y sql/migracion_auditoria_caja.sql (las tres deben estar aplicadas).
-- Idempotente (re-ejecutable). Compatible PostgreSQL 9.4 y superiores.
--
-- QUE AGREGA
--
--   1. CRUCE DE SALDOS A FAVOR
--      No necesita estructura nueva: el saldo a favor ya es implicito
--      (cabecera.total - SUM(detalle)). Solo se agrega el indice que usa la
--      consulta de facturas pendientes del cruce.
--
--   2. INTEGRACION CON CAJA
--      tipos_abonos.agregar_a_ingreso   1 = el abono entra a caja como ingreso
--      cuentas_ingresos.abono_a_credito 1 = cuenta destino de esos ingresos
--      ingresos.id_abono_credito        cabecera de abono que genero el ingreso
--      ingresos.id_vendedor             vendedor al que se le abona
--      Borrar el pago borra su ingreso (lo hace DBabonos.EliminarPago).
--
--   3. COMISIONES DE VENDEDOR
--      creditos.id_empleado     vendedor dueño del credito (contactos.empleado=1)
--      creditos.comisionable    el credito genera comision (por defecto si)
--      tipos_abonos.comisionable  el tipo de pago genera comision
--      abonos.comision_pagada / abonos_cabeceras.comision_pagada
--                               marca de comision ya liquidada
--      porcentajes_comision     escala dias-de-cobro -> porcentaje
--
--   4. AUDITORIA
--      Se REUTILIZA el marco de sql/migracion_auditoria_caja.sql (auditoria_caja
--      + auditoria_caja_campos + fn_auditoria_caja). Es generico: la funcion
--      trabaja con TG_TABLE_NAME y row_to_json, sirve para cualquier tabla.
--      Aqui solo se enseñan los nombres/etiquetas de las tablas de cartera y se
--      enganchan sus triggers. Queda UN solo libro de auditoria para todo el
--      dinero (caja y cartera), inmutable y con detalle campo por campo.
--
-- OJO: este script vuelve a definir caja_audit_nombre_tabla() y
--      caja_audit_etiqueta() agregandoles los casos de cartera. Si algun dia se
--      re-ejecuta sql/migracion_auditoria_caja.sql, hay que re-ejecutar este
--      tambien (o quedan las etiquetas de cartera sin traducir; no se pierde
--      ningun dato, solo se ven los ids en crudo).
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. CAMPOS NUEVOS EN TABLAS EXISTENTES
--    PG 9.4 no tiene ADD COLUMN IF NOT EXISTS: se verifica a mano.
-- =============================================================================

DO $$
BEGIN
    -- ---- tipos_abonos: los dos interruptores del catalogo ----
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'tipos_abonos' AND column_name = 'agregar_a_ingreso') THEN
        -- 1 por defecto: lo natural es que un pago recibido entre a caja.
        ALTER TABLE tipos_abonos ADD COLUMN agregar_a_ingreso integer NOT NULL DEFAULT 1;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'tipos_abonos' AND column_name = 'comisionable') THEN
        -- 0 por defecto: la comision se habilita a conciencia, tipo por tipo.
        ALTER TABLE tipos_abonos ADD COLUMN comisionable integer NOT NULL DEFAULT 0;
    END IF;

    -- ---- creditos: vendedor y marca de comisionable ----
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'creditos' AND column_name = 'id_empleado') THEN
        ALTER TABLE creditos ADD COLUMN id_empleado integer;
        ALTER TABLE creditos ADD CONSTRAINT fk_creditos_empleado
            FOREIGN KEY (id_empleado) REFERENCES contactos (id)
            ON UPDATE NO ACTION ON DELETE NO ACTION;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'creditos' AND column_name = 'comisionable') THEN
        ALTER TABLE creditos ADD COLUMN comisionable boolean NOT NULL DEFAULT true;
    END IF;

    -- ---- abonos / abonos_cabeceras: marca de comision liquidada ----
    --      En el detalle para los abonos aplicados a creditos; en la cabecera
    --      para el anticipo puro, que no tiene detalle donde marcarla.
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'abonos' AND column_name = 'comision_pagada') THEN
        ALTER TABLE abonos ADD COLUMN comision_pagada integer NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'abonos_cabeceras' AND column_name = 'comision_pagada') THEN
        ALTER TABLE abonos_cabeceras ADD COLUMN comision_pagada integer NOT NULL DEFAULT 0;
    END IF;

    -- ---- cuentas_ingresos: cual es la cuenta de "abono a credito" ----
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'cuentas_ingresos' AND column_name = 'abono_a_credito') THEN
        ALTER TABLE cuentas_ingresos ADD COLUMN abono_a_credito integer NOT NULL DEFAULT 0;
    END IF;

    -- ---- ingresos: de que abono viene y a que vendedor se le acredita ----
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'ingresos' AND column_name = 'id_abono_credito') THEN
        ALTER TABLE ingresos ADD COLUMN id_abono_credito integer;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'ingresos' AND column_name = 'id_vendedor') THEN
        ALTER TABLE ingresos ADD COLUMN id_vendedor integer;
    END IF;
END $$;

-- =============================================================================
-- 2. TABLA NUEVA: escala de comisiones
--    Se lee ordenada por dias y se toma el PRIMER rango cuyo "dias" es mayor o
--    igual a los dias que tardo el cobro. Ejemplo de escala tipica:
--        dias=30  porcentaje=3     cobrado dentro de 30 dias -> 3%
--        dias=60  porcentaje=2     entre 31 y 60 dias        -> 2%
--        dias=90  porcentaje=1     entre 61 y 90 dias        -> 1%
--    Un anticipo siempre toma el primer rango (el mejor porcentaje).
--    No se siembra ninguna fila: la escala es una decision comercial del
--    cliente. Con la tabla vacia el reporte muestra comision 0 y lo avisa.
-- =============================================================================

CREATE TABLE IF NOT EXISTS porcentajes_comision (
    id serial NOT NULL,
    dias integer NOT NULL,
    porcentaje double precision NOT NULL,
    CONSTRAINT pk_porcentajes_comision PRIMARY KEY (id)
);

-- =============================================================================
-- 3. CUENTA DE INGRESOS DESTINO DE LOS ABONOS
--    DBIngresos.Guardar_desde_abono_credito resuelve la cuenta con
--    (select id from cuentas_ingresos where abono_a_credito=1). Si no hay
--    ninguna marcada el INSERT falla, asi que se garantiza que exista una.
-- =============================================================================

--    No se adivina cual de las cuentas existentes es: se crea una dedicada.
--    Si el cliente prefiere usar otra, la cambia desde Caja > Cuentas de
--    ingresos (el formulario permite mover la marca) y borra esta.
DO $$
DECLARE
    v_id integer;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM cuentas_ingresos WHERE abono_a_credito = 1) THEN
        SELECT id INTO v_id FROM cuentas_ingresos
        WHERE upper(btrim(nombre)) = 'ABONO A CREDITO' ORDER BY id LIMIT 1;

        IF v_id IS NULL THEN
            INSERT INTO cuentas_ingresos (nombre, predeterminado, abono_a_credito)
            VALUES ('ABONO A CREDITO', 0, 1);
        ELSE
            UPDATE cuentas_ingresos SET abono_a_credito = 1 WHERE id = v_id;
        END IF;
    END IF;
END $$;

-- Solo puede haber UNA cuenta marcada: el subselect del INSERT de ingresos no
-- admite mas de una fila.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_cuentas_ingresos_abono_credito') THEN
        CREATE UNIQUE INDEX idx_cuentas_ingresos_abono_credito
            ON cuentas_ingresos (abono_a_credito) WHERE abono_a_credito = 1;
    END IF;
END $$;

-- =============================================================================
-- 4. INDICES
-- =============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_ingresos_abono_credito') THEN
        CREATE INDEX idx_ingresos_abono_credito ON ingresos(id_abono_credito);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_creditos_empleado') THEN
        CREATE INDEX idx_creditos_empleado ON creditos(id_empleado);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_creditos_fecha_creacion') THEN
        CREATE INDEX idx_creditos_fecha_creacion ON creditos(fecha_creacion);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_abonos_fecha') THEN
        CREATE INDEX idx_abonos_fecha ON abonos(fecha);
    END IF;
END $$;

-- =============================================================================
-- 5. AUDITORIA DE CARTERA
--    Reutiliza el marco de sql/migracion_auditoria_caja.sql.
-- =============================================================================

-- 5.1 Nombres legibles: se agregan las tablas de cartera a las de caja.
CREATE OR REPLACE FUNCTION caja_audit_nombre_tabla(p_tabla text)
RETURNS text AS $$
BEGIN
    RETURN CASE p_tabla
        WHEN 'ingresos'             THEN 'ingreso'
        WHEN 'egresos'              THEN 'egreso'
        WHEN 'transferencias'       THEN 'traslado'
        WHEN 'fondos'               THEN 'fondo'
        WHEN 'cuentas_ingresos'     THEN 'cuenta de ingresos'
        WHEN 'cuentas_egresos'      THEN 'cuenta de egresos'
        WHEN 'fotos_registros'      THEN 'soporte fotografico'
        -- Cartera
        WHEN 'creditos'             THEN 'credito'
        WHEN 'abonos_cabeceras'     THEN 'pago'
        WHEN 'abonos'               THEN 'aplicacion de pago'
        WHEN 'tipos_abonos'         THEN 'tipo de abono'
        WHEN 'cuentas'              THEN 'cuenta de credito'
        WHEN 'porcentajes_comision' THEN 'porcentaje de comision'
        ELSE p_tabla
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 5.2 Traduccion de llaves foraneas. Se agregan las de cartera cuidando que
--     id_cuenta signifique cosas distintas segun la tabla (cuentas_ingresos en
--     caja, cuentas en un credito).
CREATE OR REPLACE FUNCTION caja_audit_etiqueta(p_tabla text, p_campo text, p_valor text)
RETURNS text AS $$
DECLARE
    v_id integer;
    v_nombre text;
BEGIN
    IF p_valor IS NULL OR p_valor !~ '^[0-9]+$' THEN
        RETURN NULL;
    END IF;
    v_id := p_valor::integer;

    IF p_campo IN ('id_fondo', 'id_fondo_origen', 'id_fondo_destino') THEN
        SELECT f.nombre INTO v_nombre FROM fondos f WHERE f.id = v_id;

    ELSIF p_campo = 'id_cuenta' THEN
        IF p_tabla = 'ingresos' THEN
            SELECT c.nombre INTO v_nombre FROM cuentas_ingresos c WHERE c.id = v_id;
        ELSIF p_tabla = 'egresos' THEN
            SELECT c.nombre INTO v_nombre FROM cuentas_egresos c WHERE c.id = v_id;
        ELSIF p_tabla = 'creditos' THEN
            SELECT c.nombre INTO v_nombre FROM cuentas c WHERE c.id = v_id;
        END IF;

    ELSIF p_campo IN ('id_cliente', 'id_contacto', 'id_empleado', 'id_vendedor') THEN
        SELECT c.nombre INTO v_nombre FROM contactos c WHERE c.id = v_id;

    ELSIF p_campo = 'id_user' THEN
        SELECT u.user_name INTO v_nombre FROM users u WHERE u.id = v_id;

    ELSIF p_campo = 'id_tipo_abono' THEN
        SELECT t.nombre INTO v_nombre FROM tipos_abonos t WHERE t.id = v_id;

    ELSIF p_campo = 'id_credito' THEN
        SELECT COALESCE(cr.codigo, '#' || cr.id::text) INTO v_nombre
        FROM creditos cr WHERE cr.id = v_id;

    ELSIF p_campo IN ('id_ingreso', 'id_egreso', 'id_cabecera', 'id_abono_credito') THEN
        v_nombre := NULL;   -- son enlaces internos, no tienen nombre que mostrar
    END IF;

    RETURN v_nombre;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- 5.3 Enganche de los triggers. DROP + CREATE para que re-ejecutar no duplique.
--     Se auditan el dinero (creditos, abonos_cabeceras, abonos) y los catalogos
--     que cambian el significado del historico (tipos_abonos, cuentas,
--     porcentajes_comision: tocar un porcentaje reescribe lo que se le debe a
--     un vendedor).

DROP TRIGGER IF EXISTS trg_auditoria_creditos ON creditos;
CREATE TRIGGER trg_auditoria_creditos
    AFTER INSERT OR UPDATE OR DELETE ON creditos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

DROP TRIGGER IF EXISTS trg_auditoria_abonos_cabeceras ON abonos_cabeceras;
CREATE TRIGGER trg_auditoria_abonos_cabeceras
    AFTER INSERT OR UPDATE OR DELETE ON abonos_cabeceras
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

DROP TRIGGER IF EXISTS trg_auditoria_abonos ON abonos;
CREATE TRIGGER trg_auditoria_abonos
    AFTER INSERT OR UPDATE OR DELETE ON abonos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

DROP TRIGGER IF EXISTS trg_auditoria_tipos_abonos ON tipos_abonos;
CREATE TRIGGER trg_auditoria_tipos_abonos
    AFTER INSERT OR UPDATE OR DELETE ON tipos_abonos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

DROP TRIGGER IF EXISTS trg_auditoria_cuentas ON cuentas;
CREATE TRIGGER trg_auditoria_cuentas
    AFTER INSERT OR UPDATE OR DELETE ON cuentas
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

DROP TRIGGER IF EXISTS trg_auditoria_porcentajes_comision ON porcentajes_comision;
CREATE TRIGGER trg_auditoria_porcentajes_comision
    AFTER INSERT OR UPDATE OR DELETE ON porcentajes_comision
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

-- 5.4 Vista de lectura de la auditoria de cartera (misma forma que
--     v_auditoria_caja, filtrada a las tablas del modulo Creditos).
CREATE OR REPLACE VIEW v_auditoria_creditos AS
SELECT a.id,
       a.fecha,
       a.hora,
       a.fecha_hora,
       a.tabla,
       a.operacion,
       a.id_registro,
       COALESCE(a.nombre_usuario, a.usuario, a.usuario_bd) AS usuario_visible,
       a.usuario,
       a.origen,
       a.equipo,
       a.total_anterior,
       a.total_nuevo,
       a.diferencia,
       a.descripcion,
       (SELECT string_agg(c.campo || ': ' ||
                          COALESCE(c.etiqueta_anterior, c.valor_anterior, '(vacio)') || ' -> ' ||
                          COALESCE(c.etiqueta_nueva,   c.valor_nuevo,    '(vacio)'), ' | ')
          FROM auditoria_caja_campos c
         WHERE c.id_auditoria = a.id) AS cambios
FROM auditoria_caja a
WHERE a.tabla IN ('creditos', 'abonos_cabeceras', 'abonos',
                  'tipos_abonos', 'cuentas', 'porcentajes_comision');

-- =============================================================================
-- 6. OPCIONES DE PERMISOS NUEVAS
--    Cerradas por defecto (es cartera y plata de vendedores): el Admin las
--    reparte desde la pantalla de permisos.
-- =============================================================================

INSERT INTO opciones (clave, nombre, modulo, componente, orden)
SELECT v.clave, v.nombre, v.modulo, v.componente, v.orden
FROM (
    VALUES
    ('creditos_porcentajes_comision', 'Porcentajes de comision', 'Creditos', 'itemCreditosPorcentajes', 70),
    ('creditos_comisiones',           'Reporte de comisiones',   'Creditos', 'itemCreditosComisiones',  80),
    ('creditos_cruzar_saldo',         'Cruzar saldos a favor',   'Creditos', 'btnCruzarSaldo',          90),
    ('creditos_auditoria',            'Auditoria de cartera',    'Creditos', 'itemCreditosAuditoria',  100)
) AS v (clave, nombre, modulo, componente, orden)
WHERE NOT EXISTS (SELECT 1 FROM opciones o WHERE o.clave = v.clave);

COMMIT;

-- =============================================================================
-- CONSULTAS DE VERIFICACION
--
--   -- Estructura nueva en su sitio
--   SELECT table_name, column_name FROM information_schema.columns
--   WHERE (table_name='tipos_abonos'     AND column_name IN ('comisionable','agregar_a_ingreso'))
--      OR (table_name='creditos'         AND column_name IN ('id_empleado','comisionable'))
--      OR (table_name='abonos'           AND column_name='comision_pagada')
--      OR (table_name='abonos_cabeceras' AND column_name='comision_pagada')
--      OR (table_name='ingresos'         AND column_name IN ('id_abono_credito','id_vendedor'))
--      OR (table_name='cuentas_ingresos' AND column_name='abono_a_credito')
--   ORDER BY table_name, column_name;
--
--   -- Cuenta destino de los abonos (debe devolver exactamente una fila)
--   SELECT id, nombre FROM cuentas_ingresos WHERE abono_a_credito = 1;
--
--   -- Triggers de auditoria de cartera enganchados (deben ser 6)
--   SELECT tgrelid::regclass AS tabla, tgname FROM pg_trigger
--   WHERE tgname LIKE 'trg_auditoria_%' AND NOT tgisinternal
--     AND tgrelid::regclass::text IN ('creditos','abonos_cabeceras','abonos',
--                                     'tipos_abonos','cuentas','porcentajes_comision')
--   ORDER BY 1;
--
--   -- Movimientos de cartera auditados hoy
--   SELECT * FROM v_auditoria_creditos WHERE fecha = current_date ORDER BY id DESC;
--
--   -- Saldo a favor sin cruzar, por cliente
--   SELECT co.nombre,
--          SUM(ca.total) - COALESCE(SUM(ap.aplicado),0) AS saldo_favor
--   FROM abonos_cabeceras ca
--   JOIN contactos co ON co.id = ca.id_contacto
--   LEFT JOIN (SELECT id_cabecera, SUM(abono) AS aplicado FROM abonos GROUP BY id_cabecera) ap
--          ON ap.id_cabecera = ca.id
--   GROUP BY co.nombre HAVING SUM(ca.total) - COALESCE(SUM(ap.aplicado),0) > 0.009
--   ORDER BY 2 DESC;
-- =============================================================================
