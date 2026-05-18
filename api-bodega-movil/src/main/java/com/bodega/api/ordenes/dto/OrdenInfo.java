package com.bodega.api.ordenes.dto;

import java.util.ArrayList;
import java.util.List;

/** Cabecera de la orden + sus productos pendientes. */
public class OrdenInfo {

    public int idFactura;
    public String codigoFactura;
    public int idCliente;
    public String nombreCliente;
    public String cedulaCliente;
    public String fecha;
    public String tipoFactura;
    public int idBodegaOrden;
    public String nombreBodegaOrden;
    public String observacion;
    public boolean anulada;
    public List<ProductoPendiente> productos = new ArrayList<>();

    public double getTotalPendiente() {
        double t = 0.0;
        for (ProductoPendiente p : productos) {
            t += p.pendiente;
        }
        return t;
    }

    public int getIdFactura() { return idFactura; }
    public String getCodigoFactura() { return codigoFactura; }
    public int getIdCliente() { return idCliente; }
    public String getNombreCliente() { return nombreCliente; }
    public String getCedulaCliente() { return cedulaCliente; }
    public String getFecha() { return fecha; }
    public String getTipoFactura() { return tipoFactura; }
    public int getIdBodegaOrden() { return idBodegaOrden; }
    public String getNombreBodegaOrden() { return nombreBodegaOrden; }
    public String getObservacion() { return observacion; }
    public boolean isAnulada() { return anulada; }
    public List<ProductoPendiente> getProductos() { return productos; }
}
