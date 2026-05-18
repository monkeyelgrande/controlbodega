package com.bodega.api.entregas;

import com.bodega.api.entregas.dto.EntregaRequest;
import com.bodega.api.entregas.dto.EntregaResponse;
import com.bodega.api.entregas.dto.ItemEntrega;
import com.bodega.api.ordenes.EscaneoService;
import com.bodega.api.ordenes.OrdenService;
import com.bodega.api.ordenes.dto.OrdenInfo;
import com.bodega.api.ordenes.dto.ProductoPendiente;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orquesta la entrega, portado de {@code EntregaQRService.ejecutarEntrega}.
 *
 * Orden de operaciones (igual que el escritorio):
 *   1) cabecera + detalles en su transaccion (commit)
 *   2) un movimiento de stock por producto, cada uno en su transaccion
 *   3) marcar el escaneo con la accion realizada
 *
 * La orden se RECARGA en el servidor: no se confia en lo que mande el
 * telefono (bodega, pendientes y existencia se validan aqui).
 */
@Service
public class EntregaService {

    public static final String ENTREGA_COMPLETA = "ENTREGA_COMPLETA";
    public static final String ENTREGA_PARCIAL = "ENTREGA_PARCIAL";

    private static final Pattern QR_PATTERN = Pattern.compile("^ORDEN-(\\d+)$");

    private final OrdenService ordenService;
    private final EntregaDao entregaDao;
    private final StockService stockService;
    private final EscaneoService escaneoService;

    public EntregaService(OrdenService ordenService, EntregaDao entregaDao,
                          StockService stockService, EscaneoService escaneoService) {
        this.ordenService = ordenService;
        this.entregaDao = entregaDao;
        this.stockService = stockService;
        this.escaneoService = escaneoService;
    }

    public EntregaResponse ejecutar(EntregaRequest req, int idUser, int idBodegaSesion) {

        String accion = req.getAccion();
        if (!ENTREGA_COMPLETA.equals(accion) && !ENTREGA_PARCIAL.equals(accion)) {
            throw new EntregaInvalidaException(
                    "accion debe ser ENTREGA_COMPLETA o ENTREGA_PARCIAL.");
        }

        int idFactura = resolverIdFactura(req);
        OrdenInfo orden = ordenService.cargarOrden(idFactura);
        if (orden == null) {
            throw new EntregaInvalidaException("La orden " + idFactura + " no existe.");
        }
        if (orden.anulada) {
            throw new EntregaInvalidaException(
                    "La orden " + orden.codigoFactura + " esta anulada.");
        }
        if (orden.idBodegaOrden != idBodegaSesion) {
            throw new EntregaInvalidaException(
                    "La orden pertenece a la bodega '" + orden.nombreBodegaOrden
                    + "'. Tu sesion esta en otra bodega.");
        }

        Map<Integer, ProductoPendiente> pendientesPorProd = new LinkedHashMap<>();
        for (ProductoPendiente p : orden.productos) {
            pendientesPorProd.put(p.idProducto, p);
        }

        List<ItemEntrega> efectivos = construirItems(req, accion, pendientesPorProd);
        if (efectivos.isEmpty()) {
            throw new EntregaInvalidaException(
                    "No hay cantidades para entregar.");
        }

        // 1) cabecera + detalles (transaccion propia, ya confirmada al volver)
        int idCab = entregaDao.crearCabeceraConDetalles(
                idFactura, idUser, idBodegaSesion, efectivos);

        // 2) movimiento de stock por producto (cada uno su transaccion)
        String obs = (ENTREGA_COMPLETA.equals(accion)
                ? "Entrega completa QR - Orden #" : "Entrega parcial QR - Orden #")
                + idFactura;

        EntregaResponse resp = new EntregaResponse();
        for (ItemEntrega it : efectivos) {
            stockService.entrega(it.getIdProducto(), idBodegaSesion, idUser,
                    it.getCantidad(), idCab, obs);
            ProductoPendiente p = pendientesPorProd.get(it.getIdProducto());
            String desc = p != null ? p.descripcion : ("producto " + it.getIdProducto());
            resp.getDetalle().add(desc + " x " + it.getCantidad());
        }

        // 3) marcar el escaneo con la accion realizada
        if (req.getIdEscaneo() > 0) {
            escaneoService.actualizarAccion(req.getIdEscaneo(), accion, idCab);
        }

        resp.setOk(true);
        resp.setIdCabecera(idCab);
        resp.setAccion(accion);
        resp.setMensaje("Entrega registrada (cabecera #" + idCab + ").");
        return resp;
    }

    private int resolverIdFactura(EntregaRequest req) {
        if (req.getIdFactura() != null) {
            return req.getIdFactura();
        }
        if (req.getQr() != null) {
            Matcher m = QR_PATTERN.matcher(req.getQr().trim());
            if (m.matches()) {
                return Integer.parseInt(m.group(1));
            }
        }
        throw new EntregaInvalidaException(
                "Debe enviar idFactura o un qr valido (ORDEN-<id>).");
    }

    private List<ItemEntrega> construirItems(EntregaRequest req, String accion,
                                             Map<Integer, ProductoPendiente> pend) {
        List<ItemEntrega> efectivos = new ArrayList<>();

        boolean sinItems = req.getItems() == null || req.getItems().isEmpty();

        if (sinItems && ENTREGA_COMPLETA.equals(accion)) {
            for (ProductoPendiente p : pend.values()) {
                if (p.pendiente > 0) {
                    ItemEntrega it = new ItemEntrega();
                    it.setIdProducto(p.idProducto);
                    it.setCantidad(p.pendiente);
                    efectivos.add(it);
                }
            }
            return efectivos;
        }

        if (sinItems) {
            throw new EntregaInvalidaException(
                    "ENTREGA_PARCIAL requiere la lista de items.");
        }

        for (ItemEntrega it : req.getItems()) {
            if (it.getCantidad() <= 0) {
                continue;
            }
            ProductoPendiente p = pend.get(it.getIdProducto());
            if (p == null) {
                throw new EntregaInvalidaException(
                        "El producto " + it.getIdProducto()
                        + " no pertenece a esta orden.");
            }
            if (it.getCantidad() > p.pendiente + 1e-9) {
                throw new EntregaInvalidaException(
                        "Cantidad (" + it.getCantidad() + ") mayor que lo pendiente ("
                        + p.pendiente + ") en '" + p.descripcion + "'.");
            }
            efectivos.add(it);
        }
        return efectivos;
    }
}
