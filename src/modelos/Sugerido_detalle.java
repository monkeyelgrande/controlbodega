/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

/**
 * DTO de una línea de un sugerido de pedidos.
 *
 * @author Monkeyelgrande
 */
public class Sugerido_detalle {

    private int id;
    private int id_sugerido_cab;
    private int id_producto;
    private double cantidad_sugerida;
    private double existencia;
    private double rotacion_mensual;
    private double ultima_compra;
    private boolean seleccionado = true;
    private Double cantidad_final;   // nullable
    private String observacion;

    // auxiliares para UI
    private String codigo;
    private String descripcion;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_sugerido_cab() {
        return id_sugerido_cab;
    }

    public void setId_sugerido_cab(int id_sugerido_cab) {
        this.id_sugerido_cab = id_sugerido_cab;
    }

    public int getId_producto() {
        return id_producto;
    }

    public void setId_producto(int id_producto) {
        this.id_producto = id_producto;
    }

    public double getCantidad_sugerida() {
        return cantidad_sugerida;
    }

    public void setCantidad_sugerida(double cantidad_sugerida) {
        this.cantidad_sugerida = cantidad_sugerida;
    }

    public double getExistencia() {
        return existencia;
    }

    public void setExistencia(double existencia) {
        this.existencia = existencia;
    }

    public double getRotacion_mensual() {
        return rotacion_mensual;
    }

    public void setRotacion_mensual(double rotacion_mensual) {
        this.rotacion_mensual = rotacion_mensual;
    }

    public double getUltima_compra() {
        return ultima_compra;
    }

    public void setUltima_compra(double ultima_compra) {
        this.ultima_compra = ultima_compra;
    }

    public boolean isSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado = seleccionado;
    }

    public Double getCantidad_final() {
        return cantidad_final;
    }

    public void setCantidad_final(Double cantidad_final) {
        this.cantidad_final = cantidad_final;
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
