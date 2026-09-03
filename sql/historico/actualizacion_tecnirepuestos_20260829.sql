-- =============================================================================
-- ACTUALIZACION CONSOLIDADA  ->  bodega_tecnirepuestos
--
-- Objetivo: llevar la base de TECNIREPUESTOS DEL SUR (congelada en el estado
--           del 17-may-2026) al esquema actual de controlbodega, SIN TOCAR
--           NINGUN DATO DE NEGOCIO EXISTENTE.
--
-- Generado: 2026-08-29   |   Motor destino: PostgreSQL 9.4
--
-- Reemplaza la aplicacion encadenada de estas migraciones:
--     sql/migracion_permisos.sql                  (borrada del repo en e519e3c)
--     sql/migracion_permisos_compras.sql
--     sql/migracion_compras_pipeline.sql
--     sql/migracion_comisiones_precios.sql
--     sql/migracion_permisos_unir_ordenes.sql
--     sql/migracion_permisos_ordenes_editar.sql
--     sql/migracion_bodega_colores.sql
--     sql/migracion_bodegas_orden_automatica.sql  (borrada del repo en e519e3c)
--     sql/migracion_fusion_agro.sql
--     sql/migracion_bodega_entrega_automatica.sql
--     sql/migracion_permisos_entrega_masiva.sql
--     sql/asignar_bodegas_entrega_funcion.sql
--     sql/actualizacion_produccion_20260723.sql   (modulos + Creditos + Caja)
--     sql/migracion_modulo_caja.sql / migracion_caja_dos.sql
--     sql/migracion_recibo_caja.sql / migracion_auditoria_caja.sql
--     sql/migracion_permisos_anular_orden.sql
--     sql/migracion_permisos_reimprimir_orden.sql
--     sql/migracion_auditoria_ingresos.sql
--     sql/migracion_modo_precios.sql
--
-- Cubre ademas lo que exige wo-printer (proyecto aparte que escribe sobre esta
-- misma base): equivale a correr su ../wo-printer/sql/migracion_produccion.sql
-- completo (fases 1.3, 6.1, 6.2, 9, 10 y 11) mas el reparto por unidades de
-- entrega de su ultimo commit.
--
-- QUE HACE (todo aditivo e idempotente / re-ejecutable):
--   1. Habilita las extensiones pg_trgm y unaccent.
--   2. Agrega 31 columnas NUEVAS a 6 tablas existentes (productos, contactos,
--      configuraciones, users, bodegas, facturas_impresas) y ensancha
--      contactos.contacto 20->200.
--   3. Crea 36 tablas nuevas con sus 23 secuencias, 38 PK/UNIQUE, 63 FK y
--      34 indices  (permisos, Compras, Precios, Creditos, Caja, Caja Dos,
--      auditoria de ingresos, unidades de entrega, dedup de contactos).
--   4. Crea 11 funciones, 5 triggers y la vista v_auditoria_ingresos.
--   5. Agrega los indices y FK que faltaban en tablas ya existentes, renombra
--      idx_detalle_novedad -> idx_detalle_factura_novedad y resincroniza las
--      secuencias que quedaron atras del MAX(id).
--   6. Siembra SOLO datos de configuracion, con guardas WHERE NOT EXISTS:
--        perfiles 6-9  (Almacenista, Precios, CAJA, CARTERA)
--        modulos       (5 filas: Compras, Precios, Creditos, Caja, Caja Dos)
--        opciones      (60 filas: catalogo de permisos)
--        perfil_opciones (139 filas: permisos por perfil)
--
-- QUE NO HACE:
--   * NO ejecuta ningun UPDATE ni DELETE sobre filas existentes, salvo el
--     UPDATE de configuraciones.modo_precios sobre la columna que el mismo
--     script acaba de crear (la deja en AGRO). La resincronizacion de
--     secuencias de la seccion 6.5 mueve contadores, no filas.
--   * NO siembra datos de negocio: fondos, cuentas, tipos_abonos y descuentos
--     quedan VACIAS; se cargan desde la app.
--   * NO copia usuario_opciones ni usuario_roles_precios de la base de
--     desarrollo: son excepciones por usuario y los users son distintos.
--   * NO crea los indices unicos de cedula de contactos (dedup): exigen datos
--     limpios. Ver seccion 8 al final.
--   * NO crea la tabla migracion_mapeo (bitacora de la migracion agro).
--
-- TODO EN UNA SOLA TRANSACCION: si algo falla, no se aplica nada (rollback).
--
-- COMO EJECUTAR:
--   "C:\Program Files\PostgreSQL\9.4\bin\psql.exe" -h <IP> -p 5432 ^
--       -U postgres -d bodega_tecnirepuestos -v ON_ERROR_STOP=1 ^
--       -f sql\actualizacion_tecnirepuestos_20260829.sql
--
--   >> HAZ UN BACKUP (pg_dump) DE LA BASE ANTES DE CORRERLO. <<
--   >> CORRELO CON LOS USUARIOS FUERA DE LA APP (crea tablas y toma locks). <<
--
-- Al final, FUERA de la transaccion, hay:
--   * seccion 8: pasos que requieren decision tuya (modo TECNI, modulos a
--     apagar, dedup de contactos), todos comentados.
--   * seccion 9: verificacion de solo lectura.
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='abonos_cabeceras_pkey'
                   AND conrelid='public.abonos_cabeceras'::regclass) THEN
        ALTER TABLE ONLY abonos_cabeceras ADD CONSTRAINT abonos_cabeceras_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='abonos_pkey'
                   AND conrelid='public.abonos'::regclass) THEN
        ALTER TABLE ONLY abonos ADD CONSTRAINT abonos_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_cabecera_pkey'
                   AND conrelid='public.comparativos_cabecera'::regclass) THEN
        ALTER TABLE ONLY comparativos_cabecera ADD CONSTRAINT comparativos_cabecera_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_precios_pkey'
                   AND conrelid='public.comparativos_precios'::regclass) THEN
        ALTER TABLE ONLY comparativos_precios ADD CONSTRAINT comparativos_precios_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_productos_pkey'
                   AND conrelid='public.comparativos_productos'::regclass) THEN
        ALTER TABLE ONLY comparativos_productos ADD CONSTRAINT comparativos_productos_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='comparativos_proveedores_pkey'
                   AND conrelid='public.comparativos_proveedores'::regclass) THEN
        ALTER TABLE ONLY comparativos_proveedores ADD CONSTRAINT comparativos_proveedores_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='cotizaciones_compra_cabecera_pkey'
                   AND conrelid='public.cotizaciones_compra_cabecera'::regclass) THEN
        ALTER TABLE ONLY cotizaciones_compra_cabecera ADD CONSTRAINT cotizaciones_compra_cabecera_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='cotizaciones_compra_detalle_pkey'
                   AND conrelid='public.cotizaciones_compra_detalle'::regclass) THEN
        ALTER TABLE ONLY cotizaciones_compra_detalle ADD CONSTRAINT cotizaciones_compra_detalle_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='cuentas_pkey'
                   AND conrelid='public.cuentas'::regclass) THEN
        ALTER TABLE ONLY cuentas ADD CONSTRAINT cuentas_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='dedup_contactos_log_pkey'
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='modulos_pkey'
                   AND conrelid='public.modulos'::regclass) THEN
        ALTER TABLE ONLY modulos ADD CONSTRAINT modulos_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ordenes_compra_cabecera_pkey'
                   AND conrelid='public.ordenes_compra_cabecera'::regclass) THEN
        ALTER TABLE ONLY ordenes_compra_cabecera ADD CONSTRAINT ordenes_compra_cabecera_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='ordenes_compra_detalle_pkey'
                   AND conrelid='public.ordenes_compra_detalle'::regclass) THEN
        ALTER TABLE ONLY ordenes_compra_detalle ADD CONSTRAINT ordenes_compra_detalle_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_auditoria_ingresos'
                   AND conrelid='public.auditoria_ingresos'::regclass) THEN
        ALTER TABLE ONLY auditoria_ingresos ADD CONSTRAINT pk_auditoria_ingresos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_auditoria_ingresos_campos'
                   AND conrelid='public.auditoria_ingresos_campos'::regclass) THEN
        ALTER TABLE ONLY auditoria_ingresos_campos ADD CONSTRAINT pk_auditoria_ingresos_campos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_caja_ingresos'
                   AND conrelid='public.ingresos'::regclass) THEN
        ALTER TABLE ONLY ingresos ADD CONSTRAINT pk_caja_ingresos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_creditos'
                   AND conrelid='public.creditos'::regclass) THEN
        ALTER TABLE ONLY creditos ADD CONSTRAINT pk_creditos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_cuentas_egresos'
                   AND conrelid='public.cuentas_egresos'::regclass) THEN
        ALTER TABLE ONLY cuentas_egresos ADD CONSTRAINT pk_cuentas_egresos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_cuentas_ingresos'
                   AND conrelid='public.cuentas_ingresos'::regclass) THEN
        ALTER TABLE ONLY cuentas_ingresos ADD CONSTRAINT pk_cuentas_ingresos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_descuentos_precios'
                   AND conrelid='public.descuentos'::regclass) THEN
        ALTER TABLE ONLY descuentos ADD CONSTRAINT pk_descuentos_precios PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_egresos'
                   AND conrelid='public.egresos'::regclass) THEN
        ALTER TABLE ONLY egresos ADD CONSTRAINT pk_egresos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_fondos'
                   AND conrelid='public.fondos'::regclass) THEN
        ALTER TABLE ONLY fondos ADD CONSTRAINT pk_fondos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_fotos_registros'
                   AND conrelid='public.fotos_registros'::regclass) THEN
        ALTER TABLE ONLY fotos_registros ADD CONSTRAINT pk_fotos_registros PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_ingresos_productos_cabecera'
                   AND conrelid='public.ingresos_productos_cabecera'::regclass) THEN
        ALTER TABLE ONLY ingresos_productos_cabecera ADD CONSTRAINT pk_ingresos_productos_cabecera PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_ingresos_productos_detalle'
                   AND conrelid='public.ingresos_productos_detalle'::regclass) THEN
        ALTER TABLE ONLY ingresos_productos_detalle ADD CONSTRAINT pk_ingresos_productos_detalle PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_opciones'
                   AND conrelid='public.opciones'::regclass) THEN
        ALTER TABLE ONLY opciones ADD CONSTRAINT pk_opciones PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_pagos_ingresos_productos'
                   AND conrelid='public.pagos_ingresos_productos'::regclass) THEN
        ALTER TABLE ONLY pagos_ingresos_productos ADD CONSTRAINT pk_pagos_ingresos_productos PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_perfil_opciones'
                   AND conrelid='public.perfil_opciones'::regclass) THEN
        ALTER TABLE ONLY perfil_opciones ADD CONSTRAINT pk_perfil_opciones PRIMARY KEY (id_perfil, id_opcion);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_transferencias'
                   AND conrelid='public.transferencias'::regclass) THEN
        ALTER TABLE ONLY transferencias ADD CONSTRAINT pk_transferencias PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_usuario_opciones'
                   AND conrelid='public.usuario_opciones'::regclass) THEN
        ALTER TABLE ONLY usuario_opciones ADD CONSTRAINT pk_usuario_opciones PRIMARY KEY (id_user, id_opcion);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='pk_usuario_roles_precios'
                   AND conrelid='public.usuario_roles_precios'::regclass) THEN
        ALTER TABLE ONLY usuario_roles_precios ADD CONSTRAINT pk_usuario_roles_precios PRIMARY KEY (id_user, rol);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='producto_proveedores_pkey'
                   AND conrelid='public.producto_proveedores'::regclass) THEN
        ALTER TABLE ONLY producto_proveedores ADD CONSTRAINT producto_proveedores_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='productos_unidades_entrega_pkey'
                   AND conrelid='public.productos_unidades_entrega'::regclass) THEN
        ALTER TABLE ONLY productos_unidades_entrega ADD CONSTRAINT productos_unidades_entrega_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='sugeridos_cabecera_pkey'
                   AND conrelid='public.sugeridos_cabecera'::regclass) THEN
        ALTER TABLE ONLY sugeridos_cabecera ADD CONSTRAINT sugeridos_cabecera_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='sugeridos_detalle_pkey'
                   AND conrelid='public.sugeridos_detalle'::regclass) THEN
        ALTER TABLE ONLY sugeridos_detalle ADD CONSTRAINT sugeridos_detalle_pkey PRIMARY KEY (id);
    END IF;
