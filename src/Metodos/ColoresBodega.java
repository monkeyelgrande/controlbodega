/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Metodos;

import conexiondb.DB_consultas_R_D;
import java.awt.Color;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache compartida del color identificador de cada bodega (bodegas.color, hex
 * "#RRGGBB"), indexada por nombre de bodega. La usan los renderers de las tablas
 * que muestran una columna "Bodega" para pintarla con su color, sin consultar la
 * BD en cada celda. Se refresca con recargar() al actualizar cada tabla.
 *
 * @author Monkeyelgrande
 */
public class ColoresBodega {

    private static Map<String, Color> cache = new HashMap<>();

    public static void recargar() {
        Map<String, Color> nuevos = new HashMap<>();
        ResultSet rs = DB_consultas_R_D.getTabla("select nombre, color from bodegas");
        try {
            while (rs.next()) {
                String nombre = rs.getString("nombre");
                String hex = rs.getString("color");
                if (nombre != null && hex != null && !hex.trim().isEmpty()) {
                    try {
                        nuevos.put(nombre, Color.decode(hex.trim()));
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        cache = nuevos;
    }

    /**
     * Color asignado a la bodega con ese nombre, o null si no tiene color o el
     * nombre no corresponde a ninguna bodega.
     */
    public static Color get(String nombreBodega) {
        return nombreBodega == null ? null : cache.get(nombreBodega);
    }

    /**
     * Color de texto (blanco o negro) que mejor contrasta con el color de fondo
     * dado, segun su luminancia.
     */
    public static Color textoContraste(Color fondo) {
        double lum = (0.299 * fondo.getRed() + 0.587 * fondo.getGreen() + 0.114 * fondo.getBlue()) / 255.0;
        return lum < 0.55 ? Color.WHITE : Color.BLACK;
    }
}
