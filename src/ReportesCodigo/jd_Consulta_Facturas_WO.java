package ReportesCodigo;

import Metodos.EstiloCompras;
import Metodos.ExportarExcel;
import Metodos.FontAwesome;
import conexiondb.DB_consultas_R_D;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Consulta de facturas capturadas automaticamente desde World Office
 * (tablas facturas_impresas + detalle_factura, alimentadas por wo-printer).
 *
 * Dos modos de consulta:
 *  - Por factura: rango de fechas, cliente, vendedor, forma de pago,
 *    empresa y numero de factura. Maestro-detalle con totales.
 *  - Por producto: busqueda en vivo por codigo o nombre, con historial
 *    completo de apariciones (precio, cantidad, novedades) y estadisticas.
 *
 * @author Monkeyelgrande
 */
public class jd_Consulta_Facturas_WO extends javax.swing.JDialog {

    private static final int LIMITE_FACTURAS = 500;
    private static final int LIMITE_LINEAS = 1000;

    private static final DecimalFormat FMT_MONEDA = new DecimalFormat("$ #,##0.##");
    private static final DecimalFormat FMT_CANTIDAD = new DecimalFormat("#,##0.##");

    private JTabbedPane tabs;

    // --- Tab por factura ---
    private com.toedter.calendar.JDateChooser dateDesde;
    private com.toedter.calendar.JDateChooser dateHasta;
    private JTextField txtNumeroFactura;
    private JComboBox<String> cmbCliente;
    private JComboBox<String> cmbVendedor;
    private JComboBox<String> cmbFormaPago;
    private JComboBox<String> cmbEmpresa;
    private JTable tablaFacturas;
    private JTable tablaDetalle;
    private DefaultTableModel modeloFacturas;
    private DefaultTableModel modeloDetalle;
    private JLabel lblResumenFacturas;

    // --- Tab por producto ---
    private JTextField txtProducto;
    private com.toedter.calendar.JDateChooser dateDesdeProd;
    private com.toedter.calendar.JDateChooser dateHastaProd;
    private JTable tablaProductos;
    private DefaultTableModel modeloProductos;
    private JLabel lblResumenProductos;
    private JLabel valRegistros, valFacturas, valCantidad, valTotal;
    private JLabel valPrecioUltimo, valPrecioMin, valPrecioMax, valPrecioProm;
    private Timer debounceProducto;

    public jd_Consulta_Facturas_WO(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponentes();
        cargarCombos();
        setLocationRelativeTo(parent);
    }

