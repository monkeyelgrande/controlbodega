package Estilos;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.util.Collections;
import java.util.prefs.Preferences;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * Tema visual global del sistema: FlatLaf con estilo material design.
 * Llamar a {@link #aplicar()} una sola vez al inicio del programa,
 * antes de crear cualquier ventana.
 *
 * Soporta modo claro y oscuro; la elección se guarda en las preferencias
 * del usuario y se puede cambiar en caliente con {@link #alternar()}.
 *
 * Traído del rediseño de electro-industrial, con la paleta verde
 * de controlbodega (la misma del login).
 *
 * @author Monkeyelgrande
 */
public final class Tema {

    /** Color primario del sistema (material green 800, el del login). */
    public static final Color PRIMARIO = new Color(0x2E7D32);
    /** Variante clara del primario (material green 400). */
    public static final Color PRIMARIO_CLARO = new Color(0x66BB6A);
    /** Variante oscura del primario (material green 900). */
    public static final Color PRIMARIO_OSCURO = new Color(0x1B5E20);
    /** Inicio del degradado de paneles decorativos. */
    public static final Color DEGRADADO_INICIO = new Color(0x0F2012);
    /** Punto medio del degradado. */
    public static final Color DEGRADADO_MEDIO = new Color(0x1B3A20);
    /** Fin del degradado. */
    public static final Color DEGRADADO_FIN = new Color(0x2C5334);
    /** Color de texto principal sobre fondos claros. */
    public static final Color TEXTO = new Color(0x263238);
    /** Color de texto secundario. */
    public static final Color TEXTO_SUAVE = new Color(0x78909C);

    private static final String PREF_MODO_OSCURO = "modoOscuro";
    private static boolean oscuro;
    private static String nombreFuente;

    private Tema() {
    }

    /**
     * Configura FlatLaf, la fuente base y los ajustes de estilo material
     * para todo el sistema. Restaura el modo claro/oscuro guardado.
     */
    public static void aplicar() {
        FontAwesome.cargar();

        oscuro = Preferences.userNodeForPackage(Tema.class)
                .getBoolean(PREF_MODO_OSCURO, false);

        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#2E7D32"));
        instalarLaf();

        // Barras de título modernas dibujadas por FlatLaf en todas las ventanas
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        ajustesComunes();
    }

    /**
     * Cambia entre modo claro y oscuro, actualiza todas las ventanas
     * abiertas y guarda la preferencia.
     */
    public static void alternar() {
        oscuro = !oscuro;
        Preferences.userNodeForPackage(Tema.class).putBoolean(PREF_MODO_OSCURO, oscuro);
        instalarLaf();
        ajustesComunes();
        FlatLaf.updateUI();
    }

    /** Indica si el modo oscuro está activo. */
    public static boolean esOscuro() {
        return oscuro;
    }

    private static void instalarLaf() {
        if (oscuro) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
    }

    /**
     * Ajustes de UIManager que se pierden al cambiar de look and feel,
     * por eso se re-aplican tanto en {@link #aplicar()} como en
     * {@link #alternar()}.
     */
    private static void ajustesComunes() {
        UIManager.put("defaultFont", new FontUIResource(fuenteBase(Font.PLAIN, 13)));

        // Esquinas redondeadas estilo material
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("CheckBox.arc", 6);

        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("Button.margin", new Insets(6, 16, 6, 16));

        UIManager.put("Table.rowHeight", 26);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));

        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));

        UIManager.put("PasswordField.showRevealButton", true);
        UIManager.put("TitlePane.unifiedBackground", true);
    }

    // ---- Colores de la interfaz principal (dependen del modo) ----

    /** Fondo de tarjetas y barras (blanco en claro, gris verdoso en oscuro). */
    public static Color tarjeta() {
        return oscuro ? new Color(0x18231B) : Color.WHITE;
    }

    /** Color de bordes y separadores sutiles. */
    public static Color borde() {
        return oscuro ? new Color(0x2A3A2E) : new Color(0xDCE6DD);
    }

    // ---- Colores de la barra lateral (siempre oscura, tono bosque) ----

    /** Inicio del degradado vertical de la barra lateral. */
    public static Color barraFondo() {
        return oscuro ? new Color(0x0A1A0E) : new Color(0x102A17);
    }

    /** Fin del degradado vertical de la barra lateral. */
    public static Color barraFondo2() {
        return oscuro ? new Color(0x081409) : new Color(0x0C2011);
    }

    /** Texto normal de la barra lateral. */
    public static Color barraTexto() {
        return new Color(0xBCD2BF);
    }

    /** Texto secundario de la barra lateral (subitems, secciones). */
    public static Color barraTextoSuave() {
        return new Color(0x7B947E);
    }

    /** Fondo de la opcion bajo el cursor en la barra lateral. */
    public static Color barraHover() {
        return new Color(255, 255, 255, 18);
    }

    /** Fondo de la opcion activa en la barra lateral. */
    public static Color barraSeleccion() {
        return new Color(46, 125, 50, 70);
    }

    /**
     * Fuente base del sistema.
     *
     * Se usa Segoe UI (estática, presente en todo Windows) y no Segoe UI
     * Variable: esta última es una fuente variable de Windows 11 y el motor
     * de fuentes de Java 8 no la soporta, por lo que en algunos equipos
     * resuelve mal el mapa de glifos y el texto sale corrido un carácter
     * ("bodega_nuevo" se dibuja como "ancdf`^mtdun").
     *
     * Se puede forzar otra familia en campo con
     * -Dcontrolbodega.fuente="Nombre de la fuente".
     */
    public static Font fuenteBase(int estilo, int tamano) {
        return new Font(nombreFuenteBase(), estilo, tamano);
    }

    private static String nombreFuenteBase() {
        // se resuelve una sola vez: fuenteBase() se llama desde paintComponent
        if (nombreFuente == null) {
            String forzada = System.getProperty("controlbodega.fuente");
            if (forzada != null && !forzada.trim().isEmpty() && existeFuente(forzada.trim())) {
                nombreFuente = forzada.trim();
            } else if (existeFuente("Segoe UI")) {
                nombreFuente = "Segoe UI";
            } else {
                nombreFuente = Font.SANS_SERIF;
            }
        }
        return nombreFuente;
    }

    private static boolean existeFuente(String nombre) {
        for (String familia : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (familia.equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }
}
