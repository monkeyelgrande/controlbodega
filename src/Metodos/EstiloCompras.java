package Metodos;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Sistema de estilos Material/FlatLaf para el módulo de Órdenes de Compra.
 * Reutiliza la paleta y los patrones de jif_crear_user e iconos Font Awesome
 * (package fonts) para mantener una estética consistente con el resto de la app.
 *
 * @author Monkeyelgrande
 */
public final class EstiloCompras {

    public static final Color PRIMARY = new Color(0x1565C0);
    public static final Color PRIMARY_DARK = new Color(0x0D47A1);
    public static final Color PRIMARY_HOVER = new Color(0x1976D2);
    public static final Color SUCCESS = new Color(0x2E7D32);
    public static final Color SUCCESS_HOVER = new Color(0x388E3C);
    public static final Color DANGER = new Color(0xC62828);
    public static final Color DANGER_HOVER = new Color(0xD32F2F);
    public static final Color BG_FORM = Color.WHITE;
    public static final Color BG_SECTION = new Color(0xFAFAFA);
    public static final Color TEXT_PRIMARY = new Color(0x212121);
    public static final Color TEXT_SECONDARY = new Color(0x757575);
    public static final Color DIVIDER = new Color(0xE0E0E0);

    private EstiloCompras() {
    }

    // ---------- Header con gradiente ----------
    public static JPanel gradientBar(int height) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, PRIMARY_DARK, getWidth(), 0, PRIMARY));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(10, height));
        return p;
    }

    /**
     * Header con icono Font Awesome + título y, opcionalmente, botón cerrar.
     */
    public static JPanel header(String faGlyph, String titulo, final Runnable onClose) {
        JPanel header = gradientBar(64);
        header.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 12));

        JLabel icon = new JLabel(FontAwesome.icon(faGlyph, 22f, Color.WHITE));
        JLabel title = new JLabel(titulo);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JPanel tw = new JPanel();
        tw.setOpaque(false);
        tw.setLayout(new BoxLayout(tw, BoxLayout.X_AXIS));
        tw.add(icon);
        tw.add(title);
        header.add(tw, BorderLayout.WEST);

        if (onClose != null) {
            JButton close = headerIconButton(FontAwesome.CLOSE);
            close.addActionListener(e -> onClose.run());
            header.add(close, BorderLayout.EAST);
        }
        return header;
    }

    public static JButton headerIconButton(String faGlyph) {
        final JButton b = new JButton(FontAwesome.icon(faGlyph, 18f, Color.WHITE));
        b.setOpaque(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------- Botones ----------
    private static void colorButton(final JButton b, final Color bg, final Color hover, Color fg) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(fg);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setIconTextGap(8);
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) {
                    b.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(bg);
            }
        });
    }

    public static void primaryButton(JButton b) {
        colorButton(b, PRIMARY, PRIMARY_HOVER, Color.WHITE);
    }

    public static void successButton(JButton b) {
        colorButton(b, SUCCESS, SUCCESS_HOVER, Color.WHITE);
    }

    public static void dangerButton(JButton b) {
        colorButton(b, DANGER, DANGER_HOVER, Color.WHITE);
    }

    public static void secondaryButton(final JButton b) {
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setForeground(PRIMARY);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIVIDER, 1),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setIconTextGap(8);
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) {
                    b.setBackground(BG_SECTION);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(Color.WHITE);
            }
        });
    }

    /** Botón primario con icono Font Awesome blanco. */
    public static JButton primaryBtn(String texto, String faGlyph) {
        JButton b = new JButton(texto, FontAwesome.icon(faGlyph, 14f, Color.WHITE));
        primaryButton(b);
        return b;
    }

    public static JButton successBtn(String texto, String faGlyph) {
        JButton b = new JButton(texto, FontAwesome.icon(faGlyph, 14f, Color.WHITE));
        successButton(b);
        return b;
    }

    public static JButton dangerBtn(String texto, String faGlyph) {
        JButton b = new JButton(texto, FontAwesome.icon(faGlyph, 14f, Color.WHITE));
        dangerButton(b);
        return b;
    }

    /** Botón secundario (contorno) con icono Font Awesome en color primario. */
    public static JButton secondaryBtn(String texto, String faGlyph) {
        JButton b = new JButton(texto, FontAwesome.icon(faGlyph, 14f, PRIMARY));
        secondaryButton(b);
        return b;
    }

    // ---------- Campos ----------
    public static JTextField field(String placeholder, String faGlyph) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setPreferredSize(new Dimension(0, 38));
        if (placeholder != null) {
            f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        }
        if (faGlyph != null) {
            f.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                    FontAwesome.icon(faGlyph, 15f, TEXT_SECONDARY));
        }
        return f;
    }

    public static void styleCombo(JComboBox combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(0, 38));
    }

    // ---------- Tablas ----------
    public static void styleTable(JTable t) {
        t.setRowHeight(40);
        t.setFont(new Font("Roboto", Font.PLAIN, 14));
        t.setShowVerticalLines(false);
        t.setGridColor(new Color(0xEEEEEE));
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setDefaultRenderer(new EstiloTablasHeader());
        t.setDefaultRenderer(Object.class, new EstiloTablasBody());
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(DIVIDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /** Fija anchos preferidos por columna (en orden). */
    public static void anchoColumnas(JTable t, int... widths) {
        for (int i = 0; i < widths.length && i < t.getColumnModel().getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** Oculta una columna sin quitarla del modelo (ancho 0). */
    public static void ocultarColumna(JTable t, int modelIndex) {
        TableColumn c = t.getColumnModel().getColumn(modelIndex);
        c.setMinWidth(0);
        c.setMaxWidth(0);
        c.setPreferredWidth(0);
    }

    /** Pinta la columna de estado con un color tipo "chip" por cada estado. */
    public static void aplicarEstadoRenderer(JTable t, int modelCol) {
        t.getColumnModel().getColumn(modelCol).setCellRenderer(new EstadoRenderer());
    }

    private static class EstadoRenderer extends DefaultTableCellRenderer {

        EstadoRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String v = value == null ? "" : value.toString().trim();
            setText(v);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            Color bg, fg;
            if ("Aprobada".equals(v)) {
                bg = new Color(0xE8F5E9);
                fg = new Color(0x2E7D32);
            } else if ("Pendiente".equals(v)) {
                bg = new Color(0xFFF8E1);
                fg = new Color(0xF57F17);
            } else if ("Rechazada".equals(v)) {
                bg = new Color(0xFFEBEE);
                fg = new Color(0xC62828);
            } else { // Borrador u otros
                bg = new Color(0xECEFF1);
                fg = new Color(0x546E7A);
            }
            setBackground(bg);
            setForeground(fg);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return this;
        }
    }

    // ---------- Etiquetas / layout ----------
    public static JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(PRIMARY);
        l.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
                BorderFactory.createEmptyBorder(0, 0, 6, 0)));
        return l;
    }

    public static JComponent labeled(String label, JComponent field, int fixedWidth) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        field.setAlignmentX(JComponent.LEFT_ALIGNMENT);

        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(field);

        if (fixedWidth > 0) {
            Dimension d = new Dimension(fixedWidth, 70);
            p.setMaximumSize(d);
            p.setPreferredSize(d);
        } else {
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        }
        return p;
    }
}
