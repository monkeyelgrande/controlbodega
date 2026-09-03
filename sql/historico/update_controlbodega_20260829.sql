-- =============================================================================
-- LINEA BASE 2026-08-29   -   puesta al dia, se corre UNA sola vez
--
-- ARCHIVADO. Este archivo dejo de ser el actualizador del sistema el
-- 2026-08-30: de ahi en adelante los cambios nuevos van en
-- sql/update_controlbodega.sql, que arranca desde esta linea base.
--
-- A DIFERENCIA DEL RESTO DE sql/historico/, ESTE SI SE CORRE: es la puesta
-- al dia de una base que se quedo atras de la linea base del 2026-08-29.
-- Corrilo una vez y despues sigue con sql/update_controlbodega.sql, que
-- verifica esta misma linea base antes de aplicar nada.
--
-- Sigue siendo acumulativo e idempotente. Se corre tal cual sobre cualquier
-- base de controlbodega y la deja en la linea base del 2026-08-29:
--     bodega_nuevo           (AGROINSUMOS / desarrollo)
--     bodega_tecnirepuestos  (TECNIREPUESTOS DEL SUR)
--     cualquier cliente nuevo que se monte en adelante
--
-- Es RE-EJECUTABLE: correrlo dos veces no hace dano y no duplica nada. Cada
-- objeto se crea solo si falta; los que ya existen se dejan como estan.
--
-- Motor destino: PostgreSQL 9.4 (por eso no se usa ADD COLUMN IF NOT EXISTS
-- ni CREATE INDEX IF NOT EXISTS: se verifica a mano con DO-blocks).
--
-- -----------------------------------------------------------------------------
-- REGLA DE TRABAJO (para cada cambio nuevo del sistema)
--
--   1. El cambio de esquema o de catalogo se agrega al final, en la PARTE B,
--      como un incremento nuevo con su fecha. NO se crea un archivo de
--      migracion aparte.
--   2. El MISMO cambio se refleja en sql/controlbodega.sql, que es el script
--      de construccion desde cero para una empresa nueva. Los dos archivos
--      viajan siempre juntos, en el mismo commit.
--   3. Aqui solo entra lo que es seguro correr en cualquier base:
--          tablas, columnas, indices, llaves, funciones, triggers, vistas
--          catalogos del sistema (perfiles, modulos, opciones de permisos)
--          semillas minimas, y solo cuando la tabla esta vacia
--      NO entra nada que toque datos del negocio de un cliente: cargas de
--      inventario, deduplicaciones, unificaciones de usuarios, correcciones
--      puntuales. Eso sigue siendo un script propio en sql/, se corre a mano
--      una sola vez y se documenta en la PARTE C.
--
-- -----------------------------------------------------------------------------
-- ESTRUCTURA
--
--   PARTE A  LINEA BASE (2026-08-29). Congelada: lleva cualquier base al
--            esquema de esa fecha. No se edita mas; lo nuevo va a la PARTE B.
--   PARTE B  INCREMENTOS, en orden cronologico. Cada uno con su fecha, su
--            propia transaccion y una nota de que agrega y por que.
--   PARTE C  PASOS QUE REQUIEREN DECISION. Fuera de transaccion y COMENTADOS:
--            modo de precios, modulos a apagar, dedup, datos por cliente.
--   PARTE D  VERIFICACION de solo lectura. No cambia nada.
--
-- -----------------------------------------------------------------------------
-- COMO EJECUTAR
--
--   "C:\Program Files\PostgreSQL\9.4\bin\psql.exe" -h <IP> -p 5432 ^
--       -U postgres -d <base> -v ON_ERROR_STOP=1 ^
--       -f sql\update_controlbodega.sql
--
--   >> HAZ UN BACKUP (pg_dump) DE LA BASE ANTES DE CORRERLO. <<
--   >> CORRELO CON LOS USUARIOS FUERA DE LA APP (crea tablas y toma locks). <<
-- 
-- QUE SUPONE: que la base ya es una controlbodega, es decir que ya existen
-- las tablas del nucleo (productos, contactos, users, perfiles, bodegas,
-- facturas_cabeceras, detalle_factura, stock, movimientos_inventario, ...).
-- Una empresa NUEVA no se monta con este archivo sino con sql/controlbodega.sql,
-- que crea todo desde cero; despues, esta actualizacion queda en no-op.
-- =============================================================================


-- #############################################################################
-- ##  PARTE A   LINEA BASE 2026-08-29   (CONGELADA - no editar)              ##
-- #############################################################################
--
-- Reemplaza la aplicacion encadenada de estas migraciones historicas
-- (todas movidas a sql/historico/):
--     migracion_permisos.sql / migracion_permisos_compras.sql
--     migracion_compras_pipeline.sql / migracion_comisiones_precios.sql
--     migracion_permisos_unir_ordenes.sql / migracion_permisos_ordenes_editar.sql
--     migracion_bodega_colores.sql / migracion_bodegas_orden_automatica.sql
--     migracion_fusion_agro.sql / migracion_bodega_entrega_automatica.sql
--     migracion_permisos_entrega_masiva.sql / asignar_bodegas_entrega_funcion.sql
--     actualizacion_produccion_20260723.sql (modulos + Creditos + Caja)
--     migracion_modulos.sql / migracion_modulo_creditos.sql
--     migracion_modulo_caja.sql / migracion_caja_dos.sql
--     migracion_recibo_caja.sql / migracion_auditoria_ingresos.sql
--     migracion_permisos_anular_orden.sql / migracion_permisos_reimprimir_orden.sql
--     migracion_modo_precios.sql
--
-- Cubre ademas lo que exige wo-printer (proyecto aparte que escribe sobre esta
-- misma base): fases 1.3, 6.1, 6.2, 9, 10 y 11 de su migracion_produccion.sql
-- mas el reparto por unidades de entrega.
--
-- QUE HACE (todo aditivo e idempotente):
--   1. Habilita las extensiones pg_trgm y unaccent.
--   2. Agrega 31 columnas nuevas a 6 tablas existentes (productos, contactos,
--      configuraciones, users, bodegas, facturas_impresas) y ensancha
--      contactos.contacto 20->200.
--   3. Crea 36 tablas nuevas con sus secuencias, llaves e indices (permisos,
--      Compras, Precios, Creditos, Caja, Caja Dos, auditoria de ingresos,
--      unidades de entrega, dedup de contactos).
--   4. Crea 11 funciones, 5 triggers y la vista v_auditoria_ingresos.
--   5. Agrega indices y FK que faltaban en tablas ya existentes, renombra
--      idx_detalle_novedad -> idx_detalle_factura_novedad y resincroniza las
--      secuencias que quedaron atras del MAX(id).
--   6. Siembra SOLO datos de configuracion, con guardas WHERE NOT EXISTS:
--        perfiles 6-9, modulos (5), opciones (60), perfil_opciones (139).
--
-- QUE NO HACE: ningun UPDATE ni DELETE sobre filas existentes (salvo dejar
-- configuraciones.modo_precios en AGRO cuando la columna nace en NULL); no
-- siembra datos de negocio; no copia permisos por usuario entre bases.
-- =============================================================================


BEGIN;

-- -----------------------------------------------------------------------------
-- 0. EXTENSIONES
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;


-- -----------------------------------------------------------------------------
-- 1. COLUMNAS NUEVAS EN TABLAS EXISTENTES
--    PostgreSQL 9.4 no tiene ADD COLUMN IF NOT EXISTS -> DO-blocks.
-- -----------------------------------------------------------------------------

-- 1.1 productos: precios del modulo Precios (fusion agro) + margenes 2 y 3 del
--     modo TECNI + peso unitario (pipeline de compras).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='productos' AND column_name='venta') THEN
        ALTER TABLE productos ADD COLUMN venta double precision;
        ALTER TABLE productos ADD COLUMN valor_desc_1 double precision;
        ALTER TABLE productos ADD COLUMN valor_desc_2 double precision;
        ALTER TABLE productos ADD COLUMN valor_s_y_t double precision;
        ALTER TABLE productos ADD COLUMN valor_credito double precision;
        ALTER TABLE productos ADD COLUMN iva double precision;
        ALTER TABLE productos ADD COLUMN porcentaje_utilidad double precision;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='productos' AND column_name='porcentaje_utilidad2') THEN
        ALTER TABLE productos ADD COLUMN porcentaje_utilidad2 double precision;
        ALTER TABLE productos ADD COLUMN porcentaje_utilidad3 double precision;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='productos' AND column_name='peso_unitario') THEN
        ALTER TABLE productos ADD COLUMN peso_unitario numeric;
    END IF;
END $$;

-- 1.2 contactos: traza de origen (fusion agro) + campos de cartera (Creditos).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='contactos' AND column_name='origen') THEN
        ALTER TABLE contactos ADD COLUMN origen character varying(20);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='contactos' AND column_name='cupo') THEN
        ALTER TABLE contactos ADD COLUMN cupo double precision;
        ALTER TABLE contactos ADD COLUMN interes double precision;
        ALTER TABLE contactos ADD COLUMN empleado integer;
        ALTER TABLE contactos ADD COLUMN antiguo integer;
        ALTER TABLE contactos ADD COLUMN predeterminado integer;
    END IF;
    -- agro guarda hasta 95 caracteres en "contacto"; en bodega era varchar(20)
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='contactos' AND column_name='contacto'
                 AND character_maximum_length < 200) THEN
        ALTER TABLE contactos ALTER COLUMN contacto TYPE character varying(200);
    END IF;
END $$;

-- 1.3 configuraciones: porcentajes de Precios, modo de precios por empresa,
--     rutas/impresora de Creditos e interruptor de ingreso de dinero de Caja.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='configuraciones' AND column_name='porcentaje_operacion') THEN
        ALTER TABLE configuraciones ADD COLUMN porcentaje_operacion double precision;
        ALTER TABLE configuraciones ADD COLUMN porcentaje_s_y_t double precision;
        ALTER TABLE configuraciones ADD COLUMN porcentaje_credito double precision;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='configuraciones' AND column_name='modo_precios') THEN
        ALTER TABLE configuraciones ADD COLUMN modo_precios character varying(10) DEFAULT 'AGRO';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='configuraciones' AND column_name='ruta_imagenes') THEN
        ALTER TABLE configuraciones ADD COLUMN ruta_imagenes character varying;
        ALTER TABLE configuraciones ADD COLUMN tipo_impresora character varying;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='configuraciones' AND column_name='ingreso_dinero') THEN
        ALTER TABLE configuraciones ADD COLUMN ingreso_dinero integer;
    END IF;
END $$;

-- La fila existente queda explicitamente en AGRO (ver seccion 8 para TECNI).
UPDATE configuraciones SET modo_precios = 'AGRO' WHERE modo_precios IS NULL;

-- 1.4 users: rol del modulo Precios, aprobacion de compras y panel de
--     notificaciones. Solo 11 filas: la reescritura del NOT NULL es trivial.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='users' AND column_name='rol_precios') THEN
        ALTER TABLE users ADD COLUMN rol_precios integer NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='users' AND column_name='aprueba_compras') THEN
        ALTER TABLE users ADD COLUMN aprueba_compras boolean NOT NULL DEFAULT false;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='users' AND column_name='panel_notificaciones') THEN
        ALTER TABLE users ADD COLUMN panel_notificaciones boolean DEFAULT false;
    END IF;
END $$;

-- 1.5 bodegas: color de identidad, entrega masiva automatica y generacion
--     automatica de orden.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='bodegas' AND column_name='color') THEN
        ALTER TABLE bodegas ADD COLUMN color character varying(7);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='bodegas' AND column_name='entrega_automatica') THEN
        ALTER TABLE bodegas ADD COLUMN entrega_automatica boolean NOT NULL DEFAULT false;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='bodegas' AND column_name='genera_orden_automatica') THEN
        ALTER TABLE bodegas ADD COLUMN genera_orden_automatica boolean NOT NULL DEFAULT false;
    END IF;
END $$;

-- 1.6 facturas_impresas: NIT del cliente. La escribe wo-printer cuando la
--     factura de WorldOffice viene en HTML o RTF (el Excel no trae el NIT, ahi
--     queda NULL). Sin esta columna wo-printer falla al guardar la factura:
--       ERROR: no existe la columna nit_cliente en la relacion facturas_impresas
--     Equivale a la FASE 11 de ../wo-printer/sql/migracion_produccion.sql.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='facturas_impresas' AND column_name='nit_cliente') THEN
        ALTER TABLE facturas_impresas ADD COLUMN nit_cliente character varying(50);
    END IF;
END $$;


-- -----------------------------------------------------------------------------
-- 2. TABLAS NUEVAS (36), SECUENCIAS, LLAVES E INDICES
--    Extraidas del esquema vivo de bodega_nuevo y envueltas en guardas.
-- -----------------------------------------------------------------------------

-- 2.1 Secuencias
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='abonos_cabeceras_id_seq') THEN
        CREATE SEQUENCE abonos_cabeceras_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='abonos_id_seq') THEN
        CREATE SEQUENCE abonos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='auditoria_ingresos_campos_id_seq') THEN
        CREATE SEQUENCE auditoria_ingresos_campos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='auditoria_ingresos_id_seq') THEN
        CREATE SEQUENCE auditoria_ingresos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='creditos_id_seq') THEN
        CREATE SEQUENCE creditos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='cuentas_egresos_id_seq') THEN
        CREATE SEQUENCE cuentas_egresos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='cuentas_id_seq') THEN
        CREATE SEQUENCE cuentas_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='cuentas_ingresos_id_seq') THEN
        CREATE SEQUENCE cuentas_ingresos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='dedup_contactos_log_id_seq') THEN
        CREATE SEQUENCE dedup_contactos_log_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='descuentos_id_seq') THEN
        CREATE SEQUENCE descuentos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='egresos_id_seq') THEN
        CREATE SEQUENCE egresos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='fondos_id_seq') THEN
        CREATE SEQUENCE fondos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='fotos_registros_id_seq') THEN
        CREATE SEQUENCE fotos_registros_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='ingresos_id_seq') THEN
        CREATE SEQUENCE ingresos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='ingresos_productos_cabecera_id_seq') THEN
        CREATE SEQUENCE ingresos_productos_cabecera_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='ingresos_productos_detalle_id_seq') THEN
        CREATE SEQUENCE ingresos_productos_detalle_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='modulos_id_seq') THEN
        CREATE SEQUENCE modulos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='opciones_id_seq') THEN
        CREATE SEQUENCE opciones_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='pagos_ingresos_productos_id_seq') THEN
        CREATE SEQUENCE pagos_ingresos_productos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='producto_proveedores_id_seq') THEN
        CREATE SEQUENCE producto_proveedores_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='productos_unidades_entrega_id_seq') THEN
        CREATE SEQUENCE productos_unidades_entrega_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='tipos_abonos_id_seq') THEN
        CREATE SEQUENCE tipos_abonos_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                   WHERE c.relkind='S' AND n.nspname='public' AND c.relname='transferencias_id_seq') THEN
        CREATE SEQUENCE transferencias_id_seq
            START WITH 1
            INCREMENT BY 1
            NO MINVALUE
            NO MAXVALUE
            CACHE 1;
    END IF;
