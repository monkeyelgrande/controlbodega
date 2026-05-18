package com.bodega.appmovil.net.modelo;

import java.util.ArrayList;
import java.util.List;

public class EntregaResponse {
    public boolean ok;
    public int idCabecera;
    public String accion;
    public String mensaje;
    public List<String> detalle = new ArrayList<>();
}