END $$;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='tipos_abonos_pkey'
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

-- 7.1 Perfiles 6-9. Los perfiles 1-5 ya existen con los mismos nombres.
INSERT INTO perfiles (id, perfil)
SELECT v.id, v.perfil
FROM (VALUES
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

-- 7.3 Catalogo de opciones de permisos (61).
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
    ('ordenes_eliminar_entrega', 'Eliminar entregas de mercancia', 'Ordenes', 'jtabla_entregados_cabecera', 14),
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


-- =============================================================================
-- 8. PASOS QUE REQUIEREN DECISION  (fuera de la transaccion, TODOS COMENTADOS)
--    Descomenta y ejecuta solo los que apliquen a esta instalacion.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 8.1 MODO DE PRECIOS
--     La empresa es TECNIREPUESTOS, asi que lo esperable es el modo TECNI:
--     3 margenes independientes -> Precio 1 / Precio 2 / Precio 3, sin
--     descuentos escalonados y sin precio de credito.
--
--     OJO: esto cambia la interpretacion de las columnas de productos:
--       modo AGRO   venta=precio de venta, valor_desc_1/2=descuentos
--       modo TECNI  venta=Precio 1, valor_desc_1=Precio 2, valor_desc_2=Precio 3
--     Como los productos de esta base todavia tienen esas columnas en NULL,
--     ahora es el momento barato de decidirlo.
--
--     UPDATE configuraciones SET modo_precios = 'TECNI' WHERE id = 1;

-- -----------------------------------------------------------------------------
-- 8.2 MODULOS A APAGAR EN ESTA INSTALACION
--     Los 5 quedan encendidos. Apaga los que este cliente no compro: el modulo
--     desaparece de los menus para todos, incluso para el Admin.
--
--     UPDATE modulos SET activo = false WHERE clave = 'Compras';
--     UPDATE modulos SET activo = false WHERE clave = 'Creditos';
--     UPDATE modulos SET activo = false WHERE clave = 'Caja Dos';

-- -----------------------------------------------------------------------------
-- 8.3 DESCUENTOS ESCALONADOS (solo si el modo queda en AGRO)
--     En modo TECNI la tabla descuentos no se usa y debe quedar vacia.
--
--     INSERT INTO descuentos (tipo, utilidad, descuento) VALUES
--         (1, 5, 5), (1, 50, 15), (1, 100, 17), (1, 100000, 30),
--         (2, 5, 7), (2, 50, 20), (2, 100, 25), (2, 100000, 35);

-- -----------------------------------------------------------------------------
-- 8.4 DEDUPLICACION DE CONTACTOS
--     Los indices unicos de cedula NO se crean aqui porque fallan si hay
--     duplicados. Primero diagnostica:
--
--     SELECT cedula, count(*) FROM contactos
--      WHERE cedula IS NOT NULL AND cedula <> ''
--      GROUP BY 1 HAVING count(*) > 1 ORDER BY 2 DESC;
--
--     SELECT normaliza_cedula(cedula) AS ced, count(*) FROM contactos
--      WHERE cedula ~ '^[0-9]+(-[0-9]+)?$'
--      GROUP BY 1 HAVING count(*) > 1 ORDER BY 2 DESC;
--
--     Si las dos salen vacias, se pueden crear ya (definicion exacta del
--     esquema actual):
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
-- 8.5 COLUMNA MUERTA
--     contactos.contacto_maestro existe en esta base y no en el esquema actual;
--     ningun .java la usa. Es un residuo de una migracion vieja.
--
--     ALTER TABLE contactos DROP COLUMN IF EXISTS contacto_maestro;

-- -----------------------------------------------------------------------------
-- 8.6 DIFERENCIA COSMETICA
--     impresoras.tipo_notificaciones es NULLABLE aqui y NOT NULL DEFAULT false
--     en el esquema actual. La app lee con rs.getBoolean(), que devuelve false
--     ante NULL, asi que funciona igual. Para alinearlo:
--
--     UPDATE impresoras SET tipo_notificaciones = false WHERE tipo_notificaciones IS NULL;
--     ALTER TABLE impresoras ALTER COLUMN tipo_notificaciones SET DEFAULT false;
--     ALTER TABLE impresoras ALTER COLUMN tipo_notificaciones SET NOT NULL;

-- -----------------------------------------------------------------------------
-- 8.7 DATOS PROPIOS DE ESTE CLIENTE
--     Revisa si ya se aplicaron; no forman parte de esta actualizacion:
--       carga_inicial_tecnirepuestos.sql
--       sql/migracion_stock_minimo_unidades.sql   (SOBRESCRIBE stock_minimo)


-- =============================================================================
-- 9. VERIFICACION (solo lectura). Ejecutala despues del COMMIT.
-- =============================================================================

-- 73 tablas y no 74 como bodega_nuevo: aqui no se crea migracion_mapeo.
-- 56 secuencias y no 44: esta base arrastra 33 secuencias heredadas (12 mas
-- que el esquema de desarrollo) y el script agrega 23.

SELECT 'tablas'    AS objeto, count(*) AS hay, 73  AS esperado
  FROM information_schema.tables
 WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
UNION ALL
SELECT 'secuencias', count(*), 56
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE c.relkind = 'S' AND n.nspname = 'public'
UNION ALL
SELECT 'vistas', count(*), 1
  FROM information_schema.views WHERE table_schema = 'public'
UNION ALL
SELECT 'opciones', count(*), 60 FROM opciones
UNION ALL
SELECT 'perfil_opciones', count(*), 139 FROM perfil_opciones
UNION ALL
SELECT 'perfiles', count(*), 9 FROM perfiles
UNION ALL
SELECT 'modulos', count(*), 5 FROM modulos;

-- Checklist de wo-printer (su propia query de verificacion, ampliada con lo
-- que pide su ultimo commit). Las 15 deben salir en t.
SELECT '1.3 idx_mov_prod_tipo_fecha' AS item,
       EXISTS (SELECT 1 FROM pg_indexes WHERE indexname='idx_mov_prod_tipo_fecha') AS ok
UNION ALL SELECT '6.1 detalle_factura.es_novedad',
       EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='detalle_factura' AND column_name='es_novedad')