END $$;

-- 2.2 Tablas
CREATE TABLE IF NOT EXISTS abonos (
    id integer NOT NULL,
    id_cabecera integer NOT NULL,
    id_credito integer NOT NULL,
    abono double precision NOT NULL,
    fecha date NOT NULL,
    hora character varying DEFAULT ''::character varying
);

CREATE TABLE IF NOT EXISTS abonos_cabeceras (
    id integer NOT NULL,
    id_contacto integer NOT NULL,
    id_user integer,
    id_tipo_abono integer,
    total double precision NOT NULL,
    fecha date NOT NULL,
    hora character varying DEFAULT ''::character varying,
    observacion character varying DEFAULT ''::character varying,
    foto character varying DEFAULT ''::character varying,
    pdf character varying DEFAULT ''::character varying
);

CREATE TABLE IF NOT EXISTS auditoria_ingresos (
    id bigint NOT NULL,
    fecha_hora timestamp without time zone DEFAULT now() NOT NULL,
    fecha date DEFAULT ('now'::text)::date NOT NULL,
    hora character varying(8) DEFAULT to_char(now(), 'HH24:MI:SS'::text) NOT NULL,
    tabla character varying(40) NOT NULL,
    operacion character varying(10) NOT NULL,
    id_registro integer,
    descripcion character varying,
    estado_anterior integer,
    estado_nuevo integer,
    id_usuario integer,
    usuario character varying(100),
    nombre_usuario character varying(150),
    origen character varying(80),
    equipo character varying(60),
    aplicacion character varying(80),
    usuario_bd character varying(60),
    datos_anteriores json,
    datos_nuevos json
);

CREATE TABLE IF NOT EXISTS auditoria_ingresos_campos (
    id bigint NOT NULL,
    id_auditoria bigint NOT NULL,
    campo character varying(40) NOT NULL,
    valor_anterior text,
    valor_nuevo text,
    etiqueta_anterior character varying(200),
    etiqueta_nueva character varying(200)
);

CREATE TABLE IF NOT EXISTS comparativos_cabecera (
    id integer NOT NULL,
    numero character varying(50),
    id_sugerido integer,
    id_user integer,
    fecha date,
    hora time without time zone,
    iva_pct numeric(6,4) DEFAULT 0.19 NOT NULL,
    capacidad_camion_ton numeric(10,2) DEFAULT 30,
    estado integer DEFAULT 0 NOT NULL,
    decision character varying(20),
    id_proveedor_unico integer,
    id_user_autoriza integer,
    fecha_autorizacion timestamp without time zone,
    observacion text
);

CREATE TABLE IF NOT EXISTS comparativos_precios (
    id integer NOT NULL,
    id_comparativo integer NOT NULL,
    id_comp_producto integer NOT NULL,
    id_comp_proveedor integer NOT NULL,
    precio_lista numeric(18,4)
);

CREATE TABLE IF NOT EXISTS comparativos_productos (
    id integer NOT NULL,
    id_comparativo integer NOT NULL,
    id_producto integer NOT NULL,
    cantidad numeric(18,4) DEFAULT 0 NOT NULL,
    peso_unitario numeric(18,4),
    posicion integer
);

CREATE TABLE IF NOT EXISTS comparativos_proveedores (
    id integer NOT NULL,
    id_comparativo integer NOT NULL,
    id_proveedor integer NOT NULL,
    descuento_pronto_pago numeric(6,4) DEFAULT 0 NOT NULL,
    condicion_pago character varying(120),
    flete numeric(18,4) DEFAULT 0 NOT NULL,
    posicion integer
);

CREATE TABLE IF NOT EXISTS cotizaciones_compra_cabecera (
    id integer NOT NULL,
    numero character varying(50),
    id_sugerido integer,
    id_proveedor integer NOT NULL,
    id_user integer,
    fecha date,
    hora time without time zone,
    estado integer DEFAULT 0 NOT NULL,
    fecha_envio date,
    fecha_limite date,
    condicion_pago character varying(120),
    validez character varying(120),
    observacion text
);

CREATE TABLE IF NOT EXISTS cotizaciones_compra_detalle (
    id integer NOT NULL,
    id_cotiz_cab integer NOT NULL,
    id_producto integer NOT NULL,
    cantidad numeric(18,4) NOT NULL,
    precio_unitario numeric(18,4),
    iva_pct numeric(6,4),
    plazo_entrega character varying(80),
    observacion text
);

CREATE TABLE IF NOT EXISTS creditos (
    id integer NOT NULL,
    codigo character varying,
    id_contacto integer,
    id_user integer,
    total double precision,
    fecha_creacion date,
    fecha_vencimiento date,
    estado integer,
    interes double precision,
    hora character varying(8),
    descripcion character varying,
    monto_descuento double precision,
    id_cuenta integer,
    foto character varying,
    pdf character varying
);

CREATE TABLE IF NOT EXISTS cuentas (
    id integer NOT NULL,
    nombre character varying
);

CREATE TABLE IF NOT EXISTS cuentas_egresos (
    id integer NOT NULL,
    nombre character varying(50),
    predeterminado integer,
    id_caja integer DEFAULT 1 NOT NULL
);

CREATE TABLE IF NOT EXISTS cuentas_ingresos (
    id integer NOT NULL,
    nombre character varying(50),
    predeterminado integer,
    id_caja integer DEFAULT 1 NOT NULL
);

