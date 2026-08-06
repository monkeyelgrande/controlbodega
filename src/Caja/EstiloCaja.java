package Caja;

import Metodos.FontAwesome;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Sistema de estilos "SaaS claro" (inspirado en Stripe/Linear/Vercel) para el
 * modulo Caja: paleta neutra clara, un unico color primario indigo y colores
 * semanticos solo para estados. Usa FlatLaf + iconos Font Awesome (package
 * fonts). Pensado para reutilizarse en Ingresos y Egresos.
 *
 * @author Monkeyelgrande
 */
public final class EstiloCaja {

    // ---- Paleta neutra clara ----
    public static final Color BG = new Color(0xF8F9FB);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SURFACE_2 = new Color(0xFAFBFC);
    public static final Color HOVER = new Color(0xF4F5F7);
    public static final Color LINE = new Color(0xE9EAEE);
    public static final Color LINE_2 = new Color(0xE3E5E9);
    public static final Color TEXT = new Color(0x16181D);
    public static final Color TEXT_2 = new Color(0x5B616E);
    public static final Color TEXT_3 = new Color(0x8A909C);
    public static final Color TEXT_4 = new Color(0xAAB0BA);

    // ---- Primario neutro de respaldo ----
    public static final Color PRIMARY = new Color(0x4F46E5);
    public static final Color PRIMARY_HOVER = new Color(0x4338CA);
    public static final Color PRIMARY_TINT = new Color(0xEEF0FE);

    /**
     * Color de identidad del modulo. Permite diferenciar de un vistazo un
     * formulario de otro: verde = Ingresos (entra dinero), rojo = Egresos
     * (sale dinero). Se aplica solo a zonas clave (franja superior de las
     * tarjetas, boton guardar, foco, seleccion de tabla y total), el resto
     * de la interfaz se mantiene neutra.
     */
    public static final class Accent {

        public final Color base;
        public final Color hover;
        public final Color pressed;
        public final Color tint;
        public final String hex;
        public final String hexHover;
        public final String hexPressed;

        public Accent(int base, int hover, int pressed, int tint) {
            this.base = new Color(base);
            this.hover = new Color(hover);
            this.pressed = new Color(pressed);
            this.tint = new Color(tint);
            this.hex = String.format("#%06X", base);
            this.hexHover = String.format("#%06X", hover);
            this.hexPressed = String.format("#%06X", pressed);
        }
    }

    /** Verde material (el mismo del login/tema): modulo Ingresos. */
    public static final Accent INGRESOS = new Accent(0x2E7D32, 0x388E3C, 0x1B5E20, 0xE8F5E9);
    /** Rojo material: modulo Egresos. */
    public static final Accent EGRESOS = new Accent(0xC62828, 0xD32F2F, 0xB71C1C, 0xFDECEA);
    /** Azul material: modulo Traslados (el dinero no entra ni sale, se mueve). */
    public static final Accent TRASLADOS = new Accent(0x1565C0, 0x1976D2, 0x0D47A1, 0xE7F0FB);
    /** Indigo: pantalla de Reportes de caja (cubre ingresos y egresos a la vez). */
    public static final Accent REPORTES = new Accent(0x4F46E5, 0x4338CA, 0x3730A3, 0xEEF0FE);

    // ---- Semanticos (solo estados) ----
    public static final Color INFO = new Color(0x175CD3);
    public static final Color INFO_BG = new Color(0xE9F0FD);
    public static final Color OK = new Color(0x15803D);
    public static final Color OK_BG = new Color(0xE7F6EC);
    public static final Color WARN = new Color(0xB45309);
    public static final Color WARN_BG = new Color(0xFDF0DC);

    // ---- Colores de acento de fondos (para los puntos/leyendas) ----
    public static final Color[] FONDO_COLORS = {
        new Color(0x4F46E5), new Color(0x0EA5E9), new Color(0xF59E0B),
        new Color(0x10B981), new Color(0xEC4899), new Color(0x8B5CF6)
    };

    private static final Cursor HAND = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    private EstiloCaja() {
    }

    public static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    // =========================================================
    //  Tarjeta con esquinas redondeadas y borde sutil
    // =========================================================
    public static class Card extends JPanel {

        private final int arc;
        private Color accentTop;
        private int accentHeight = 4;

        public Card(LayoutManager lm, int arc) {
            super(lm);
            this.arc = arc;
            setOpaque(false);
            setBackground(SURFACE);
        }

        /** Pinta una franja de color de identidad en el borde superior. */
        public void setAccentTop(Color c) {
            this.accentTop = c;
            repaint();
        }

