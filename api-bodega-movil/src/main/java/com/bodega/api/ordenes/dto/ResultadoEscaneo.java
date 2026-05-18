package com.bodega.api.ordenes.dto;

/**
 * Resultado de procesar una lectura de QR.
 *
 * resultado: OK | QR_INVALIDO | ORDEN_NO_EXISTE | OTRA_BODEGA | ANULADA |
 *            YA_ENTREGADA  (mismos valores que el modulo de escritorio)
 */
public class ResultadoEscaneo {

    public String resultado;
    public String mensaje;
    public int idEscaneo;
    public OrdenInfo orden;

    public String getResultado() { return resultado; }
    public String getMensaje() { return mensaje; }
    public int getIdEscaneo() { return idEscaneo; }
    public OrdenInfo getOrden() { return orden; }
}
