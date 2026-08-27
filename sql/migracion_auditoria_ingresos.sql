-- =============================================================================
-- AUDITORIA DE INGRESOS DEL MODULO PRECIOS
-- Tabla auditada: ingresos_productos_cabecera (flujo Recibido/Ingresado/Precios).
-- Idempotente (re-ejecutable). Compatible PostgreSQL 9.4 (y superiores).
--
-- QUE HACE
--   Deja registro INMUTABLE de TODO cambio (INSERT / UPDATE / DELETE) sobre la
--   cabecera de un ingreso, con la fecha y HORA REALES del servidor (now()), el
--   usuario del sistema, el tipo de operacion y, campo por campo, el valor
--   anterior y el nuevo. En especial deja el rastro de CADA cambio de estado
--   (Recibido -> Ingresado -> Precios) con su momento exacto.
--
--   La captura es por TRIGGER de base de datos, no por codigo Java: cualquier
--   ruta que toque la tabla queda auditada (el formulario, el borrado generico
--   DB_consultas_R_D.eliminar(), un script SQL manual, pgAdmin, etc.).
--
-- TABLAS NUEVAS
--   auditoria_ingresos         una fila por operacion (el "que paso" y "cuando")
--   auditoria_ingresos_campos  una fila por campo que cambio (el "que valor")
--
--   Las lee el panel de historial del listado de ingresos (frm_ingresos_precios)
--   al seleccionar un registro.
--
-- USUARIO DEL SISTEMA
--   La aplicacion publica el usuario logueado en cada conexion con
--   set_config('app.id_usuario', ...) desde conexiondb.AuditoriaCaja (se aplica
--   en DB_consultas_R_D.getConexion() para TODAS las conexiones). Si por alguna
--   razon no llega, el trigger cae al id_user de la propia fila.
--
-- INMUTABILIDAD
--   Un trigger bloquea UPDATE / DELETE / TRUNCATE sobre las tablas de auditoria:
--   son de solo agregar. Para depurar historico antiguo (solo el administrador
--   de la base, a conciencia):
--       ALTER TABLE auditoria_ingresos        DISABLE TRIGGER trg_auditoria_ingresos_inmutable;
--       ALTER TABLE auditoria_ingresos_campos DISABLE TRIGGER trg_auditoria_ingresos_campos_inmutable;
--       DELETE FROM auditoria_ingresos WHERE fecha < DATE '2024-01-01';
--       ALTER TABLE auditoria_ingresos        ENABLE  TRIGGER trg_auditoria_ingresos_inmutable;
--       ALTER TABLE auditoria_ingresos_campos ENABLE  TRIGGER trg_auditoria_ingresos_campos_inmutable;
-- =============================================================================

BEGIN;

-- 1. Tablas de auditoria ------------------------------------------------------

