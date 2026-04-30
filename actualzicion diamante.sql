ALTER TABLE configuraciones ADD COLUMN impresora_venta_80mm character varying;


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
  CONSTRAINT fkcontactocot FOREIGN KEY (id_contacto)
      REFERENCES contactos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
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

CREATE TABLE devoluciones
(
  id serial NOT NULL,
  id_factura integer,
  id_user integer,
  total double precision,
  fecha date,
  hora character varying(8),
  CONSTRAINT pkdevoluciones PRIMARY KEY (id),
  CONSTRAINT fk_id_userdev FOREIGN KEY (id_user)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fkfactura FOREIGN KEY (id_factura)
      REFERENCES facturas_cabeceras (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE TABLE devoluciones_detalles
(
  id serial NOT NULL,
  id_cabecera_devolucion integer,
  id_producto integer,
  cantidad double precision,
  valor_unitario double precision,
  total double precision,
  CONSTRAINT pkdetallesdev PRIMARY KEY (id),
  CONSTRAINT fk_id_cabecera_devolucion FOREIGN KEY (id_cabecera_devolucion)
      REFERENCES devoluciones (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fkproducto_devolucion FOREIGN KEY (id_producto)
      REFERENCES productos (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

ALTER TABLE facturas_cabeceras ADD COLUMN tipo_pago integer;
update facturas_cabeceras set tipo_pago=0;

ALTER TABLE facturas_cabeceras ADD COLUMN id_user_edita integer;
ALTER TABLE facturas_cabeceras ADD COLUMN importado integer;

ALTER TABLE facturas_detalles ADD COLUMN id_factura integer;
update facturas_detalles set id_factura=0;

ALTER TABLE productos ADD COLUMN precio_venta2 double precision;
update productos set precio_venta2=precio_venta;
ALTER TABLE productos ADD COLUMN precio_venta3 double precision;
update productos set precio_venta3=precio_venta;