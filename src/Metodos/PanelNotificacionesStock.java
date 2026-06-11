package Metodos;

import conexiondb.DB_consultas_R_D;
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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * Panel (JInternalFrame) que vive dentro del JDesktopPane de frm_main y muestra
 * tarjetas con productos agotados (stock total = 0) o en negativo (stock < 0).
 *
 * El stock se calcula global (suma de stock_productos de todas las bodegas) por
 * producto. Se refresca automaticamente cada {@link #REFRESCO_MS} y tambien con
 * el boton "Actualizar". La consulta corre en segundo plano (SwingWorker) y solo
 * el pintado de tarjetas ocurre en el EDT.
 *
 * Se activa por usuario con el flag users.panel_notificaciones.
 */
public class PanelNotificacionesStock extends JInternalFrame {

    private static final long REFRESCO_MS = 60_000L;

    // Paleta — alineada con login / BarraNotificacionesPanel
    private static final Color PRIMARY        = new Color(0x2E, 0x7D, 0x32);
    private static final Color BG_SURFACE     = new Color(0xEC, 0xEF, 0xF1);
    private static final Color BG_CARD        = Color.WHITE;
    private static final Color BG_CARD_HOVER  = new Color(0xFA, 0xFB, 0xFC);

    private static final Color TEXT_PRIMARY   = new Color(0x21, 0x21, 0x21);
    private static final Color TEXT_SECONDARY = new Color(0x60, 0x6A, 0x70);
    private static final Color TEXT_HINT      = new Color(0x9E, 0x9E, 0x9E);
    private static final Color DIVIDER        = new Color(0xE0, 0xE3, 0xE6);

    // Acentos por severidad
    private static final Color ROJO       = new Color(0xD3, 0x2F, 0x2F); // negativo
    private static final Color ROJO_BG    = new Color(0xFC, 0xE4, 0xE2);
    private static final Color AMBAR      = new Color(0xE6, 0x5C, 0x00); // agotado
    private static final Color AMBAR_BG   = new Color(0xFF, 0xEC, 0xD1);

    // Tipos de filtro disponibles en el combo
    private static final String F_TODOS    = "Todos";
    private static final String F_NEGATIVO = "Solo negativos";
    private static final String F_AGOTADO  = "Solo agotados";

    private final JPanel listaPanel;
    private Pill contador;
    private JLabel lblEstado;
    private javax.swing.JComboBox<String> filtroCombo;
    private final DecimalFormat fmt = new DecimalFormat("#,##0.##");

    // Ultima carga desde BD; el filtro se aplica sobre esta lista sin reconsultar.
    private List<Alerta> ultimaCarga = new ArrayList<>();

    private Timer timer;
    private volatile boolean cargando = false;

    public PanelNotificacionesStock() {
        super("Alertas de inventario", true, true, true, true);
        setFrameIcon(null);
        setSize(370, 480);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_SURFACE);

        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        norte.setOpaque(false);
        norte.add(buildHeader());
        norte.add(buildFiltro());
        content.add(norte, BorderLayout.NORTH);

        listaPanel = new JPanel();
        listaPanel.setLayout(new BoxLayout(listaPanel, BoxLayout.Y_AXIS));
        listaPanel.setBackground(BG_SURFACE);
        listaPanel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG_SURFACE);
        wrap.add(listaPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrap,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scroll, BorderLayout.CENTER);

        content.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(content);

        addInternalFrameListener(new InternalFrameAdapter() {
            @Override public void internalFrameClosed(InternalFrameEvent e) {
                detener();
            }
        });
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
                new EmptyBorder(16, 18, 16, 16)));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel titulo = new JLabel("Productos sin stock");
        titulo.setForeground(TEXT_PRIMARY);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitulo = new JLabel("Agotados o en negativo");
        subtitulo.setForeground(TEXT_HINT);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(titulo);
        left.add(Box.createVerticalStrut(3));
        left.add(subtitulo);

        contador = new Pill("0", ROJO, Color.WHITE, 13);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(contador);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildFiltro() {
        JPanel barra = new JPanel(new BorderLayout(10, 0));
        barra.setBackground(BG_CARD);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
                new EmptyBorder(8, 18, 8, 16)));
        barra.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel lbl = new JLabel("Mostrar");
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        filtroCombo = new javax.swing.JComboBox<>(
                new String[]{F_TODOS, F_NEGATIVO, F_AGOTADO});
        filtroCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filtroCombo.setBackground(BG_CARD);
        filtroCombo.setFocusable(false);
        filtroCombo.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                render();
            }
        });

        barra.add(lbl, BorderLayout.WEST);
        barra.add(filtroCombo, BorderLayout.CENTER);
        return barra;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG_CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER),
                new EmptyBorder(10, 16, 10, 14)));

        lblEstado = new JLabel("—");
        lblEstado.setForeground(TEXT_HINT);
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        final JButton btnActualizar = new JButton("Actualizar");
        btnActualizar.setForeground(PRIMARY);
        btnActualizar.setFocusPainted(false);
        btnActualizar.setContentAreaFilled(false);
        btnActualizar.setOpaque(false);
        btnActualizar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 1, true),
                new EmptyBorder(6, 16, 6, 16)));
        btnActualizar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnActualizar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnActualizar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                refrescar();
            }
        });

        footer.add(lblEstado, BorderLayout.WEST);
        footer.add(btnActualizar, BorderLayout.EAST);
        return footer;
    }

    /** Arranca la primera carga y el refresco periodico. */
    public void iniciar() {
        refrescar();
        if (timer == null) {
            timer = new Timer((int) REFRESCO_MS, new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    refrescar();
                }
            });
            timer.setRepeats(true);
        }
        timer.start();
    }

    /** Detiene el refresco periodico (al cerrar el panel). */
    public void detener() {
        if (timer != null) {
            timer.stop();
        }
    }

    /** Lanza una consulta en segundo plano y repinta las tarjetas en el EDT. */
    public void refrescar() {
        if (cargando) {
            return;
        }
        cargando = true;
        lblEstado.setText("Consultando…");
        new SwingWorker<List<Alerta>, Void>() {
            @Override protected List<Alerta> doInBackground() {
                return consultar();
            }
            @Override protected void done() {
                try {
                    pintar(get());
                } catch (Exception ex) {
                    lblEstado.setText("Error al consultar");
                } finally {
                    cargando = false;
                }
            }
        }.execute();
    }

    private List<Alerta> consultar() {
        List<Alerta> lista = new ArrayList<>();
        String sql = "SELECT p.codigo_barras, p.descripcion, "
                + "       COALESCE(SUM(sp.cantidad), 0) AS total "
                + "FROM productos p "
                + "LEFT JOIN stock_productos sp ON sp.id_producto = p.id "
                + "WHERE COALESCE(p.estado, true) = true "
                + "GROUP BY p.id, p.codigo_barras, p.descripcion "
                + "HAVING COALESCE(SUM(sp.cantidad), 0) <= 0 "
                + "ORDER BY total ASC, p.codigo_barras";
        Connection c = null;
        try {
            c = DB_consultas_R_D.getConexion();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    lista.add(new Alerta(
                            rs.getString("codigo_barras"),
                            rs.getString("descripcion"),
                            rs.getDouble("total")));
                }
            }
        } catch (Exception ex) {
            System.out.println("[PanelNotificacionesStock] Error: " + ex.getMessage());
        } finally {
            try { if (c != null) c.close(); } catch (Exception ignored) {}
        }
        return lista;
    }

    private void pintar(List<Alerta> alertas) {
        ultimaCarga = (alertas == null) ? new ArrayList<Alerta>() : alertas;
        lblEstado.setText("Actualizado "
                + new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));
        render();
    }

    /** Repinta la lista aplicando el filtro seleccionado sobre {@link #ultimaCarga}. */
    private void render() {
        String filtro = (filtroCombo == null) ? F_TODOS
                : String.valueOf(filtroCombo.getSelectedItem());

        List<Alerta> visibles = new ArrayList<>();
        for (Alerta a : ultimaCarga) {
            if (F_NEGATIVO.equals(filtro) && !a.esNegativo()) continue;
            if (F_AGOTADO.equals(filtro) && a.esNegativo()) continue;
            visibles.add(a);
        }

        listaPanel.removeAll();
        if (visibles.isEmpty()) {
            String msg = ultimaCarga.isEmpty()
                    ? "Sin productos agotados ni en negativo"
                    : "Sin productos para este filtro";
            JPanel vacio = new JPanel(new BorderLayout());
            vacio.setBackground(BG_SURFACE);
            vacio.setBorder(new EmptyBorder(40, 8, 40, 8));
            JLabel l = new JLabel(msg, JLabel.CENTER);
            l.setForeground(TEXT_SECONDARY);
            l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            vacio.add(l, BorderLayout.CENTER);
            listaPanel.add(vacio);
        } else {
            for (int i = 0; i < visibles.size(); i++) {
                if (i > 0) {
                    listaPanel.add(Box.createVerticalStrut(10));
                }
                listaPanel.add(new Tarjeta(visibles.get(i)));
            }
        }
        contador.setText(String.valueOf(visibles.size()));
        listaPanel.revalidate();
        listaPanel.repaint();
    }

    // Modelo de fila
    private static class Alerta {
        final String codigo;
        final String descripcion;
        final double total;
        Alerta(String codigo, String descripcion, double total) {
            this.codigo = codigo;
            this.descripcion = descripcion;
            this.total = total;
        }
        boolean esNegativo() { return total < 0; }
    }

    // ------------------------------------------------------------------------
    // Pill: etiqueta con fondo redondeado que se ajusta a su contenido.
    // ------------------------------------------------------------------------
    private static class Pill extends JLabel {
        private final Color fondo;
        Pill(String text, Color fondo, Color texto, int size) {
            super(text);
            this.fondo = fondo;
            setForeground(texto);
            setFont(new Font("Segoe UI", Font.BOLD, size));
            setHorizontalAlignment(CENTER);
            setBorder(new EmptyBorder(4, 13, 4, 13));
            setOpaque(false);
        }
        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            int min = d.height; // alto >= ancho minimo para que sea pill
            if (d.width < min) d.width = min;
            return d;
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ------------------------------------------------------------------------
    // Tarjeta Material: fondo blanco, sombra suave, acento lateral por severidad
    // y badge tipo pill (AGOTADO / NEGATIVO).
    // ------------------------------------------------------------------------
    private class Tarjeta extends JPanel {

        private static final int ARC = 12;
        private static final int SHADOW = 5;
        private static final int ACCENT_W = 5;

        private final Color acento;
        private Color fondo = BG_CARD;

        Tarjeta(Alerta a) {
            this.acento = a.esNegativo() ? ROJO : AMBAR;
            Color badgeBg = a.esNegativo() ? ROJO_BG : AMBAR_BG;

            setOpaque(false);
            setLayout(new GridBagLayout());
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(new EmptyBorder(13, 16 + ACCENT_W, 14, 14 + SHADOW));

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.gridx = 0;
            gc.gridy = 0;
            gc.insets = new Insets(0, 0, 7, 0);

            // Fila 1: codigo + badge severidad
            JPanel fila1 = new JPanel(new BorderLayout(8, 0));
            fila1.setOpaque(false);

            JLabel lblCod = new JLabel(a.codigo == null ? "—" : a.codigo);
            lblCod.setForeground(TEXT_PRIMARY);
            lblCod.setFont(new Font("Segoe UI", Font.BOLD, 14));

            Pill badge = new Pill(a.esNegativo() ? "NEGATIVO" : "AGOTADO",
                    badgeBg, acento, 10);

            JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            badgeWrap.setOpaque(false);
            badgeWrap.add(badge);

            fila1.add(lblCod, BorderLayout.WEST);
            fila1.add(badgeWrap, BorderLayout.EAST);
            add(fila1, gc);

            // Fila 2: descripcion (envuelve)
            gc.gridy++;
            gc.insets = new Insets(0, 0, 9, 0);
            JTextArea desc = new JTextArea(a.descripcion == null ? "" : a.descripcion);
            desc.setEditable(false);
            desc.setOpaque(false);
            desc.setFocusable(false);
            desc.setLineWrap(true);
            desc.setWrapStyleWord(true);
            desc.setBorder(null);
            desc.setForeground(TEXT_SECONDARY);
            desc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            add(desc, gc);

            // Fila 3: existencia
            gc.gridy++;
            gc.insets = new Insets(0, 0, 0, 0);
            JPanel fila3 = new JPanel(new BorderLayout());
            fila3.setOpaque(false);
            JLabel cap = new JLabel("Existencia");
            cap.setForeground(TEXT_HINT);
            cap.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            JLabel val = new JLabel(fmt.format(a.total));
            val.setForeground(acento);
            val.setFont(new Font("Segoe UI", Font.BOLD, 16));
            val.setHorizontalAlignment(JLabel.RIGHT);
            fila3.add(cap, BorderLayout.WEST);
            fila3.add(val, BorderLayout.EAST);
            add(fila3, gc);
        }

        @Override public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - SHADOW;
            int h = getHeight() - SHADOW;

            // Sombra suave
            for (int i = 0; i < SHADOW; i++) {
                int alpha = 10 - i * 2;
                if (alpha < 2) alpha = 2;
                g2.setColor(new Color(0, 0, 0, alpha));
                g2.fillRoundRect(SHADOW - i, SHADOW - i + 1, w, h, ARC, ARC);
            }

            // Cuerpo
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, w, h, ARC, ARC);

            // Acento lateral
            g2.setColor(acento);
            g2.fillRoundRect(0, 0, ACCENT_W + ARC, h, ARC, ARC);
            g2.fillRect(ACCENT_W, 0, 2, h);

            g2.dispose();
        }
    }
}
