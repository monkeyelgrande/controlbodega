package com.bodega.api.entregas.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Cuerpo del POST /api/entregas.
 *
 * idFactura     : id de la orden (alternativa: enviar 'qr' = "ORDEN-<id>")
 * idEscaneo     : id que devolvio GET /api/ordenes (para enlazar la traza)
 * accion        : ENTREGA_COMPLETA | ENTREGA_PARCIAL
 * items         : productos y cantidades; si va vacio y accion=COMPLETA, el
 *                 servidor entrega todo lo pendiente.
 */
public class EntregaRequest {

    private Integer idFactura;
    private String qr;
    private int idEscaneo;
    private String accion;
    private List<ItemEntrega> items = new ArrayList<>();

    public Integer getIdFactura() { return idFactura; }
    public void setIdFactura(Integer idFactura) { this.idFactura = idFactura; }

    public String getQr() { return qr; }
    public void setQr(String qr) { this.qr = qr; }

    public int getIdEscaneo() { return idEscaneo; }
    public void setIdEscaneo(int idEscaneo) { this.idEscaneo = idEscaneo; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public List<ItemEntrega> getItems() { return items; }
    public void setItems(List<ItemEntrega> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }
}
