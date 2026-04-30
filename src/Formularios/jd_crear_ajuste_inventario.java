package Formularios;

import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBajustes_inventario;
import modelos.Bodegas;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

/**
 * Dialogo para crear o ver un ajuste de inventario.
 * Estilo Material Design.
 *
 * @author M-Work
 */
public class jd_crear_ajuste_inventario extends javax.swing.JDialog {

    // Paleta
    private static final Color PRIMARY      = new Color(40, 53, 147);
    private static final Color PRIMARY_DARK = new Color(26, 35, 126);
    private static final Color SURFACE      = new Color(250, 250, 252);
    private static final Color CARD_BG      = Color.WHITE;
    private static final Color TEXT_PRIMARY  = new Color(33, 33, 33);
    private static final Color TEXT_SECOND   = new Color(117, 117, 117);
    private static final Color DIVIDER       = new Color(224, 224, 224);
    private static final Color ROW_ALT       = new Color(245, 247, 251);
    private static final Color SELECTION     = new Color(197, 202, 233);
    private static final Color SUCCESS       = new Color(67, 160, 71);
    private static final Color DANGER        = new Color(229, 57, 53);
    private static final String FONT_FAMILY  = "Segoe UI";

    // Componentes
    private JTextField txtCodigoBarras;
    private JTextField txtCantidadNueva;
    private JTextField txtObsProducto;
    private JTextField txtObsGeneral;
    private JComboBox<Bodegas> cmbBodega;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblTotal;
    private JButton btnGuardar;

    private boolean modoVer = false;
    private final DecimalFormat df = new DecimalFormat("###,###.##");

    public jd_crear_ajuste_inventario(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponentes();
        setLocationRelativeTo(parent);
        try { metodos.addEscapeListenerWindowDialog(this); } catch (Exception ignore) {}
    }

    /**
     * Carga un ajuste existente en modo solo lectura.
     */
    public void cargarAjuste(int idAjuste) {
        modoVer = true;
        txtCodigoBarras.setEnabled(false);
        txtCantidadNueva.setEnabled(false);
        txtObsProducto.setEnabled(false);
        txtObsGeneral.setEnabled(false);
        cmbBodega.setEnabled(false);
        btnGuardar.setVisible(false);

        // Cargar cabecera
        try {
            ResultSet rsCab = DB_consultas_R_D.getTabla(
                    "SELECT a.id, a.fecha, a.hora, b.nombre as bodega, a.observacion, a.estado, a.id_bodega "
                    + "FROM ajustes_inventario_cabecera a "
                    + "JOIN bodegas b ON b.id = a.id_bodega "
                    + "WHERE a.id = " + idAjuste);
            if (rsCab.next()) {
                setTitle("Ajuste #" + rsCab.getString("id") + " - "
                        + rsCab.getString("fecha") + " "
                        + (rsCab.getString("hora") != null ? rsCab.getString("hora") : "")
                        + (rsCab.getInt("estado") == 0 ? " [ANULADO]" : ""));
                txtObsGeneral.setText(rsCab.getString("observacion") != null ? rsCab.getString("observacion") : "");

                // Seleccionar bodega
                String bodegaNombre = rsCab.getString("bodega");
                for (int i = 0; i < cmbBodega.getItemCount(); i++) {
                    if (cmbBodega.getItemAt(i).toString().equals(bodegaNombre)) {
                        cmbBodega.setSelectedIndex(i);
                        break;
                    }
                }
            }
            rsCab.close();
        } catch (Exception e) {
            System.err.println("Error cargando cabecera: " + e.getMessage());
        }

        // Cargar detalles
        modelo.setRowCount(0);
        try {
            ResultSet rsDet = DB_consultas_R_D.getTabla(
                    "SELECT d.id_producto, p.codigo_barras, p.descripcion, "
                    + "d.cantidad_anterior, d.cantidad_nueva, d.diferencia, d.observacion "
                    + "FROM ajustes_inventario_detalle d "
                    + "JOIN productos p ON p.id = d.id_producto "
                    + "WHERE d.id_ajuste_cabecera = " + idAjuste
                    + " ORDER BY d.id");
            while (rsDet.next()) {
                double dif = rsDet.getDouble("diferencia");
                modelo.addRow(new Object[]{
                    rsDet.getString("id_producto"),
                    rsDet.getString("codigo_barras"),
                    rsDet.getString("descripcion"),
                    df.format(rsDet.getDouble("cantidad_anterior")),
                    df.format(rsDet.getDouble("cantidad_nueva")),
                    (dif >= 0 ? "+" : "") + df.format(dif),
                    rsDet.getString("observacion") != null ? rsDet.getString("observacion") : ""
                });
            }
            rsDet.close();
        } catch (Exception e) {
            System.err.println("Error cargando detalles: " + e.getMessage());
        }

        actualizarTotal();
    }

