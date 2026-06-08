/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

/**
 * Rango de cantidad -> bodega de descarga, para un producto.
 *
 * cantidad_max == null significa "en adelante" (sin tope superior).
 *
 * @author Monkeyelgrande
 */
public class ProductoBodegaRango {

    int id, id_producto, id_bodega;
    double cantidad_min;
    Double cantidad_max; // null = sin tope ("en adelante")

    public ProductoBodegaRango() {
    }

    public ProductoBodegaRango(int id_producto, double cantidad_min, Double cantidad_max, int id_bodega) {
        this.id_producto = id_producto;
        this.cantidad_min = cantidad_min;
        this.cantidad_max = cantidad_max;
        this.id_bodega = id_bodega;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_producto() {
        return id_producto;
    }

    public void setId_producto(int id_producto) {
        this.id_producto = id_producto;
    }

    public int getId_bodega() {
        return id_bodega;
    }

    public void setId_bodega(int id_bodega) {
        this.id_bodega = id_bodega;
    }

    public double getCantidad_min() {
        return cantidad_min;
    }

    public void setCantidad_min(double cantidad_min) {
        this.cantidad_min = cantidad_min;
    }

    public Double getCantidad_max() {
        return cantidad_max;
    }

    public void setCantidad_max(Double cantidad_max) {
        this.cantidad_max = cantidad_max;
    }
}
