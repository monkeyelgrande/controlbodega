/**
 *
 * @author Monkeyelgrande
 */
package Formularios_internos;

import Formularios.frm_productos;
import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.TextPrompt;
import Metodos.metodos;
import com.formdev.flatlaf.FlatClientProperties;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBproductos;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import modelos.Productos;
import modelos.Unidades;

/**
 * Creacion/edicion de productos. Rediseño Material (FlatLaf + iconos Font
 * Awesome del package fonts), consistente con jif_crear_user / EstiloCompras.
 *
 * Conserva los nombres de los campos publicos estaticos porque frm_productos,
 * jd_buscar_producto_padre y crear_nuevo_producto los referencian por nombre.
 * Los campos de dinero se autoformatean en vivo (miles con punto, sin
 * centavos). Se retiró la UI de "producto padre" (los labels se conservan
 * ocultos para no romper esas referencias externas).
 */
public class jif_crear_producto extends javax.swing.JDialog {

    public static String formulario = "";
    public static int id_unidad;

    // --- Componentes (nombres conservados por referencias externas) ---
    public static javax.swing.JButton btn_cargar;
    public static javax.swing.JButton btn_e_cod_barras;
    public static javax.swing.JButton btn_editar;
    public static javax.swing.JButton btn_guardar;
    public static javax.swing.JButton btn_limpiar;
    public static javax.swing.JCheckBox chk_cerrar;
    public static javax.swing.JComboBox jbox_tipo;
    public static javax.swing.JComboBox<Unidades> jbox_unidad;
    public static javax.swing.JTextArea jtxt_descripcion;
    public static javax.swing.JLabel lbl_id_producto_padre;
    public static javax.swing.JLabel lbl_producto_padre;
    public static javax.swing.JTextField txt_cant_paquete;
    public static javax.swing.JTextField txt_cod_barras;
    public static javax.swing.JTextField txt_id;
    public static javax.swing.JTextField txt_pcosto;
    public static javax.swing.JTextField txt_pventa;
    public static javax.swing.JTextField txt_pventa2;
    public static javax.swing.JTextField txt_pventa3;
    public static javax.swing.JTextField txt_stock_ideal;
    public static javax.swing.JTextField txt_stock_minimo;

    // --- Kardex ---
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JTable tablaKardex;
    private javax.swing.table.DefaultTableModel modeloKardex;
    private javax.swing.JPanel panelKardex;
    // Tab "Unidades de entrega" (paquetes -> bodega + bodega por unidad, por producto)
    private javax.swing.JTable tablaPaquetes;
    private javax.swing.table.DefaultTableModel modeloPaquetes;
    private javax.swing.JPanel panelUnidades;
    private javax.swing.JComboBox<modelos.Bodegas> comboBodegaUnidad;
    private java.util.List<modelos.Bodegas> listaBodegasUnidades = new java.util.ArrayList<>();

    public jif_crear_producto() {
        initUI();
        this.setLocationRelativeTo(null);
        cargar_campos_defecto();
        btn_editar.setVisible(false);
        metodos.addEscapeListenerWindowDialog(this);
        holders();
        metodos.EvitarTabEnJTextArea(jtxt_descripcion);
        btn_e_cod_barras.setVisible(false);
        Unidades.mostrarUnidades(jbox_unidad);
    }

