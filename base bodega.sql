CREATE TABLE cajas
(
  id serial NOT NULL,
  total double precision,
  retiro double precision,
  saldo double precision,
  fecha_cierre date,
  hora_cierre character varying(8),
  CONSTRAINT claveprimariacaja PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE configuraciones
(
  id serial NOT NULL,
  nombre_negocio character varying(60),
  nit_negocio character varying(15),
  contacto_negocio character varying(15),
  contacto2_negocio character varying(15),
  direccion character varying,
  utilidad_venta integer,
  utilidad_mayorista integer,
  utilidad_credito integer,
  iva integer,
  nombre_impresora character varying,
  imprimir_factura integer,
  productos_repetidos integer,
  ruta_backup character varying,
  pie_legal character varying,
  servicios character varying,
  dinero integer,
  impresora_venta_80mm character varying,
  -- Modulo Creditos: carpeta donde se guardan fotos y PDF de soportes
  ruta_imagenes character varying,
  -- Modulo Caja: tipo de impresora de recibos y flag de ingreso de dinero
  tipo_impresora character varying,
  ingreso_dinero integer,
  CONSTRAINT configuraciones_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE contactos
(
  id serial NOT NULL,
  nombre character varying(600) NOT NULL,
  cedula character varying(150),
  direccion character varying(80),
  ciudad character varying(80),
  contacto character varying(20),
  contacto2 character varying(20),
  descuento double precision,
  email character varying(50),
  forma_pago character varying(20),
  cuenta character varying(20),
  tipo_cuenta character varying(20),
  numero_cuenta character varying(20),
  observaciones character varying,
  proveedor integer,
  contacto_maestro integer,
  -- Campos del modulo Creditos (cliente de credito); nullables, sin efecto
  -- para instalaciones sin el modulo
  cupo double precision,
  interes double precision,
  empleado integer,
  antiguo integer,
  -- Campo del modulo Caja: contacto por defecto en ingresos de dinero
  predeterminado integer,
  CONSTRAINT claveprimaria_ PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE perfiles
(
  id serial NOT NULL,
  perfil character varying(30),
  CONSTRAINT perfiles_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE unidades_medidas
(
  id serial NOT NULL,
  nombre character varying(30),
  CONSTRAINT unidades_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE bodegas
(
  id serial NOT NULL,
  nombre character varying(30),
  imprime integer,
  CONSTRAINT pkey_bodega PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE users
(
  id serial NOT NULL,
  nombre character varying(60) NOT NULL,
  password character varying NOT NULL,
  user_name character varying(30) NOT NULL,
  direccion character varying(80),
  telefono character varying(20),
  telefono2 character varying(20),
  sitioweb character varying(50),
  estado character varying(8),
  email character varying(50),
  id_perfil integer NOT NULL,
  id_bodega integer,
  imprime_ordenes boolean NOT NULL DEFAULT FALSE,
  nombre_impresora character varying(150),
  imp_ticket_bodega_asignada boolean NOT NULL DEFAULT FALSE,
  barra_notificaciones boolean NOT NULL DEFAULT FALSE,
  panel_notificaciones boolean NOT NULL DEFAULT FALSE,
  aprueba_compras boolean NOT NULL DEFAULT FALSE,
  CONSTRAINT users_pkey PRIMARY KEY (id),
  CONSTRAINT fk_bodega_user FOREIGN KEY (id_bodega) REFERENCES bodegas (id),
  CONSTRAINT fk_perfil FOREIGN KEY (id_perfil)
      REFERENCES perfiles (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);


CREATE TABLE productos
(
  id serial NOT NULL,
  codigo_barras character varying,
  descripcion character varying(1000),
  stock_minimo double precision,
  id_proveedor integer,
  stock_ideal double precision,
  id_unidad integer,
  id_padre integer,
  tipo integer,
  cant_paquete integer,
  precio_costo double precision,
  precio_venta double precision,
  precio_venta2 double precision,
  precio_venta3 double precision,
  estado boolean,
  CONSTRAINT clave_primaria PRIMARY KEY (id),
  CONSTRAINT productos_codigo_barras_key UNIQUE (codigo_barras),
    CONSTRAINT fk_uniadd FOREIGN KEY (id_unidad)
      REFERENCES unidades_medidas (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE facturas_cabeceras
(
  id serial NOT NULL,
  codigo character varying(200),
  id_contacto integer,
  id_user integer,
  id_user_edita integer,
  fecha date,
  hora character varying(8),
  tipo_factura character varying(100),
  observacion character varying,
  observacion_entrega character varying,
  anulado integer,
  tipo_pago integer,
  importado integer,
  id_bodega integer,
  impreso_vendedor integer DEFAULT 0,
  impreso_auto boolean NOT NULL DEFAULT FALSE,
  CONSTRAINT pkfacturacabecera PRIMARY KEY (id),
  CONSTRAINT fk_id_userc FOREIGN KEY (id_user)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fkcontactoc FOREIGN KEY (id_contacto) REFERENCES contactos (id)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE facturas_detalles
(
  id serial NOT NULL,
  id_cabecera integer,
  id_producto integer,
  cantidad double precision,
  subtotal double precision,
  id_factura integer DEFAULT 0,
  CONSTRAINT pkfacturadetalle PRIMARY KEY (id),
  CONSTRAINT fkcabecera FOREIGN KEY (id_cabecera)
      REFERENCES facturas_cabeceras (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fkproducto FOREIGN KEY (id_producto)
      REFERENCES productos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

-- COTIZACION

CREATE TABLE cotizaciones_cabeceras
(
  id serial NOT NULL,
  codigo character varying(200),
  id_contacto integer,
  id_user integer,
  id_user_edita integer,
  fecha date,
  hora character varying(8),
  tipo_factura character varying(100),
  observacion character varying,
  observacion_entrega character varying,
  anulado integer,
  tipo_pago integer,
  importado integer,
  CONSTRAINT pkcotcabecera PRIMARY KEY (id),
  CONSTRAINT fk_id_usercot FOREIGN KEY (id_user)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fkcontactocot FOREIGN KEY (id_contacto) REFERENCES contactos (id)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE cotizaciones_detalles
(
  id serial NOT NULL,
  id_cabecera integer,
  id_producto integer,
  cantidad double precision,
  subtotal double precision,
  id_factura integer,
  CONSTRAINT pk_cot_detalle PRIMARY KEY (id),
  CONSTRAINT fkcabeceracot FOREIGN KEY (id_cabecera)
      REFERENCES facturas_cabeceras (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fkproductocot FOREIGN KEY (id_producto)
      REFERENCES productos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);


CREATE TABLE tipo_ingreso
(
  id serial NOT NULL,
  nombre character varying(100),
  CONSTRAINT pk_tipo_ingresos PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE ingresos_mercancias_cabecera
(
  id serial NOT NULL,
  no_factura character varying(100),
  id_proveedor integer,
  id_user integer,
  id_tipo integer,
  total double precision,
  fecha date,
  hora character varying(8),
  descripcion character varying,
  id_transportador integer,
  estado integer,
  id_bodega integer,
  fecha_vencimiento date,
  CONSTRAINT pk_ingresos PRIMARY KEY (id),
  CONSTRAINT fk_id_useri FOREIGN KEY (id_user)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_proveedor FOREIGN KEY (id_proveedor)
      REFERENCES contactos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_tipo_ingreso FOREIGN KEY (id_tipo)
      REFERENCES tipo_ingreso (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_bodega_ingreso FOREIGN KEY (id_bodega)
      REFERENCES bodegas (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_transportador FOREIGN KEY (id_transportador)
      REFERENCES contactos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);



CREATE TABLE ingresos_mercancias_detalle
(
  id serial NOT NULL,
  id_producto integer,
  id_ingreso_cabecera integer,
  cantidad double precision,
  precio_iva double precision,
  precio_costo double precision,
  CONSTRAINT pk_ingresos_detalle PRIMARY KEY (id),
  CONSTRAINT fk_ingreso_cabecera FOREIGN KEY (id_ingreso_cabecera)
      REFERENCES ingresos_mercancias_cabecera (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_producto FOREIGN KEY (id_producto)
      REFERENCES productos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);



CREATE TABLE sucursales
(
  id serial NOT NULL,
  nombre character varying(20),
  telefono character varying(20),
  responsable character varying(20),
  direccion character varying(30),
  CONSTRAINT sucursales_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);


CREATE TABLE verificar_inventario
(
  id serial NOT NULL,
  id_producto integer,
  codigo_barras character varying,
  descripcion character varying(500),
  cantidad_actual double precision,
  cantidad_real double precision,
  observaciones1 character varying(200),
  observaciones2 character varying(200),
  observaciones3 character varying(200),
  CONSTRAINT clave_primaria_inventario PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE stock
(
  codigo_barras character varying NOT NULL,
  stock double precision,
  pendientes_entregas double precision,
  costo double precision,
  CONSTRAINT clave_primaria_stock_actual PRIMARY KEY (codigo_barras)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE entregas_productos_cabecera
(
  id serial NOT NULL,
  id_factura integer NOT NULL,
  id_user integer,
  fecha_entrega date,
  hora_entrega character varying(8),
  id_bodega integer,
  CONSTRAINT pk_entrega_cabecera PRIMARY KEY (id),
  CONSTRAINT fk_user_entrega FOREIGN KEY (id_user) REFERENCES users (id) 
)
WITH (
  OIDS=FALSE
);


CREATE TABLE entregas_productos
(
  id serial NOT NULL,
  id_cabecera integer NOT NULL,
  id_producto integer,
  cantidad double precision,
  id_factura integer,
  CONSTRAINT pk_entregas PRIMARY KEY (id),
  CONSTRAINT fk_entrega FOREIGN KEY (id_cabecera) REFERENCES entregas_productos_cabecera (id) on delete CASCADE
)
WITH (
  OIDS=FALSE
);


CREATE TABLE pagos_ingresos(
  id serial,
  id_ingresos_mercancias_cabecera integer,
  total double precision,
  fecha date,
  hora varchar(8), 
  cod_pago varchar,
  CONSTRAINT pk_pagos_facturas PRIMARY KEY (id),
  CONSTRAINT fk_id_ingreso_mercancia_cabecera FOREIGN KEY (id_ingresos_mercancias_cabecera) REFERENCES ingresos_mercancias_cabecera (id) on delete cascade
);


CREATE TABLE devoluciones(
  id serial NOT NULL, -- Código de la factura cabecera
  id_factura integer, -- Código del contacto en la factura cabecera
  id_user integer, -- Código del contacto en la factura cabecera
  total double precision, -- Monto total a pagar
  fecha date, -- Fecha de emisión de la factura
  hora varchar(8), -- Fecha de emisión de la factura
  id_bodega integer,
  CONSTRAINT pkdevoluciones PRIMARY KEY (id),
  CONSTRAINT fkfactura FOREIGN KEY (id_factura) REFERENCES facturas_cabeceras (id),
  CONSTRAINT fk_bodega_devolucion FOREIGN KEY (id_bodega) REFERENCES bodegas (id),
  CONSTRAINT fk_id_userdev FOREIGN KEY (id_user) REFERENCES users (id)
);

CREATE TABLE devoluciones_detalles(
  id serial, -- Código del detalle de la factura
  id_cabecera_devolucion integer,
  id_producto integer, -- Código del producto en el detalle de la factura
  cantidad double precision, -- Cantidad vendida del producto
  valor_unitario double precision, -- Precio unitario del producto
  total double precision, 
  CONSTRAINT pkdetallesdev PRIMARY KEY (id),
  CONSTRAINT fk_cabecera_devoluciones FOREIGN KEY (id_cabecera_devolucion) REFERENCES devoluciones (id),
  CONSTRAINT fkproducto_devolucion FOREIGN KEY (id_producto) REFERENCES productos (id)
);

CREATE TABLE traslados_productos(
  id serial, -- Código del detalle de la factura
  id_producto integer, -- Código del producto en el detalle de la factura
  cantidad double precision, -- Cantidad vendida del producto
  id_bodega_origen double precision, -- Precio unitario del producto
  id_bodega_destino double precision, 
  fecha date,
  hora varchar(8),
  observacion varchar,
  id_user integer,
  CONSTRAINT pk_traslado PRIMARY KEY (id),
  CONSTRAINT fk_user_traslado FOREIGN KEY (id_user) REFERENCES users (id),
  CONSTRAINT fk_producto_traslado FOREIGN KEY (id_producto) REFERENCES productos (id)
);

-- ============================================================================
-- SISTEMA DE INVENTARIO POR BODEGA
-- Compatible con PostgreSQL 9.4
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Tabla de stock por producto y bodega
-- ----------------------------------------------------------------------------
CREATE TABLE stock_productos
(
  id_producto    integer NOT NULL,
  id_bodega      integer NOT NULL,
  cantidad       double precision NOT NULL DEFAULT 0,
  pendientes     double precision NOT NULL DEFAULT 0,
  costo_promedio double precision DEFAULT 0,
  updated_at     timestamp without time zone NOT NULL DEFAULT now(),
  
  CONSTRAINT pk_stock_productos PRIMARY KEY (id_producto, id_bodega),
  CONSTRAINT fk_stock_producto FOREIGN KEY (id_producto)
      REFERENCES productos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_stock_bodega FOREIGN KEY (id_bodega)
      REFERENCES bodegas (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);

CREATE INDEX idx_stock_bodega ON stock_productos (id_bodega);

-- ----------------------------------------------------------------------------
-- Unidades de entrega por producto (opcional): partición de cantidad entre
-- bodegas según el tamaño de paquete.
--   cantidad_paquete = 1  -> bodega para entregas por unidad (sobrante).
--   cantidad_paquete > 1  -> paquete (ej. caja de 50) con su bodega.
--   Si un producto no tiene filas aqui, usa la seleccion automatica normal.
-- ----------------------------------------------------------------------------
CREATE TABLE productos_unidades_entrega
(
  id               serial NOT NULL,
  id_producto      integer NOT NULL,
  nombre           character varying(60),
  cantidad_paquete double precision NOT NULL,
  id_bodega        integer NOT NULL,

  CONSTRAINT pk_productos_unidades_entrega PRIMARY KEY (id),
  CONSTRAINT fk_pue_producto FOREIGN KEY (id_producto)
      REFERENCES productos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_pue_bodega FOREIGN KEY (id_bodega)
      REFERENCES bodegas (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE INDEX idx_pue_producto ON productos_unidades_entrega (id_producto);

-- ----------------------------------------------------------------------------
-- Tabla de movimientos de inventario (histórico/auditoría)
-- ----------------------------------------------------------------------------
CREATE TABLE movimientos_inventario
(
  id serial NOT NULL,
  
  -- Identificación
  id_producto       integer NOT NULL,
  id_bodega         integer NOT NULL,
  id_user           integer NOT NULL,
  
  -- Tipo y comportamiento
  tipo              character varying(30) NOT NULL,
  afecta_cantidad   integer NOT NULL,
  afecta_pendientes integer NOT NULL,
  
  -- Valor del movimiento (delta aplicado, siempre positivo)
  valor             double precision NOT NULL,
  
  -- Para ediciones: valores anterior y nuevo del documento original
  valor_anterior    double precision,
  valor_nuevo       double precision,
  
  -- Costos
  costo_unitario          double precision,
  costo_promedio_anterior double precision,
  costo_promedio_nuevo    double precision,
  
  -- Snapshots del stock para auditoría
  cantidad_anterior   double precision,
  cantidad_nueva      double precision,
  pendientes_anterior double precision,
  pendientes_nuevo    double precision,
  
  -- Trazabilidad
  id_referencia    integer,
  tabla_referencia character varying(50),
  
  -- Fecha/hora y notas
  fecha       date NOT NULL DEFAULT ('now'::text)::date,
  hora        character varying(8),
  observacion character varying(500),
  
  CONSTRAINT pk_movimientos_inventario PRIMARY KEY (id),
  CONSTRAINT fk_mov_producto FOREIGN KEY (id_producto)
      REFERENCES productos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_mov_bodega FOREIGN KEY (id_bodega)
      REFERENCES bodegas (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_mov_user FOREIGN KEY (id_user)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE INDEX idx_mov_producto_fecha ON movimientos_inventario (id_producto, fecha);
CREATE INDEX idx_mov_referencia ON movimientos_inventario (tabla_referencia, id_referencia);
CREATE INDEX idx_mov_bodega ON movimientos_inventario (id_bodega);
CREATE INDEX idx_mov_tipo ON movimientos_inventario (tipo);
CREATE INDEX idx_mov_prod_tipo_fecha ON movimientos_inventario (id_producto, tipo, fecha DESC, id DESC);

-- ============================================================================
-- SISTEMA DE IMPRESIÓN
-- ============================================================================

CREATE TABLE impresoras
(
  id serial NOT NULL,
  nombre character varying(100) NOT NULL,
  nombre_windows character varying(200) NOT NULL,
  activa boolean NOT NULL DEFAULT true,
  fecha_creacion timestamp without time zone NOT NULL DEFAULT now(),
  tipo_bodega boolean NOT NULL DEFAULT true,
  tipo_venta boolean NOT NULL DEFAULT false,
  id_bodega integer,
  tipo_notificaciones boolean NOT NULL DEFAULT false,
  CONSTRAINT impresoras_pkey PRIMARY KEY (id),
  CONSTRAINT fk_impresoras_bodega FOREIGN KEY (id_bodega) REFERENCES bodegas (id)
)
WITH (
  OIDS=FALSE
);

CREATE TABLE facturas_impresas
(
  id serial NOT NULL,
  numero_factura character varying(50) NOT NULL,
  cliente character varying(300),
  fecha_factura date,
  fecha_impresion timestamp without time zone NOT NULL DEFAULT now(),
  vendedor character varying(300),
  concepto character varying(500),
  forma_pago character varying(100),
  prefijo character varying(20),
  empresa character varying(300),
  CONSTRAINT facturas_impresas_pkey PRIMARY KEY (id),
  CONSTRAINT facturas_impresas_numero_factura_key UNIQUE (numero_factura)
)
WITH (
  OIDS=FALSE
);

CREATE INDEX idx_facturas_numero ON facturas_impresas (numero_factura);

CREATE TABLE detalle_factura
(
  id serial NOT NULL,
  factura_id integer NOT NULL,
  codigo_producto character varying(50) NOT NULL,
  descripcion character varying(500),
  cantidad double precision NOT NULL DEFAULT 0,
  iva double precision NOT NULL DEFAULT 0,
  precio_unitario double precision NOT NULL DEFAULT 0,
  total_linea double precision NOT NULL DEFAULT 0,
  es_novedad boolean NOT NULL DEFAULT false,
  motivo_novedad character varying(200),
  CONSTRAINT detalle_factura_pkey PRIMARY KEY (id),
  CONSTRAINT detalle_factura_factura_id_fkey FOREIGN KEY (factura_id)
      REFERENCES facturas_impresas (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE INDEX idx_detalle_factura_novedad
    ON detalle_factura (factura_id) WHERE es_novedad = TRUE;

CREATE TABLE log_impresiones
(
  id serial NOT NULL,
  numero_factura character varying(50) NOT NULL,
  impresora_id integer,
  archivo_origen character varying(500) NOT NULL,
  estado character varying(20) NOT NULL DEFAULT 'PENDIENTE',
  mensaje_error text,
  fecha_procesado timestamp without time zone NOT NULL DEFAULT now(),
  concepto character varying,
  CONSTRAINT log_impresiones_pkey PRIMARY KEY (id),
  CONSTRAINT log_impresiones_impresora_id_fkey FOREIGN KEY (impresora_id)
      REFERENCES impresoras (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

-- ============================================================================
-- AJUSTES DE INVENTARIO
-- ============================================================================

CREATE TABLE ajustes_inventario_cabecera
(
  id serial NOT NULL,
  fecha date NOT NULL DEFAULT ('now'::text)::date,
  hora character varying(8),
  id_user integer NOT NULL,
  id_bodega integer NOT NULL,
  observacion character varying(500),
  estado integer NOT NULL DEFAULT 1,
  CONSTRAINT ajustes_inventario_cabecera_pkey PRIMARY KEY (id),
  CONSTRAINT ajustes_inventario_cabecera_id_user_fkey FOREIGN KEY (id_user)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT ajustes_inventario_cabecera_id_bodega_fkey FOREIGN KEY (id_bodega)
      REFERENCES bodegas (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE INDEX idx_ajuste_cab_bodega ON ajustes_inventario_cabecera (id_bodega);
CREATE INDEX idx_ajuste_cab_fecha ON ajustes_inventario_cabecera (fecha);

CREATE TABLE ajustes_inventario_detalle
(
  id serial NOT NULL,
  id_ajuste_cabecera integer NOT NULL,
  id_producto integer NOT NULL,
  cantidad_anterior double precision NOT NULL DEFAULT 0,
  cantidad_nueva double precision NOT NULL DEFAULT 0,
  diferencia double precision NOT NULL DEFAULT 0,
  pendientes_anterior double precision NOT NULL DEFAULT 0,
  pendientes_nuevo double precision NOT NULL DEFAULT 0,
  diferencia_pendientes double precision NOT NULL DEFAULT 0,
  observacion character varying(300),
  CONSTRAINT ajustes_inventario_detalle_pkey PRIMARY KEY (id),
  CONSTRAINT ajustes_inventario_detalle_id_ajuste_cabecera_fkey FOREIGN KEY (id_ajuste_cabecera)
      REFERENCES ajustes_inventario_cabecera (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT ajustes_inventario_detalle_id_producto_fkey FOREIGN KEY (id_producto)
      REFERENCES productos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE INDEX idx_ajuste_det_cabecera ON ajustes_inventario_detalle (id_ajuste_cabecera);

insert into perfiles (id,perfil) values (1,'admin');
insert into perfiles (id,perfil) values (2,'cajero');
insert into perfiles (id,perfil) values (3,'vendedor');
insert into users (id,nombre,password,user_name,id_perfil) values (1,'admin','6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b','admin',1);
insert into users (id,nombre,password,user_name,id_perfil) values (2,'caja','000c285457fc971f862a79b786476c78812c8897063c6fa9c045f579a3b2d63f','caja',2);
insert into contactos (id,nombre,cedula,direccion) values (1,'Ventas Diarias','0000','Home');


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

DROP TRIGGER IF EXISTS trg_notify_orden_nueva ON facturas_cabeceras;
CREATE TRIGGER trg_notify_orden_nueva
AFTER INSERT ON facturas_cabeceras
FOR EACH ROW EXECUTE PROCEDURE fn_notify_orden_nueva();


-- ============================================================================
-- Bitácora por usuario de tickets completos auto-impresos.
-- Deduplica el ticket completo cuando un usuario tiene
-- imp_ticket_bodega_asignada = true y wo-printer parte una factura en
-- varias cabeceras (cada una dispara NOTIFY, pero el ticket completo se
-- debe imprimir una sola vez por usuario y por numero_factura).
-- ============================================================================
CREATE TABLE auto_impresion_log_completo (
    id_user        INTEGER NOT NULL,
    numero_factura VARCHAR(200) NOT NULL,
    impreso_at     TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id_user, numero_factura)
);

CREATE INDEX idx_auto_impresion_log_fecha
    ON auto_impresion_log_completo (impreso_at);


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

DROP TRIGGER IF EXISTS trg_notify_factura_solo_novedad ON facturas_impresas;
CREATE TRIGGER trg_notify_factura_solo_novedad
AFTER INSERT ON facturas_impresas
FOR EACH ROW EXECUTE PROCEDURE fn_notify_factura_solo_novedad();


-- ============================================================================
-- Bitácora de novedades detectadas en facturas importadas
-- (códigos inválidos / no existentes / inactivos / cantidad cero).
-- ============================================================================
CREATE TABLE novedades_facturas
(
  id serial NOT NULL,
  factura_impresa_id integer,
  numero_factura character varying(50) NOT NULL,
  fecha_deteccion timestamp without time zone NOT NULL DEFAULT now(),
  tipo character varying(30) NOT NULL,
  codigo_original character varying(100),
  codigo_normalizado character varying(100),
  descripcion character varying(500),
  cantidad double precision,
  motivo character varying(300),
  estado_revision character varying(20) NOT NULL DEFAULT 'PENDIENTE',
  id_producto_asociado integer,
  observacion_revision character varying(500),
  revisado_por integer,
  fecha_revision timestamp without time zone,
  CONSTRAINT novedades_facturas_pkey PRIMARY KEY (id),
  CONSTRAINT novedades_facturas_factura_impresa_id_fkey FOREIGN KEY (factura_impresa_id)
      REFERENCES facturas_impresas (id),
  CONSTRAINT novedades_facturas_id_producto_asociado_fkey FOREIGN KEY (id_producto_asociado)
      REFERENCES productos (id),
  CONSTRAINT novedades_facturas_revisado_por_fkey FOREIGN KEY (revisado_por)
      REFERENCES users (id)
)
WITH (
  OIDS=FALSE
);

CREATE INDEX idx_novedades_tipo    ON novedades_facturas (tipo);
CREATE INDEX idx_novedades_estado  ON novedades_facturas (estado_revision);
CREATE INDEX idx_novedades_factura ON novedades_facturas (numero_factura);
CREATE INDEX idx_novedades_codigo  ON novedades_facturas (codigo_normalizado);

-- ============================================================================
-- TABLA: escaneos_qr_ordenes
-- ============================================================================
-- Log de lecturas de codigo QR del modulo "Entregas Rapidas".
-- Registra cada escaneo (valido o invalido) realizado desde la pantalla
-- tactil con lector QR. Permite generar reportes historicos de la orden
-- incluyendo todas sus lecturas.
-- ============================================================================
CREATE TABLE escaneos_qr_ordenes
(
  id serial NOT NULL,
  id_factura integer,                       -- NULL si la lectura fue invalida o no resolvio orden
  id_user integer NOT NULL,                 -- usuario que escaneo
  id_bodega integer NOT NULL,               -- bodega del usuario al momento del escaneo
  fecha_escaneo date NOT NULL,
  hora_escaneo time without time zone NOT NULL,
  qr_leido character varying(150),          -- texto crudo recibido del lector (auditoria)
  resultado character varying(30) NOT NULL, -- OK | QR_INVALIDO | ORDEN_NO_EXISTE | OTRA_BODEGA | ANULADA | YA_ENTREGADA
  accion character varying(30) NOT NULL,    -- NINGUNA | ENTREGA_COMPLETA | ENTREGA_PARCIAL
  id_entrega_cab integer,                   -- FK a entregas_productos_cabecera si genero entrega
  pc_origen character varying(80),          -- hostname del PC tactil
  CONSTRAINT pk_escaneos_qr_ordenes PRIMARY KEY (id),
  CONSTRAINT fk_escaneos_qr_user FOREIGN KEY (id_user)
      REFERENCES users (id),
  CONSTRAINT fk_escaneos_qr_bodega FOREIGN KEY (id_bodega)
      REFERENCES bodegas (id),
  CONSTRAINT fk_escaneos_qr_factura FOREIGN KEY (id_factura)
      REFERENCES facturas_cabeceras (id),
  CONSTRAINT fk_escaneos_qr_entrega_cab FOREIGN KEY (id_entrega_cab)
      REFERENCES entregas_productos_cabecera (id) ON DELETE SET NULL
)
WITH (
  OIDS=FALSE
);

CREATE INDEX idx_escaneos_qr_id_factura ON escaneos_qr_ordenes (id_factura);
CREATE INDEX idx_escaneos_qr_fecha      ON escaneos_qr_ordenes (fecha_escaneo);
CREATE INDEX idx_escaneos_qr_user       ON escaneos_qr_ordenes (id_user);
CREATE INDEX idx_escaneos_qr_bodega     ON escaneos_qr_ordenes (id_bodega);
CREATE INDEX idx_novedades_fecha   ON novedades_facturas (fecha_deteccion DESC);


-- ============================================================================
-- ÓRDENES DE COMPRA (solicitudes de pedido por escalas)
-- Cualquier usuario crea una orden con productos y cantidades (sin proveedor ni
-- precios). Un usuario con users.aprueba_compras analiza el histórico y aprueba
-- o rechaza, asignando proveedor y precio POR LÍNEA.
-- estado: 0=BORRADOR, 1=PENDIENTE, 2=APROBADA, 3=RECHAZADA
-- ============================================================================
CREATE TABLE ordenes_compra_cabecera
(
  id integer NOT NULL,
  numero character varying(50),
  id_user_crea integer NOT NULL,
  fecha date,
  hora time without time zone,
  estado integer NOT NULL DEFAULT 0,
  observacion text,
  id_bodega integer,
  id_user_aprueba integer,
  fecha_aprobacion timestamp without time zone,
  CONSTRAINT pk_ordenes_compra_cabecera PRIMARY KEY (id),
  CONSTRAINT fk_oc_user_crea FOREIGN KEY (id_user_crea)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_oc_user_aprueba FOREIGN KEY (id_user_aprueba)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_oc_bodega FOREIGN KEY (id_bodega)
      REFERENCES bodegas (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);



CREATE TABLE ordenes_compra_detalle
(
  id integer NOT NULL,
  id_orden_cabecera integer NOT NULL,
  id_producto integer NOT NULL,
  cantidad numeric(18,4) NOT NULL,
  id_proveedor integer,
  precio_unitario numeric(18,4),
  observacion text,
  CONSTRAINT pk_ordenes_compra_detalle PRIMARY KEY (id),
  CONSTRAINT fk_ocd_cabecera FOREIGN KEY (id_orden_cabecera)
      REFERENCES ordenes_compra_cabecera (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_ocd_producto FOREIGN KEY (id_producto)
      REFERENCES productos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_ocd_proveedor FOREIGN KEY (id_proveedor)
      REFERENCES contactos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE INDEX idx_ordenes_compra_detalle_cabecera ON ordenes_compra_detalle (id_orden_cabecera);



-- =============================================================================
-- SISTEMA DE PERMISOS ADMINISTRABLES
-- (equivale a sql/migracion_permisos.sql ya aplicada en produccion)
--
-- Modelo:
--   opciones               catalogo de funciones gobernables (menus/botones/acciones)
--   perfil_opciones        lo que cada perfil concede por defecto (presencia = concedido)
--   usuario_opciones       excepciones por usuario (concedido=true agrega, false revoca)
--   usuario_roles_precios  roles del modulo Precios por usuario (2=almacenista,
--                          3=contable, 4=precios); un usuario puede tener varios
--
-- Permiso efectivo = perfil_opciones(perfil del usuario)
--                    + usuario_opciones(concedido=true)
--                    - usuario_opciones(concedido=false)
-- El perfil 1 (Admin) siempre tiene todo (la app no lo consulta).
--
-- En una instalacion nueva los perfiles se crean vacios: concede sus opciones
-- desde la pantalla "Permisos de la aplicacion" (menu Configuraciones, Admin).
-- =============================================================================

CREATE TABLE opciones (
    id serial NOT NULL,
    clave character varying(60) NOT NULL,    -- identificador estable usado por el codigo
    nombre character varying(100) NOT NULL,  -- etiqueta legible para la pantalla de admin
    modulo character varying(60) NOT NULL,   -- agrupacion en el arbol de admin (= modulos.clave)
    componente character varying(60),        -- campo de frm_main que gobierna (null = accion logica)
    orden integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_opciones PRIMARY KEY (id),
    CONSTRAINT uq_opciones_clave UNIQUE (clave)
);

CREATE TABLE perfil_opciones (
    id_perfil integer NOT NULL,
    id_opcion integer NOT NULL,
    CONSTRAINT pk_perfil_opciones PRIMARY KEY (id_perfil, id_opcion),
    CONSTRAINT fk_po_perfil FOREIGN KEY (id_perfil) REFERENCES perfiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_po_opcion FOREIGN KEY (id_opcion) REFERENCES opciones (id) ON DELETE CASCADE
);

CREATE TABLE usuario_opciones (
    id_user integer NOT NULL,
    id_opcion integer NOT NULL,
    concedido boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_usuario_opciones PRIMARY KEY (id_user, id_opcion),
    CONSTRAINT fk_uo_user FOREIGN KEY (id_user) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_uo_opcion FOREIGN KEY (id_opcion) REFERENCES opciones (id) ON DELETE CASCADE
);

CREATE TABLE usuario_roles_precios (
    id_user integer NOT NULL,
    rol integer NOT NULL,                    -- 2=almacenista 3=contable 4=precios
    CONSTRAINT pk_usuario_roles_precios PRIMARY KEY (id_user, rol),
    CONSTRAINT fk_urp_user FOREIGN KEY (id_user) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_urp_rol CHECK (rol IN (2, 3, 4))
);

-- Catalogo de opciones gobernables. Espejo del catalogo de produccion
-- (sin Recortes, modulo eliminado de la aplicacion). Al agregar una opcion
-- nueva por migracion incremental, agregarla tambien aqui.
INSERT INTO opciones (clave, nombre, modulo, componente, orden) VALUES
    -- Compras
    ('menu_compras', 'Menu Compras (ordenes de compra)', 'Compras', 'menuCompras', 10),
    ('compras_crear', 'Crear orden de compra', 'Compras', 'btn_nueva', 20),
    ('compras_editar', 'Editar orden de compra', 'Compras', 'btn_editar', 30),
    -- Configuracion
    ('jmenu_configuraciones', 'Configuraciones', 'Configuracion', 'jmenu_configuraciones', 10),
    ('jmenu_user', 'Usuarios', 'Configuracion', 'jmenu_user', 20),
    ('jmenu_backup', 'BackUp', 'Configuracion', 'jmenu_backup', 30),
    ('jMenu_tipo_ingreso', 'Tipo de ingreso', 'Configuracion', 'jMenu_tipo_ingreso', 40),
    ('jmenu_bodegas', 'Bodegas', 'Configuracion', 'jmenu_bodegas', 50),
    ('jMenu_unidades', 'Unidades de medida', 'Configuracion', 'jMenu_unidades', 60),
    ('jmenu_permisos', 'Permisos de la aplicacion', 'Configuracion', NULL, 70),
    -- Contactos
    ('jmenu_con', 'Contactos (menu)', 'Contactos', 'jmenu_con', 10),
    ('jmenu_contactos', 'Contactos (item de menu)', 'Contactos', 'jmenu_contactos', 20),
    ('btn_contactos', 'Contactos (boton)', 'Contactos', 'btn_contactos', 30),
    -- Ordenes
    ('jMenu_ordenes', 'Ordenes (menu)', 'Ordenes', 'jMenu_ordenes', 10),
    ('ordenes_unir', 'Unir ordenes de entrega', 'Ordenes', 'btn_unir', 10),
    ('ordenes_entrega_masiva', 'Entrega masiva por bodega', 'Ordenes', 'btn_entregar_todo', 11),
    ('jmenu_facturacion', 'Generar orden (menu)', 'Ordenes', 'jmenu_facturacion', 20),
    ('btn_generar_orden', 'Generar orden (boton)', 'Ordenes', 'btn_generar_orden', 30),
    ('btn_ver_ordenes', 'Ver ordenes', 'Ordenes', 'btn_ver_ordenes', 40),
    ('ordenes_editar', 'Editar ordenes', 'Ordenes', 'btn_editar', 45),
    ('btn_facturar', 'Ordenes (boton)', 'Ordenes', 'btn_facturar', 50),
    ('btn_ver_facturas', 'Ver ordenes (boton)', 'Ordenes', 'btn_ver_facturas', 60),
    ('btn_decolucion', 'Devolucion', 'Ordenes', 'btn_decolucion', 70),
    -- Precios
    ('menu_precios_ingresos', 'Ingresos de productos', 'Precios', 'itemIngresosPrecios', 10),
    ('menu_precios_productos', 'Precios de productos', 'Precios', 'itemPreciosProductos', 20),
    ('menu_precios_descuentos', 'Descuentos escalonados', 'Precios', 'itemDescuentosPrecios', 30),
    ('menu_precios_etiquetas', 'Imprimir etiquetas', 'Precios', 'itemEtiquetasPrecios', 40),
    ('menu_precios_comisiones', 'Analizar comisiones', 'Precios', 'itemComisionesPrecios', 45),
    ('menu_precios_reportes', 'Reportes', 'Precios', 'menuReportesPrecios', 50),
    ('menu_precios_config', 'Configuracion de precios', 'Precios', 'itemConfigPrecios', 60),
    -- Productos
    ('jMenu_productos_principal', 'Productos (menu)', 'Productos', 'jMenu_productos_principal', 10),
    ('btn_productos', 'Productos (boton)', 'Productos', 'btn_productos', 20),
    ('btn_ingreso_productos', 'Ingreso de productos', 'Productos', 'btn_ingreso_productos', 30),
    -- Creditos (modulo licenciable importado de control_creditos)
    ('menu_creditos', 'Menu Creditos (completo)', 'Creditos', 'menuCreditos', 10),
    ('creditos_ver', 'Creditos (cartera)', 'Creditos', 'itemCreditosVer', 20),
    ('creditos_clientes', 'Clientes de credito', 'Creditos', 'itemCreditosClientes', 30),
    ('creditos_cuentas', 'Cuentas', 'Creditos', 'itemCreditosCuentas', 40),
    ('creditos_tipos_abonos', 'Tipos de abonos', 'Creditos', 'itemCreditosTipos', 50),
    ('creditos_reportes', 'Reportes de creditos', 'Creditos', 'itemCreditosReportes', 60),
    -- Caja (modulo licenciable importado de cajadiaria)
    ('menu_caja', 'Menu Caja (completo)', 'Caja', 'menuCaja', 10),
    ('caja_ingresos', 'Ingresos de dinero', 'Caja', 'itemCajaIngresos', 20),
    ('caja_egresos', 'Egresos de dinero', 'Caja', 'itemCajaEgresos', 30),
    ('caja_traslados', 'Traslados entre fondos', 'Caja', 'itemCajaTraslados', 40),
    ('caja_fondos', 'Fondos (cajas y bancos)', 'Caja', 'itemCajaFondos', 50),
    ('caja_cuentas_ingresos', 'Cuentas de ingresos', 'Caja', 'itemCajaCtasIngresos', 60),
    ('caja_cuentas_egresos', 'Cuentas de egresos', 'Caja', 'itemCajaCtasEgresos', 70),
    ('caja_reportes', 'Reportes de caja', 'Caja', 'itemCajaReportes', 80);



-- =============================================================================
-- MODULOS LICENCIABLES POR INSTALACION
-- (equivale a sql/migracion_modulos.sql)
--
-- Interruptor comercial por cliente, distinto de los permisos por usuario:
--   * modulos.clave se corresponde con opciones.modulo.
--   * Un modulo SIN fila aqui es NUCLEO: siempre activo.
--   * activo = false apaga el modulo completo para todos, incluso Admin.
--
-- Para un cliente que no licencia un modulo:
--   UPDATE modulos SET activo = false WHERE clave = 'Compras';
-- =============================================================================

CREATE TABLE modulos (
    id     serial       PRIMARY KEY,
    clave  varchar(50)  NOT NULL UNIQUE,
    nombre varchar(100) NOT NULL,
    activo boolean      NOT NULL DEFAULT true
);

INSERT INTO modulos (clave, nombre, activo) VALUES
    ('Compras', 'Compras (pipeline de ordenes de compra)', true),
    ('Precios', 'Precios (agroinsumos y comisiones)', true),
    ('Creditos', 'Creditos (cartera y abonos)', true),
    ('Caja', 'Caja (ingresos, egresos y fondos)', true);



-- =============================================================================
-- MODULO CREDITOS (importado de control_creditos)
-- (equivale a sql/migracion_modulo_creditos.sql)
--
-- creditos          el credito otorgado (en control_creditos era "facturas")
-- abonos_cabeceras  el pago como evento (total + soportes foto/pdf)
-- abonos (detalle)  aplicacion de un pago a un credito; saldo a favor
--                   implicito = cabecera.total - SUM(detalle)
-- tipos_abonos      catalogo de tipos de pago (color y flag anticipo)
-- cuentas           catalogo de cuentas destino
--
-- Reutiliza users y contactos de bodega (campos cupo/interes/empleado/antiguo
-- estan en CREATE TABLE contactos, y configuraciones.ruta_imagenes arriba).
-- =============================================================================

CREATE TABLE cuentas
(
  id serial NOT NULL,
  nombre character varying,
  CONSTRAINT cuentas_pkey PRIMARY KEY (id)
);

CREATE TABLE tipos_abonos
(
  id serial NOT NULL,
  nombre character varying,
  color character varying,
  anticipo integer,
  CONSTRAINT tipos_abonos_pkey PRIMARY KEY (id)
);

CREATE TABLE creditos
(
  id serial NOT NULL,
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
  pdf character varying,
  CONSTRAINT pk_creditos PRIMARY KEY (id),
  CONSTRAINT fk_creditos_user FOREIGN KEY (id_user)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_creditos_contacto FOREIGN KEY (id_contacto)
      REFERENCES contactos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_creditos_cuenta FOREIGN KEY (id_cuenta)
      REFERENCES cuentas (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE abonos_cabeceras
(
  id serial PRIMARY KEY,
  id_contacto integer NOT NULL REFERENCES contactos (id),
  id_user integer REFERENCES users (id),
  id_tipo_abono integer REFERENCES tipos_abonos (id),
  total double precision NOT NULL,
  fecha date NOT NULL,
  hora character varying DEFAULT '',
  observacion character varying DEFAULT '',
  foto character varying DEFAULT '',
  pdf character varying DEFAULT ''
);

CREATE TABLE abonos
(
  id serial PRIMARY KEY,
  id_cabecera integer NOT NULL REFERENCES abonos_cabeceras (id) ON DELETE CASCADE,
  id_credito integer NOT NULL REFERENCES creditos (id),
  abono double precision NOT NULL,
  fecha date NOT NULL,
  hora character varying DEFAULT ''
);

CREATE INDEX idx_abonos_id_cabecera        ON abonos(id_cabecera);
CREATE INDEX idx_abonos_id_credito         ON abonos(id_credito);
CREATE INDEX idx_abonos_cabeceras_contacto ON abonos_cabeceras(id_contacto);
CREATE INDEX idx_abonos_cabeceras_fecha    ON abonos_cabeceras(fecha);
CREATE INDEX idx_creditos_contacto         ON creditos(id_contacto);



-- =============================================================================
-- MODULO CAJA (importado de cajadiaria / ContaMonkey V2.0, rediseñado)
-- (equivale a sql/migracion_modulo_caja.sql)
--
-- fondos            a donde entra/sale el dinero (caja fisica o digital).
--                   Distinto de "cuentas" del modulo Creditos: separados
--                   a proposito.
-- cuentas_ingresos  catalogo de conceptos de ingreso
-- cuentas_egresos   catalogo de conceptos de egreso
-- ingresos          documento de entrada de DINERO (no confundir con
--                   ingresos_mercancias_*); id_fondo directo: un ingreso
--                   tiene UN solo pago (sin tablas de abonos)
-- egresos           documento de salida de dinero
-- transferencias    traslado entre fondos; genera un par egreso (origen) +
--                   ingreso (destino) marcados con transferencia=1
-- fotos_registros   soportes fotograficos de ingresos/egresos
--
-- Saldo de un fondo = SUM(ingresos.total) - SUM(egresos.total) por id_fondo
-- (derivado por consulta; sin base_diaria ni cierres de caja).
--
-- Reutiliza users y contactos de bodega (contactos.predeterminado arriba es
-- de este modulo, igual que configuraciones.tipo_impresora e ingreso_dinero).
-- =============================================================================

CREATE TABLE fondos (
    id serial NOT NULL,
    nombre character varying(100),
    predeterminado integer,
    fisico_digital integer,          -- 0=fisico (efectivo), 1=digital (banco)
    CONSTRAINT pk_fondos PRIMARY KEY (id)
);

CREATE TABLE cuentas_ingresos (
    id serial NOT NULL,
    nombre character varying(50),
    predeterminado integer,
    CONSTRAINT pk_cuentas_ingresos PRIMARY KEY (id)
);

CREATE TABLE cuentas_egresos (
    id serial NOT NULL,
    nombre character varying(50),
    predeterminado integer,
    CONSTRAINT pk_cuentas_egresos PRIMARY KEY (id)
);

-- factura_remision: 1=Factura, 0=Remision (filtro de reportes)
-- transferencia:    1 cuando el registro lo genero un traslado entre fondos
CREATE TABLE ingresos (
    id serial NOT NULL,
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
    -- pk_ingresos ya lo usa ingresos_mercancias_cabecera; nombre distinto
    CONSTRAINT pk_caja_ingresos PRIMARY KEY (id),
    CONSTRAINT fk_ingreso_cuenta FOREIGN KEY (id_cuenta) REFERENCES cuentas_ingresos (id),
    CONSTRAINT fk_ingreso_cliente FOREIGN KEY (id_cliente) REFERENCES contactos (id),
    CONSTRAINT fk_ingreso_fondo FOREIGN KEY (id_fondo) REFERENCES fondos (id),
    CONSTRAINT fk_ingreso_user FOREIGN KEY (id_user) REFERENCES users (id)
);

CREATE TABLE egresos (
    id serial NOT NULL,
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
    CONSTRAINT pk_egresos PRIMARY KEY (id),
    CONSTRAINT fk_egreso_cuenta FOREIGN KEY (id_cuenta) REFERENCES cuentas_egresos (id),
    CONSTRAINT fk_egreso_cliente FOREIGN KEY (id_cliente) REFERENCES contactos (id),
    CONSTRAINT fk_egreso_fondo FOREIGN KEY (id_fondo) REFERENCES fondos (id),
    CONSTRAINT fk_egreso_user FOREIGN KEY (id_user) REFERENCES users (id)
);

CREATE TABLE transferencias (
    id serial NOT NULL,
    id_user integer,
    id_fondo_origen integer,
    id_fondo_destino integer,
    descripcion character varying,
    total double precision,
    fecha date,
    hora character varying(8),
    id_ingreso integer,              -- ingreso generado en el fondo destino
    id_egreso integer,               -- egreso generado en el fondo origen
    CONSTRAINT pk_transferencias PRIMARY KEY (id),
    CONSTRAINT fk_transferencia_fondo_origen FOREIGN KEY (id_fondo_origen) REFERENCES fondos (id),
    CONSTRAINT fk_transferencia_fondo_destino FOREIGN KEY (id_fondo_destino) REFERENCES fondos (id),
    CONSTRAINT fk_transferencia_ingreso FOREIGN KEY (id_ingreso) REFERENCES ingresos (id),
    CONSTRAINT fk_transferencia_egreso FOREIGN KEY (id_egreso) REFERENCES egresos (id),
    CONSTRAINT fk_transferencia_user FOREIGN KEY (id_user) REFERENCES users (id)
);

-- tipo_registro: 1=ingreso, 2=egreso (id_registro apunta a esa tabla)
CREATE TABLE fotos_registros (
    id serial NOT NULL,
    nombre character varying,
    id_registro integer,
    tipo_registro integer,
    CONSTRAINT pk_fotos_registros PRIMARY KEY (id)
);

CREATE INDEX idx_ingresos_fecha           ON ingresos(fecha);
CREATE INDEX idx_ingresos_fondo           ON ingresos(id_fondo);
CREATE INDEX idx_ingresos_cliente         ON ingresos(id_cliente);
CREATE INDEX idx_egresos_fecha            ON egresos(fecha);
CREATE INDEX idx_egresos_fondo            ON egresos(id_fondo);
CREATE INDEX idx_egresos_cliente          ON egresos(id_cliente);
CREATE INDEX idx_transferencias_fecha     ON transferencias(fecha);
CREATE INDEX idx_fotos_registros_registro ON fotos_registros(id_registro, tipo_registro);
