-- ============================================================================
-- Deduplicacion de contactos - Paso 01: Funcion nucleo fusionar_contacto()
-- ----------------------------------------------------------------------------
-- Absorbe el contacto p_dup dentro de p_super:
--   1. Copia al sobreviviente los datos que este tenga vacios.
--   2. Reasigna TODOS los registros hijos (13 FKs) de p_dup a p_super.
--      - producto_proveedores: maneja la colision por uq(id_producto,id_proveedor).
--      - facturas_cabeceras / ingresos_*: concatena el nombre antiguo en su
--        columna de texto (observacion / descripcion / observacion).
--   3. Si el sobreviviente queda sin cedula -> cedula = nombre.
--   4. Registra en dedup_contactos_log y elimina p_dup.
--
-- Requiere haber ejecutado dedup_contactos_00_preparacion.sql.
-- La funcion NO abre transaccion propia: corre dentro de la del script que la llame.
-- ============================================================================

CREATE OR REPLACE FUNCTION fusionar_contacto(p_super integer, p_dup integer, p_fase text DEFAULT 'manual')
RETURNS void
LANGUAGE plpgsql
AS $$
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
$$;

SELECT 'funcion fusionar_contacto creada' AS estado;
