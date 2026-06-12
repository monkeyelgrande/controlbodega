-- =============================================================================
-- MODULO DE PERMISOS ADMINISTRABLES (bodega_nuevo)
-- FASE 1: esquema + sembrado desde el estado actual del switch frm_main.permisos()
-- Idempotente (re-ejecutable). Compatible PostgreSQL 9.4.
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
-- El perfil 1 (Admin) siempre tiene todo (la app no consulta exclusiones para el).
--
-- El sembrado de perfil_opciones y usuario_roles_precios SOLO corre si la tabla
-- esta vacia: re-ejecutar este script nunca pisa cambios hechos desde la
-- pantalla de administracion.
-- =============================================================================

BEGIN;

-- 1. Tablas
CREATE TABLE IF NOT EXISTS opciones (
    id serial NOT NULL,
    clave character varying(60) NOT NULL,    -- identificador estable usado por el codigo
    nombre character varying(100) NOT NULL,  -- etiqueta legible para la pantalla de admin
    modulo character varying(60) NOT NULL,   -- agrupacion en el arbol de admin
    componente character varying(60),        -- campo de frm_main que gobierna (null = accion logica)
    orden integer NOT NULL DEFAULT 0,
    CONSTRAINT pk_opciones PRIMARY KEY (id),
    CONSTRAINT uq_opciones_clave UNIQUE (clave)
);

