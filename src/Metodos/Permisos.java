package Metodos;

import java.util.HashSet;
import java.util.Set;

/**
 * Permisos efectivos del usuario logueado, cargados una vez en el login desde
 * las tablas de permisos administrables (ver DBpermisos). Si la carga falla
 * (BD sin la migracion de permisos o sin conexion), estaCargado() queda en
 * false y frm_main.permisos() cae al switch interno anterior.
 *
 * @author Monkeyelgrande
 */
public final class Permisos {

    private static Set<String> opciones = new HashSet<>();
    private static boolean cargado = false;

    private Permisos() {
    }

    /** Carga las opciones efectivas del usuario. Llamar tras el login. */
    public static boolean cargar(int idUser, int idPerfil) {
        Set<String> s = conexiondb.DBpermisos.opcionesEfectivas(idUser, idPerfil);
        cargado = (s != null);
        opciones = cargado ? s : new HashSet<String>();
        return cargado;
    }

    public static boolean estaCargado() {
        return cargado;
    }

    /** true si el usuario tiene la opcion. El perfil 1 (Admin) siempre puede. */
    public static boolean puede(String clave) {
        return Formularios.frm_main.perfil == 1 || opciones.contains(clave);
    }
}
