/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios_internos;

import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DBcotizaciones;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Editor de una solicitud de cotización (RFQ): condiciones y captura de la
 * respuesta del proveedor (precio, IVA, plazo por línea).
 *
 * @author Monkeyelgrande
 */
public class jd_cotizacion extends JDialog {

    private final int idCotiz;
    private JLabel lbl_info;
    private JTextField txt_condicion;
    private JTextField txt_validez;
    private JTextField txt_limite;
    private JTextField txt_obs;
    private JTable jtabla;
    private DefaultTableModel modelo;

    private static final int C_ID = 0;
    private static final int C_COD = 1;
    private static final int C_DESC = 2;
    private static final int C_CANT = 3;
    private static final int C_PRECIO = 4;
    private static final int C_IVA = 5;
    private static final int C_PLAZO = 6;

    public jd_cotizacion(int idCotiz) {
        this.idCotiz = idCotiz;
        initUI();
        cargar();
        setLocationRelativeTo(null);
        metodos.addEscapeListenerWindowDialog(this);
    }

    private void initUI() {
        setModal(true);
        setUndecorated(true);
        setSize(1040, 640);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));
        root.add(EstiloCompras.header(FontAwesome.FILE_INVOICE, "Solicitud de cotización (RFQ)", () -> dispose()),
                BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setBackground(EstiloCompras.BG_FORM);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 20, 16, 20));
        cuerpo.add(buildInfo(), BorderLayout.NORTH);

        modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int f, int c) {
                return c == C_PRECIO || c == C_IVA || c == C_PLAZO;
            }
        };
        modelo.setColumnIdentifiers(new Object[]{
            "ID", "CÓDIGO", "DESCRIPCIÓN", "CANTIDAD", "PRECIO UNIT", "IVA %", "PLAZO ENTREGA"});
        jtabla = new JTable(modelo);
        EstiloCompras.styleTable(jtabla);
        EstiloCompras.ocultarColumna(jtabla, C_ID);
        EstiloCompras.anchoColumnas(jtabla, 0, 100, 360, 90, 120, 70, 140);
        cuerpo.add(EstiloCompras.scroll(jtabla), BorderLayout.CENTER);

        root.add(cuerpo, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JComponent buildInfo() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        lbl_info = new JLabel("...");
        lbl_info.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl_info.setForeground(EstiloCompras.PRIMARY);
        lbl_info.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(lbl_info);
        p.add(Box.createVerticalStrut(8));

        txt_condicion = EstiloCompras.field("Condición de pago", null);
        txt_validez = EstiloCompras.field("Validez de la oferta", null);
        txt_limite = EstiloCompras.field("Fecha límite (yyyy-MM-dd)", null);
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(EstiloCompras.labeled("Condición de pago", txt_condicion, 0));
        row.add(Box.createHorizontalStrut(12));
        row.add(EstiloCompras.labeled("Validez", txt_validez, 0));
        row.add(Box.createHorizontalStrut(12));
        row.add(EstiloCompras.labeled("Fecha límite", txt_limite, 200));
        row.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(row);
        p.add(Box.createVerticalStrut(8));

        txt_obs = EstiloCompras.field("Observación", FontAwesome.CLIPBOARD);
        JComponent obsLab = EstiloCompras.labeled("Observación", txt_obs, 0);
        obsLab.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        p.add(obsLab);
        return p;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(EstiloCompras.BG_SECTION);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCompras.DIVIDER),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JButton btnExcel = EstiloCompras.secondaryBtn("Exportar Excel", FontAwesome.LIST);
        btnExcel.addActionListener(e -> {
            try {
                new Metodos.ExportarExcel().exportarExcel(jtabla);
            } catch (Exception ex) {
            }
        });
        JButton btnPdf = EstiloCompras.secondaryBtn("Exportar PDF", FontAwesome.FILE_INVOICE);
        btnPdf.addActionListener(e -> new Metodos.ImprimirCotizacionPDF().imprimir(idCotiz));
        JButton btnEnviada = EstiloCompras.secondaryBtn("Marcar enviada", FontAwesome.CHECK);
        btnEnviada.addActionListener(e -> cambiarEstado(1));
        JButton btnSin = EstiloCompras.secondaryBtn("Sin respuesta", FontAwesome.CLOSE);
        btnSin.addActionListener(e -> cambiarEstado(3));
        JButton btnResp = EstiloCompras.successBtn("Guardar respuesta", FontAwesome.SAVE);
        btnResp.addActionListener(e -> guardarRespuesta());

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.add(btnExcel);
        left.add(Box.createHorizontalStrut(8));
        left.add(btnPdf);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(btnEnviada);
        right.add(Box.createHorizontalStrut(8));
        right.add(btnSin);
        right.add(Box.createHorizontalStrut(8));
        right.add(btnResp);

        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void cargar() {
        ResultSet rs = DBcotizaciones.cargarCabecera(idCotiz);
        try {
            if (rs.next()) {
                lbl_info.setText("RFQ " + rs.getString("numero") + "  •  Proveedor: "
                        + rs.getString("proveedor") + "  •  Cel: " + rs.getString("celular")
                        + "  •  Estado: " + DBcotizaciones.nombreEstado(rs.getInt("estado")));
                txt_condicion.setText(nz(rs.getString("condicion_pago")));
                txt_validez.setText(nz(rs.getString("validez")));
                java.sql.Date fl = rs.getDate("fecha_limite");
                txt_limite.setText(fl == null ? "" : fl.toString());
                txt_obs.setText(nz(rs.getString("observacion")));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        modelo.setRowCount(0);
        rs = DBcotizaciones.cargarDetalles(idCotiz);
        try {
            while (rs.next()) {
                Object precio = rs.getObject("precio_unitario") == null ? ""
                        : metodos.formateador_decimal().format(rs.getDouble("precio_unitario"));
                Object iva = rs.getObject("iva_pct") == null ? "" : rs.getDouble("iva_pct");
                modelo.addRow(new Object[]{
                    rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                    metodos.formateador_decimal().format(rs.getDouble("cantidad")),
                    precio, iva, rs.getString("plazo_entrega")});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private void guardarRespuesta() {
        if (jtabla.isEditing()) {
            jtabla.getCellEditor().stopCellEditing();
        }
        boolean ok = new DBcotizaciones().guardarRespuesta(idCotiz, txt_condicion.getText(),
                txt_validez.getText(), txt_limite.getText().trim(), txt_obs.getText(),
                modelo, C_ID, C_PRECIO, C_IVA, C_PLAZO);
        if (ok) {
            new DBcotizaciones().actualizarEstado(idCotiz, 2); // respondida
            JOptionPane.showMessageDialog(this, "Cotización guardada (marcada como Respondida).");
            dispose();
        }
    }

    private void cambiarEstado(int estado) {
        if (new DBcotizaciones().actualizarEstado(idCotiz, estado) > 0) {
            JOptionPane.showMessageDialog(this, "Estado actualizado: " + DBcotizaciones.nombreEstado(estado));
            dispose();
        }
    }
}
