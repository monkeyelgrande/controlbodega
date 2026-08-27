/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Formularios;

import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import modelos.Bodegas;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Kardex de un producto: todos los movimientos de inventario (tabla
 * movimientos_inventario) con entradas, salidas y saldo por bodega, y un
 * panel lateral con estadisticas de rotacion de los ultimos 3 meses
 * (mes actual + 2 anteriores).
 *
 * Se abre con doble clic sobre un producto en frm_consulta.
 *
 * Mismo lenguaje visual Material (Indigo / Amber) de frm_consulta.
 *
 * @author M-Work
 */
public class jd_kardex_producto extends javax.swing.JDialog {

    // ================================================================
    // Paleta Material (igual a frm_consulta)
    // ================================================================
    private static final Color PRIMARY       = new Color(40, 53, 147);    // Indigo 800
    private static final Color PRIMARY_DARK  = new Color(26, 35, 126);    // Indigo 900
    private static final Color PRIMARY_LIGHT = new Color(92, 107, 192);   // Indigo 400
    private static final Color ACCENT_DARK   = new Color(255, 160, 0);    // Amber 700
    private static final Color SURFACE       = new Color(250, 250, 252);
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color TEXT_PRIMARY  = new Color(33, 33, 33);
    private static final Color TEXT_SECOND   = new Color(117, 117, 117);
    private static final Color DIVIDER       = new Color(224, 224, 224);
    private static final Color ROW_ALT       = new Color(245, 247, 251);
    private static final Color SELECTION     = new Color(197, 202, 233);
    private static final Color SUCCESS       = new Color(67, 160, 71);    // Green 600
    private static final Color DANGER        = new Color(229, 57, 53);    // Red 600

    private static final String FONT_FAMILY = "Segoe UI";

