package migracionagro;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acumula contadores y observaciones de la migracion y los vuelca a consola
 * y a un archivo de texto junto al proyecto.
 */
public class ReporteMigracion {

    private final Map<String, Integer> contadores = new LinkedHashMap<>();
    private final List<String> lineas = new ArrayList<>();
    private final List<String> advertencias = new ArrayList<>();

    public void incrementar(String clave) {
        Integer v = contadores.get(clave);
        contadores.put(clave, v == null ? 1 : v + 1);
    }

    public int valor(String clave) {
        Integer v = contadores.get(clave);
        return v == null ? 0 : v;
    }

    public void linea(String texto) {
        lineas.add(texto);
        System.out.println(texto);
    }

    public void advertencia(String texto) {
        advertencias.add(texto);
        System.out.println("  [!] " + texto);
    }

    public void resumen() {
        linea("");
        linea("==================== RESUMEN ====================");
        for (Map.Entry<String, Integer> e : contadores.entrySet()) {
            linea(String.format("  %-55s %8d", e.getKey(), e.getValue()));
        }
        if (!advertencias.isEmpty()) {
            linea("");
            linea("---------------- ADVERTENCIAS (" + advertencias.size() + ") ----------------");
            for (String a : advertencias) {
                linea("  [!] " + a);
            }
        }
    }

    public void guardar(String dryRun) {
        String nombre = "reporte_migracion_agro_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
                + (dryRun.isEmpty() ? "" : "_" + dryRun) + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(nombre))) {
            for (String l : lineas) {
                pw.println(l);
            }
            System.out.println("\nReporte guardado en: " + nombre);
        } catch (IOException ex) {
            System.out.println("No se pudo guardar el reporte: " + ex.getMessage());
        }
    }
}