CREATE TABLE IF NOT EXISTS dedup_contactos_log (
    id integer NOT NULL,
    id_sobreviviente integer NOT NULL,
    id_eliminado integer NOT NULL,
    nombre_eliminado text,
    cedula_eliminado text,
    motivo text,
    fase text,
    fecha timestamp without time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS descuentos (
    id integer NOT NULL,
    tipo integer,
    utilidad double precision,
    descuento double precision
);

CREATE TABLE IF NOT EXISTS egresos (
    id integer NOT NULL,
    id_user integer,
    id_cuenta integer NOT NULL,
    id_cliente integer,
    id_fondo integer,
    descripcion character varying,
    total double precision,
    fecha date,
    hora character varying(8),
    factura_remision integer,
    transferencia integer,
    id_caja integer DEFAULT 1 NOT NULL
);

CREATE TABLE IF NOT EXISTS fondos (
    id integer NOT NULL,
    nombre character varying(100),
    predeterminado integer,
    fisico_digital integer,
    id_caja integer DEFAULT 1 NOT NULL
);

CREATE TABLE IF NOT EXISTS fotos_registros (
    id integer NOT NULL,
    nombre character varying,
    id_registro integer,
    tipo_registro integer
);

CREATE TABLE IF NOT EXISTS ingresos (
    id integer NOT NULL,
    id_user integer,
    id_cuenta integer NOT NULL,
    id_cliente integer,
    id_fondo integer,
    descripcion character varying,
    total double precision,
    fecha date,
    hora character varying(8),
    factura_remision integer,
    transferencia integer,
    recibo_caja integer DEFAULT 0,
    id_caja integer DEFAULT 1 NOT NULL
);

CREATE TABLE IF NOT EXISTS ingresos_productos_cabecera (
    id integer NOT NULL,
    no_factura character varying(100),
    id_proveedor integer,
    id_transportador integer,
    id_user integer,
    total double precision,
    fecha date,
    hora character varying(8),
    estado integer,
    observacion character varying,
    id_bodega integer,
    fecha_vencimiento date,
    enviado_control_bodega boolean DEFAULT false
);

CREATE TABLE IF NOT EXISTS ingresos_productos_detalle (
    id integer NOT NULL,
    id_ingreso_cabecera integer,
    id_producto integer,
    cantidad double precision,
    iva double precision,
    precio_costo double precision,
    venta double precision,
    valor_desc_1 double precision,
    valor_desc_2 double precision,
    valor_s_y_t double precision,
    valor_credito double precision,
    descuento double precision,
    porcentaje_utilidad double precision,
    desc_n_1 double precision,
    desc_n_2 double precision,
    etiquetas character varying,
    id_bodega_control integer
);

CREATE TABLE IF NOT EXISTS modulos (
    id integer NOT NULL,
    clave character varying(50) NOT NULL,
    nombre character varying(100) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);

CREATE TABLE IF NOT EXISTS opciones (
    id integer NOT NULL,
    clave character varying(60) NOT NULL,
    nombre character varying(100) NOT NULL,
    modulo character varying(60) NOT NULL,
    componente character varying(60),
    orden integer DEFAULT 0 NOT NULL
);

CREATE TABLE IF NOT EXISTS ordenes_compra_cabecera (
    id integer NOT NULL,
    numero character varying(50),
    id_user_crea integer NOT NULL,
    fecha date,
    hora time without time zone,
    estado integer DEFAULT 0 NOT NULL,
    observacion text,
    id_bodega integer,
    id_user_aprueba integer,
    fecha_aprobacion timestamp without time zone,
    id_comparativo integer,
    iva_pct numeric(6,4) DEFAULT 0.19,
    condicion_pago character varying(120),
    fecha_entrega date,
    lugar_entrega character varying(200)
);

CREATE TABLE IF NOT EXISTS ordenes_compra_detalle (
    id integer NOT NULL,
    id_orden_cabecera integer NOT NULL,
    id_producto integer NOT NULL,
    cantidad numeric(18,4) NOT NULL,
    id_proveedor integer,
    precio_unitario numeric(18,4),
    observacion text
);

CREATE TABLE IF NOT EXISTS pagos_ingresos_productos (
    id integer NOT NULL,
    id_ingreso_productos_cabecera integer,
    total double precision,
    fecha date,
    hora character varying(8),
    cod_pago character varying
);

CREATE TABLE IF NOT EXISTS perfil_opciones (
    id_perfil integer NOT NULL,
    id_opcion integer NOT NULL
);

CREATE TABLE IF NOT EXISTS producto_proveedores (
    id integer NOT NULL,
    id_producto integer NOT NULL,
    id_proveedor integer NOT NULL
);

CREATE TABLE IF NOT EXISTS productos_unidades_entrega (
    id integer NOT NULL,
    id_producto integer NOT NULL,
    nombre character varying(60),
    cantidad_paquete double precision NOT NULL,
    id_bodega integer NOT NULL
);

CREATE TABLE IF NOT EXISTS sugeridos_cabecera (
    id integer NOT NULL,
    numero character varying(50),
    id_user_crea integer NOT NULL,
    fecha date,
    hora time without time zone,
    estado integer DEFAULT 0 NOT NULL,
    observacion text,
    id_bodega integer,
    meses_cobertura numeric(6,2) DEFAULT 1
);

CREATE TABLE IF NOT EXISTS sugeridos_detalle (
    id integer NOT NULL,
    id_sugerido_cab integer NOT NULL,
    id_producto integer NOT NULL,
    cantidad_sugerida numeric(18,4) NOT NULL,
    existencia numeric(18,4),
    rotacion_mensual numeric(18,4),
    ultima_compra numeric(18,4),
    seleccionado boolean DEFAULT true NOT NULL,
    cantidad_final numeric(18,4),
    observacion text
);

CREATE TABLE IF NOT EXISTS tipos_abonos (
    id integer NOT NULL,
    nombre character varying,
    color character varying,
    anticipo integer
);

CREATE TABLE IF NOT EXISTS transferencias (
    id integer NOT NULL,
    id_user integer,
    id_fondo_origen integer,
    id_fondo_destino integer,
    descripcion character varying,
    total double precision,
    fecha date,
    hora character varying(8),
    id_ingreso integer,
    id_egreso integer,
    id_caja integer DEFAULT 1 NOT NULL
);

CREATE TABLE IF NOT EXISTS usuario_opciones (
    id_user integer NOT NULL,
    id_opcion integer NOT NULL,
    concedido boolean DEFAULT true NOT NULL
);

CREATE TABLE IF NOT EXISTS usuario_roles_precios (
    id_user integer NOT NULL,
    rol integer NOT NULL,
    CONSTRAINT ck_urp_rol CHECK ((rol = ANY (ARRAY[2, 3, 4])))
);

-- 2.3 Pertenencia y defaults de las secuencias (idempotente por naturaleza)
ALTER SEQUENCE abonos_cabeceras_id_seq OWNED BY abonos_cabeceras.id;
ALTER SEQUENCE abonos_id_seq OWNED BY abonos.id;
ALTER SEQUENCE auditoria_ingresos_campos_id_seq OWNED BY auditoria_ingresos_campos.id;
ALTER SEQUENCE auditoria_ingresos_id_seq OWNED BY auditoria_ingresos.id;
ALTER SEQUENCE creditos_id_seq OWNED BY creditos.id;
ALTER SEQUENCE cuentas_egresos_id_seq OWNED BY cuentas_egresos.id;
ALTER SEQUENCE cuentas_id_seq OWNED BY cuentas.id;
ALTER SEQUENCE cuentas_ingresos_id_seq OWNED BY cuentas_ingresos.id;
ALTER SEQUENCE dedup_contactos_log_id_seq OWNED BY dedup_contactos_log.id;
ALTER SEQUENCE descuentos_id_seq OWNED BY descuentos.id;
ALTER SEQUENCE egresos_id_seq OWNED BY egresos.id;
ALTER SEQUENCE fondos_id_seq OWNED BY fondos.id;
ALTER SEQUENCE fotos_registros_id_seq OWNED BY fotos_registros.id;
ALTER SEQUENCE ingresos_id_seq OWNED BY ingresos.id;
ALTER SEQUENCE ingresos_productos_cabecera_id_seq OWNED BY ingresos_productos_cabecera.id;
ALTER SEQUENCE ingresos_productos_detalle_id_seq OWNED BY ingresos_productos_detalle.id;
ALTER SEQUENCE modulos_id_seq OWNED BY modulos.id;
ALTER SEQUENCE opciones_id_seq OWNED BY opciones.id;
ALTER SEQUENCE pagos_ingresos_productos_id_seq OWNED BY pagos_ingresos_productos.id;
ALTER SEQUENCE producto_proveedores_id_seq OWNED BY producto_proveedores.id;
ALTER SEQUENCE productos_unidades_entrega_id_seq OWNED BY productos_unidades_entrega.id;
ALTER SEQUENCE tipos_abonos_id_seq OWNED BY tipos_abonos.id;
ALTER SEQUENCE transferencias_id_seq OWNED BY transferencias.id;
ALTER TABLE ONLY abonos ALTER COLUMN id SET DEFAULT nextval('abonos_id_seq'::regclass);
ALTER TABLE ONLY abonos_cabeceras ALTER COLUMN id SET DEFAULT nextval('abonos_cabeceras_id_seq'::regclass);
ALTER TABLE ONLY auditoria_ingresos ALTER COLUMN id SET DEFAULT nextval('auditoria_ingresos_id_seq'::regclass);
ALTER TABLE ONLY auditoria_ingresos_campos ALTER COLUMN id SET DEFAULT nextval('auditoria_ingresos_campos_id_seq'::regclass);
ALTER TABLE ONLY creditos ALTER COLUMN id SET DEFAULT nextval('creditos_id_seq'::regclass);
ALTER TABLE ONLY cuentas ALTER COLUMN id SET DEFAULT nextval('cuentas_id_seq'::regclass);
ALTER TABLE ONLY cuentas_egresos ALTER COLUMN id SET DEFAULT nextval('cuentas_egresos_id_seq'::regclass);
ALTER TABLE ONLY cuentas_ingresos ALTER COLUMN id SET DEFAULT nextval('cuentas_ingresos_id_seq'::regclass);
ALTER TABLE ONLY dedup_contactos_log ALTER COLUMN id SET DEFAULT nextval('dedup_contactos_log_id_seq'::regclass);
ALTER TABLE ONLY descuentos ALTER COLUMN id SET DEFAULT nextval('descuentos_id_seq'::regclass);
ALTER TABLE ONLY egresos ALTER COLUMN id SET DEFAULT nextval('egresos_id_seq'::regclass);
ALTER TABLE ONLY fondos ALTER COLUMN id SET DEFAULT nextval('fondos_id_seq'::regclass);
ALTER TABLE ONLY fotos_registros ALTER COLUMN id SET DEFAULT nextval('fotos_registros_id_seq'::regclass);
ALTER TABLE ONLY ingresos ALTER COLUMN id SET DEFAULT nextval('ingresos_id_seq'::regclass);
ALTER TABLE ONLY ingresos_productos_cabecera ALTER COLUMN id SET DEFAULT nextval('ingresos_productos_cabecera_id_seq'::regclass);
ALTER TABLE ONLY ingresos_productos_detalle ALTER COLUMN id SET DEFAULT nextval('ingresos_productos_detalle_id_seq'::regclass);
ALTER TABLE ONLY modulos ALTER COLUMN id SET DEFAULT nextval('modulos_id_seq'::regclass);
ALTER TABLE ONLY opciones ALTER COLUMN id SET DEFAULT nextval('opciones_id_seq'::regclass);
ALTER TABLE ONLY pagos_ingresos_productos ALTER COLUMN id SET DEFAULT nextval('pagos_ingresos_productos_id_seq'::regclass);
ALTER TABLE ONLY producto_proveedores ALTER COLUMN id SET DEFAULT nextval('producto_proveedores_id_seq'::regclass);
ALTER TABLE ONLY productos_unidades_entrega ALTER COLUMN id SET DEFAULT nextval('productos_unidades_entrega_id_seq'::regclass);
ALTER TABLE ONLY tipos_abonos ALTER COLUMN id SET DEFAULT nextval('tipos_abonos_id_seq'::regclass);
ALTER TABLE ONLY transferencias ALTER COLUMN id SET DEFAULT nextval('transferencias_id_seq'::regclass);

-- 2.4 Llaves primarias y unicas
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.abonos_cabeceras'::regclass) THEN
        ALTER TABLE ONLY abonos_cabeceras ADD CONSTRAINT abonos_cabeceras_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.abonos'::regclass) THEN
        ALTER TABLE ONLY abonos ADD CONSTRAINT abonos_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.comparativos_cabecera'::regclass) THEN
        ALTER TABLE ONLY comparativos_cabecera ADD CONSTRAINT comparativos_cabecera_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.comparativos_precios'::regclass) THEN
        ALTER TABLE ONLY comparativos_precios ADD CONSTRAINT comparativos_precios_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.comparativos_productos'::regclass) THEN
        ALTER TABLE ONLY comparativos_productos ADD CONSTRAINT comparativos_productos_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.comparativos_proveedores'::regclass) THEN
        ALTER TABLE ONLY comparativos_proveedores ADD CONSTRAINT comparativos_proveedores_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.cotizaciones_compra_cabecera'::regclass) THEN
        ALTER TABLE ONLY cotizaciones_compra_cabecera ADD CONSTRAINT cotizaciones_compra_cabecera_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.cotizaciones_compra_detalle'::regclass) THEN
        ALTER TABLE ONLY cotizaciones_compra_detalle ADD CONSTRAINT cotizaciones_compra_detalle_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.cuentas'::regclass) THEN
        ALTER TABLE ONLY cuentas ADD CONSTRAINT cuentas_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.dedup_contactos_log'::regclass) THEN
        ALTER TABLE ONLY dedup_contactos_log ADD CONSTRAINT dedup_contactos_log_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='modulos_clave_key'
                   AND conrelid='public.modulos'::regclass) THEN
        ALTER TABLE ONLY modulos ADD CONSTRAINT modulos_clave_key UNIQUE (clave);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.modulos'::regclass) THEN
        ALTER TABLE ONLY modulos ADD CONSTRAINT modulos_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.ordenes_compra_cabecera'::regclass) THEN
        ALTER TABLE ONLY ordenes_compra_cabecera ADD CONSTRAINT ordenes_compra_cabecera_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.ordenes_compra_detalle'::regclass) THEN
        ALTER TABLE ONLY ordenes_compra_detalle ADD CONSTRAINT ordenes_compra_detalle_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.auditoria_ingresos'::regclass) THEN
        ALTER TABLE ONLY auditoria_ingresos ADD CONSTRAINT pk_auditoria_ingresos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.auditoria_ingresos_campos'::regclass) THEN
        ALTER TABLE ONLY auditoria_ingresos_campos ADD CONSTRAINT pk_auditoria_ingresos_campos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.ingresos'::regclass) THEN
        ALTER TABLE ONLY ingresos ADD CONSTRAINT pk_caja_ingresos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.creditos'::regclass) THEN
        ALTER TABLE ONLY creditos ADD CONSTRAINT pk_creditos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.cuentas_egresos'::regclass) THEN
        ALTER TABLE ONLY cuentas_egresos ADD CONSTRAINT pk_cuentas_egresos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.cuentas_ingresos'::regclass) THEN
        ALTER TABLE ONLY cuentas_ingresos ADD CONSTRAINT pk_cuentas_ingresos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.descuentos'::regclass) THEN
        ALTER TABLE ONLY descuentos ADD CONSTRAINT pk_descuentos_precios PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.egresos'::regclass) THEN
        ALTER TABLE ONLY egresos ADD CONSTRAINT pk_egresos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.fondos'::regclass) THEN
        ALTER TABLE ONLY fondos ADD CONSTRAINT pk_fondos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.fotos_registros'::regclass) THEN
        ALTER TABLE ONLY fotos_registros ADD CONSTRAINT pk_fotos_registros PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.ingresos_productos_cabecera'::regclass) THEN
        ALTER TABLE ONLY ingresos_productos_cabecera ADD CONSTRAINT pk_ingresos_productos_cabecera PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.ingresos_productos_detalle'::regclass) THEN
        ALTER TABLE ONLY ingresos_productos_detalle ADD CONSTRAINT pk_ingresos_productos_detalle PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.opciones'::regclass) THEN
        ALTER TABLE ONLY opciones ADD CONSTRAINT pk_opciones PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.pagos_ingresos_productos'::regclass) THEN
        ALTER TABLE ONLY pagos_ingresos_productos ADD CONSTRAINT pk_pagos_ingresos_productos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.perfil_opciones'::regclass) THEN
        ALTER TABLE ONLY perfil_opciones ADD CONSTRAINT pk_perfil_opciones PRIMARY KEY (id_perfil, id_opcion);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.transferencias'::regclass) THEN
        ALTER TABLE ONLY transferencias ADD CONSTRAINT pk_transferencias PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.usuario_opciones'::regclass) THEN
        ALTER TABLE ONLY usuario_opciones ADD CONSTRAINT pk_usuario_opciones PRIMARY KEY (id_user, id_opcion);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.usuario_roles_precios'::regclass) THEN
        ALTER TABLE ONLY usuario_roles_precios ADD CONSTRAINT pk_usuario_roles_precios PRIMARY KEY (id_user, rol);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.producto_proveedores'::regclass) THEN
        ALTER TABLE ONLY producto_proveedores ADD CONSTRAINT producto_proveedores_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.productos_unidades_entrega'::regclass) THEN
        ALTER TABLE ONLY productos_unidades_entrega ADD CONSTRAINT productos_unidades_entrega_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.sugeridos_cabecera'::regclass) THEN
        ALTER TABLE ONLY sugeridos_cabecera ADD CONSTRAINT sugeridos_cabecera_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.sugeridos_detalle'::regclass) THEN
        ALTER TABLE ONLY sugeridos_detalle ADD CONSTRAINT sugeridos_detalle_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE contype='p'
                   AND conrelid='public.tipos_abonos'::regclass) THEN
        ALTER TABLE ONLY tipos_abonos ADD CONSTRAINT tipos_abonos_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='uq_opciones_clave'
                   AND conrelid='public.opciones'::regclass) THEN
        ALTER TABLE ONLY opciones ADD CONSTRAINT uq_opciones_clave UNIQUE (clave);
    END IF;
END $$;