UNION ALL SELECT '6.1 detalle_factura.motivo_novedad',
       EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='detalle_factura' AND column_name='motivo_novedad')
UNION ALL SELECT '6.2 idx_detalle_factura_novedad',
       EXISTS (SELECT 1 FROM pg_indexes WHERE indexname='idx_detalle_factura_novedad')
UNION ALL SELECT '10 tabla novedades_facturas',
       EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='novedades_facturas')
UNION ALL SELECT '10 idx_novedades_tipo',
       EXISTS (SELECT 1 FROM pg_indexes WHERE indexname='idx_novedades_tipo')
UNION ALL SELECT '10 idx_novedades_estado',
       EXISTS (SELECT 1 FROM pg_indexes WHERE indexname='idx_novedades_estado')
UNION ALL SELECT '10 idx_novedades_factura',
       EXISTS (SELECT 1 FROM pg_indexes WHERE indexname='idx_novedades_factura')
UNION ALL SELECT '10 idx_novedades_codigo',
       EXISTS (SELECT 1 FROM pg_indexes WHERE indexname='idx_novedades_codigo')
UNION ALL SELECT '10 idx_novedades_fecha',
       EXISTS (SELECT 1 FROM pg_indexes WHERE indexname='idx_novedades_fecha')
UNION ALL SELECT '11 facturas_impresas.nit_cliente',
       EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='facturas_impresas' AND column_name='nit_cliente')
UNION ALL SELECT 'RTF productos_unidades_entrega',
       EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='productos_unidades_entrega')
UNION ALL SELECT 'RTF fn asignar_bodegas_entrega',
       EXISTS (SELECT 1 FROM pg_proc WHERE proname='asignar_bodegas_entrega')
UNION ALL SELECT 'RTF fn seleccionar_bodega_descarga',
       EXISTS (SELECT 1 FROM pg_proc WHERE proname='seleccionar_bodega_descarga')
UNION ALL SELECT 'bodegas.genera_orden_automatica',
       EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='bodegas' AND column_name='genera_orden_automatica');

-- Los datos de negocio no se tocaron: estos conteos deben ser identicos a los
-- de antes de correr el script (3922 / 124 / 11 / 1249 / 10018).
SELECT 'productos' AS tabla, count(*) FROM productos
UNION ALL SELECT 'contactos', count(*) FROM contactos
UNION ALL SELECT 'users', count(*) FROM users
UNION ALL SELECT 'facturas_cabeceras', count(*) FROM facturas_cabeceras
UNION ALL SELECT 'movimientos_inventario', count(*) FROM movimientos_inventario;
