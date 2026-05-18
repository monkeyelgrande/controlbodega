package com.bodega.appmovil.net.modelo;

import java.util.ArrayList;
import java.util.List;

public class ProductoInventario {
    public int idProducto;
    public String codigo;
    public String descripcion;
    public double totalCantidad;
    public double totalPendientes;
    public double totalDisponible;
    public List<BodegaStock> bodegas = new ArrayList<>();
}
