package Metodos;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.swing.ImageIcon;

/**
 * Helper para cargar y usar Font Awesome (fa-solid.otf, fa-regular.otf).
 *
 * Uso:
 *   button.setFont(FontAwesome.solid(16f));
 *   button.setText(FontAwesome.SEARCH);
 *
 * @author M-Work
 */
public final class FontAwesome {

    // Glifos solid (subset usado en la app) - codigos unicode FA 6
    public static final String SEARCH       = "";
    public static final String PLUS         = "";
    public static final String MINUS        = "";
    public static final String TRASH        = "";
    public static final String TRASH_ALT    = "";
    public static final String SAVE         = "";
    public static final String CHECK        = "";
    public static final String CLOSE        = "";
    public static final String EDIT         = "";
    public static final String PENCIL       = "";
    public static final String EYE          = "";
    public static final String SYNC         = "";
    public static final String BOX          = "";
    public static final String BOX_OPEN     = "";
    public static final String WAREHOUSE    = "";
    public static final String CLOCK        = "";
    public static final String CLIPBOARD    = "";
    public static final String EXCLAMATION  = "";
    public static final String INFO         = "";
    public static final String ARROW_UP     = "";
    public static final String ARROW_DOWN   = "";
    public static final String BARCODE      = "";
    public static final String CUBES        = "";
    public static final String LIST         = "";
    public static final String FILE_INVOICE = "";

    /** Campana de notificaciones (FA solid u+f0f3). */
    public static final String BELL         = "";

    /** Chevrons / hamburguesa para la barra colapsable. */
    public static final String CHEVRON_LEFT  = "";
    public static final String CHEVRON_RIGHT = "";
    public static final String BARS          = "";

    private static Font solidBase;
    private static Font regularBase;

    private FontAwesome() {}

    public static Font solid(float size) {
        if (solidBase == null) {
            solidBase = loadFont("/fonts/fa-solid.otf");
        }
        return solidBase.deriveFont(Font.PLAIN, size);
    }

    public static Font regular(float size) {
        if (regularBase == null) {
            regularBase = loadFont("/fonts/fa-regular.otf");
        }
        return regularBase.deriveFont(Font.PLAIN, size);
    }

    /**
     * Renderiza un glifo Font Awesome solid como ImageIcon de tamano fijo.
     * Util para JButton.setIcon() combinado con texto en otra fuente.
     */
    public static ImageIcon icon(String glyph, float size, Color color) {
        return iconWith(solid(size), glyph, size, color);
    }

    public static ImageIcon iconRegular(String glyph, float size, Color color) {
        return iconWith(regular(size), glyph, size, color);
    }

    private static ImageIcon iconWith(Font font, String glyph, float size, Color color) {
        int s = Math.max(8, Math.round(size * 1.4f));
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(glyph);
        int x = (s - w) / 2;
        int y = (s - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(glyph, x, y);
        g.dispose();
        return new ImageIcon(img);
    }

    private static Font loadFont(String resource) {
        try (InputStream is = FontAwesome.class.getResourceAsStream(resource)) {
            if (is == null) {
                System.err.println("FontAwesome: no se encontro " + resource);
                return new Font("Dialog", Font.PLAIN, 14);
            }
            Font f = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            return f;
        } catch (Exception e) {
            System.err.println("FontAwesome: error cargando " + resource + " - " + e.getMessage());
            return new Font("Dialog", Font.PLAIN, 14);
        }
    }
}
