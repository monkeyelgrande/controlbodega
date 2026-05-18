package com.bodega.api.ordenes.dto;

/** Una linea de la orden con lo que falta por entregar. */
public class ProductoPendiente {

    public int idProducto;
    public String codigo;
    public String descripcion;
    public double pedido;
    public double entregado;
    public double pendiente;
    public double stockBodega;

    public int getIdProducto() { return idProducto; }
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public double getPedido() { return pedido; }
    public double getEntregado() { return entregado; }
    public double getPendiente() { return pendiente; }
    public double getStockBodega() { return stockBodega; }
}
