package Formularios;

import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBconfiguraciones;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import modelos.Configuraciones;

/**
 * Diálogo de configuración con estilo Material / empresarial.
 *
 * El layout se construye manualmente; no usa NetBeans Form Editor (no existe .form).
 */
public class jd_configuraciones extends JDialog {

    // ---------- paleta corporativa ----------
    private static final Color PRIMARY       = new Color(0x1565C0);
    private static final Color PRIMARY_DARK  = new Color(0x0D47A1);
    private static final Color PRIMARY_HOVER = new Color(0x1976D2);
    private static final Color BG            = new Color(0xF4F6FA);
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color BORDER        = new Color(0xE2E6EE);
    private static final Color FIELD_BORDER  = new Color(0xCBD2DC);
    private static final Color TEXT          = new Color(0x1F2937);
    private static final Color TEXT_MUTED    = new Color(0x6B7280);
    private static final Color FIELD_BG      = new Color(0xF9FAFC);

    // ---------- tipografía ----------
    private static final Font  TITLE_FONT    = new Font("Segoe UI Semibold", Font.PLAIN, 22);
    private static final Font  SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  CARD_TITLE    = new Font("Segoe UI Semibold", Font.PLAIN, 15);
    private static final Font  CARD_HINT     = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font  LABEL_FONT    = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font  FIELD_FONT    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  BUTTON_FONT   = new Font("Segoe UI Semibold", Font.PLAIN, 13);

    // ---------- componentes ----------
    private JTextField txtNombre, txtNit, txtTel1, txtTel2, txtDireccion;
    private JTextField txtVenta, txtMayorista, txtCredito, txtIva;
    private JTextField txtImpresora, txtImpresora80;
    private JTextArea  txtPieLegal, txtServicios;
    private JRadioButton rbtnImprimirSi, rbtnImprimirNo;
    private JRadioButton rbtnRepetirSi,  rbtnRepetirNo;
    private JButton btnGuardar, btnCancelar;

    public jd_configuraciones(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        buildUi();
        if (DB_consultas_R_D.consultar_existencia_campo_String("id", "1", "configuraciones") == 1) {
            cargarCampos();
        }
        metodos.addEscapeListenerWindowDialog(this);
        setLocationRelativeTo(parent);
    }

    // ===========================================================
    //                            UI
    // ===========================================================
    private void buildUi() {
        setTitle("Configuración del sistema");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildContent(), BorderLayout.CENTER);
        root.add(buildFooter(),  BorderLayout.SOUTH);

