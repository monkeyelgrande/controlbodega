/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

/**
 * DTO de la cabecera de una orden de compra.
 *
 * estado: 0=BORRADOR, 1=PENDIENTE, 2=APROBADA, 3=RECHAZADA.
 * Las fechas se manejan como String ya formateado ("yyyy-MM-dd" / "HH:mm:ss")
 * para encajar con el patrón del resto de la app.
 *
 * @author Monkeyelgrande
 */
public class Ordenes_compra_cabecera {

    public static final int ESTADO_BORRADOR = 0;
    public static final int ESTADO_PENDIENTE = 1;
    public static final int ESTADO_APROBADA = 2;
    public static final int ESTADO_RECHAZADA = 3;

    private int id;
    private String numero;
    private int id_user_crea;
    private String fecha;
    private String hora;
    private int estado;
    private String observacion;
    private int id_bodega;
    private Integer id_user_aprueba;   // nullable
    private String fecha_aprobacion;   // nullable

    public Ordenes_compra_cabecera() {
    }

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

    public Integer getId_user_aprueba() {
        return id_user_aprueba;
    }

    public void setId_user_aprueba(Integer id_user_aprueba) {
        this.id_user_aprueba = id_user_aprueba;
    }

    public String getFecha_aprobacion() {
        return fecha_aprobacion;
    }

    public void setFecha_aprobacion(String fecha_aprobacion) {
        this.fecha_aprobacion = fecha_aprobacion;
    }

    /** Texto legible del estado para mostrar en tablas. */
    public static String nombreEstado(int estado) {
        switch (estado) {
            case ESTADO_BORRADOR:
                return "Borrador";
            case ESTADO_PENDIENTE:
                return "Pendiente";
            case ESTADO_APROBADA:
                return "Aprobada";
            case ESTADO_RECHAZADA:
                return "Rechazada";
            default:
                return "Desconocido";
        }
    }
}
