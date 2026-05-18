package com.bodega.appmovil.net.modelo;

/** Respuesta de GET /api/auth/me (validar el token guardado). */
public class SesionInfo {
    public int idUser;
    public int idPerfil;
    public int idBodega;
    public boolean esAdmin;
}
