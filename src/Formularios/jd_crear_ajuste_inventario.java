package Formularios;

import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBajustes_inventario;
import modelos.Bodegas;

import javax.swing.*;
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
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Dialogo para crear o ver un ajuste de inventario.
 * Soporta ajustar tanto la cantidad fisica como los pendientes.
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
    private static final Color INFO_BG       = new Color(232, 234, 246);
    private static final String FONT_FAMILY  = "Segoe UI";

    // Indices de columnas (mantener sincronizados con la creacion del modelo)
    private static final int COL_ID         = 0;
    private static final int COL_CODIGO     = 1;
    private static final int COL_DESC       = 2;
    private static final int COL_CANT_ACT   = 3;
    private static final int COL_CANT_NUEVA = 4;
    private static final int COL_DIF_CANT   = 5;
    private static final int COL_PEND_ACT   = 6;
    private static final int COL_PEND_NUEVA = 7;
    private static final int COL_DIF_PEND   = 8;
    private static final int COL_NOTA       = 9;

    // Componentes
    private JTextField txtCodigoBarras;
    private JTextField txtCantidadNueva;
    private JTextField txtPendientesNueva;
    private JTextField txtObsProducto;
    private JTextField txtObsGeneral;
    private JComboBox<Bodegas> cmbBodega;
    private JTable tabla;
    private DefaultTableModel modelo;
    private JLabel lblTotal;
    private JLabel lblInfoActual;
    private JButton btnGuardar;
    private JButton btnEliminarFila;

    // Estado del producto previsualizado
    private int previewIdProducto = -1;
    private String previewDescripcion = "";
    private double previewCantActual = 0;
    private double previewPendActual = 0;

    private boolean modoVer = false;
    // Símbolos fijos: agrupador ',' y decimal '.', para que el formateo coincida
    // con parseNum() sin importar el locale del sistema (en es-* el agrupador
    // por defecto es '.', lo que corrompía 40.000 -> 40 al re-parsear).
    private final DecimalFormat df = new DecimalFormat("###,###.##",
            new DecimalFormatSymbols(Locale.US));

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
        txtPendientesNueva.setEnabled(false);
        txtObsProducto.setEnabled(false);
        txtObsGeneral.setEnabled(false);
        cmbBodega.setEnabled(false);
        btnGuardar.setVisible(false);
        btnEliminarFila.setVisible(false);

        // Cabecera
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

        // Detalles
        modelo.setRowCount(0);
        try {
            ResultSet rsDet = DB_consultas_R_D.getTabla(
                    "SELECT d.id_producto, p.codigo_barras, p.descripcion, "
                    + "d.cantidad_anterior, d.cantidad_nueva, d.diferencia, "
                    + "d.pendientes_anterior, d.pendientes_nuevo, d.diferencia_pendientes, "
                    + "d.observacion "
                    + "FROM ajustes_inventario_detalle d "
                    + "JOIN productos p ON p.id = d.id_producto "
                    + "WHERE d.id_ajuste_cabecera = " + idAjuste
                    + " ORDER BY d.id");
            while (rsDet.next()) {
                double difC = rsDet.getDouble("diferencia");
                double difP = rsDet.getDouble("diferencia_pendientes");
                modelo.addRow(new Object[]{
                    rsDet.getString("id_producto"),
                    rsDet.getString("codigo_barras"),
                    rsDet.getString("descripcion"),
                    df.format(rsDet.getDouble("cantidad_anterior")),
                    df.format(rsDet.getDouble("cantidad_nueva")),
                    (difC >= 0 ? "+" : "") + df.format(difC),
                    df.format(rsDet.getDouble("pendientes_anterior")),
                    df.format(rsDet.getDouble("pendientes_nuevo")),
                    (difP >= 0 ? "+" : "") + df.format(difP),
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
        setSize(1180, 680);
        setMinimumSize(new Dimension(1000, 580));
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
        c.gridx = 1; c.gridwidth = 3; card.add(buildLabel("OBSERVACIÓN GENERAL"), c);
        c.gridwidth = 1;

        // Fila 1: Bodega + Observacion general
        c.gridy = 1;
        cmbBodega = new JComboBox<Bodegas>();
        cmbBodega.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        cmbBodega.setPreferredSize(new Dimension(220, 34));
        new Bodegas().mostrarBodegas(cmbBodega);
        for (int i = 0; i < cmbBodega.getItemCount(); i++) {
            if (cmbBodega.getItemAt(i).getId() == frm_main.id_bodega) {
                cmbBodega.setSelectedIndex(i);
                break;
            }
        }
        cmbBodega.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { actualizarPreview(); }
        });
        c.gridx = 0; c.weightx = 0.25;
        card.add(cmbBodega, c);

        txtObsGeneral = new JTextField();
        txtObsGeneral.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        txtObsGeneral.setPreferredSize(new Dimension(300, 34));
        c.gridx = 1; c.gridwidth = 3; c.weightx = 0.75;
        card.add(txtObsGeneral, c);
        c.gridwidth = 1;

        // Separador visual
        c.gridy = 2; c.gridx = 0; c.gridwidth = 5; c.weightx = 1;
        card.add(new JSeparator(), c);
        c.gridwidth = 1;

        // Fila 3: Labels producto
        c.gridy = 3;
        c.gridx = 0; card.add(buildLabel("CÓDIGO DE BARRAS"), c);
        c.gridx = 1; card.add(buildLabel("CANTIDAD NUEVA"), c);
        c.gridx = 2; card.add(buildLabel("PENDIENTES NUEVOS"), c);
        c.gridx = 3; card.add(buildLabel("NOTA (OPCIONAL)"), c);

        // Fila 4: Inputs producto
        c.gridy = 4;

        // Panel codigo + boton buscar
        JPanel panelCodigo = new JPanel(new BorderLayout(4, 0));
        panelCodigo.setOpaque(false);

        txtCodigoBarras = new JTextField();
        txtCodigoBarras.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
        txtCodigoBarras.setPreferredSize(new Dimension(180, 38));
        panelCodigo.add(txtCodigoBarras, BorderLayout.CENTER);

        JButton btnBuscarProducto = new JButton(FontAwesome.icon(FontAwesome.SEARCH, 16f, Color.WHITE));
        btnBuscarProducto.setPreferredSize(new Dimension(46, 38));
        btnBuscarProducto.setBackground(PRIMARY);
        btnBuscarProducto.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBuscarProducto.setToolTipText("Buscar producto por nombre o código");
        btnBuscarProducto.setFocusPainted(false);
        btnBuscarProducto.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        btnBuscarProducto.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { abrirBuscadorProductos(); }
        });
        panelCodigo.add(btnBuscarProducto, BorderLayout.EAST);

        c.gridx = 0; c.weightx = 0.25;
        card.add(panelCodigo, c);

        txtCantidadNueva = new JTextField();
        txtCantidadNueva.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
        txtCantidadNueva.setPreferredSize(new Dimension(140, 38));
        txtCantidadNueva.setToolTipText("Dejar vacio para no modificar la cantidad fisica");
        c.gridx = 1; c.weightx = 0.18;
        card.add(txtCantidadNueva, c);

        txtPendientesNueva = new JTextField();
        txtPendientesNueva.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
        txtPendientesNueva.setPreferredSize(new Dimension(140, 38));
        txtPendientesNueva.setToolTipText("Dejar vacio para no modificar los pendientes");
        c.gridx = 2; c.weightx = 0.18;
        card.add(txtPendientesNueva, c);

        txtObsProducto = new JTextField();
        txtObsProducto.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        txtObsProducto.setPreferredSize(new Dimension(220, 38));
        c.gridx = 3; c.weightx = 0.39;
        card.add(txtObsProducto, c);

        // Fila 5: Preview del stock actual del producto
        c.gridy = 5;
        lblInfoActual = new JLabel(" ");
        lblInfoActual.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        lblInfoActual.setForeground(TEXT_SECOND);
        lblInfoActual.setOpaque(true);
        lblInfoActual.setBackground(INFO_BG);
        lblInfoActual.setBorder(new EmptyBorder(6, 10, 6, 10));
        c.gridx = 0; c.gridwidth = 4; c.weightx = 1;
        card.add(lblInfoActual, c);
        c.gridwidth = 1;

        // Listeners
        txtCodigoBarras.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    actualizarPreview();
                    txtCantidadNueva.requestFocus();
                }
            }
        });
        txtCodigoBarras.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { actualizarPreview(); }
        });

        KeyAdapter enterAgrega = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) agregarProducto();
            }
        };
        txtCantidadNueva.addKeyListener(enterAgrega);
        txtPendientesNueva.addKeyListener(enterAgrega);
        txtObsProducto.addKeyListener(enterAgrega);

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
                new Object[]{"ID", "CÓDIGO", "DESCRIPCIÓN",
                        "CANT. ACT.", "CANT. NUEVA", "DIF. CANT.",
                        "PEND. ACT.", "PEND. NUEVO", "DIF. PEND.",
                        "NOTA"}, 0) {
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
                if (col >= COL_CANT_ACT && col <= COL_DIF_PEND) l.setHorizontalAlignment(SwingConstants.RIGHT);
                else l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });
        header.setPreferredSize(new Dimension(0, 40));
        header.setReorderingAllowed(false);

        // Renderer con colores para diferencias
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

                if (col >= COL_CANT_ACT && col <= COL_DIF_PEND) {
                    l.setHorizontalAlignment(SwingConstants.RIGHT);
                    l.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }

                // Color en columnas de diferencia
                if ((col == COL_DIF_CANT || col == COL_DIF_PEND) && value != null && !sel) {
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

        TableColumnModel cm = tabla.getColumnModel();
        cm.getColumn(COL_ID).setPreferredWidth(45);
        cm.getColumn(COL_ID).setMaxWidth(60);
        cm.getColumn(COL_CODIGO).setPreferredWidth(110);
        cm.getColumn(COL_DESC).setPreferredWidth(240);
        cm.getColumn(COL_CANT_ACT).setPreferredWidth(85);
        cm.getColumn(COL_CANT_NUEVA).setPreferredWidth(95);
        cm.getColumn(COL_DIF_CANT).setPreferredWidth(85);
        cm.getColumn(COL_PEND_ACT).setPreferredWidth(85);
        cm.getColumn(COL_PEND_NUEVA).setPreferredWidth(95);
        cm.getColumn(COL_DIF_PEND).setPreferredWidth(85);
        cm.getColumn(COL_NOTA).setPreferredWidth(140);

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

        btnEliminarFila = new JButton("Eliminar fila",
                FontAwesome.icon(FontAwesome.TRASH, 14f, DANGER));
        btnEliminarFila.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        btnEliminarFila.setForeground(DANGER);
        btnEliminarFila.setBackground(CARD_BG);
        btnEliminarFila.setFocusPainted(false);
        btnEliminarFila.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEliminarFila.setIconTextGap(8);
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

        btnGuardar = new JButton("GUARDAR AJUSTE",
                FontAwesome.icon(FontAwesome.SAVE, 15f, Color.WHITE));
        btnGuardar.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setBackground(PRIMARY);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardar.setIconTextGap(10);
        btnGuardar.setBorder(new EmptyBorder(10, 24, 10, 24));
        btnGuardar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { guardarAjuste(); }
        });

        panelAcciones.add(btnEliminarFila);
        panelAcciones.add(btnGuardar);
        footer.add(panelAcciones, BorderLayout.EAST);

        return footer;
    }

    // ================================================================
    // Preview del stock actual del producto
    // ================================================================
    private void actualizarPreview() {
        String codigo = txtCodigoBarras.getText().trim();
        if (codigo.isEmpty()) {
            previewIdProducto = -1;
            previewDescripcion = "";
            previewCantActual = 0;
            previewPendActual = 0;
            lblInfoActual.setText(" ");
            return;
        }

        Bodegas bodegaSel = (Bodegas) cmbBodega.getSelectedItem();
        int idBodega = bodegaSel != null ? bodegaSel.getId() : frm_main.id_bodega;

        String sql = "SELECT p.id, p.descripcion, "
                + "COALESCE(sp.cantidad, 0) as cantidad, "
                + "COALESCE(sp.pendientes, 0) as pendientes "
                + "FROM productos p "
                + "LEFT JOIN stock_productos sp ON sp.id_producto = p.id AND sp.id_bodega = " + idBodega + " "
                + "WHERE p.codigo_barras = '" + codigo.replace("'", "''") + "' "
                + "AND COALESCE(p.estado, true) = true";

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            if (rs.next()) {
                previewIdProducto  = rs.getInt("id");
                previewDescripcion = rs.getString("descripcion");
                previewCantActual  = rs.getDouble("cantidad");
                previewPendActual  = rs.getDouble("pendientes");
                double disponible  = previewCantActual - previewPendActual;
                lblInfoActual.setText(
                        "<html><b>" + previewDescripcion + "</b>"
                        + "  &nbsp;|&nbsp;  Cantidad actual: <b>" + df.format(previewCantActual) + "</b>"
                        + "  &nbsp;|&nbsp;  Pendientes: <b>" + df.format(previewPendActual) + "</b>"
                        + "  &nbsp;|&nbsp;  Disponible: <b>" + df.format(disponible) + "</b></html>");
                lblInfoActual.setForeground(TEXT_PRIMARY);
            } else {
                previewIdProducto = -1;
                lblInfoActual.setText("Producto con código '" + codigo + "' no encontrado.");
                lblInfoActual.setForeground(DANGER);
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error preview producto: " + e.getMessage());
        }
    }

    // ================================================================
    // Buscador de productos
    // ================================================================
    private void abrirBuscadorProductos() {
        if (modoVer) return;

        Bodegas bodegaSel = (Bodegas) cmbBodega.getSelectedItem();
        int idBodega = bodegaSel != null ? bodegaSel.getId() : frm_main.id_bodega;

        final JDialog dlg = new JDialog(this, "Buscar Producto", true);
        dlg.setSize(820, 520);
        dlg.setMinimumSize(new Dimension(680, 420));
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
        JLabel lblTit = new JLabel("Buscar Producto",
                FontAwesome.icon(FontAwesome.SEARCH, 16f, Color.WHITE), SwingConstants.LEFT);
        lblTit.setForeground(Color.WHITE);
        lblTit.setFont(new Font(FONT_FAMILY, Font.BOLD, 18));
        lblTit.setIconTextGap(10);
        appBar.add(lblTit, BorderLayout.WEST);
        JLabel lblHint = new JLabel("Doble clic o Enter para seleccionar");
        lblHint.setForeground(new Color(255, 255, 255, 180));
        lblHint.setFont(new Font(FONT_FAMILY, Font.ITALIC, 11));
        appBar.add(lblHint, BorderLayout.EAST);
        dlg.add(appBar, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(SURFACE);
        content.setBorder(new EmptyBorder(12, 16, 12, 16));

        final JTextField txtBuscar = new JTextField();
        txtBuscar.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        txtBuscar.setPreferredSize(new Dimension(0, 38));
        txtBuscar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIVIDER),
                new EmptyBorder(6, 12, 6, 12)));
        txtBuscar.setToolTipText("Escriba código o nombre del producto");
        content.add(txtBuscar, BorderLayout.NORTH);

        final DefaultTableModel modeloBuscar = new DefaultTableModel(
                new Object[]{"CÓDIGO", "DESCRIPCIÓN", "CANTIDAD", "PENDIENTES", "DISPONIBLE"}, 0) {
            @Override public boolean isCellEditable(int r, int col) { return false; }
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

        tablaBuscar.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                l.setBackground(new Color(245, 246, 250));
                l.setForeground(TEXT_SECOND);
                l.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
                l.setBorder(new EmptyBorder(8, 10, 8, 10));
                if (col >= 2) l.setHorizontalAlignment(SwingConstants.RIGHT);
                return l;
            }
        });
        tablaBuscar.getTableHeader().setPreferredSize(new Dimension(0, 38));

        tablaBuscar.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                l.setFont(new Font(FONT_FAMILY, col == 1 ? Font.BOLD : Font.PLAIN, 13));
                l.setBorder(new EmptyBorder(0, 10, 0, 10));
                l.setOpaque(true);
                if (sel) l.setBackground(SELECTION);
                else l.setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
                l.setForeground(TEXT_PRIMARY);
                if (col >= 2) l.setHorizontalAlignment(SwingConstants.RIGHT);
                else l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });

        TableColumnModel cmBuscar = tablaBuscar.getColumnModel();
        cmBuscar.getColumn(0).setPreferredWidth(130);
        cmBuscar.getColumn(1).setPreferredWidth(360);
        cmBuscar.getColumn(2).setPreferredWidth(90);
        cmBuscar.getColumn(3).setPreferredWidth(90);
        cmBuscar.getColumn(4).setPreferredWidth(90);

        JScrollPane scrollBuscar = new JScrollPane(tablaBuscar);
        scrollBuscar.setBorder(BorderFactory.createLineBorder(DIVIDER));
        scrollBuscar.getViewport().setBackground(CARD_BG);
        content.add(scrollBuscar, BorderLayout.CENTER);

        final JLabel lblCount = new JLabel("0 productos");
        lblCount.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        lblCount.setForeground(TEXT_SECOND);
        content.add(lblCount, BorderLayout.SOUTH);

        dlg.add(content, BorderLayout.CENTER);

        // Cargar productos
        String sqlProductos = "SELECT p.codigo_barras, p.descripcion, "
                + "COALESCE(sp.cantidad, 0) as cantidad, "
                + "COALESCE(sp.pendientes, 0) as pendientes "
                + "FROM productos p "
                + "LEFT JOIN stock_productos sp ON sp.id_producto = p.id AND sp.id_bodega = " + idBodega + " "
                + "WHERE COALESCE(p.estado, true) = true "
                + "ORDER BY p.descripcion";

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sqlProductos);
            int count = 0;
            while (rs.next()) {
                double cant = rs.getDouble("cantidad");
                double pend = rs.getDouble("pendientes");
                modeloBuscar.addRow(new Object[]{
                    rs.getString("codigo_barras"),
                    rs.getString("descripcion") != null ? rs.getString("descripcion") : "",
                    df.format(cant),
                    df.format(pend),
                    df.format(cant - pend)
                });
                count++;
            }
            rs.close();
            lblCount.setText(count + " productos");
        } catch (Exception e) {
            System.err.println("Error cargando productos: " + e.getMessage());
        }

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

        final Runnable seleccionar = new Runnable() {
            @Override public void run() {
                int viewRow = tablaBuscar.getSelectedRow();
                if (viewRow < 0) return;
                int modelRow = tablaBuscar.convertRowIndexToModel(viewRow);
                String codigo = modeloBuscar.getValueAt(modelRow, 0).toString();
                txtCodigoBarras.setText(codigo);
                dlg.dispose();
                actualizarPreview();
                txtCantidadNueva.requestFocus();
            }
        };

        tablaBuscar.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) seleccionar.run();
            }
        });

        tablaBuscar.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sel");
        tablaBuscar.getActionMap().put("sel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { seleccionar.run(); }
        });

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
        if (modoVer) return;

        String codigoBarras = txtCodigoBarras.getText().trim();
        String cantNuevaStr = txtCantidadNueva.getText().trim();
        String pendNuevaStr = txtPendientesNueva.getText().trim();

        if (codigoBarras.isEmpty()) {
            txtCodigoBarras.requestFocus();
            return;
        }
        if (cantNuevaStr.isEmpty() && pendNuevaStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Ingrese al menos una cantidad nueva o pendientes nuevos.");
            txtCantidadNueva.requestFocus();
            return;
        }

        // Refrescar preview si no esta cargado o si el codigo cambio
        if (previewIdProducto <= 0) {
            actualizarPreview();
        }
        if (previewIdProducto <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Producto con código '" + codigoBarras + "' no encontrado.",
                    "Producto no encontrado", JOptionPane.WARNING_MESSAGE);
            txtCodigoBarras.selectAll();
            txtCodigoBarras.requestFocus();
            return;
        }

        // Si vacio, no cambia (queda igual al actual)
        double cantNueva = previewCantActual;
        double pendNueva = previewPendActual;

        if (!cantNuevaStr.isEmpty()) {
            try {
                cantNueva = Double.parseDouble(cantNuevaStr);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "La cantidad nueva debe ser un número válido.");
                txtCantidadNueva.requestFocus();
                txtCantidadNueva.selectAll();
                return;
            }
        }
        if (!pendNuevaStr.isEmpty()) {
            try {
                pendNueva = Double.parseDouble(pendNuevaStr);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Los pendientes deben ser un número válido.");
                txtPendientesNueva.requestFocus();
                txtPendientesNueva.selectAll();
                return;
            }
        }

        // Validacion: pendientes no puede ser mayor que cantidad
        if (pendNueva > cantNueva) {
            int resp = JOptionPane.showConfirmDialog(this,
                    "Los pendientes nuevos (" + df.format(pendNueva) + ") superan la cantidad nueva ("
                    + df.format(cantNueva) + ").\nEsto deja disponible negativo. ¿Desea continuar?",
                    "Advertencia", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (resp != JOptionPane.YES_OPTION) return;
        }

        // Verificar duplicado
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (modelo.getValueAt(i, COL_CODIGO).toString().equals(codigoBarras)) {
                JOptionPane.showMessageDialog(this, "Este producto ya fue agregado.");
                limpiarInputsProducto();
                return;
            }
        }

        double difCant = cantNueva - previewCantActual;
        double difPend = pendNueva - previewPendActual;

        modelo.addRow(new Object[]{
            String.valueOf(previewIdProducto),
            codigoBarras,
            previewDescripcion,
            df.format(previewCantActual),
            df.format(cantNueva),
            (difCant >= 0 ? "+" : "") + df.format(difCant),
            df.format(previewPendActual),
            df.format(pendNueva),
            (difPend >= 0 ? "+" : "") + df.format(difPend),
            txtObsProducto.getText().trim()
        });

        actualizarTotal();
        limpiarInputsProducto();
    }

    private void limpiarInputsProducto() {
        txtCodigoBarras.setText("");
        txtCantidadNueva.setText("");
        txtPendientesNueva.setText("");
        txtObsProducto.setText("");
        lblInfoActual.setText(" ");
        previewIdProducto = -1;
        previewCantActual = 0;
        previewPendActual = 0;
        txtCodigoBarras.requestFocus();
    }

    private void actualizarTotal() {
        int cAum = 0, cDis = 0, pAum = 0, pDis = 0;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            try {
                double dC = Double.parseDouble(modelo.getValueAt(i, COL_DIF_CANT).toString()
                        .replace(",", "").replace("+", ""));
                if (dC > 0) cAum++; else if (dC < 0) cDis++;
            } catch (Exception ignore) {}
            try {
                double dP = Double.parseDouble(modelo.getValueAt(i, COL_DIF_PEND).toString()
                        .replace(",", "").replace("+", ""));
                if (dP > 0) pAum++; else if (dP < 0) pDis++;
            } catch (Exception ignore) {}
        }
        lblTotal.setText(modelo.getRowCount() + " productos  |  "
                + "Cant: +" + cAum + " / -" + cDis + "  |  "
                + "Pend: +" + pAum + " / -" + pDis);
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
                "Se ajustarán " + modelo.getRowCount() + " productos en " + bodegaSel.toString() + ".\n"
                + "¿Desea continuar?",
                "Confirmar ajuste", JOptionPane.YES_NO_OPTION);
        if (resp != JOptionPane.YES_OPTION) return;

        int idBodega = bodegaSel.getId();
        int idUser = frm_main.id_user;
        String observacion = txtObsGeneral.getText().trim();

        double[][] productos = new double[modelo.getRowCount()][7];
        String[] obsProductos = new String[modelo.getRowCount()];

        try {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                productos[i][0] = Double.parseDouble(modelo.getValueAt(i, COL_ID).toString());
                productos[i][1] = parseNum(modelo.getValueAt(i, COL_CANT_ACT));
                productos[i][2] = parseNum(modelo.getValueAt(i, COL_CANT_NUEVA));
                productos[i][3] = parseNum(modelo.getValueAt(i, COL_DIF_CANT));
                productos[i][4] = parseNum(modelo.getValueAt(i, COL_PEND_ACT));
                productos[i][5] = parseNum(modelo.getValueAt(i, COL_PEND_NUEVA));
                productos[i][6] = parseNum(modelo.getValueAt(i, COL_DIF_PEND));
                obsProductos[i] = modelo.getValueAt(i, COL_NOTA).toString();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error procesando datos:\n" + e.getMessage());
            return;
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

    private static double parseNum(Object v) {
        if (v == null) return 0;
        String s = v.toString().replace(",", "").replace("+", "").trim();
        if (s.isEmpty()) return 0;
        return Double.parseDouble(s);
    }
}