    private void initComponentes() {
        setTitle("Consulta Facturas World Office");
        setSize(1150, 720);
        setMinimumSize(new Dimension(950, 560));
        getContentPane().setBackground(EstiloCompras.BG_FORM);
        setLayout(new BorderLayout());

        add(EstiloCompras.header(FontAwesome.FILE_INVOICE,
                "Consulta Facturas World Office", new Runnable() {
            @Override
            public void run() { dispose(); }
        }), BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(EstiloCompras.BG_FORM);
        tabs.addTab("  Por factura  ", construirTabFacturas());
        tabs.addTab("  Por producto  ", construirTabProductos());
        add(tabs, BorderLayout.CENTER);
    }

    // ================================================================
    // TAB 1: consulta por factura
    // ================================================================

    private JPanel construirTabFacturas() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(EstiloCompras.BG_FORM);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // ---- Filtros ----
        dateDesde = crearDateChooser();
        dateHasta = crearDateChooser();
        // por defecto: ultimos 30 dias
        Calendar cal = Calendar.getInstance();
        dateHasta.setDate(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, -30);
        dateDesde.setDate(cal.getTime());

        txtNumeroFactura = EstiloCompras.field("Ej: FVE-83892", FontAwesome.BARCODE);
        cmbCliente = new JComboBox<String>();
        cmbCliente.setEditable(true);
        EstiloCompras.styleCombo(cmbCliente);
        cmbVendedor = new JComboBox<String>();
        EstiloCompras.styleCombo(cmbVendedor);
        cmbFormaPago = new JComboBox<String>();
        EstiloCompras.styleCombo(cmbFormaPago);
        cmbEmpresa = new JComboBox<String>();
        EstiloCompras.styleCombo(cmbEmpresa);

        JButton btnBuscar = EstiloCompras.primaryBtn("Buscar", FontAwesome.SEARCH);
        btnBuscar.addActionListener(e -> buscarFacturas());
        JButton btnLimpiar = EstiloCompras.secondaryBtn("Limpiar", FontAwesome.SYNC);
        btnLimpiar.addActionListener(e -> limpiarFiltrosFacturas());
        JButton btnExportar = EstiloCompras.successBtn("Exportar", FontAwesome.SAVE);
        btnExportar.addActionListener(e -> exportar(tablaFacturas));

        JPanel fila1 = filaFiltros();
        fila1.add(EstiloCompras.labeled("Desde", dateDesde, 150));
        fila1.add(Box.createHorizontalStrut(8));
        fila1.add(EstiloCompras.labeled("Hasta", dateHasta, 150));
        fila1.add(Box.createHorizontalStrut(8));
        fila1.add(EstiloCompras.labeled("Número de factura", txtNumeroFactura, 180));
        fila1.add(Box.createHorizontalStrut(8));
        fila1.add(EstiloCompras.labeled("Forma de pago", cmbFormaPago, 140));
        fila1.add(Box.createHorizontalStrut(8));
        fila1.add(EstiloCompras.labeled("Empresa", cmbEmpresa, 230));
        fila1.add(Box.createHorizontalGlue());

        JPanel fila2 = filaFiltros();
        fila2.add(EstiloCompras.labeled("Cliente (escriba para filtrar)", cmbCliente, 320));
        fila2.add(Box.createHorizontalStrut(8));
        fila2.add(EstiloCompras.labeled("Vendedor", cmbVendedor, 280));
        fila2.add(Box.createHorizontalStrut(16));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnBuscar);
        botones.add(btnLimpiar);
        botones.add(btnExportar);
        fila2.add(EstiloCompras.labeled(" ", botones, 0));
        fila2.add(Box.createHorizontalGlue());

        JPanel filtros = new JPanel();
        filtros.setOpaque(false);
        filtros.setLayout(new BoxLayout(filtros, BoxLayout.Y_AXIS));
        filtros.add(fila1);
        filtros.add(Box.createVerticalStrut(4));
        filtros.add(fila2);
        panel.add(filtros, BorderLayout.NORTH);

        // ---- Tablas maestro-detalle ----
        modeloFacturas = modeloNoEditable(new Object[]{
            "id", "Número", "Fecha factura", "Fecha impresión",
            "Cliente", "Vendedor", "Forma pago", "Items", "Total"});
        tablaFacturas = new JTable(modeloFacturas);
        EstiloCompras.styleTable(tablaFacturas);
        EstiloCompras.ocultarColumna(tablaFacturas, 0);
        EstiloCompras.anchoColumnas(tablaFacturas, 0, 110, 95, 130, 260, 220, 90, 60, 110);
        alinearDerecha(tablaFacturas, 7, 8);