    // ================================================================
    // UI
    // ================================================================
    private void initComponentes() {
        setTitle("Nuevo Ajuste de Inventario");
        setSize(1000, 650);
        setMinimumSize(new Dimension(850, 550));
        getContentPane().setBackground(SURFACE);
        setLayout(new BorderLayout());

        add(buildAppBar(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(SURFACE);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));

        content.add(buildInputCard(), BorderLayout.NORTH);
        content.add(buildTableCard(), BorderLayout.CENTER);
        content.add(buildFooterCard(), BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

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
        appBar.setPreferredSize(new Dimension(0, 58));

        JLabel lblTitulo = new JLabel("Ajuste de Inventario");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font(FONT_FAMILY, Font.BOLD, 20));
        appBar.add(lblTitulo, BorderLayout.WEST);

        return appBar;
    }

    private JPanel buildInputCard() {
        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(8, 0, 8, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        // Fila 0: Labels
        c.gridy = 0;
        c.gridx = 0; card.add(buildLabel("BODEGA"), c);
        c.gridx = 1; card.add(buildLabel("OBSERVACI\u00d3N GENERAL"), c);

        // Fila 1: Bodega + Observacion general
        c.gridy = 1;
        cmbBodega = new JComboBox<Bodegas>();
        cmbBodega.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        cmbBodega.setPreferredSize(new Dimension(220, 34));
        new Bodegas().mostrarBodegas(cmbBodega);
        // Pre-seleccionar bodega del usuario
        for (int i = 0; i < cmbBodega.getItemCount(); i++) {
            if (cmbBodega.getItemAt(i).getId() == frm_main.id_bodega) {
                cmbBodega.setSelectedIndex(i);
                break;
            }
        }
        c.gridx = 0; c.weightx = 0.3;
        card.add(cmbBodega, c);

        txtObsGeneral = new JTextField();
        txtObsGeneral.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        txtObsGeneral.setPreferredSize(new Dimension(300, 34));
        c.gridx = 1; c.weightx = 0.7;
        card.add(txtObsGeneral, c);

        // Separador visual
        c.gridy = 2; c.gridx = 0; c.gridwidth = 4; c.weightx = 1;
        card.add(new JSeparator(), c);
        c.gridwidth = 1;

        // Fila 3: Labels producto
        c.gridy = 3;
        c.gridx = 0; card.add(buildLabel("C\u00d3DIGO DE BARRAS"), c);
        c.gridx = 1; card.add(buildLabel("CANTIDAD NUEVA"), c);
        c.gridx = 2; card.add(buildLabel("NOTA (OPCIONAL)"), c);

        // Fila 4: Inputs producto
        c.gridy = 4;

        // Panel codigo + boton buscar
        JPanel panelCodigo = new JPanel(new BorderLayout(4, 0));
        panelCodigo.setOpaque(false);

        txtCodigoBarras = new JTextField();
        txtCodigoBarras.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
        txtCodigoBarras.setPreferredSize(new Dimension(180, 38));
        panelCodigo.add(txtCodigoBarras, BorderLayout.CENTER);

        JButton btnBuscarProducto = new JButton("\uD83D\uDD0D");
        btnBuscarProducto.setFont(new Font(FONT_FAMILY, Font.PLAIN, 16));
        btnBuscarProducto.setPreferredSize(new Dimension(42, 38));
        btnBuscarProducto.setBackground(PRIMARY);
        btnBuscarProducto.setForeground(Color.WHITE);
        btnBuscarProducto.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBuscarProducto.setToolTipText("Buscar producto por nombre o c\u00f3digo");
        btnBuscarProducto.setFocusPainted(false);
        btnBuscarProducto.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        btnBuscarProducto.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { abrirBuscadorProductos(); }
        });
        panelCodigo.add(btnBuscarProducto, BorderLayout.EAST);

