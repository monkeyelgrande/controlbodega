package Metodos;

import conexiondb.DBfacturas_cabeceras;
import conexiondb.DBstock_productos;
import conexiondb.DB_consultas_R_D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import modelos.Facturas_cabeceras;

/**
 * Genera órdenes de entrega tipo "Salida" automáticamente a partir de una
 * Venta, agrupando los productos por bodega de descarga y creando una Salida
 * por cada bodega marcada con genera_orden_automatica = TRUE.
 *
 * Para cada Salida generada se registra una ORDEN_REFERENCIADA en
 * movimientos_inventario, que revierte el descuento de la VENTA original
 * (cantidad +1) y deja el producto en pendientes (+1). Cuando luego se
 * registre la ENTREGA física desde frm_EntregasQR, se descontará una sola
 * vez. No hay doble descuento.
 *
 * El trigger fn_notify_orden_nueva en facturas_cabeceras dispara LISTEN/NOTIFY
 * por cada Salida insertada, lo que activa la impresión automática vía
 * AutoImpresionOrdenesService.
 */
public class GeneradorOrdenAuto {

    /**
     * Producto facturado en una venta, con la bodega de la que se descargó.
     */
    public static class ItemFacturado {

        public final int idProducto;
        public final int idBodegaDescarga;
        public final double cantidad;
        public final double subtotal;

        public ItemFacturado(int idProducto, int idBodegaDescarga, double cantidad, double subtotal) {
            this.idProducto = idProducto;
            this.idBodegaDescarga = idBodegaDescarga;
            this.cantidad = cantidad;
            this.subtotal = subtotal;
        }
    }

    /**
     * Crea las Salidas necesarias para los items que provienen de bodegas
     * marcadas como generadoras de orden automática. Los items de bodegas sin
     * el flag se ignoran (su descuento inmediato vía VENTA ya quedó hecho).
     *
     * @return Lista de ids de Salida creadas. Vacía si ningún item proviene
     * de una bodega marcada.
     */
    public static List<Integer> generarSalidasDesdeVenta(
            int idVenta,
            int idUser,
            int idContacto,
            String fechaIso,
            String hora,
            String codigo,
            int tipoPago,
            String observacionVenta,
            List<ItemFacturado> items) {

        List<Integer> salidasCreadas = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return salidasCreadas;
        }

        Map<Integer, Boolean> cacheBodegaFlag = cargarFlags();

        // Agrupar items por bodega marcada.
        Map<Integer, List<ItemFacturado>> porBodega = new HashMap<>();
        for (ItemFacturado it : items) {
            Boolean flag = cacheBodegaFlag.get(it.idBodegaDescarga);
            if (flag != null && flag) {
                porBodega.computeIfAbsent(it.idBodegaDescarga, k -> new ArrayList<>()).add(it);
            }
        }

        if (porBodega.isEmpty()) {
            return salidasCreadas;
        }

        DBstock_productos dbStock = new DBstock_productos();
        DBfacturas_cabeceras dbCab = new DBfacturas_cabeceras();

        for (Map.Entry<Integer, List<ItemFacturado>> entry : porBodega.entrySet()) {
            int idBodega = entry.getKey();
            List<ItemFacturado> grupo = entry.getValue();

            int idSalida;
            try {
                idSalida = Integer.parseInt(DB_consultas_R_D.cargarId("facturas_cabeceras"));
            } catch (NumberFormatException ex) {
                System.err.println("[GeneradorOrdenAuto] No se pudo cargar id para Salida: " + ex.getMessage());
                continue;
            }

            Facturas_cabeceras fc = new Facturas_cabeceras();
            fc.setId(idSalida);
            fc.setId_cliente(idContacto);
            fc.setId_user(idUser);
            fc.setFecha(fechaIso);
            fc.setHora(hora);
            fc.setTipo("Salida");
            fc.setCodigo(codigo == null ? "" : codigo);
            String obs = "Orden auto desde Venta " + idVenta;
            if (observacionVenta != null && !observacionVenta.trim().isEmpty()) {
                obs += " | " + observacionVenta.trim();
            }
            fc.setObservacion(obs);
            fc.setObservacion_entrega("");
            fc.setAnulado(1);
            fc.setTipo_pago(tipoPago);
            fc.setId_bodega(idBodega);

            if (dbCab.Guardar(fc) != 1) {
                System.err.println("[GeneradorOrdenAuto] Falló guardar cabecera Salida para bodega " + idBodega);
                continue;
            }

            if (!insertarDetallesYMovimientos(idSalida, idVenta, idBodega, idUser, grupo, dbStock)) {
                System.err.println("[GeneradorOrdenAuto] Falló insertar detalles de Salida " + idSalida
                        + ". La cabecera quedó creada pero sin detalles — revisar manualmente.");
                continue;
            }

            salidasCreadas.add(idSalida);
            System.out.println("[GeneradorOrdenAuto] Salida " + idSalida + " creada para bodega "
                    + idBodega + " con " + grupo.size() + " producto(s) desde Venta " + idVenta);
        }

        return salidasCreadas;
    }

    private static Map<Integer, Boolean> cargarFlags() {
        Map<Integer, Boolean> result = new HashMap<>();
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id, genera_orden_automatica FROM bodegas");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("id"), rs.getBoolean("genera_orden_automatica"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[GeneradorOrdenAuto] Error cargando flags de bodegas: " + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignored) {
                }
            }
        }
        return result;
    }

    private static boolean insertarDetallesYMovimientos(
            int idSalida, int idVenta, int idBodega, int idUser,
            List<ItemFacturado> items, DBstock_productos dbStock) {

        StringBuilder sql = new StringBuilder();
        for (ItemFacturado it : items) {
            // Mismo patrón que frm_Crear_Orden línea 1236: id_factura apunta a la Venta origen.
            sql.append("INSERT INTO facturas_detalles (id, id_cabecera, id_producto, cantidad, subtotal, id_factura) ")
                    .append("VALUES ((SELECT COALESCE(MAX(id),0)+1 FROM facturas_detalles),")
                    .append(idSalida).append(",")
                    .append(it.idProducto).append(",")
                    .append(it.cantidad).append(",")
                    .append(it.subtotal).append(",")
                    .append(idVenta).append(");\n");
        }

        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[GeneradorOrdenAuto] Error insertando detalles: " + e.getMessage());
            return false;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignored) {
                }
            }
        }

        // Movimientos: ORDEN_REFERENCIADA por cada item — revierte VENTA y reserva pendiente.
        for (ItemFacturado it : items) {
            dbStock.ordenReferenciada(
                    it.idProducto,
                    idBodega,
                    idUser,
                    it.cantidad,
                    idSalida,
                    "Orden auto bodega " + idBodega + " - Venta " + idVenta
            );
        }
        return true;
    }
}
