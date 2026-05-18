package com.bodega.api.entregas.dto;

/** Un producto y la cantidad que se va a entregar. */
public class ItemEntrega {

    private int idProducto;
    private double cantidad;

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
}
