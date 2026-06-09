/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

/**
 * Unidad de entrega de un producto: tamaño de paquete -> bodega.
 *
 *   cantidad_paquete = 1  -> bodega para entregas por unidad (absorbe sobrante).
 *   cantidad_paquete > 1  -> paquete (ej. caja de 50) con su bodega.
 *
 * @author Monkeyelgrande
 */
public class ProductoUnidadEntrega {

    int id, id_producto, id_bodega;
    String nombre;
    double cantidad_paquete;

    public ProductoUnidadEntrega() {
    }

    public ProductoUnidadEntrega(int id_producto, String nombre, double cantidad_paquete, int id_bodega) {
        this.id_producto = id_producto;
        this.nombre = nombre;
        this.cantidad_paquete = cantidad_paquete;
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getCantidad_paquete() {
        return cantidad_paquete;
    }

    public void setCantidad_paquete(double cantidad_paquete) {
        this.cantidad_paquete = cantidad_paquete;
    }
}
