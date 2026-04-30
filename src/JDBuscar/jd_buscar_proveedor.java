package JDBuscar;

import conexiondb.DB_consultas_R_D;
import modelos.Contactos;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.ResultSet;

/**
 * Dialogo de busqueda de proveedores con estilo Material Design.
 *
 * Muestra unicamente contactos donde proveedor = 1.
 * Al dar doble clic o Enter sobre una fila, retorna el contacto seleccionado.
 *
 * @author M-Work
 */
public class jd_buscar_proveedor extends javax.swing.JDialog {

    // ================================================================
    // Paleta Material
    // ================================================================
    private static final Color PRIMARY       = new Color(40, 53, 147);
    private static final Color PRIMARY_DARK  = new Color(26, 35, 126);
    private static final Color SURFACE       = new Color(250, 250, 252);
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color TEXT_PRIMARY  = new Color(33, 33, 33);
    private static final Color TEXT_SECOND   = new Color(117, 117, 117);
    private static final Color DIVIDER       = new Color(224, 224, 224);
    private static final Color ROW_ALT       = new Color(245, 247, 251);
    private static final Color SELECTION     = new Color(197, 202, 233);
    private static final Color ACCENT        = new Color(255, 152, 0);
    private static final String FONT_FAMILY  = "Segoe UI";

    // ================================================================
    // Componentes
    // ================================================================
    private JTextField txtBuscar;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblResultados;

    /** Contacto seleccionado (null si se cerro sin seleccionar). */
    private Contactos contactoSeleccionado = null;

    // ================================================================
    // Constructor
    // ================================================================
    public jd_buscar_proveedor(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponentes();
        cargarProveedores();
        configurarFiltroEnVivo();
        setLocationRelativeTo(parent);
    }

    public jd_buscar_proveedor(java.awt.Dialog parent, boolean modal) {
        super(parent, modal);
        initComponentes();
        cargarProveedores();
        configurarFiltroEnVivo();
        setLocationRelativeTo(parent);
    }

    // ================================================================
    // Getter
    // ================================================================
    public Contactos getContactoSeleccionado() {
        return contactoSeleccionado;
    }

