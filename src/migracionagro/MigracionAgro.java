package migracionagro;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Migracion one-shot productos_agro -> bodega_nuevo (fusion de sistemas).
 *
 * Ejecutar con AMBAS aplicaciones CERRADAS (los ids max+1 no toleran
 * concurrencia). Re-ejecutable: lo ya migrado (registrado en migracion_mapeo)
 * se omite.
 *
 * Uso:  java migracionagro.MigracionAgro [--dry-run] [--destino=nombre_bd]
 *   --dry-run: ejecuta todo en una transaccion y hace ROLLBACK al final
 *              (no toca secuencias). Sirve para revisar el reporte.
 *   --destino=nombre_bd: migra hacia otra base (p. ej. una copia de prueba
 *              restaurada de bodega_nuevo) en lugar de bodega_nuevo.
 *
 * Requiere haber aplicado antes sql/migracion_fusion_agro.sql sobre
 * bodega_nuevo.
 */
public class MigracionAgro {

    private static final String URL_ORIGEN = "jdbc:postgresql://localhost:5432/productos_agro";
    private static final String URL_DESTINO_BASE = "jdbc:postgresql://localhost:5432/";
    private static final String BD_DESTINO_DEFAULT = "bodega_nuevo";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "monkey";

    private final Connection origen;
    private final Connection destino;
    private final boolean dryRun;
    private final Mapeo mapeo;
    private final ReporteMigracion rep = new ReporteMigracion();

    /** codigo_barras (trim) -> datos del producto ya existente en bodega. */
    private static class ProductoDestino {
        int id;
        double precioCosto;
        double precioVenta;
        int cantPaquete;
        String descripcion;
    }

    public static void main(String[] args) throws Exception {
        boolean dry = false;
        String bdDestino = BD_DESTINO_DEFAULT;
        for (String a : args) {
            if ("--dry-run".equalsIgnoreCase(a)) {
                dry = true;
            } else if (a.toLowerCase().startsWith("--destino=")) {
                bdDestino = a.substring("--destino=".length()).trim();
            }
        }
        MigracionAgro m = new MigracionAgro(dry, bdDestino);
        try {
            m.ejecutar();
        } finally {
            m.cerrar();
        }
    }

    private final String bdDestino;

    private MigracionAgro(boolean dryRun, String bdDestino) throws SQLException {
        this.dryRun = dryRun;
        this.bdDestino = bdDestino;
        this.origen = DriverManager.getConnection(URL_ORIGEN, DB_USER, DB_PASS);
        this.destino = DriverManager.getConnection(URL_DESTINO_BASE + bdDestino, DB_USER, DB_PASS);
        this.origen.setReadOnly(true);
        this.destino.setAutoCommit(false);
        this.mapeo = new Mapeo(destino);
    }

    private void ejecutar() {
        rep.linea("MIGRACION productos_agro -> " + bdDestino + " " + (dryRun ? "(DRY-RUN: sin commit)" : ""));
        rep.linea("=========================================================");
        try {
            migrarUnidades();
            commitPaso("unidades");
            migrarUsers();
            commitPaso("users");
            migrarContactos();
            commitPaso("contactos");
            migrarProductos();
            commitPaso("productos");
            migrarIngresosCabecera();
            commitPaso("ingresos cabecera");
            migrarIngresosDetalle();
            commitPaso("ingresos detalle");
            migrarPagos();
            commitPaso("pagos");
            migrarDescuentosYConfig();
            commitPaso("descuentos y configuracion");
            if (!dryRun) {
                ajustarSecuencias();
                commitPaso("secuencias");
            } else {
                rep.linea("[dry-run] secuencias: omitidas (setval no es transaccional)");
            }
            validar();
            if (dryRun) {
                destino.rollback();
                rep.linea("");
                rep.linea("DRY-RUN: ROLLBACK ejecutado, la base destino NO fue modificada.");
            }
            rep.resumen();
        } catch (Exception ex) {
            try {
                destino.rollback();
            } catch (SQLException ignore) {
            }
            rep.linea("");
            rep.linea("ERROR - migracion detenida (el paso actual fue revertido): " + ex);
            ex.printStackTrace();
            rep.resumen();
        }
        rep.guardar(dryRun ? "dryrun" : "");
    }

    private void commitPaso(String paso) throws SQLException {
        if (!dryRun) {
            destino.commit();
        }
        rep.linea("-- paso completado: " + paso);
    }

    private void cerrar() {
        try {
            origen.close();
        } catch (SQLException ignore) {
        }
        try {
            destino.close();
        } catch (SQLException ignore) {
        }
    }

