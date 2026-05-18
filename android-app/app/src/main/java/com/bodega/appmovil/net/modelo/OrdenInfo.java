package com.bodega.appmovil.net.modelo;

import java.util.ArrayList;
import java.util.List;

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
    public double totalPendiente;
    public List<ProductoPendiente> productos = new ArrayList<>();
}