-- 2.5 Llaves foraneas
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='abonos_cabeceras_id_contacto_fkey'
                   AND conrelid='public.abonos_cabeceras'::regclass) THEN
        ALTER TABLE ONLY abonos_cabeceras ADD CONSTRAINT abonos_cabeceras_id_contacto_fkey FOREIGN KEY (id_contacto) REFERENCES contactos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='abonos_cabeceras_id_tipo_abono_fkey'
                   AND conrelid='public.abonos_cabeceras'::regclass) THEN
        ALTER TABLE ONLY abonos_cabeceras ADD CONSTRAINT abonos_cabeceras_id_tipo_abono_fkey FOREIGN KEY (id_tipo_abono) REFERENCES tipos_abonos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='abonos_cabeceras_id_user_fkey'
                   AND conrelid='public.abonos_cabeceras'::regclass) THEN
        ALTER TABLE ONLY abonos_cabeceras ADD CONSTRAINT abonos_cabeceras_id_user_fkey FOREIGN KEY (id_user) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='abonos_id_cabecera_fkey'
                   AND conrelid='public.abonos'::regclass) THEN
        ALTER TABLE ONLY abonos ADD CONSTRAINT abonos_id_cabecera_fkey FOREIGN KEY (id_cabecera) REFERENCES abonos_cabeceras(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='abonos_id_credito_fkey'
                   AND conrelid='public.abonos'::regclass) THEN
        ALTER TABLE ONLY abonos ADD CONSTRAINT abonos_id_credito_fkey FOREIGN KEY (id_credito) REFERENCES creditos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_cabecera_id_proveedor_unico_fkey'
                   AND conrelid='public.comparativos_cabecera'::regclass) THEN
        ALTER TABLE ONLY comparativos_cabecera ADD CONSTRAINT comparativos_cabecera_id_proveedor_unico_fkey FOREIGN KEY (id_proveedor_unico) REFERENCES contactos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_cabecera_id_sugerido_fkey'
                   AND conrelid='public.comparativos_cabecera'::regclass) THEN
        ALTER TABLE ONLY comparativos_cabecera ADD CONSTRAINT comparativos_cabecera_id_sugerido_fkey FOREIGN KEY (id_sugerido) REFERENCES sugeridos_cabecera(id) ON DELETE SET NULL;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_cabecera_id_user_autoriza_fkey'
                   AND conrelid='public.comparativos_cabecera'::regclass) THEN
        ALTER TABLE ONLY comparativos_cabecera ADD CONSTRAINT comparativos_cabecera_id_user_autoriza_fkey FOREIGN KEY (id_user_autoriza) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_cabecera_id_user_fkey'
                   AND conrelid='public.comparativos_cabecera'::regclass) THEN
        ALTER TABLE ONLY comparativos_cabecera ADD CONSTRAINT comparativos_cabecera_id_user_fkey FOREIGN KEY (id_user) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_precios_id_comp_producto_fkey'
                   AND conrelid='public.comparativos_precios'::regclass) THEN
        ALTER TABLE ONLY comparativos_precios ADD CONSTRAINT comparativos_precios_id_comp_producto_fkey FOREIGN KEY (id_comp_producto) REFERENCES comparativos_productos(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_precios_id_comp_proveedor_fkey'
                   AND conrelid='public.comparativos_precios'::regclass) THEN
        ALTER TABLE ONLY comparativos_precios ADD CONSTRAINT comparativos_precios_id_comp_proveedor_fkey FOREIGN KEY (id_comp_proveedor) REFERENCES comparativos_proveedores(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_precios_id_comparativo_fkey'
                   AND conrelid='public.comparativos_precios'::regclass) THEN
        ALTER TABLE ONLY comparativos_precios ADD CONSTRAINT comparativos_precios_id_comparativo_fkey FOREIGN KEY (id_comparativo) REFERENCES comparativos_cabecera(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_productos_id_comparativo_fkey'
                   AND conrelid='public.comparativos_productos'::regclass) THEN
        ALTER TABLE ONLY comparativos_productos ADD CONSTRAINT comparativos_productos_id_comparativo_fkey FOREIGN KEY (id_comparativo) REFERENCES comparativos_cabecera(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_productos_id_producto_fkey'
                   AND conrelid='public.comparativos_productos'::regclass) THEN
        ALTER TABLE ONLY comparativos_productos ADD CONSTRAINT comparativos_productos_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES productos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_proveedores_id_comparativo_fkey'
                   AND conrelid='public.comparativos_proveedores'::regclass) THEN
        ALTER TABLE ONLY comparativos_proveedores ADD CONSTRAINT comparativos_proveedores_id_comparativo_fkey FOREIGN KEY (id_comparativo) REFERENCES comparativos_cabecera(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_proveedores_id_proveedor_fkey'
                   AND conrelid='public.comparativos_proveedores'::regclass) THEN
        ALTER TABLE ONLY comparativos_proveedores ADD CONSTRAINT comparativos_proveedores_id_proveedor_fkey FOREIGN KEY (id_proveedor) REFERENCES contactos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='cotizaciones_compra_cabecera_id_proveedor_fkey'
                   AND conrelid='public.cotizaciones_compra_cabecera'::regclass) THEN
        ALTER TABLE ONLY cotizaciones_compra_cabecera ADD CONSTRAINT cotizaciones_compra_cabecera_id_proveedor_fkey FOREIGN KEY (id_proveedor) REFERENCES contactos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='cotizaciones_compra_cabecera_id_sugerido_fkey'
                   AND conrelid='public.cotizaciones_compra_cabecera'::regclass) THEN
        ALTER TABLE ONLY cotizaciones_compra_cabecera ADD CONSTRAINT cotizaciones_compra_cabecera_id_sugerido_fkey FOREIGN KEY (id_sugerido) REFERENCES sugeridos_cabecera(id) ON DELETE SET NULL;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='cotizaciones_compra_cabecera_id_user_fkey'
                   AND conrelid='public.cotizaciones_compra_cabecera'::regclass) THEN
        ALTER TABLE ONLY cotizaciones_compra_cabecera ADD CONSTRAINT cotizaciones_compra_cabecera_id_user_fkey FOREIGN KEY (id_user) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='cotizaciones_compra_detalle_id_cotiz_cab_fkey'
                   AND conrelid='public.cotizaciones_compra_detalle'::regclass) THEN
        ALTER TABLE ONLY cotizaciones_compra_detalle ADD CONSTRAINT cotizaciones_compra_detalle_id_cotiz_cab_fkey FOREIGN KEY (id_cotiz_cab) REFERENCES cotizaciones_compra_cabecera(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='cotizaciones_compra_detalle_id_producto_fkey'
                   AND conrelid='public.cotizaciones_compra_detalle'::regclass) THEN
        ALTER TABLE ONLY cotizaciones_compra_detalle ADD CONSTRAINT cotizaciones_compra_detalle_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES productos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_auditoria_ingresos_campos'
                   AND conrelid='public.auditoria_ingresos_campos'::regclass) THEN
        ALTER TABLE ONLY auditoria_ingresos_campos ADD CONSTRAINT fk_auditoria_ingresos_campos FOREIGN KEY (id_auditoria) REFERENCES auditoria_ingresos(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_creditos_contacto'
                   AND conrelid='public.creditos'::regclass) THEN
        ALTER TABLE ONLY creditos ADD CONSTRAINT fk_creditos_contacto FOREIGN KEY (id_contacto) REFERENCES contactos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_creditos_cuenta'
                   AND conrelid='public.creditos'::regclass) THEN
        ALTER TABLE ONLY creditos ADD CONSTRAINT fk_creditos_cuenta FOREIGN KEY (id_cuenta) REFERENCES cuentas(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_creditos_user'
                   AND conrelid='public.creditos'::regclass) THEN
        ALTER TABLE ONLY creditos ADD CONSTRAINT fk_creditos_user FOREIGN KEY (id_user) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_egreso_cliente'
                   AND conrelid='public.egresos'::regclass) THEN
        ALTER TABLE ONLY egresos ADD CONSTRAINT fk_egreso_cliente FOREIGN KEY (id_cliente) REFERENCES contactos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_egreso_cuenta'
                   AND conrelid='public.egresos'::regclass) THEN
        ALTER TABLE ONLY egresos ADD CONSTRAINT fk_egreso_cuenta FOREIGN KEY (id_cuenta) REFERENCES cuentas_egresos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_egreso_fondo'
                   AND conrelid='public.egresos'::regclass) THEN
        ALTER TABLE ONLY egresos ADD CONSTRAINT fk_egreso_fondo FOREIGN KEY (id_fondo) REFERENCES fondos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_egreso_user'
                   AND conrelid='public.egresos'::regclass) THEN
        ALTER TABLE ONLY egresos ADD CONSTRAINT fk_egreso_user FOREIGN KEY (id_user) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_ingreso_cliente'
                   AND conrelid='public.ingresos'::regclass) THEN
        ALTER TABLE ONLY ingresos ADD CONSTRAINT fk_ingreso_cliente FOREIGN KEY (id_cliente) REFERENCES contactos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_ingreso_cuenta'
                   AND conrelid='public.ingresos'::regclass) THEN
        ALTER TABLE ONLY ingresos ADD CONSTRAINT fk_ingreso_cuenta FOREIGN KEY (id_cuenta) REFERENCES cuentas_ingresos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_ingreso_fondo'
                   AND conrelid='public.ingresos'::regclass) THEN
        ALTER TABLE ONLY ingresos ADD CONSTRAINT fk_ingreso_fondo FOREIGN KEY (id_fondo) REFERENCES fondos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_ingreso_user'
                   AND conrelid='public.ingresos'::regclass) THEN
        ALTER TABLE ONLY ingresos ADD CONSTRAINT fk_ingreso_user FOREIGN KEY (id_user) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_ipc_proveedor'
                   AND conrelid='public.ingresos_productos_cabecera'::regclass) THEN
        ALTER TABLE ONLY ingresos_productos_cabecera ADD CONSTRAINT fk_ipc_proveedor FOREIGN KEY (id_proveedor) REFERENCES contactos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_ipc_transportador'
                   AND conrelid='public.ingresos_productos_cabecera'::regclass) THEN
        ALTER TABLE ONLY ingresos_productos_cabecera ADD CONSTRAINT fk_ipc_transportador FOREIGN KEY (id_transportador) REFERENCES contactos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_ipc_user'
                   AND conrelid='public.ingresos_productos_cabecera'::regclass) THEN
        ALTER TABLE ONLY ingresos_productos_cabecera ADD CONSTRAINT fk_ipc_user FOREIGN KEY (id_user) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_ipd_cabecera'
                   AND conrelid='public.ingresos_productos_detalle'::regclass) THEN
        ALTER TABLE ONLY ingresos_productos_detalle ADD CONSTRAINT fk_ipd_cabecera FOREIGN KEY (id_ingreso_cabecera) REFERENCES ingresos_productos_cabecera(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_ipd_producto'
                   AND conrelid='public.ingresos_productos_detalle'::regclass) THEN
        ALTER TABLE ONLY ingresos_productos_detalle ADD CONSTRAINT fk_ipd_producto FOREIGN KEY (id_producto) REFERENCES productos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_pip_cabecera'
                   AND conrelid='public.pagos_ingresos_productos'::regclass) THEN
        ALTER TABLE ONLY pagos_ingresos_productos ADD CONSTRAINT fk_pip_cabecera FOREIGN KEY (id_ingreso_productos_cabecera) REFERENCES ingresos_productos_cabecera(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_po_opcion'
                   AND conrelid='public.perfil_opciones'::regclass) THEN
        ALTER TABLE ONLY perfil_opciones ADD CONSTRAINT fk_po_opcion FOREIGN KEY (id_opcion) REFERENCES opciones(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_po_perfil'
                   AND conrelid='public.perfil_opciones'::regclass) THEN
        ALTER TABLE ONLY perfil_opciones ADD CONSTRAINT fk_po_perfil FOREIGN KEY (id_perfil) REFERENCES perfiles(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_pue_bodega'
                   AND conrelid='public.productos_unidades_entrega'::regclass) THEN
        ALTER TABLE ONLY productos_unidades_entrega ADD CONSTRAINT fk_pue_bodega FOREIGN KEY (id_bodega) REFERENCES bodegas(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_pue_producto'
                   AND conrelid='public.productos_unidades_entrega'::regclass) THEN
        ALTER TABLE ONLY productos_unidades_entrega ADD CONSTRAINT fk_pue_producto FOREIGN KEY (id_producto) REFERENCES productos(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_transferencia_egreso'
                   AND conrelid='public.transferencias'::regclass) THEN
        ALTER TABLE ONLY transferencias ADD CONSTRAINT fk_transferencia_egreso FOREIGN KEY (id_egreso) REFERENCES egresos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_transferencia_fondo_destino'
                   AND conrelid='public.transferencias'::regclass) THEN
        ALTER TABLE ONLY transferencias ADD CONSTRAINT fk_transferencia_fondo_destino FOREIGN KEY (id_fondo_destino) REFERENCES fondos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_transferencia_fondo_origen'
                   AND conrelid='public.transferencias'::regclass) THEN
        ALTER TABLE ONLY transferencias ADD CONSTRAINT fk_transferencia_fondo_origen FOREIGN KEY (id_fondo_origen) REFERENCES fondos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_transferencia_ingreso'
                   AND conrelid='public.transferencias'::regclass) THEN
        ALTER TABLE ONLY transferencias ADD CONSTRAINT fk_transferencia_ingreso FOREIGN KEY (id_ingreso) REFERENCES ingresos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_transferencia_user'
                   AND conrelid='public.transferencias'::regclass) THEN
        ALTER TABLE ONLY transferencias ADD CONSTRAINT fk_transferencia_user FOREIGN KEY (id_user) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_uo_opcion'
                   AND conrelid='public.usuario_opciones'::regclass) THEN
        ALTER TABLE ONLY usuario_opciones ADD CONSTRAINT fk_uo_opcion FOREIGN KEY (id_opcion) REFERENCES opciones(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_uo_user'
                   AND conrelid='public.usuario_opciones'::regclass) THEN
        ALTER TABLE ONLY usuario_opciones ADD CONSTRAINT fk_uo_user FOREIGN KEY (id_user) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_urp_user'
                   AND conrelid='public.usuario_roles_precios'::regclass) THEN
        ALTER TABLE ONLY usuario_roles_precios ADD CONSTRAINT fk_urp_user FOREIGN KEY (id_user) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ordenes_compra_cabecera_id_bodega_fkey'
                   AND conrelid='public.ordenes_compra_cabecera'::regclass) THEN
        ALTER TABLE ONLY ordenes_compra_cabecera ADD CONSTRAINT ordenes_compra_cabecera_id_bodega_fkey FOREIGN KEY (id_bodega) REFERENCES bodegas(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ordenes_compra_cabecera_id_user_aprueba_fkey'
                   AND conrelid='public.ordenes_compra_cabecera'::regclass) THEN
        ALTER TABLE ONLY ordenes_compra_cabecera ADD CONSTRAINT ordenes_compra_cabecera_id_user_aprueba_fkey FOREIGN KEY (id_user_aprueba) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ordenes_compra_cabecera_id_user_crea_fkey'
                   AND conrelid='public.ordenes_compra_cabecera'::regclass) THEN
        ALTER TABLE ONLY ordenes_compra_cabecera ADD CONSTRAINT ordenes_compra_cabecera_id_user_crea_fkey FOREIGN KEY (id_user_crea) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ordenes_compra_detalle_id_orden_cabecera_fkey'
                   AND conrelid='public.ordenes_compra_detalle'::regclass) THEN
        ALTER TABLE ONLY ordenes_compra_detalle ADD CONSTRAINT ordenes_compra_detalle_id_orden_cabecera_fkey FOREIGN KEY (id_orden_cabecera) REFERENCES ordenes_compra_cabecera(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ordenes_compra_detalle_id_producto_fkey'
                   AND conrelid='public.ordenes_compra_detalle'::regclass) THEN
        ALTER TABLE ONLY ordenes_compra_detalle ADD CONSTRAINT ordenes_compra_detalle_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES productos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ordenes_compra_detalle_id_proveedor_fkey'
                   AND conrelid='public.ordenes_compra_detalle'::regclass) THEN
        ALTER TABLE ONLY ordenes_compra_detalle ADD CONSTRAINT ordenes_compra_detalle_id_proveedor_fkey FOREIGN KEY (id_proveedor) REFERENCES contactos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='producto_proveedores_id_producto_fkey'
                   AND conrelid='public.producto_proveedores'::regclass) THEN
        ALTER TABLE ONLY producto_proveedores ADD CONSTRAINT producto_proveedores_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES productos(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='producto_proveedores_id_proveedor_fkey'
                   AND conrelid='public.producto_proveedores'::regclass) THEN
        ALTER TABLE ONLY producto_proveedores ADD CONSTRAINT producto_proveedores_id_proveedor_fkey FOREIGN KEY (id_proveedor) REFERENCES contactos(id) ON DELETE CASCADE;
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='sugeridos_cabecera_id_bodega_fkey'
                   AND conrelid='public.sugeridos_cabecera'::regclass) THEN
        ALTER TABLE ONLY sugeridos_cabecera ADD CONSTRAINT sugeridos_cabecera_id_bodega_fkey FOREIGN KEY (id_bodega) REFERENCES bodegas(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='sugeridos_cabecera_id_user_crea_fkey'
                   AND conrelid='public.sugeridos_cabecera'::regclass) THEN
        ALTER TABLE ONLY sugeridos_cabecera ADD CONSTRAINT sugeridos_cabecera_id_user_crea_fkey FOREIGN KEY (id_user_crea) REFERENCES users(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='sugeridos_detalle_id_producto_fkey'
                   AND conrelid='public.sugeridos_detalle'::regclass) THEN
        ALTER TABLE ONLY sugeridos_detalle ADD CONSTRAINT sugeridos_detalle_id_producto_fkey FOREIGN KEY (id_producto) REFERENCES productos(id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='sugeridos_detalle_id_sugerido_cab_fkey'
                   AND conrelid='public.sugeridos_detalle'::regclass) THEN
        ALTER TABLE ONLY sugeridos_detalle ADD CONSTRAINT sugeridos_detalle_id_sugerido_cab_fkey FOREIGN KEY (id_sugerido_cab) REFERENCES sugeridos_cabecera(id) ON DELETE CASCADE;
    END IF;
END $$;

