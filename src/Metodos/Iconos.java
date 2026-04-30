package Metodos;

import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * Helper para cargar iconos SVG de FlatLaf desde el classpath.
 *
 * Coloca los archivos .svg en src/imagenes/svg y referencialos como
 * Iconos.svg("nombre.svg") o Iconos.svg("nombre.svg", 20, 20).
 */
public final class Iconos {

    private static final String BASE = "imagenes/svg/";

    private Iconos() {
    }

    public static FlatSVGIcon svg(String fileName) {
        return new FlatSVGIcon(BASE + fileName);
    }

    public static FlatSVGIcon svg(String fileName, int width, int height) {
        return new FlatSVGIcon(BASE + fileName, width, height);
    }

    public static FlatSVGIcon svg(String fileName, float scale) {
        return (FlatSVGIcon) new FlatSVGIcon(BASE + fileName).derive(scale);
    }
}
