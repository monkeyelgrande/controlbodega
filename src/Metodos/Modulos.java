package Metodos;

import java.util.HashSet;
import java.util.Set;

/**
 * Modulos activos en esta instalacion, cargados una vez en el login desde la
 * tabla modulos (ver DBmodulos y sql/migracion_modulos.sql). Es el interruptor
 * comercial por cliente: un modulo apagado desaparece para todos los usuarios,
 * incluso el Admin — a diferencia de {@link Permisos}, que decide que ve cada
 * usuario dentro de un modulo activo.
 *
 * Si la BD no tiene la migracion de modulos, los conjuntos quedan vacios y
 * todo se considera activo (comportamiento anterior).
 *
 * @author Monkeyelgrande
 */
public final class Modulos {

    private static Set<String> modulosApagados = new HashSet<>();
    private static Set<String> opcionesApagadas = new HashSet<>();

    private Modulos() {
    }

    /** Carga el estado de los modulos. Llamar tras el login, antes de Permisos.cargar. */
    public static void cargar() {
        modulosApagados = conexiondb.DBmodulos.modulosInactivos();
        opcionesApagadas = conexiondb.DBmodulos.opcionesDeModulosInactivos();
    }

    /**
     * true si el modulo esta activo en esta instalacion. La clave corresponde
     * a opciones.modulo / modulos.clave ('Compras', 'Precios', ...). Un modulo
     * sin fila en la tabla es nucleo y siempre esta activo.
     */
    public static boolean activo(String clave) {
        return !modulosApagados.contains(clave);
    }

    /** true si la opcion (opciones.clave) pertenece a un modulo apagado. */
    public static boolean opcionApagada(String claveOpcion) {
        return opcionesApagadas.contains(claveOpcion);
    }
}
