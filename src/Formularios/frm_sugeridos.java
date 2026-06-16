/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios;

import Formularios_internos.jd_seleccion_sugerido;
import Formularios_internos.jif_crear_sugerido;
import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DBsugeridos;
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
import modelos.Sugerido_cabecera;

/**
 * Listado de sugeridos de pedidos (RF-01). Crear, ver, editar (abiertos),
 * bloquear y pasar a selección/almacén (bloqueados).
 *
 * @author Monkeyelgrande
 */
public class frm_sugeridos extends javax.swing.JInternalFrame {

    private JComboBox<String> jbox_estado;
    private JTextField txt_Filtro;
    private JTable jtabla;
    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int f, int c) {
            return false;
        }
    };

    public frm_sugeridos() {
        initUI();
        actualizar();
        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    ver();
                }
            }
        });
    }

    private void initUI() {
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Sugeridos de pedido");
        setSize(1040, 580);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.add(EstiloCompras.header(FontAwesome.CLIPBOARD, "Sugeridos de pedido", null), BorderLayout.NORTH);

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
        jbox_estado = new JComboBox<>(new String[]{"Todos", "Abierto", "Bloqueado", "Procesado"});
        EstiloCompras.styleCombo(jbox_estado);
        jbox_estado.setPreferredSize(new Dimension(150, 36));
        jbox_estado.addActionListener(e -> actualizar());
        txt_Filtro = EstiloCompras.field("Buscar...", FontAwesome.SEARCH);
        txt_Filtro.setPreferredSize(new Dimension(260, 36));
        filtros.add(l);
        filtros.add(jbox_estado);
        filtros.add(Box.createHorizontalStrut(6));
        filtros.add(txt_Filtro);

        JPanel acciones = new JPanel();
        acciones.setOpaque(false);
        acciones.setLayout(new BoxLayout(acciones, BoxLayout.X_AXIS));
        JButton btnNuevo = EstiloCompras.primaryBtn("Nuevo", FontAwesome.PLUS);
        JButton btnVer = EstiloCompras.secondaryBtn("Ver", FontAwesome.EYE);
        JButton btnEditar = EstiloCompras.secondaryBtn("Editar", FontAwesome.EDIT);
        JButton btnSel = EstiloCompras.successBtn("Selección almacén", FontAwesome.CHECK);
        JButton btnAct = EstiloCompras.secondaryBtn("Actualizar", FontAwesome.SYNC);
        btnNuevo.addActionListener(e -> nuevo());
        btnVer.addActionListener(e -> ver());
        btnEditar.addActionListener(e -> editar());
        btnSel.addActionListener(e -> seleccion());
        btnAct.addActionListener(e -> actualizar());
        acciones.add(btnNuevo);
        acciones.add(Box.createHorizontalStrut(8));
        acciones.add(btnVer);
        acciones.add(Box.createHorizontalStrut(8));
        acciones.add(btnEditar);
        acciones.add(Box.createHorizontalStrut(8));
        acciones.add(btnSel);
        acciones.add(Box.createHorizontalStrut(8));
        acciones.add(btnAct);

        bar.add(filtros, BorderLayout.WEST);
        bar.add(acciones, BorderLayout.EAST);
        cuerpo.add(bar, BorderLayout.NORTH);

        modelo.setColumnIdentifiers(new Object[]{"id", "N°", "Creado por", "Fecha", "Estado", "Bodega", "Items"});
        jtabla = new JTable(modelo);
        EstiloCompras.styleTable(jtabla);
        EstiloCompras.anchoColumnas(jtabla, 50, 90, 220, 110, 120, 170, 70);
        EstiloCompras.aplicarEstadoRenderer(jtabla, 4);
        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        cuerpo.add(EstiloCompras.scroll(jtabla), BorderLayout.CENTER);

        root.add(cuerpo, BorderLayout.CENTER);
        setContentPane(root);
    }

    private int estadoFiltro() {
        switch (jbox_estado.getSelectedIndex()) {
            case 1:
                return Sugerido_cabecera.ESTADO_ABIERTO;
            case 2:
                return Sugerido_cabecera.ESTADO_BLOQUEADO;
            case 3:
                return Sugerido_cabecera.ESTADO_PROCESADO;
            default:
                return -1;
        }
    }

    public final void actualizar() {
        modelo.setRowCount(0);
        ResultSet rs = DBsugeridos.listar(estadoFiltro());
        try {
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getString("id"), rs.getString("numero"), rs.getString("creador"),
                    rs.getDate("fecha"), Sugerido_cabecera.nombreEstado(rs.getInt("estado")),
                    rs.getString("bodega"), rs.getString("items")});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private int idSel() {
        int f = jtabla.getSelectedRow();
        if (f < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un sugerido");
            return -1;
        }
        return Integer.parseInt(jtabla.getValueAt(f, 0).toString());
    }

    private String estadoSel() {
        return String.valueOf(jtabla.getValueAt(jtabla.getSelectedRow(), 4));
    }

    private void nuevo() {
        new jif_crear_sugerido().setVisible(true);
        actualizar();
    }

    private void ver() {
        int id = idSel();
        if (id < 0) {
            return;
        }
        jif_crear_sugerido f = new jif_crear_sugerido();
        f.cargarSugerido(id, true);
        f.setVisible(true);
    }

    private void editar() {
        int id = idSel();
        if (id < 0) {
            return;
        }
        if (!"Abierto".equals(estadoSel())) {
            JOptionPane.showMessageDialog(this, "Solo se pueden editar sugeridos en estado Abierto.");
            return;
        }
        jif_crear_sugerido f = new jif_crear_sugerido();
        f.cargarSugerido(id, false);
        f.setVisible(true);
        actualizar();
    }

    private void seleccion() {
        int id = idSel();
        if (id < 0) {
            return;
        }
        if ("Abierto".equals(estadoSel())) {
            JOptionPane.showMessageDialog(this,
                    "El sugerido debe estar Bloqueado para pasar a selección en almacén.");
            return;
        }
        new jd_seleccion_sugerido(id).setVisible(true);
        actualizar();
    }
}
