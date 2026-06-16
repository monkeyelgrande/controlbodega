package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Fusión (unión) de órdenes de entrega.
 *
 * Un mismo cliente puede generar varias órdenes para una misma compra (una
 * desde World Office y otra desde el módulo de ventas interno). Esta clase
 * permite unirlas: sobrevive la orden más antigua (id menor) y las demás se
 * anulan moviendo sus líneas de facturas_detalles a la sobreviviente.
 *
 * El stock NO se toca: las líneas conservan su id_factura de referencia
 * (0 = orden WO/normal, >0 = venta origen), por lo que lo pendiente sigue
 * pendiente bajo la orden sobreviviente y una anulación posterior reversa
 * cada línea correctamente. Solo se insertan movimientos informativos
 * FUSION_ORDEN (afecta 0/0) para auditoría.
 *
 * Las órdenes absorbidas quedan con impreso_auto = true además de anulado = 0:
 * el catch-up de AutoImpresionOrdenesService (procesarPendientes) no filtra
 * por anulado y las reimprimiría si quedaran como no impresas.
 */
public class DBfusion_ordenes {

    /** Datos de una orden candidata para el diálogo de confirmación. */
    public static class OrdenResumen {

        public int id;
        public String codigo;
        public String cliente;
        public int idBodega;
        public String bodega;
        public int lineas;
    }

    private DBfusion_ordenes() {
    }