    private static final String[] MESES_ES = {
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    /** Cobertura objetivo para la sugerencia de compra (pedido del cliente). */
    private static final int DIAS_COBERTURA_OBJETIVO = 60;

    /**
     * Mezcla un color con el fondo de la card y devuelve un color SOLIDO.
     * Importante: componentes opacos con fondo semitransparente causan
     * "smearing" al repintar (los textos viejos quedan pintados debajo).
     */
    private static Color tint(Color c, int alpha) {
        int r = (c.getRed()   * alpha + CARD_BG.getRed()   * (255 - alpha)) / 255;
        int g = (c.getGreen() * alpha + CARD_BG.getGreen() * (255 - alpha)) / 255;
        int b = (c.getBlue()  * alpha + CARD_BG.getBlue()  * (255 - alpha)) / 255;
        return new Color(r, g, b);
    }

    // ================================================================
    // Datos del producto
    // ================================================================
    private final int idProducto;
    private final String codigo;
    private final String descripcion;

    // ================================================================
    // Componentes
    // ================================================================
    private JComboBox<Bodegas> cmbBodega;
    private JComboBox<String> cmbTipoMov;

    private JTable tabla;
    private DefaultTableModel modelo;
    /** afecta_cantidad por fila del modelo (para colorear entrada/salida/reserva). */
    private final java.util.List<Integer> afectasFilas = new java.util.ArrayList<Integer>();

    private JLabel chipMovimientos;
    private JLabel chipEntradas;
    private JLabel chipSalidas;
    private JLabel chipStockActual;

    // Panel de estadisticas
    private int mesesPeriodo = 3;
    private JToggleButton btnPeriodo3m;
    private JToggleButton btnPeriodo6m;
    private JLabel tituloSalidasPeriodo;
    private JLabel valSalidas3m;
    private JLabel valPromMensual;
    private JLabel valRotacion;
    private JLabel valCobertura;
    private JLabel valCompraSugerida;
    private JLabel detEntradas;
    private JLabel detAjustes;
    private JLabel detTraslados;
    private JLabel detStockActual;
    private JLabel detStockPromedio;
    private JLabel detUltimaEntrada;
    private JLabel detUltimaSalida;
    private JLabel lblPeriodo;
    private ChartBarras chart;
    private JPanel statsCard;

    private final DecimalFormat df = new DecimalFormat("###,###.##");

    // ================================================================
    // Constructor
    // ================================================================
    public jd_kardex_producto(java.awt.Window parent, int idProducto, String codigo, String descripcion) {
        super(parent, "Kardex de Producto", ModalityType.APPLICATION_MODAL);
        this.idProducto = idProducto;
        this.codigo = codigo != null ? codigo : "";
        this.descripcion = descripcion != null ? descripcion : "";

        initComponentes();
        cargarBodegas();
        cargarMovimientos();
        cargarEstadisticas();

        setLocationRelativeTo(parent);
        try { metodos.addEscapeListenerWindowDialog(this); } catch (Exception ignore) {}
    }

    // ================================================================
    // UI
    // ================================================================
    private void initComponentes() {
        setSize(1500, 950);
        setMinimumSize(new Dimension(1200, 650));
        setLayout(new BorderLayout());
        getContentPane().setBackground(SURFACE);

        add(buildAppBar(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setBackground(SURFACE);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Columna izquierda: filtros + tabla + footer
        JPanel colIzquierda = new JPanel(new BorderLayout(0, 16));
        colIzquierda.setOpaque(false);
        colIzquierda.add(buildFiltersCard(), BorderLayout.NORTH);
        colIzquierda.add(buildTableCard(),   BorderLayout.CENTER);
        colIzquierda.add(buildFooterCard(),  BorderLayout.SOUTH);

        content.add(colIzquierda, BorderLayout.CENTER);
        content.add(buildStatsCard(), BorderLayout.EAST);

        add(content, BorderLayout.CENTER);
    }

    // ----------------------------------------------------------------
    // App bar
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
        appBar.setBorder(new EmptyBorder(16, 24, 16, 24));
        appBar.setPreferredSize(new Dimension(0, 92));

        JLabel lblTitulo = new JLabel("Kardex · " + descripcion);
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font(FONT_FAMILY, Font.BOLD, 20));

        JLabel lblSubtitulo = new JLabel("Código: " + codigo
                + "  ·  Movimientos de inventario y rotación");
        lblSubtitulo.setForeground(new Color(255, 255, 255, 200));
        lblSubtitulo.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.add(lblTitulo);
        titleWrap.add(Box.createVerticalStrut(2));
        titleWrap.add(lblSubtitulo);

        appBar.add(titleWrap, BorderLayout.WEST);
        return appBar;
    }

    // ----------------------------------------------------------------
    // Card de filtros
    // ----------------------------------------------------------------
    private JPanel buildFiltersCard() {
        JPanel card = new CardPanel();
        card.setLayout(new GridBagLayout());
        card.setBorder(new CompoundBorderShadow(new EmptyBorder(14, 20, 14, 20)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        c.gridy = 0;
        c.gridx = 0; c.weightx = 0;
        card.add(buildFieldLabel("BODEGA"), c);
        c.gridx = 1;
        card.add(buildFieldLabel("MOVIMIENTOS"), c);

        c.gridy = 1;

        cmbBodega = new JComboBox<Bodegas>();
        styleComboBox(cmbBodega);
        cmbBodega.setPreferredSize(new Dimension(260, 40));
        c.gridx = 0; c.weightx = 0.5;
        card.add(cmbBodega, c);

        cmbTipoMov = new JComboBox<String>(new String[]{
            "Entradas y salidas",
            "Solo entradas",
            "Solo salidas",
            "Todos (incluye reservas de órdenes)"});
        styleComboBox(cmbTipoMov);
        cmbTipoMov.setPreferredSize(new Dimension(280, 40));
        c.gridx = 1; c.weightx = 0.5;
        card.add(cmbTipoMov, c);

        // Relleno para empujar a la izquierda
        c.gridx = 2; c.weightx = 1;
        card.add(Box.createHorizontalGlue(), c);

        cmbBodega.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                cargarMovimientos();
                cargarEstadisticas();
            }
        });
        cmbTipoMov.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { cargarMovimientos(); }
        });

        return card;
    }

    private JLabel buildFieldLabel(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        l.setForeground(TEXT_SECOND);
        return l;
    }

    // ----------------------------------------------------------------
    // Card con la tabla
    // ----------------------------------------------------------------
    private JPanel buildTableCard() {
        JPanel card = new CardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new CompoundBorderShadow(new EmptyBorder(0, 0, 0, 0)));

        modelo = new DefaultTableModel(new Object[]{
            "FECHA", "HORA", "TIPO", "BODEGA", "ENTRADA", "SALIDA", "SALDO BOD.", "USUARIO", "DETALLE"}, 0) {
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
        // Sin sorter: el kardex se lee en orden cronologico (mas reciente arriba)

        JTableHeader header = tabla.getTableHeader();
        header.setDefaultRenderer(new MaterialHeaderRenderer());
        header.setPreferredSize(new Dimension(0, 44));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER));

        KardexCellRenderer bodyRenderer = new KardexCellRenderer();
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(bodyRenderer);
        }

        TableColumnModel cm = tabla.getColumnModel();
        cm.getColumn(0).setPreferredWidth(105);  // fecha
        cm.getColumn(1).setPreferredWidth(60);   // hora
        cm.getColumn(2).setPreferredWidth(165);  // tipo
        cm.getColumn(3).setPreferredWidth(115);  // bodega
        cm.getColumn(4).setPreferredWidth(80);   // entrada
        cm.getColumn(5).setPreferredWidth(80);   // salida
        cm.getColumn(6).setPreferredWidth(90);   // saldo
        cm.getColumn(7).setPreferredWidth(100);  // usuario
        cm.getColumn(8).setPreferredWidth(230);  // detalle

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);
        scroll.setBackground(CARD_BG);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ----------------------------------------------------------------
    // Footer con chips
    // ----------------------------------------------------------------
    private JPanel buildFooterCard() {
        JPanel card = new CardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new CompoundBorderShadow(new EmptyBorder(12, 20, 12, 20)));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chips.setOpaque(false);

        chipMovimientos = buildChip("Movimientos", "0", PRIMARY);
        chipEntradas    = buildChip("Entradas",    "0", SUCCESS);
        chipSalidas     = buildChip("Salidas",     "0", DANGER);
        chipStockActual = buildChip("Stock actual","0", PRIMARY_LIGHT);

        chips.add(chipMovimientos);
        chips.add(chipEntradas);
        chips.add(chipSalidas);
        chips.add(chipStockActual);

        card.add(chips, BorderLayout.WEST);
        return card;
    }

    private JLabel buildChip(String titulo, String valor, Color color) {
        JLabel chip = new JLabel();
        chip.setOpaque(true);
        chip.setBackground(tint(color, 25));
        chip.setBorder(new EmptyBorder(8, 14, 8, 14));
        chip.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        chip.putClientProperty("chipColor", color);
        updateChip(chip, titulo, valor);
        return chip;
    }

    private void updateChip(JLabel chip, String titulo, String valor) {
        Color color = (Color) chip.getClientProperty("chipColor");
        chip.setText("<html><span style='color:#757575;font-size:10px'>"
                + titulo.toUpperCase() + "</span>&nbsp;&nbsp;<b style='font-size:13px;color:rgb("
                + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")'>"
                + valor + "</b></html>");
    }

    // ----------------------------------------------------------------
    // Card de estadisticas de rotacion (columna derecha)
    // ----------------------------------------------------------------
    private JPanel buildStatsCard() {
        JPanel card = new CardPanel();
        statsCard = card;
        card.setLayout(new BorderLayout());
        card.setBorder(new CompoundBorderShadow(new EmptyBorder(18, 20, 18, 20)));
        card.setPreferredSize(new Dimension(390, 0));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("ROTACIÓN DEL PRODUCTO");
        titulo.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        titulo.setForeground(TEXT_PRIMARY);

        lblPeriodo = new JLabel("Últimos 3 meses");
        lblPeriodo.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11));
        lblPeriodo.setForeground(TEXT_SECOND);

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.add(titulo);
        titleWrap.add(Box.createVerticalStrut(2));
        titleWrap.add(lblPeriodo);

        // Toggle de periodo: 3 o 6 meses
        btnPeriodo3m = buildTogglePeriodo("3 MESES");
        btnPeriodo6m = buildTogglePeriodo("6 MESES");
        btnPeriodo3m.setSelected(true);
        btnPeriodo3m.setForeground(Color.WHITE);
        ButtonGroup grupoPeriodo = new ButtonGroup();
        grupoPeriodo.add(btnPeriodo3m);
        grupoPeriodo.add(btnPeriodo6m);

        ActionListener cambioPeriodo = new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                int m = btnPeriodo6m.isSelected() ? 6 : 3;
                if (m != mesesPeriodo) {
                    mesesPeriodo = m;
                    cargarEstadisticas();
                }
            }
        };
        btnPeriodo3m.addActionListener(cambioPeriodo);
        btnPeriodo6m.addActionListener(cambioPeriodo);

        JPanel toggles = new JPanel(new GridLayout(1, 2, 4, 0));
        toggles.setOpaque(false);
        toggles.add(btnPeriodo3m);
        toggles.add(btnPeriodo6m);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        headerRow.add(titleWrap, BorderLayout.WEST);
        headerRow.add(toggles, BorderLayout.EAST);

        inner.add(headerRow);
        inner.add(Box.createVerticalStrut(14));

        // ------ Tiles 2x2 ------
        JPanel tiles = new JPanel(new GridLayout(2, 2, 10, 10));
        tiles.setOpaque(false);
        tiles.setAlignmentX(Component.LEFT_ALIGNMENT);
        tiles.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        valSalidas3m   = new JLabel("0");
        valPromMensual = new JLabel("0");
        valRotacion    = new JLabel("-");
        valCobertura   = new JLabel("-");

        tituloSalidasPeriodo = new JLabel("SALIDAS 3M");
        tiles.add(buildTile(tituloSalidasPeriodo, valSalidas3m, DANGER));
        tiles.add(buildTile(new JLabel("PROM. MENSUAL"), valPromMensual, ACCENT_DARK));
        tiles.add(buildTile(new JLabel("ROTACIÓN"), valRotacion, PRIMARY));
        tiles.add(buildTile(new JLabel("COBERTURA"), valCobertura, SUCCESS));

        inner.add(tiles);
        inner.add(Box.createVerticalStrut(10));

        // ------ Sugerencia de compra para cobertura objetivo ------
        valCompraSugerida = new JLabel("-");
        JPanel tileCompra = buildTile(
                new JLabel("COMPRA SUGERIDA · COBERTURA " + DIAS_COBERTURA_OBJETIVO + " DÍAS"),
                valCompraSugerida, PRIMARY_LIGHT);
        tileCompra.setAlignmentX(Component.LEFT_ALIGNMENT);
        tileCompra.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        inner.add(tileCompra);
        inner.add(Box.createVerticalStrut(16));

        // ------ Grafico entradas vs salidas por mes ------
        JLabel lblChart = new JLabel("ENTRADAS VS SALIDAS POR MES");
        lblChart.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        lblChart.setForeground(TEXT_SECOND);
        lblChart.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(lblChart);
        inner.add(Box.createVerticalStrut(6));

        chart = new ChartBarras();
        chart.setAlignmentX(Component.LEFT_ALIGNMENT);
        chart.setPreferredSize(new Dimension(340, 190));
        chart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        inner.add(chart);
        inner.add(Box.createVerticalStrut(16));

        // ------ Detalle ------
        JLabel lblDetalle = new JLabel("DETALLE DEL PERÍODO");
        lblDetalle.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        lblDetalle.setForeground(TEXT_SECOND);
        lblDetalle.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(lblDetalle);
        inner.add(Box.createVerticalStrut(6));

        detEntradas      = addDetalleRow(inner, "Entradas 3M");
        detAjustes       = addDetalleRow(inner, "Ajustes netos 3M");
        detTraslados     = addDetalleRow(inner, "Traslados netos 3M");
        detStockActual   = addDetalleRow(inner, "Stock actual");
        detStockPromedio = addDetalleRow(inner, "Stock promedio 3M");
        detUltimaEntrada = addDetalleRow(inner, "Última entrada");
        detUltimaSalida  = addDetalleRow(inner, "Última salida");

        inner.add(Box.createVerticalGlue());

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    /** Boton de un toggle de periodo (3 / 6 meses) estilo segmentado. */
    private JToggleButton buildTogglePeriodo(String texto) {
        final JToggleButton b = new JToggleButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected() ? PRIMARY : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(isSelected() ? PRIMARY : DIVIDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font(FONT_FAMILY, Font.BOLD, 10));
        b.setForeground(TEXT_SECOND);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(6, 10, 6, 10));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addItemListener(new java.awt.event.ItemListener() {
            @Override public void itemStateChanged(java.awt.event.ItemEvent e) {
                b.setForeground(b.isSelected() ? Color.WHITE : TEXT_SECOND);
            }
        });
        return b;
    }

    /** Tile de estadistica con titulo pequeno y valor grande. */
    private JPanel buildTile(JLabel lblTitulo, JLabel valor, Color color) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(tint(color, 18));
        tile.setBorder(new EmptyBorder(10, 12, 10, 12));

        lblTitulo.setFont(new Font(FONT_FAMILY, Font.BOLD, 10));
        lblTitulo.setForeground(TEXT_SECOND);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        valor.setFont(new Font(FONT_FAMILY, Font.BOLD, 20));
        valor.setForeground(color);
        valor.setAlignmentX(Component.LEFT_ALIGNMENT);

        tile.add(lblTitulo);
        tile.add(Box.createVerticalStrut(3));
        tile.add(valor);
        return tile;
    }

    /** Fila clave-valor del bloque de detalle. Devuelve el label del valor. */
    private JLabel addDetalleRow(JPanel parent, String titulo) {
        JPanel fila = new JPanel(new BorderLayout());
        fila.setOpaque(false);
        fila.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 244)),
                new EmptyBorder(6, 0, 6, 0)));
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        lblTitulo.setForeground(TEXT_SECOND);

        JLabel lblValor = new JLabel("-");
        lblValor.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        lblValor.setForeground(TEXT_PRIMARY);
        // Guardar el label del titulo para poder renombrar "3M"/"6M" al cambiar periodo
        lblValor.putClientProperty("lblTitulo", lblTitulo);

        fila.add(lblTitulo, BorderLayout.WEST);
        fila.add(lblValor, BorderLayout.EAST);
        parent.add(fila);
        return lblValor;
    }

    /** Renombra el titulo de una fila de detalle creada con addDetalleRow. */
    private void setTituloDetalle(JLabel lblValor, String titulo) {
        JLabel lblTitulo = (JLabel) lblValor.getClientProperty("lblTitulo");
        if (lblTitulo != null) lblTitulo.setText(titulo);
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXT_PRIMARY);
        combo.setBorder(new RoundedBorder(8, DIVIDER));
        combo.setFocusable(false);
        ((JComponent) combo.getRenderer()).setOpaque(true);
    }

    // ================================================================
    // Carga de bodegas
    // ================================================================
    private void cargarBodegas() {
        try {
            new Bodegas().mostrarBodegasConTodas(cmbBodega);
        } catch (Exception e) {
            System.err.println("Error cargando bodegas: " + e.getMessage());
        }
    }

    private int idBodegaSeleccionada() {
        Bodegas b = (Bodegas) cmbBodega.getSelectedItem();
        return (b != null) ? b.getId() : 0;
    }

    // ================================================================
    // Carga de movimientos (kardex)
    // ================================================================
    private void cargarMovimientos() {
        if (modelo == null) return;
        modelo.setRowCount(0);
        afectasFilas.clear();

        int idBodega = idBodegaSeleccionada();
        int filtroTipo = (cmbTipoMov != null) ? cmbTipoMov.getSelectedIndex() : 0;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT m.fecha, m.hora, m.tipo, m.valor, ")
           .append("       m.afecta_cantidad, m.afecta_pendientes, ")
           .append("       m.cantidad_nueva, m.observacion, ")
           .append("       m.id_referencia, m.tabla_referencia, ")
           .append("       b.nombre AS bodega, u.nombre AS usuario ")
           .append("FROM movimientos_inventario m ")
           .append("INNER JOIN bodegas b ON b.id = m.id_bodega ")
           .append("LEFT JOIN users u ON u.id = m.id_user ")
           .append("WHERE m.id_producto = ").append(idProducto).append(" ");

        if (idBodega > 0) {
            sql.append("  AND m.id_bodega = ").append(idBodega).append(" ");
        }

        switch (filtroTipo) {
            case 0: sql.append("  AND m.afecta_cantidad <> 0 "); break; // entradas y salidas
            case 1: sql.append("  AND m.afecta_cantidad = 1 ");  break; // solo entradas
            case 2: sql.append("  AND m.afecta_cantidad = -1 "); break; // solo salidas
            default: break;                                             // todos
        }

        sql.append("ORDER BY m.fecha DESC, m.id DESC ");
        sql.append("LIMIT 3000");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        double totalEntradas = 0;
        double totalSalidas = 0;
        int filas = 0;

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql.toString());
            while (rs.next()) {
                java.sql.Date fecha = rs.getDate("fecha");
                String hora = rs.getString("hora");
                String tipo = rs.getString("tipo");
                double valor = rs.getDouble("valor");
                int afectaCantidad = rs.getInt("afecta_cantidad");
                int afectaPendientes = rs.getInt("afecta_pendientes");
                double saldo = rs.getDouble("cantidad_nueva");
                String observacion = rs.getString("observacion");
                int idRef = rs.getInt("id_referencia");
                boolean tieneRef = !rs.wasNull();
                String tablaRef = rs.getString("tabla_referencia");
                String bodega = rs.getString("bodega");
                String usuario = rs.getString("usuario");

                String entrada = (afectaCantidad == 1)  ? df.format(valor) : "";
                String salida  = (afectaCantidad == -1) ? df.format(valor) : "";

                String detalle = construirDetalle(observacion, tablaRef, tieneRef ? idRef : null,
                        afectaCantidad, afectaPendientes, valor);

                modelo.addRow(new Object[]{
                    fecha != null ? sdf.format(fecha) : "",
                    hora != null ? hora : "",
                    etiquetaTipo(tipo),
                    bodega != null ? bodega : "",
                    entrada,
                    salida,
                    df.format(saldo),
                    usuario != null ? usuario : "",
                    detalle
                });
                afectasFilas.add(afectaCantidad);

                if (afectaCantidad == 1)  totalEntradas += valor;
                if (afectaCantidad == -1) totalSalidas  += valor;
                filas++;
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error consultando kardex: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Error consultando el kardex:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        updateChip(chipMovimientos, "Movimientos", String.valueOf(filas));
        updateChip(chipEntradas,    "Entradas",    df.format(totalEntradas));
        updateChip(chipSalidas,     "Salidas",     df.format(totalSalidas));
    }

    /** Detalle legible: referencia al documento origen + observacion. */
    private String construirDetalle(String observacion, String tablaRef, Integer idRef,
            int afectaCantidad, int afectaPendientes, double valor) {

        StringBuilder sb = new StringBuilder();

        // Reserva pura (no mueve stock fisico): mostrar cuanto reservo/libero
        if (afectaCantidad == 0 && afectaPendientes != 0) {
            sb.append(afectaPendientes > 0 ? "[Reserva +" : "[Libera -")
              .append(df.format(valor)).append("] ");
        }

        if (tablaRef != null && idRef != null) {
            String doc;
            if (tablaRef.equals("ingresos_mercancias_cabecera")) doc = "Ingreso";
            else if (tablaRef.equals("facturas_cabeceras"))      doc = "Factura";
            else if (tablaRef.equals("entregas_productos_cabecera")) doc = "Entrega";
            else if (tablaRef.equals("traslados_productos"))     doc = "Traslado";
            else if (tablaRef.equals("devoluciones"))            doc = "Devolución";
            else doc = tablaRef;
            sb.append(doc).append(" #").append(idRef);
        }

        if (observacion != null && !observacion.trim().isEmpty()) {
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append(observacion.trim());
        }
        return sb.toString();
    }

    /** Nombre legible de cada tipo de movimiento. */
    private static String etiquetaTipo(String tipo) {
        if (tipo == null) return "";
        switch (tipo) {
            case "INGRESO":                return "Ingreso mercancía";
            case "EDICION_INGRESO":        return "Edición ingreso";
            case "ELIM_INGRESO":           return "Ingreso eliminado";
            case "VENTA":                  return "Venta";
            case "EDICION_VENTA":          return "Edición venta";
            case "ANULACION_VENTA":        return "Venta anulada";
            case "ORDEN":                  return "Orden (reserva)";
            case "EDICION_ORDEN":          return "Edición orden (reserva)";
            case "ANULACION_ORDEN":        return "Orden anulada (libera)";
            case "ORDEN_REFERENCIADA":     return "Orden referenciada";
            case "EDICION_ORDEN_REF":      return "Edición orden ref.";
            case "ANULACION_ORDEN_REF":    return "Anulación orden ref.";
            case "ENTREGA":                return "Entrega";
            case "EDICION_ENTREGA":        return "Edición entrega";
            case "ELIM_ENTREGA":           return "Entrega eliminada";
            case "ANULACION_ENTREGA":      return "Entrega anulada";
            case "DEVOLUCION":             return "Devolución";
            case "EDICION_DEVOLUCION":     return "Edición devolución";
            case "ELIM_DEVOLUCION":        return "Devolución eliminada";
            case "ANULACION_DEVOLUCION":   return "Devolución anulada";
            case "TRASLADO_SALIDA":        return "Traslado salida";
            case "TRASLADO_ENTRADA":       return "Traslado entrada";
            case "EDICION_TRASLADO_SAL":   return "Edición traslado sal.";
            case "EDICION_TRASLADO_ENT":   return "Edición traslado ent.";
            case "ELIM_TRASLADO_SAL":      return "Traslado eliminado (sal.)";
            case "ELIM_TRASLADO_ENT":      return "Traslado eliminado (ent.)";
            case "ANULACION_TRASLADO_SALIDA": return "Traslado anulado (sal.)";
            case "ANULACION_TRASLADO_ENTRADA": return "Traslado anulado (ent.)";
            case "AJUSTE_POSITIVO":        return "Ajuste inventario (+)";
            case "AJUSTE_NEGATIVO":        return "Ajuste inventario (-)";
            case "AUTOMATICO":             return "Automático (WO)";
            default:                       return tipo;
        }
    }

    // ================================================================
    // Estadisticas de rotacion: mes actual + 2 anteriores
    // ================================================================
    private void cargarEstadisticas() {
        if (valSalidas3m == null) return;

        int idBodega = idBodegaSeleccionada();
        String filtroBodega = (idBodega > 0) ? (" AND m.id_bodega = " + idBodega + " ") : " ";

        // Periodo seleccionado: mes actual + (n-1) anteriores
        final int n = mesesPeriodo;

        // Inicio del periodo: dia 1 del mes, (n-1) meses atras
        Calendar inicio = Calendar.getInstance();
        inicio.set(Calendar.DAY_OF_MONTH, 1);
        inicio.add(Calendar.MONTH, -(n - 1));

        // Claves y etiquetas de los n meses (en orden cronologico)
        String[] claveMes = new String[n];
        String[] labelMes = new String[n];
        Calendar it = (Calendar) inicio.clone();
        for (int i = 0; i < n; i++) {
            claveMes[i] = String.format("%04d-%02d", it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1);
            labelMes[i] = MESES_ES[it.get(Calendar.MONTH)];
            it.add(Calendar.MONTH, 1);
        }
        lblPeriodo.setText("Últimos " + n + " meses (" + labelMes[0] + " - " + labelMes[n - 1] + ")");

        double[] entradasMes = new double[n];
        double[] salidasMes = new double[n];
        double entradas3m = 0, salidas3m = 0, ajustesNetos = 0, trasladosNetos = 0, netoTotal = 0;

        // ------- 1) Agregados del periodo por mes -------
        // Entradas/salidas fisicas EXCLUYENDO traslados (movimiento interno,
        // no es compra ni consumo); traslados y ajustes se muestran aparte.
        String sqlMes = "SELECT to_char(m.fecha, 'YYYY-MM') AS mes, "
                + " SUM(CASE WHEN m.afecta_cantidad = 1  AND m.tipo NOT LIKE '%TRASLADO%' THEN m.valor ELSE 0 END) AS entradas, "
                + " SUM(CASE WHEN m.afecta_cantidad = -1 AND m.tipo NOT LIKE '%TRASLADO%' THEN m.valor ELSE 0 END) AS salidas, "
                + " SUM(CASE WHEN m.tipo LIKE 'AJUSTE%' THEN m.valor * m.afecta_cantidad ELSE 0 END) AS ajustes, "
                + " SUM(CASE WHEN m.tipo LIKE '%TRASLADO%' THEN m.valor * m.afecta_cantidad ELSE 0 END) AS traslados, "
                + " SUM(m.valor * m.afecta_cantidad) AS neto "
                + "FROM movimientos_inventario m "
                + "WHERE m.id_producto = " + idProducto + " "
                + "  AND m.fecha >= (date_trunc('month', CURRENT_DATE) - INTERVAL '" + (n - 1) + " months') "
                + filtroBodega
                + "GROUP BY 1 ORDER BY 1";

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sqlMes);
            while (rs.next()) {
                String mes = rs.getString("mes");
                double ent = rs.getDouble("entradas");
                double sal = rs.getDouble("salidas");
                for (int i = 0; i < n; i++) {
                    if (claveMes[i].equals(mes)) {
                        entradasMes[i] = ent;
                        salidasMes[i] = sal;
                    }
                }
                entradas3m     += ent;
                salidas3m      += sal;
                ajustesNetos   += rs.getDouble("ajustes");
                trasladosNetos += rs.getDouble("traslados");
                netoTotal      += rs.getDouble("neto");
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error consultando rotacion por mes: " + e.getMessage());
        }

        // ------- 2) Stock actual (segun bodega filtrada) -------
        double stockActual = 0;
        String sqlStock = "SELECT COALESCE(SUM(cantidad), 0) AS cantidad "
                + "FROM stock_productos WHERE id_producto = " + idProducto
                + ((idBodega > 0) ? (" AND id_bodega = " + idBodega) : "");
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sqlStock);
            if (rs.next()) stockActual = rs.getDouble("cantidad");
            rs.close();
        } catch (Exception e) {
            System.err.println("Error consultando stock actual: " + e.getMessage());
        }

        // ------- 3) Ultima entrada / ultima salida (historico completo) -------
        String ultEntrada = "-", ultSalida = "-";
        String sqlUltimos = "SELECT "
                + " MAX(CASE WHEN m.afecta_cantidad = 1  AND m.tipo NOT LIKE '%TRASLADO%' THEN m.fecha END) AS ult_entrada, "
                + " MAX(CASE WHEN m.afecta_cantidad = -1 AND m.tipo NOT LIKE '%TRASLADO%' THEN m.fecha END) AS ult_salida "
                + "FROM movimientos_inventario m "
                + "WHERE m.id_producto = " + idProducto + " " + filtroBodega;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sqlUltimos);
            if (rs.next()) {
                ultEntrada = formatearFechaConDias(rs.getDate("ult_entrada"));
                ultSalida  = formatearFechaConDias(rs.getDate("ult_salida"));
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error consultando ultimos movimientos: " + e.getMessage());
        }

        // ------- 4) Calculos de rotacion -------
        long diasPeriodo = Math.max(1,
                (System.currentTimeMillis() - inicio.getTimeInMillis()) / (24L * 60 * 60 * 1000) + 1);

        double promedioDiario = salidas3m / diasPeriodo;
        double promedioMensual = promedioDiario * 30.0;

        double stockInicial = stockActual - netoTotal;
        double stockPromedio = (stockInicial + stockActual) / 2.0;

        String rotacionTxt;
        if (stockPromedio > 0 && salidas3m > 0) {
            rotacionTxt = new DecimalFormat("#,##0.0#").format(salidas3m / stockPromedio) + "x";
        } else {
            rotacionTxt = "-";
        }

        String coberturaTxt;
        if (promedioDiario > 0) {
            coberturaTxt = df.format(Math.round(stockActual / promedioDiario)) + " días";
        } else {
            coberturaTxt = stockActual > 0 ? "Sin salidas" : "-";
        }

        // Compra sugerida: cuanto comprar hoy para que el stock cubra
        // DIAS_COBERTURA_OBJETIVO dias de venta al ritmo del periodo.
        // Si el stock es negativo, la compra tambien repone ese faltante.
        String compraTxt;
        if (promedioDiario > 0) {
            double faltante = (promedioDiario * DIAS_COBERTURA_OBJETIVO) - stockActual;
            compraTxt = (faltante > 0)
                    ? df.format(Math.ceil(faltante)) + " unid."
                    : "0 · ya cubierto";
        } else {
            compraTxt = "Sin salidas en el período";
        }

        // ------- 5) Pintar -------
        String suf = n + "M";
        tituloSalidasPeriodo.setText("SALIDAS " + suf);
        setTituloDetalle(detEntradas,      "Entradas " + suf);
        setTituloDetalle(detAjustes,       "Ajustes netos " + suf);
        setTituloDetalle(detTraslados,     "Traslados netos " + suf);
        setTituloDetalle(detStockPromedio, "Stock promedio " + suf);

        valSalidas3m.setText(df.format(salidas3m));
        valPromMensual.setText(df.format(promedioMensual));
        valRotacion.setText(rotacionTxt);
        valCobertura.setText(coberturaTxt);
        valCompraSugerida.setText(compraTxt);

        detEntradas.setText(df.format(entradas3m));
        detAjustes.setText((ajustesNetos >= 0 ? "+" : "") + df.format(ajustesNetos));
        detTraslados.setText((trasladosNetos >= 0 ? "+" : "") + df.format(trasladosNetos));
        detStockActual.setText(df.format(stockActual));
        detStockPromedio.setText(df.format(stockPromedio));
        detUltimaEntrada.setText(ultEntrada);
        detUltimaSalida.setText(ultSalida);

        updateChip(chipStockActual, "Stock actual", df.format(stockActual));

        chart.setDatos(labelMes, entradasMes, salidasMes);

        // Repintado completo de la card: al cambiar textos/periodo los labels
        // se reacomodan y quedan restos pintados si solo se repinta cada uno.
        if (statsCard != null) {
            statsCard.revalidate();
            statsCard.repaint();
        }
    }

    /** "dd/MM/yyyy (hace N dias)" o "-" si es null. */
    private String formatearFechaConDias(java.sql.Date fecha) {
        if (fecha == null) return "-";
        long dias = (System.currentTimeMillis() - fecha.getTime()) / (24L * 60 * 60 * 1000);
        if (dias < 0) dias = 0;
        String cuando = (dias == 0) ? "hoy" : (dias == 1) ? "ayer" : "hace " + dias + " días";
        return new SimpleDateFormat("dd/MM/yyyy").format(fecha) + " (" + cuando + ")";
    }

    // ================================================================
    // ============== CLASES INTERNAS: widgets ========================
    // ================================================================

    /** Grafico de barras agrupadas: entradas vs salidas por mes. */
    private class ChartBarras extends JPanel {
        private String[] labels = new String[0];
        private double[] entradas = new double[0];
        private double[] salidas = new double[0];

        public ChartBarras() {
            setOpaque(false);
        }

        public void setDatos(String[] labels, double[] entradas, double[] salidas) {
            this.labels = labels;
            this.entradas = entradas;
            this.salidas = salidas;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int baseY = h - 34;      // linea base (deja sitio a labels de mes)
            int topY = 22;           // margen superior (deja sitio a valores)

            double max = 0;
            for (int i = 0; i < labels.length; i++) {
                max = Math.max(max, Math.max(entradas[i], salidas[i]));
            }

            // Linea base
            g2.setColor(DIVIDER);
            g2.drawLine(6, baseY, w - 6, baseY);

            if (labels.length == 0 || max <= 0) {
                g2.setColor(TEXT_SECOND);
                g2.setFont(new Font(FONT_FAMILY, Font.ITALIC, 12));
                String msg = "Sin movimientos en el período";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, (baseY + topY) / 2);
            } else {
                int nGrupos = labels.length;
                int anchoGrupo = (w - 20) / nGrupos;
                int anchoBarra = Math.max(8, Math.min(34, (anchoGrupo - (nGrupos > 3 ? 12 : 24)) / 2));
                Font fVal = new Font(FONT_FAMILY, Font.BOLD, nGrupos > 3 ? 9 : 10);
                Font fMes = new Font(FONT_FAMILY, Font.PLAIN, 11);

                for (int i = 0; i < nGrupos; i++) {
                    int cx = 10 + i * anchoGrupo + anchoGrupo / 2;

                    int hEnt = (int) Math.round((baseY - topY) * (entradas[i] / max));
                    int hSal = (int) Math.round((baseY - topY) * (salidas[i] / max));

                    int xEnt = cx - anchoBarra - 3;
                    int xSal = cx + 3;

                    // Barra entradas (verde)
                    g2.setColor(new Color(SUCCESS.getRed(), SUCCESS.getGreen(), SUCCESS.getBlue(), 210));
                    g2.fill(new RoundRectangle2D.Double(xEnt, baseY - hEnt, anchoBarra, hEnt, 6, 6));
                    // Barra salidas (rojo)
                    g2.setColor(new Color(DANGER.getRed(), DANGER.getGreen(), DANGER.getBlue(), 210));
                    g2.fill(new RoundRectangle2D.Double(xSal, baseY - hSal, anchoBarra, hSal, 6, 6));

                    // Valores encima
                    g2.setFont(fVal);
                    FontMetrics fmv = g2.getFontMetrics();
                    g2.setColor(SUCCESS.darker());
                    String vEnt = df.format(entradas[i]);
                    g2.drawString(vEnt, xEnt + (anchoBarra - fmv.stringWidth(vEnt)) / 2, baseY - hEnt - 4);
                    g2.setColor(DANGER.darker());
                    String vSal = df.format(salidas[i]);
                    g2.drawString(vSal, xSal + (anchoBarra - fmv.stringWidth(vSal)) / 2, baseY - hSal - 4);

                    // Mes debajo
                    g2.setFont(fMes);
                    g2.setColor(TEXT_SECOND);
                    FontMetrics fmm = g2.getFontMetrics();
                    g2.drawString(labels[i], cx - fmm.stringWidth(labels[i]) / 2, baseY + 16);
                }
            }

            // Leyenda
            g2.setFont(new Font(FONT_FAMILY, Font.PLAIN, 10));
            int ly = h - 6;
            g2.setColor(SUCCESS);
            g2.fillRect(8, ly - 8, 8, 8);
            g2.setColor(TEXT_SECOND);
            g2.drawString("Entradas", 20, ly);
            g2.setColor(DANGER);
            g2.fillRect(80, ly - 8, 8, 8);
            g2.setColor(TEXT_SECOND);
            g2.drawString("Salidas", 92, ly);

            g2.dispose();
        }
    }

    /** Panel con fondo redondeado y sombra sutil. */
    private static class CardPanel extends JPanel {
        public CardPanel() {
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 14;
            for (int i = 0; i < 6; i++) {
                g2.setColor(new Color(0, 0, 0, 5 + i));
                g2.fill(new RoundRectangle2D.Double(i, i + 1, getWidth() - (i * 2), getHeight() - (i * 2), arc, arc));
            }
            g2.setColor(CARD_BG);
            g2.fill(new RoundRectangle2D.Double(2, 2, getWidth() - 4, getHeight() - 6, arc, arc));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Borde vacio que reserva espacio para la sombra de CardPanel. */
    private static class CompoundBorderShadow extends AbstractBorder {
        private final Border inner;
        public CompoundBorderShadow(Border inner) { this.inner = inner; }
        @Override
        public Insets getBorderInsets(Component c) {
            Insets i = (Insets) inner.getBorderInsets(c).clone();
            i.top += 6; i.left += 6; i.right += 6; i.bottom += 10;
            return i;
        }
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            Insets i = getBorderInsets(c);
            insets.set(i.top, i.left, i.bottom, i.right);
            return insets;
        }
    }

    /** Borde redondeado de 1px. */
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
        @Override
        public Insets getBorderInsets(Component c) { return new Insets(8, 12, 8, 12); }
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(8, 12, 8, 12);
            return insets;
        }
    }

    /** Header renderer igual al de frm_consulta. */
    private static class MaterialHeaderRenderer extends DefaultTableCellRenderer {
        public MaterialHeaderRenderer() {
            setHorizontalAlignment(SwingConstants.LEFT);
            setOpaque(true);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setBackground(new Color(245, 246, 250));
            l.setForeground(TEXT_SECOND);
            l.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
            l.setBorder(new EmptyBorder(10, 14, 10, 10));
            if (column >= 4 && column <= 6) {
                l.setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                l.setHorizontalAlignment(SwingConstants.LEFT);
            }
            return l;
        }
    }

    /** Body renderer: zebra + colores por entrada/salida/reserva. */
    private class KardexCellRenderer extends DefaultTableCellRenderer {
        public KardexCellRenderer() {
            setOpaque(true);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
            l.setBorder(new EmptyBorder(0, 14, 0, 10));

            if (isSelected) {
                l.setBackground(SELECTION);
            } else {
                l.setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
            }
            l.setForeground(TEXT_PRIMARY);

            int afecta = (row >= 0 && row < afectasFilas.size()) ? afectasFilas.get(row) : 0;

            if (column >= 4 && column <= 6) {
                l.setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                l.setHorizontalAlignment(SwingConstants.LEFT);
            }

            if (column == 2) {
                // Tipo coloreado segun su efecto en el stock fisico
                l.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
                if (afecta == 1)       l.setForeground(SUCCESS);
                else if (afecta == -1) l.setForeground(DANGER);
                else                   l.setForeground(TEXT_SECOND);
            } else if (column == 4 && value != null && !value.toString().isEmpty()) {
                l.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
                l.setForeground(SUCCESS);
            } else if (column == 5 && value != null && !value.toString().isEmpty()) {
                l.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
                l.setForeground(DANGER);
            } else if (column == 6) {
                l.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
            } else if (column == 8) {
                l.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
                l.setForeground(TEXT_SECOND);
            }

            if (isSelected && column != 2 && column != 4 && column != 5) {
                l.setForeground(TEXT_PRIMARY);
            }
            return l;
        }
    }
}
