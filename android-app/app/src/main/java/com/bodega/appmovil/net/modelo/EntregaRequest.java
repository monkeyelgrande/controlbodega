package com.bodega.appmovil.net.modelo;

import java.util.ArrayList;
import java.util.List;

public class EntregaRequest {
    public Integer idFactura;
    public int idEscaneo;
    public String accion;   // ENTREGA_COMPLETA | ENTREGA_PARCIAL
    public List<ItemEntrega> items = new ArrayList<>();
}
