package Metodos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

/**
 * Panel lateral con notificaciones en tiempo real — estilo Material Design
 * empresarial (paleta verde alineada con login). Tarjetas blancas con
 * elevation, header limpio, animacion de entrada con fade verde a blanco.
 *
 * Las notificaciones se anaden via {@link #agregarNotificacion}, llamado desde
 * {@link NotificacionesService} en el EDT (SwingUtilities.invokeLater).
 */
public class BarraNotificacionesPanel extends JPanel {

    private static final int MAX_NOTIFICACIONES = 20;

    // Paleta Material — sincronizada con login.java
    private static final Color PRIMARY        = new Color(0x2E, 0x7D, 0x32);
    private static final Color PRIMARY_DARK   = new Color(0x1B, 0x5E, 0x20);
    private static final Color PRIMARY_LIGHT  = new Color(0xC8, 0xE6, 0xC9);

    private static final Color BG_SURFACE     = new Color(0xF5, 0xF7, 0xF9);
    private static final Color BG_CARD        = Color.WHITE;
    private static final Color BG_CARD_HOVER  = new Color(0xF1, 0xF8, 0xE9);

    private static final Color TEXT_PRIMARY   = new Color(0x21, 0x21, 0x21);
    private static final Color TEXT_SECONDARY = new Color(0x61, 0x61, 0x61);
    private static final Color TEXT_HINT      = new Color(0x9E, 0x9E, 0x9E);
    private static final Color DIVIDER        = new Color(0xE0, 0xE0, 0xE0);

    private final JPanel listaPanel;
    private JLabel lblContador;
    private final SimpleDateFormat horaFmt = new SimpleDateFormat("HH:mm");

    /** Dedupe por id de cabecera. Cada cabecera = una tarjeta — incluso cuando
     *  wo-printer parte una factura en varias cabeceras (una por bodega), cada
     *  bodega genera su propia notificacion. Solo evita duplicados reales por
     *  reentregas del LISTEN. Para payloads sin id (NOVEDAD) se usa codigo. */
    private final LinkedHashSet<String> claves = new LinkedHashSet<>();

