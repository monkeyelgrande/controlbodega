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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
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
    private static final Color BG_SURFACE     = new Color(0xF5, 0xF7, 0xF9);
    private static final Color BG_CARD        = Color.WHITE;

    private static final Color TEXT_PRIMARY   = new Color(0x21, 0x21, 0x21);
    private static final Color TEXT_SECONDARY = new Color(0x61, 0x61, 0x61);
    private static final Color TEXT_HINT      = new Color(0x9E, 0x9E, 0x9E);
    private static final Color DIVIDER        = new Color(0xE0, 0xE0, 0xE0);

    // Acentos por severidad
    private static final Color ROJO       = new Color(0xC6, 0x28, 0x28); // negativo
    private static final Color ROJO_BG    = new Color(0xFD, 0xEC, 0xEA);
    private static final Color AMBAR      = new Color(0xF5, 0x7F, 0x17); // agotado
    private static final Color AMBAR_BG   = new Color(0xFF, 0xF8, 0xE1);

    private final JPanel listaPanel;
    private JLabel lblContador;
    private JLabel lblEstado;
    private final DecimalFormat fmt = new DecimalFormat("#,##0.##");

    private Timer timer;
    private volatile boolean cargando = false;

    public PanelNotificacionesStock() {
        super("Alertas de inventario", true, true, true, true);
        setFrameIcon(null);
        setSize(360, 470);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_SURFACE);

        content.add(buildHeader(), BorderLayout.NORTH);

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
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
                new EmptyBorder(14, 16, 14, 14)));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel titulo = new JLabel("Productos sin stock");
        titulo.setForeground(TEXT_PRIMARY);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JLabel subtitulo = new JLabel("Agotados o en negativo");
        subtitulo.setForeground(TEXT_HINT);
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        left.add(titulo);
        left.add(Box.createVerticalStrut(2));
        left.add(subtitulo);

        lblContador = new JLabel("0") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ROJO);
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
        lblContador.setPreferredSize(new Dimension(40, 24));

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

        lblEstado = new JLabel("—");
        lblEstado.setForeground(TEXT_HINT);
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        final JButton btnActualizar = new JButton("Actualizar");
        btnActualizar.setBackground(BG_CARD);
        btnActualizar.setForeground(PRIMARY);
        btnActualizar.setFocusPainted(false);
        btnActualizar.setContentAreaFilled(false);
        btnActualizar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY, 1),
                new EmptyBorder(5, 14, 5, 14)));
        btnActualizar.setFont(new Font("Segoe UI", Font.BOLD, 11));
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
        listaPanel.removeAll();
        if (alertas == null || alertas.isEmpty()) {
            JLabel vacio = new JLabel("Sin productos agotados ni en negativo");
            vacio.setForeground(TEXT_SECONDARY);
            vacio.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            vacio.setAlignmentX(Component.LEFT_ALIGNMENT);
            vacio.setBorder(new EmptyBorder(16, 4, 16, 4));
            listaPanel.add(vacio);
        } else {
            for (Alerta a : alertas) {
                listaPanel.add(new Tarjeta(a));
                listaPanel.add(Box.createVerticalStrut(8));
            }
        }
        lblContador.setText(String.valueOf(alertas == null ? 0 : alertas.size()));
        lblEstado.setText("Actualizado " + new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));
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

    // Tarjeta Material con borde de acento por severidad
    private class Tarjeta extends JPanel {

        private static final int ARC = 10;
        private static final int SHADOW = 4;
        private static final int ACCENT_W = 4;

        private final Color acento;
        private final Color fondo;

        Tarjeta(Alerta a) {
            this.acento = a.esNegativo() ? ROJO : AMBAR;
            this.fondo = a.esNegativo() ? ROJO_BG : AMBAR_BG;

            setOpaque(false);
            setLayout(new GridBagLayout());
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(new EmptyBorder(12, 14 + ACCENT_W, 14, 14));

            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.gridx = 0;
            gc.gridy = 0;
            gc.insets = new Insets(0, 0, 6, 0);

            // Encabezado: codigo + chip de estado
            JPanel encabezado = new JPanel(new BorderLayout());
            encabezado.setOpaque(false);

            JLabel lblCod = new JLabel(a.codigo == null ? "—" : a.codigo);
            lblCod.setForeground(TEXT_PRIMARY);
            lblCod.setFont(new Font("Segoe UI", Font.BOLD, 13));

            JLabel chip = new JLabel(a.esNegativo() ? "NEGATIVO" : "AGOTADO");
            chip.setForeground(acento);
            chip.setFont(new Font("Segoe UI", Font.BOLD, 11));

            encabezado.add(lblCod, BorderLayout.WEST);
            encabezado.add(chip, BorderLayout.EAST);
            add(encabezado, gc);

            // Descripcion
            gc.gridy++;
            gc.insets = new Insets(0, 0, 6, 0);
            JLabel lblDesc = new JLabel("<html><div style='width:230px;'>"
                    + escape(a.descripcion == null ? "" : a.descripcion) + "</div></html>");
            lblDesc.setForeground(TEXT_SECONDARY);
            lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            add(lblDesc, gc);

            // Cantidad
            gc.gridy++;
            gc.insets = new Insets(0, 0, 0, 0);
            JLabel lblCant = new JLabel("Existencia: " + fmt.format(a.total));
            lblCant.setForeground(acento);
            lblCant.setFont(new Font("Segoe UI", Font.BOLD, 12));
            add(lblCant, gc);

            setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            for (int i = 0; i < SHADOW; i++) {
                int alpha = 12 - i * 2;
                if (alpha < 2) alpha = 2;
                g2.setColor(new Color(0, 0, 0, alpha));
                g2.fillRoundRect(i, i + 1, w - i * 2, h - i - 1, ARC, ARC);
            }

            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, w - SHADOW, h - SHADOW, ARC, ARC);

            g2.setColor(acento);
            g2.fillRoundRect(0, 0, ACCENT_W + ARC, h - SHADOW, ARC, ARC);
            g2.fillRect(ACCENT_W, 0, 2, h - SHADOW);

            g2.dispose();
        }

        private String escape(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
