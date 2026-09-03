-- =============================================================================
-- AUDITORIA DEL MODULO CAJA (ingresos, egresos y traslados)
-- Incremento sobre sql/migracion_modulo_caja.sql (debe estar aplicada).
-- Idempotente (re-ejecutable). Compatible PostgreSQL 9.4 (y superiores).
--
-- QUE HACE
--   Deja registro INMUTABLE de TODO movimiento (INSERT / UPDATE / DELETE) sobre
--   las tablas de dinero del modulo Caja, con fecha, hora, usuario del sistema,
--   tipo de operacion, valores anteriores y nuevos, campo por campo.
--
--   La captura es por TRIGGER de base de datos, no por codigo Java: cualquier
--   ruta que toque esas tablas queda auditada (el formulario, el borrado
--   generico DB_consultas_R_D.eliminar(), un script SQL manual, pgAdmin, etc.).
--   No hay forma de mover dinero en estas tablas sin dejar rastro.
--
-- TABLAS QUE SE AUDITAN
--   ingresos, egresos, transferencias   (el dinero)
--   fondos, cuentas_ingresos, cuentas_egresos  (catalogos: renombrar una cuenta
--                                               cambia el significado del
--                                               historico, por eso se auditan)
--   fotos_registros                     (soportes fotograficos)
--
-- TABLAS NUEVAS
--   auditoria_caja         una fila por operacion (el "que paso")
--   auditoria_caja_campos  una fila por campo que cambio (el "que valor")
--
--   No las ve el usuario final: no hay formulario ni menu. Quedan listas para
--   un reporte futuro (ver ejemplos de consulta al final del archivo).
--
-- USUARIO DEL SISTEMA
--   La aplicacion publica el usuario logueado en cada conexion con
--   set_config('app.id_usuario', ...) desde conexiondb.AuditoriaCaja.
--   Si por alguna razon no llega (script manual, version vieja del .jar), el
--   trigger cae al id_user de la propia fila y siempre deja constancia del
--   usuario de base de datos y de la maquina cliente.
--
-- INMUTABILIDAD
--   Un trigger bloquea UPDATE / DELETE / TRUNCATE sobre las tablas de
--   auditoria: son de solo agregar. Para depurar historico antiguo (solo el
--   administrador de la base, a conciencia):
--       ALTER TABLE auditoria_caja        DISABLE TRIGGER trg_auditoria_caja_inmutable;
--       ALTER TABLE auditoria_caja_campos DISABLE TRIGGER trg_auditoria_campos_inmutable;
--       DELETE FROM auditoria_caja WHERE fecha < DATE '2024-01-01';
--       ALTER TABLE auditoria_caja        ENABLE  TRIGGER trg_auditoria_caja_inmutable;
--       ALTER TABLE auditoria_caja_campos ENABLE  TRIGGER trg_auditoria_campos_inmutable;
-- =============================================================================

BEGIN;

-- 1. Tablas de auditoria ------------------------------------------------------