    // =========================================================================
    // 1. Unidades de medida
    // =========================================================================
    private void migrarUnidades() throws SQLException {
        rep.linea("");
        rep.linea("1) Unidades de medida");

        // Equivalencias explicitas agro -> bodega (en minusculas)
        Map<String, String> equivalencias = new HashMap<>();
        equivalencias.put("und.", "unidad");
        equivalencias.put("m", "metro");
        equivalencias.put("kg", "kilogramo");

        Map<String, Integer> destinoPorNombre = new HashMap<>();
        try (Statement st = destino.createStatement();
                ResultSet rs = st.executeQuery("select id, nombre from unidades_medidas")) {
            while (rs.next()) {
                destinoPorNombre.put(rs.getString("nombre").trim().toLowerCase(), rs.getInt("id"));
            }
        }
        int nextId = maxId("unidades_medidas");

        try (Statement st = origen.createStatement();
                ResultSet rs = st.executeQuery("select id, nombre from unidades_medidas order by id")) {
            while (rs.next()) {
                int idViejo = rs.getInt("id");
                String nombre = rs.getString("nombre") == null ? "" : rs.getString("nombre").trim();
                if (mapeo.existe("unidades_medidas", idViejo)) {
                    rep.incrementar("unidades ya migradas (omitidas)");
                    continue;
                }
                String clave = nombre.toLowerCase();
                String claveDestino = equivalencias.containsKey(clave) ? equivalencias.get(clave) : clave;
                Integer idDestino = destinoPorNombre.get(claveDestino);
                if (idDestino != null) {
                    mapeo.put("unidades_medidas", idViejo, idDestino, "mapeado");
                    rep.incrementar("unidades mapeadas a existentes");
                } else {
                    nextId++;
                    try (PreparedStatement ps = destino.prepareStatement(
                            "insert into unidades_medidas (id, nombre) values (?,?)")) {
                        ps.setInt(1, nextId);
                        ps.setString(2, trunc(nombre, 30));
                        ps.executeUpdate();
                    }
                    destinoPorNombre.put(clave, nextId);
                    mapeo.put("unidades_medidas", idViejo, nextId, "creado");
                    rep.incrementar("unidades creadas");
                }
            }
        }
    }

    // =========================================================================
    // 2. Usuarios
    // =========================================================================
    private void migrarUsers() throws SQLException {
        rep.linea("");
        rep.linea("2) Usuarios");

        Map<String, Integer> destinoPorUserName = new HashMap<>();
        try (Statement st = destino.createStatement();
                ResultSet rs = st.executeQuery("select id, user_name from users")) {
            while (rs.next()) {
                destinoPorUserName.put(rs.getString("user_name").trim().toLowerCase(), rs.getInt("id"));
            }
        }
        int nextId = maxId("users");

        try (Statement st = origen.createStatement();
                ResultSet rs = st.executeQuery(
                        "select id, nombre, password, user_name, direccion, telefono, telefono2, "
                        + "sitioweb, estado, email, id_perfil from users order by id")) {
            while (rs.next()) {
                int idViejo = rs.getInt("id");
                if (mapeo.existe("users", idViejo)) {
                    rep.incrementar("users ya migrados (omitidos)");
                    continue;
                }
                String userName = rs.getString("user_name") == null ? "" : rs.getString("user_name").trim();
                int rolPrecios = rolPreciosDesdePerfilAgro(rs.getInt("id_perfil"));
                Integer idDestino = destinoPorUserName.get(userName.toLowerCase());
                if (idDestino != null) {
                    // Usuario ya existe en bodega: solo se le asigna el rol del
                    // modulo Precios si aun no tiene uno.
                    try (PreparedStatement ps = destino.prepareStatement(
                            "update users set rol_precios = ? where id = ? and coalesce(rol_precios,0) = 0")) {
                        ps.setInt(1, rolPrecios);
                        ps.setInt(2, idDestino);
                        ps.executeUpdate();
                    }
                    mapeo.put("users", idViejo, idDestino, "mapeado");
                    rep.incrementar("users mapeados a existentes (rol_precios asignado)");
                } else {
                    // Usuario nuevo ACTIVO con su mismo password de agro (ambos
                    // sistemas usan sha256Hex). Perfil vendedor (minimo
                    // privilegio en bodega); su acceso real es rol_precios.
                    nextId++;
                    try (PreparedStatement ps = destino.prepareStatement(
                            "insert into users (id, nombre, password, user_name, direccion, telefono, "
                            + "telefono2, sitioweb, estado, email, id_perfil, rol_precios, "
                            + "imprime_ordenes, aprueba_compras) "
                            + "values (?,?,?,?,?,?,?,?,?,?,?,?,false,false)")) {
                        ps.setInt(1, nextId);
                        ps.setString(2, trunc(rs.getString("nombre"), 60));
                        ps.setString(3, rs.getString("password"));
                        ps.setString(4, trunc(userName, 30));
                        ps.setString(5, trunc(rs.getString("direccion"), 80));
                        ps.setString(6, trunc(rs.getString("telefono"), 20));
                        ps.setString(7, trunc(rs.getString("telefono2"), 20));
                        ps.setString(8, trunc(rs.getString("sitioweb"), 50));
                        ps.setString(9, "Activo");
                        ps.setString(10, trunc(rs.getString("email"), 50));
                        ps.setInt(11, 3); // vendedor: minimo privilegio en bodega
                        ps.setInt(12, rolPrecios);
                        ps.executeUpdate();
                    }
                    mapeo.put("users", idViejo, nextId, "creado");
                    rep.incrementar("users creados (Activos, mismo password de agro)");
                    rep.advertencia("Usuario agro '" + userName + "' creado ACTIVO con su password "
                            + "de agro, perfil vendedor + rol_precios=" + rolPrecios + ".");
                }
            }
        }
    }