    public BarraNotificacionesPanel(int anchoPreferido) {
        setLayout(new BorderLayout());
        setBackground(BG_SURFACE);
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, DIVIDER));
        setPreferredSize(new Dimension(anchoPreferido, 100));

        add(buildHeader(), BorderLayout.NORTH);

        listaPanel = new JPanel();
        listaPanel.setLayout(new BoxLayout(listaPanel, BoxLayout.Y_AXIS));
        listaPanel.setBackground(BG_SURFACE);
        listaPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG_SURFACE);
        wrap.add(listaPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrap,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);

        actualizarContador();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
                new EmptyBorder(16, 18, 16, 14)));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel titulo = new JLabel("Notificaciones");
        titulo.setForeground(TEXT_PRIMARY);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JLabel subtitulo = new JLabel("Ordenes recientes");
        subtitulo.setForeground(TEXT_HINT);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        left.add(titulo);
        left.add(Box.createVerticalStrut(2));
        left.add(subtitulo);

        // Contador estilo "chip" Material
        lblContador = new JLabel("0") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblContador.setForeground(Color.WHITE);
        lblContador.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblContador.setOpaque(false);
        lblContador.setHorizontalAlignment(JLabel.CENTER);
        lblContador.setBorder(new EmptyBorder(4, 12, 4, 12));
        lblContador.setPreferredSize(new Dimension(36, 24));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(lblContador);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG_CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER),
                new EmptyBorder(10, 14, 10, 14)));

        JLabel info = new JLabel("Maximo " + MAX_NOTIFICACIONES);
        info.setForeground(TEXT_HINT);
        info.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        final JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.setBackground(BG_CARD);
        btnLimpiar.setForeground(PRIMARY);
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.setContentAreaFilled(false);
        btnLimpiar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 1),
                new EmptyBorder(5, 14, 5, 14)));
        btnLimpiar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnLimpiar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLimpiar.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btnLimpiar.setContentAreaFilled(true);
                btnLimpiar.setBackground(PRIMARY_LIGHT);
            }
            @Override public void mouseExited(MouseEvent e) {
                btnLimpiar.setContentAreaFilled(false);
            }
        });
        btnLimpiar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                listaPanel.removeAll();
                claves.clear();
                listaPanel.revalidate();
                listaPanel.repaint();
                actualizarContador();
            }
        });

        footer.add(info, BorderLayout.WEST);
        footer.add(btnLimpiar, BorderLayout.EAST);
        return footer;
    }

    /**
     * Inserta una notificacion arriba. Dedupe segun {@link #claves}.
     * Debe llamarse en el EDT.
     */
    public void agregarNotificacion(int numeroOrden, String numeroFactura,
                                    String cliente, String bodega) {
        String clave = (numeroOrden > 0)
                ? "ID:" + numeroOrden
                : "NF:" + (numeroFactura == null ? "" : numeroFactura);
        if (!claves.add(clave)) {
            return;
        }

        TarjetaNotificacion tarjeta = new TarjetaNotificacion(
                numeroOrden, numeroFactura, cliente, bodega);
        listaPanel.add(tarjeta, 0);
        listaPanel.add(Box.createVerticalStrut(8), 1);

        // Recortar al maximo (cada notificacion ocupa 2 componentes: tarjeta + strut)
        while (contarTarjetas() > MAX_NOTIFICACIONES) {
            int last = listaPanel.getComponentCount() - 1;
            if (last >= 0) listaPanel.remove(last);
            last = listaPanel.getComponentCount() - 1;
            if (last >= 0) listaPanel.remove(last);
        }

        listaPanel.revalidate();
        listaPanel.repaint();
        actualizarContador();
        lanzarFade(tarjeta);
    }

    private int contarTarjetas() {
        int c = 0;
        for (Component cmp : listaPanel.getComponents()) {
            if (cmp instanceof TarjetaNotificacion) c++;
        }
        return c;
    }

    private void actualizarContador() {
        if (lblContador != null) {
            lblContador.setText(String.valueOf(contarTarjetas()));
        }
    }

    /**
     * Animacion de bienvenida: la tarjeta aparece con un tinte verde suave
     * (PRIMARY_LIGHT) que se desvanece a blanco en 2.5s. Mucho mas sobrio
     * que el flash policromatico anterior.
     */
    private void lanzarFade(final TarjetaNotificacion tarjeta) {
        final int duracionMs = 2500;
        final int frameMs = 40;
        final int totalFrames = duracionMs / frameMs;
        final int[] frame = {0};
        Timer t = new Timer(frameMs, null);
        t.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (frame[0] >= totalFrames) {
                    tarjeta.setHighlight(BG_CARD);
                    t.stop();
                    return;
                }
                float p = (float) frame[0] / (float) totalFrames;
                Color mezcla = lerp(PRIMARY_LIGHT, BG_CARD, easeOutCubic(p));
                tarjeta.setHighlight(mezcla);
                frame[0]++;
            }
        });
        t.setInitialDelay(0);
        t.start();
    }

    private static Color lerp(Color a, Color b, float t) {
        if (t < 0) t = 0; if (t > 1) t = 1;
        int r = Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl= Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
        return new Color(r, g, bl);
    }

    private static float easeOutCubic(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** Hook util para pruebas. */
    public void agregarDemo() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                int n = 1000 + (int) (Math.random() * 999);
                agregarNotificacion(n, "FE-" + n, "Cliente Demo " + n, "Principal");
            }
        });
    }

    // ------------------------------------------------------------------------
    // Tarjeta Material con esquinas redondeadas, sombra suave y border-left.
    // ------------------------------------------------------------------------
    private class TarjetaNotificacion extends JPanel {

        private static final int ARC = 10;
        private static final int SHADOW = 4;
        private static final int ACCENT_W = 4;
        private Color highlight = BG_CARD;

        TarjetaNotificacion(int numeroOrden, String numeroFactura,
                            String cliente, String bodega) {
            setOpaque(false);
            setLayout(new GridBagLayout());
            setAlignmentX(Component.LEFT_ALIGNMENT);
            // Padding interior + espacio para sombra y borde de acento
            setBorder(new EmptyBorder(12, 14 + ACCENT_W, 14, 14));

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.gridx = 0;
            gc.gridy = 0;
            gc.insets = new Insets(0, 0, 6, 0);

            // Encabezado: titulo + hora
            JPanel encabezado = new JPanel(new BorderLayout());
            encabezado.setOpaque(false);

            JLabel lblTit = new JLabel("Orden #" + numeroOrden);
            lblTit.setForeground(PRIMARY_DARK);
            lblTit.setFont(new Font("Segoe UI", Font.BOLD, 13));

            JLabel lblHora = new JLabel(horaFmt.format(new Date()));
            lblHora.setForeground(TEXT_HINT);
            lblHora.setFont(new Font("Segoe UI", Font.PLAIN, 11));

            encabezado.add(lblTit, BorderLayout.WEST);
            encabezado.add(lblHora, BorderLayout.EAST);
            add(encabezado, gc);

            gc.gridy++;
            gc.insets = new Insets(0, 0, 3, 0);
            add(linea("Factura", numeroFactura == null ? "—" : numeroFactura), gc);
            gc.gridy++;
            add(linea("Cliente", cliente == null ? "—" : cliente), gc);
            gc.gridy++;
            gc.insets = new Insets(0, 0, 0, 0);
            add(linea("Bodega",  bodega == null ? "—" : bodega), gc);

            // Hover: leve background verde
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    setHighlight(BG_CARD_HOVER);
                }
                @Override public void mouseExited(MouseEvent e) {
                    setHighlight(BG_CARD);
                }
            });

            setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        }

        void setHighlight(Color c) {
            this.highlight = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Sombra suave (capas semitransparentes)
            for (int i = 0; i < SHADOW; i++) {
                int alpha = 12 - i * 2;
                if (alpha < 2) alpha = 2;
                g2.setColor(new Color(0, 0, 0, alpha));
                g2.fillRoundRect(i, i + 1, w - i * 2, h - i - 1, ARC, ARC);
            }

            // Cuerpo de la tarjeta
            g2.setColor(highlight);
            g2.fillRoundRect(0, 0, w - SHADOW, h - SHADOW, ARC, ARC);

            // Acento lateral izquierdo (verde primario)
            g2.setColor(PRIMARY);
            g2.fillRoundRect(0, 0, ACCENT_W + ARC, h - SHADOW, ARC, ARC);
            g2.fillRect(ACCENT_W, 0, 2, h - SHADOW); // tapa la curva derecha del acento

            g2.dispose();
            // Sin super.paintComponent — opaco controlado por nosotros
        }

        private JLabel linea(String etiqueta, String valor) {
            JLabel l = new JLabel("<html>"
                    + "<span style='color:#9e9e9e; font-size:10px;'>"
                    + escape(etiqueta.toUpperCase()) + "</span>"
                    + "&nbsp;&nbsp;"
                    + "<span style='color:#212121;'>" + escape(valor) + "</span>"
                    + "</html>");
            l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            return l;
        }

        private String escape(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