CREATE TABLE IF NOT EXISTS auditoria_caja (
    id bigserial NOT NULL,
    -- CUANDO
    fecha_hora timestamp without time zone NOT NULL DEFAULT now(),
    fecha date NOT NULL DEFAULT current_date,
    hora character varying(8) NOT NULL DEFAULT to_char(now(), 'HH24:MI:SS'),
    -- QUE
    tabla character varying(40) NOT NULL,        -- ingresos, egresos, transferencias, ...
    operacion character varying(10) NOT NULL,    -- INSERT | UPDATE | DELETE
    id_registro integer,                         -- id de la fila afectada
    descripcion character varying,               -- resumen legible de la operacion
    -- QUIEN
    id_usuario integer,                          -- users.id (contexto de la app)
    usuario character varying(100),              -- users.user_name congelado al momento
    nombre_usuario character varying(150),       -- users.nombre congelado al momento
    -- DESDE DONDE
    origen character varying(80),                -- formulario/accion (app.origen)
    equipo character varying(60),                -- IP del cliente
    aplicacion character varying(80),            -- application_name de la conexion
    usuario_bd character varying(60),            -- rol de PostgreSQL
    -- CUANTO (para cuadres rapidos sin abrir el json)
    total_anterior double precision,
    total_nuevo double precision,
    diferencia double precision,                 -- total_nuevo - total_anterior
    -- MOTIVO (opcional; lo publica la app en app.motivo si algun dia se pide)
    motivo character varying,
    -- LA FOTO COMPLETA DE LA FILA
    datos_anteriores json,
    datos_nuevos json,
    CONSTRAINT pk_auditoria_caja PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS auditoria_caja_campos (
    id bigserial NOT NULL,
    id_auditoria bigint NOT NULL,
    campo character varying(40) NOT NULL,
    valor_anterior text,
    valor_nuevo text,
    -- Traduccion legible de las llaves foraneas (id_fondo -> "Caja principal")
    etiqueta_anterior character varying(200),
    etiqueta_nueva character varying(200),
    CONSTRAINT pk_auditoria_caja_campos PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_caja_campos FOREIGN KEY (id_auditoria)
        REFERENCES auditoria_caja (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE CASCADE
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_auditoria_caja_registro') THEN
        CREATE INDEX idx_auditoria_caja_registro ON auditoria_caja(tabla, id_registro);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_auditoria_caja_fecha') THEN
        CREATE INDEX idx_auditoria_caja_fecha ON auditoria_caja(fecha);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_auditoria_caja_fecha_hora') THEN
        CREATE INDEX idx_auditoria_caja_fecha_hora ON auditoria_caja(fecha_hora);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_auditoria_caja_usuario') THEN
        CREATE INDEX idx_auditoria_caja_usuario ON auditoria_caja(id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_auditoria_caja_operacion') THEN
        CREATE INDEX idx_auditoria_caja_operacion ON auditoria_caja(operacion);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_auditoria_campos_auditoria') THEN
        CREATE INDEX idx_auditoria_campos_auditoria ON auditoria_caja_campos(id_auditoria);
    END IF;
END $$;

-- 2. Funciones auxiliares -----------------------------------------------------

-- Lee una variable de sesion publicada por la aplicacion. En 9.4
-- current_setting() revienta si la variable no existe: por eso el EXCEPTION.
CREATE OR REPLACE FUNCTION caja_audit_contexto(p_clave text)
RETURNS text AS $$
BEGIN
    RETURN nullif(btrim(current_setting(p_clave)), '');
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- Igual que la anterior pero devolviendo entero (NULL si no es numerica).
CREATE OR REPLACE FUNCTION caja_audit_contexto_int(p_clave text)
RETURNS integer AS $$
DECLARE
    v text;
BEGIN
    v := caja_audit_contexto(p_clave);
    IF v IS NULL OR v !~ '^[0-9]+$' THEN
        RETURN NULL;
    END IF;
    RETURN v::integer;
END;
$$ LANGUAGE plpgsql STABLE;

-- Nombre legible de la tabla, para la descripcion de la operacion.
CREATE OR REPLACE FUNCTION caja_audit_nombre_tabla(p_tabla text)
RETURNS text AS $$
BEGIN
    RETURN CASE p_tabla
        WHEN 'ingresos'         THEN 'ingreso'
        WHEN 'egresos'          THEN 'egreso'
        WHEN 'transferencias'   THEN 'traslado'
        WHEN 'fondos'           THEN 'fondo'
        WHEN 'cuentas_ingresos' THEN 'cuenta de ingresos'
        WHEN 'cuentas_egresos'  THEN 'cuenta de egresos'
        WHEN 'fotos_registros'  THEN 'soporte fotografico'
        ELSE p_tabla
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Traduce el valor de una llave foranea a su nombre, para que el reporte no
-- muestre "id_fondo: 3 -> 7" sino "Caja principal -> Bancolombia".
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
        END IF;
    ELSIF p_campo = 'id_cliente' THEN
        SELECT c.nombre INTO v_nombre FROM contactos c WHERE c.id = v_id;
    ELSIF p_campo = 'id_user' THEN
        SELECT u.user_name INTO v_nombre FROM users u WHERE u.id = v_id;
    ELSIF p_campo IN ('id_ingreso', 'id_egreso') THEN
        v_nombre := NULL;   -- son el par generado por el traslado, no requieren nombre
    END IF;

    RETURN v_nombre;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- Formato de dinero para la descripcion (sin depender del locale).
CREATE OR REPLACE FUNCTION caja_audit_money(p_valor double precision)
RETURNS text AS $$
BEGIN
    IF p_valor IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN btrim(to_char(p_valor, 'FM9999999999990.00'));
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 3. Trigger de auditoria -----------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditoria_caja()
RETURNS trigger AS $$
DECLARE
    v_ant json;
    v_nue json;
    v_id integer;
    v_id_usuario integer;
    v_usuario character varying(100);
    v_nombre character varying(150);
    v_total_ant double precision;
    v_total_nue double precision;
    v_cambios integer;
    v_id_auditoria bigint;
    v_descripcion text;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        v_ant := row_to_json(OLD);
    END IF;
    IF TG_OP <> 'DELETE' THEN
        v_nue := row_to_json(NEW);
    END IF;

    -- Un UPDATE que no cambia nada (guardar sin editar) no ensucia la auditoria.
    IF TG_OP = 'UPDATE' THEN
        SELECT count(*) INTO v_cambios
        FROM json_each_text(v_ant) a
        WHERE a.value IS DISTINCT FROM (v_nue ->> a.key);
        IF v_cambios = 0 THEN
            RETURN NULL;
        END IF;
    END IF;

    v_id := COALESCE((v_nue ->> 'id')::integer, (v_ant ->> 'id')::integer);
    v_total_ant := (v_ant ->> 'total')::double precision;
    v_total_nue := (v_nue ->> 'total')::double precision;

    -- Usuario: primero el contexto publicado por la app; si no llego, el dueño
    -- del documento (id_user de la fila).
    v_id_usuario := caja_audit_contexto_int('app.id_usuario');
    IF v_id_usuario IS NULL THEN
        v_id_usuario := COALESCE((v_nue ->> 'id_user')::integer, (v_ant ->> 'id_user')::integer);
    END IF;
    IF v_id_usuario IS NOT NULL THEN
        SELECT u.user_name, u.nombre INTO v_usuario, v_nombre
        FROM users u WHERE u.id = v_id_usuario;
    END IF;
    IF v_usuario IS NULL THEN
        v_usuario := caja_audit_contexto('app.usuario');
    END IF;

    -- Resumen legible ("Eliminacion de ingreso #142 por 350000.00")
    v_descripcion := CASE TG_OP
                        WHEN 'INSERT' THEN 'Creacion'
                        WHEN 'UPDATE' THEN 'Modificacion'
                        ELSE 'Eliminacion'
                     END
                     || ' de ' || caja_audit_nombre_tabla(TG_TABLE_NAME)
                     || ' #' || COALESCE(v_id::text, '?');
    IF TG_OP = 'UPDATE' AND v_total_ant IS DISTINCT FROM v_total_nue THEN
        v_descripcion := v_descripcion || ' - total ' || COALESCE(caja_audit_money(v_total_ant), 'NULO')
                         || ' -> ' || COALESCE(caja_audit_money(v_total_nue), 'NULO');
    ELSIF COALESCE(v_total_nue, v_total_ant) IS NOT NULL THEN
        v_descripcion := v_descripcion || ' por ' || caja_audit_money(COALESCE(v_total_nue, v_total_ant));
    END IF;
    IF TG_OP = 'UPDATE' THEN
        v_descripcion := v_descripcion || ' (' || v_cambios || ' campo(s))';
    END IF;

    INSERT INTO auditoria_caja (
        tabla, operacion, id_registro, descripcion,
        id_usuario, usuario, nombre_usuario,
        origen, equipo, aplicacion, usuario_bd,
        total_anterior, total_nuevo, diferencia, motivo,
        datos_anteriores, datos_nuevos)
    VALUES (
        TG_TABLE_NAME, TG_OP, v_id, v_descripcion,
        v_id_usuario, v_usuario, v_nombre,
        caja_audit_contexto('app.origen'),
        host(inet_client_addr()),
        nullif(btrim(current_setting('application_name')), ''),
        session_user,
        v_total_ant, v_total_nue,
        CASE WHEN v_total_ant IS NULL AND v_total_nue IS NULL THEN NULL
             ELSE COALESCE(v_total_nue, 0) - COALESCE(v_total_ant, 0) END,
        caja_audit_contexto('app.motivo'),
        v_ant, v_nue)
    RETURNING id INTO v_id_auditoria;

    -- Detalle campo por campo.
    IF TG_OP = 'UPDATE' THEN
        INSERT INTO auditoria_caja_campos (id_auditoria, campo, valor_anterior, valor_nuevo,
                                           etiqueta_anterior, etiqueta_nueva)
        SELECT v_id_auditoria, a.key, a.value, v_nue ->> a.key,
               caja_audit_etiqueta(TG_TABLE_NAME, a.key, a.value),
               caja_audit_etiqueta(TG_TABLE_NAME, a.key, v_nue ->> a.key)
        FROM json_each_text(v_ant) a
        WHERE a.value IS DISTINCT FROM (v_nue ->> a.key);

    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO auditoria_caja_campos (id_auditoria, campo, valor_anterior, valor_nuevo,
                                           etiqueta_anterior, etiqueta_nueva)
        SELECT v_id_auditoria, a.key, NULL, a.value,
               NULL, caja_audit_etiqueta(TG_TABLE_NAME, a.key, a.value)
        FROM json_each_text(v_nue) a
        WHERE a.value IS NOT NULL;

    ELSE  -- DELETE: se guarda todo lo que se llevo por delante
        INSERT INTO auditoria_caja_campos (id_auditoria, campo, valor_anterior, valor_nuevo,
                                           etiqueta_anterior, etiqueta_nueva)
        SELECT v_id_auditoria, a.key, a.value, NULL,
               caja_audit_etiqueta(TG_TABLE_NAME, a.key, a.value), NULL
        FROM json_each_text(v_ant) a
        WHERE a.value IS NOT NULL;
    END IF;

    RETURN NULL;   -- trigger AFTER: el valor de retorno se ignora
END;
$$ LANGUAGE plpgsql;

-- 4. Enganche de los triggers en las tablas del modulo ------------------------
--    DROP + CREATE para que re-ejecutar el script no duplique triggers.

DROP TRIGGER IF EXISTS trg_auditoria_ingresos ON ingresos;
CREATE TRIGGER trg_auditoria_ingresos
    AFTER INSERT OR UPDATE OR DELETE ON ingresos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

DROP TRIGGER IF EXISTS trg_auditoria_egresos ON egresos;
CREATE TRIGGER trg_auditoria_egresos
    AFTER INSERT OR UPDATE OR DELETE ON egresos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

DROP TRIGGER IF EXISTS trg_auditoria_transferencias ON transferencias;
CREATE TRIGGER trg_auditoria_transferencias
    AFTER INSERT OR UPDATE OR DELETE ON transferencias
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

DROP TRIGGER IF EXISTS trg_auditoria_fondos ON fondos;
CREATE TRIGGER trg_auditoria_fondos
    AFTER INSERT OR UPDATE OR DELETE ON fondos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

DROP TRIGGER IF EXISTS trg_auditoria_cuentas_ingresos ON cuentas_ingresos;
CREATE TRIGGER trg_auditoria_cuentas_ingresos
    AFTER INSERT OR UPDATE OR DELETE ON cuentas_ingresos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

DROP TRIGGER IF EXISTS trg_auditoria_cuentas_egresos ON cuentas_egresos;
CREATE TRIGGER trg_auditoria_cuentas_egresos
    AFTER INSERT OR UPDATE OR DELETE ON cuentas_egresos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

DROP TRIGGER IF EXISTS trg_auditoria_fotos_registros ON fotos_registros;
CREATE TRIGGER trg_auditoria_fotos_registros
    AFTER INSERT OR UPDATE OR DELETE ON fotos_registros
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja();

-- 5. La auditoria es de solo agregar -----------------------------------------

CREATE OR REPLACE FUNCTION fn_auditoria_caja_inmutable()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La auditoria de caja es de solo lectura: no se permite % sobre %',
        TG_OP, TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditoria_caja_inmutable ON auditoria_caja;
CREATE TRIGGER trg_auditoria_caja_inmutable
    BEFORE UPDATE OR DELETE ON auditoria_caja
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja_inmutable();

DROP TRIGGER IF EXISTS trg_auditoria_caja_truncate ON auditoria_caja;
CREATE TRIGGER trg_auditoria_caja_truncate
    BEFORE TRUNCATE ON auditoria_caja
    FOR EACH STATEMENT EXECUTE PROCEDURE fn_auditoria_caja_inmutable();

DROP TRIGGER IF EXISTS trg_auditoria_campos_inmutable ON auditoria_caja_campos;
CREATE TRIGGER trg_auditoria_campos_inmutable
    BEFORE UPDATE OR DELETE ON auditoria_caja_campos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_caja_inmutable();

DROP TRIGGER IF EXISTS trg_auditoria_campos_truncate ON auditoria_caja_campos;
CREATE TRIGGER trg_auditoria_campos_truncate
    BEFORE TRUNCATE ON auditoria_caja_campos
    FOR EACH STATEMENT EXECUTE PROCEDURE fn_auditoria_caja_inmutable();

-- 6. Vista de lectura comoda (base del reporte futuro) ------------------------
--    No se muestra en ningun formulario todavia; sirve para consultar desde
--    pgAdmin cuando haya que investigar un descuadre.

CREATE OR REPLACE VIEW v_auditoria_caja AS
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
FROM auditoria_caja a;

COMMIT;

-- =============================================================================
-- CONSULTAS DE EJEMPLO (para el dia que se haga el reporte)
--
--   -- Todo lo que paso hoy en caja, lo mas reciente arriba
--   SELECT * FROM v_auditoria_caja WHERE fecha = current_date ORDER BY id DESC;
--
--   -- Historial completo de un ingreso puntual
--   SELECT * FROM v_auditoria_caja WHERE tabla = 'ingresos' AND id_registro = 142
--   ORDER BY id;
--
--   -- Borrados de dinero (lo mas delicado)
--   SELECT fecha, hora, usuario_visible, tabla, id_registro, total_anterior, origen
--   FROM v_auditoria_caja
--   WHERE operacion = 'DELETE' AND tabla IN ('ingresos','egresos','transferencias')
--   ORDER BY id DESC;
--
--   -- Cambios de valor en registros ya guardados
--   SELECT fecha, hora, usuario_visible, tabla, id_registro, total_anterior, total_nuevo, diferencia
--   FROM v_auditoria_caja
--   WHERE operacion = 'UPDATE' AND total_anterior IS DISTINCT FROM total_nuevo
--   ORDER BY id DESC;
--
--   -- Detalle campo por campo de una operacion
--   SELECT campo, COALESCE(etiqueta_anterior, valor_anterior) AS antes,
--          COALESCE(etiqueta_nueva, valor_nuevo) AS despues
--   FROM auditoria_caja_campos WHERE id_auditoria = 1;
-- =============================================================================
