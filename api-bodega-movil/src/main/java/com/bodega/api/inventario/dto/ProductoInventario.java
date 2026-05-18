package com.bodega.api.inventario.dto;

import java.util.ArrayList;
import java.util.List;

/** Un producto con su stock agrupado por bodega. */
public class ProductoInventario {
    public int idProducto;
    public String codigo;
    public String descripcion;
    public double totalCantidad;
    public double totalPendientes;
    public double totalDisponible;
    public List<BodegaStock> bodegas = new ArrayList<>();

    public int getIdProducto() { return idProducto; }
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public double getTotalCantidad() { return totalCantidad; }
    public double getTotalPendientes() { return totalPendientes; }
    public double getTotalDisponible() { return totalDisponible; }
    public List<BodegaStock> getBodegas() { return bodegas; }
}
