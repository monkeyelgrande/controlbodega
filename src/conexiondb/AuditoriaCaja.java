/*
 * Contexto de auditoria del modulo Caja.
 *
 * La auditoria real la hacen los triggers de PostgreSQL (ver
 * sql/migracion_auditoria_caja.sql). Esta clase solo le cuenta a la base de
 * datos QUIEN esta trabajando y DESDE DONDE, publicando variables de sesion
 * (app.id_usuario, app.usuario, app.origen, app.motivo) en cada conexion.
 *
 * Si esto fallara, la auditoria NO se pierde: el trigger cae al id_user de la
 * fila y siempre registra el rol de base de datos y la IP del equipo.
 */
package conexiondb;

import Formularios.frm_main;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 *
 * @author Monkeyelgrande
 */
public class AuditoriaCaja {

    // Formulario/accion que esta ejecutando la operacion. Es por hilo para que
    // dos ventanas no se pisen el valor.
    private static final ThreadLocal<String> ORIGEN = new ThreadLocal<String>();
    // Motivo opcional (p.ej. por que se elimino un ingreso). Hoy nadie lo pide
    // al usuario; la columna y el conducto quedan listos por si se agrega.
    private static final ThreadLocal<String> MOTIVO = new ThreadLocal<String>();

    /** Marca el origen de las operaciones siguientes en este hilo. */
    public static void setOrigen(String origen) {
        ORIGEN.set(origen);
    }

    /** Motivo de la operacion (opcional). */
    public static void setMotivo(String motivo) {
        MOTIVO.set(motivo);
    }

    /** Se llama en el finally de la accion para no dejar contexto viejo pegado. */
    public static void limpiar() {
        ORIGEN.remove();
        MOTIVO.remove();
    }

    /**
     * Publica el contexto en la conexion recien abierta. Un solo viaje a la
     * base. Cualquier error se traga: la auditoria nunca puede impedir trabajar.
     */
    public static void aplicar(Connection con) {
        if (con == null) {
            return;
        }
        try {
            PreparedStatement psql = con.prepareStatement(
                    "select set_config('app.id_usuario', ?, false), "
                    + "set_config('app.usuario', ?, false), "
                    + "set_config('app.origen', ?, false), "
                    + "set_config('app.motivo', ?, false)");
            psql.setString(1, frm_main.id_user > 0 ? String.valueOf(frm_main.id_user) : "");
            psql.setString(2, texto(nombreUsuario()));
            psql.setString(3, texto(ORIGEN.get()));
            psql.setString(4, texto(MOTIVO.get()));
            psql.execute();
            psql.close();
        } catch (Exception e) {
            // Silencio a proposito: sin contexto el trigger igual audita.
        }
    }

    /** Usuario logueado segun la ventana principal (puede no existir aun). */
    private static String nombreUsuario() {
        try {
            if (frm_main.lbl_user != null) {
                return frm_main.lbl_user.getText();
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
