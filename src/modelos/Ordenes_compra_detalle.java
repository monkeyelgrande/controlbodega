/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

/**
 * DTO de una línea (detalle) de una orden de compra.
 *
 * id_proveedor y precio_unitario son nulos al crear la orden y se asignan
 * durante el análisis/aprobación (cada producto puede ir a un proveedor
 * distinto, con su propio precio).
 *
 * @author Monkeyelgrande
 */
public class Ordenes_compra_detalle {

    private int id;
    private int id_orden_cabecera;
    private int id_producto;
    private double cantidad;
    private Integer id_proveedor;     // nullable
    private Double precio_unitario;   // nullable
    private String observacion;

    // Campos auxiliares (no se persisten en esta tabla) para mostrar en UI.
    private String codigo;
    private String descripcion;

    public Ordenes_compra_detalle() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_orden_cabecera() {
        return id_orden_cabecera;
    }

    public void setId_orden_cabecera(int id_orden_cabecera) {
        this.id_orden_cabecera = id_orden_cabecera;
    }

    public int getId_producto() {
        return id_producto;
    }

    public void setId_producto(int id_producto) {
        this.id_producto = id_producto;
    }

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public Integer getId_proveedor() {
        return id_proveedor;
    }

    public void setId_proveedor(Integer id_proveedor) {
        this.id_proveedor = id_proveedor;
    }

    public Double getPrecio_unitario() {
        return precio_unitario;
    }

    public void setPrecio_unitario(Double precio_unitario) {
        this.precio_unitario = precio_unitario;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
