/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos.modelos;

/**
 * Detalle de un pago: la aplicación de una parte del pago (cabecera) a una
 * factura concreta. Todo abono pertenece a una cabecera (id_cabecera NOT NULL)
 * y aplica a un crédito (id_credito NOT NULL).
 *
 * @author Monkeyelgrande
 */
public class Abonos {

    int id, id_cabecera, id_credito;
    double abono;
    String fecha, hora;

    public Abonos() {
    }

    public Abonos(int id_credito, double abono, String fecha, String hora) {
        this.id_credito = id_credito;
        this.abono = abono;
        this.fecha = fecha;
        this.hora = hora;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_cabecera() {
        return id_cabecera;
    }

    public void setId_cabecera(int id_cabecera) {
        this.id_cabecera = id_cabecera;
    }

    public int getId_credito() {
        return id_credito;
    }

    public void setId_credito(int id_credito) {
        this.id_credito = id_credito;
    }

    public double getAbono() {
        return abono;
    }

    public void setAbono(double abono) {
        this.abono = abono;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }
}