    /** Perfiles agro: 1=admin, 2=Bodega, 3=Contadora, 4=Precios. */
    private int rolPreciosDesdePerfilAgro(int idPerfilAgro) {
        switch (idPerfilAgro) {
            case 1: return 4;
            case 2: return 2;
            case 3: return 3;
            case 4: return 4;
            default: return 0;
        }
    }

    // =========================================================================
    // 3. Contactos (todos, sin deduplicacion - decision del usuario)
    // =========================================================================
    private void migrarContactos() throws SQLException {
        rep.linea("");
        rep.linea("3) Contactos (sin deduplicacion, origen='agro')");

        // bodega_nuevo tiene un indice UNIQUE sobre cedula (no nula / no vacia):
        // las cedulas que colisionen entran con cedula NULL y la original queda
        // anotada en observaciones para la futura herramienta de unificacion.
        Set<String> cedulasUsadas = new HashSet<>();
        try (Statement st = destino.createStatement();
                ResultSet rs = st.executeQuery(
                        "select trim(cedula) as c from contactos "
                        + "where cedula is not null and trim(cedula) <> ''")) {
            while (rs.next()) {
                cedulasUsadas.add(rs.getString("c"));
            }
        }
        int nextId = maxId("contactos");

        try (Statement st = origen.createStatement();
                ResultSet rs = st.executeQuery(
                        "select id, nombre, cedula, direccion, ciudad, contacto, contacto2, descuento, "
                        + "email, forma_pago, cuenta, tipo_cuenta, numero_cuenta, observaciones, proveedor "
                        + "from contactos order by id")) {
            while (rs.next()) {
                int idViejo = rs.getInt("id");
                if (mapeo.existe("contactos", idViejo)) {
                    rep.incrementar("contactos ya migrados (omitidos)");
                    continue;
                }
                String cedula = rs.getString("cedula") == null ? "" : rs.getString("cedula").trim();
                String observaciones = rs.getString("observaciones");
                if (!cedula.isEmpty()) {
                    if (cedulasUsadas.contains(cedula)) {
                        observaciones = (observaciones == null ? "" : observaciones + " ")
                                + "[Cedula original: " + cedula + " - duplicada en bodega, migrado de agro]";
                        cedula = null;
                        rep.incrementar("contactos con cedula duplicada (cedula -> NULL + nota)");
                    } else {
                        cedulasUsadas.add(cedula);
                    }
                } else {
                    cedula = null;
                }

                nextId++;
                try (PreparedStatement ps = destino.prepareStatement(
                        "insert into contactos (id, nombre, cedula, direccion, ciudad, contacto, contacto2, "
                        + "descuento, email, forma_pago, cuenta, tipo_cuenta, numero_cuenta, observaciones, "
                        + "proveedor, origen) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setInt(1, nextId);
                    ps.setString(2, trunc(rs.getString("nombre"), 600));
                    ps.setString(3, trunc(cedula, 150));
                    ps.setString(4, trunc(rs.getString("direccion"), 80));
                    ps.setString(5, trunc(rs.getString("ciudad"), 80));
                    ps.setString(6, trunc(rs.getString("contacto"), 200));
                    ps.setString(7, trunc(rs.getString("contacto2"), 20));
                    ps.setDouble(8, rs.getDouble("descuento"));
                    ps.setString(9, trunc(rs.getString("email"), 50));
                    ps.setString(10, trunc(rs.getString("forma_pago"), 20));
                    ps.setString(11, trunc(rs.getString("cuenta"), 20));
                    ps.setString(12, trunc(rs.getString("tipo_cuenta"), 20));
                    ps.setString(13, trunc(rs.getString("numero_cuenta"), 20));
                    ps.setString(14, observaciones);
                    ps.setObject(15, rs.getObject("proveedor"));
                    ps.setString(16, "agro");
                    ps.executeUpdate();
                }
                mapeo.put("contactos", idViejo, nextId, "creado");
                rep.incrementar("contactos creados");
            }
        }
    }

    // =========================================================================
    // 4. Productos (fusion por codigo_barras + backfill de precios en 0)
    // =========================================================================
    private void migrarProductos() throws SQLException {
        rep.linea("");
        rep.linea("4) Productos (fusion por codigo_barras)");

        Map<String, ProductoDestino> porCodigo = new HashMap<>();
        try (Statement st = destino.createStatement();
                ResultSet rs = st.executeQuery(
                        "select id, trim(codigo_barras) as codigo, coalesce(precio_costo,0) as pc, "
                        + "coalesce(precio_venta,0) as pv, coalesce(cant_paquete,0) as cp, descripcion "
                        + "from productos")) {
            while (rs.next()) {
                ProductoDestino p = new ProductoDestino();
                p.id = rs.getInt("id");
                p.precioCosto = rs.getDouble("pc");
                p.precioVenta = rs.getDouble("pv");
                p.cantPaquete = rs.getInt("cp");
                p.descripcion = rs.getString("descripcion");
                porCodigo.put(rs.getString("codigo"), p);
            }
        }
        int nextId = maxId("productos");
        List<String> descDiferentes = new ArrayList<>();

        String sqlUpdate = "update productos set venta=?, valor_desc_1=?, valor_desc_2=?, "
                + "valor_s_y_t=?, valor_credito=?, iva=?, porcentaje_utilidad=?, "
                + "cant_paquete = case when coalesce(cant_paquete,0) <= 1 and ? > 1 then ? else cant_paquete end, "
                + "precio_costo = case when coalesce(precio_costo,0) = 0 then ? else precio_costo end, "
                + "precio_venta = case when coalesce(precio_venta,0) = 0 then ? else precio_venta end "
                + "where id = ?";
        String sqlInsert = "insert into productos (id, codigo_barras, descripcion, stock_minimo, stock_ideal, "
                + "id_unidad, cant_paquete, precio_costo, precio_venta, precio_venta2, precio_venta3, estado, "
                + "venta, valor_desc_1, valor_desc_2, valor_s_y_t, valor_credito, iva, porcentaje_utilidad) "
                + "values (?,?,?,0,0,?,?,?,?,0,0,?,?,?,?,?,?,?,?)";

        try (Statement st = origen.createStatement();
                ResultSet rs = st.executeQuery(
                        "select id, trim(codigo_barras) as codigo, descripcion, id_unidad, cant_paquete, "
                        + "precio_costo, venta, valor_desc_1, valor_desc_2, valor_s_y_t, valor_credito, "
                        + "iva, porcentaje_utilidad, estado from productos order by id")) {
            while (rs.next()) {
                int idViejo = rs.getInt("id");
                if (mapeo.existe("productos", idViejo)) {
                    rep.incrementar("productos ya migrados (omitidos)");
                    continue;
                }
                String codigo = rs.getString("codigo");
                ProductoDestino dest = porCodigo.get(codigo);
                if (dest != null) {
                    int cantPaqueteAgro = rs.getInt("cant_paquete");
                    try (PreparedStatement ps = destino.prepareStatement(sqlUpdate)) {
                        ps.setDouble(1, rs.getDouble("venta"));
                        ps.setDouble(2, rs.getDouble("valor_desc_1"));
                        ps.setDouble(3, rs.getDouble("valor_desc_2"));
                        ps.setDouble(4, rs.getDouble("valor_s_y_t"));
                        ps.setDouble(5, rs.getDouble("valor_credito"));
                        ps.setDouble(6, rs.getDouble("iva"));
                        ps.setDouble(7, rs.getDouble("porcentaje_utilidad"));
                        ps.setInt(8, cantPaqueteAgro);
                        ps.setInt(9, cantPaqueteAgro);
                        ps.setDouble(10, rs.getDouble("precio_costo"));
                        ps.setDouble(11, rs.getDouble("venta"));
                        ps.setInt(12, dest.id);
                        ps.executeUpdate();
                    }
                    if (dest.precioCosto == 0 && rs.getDouble("precio_costo") != 0) {
                        rep.incrementar("productos backfill precio_costo (estaba en 0)");
                    }
                    if (dest.precioVenta == 0 && rs.getDouble("venta") != 0) {
                        rep.incrementar("productos backfill precio_venta (estaba en 0)");
                    }
                    if (dest.cantPaquete <= 1 && cantPaqueteAgro > 1) {
                        rep.incrementar("productos cant_paquete completado desde agro");
                    }
                    String descAgro = rs.getString("descripcion") == null ? "" : rs.getString("descripcion").trim();
                    String descBodega = dest.descripcion == null ? "" : dest.descripcion.trim();
                    if (!descAgro.equalsIgnoreCase(descBodega)) {
                        descDiferentes.add(codigo + " | agro: " + descAgro + " | bodega: " + descBodega);
                    }
                    mapeo.put("productos", idViejo, dest.id, "fusionado");
                    rep.incrementar("productos fusionados (codigo ya existia)");
                } else {
                    nextId++;
                    try (PreparedStatement ps = destino.prepareStatement(sqlInsert)) {
                        ps.setInt(1, nextId);
                        ps.setString(2, codigo);
                        ps.setString(3, trunc(rs.getString("descripcion"), 1000));
                        Object idUnidadAgro = rs.getObject("id_unidad");
                        ps.setObject(4, idUnidadAgro == null ? null
                                : mapeo.remap("unidades_medidas", ((Number) idUnidadAgro).intValue()));
                        ps.setInt(5, rs.getInt("cant_paquete"));
                        ps.setDouble(6, rs.getDouble("precio_costo"));
                        ps.setDouble(7, rs.getDouble("venta")); // backfill: usable en bodega
                        ps.setBoolean(8, rs.getInt("estado") == 1);
                        ps.setDouble(9, rs.getDouble("venta"));
                        ps.setDouble(10, rs.getDouble("valor_desc_1"));
                        ps.setDouble(11, rs.getDouble("valor_desc_2"));
                        ps.setDouble(12, rs.getDouble("valor_s_y_t"));
                        ps.setDouble(13, rs.getDouble("valor_credito"));
                        ps.setDouble(14, rs.getDouble("iva"));
                        ps.setDouble(15, rs.getDouble("porcentaje_utilidad"));
                        ps.executeUpdate();
                    }
                    mapeo.put("productos", idViejo, nextId, "creado");
                    rep.incrementar("productos creados (codigo nuevo en bodega)");
                }
            }
        }

        if (!descDiferentes.isEmpty()) {
            rep.linea("  Productos fusionados con descripcion DISTINTA entre bases (revision humana): "
                    + descDiferentes.size());
            int mostrar = Math.min(descDiferentes.size(), 200);
            for (int i = 0; i < mostrar; i++) {
                rep.linea("    " + descDiferentes.get(i));
            }
            if (descDiferentes.size() > mostrar) {
                rep.linea("    ... y " + (descDiferentes.size() - mostrar) + " mas");
            }
        }
    }

    // =========================================================================
    // 5. Ingresos: cabecera (ids originales conservados)
    // =========================================================================
    private void migrarIngresosCabecera() throws SQLException {
        rep.linea("");
        rep.linea("5) Ingresos de productos: cabecera");

        String sqlInsert = "insert into ingresos_productos_cabecera (id, no_factura, id_proveedor, "
                + "id_transportador, id_user, total, fecha, hora, estado, observacion, id_bodega, "
                + "fecha_vencimiento, enviado_control_bodega) values (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Statement st = origen.createStatement();
                ResultSet rs = st.executeQuery(
                        "select id, no_factura, id_proveedor, id_transportador, id_user, total, fecha, hora, "
                        + "estado, observacion, id_bodega, fecha_vencimiento, "
                        + "coalesce(enviado_control_bodega, false) as enviado "
                        + "from ingresos_productos_cabecera order by id")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                if (mapeo.existe("ingresos_productos_cabecera", id)) {
                    rep.incrementar("cabeceras ya migradas (omitidas)");
                    continue;
                }
                try (PreparedStatement ps = destino.prepareStatement(sqlInsert)) {
                    ps.setInt(1, id);
                    ps.setString(2, rs.getString("no_factura"));
                    ps.setObject(3, mapeo.remap("contactos", (Integer) rs.getObject("id_proveedor")));
                    ps.setObject(4, mapeo.remap("contactos", (Integer) rs.getObject("id_transportador")));
                    ps.setObject(5, mapeo.remap("users", (Integer) rs.getObject("id_user")));
                    ps.setObject(6, rs.getObject("total"));
                    ps.setDate(7, rs.getDate("fecha"));
                    ps.setString(8, rs.getString("hora"));
                    ps.setObject(9, rs.getObject("estado"));
                    ps.setString(10, rs.getString("observacion"));
                    ps.setObject(11, rs.getObject("id_bodega"));
                    ps.setDate(12, rs.getDate("fecha_vencimiento"));
                    ps.setBoolean(13, rs.getBoolean("enviado"));
                    ps.executeUpdate();
                }
                mapeo.put("ingresos_productos_cabecera", id, id, "creado");
                rep.incrementar("cabeceras de ingreso migradas");
            }
        }
    }

    // =========================================================================
    // 6. Ingresos: detalle (batch, ids originales conservados)
    // =========================================================================
    private void migrarIngresosDetalle() throws SQLException {
        rep.linea("");
        rep.linea("6) Ingresos de productos: detalle");

        String sqlInsert = "insert into ingresos_productos_detalle (id, id_ingreso_cabecera, id_producto, "
                + "cantidad, iva, precio_costo, venta, valor_desc_1, valor_desc_2, valor_s_y_t, "
                + "valor_credito, descuento, porcentaje_utilidad, desc_n_1, desc_n_2, etiquetas, "
                + "id_bodega_control) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ins = destino.prepareStatement(sqlInsert);
                Statement st = origen.createStatement();
                ResultSet rs = st.executeQuery(
                        "select id, id_ingreso_cabecera, id_producto, cantidad, iva, precio_costo, venta, "
                        + "valor_desc_1, valor_desc_2, valor_s_y_t, valor_credito, descuento, "
                        + "porcentaje_utilidad, desc_n_1, desc_n_2, etiquetas, id_bodega_control "
                        + "from ingresos_productos_detalle order by id")) {
            int enBatch = 0;
            List<Integer> idsBatch = new ArrayList<>();
            while (rs.next()) {
                int id = rs.getInt("id");
                if (mapeo.existe("ingresos_productos_detalle", id)) {
                    rep.incrementar("detalles ya migrados (omitidos)");
                    continue;
                }
                ins.setInt(1, id);
                ins.setObject(2, rs.getObject("id_ingreso_cabecera"));
                ins.setObject(3, mapeo.remap("productos", (Integer) rs.getObject("id_producto")));
                ins.setObject(4, rs.getObject("cantidad"));
                ins.setObject(5, rs.getObject("iva"));
                ins.setObject(6, rs.getObject("precio_costo"));
                ins.setObject(7, rs.getObject("venta"));
                ins.setObject(8, rs.getObject("valor_desc_1"));
                ins.setObject(9, rs.getObject("valor_desc_2"));
                ins.setObject(10, rs.getObject("valor_s_y_t"));
                ins.setObject(11, rs.getObject("valor_credito"));
                ins.setObject(12, rs.getObject("descuento"));
                ins.setObject(13, rs.getObject("porcentaje_utilidad"));
                ins.setObject(14, rs.getObject("desc_n_1"));
                ins.setObject(15, rs.getObject("desc_n_2"));
                ins.setString(16, rs.getString("etiquetas"));
                ins.setObject(17, rs.getObject("id_bodega_control"));
                ins.addBatch();
                idsBatch.add(id);
                if (++enBatch >= 500) {
                    ins.executeBatch();
                    registrarBatchDetalle(idsBatch);
                    enBatch = 0;
                    idsBatch.clear();
                }
            }
            if (enBatch > 0) {
                ins.executeBatch();
                registrarBatchDetalle(idsBatch);
            }
        }
    }

    private void registrarBatchDetalle(List<Integer> ids) throws SQLException {
        try (PreparedStatement ps = destino.prepareStatement(
                "insert into migracion_mapeo (tabla_origen, id_viejo, id_nuevo, accion) values (?,?,?,?)")) {
            for (Integer id : ids) {
                ps.setString(1, "ingresos_productos_detalle");
                ps.setInt(2, id);
                ps.setInt(3, id);
                ps.setString(4, "creado");
                ps.addBatch();
                rep.incrementar("detalles de ingreso migrados");
            }
            ps.executeBatch();
        }
        // refresca cache para idempotencia dentro de la misma corrida
        for (Integer id : ids) {
            mapeo.putSoloCache("ingresos_productos_detalle", id, id);
        }
    }

    // =========================================================================
    // 7. Pagos a proveedores (hoy 0 filas en agro; queda por si entran antes
    //    del corte)
    // =========================================================================
    private void migrarPagos() throws SQLException {
        rep.linea("");
        rep.linea("7) Pagos de ingresos");

        try (Statement st = origen.createStatement();
                ResultSet rs = st.executeQuery(
                        "select id, id_ingresos_mercancias_cabecera, total, fecha, hora, cod_pago "
                        + "from pagos_ingresos order by id")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                if (mapeo.existe("pagos_ingresos_productos", id)) {
                    rep.incrementar("pagos ya migrados (omitidos)");
                    continue;
                }
                try (PreparedStatement ps = destino.prepareStatement(
                        "insert into pagos_ingresos_productos (id, id_ingreso_productos_cabecera, total, "
                        + "fecha, hora, cod_pago) values (?,?,?,?,?,?)")) {
                    ps.setInt(1, id);
                    ps.setObject(2, rs.getObject("id_ingresos_mercancias_cabecera"));
                    ps.setObject(3, rs.getObject("total"));
                    ps.setDate(4, rs.getDate("fecha"));
                    ps.setString(5, rs.getString("hora"));
                    ps.setString(6, rs.getString("cod_pago"));
                    ps.executeUpdate();
                }
                mapeo.put("pagos_ingresos_productos", id, id, "creado");
                rep.incrementar("pagos migrados");
            }
        }
    }

    // =========================================================================
    // 8. Descuentos escalonados + porcentajes de configuracion
    // =========================================================================
    private void migrarDescuentosYConfig() throws SQLException {
        rep.linea("");
        rep.linea("8) Descuentos escalonados y configuracion de precios");

        try (Statement st = origen.createStatement();
                ResultSet rs = st.executeQuery("select id, tipo, utilidad, descuento from descuentos order by id")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                if (mapeo.existe("descuentos", id)) {
                    rep.incrementar("descuentos ya migrados (omitidos)");
                    continue;
                }
                try (PreparedStatement ps = destino.prepareStatement(
                        "insert into descuentos (id, tipo, utilidad, descuento) values (?,?,?,?)")) {
                    ps.setInt(1, id);
                    ps.setObject(2, rs.getObject("tipo"));
                    ps.setObject(3, rs.getObject("utilidad"));
                    ps.setObject(4, rs.getObject("descuento"));
                    ps.executeUpdate();
                }
                mapeo.put("descuentos", id, id, "creado");
                rep.incrementar("descuentos migrados");
            }
        }

        try (Statement st = origen.createStatement();
                ResultSet rs = st.executeQuery(
                        "select porcentaje_operacion, porcentaje_s_y_t, porcentaje_credito "
                        + "from configuraciones where id = 1")) {
            if (rs.next()) {
                try (PreparedStatement ps = destino.prepareStatement(
                        "update configuraciones set porcentaje_operacion = ?, porcentaje_s_y_t = ?, "
                        + "porcentaje_credito = ? where id = 1")) {
                    ps.setObject(1, rs.getObject("porcentaje_operacion"));
                    ps.setObject(2, rs.getObject("porcentaje_s_y_t"));
                    ps.setObject(3, rs.getObject("porcentaje_credito"));
                    ps.executeUpdate();
                }
                rep.linea("  configuraciones: porcentaje_operacion=" + rs.getObject("porcentaje_operacion")
                        + ", porcentaje_s_y_t=" + rs.getObject("porcentaje_s_y_t")
                        + ", porcentaje_credito=" + rs.getObject("porcentaje_credito"));
            }
        }
    }

    // =========================================================================
    // 9. Secuencias (la app usa max(id)+1, pero es un seguro barato)
    // =========================================================================
    private void ajustarSecuencias() throws SQLException {
        rep.linea("");
        rep.linea("9) Ajuste de secuencias");
        String[] tablas = {"contactos", "productos", "users", "unidades_medidas", "descuentos",
            "ingresos_productos_cabecera", "ingresos_productos_detalle", "pagos_ingresos_productos"};
        for (String tabla : tablas) {
            try (Statement st = destino.createStatement();
                    ResultSet rs = st.executeQuery(
                            "select setval(pg_get_serial_sequence('" + tabla + "', 'id'), "
                            + "greatest((select coalesce(max(id),1) from " + tabla + "), 1))")) {
                if (rs.next()) {
                    rep.linea("  " + tabla + " -> setval " + rs.getLong(1));
                }
            } catch (SQLException ex) {
                rep.advertencia("No se pudo ajustar secuencia de " + tabla + ": " + ex.getMessage());
            }
        }
    }

    // =========================================================================
    // Validaciones origen vs destino
    // =========================================================================
    private void validar() throws SQLException {
        rep.linea("");
        rep.linea("10) VALIDACIONES origen vs destino");

        compararEscalar("cabeceras de ingreso",
                "select count(*) from ingresos_productos_cabecera",
                "select count(*) from ingresos_productos_cabecera");
        compararEscalar("detalles de ingreso",
                "select count(*) from ingresos_productos_detalle",
                "select count(*) from ingresos_productos_detalle");
        compararEscalar("pagos",
                "select count(*) from pagos_ingresos",
                "select count(*) from pagos_ingresos_productos");
        compararEscalar("descuentos",
                "select count(*) from descuentos",
                "select count(*) from descuentos");
        compararEscalar("suma cantidades del detalle",
                "select round(sum(cantidad)::numeric, 2) from ingresos_productos_detalle",
                "select round(sum(cantidad)::numeric, 2) from ingresos_productos_detalle");
        compararEscalar("suma cantidad*precio_costo del detalle",
                "select round(sum(cantidad*coalesce(precio_costo,0))::numeric, 1) from ingresos_productos_detalle",
                "select round(sum(cantidad*coalesce(precio_costo,0))::numeric, 1) from ingresos_productos_detalle");
        compararEscalar("cabeceras por estado",
                "select string_agg(estado || ':' || c, ', ') from (select estado, count(*) c "
                + "from ingresos_productos_cabecera group by estado order by estado) t",
                "select string_agg(estado || ':' || c, ', ') from (select estado, count(*) c "
                + "from ingresos_productos_cabecera group by estado order by estado) t");

        // huerfanos en destino
        verificarCero("detalles con id_producto no resoluble",
                "select count(*) from ingresos_productos_detalle d left join productos p on p.id = d.id_producto "
                + "where d.id_producto is not null and p.id is null");
        verificarCero("detalles sin cabecera",
                "select count(*) from ingresos_productos_detalle d "
                + "left join ingresos_productos_cabecera c on c.id = d.id_ingreso_cabecera "
                + "where d.id_ingreso_cabecera is not null and c.id is null");
        verificarCero("cabeceras con proveedor no resoluble",
                "select count(*) from ingresos_productos_cabecera i left join contactos c on c.id = i.id_proveedor "
                + "where i.id_proveedor is not null and c.id is null");
        verificarCero("cabeceras con usuario no resoluble",
                "select count(*) from ingresos_productos_cabecera i left join users u on u.id = i.id_user "
                + "where i.id_user is not null and u.id is null");

        spotCheck();
    }

    private void compararEscalar(String etiqueta, String sqlOrigen, String sqlDestino) throws SQLException {
        String vo = escalar(origen, sqlOrigen);
        String vd = escalar(destino, sqlDestino);
        boolean ok = (vo == null && vd == null) || (vo != null && vo.equals(vd));
        rep.linea(String.format("  %-45s origen=%s destino=%s %s",
                etiqueta, vo, vd, ok ? "OK" : "<<< DIFERENTE"));
        if (!ok) {
            rep.advertencia("Validacion DIFERENTE: " + etiqueta + " (origen=" + vo + ", destino=" + vd + ")");
        }
    }

    private void verificarCero(String etiqueta, String sqlDestino) throws SQLException {
        String v = escalar(destino, sqlDestino);
        boolean ok = "0".equals(v);
        rep.linea(String.format("  %-45s %s %s", etiqueta, v, ok ? "OK" : "<<< DEBE SER 0"));
        if (!ok) {
            rep.advertencia("Huerfanos detectados: " + etiqueta + " = " + v);
        }
    }

    /** Spot-check: 10 cabeceras repartidas, comparando conteo y sumas del detalle. */
    private void spotCheck() throws SQLException {
        rep.linea("  Spot-check de 10 ingresos (conteo y sumas del detalle):");
        List<Integer> ids = new ArrayList<>();
        try (Statement st = origen.createStatement();
                ResultSet rs = st.executeQuery("select id from ingresos_productos_cabecera order by id")) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        if (ids.isEmpty()) {
            rep.linea("    (sin cabeceras)");
            return;
        }
        int paso = Math.max(1, ids.size() / 10);
        for (int i = 0; i < ids.size() && i / paso < 10; i += paso) {
            int id = ids.get(i);
            String sql = "select count(*) || '|' || coalesce(round(sum(cantidad)::numeric,2),0) || '|' || "
                    + "coalesce(round(sum(cantidad*coalesce(precio_costo,0))::numeric,1),0) "
                    + "from ingresos_productos_detalle where id_ingreso_cabecera = " + id;
            String vo = escalar(origen, sql);
            String vd = escalar(destino, sql);
            boolean ok = vo != null && vo.equals(vd);
            rep.linea("    ingreso #" + id + ": origen=" + vo + " destino=" + vd + (ok ? " OK" : " <<< DIFERENTE"));
            if (!ok) {
                rep.advertencia("Spot-check DIFERENTE en ingreso #" + id);
            }
        }
    }

    private String escalar(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return null;
    }

    private int maxId(String tabla) throws SQLException {
        try (Statement st = destino.createStatement();
                ResultSet rs = st.executeQuery("select coalesce(max(id), 0) from " + tabla)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static String trunc(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