CREATE TABLE IF NOT EXISTS perfil_opciones (
    id_perfil integer NOT NULL,
    id_opcion integer NOT NULL,
    CONSTRAINT pk_perfil_opciones PRIMARY KEY (id_perfil, id_opcion),
    CONSTRAINT fk_po_perfil FOREIGN KEY (id_perfil) REFERENCES perfiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_po_opcion FOREIGN KEY (id_opcion) REFERENCES opciones (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS usuario_opciones (
    id_user integer NOT NULL,
    id_opcion integer NOT NULL,
    concedido boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_usuario_opciones PRIMARY KEY (id_user, id_opcion),
    CONSTRAINT fk_uo_user FOREIGN KEY (id_user) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_uo_opcion FOREIGN KEY (id_opcion) REFERENCES opciones (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS usuario_roles_precios (
    id_user integer NOT NULL,
    rol integer NOT NULL,                    -- 2=almacenista 3=contable 4=precios
    CONSTRAINT pk_usuario_roles_precios PRIMARY KEY (id_user, rol),
    CONSTRAINT fk_urp_user FOREIGN KEY (id_user) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_urp_rol CHECK (rol IN (2, 3, 4))
);

-- 2. Catalogo de opciones (un registro por componente gobernado en permisos()).
--    Idempotente por clave: agrega solo las que falten.
INSERT INTO opciones (clave, nombre, modulo, componente, orden)
SELECT v.clave, v.nombre, v.modulo, v.componente, v.orden
FROM (
    VALUES
    -- Configuracion
    ('jmenu_configuraciones',      'Configuraciones',          'Configuracion', 'jmenu_configuraciones',      10),
    ('jmenu_user',                 'Usuarios',                 'Configuracion', 'jmenu_user',                 20),
    ('jmenu_backup',               'BackUp',                   'Configuracion', 'jmenu_backup',               30),
    ('jMenu_tipo_ingreso',         'Tipo de ingreso',          'Configuracion', 'jMenu_tipo_ingreso',         40),
    ('jmenu_bodegas',              'Bodegas',                  'Configuracion', 'jmenu_bodegas',              50),
    ('jMenu_unidades',             'Unidades de medida',       'Configuracion', 'jMenu_unidades',             60),
    ('jmenu_permisos',             'Permisos de la aplicacion','Configuracion', NULL,                         70),
    -- Contactos
    ('jmenu_con',                  'Contactos (menu)',         'Contactos',     'jmenu_con',                  10),
    ('jmenu_contactos',            'Contactos (item de menu)', 'Contactos',     'jmenu_contactos',            20),
    ('btn_contactos',              'Contactos (boton)',        'Contactos',     'btn_contactos',              30),
    -- Productos
    ('jMenu_productos_principal',  'Productos (menu)',         'Productos',     'jMenu_productos_principal',  10),
    ('btn_productos',              'Productos (boton)',        'Productos',     'btn_productos',              20),
    ('btn_ingreso_productos',      'Ingreso de productos',     'Productos',     'btn_ingreso_productos',      30),
    -- Ordenes
    ('jMenu_ordenes',              'Ordenes (menu)',           'Ordenes',       'jMenu_ordenes',              10),
    ('jmenu_facturacion',          'Generar orden (menu)',     'Ordenes',       'jmenu_facturacion',          20),
    ('btn_generar_orden',          'Generar orden (boton)',    'Ordenes',       'btn_generar_orden',          30),
    ('btn_ver_ordenes',            'Ver ordenes',              'Ordenes',       'btn_ver_ordenes',            40),
    ('btn_facturar',               'Ordenes (boton)',          'Ordenes',       'btn_facturar',               50),
    ('btn_ver_facturas',           'Ver ordenes (boton)',      'Ordenes',       'btn_ver_facturas',           60),
    ('btn_decolucion',             'Devolucion',               'Ordenes',       'btn_decolucion',             70),
    -- Recortes
    ('jMenu_recortes',             'Recortes (menu)',          'Recortes',      'jMenu_recortes',             10),
    ('btn_generar_recorte',        'Generar recorte',          'Recortes',      'btn_generar_recorte',        20),
    ('btn_ver_recortes',           'Ver recortes',             'Recortes',      'btn_ver_recortes',           30)
) AS v (clave, nombre, modulo, componente, orden)
WHERE NOT EXISTS (SELECT 1 FROM opciones o WHERE o.clave = v.clave);

-- 3. Sembrado de perfil_opciones: reproduce EXACTAMENTE el switch actual de
--    frm_main.permisos() (solo lineas activas; las comentadas quedan visibles).
--    Concedido = NO oculto. Solo corre con la tabla vacia.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM perfil_opciones) THEN

        -- Perfil 1 (Admin): todas las opciones
        INSERT INTO perfil_opciones (id_perfil, id_opcion)
        SELECT p.id, o.id FROM perfiles p, opciones o WHERE p.id = 1;

        -- Perfil 2 (Bodeguero)
        INSERT INTO perfil_opciones (id_perfil, id_opcion)
        SELECT p.id, o.id FROM perfiles p, opciones o
        WHERE p.id = 2
          AND o.clave NOT IN (
              'jmenu_permisos',
              'jmenu_configuraciones', 'jmenu_user', 'jmenu_contactos', 'jmenu_con',
              'jMenu_productos_principal', 'jmenu_backup', 'jmenu_facturacion',
              'jMenu_tipo_ingreso', 'jmenu_bodegas', 'jMenu_unidades',
              'btn_contactos', 'btn_productos', 'btn_ingreso_productos',
              'btn_generar_recorte', 'btn_facturar', 'btn_ver_facturas', 'btn_decolucion');

        -- Perfil 3 (Contadora): como el 2 pero con generar orden (menu) y generar recorte
        INSERT INTO perfil_opciones (id_perfil, id_opcion)
        SELECT p.id, o.id FROM perfiles p, opciones o
        WHERE p.id = 3
          AND o.clave NOT IN (
              'jmenu_permisos',
              'jmenu_configuraciones', 'jmenu_user', 'jmenu_contactos', 'jmenu_con',
              'jMenu_productos_principal', 'jmenu_backup',
              'jMenu_tipo_ingreso', 'jmenu_bodegas', 'jMenu_unidades',
              'btn_contactos', 'btn_productos', 'btn_ingreso_productos',
              'btn_facturar', 'btn_ver_facturas', 'btn_decolucion');

        -- Perfil 4 (Precios): solo facturacion (btn_facturar/ver_facturas/decolucion)
        INSERT INTO perfil_opciones (id_perfil, id_opcion)
        SELECT p.id, o.id FROM perfiles p, opciones o
        WHERE p.id = 4
          AND o.clave NOT IN (
              'jmenu_permisos',
              'jmenu_configuraciones', 'jmenu_user', 'jmenu_contactos', 'jmenu_con',
              'jMenu_productos_principal', 'jmenu_backup', 'jmenu_facturacion',
              'jMenu_tipo_ingreso', 'jmenu_bodegas', 'jMenu_unidades',
              'jMenu_recortes', 'jMenu_ordenes',
              'btn_contactos', 'btn_productos', 'btn_ingreso_productos',
              'btn_generar_orden', 'btn_generar_recorte',
              'btn_ver_ordenes', 'btn_ver_recortes');

        -- Perfil 5 (Ventas): conserva productos, generar orden y recortes
        INSERT INTO perfil_opciones (id_perfil, id_opcion)
        SELECT p.id, o.id FROM perfiles p, opciones o
        WHERE p.id = 5
          AND o.clave NOT IN (
              'jmenu_permisos',
              'jmenu_configuraciones', 'jmenu_user', 'jmenu_contactos', 'jmenu_con',
              'jmenu_backup', 'jMenu_tipo_ingreso', 'jmenu_bodegas', 'jMenu_unidades',
              'btn_contactos', 'btn_ingreso_productos',
              'btn_facturar', 'btn_ver_facturas', 'btn_decolucion');

    END IF;
END $$;

-- 4. Roles de Precios por usuario, sembrados desde users.rol_precios actual.
--    La columna users.rol_precios se CONSERVA durante la transicion.
--    Solo corre con la tabla vacia.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuario_roles_precios) THEN
        INSERT INTO usuario_roles_precios (id_user, rol)
        SELECT id, rol_precios FROM users WHERE rol_precios IN (2, 3, 4);
    END IF;
END $$;

COMMIT;