        c.gridx = 0; c.weightx = 0.3;
        card.add(panelCodigo, c);

        txtCantidadNueva = new JTextField();
        txtCantidadNueva.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
        txtCantidadNueva.setPreferredSize(new Dimension(140, 38));
        c.gridx = 1; c.weightx = 0.2;
        card.add(txtCantidadNueva, c);

        txtObsProducto = new JTextField();
        txtObsProducto.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        txtObsProducto.setPreferredSize(new Dimension(250, 38));
        c.gridx = 2; c.weightx = 0.5;
        card.add(txtObsProducto, c);

        // Listeners
        txtCodigoBarras.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    txtCantidadNueva.requestFocus();
                }
            }
        });

        txtCantidadNueva.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    agregarProducto();
                }
            }
        });

        txtObsProducto.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    agregarProducto();
                }
            }
        });

        return card;
    }

    private JLabel buildLabel(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font(FONT_FAMILY, Font.BOLD, 10));
        l.setForeground(TEXT_SECOND);
        return l;
    }

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(DIVIDER));

        modelo = new DefaultTableModel(
                new Object[]{"ID", "C\u00d3DIGO", "DESCRIPCI\u00d3N", "CANT. ACTUAL", "CANT. NUEVA", "DIFERENCIA", "NOTA"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        tabla = new JTable(modelo);
        tabla.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        tabla.setForeground(TEXT_PRIMARY);
        tabla.setRowHeight(34);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(SELECTION);
        tabla.setSelectionForeground(TEXT_PRIMARY);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = tabla.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                l.setBackground(new Color(245, 246, 250));
                l.setForeground(TEXT_SECOND);
                l.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
                l.setBorder(new EmptyBorder(8, 10, 8, 10));
                if (col >= 3 && col <= 5) l.setHorizontalAlignment(SwingConstants.RIGHT);
                else l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });
        header.setPreferredSize(new Dimension(0, 40));
        header.setReorderingAllowed(false);

        // Cell renderer con colores para diferencia
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                l.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
                l.setBorder(new EmptyBorder(0, 10, 0, 10));
                l.setOpaque(true);

                if (sel) {
                    l.setBackground(SELECTION);
                    l.setForeground(TEXT_PRIMARY);
                } else {
                    l.setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
                    l.setForeground(TEXT_PRIMARY);
                }

                if (col >= 3 && col <= 5) {
                    l.setHorizontalAlignment(SwingConstants.RIGHT);
                    l.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }

                // Colorear diferencia
                if (col == 5 && value != null && !sel) {
                    String s = value.toString().replace(",", "").replace("+", "");
                    try {
                        double v = Double.parseDouble(s);
                        if (v > 0)      l.setForeground(SUCCESS);
                        else if (v < 0) l.setForeground(DANGER);
                    } catch (Exception ignore) {}
                }

                return l;
            }
        });

        // Anchos
        TableColumnModel cm = tabla.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(0).setMaxWidth(70);
        cm.getColumn(1).setPreferredWidth(120);
        cm.getColumn(2).setPreferredWidth(300);
        cm.getColumn(3).setPreferredWidth(100);
        cm.getColumn(4).setPreferredWidth(100);
        cm.getColumn(5).setPreferredWidth(100);
        cm.getColumn(6).setPreferredWidth(150);

        // Tecla Delete para eliminar fila
        tabla.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "eliminarFila");
        tabla.getActionMap().put("eliminarFila", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (modoVer) return;
                int row = tabla.getSelectedRow();
                if (row >= 0) {
                    modelo.removeRow(row);
                    actualizarTotal();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildFooterCard() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 0, 0, 0));

        lblTotal = new JLabel("0 productos");
        lblTotal.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        lblTotal.setForeground(TEXT_SECOND);
        footer.add(lblTotal, BorderLayout.WEST);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelAcciones.setOpaque(false);

        JButton btnEliminarFila = new JButton("Eliminar fila");
        btnEliminarFila.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        btnEliminarFila.setForeground(DANGER);
        btnEliminarFila.setBackground(CARD_BG);
        btnEliminarFila.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEliminarFila.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (modoVer) return;
                int row = tabla.getSelectedRow();
                if (row >= 0) {
                    modelo.removeRow(row);
                    actualizarTotal();
                }
            }
        });

        btnGuardar = new JButton("GUARDAR AJUSTE");
        btnGuardar.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setBackground(PRIMARY);
        btnGuardar.setBorder(new EmptyBorder(12, 30, 12, 30));
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { guardarAjuste(); }
        });

        panelAcciones.add(btnEliminarFila);
        panelAcciones.add(btnGuardar);
        footer.add(panelAcciones, BorderLayout.EAST);

        return footer;
    }

    // ================================================================
    // Buscador de productos
    // ================================================================

    private void abrirBuscadorProductos() {
        if (modoVer) return;

        Bodegas bodegaSel = (Bodegas) cmbBodega.getSelectedItem();
        int idBodega = bodegaSel != null ? bodegaSel.getId() : frm_main.id_bodega;

        // Crear dialogo
        final JDialog dlg = new JDialog(this, "Buscar Producto", true);
        dlg.setSize(750, 500);
        dlg.setMinimumSize(new Dimension(600, 400));
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(SURFACE);
        dlg.setLayout(new BorderLayout());

        // AppBar
        JPanel appBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, PRIMARY_DARK, getWidth(), 0, PRIMARY));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        appBar.setOpaque(false);
        appBar.setBorder(new EmptyBorder(12, 18, 12, 18));
        appBar.setPreferredSize(new Dimension(0, 50));
        JLabel lblTit = new JLabel("Buscar Producto");
        lblTit.setForeground(Color.WHITE);
        lblTit.setFont(new Font(FONT_FAMILY, Font.BOLD, 18));
        appBar.add(lblTit, BorderLayout.WEST);
        JLabel lblHint = new JLabel("Doble clic o Enter para seleccionar");
        lblHint.setForeground(new Color(255, 255, 255, 180));
        lblHint.setFont(new Font(FONT_FAMILY, Font.ITALIC, 11));
        appBar.add(lblHint, BorderLayout.EAST);
        dlg.add(appBar, BorderLayout.NORTH);

        // Contenido
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(SURFACE);
        content.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Campo de busqueda
        final JTextField txtBuscar = new JTextField();
        txtBuscar.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        txtBuscar.setPreferredSize(new Dimension(0, 38));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIVIDER),
                new EmptyBorder(6, 12, 6, 12)));
        txtBuscar.setToolTipText("Escriba c\u00f3digo o nombre del producto");
        content.add(txtBuscar, BorderLayout.NORTH);

        // Tabla de productos
        final DefaultTableModel modeloBuscar = new DefaultTableModel(
                new Object[]{"C\u00d3DIGO", "DESCRIPCI\u00d3N", "STOCK EN BODEGA"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        final JTable tablaBuscar = new JTable(modeloBuscar);
        tablaBuscar.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        tablaBuscar.setRowHeight(32);
        tablaBuscar.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaBuscar.setShowGrid(false);
        tablaBuscar.setIntercellSpacing(new Dimension(0, 0));
        tablaBuscar.setFillsViewportHeight(true);
        tablaBuscar.setSelectionBackground(SELECTION);
        tablaBuscar.setSelectionForeground(TEXT_PRIMARY);

        // Header style
        tablaBuscar.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                l.setBackground(new Color(245, 246, 250));
                l.setForeground(TEXT_SECOND);
                l.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
                l.setBorder(new EmptyBorder(8, 10, 8, 10));
                if (col == 2) l.setHorizontalAlignment(SwingConstants.RIGHT);
                return l;
            }
        });
        tablaBuscar.getTableHeader().setPreferredSize(new Dimension(0, 38));

        // Body renderer
        tablaBuscar.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                l.setFont(new Font(FONT_FAMILY, col == 1 ? Font.BOLD : Font.PLAIN, 13));
                l.setBorder(new EmptyBorder(0, 10, 0, 10));
                l.setOpaque(true);
                if (sel) {
                    l.setBackground(SELECTION);
                } else {
                    l.setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
                }
                l.setForeground(TEXT_PRIMARY);
                if (col == 2) l.setHorizontalAlignment(SwingConstants.RIGHT);
                else l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });

        TableColumnModel cmBuscar = tablaBuscar.getColumnModel();
        cmBuscar.getColumn(0).setPreferredWidth(130);
        cmBuscar.getColumn(1).setPreferredWidth(400);
        cmBuscar.getColumn(2).setPreferredWidth(120);

        JScrollPane scrollBuscar = new JScrollPane(tablaBuscar);
        scrollBuscar.setBorder(BorderFactory.createLineBorder(DIVIDER));
        scrollBuscar.getViewport().setBackground(CARD_BG);
        content.add(scrollBuscar, BorderLayout.CENTER);

        // Footer con conteo
        final JLabel lblCount = new JLabel("0 productos");
        lblCount.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        lblCount.setForeground(TEXT_SECOND);
        content.add(lblCount, BorderLayout.SOUTH);

        dlg.add(content, BorderLayout.CENTER);

        // Cargar productos
        String sqlProductos = "SELECT p.codigo_barras, p.descripcion, "
                + "COALESCE(sp.cantidad, 0) as stock "
                + "FROM productos p "
                + "LEFT JOIN stock_productos sp ON sp.id_producto = p.id AND sp.id_bodega = " + idBodega + " "
                + "WHERE COALESCE(p.estado, true) = true "
                + "ORDER BY p.descripcion";

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sqlProductos);
            int count = 0;
            while (rs.next()) {
                modeloBuscar.addRow(new Object[]{
                    rs.getString("codigo_barras"),
                    rs.getString("descripcion") != null ? rs.getString("descripcion") : "",
                    df.format(rs.getDouble("stock"))
                });
                count++;
            }
            rs.close();
            lblCount.setText(count + " productos");
        } catch (Exception e) {
            System.err.println("Error cargando productos: " + e.getMessage());
        }

        // Filtro en vivo
        final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(modeloBuscar);
        tablaBuscar.setRowSorter(sorter);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e)  { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
            private void filtrar() {
                String t = txtBuscar.getText().trim();
                if (t.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + t));
                lblCount.setText(tablaBuscar.getRowCount() + " productos");
            }
        });

        // Accion de seleccionar
        final Runnable seleccionar = new Runnable() {
            @Override public void run() {
                int viewRow = tablaBuscar.getSelectedRow();
                if (viewRow < 0) return;
                int modelRow = tablaBuscar.convertRowIndexToModel(viewRow);
                String codigo = modeloBuscar.getValueAt(modelRow, 0).toString();
                txtCodigoBarras.setText(codigo);
                dlg.dispose();
                txtCantidadNueva.requestFocus();
            }
        };

        // Doble clic
        tablaBuscar.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) seleccionar.run();
            }
        });

        // Enter en tabla
        tablaBuscar.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sel");
        tablaBuscar.getActionMap().put("sel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { seleccionar.run(); }
        });

        // Enter en campo busqueda = seleccionar primera fila
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && tablaBuscar.getRowCount() > 0) {
                    tablaBuscar.setRowSelectionInterval(0, 0);
                    seleccionar.run();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN && tablaBuscar.getRowCount() > 0) {
                    tablaBuscar.requestFocus();
                    tablaBuscar.setRowSelectionInterval(0, 0);
                }
            }
        });

        // Escape cierra
        dlg.getRootPane().registerKeyboardAction(
                new ActionListener() { @Override public void actionPerformed(ActionEvent e) { dlg.dispose(); } },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        txtBuscar.requestFocusInWindow();
        dlg.setVisible(true);
    }

    // ================================================================
    // Logica
    // ================================================================

    private void agregarProducto() {
        String codigoBarras = txtCodigoBarras.getText().trim();
        String cantNuevaStr = txtCantidadNueva.getText().trim();

        if (codigoBarras.isEmpty()) {
            txtCodigoBarras.requestFocus();
            return;
        }
        if (cantNuevaStr.isEmpty()) {
            txtCantidadNueva.requestFocus();
            return;
        }

        double cantNueva;
        try {
            cantNueva = Double.parseDouble(cantNuevaStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "La cantidad nueva debe ser un n\u00famero v\u00e1lido.");
            txtCantidadNueva.requestFocus();
            txtCantidadNueva.selectAll();
            return;
        }

        // Verificar que no este ya en la tabla
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (modelo.getValueAt(i, 1).toString().equals(codigoBarras)) {
                JOptionPane.showMessageDialog(this, "Este producto ya fue agregado.");
                txtCodigoBarras.setText("");
                txtCodigoBarras.requestFocus();
                return;
            }
        }

        // Buscar producto
        Bodegas bodegaSel = (Bodegas) cmbBodega.getSelectedItem();
        int idBodega = bodegaSel != null ? bodegaSel.getId() : frm_main.id_bodega;

        String sql = "SELECT p.id, p.codigo_barras, p.descripcion, "
                + "COALESCE(sp.cantidad, 0) as cantidad_actual "
                + "FROM productos p "
                + "LEFT JOIN stock_productos sp ON sp.id_producto = p.id AND sp.id_bodega = " + idBodega + " "
                + "WHERE p.codigo_barras = '" + codigoBarras.replace("'", "''") + "' "
                + "AND COALESCE(p.estado, true) = true";

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            if (rs.next()) {
                String idProducto = rs.getString("id");
                String descripcion = rs.getString("descripcion");
                double cantActual = rs.getDouble("cantidad_actual");
                double diferencia = cantNueva - cantActual;

                modelo.addRow(new Object[]{
                    idProducto,
                    codigoBarras,
                    descripcion,
                    df.format(cantActual),
                    df.format(cantNueva),
                    (diferencia >= 0 ? "+" : "") + df.format(diferencia),
                    txtObsProducto.getText().trim()
                });

                actualizarTotal();

                txtCodigoBarras.setText("");
                txtCantidadNueva.setText("");
                txtObsProducto.setText("");
                txtCodigoBarras.requestFocus();

            } else {
                JOptionPane.showMessageDialog(this,
                        "Producto con c\u00f3digo '" + codigoBarras + "' no encontrado.",
                        "Producto no encontrado", JOptionPane.WARNING_MESSAGE);
                txtCodigoBarras.selectAll();
                txtCodigoBarras.requestFocus();
            }
            rs.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error buscando producto:\n" + e.getMessage());
        }
    }

    private void actualizarTotal() {
        int positivos = 0, negativos = 0, sinCambio = 0;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            String difStr = modelo.getValueAt(i, 5).toString().replace(",", "").replace("+", "");
            try {
                double d = Double.parseDouble(difStr);
                if (d > 0) positivos++;
                else if (d < 0) negativos++;
                else sinCambio++;
            } catch (Exception ignore) {}
        }
        lblTotal.setText(modelo.getRowCount() + " productos  |  "
                + positivos + " aumentos  |  "
                + negativos + " disminuciones  |  "
                + sinCambio + " sin cambio");
    }

    private void guardarAjuste() {
        if (modelo.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Agregue al menos un producto.");
            return;
        }

        Bodegas bodegaSel = (Bodegas) cmbBodega.getSelectedItem();
        if (bodegaSel == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una bodega.");
            return;
        }

        int resp = JOptionPane.showConfirmDialog(this,
                "Se ajustar\u00e1n " + modelo.getRowCount() + " productos en " + bodegaSel.toString() + ".\n"
                + "\u00bfDesea continuar?",
                "Confirmar ajuste", JOptionPane.YES_NO_OPTION);
        if (resp != JOptionPane.YES_OPTION) return;

        int idBodega = bodegaSel.getId();
        int idUser = frm_main.id_user;
        String observacion = txtObsGeneral.getText().trim();

        double[][] productos = new double[modelo.getRowCount()][4];
        String[] obsProductos = new String[modelo.getRowCount()];

        for (int i = 0; i < modelo.getRowCount(); i++) {
            productos[i][0] = Double.parseDouble(modelo.getValueAt(i, 0).toString()); // id_producto
            productos[i][1] = Double.parseDouble(modelo.getValueAt(i, 3).toString().replace(",", "")); // cant_anterior
            productos[i][2] = Double.parseDouble(modelo.getValueAt(i, 4).toString().replace(",", "")); // cant_nueva
            productos[i][3] = Double.parseDouble(modelo.getValueAt(i, 5).toString().replace(",", "").replace("+", "")); // diferencia
            obsProductos[i] = modelo.getValueAt(i, 6).toString();
        }

        DBajustes_inventario db = new DBajustes_inventario();
        int idAjuste = db.guardar(idUser, idBodega, observacion, productos, obsProductos);

        if (idAjuste > 0) {
            JOptionPane.showMessageDialog(this,
                    "Ajuste #" + idAjuste + " guardado exitosamente.\n"
                    + modelo.getRowCount() + " productos ajustados.",
                    "Ajuste guardado", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        }
    }
}
