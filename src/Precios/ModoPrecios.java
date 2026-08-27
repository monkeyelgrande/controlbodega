package Precios;

import conexiondb.DB_consultas_R_D;
import java.sql.ResultSet;

/**
 * Modo de calculo de precios de la instalacion (configuraciones.modo_precios):
 * controlbodega es un solo desarrollo para varias empresas y lo unico que
 * cambia entre ellas es como se calculan los precios de venta.
 *
 *   AGRO  (default) = 1 margen + descuentos escalonados + S&T + credito
 *   TECNI           = 3 margenes independientes -> Precio 1/2/3 (sin
 *                     descuentos escalonados ni credito)
 *
 * En TECNI se reutilizan las columnas existentes: productos.venta = Precio 1,
 * valor_desc_1 = Precio 2, valor_desc_2 = Precio 3; los margenes 2 y 3 viven
 * en productos.porcentaje_utilidad2/3, y en ingresos_productos_detalle los
 * porcentajes de la linea viajan en desc_n_1/desc_n_2.
 *
 * @author Monkeyelgrande
 */
public final class ModoPrecios {

    public static final String AGRO = "AGRO";
    public static final String TECNI = "TECNI";

    private static String modo = null;

    private ModoPrecios() {
    }

    /** @return el modo vigente ('AGRO' o 'TECNI'), leido una sola vez de la base. */
    public static synchronized String actual() {
        if (modo == null) {
            modo = leer();
        }
        return modo;
    }

    public static boolean esTecni() {
        return TECNI.equals(actual());
    }

    /** Descarta el valor cacheado (tras cambiar el modo en configuraciones). */
    public static synchronized void recargar() {
        modo = null;
    }

    private static String leer() {
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(
                    "select coalesce(modo_precios,'AGRO') as modo_precios from configuraciones where id = 1");
            if (rs != null && rs.next()) {
                String m = rs.getString("modo_precios");
                rs.close();
                if (m != null && TECNI.equalsIgnoreCase(m.trim())) {
                    return TECNI;
                }
                return AGRO;
            }
            if (rs != null) {
                rs.close();
            }
        } catch (Exception e) {
            System.out.println("No se pudo leer configuraciones.modo_precios: " + e);
        }
        // Si la columna no existe (migracion sin correr) el modulo se comporta
        // como siempre: modo AGRO.
        return AGRO;
    }
}
