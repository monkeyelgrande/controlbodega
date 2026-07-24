package Estilos;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.swing.Icon;

/**
 * Iconos vectoriales a partir de las fuentes Font Awesome ubicadas en
 * el package fonts (fa-solid.otf, fa-regular.otf, fa-brands.otf).
 *
 * Uso: {@code boton.setIcon(FontAwesome.icono(FontAwesome.USUARIO, 16f, Color.WHITE));}
 *
 * Nota: existe también Metodos.FontAwesome (API por String) que usan los
 * formularios de Compras; esta clase es la del rediseño del frm_main
 * (API por char, idéntica a la de electro-industrial).
 *
 * @author Monkeyelgrande
 */
public final class FontAwesome {

    // Glifos de uso común (estilo solid)
    public static final char USUARIO = '';
    public static final char CANDADO = '';
    public static final char CERRAR = '';
    public static final char ENTRAR = '';
    public static final char OJO = '';
    public static final char RAYO = '';
    public static final char ENGRANAJE = '';
    public static final char BUSCAR = '';
    public static final char GUARDAR = '';
    public static final char IMPRESORA = '';
    public static final char CAJA = '';
    public static final char HISTORIAL = '';
    public static final char ETIQUETA = '';
    public static final char CODIGO_BARRAS = '';
    public static final char MAS = '';
    public static final char ESCOBA = '';
    public static final char LAPIZ = '';
    public static final char REFRESCAR = '';
    public static final char ALMACEN = '';
    public static final char DINERO = '';
    public static final char PORCENTAJE = '';
    public static final char CHECK = '';
    public static final char TEXTO = '';
    public static final char BARRAS = '';
    public static final char BASE_DATOS = '';
    public static final char SOL = '';
    public static final char LUNA = '';
    public static final char USUARIOS = '';
    public static final char CAJAS = '';
    public static final char CAJA_REGISTRADORA = '';
    public static final char BILLETERA = '';
    public static final char HERRAMIENTAS = '';
    public static final char GRAFICA = '';
    public static final char CAPAS = '';
    public static final char SALIR = '';
    public static final char CHEVRON_DERECHA = '';
    public static final char CHEVRON_ABAJO = '';
    public static final char BASURA = '';
    public static final char PROHIBIDO = '';
    public static final char CONTACTOS = '';
    public static final char CAMION = '';
    public static final char DESHACER = '';
    public static final char FACTURA = '';
    public static final char EGRESO = '';
    public static final char PRESTAMO = '';
    public static final char CALENDARIO = '';
    public static final char TRANSFERIR = '';
    public static final char DESCARGAR = '';
    public static final char SUBIR = '';
    public static final char ARCHIVO_EXCEL = '';
    public static final char ARCHIVO_IMPORTAR = '';
    public static final char CALCULADORA = '';

    private static Font solid;
    private static Font regular;
    private static Font brands;

    private FontAwesome() {
    }

    /** Carga y registra las fuentes. Se puede llamar varias veces sin costo. */
    public static synchronized void cargar() {
        if (solid != null) {
            return;
        }
        solid = cargarFuente("/fonts/fa-solid.otf");
        regular = cargarFuente("/fonts/fa-regular.otf");
        brands = cargarFuente("/fonts/fa-brands.otf");
    }

    public static Font solid(float tamano) {
        cargar();
        return solid.deriveFont(tamano);
    }

    public static Font regular(float tamano) {
        cargar();
        return regular.deriveFont(tamano);
    }

    public static Font brands(float tamano) {
        cargar();
        return brands.deriveFont(tamano);
    }

    /** Crea un icono con un glifo del estilo solid. */
    public static Icon icono(char glifo, float tamano, Color color) {
        return new IconoFA(solid(tamano), glifo, color);
    }

    /** Crea un icono con la fuente indicada (solid, regular o brands). */
    public static Icon icono(Font fuente, char glifo, Color color) {
        return new IconoFA(fuente, glifo, color);
    }

    private static Font cargarFuente(String ruta) {
        InputStream entrada = FontAwesome.class.getResourceAsStream(ruta);
        try {
            Font fuente = Font.createFont(Font.TRUETYPE_FONT, entrada);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(fuente);
            return fuente;
        } catch (Exception ex) {
            return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        } finally {
            if (entrada != null) {
                try {
                    entrada.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    /** Icono que dibuja un glifo de Font Awesome con antialiasing. */
    private static class IconoFA implements Icon {

        private final Font fuente;
        private final String texto;
        private final Color color;
        private final int ancho;
        private final int alto;

        IconoFA(Font fuente, char glifo, Color color) {
            this.fuente = fuente;
            this.texto = String.valueOf(glifo);
            this.color = color;

            BufferedImage medida = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = medida.createGraphics();
            g2.setFont(fuente);
            FontMetrics fm = g2.getFontMetrics();
            this.ancho = fm.stringWidth(texto);
            this.alto = fm.getHeight();
            g2.dispose();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(fuente);
            g2.setColor(color);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(texto, x, y + fm.getAscent());
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return ancho;
        }

        @Override
        public int getIconHeight() {
            return alto;
        }
    }
}
