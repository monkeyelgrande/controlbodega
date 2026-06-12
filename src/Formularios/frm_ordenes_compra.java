/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios;

import Formularios_internos.jd_analisis_orden_compra;
import Formularios_internos.jd_ver_orden_compra;
import Formularios_internos.jif_crear_orden_compra;
import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DBordenes_compra;
import java.awt.BorderLayout;
import java.awt.Color;
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
import modelos.Ordenes_compra_cabecera;

/**
 * Listado maestro de órdenes de compra. Permite crear, ver, editar (solo
 * borradores) y, para usuarios con permiso, analizar/aprobar las pendientes.
 *
 * @author Monkeyelgrande
 */
public class frm_ordenes_compra extends javax.swing.JInternalFrame {

    private JComboBox<String> jbox_estado;
    private JTextField txt_Filtro;
    private JTable jtabla;
    private JButton btn_nueva;
    private JButton btn_ver;
    private JButton btn_editar;
    private JButton btn_analizar;
    private JButton btn_actualizar;

    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public frm_ordenes_compra() {
        initUI();
        actualizar();
        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    verOrden();
                }
            }
        });
        boolean puedeAprobar = frm_main.aprueba_compras || frm_main.perfil == 1;
        btn_analizar.setEnabled(puedeAprobar);

        // Crear/editar gobernados por el sistema de permisos (con la BD sin
        // migrar se conserva el comportamiento anterior: ambos disponibles).
        if (Metodos.Permisos.estaCargado()) {
            btn_nueva.setVisible(Metodos.Permisos.puede("compras_crear"));
            btn_editar.setVisible(Metodos.Permisos.puede("compras_editar"));
        }
    }

    private void initUI() {
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Órdenes de compra");
        setSize(1060, 600);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);

        // ---- Header con gradiente ----
        root.add(EstiloCompras.header(FontAwesome.FILE_INVOICE, "Órdenes de compra", null),
                BorderLayout.NORTH);

        // ---- Cuerpo: toolbar + tabla ----
        JPanel cuerpo = new JPanel(new BorderLayout(0, 10));
        cuerpo.setBackground(EstiloCompras.BG_FORM);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));

        cuerpo.add(buildToolbar(), BorderLayout.NORTH);

        modelo.setColumnIdentifiers(new Object[]{
            "id", "N°", "Creada por", "Fecha", "Estado", "Bodega", "Items"});
        jtabla = new JTable(modelo);
        EstiloCompras.styleTable(jtabla);
        EstiloCompras.anchoColumnas(jtabla, 50, 90, 220, 110, 120, 170, 70);
        EstiloCompras.aplicarEstadoRenderer(jtabla, 4); // columna "Estado"
        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        cuerpo.add(EstiloCompras.scroll(jtabla), BorderLayout.CENTER);

        root.add(cuerpo, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(EstiloCompras.BG_FORM);

        // Filtros (izquierda)
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        JLabel lblEstado = new JLabel("Estado:");
        lblEstado.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblEstado.setForeground(EstiloCompras.TEXT_SECONDARY);
        jbox_estado = new JComboBox<>(new String[]{
            "Todas", "Borrador", "Pendiente", "Aprobada", "Rechazada"});
        EstiloCompras.styleCombo(jbox_estado);
        jbox_estado.setPreferredSize(new Dimension(150, 36));
        jbox_estado.addActionListener(e -> actualizar());

        txt_Filtro = EstiloCompras.field("Buscar...", FontAwesome.SEARCH);
        txt_Filtro.setPreferredSize(new Dimension(280, 36));

        filtros.add(lblEstado);
        filtros.add(jbox_estado);
        filtros.add(Box.createHorizontalStrut(6));
        filtros.add(txt_Filtro);

        // Acciones (derecha)
        JPanel acciones = new JPanel();
        acciones.setOpaque(false);
        acciones.setLayout(new BoxLayout(acciones, BoxLayout.X_AXIS));
        btn_nueva = EstiloCompras.primaryBtn("Nueva", FontAwesome.PLUS);
        btn_ver = EstiloCompras.secondaryBtn("Ver", FontAwesome.EYE);
        btn_editar = EstiloCompras.secondaryBtn("Editar", FontAwesome.EDIT);
        btn_analizar = EstiloCompras.successBtn("Analizar / Aprobar", FontAwesome.CHECK);
        btn_actualizar = EstiloCompras.secondaryBtn("Actualizar", FontAwesome.SYNC);
        btn_nueva.addActionListener(e -> nuevaOrden());
        btn_ver.addActionListener(e -> verOrden());
        btn_editar.addActionListener(e -> editarOrden());
        btn_analizar.addActionListener(e -> analizarOrden());
        btn_actualizar.addActionListener(e -> actualizar());
        acciones.add(btn_nueva);
        acciones.add(Box.createHorizontalStrut(8));
        acciones.add(btn_ver);
        acciones.add(Box.createHorizontalStrut(8));
        acciones.add(btn_editar);
        acciones.add(Box.createHorizontalStrut(8));
        acciones.add(btn_analizar);
        acciones.add(Box.createHorizontalStrut(8));
        acciones.add(btn_actualizar);

        bar.add(filtros, BorderLayout.WEST);
        bar.add(acciones, BorderLayout.EAST);
        return bar;
    }

    /** -1 = todas; de lo contrario el estado seleccionado. */
    private int estadoFiltro() {
        switch (jbox_estado.getSelectedIndex()) {
            case 1:
                return Ordenes_compra_cabecera.ESTADO_BORRADOR;
            case 2:
                return Ordenes_compra_cabecera.ESTADO_PENDIENTE;
            case 3:
                return Ordenes_compra_cabecera.ESTADO_APROBADA;
            case 4:
                return Ordenes_compra_cabecera.ESTADO_RECHAZADA;
            default:
                return -1;
        }
    }

    public final void actualizar() {
        modelo.setRowCount(0);
        ResultSet rs = DBordenes_compra.listar(estadoFiltro());
        try {
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getString("id"), rs.getString("numero"), rs.getString("creador"),
                    rs.getDate("fecha"),
                    Ordenes_compra_cabecera.nombreEstado(rs.getInt("estado")),
                    rs.getString("bodega"), rs.getString("items")});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /** Id de la orden seleccionada, o -1 si no hay selección. */
    private int idSeleccionado() {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una orden");
            return -1;
        }
        return Integer.parseInt(jtabla.getValueAt(fila, 0).toString());
    }

    private String estadoSeleccionado() {
        int fila = jtabla.getSelectedRow();
        return String.valueOf(jtabla.getValueAt(fila, 4));
    }

    private void nuevaOrden() {
        jif_crear_orden_compra f = new jif_crear_orden_compra();
        f.setVisible(true);
        actualizar();
    }

    private void verOrden() {
        int id = idSeleccionado();
        if (id < 0) {
            return;
        }
        String estado = estadoSeleccionado();
        // Aprobadas/Rechazadas: vista de resultado con proveedor, precios y PDF.
        if ("Aprobada".equals(estado) || "Rechazada".equals(estado)) {
            jd_ver_orden_compra f = new jd_ver_orden_compra(id);
            f.setVisible(true);
            return;
        }
        // Borrador/Pendiente: vista de solo lectura de productos y cantidades.
        jif_crear_orden_compra f = new jif_crear_orden_compra();
        f.cargarOrden(id, true);
        f.setVisible(true);
    }

    private void editarOrden() {
        int id = idSeleccionado();
        if (id < 0) {
            return;
        }
        if (!"Borrador".equals(estadoSeleccionado())) {
            JOptionPane.showMessageDialog(this,
                    "Solo se pueden editar órdenes en estado Borrador.");
            return;
        }
        jif_crear_orden_compra f = new jif_crear_orden_compra();
        f.cargarOrden(id, false);
        f.setVisible(true);
        actualizar();
    }

    private void analizarOrden() {
        int id = idSeleccionado();
        if (id < 0) {
            return;
        }
        String estado = estadoSeleccionado();
        if (!"Pendiente".equals(estado)) {
            JOptionPane.showMessageDialog(this,
                    "Solo se pueden analizar/aprobar órdenes en estado Pendiente.\n"
                    + "Estado actual: " + estado);
            return;
        }
        jd_analisis_orden_compra f = new jd_analisis_orden_compra(id);
        f.setVisible(true);
        actualizar();
    }
}
