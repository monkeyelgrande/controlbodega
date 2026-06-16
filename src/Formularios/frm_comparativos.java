/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios;

import Formularios_internos.jd_comparativo;
import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DBcomparativos;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Listado de comparativos de cotizaciones (RF-04).
 *
 * @author Monkeyelgrande
 */
public class frm_comparativos extends javax.swing.JInternalFrame {

    private JComboBox<String> jbox_estado;
    private JTextField txt_Filtro;
    private JTable jtabla;
    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int f, int c) {
            return false;
        }
    };

    public frm_comparativos() {
        initUI();
        actualizar();
        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    abrir();
                }
            }
        });
    }

    private void initUI() {
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Comparativos de cotizaciones");
        setSize(980, 560);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.add(EstiloCompras.header(FontAwesome.FILE_INVOICE, "Comparativos de cotizaciones", null), BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 10));
        cuerpo.setBackground(EstiloCompras.BG_FORM);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));

        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        JLabel l = new JLabel("Estado:");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(EstiloCompras.TEXT_SECONDARY);
        jbox_estado = new JComboBox<>(new String[]{"Todos", "Abierto", "Decidido", "Autorizado"});
        EstiloCompras.styleCombo(jbox_estado);
        jbox_estado.setPreferredSize(new Dimension(160, 36));
        jbox_estado.addActionListener(e -> actualizar());
        txt_Filtro = EstiloCompras.field("Buscar...", FontAwesome.SEARCH);
        txt_Filtro.setPreferredSize(new Dimension(260, 36));
        filtros.add(l);
        filtros.add(jbox_estado);
        filtros.add(Box.createHorizontalStrut(6));
        filtros.add(txt_Filtro);

        JPanel acc = new JPanel();
        acc.setOpaque(false);
        acc.setLayout(new BoxLayout(acc, BoxLayout.X_AXIS));
        JButton btnAbrir = EstiloCompras.primaryBtn("Abrir", FontAwesome.EYE);
        JButton btnAct = EstiloCompras.secondaryBtn("Actualizar", FontAwesome.SYNC);
        btnAbrir.addActionListener(e -> abrir());
        btnAct.addActionListener(e -> actualizar());
        acc.add(btnAbrir);
        acc.add(Box.createHorizontalStrut(8));
        acc.add(btnAct);

        bar.add(filtros, BorderLayout.WEST);
        bar.add(acc, BorderLayout.EAST);
        cuerpo.add(bar, BorderLayout.NORTH);

        modelo.setColumnIdentifiers(new Object[]{"id", "N°", "Creado por", "Fecha", "Estado", "Items", "Provs"});
        jtabla = new JTable(modelo);
        EstiloCompras.styleTable(jtabla);
        EstiloCompras.anchoColumnas(jtabla, 50, 90, 240, 110, 130, 70, 70);
        EstiloCompras.aplicarEstadoRenderer(jtabla, 4);
        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        cuerpo.add(EstiloCompras.scroll(jtabla), BorderLayout.CENTER);

        root.add(cuerpo, BorderLayout.CENTER);
        setContentPane(root);
    }

    private int estadoFiltro() {
        int i = jbox_estado.getSelectedIndex();
        return i <= 0 ? -1 : i - 1; // 0 Abierto,1 Decidido,2 Autorizado
    }

    private String nombreEstado(int e) {
        switch (e) {
            case 0:
                return "Abierto";
            case 1:
                return "Decidido";
            case 2:
                return "Autorizado";
            default:
                return "?";
        }
    }

    public final void actualizar() {
        modelo.setRowCount(0);
        ResultSet rs = DBcomparativos.listar(estadoFiltro());
        try {
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getString("id"), rs.getString("numero"), rs.getString("creador"),
                    rs.getDate("fecha"), nombreEstado(rs.getInt("estado")),
                    rs.getString("items"), rs.getString("provs")});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void abrir() {
        int f = jtabla.getSelectedRow();
        if (f < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un comparativo");
            return;
        }
        int id = Integer.parseInt(jtabla.getValueAt(f, 0).toString());
        new jd_comparativo(id).setVisible(true);
        actualizar();
    }
}
