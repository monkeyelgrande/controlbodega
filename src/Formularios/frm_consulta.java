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
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Consulta de stock de productos leyendo de la tabla stock_productos.
 *
 * Dise\u00f1o inspirado en Material Design:
 *   - Paleta de color Indigo / Amber
 *   - Tarjetas con sombra, bordes redondeados
 *   - Tipograf\u00eda Roboto / Segoe UI
 *   - Botones "contained" con hover
 *   - Chips de totales en el footer
 *
 * Permite:
 *   - Buscar por c\u00f3digo de barras o descripci\u00f3n
 *   - Filtrar por bodega (incluye "Todas las bodegas")
 *   - Ver la cantidad disponible por bodega (incluso existencia 0)
 *
 * @author M-Work
 */
public class frm_consulta extends javax.swing.JDialog {

    // ================================================================
    // Paleta Material
    // ================================================================
    private static final Color PRIMARY       = new Color(40, 53, 147);    // Indigo 800
    private static final Color PRIMARY_DARK  = new Color(26, 35, 126);    // Indigo 900
    private static final Color PRIMARY_LIGHT = new Color(92, 107, 192);   // Indigo 400
    private static final Color ACCENT        = new Color(255, 193, 7);    // Amber 500
    private static final Color ACCENT_DARK   = new Color(255, 160, 0);    // Amber 700
    private static final Color SURFACE       = new Color(250, 250, 252);  // Gris casi blanco
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color TEXT_PRIMARY  = new Color(33, 33, 33);
    private static final Color TEXT_SECOND   = new Color(117, 117, 117);
    private static final Color DIVIDER       = new Color(224, 224, 224);
    private static final Color ROW_ALT       = new Color(245, 247, 251);
    private static final Color SELECTION     = new Color(197, 202, 233);  // Indigo 100
    private static final Color SUCCESS       = new Color(67, 160, 71);    // Green 600
    private static final Color DANGER        = new Color(229, 57, 53);    // Red 600

    private static final String FONT_FAMILY = "Segoe UI";

    // ================================================================
    // Componentes
    // ================================================================
    private JTextField txtBuscar;
    private JComboBox<Bodegas> cmbBodega;
    private MaterialButton btnBuscar;
    private MaterialButton btnLimpiar;

    private JLabel lblRegistros;
    private JLabel lblTotalDisponible;
    private JLabel lblTotalCantidad;
    private JLabel lblTotalPendientes;

    private JTable tabla;
    private DefaultTableModel modelo;

    private final DecimalFormat df = new DecimalFormat("###,###.##");

    // ================================================================
    // Constructor
    // ================================================================
    public frm_consulta(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponentes();
        cargarBodegas();
        consultarStock();
        // Filtro en vivo a medida que se escribe (sin necesidad de Enter)
        try { metodos.BuscarEnTabla(txtBuscar, tabla); } catch (Exception ignore) {}
        setLocationRelativeTo(parent);
        try { metodos.addEscapeListenerWindowDialog(this); } catch (Exception ignore) {}
    }

