package com.bodega.api.entregas.dto;

import java.util.ArrayList;
import java.util.List;

/** Resultado de ejecutar una entrega. */
public class EntregaResponse {

    private boolean ok;
    private int idCabecera;
    private String accion;
    private String mensaje;
    private List<String> detalle = new ArrayList<>();

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }

    public int getIdCabecera() { return idCabecera; }
    public void setIdCabecera(int idCabecera) { this.idCabecera = idCabecera; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public List<String> getDetalle() { return detalle; }
    public void setDetalle(List<String> detalle) { this.detalle = detalle; }
}