        public void setAccentTop(Color c, int height) {
            this.accentTop = c;
            this.accentHeight = height;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1, h = getHeight() - 1;
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            if (accentTop != null) {
                java.awt.Shape old = g2.getClip();
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, arc, arc));
                g2.setColor(accentTop);
                g2.fillRect(0, 0, w + 1, accentHeight);
                g2.setClip(old);
            }
            g2.setColor(LINE);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, w, h, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Barra vertical de color de identidad (para acompanar un titulo). */
    public static JPanel accentBar(Color c, int width, int height) {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getForeground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getWidth(), getWidth());
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setForeground(c);
        p.setPreferredSize(new Dimension(width, height));
        p.setMaximumSize(new Dimension(width, height));
        return p;
    }

    public static Card card(LayoutManager lm) {
        return new Card(lm, 16);
    }

    /**
     * Panel para formularios dentro de un JScrollPane: obliga al contenido a
     * usar exactamente el ancho del viewport, de modo que los campos nunca se
     * salgan/corten horizontalmente.
     */
    public static class FormPanel extends JPanel implements javax.swing.Scrollable {

        public FormPanel() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return orientation == javax.swing.SwingConstants.VERTICAL ? r.height : r.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true; // clave: el contenido se ajusta al ancho disponible
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // =========================================================
    //  Etiquetas
    // =========================================================
    public static JLabel title(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    public static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.PLAIN, 12));
        l.setForeground(TEXT_2);
        return l;
    }

    public static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(font(Font.BOLD, 11));
        l.setForeground(TEXT_4);
        return l;
    }

    // =========================================================
    //  Botones
    // =========================================================
    public static JButton primary(String text, String glyph, Accent a) {
        JButton b = new JButton(text, glyph != null ? FontAwesome.icon(glyph, 14f, Color.WHITE) : null);
        b.setFont(font(Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setIconTextGap(8);
        b.setCursor(HAND);
        b.setFocusPainted(false);
        b.putClientProperty(FlatClientProperties.STYLE,
                "background:" + a.hex + "; foreground:#ffffff; borderWidth:0; focusWidth:0; innerFocusWidth:0;"
                + " arc:10; hoverBackground:" + a.hexHover + "; pressedBackground:" + a.hexPressed + "; margin:8,16,8,16;");
        return b;
    }

    public static JButton ghost(String text, String glyph) {
        JButton b = new JButton(text, glyph != null ? FontAwesome.icon(glyph, 14f, TEXT_2) : null);
        b.setFont(font(Font.PLAIN, 13));
        b.setForeground(TEXT_2);
        b.setIconTextGap(8);
        b.setCursor(HAND);
        b.setFocusPainted(false);
        b.putClientProperty(FlatClientProperties.STYLE,
                "background:#ffffff; foreground:#5B616E; borderColor:#E3E5E9; borderWidth:1; focusWidth:0;"
                + " arc:10; hoverBackground:#F4F5F7; hoverBorderColor:#CBD0D8; margin:8,14,8,14;");
        return b;
    }

    /** Boton solo icono (acciones discretas de la tabla). */
    public static JButton iconButton(String glyph, String tooltip) {
        JButton b = new JButton(FontAwesome.icon(glyph, 15f, TEXT_2));
        b.setToolTipText(tooltip);
        b.setCursor(HAND);
        b.setFocusPainted(false);
        b.putClientProperty(FlatClientProperties.STYLE,
                "background:#ffffff; borderColor:#E3E5E9; borderWidth:1; focusWidth:0;"
                + " arc:8; hoverBackground:#F4F5F7; hoverBorderColor:#CBD0D8; margin:7,9,7,9;");
        return b;
    }

    /** Boton "+" cuadrado, tinte del color de identidad (crear contacto, etc.). */
    public static JButton plusButton(String glyph, String tooltip, Accent a) {
        JButton b = new JButton(FontAwesome.icon(glyph, 14f, a.base));
        b.setToolTipText(tooltip);
        b.setCursor(HAND);
        b.setFocusPainted(false);
        b.putClientProperty(FlatClientProperties.STYLE,
                "background:#ffffff; borderColor:#E3E5E9; borderWidth:1; focusWidth:0;"
                + " arc:10; hoverBackground:" + String.format("#%02X%02X%02X", a.tint.getRed(), a.tint.getGreen(), a.tint.getBlue())
                + "; hoverBorderColor:" + a.hex + "; margin:8,10,8,10;");
        return b;
    }

    // =========================================================
    //  Campos
    // =========================================================
    public static void styleField(JTextField f, String placeholder, String glyph, Accent a) {
        f.setFont(font(Font.PLAIN, 14));
        f.putClientProperty(FlatClientProperties.STYLE,
                "arc:10; focusedBorderColor:" + a.hex + "; borderColor:#E3E5E9; margin:7,10,7,10;");
        if (placeholder != null) {
            f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        }
        if (glyph != null) {
            f.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                    FontAwesome.icon(glyph, 14f, TEXT_3));
        }
    }

    public static void styleCombo(JComboBox<?> c, Accent a) {
        c.setFont(font(Font.PLAIN, 13));
        c.setMaximumRowCount(12);
        c.putClientProperty(FlatClientProperties.STYLE,
                "arc:10; focusedBorderColor:" + a.hex + "; borderColor:#E3E5E9; padding:2,6,2,6;");
    }

    public static void styleArea(JTextArea a) {
        a.setFont(font(Font.PLAIN, 13));
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }

    /** ScrollPane limpio (borde 1px redondeado gestionado por FlatLaf). */
    public static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        // OJO: FlatScrollPaneUI no soporta la propiedad de estilo "arc"
        // (lanza UnknownStyleException); el borde va con LineBorder.
        sp.setBorder(BorderFactory.createLineBorder(LINE_2, 1));
        sp.getViewport().setBackground(SURFACE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // =========================================================
    //  Tablas
    // =========================================================
    public static void styleTable(JTable t, Accent a) {
        t.setRowHeight(42);
        t.setFont(font(Font.PLAIN, 13));
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(LINE);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setForeground(TEXT);
        t.setBackground(SURFACE);
        t.setSelectionBackground(a.tint);
        t.setSelectionForeground(TEXT);
        t.setDefaultRenderer(Object.class, new BodyRenderer());

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new HeaderRenderer());
        h.setPreferredSize(new Dimension(0, 38));
    }

    /** Renderer del encabezado: fondo gris muy claro, texto pequeno gris. */
    public static class HeaderRenderer extends DefaultTableCellRenderer {

        public HeaderRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(value == null ? "" : value.toString().toUpperCase());
            setFont(font(Font.BOLD, 11));
            setForeground(TEXT_3);
            setBackground(SURFACE_2);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                    BorderFactory.createEmptyBorder(0, 14, 0, 14)));
            return this;
        }
    }

    /** Renderer del cuerpo: padding y color de texto consistentes. */
    public static class BodyRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            setFont(font(Font.BOLD, 13));
            setForeground(TEXT);
            return this;
        }
    }

    /** Renderer de dinero: alineado a la derecha, negrita, tabular. */
    public static class MoneyRenderer extends DefaultTableCellRenderer {

        public MoneyRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String v = value == null ? "" : value.toString().trim();
            // celda vacia se deja vacia (no "$" suelto)
            setText(v.isEmpty() ? "" : (v.startsWith("$") ? v : "$" + v));
            setFont(font(Font.BOLD, 13));
            setForeground(TEXT);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 16));
            return this;
        }
    }

    /** Renderer de texto atenuado (fecha, hora). */
    public static class MutedRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(font(Font.BOLD, 13));
            setForeground(TEXT_2);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            return this;
        }
    }

    /** Renderer tipo F/R: texto en color (F azul, R gris), negrita. */
    public static class TipoRenderer extends DefaultTableCellRenderer {

        private final Accent accent;

        public TipoRenderer(Accent accent) {
            this.accent = accent;
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String v = value == null ? "" : value.toString().trim();
            // F = Factura / E = Empresa se resaltan; R = Remision / P = Personal quedan neutros
            boolean destacado = v.equalsIgnoreCase("F") || v.equalsIgnoreCase("E");
            setText(v.toUpperCase());
            setFont(font(Font.BOLD, 12));
            setForeground(destacado ? INFO : TEXT_2);
            setBackground(isSelected ? accent.tint : SURFACE);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return this;
        }
    }

    /** Renderer Si/No: "Si" resaltado con el color de identidad, "No" atenuado. */
    public static class SiNoRenderer extends DefaultTableCellRenderer {

        private final Accent accent;

        public SiNoRenderer(Accent accent) {
            this.accent = accent;
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String v = value == null ? "" : value.toString().trim();
            boolean si = v.equalsIgnoreCase("Sí") || v.equalsIgnoreCase("Si") || "1".equals(v);
            setText(si ? "Sí" : "—");
            setFont(font(Font.BOLD, 12));
            setForeground(si ? accent.base : TEXT_4);
            setBackground(isSelected ? accent.tint : SURFACE);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return this;
        }
    }

    /** Etiqueta con forma de "chip" (fondo redondeado). */
    public static class ChipLabel extends JLabel {

        private final Color chipFg;
        private final Color chipBg;

        public ChipLabel(String text, Color fg, Color bg) {
            super(text);
            this.chipFg = fg;
            this.chipBg = bg;
            setFont(font(Font.BOLD, 11));
            setForeground(fg);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getFontMetrics(getFont()).stringWidth(getText()) + 22;
            return new Dimension(w, 24);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = Math.min(22, getHeight());
            int w = getFontMetrics(getFont()).stringWidth(getText()) + 20;
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;
            g2.setColor(chipBg);
            g2.fillRoundRect(x, y, w, h, h, h);
            g2.setColor(chipFg);
            g2.setFont(getFont());
            int tx = x + (w - g2.getFontMetrics().stringWidth(getText())) / 2;
            int ty = y + (h - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }

    /** Borde de padding reutilizable. */
    public static Border pad(int t, int l, int b, int r) {
        return BorderFactory.createEmptyBorder(t, l, b, r);
    }
}
