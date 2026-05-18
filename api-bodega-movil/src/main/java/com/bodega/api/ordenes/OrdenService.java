package com.bodega.api.ordenes;

import com.bodega.api.entregas.StockService;
import com.bodega.api.ordenes.dto.OrdenInfo;
import com.bodega.api.ordenes.dto.ProductoPendiente;
import com.bodega.api.ordenes.dto.ResultadoEscaneo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logica de "Entregas Rapidas" portada de {@code EntregaQRService} (escritorio).
 *
 * El QR esperado es {@code ORDEN-<id>} (el mismo que genera la app de
 * escritorio al imprimir la factura/orden).
 *
 * Validaciones, en orden (igual que el escritorio):
 *   QR_INVALIDO -> ORDEN_NO_EXISTE -> ANULADA -> OTRA_BODEGA ->
 *   YA_ENTREGADA -> OK
 */
@Service
public class OrdenService {

    public static final String OK = "OK";
    public static final String QR_INVALIDO = "QR_INVALIDO";
    public static final String ORDEN_NO_EXISTE = "ORDEN_NO_EXISTE";
    public static final String OTRA_BODEGA = "OTRA_BODEGA";
    public static final String ANULADA = "ANULADA";
    public static final String YA_ENTREGADA = "YA_ENTREGADA";

    public static final String ACCION_NINGUNA = "NINGUNA";

    private static final Pattern QR_PATTERN = Pattern.compile("^ORDEN-(\\d+)$");

    private final JdbcTemplate jdbc;
    private final StockService stockService;
    private final EscaneoService escaneoService;

    public OrdenService(JdbcTemplate jdbc, StockService stockService,
                        EscaneoService escaneoService) {
        this.jdbc = jdbc;
        this.stockService = stockService;
        this.escaneoService = escaneoService;
    }

    public ResultadoEscaneo procesarLectura(String qrCrudo, int idUser,
                                            int idBodegaUsuario, String pcOrigen) {
        ResultadoEscaneo r = new ResultadoEscaneo();
        String texto = qrCrudo == null ? "" : qrCrudo.trim();

        Integer idFactura = parsearIdOrden(texto);
        if (idFactura == null) {
            r.resultado = QR_INVALIDO;
            r.mensaje = "QR no reconocido. Esperado formato 'ORDEN-<id>'.";
            r.idEscaneo = escaneoService.registrar(null, idUser, idBodegaUsuario,
                    texto, r.resultado, ACCION_NINGUNA, null, pcOrigen);
            return r;
        }

        OrdenInfo info = cargarOrden(idFactura);
        if (info == null) {
            r.resultado = ORDEN_NO_EXISTE;
            r.mensaje = "La orden " + idFactura + " no existe.";
            r.idEscaneo = escaneoService.registrar(idFactura, idUser,
                    idBodegaUsuario, texto, r.resultado, ACCION_NINGUNA, null, pcOrigen);
            return r;
        }

        if (info.anulada) {
            r.resultado = ANULADA;
            r.orden = info;
            r.mensaje = "La orden " + info.codigoFactura + " esta anulada.";
            r.idEscaneo = escaneoService.registrar(idFactura, idUser,
                    idBodegaUsuario, texto, r.resultado, ACCION_NINGUNA, null, pcOrigen);
            return r;
        }

        if (info.idBodegaOrden != idBodegaUsuario) {
            r.resultado = OTRA_BODEGA;
            r.orden = info;
            r.mensaje = "La orden pertenece a la bodega '" + info.nombreBodegaOrden
                    + "'. Tu sesion esta en otra bodega.";
            r.idEscaneo = escaneoService.registrar(idFactura, idUser,
                    idBodegaUsuario, texto, r.resultado, ACCION_NINGUNA, null, pcOrigen);
            return r;
        }

        if (info.getTotalPendiente() <= 0) {
            r.resultado = YA_ENTREGADA;
            r.orden = info;
            r.mensaje = "La orden ya fue entregada por completo.";
            r.idEscaneo = escaneoService.registrar(idFactura, idUser,
                    idBodegaUsuario, texto, r.resultado, ACCION_NINGUNA, null, pcOrigen);
            return r;
        }

        r.resultado = OK;
        r.orden = info;
        r.mensaje = "Orden lista para entregar.";
        r.idEscaneo = escaneoService.registrar(idFactura, idUser,
                idBodegaUsuario, texto, r.resultado, ACCION_NINGUNA, null, pcOrigen);
        return r;
    }

