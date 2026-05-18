package com.bodega.api.inventario.dto;

/** Stock de un producto en una bodega concreta. */
public class BodegaStock {
    public int idBodega;
    public String bodega;
    public double cantidad;
    public double pendientes;
    public double disponible;   // cantidad - pendientes

    public int getIdBodega() { return idBodega; }
    public String getBodega() { return bodega; }
    public double getCantidad() { return cantidad; }
    public double getPendientes() { return pendientes; }
    public double getDisponible() { return disponible; }
}
