package com.bodega.api.common;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Formato de hora identico al de la app de escritorio
 * ({@code DB_consultas_R_D.obtener_hora}).
 *
 * OJO: NO usa ceros a la izquierda (ej. "9:5:3"), a proposito, para que los
 * datos escritos desde la API queden con el mismo formato que los ya
 * guardados por el escritorio en {@code entregas_productos_cabecera.hora_entrega}
 * (columna varchar(8)).
 */
public final class Horas {

    private Horas() {
    }

    public static String horaEntrega() {
        Calendar c = new GregorianCalendar();
        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);
        int s = c.get(Calendar.SECOND);
        return h + ":" + m + ":" + s;
    }
}
