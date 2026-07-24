package Creditos;

import conexiondb.DB_consultas_R_D;
import java.awt.Color;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JTable;

/**
 * Helpers propios del modulo Creditos que no existen en Metodos.metodos de
 * controlbodega (vienen del metodos.java de control_creditos). El resto de
 * utilidades compartidas se usan directamente de Metodos.metodos.
 *
 * @author Monkeyelgrande
 */
public final class MetodosCreditos {

    private MetodosCreditos() {
    }

    public static void EstiloTablaMaterialGlobalPequeno(JTable jtabla) {
        jtabla.getTableHeader().setReorderingAllowed(false);
        jtabla.getTableHeader().setDefaultRenderer(new EstiloTablasHeaderPequeno());
        jtabla.setDefaultRenderer(Object.class, new EstiloTablasBodyPequeno());
        jtabla.setRowHeight(25);
    }

    /**
     * Mapa nombre de tipo de abono -> color, para los renderers de las tablas
     * de creditos (en control_creditos vivia en frm_main). Se carga perezoso
     * y frm_Tipos_abonos puede recargarlo con Crear_Mapeo_Colores().
     */
    public static Map<String, Color> mapaColores = new HashMap<>();

    public static Color Palabra_a_Color(String palabra) {
        if (mapaColores.isEmpty()) {
            Crear_Mapeo_Colores();
        }
        return mapaColores.get(palabra);
    }

    public static void Crear_Mapeo_Colores() {
        String consulta = "select * from tipos_abonos";
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                switch (rs.getString("color")) {
                    case "AMARILLO":
                        mapaColores.put(rs.getString("NOMBRE"), Color.YELLOW);
                        break;
                    case "AZUL":
                        mapaColores.put(rs.getString("NOMBRE"), Color.CYAN);
                        break;
                    case "ROJO":
                        mapaColores.put(rs.getString("NOMBRE"), Color.RED);
                        break;
                    case "BLACO":
                        mapaColores.put(rs.getString("NOMBRE"), Color.WHITE);
                        break;
                    case "NARANJA":
                        mapaColores.put(rs.getString("NOMBRE"), Color.ORANGE);
                        break;
                    case "VERDE":
                        mapaColores.put(rs.getString("NOMBRE"), Color.GREEN);
                        break;
                    case "GRIS":
                        mapaColores.put(rs.getString("NOMBRE"), Color.GRAY);
                        break;
                    case "ROSA":
                        mapaColores.put(rs.getString("NOMBRE"), Color.PINK);
                        break;
                }
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("MetodosCreditos.Crear_Mapeo_Colores: " + e);
        }
    }

    public static DecimalFormat formateador_tres_decimales() {
        DecimalFormat formatea = null;
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator('.');
        simbolos.setGroupingSeparator(',');
        formatea = new DecimalFormat("###,###.###", simbolos);
        return formatea;
    }
}
