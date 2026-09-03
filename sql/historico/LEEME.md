# sql/historico

Migraciones incrementales que ya **no se corren**: su contenido esta dentro de
`sql/update_controlbodega.sql` (PARTE A o PARTE B). Se conservan solo como
referencia de cuando y por que se hizo cada cambio.

**No agregues archivos nuevos aqui ni a `sql/`.** Todo cambio de esquema o de
catalogo va al final de `sql/update_controlbodega.sql`, y el mismo cambio se
refleja en `sql/controlbodega.sql`.

Lo unico que sigue viviendo en `sql/` son los scripts de **datos**, que se
corren a mano una sola vez y solo en la base donde apliquen:
`dedup_contactos_*`, `unificacion_usuarios`, `migracion_stock_minimo_unidades`,
`reparar_*`, `diagnostico_*`.
