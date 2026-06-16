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
import conexiondb.DBcomparativos;
import conexiondb.DBcotizaciones;
import conexiondb.DBproducto_proveedores;
import conexiondb.DBsugeridos;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
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
import javax.swing.table.DefaultTableModel;
import modelos.Contactos;
import modelos.Sugerido_detalle;

/**
 * Revisión y selección en almacén (RF-02): el equipo marca qué productos se
 * piden realmente, ajusta la cantidad final, ve los proveedores amarrados a
 * cada producto y genera la solicitud de cotización o el comparativo.
 *
 * @author Monkeyelgrande
 */
public class jd_seleccion_sugerido extends JDialog {

    private final int idSugerido;
    private JLabel lbl_info;
    private JTable jtabla;
    private DefaultTableModel modelo;

    private static final int C_IDDET = 0;
    private static final int C_IDPROD = 1;
    private static final int C_COD = 2;
    private static final int C_DESC = 3;
    private static final int C_EXIST = 4;
    private static final int C_ROT = 5;
    private static final int C_SUG = 6;
    private static final int C_PEDIR = 7;
    private static final int C_FINAL = 8;
    private static final int C_PROV = 9;

    public jd_seleccion_sugerido(int idSugerido) {
        this.idSugerido = idSugerido;
        initUI();
        cargar();
        setLocationRelativeTo(null);
        metodos.addEscapeListenerWindowDialog(this);
    }

