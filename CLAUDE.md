# controlbodega

Sistema de bodega/facturacion en Java Swing (NetBeans, Ant) sobre PostgreSQL 9.4.
Desde 2026 atiende a **dos empresas** con el mismo codigo y bases separadas:

- `bodega_nuevo` — AGROINSUMOS (tambien es la base de desarrollo)
- `bodega_tecnirepuestos` — TECNIREPUESTOS DEL SUR

Lo que las distingue son datos de configuracion, no codigo: `configuraciones.modo_precios`
(AGRO / TECNI), la tabla `modulos` (que modulos licencio cada cliente) y los permisos.

## Reglas de SQL (importante)

Hay **dos** archivos de SQL vivos, y **siempre se editan juntos, en el mismo commit**:

| archivo | para que |
|---|---|
| `sql/update_controlbodega.sql` | actualizador **acumulativo e idempotente**. Se corre tal cual sobre cualquier base existente y la deja al dia. |
| `sql/controlbodega.sql` | script de **construccion desde cero**, para montar una empresa nueva. |

1. **No se crean archivos de migracion nuevos.** Todo cambio de esquema o de catalogo
   se agrega al final de `sql/update_controlbodega.sql`, en la PARTE B, como un
   incremento nuevo con su fecha (`B.2`, `B.3`, ...), con su propio `BEGIN;/COMMIT;`
   y una nota de que agrega y por que.
2. El **mismo** cambio se refleja en `sql/controlbodega.sql`, en el lugar que le
   corresponde del esquema (columna dentro del `CREATE TABLE`, fila en el catalogo
   de `opciones`, etc.).
3. El actualizador **arranca desde la linea base del 2026-08-29**. Todo lo anterior
   se archivo en `sql/historico/update_controlbodega_20260829.sql`. La **PARTE A**
   del actualizador verifica esa linea base y frena con un mensaje claro si la base
   viene atrasada, diciendo exactamente que falta.
4. En el actualizador **solo entra lo que es seguro correr en cualquier base**:
   tablas, columnas, indices, llaves, funciones, triggers, vistas, catalogos del
   sistema (perfiles, modulos, opciones) y semillas minimas condicionadas a que la
   tabla este vacia. **No entra nada que toque datos del negocio** (cargas de
   inventario, deduplicaciones, unificaciones, correcciones puntuales): eso sigue
   siendo un script propio en `sql/`, se corre a mano una sola vez y se documenta
   en la PARTE C.
5. Motor destino **PostgreSQL 9.4**: no hay `ADD COLUMN IF NOT EXISTS` ni
   `CREATE INDEX IF NOT EXISTS`. Se verifica a mano con `DO $$ ... END $$;` contra
   `information_schema` / `pg_class` / `pg_constraint`.
6. `sql/historico/` guarda las migraciones viejas ya absorbidas: es solo referencia
   y no se agregan archivos ahi. **La unica excepcion** es
   `update_controlbodega_20260829.sql`, que si se corre: es la puesta al dia de una
   base que se quedo atras de la linea base.

### Poner una base al dia

```
psql -f sql/historico/update_controlbodega_20260829.sql   (solo si la PARTE A se queja)
psql -f sql/update_controlbodega.sql
```

### Montar una empresa nueva

```
createdb <base>
psql -f sql/controlbodega.sql
psql -f sql/historico/update_controlbodega_20260829.sql
psql -f sql/update_controlbodega.sql
```

> `controlbodega.sql` todavia no construye la linea base completa: le faltan
> `auditoria_ingresos` + `v_auditoria_ingresos`, las funciones
> `asignar_bodegas_entrega` y `seleccionar_bodega_descarga`, y las columnas
> `bodegas.entrega_automatica` y `facturas_impresas.nit_cliente`. Por eso el paso
> del archivo historico. Cerrar esa brecha en `controlbodega.sql` esta pendiente.

Validacion antes de dar por bueno un cambio de SQL (en PG 9.4):

```
createdb test_cb -> las 3 lineas de "montar una empresa nueva" (todas exit 0)
                 -> psql -f sql/update_controlbodega.sql otra vez (no debe cambiar nada)
clon de una base real -> psql -f sql/update_controlbodega.sql (dos veces seguidas)
```

La PARTE D del actualizador imprime la verificacion: las filas de D.1 deben salir
en `t` y los conteos de datos del negocio deben quedar identicos a los de antes.

## Permisos

Los permisos son administrables en BD (`opciones` + `perfil_opciones` + `usuario_opciones`),
no estan quemados en el codigo. Para condicionar una accion:

```java
boolean autorizado = Metodos.Permisos.estaCargado()
        ? Metodos.Permisos.puede("clave_de_la_opcion")
        : DB_consultas_R_D.validar_admin();   // base sin migrar: comportamiento anterior
```

El perfil 1 (Admin) siempre puede, salvo que la opcion pertenezca a un modulo apagado
en `modulos`. Una opcion nueva se agrega al catalogo `opciones` en los dos archivos SQL.

## Compilar

El proyecto es Ant (NetBeans). Para verificar un archivo suelto sin abrir el IDE:

```
javac -encoding UTF-8 -cp "build/classes;<todos los dist/lib/*.jar>" -d <salida> src/.../Archivo.java
```

## Runtime

Los equipos deben correr con **JRE Adoptium**; con el JRE viejo de Oracle el texto
de Swing sale corrido un caracter.