        tablaFacturas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tablaFacturas.getSelectedRow();
                if (row >= 0) {
                    int id = Integer.parseInt(modeloFacturas.getValueAt(
                            tablaFacturas.convertRowIndexToModel(row), 0).toString());
                    cargarDetalle(id);
                }
            }
        });

        modeloDetalle = modeloNoEditable(new Object[]{
            "Código", "Descripción", "Cantidad", "Precio unit.",
            "IVA", "Total línea", "Novedad"});
        tablaDetalle = new JTable(modeloDetalle);
        EstiloCompras.styleTable(tablaDetalle);
        EstiloCompras.anchoColumnas(tablaDetalle, 110, 360, 80, 100, 60, 110, 180);
        alinearDerecha(tablaDetalle, 2, 3, 4, 5);
        tablaDetalle.getColumnModel().getColumn(6).setCellRenderer(new NovedadRenderer());

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        split.setTopComponent(panelTitulado("Facturas encontradas", tablaFacturas));
        split.setBottomComponent(panelTitulado("Detalle de la factura seleccionada", tablaDetalle));
        panel.add(split, BorderLayout.CENTER);

        lblResumenFacturas = crearResumen();
        panel.add(lblResumenFacturas, BorderLayout.SOUTH);
        return panel;
    }

    private void limpiarFiltrosFacturas() {
        dateDesde.setDate(null);
        dateHasta.setDate(null);
        txtNumeroFactura.setText("");
        cmbCliente.setSelectedIndex(cmbCliente.getItemCount() > 0 ? 0 : -1);
        cmbVendedor.setSelectedIndex(cmbVendedor.getItemCount() > 0 ? 0 : -1);
        cmbFormaPago.setSelectedIndex(cmbFormaPago.getItemCount() > 0 ? 0 : -1);
        cmbEmpresa.setSelectedIndex(cmbEmpresa.getItemCount() > 0 ? 0 : -1);
        modeloFacturas.setRowCount(0);
        modeloDetalle.setRowCount(0);
        lblResumenFacturas.setText(" ");
    }

    private void buscarFacturas() {
        modeloFacturas.setRowCount(0);
        modeloDetalle.setRowCount(0);

        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        agregarFiltroFechas(where, "f.fecha_factura", dateDesde, dateHasta);

        String numero = txtNumeroFactura.getText().trim();
        if (!numero.isEmpty()) {
            where.append("AND f.numero_factura ILIKE '%").append(esc(numero)).append("%' ");
        }
        String cliente = textoCombo(cmbCliente);
        if (!cliente.isEmpty()) {
            where.append("AND f.cliente ILIKE '%").append(esc(cliente)).append("%' ");
        }
        String vendedor = seleccionCombo(cmbVendedor);
        if (vendedor != null) {
            where.append("AND f.vendedor = '").append(esc(vendedor)).append("' ");
        }
        String formaPago = seleccionCombo(cmbFormaPago);
        if (formaPago != null) {
            where.append("AND f.forma_pago = '").append(esc(formaPago)).append("' ");
        }
        String empresa = seleccionCombo(cmbEmpresa);
        if (empresa != null) {
            where.append("AND f.empresa = '").append(esc(empresa)).append("' ");
        }

        String sql = "SELECT f.id, f.numero_factura, f.fecha_factura, f.fecha_impresion, "
                + "f.cliente, f.vendedor, f.forma_pago, "
                + "COALESCE(d.items, 0) AS items, COALESCE(d.total, 0) AS total "
                + "FROM facturas_impresas f "
                + "LEFT JOIN (SELECT factura_id, COUNT(*) AS items, "
                + "           SUM(COALESCE(total_linea, 0)) AS total "
                + "           FROM detalle_factura GROUP BY factura_id) d ON d.factura_id = f.id "
                + where
                + "ORDER BY f.fecha_factura DESC, f.id DESC LIMIT " + LIMITE_FACTURAS;

        int count = 0;
        double totalGeneral = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                double total = rs.getDouble("total");
                totalGeneral += total;
                modeloFacturas.addRow(new Object[]{
                    rs.getInt("id"),
                    nvl(rs.getString("numero_factura")),
                    rs.getDate("fecha_factura") != null ? rs.getDate("fecha_factura").toString() : "",
                    rs.getTimestamp("fecha_impresion") != null
                        ? rs.getTimestamp("fecha_impresion").toString().substring(0, 16) : "",
                    nvl(rs.getString("cliente")),
                    nvl(rs.getString("vendedor")),
                    nvl(rs.getString("forma_pago")),
                    FMT_CANTIDAD.format(rs.getInt("items")),
                    FMT_MONEDA.format(total)
                });
                count++;
            }
            rs.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error consultando facturas: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String aviso = count >= LIMITE_FACTURAS
                ? "  (se muestran las primeras " + LIMITE_FACTURAS + ", afine los filtros)" : "";
        lblResumenFacturas.setText("  " + count + " facturas   |   Total: "
                + FMT_MONEDA.format(totalGeneral) + aviso);
        if (count == 0) {
            JOptionPane.showMessageDialog(this,
                    "No se encontraron facturas con los filtros indicados");
        }
    }

    private void cargarDetalle(int facturaId) {
        modeloDetalle.setRowCount(0);
        String sql = "SELECT codigo_producto, descripcion, cantidad, precio_unitario, "
                + "iva, total_linea, es_novedad, motivo_novedad "
                + "FROM detalle_factura WHERE factura_id = " + facturaId + " ORDER BY id";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                modeloDetalle.addRow(new Object[]{
                    nvl(rs.getString("codigo_producto")),
                    nvl(rs.getString("descripcion")),
                    FMT_CANTIDAD.format(rs.getDouble("cantidad")),
                    FMT_MONEDA.format(rs.getDouble("precio_unitario")),
                    FMT_CANTIDAD.format(rs.getDouble("iva")),
                    FMT_MONEDA.format(rs.getDouble("total_linea")),
                    rs.getBoolean("es_novedad") ? nvl2(rs.getString("motivo_novedad"), "NOVEDAD") : ""
                });
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error cargando detalle de factura: " + e.getMessage());
        }
    }

    // ================================================================
    // TAB 2: consulta por producto
    // ================================================================

    private JPanel construirTabProductos() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(EstiloCompras.BG_FORM);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        txtProducto = EstiloCompras.field(
                "Código o nombre del producto (mín. 2 letras, busca en vivo)",
                FontAwesome.SEARCH);
        dateDesdeProd = crearDateChooser();
        dateHastaProd = crearDateChooser();

        JButton btnBuscarProd = EstiloCompras.primaryBtn("Buscar", FontAwesome.SEARCH);
        btnBuscarProd.addActionListener(e -> buscarProductos());
        JButton btnExportarProd = EstiloCompras.successBtn("Exportar", FontAwesome.SAVE);
        btnExportarProd.addActionListener(e -> exportar(tablaProductos));

        debounceProducto = new Timer(450, e -> buscarProductos());
        debounceProducto.setRepeats(false);
        txtProducto.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { programarBusqueda(); }
            @Override
            public void removeUpdate(DocumentEvent e) { programarBusqueda(); }
            @Override
            public void changedUpdate(DocumentEvent e) { programarBusqueda(); }
        });
        txtProducto.addActionListener(e -> buscarProductos());

        JPanel fila = filaFiltros();
        fila.add(EstiloCompras.labeled("Producto", txtProducto, 420));
        fila.add(Box.createHorizontalStrut(8));
        fila.add(EstiloCompras.labeled("Desde (opcional)", dateDesdeProd, 150));
        fila.add(Box.createHorizontalStrut(8));
        fila.add(EstiloCompras.labeled("Hasta (opcional)", dateHastaProd, 150));
        fila.add(Box.createHorizontalStrut(16));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnBuscarProd);
        botones.add(btnExportarProd);
        fila.add(EstiloCompras.labeled(" ", botones, 0));
        fila.add(Box.createHorizontalGlue());
        panel.add(fila, BorderLayout.NORTH);

        modeloProductos = modeloNoEditable(new Object[]{
            "Fecha", "Número", "Cliente", "Vendedor", "Código",
            "Descripción", "Cantidad", "Precio unit.", "IVA", "Total línea", "Novedad"});
        tablaProductos = new JTable(modeloProductos);
        EstiloCompras.styleTable(tablaProductos);
        EstiloCompras.anchoColumnas(tablaProductos, 90, 100, 220, 180, 100, 280, 75, 100, 55, 105, 150);
        alinearDerecha(tablaProductos, 6, 7, 8, 9);
        tablaProductos.getColumnModel().getColumn(10).setCellRenderer(new NovedadRenderer());

        // Doble clic en una linea: salta al tab "Por factura" con esa factura
        tablaProductos.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tablaProductos.getSelectedRow();
                    if (row >= 0) {
                        String numero = modeloProductos.getValueAt(
                                tablaProductos.convertRowIndexToModel(row), 1).toString();
                        limpiarFiltrosFacturas();
                        txtNumeroFactura.setText(numero);
                        tabs.setSelectedIndex(0);
                        buscarFacturas();
                        if (modeloFacturas.getRowCount() > 0) {
                            tablaFacturas.setRowSelectionInterval(0, 0);
                        }
                    }
                }
            }
        });

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        centro.add(construirPanelEstadisticas(), BorderLayout.NORTH);
        centro.add(panelTitulado(
                "Historial del producto en facturas WO (doble clic = ver la factura completa)",
                tablaProductos), BorderLayout.CENTER);
        panel.add(centro, BorderLayout.CENTER);

        lblResumenProductos = crearResumen();
        panel.add(lblResumenProductos, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel construirPanelEstadisticas() {
        JPanel stats = new JPanel(new GridLayout(1, 8, 8, 0));
        stats.setOpaque(false);
        valRegistros = new JLabel("-");
        valFacturas = new JLabel("-");
        valCantidad = new JLabel("-");
        valPrecioUltimo = new JLabel("-");
        valPrecioMin = new JLabel("-");
        valPrecioMax = new JLabel("-");
        valPrecioProm = new JLabel("-");
        valTotal = new JLabel("-");
        stats.add(tarjetaStat("Registros", valRegistros));
        stats.add(tarjetaStat("Facturas", valFacturas));
        stats.add(tarjetaStat("Cantidad total", valCantidad));
        stats.add(tarjetaStat("Precio último", valPrecioUltimo));
        stats.add(tarjetaStat("Precio mín.", valPrecioMin));
        stats.add(tarjetaStat("Precio máx.", valPrecioMax));
        stats.add(tarjetaStat("Precio prom.", valPrecioProm));
        stats.add(tarjetaStat("Total facturado", valTotal));
        return stats;
    }

    private JPanel tarjetaStat(String titulo, JLabel valor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(EstiloCompras.BG_SECTION);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JLabel lbl = new JLabel(titulo.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(EstiloCompras.TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        valor.setFont(new Font("Segoe UI", Font.BOLD, 15));
        valor.setForeground(EstiloCompras.TEXT_PRIMARY);
        valor.setAlignmentX(LEFT_ALIGNMENT);
        card.add(lbl);
        card.add(Box.createVerticalStrut(2));
        card.add(valor);
        return card;
    }

    private void programarBusqueda() {
        if (txtProducto.getText().trim().length() >= 2) {
            debounceProducto.restart();
        }
    }

    private void buscarProductos() {
        String busqueda = txtProducto.getText().trim();
        if (busqueda.length() < 2) {
            return;
        }

        modeloProductos.setRowCount(0);

        StringBuilder where = new StringBuilder("WHERE (d.codigo_producto ILIKE '%")
                .append(esc(busqueda)).append("%' OR d.descripcion ILIKE '%")
                .append(esc(busqueda)).append("%') ");
        agregarFiltroFechas(where, "f.fecha_factura", dateDesdeProd, dateHastaProd);

        String from = "FROM detalle_factura d "
                + "JOIN facturas_impresas f ON d.factura_id = f.id ";

        String sql = "SELECT f.fecha_factura, f.numero_factura, f.cliente, f.vendedor, "
                + "d.codigo_producto, d.descripcion, d.cantidad, d.precio_unitario, "
                + "d.iva, d.total_linea, d.es_novedad, d.motivo_novedad "
                + from + where
                + "ORDER BY f.fecha_factura DESC, f.id DESC, d.id LIMIT " + LIMITE_LINEAS;

        int count = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                modeloProductos.addRow(new Object[]{
                    rs.getDate("fecha_factura") != null ? rs.getDate("fecha_factura").toString() : "",
                    nvl(rs.getString("numero_factura")),
                    nvl(rs.getString("cliente")),
                    nvl(rs.getString("vendedor")),
                    nvl(rs.getString("codigo_producto")),
                    nvl(rs.getString("descripcion")),
                    FMT_CANTIDAD.format(rs.getDouble("cantidad")),
                    FMT_MONEDA.format(rs.getDouble("precio_unitario")),
                    FMT_CANTIDAD.format(rs.getDouble("iva")),
                    FMT_MONEDA.format(rs.getDouble("total_linea")),
                    rs.getBoolean("es_novedad") ? nvl2(rs.getString("motivo_novedad"), "NOVEDAD") : ""
                });
                count++;
            }
            rs.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error consultando productos: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        cargarEstadisticasProducto(from, where.toString());

        String aviso = count >= LIMITE_LINEAS
                ? "  (se muestran las primeras " + LIMITE_LINEAS
                + " líneas, las estadísticas sí incluyen todo)" : "";
        lblResumenProductos.setText("  " + count + " líneas encontradas para \""
                + busqueda + "\"" + aviso);
    }

    /**
     * Las estadisticas se calculan en la BD sobre TODO el resultado
     * (sin el LIMIT de la tabla) para que sean exactas.
     */
    private void cargarEstadisticasProducto(String from, String where) {
        String sqlAgg = "SELECT COUNT(*) AS regs, COUNT(DISTINCT d.factura_id) AS facs, "
                + "SUM(COALESCE(d.cantidad, 0)) AS cant, "
                + "MIN(d.precio_unitario) AS pmin, MAX(d.precio_unitario) AS pmax, "
                + "AVG(d.precio_unitario) AS pprom, "
                + "SUM(COALESCE(d.total_linea, 0)) AS total "
                + from + where;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sqlAgg);
            if (rs.next()) {
                valRegistros.setText(FMT_CANTIDAD.format(rs.getLong("regs")));
                valFacturas.setText(FMT_CANTIDAD.format(rs.getLong("facs")));
                valCantidad.setText(FMT_CANTIDAD.format(rs.getDouble("cant")));
                valPrecioMin.setText(FMT_MONEDA.format(rs.getDouble("pmin")));
                valPrecioMax.setText(FMT_MONEDA.format(rs.getDouble("pmax")));
                valPrecioProm.setText(FMT_MONEDA.format(rs.getDouble("pprom")));
                valTotal.setText(FMT_MONEDA.format(rs.getDouble("total")));
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error en estadisticas de producto: " + e.getMessage());
        }

        String sqlUltimo = "SELECT d.precio_unitario "
                + from + where
                + "ORDER BY f.fecha_factura DESC, d.id DESC LIMIT 1";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sqlUltimo);
            valPrecioUltimo.setText(rs.next()
                    ? FMT_MONEDA.format(rs.getDouble("precio_unitario")) : "-");
            rs.close();
        } catch (Exception e) {
            System.err.println("Error en ultimo precio: " + e.getMessage());
        }
    }

    // ================================================================
    // Carga de combos
    // ================================================================

    private void cargarCombos() {
        cargarCombo(cmbCliente, "SELECT DISTINCT cliente FROM facturas_impresas "
                + "WHERE cliente IS NOT NULL AND cliente <> '' ORDER BY cliente");
        cargarCombo(cmbVendedor, "SELECT DISTINCT vendedor FROM facturas_impresas "
                + "WHERE vendedor IS NOT NULL AND vendedor <> '' ORDER BY vendedor");
        cargarCombo(cmbFormaPago, "SELECT DISTINCT forma_pago FROM facturas_impresas "
                + "WHERE forma_pago IS NOT NULL AND forma_pago <> '' ORDER BY forma_pago");
        cargarCombo(cmbEmpresa, "SELECT DISTINCT empresa FROM facturas_impresas "
                + "WHERE empresa IS NOT NULL AND empresa <> '' ORDER BY empresa");
    }

    private void cargarCombo(JComboBox<String> combo, String sql) {
        combo.removeAllItems();
        combo.addItem("Todos");
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                combo.addItem(rs.getString(1));
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error cargando combo: " + e.getMessage());
        }
        combo.setSelectedIndex(0);
    }

    // ================================================================
    // Helpers
    // ================================================================

    private com.toedter.calendar.JDateChooser crearDateChooser() {
        com.toedter.calendar.JDateChooser dc = new com.toedter.calendar.JDateChooser();
        dc.setDateFormatString("yyyy-MM-dd");
        dc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dc.setPreferredSize(new Dimension(0, 38));
        return dc;
    }

    private JPanel filaFiltros() {
        JPanel fila = new JPanel();
        fila.setOpaque(false);
        fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));
        fila.setAlignmentX(LEFT_ALIGNMENT);
        return fila;
    }

    private JPanel panelTitulado(String titulo, JTable tabla) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(EstiloCompras.sectionTitle(titulo), BorderLayout.NORTH);
        p.add(EstiloCompras.scroll(tabla), BorderLayout.CENTER);
        return p;
    }

    private JLabel crearResumen() {
        JLabel lbl = new JLabel(" ");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(EstiloCompras.PRIMARY_DARK);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCompras.DIVIDER),
                BorderFactory.createEmptyBorder(8, 4, 4, 4)));
        return lbl;
    }

    private DefaultTableModel modeloNoEditable(Object[] columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    private void alinearDerecha(JTable tabla, int... columnas) {
        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer();
        derecha.setHorizontalAlignment(SwingConstants.RIGHT);
        derecha.setFont(new Font("Roboto", Font.PLAIN, 14));
        for (int c : columnas) {
            tabla.getColumnModel().getColumn(c).setCellRenderer(derecha);
        }
    }

    private void agregarFiltroFechas(StringBuilder where, String campo,
            com.toedter.calendar.JDateChooser desde, com.toedter.calendar.JDateChooser hasta) {
        Date d1 = desde.getDate();
        Date d2 = hasta.getDate();
        if (d1 != null) {
            where.append("AND ").append(campo).append(" >= '")
                    .append(new java.sql.Date(d1.getTime())).append("' ");
        }
        if (d2 != null) {
            where.append("AND ").append(campo).append(" <= '")
                    .append(new java.sql.Date(d2.getTime())).append("' ");
        }
    }

    private void exportar(JTable tabla) {
        if (tabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar");
            return;
        }
        try {
            new ExportarExcel().exportarExcel(tabla);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error exportando: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** El combo de cliente es editable: toma el texto escrito salvo que sea "Todos". */
    private String textoCombo(JComboBox<String> combo) {
        Object item = combo.getEditor().getItem();
        String texto = item == null ? "" : item.toString().trim();
        return "Todos".equalsIgnoreCase(texto) ? "" : texto;
    }

    /** Combos no editables: null si esta en "Todos". */
    private String seleccionCombo(JComboBox<String> combo) {
        Object sel = combo.getSelectedItem();
        if (sel == null || "Todos".equals(sel.toString())) {
            return null;
        }
        return sel.toString();
    }

    private static String esc(String s) {
        return s.replace("'", "''");
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String nvl2(String s, String porDefecto) {
        return (s == null || s.trim().isEmpty()) ? porDefecto : s;
    }

    /** Pinta la columna de novedad como chip ambar cuando hay novedad. */
    private static class NovedadRenderer extends DefaultTableCellRenderer {

        NovedadRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String v = value == null ? "" : value.toString().trim();
            setFont(new Font("Segoe UI", v.isEmpty() ? Font.PLAIN : Font.BOLD, 12));
            if (!isSelected) {
                if (v.isEmpty()) {
                    setBackground(Color.WHITE);
                    setForeground(EstiloCompras.TEXT_PRIMARY);
                } else {
                    setBackground(new Color(0xFFF8E1));
                    setForeground(new Color(0xF57F17));
                }
            }
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return this;
        }
    }
}