    private void initUI() {
        setModal(true);
        setUndecorated(true);
        setSize(1240, 700);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));
        root.add(EstiloCompras.header(FontAwesome.CHECK, "Selección en almacén", () -> dispose()),
                BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setBackground(EstiloCompras.BG_FORM);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 20, 16, 20));

        lbl_info = new JLabel("...");
        lbl_info.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl_info.setForeground(EstiloCompras.TEXT_PRIMARY);
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(0xFFF8E1));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFFE082), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        card.add(lbl_info, BorderLayout.CENTER);
        cuerpo.add(card, BorderLayout.NORTH);

        modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int f, int c) {
                return c == C_PEDIR || c == C_FINAL;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c == C_PEDIR ? Boolean.class : String.class;
            }
        };
        modelo.setColumnIdentifiers(new Object[]{
            "ID_DET", "ID_PROD", "CÓDIGO", "DESCRIPCIÓN", "EXIST.", "ROT./MES",
            "SUGERIDA", "PEDIR", "CANT. FINAL", "PROVEEDORES"});
        jtabla = new JTable(modelo);
        EstiloCompras.styleTable(jtabla);
        EstiloCompras.ocultarColumna(jtabla, C_IDDET);
        EstiloCompras.ocultarColumna(jtabla, C_IDPROD);
        EstiloCompras.anchoColumnas(jtabla, 0, 0, 90, 300, 70, 70, 80, 55, 90, 260);
        cuerpo.add(EstiloCompras.scroll(jtabla), BorderLayout.CENTER);

        root.add(cuerpo, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(EstiloCompras.BG_SECTION);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCompras.DIVIDER),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JButton btnGuardar = EstiloCompras.secondaryBtn("Guardar selección", FontAwesome.SAVE);
        btnGuardar.addActionListener(e -> {
            if (guardarSeleccion()) {
                JOptionPane.showMessageDialog(this, "Selección guardada.");
            }
        });
        JButton btnRFQ = EstiloCompras.secondaryBtn("Generar solicitud de cotización", FontAwesome.FILE_INVOICE);
        btnRFQ.addActionListener(e -> generarRFQ());
        JButton btnComp = EstiloCompras.primaryBtn("Generar comparativo", FontAwesome.LIST);
        btnComp.addActionListener(e -> generarComparativo());

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(btnGuardar);
        right.add(Box.createHorizontalStrut(8));
        right.add(btnRFQ);
        right.add(Box.createHorizontalStrut(8));
        right.add(btnComp);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void cargar() {
        ResultSet rs = DBsugeridos.cargarCabecera(idSugerido);
        try {
            if (rs.next()) {
                lbl_info.setText("<html><b>Sugerido " + rs.getString("numero") + "</b>"
                        + " &nbsp;•&nbsp; Creado por: " + rs.getString("creador")
                        + " &nbsp;•&nbsp; Bodega: " + rs.getString("bodega")
                        + " &nbsp;•&nbsp; Marque qué se pide y ajuste la cantidad final.</html>");
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        modelo.setRowCount(0);
        rs = DBsugeridos.cargarDetalles(idSugerido);
        try {
            while (rs.next()) {
                int idProd = rs.getInt("id_producto");
                double cantFinal = rs.getObject("cantidad_final") == null
                        ? rs.getDouble("cantidad_sugerida") : rs.getDouble("cantidad_final");
                modelo.addRow(new Object[]{
                    rs.getString("id"), String.valueOf(idProd),
                    rs.getString("codigo_barras"), rs.getString("descripcion"),
                    fmt(rs.getDouble("existencia")), fmt(rs.getDouble("rotacion_mensual")),
                    fmt(rs.getDouble("cantidad_sugerida")), rs.getBoolean("seleccionado"),
                    fmt(cantFinal), DBproducto_proveedores.resumenProveedores(idProd)});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private String fmt(double v) {
        return metodos.formateador_decimal().format(v);
    }

    private double num(Object o) {
        try {
            return Double.parseDouble(o.toString().replace(".", "").replace(",", "."));
        } catch (Exception e) {
            try {
                return Double.parseDouble(o.toString());
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    private boolean guardarSeleccion() {
        if (jtabla.isEditing()) {
            jtabla.getCellEditor().stopCellEditing();
        }
        List<Sugerido_detalle> dets = new ArrayList<>();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            Sugerido_detalle d = new Sugerido_detalle();
            d.setId(Integer.parseInt(modelo.getValueAt(i, C_IDDET).toString()));
            Object sel = modelo.getValueAt(i, C_PEDIR);
            d.setSeleccionado(sel instanceof Boolean ? (Boolean) sel : Boolean.parseBoolean(String.valueOf(sel)));
            d.setCantidad_final(num(modelo.getValueAt(i, C_FINAL)));
            dets.add(d);
        }
        return new DBsugeridos().GuardarSeleccion(dets);
    }

    private boolean haySeleccionados() {
        for (int i = 0; i < modelo.getRowCount(); i++) {
            Object sel = modelo.getValueAt(i, C_PEDIR);
            if (sel instanceof Boolean && (Boolean) sel) {
                return true;
            }
        }
        return false;
    }

    private void generarComparativo() {
        if (!guardarSeleccion()) {
            return;
        }
        if (!haySeleccionados()) {
            JOptionPane.showMessageDialog(this, "Marque al menos un producto para pedir.");
            return;
        }
        int idComp = new DBcomparativos().crearDesdeSugerido(idSugerido, frm_main.id_user);
        if (idComp > 0) {
            JOptionPane.showMessageDialog(this, "Comparativo generado. Agregue proveedores y precios.");
            dispose();
            new jd_comparativo(idComp).setVisible(true);
        }
    }

    private void generarRFQ() {
        if (!guardarSeleccion()) {
            return;
        }
        if (!haySeleccionados()) {
            JOptionPane.showMessageDialog(this, "Marque al menos un producto para pedir.");
            return;
        }
        Contactos prov = elegirProveedor();
        if (prov == null) {
            return;
        }
        int idCot = new DBcotizaciones().crearDesdeSugerido(idSugerido, prov.getId(), frm_main.id_user);
        if (idCot > 0) {
            new jd_cotizacion(idCot).setVisible(true);
        }
    }

    private Contactos elegirProveedor() {
        List<Contactos> lista = new ArrayList<>();
        ResultSet rs = DB_consultas_R_D.getTabla(
                "SELECT id, nombre FROM contactos WHERE proveedor=1 ORDER BY nombre");
        try {
            while (rs.next()) {
                lista.add(new Contactos(rs.getInt("id"), rs.getString("nombre")));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        if (lista.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay proveedores registrados.");
            return null;
        }
        return (Contactos) JOptionPane.showInputDialog(this,
                "Proveedor destinatario de la cotización:", "Solicitud de cotización",
                JOptionPane.QUESTION_MESSAGE, null, lista.toArray(), lista.get(0));
    }
}