-- 2.6 Indices
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_abonos_cabeceras_contacto') THEN
        CREATE INDEX idx_abonos_cabeceras_contacto ON abonos_cabeceras USING btree (id_contacto);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_abonos_cabeceras_fecha') THEN
        CREATE INDEX idx_abonos_cabeceras_fecha ON abonos_cabeceras USING btree (fecha);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_abonos_id_cabecera') THEN
        CREATE INDEX idx_abonos_id_cabecera ON abonos USING btree (id_cabecera);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_abonos_id_credito') THEN
        CREATE INDEX idx_abonos_id_credito ON abonos USING btree (id_credito);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_auditoria_ingresos_campos_aud') THEN
        CREATE INDEX idx_auditoria_ingresos_campos_aud ON auditoria_ingresos_campos USING btree (id_auditoria);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_auditoria_ingresos_fecha') THEN
        CREATE INDEX idx_auditoria_ingresos_fecha ON auditoria_ingresos USING btree (fecha);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_auditoria_ingresos_fecha_hora') THEN
        CREATE INDEX idx_auditoria_ingresos_fecha_hora ON auditoria_ingresos USING btree (fecha_hora);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_auditoria_ingresos_registro') THEN
        CREATE INDEX idx_auditoria_ingresos_registro ON auditoria_ingresos USING btree (tabla, id_registro);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_auditoria_ingresos_usuario') THEN
        CREATE INDEX idx_auditoria_ingresos_usuario ON auditoria_ingresos USING btree (id_usuario);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_comp_precios_comp') THEN
        CREATE INDEX idx_comp_precios_comp ON comparativos_precios USING btree (id_comparativo);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_cotiz_detalle_cab') THEN
        CREATE INDEX idx_cotiz_detalle_cab ON cotizaciones_compra_detalle USING btree (id_cotiz_cab);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_creditos_contacto') THEN
        CREATE INDEX idx_creditos_contacto ON creditos USING btree (id_contacto);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_cuentas_egresos_caja') THEN
        CREATE INDEX idx_cuentas_egresos_caja ON cuentas_egresos USING btree (id_caja);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_cuentas_ingresos_caja') THEN
        CREATE INDEX idx_cuentas_ingresos_caja ON cuentas_ingresos USING btree (id_caja);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_egresos_caja') THEN
        CREATE INDEX idx_egresos_caja ON egresos USING btree (id_caja);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_egresos_cliente') THEN
        CREATE INDEX idx_egresos_cliente ON egresos USING btree (id_cliente);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_egresos_fecha') THEN
        CREATE INDEX idx_egresos_fecha ON egresos USING btree (fecha);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_egresos_fondo') THEN
        CREATE INDEX idx_egresos_fondo ON egresos USING btree (id_fondo);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_fondos_caja') THEN
        CREATE INDEX idx_fondos_caja ON fondos USING btree (id_caja);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_fotos_registros_registro') THEN
        CREATE INDEX idx_fotos_registros_registro ON fotos_registros USING btree (id_registro, tipo_registro);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_ingresos_caja') THEN
        CREATE INDEX idx_ingresos_caja ON ingresos USING btree (id_caja);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_ingresos_cliente') THEN
        CREATE INDEX idx_ingresos_cliente ON ingresos USING btree (id_cliente);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_ingresos_fecha') THEN
        CREATE INDEX idx_ingresos_fecha ON ingresos USING btree (fecha);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_ingresos_fondo') THEN
        CREATE INDEX idx_ingresos_fondo ON ingresos USING btree (id_fondo);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_ipc_fecha') THEN
        CREATE INDEX idx_ipc_fecha ON ingresos_productos_cabecera USING btree (fecha);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_ipc_proveedor') THEN
        CREATE INDEX idx_ipc_proveedor ON ingresos_productos_cabecera USING btree (id_proveedor);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_ipd_cabecera') THEN
        CREATE INDEX idx_ipd_cabecera ON ingresos_productos_detalle USING btree (id_ingreso_cabecera);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_ipd_producto') THEN
        CREATE INDEX idx_ipd_producto ON ingresos_productos_detalle USING btree (id_producto);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_ordenes_compra_detalle_cabecera') THEN
        CREATE INDEX idx_ordenes_compra_detalle_cabecera ON ordenes_compra_detalle USING btree (id_orden_cabecera);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_pue_producto') THEN
        CREATE INDEX idx_pue_producto ON productos_unidades_entrega USING btree (id_producto);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_sugeridos_detalle_cab') THEN
        CREATE INDEX idx_sugeridos_detalle_cab ON sugeridos_detalle USING btree (id_sugerido_cab);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_transferencias_caja') THEN
        CREATE INDEX idx_transferencias_caja ON transferencias USING btree (id_caja);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_transferencias_fecha') THEN
        CREATE INDEX idx_transferencias_fecha ON transferencias USING btree (fecha);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='uq_producto_proveedor') THEN
        CREATE UNIQUE INDEX uq_producto_proveedor ON producto_proveedores USING btree (id_producto, id_proveedor);
    END IF;
END $$;

-- SIN CLASIFICAR (revisar):
-- CREATE TRIGGER trg_auditoria_ingresos_campos_inmutable BEFORE DELETE OR UPDATE ON public.auditoria_ingresos_campos FOR EACH ROW EXECUTE PROCEDURE public.fn_auditoria_ingresos_inmutable();
-- CREATE TRIGGER trg_auditoria_ingresos_campos_truncate BEFORE TRUNCATE ON public.auditoria_ingresos_campos FOR EACH STATEMENT EXECUTE PROCEDURE public.fn_auditoria_ingresos_inmutable();
-- CREATE TRIGGER trg_auditoria_ingresos_inmutable BEFORE DELETE OR UPDATE ON public.auditoria_ingresos FOR EACH ROW EXECUTE PROCEDURE public.fn_auditoria_ingresos_inmutable();
-- CREATE TRIGGER trg_auditoria_ingresos_productos AFTER INSERT OR DELETE OR UPDATE ON public.ingresos_productos_cabecera FOR EACH ROW EXECUTE PROCEDURE public.fn_auditoria_ingresos();
-- CREATE TRIGGER trg_auditoria_ingresos_truncate BEFORE TRUNCATE ON public.auditoria_ingresos FOR EACH STATEMENT EXECUTE PROCEDURE public.fn_auditoria_ingresos_inmutable();

-- -----------------------------------------------------------------------------
-- 3. FUNCIONES (11)
--    CREATE OR REPLACE: idempotente por naturaleza.
--      asignar_bodegas_entrega / seleccionar_bodega_descarga
--          reparto de una orden por bodega segun productos_unidades_entrega
--          (compartidas con wo-printer).
--      fn_auditoria_ingresos / fn_auditoria_ingresos_inmutable / ing_audit_*
--          auditoria de ingresos del modulo Precios.
--      normaliza_cedula / normaliza_nombre / fusionar_contacto
--          soporte de la deduplicacion de contactos.
-- -----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.asignar_bodegas_entrega(p_id_producto integer, p_cantidad double precision)
 RETURNS TABLE(id_bodega integer, cantidad double precision)
 LANGUAGE plpgsql
AS $function$
-- Los parametros de salida se llaman igual que columnas de las tablas que se
-- consultan (id_bodega, cantidad). Con la directiva use_column, dentro de los
-- SELECT esos nombres resuelven a la COLUMNA; las asignaciones (x := ...) siguen
-- apuntando a la variable de salida. Evita el error "referencia ... ambigua".
#variable_conflict use_column
DECLARE
    v_rec      RECORD;
    v_restante double precision := p_cantidad;
    v_n        bigint;
    v_asignado double precision;
    v_hay      boolean := false;       -- el producto tiene unidades configuradas
    v_bodegas  integer[]          := ARRAY[]::integer[];
    v_cant     double precision[]  := ARRAY[]::double precision[];
    v_idx      integer;
    v_pos      integer;
    v_fallback integer;
BEGIN
    -- Recorrer las unidades de entrega de mayor a menor paquete.
    -- Alias internos cp/bod (no id_bodega/cantidad) para no colisionar con los
    -- parametros de salida de la funcion.
    FOR v_rec IN
        SELECT pue.cantidad_paquete AS cp, pue.id_bodega AS bod
          FROM productos_unidades_entrega pue
         WHERE pue.id_producto = p_id_producto
         ORDER BY pue.cantidad_paquete DESC
    LOOP
        v_hay := true;
        IF v_rec.cp <= 0 THEN
            CONTINUE;
        END IF;

        v_n := floor(v_restante / v_rec.cp);
        IF v_n > 0 THEN
            v_asignado := v_n * v_rec.cp;

            -- acumular por bodega (buscar si ya existe en el array)
            v_pos := NULL;
            FOR v_idx IN 1 .. COALESCE(array_length(v_bodegas, 1), 0) LOOP
                IF v_bodegas[v_idx] = v_rec.bod THEN
                    v_pos := v_idx;
                    EXIT;
                END IF;
            END LOOP;

            IF v_pos IS NULL THEN
                v_bodegas := array_append(v_bodegas, v_rec.bod);
                v_cant    := array_append(v_cant, v_asignado);
            ELSE
                v_cant[v_pos] := v_cant[v_pos] + v_asignado;
            END IF;

            v_restante := v_restante - v_asignado;
        END IF;
    END LOOP;

    -- Sin unidades configuradas: una sola bodega por las 4 reglas
    IF NOT v_hay THEN
        id_bodega := seleccionar_bodega_descarga(p_id_producto);
        cantidad  := p_cantidad;
        RETURN NEXT;
        RETURN;
    END IF;

    -- Defensivo: si quedo sobrante (config sin renglon de paquete = 1) se
    -- asigna por las 4 reglas.
    IF v_restante > 1e-9 THEN
        v_fallback := seleccionar_bodega_descarga(p_id_producto);
        v_pos := NULL;
        FOR v_idx IN 1 .. COALESCE(array_length(v_bodegas, 1), 0) LOOP
            IF v_bodegas[v_idx] = v_fallback THEN
                v_pos := v_idx;
                EXIT;
            END IF;
        END LOOP;
        IF v_pos IS NULL THEN
            v_bodegas := array_append(v_bodegas, v_fallback);
            v_cant    := array_append(v_cant, v_restante);
        ELSE
            v_cant[v_pos] := v_cant[v_pos] + v_restante;
        END IF;
    END IF;

    -- Devolver el acumulado
    FOR v_idx IN 1 .. COALESCE(array_length(v_bodegas, 1), 0) LOOP
        id_bodega := v_bodegas[v_idx];
        cantidad  := v_cant[v_idx];
        RETURN NEXT;
    END LOOP;
    RETURN;
END;
$function$
;

CREATE OR REPLACE FUNCTION public.fn_auditoria_ingresos()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
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
$function$
;

CREATE OR REPLACE FUNCTION public.fn_auditoria_ingresos_inmutable()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    RAISE EXCEPTION 'La auditoria de ingresos es de solo lectura: no se permite % sobre %',
        TG_OP, TG_TABLE_NAME;
END;
$function$
;

CREATE OR REPLACE FUNCTION public.fusionar_contacto(p_super integer, p_dup integer, p_fase text DEFAULT 'manual'::text)
 RETURNS void
 LANGUAGE plpgsql
AS $function$
DECLARE
    v_nombre_dup  text;
    v_cedula_dup  text;
    v_marca       text;
BEGIN
    -- 0) Guardas.
    IF p_super IS NULL OR p_dup IS NULL OR p_super = p_dup THEN
        RETURN;
    END IF;

    SELECT nombre, cedula INTO v_nombre_dup, v_cedula_dup
    FROM contactos WHERE id = p_dup;
    IF NOT FOUND THEN
        RETURN;  -- el duplicado ya no existe
    END IF;

    PERFORM 1 FROM contactos WHERE id = p_super;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'fusionar_contacto: el sobreviviente % no existe', p_super;
    END IF;

    -- Marca a concatenar en las columnas de texto de los hijos reasignados.
    v_marca := ' [ex-contacto: ' || COALESCE(v_nombre_dup, '') || ']';

    -- 1) Copiar al sobreviviente los datos que tenga vacios (no piso lo existente).
    --    La cedula NO se copia aqui: se trata aparte (paso 1b) para no violar el
    --    indice unico de cedula si el valor del duplicado ya existe en otro contacto.
    UPDATE contactos s SET
        direccion     = COALESCE(NULLIF(s.direccion, ''),     d.direccion),
        ciudad        = COALESCE(NULLIF(s.ciudad, ''),        d.ciudad),
        contacto      = COALESCE(NULLIF(s.contacto, ''),      d.contacto),
        contacto2     = COALESCE(NULLIF(s.contacto2, ''),     d.contacto2),
        email         = COALESCE(NULLIF(s.email, ''),         d.email),
        forma_pago    = COALESCE(NULLIF(s.forma_pago, ''),    d.forma_pago),
        cuenta        = COALESCE(NULLIF(s.cuenta, ''),        d.cuenta),
        tipo_cuenta   = COALESCE(NULLIF(s.tipo_cuenta, ''),   d.tipo_cuenta),
        numero_cuenta = COALESCE(NULLIF(s.numero_cuenta, ''), d.numero_cuenta),
        observaciones = COALESCE(NULLIF(s.observaciones, ''), d.observaciones),
        descuento     = COALESCE(s.descuento, d.descuento),
        proveedor     = GREATEST(COALESCE(s.proveedor, 0), COALESCE(d.proveedor, 0))
    FROM contactos d
    WHERE s.id = p_super AND d.id = p_dup;

    -- 1b) Copiar la cedula del duplicado SOLO si el sobreviviente no tiene una y
    --     el valor no colisiona (ni en crudo ni normalizado) con otro contacto.
    UPDATE contactos s
       SET cedula = v_cedula_dup
     WHERE s.id = p_super
       AND (s.cedula IS NULL OR btrim(s.cedula) = '')
       AND v_cedula_dup IS NOT NULL AND btrim(v_cedula_dup) <> ''
       AND NOT EXISTS (
           SELECT 1 FROM contactos x
           WHERE x.id <> p_super
             AND normaliza_cedula(x.cedula) = normaliza_cedula(v_cedula_dup)
             AND normaliza_cedula(v_cedula_dup) <> ''
       )
       AND NOT EXISTS (
           SELECT 1 FROM contactos x
           WHERE x.id <> p_super AND x.cedula = v_cedula_dup
       );

    -- 2) Reasignar registros hijos.

    -- 2a) producto_proveedores: borrar las filas del dup que colisionarian con
    --     una fila ya existente del sobreviviente (mismo producto), luego mover.
    DELETE FROM producto_proveedores pp
    WHERE pp.id_proveedor = p_dup
      AND EXISTS (SELECT 1 FROM producto_proveedores x
                  WHERE x.id_proveedor = p_super AND x.id_producto = pp.id_producto);
    UPDATE producto_proveedores SET id_proveedor = p_super WHERE id_proveedor = p_dup;

    -- 2b) Tablas con concatenacion del nombre antiguo.
    UPDATE facturas_cabeceras
       SET id_contacto = p_super,
           observacion = COALESCE(observacion, '') || v_marca
     WHERE id_contacto = p_dup;

    UPDATE ingresos_mercancias_cabecera
       SET id_proveedor     = CASE WHEN id_proveedor     = p_dup THEN p_super ELSE id_proveedor     END,
           id_transportador = CASE WHEN id_transportador = p_dup THEN p_super ELSE id_transportador END,
           descripcion      = COALESCE(descripcion, '') || v_marca
     WHERE id_proveedor = p_dup OR id_transportador = p_dup;

    UPDATE ingresos_productos_cabecera
       SET id_proveedor     = CASE WHEN id_proveedor     = p_dup THEN p_super ELSE id_proveedor     END,
           id_transportador = CASE WHEN id_transportador = p_dup THEN p_super ELSE id_transportador END,
           observacion      = COALESCE(observacion, '') || v_marca
     WHERE id_proveedor = p_dup OR id_transportador = p_dup;

    -- 2c) Resto de tablas: reasignacion simple.
    UPDATE cotizaciones_cabeceras       SET id_contacto        = p_super WHERE id_contacto        = p_dup;
    UPDATE recortes_detalle             SET id_contacto        = p_super WHERE id_contacto        = p_dup;
    UPDATE productos                    SET id_proveedor       = p_super WHERE id_proveedor       = p_dup;
    UPDATE ordenes_compra_detalle       SET id_proveedor       = p_super WHERE id_proveedor       = p_dup;
    UPDATE comparativos_proveedores     SET id_proveedor       = p_super WHERE id_proveedor       = p_dup;
    UPDATE comparativos_cabecera        SET id_proveedor_unico = p_super WHERE id_proveedor_unico = p_dup;
    UPDATE cotizaciones_compra_cabecera SET id_proveedor       = p_super WHERE id_proveedor       = p_dup;

    -- 3) Fallback: si el sobreviviente quedo sin cedula, usar el nombre como cedula
    --    (solo si ese valor no esta ya tomado por otro contacto).
    UPDATE contactos s
       SET cedula = s.nombre
     WHERE s.id = p_super
       AND (s.cedula IS NULL OR btrim(s.cedula) = '')
       AND NOT EXISTS (
           SELECT 1 FROM contactos x WHERE x.id <> p_super AND x.cedula = s.nombre
       );

    -- 4) Auditoria + eliminacion del duplicado.
    INSERT INTO dedup_contactos_log
        (id_sobreviviente, id_eliminado, nombre_eliminado, cedula_eliminado, motivo, fase)
    VALUES (p_super, p_dup, v_nombre_dup, v_cedula_dup, 'fusion', p_fase);

    DELETE FROM contactos WHERE id = p_dup;
