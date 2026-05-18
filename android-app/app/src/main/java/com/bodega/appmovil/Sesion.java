package com.bodega.appmovil;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Guarda en el telefono el servidor, el token y los datos del usuario.
 */
public class Sesion {

    private static final String PREF = "bodega_sesion";
    private final SharedPreferences sp;

    public Sesion(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void guardarServidor(String host) {
        sp.edit().putString("servidor", host).apply();
    }

    public String getServidor() {
        return sp.getString("servidor", "");
    }

    public void guardarLogin(String token, int idUser, int idBodega,
                             String nombre, String bodega) {
        sp.edit()
                .putString("token", token)
                .putInt("idUser", idUser)
                .putInt("idBodega", idBodega)
                .putString("nombre", nombre)
                .putString("bodega", bodega)
                .apply();
    }

    public String getToken() {
        return sp.getString("token", "");
    }

    public String getNombre() {
        return sp.getString("nombre", "");
    }

    public String getBodega() {
        return sp.getString("bodega", "");
    }

    /** Hay datos para intentar reanudar sesion (token + servidor guardados). */
    public boolean tieneSesion() {
        return !getToken().isEmpty() && !getServidor().isEmpty();
    }

    /** Cierra sesion: borra el token pero conserva el servidor para el login. */
    public void cerrar() {
        sp.edit().remove("token").apply();
    }
}
