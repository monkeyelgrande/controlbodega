/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios_internos;

import Formularios.frm_main;
import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBordenes_compra;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import modelos.Contactos;
import modelos.Ordenes_compra_detalle;

/**
 * Análisis y aprobación de una orden de compra. Muestra, por cada producto, el
 * histórico de compras (proveedores y precios anteriores) para decidir a quién
 * comprar; permite fijar proveedor y precio POR LÍNEA y luego aprobar o
 * rechazar.
 *
 * @author Monkeyelgrande
 */
public class jd_analisis_orden_compra extends JDialog {

    private final int idOrden;

    private JLabel lbl_info;
    private JTable jtabla_lineas;
    private JTable jtabla_historico;
    private DefaultTableModel modeloLineas;
    private DefaultTableModel modeloHistorico;
    private JButton btn_aprobar;
    private JButton btn_rechazar;

    private final List<Contactos> proveedores = new ArrayList<>();

    private static final int COL_ID_DET = 0;
    private static final int COL_ID_PROD = 1;
    private static final int COL_CODIGO = 2;
    private static final int COL_DESC = 3;
    private static final int COL_CANT = 4;
    private static final int COL_PROV = 5;
    private static final int COL_PRECIO = 6;

    public jd_analisis_orden_compra(int idOrden) {
        this.idOrden = idOrden;
        cargarProveedores();
        initUI();
        cargarCabecera();
        cargarLineas();
        setLocationRelativeTo(null);
        metodos.addEscapeListenerWindowDialog(this);
    }