END;
$function$
;

CREATE OR REPLACE FUNCTION public.ing_audit_contexto(p_clave text)
 RETURNS text
 LANGUAGE plpgsql
 STABLE
AS $function$
BEGIN
    RETURN nullif(btrim(current_setting(p_clave)), '');
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$function$
;

CREATE OR REPLACE FUNCTION public.ing_audit_contexto_int(p_clave text)
 RETURNS integer
 LANGUAGE plpgsql
 STABLE
AS $function$
DECLARE
    v text;
BEGIN
    v := ing_audit_contexto(p_clave);
    IF v IS NULL OR v !~ '^[0-9]+$' THEN
        RETURN NULL;
    END IF;
    RETURN v::integer;
END;
$function$
;

CREATE OR REPLACE FUNCTION public.ing_audit_estado_label(p_valor text)
 RETURNS text
 LANGUAGE plpgsql
 IMMUTABLE
AS $function$
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
$function$
;

CREATE OR REPLACE FUNCTION public.ing_audit_etiqueta(p_campo text, p_valor text)
 RETURNS text
 LANGUAGE plpgsql
 STABLE
AS $function$
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
$function$
;

CREATE OR REPLACE FUNCTION public.normaliza_cedula(p_ced text)
 RETURNS text
 LANGUAGE sql
 IMMUTABLE
AS $function$
    SELECT regexp_replace(COALESCE(p_ced, ''), '[^0-9]', '', 'g');
$function$
;

CREATE OR REPLACE FUNCTION public.normaliza_nombre(p_nom text)
 RETURNS text
 LANGUAGE sql
 STABLE
AS $function$
    SELECT upper(btrim(regexp_replace(unaccent(COALESCE(p_nom, '')), '\s+', ' ', 'g')));
$function$
;

CREATE OR REPLACE FUNCTION public.seleccionar_bodega_descarga(p_id_producto integer)
 RETURNS integer
 LANGUAGE plpgsql
AS $function$
DECLARE
    v_id integer;
BEGIN
    -- 1. Mayor stock positivo
    SELECT id_bodega INTO v_id
      FROM stock_productos
     WHERE id_producto = p_id_producto AND cantidad > 0
     ORDER BY cantidad DESC
     LIMIT 1;
    IF v_id IS NOT NULL THEN
        RETURN v_id;
    END IF;

    -- 2. Ultima bodega con movimiento donde el producto tuvo stock positivo
    SELECT id_bodega INTO v_id
      FROM movimientos_inventario
     WHERE id_producto = p_id_producto AND cantidad_nueva > 0
     ORDER BY fecha DESC, id DESC
     LIMIT 1;
    IF v_id IS NOT NULL THEN
        RETURN v_id;
    END IF;

    -- 3. Bodega del ultimo ingreso de mercancia de este producto
    SELECT ic.id_bodega INTO v_id
      FROM ingresos_mercancias_detalle imd
      INNER JOIN ingresos_mercancias_cabecera ic
              ON imd.id_ingreso_cabecera = ic.id
     WHERE imd.id_producto = p_id_producto
     ORDER BY ic.fecha DESC, ic.id DESC
     LIMIT 1;
    IF v_id IS NOT NULL THEN
        RETURN v_id;
    END IF;

    -- 4. Fallback
    RETURN 1;
END;
$function$
;



-- -----------------------------------------------------------------------------
-- 4. TRIGGERS DE LA AUDITORIA DE INGRESOS (5)
--    DROP + CREATE: la unica forma idempotente en 9.4 (no hay CREATE OR REPLACE
--    TRIGGER). Se recrean identicos; no hay perdida de informacion.
-- -----------------------------------------------------------------------------

DROP TRIGGER IF EXISTS trg_auditoria_ingresos_inmutable ON auditoria_ingresos;
CREATE TRIGGER trg_auditoria_ingresos_inmutable
    BEFORE DELETE OR UPDATE ON auditoria_ingresos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_ingresos_inmutable();

DROP TRIGGER IF EXISTS trg_auditoria_ingresos_truncate ON auditoria_ingresos;
CREATE TRIGGER trg_auditoria_ingresos_truncate
    BEFORE TRUNCATE ON auditoria_ingresos
    FOR EACH STATEMENT EXECUTE PROCEDURE fn_auditoria_ingresos_inmutable();

DROP TRIGGER IF EXISTS trg_auditoria_ingresos_campos_inmutable ON auditoria_ingresos_campos;
CREATE TRIGGER trg_auditoria_ingresos_campos_inmutable
    BEFORE DELETE OR UPDATE ON auditoria_ingresos_campos
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_ingresos_inmutable();

DROP TRIGGER IF EXISTS trg_auditoria_ingresos_campos_truncate ON auditoria_ingresos_campos;
CREATE TRIGGER trg_auditoria_ingresos_campos_truncate
    BEFORE TRUNCATE ON auditoria_ingresos_campos
    FOR EACH STATEMENT EXECUTE PROCEDURE fn_auditoria_ingresos_inmutable();

DROP TRIGGER IF EXISTS trg_auditoria_ingresos_productos ON ingresos_productos_cabecera;
CREATE TRIGGER trg_auditoria_ingresos_productos
    AFTER INSERT OR DELETE OR UPDATE ON ingresos_productos_cabecera
    FOR EACH ROW EXECUTE PROCEDURE fn_auditoria_ingresos();


-- -----------------------------------------------------------------------------
-- 5. VISTA v_auditoria_ingresos
--    La consume el panel de historial del listado de ingresos.
-- -----------------------------------------------------------------------------

CREATE OR REPLACE VIEW v_auditoria_ingresos AS
 SELECT a.id,
    a.fecha,
    a.hora,
    a.fecha_hora,
    a.operacion,
    a.id_registro,
    a.descripcion,
    ing_audit_estado_label(a.estado_anterior::text) AS estado_anterior,
    ing_audit_estado_label(a.estado_nuevo::text) AS estado_nuevo,
    COALESCE(a.nombre_usuario, a.usuario, a.usuario_bd) AS usuario_visible,
    a.origen,
    a.equipo,
    ( SELECT string_agg((((c.campo::text || ': '::text) || COALESCE(c.etiqueta_anterior, c.valor_anterior::character varying, '(vacio)'::character varying)::text) || ' -> '::text) || COALESCE(c.etiqueta_nueva, c.valor_nuevo::character varying, '(vacio)'::character varying)::text, ' | '::text) AS string_agg
           FROM auditoria_ingresos_campos c
          WHERE c.id_auditoria = a.id AND c.campo::text <> 'hora'::text) AS cambios
   FROM auditoria_ingresos a;


-- -----------------------------------------------------------------------------
-- 6. INDICES, FK Y SECUENCIAS DE TABLAS YA EXISTENTES
--
--    Verificado antes de generar el script: 0 filas huerfanas en
--    productos.id_proveedor y en impresoras.id_bodega, asi que las dos FK
--    entran sin rechazo.
-- -----------------------------------------------------------------------------

-- 6.1 Indice del kardex de producto.
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_mov_prod_tipo_fecha') THEN
        CREATE INDEX idx_mov_prod_tipo_fecha ON movimientos_inventario USING btree (id_producto, tipo, fecha DESC, id DESC);
    END IF;
END $$;

-- 6.2 Indices del panel de novedades de facturas.
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_novedades_codigo') THEN
        CREATE INDEX idx_novedades_codigo ON novedades_facturas USING btree (codigo_normalizado);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_novedades_estado') THEN
        CREATE INDEX idx_novedades_estado ON novedades_facturas USING btree (estado_revision);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_novedades_factura') THEN
        CREATE INDEX idx_novedades_factura ON novedades_facturas USING btree (numero_factura);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_novedades_fecha') THEN
        CREATE INDEX idx_novedades_fecha ON novedades_facturas USING btree (fecha_deteccion DESC);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_novedades_tipo') THEN
        CREATE INDEX idx_novedades_tipo ON novedades_facturas USING btree (tipo);
    END IF;
END $$;

-- 6.3 FK de integridad referencial.
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_provee'
                   AND conrelid='public.productos'::regclass) THEN
        ALTER TABLE ONLY productos ADD CONSTRAINT fk_provee FOREIGN KEY (id_proveedor) REFERENCES contactos(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_impresoras_bodega'
                   AND conrelid='public.impresoras'::regclass) THEN
        ALTER TABLE ONLY impresoras ADD CONSTRAINT fk_impresoras_bodega FOREIGN KEY (id_bodega) REFERENCES bodegas(id);
    END IF;
END $$;

-- 6.4 Indice de novedades de detalle_factura: aqui existe con el nombre viejo
--     idx_detalle_novedad y su definicion es identica a la del esquema actual.
--     Se renombra en vez de crear un duplicado; asi tambien pasa la query de
--     verificacion de wo-printer, que lo busca por nombre.
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_detalle_factura_novedad') THEN
        IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname='idx_detalle_novedad') THEN
            ALTER INDEX idx_detalle_novedad RENAME TO idx_detalle_factura_novedad;
        ELSE
            CREATE INDEX idx_detalle_factura_novedad ON detalle_factura USING btree (factura_id) WHERE (es_novedad = true);
        END IF;
    END IF;
END $$;

-- 6.5 Resincronizacion de secuencias (FASE 9 de wo-printer).
--     El sistema legado inserta con id explicito = MAX(id)+1 en vez de usar
--     nextval(), asi que las secuencias quedan atras. wo-printer inserta con
--     SERIAL + RETURNING id y revienta con "llave duplicada" cuando eso pasa.
--     Al generar este script estaban atrasadas facturas_cabeceras (1247 vs
--     1249), facturas_detalles (2388 vs 2390), users (1 vs 11) y bodegas
--     (1 vs 2). Mueve contadores, no filas. Idempotente.
DO $$
DECLARE
    t       text;
    tablas  text[] := ARRAY['facturas_cabeceras','facturas_detalles','facturas_impresas',
                            'detalle_factura','movimientos_inventario','contactos',
                            'users','productos','bodegas','stock'];
    max_id  bigint;
    seq     text;
BEGIN
    FOREACH t IN ARRAY tablas LOOP
        seq := t || '_id_seq';
        IF EXISTS (SELECT 1 FROM pg_class WHERE relkind='S' AND relname=seq) THEN
            EXECUTE format('SELECT COALESCE(MAX(id),0) FROM %I', t) INTO max_id;
            IF max_id > 0 THEN
                PERFORM setval(seq, max_id, true);
            ELSE
                PERFORM setval(seq, 1, false);
            END IF;
            RAISE NOTICE 'Secuencia % -> %', seq, max_id;
        END IF;
    END LOOP;
END $$;


-- -----------------------------------------------------------------------------
-- 7. DATOS DE CONFIGURACION (no datos de negocio)
--    Todo con guardas: re-ejecutar nunca pisa lo que ya administraste desde la
--    pantalla de permisos ni el activo/inactivo de cada modulo.
-- -----------------------------------------------------------------------------

-- 7.1 Perfiles del sistema. En las bases en produccion los perfiles 1-5 ya
--     existen con estos mismos nombres y solo se agregan 6-9; se listan los
--     nueve porque perfil_opciones (7.4) reparte opciones a todos y la FK
--     fk_po_perfil falla si alguno no existe.
--     (corregido 2026-08-30 al validar sobre una base recien creada)
INSERT INTO perfiles (id, perfil)
SELECT v.id, v.perfil
FROM (VALUES
    (1, 'admin'),
    (2, 'bodeguero'),
    (3, 'vendedor'),
    (4, 'facturacion'),
    (5, 'supervisor'),
    (6, 'Almacenista'),
    (7, 'Precios'),
    (8, 'CAJA'),
    (9, 'CARTERA')
) AS v(id, perfil)
WHERE NOT EXISTS (SELECT 1 FROM perfiles p WHERE p.id = v.id);

-- La secuencia queda por encima del maximo insertado.
SELECT setval('perfiles_id_seq', (SELECT max(id) FROM perfiles));

-- 7.2 Modulos licenciables. Todos encendidos: el dia 1 no cambia nada.
--     Para apagar uno en esta instalacion, ver seccion 8.
INSERT INTO modulos (clave, nombre, activo)
SELECT v.clave, v.nombre, true
FROM (VALUES
    ('Compras',  'Compras (pipeline de ordenes de compra)'),
    ('Precios',  'Precios (agroinsumos y comisiones)'),
    ('Creditos', 'Creditos (cartera y abonos)'),
    ('Caja',     'Caja (ingresos, egresos y fondos)'),
    ('Caja Dos', 'Caja Dos (segunda caja independiente)')
) AS v(clave, nombre)
WHERE NOT EXISTS (SELECT 1 FROM modulos m WHERE m.clave = v.clave);

