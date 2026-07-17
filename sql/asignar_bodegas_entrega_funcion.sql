-- =============================================================================
-- Reparto de cantidad entre bodegas segun "unidades de entrega".
--
-- Replica, dentro de la base de datos, la misma logica que el sistema de
-- escritorio aplica al crear ordenes (clase DBstock_productos:
-- asignarBodegasEntrega + seleccionarBodegaDescarga). Se instala una sola vez
-- y queda disponible para CUALQUIER aplicacion que cree ordenes contra esta
-- base (incluido wo-Printer), garantizando un comportamiento identico.
--
-- Uso desde la app externa:
--   SELECT id_bodega, cantidad
--   FROM asignar_bodegas_entrega(<id_producto>, <cantidad_total_en_unidades>);
--
-- Devuelve 1 o varias filas (id_bodega, cantidad). La suma de "cantidad"
-- siempre es igual a la cantidad solicitada.
--
-- Compatible con PostgreSQL 9.4.
-- =============================================================================

-- ----------------------------------------------------------------------------
-- Seleccion de bodega cuando el producto NO tiene unidades de entrega
-- configuradas (las "4 reglas automaticas"):
--   1. Bodega con MAYOR stock positivo
--   2. Ultima bodega con movimiento donde hubo stock positivo
--   3. Bodega del ultimo ingreso de mercancia del producto
--   4. Fallback: bodega id = 1
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION seleccionar_bodega_descarga(p_id_producto integer)
RETURNS integer AS $$
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
$$ LANGUAGE plpgsql;

-- ----------------------------------------------------------------------------
-- Reparto principal. Greedy de mayor a menor tamano de paquete: cada paquete
-- completo va a su bodega; el renglon de paquete = 1 (bodega por unidad)
-- absorbe el sobrante. Las cantidades se acumulan por bodega (una fila por
-- bodega). Las cantidades estan en UNIDADES, no en numero de paquetes.
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION asignar_bodegas_entrega(
    p_id_producto integer,
    p_cantidad    double precision
)
RETURNS TABLE(id_bodega integer, cantidad double precision) AS $$
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
$$ LANGUAGE plpgsql;

-- =============================================================================
-- Ejemplos de verificacion:
--
-- Producto 100 SIN unidades configuradas, pido 30:
--   SELECT * FROM asignar_bodegas_entrega(100, 30);
--   -> una sola fila con la bodega de las 4 reglas y cantidad 30.
--
-- Producto 200 con caja de 50 -> bodega 3, y unidad (paquete=1) -> bodega 1.
-- Pido 127:
--   SELECT * FROM asignar_bodegas_entrega(200, 127);
--   -> (3, 100)  [dos cajas de 50]
--      (1, 27)   [sobrante por unidad]
-- =============================================================================
