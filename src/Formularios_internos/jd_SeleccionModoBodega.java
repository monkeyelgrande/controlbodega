package Formularios_internos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Dialogo tactil que se muestra al iniciar sesion cuando el perfil es
 * "bodeguero". Permite elegir entre el flujo clasico (frm_main) y el
 * modo Entregas Rapidas (pantalla tactil con lector QR).
 */
public class jd_SeleccionModoBodega extends JDialog {

    public enum Modo { ENTREGAS_RAPIDAS, ACCESO_NORMAL, CANCELADO }

    private static final Color PRIMARY      = new Color(0x2E7D32);
    private static final Color PRIMARY_DARK = new Color(0x1B5E20);
    private static final Color ACCENT       = new Color(0x1565C0);
    private static final Color ACCENT_DARK  = new Color(0x0D47A1);
    private static final Color BG           = Color.WHITE;
    private static final Color TEXT         = new Color(0x212121);
    private static final Color TEXT_SOFT    = new Color(0x616161);

    private static final int WIDTH_PX  = 720;
    private static final int HEIGHT_PX = 480;
    private static final int ARC       = 28;

    private Modo modo = Modo.CANCELADO;

    public jd_SeleccionModoBodega(Window parent, String nombreUsuario, String bodega) {
        super(parent, "", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setSize(WIDTH_PX, HEIGHT_PX);
        setLocationRelativeTo(parent);
        setShape(new RoundRectangle2D.Double(0, 0, WIDTH_PX, HEIGHT_PX, ARC, ARC));
        setContentPane(buildContent(nombreUsuario, bodega));

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                modo = Modo.CANCELADO;
                dispose();
            }
        });
    }

    public Modo getModo() {
        return modo;
    }

    private JPanel buildContent(String nombreUsuario, String bodega) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(buildHeader(nombreUsuario, bodega), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader(String nombreUsuario, String bodega) {
        JPanel header = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, PRIMARY_DARK, w, h, PRIMARY));
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(WIDTH_PX, 130));

        JLabel title = new JLabel("¿Cómo deseas trabajar hoy?");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);

        StringBuilder sub = new StringBuilder();
        if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
            sub.append(nombreUsuario);
        }
        if (bodega != null && !bodega.isEmpty()) {
            if (sub.length() > 0) sub.append("  •  ");
            sub.append("Bodega: ").append(bodega);
        }
        JLabel subtitle = new JLabel(sub.toString());
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(255, 255, 255, 220));
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 24, 4, 24);
        gc.gridy = 0;
        header.add(title, gc);
        gc.gridy = 1;
        gc.insets = new Insets(0, 24, 0, 24);
        header.add(subtitle, gc);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(BG);
        body.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        JButton btnRapidas = buildBigButton(
                "ENTREGAS RÁPIDAS",
                "Pantalla táctil con lector QR",
                ACCENT, ACCENT_DARK);
        btnRapidas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                modo = Modo.ENTREGAS_RAPIDAS;
                dispose();
            }
        });

        JButton btnNormal = buildBigButton(
                "ACCESO NORMAL",
                "Menú clásico de la aplicación",
                PRIMARY, PRIMARY_DARK);
        btnNormal.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                modo = Modo.ACCESO_NORMAL;
                dispose();
            }
        });

        JButton btnSalir = new JButton("Cancelar y volver al login");
        btnSalir.setFocusPainted(false);
        btnSalir.setContentAreaFilled(false);
        btnSalir.setBorderPainted(false);
        btnSalir.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSalir.setForeground(TEXT_SOFT);
        btnSalir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSalir.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                modo = Modo.CANCELADO;
                dispose();
            }
        });

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.insets = new Insets(0, 8, 0, 8);

        gc.gridx = 0; gc.gridy = 0;
        body.add(btnRapidas, gc);
        gc.gridx = 1;
        body.add(btnNormal, gc);

        gc.gridx = 0; gc.gridy = 1;
        gc.gridwidth = 2;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(18, 0, 0, 0);
        body.add(btnSalir, gc);

        return body;
    }

    private JButton buildBigButton(String titulo, String sub, final Color base, final Color hover) {
        final JButton b = new JButton("<html><div style='text-align:center;'>"
                + "<div style='font-size:18pt;font-weight:bold;'>" + titulo + "</div>"
                + "<div style='font-size:11pt;font-weight:normal;margin-top:8px;'>" + sub + "</div>"
                + "</div></html>");
        b.setForeground(Color.WHITE);
        b.setBackground(base);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(28, 16, 28, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(new Font("Segoe UI", Font.BOLD, 18));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(base); }
        });
        return b;
    }
}