    // ================================================================
    // UI
    // ================================================================
    private void initComponentes() {
        setTitle("Buscar Proveedor");
        setSize(700, 520);
        setMinimumSize(new Dimension(550, 400));
        getContentPane().setBackground(SURFACE);
        setLayout(new BorderLayout());

        // --- AppBar ---
        add(buildAppBar(), BorderLayout.NORTH);

        // --- Contenido ---
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(SURFACE);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));

        content.add(buildSearchCard(), BorderLayout.NORTH);
        content.add(buildTableCard(), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);

        // Escape para cerrar
        getRootPane().registerKeyboardAction(
                new ActionListener() { @Override public void actionPerformed(ActionEvent e) { dispose(); } },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ----------------------------------------------------------------
    private JPanel buildAppBar() {
        JPanel appBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, PRIMARY_DARK, getWidth(), 0, PRIMARY);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        appBar.setOpaque(false);
        appBar.setBorder(new EmptyBorder(14, 20, 14, 20));
        appBar.setPreferredSize(new Dimension(0, 60));

        JLabel lblTitulo = new JLabel("\uD83D\uDD0D  Buscar Proveedor");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font(FONT_FAMILY, Font.BOLD, 20));

        JLabel lblSub = new JLabel("Doble clic o Enter para seleccionar");
        lblSub.setForeground(new Color(255, 255, 255, 180));
        lblSub.setFont(new Font(FONT_FAMILY, Font.ITALIC, 11));

        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.add(lblTitulo);
        wrap.add(Box.createVerticalStrut(2));
        wrap.add(lblSub);
        appBar.add(wrap, BorderLayout.WEST);

        return appBar;
    }

    // ----------------------------------------------------------------
    private JPanel buildSearchCard() {
        JPanel card = new CardPanel();
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(new CompoundShadowBorder(new EmptyBorder(12, 16, 12, 16)));

        JLabel lbl = new JLabel("BUSCAR");
        lbl.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        lbl.setForeground(TEXT_SECOND);
        card.add(lbl, BorderLayout.WEST);

        txtBuscar = new JTextField();
        txtBuscar.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(8, DIVIDER),
                new EmptyBorder(8, 12, 8, 12)));
        txtBuscar.setToolTipText("Nombre, c\u00e9dula o tel\u00e9fono");

        // Focus styling
        txtBuscar.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                txtBuscar.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(8, PRIMARY),
                        new EmptyBorder(8, 12, 8, 12)));
            }
            @Override public void focusLost(FocusEvent e) {
                txtBuscar.setBorder(BorderFactory.createCompoundBorder(
                        new RoundedBorder(8, DIVIDER),
                        new EmptyBorder(8, 12, 8, 12)));
            }
        });

        card.add(txtBuscar, BorderLayout.CENTER);
        return card;
    }

    // ----------------------------------------------------------------
    private JPanel buildTableCard() {
        JPanel card = new CardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new CompoundShadowBorder(new EmptyBorder(0, 0, 0, 0)));

        modelo = new DefaultTableModel(
                new Object[]{"ID", "NOMBRE", "C\u00c9DULA", "TEL\u00c9FONO"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        tabla = new JTable(modelo);
        tabla.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        tabla.setForeground(TEXT_PRIMARY);
        tabla.setRowHeight(36);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(SELECTION);
        tabla.setSelectionForeground(TEXT_PRIMARY);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Header
        JTableHeader header = tabla.getTableHeader();
        header.setDefaultRenderer(new MaterialHeaderRenderer());
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER));

        // Body
        MaterialCellRenderer bodyRenderer = new MaterialCellRenderer();
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(bodyRenderer);
        }

        // Anchos
        TableColumnModel cm = tabla.getColumnModel();
        cm.getColumn(0).setPreferredWidth(60);
        cm.getColumn(0).setMaxWidth(80);
        cm.getColumn(1).setPreferredWidth(300);
        cm.getColumn(2).setPreferredWidth(130);
        cm.getColumn(3).setPreferredWidth(130);

        // Doble clic para seleccionar
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) seleccionar();
            }
        });

        // Enter para seleccionar
        tabla.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "seleccionar");
        tabla.getActionMap().put("seleccionar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { seleccionar(); }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ----------------------------------------------------------------
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        footer.setOpaque(false);
        lblResultados = new JLabel("0 proveedores encontrados");
        lblResultados.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        lblResultados.setForeground(TEXT_SECOND);
        footer.add(lblResultados);
        return footer;
    }

    // ================================================================
    // Carga de datos
    // ================================================================
    private void cargarProveedores() {
        modelo.setRowCount(0);
        String sql = "SELECT id, nombre, cedula, contacto "
                   + "FROM contactos "
                   + "WHERE proveedor = 1 "
                   + "ORDER BY nombre";
        int count = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                modelo.addRow(new Object[]{
                        rs.getString("id"),
                        rs.getString("nombre") != null ? rs.getString("nombre") : "",
                        rs.getString("cedula") != null ? rs.getString("cedula") : "",
                        rs.getString("contacto") != null ? rs.getString("contacto") : ""
                });
                count++;
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error cargando proveedores: " + e.getMessage());
        }
        lblResultados.setText(count + " proveedores encontrados");
    }

    // ================================================================
    // Filtro en vivo
    // ================================================================
    private void configurarFiltroEnVivo() {
        final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(modelo);
        tabla.setRowSorter(sorter);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }

            private void filtrar() {
                String texto = txtBuscar.getText().trim();
                if (texto.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
                }
                lblResultados.setText(tabla.getRowCount() + " proveedores encontrados");
            }
        });

        // Enter en el campo de busqueda = seleccionar primera fila
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && tabla.getRowCount() > 0) {
                    tabla.setRowSelectionInterval(0, 0);
                    seleccionar();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN && tabla.getRowCount() > 0) {
                    tabla.requestFocus();
                    tabla.setRowSelectionInterval(0, 0);
                }
            }
        });

        txtBuscar.requestFocusInWindow();
    }

    // ================================================================
    // Seleccion
    // ================================================================
    private void seleccionar() {
        int viewRow = tabla.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = tabla.convertRowIndexToModel(viewRow);

        String idStr = modelo.getValueAt(modelRow, 0).toString();
        String nombre = modelo.getValueAt(modelRow, 1).toString();

        try {
            int id = Integer.parseInt(idStr);
            contactoSeleccionado = new Contactos(id, nombre);
        } catch (Exception e) {
            contactoSeleccionado = null;
        }
        dispose();
    }

    // ================================================================
    // ============== CLASES INTERNAS: Material widgets ===============
    // ================================================================

    private static class CardPanel extends JPanel {
        public CardPanel() { setOpaque(false); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 12;
            for (int i = 0; i < 5; i++) {
                g2.setColor(new Color(0, 0, 0, 4 + i));
                g2.fill(new RoundRectangle2D.Double(i, i + 1, getWidth() - (i * 2), getHeight() - (i * 2), arc, arc));
            }
            g2.setColor(CARD_BG);
            g2.fill(new RoundRectangle2D.Double(2, 2, getWidth() - 4, getHeight() - 5, arc, arc));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class CompoundShadowBorder extends AbstractBorder {
        private final javax.swing.border.Border inner;
        public CompoundShadowBorder(javax.swing.border.Border inner) { this.inner = inner; }
        @Override
        public Insets getBorderInsets(Component c) {
            Insets i = (Insets) inner.getBorderInsets(c).clone();
            i.top += 5; i.left += 5; i.right += 5; i.bottom += 8;
            return i;
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int arc;
        private final Color color;
        public RoundedBorder(int arc, Color color) { this.arc = arc; this.color = color; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, arc, arc));
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(0, 0, 0, 0); }
    }

    private static class MaterialHeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setBackground(new Color(245, 246, 250));
            l.setForeground(TEXT_SECOND);
            l.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
            l.setBorder(new EmptyBorder(10, 12, 10, 10));
            l.setHorizontalAlignment(SwingConstants.LEFT);
            return l;
        }
    }

    private static class MaterialCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
            l.setBorder(new EmptyBorder(0, 12, 0, 10));
            l.setOpaque(true);
            if (isSelected) {
                l.setBackground(SELECTION);
                l.setForeground(TEXT_PRIMARY);
            } else {
                l.setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
                l.setForeground(TEXT_PRIMARY);
            }
            // Columna nombre en bold
            if (column == 1) {
                l.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
            }
            return l;
        }
    }
}