-- 7.3 Catalogo de opciones de permisos (60).
INSERT INTO opciones (clave, nombre, modulo, componente, orden)
SELECT v.clave, v.nombre, v.modulo, v.componente, v.orden
FROM (VALUES
    ('menu_caja', 'Menu Caja (completo)', 'Caja', 'menuCaja', 10),
    ('caja_ingresos', 'Ingresos de dinero', 'Caja', 'itemCajaIngresos', 20),
    ('caja_egresos', 'Egresos de dinero', 'Caja', 'itemCajaEgresos', 30),
    ('caja_traslados', 'Traslados entre fondos', 'Caja', 'itemCajaTraslados', 40),
    ('caja_fondos', 'Fondos (cajas y bancos)', 'Caja', 'itemCajaFondos', 50),
    ('caja_cuentas_ingresos', 'Cuentas de ingresos', 'Caja', 'itemCajaCtasIngresos', 60),
    ('caja_cuentas_egresos', 'Cuentas de egresos', 'Caja', 'itemCajaCtasEgresos', 70),
    ('caja_reportes', 'Reportes de caja', 'Caja', 'itemCajaReportes', 80),
    ('menu_caja2', 'Menu Caja Dos (completo)', 'Caja Dos', 'menuCaja2', 10),
    ('caja2_ingresos', 'Ingresos de dinero', 'Caja Dos', 'itemCaja2Ingresos', 20),
    ('caja2_egresos', 'Egresos de dinero', 'Caja Dos', 'itemCaja2Egresos', 30),
    ('caja2_traslados', 'Traslados entre fondos', 'Caja Dos', 'itemCaja2Traslados', 40),
    ('caja2_fondos', 'Fondos (cajas y bancos)', 'Caja Dos', 'itemCaja2Fondos', 50),
    ('caja2_cuentas_ingresos', 'Cuentas de ingresos', 'Caja Dos', 'itemCaja2CtasIngresos', 60),
    ('caja2_cuentas_egresos', 'Cuentas de egresos', 'Caja Dos', 'itemCaja2CtasEgresos', 70),
    ('caja2_reportes', 'Reportes de caja', 'Caja Dos', 'itemCaja2Reportes', 80),
    ('menu_compras', 'Menu Compras (ordenes de compra)', 'Compras', 'menuCompras', 10),
    ('compras_crear', 'Crear orden de compra', 'Compras', 'btn_nueva', 20),
    ('compras_editar', 'Editar orden de compra', 'Compras', 'btn_editar', 30),
    ('jmenu_configuraciones', 'Configuraciones', 'Configuracion', 'jmenu_configuraciones', 10),
    ('jmenu_user', 'Usuarios', 'Configuracion', 'jmenu_user', 20),
    ('jmenu_backup', 'BackUp', 'Configuracion', 'jmenu_backup', 30),
    ('jMenu_tipo_ingreso', 'Tipo de ingreso', 'Configuracion', 'jMenu_tipo_ingreso', 40),
    ('jmenu_bodegas', 'Bodegas', 'Configuracion', 'jmenu_bodegas', 50),
    ('jMenu_unidades', 'Unidades de medida', 'Configuracion', 'jMenu_unidades', 60),
    ('jmenu_permisos', 'Permisos de la aplicacion', 'Configuracion', NULL, 70),
    ('jmenu_con', 'Contactos (menu)', 'Contactos', 'jmenu_con', 10),
    ('jmenu_contactos', 'Contactos (item de menu)', 'Contactos', 'jmenu_contactos', 20),
    ('btn_contactos', 'Contactos (boton)', 'Contactos', 'btn_contactos', 30),
    ('menu_creditos', 'Menu Creditos (completo)', 'Creditos', 'menuCreditos', 10),
    ('creditos_ver', 'Creditos (cartera)', 'Creditos', 'itemCreditosVer', 20),
    ('creditos_clientes', 'Clientes de credito', 'Creditos', 'itemCreditosClientes', 30),
    ('creditos_cuentas', 'Cuentas', 'Creditos', 'itemCreditosCuentas', 40),
    ('creditos_tipos_abonos', 'Tipos de abonos', 'Creditos', 'itemCreditosTipos', 50),
    ('creditos_reportes', 'Reportes de creditos', 'Creditos', 'itemCreditosReportes', 60),
    ('jMenu_ordenes', 'Ordenes (menu)', 'Ordenes', 'jMenu_ordenes', 10),
    ('ordenes_unir', 'Unir ordenes de entrega', 'Ordenes', 'btn_unir', 10),
    ('ordenes_entrega_masiva', 'Entrega masiva por bodega', 'Ordenes', 'btn_entregar_todo', 11),
    ('ordenes_reimprimir', 'Reimprimir orden sin limite', 'Ordenes', 'btn_verFactura', 12),
    ('ordenes_anular', 'Anular ordenes', 'Ordenes', 'btn_eliminar', 13),
    ('jmenu_facturacion', 'Generar orden (menu)', 'Ordenes', 'jmenu_facturacion', 20),
    ('btn_generar_orden', 'Generar orden (boton)', 'Ordenes', 'btn_generar_orden', 30),
    ('btn_ver_ordenes', 'Ver ordenes', 'Ordenes', 'btn_ver_ordenes', 40),
    ('ordenes_editar', 'Editar ordenes', 'Ordenes', 'btn_editar', 45),
    ('btn_facturar', 'Ordenes (boton)', 'Ordenes', 'btn_facturar', 50),
    ('btn_ver_facturas', 'Ver ordenes (boton)', 'Ordenes', 'btn_ver_facturas', 60),
    ('btn_decolucion', 'Devolucion', 'Ordenes', 'btn_decolucion', 70),
    ('menu_precios_ingresos', 'Ingresos de productos', 'Precios', 'itemIngresosPrecios', 10),
    ('menu_precios_productos', 'Precios de productos', 'Precios', 'itemPreciosProductos', 20),
    ('menu_precios_descuentos', 'Descuentos escalonados', 'Precios', 'itemDescuentosPrecios', 30),
    ('menu_precios_etiquetas', 'Imprimir etiquetas', 'Precios', 'itemEtiquetasPrecios', 40),
    ('menu_precios_comisiones', 'Analizar comisiones', 'Precios', 'itemComisionesPrecios', 45),
    ('menu_precios_reportes', 'Reportes', 'Precios', 'menuReportesPrecios', 50),
    ('menu_precios_config', 'Configuracion de precios', 'Precios', 'itemConfigPrecios', 60),
    ('jMenu_productos_principal', 'Productos (menu)', 'Productos', 'jMenu_productos_principal', 10),
    ('btn_productos', 'Productos (boton)', 'Productos', 'btn_productos', 20),
    ('btn_ingreso_productos', 'Ingreso de productos', 'Productos', 'btn_ingreso_productos', 30),
    ('jMenu_recortes', 'Recortes (menu)', 'Recortes', 'jMenu_recortes', 10),
    ('btn_generar_recorte', 'Generar recorte', 'Recortes', 'btn_generar_recorte', 20),
    ('btn_ver_recortes', 'Ver recortes', 'Recortes', 'btn_ver_recortes', 30)
) AS v(clave, nombre, modulo, componente, orden)
WHERE NOT EXISTS (SELECT 1 FROM opciones o WHERE o.clave = v.clave);

-- 7.4 Permisos por perfil (139). Se resuelven por clave, no por id, para no
--     depender del orden en que quedaron las opciones.
INSERT INTO perfil_opciones (id_perfil, id_opcion)
SELECT v.id_perfil, o.id
FROM (VALUES
    (1, 'menu_compras'),
    (1, 'compras_crear'),
    (1, 'compras_editar'),
    (1, 'jmenu_configuraciones'),
    (1, 'jmenu_user'),
    (1, 'jmenu_backup'),
    (1, 'jMenu_tipo_ingreso'),
    (1, 'jmenu_bodegas'),
    (1, 'jMenu_unidades'),
    (1, 'jmenu_permisos'),
    (1, 'jmenu_con'),
    (1, 'jmenu_contactos'),
    (1, 'btn_contactos'),
    (1, 'jMenu_ordenes'),
    (1, 'jmenu_facturacion'),
    (1, 'btn_generar_orden'),
    (1, 'btn_ver_ordenes'),
    (1, 'ordenes_editar'),
    (1, 'btn_facturar'),
    (1, 'btn_ver_facturas'),
    (1, 'btn_decolucion'),
    (1, 'menu_precios_ingresos'),
    (1, 'menu_precios_productos'),
    (1, 'menu_precios_descuentos'),
    (1, 'menu_precios_etiquetas'),
    (1, 'menu_precios_comisiones'),
    (1, 'menu_precios_reportes'),
    (1, 'menu_precios_config'),
    (1, 'jMenu_productos_principal'),
    (1, 'btn_productos'),
    (1, 'btn_ingreso_productos'),
    (1, 'jMenu_recortes'),
    (1, 'btn_generar_recorte'),
    (1, 'btn_ver_recortes'),
    (2, 'menu_compras'),
    (2, 'compras_crear'),
    (2, 'compras_editar'),
    (2, 'jMenu_ordenes'),
    (2, 'btn_generar_orden'),
    (2, 'btn_ver_ordenes'),
    (3, 'jMenu_ordenes'),
    (3, 'jmenu_facturacion'),
    (3, 'btn_generar_orden'),
    (3, 'btn_ver_ordenes'),
    (3, 'ordenes_editar'),
    (4, 'menu_compras'),
    (4, 'compras_crear'),
    (4, 'compras_editar'),
    (4, 'ordenes_editar'),
    (4, 'btn_facturar'),
    (4, 'btn_ver_facturas'),
    (4, 'btn_decolucion'),
    (5, 'menu_compras'),
    (5, 'compras_crear'),
    (5, 'compras_editar'),
    (5, 'jmenu_configuraciones'),
    (5, 'jmenu_user'),
    (5, 'jmenu_backup'),
    (5, 'jMenu_tipo_ingreso'),
    (5, 'jmenu_bodegas'),
    (5, 'jMenu_unidades'),
    (5, 'jmenu_permisos'),
    (5, 'jmenu_con'),
    (5, 'jmenu_contactos'),
    (5, 'btn_contactos'),
    (5, 'jMenu_ordenes'),
    (5, 'jmenu_facturacion'),
    (5, 'btn_generar_orden'),
    (5, 'btn_ver_ordenes'),
    (5, 'ordenes_editar'),
    (5, 'btn_facturar'),
    (5, 'btn_ver_facturas'),
    (5, 'btn_decolucion'),
    (5, 'menu_precios_ingresos'),
    (5, 'menu_precios_productos'),
    (5, 'menu_precios_descuentos'),
    (5, 'menu_precios_etiquetas'),
    (5, 'menu_precios_comisiones'),
    (5, 'menu_precios_reportes'),
    (5, 'menu_precios_config'),
    (5, 'jMenu_productos_principal'),
    (5, 'btn_productos'),
    (5, 'btn_ingreso_productos'),
    (6, 'menu_compras'),
    (6, 'compras_crear'),
    (6, 'compras_editar'),
    (6, 'jmenu_con'),
    (6, 'ordenes_editar'),
    (6, 'menu_precios_ingresos'),
    (6, 'menu_precios_etiquetas'),
    (6, 'menu_precios_comisiones'),
    (6, 'btn_productos'),
    (7, 'menu_compras'),
    (7, 'compras_crear'),
    (7, 'compras_editar'),
    (7, 'jmenu_con'),
    (7, 'jmenu_contactos'),
    (7, 'btn_contactos'),
    (7, 'ordenes_editar'),
    (7, 'menu_precios_ingresos'),
    (7, 'menu_precios_productos'),
    (7, 'menu_precios_descuentos'),
    (7, 'menu_precios_etiquetas'),
    (7, 'menu_precios_comisiones'),
    (7, 'menu_precios_reportes'),
    (7, 'menu_precios_config'),
    (7, 'jMenu_productos_principal'),
    (7, 'btn_productos'),
    (7, 'btn_ingreso_productos'),
    (8, 'menu_caja'),
    (8, 'caja_ingresos'),
    (8, 'caja_egresos'),
    (8, 'caja_traslados'),
    (8, 'caja_reportes'),
    (8, 'jmenu_con'),
    (8, 'jMenu_ordenes'),
    (8, 'jmenu_facturacion'),
    (8, 'btn_generar_orden'),
    (8, 'btn_ver_ordenes'),
    (8, 'ordenes_editar'),
    (8, 'btn_facturar'),
    (8, 'btn_ver_facturas'),
    (8, 'btn_decolucion'),
    (8, 'jMenu_productos_principal'),
    (9, 'menu_caja2'),
    (9, 'caja2_ingresos'),
    (9, 'caja2_egresos'),
    (9, 'caja2_traslados'),
    (9, 'caja2_fondos'),
    (9, 'caja2_cuentas_ingresos'),
    (9, 'caja2_cuentas_egresos'),
    (9, 'caja2_reportes'),
    (9, 'jmenu_con'),
    (9, 'menu_creditos'),
    (9, 'creditos_ver'),
    (9, 'creditos_clientes'),
    (9, 'creditos_cuentas'),
    (9, 'creditos_tipos_abonos'),
    (9, 'creditos_reportes')
) AS v(id_perfil, clave)
JOIN opciones o ON o.clave = v.clave
WHERE NOT EXISTS (
    SELECT 1 FROM perfil_opciones po
    WHERE po.id_perfil = v.id_perfil AND po.id_opcion = o.id
);


COMMIT;


-- #############################################################################
-- ##  PARTE B   INCREMENTOS                                                  ##
-- ##  Cada cambio nuevo del sistema se agrega AQUI, al final, con su fecha.  ##
-- #############################################################################


-- =============================================================================
-- B.1  (2026-08-30)  AUDITORIA DE CAJA
--      Recuperado: quedo por fuera de la linea base por error, aunque el
--      modulo Caja ya lo usa. Crea el libro inmutable auditoria_caja +
--      auditoria_caja_campos, las funciones caja_audit_*, fn_auditoria_caja,
--      los triggers de las 7 tablas de caja y la vista v_auditoria_caja.
--      Origen: sql/historico/migracion_auditoria_caja.sql
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
-- B.2  (2026-08-30)  CREDITOS FASE 2: cruce de saldos, caja, comisiones,
--      auditoria de cartera. Depende de B.1.
--      Origen: sql/historico/migracion_creditos_avanzado.sql
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
-- B.3  (2026-08-30)  NOTIFY DE ORDENES NUEVAS
--      Alimenta la barra de notificaciones de la app y el listener de
--      wo-printer. Los triggers se enganchan solo si la tabla existe.
--      Origen: sql/controlbodega.sql
-- =============================================================================

BEGIN;


-- ============================================================================
-- Auto-impresión de órdenes: trigger que emite NOTIFY al insertar
-- una factura distinta de 'Venta'.
-- ============================================================================
CREATE OR REPLACE FUNCTION fn_notify_orden_nueva()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.tipo_factura IS DISTINCT FROM 'Venta' THEN
        PERFORM pg_notify(
            'orden_nueva',
            NEW.id::text                              || '|' ||
            COALESCE(NEW.id_bodega::text, '')         || '|' ||
            COALESCE(NEW.id_user::text, '')           || '|' ||
            COALESCE(NEW.tipo_factura, '')
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $wrap$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'facturas_cabeceras') THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_notify_orden_nueva ON facturas_cabeceras';
        EXECUTE 'CREATE TRIGGER trg_notify_orden_nueva AFTER INSERT ON facturas_cabeceras '
             || 'FOR EACH ROW EXECUTE PROCEDURE fn_notify_orden_nueva()';
    END IF;
END $wrap$;