    private static String placeholders(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("?");
        }
        return sb.toString();
    }

    /**
     * Pre-chequeo (fuera de transacción) para la UI. Reglas: mínimo 2 órdenes,
     * todas existentes, vigentes (anulado=1), tipo distinto de 'Venta', misma
     * bodega (asignada), ninguna impresa (auto ni vendedor) y ninguna con
     * entregas registradas. El cliente puede variar.
     *
     * @param ids ids de las órdenes seleccionadas
     * @param resumen lista de salida; queda ordenada por id ascendente (la
     * primera es la sobreviviente)
     * @return null si todo OK; mensaje de error si alguna regla falla
     */
    public static String validarUnion(List<Integer> ids, List<OrdenResumen> resumen) {
        if (ids == null || ids.size() < 2) {
            return "Seleccione al menos 2 órdenes para unir.";
        }

        String sql = "SELECT fc.id, COALESCE(fc.codigo, '') AS codigo, fc.anulado, fc.tipo_factura, "
                + "COALESCE(fc.id_bodega, 0) AS id_bodega, "
                + "COALESCE(fc.impreso_auto, false) AS impreso_auto, "
                + "COALESCE(fc.impreso_vendedor, 0) AS impreso_vendedor, "
                + "c.nombre AS cliente, COALESCE(b.nombre, 'SIN BODEGA') AS bodega, "
                + "(SELECT COUNT(*) FROM facturas_detalles fd WHERE fd.id_cabecera = fc.id) AS lineas, "
                + "(SELECT COUNT(*) FROM entregas_productos_cabecera ep WHERE ep.id_factura = fc.id) AS entregas "
                + "FROM facturas_cabeceras fc "
                + "JOIN contactos c ON c.id = fc.id_contacto "
                + "LEFT JOIN bodegas b ON b.id = fc.id_bodega "
                + "WHERE fc.id IN (" + placeholders(ids.size()) + ") "
                + "ORDER BY fc.id";

        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 0; i < ids.size(); i++) {
                ps.setInt(i + 1, ids.get(i));
            }

            int encontradas = 0;
            int bodegaUnica = -1;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    encontradas++;
                    int id = rs.getInt("id");

                    if (rs.getInt("anulado") != 1) {
                        return "La orden #" + id + " ya está anulada.";
                    }
                    if ("Venta".equals(rs.getString("tipo_factura"))) {
                        return "La orden #" + id + " es una venta, no una orden de entrega.";
                    }
                    if (rs.getBoolean("impreso_auto") || rs.getInt("impreso_vendedor") != 0) {
                        return "La orden #" + id + " ya fue impresa.\nSolo se pueden unir órdenes sin imprimir.";
                    }
                    if (rs.getInt("entregas") > 0) {
                        return "La orden #" + id + " ya tiene entregas registradas.";
                    }
                    int idBodega = rs.getInt("id_bodega");
                    if (idBodega == 0) {
                        return "La orden #" + id + " no tiene bodega asignada.";
                    }
                    if (bodegaUnica == -1) {
                        bodegaUnica = idBodega;
                    } else if (idBodega != bodegaUnica) {
                        return "Las órdenes seleccionadas no son de la misma bodega.";
                    }

                    OrdenResumen o = new OrdenResumen();
                    o.id = id;
                    o.codigo = rs.getString("codigo").trim();
                    o.cliente = rs.getString("cliente");
                    o.idBodega = idBodega;
                    o.bodega = rs.getString("bodega");
                    o.lineas = rs.getInt("lineas");
                    resumen.add(o);
                }
            }
            if (encontradas != ids.size()) {
                return "No se encontraron todas las órdenes seleccionadas.\nActualice el listado e intente de nuevo.";
            }
        } catch (SQLException e) {
            return "Error consultando las órdenes:\n" + e.getMessage();
        }
        return null;
    }

    /**
     * Ejecuta la fusión en UNA transacción. Re-valida con FOR UPDATE (bloquea
     * la carrera con marcarImpreso del servicio de auto-impresión: si alguien
     * imprimió una orden entre el pre-chequeo y este punto, se rechaza).
     *
     * @param ids ids de las órdenes a unir
     * @param idUser usuario que ejecuta la fusión
     * @return id de la orden sobreviviente (la de id menor)
     * @throws SQLException si falla una validación o la BD; garantiza rollback
     */
    public static int unirOrdenes(List<Integer> ids, int idUser) throws SQLException {
        if (ids == null || ids.size() < 2) {
            throw new SQLException("Seleccione al menos 2 órdenes para unir.");
        }

        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            // 1) Lock + re-validación (sin joins para poder usar FOR UPDATE).
            String sqlLock = "SELECT id, COALESCE(codigo, '') AS codigo, anulado, tipo_factura, "
                    + "COALESCE(id_bodega, 0) AS id_bodega, "
                    + "COALESCE(impreso_auto, false) AS impreso_auto, "
                    + "COALESCE(impreso_vendedor, 0) AS impreso_vendedor "
                    + "FROM facturas_cabeceras "
                    + "WHERE id IN (" + placeholders(ids.size()) + ") "
                    + "ORDER BY id "
                    + "FOR UPDATE";

            int idSobrev = -1;
            int idBodega = 0;
            int encontradas = 0;
            java.util.List<Integer> absorbidas = new java.util.ArrayList<>();
            StringBuilder codigosAbsorbidas = new StringBuilder();

            try (PreparedStatement ps = con.prepareStatement(sqlLock)) {
                for (int i = 0; i < ids.size(); i++) {
                    ps.setInt(i + 1, ids.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        encontradas++;
                        int id = rs.getInt("id");

                        if (rs.getInt("anulado") != 1) {
                            throw new SQLException("La orden #" + id + " ya está anulada.");
                        }
                        if ("Venta".equals(rs.getString("tipo_factura"))) {
                            throw new SQLException("La orden #" + id + " es una venta, no una orden de entrega.");
                        }
                        if (rs.getBoolean("impreso_auto") || rs.getInt("impreso_vendedor") != 0) {
                            throw new SQLException("La orden #" + id + " ya fue impresa. Solo se pueden unir órdenes sin imprimir.");
                        }
                        if (rs.getInt("id_bodega") == 0) {
                            throw new SQLException("La orden #" + id + " no tiene bodega asignada.");
                        }

                        if (idSobrev == -1) {
                            // Primera fila (ORDER BY id) = la más antigua: sobrevive.
                            idSobrev = id;
                            idBodega = rs.getInt("id_bodega");
                        } else {
                            if (rs.getInt("id_bodega") != idBodega) {
                                throw new SQLException("Las órdenes seleccionadas no son de la misma bodega.");
                            }
                            absorbidas.add(id);
                            if (codigosAbsorbidas.length() > 0) {
                                codigosAbsorbidas.append(", ");
                            }
                            String cod = rs.getString("codigo").trim();
                            codigosAbsorbidas.append("#").append(id);
                            if (!cod.isEmpty()) {
                                codigosAbsorbidas.append(" (").append(cod).append(")");
                            }
                        }
                    }
                }
            }
            if (encontradas != ids.size()) {
                throw new SQLException("No se encontraron todas las órdenes seleccionadas. Actualice el listado e intente de nuevo.");
            }

            // Entregas registradas (incluida la sobreviviente).
            String sqlEntregas = "SELECT id_factura FROM entregas_productos_cabecera "
                    + "WHERE id_factura IN (" + placeholders(ids.size()) + ") "
                    + "GROUP BY id_factura";
            try (PreparedStatement ps = con.prepareStatement(sqlEntregas)) {
                for (int i = 0; i < ids.size(); i++) {
                    ps.setInt(i + 1, ids.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        throw new SQLException("La orden #" + rs.getInt("id_factura") + " ya tiene entregas registradas.");
                    }
                }
            }

            String inAbsorbidas = placeholders(absorbidas.size());
            String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());

            // 2) Auditoría: un movimiento FUSION_ORDEN (neto cero) por línea movida.
            String sqlMov = "INSERT INTO movimientos_inventario ("
                    + "id_producto, id_bodega, id_user, tipo, afecta_cantidad, afecta_pendientes, valor, "
                    + "id_referencia, tabla_referencia, fecha, hora, observacion) "
                    + "SELECT fd.id_producto, ?, ?, 'FUSION_ORDEN', 0, 0, fd.cantidad, "
                    + "?, 'facturas_cabeceras', ('now'::text)::date, ?, "
                    + "'Fusion: linea movida de orden #' || fd.id_cabecera || ' a orden #' || ? "
                    + "FROM facturas_detalles fd "
                    + "WHERE fd.id_cabecera IN (" + inAbsorbidas + ")";
            try (PreparedStatement ps = con.prepareStatement(sqlMov)) {
                int idx = 1;
                ps.setInt(idx++, idBodega);
                ps.setInt(idx++, idUser);
                ps.setInt(idx++, idSobrev);
                ps.setString(idx++, hora);
                ps.setString(idx++, String.valueOf(idSobrev));
                for (int id : absorbidas) {
                    ps.setInt(idx++, id);
                }
                ps.executeUpdate();
            }

            // 3) Mover las líneas (conservan su id_factura de referencia).
            String sqlMover = "UPDATE facturas_detalles SET id_cabecera = ? "
                    + "WHERE id_cabecera IN (" + inAbsorbidas + ")";
            try (PreparedStatement ps = con.prepareStatement(sqlMover)) {
                int idx = 1;
                ps.setInt(idx++, idSobrev);
                for (int id : absorbidas) {
                    ps.setInt(idx++, id);
                }
                ps.executeUpdate();
            }

            // 4) Anular absorbidas. impreso_auto = true las blinda contra el
            //    catch-up del servicio de auto-impresión (no filtra anulado).
            String sqlAnular = "UPDATE facturas_cabeceras "
                    + "SET anulado = 0, impreso_auto = true, "
                    + "observacion = COALESCE(observacion, '') || ? "
                    + "WHERE id IN (" + inAbsorbidas + ")";
            try (PreparedStatement ps = con.prepareStatement(sqlAnular)) {
                int idx = 1;
                ps.setString(idx++, " | Fusionada en orden #" + idSobrev);
                for (int id : absorbidas) {
                    ps.setInt(idx++, id);
                }
                ps.executeUpdate();
            }

            // 5) Rastro en la sobreviviente.
            String sqlSobrev = "UPDATE facturas_cabeceras "
                    + "SET observacion = COALESCE(observacion, '') || ? "
                    + "WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlSobrev)) {
                ps.setString(1, " | Incluye ordenes fusionadas: " + codigosAbsorbidas);
                ps.setInt(2, idSobrev);
                ps.executeUpdate();
            }

            con.commit();
            return idSobrev;

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback de fusión: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException ex) {
                    // ignorar: la conexión se cierra a continuación
                }
                try {
                    con.close();
                } catch (SQLException ex) {
                    // ignorar
                }
            }
        }
    }
}
