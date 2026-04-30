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


CREATE TABLE recortes_cabecera
(
  id serial NOT NULL,
  id_producto integer,
  cantidad double precision,
  fecha date,
  hora character varying,
  id_user integer,
  observacion character varying,
  CONSTRAINT pk_recorte PRIMARY KEY (id),
  CONSTRAINT fk_producto_recorte FOREIGN KEY (id_producto) REFERENCES productos(id) on delete CASCADE,
  CONSTRAINT fk_user_recorte FOREIGN KEY (id_user) REFERENCES users (id)
)
WITH (
  OIDS=FALSE
);


CREATE TABLE recortes_detalle
(
  id serial NOT NULL,
  id_cabecera integer NOT NULL,
  cantidad double precision,
  fecha date,
  hora character varying(8),
  observacion character varying,
  id_user integer,
  codigo character varying,
  id_contacto integer,
  estado integer,
  fecha_entrega date,
  hora_entrega character varying(8),
  CONSTRAINT pk_recorte_detalle PRIMARY KEY (id),
  CONSTRAINT fk_recorte_det FOREIGN KEY (id_cabecera) REFERENCES recortes_cabecera (id) on delete CASCADE,
  CONSTRAINT fk_user_recorte_detalle FOREIGN KEY (id_user) REFERENCES users (id),
  CONSTRAINT fk_contacto_recorte FOREIGN KEY (id_contacto) REFERENCES contactos (id)
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
CREATE INDEX idx_novedades_fecha   ON novedades_facturas (fecha_deteccion DESC);