    // ====================================================================
    //  Construccion de la interfaz (Material)
    // ====================================================================
    private void initUI() {
        setModal(true);
        setUndecorated(true);
        setSize(1180, 800);
        setMinimumSize(new Dimension(720, 620));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));

        root.add(EstiloCompras.header(FontAwesome.BOX, "Gestion de producto", new Runnable() {
            @Override
            public void run() {
                dispose();
            }
        }), BorderLayout.NORTH);

        tabbedPane = new javax.swing.JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.addTab("Producto", buildTabProducto());

        panelKardex = construirPanelKardex();
        tabbedPane.addTab("Kardex", panelKardex);

        panelUnidades = construirPanelUnidadesEntrega();
        tabbedPane.addTab("Unidades de entrega", panelUnidades);

        tabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                if (tabbedPane.getSelectedComponent() == panelUnidades) {
                    cargarUnidades(txt_id.getText());
                }
            }
        });

        root.add(tabbedPane, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    /** Tab 1: formulario de producto con secciones Material. */
    private JComponent buildTabProducto() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(EstiloCompras.BG_FORM);
        body.setBorder(BorderFactory.createEmptyBorder(20, 28, 16, 28));

        // ---- Labels de "padre" conservados pero OCULTOS (referencias externas) ----
        lbl_producto_padre = new JLabel("-");
        lbl_producto_padre.setVisible(false);
        lbl_id_producto_padre = new JLabel("0");
        lbl_id_producto_padre.setVisible(false);
        body.add(lbl_producto_padre);
        body.add(lbl_id_producto_padre);

        // ---------- IDENTIFICACION ----------
        body.add(EstiloCompras.sectionTitle("Identificacion"));
        body.add(Box.createVerticalStrut(10));

        txt_id = newField();
        txt_id.setEditable(false);
        txt_id.setEnabled(false);

        btn_cargar = EstiloCompras.secondaryBtn("Generar codigo", FontAwesome.SYNC);
        btn_cargar.addActionListener(e -> generarNuevoCodigo());

        JPanel idRow = new JPanel();
        idRow.setOpaque(false);
        idRow.setLayout(new BoxLayout(idRow, BoxLayout.X_AXIS));
        idRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        idRow.add(labeled("ID", txt_id, 110));
        idRow.add(Box.createHorizontalStrut(14));
        idRow.add(labeledButton("Codigo automatico", btn_cargar));
        idRow.add(Box.createHorizontalGlue());
        idRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        body.add(idRow);
        body.add(Box.createVerticalStrut(12));

        txt_cod_barras = newField();
        txt_cod_barras.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Obligatorio");
        txt_cod_barras.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                FontAwesome.icon(FontAwesome.BARCODE, 16f, EstiloCompras.TEXT_SECONDARY));
        txt_cod_barras.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent evt) {
                validarCodigoBarras();
            }
        });

        btn_e_cod_barras = EstiloCompras.secondaryBtn("Editar", FontAwesome.PENCIL);
        btn_e_cod_barras.addActionListener(e -> txt_cod_barras.setEnabled(true));

        JPanel codRow = new JPanel();
        codRow.setOpaque(false);
        codRow.setLayout(new BoxLayout(codRow, BoxLayout.X_AXIS));
        codRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        codRow.add(labeled("Codigo de barras", txt_cod_barras, 0));
        codRow.add(Box.createHorizontalStrut(10));
        codRow.add(labeledButton(" ", btn_e_cod_barras));
        codRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        body.add(codRow);
        body.add(Box.createVerticalStrut(12));

        jtxt_descripcion = new JTextArea();
        jtxt_descripcion.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        jtxt_descripcion.setLineWrap(true);
        jtxt_descripcion.setWrapStyleWord(true);
        jtxt_descripcion.setRows(3);
        body.add(labeledArea("Descripcion", jtxt_descripcion, 80));
        body.add(Box.createVerticalStrut(20));

        // ---------- INVENTARIO ----------
        body.add(EstiloCompras.sectionTitle("Inventario"));
        body.add(Box.createVerticalStrut(10));

        txt_stock_minimo = newField();
        txt_stock_minimo.setText("0");
        soloEnteros(txt_stock_minimo);
        txt_stock_ideal = newField();
        txt_stock_ideal.setText("0");
        soloEnteros(txt_stock_ideal);

        JPanel stockRow = new JPanel();
        stockRow.setOpaque(false);
        stockRow.setLayout(new BoxLayout(stockRow, BoxLayout.X_AXIS));
        stockRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        stockRow.add(labeled("Stock minimo", txt_stock_minimo, 0));
        stockRow.add(Box.createHorizontalStrut(14));
        stockRow.add(labeled("Stock ideal", txt_stock_ideal, 0));
        stockRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        body.add(stockRow);
        body.add(Box.createVerticalStrut(12));

        jbox_unidad = new JComboBox<>();
        EstiloCompras.styleCombo(jbox_unidad);
        txt_cant_paquete = newField();
        txt_cant_paquete.setText("1");
        soloEnteros(txt_cant_paquete);

        JPanel uniRow = new JPanel();
        uniRow.setOpaque(false);
        uniRow.setLayout(new BoxLayout(uniRow, BoxLayout.X_AXIS));
        uniRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        uniRow.add(labeled("Unidad", jbox_unidad, 0));
        uniRow.add(Box.createHorizontalStrut(14));
        uniRow.add(labeled("Cantidad por paquete", txt_cant_paquete, 0));
        uniRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        body.add(uniRow);
        body.add(Box.createVerticalStrut(20));

        // ---------- PRECIOS ----------
        body.add(EstiloCompras.sectionTitle("Precios"));
        body.add(Box.createVerticalStrut(10));

        txt_pcosto = newMoneyField();
        jbox_tipo = new JComboBox(new String[]{"FV", "FR"});
        EstiloCompras.styleCombo(jbox_tipo);

        JPanel costoRow = new JPanel();
        costoRow.setOpaque(false);
        costoRow.setLayout(new BoxLayout(costoRow, BoxLayout.X_AXIS));
        costoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        costoRow.add(labeled("Precio costo", txt_pcosto, 0));
        costoRow.add(Box.createHorizontalStrut(14));
        costoRow.add(labeled("Tipo", jbox_tipo, 160));
        costoRow.add(Box.createHorizontalGlue());
        costoRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        body.add(costoRow);
        body.add(Box.createVerticalStrut(12));

        txt_pventa = newMoneyField();
        txt_pventa2 = newMoneyField();
        txt_pventa3 = newMoneyField();

        JPanel ventaRow = new JPanel();
        ventaRow.setOpaque(false);
        ventaRow.setLayout(new BoxLayout(ventaRow, BoxLayout.X_AXIS));
        ventaRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        ventaRow.add(labeled("Precio venta 1", txt_pventa, 0));
        ventaRow.add(Box.createHorizontalStrut(14));
        ventaRow.add(labeled("Precio venta 2", txt_pventa2, 0));
        ventaRow.add(Box.createHorizontalStrut(14));
        ventaRow.add(labeled("Precio venta 3", txt_pventa3, 0));
        ventaRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        body.add(ventaRow);
        body.add(Box.createVerticalStrut(8));

        JScrollPane scroll = new JScrollPane(body,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(EstiloCompras.BG_FORM);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(EstiloCompras.BG_SECTION);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCompras.DIVIDER),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)));

        chk_cerrar = new JCheckBox("Cerrar al guardar");
        chk_cerrar.setSelected(true);
        chk_cerrar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chk_cerrar.setOpaque(false);
        chk_cerrar.setForeground(EstiloCompras.TEXT_SECONDARY);
        chk_cerrar.setFocusPainted(false);

        btn_limpiar = EstiloCompras.secondaryBtn("Limpiar", FontAwesome.SYNC);
        btn_limpiar.addActionListener(e -> limpiar());

        btn_editar = EstiloCompras.secondaryBtn("Editar", FontAwesome.EDIT);
        btn_editar.addActionListener(e -> habilitarEdicion());

        btn_guardar = EstiloCompras.primaryBtn("Guardar", FontAwesome.SAVE);
        btn_guardar.addActionListener(e -> guardar());

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(btn_editar);
        right.add(Box.createHorizontalStrut(8));
        right.add(btn_limpiar);
        right.add(Box.createHorizontalStrut(8));
        right.add(btn_guardar);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.add(chk_cerrar);

        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    // ---------- HELPERS UI ----------
    private JTextField newField() {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setPreferredSize(new Dimension(0, 38));
        return f;
    }

    /** Campo de dinero con auto-formato de miles en vivo. */
    private JTextField newMoneyField() {
        JTextField f = newField();
        f.setText("0");
        f.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                FontAwesome.icon(FontAwesome.FILE_INVOICE, 14f, EstiloCompras.TEXT_SECONDARY));
        metodos.instalarFormatoMiles(f);
        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                f.selectAll();
            }
        });
        return f;
    }

    private void soloEnteros(final JTextField f) {
        f.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent evt) {
                DB_consultas_R_D.validar_numeros(evt, evt.getKeyChar());
            }
        });
    }

    private JComponent labeled(String label, JComponent field, int fixedWidth) {
        return EstiloCompras.labeled(label, field, fixedWidth);
    }

    /** Bloque vertical etiqueta + boton (alinea el boton con los campos). */
    private JComponent labeledButton(String label, JButton btn) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(EstiloCompras.TEXT_SECONDARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(btn);
        p.setMaximumSize(new Dimension(220, 72));
        return p;
    }

    /** Bloque vertical etiqueta + JTextArea (con scroll y borde). */
    private JComponent labeledArea(String label, JTextArea area, int height) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(EstiloCompras.TEXT_SECONDARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane sp = new JScrollPane(area);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));
        sp.setPreferredSize(new Dimension(0, height));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(sp);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, height + 22));
        return p;
    }

    // ====================================================================
    //  Tab Kardex
    // ====================================================================
    private javax.swing.JPanel construirPanelKardex() {
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
        panel.setBackground(java.awt.Color.WHITE);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Info superior
        javax.swing.JLabel lblInfo = new javax.swing.JLabel("Historial de movimientos de inventario del producto");
        lblInfo.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        lblInfo.setForeground(new java.awt.Color(40, 53, 147));
        panel.add(lblInfo, java.awt.BorderLayout.NORTH);

        // Tabla kardex
        modeloKardex = new javax.swing.table.DefaultTableModel(
                new Object[]{"Fecha", "Hora", "Tipo", "Bodega", "Valor", "Cant. Anterior", "Cant. Nueva", "Pend. Anterior", "Pend. Nuevo", "Referencia", "Usuario", "Observación"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tablaKardex = new javax.swing.JTable(modeloKardex);
        tablaKardex.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        tablaKardex.setRowHeight(28);
        tablaKardex.setAutoCreateRowSorter(true);
        tablaKardex.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablaKardex.setShowGrid(false);
        tablaKardex.setIntercellSpacing(new java.awt.Dimension(0, 0));
        tablaKardex.setFillsViewportHeight(true);
        tablaKardex.setSelectionBackground(new java.awt.Color(197, 202, 233));

        // Header estilo Material
        tablaKardex.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                javax.swing.JLabel l = (javax.swing.JLabel) super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                l.setBackground(new java.awt.Color(40, 53, 147));
                l.setForeground(java.awt.Color.WHITE);
                l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 11));
                l.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
                l.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                return l;
            }
        });
        tablaKardex.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 36));
        tablaKardex.getTableHeader().setReorderingAllowed(false);

        // Body renderer con colores por tipo
        tablaKardex.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                javax.swing.JLabel l = (javax.swing.JLabel) super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
                l.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 6));
                l.setOpaque(true);

                if (sel) {
                    l.setBackground(new java.awt.Color(197, 202, 233));
                    l.setForeground(new java.awt.Color(33, 33, 33));
                } else {
                    l.setBackground(row % 2 == 0 ? java.awt.Color.WHITE : new java.awt.Color(245, 247, 251));
                    l.setForeground(new java.awt.Color(33, 33, 33));
                }

                // Colorear tipo de movimiento
                if (col == 2 && value != null && !sel) {
                    String tipo = value.toString().toUpperCase();
                    if (tipo.contains("INGRESO") || tipo.contains("POSITIVO") || tipo.contains("ENTRADA")) {
                        l.setForeground(new java.awt.Color(67, 160, 71));
                        l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
                    } else if (tipo.contains("VENTA") || tipo.contains("NEGATIVO") || tipo.contains("SALIDA") || tipo.contains("ORDEN") || tipo.contains("ANULACION")) {
                        l.setForeground(new java.awt.Color(229, 57, 53));
                        l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
                    }
                }

                // Numeros alineados a la derecha
                if (col >= 4 && col <= 8) {
                    l.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                } else {
                    l.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                }

                return l;
            }
        });

        // Anchos
        javax.swing.table.TableColumnModel cm = tablaKardex.getColumnModel();
        cm.getColumn(0).setPreferredWidth(85);   // Fecha
        cm.getColumn(1).setPreferredWidth(60);   // Hora
        cm.getColumn(2).setPreferredWidth(130);  // Tipo
        cm.getColumn(3).setPreferredWidth(120);  // Bodega
        cm.getColumn(4).setPreferredWidth(70);   // Valor
        cm.getColumn(5).setPreferredWidth(90);   // Cant anterior
        cm.getColumn(6).setPreferredWidth(90);   // Cant nueva
        cm.getColumn(7).setPreferredWidth(90);   // Pend anterior
        cm.getColumn(8).setPreferredWidth(90);   // Pend nuevo
        cm.getColumn(9).setPreferredWidth(100);  // Referencia
        cm.getColumn(10).setPreferredWidth(100); // Usuario
        cm.getColumn(11).setPreferredWidth(200); // Observacion

        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(tablaKardex);
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(224, 224, 224)));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        panel.add(scroll, java.awt.BorderLayout.CENTER);

        return panel;
    }

    /**
     * Carga el kardex (movimientos de inventario) de un producto. Debe llamarse
     * desde frm_productos al abrir en modo "ver".
     *
     * @param idProducto ID del producto
     */
    public void cargarKardex(String idProducto) {
        modeloKardex.setRowCount(0);

        String sql = "SELECT m.fecha, m.hora, m.tipo, b.nombre as bodega, "
                + "m.valor, m.cantidad_anterior, m.cantidad_nueva, "
                + "m.pendientes_anterior, m.pendientes_nuevo, "
                + "CASE WHEN m.tabla_referencia IS NOT NULL "
                + "     THEN m.tabla_referencia || ' #' || m.id_referencia "
                + "     ELSE '' END as referencia, "
                + "u.nombre as usuario, "
                + "COALESCE(m.observacion, '') as observacion "
                + "FROM movimientos_inventario m "
                + "JOIN bodegas b ON b.id = m.id_bodega "
                + "JOIN users u ON u.id = m.id_user "
                + "WHERE m.id_producto = " + idProducto + " "
                + "ORDER BY m.fecha DESC, m.id DESC";

        java.text.DecimalFormat df = new java.text.DecimalFormat("###,###.##");

        try {
            java.sql.ResultSet rs = conexiondb.DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                modeloKardex.addRow(new Object[]{
                    rs.getString("fecha"),
                    rs.getString("hora") != null ? rs.getString("hora") : "",
                    rs.getString("tipo"),
                    rs.getString("bodega"),
                    df.format(rs.getDouble("valor")),
                    df.format(rs.getDouble("cantidad_anterior")),
                    df.format(rs.getDouble("cantidad_nueva")),
                    df.format(rs.getDouble("pendientes_anterior")),
                    df.format(rs.getDouble("pendientes_nuevo")),
                    rs.getString("referencia"),
                    rs.getString("usuario"),
                    rs.getString("observacion")
                });
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error cargando kardex: " + e.getMessage());
        }

        // Actualizar titulo del tab
        tabbedPane.setTitleAt(1, "Kardex (" + modeloKardex.getRowCount() + ")");
    }

    // ====================================================================
    //  Tab Unidades de entrega
    // ====================================================================
    private javax.swing.JPanel construirPanelUnidadesEntrega() {
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
        panel.setBackground(java.awt.Color.WHITE);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Cargar bodegas una sola vez
        listaBodegasUnidades.clear();
        try {
            java.sql.ResultSet rs = conexiondb.DB_consultas_R_D.getTabla("select id,nombre from bodegas order by nombre");
            while (rs.next()) {
                listaBodegasUnidades.add(new modelos.Bodegas(rs.getInt("id"), rs.getString("nombre")));
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error cargando bodegas para unidades de entrega: " + e.getMessage());
        }

        // Info superior + selector de bodega por unidad
        javax.swing.JPanel norte = new javax.swing.JPanel(new java.awt.GridLayout(0, 1, 0, 4));
        norte.setOpaque(false);
        javax.swing.JLabel lblInfo = new javax.swing.JLabel("Unidades de entrega del producto (parte la cantidad entre bodegas)");
        lblInfo.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        lblInfo.setForeground(new java.awt.Color(40, 53, 147));
        javax.swing.JLabel lblNota = new javax.swing.JLabel("<html>Los paquetes salen en cajas completas de su bodega (mayor a menor) y "
                + "el sobrante de la <b>bodega por unidad</b>. Si no define unidades, el producto usa la selección automática.</html>");
        lblNota.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        lblNota.setForeground(new java.awt.Color(97, 97, 97));

        comboBodegaUnidad = new javax.swing.JComboBox<>();
        comboBodegaUnidad.addItem(new modelos.Bodegas(0, "(sin asignar)"));
        for (modelos.Bodegas b : listaBodegasUnidades) {
            comboBodegaUnidad.addItem(b);
        }
        javax.swing.JPanel filaUnidad = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        filaUnidad.setOpaque(false);
        javax.swing.JLabel lblUnidad = new javax.swing.JLabel("Bodega para entregas por unidad:");
        lblUnidad.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        filaUnidad.add(lblUnidad);
        filaUnidad.add(comboBodegaUnidad);

        norte.add(lblInfo);
        norte.add(lblNota);
        norte.add(filaUnidad);
        panel.add(norte, java.awt.BorderLayout.NORTH);

        // Tabla de paquetes (cantidad > 1)
        modeloPaquetes = new javax.swing.table.DefaultTableModel(
                new Object[]{"Etiqueta (opcional)", "Cantidad por paquete", "Bodega"}, 0);
        tablaPaquetes = new javax.swing.JTable(modeloPaquetes);
        tablaPaquetes.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        tablaPaquetes.setRowHeight(30);
        tablaPaquetes.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablaPaquetes.getTableHeader().setReorderingAllowed(false);
        tablaPaquetes.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                javax.swing.JLabel l = (javax.swing.JLabel) super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                l.setBackground(new java.awt.Color(40, 53, 147));
                l.setForeground(java.awt.Color.WHITE);
                l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
                l.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
                l.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                return l;
            }
        });
        tablaPaquetes.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 36));

        // Combo de bodegas para la columna "Bodega" de la tabla de paquetes
        javax.swing.JComboBox<modelos.Bodegas> comboPaqueteBodega = new javax.swing.JComboBox<>();
        for (modelos.Bodegas b : listaBodegasUnidades) {
            comboPaqueteBodega.addItem(b);
        }
        tablaPaquetes.getColumnModel().getColumn(2).setCellEditor(new javax.swing.DefaultCellEditor(comboPaqueteBodega));

        panel.add(new javax.swing.JScrollPane(tablaPaquetes), java.awt.BorderLayout.CENTER);

        // Botonera
        javax.swing.JPanel sur = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        sur.setOpaque(false);
        javax.swing.JButton btnAgregar = new javax.swing.JButton("Agregar paquete");
        javax.swing.JButton btnEliminar = new javax.swing.JButton("Eliminar paquete");
        javax.swing.JButton btnGuardar = new javax.swing.JButton("Guardar configuración");
        btnGuardar.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));

        btnAgregar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                modelos.Bodegas bodPorDefecto = listaBodegasUnidades.isEmpty() ? null : listaBodegasUnidades.get(0);
                modeloPaquetes.addRow(new Object[]{"", "", bodPorDefecto});
            }
        });
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (tablaPaquetes.isEditing()) {
                    tablaPaquetes.getCellEditor().stopCellEditing();
                }
                int fila = tablaPaquetes.getSelectedRow();
                if (fila >= 0) {
                    modeloPaquetes.removeRow(fila);
                }
            }
        });
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                guardarUnidadesUI();
            }
        });
        sur.add(btnAgregar);
        sur.add(btnEliminar);
        sur.add(btnGuardar);
        panel.add(sur, java.awt.BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Carga las unidades de entrega del producto indicado: la bodega por unidad
     * (renglón de paquete = 1) en el combo, y los paquetes (> 1) en la tabla.
     *
     * @param idProductoTxt ID del producto (texto del campo txt_id)
     */
    public void cargarUnidades(String idProductoTxt) {
        if (modeloPaquetes == null) {
            return;
        }
        modeloPaquetes.setRowCount(0);
        if (comboBodegaUnidad.getItemCount() > 0) {
            comboBodegaUnidad.setSelectedIndex(0); // (sin asignar)
        }
        int idProducto;
        try {
            idProducto = Integer.parseInt(idProductoTxt.trim());
        } catch (Exception e) {
            tabbedPane.setTitleAt(2, "Unidades de entrega");
            return;
        }

        java.util.List<modelos.ProductoUnidadEntrega> unidades
                = conexiondb.DBproductoUnidadEntrega.listarPorProducto(idProducto);
        for (modelos.ProductoUnidadEntrega u : unidades) {
            if (u.getCantidad_paquete() == 1) {
                seleccionarBodegaCombo(comboBodegaUnidad, u.getId_bodega());
            } else {
                modeloPaquetes.addRow(new Object[]{
                    u.getNombre() == null ? "" : u.getNombre(),
                    fmtCantidad(u.getCantidad_paquete()),
                    buscarBodega(u.getId_bodega())
                });
            }
        }
        tabbedPane.setTitleAt(2, "Unidades de entrega (" + unidades.size() + ")");
    }

    /**
     * Valida y persiste las unidades de entrega (bodega por unidad + paquetes)
     * del producto actual.
     */
    private void guardarUnidadesUI() {
        // El producto debe existir (FK por id_producto)
        if (conexiondb.DB_consultas_R_D.consultarId(txt_id.getText(), "productos") != 1) {
            JOptionPane.showMessageDialog(this, "Guarde el producto primero para poder configurar sus unidades de entrega.",
                    "Producto no guardado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (tablaPaquetes.isEditing()) {
            tablaPaquetes.getCellEditor().stopCellEditing();
        }
        int idProducto = Integer.parseInt(txt_id.getText().trim());

        // Bodega por unidad (0 = sin asignar)
        modelos.Bodegas bodUnidad = (modelos.Bodegas) comboBodegaUnidad.getSelectedItem();
        int idBodegaUnidad = (bodUnidad == null) ? 0 : bodUnidad.getId();

        // Paquetes de la tabla
        java.util.List<modelos.ProductoUnidadEntrega> unidades = new java.util.ArrayList<>();
        for (int i = 0; i < modeloPaquetes.getRowCount(); i++) {
            String etiqueta = ("" + (modeloPaquetes.getValueAt(i, 0) == null ? "" : modeloPaquetes.getValueAt(i, 0))).trim();

            double paquete;
            try {
                paquete = Double.parseDouble(("" + modeloPaquetes.getValueAt(i, 1)).replace(",", "").trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Paquete " + (i + 1) + ": la cantidad por paquete no es válida.",
                        "Datos inválidos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (paquete <= 1) {
                JOptionPane.showMessageDialog(this, "Paquete " + (i + 1) + ": la cantidad por paquete debe ser mayor que 1 "
                        + "(la unidad la cubre la bodega por unidad).",
                        "Datos inválidos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Object bodObj = modeloPaquetes.getValueAt(i, 2);
            if (!(bodObj instanceof modelos.Bodegas)) {
                JOptionPane.showMessageDialog(this, "Paquete " + (i + 1) + ": seleccione una bodega.",
                        "Datos inválidos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int idBodega = ((modelos.Bodegas) bodObj).getId();

            unidades.add(new modelos.ProductoUnidadEntrega(idProducto, etiqueta.isEmpty() ? null : etiqueta, paquete, idBodega));
        }

        // Si hay paquetes, la bodega por unidad es obligatoria
        if (!unidades.isEmpty() && idBodegaUnidad == 0) {
            JOptionPane.showMessageDialog(this, "Defina la bodega para entregas por unidad: es obligatoria cuando hay paquetes "
                    + "(de ahí salen las unidades sobrantes).",
                    "Falta bodega por unidad", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Agregar el renglón de bodega por unidad (paquete = 1) si está asignada
        if (idBodegaUnidad != 0) {
            unidades.add(new modelos.ProductoUnidadEntrega(idProducto, "Unidad", 1, idBodegaUnidad));
        }

        boolean ok = conexiondb.DBproductoUnidadEntrega.guardarUnidades(idProducto, unidades);
        if (ok) {
            JOptionPane.showMessageDialog(this, "Unidades de entrega guardadas.",
                    "Listo", JOptionPane.INFORMATION_MESSAGE);
            cargarUnidades(txt_id.getText());
        }
    }

    /**
     * Selecciona en el combo el item Bodegas cuyo id coincide.
     */
    private void seleccionarBodegaCombo(javax.swing.JComboBox<modelos.Bodegas> combo, int idBodega) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getId() == idBodega) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Busca un objeto Bodegas (ya cargado) por su id. Null si no existe.
     */
    private modelos.Bodegas buscarBodega(int idBodega) {
        for (modelos.Bodegas b : listaBodegasUnidades) {
            if (b.getId() == idBodega) {
                return b;
            }
        }
        return null;
    }

    /**
     * Formatea una cantidad: entero sin decimales, o con decimales si los
     * tiene.
     */
    private String fmtCantidad(double v) {
        if (v == Math.rint(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    // ====================================================================
    //  Logica
    // ====================================================================
    public void holders() {
        // El codigo de barras usa el placeholder nativo de FlatLaf (alineado
        // despues del icono); solo la descripcion usa TextPrompt.
        TextPrompt desc = new TextPrompt("Obligatorio", jtxt_descripcion);
    }

    public void cargar_campos_defecto() {
        txt_id.setText(DB_consultas_R_D.cargarId("productos"));
    }

    public boolean validaciones() {
        if (txt_cod_barras.getText().isEmpty()) {
            txt_cod_barras.setBackground(Color.pink);
            return false;
        } else {
            txt_cod_barras.setBackground(Color.white);
        }

        if (jtxt_descripcion.getText().isEmpty()) {
            jtxt_descripcion.setBackground(Color.pink);
            return false;
        } else {
            jtxt_descripcion.setBackground(Color.white);
        }

        return true;
    }

    private void guardar() {
        if (!validaciones()) {
            return;
        }
        DBproductos dbproductos = new DBproductos();

        Productos producto = new Productos();
        try {
            producto.setId(Integer.parseInt(txt_id.getText()));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e);
        }
        producto.setCodigo_barras(txt_cod_barras.getText());
        producto.setDescripcion(jtxt_descripcion.getText());
        try {
            producto.setStock_minimo(Integer.parseInt(txt_stock_minimo.getText()));
        } catch (Exception e) {
            producto.setStock_minimo(0);
        }
        try {
            producto.setStock_ideal(Integer.parseInt(txt_stock_ideal.getText()));
        } catch (Exception e) {
            producto.setStock_ideal(0);
        }
        try {
            producto.setId_unidad(jbox_unidad.getItemAt(jbox_unidad.getSelectedIndex()).getId());
        } catch (Exception e) {
            producto.setId_unidad(id_unidad);
        }
        try {
            producto.setTipo(jbox_tipo.getSelectedIndex());
        } catch (Exception e) {
            producto.setTipo(0);
        }
        try {
            producto.setCant_paquete(Integer.parseInt(txt_cant_paquete.getText()));
        } catch (Exception e) {
            producto.setCant_paquete(1);
        }
        // "Producto padre" retirado de la UI: se conserva el valor (0 por
        // defecto, o el del producto en edicion) para no alterar la BD.
        try {
            producto.setId_padre(Integer.parseInt(lbl_id_producto_padre.getText()));
        } catch (Exception e) {
            producto.setId_padre(0);
        }
        producto.setPrecio_costo(Double.parseDouble(metodos.EliminaCaracteres(txt_pcosto.getText(), ".")));
        producto.setPrecio_venta(Double.parseDouble(metodos.EliminaCaracteres(txt_pventa.getText(), ".")));
        producto.setPrecio_venta2(Double.parseDouble(metodos.EliminaCaracteres(txt_pventa2.getText(), ".")));
        producto.setPrecio_venta3(Double.parseDouble(metodos.EliminaCaracteres(txt_pventa3.getText(), ".")));

        if (DB_consultas_R_D.consultarId(txt_id.getText(), "productos") == 1) {
            dbproductos.Actualizar(producto);
        } else {
            dbproductos.Guardar(producto);
        }

        switch (formulario) {
            case "crear":
                frm_productos.btn_actualizar.doClick();
                break;

            case "ingreso_mercancia":
                JOptionPane.showMessageDialog(this, "Se ha creado el producto");
                jif_crear_ingreso_mercancia.txt_codigo_barras.setText(txt_cod_barras.getText());
                break;

            case "precios":
                // Creado desde el ingreso del modulo Precios: agregarlo de una
                // vez a la tabla del ingreso abierto.
                try {
                    if (Precios.jif_crear_ingreso_precios.instancia != null) {
                        Precios.jif_crear_ingreso_precios.instancia.agregar_cod(txt_cod_barras.getText(), 1);
                        Precios.jif_crear_ingreso_precios.calcular_total();
                    }
                } catch (Exception ex) {
                    System.out.println("No se pudo agregar el producto al ingreso: " + ex);
                }
                break;
        }
        limpiar();
        txt_id.setText(DB_consultas_R_D.cargarId("productos"));
        if (chk_cerrar.isSelected()) {
            this.dispose();
        }
    }

    public void limpiar() {
        txt_cod_barras.setText("");
        jtxt_descripcion.setText("");
        txt_cod_barras.setBackground(Color.white);
        jtxt_descripcion.setBackground(Color.white);
    }

    private void habilitarEdicion() {
        btn_e_cod_barras.setVisible(true);
        jtxt_descripcion.setEnabled(true);
        txt_stock_minimo.setEnabled(true);
        txt_stock_ideal.setEnabled(true);
        txt_pcosto.setEnabled(true);
        txt_pventa.setEnabled(true);
        txt_pventa2.setEnabled(true);
        txt_pventa3.setEnabled(true);
        jbox_unidad.setEnabled(true);
        btn_guardar.setEnabled(true);
        btn_limpiar.setEnabled(true);
        chk_cerrar.setEnabled(true);
        jbox_tipo.setEnabled(true);
    }

    private void validarCodigoBarras() {
        if (DB_consultas_R_D.consultar_existencia_campo_String("codigo_barras", txt_cod_barras.getText(), "productos") == 1) {
            JOptionPane.showMessageDialog(this, "El código de barras ingresado ya esta registrado.\nIngrese un código distinto");
            txt_cod_barras.setText("");
            txt_cod_barras.requestFocus();
        }
    }

    private void generarNuevoCodigo() {
        try {
            // Agrupa TODOS los codigos existentes por familia (su prefijo de
            // letras) y guarda el mayor numero de cada una. Asi cada sistema de
            // codigos (REM..., A..., solo numero, ...) continua su propia
            // secuencia de forma independiente.
            TreeMap<String, Integer> maxPorPrefijo = new TreeMap<>();
            ResultSet rs = DB_consultas_R_D.getTabla("SELECT codigo_barras FROM productos");
            while (rs != null && rs.next()) {
                String codigo = rs.getString("codigo_barras");
                if (codigo == null) {
                    continue;
                }
                codigo = codigo.trim();
                if (codigo.isEmpty()) {
                    continue;
                }
                String prefijo = codigo.replaceAll("[0-9]", "");    // parte de letras
                String numeroStr = codigo.replaceAll("[^0-9]", ""); // parte numerica
                int numero = 0;
                if (!numeroStr.isEmpty()) {
                    try {
                        numero = Integer.parseInt(numeroStr);
                    } catch (NumberFormatException nfe) {
                        numero = 0; // numero demasiado largo: se ignora
                    }
                }
                Integer actual = maxPorPrefijo.get(prefijo);
                if (actual == null || numero > actual) {
                    maxPorPrefijo.put(prefijo, numero);
                }
            }
            rs.close();

            // Sin productos aun: arranca la primera secuencia sin prefijo.
            if (maxPorPrefijo.isEmpty()) {
                txt_cod_barras.setText("1");
                return;
            }

            String prefijoElegido;
            if (maxPorPrefijo.size() == 1) {
                // Una sola familia: se continua directo, sin preguntar.
                prefijoElegido = maxPorPrefijo.firstKey();
            } else {
                // Varias familias: se pregunta cual continuar, mostrando de una
                // vez cual seria el proximo codigo de cada una.
                List<String> prefijos = new ArrayList<>(maxPorPrefijo.keySet());
                String[] opciones = new String[prefijos.size()];
                for (int i = 0; i < prefijos.size(); i++) {
                    String p = prefijos.get(i);
                    int proximo = maxPorPrefijo.get(p) + 1;
                    String etiqueta = p.isEmpty() ? "(solo numero)" : p;
                    opciones[i] = etiqueta + "  →  " + p + proximo;
                }
                String sel = (String) JOptionPane.showInputDialog(this,
                        "Existen varios sistemas de codigos.\nElija cual desea continuar:",
                        "Generar codigo", JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
                if (sel == null) {
                    return; // el usuario cancelo
                }
                prefijoElegido = prefijos.get(Arrays.asList(opciones).indexOf(sel));
            }

            int nuevoNumero = maxPorPrefijo.get(prefijoElegido) + 1;
            txt_cod_barras.setText(prefijoElegido + nuevoNumero);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al generar el código: " + e.getMessage());
        }
    }
}