    public OrdenInfo cargarOrden(int idFactura) {
        String sqlCab =
                "SELECT fc.id, fc.codigo, fc.fecha, fc.tipo_factura, fc.anulado, "
                + "       fc.observacion, fc.id_bodega, b.nombre AS bodega, "
                + "       c.id AS id_cliente, c.nombre AS cliente, c.cedula AS cedula "
                + "FROM facturas_cabeceras fc "
                + "LEFT JOIN bodegas b ON b.id = fc.id_bodega "
                + "LEFT JOIN contactos c ON c.id = fc.id_contacto "
                + "WHERE fc.id = ?";

        List<OrdenInfo> cab = jdbc.query(sqlCab, new Object[]{idFactura}, (rs, n) -> {
            OrdenInfo o = new OrdenInfo();
            o.idFactura = rs.getInt("id");
            o.codigoFactura = rs.getString("codigo");
            o.fecha = rs.getString("fecha");
            o.tipoFactura = rs.getString("tipo_factura");
            int anulado = rs.getInt("anulado");
            // En el codigo existente anulado = 1 significa "activa".
            o.anulada = (anulado != 1);
            o.idBodegaOrden = rs.getInt("id_bodega");
            o.nombreBodegaOrden = rs.getString("bodega");
            o.idCliente = rs.getInt("id_cliente");
            o.nombreCliente = rs.getString("cliente");
            o.cedulaCliente = rs.getString("cedula");
            o.observacion = rs.getString("observacion");
            return o;
        });

        if (cab.isEmpty()) {
            return null;
        }
        OrdenInfo info = cab.get(0);

        String sqlDet =
                "WITH entregas AS ("
                + "  SELECT id_producto, SUM(cantidad) AS total_entregado "
                + "  FROM entregas_productos WHERE id_factura = ? "
                + "  GROUP BY id_producto "
                + ") "
                + "SELECT fd.id_producto, p.codigo_barras AS codigo, p.descripcion, "
                + "       SUM(fd.cantidad) AS pedido, "
                + "       COALESCE(MAX(e.total_entregado), 0) AS entregado "
                + "FROM facturas_detalles fd "
                + "JOIN productos p ON p.id = fd.id_producto "
                + "LEFT JOIN entregas e ON e.id_producto = fd.id_producto "
                + "WHERE fd.id_cabecera = ? "
                + "GROUP BY fd.id_producto, p.codigo_barras, p.descripcion "
                + "ORDER BY p.descripcion";

        List<ProductoPendiente> prods = jdbc.query(
                sqlDet, new Object[]{idFactura, idFactura}, (rs, n) -> {
                    ProductoPendiente pp = new ProductoPendiente();
                    pp.idProducto = rs.getInt("id_producto");
                    pp.codigo = rs.getString("codigo");
                    pp.descripcion = rs.getString("descripcion");
                    pp.pedido = rs.getDouble("pedido");
                    pp.entregado = rs.getDouble("entregado");
                    pp.pendiente = Math.max(0.0, pp.pedido - pp.entregado);
                    return pp;
                });

        for (ProductoPendiente pp : prods) {
            try {
                pp.stockBodega = stockService.cantidadEnBodega(
                        pp.idProducto, info.idBodegaOrden);
            } catch (Exception ignore) {
                pp.stockBodega = 0.0;
            }
        }
        info.productos = prods;
        return info;
    }

    private static Integer parsearIdOrden(String texto) {
        if (texto == null) {
            return null;
        }
        Matcher m = QR_PATTERN.matcher(texto);
        if (!m.matches()) {
            return null;
        }
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