-- ============================================================================
-- Trigger que notifica facturas que son SOLO novedades.
-- Cuando wo-printer recibe una factura donde TODOS los ítems son novedades
-- (códigos inválidos / no existen / inactivos / cantidad cero), NO inserta
-- ninguna fila en facturas_cabeceras. El trigger normal nunca dispara y el
-- listener no se entera. Este trigger se dispara en facturas_impresas y emite
-- NOTIFY sólo cuando no existe ninguna cabecera para esa numero_factura.
-- ============================================================================
CREATE OR REPLACE FUNCTION fn_notify_factura_solo_novedad()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM facturas_cabeceras
        WHERE codigo = NEW.numero_factura
    ) THEN
        PERFORM pg_notify(
            'orden_nueva',
            'NOVEDAD|' || NEW.id::text || '|' || COALESCE(NEW.numero_factura, '')
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


DO $wrap$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = current_schema() AND table_name = 'facturas_impresas') THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_notify_factura_solo_novedad ON facturas_impresas';
        EXECUTE 'CREATE TRIGGER trg_notify_factura_solo_novedad AFTER INSERT ON facturas_impresas '
             || 'FOR EACH ROW EXECUTE PROCEDURE fn_notify_factura_solo_novedad()';
    END IF;
END $wrap$;

COMMIT;


-- =============================================================================
-- B.4  (2026-08-30)  ENTREGAS RAPIDAS: log de lecturas QR
--      Origen: sql/historico/migracion_escaneos_qr_ordenes.sql
-- =============================================================================

BEGIN;

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

COMMIT;


-- =============================================================================
-- B.5  (2026-08-30)  AJUSTES DE INVENTARIO: columnas de pendientes
--      Permite que un ajuste tambien mueva la cantidad reservada contra
--      ordenes, no solo la fisica. Reescrito con guardas 9.4.
--      Origen: sql/historico/migracion_ajuste_pendientes.sql
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_name = 'ajustes_inventario_detalle')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_name = 'ajustes_inventario_detalle'
                         AND column_name = 'pendientes_anterior') THEN
        ALTER TABLE ajustes_inventario_detalle
            ADD COLUMN pendientes_anterior   double precision NOT NULL DEFAULT 0,
            ADD COLUMN pendientes_nuevo      double precision NOT NULL DEFAULT 0,
            ADD COLUMN diferencia_pendientes double precision NOT NULL DEFAULT 0;
    END IF;
END $$;

COMMIT;


-- =============================================================================
-- B.6  (2026-08-30)  CATALOGO MINIMO DE TIPOS DE ABONO
--      Sin al menos una fila el combo de "tipo de abono" de las pantallas de
--      pago queda vacio y no se puede registrar ningun abono. Solo se siembra
--      si la tabla esta VACIA: nunca pisa el catalogo del cliente.
-- =============================================================================

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM tipos_abonos) THEN
        INSERT INTO tipos_abonos (id, nombre, color, anticipo) VALUES
            (1, 'EFECTIVO',      'VERDE',    0),
            (2, 'TRANSFERENCIA', 'AZUL',     0),
            (3, 'CHEQUE',        'AMARILLO', 0),
            (4, 'ANTICIPO',      'NARANJA',  1);
        PERFORM setval('tipos_abonos_id_seq', (SELECT MAX(id) FROM tipos_abonos));
    END IF;
END $$;

COMMIT;


-- =============================================================================
-- B.7  (2026-08-30)  PERMISO: ELIMINAR ENTREGAS DE MERCANCIA
--      Reemplaza la clave de administrador que pedia el borrado de una entrega
--      desde la tabla de entregas de frm_ver_orden (tecla SUPR sobre
--      jtabla_entregados_cabecera). Al eliminar la entrega se reversa el stock,
--      por eso NO se concede a ningun perfil: es una propiedad por usuario que
--      se otorga desde la pantalla de permisos (tabla usuario_opciones). El
--      perfil 1 (Admin) la tiene siempre por ser admin.
-- =============================================================================

BEGIN;

INSERT INTO opciones (clave, nombre, modulo, componente, orden)
SELECT v.clave, v.nombre, v.modulo, v.componente, v.orden
FROM (VALUES
    ('ordenes_eliminar_entrega', 'Eliminar entregas de mercancia', 'Ordenes', 'jtabla_entregados_cabecera', 14)
) AS v (clave, nombre, modulo, componente, orden)
WHERE NOT EXISTS (SELECT 1 FROM opciones o WHERE o.clave = v.clave);

COMMIT;




-- #############################################################################
-- ##  PARTE C   PASOS QUE REQUIEREN DECISION                                 ##
-- ##  Fuera de transaccion y TODOS COMENTADOS. Descomenta y ejecuta a mano   ##
-- ##  solo los que apliquen a esta instalacion.                              ##
-- #############################################################################

-- -----------------------------------------------------------------------------
-- C.1 MODO DE PRECIOS
--     AGRO  (por defecto)  venta = precio de venta, valor_desc_1/2 = descuentos
--     TECNI                venta = Precio 1, valor_desc_1 = Precio 2,
--                          valor_desc_2 = Precio 3, sin descuentos escalonados
--                          y sin precio de credito.
--     Decidelo ANTES de cargar precios: cambia la interpretacion de esas
--     columnas en todos los productos.
--
--     UPDATE configuraciones SET modo_precios = 'TECNI' WHERE id = 1;

-- -----------------------------------------------------------------------------
-- C.2 MODULOS A APAGAR EN ESTA INSTALACION
--     Los 5 quedan encendidos. Apaga los que este cliente no compro: el modulo
--     desaparece de los menus para todos, incluso para el Admin.
--
--     UPDATE modulos SET activo = false WHERE clave = 'Compras';
--     UPDATE modulos SET activo = false WHERE clave = 'Creditos';
--     UPDATE modulos SET activo = false WHERE clave = 'Caja Dos';

-- -----------------------------------------------------------------------------
-- C.3 DESCUENTOS ESCALONADOS (solo si el modo queda en AGRO)
--     En modo TECNI la tabla descuentos no se usa y debe quedar vacia.
--
--     INSERT INTO descuentos (tipo, utilidad, descuento) VALUES
--         (1, 5, 5), (1, 50, 15), (1, 100, 17), (1, 100000, 30),
--         (2, 5, 7), (2, 50, 20), (2, 100, 25), (2, 100000, 35);

-- -----------------------------------------------------------------------------
-- C.4 ESCALA DE COMISIONES DE CARTERA (modulo Creditos)
--     porcentajes_comision nace vacia a proposito: la escala es una decision
--     comercial. Se lee ordenada por dias y se toma el primer rango cuyo
--     "dias" es mayor o igual a los dias que tardo el cobro. Con la tabla
--     vacia el reporte de comisiones muestra 0 y lo avisa.
--
--     INSERT INTO porcentajes_comision (dias, porcentaje) VALUES
--         (30, 3), (60, 2), (90, 1);

-- -----------------------------------------------------------------------------
-- C.5 DEDUPLICACION DE CONTACTOS
--     Los indices unicos de cedula NO se crean automaticamente porque fallan
--     si hay duplicados. Primero diagnostica:
--
--     SELECT cedula, count(*) FROM contactos
--      WHERE cedula IS NOT NULL AND cedula <> ''
--      GROUP BY 1 HAVING count(*) > 1 ORDER BY 2 DESC;
--
--     SELECT normaliza_cedula(cedula) AS ced, count(*) FROM contactos
--      WHERE cedula ~ '^[0-9]+(-[0-9]+)?$'
--      GROUP BY 1 HAVING count(*) > 1 ORDER BY 2 DESC;
--
--     Si las dos salen vacias, se pueden crear ya:
--
--     CREATE UNIQUE INDEX contactos_cedula_unique
--         ON contactos USING btree (cedula)
--         WHERE (cedula IS NOT NULL AND cedula::text <> ''::text);
--     CREATE UNIQUE INDEX contactos_cedula_norm_unique
--         ON contactos USING btree (normaliza_cedula(cedula::text))
--         WHERE (cedula::text ~ '^[0-9]+(-[0-9]+)?$'::text);
--
--     Si sale con filas, corre antes sql/dedup_contactos_02..04 (esos SI
--     modifican datos) y despues sql/dedup_contactos_05_prevencion.sql.

-- -----------------------------------------------------------------------------
-- C.6 SCRIPTS DE DATOS, POR CLIENTE (NO forman parte de esta actualizacion)
--     Se corren a mano, una sola vez, y solo donde apliquen:
--       carga_inicial_tecnirepuestos.sql          alta inicial TECNIREPUESTOS
--       sql/migracion_stock_minimo_unidades.sql   SOBRESCRIBE stock_minimo
--       sql/dedup_contactos_*.sql                 fusion de contactos duplicados
--       sql/unificacion_usuarios.sql              fusion de usuarios duplicados
--       sql/reparar_entregas_bodega_distinta_grupoB.sql

-- -----------------------------------------------------------------------------
-- C.7 LIMPIEZAS OPCIONALES
--     contactos.contacto_maestro es un residuo de una migracion vieja; existe
--     en algunas bases y ningun .java la usa.
--
--     ALTER TABLE contactos DROP COLUMN IF EXISTS contacto_maestro;
--
--     impresoras.tipo_notificaciones quedo NULLABLE en algunas bases y es
--     NOT NULL DEFAULT false en el esquema actual. La app lee con
--     rs.getBoolean() (NULL se lee como false), asi que funciona igual.
--     Para alinearlo:
--
--     UPDATE impresoras SET tipo_notificaciones = false WHERE tipo_notificaciones IS NULL;
--     ALTER TABLE impresoras ALTER COLUMN tipo_notificaciones SET DEFAULT false;
--     ALTER TABLE impresoras ALTER COLUMN tipo_notificaciones SET NOT NULL;





-- #############################################################################
-- ##  PARTE D   VERIFICACION (solo lectura)                                  ##
-- #############################################################################

-- D.1 Objetos que este archivo garantiza. Las 24 filas deben salir en t.
--     Una f significa que la actualizacion no quedo completa: revisa el log
--     de psql buscando el ERROR.
SELECT 'ext pg_trgm'                  AS objeto, EXISTS (SELECT 1 FROM pg_extension WHERE extname='pg_trgm') AS ok
UNION ALL SELECT 'tabla opciones',              EXISTS (SELECT 1 FROM information_schema.tables  WHERE table_name='opciones')
UNION ALL SELECT 'tabla modulos',               EXISTS (SELECT 1 FROM information_schema.tables  WHERE table_name='modulos')
UNION ALL SELECT 'tabla usuario_opciones',      EXISTS (SELECT 1 FROM information_schema.tables  WHERE table_name='usuario_opciones')
UNION ALL SELECT 'Compras ordenes_compra_cab',  EXISTS (SELECT 1 FROM information_schema.tables  WHERE table_name='ordenes_compra_cabecera')
UNION ALL SELECT 'Precios descuentos',          EXISTS (SELECT 1 FROM information_schema.tables  WHERE table_name='descuentos')
UNION ALL SELECT 'Creditos creditos',           EXISTS (SELECT 1 FROM information_schema.tables  WHERE table_name='creditos')
UNION ALL SELECT 'Creditos porcentajes_comis',  EXISTS (SELECT 1 FROM information_schema.tables  WHERE table_name='porcentajes_comision')
UNION ALL SELECT 'Caja ingresos',               EXISTS (SELECT 1 FROM information_schema.tables  WHERE table_name='ingresos')
UNION ALL SELECT 'Caja Dos ingresos.id_caja',   EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='ingresos' AND column_name='id_caja')
UNION ALL SELECT 'auditoria_caja',              EXISTS (SELECT 1 FROM information_schema.tables  WHERE table_name='auditoria_caja')
UNION ALL SELECT 'auditoria_ingresos',          EXISTS (SELECT 1 FROM information_schema.tables  WHERE table_name='auditoria_ingresos')
UNION ALL SELECT 'vista v_auditoria_caja',      EXISTS (SELECT 1 FROM information_schema.views   WHERE table_name='v_auditoria_caja')
UNION ALL SELECT 'vista v_auditoria_creditos',  EXISTS (SELECT 1 FROM information_schema.views   WHERE table_name='v_auditoria_creditos')
UNION ALL SELECT 'vista v_auditoria_ingresos',  EXISTS (SELECT 1 FROM information_schema.views   WHERE table_name='v_auditoria_ingresos')
UNION ALL SELECT 'fn fn_auditoria_caja',        EXISTS (SELECT 1 FROM pg_proc WHERE proname='fn_auditoria_caja')
UNION ALL SELECT 'fn asignar_bodegas_entrega',  EXISTS (SELECT 1 FROM pg_proc WHERE proname='asignar_bodegas_entrega')
UNION ALL SELECT 'fn seleccionar_bodega_desc',  EXISTS (SELECT 1 FROM pg_proc WHERE proname='seleccionar_bodega_descarga')
UNION ALL SELECT 'fn fn_notify_orden_nueva',    EXISTS (SELECT 1 FROM pg_proc WHERE proname='fn_notify_orden_nueva')
UNION ALL SELECT 'escaneos_qr_ordenes',         EXISTS (SELECT 1 FROM information_schema.tables  WHERE table_name='escaneos_qr_ordenes')
UNION ALL SELECT 'productos_unidades_entrega',  EXISTS (SELECT 1 FROM information_schema.tables  WHERE table_name='productos_unidades_entrega')
UNION ALL SELECT 'configuraciones.modo_precios',EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='configuraciones' AND column_name='modo_precios')
UNION ALL SELECT 'bodegas.entrega_automatica',  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='bodegas' AND column_name='entrega_automatica')
UNION ALL SELECT 'facturas_impresas.nit_client',EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='facturas_impresas' AND column_name='nit_cliente');

-- D.2 Catalogos del sistema. Los minimos que debe haber despues de correr esto.
SELECT 'perfiles'        AS catalogo, count(*) AS hay, 9  AS minimo_esperado FROM perfiles
UNION ALL SELECT 'modulos',           count(*), 5   FROM modulos
UNION ALL SELECT 'opciones',          count(*), 65  FROM opciones
UNION ALL SELECT 'perfil_opciones',   count(*), 139 FROM perfil_opciones;

-- D.3 Los datos del negocio no se tocaron: estos conteos deben ser IDENTICOS
--     a los de antes de correr el script.
SELECT 'productos' AS tabla, count(*) FROM productos
UNION ALL SELECT 'contactos',              count(*) FROM contactos
UNION ALL SELECT 'users',                  count(*) FROM users
UNION ALL SELECT 'facturas_cabeceras',     count(*) FROM facturas_cabeceras
UNION ALL SELECT 'movimientos_inventario', count(*) FROM movimientos_inventario;

-- D.4 Permisos por usuario concedidos en esta base (el script no los toca).
-- SELECT u.user_name, o.clave, uo.concedido
--   FROM usuario_opciones uo
--   JOIN users u    ON u.id = uo.id_user
--   JOIN opciones o ON o.id = uo.id_opcion
--  ORDER BY 1, 2;