CREATE TABLE IF NOT EXISTS auditoria_ingresos (
    id bigserial NOT NULL,
    -- CUANDO (hora real del servidor, no la del formulario)
    fecha_hora timestamp without time zone NOT NULL DEFAULT now(),
    fecha date NOT NULL DEFAULT current_date,
    hora character varying(8) NOT NULL DEFAULT to_char(now(), 'HH24:MI:SS'),
    -- QUE
    tabla character varying(40) NOT NULL,        -- ingresos_productos_cabecera
    operacion character varying(10) NOT NULL,    -- INSERT | UPDATE | DELETE
    id_registro integer,                         -- id del ingreso afectado
    descripcion character varying,               -- resumen legible ("Cambio de estado: Recibido -> Ingresado")
    -- ESTADO (para filtrar/leer el flujo sin abrir el json)
    estado_anterior integer,
    estado_nuevo integer,
    -- QUIEN
    id_usuario integer,                          -- users.id (contexto de la app)
    usuario character varying(100),              -- users.user_name congelado al momento
    nombre_usuario character varying(150),       -- users.nombre congelado al momento
    -- DESDE DONDE
    origen character varying(80),                -- formulario/accion (app.origen)
    equipo character varying(60),                -- IP del cliente
    aplicacion character varying(80),            -- application_name de la conexion
    usuario_bd character varying(60),            -- rol de PostgreSQL
    -- LA FOTO COMPLETA DE LA FILA
    datos_anteriores json,
    datos_nuevos json,
    CONSTRAINT pk_auditoria_ingresos PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS auditoria_ingresos_campos (
    id bigserial NOT NULL,
    id_auditoria bigint NOT NULL,
    campo character varying(40) NOT NULL,
    valor_anterior text,
    valor_nuevo text,
    -- Traduccion legible de llaves foraneas y del estado
    -- (estado: 0 -> "Recibido"; id_proveedor: 12 -> "Distribuidora XYZ")
    etiqueta_anterior character varying(200),
    etiqueta_nueva character varying(200),
    CONSTRAINT pk_auditoria_ingresos_campos PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_ingresos_campos FOREIGN KEY (id_auditoria)
        REFERENCES auditoria_ingresos (id) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE CASCADE
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_auditoria_ingresos_registro') THEN
        CREATE INDEX idx_auditoria_ingresos_registro ON auditoria_ingresos(tabla, id_registro);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_auditoria_ingresos_fecha') THEN
        CREATE INDEX idx_auditoria_ingresos_fecha ON auditoria_ingresos(fecha);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_auditoria_ingresos_fecha_hora') THEN
        CREATE INDEX idx_auditoria_ingresos_fecha_hora ON auditoria_ingresos(fecha_hora);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_auditoria_ingresos_usuario') THEN
        CREATE INDEX idx_auditoria_ingresos_usuario ON auditoria_ingresos(id_usuario);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_auditoria_ingresos_campos_aud') THEN
        CREATE INDEX idx_auditoria_ingresos_campos_aud ON auditoria_ingresos_campos(id_auditoria);
    END IF;
END $$;

-- 2. Funciones auxiliares -----------------------------------------------------

-- Lee una variable de sesion publicada por la aplicacion. En 9.4
-- current_setting() revienta si la variable no existe: por eso el EXCEPTION.
CREATE OR REPLACE FUNCTION ing_audit_contexto(p_clave text)
RETURNS text AS $$
BEGIN
    RETURN nullif(btrim(current_setting(p_clave)), '');
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- Igual que la anterior pero devolviendo entero (NULL si no es numerica).
CREATE OR REPLACE FUNCTION ing_audit_contexto_int(p_clave text)
RETURNS integer AS $$
DECLARE
    v text;
BEGIN
    v := ing_audit_contexto(p_clave);
    IF v IS NULL OR v !~ '^[0-9]+$' THEN
        RETURN NULL;
    END IF;
    RETURN v::integer;
END;
$$ LANGUAGE plpgsql STABLE;

-- Traduccion del estado entero del flujo Precios a su etiqueta.
CREATE OR REPLACE FUNCTION ing_audit_estado_label(p_valor text)
RETURNS text AS $$
BEGIN
    IF p_valor IS NULL OR p_valor !~ '^[0-9]+$' THEN
        RETURN NULL;
    END IF;
    RETURN CASE p_valor::integer
        WHEN 0 THEN 'Recibido'
        WHEN 1 THEN 'Ingresado'
        WHEN 2 THEN 'Precios'
        ELSE 'Estado ' || p_valor
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Traduce el valor de un campo (llave foranea o estado) a algo legible, para
-- que el historial no muestre "estado: 0 -> 1" sino "Recibido -> Ingresado".
CREATE OR REPLACE FUNCTION ing_audit_etiqueta(p_campo text, p_valor text)
RETURNS text AS $$
DECLARE
    v_id integer;
    v_nombre text;
BEGIN
    IF p_valor IS NULL THEN
        RETURN NULL;
    END IF;

    IF p_campo = 'estado' THEN
        RETURN ing_audit_estado_label(p_valor);
    END IF;

    IF p_valor !~ '^[0-9]+$' THEN
        RETURN NULL;
    END IF;
    v_id := p_valor::integer;

    IF p_campo IN ('id_proveedor', 'id_transportador') THEN
        SELECT c.nombre INTO v_nombre FROM contactos c WHERE c.id = v_id;
    ELSIF p_campo = 'id_user' THEN
        SELECT u.user_name INTO v_nombre FROM users u WHERE u.id = v_id;
    ELSIF p_campo = 'id_bodega' THEN
        SELECT b.nombre INTO v_nombre FROM bodegas b WHERE b.id = v_id;
    END IF;

    RETURN v_nombre;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- 3. Trigger de auditoria -----------------------------------------------------

CREATE OR REPLACE FUNCTION fn_auditoria_ingresos()
RETURNS trigger AS $$
DECLARE
    v_ant json;
    v_nue json;
    v_id integer;
    v_id_usuario integer;
    v_usuario character varying(100);
    v_nombre character varying(150);
    v_estado_ant integer;
    v_estado_nue integer;
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
    v_estado_ant := (v_ant ->> 'estado')::integer;
    v_estado_nue := (v_nue ->> 'estado')::integer;

    -- Usuario: primero el contexto publicado por la app; si no llego, el dueño
    -- del documento (id_user de la fila).
    v_id_usuario := ing_audit_contexto_int('app.id_usuario');
    IF v_id_usuario IS NULL THEN
        v_id_usuario := COALESCE((v_nue ->> 'id_user')::integer, (v_ant ->> 'id_user')::integer);
    END IF;
    IF v_id_usuario IS NOT NULL THEN
        SELECT u.user_name, u.nombre INTO v_usuario, v_nombre
        FROM users u WHERE u.id = v_id_usuario;
    END IF;
    IF v_usuario IS NULL THEN
        v_usuario := ing_audit_contexto('app.usuario');
    END IF;

    -- Resumen legible del evento.
    IF TG_OP = 'INSERT' THEN
        v_descripcion := 'Creacion del ingreso #' || COALESCE(v_id::text, '?')
                         || ' - estado ' || COALESCE(ing_audit_estado_label(v_estado_nue::text), 'NULO');
    ELSIF TG_OP = 'DELETE' THEN
        v_descripcion := 'Eliminacion del ingreso #' || COALESCE(v_id::text, '?');
    ELSE  -- UPDATE
        IF v_estado_ant IS DISTINCT FROM v_estado_nue THEN
            v_descripcion := 'Cambio de estado: '
                             || COALESCE(ing_audit_estado_label(v_estado_ant::text), 'NULO')
                             || ' -> ' || COALESCE(ing_audit_estado_label(v_estado_nue::text), 'NULO');
        ELSE
            v_descripcion := 'Modificacion del ingreso #' || COALESCE(v_id::text, '?')
                             || ' (' || v_cambios || ' campo(s))';
        END IF;
    END IF;

    INSERT INTO auditoria_ingresos (
        tabla, operacion, id_registro, descripcion,
        estado_anterior, estado_nuevo,
        id_usuario, usuario, nombre_usuario,
        origen, equipo, aplicacion, usuario_bd,
        datos_anteriores, datos_nuevos)
    VALUES (
        TG_TABLE_NAME, TG_OP, v_id, v_descripcion,
        v_estado_ant, v_estado_nue,
        v_id_usuario, v_usuario, v_nombre,
        ing_audit_contexto('app.origen'),
        host(inet_client_addr()),
        nullif(btrim(current_setting('application_name')), ''),
        session_user,
        v_ant, v_nue)
    RETURNING id INTO v_id_auditoria;

    -- Detalle campo por campo.
    IF TG_OP = 'UPDATE' THEN
        INSERT INTO auditoria_ingresos_campos (id_auditoria, campo, valor_anterior, valor_nuevo,
                                               etiqueta_anterior, etiqueta_nueva)
        SELECT v_id_auditoria, a.key, a.value, v_nue ->> a.key,
               ing_audit_etiqueta(a.key, a.value),
               ing_audit_etiqueta(a.key, v_nue ->> a.key)
        FROM json_each_text(v_ant) a
        WHERE a.value IS DISTINCT FROM (v_nue ->> a.key);

    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO auditoria_ingresos_campos (id_auditoria, campo, valor_anterior, valor_nuevo,
                                               etiqueta_anterior, etiqueta_nueva)
        SELECT v_id_auditoria, a.key, NULL, a.value,
               NULL, ing_audit_etiqueta(a.key, a.value)
        FROM json_each_text(v_nue) a
        WHERE a.value IS NOT NULL;

    ELSE  -- DELETE
        INSERT INTO auditoria_ingresos_campos (id_auditoria, campo, valor_anterior, valor_nuevo,
                                               etiqueta_anterior, etiqueta_nueva)
        SELECT v_id_auditoria, a.key, a.value, NULL,
               ing_audit_etiqueta(a.key, a.value), NULL
        FROM json_each_text(v_ant) a
        WHERE a.value IS NOT NULL;
    END IF;

    RETURN NULL;   -- trigger AFTER: el valor de retorno se ignora
END;
$$ LANGUAGE plpgsql;

-- 4. Enganche del trigger -----------------------------------------------------
--    DROP + CREATE para que re-ejecutar el script no duplique el trigger.

DROP TRIGGER IF EXISTS trg_auditoria_ingresos_productos ON ingresos_productos_cabecera;
CREATE TRIGGER trg_auditoria_ingresos_productos
    AFTER INSERT OR UPDATE OR DELETE ON ingresos_productos_cabecera
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_ingresos();

-- 5. La auditoria es de solo agregar -----------------------------------------

CREATE OR REPLACE FUNCTION fn_auditoria_ingresos_inmutable()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La auditoria de ingresos es de solo lectura: no se permite % sobre %',
        TG_OP, TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditoria_ingresos_inmutable ON auditoria_ingresos;
CREATE TRIGGER trg_auditoria_ingresos_inmutable
    BEFORE UPDATE OR DELETE ON auditoria_ingresos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_ingresos_inmutable();

DROP TRIGGER IF EXISTS trg_auditoria_ingresos_truncate ON auditoria_ingresos;
CREATE TRIGGER trg_auditoria_ingresos_truncate
    BEFORE TRUNCATE ON auditoria_ingresos
    FOR EACH STATEMENT EXECUTE PROCEDURE fn_auditoria_ingresos_inmutable();

DROP TRIGGER IF EXISTS trg_auditoria_ingresos_campos_inmutable ON auditoria_ingresos_campos;
CREATE TRIGGER trg_auditoria_ingresos_campos_inmutable
    BEFORE UPDATE OR DELETE ON auditoria_ingresos_campos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_ingresos_inmutable();

DROP TRIGGER IF EXISTS trg_auditoria_ingresos_campos_truncate ON auditoria_ingresos_campos;
CREATE TRIGGER trg_auditoria_ingresos_campos_truncate
    BEFORE TRUNCATE ON auditoria_ingresos_campos
    FOR EACH STATEMENT EXECUTE PROCEDURE fn_auditoria_ingresos_inmutable();

-- 6. Vista de lectura comoda (base del panel de historial) --------------------

CREATE OR REPLACE VIEW v_auditoria_ingresos AS
SELECT a.id,
       a.fecha,
       a.hora,
       a.fecha_hora,
       a.operacion,
       a.id_registro,
       a.descripcion,
       ing_audit_estado_label(a.estado_anterior::text) AS estado_anterior,
       ing_audit_estado_label(a.estado_nuevo::text)    AS estado_nuevo,
       COALESCE(a.nombre_usuario, a.usuario, a.usuario_bd) AS usuario_visible,
       a.origen,
       a.equipo,
       (SELECT string_agg(c.campo || ': ' ||
                          COALESCE(c.etiqueta_anterior, c.valor_anterior, '(vacio)') || ' -> ' ||
                          COALESCE(c.etiqueta_nueva,   c.valor_nuevo,    '(vacio)'), ' | ')
          FROM auditoria_ingresos_campos c
         WHERE c.id_auditoria = a.id
           AND c.campo NOT IN ('hora')) AS cambios
FROM auditoria_ingresos a;

COMMIT;

-- =============================================================================
-- CONSULTAS DE EJEMPLO
--
--   -- Historial completo de un ingreso puntual (lo mas antiguo arriba)
--   SELECT fecha, hora, usuario_visible, descripcion
--   FROM v_auditoria_ingresos WHERE id_registro = 142 ORDER BY id;
--
--   -- Solo los cambios de estado del dia
--   SELECT fecha, hora, id_registro, estado_anterior, estado_nuevo, usuario_visible
--   FROM v_auditoria_ingresos
--   WHERE operacion = 'UPDATE' AND estado_anterior IS DISTINCT FROM estado_nuevo
--     AND fecha = current_date
--   ORDER BY id DESC;
-- =============================================================================