        setContentPane(root);
        setSize(960, 740);
        setMinimumSize(new Dimension(960, 600));
    }

    private JComponent buildHeader() {
        GradientPanel header = new GradientPanel(PRIMARY, PRIMARY_DARK);
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(22, 30, 22, 30));

        JLabel title = new JLabel("Configuración del sistema");
        title.setFont(TITLE_FONT);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Parámetros generales del negocio, precios, impresión y mensajes");
        subtitle.setFont(SUBTITLE_FONT);
        subtitle.setForeground(new Color(255, 255, 255, 210));

        JPanel txt = new JPanel();
        txt.setOpaque(false);
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        txt.add(title);
        txt.add(Box.createVerticalStrut(4));
        txt.add(subtitle);

        header.add(txt, BorderLayout.WEST);
        return header;
    }

    private JComponent buildContent() {
        JPanel content = new JPanel();
        content.setBackground(BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(22, 24, 16, 24));

        // --- Datos del negocio ---
        txtNombre    = textField();
        txtNit       = textField();
        txtTel1      = textField();
        txtTel2      = textField();
        txtDireccion = textField();
        Card cNeg = new Card("Datos del negocio",
                "Información que aparecerá en facturas y reportes");
        cNeg.addRow("Nombre del negocio", txtNombre, "NIT", txtNit);
        cNeg.addRow("Teléfono 1", txtTel1, "Teléfono 2", txtTel2);
        cNeg.addFullRow("Dirección", txtDireccion);

        // --- Precios e impuestos ---
        txtVenta     = numberField();
        txtMayorista = numberField();
        txtCredito   = numberField();
        txtIva       = numberField();
        Card cPre = new Card("Precios e impuestos",
                "Porcentajes aplicados al cálculo de precios y tributos");
        cPre.addRow("Utilidad venta (%)",     txtVenta,    "Utilidad mayorista (%)", txtMayorista);
        cPre.addRow("Utilidad crédito (%)",   txtCredito,  "IVA (%)",                txtIva);

        // --- Impresoras ---
        txtImpresora   = textField();
        txtImpresora80 = textField();
        Card cImp = new Card("Impresoras",
                "Dispositivos predeterminados para imprimir documentos");
        cImp.addRow("Impresora carta", txtImpresora, "Impresora venta 80mm", txtImpresora80);

        // --- Comportamiento ---
        rbtnImprimirSi = radio("Imprimir factura automáticamente al guardar");
        rbtnImprimirNo = radio("No imprimir automáticamente");
        ButtonGroup bg1 = new ButtonGroup();
        bg1.add(rbtnImprimirSi); bg1.add(rbtnImprimirNo);

        rbtnRepetirSi = radio("Permitir repetir el mismo producto en una factura");
        rbtnRepetirNo = radio("No permitir productos repetidos");
        ButtonGroup bg2 = new ButtonGroup();
        bg2.add(rbtnRepetirSi); bg2.add(rbtnRepetirNo);

        Card cComp = new Card("Comportamiento",
                "Reglas operativas durante la facturación");
        cComp.addOptionGroup("Impresión automática",      rbtnImprimirSi, rbtnImprimirNo);
        cComp.addOptionGroup("Repetición de productos",   rbtnRepetirSi,  rbtnRepetirNo);

        // --- Mensajes ---
        txtPieLegal  = textArea();
        txtServicios = textArea();
        Card cMsg = new Card("Mensajes en facturas",
                "Texto adicional que se imprime en cada factura");
        cMsg.addAreaPair("Pie legal", txtPieLegal, "Servicios", txtServicios);

        addCard(content, cNeg);
        addCard(content, cPre);
        addCard(content, cImp);
        addCard(content, cComp);
        addCard(content, cMsg);

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.setBackground(BG);
        sp.getViewport().setBackground(BG);
        return sp;
    }

    private void addCard(JPanel parent, Card c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(c);
        parent.add(Box.createVerticalStrut(16));
    }

    private JComponent buildFooter() {
        JPanel f = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        f.setBackground(CARD_BG);
        f.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));

        btnCancelar = new MaterialButton("Cancelar", false);
        btnCancelar.addActionListener(e -> dispose());

        btnGuardar = new MaterialButton("Guardar cambios", true);
        btnGuardar.addActionListener(e -> guardar());

        f.add(btnCancelar);
        f.add(btnGuardar);
        return f;
    }

    // ===========================================================
    //                        Lógica
    // ===========================================================
    public void cargarCampos() {
        ResultSet rs = DB_consultas_R_D.getTabla("select * from configuraciones where id = 1");
        try {
            while (rs.next()) {
                txtNombre.setText(safe(rs.getString("nombre_negocio")));
                txtNit.setText(safe(rs.getString("nit_negocio")));
                txtTel1.setText(safe(rs.getString("contacto_negocio")));
                txtTel2.setText(safe(rs.getString("contacto2_negocio")));
                txtVenta.setText(safe(rs.getString("utilidad_venta")));
                txtMayorista.setText(safe(rs.getString("utilidad_mayorista")));
                txtCredito.setText(safe(rs.getString("utilidad_credito")));
                txtIva.setText(safe(rs.getString("iva")));
                txtDireccion.setText(safe(rs.getString("direccion")));
                txtImpresora.setText(safe(rs.getString("nombre_impresora")));
                txtImpresora80.setText(safe(rs.getString("impresora_venta_80mm")));
                txtPieLegal.setText(safe(rs.getString("pie_legal")));
                txtServicios.setText(safe(rs.getString("servicios")));

                if (rs.getInt("imprimir_factura") == 1) rbtnImprimirSi.setSelected(true);
                else                                    rbtnImprimirNo.setSelected(true);

                if (rs.getInt("productos_repetidos") == 1) rbtnRepetirSi.setSelected(true);
                else                                       rbtnRepetirNo.setSelected(true);
            }
            rs.close();
        } catch (SQLException ex) {
            Logger.getLogger(jd_configuraciones.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void guardar() {
        if (txtNombre.getText().trim().isEmpty()
                || txtNit.getText().trim().isEmpty()
                || txtTel1.getText().trim().isEmpty()
                || txtTel2.getText().trim().isEmpty()
                || txtDireccion.getText().trim().isEmpty()
                || txtVenta.getText().trim().isEmpty()
                || txtMayorista.getText().trim().isEmpty()
                || txtCredito.getText().trim().isEmpty()
                || txtIva.getText().trim().isEmpty()
                || (!rbtnImprimirSi.isSelected() && !rbtnImprimirNo.isSelected())
                || (!rbtnRepetirSi.isSelected()  && !rbtnRepetirNo.isSelected())) {
            JOptionPane.showMessageDialog(this,
                    "Debe completar todos los campos marcados.",
                    "Datos incompletos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Configuraciones config = new Configuraciones();
        config.setNombre_negocio(txtNombre.getText().trim());
        config.setNit_negocio(txtNit.getText().trim());
        config.setContacto_Negocio(txtTel1.getText().trim());
        config.setContacto2_Negocio(txtTel2.getText().trim());
        config.setUtilida_venta(parseInt(txtVenta.getText()));
        config.setUtilida_mayorista(parseInt(txtMayorista.getText()));
        config.setUtilida_credito(parseInt(txtCredito.getText()));
        config.setIva(parseInt(txtIva.getText()));
        config.setDireccion(txtDireccion.getText().trim());
        config.setNombre_impresota(txtImpresora.getText().trim());
        config.setImpresora_venta_80mm(txtImpresora80.getText().trim());
        config.setPie_legal(txtPieLegal.getText());
        config.setServicios(txtServicios.getText());
        config.setImprimir_factura(rbtnImprimirSi.isSelected() ? 1 : 0);
        config.setProductos_repetidos(rbtnRepetirSi.isSelected() ? 1 : 0);

        int r = new DBconfiguraciones().Guardar(config);
        if (r > 0) {
            JOptionPane.showMessageDialog(this,
                    "La configuración se guardó correctamente.",
                    "Configuración actualizada",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        }
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // ===========================================================
    //                Fábricas de componentes
    // ===========================================================
    private JTextField textField() {
        JTextField f = new JTextField();
        styleField(f);
        return f;
    }

    private JTextField numberField() {
        JTextField f = textField();
        f.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                DB_consultas_R_D.validar_numeros(e, c);
            }
        });
        return f;
    }

    private JTextArea textArea() {
        JTextArea a = new JTextArea(4, 20);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(FIELD_FONT);
        a.setForeground(TEXT);
        a.setBackground(FIELD_BG);
        a.setBorder(new EmptyBorder(8, 10, 8, 10));
        return a;
    }

    private void styleField(JTextField f) {
        f.setFont(FIELD_FONT);
        f.setForeground(TEXT);
        f.setBackground(FIELD_BG);
        f.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(8, FIELD_BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)));
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                        new RoundBorder(8, PRIMARY, 2),
                        new EmptyBorder(5, 9, 5, 9)));
            }
            @Override public void focusLost(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                        new RoundBorder(8, FIELD_BORDER, 1),
                        new EmptyBorder(6, 10, 6, 10)));
            }
        });
    }

    private JRadioButton radio(String text) {
        JRadioButton r = new JRadioButton(text);
        r.setFont(FIELD_FONT);
        r.setForeground(TEXT);
        r.setBackground(CARD_BG);
        r.setFocusPainted(false);
        return r;
    }

    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LABEL_FONT);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    // ===========================================================
    //                  Clases auxiliares
    // ===========================================================
    /** Tarjeta blanca con título, descripción y grid interno. */
    private static class Card extends JPanel {
        private final JPanel grid = new JPanel(new GridBagLayout());
        private int row = 0;

        Card(String title, String hint) {
            setLayout(new BorderLayout());
            setBackground(CARD_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    new RoundBorder(10, BORDER, 1),
                    new EmptyBorder(18, 22, 18, 22)));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            JLabel lblT = new JLabel(title);
            lblT.setFont(CARD_TITLE);
            lblT.setForeground(TEXT);

            JLabel lblH = new JLabel(hint);
            lblH.setFont(CARD_HINT);
            lblH.setForeground(TEXT_MUTED);

            JPanel head = new JPanel();
            head.setOpaque(false);
            head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
            lblT.setAlignmentX(LEFT_ALIGNMENT);
            lblH.setAlignmentX(LEFT_ALIGNMENT);
            head.add(lblT);
            head.add(Box.createVerticalStrut(2));
            head.add(lblH);
            head.add(Box.createVerticalStrut(14));

            grid.setOpaque(false);

            add(head, BorderLayout.NORTH);
            add(grid, BorderLayout.CENTER);
        }

        /** Dos campos en una fila (etiqueta sobre el campo). */
        void addRow(String l1, JComponent c1, String l2, JComponent c2) {
            addCell(l1, c1, 0, row, 1.0);
            addCell(l2, c2, 1, row, 1.0);
            row++;
        }

        /** Un solo campo a todo el ancho. */
        void addFullRow(String label, JComponent c) {
            GridBagConstraints g = baseGbc(0, row);
            g.gridwidth = 2;
            g.weightx = 1.0;
            JPanel cell = labeledCell(label, c);
            grid.add(cell, g);
            row++;
        }

        /** Bloque de opciones (dos radios uno debajo del otro). */
        void addOptionGroup(String label, JRadioButton r1, JRadioButton r2) {
            JLabel l = fieldLabel(label);
            GridBagConstraints lg = baseGbc(0, row);
            lg.gridwidth = 2;
            lg.insets = new Insets(8, 0, 4, 0);
            grid.add(l, lg);
            row++;

            JPanel box = new JPanel();
            box.setOpaque(false);
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            r1.setAlignmentX(LEFT_ALIGNMENT);
            r2.setAlignmentX(LEFT_ALIGNMENT);
            box.add(r1);
            box.add(Box.createVerticalStrut(2));
            box.add(r2);

            GridBagConstraints bg = baseGbc(0, row);
            bg.gridwidth = 2;
            bg.insets = new Insets(0, 0, 6, 0);
            grid.add(box, bg);
            row++;
        }

        /** Dos áreas de texto lado a lado con scroll. */
        void addAreaPair(String l1, JTextArea a1, String l2, JTextArea a2) {
            addAreaCell(l1, a1, 0, row);
            addAreaCell(l2, a2, 1, row);
            row++;
        }

        private void addCell(String label, JComponent comp, int col, int rowIx, double weight) {
            GridBagConstraints g = baseGbc(col, rowIx);
            g.weightx = weight;
            grid.add(labeledCell(label, comp), g);
        }

        private void addAreaCell(String label, JTextArea area, int col, int rowIx) {
            JScrollPane sp = new JScrollPane(area);
            sp.setBorder(new RoundBorder(8, FIELD_BORDER, 1));
            sp.getViewport().setBackground(area.getBackground());
            sp.setPreferredSize(new Dimension(10, 110));
            GridBagConstraints g = baseGbc(col, rowIx);
            g.weightx = 1.0;
            g.fill = GridBagConstraints.BOTH;
            grid.add(labeledCell(label, sp), g);
        }

        private static JPanel labeledCell(String label, JComponent comp) {
            JPanel p = new JPanel();
            p.setOpaque(false);
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            JLabel l = fieldLabel(label);
            l.setAlignmentX(LEFT_ALIGNMENT);
            comp.setAlignmentX(LEFT_ALIGNMENT);
            if (comp instanceof JTextField) {
                comp.setPreferredSize(new Dimension(10, 34));
                comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            }
            p.add(l);
            p.add(Box.createVerticalStrut(4));
            p.add(comp);
            return p;
        }

        private static GridBagConstraints baseGbc(int col, int rowIx) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = col;
            g.gridy = rowIx;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.anchor = GridBagConstraints.NORTHWEST;
            g.insets = new Insets(0, col == 0 ? 0 : 14, 12, 0);
            return g;
        }
    }

    /** Botón Material (filled o outlined) con esquinas redondeadas y hover. */
    private static class MaterialButton extends JButton {
        private final boolean filled;
        private boolean hover = false;

        MaterialButton(String text, boolean filled) {
            super(text);
            this.filled = filled;
            setFont(BUTTON_FONT);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 22, 8, 22));
            setForeground(filled ? Color.WHITE : PRIMARY);
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited (java.awt.event.MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 8;
            int w = getWidth(), h = getHeight();
            if (filled) {
                Color base = hover ? PRIMARY_HOVER : PRIMARY;
                g2.setColor(base);
                g2.fillRoundRect(0, 0, w, h, arc, arc);
            } else {
                if (hover) {
                    g2.setColor(new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 22));
                    g2.fillRoundRect(0, 0, w, h, arc, arc);
                }
                g2.setColor(PRIMARY);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Borde redondeado pintado sobre el componente. */
    private static class RoundBorder implements javax.swing.border.Border {
        private final int radius;
        private final Color color;
        private final int thickness;

        RoundBorder(int radius, Color color, int thickness) {
            this.radius = radius; this.color = color; this.thickness = thickness;
        }

        @Override public Insets getBorderInsets(Component c) {
            return new Insets(thickness + 1, thickness + 2, thickness + 1, thickness + 2);
        }
        @Override public boolean isBorderOpaque() { return false; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new java.awt.BasicStroke(thickness));
            g2.drawRoundRect(x + thickness / 2, y + thickness / 2,
                             w - thickness - 1, h - thickness - 1, radius, radius);
            g2.dispose();
        }
    }

    /** Panel con degradado vertical (header). */
    private static class GradientPanel extends JPanel {
        private final Color top, bottom;
        GradientPanel(Color top, Color bottom) {
            this.top = top; this.bottom = bottom; setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ===========================================================
    //                       main de prueba
    // ===========================================================
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        java.awt.EventQueue.invokeLater(() -> {
            jd_configuraciones d = new jd_configuraciones(new javax.swing.JFrame(), true);
            d.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent e) { System.exit(0); }
            });
            d.setVisible(true);
        });
    }
}