    private void cargarProveedores() {
        proveedores.add(new Contactos(0, "(seleccione)"));
        ResultSet rs = DB_consultas_R_D.getTabla(
                "SELECT id, nombre FROM contactos WHERE proveedor = 1 ORDER BY nombre");
        try {
            while (rs.next()) {
                proveedores.add(new Contactos(rs.getInt("id"), rs.getString("nombre")));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private Contactos proveedorPorId(int id) {
        for (Contactos c : proveedores) {
            if (c.getId() == id) {
                return c;
            }
        }
        return proveedores.get(0);
    }

    private void initUI() {
        setModal(true);
        setUndecorated(true);
        setSize(1200, 840);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));

        root.add(EstiloCompras.header(FontAwesome.CLIPBOARD,
                "Análisis y aprobación de orden de compra", () -> dispose()), BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setBackground(EstiloCompras.BG_FORM);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 20, 16, 20));

        cuerpo.add(buildInfo(), BorderLayout.NORTH);
        cuerpo.add(buildSplit(), BorderLayout.CENTER);

        root.add(cuerpo, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JComponent buildInfo() {
        lbl_info = new JLabel("Cargando...");
        lbl_info.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl_info.setForeground(EstiloCompras.TEXT_PRIMARY);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(0xE3F2FD));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBBDEFB), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel ic = new JLabel(FontAwesome.icon(FontAwesome.INFO, 16f, EstiloCompras.PRIMARY));
        ic.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        card.add(ic, BorderLayout.WEST);
        card.add(lbl_info, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildSplit() {
        // ---- Tabla de líneas ----
        modeloLineas = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return columna == COL_PROV || columna == COL_PRECIO;
            }
        };
        modeloLineas.setColumnIdentifiers(new Object[]{
            "ID_DET", "ID_PROD", "CÓDIGO", "DESCRIPCIÓN", "CANTIDAD", "PROVEEDOR", "PRECIO UNIT"});
        jtabla_lineas = new JTable(modeloLineas);
        EstiloCompras.styleTable(jtabla_lineas);
        jtabla_lineas.setRowHeight(34);

        JComboBox<Contactos> comboProv = new JComboBox<>();
        comboProv.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        for (Contactos c : proveedores) {
            comboProv.addItem(c);
        }
        jtabla_lineas.getColumnModel().getColumn(COL_PROV).setCellEditor(new DefaultCellEditor(comboProv));

        ocultarColumna(jtabla_lineas, COL_ID_DET);
        ocultarColumna(jtabla_lineas, COL_ID_PROD);
        // CÓDIGO estrecho; DESCRIPCIÓN amplia; resto moderado.
        EstiloCompras.anchoColumnas(jtabla_lineas, 0, 0, 110, 480, 90, 200, 120);

        jtabla_lineas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarHistoricoDeSeleccion();
            }
        });

        JPanel pnlLineas = new JPanel(new BorderLayout(0, 6));
        pnlLineas.setOpaque(false);
        pnlLineas.add(tituloPanel("Productos de la orden — asigne proveedor y precio a cada línea"),
                BorderLayout.NORTH);
        pnlLineas.add(EstiloCompras.scroll(jtabla_lineas), BorderLayout.CENTER);

        // ---- Tabla de histórico ----
        modeloHistorico = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        modeloHistorico.setColumnIdentifiers(new Object[]{
            "ID_PROV", "FECHA", "FACTURA", "PROVEEDOR", "CANTIDAD", "PRECIO COSTO"});
        jtabla_historico = new JTable(modeloHistorico);
        EstiloCompras.styleTable(jtabla_historico);
        jtabla_historico.setRowHeight(30);
        ocultarColumna(jtabla_historico, 0);
        EstiloCompras.anchoColumnas(jtabla_historico, 0, 110, 140, 360, 100, 130);
        jtabla_historico.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent me) {
                if (me.getClickCount() == 2) {
                    copiarHistoricoALinea();
                }
            }
        });

        JPanel pnlHist = new JPanel(new BorderLayout(0, 6));
        pnlHist.setOpaque(false);
        pnlHist.add(tituloPanel("Histórico de compras del producto seleccionado "
                + "(doble clic para copiar proveedor y precio)"), BorderLayout.NORTH);
        pnlHist.add(EstiloCompras.scroll(jtabla_historico), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pnlLineas, pnlHist);
        split.setBorder(null);
        split.setOpaque(false);
        split.setResizeWeight(0.5);
        split.setDividerLocation(300);
        return split;
    }

    private JLabel tituloPanel(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(EstiloCompras.TEXT_SECONDARY);
        return l;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(EstiloCompras.BG_SECTION);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCompras.DIVIDER),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        btn_rechazar = EstiloCompras.dangerBtn("Rechazar", FontAwesome.CLOSE);
        btn_rechazar.addActionListener(e -> rechazar());
        btn_aprobar = EstiloCompras.successBtn("Aprobar", FontAwesome.CHECK);
        btn_aprobar.addActionListener(e -> aprobar());

        JButton btn_cerrar = EstiloCompras.secondaryBtn("Cerrar", FontAwesome.CLOSE);
        btn_cerrar.addActionListener(e -> dispose());

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(btn_cerrar);
        right.add(Box.createHorizontalStrut(8));
        right.add(btn_rechazar);
        right.add(Box.createHorizontalStrut(8));
        right.add(btn_aprobar);

        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void ocultarColumna(JTable tabla, int modelIndex) {
        TableColumn col = tabla.getColumnModel().getColumn(modelIndex);
        col.setMinWidth(0);
        col.setMaxWidth(0);
        col.setPreferredWidth(0);
    }

    private void cargarCabecera() {
        ResultSet rs = DBordenes_compra.cargarCabecera(idOrden);
        try {
            if (rs.next()) {
                lbl_info.setText("<html><b>Orden " + rs.getString("numero") + "</b>"
                        + " &nbsp;•&nbsp; Creada por: " + rs.getString("creador")
                        + " &nbsp;•&nbsp; Fecha: " + rs.getDate("fecha")
                        + " &nbsp;•&nbsp; Bodega: " + rs.getString("bodega")
                        + " &nbsp;•&nbsp; Obs: "
                        + (rs.getString("observacion") == null ? "—" : rs.getString("observacion"))
                        + "</html>");
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void cargarLineas() {
        modeloLineas.setRowCount(0);
        ResultSet rs = DBordenes_compra.cargarDetalles(idOrden);
        try {
            while (rs.next()) {
                int idProv = rs.getInt("id_proveedor");
                Object precio = rs.getObject("precio_unitario") == null
                        ? "" : metodos.formateador_decimal().format(rs.getDouble("precio_unitario"));
                modeloLineas.addRow(new Object[]{
                    rs.getString("id"), rs.getString("id_producto"),
                    rs.getString("codigo_barras"), rs.getString("descripcion"),
                    metodos.formateador_decimal().format(rs.getDouble("cantidad")),
                    proveedorPorId(idProv), precio});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        if (modeloLineas.getRowCount() > 0) {
            jtabla_lineas.setRowSelectionInterval(0, 0);
        }
    }

    private void cargarHistoricoDeSeleccion() {
        modeloHistorico.setRowCount(0);
        int fila = jtabla_lineas.getSelectedRow();
        if (fila < 0) {
            return;
        }
        int idProducto = Integer.parseInt(modeloLineas.getValueAt(fila, COL_ID_PROD).toString());
        ResultSet rs = DBordenes_compra.historicoComprasProducto(idProducto);
        try {
            while (rs.next()) {
                modeloHistorico.addRow(new Object[]{
                    rs.getString("id_proveedor"), rs.getDate("fecha"), rs.getString("no_factura"),
                    rs.getString("proveedor"),
                    metodos.formateador_decimal().format(rs.getDouble("cantidad")),
                    metodos.formateador_decimal().format(rs.getDouble("precio_costo"))});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void copiarHistoricoALinea() {
        int filaLinea = jtabla_lineas.getSelectedRow();
        int filaHist = jtabla_historico.getSelectedRow();
        if (filaLinea < 0 || filaHist < 0) {
            return;
        }
        if (jtabla_lineas.isEditing()) {
            jtabla_lineas.getCellEditor().stopCellEditing();
        }
        int idProv = Integer.parseInt(modeloHistorico.getValueAt(filaHist, 0).toString());
        Object precio = modeloHistorico.getValueAt(filaHist, 5);
        modeloLineas.setValueAt(proveedorPorId(idProv), filaLinea, COL_PROV);
        modeloLineas.setValueAt(precio, filaLinea, COL_PRECIO);
    }

    private List<Ordenes_compra_detalle> leerLineasValidadas() {
        if (jtabla_lineas.isEditing()) {
            jtabla_lineas.getCellEditor().stopCellEditing();
        }
        List<Ordenes_compra_detalle> lineas = new ArrayList<>();
        for (int i = 0; i < modeloLineas.getRowCount(); i++) {
            Object provObj = modeloLineas.getValueAt(i, COL_PROV);
            int idProv = (provObj instanceof Contactos) ? ((Contactos) provObj).getId() : 0;
            if (idProv <= 0) {
                JOptionPane.showMessageDialog(this, "Asigne un proveedor a la fila " + (i + 1));
                return null;
            }
            double precio;
            try {
                precio = Double.parseDouble(String.valueOf(modeloLineas.getValueAt(i, COL_PRECIO))
                        .replace(".", "").replace(",", "."));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Precio inválido en la fila " + (i + 1));
                return null;
            }
            if (precio <= 0) {
                JOptionPane.showMessageDialog(this,
                        "El precio debe ser mayor a 0 en la fila " + (i + 1));
                return null;
            }
            Ordenes_compra_detalle d = new Ordenes_compra_detalle();
            d.setId(Integer.parseInt(modeloLineas.getValueAt(i, COL_ID_DET).toString()));
            d.setId_proveedor(idProv);
            d.setPrecio_unitario(precio);
            lineas.add(d);
        }
        return lineas;
    }

    private void aprobar() {
        List<Ordenes_compra_detalle> lineas = leerLineasValidadas();
        if (lineas == null) {
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "¿Aprobar esta orden de compra con los proveedores y precios asignados?",
                "Confirmar aprobación", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        DBordenes_compra dao = new DBordenes_compra();
        if (dao.AprobarConLineas(idOrden, frm_main.id_user, lineas)) {
            JOptionPane.showMessageDialog(this, "Orden APROBADA correctamente.");
            dispose();
        }
    }

    private void rechazar() {
        int r = JOptionPane.showConfirmDialog(this,
                "¿Rechazar esta orden de compra?",
                "Confirmar rechazo", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        DBordenes_compra dao = new DBordenes_compra();
        if (dao.Rechazar(idOrden, frm_main.id_user) > 0) {
            JOptionPane.showMessageDialog(this, "Orden RECHAZADA.");
            dispose();
        }
    }
}
