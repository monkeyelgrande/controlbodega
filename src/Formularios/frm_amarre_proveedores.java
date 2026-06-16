/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios;

import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBproducto_proveedores;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.ResultSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modelos.Contactos;

/**
 * Administración del amarre producto ↔ proveedor (RF-02). Permite buscar un
 * producto y asociarle uno o varios proveedores; los datos de contacto salen de
 * la ficha del proveedor.
 *
 * @author Monkeyelgrande
 */
public class frm_amarre_proveedores extends javax.swing.JInternalFrame {

    private JTextField txt_Filtro;
    private JTable jtabla_prod;
    private JTable jtabla_prov;
    private JComboBox<Contactos> jbox_prov;
    private JLabel lbl_prod;
    private DefaultTableModel modeloProd;
    private DefaultTableModel modeloProv;
    private int idProductoSel = -1;

    public frm_amarre_proveedores() {
        initUI();
        cargarProductos();
        Contactos c = new Contactos();
        c.MostrarNombreProveedores(jbox_prov);
    }

    private void initUI() {
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Proveedores por producto");
        setSize(1040, 580);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.add(EstiloCompras.header(FontAwesome.WAREHOUSE, "Amarre de proveedores por producto", null),
                BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout());
        cuerpo.setBackground(EstiloCompras.BG_FORM);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        // ---- Productos ----
        modeloProd = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int f, int c) {
                return false;
            }
        };
        modeloProd.setColumnIdentifiers(new Object[]{"ID", "CÓDIGO", "DESCRIPCIÓN"});
        jtabla_prod = new JTable(modeloProd);
        EstiloCompras.styleTable(jtabla_prod);
        EstiloCompras.anchoColumnas(jtabla_prod, 45, 110, 480);
        jtabla_prod.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onProductoSeleccionado();
            }
        });
        txt_Filtro = EstiloCompras.field("Buscar producto...", FontAwesome.SEARCH);
        metodos.BuscarEnTabla(txt_Filtro, jtabla_prod);

        JPanel izq = new JPanel(new BorderLayout(0, 6));
        izq.setOpaque(false);
        izq.add(txt_Filtro, BorderLayout.NORTH);
        izq.add(EstiloCompras.scroll(jtabla_prod), BorderLayout.CENTER);

        // ---- Proveedores del producto ----
        modeloProv = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int f, int c) {
                return false;
            }
        };
        modeloProv.setColumnIdentifiers(new Object[]{"ID_AMARRE", "PROVEEDOR", "CELULAR", "TELÉFONO"});
        jtabla_prov = new JTable(modeloProv);
        EstiloCompras.styleTable(jtabla_prov);
        EstiloCompras.ocultarColumna(jtabla_prov, 0);
        EstiloCompras.anchoColumnas(jtabla_prov, 0, 260, 130, 130);

        lbl_prod = new JLabel("Seleccione un producto");
        lbl_prod.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl_prod.setForeground(EstiloCompras.TEXT_PRIMARY);

        jbox_prov = new JComboBox<>();
        EstiloCompras.styleCombo(jbox_prov);
        jbox_prov.setPreferredSize(new Dimension(240, 34));
        JButton btnAgregar = EstiloCompras.primaryBtn("Asociar", FontAwesome.PLUS);
        btnAgregar.addActionListener(e -> agregarProveedor());
        JButton btnQuitar = EstiloCompras.secondaryBtn("Quitar", FontAwesome.TRASH);
        btnQuitar.addActionListener(e -> quitarProveedor());

        JPanel barra = new JPanel();
        barra.setOpaque(false);
        barra.setLayout(new BoxLayout(barra, BoxLayout.X_AXIS));
        barra.add(jbox_prov);
        barra.add(Box.createHorizontalStrut(8));
        barra.add(btnAgregar);
        barra.add(Box.createHorizontalStrut(8));
        barra.add(btnQuitar);

        JPanel der = new JPanel(new BorderLayout(0, 6));
        der.setOpaque(false);
        JPanel derTop = new JPanel(new BorderLayout(0, 6));
        derTop.setOpaque(false);
        derTop.add(lbl_prod, BorderLayout.NORTH);
        derTop.add(barra, BorderLayout.SOUTH);
        der.add(derTop, BorderLayout.NORTH);
        der.add(EstiloCompras.scroll(jtabla_prov), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izq, der);
        split.setBorder(null);
        split.setResizeWeight(0.55);
        split.setDividerLocation(560);
        cuerpo.add(split, BorderLayout.CENTER);

        root.add(cuerpo, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void cargarProductos() {
        modeloProd.setRowCount(0);
        ResultSet rs = DB_consultas_R_D.getTabla(
                "SELECT id, codigo_barras, descripcion FROM productos "
                + "WHERE COALESCE(estado,true)=true ORDER BY descripcion");
        try {
            while (rs.next()) {
                modeloProd.addRow(new Object[]{
                    rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion")});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void onProductoSeleccionado() {
        int fila = jtabla_prod.getSelectedRow();
        if (fila < 0) {
            return;
        }
        idProductoSel = Integer.parseInt(jtabla_prod.getValueAt(fila, 0).toString());
        lbl_prod.setText("Proveedores de: " + jtabla_prod.getValueAt(fila, 2));
        cargarProveedores();
    }

    private void cargarProveedores() {
        modeloProv.setRowCount(0);
        if (idProductoSel < 0) {
            return;
        }
        ResultSet rs = DBproducto_proveedores.proveedoresDeProducto(idProductoSel);
        try {
            while (rs.next()) {
                modeloProv.addRow(new Object[]{
                    rs.getString("id"), rs.getString("nombre"),
                    rs.getString("celular"), rs.getString("telefono")});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void agregarProveedor() {
        if (idProductoSel < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione primero un producto");
            return;
        }
        Contactos prov = (Contactos) jbox_prov.getSelectedItem();
        if (prov == null) {
            return;
        }
        if (DBproducto_proveedores.agregar(idProductoSel, prov.getId())) {
            cargarProveedores();
        }
    }

    private void quitarProveedor() {
        int fila = jtabla_prov.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un proveedor de la lista");
            return;
        }
        int idAmarre = Integer.parseInt(jtabla_prov.getValueAt(fila, 0).toString());
        if (DBproducto_proveedores.quitar(idAmarre)) {
            cargarProveedores();
        }
    }
}
