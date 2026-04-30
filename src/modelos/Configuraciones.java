/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

/**
 *
 * @author Monkeyelgrande
 */
public class Configuraciones {

    String nombre_negocio, nit_negocio, contacto_Negocio, contacto2_Negocio, direccion, nombre_impresota, pie_legal, servicios, impresora_venta_80mm;
    int id, utilida_venta, utilida_mayorista, utilida_credito, iva, imprimir_factura, productos_repetidos;

    public String getImpresora_venta_80mm() {
        return impresora_venta_80mm;
    }

    public void setImpresora_venta_80mm(String impresora_venta_80mm) {
        this.impresora_venta_80mm = impresora_venta_80mm;
    }

    public String getPie_legal() {
        return pie_legal;
    }

    public void setPie_legal(String pie_legal) {
        this.pie_legal = pie_legal;
    }

    public String getServicios() {
        return servicios;
    }

    public void setServicios(String servicios) {
        this.servicios = servicios;
    }

    public int getProductos_repetidos() {
        return productos_repetidos;
    }

    public void setProductos_repetidos(int productos_repetidos) {
        this.productos_repetidos = productos_repetidos;
    }

    public int getImprimir_factura() {
        return imprimir_factura;
    }

    public void setImprimir_factura(int imprimir_factura) {
        this.imprimir_factura = imprimir_factura;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getNombre_negocio() {
        return nombre_negocio;
    }

    public void setNombre_negocio(String nombre_negocio) {
        this.nombre_negocio = nombre_negocio;
    }

    public String getNit_negocio() {
        return nit_negocio;
    }

    public void setNit_negocio(String nit_negocio) {
        this.nit_negocio = nit_negocio;
    }

    public String getContacto_Negocio() {
        return contacto_Negocio;
    }

    public void setContacto_Negocio(String contacto_Negocio) {
        this.contacto_Negocio = contacto_Negocio;
    }

    public String getContacto2_Negocio() {
        return contacto2_Negocio;
    }

    public void setContacto2_Negocio(String contacto2_Negocio) {
        this.contacto2_Negocio = contacto2_Negocio;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUtilida_venta() {
        return utilida_venta;
    }

    public void setUtilida_venta(int utilida_venta) {
        this.utilida_venta = utilida_venta;
    }

    public int getUtilida_mayorista() {
        return utilida_mayorista;
    }

    public void setUtilida_mayorista(int utilida_mayorista) {
        this.utilida_mayorista = utilida_mayorista;
    }

    public int getUtilida_credito() {
        return utilida_credito;
    }

    public void setUtilida_credito(int utilida_credito) {
        this.utilida_credito = utilida_credito;
    }

    public int getIva() {
        return iva;
    }

    public void setIva(int iva) {
        this.iva = iva;
    }

    public String getNombre_impresota() {
        return nombre_impresota;
    }

    public void setNombre_impresota(String nombre_impresota) {
        this.nombre_impresota = nombre_impresota;
    }

}