    // ================================================================
    // UI
    // ================================================================
    private void initComponentes() {
        setTitle("Consulta de Stock por Bodega");
        setSize(1350, 1050);
        setMinimumSize(new Dimension(900, 560));
        setLayout(new BorderLayout());
        getContentPane().setBackground(SURFACE);

        // ---------- AppBar ------------------------------------------
        add(buildAppBar(), BorderLayout.NORTH);

        // ---------- Contenido ---------------------------------------
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(SURFACE);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        content.add(buildFiltersCard(), BorderLayout.NORTH);
        content.add(buildTableCard(),   BorderLayout.CENTER);
        content.add(buildFooterCard(),  BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    // ----------------------------------------------------------------
    // App bar superior
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

        JLabel lblTitulo = new JLabel("Consulta de Stock");
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setFont(new Font(FONT_FAMILY, Font.BOLD, 22));

        JLabel lblSubtitulo = new JLabel("Existencias por bodega  \u00b7  tabla stock_productos");
        lblSubtitulo.setForeground(new Color(255, 255, 255, 200));
        lblSubtitulo.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.add(lblTitulo);
        titleWrap.add(Box.createVerticalStrut(2));
        titleWrap.add(lblSubtitulo);

        appBar.add(titleWrap, BorderLayout.WEST);

        // Botones de exportacion
        JPanel panelExport = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelExport.setOpaque(false);

        MaterialButton btnExcel = new MaterialButton("EXCEL", new Color(33, 133, 89), Color.WHITE);
        MaterialButton btnPdf   = new MaterialButton("PDF",   new Color(183, 28, 28), Color.WHITE);

        btnExcel.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { exportarExcel(); }
        });
        btnPdf.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { exportarPdf(); }
        });

        panelExport.add(btnExcel);
        panelExport.add(btnPdf);
        appBar.add(panelExport, BorderLayout.EAST);

        return appBar;
    }

    // ----------------------------------------------------------------
    // Card de filtros
    // ----------------------------------------------------------------
    private JPanel buildFiltersCard() {
        JPanel card = new CardPanel();
        card.setLayout(new GridBagLayout());
        card.setBorder(new CompoundBorderShadow(new EmptyBorder(16, 20, 16, 20)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;

        // Label Producto
        c.gridx = 0; c.weightx = 0;
        card.add(buildFieldLabel("PRODUCTO"), c);

        // Label Bodega
        c.gridx = 1; c.weightx = 0;
        card.add(buildFieldLabel("BODEGA"), c);

        // Fila 1: inputs
        c.gridy = 1;

        txtBuscar = new MaterialTextField("C\u00f3digo o descripci\u00f3n");
        txtBuscar.setPreferredSize(new Dimension(360, 40));
        c.gridx = 0; c.weightx = 1;
        card.add(txtBuscar, c);

        cmbBodega = new JComboBox<Bodegas>();
        styleComboBox(cmbBodega);
        cmbBodega.setPreferredSize(new Dimension(260, 40));
        c.gridx = 1; c.weightx = 0.6;
        card.add(cmbBodega, c);

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelBotones.setOpaque(false);

        btnBuscar  = new MaterialButton("BUSCAR",  PRIMARY, Color.WHITE);
        btnLimpiar = new MaterialButton("LIMPIAR", Color.WHITE, PRIMARY);
        btnLimpiar.setOutlined(true);

        panelBotones.add(btnLimpiar);
        panelBotones.add(btnBuscar);

        c.gridx = 2; c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        card.add(panelBotones, c);

        // --- Listeners ---
        ActionListener buscarListener = new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { consultarStock(); }
        };
        btnBuscar.addActionListener(buscarListener);
        cmbBodega.addActionListener(buscarListener);
        btnLimpiar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                txtBuscar.setText("");
                if (cmbBodega.getItemCount() > 0) cmbBodega.setSelectedIndex(0);
                consultarStock();
            }
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

        modelo = new DefaultTableModel(
                new Object[]{"C\u00d3DIGO", "DESCRIPCI\u00d3N", "BODEGA", "CANTIDAD", "PENDIENTES", "DISPONIBLE"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        tabla = new JTable(modelo);
        tabla.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        tabla.setForeground(TEXT_PRIMARY);
        tabla.setRowHeight(38);
        tabla.setShowGrid(false);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionBackground(SELECTION);
        tabla.setSelectionForeground(TEXT_PRIMARY);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Header
        JTableHeader header = tabla.getTableHeader();
        header.setDefaultRenderer(new MaterialHeaderRenderer());
        header.setPreferredSize(new Dimension(0, 44));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER));

        // Body
        MaterialCellRenderer bodyRenderer = new MaterialCellRenderer();
        for (int i = 0; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(bodyRenderer);
        }

        // Anchos
        TableColumnModel cm = tabla.getColumnModel();
        cm.getColumn(0).setPreferredWidth(130);
        cm.getColumn(1).setPreferredWidth(340);
        cm.getColumn(2).setPreferredWidth(170);
        cm.getColumn(3).setPreferredWidth(100);
        cm.getColumn(4).setPreferredWidth(100);
        cm.getColumn(5).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);
        scroll.setBackground(CARD_BG);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ----------------------------------------------------------------
    // Card footer con chips de totales
    // ----------------------------------------------------------------
    private JPanel buildFooterCard() {
        JPanel card = new CardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new CompoundBorderShadow(new EmptyBorder(14, 20, 14, 20)));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chips.setOpaque(false);

        lblRegistros       = buildChip("Registros", "0",    PRIMARY);
        lblTotalCantidad   = buildChip("Cantidad",  "0",    PRIMARY_LIGHT);
        lblTotalPendientes = buildChip("Pendientes","0",    DANGER);
        lblTotalDisponible = buildChip("Disponible","0",    SUCCESS);

        chips.add(lblRegistros);
        chips.add(lblTotalCantidad);
        chips.add(lblTotalPendientes);
        chips.add(lblTotalDisponible);

        card.add(chips, BorderLayout.WEST);
        return card;
    }

    private JLabel buildChip(String titulo, String valor, Color color) {
        JLabel chip = new JLabel("<html><span style='color:#757575;font-size:10px'>"
                + titulo.toUpperCase() + "</span>&nbsp;&nbsp;<b style='font-size:13px;color:rgb("
                + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")'>"
                + valor + "</b></html>");
        chip.setOpaque(true);
        chip.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
        chip.setBorder(new EmptyBorder(8, 14, 8, 14));
        chip.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        chip.putClientProperty("chipColor", color);
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
    // Combo box styling
    // ----------------------------------------------------------------
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

    // ================================================================
    // Consulta principal
    // ================================================================
    private void consultarStock() {
        modelo.setRowCount(0);

        Bodegas bodegaSel = (Bodegas) cmbBodega.getSelectedItem();
        int idBodega = (bodegaSel != null) ? bodegaSel.getId() : 0;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.codigo_barras, p.descripcion, ")
           .append("       b.id AS id_bodega, b.nombre AS bodega, ")
           .append("       sp.cantidad   AS cantidad, ")
           .append("       sp.pendientes AS pendientes ")
           .append("FROM productos p ")
           .append("INNER JOIN stock_productos sp ON sp.id_producto = p.id ")
           .append("INNER JOIN bodegas b ON b.id = sp.id_bodega ")
           .append("WHERE 1 = 1 ");

        sql.append("  AND COALESCE(p.estado, true) = true ");

        if (idBodega > 0) {
            sql.append("  AND b.id = ").append(idBodega).append(" ");
        }

        sql.append("ORDER BY p.descripcion, b.nombre ");
        sql.append("LIMIT 5000");

        double totalCantidad = 0;
        double totalPendientes = 0;
        double totalDisponible = 0;
        int filas = 0;

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql.toString());
            while (rs.next()) {
                String codigo = rs.getString("codigo_barras");
                String descripcion = rs.getString("descripcion");
                String nombreBodega = rs.getString("bodega");
                double cantidad = rs.getDouble("cantidad");
                double pendientes = rs.getDouble("pendientes");
                double disponible = cantidad - pendientes;

                modelo.addRow(new Object[]{
                        codigo != null ? codigo : "",
                        descripcion != null ? descripcion : "",
                        nombreBodega != null ? nombreBodega : "",
                        df.format(cantidad),
                        df.format(pendientes),
                        df.format(disponible)
                });

                totalCantidad   += cantidad;
                totalPendientes += pendientes;
                totalDisponible += disponible;
                filas++;
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error consultando stock: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Error consultando stock:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        updateChip(lblRegistros,       "Registros",  String.valueOf(filas));
        updateChip(lblTotalCantidad,   "Cantidad",   df.format(totalCantidad));
        updateChip(lblTotalPendientes, "Pendientes", df.format(totalPendientes));
        updateChip(lblTotalDisponible, "Disponible", df.format(totalDisponible));
    }

    // ================================================================
    // Exportar a Excel
    // ================================================================
    private void exportarExcel() {
        if (tabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar.", "Sin datos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar como Excel");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivo Excel (*.xls)", "xls"));
        chooser.setSelectedFile(new File("Stock_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xls"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String ruta = chooser.getSelectedFile().toString();
        if (!ruta.toLowerCase().endsWith(".xls")) ruta += ".xls";

        try {
            HSSFWorkbook libro = new HSSFWorkbook();
            Sheet hoja = libro.createSheet("Stock por Bodega");

            // Estilo header
            HSSFFont fontHeader = libro.createFont();
            fontHeader.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
            fontHeader.setColor(HSSFColor.WHITE.index);
            fontHeader.setFontName("Arial");
            fontHeader.setFontHeightInPoints((short) 11);

            HSSFCellStyle estiloHeader = libro.createCellStyle();
            estiloHeader.setFont(fontHeader);
            estiloHeader.setFillForegroundColor(HSSFColor.DARK_BLUE.index);
            estiloHeader.setFillPattern(CellStyle.SOLID_FOREGROUND);
            estiloHeader.setAlignment(CellStyle.ALIGN_CENTER);

            // Estilo datos
            HSSFFont fontDatos = libro.createFont();
            fontDatos.setFontName("Arial");
            fontDatos.setFontHeightInPoints((short) 10);

            HSSFCellStyle estiloDatos = libro.createCellStyle();
            estiloDatos.setFont(fontDatos);

            HSSFCellStyle estiloNumero = libro.createCellStyle();
            estiloNumero.setFont(fontDatos);
            estiloNumero.setAlignment(CellStyle.ALIGN_RIGHT);

            // Titulo
            Row filaTitulo = hoja.createRow(0);
            Cell celdaTitulo = filaTitulo.createCell(0);
            HSSFFont fontTitulo = libro.createFont();
            fontTitulo.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
            fontTitulo.setFontHeightInPoints((short) 14);
            fontTitulo.setFontName("Arial");
            HSSFCellStyle estiloTitulo = libro.createCellStyle();
            estiloTitulo.setFont(fontTitulo);
            celdaTitulo.setCellStyle(estiloTitulo);

            String bodegaFiltro = cmbBodega.getSelectedItem() != null ? cmbBodega.getSelectedItem().toString() : "Todas";
            celdaTitulo.setCellValue("Consulta de Stock - " + bodegaFiltro);

            Row filaFecha = hoja.createRow(1);
            filaFecha.createCell(0).setCellValue("Fecha: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));

            // Headers de columnas
            Row filaHeader = hoja.createRow(3);
            String[] headers = {"C\u00f3digo", "Descripci\u00f3n", "Bodega", "Cantidad", "Pendientes", "Disponible"};
            for (int c = 0; c < headers.length; c++) {
                Cell celda = filaHeader.createCell(c);
                celda.setCellValue(headers[c]);
                celda.setCellStyle(estiloHeader);
            }

            // Datos (lo visible en tabla con filtros aplicados)
            for (int f = 0; f < tabla.getRowCount(); f++) {
                Row fila = hoja.createRow(f + 4);
                for (int c = 0; c < tabla.getColumnCount(); c++) {
                    Cell celda = fila.createCell(c);
                    Object val = tabla.getValueAt(f, c);
                    String sval = val != null ? val.toString() : "";

                    if (c >= 3) {
                        // Columnas numericas
                        try {
                            celda.setCellValue(Double.parseDouble(sval.replace(",", "")));
                        } catch (Exception ex) {
                            celda.setCellValue(sval);
                        }
                        celda.setCellStyle(estiloNumero);
                    } else {
                        celda.setCellValue(sval);
                        celda.setCellStyle(estiloDatos);
                    }
                }
            }

            // Fila totales
            int filaTotalIdx = tabla.getRowCount() + 5;
            Row filaTotales = hoja.createRow(filaTotalIdx);
            Cell celdaLabel = filaTotales.createCell(2);
            celdaLabel.setCellValue("TOTALES:");
            celdaLabel.setCellStyle(estiloHeader);
            for (int c = 3; c <= 5; c++) {
                Cell celda = filaTotales.createCell(c);
                // SUM formula desde fila 5 hasta la ultima de datos
                String colLetter = (c == 3) ? "D" : (c == 4) ? "E" : "F";
                celda.setCellFormula("SUM(" + colLetter + "5:" + colLetter + (tabla.getRowCount() + 4) + ")");
                celda.setCellStyle(estiloHeader);
            }

            // Auto-size columnas
            for (int c = 0; c < 6; c++) hoja.autoSizeColumn(c);

            FileOutputStream archivo = new FileOutputStream(ruta);
            libro.write(archivo);
            archivo.close();

            Desktop.getDesktop().open(new File(ruta));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error exportando a Excel:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    // Exportar a PDF
    // ================================================================
    private void exportarPdf() {
        if (tabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar.", "Sin datos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar como PDF");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivo PDF (*.pdf)", "pdf"));
        chooser.setSelectedFile(new File("Stock_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String ruta = chooser.getSelectedFile().toString();
        if (!ruta.toLowerCase().endsWith(".pdf")) ruta += ".pdf";

        try {
            Document doc = new Document(PageSize.LETTER.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();

            // Colores
            java.awt.Color headerBg = new java.awt.Color(40, 53, 147);
            java.awt.Color altRowBg = new java.awt.Color(240, 242, 248);

            // Fuentes
            com.lowagie.text.Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, java.awt.Color.DARK_GRAY);
            com.lowagie.text.Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 10, java.awt.Color.GRAY);
            com.lowagie.text.Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.WHITE);
            com.lowagie.text.Font fontDatos = FontFactory.getFont(FontFactory.HELVETICA, 9, java.awt.Color.DARK_GRAY);
            com.lowagie.text.Font fontTotales = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.WHITE);

            // Titulo
            String bodegaFiltro = cmbBodega.getSelectedItem() != null ? cmbBodega.getSelectedItem().toString() : "Todas";
            Paragraph titulo = new Paragraph("Consulta de Stock - " + bodegaFiltro, fontTitulo);
            titulo.setAlignment(Element.ALIGN_LEFT);
            doc.add(titulo);

            Paragraph fecha = new Paragraph("Generado: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())
                    + "  |  Registros: " + tabla.getRowCount(), fontSubtitulo);
            fecha.setSpacingAfter(12);
            doc.add(fecha);

            // Tabla PDF
            float[] anchos = {12f, 35f, 17f, 12f, 12f, 12f};
            PdfPTable pdfTabla = new PdfPTable(anchos);
            pdfTabla.setWidthPercentage(100);
            pdfTabla.setSpacingBefore(5);

            // Header
            String[] headers = {"C\u00f3digo", "Descripci\u00f3n", "Bodega", "Cantidad", "Pendientes", "Disponible"};
            for (int c = 0; c < headers.length; c++) {
                PdfPCell celda = new PdfPCell(new Phrase(headers[c], fontHeader));
                celda.setBackgroundColor(headerBg);
                celda.setPadding(7);
                celda.setHorizontalAlignment(c >= 3 ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                celda.setBorderWidth(0);
                pdfTabla.addCell(celda);
            }

            // Datos
            for (int f = 0; f < tabla.getRowCount(); f++) {
                java.awt.Color rowBg = (f % 2 == 0) ? java.awt.Color.WHITE : altRowBg;

                for (int c = 0; c < tabla.getColumnCount(); c++) {
                    Object val = tabla.getValueAt(f, c);
                    String sval = val != null ? val.toString() : "";

                    PdfPCell celda = new PdfPCell(new Phrase(sval, fontDatos));
                    celda.setBackgroundColor(rowBg);
                    celda.setPadding(5);
                    celda.setHorizontalAlignment(c >= 3 ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                    celda.setBorderWidth(0);
                    celda.setBorderWidthBottom(0.3f);
                    celda.setBorderColor(new java.awt.Color(220, 220, 220));
                    pdfTabla.addCell(celda);
                }
            }

            // Fila totales
            PdfPCell celdaVacia1 = new PdfPCell(new Phrase("", fontTotales));
            celdaVacia1.setBackgroundColor(headerBg);
            celdaVacia1.setBorderWidth(0);
            pdfTabla.addCell(celdaVacia1);

            PdfPCell celdaVacia2 = new PdfPCell(new Phrase("", fontTotales));
            celdaVacia2.setBackgroundColor(headerBg);
            celdaVacia2.setBorderWidth(0);
            pdfTabla.addCell(celdaVacia2);

            PdfPCell celdaTotalLabel = new PdfPCell(new Phrase("TOTALES:", fontTotales));
            celdaTotalLabel.setBackgroundColor(headerBg);
            celdaTotalLabel.setPadding(6);
            celdaTotalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            celdaTotalLabel.setBorderWidth(0);
            pdfTabla.addCell(celdaTotalLabel);

            // Calcular totales sumando las columnas visibles
            double[] totales = new double[3];
            for (int f = 0; f < tabla.getRowCount(); f++) {
                for (int c = 3; c <= 5; c++) {
                    try {
                        String sval = tabla.getValueAt(f, c).toString().replace(",", "");
                        totales[c - 3] += Double.parseDouble(sval);
                    } catch (Exception ignore) {}
                }
            }
            for (int t = 0; t < 3; t++) {
                PdfPCell celda = new PdfPCell(new Phrase(df.format(totales[t]), fontTotales));
                celda.setBackgroundColor(headerBg);
                celda.setPadding(6);
                celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
                celda.setBorderWidth(0);
                pdfTabla.addCell(celda);
            }

            doc.add(pdfTabla);
            doc.close();

            Desktop.getDesktop().open(new File(ruta));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error exportando a PDF:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    // ============== CLASES INTERNAS: Material widgets ===============
    // ================================================================

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
            // Sombra
            for (int i = 0; i < 6; i++) {
                g2.setColor(new Color(0, 0, 0, 5 + i));
                g2.fill(new RoundRectangle2D.Double(i, i + 1, getWidth() - (i * 2), getHeight() - (i * 2), arc, arc));
            }
            // Fondo
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

    /** Borde redondeado de 1px con color configurable. */
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

    /** TextField con placeholder y borde redondeado. */
    private static class MaterialTextField extends JTextField {
        private final String placeholder;
        public MaterialTextField(String placeholder) {
            this.placeholder = placeholder;
            setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
            setForeground(TEXT_PRIMARY);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(8, DIVIDER),
                    new EmptyBorder(0, 4, 0, 4)));
            setOpaque(true);
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    setBorder(BorderFactory.createCompoundBorder(
                            new RoundedBorder(8, PRIMARY),
                            new EmptyBorder(0, 4, 0, 4)));
                    repaint();
                }
                @Override public void focusLost(FocusEvent e) {
                    setBorder(BorderFactory.createCompoundBorder(
                            new RoundedBorder(8, DIVIDER),
                            new EmptyBorder(0, 4, 0, 4)));
                    repaint();
                }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT_SECOND);
                g2.setFont(getFont().deriveFont(Font.ITALIC));
                Insets ins = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, ins.left + 2, y);
                g2.dispose();
            }
        }
    }

    /** Boton Material "contained" / "outlined" con hover, ripple simple y bordes redondeados. */
    private static class MaterialButton extends JButton {
        private Color bg;
        private Color fg;
        private boolean hover = false;
        private boolean pressed = false;
        private boolean outlined = false;

        public MaterialButton(String text, Color bg, Color fg) {
            super(text);
            this.bg = bg;
            this.fg = fg;
            setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
            setForeground(fg);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setBorder(new EmptyBorder(10, 22, 10, 22));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }

        public void setOutlined(boolean outlined) {
            this.outlined = outlined;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 10;

            if (outlined) {
                Color fill = hover ? new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), 20) : Color.WHITE;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(PRIMARY);
                g2.setStroke(new BasicStroke(1.3f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            } else {
                // Sombra
                if (!pressed) {
                    g2.setColor(new Color(0, 0, 0, 25));
                    g2.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 2, arc, arc);
                }
                Color fill = bg;
                if (pressed) fill = bg.darker();
                else if (hover) fill = brighten(bg, 0.08f);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() - (pressed ? 1 : 2), arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }

        private Color brighten(Color c, float f) {
            int r = Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * f));
            int gg = Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * f));
            int b = Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * f));
            return new Color(r, gg, b);
        }
    }

    /** Header renderer con fondo claro, tipografia bold, uppercase, separadores sutiles. */
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
            if (column == 3 || column == 4 || column == 5) {
                l.setHorizontalAlignment(SwingConstants.RIGHT);
            }
            return l;
        }
    }

    /** Body renderer con zebra, padding y alineacion numerica a la derecha. */
    private static class MaterialCellRenderer extends DefaultTableCellRenderer {
        public MaterialCellRenderer() {
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
                l.setForeground(TEXT_PRIMARY);
            } else {
                l.setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
                l.setForeground(TEXT_PRIMARY);
            }

            // Numericas a la derecha
            if (column == 3 || column == 4 || column == 5) {
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                // resaltar disponible
                if (column == 5 && value != null) {
                    String s = value.toString().replace(",", "").replace(".", "");
                    try {
                        double v = Double.parseDouble(value.toString().replace(",", ""));
                        if (!isSelected) {
                            if (v <= 0)      l.setForeground(DANGER);
                            else if (v < 10) l.setForeground(ACCENT_DARK);
                            else             l.setForeground(SUCCESS);
                        }
                        l.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
                    } catch (Exception ignore) {}
                }
            } else {
                l.setHorizontalAlignment(SwingConstants.LEFT);
            }
            return l;
        }
    }
}
