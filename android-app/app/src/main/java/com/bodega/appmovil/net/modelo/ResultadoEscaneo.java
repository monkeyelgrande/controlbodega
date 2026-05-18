package com.bodega.appmovil.net.modelo;

public class ResultadoEscaneo {
    public String resultado;   // OK | QR_INVALIDO | ORDEN_NO_EXISTE | OTRA_BODEGA | ANULADA | YA_ENTREGADA
    public String mensaje;
    public int idEscaneo;
    public OrdenInfo orden;     // null si la lectura no resolvio una orden
}
