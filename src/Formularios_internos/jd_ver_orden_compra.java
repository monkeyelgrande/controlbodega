/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios_internos;

import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.ImprimirOrdenCompraPDF;
import Metodos.metodos;
import conexiondb.DBordenes_compra;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.sql.ResultSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import modelos.Ordenes_compra_cabecera;

/**
 * Vista de solo lectura de una orden de compra ya aprobada (o rechazada): cómo
 * quedó establecida con proveedor, precio y subtotal por línea, total general,
 * y opción de imprimir/exportar un PDF.
 *
 * @author Monkeyelgrande
 */
public class jd_ver_orden_compra extends JDialog {

    private final int idOrden;

    private JLabel lbl_info;
    private JLabel lbl_total;
    private JTable jtabla;
    private DefaultTableModel modelo;

    public jd_ver_orden_compra(int idOrden) {
        this.idOrden = idOrden;
        initUI();
        cargarCabecera();
        cargarDetalles();
        setLocationRelativeTo(null);
        metodos.addEscapeListenerWindowDialog(this);
    }

    private void initUI() {
        setModal(true);
        setUndecorated(true);
        setSize(1100, 660);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));

        root.add(EstiloCompras.header(FontAwesome.FILE_INVOICE,
                "Orden de compra aprobada", () -> dispose()), BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setBackground(EstiloCompras.BG_FORM);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 20, 16, 20));

        cuerpo.add(buildInfo(), BorderLayout.NORTH);

        modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        modelo.setColumnIdentifiers(new Object[]{
            "CÓDIGO", "DESCRIPCIÓN", "CANTIDAD", "PROVEEDOR", "PRECIO UNIT", "SUBTOTAL"});
        jtabla = new JTable(modelo);
        EstiloCompras.styleTable(jtabla);
        EstiloCompras.anchoColumnas(jtabla, 110, 420, 90, 200, 120, 130);
        cuerpo.add(EstiloCompras.scroll(jtabla), BorderLayout.CENTER);

        root.add(cuerpo, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JComponent buildInfo() {
        lbl_info = new JLabel("Cargando...");
        lbl_info.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl_info.setForeground(EstiloCompras.TEXT_PRIMARY);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(0xE8F5E9));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC8E6C9), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel ic = new JLabel(FontAwesome.icon(FontAwesome.CHECK, 16f, EstiloCompras.SUCCESS));
        ic.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        card.add(ic, BorderLayout.WEST);
        card.add(lbl_info, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(EstiloCompras.BG_SECTION);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCompras.DIVIDER),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        lbl_total = new JLabel("Total estimado: $ 0");
        lbl_total.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl_total.setForeground(EstiloCompras.PRIMARY);

        JButton btn_pdf = EstiloCompras.primaryBtn("Imprimir PDF", FontAwesome.FILE_INVOICE);
        btn_pdf.addActionListener(e -> new ImprimirOrdenCompraPDF().imprimir(idOrden));
        JButton btn_cerrar = EstiloCompras.secondaryBtn("Cerrar", FontAwesome.CLOSE);
        btn_cerrar.addActionListener(e -> dispose());

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(btn_pdf);
        right.add(Box.createHorizontalStrut(8));
        right.add(btn_cerrar);

        footer.add(lbl_total, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void cargarCabecera() {
        ResultSet rs = DBordenes_compra.cargarCabecera(idOrden);
        try {
            if (rs.next()) {
                String fa = rs.getString("fecha_aprobacion");
                lbl_info.setText("<html><b>Orden " + rs.getString("numero") + "</b>"
                        + " &nbsp;•&nbsp; Estado: " + Ordenes_compra_cabecera.nombreEstado(rs.getInt("estado"))
                        + " &nbsp;•&nbsp; Creada por: " + rs.getString("creador")
                        + " &nbsp;•&nbsp; Aprobada por: " + rs.getString("aprobador")
                        + " &nbsp;•&nbsp; Bodega: " + rs.getString("bodega")
                        + (fa == null ? "" : " &nbsp;•&nbsp; Aprobación: " + fa)
                        + " &nbsp;•&nbsp; Obs: "
                        + (rs.getString("observacion") == null ? "—" : rs.getString("observacion"))
                        + "</html>");
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void cargarDetalles() {
        modelo.setRowCount(0);
        double total = 0;
        ResultSet rs = DBordenes_compra.cargarDetalles(idOrden);
        try {
            while (rs.next()) {
                double cant = rs.getDouble("cantidad");
                double precio = rs.getObject("precio_unitario") == null ? 0 : rs.getDouble("precio_unitario");
                double subtotal = cant * precio;
                total += subtotal;
                modelo.addRow(new Object[]{
                    rs.getString("codigo_barras"), rs.getString("descripcion"),
                    metodos.formateador_decimal().format(cant),
                    rs.getString("proveedor"),
                    "$ " + metodos.formateador_decimal().format(precio),
                    "$ " + metodos.formateador_decimal().format(subtotal)});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        lbl_total.setText("Total estimado: $ " + metodos.formateador_decimal().format(total));
    }
}
