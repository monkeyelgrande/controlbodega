/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

/**
 * DTO de la cabecera de un sugerido de pedidos (RF-01).
 * estado: 0=ABIERTO, 1=BLOQUEADO, 2=PROCESADO.
 *
 * @author Monkeyelgrande
 */
public class Sugerido_cabecera {

    public static final int ESTADO_ABIERTO = 0;
    public static final int ESTADO_BLOQUEADO = 1;
    public static final int ESTADO_PROCESADO = 2;

    private int id;
    private String numero;
    private int id_user_crea;
    private String fecha;
    private String hora;
    private int estado;
    private String observacion;
    private int id_bodega;
    private double meses_cobertura = 1;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public int getId_user_crea() {
        return id_user_crea;
    }

    public void setId_user_crea(int id_user_crea) {
        this.id_user_crea = id_user_crea;
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

    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public int getId_bodega() {
        return id_bodega;
    }

    public void setId_bodega(int id_bodega) {
        this.id_bodega = id_bodega;
    }

    public double getMeses_cobertura() {
        return meses_cobertura;
    }

    public void setMeses_cobertura(double meses_cobertura) {
        this.meses_cobertura = meses_cobertura;
    }

    public static String nombreEstado(int estado) {
        switch (estado) {
            case ESTADO_ABIERTO:
                return "Abierto";
            case ESTADO_BLOQUEADO:
                return "Bloqueado";
            case ESTADO_PROCESADO:
                return "Procesado";
            default:
                return "Desconocido";
        }
    }
}
