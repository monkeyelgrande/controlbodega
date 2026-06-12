package Precios;

import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Descuentos escalonados por utilidad (modulo Precios, portado del
 * frm_Descuentos de productos-agroinsumos).
 *
 * @author Monkeyelgrande
 */
public class frm_descuentos_precios extends javax.swing.JInternalFrame {

    private JTable jtabla;
    private JTextField txt_Filtro;

    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public frm_descuentos_precios() {
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Descuentos escalonados (Precios)");
        construir();
        mostrar();
        metodos.BuscarEnTabla(txt_Filtro, jtabla);
    }

    private void construir() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.setBackground(java.awt.Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new java.awt.Color(46, 125, 50));
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel titulo = new JLabel("Descuentos escalonados");
        titulo.setFont(new java.awt.Font("Tahoma", 1, 22));
        titulo.setForeground(java.awt.Color.WHITE);
        header.add(titulo, BorderLayout.WEST);

        JPanel barra = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4));
        barra.setOpaque(false);
        txt_Filtro = new JTextField(25);
        txt_Filtro.setFont(new java.awt.Font("Tahoma", 0, 16));

        JButton btnNuevo = new JButton("Nuevo");
        btnNuevo.setFont(new java.awt.Font("Tahoma", 1, 14));
        btnNuevo.setBackground(new java.awt.Color(46, 125, 50));
        btnNuevo.setForeground(java.awt.Color.WHITE);
        btnNuevo.addActionListener(e -> {
            jd_crear_descuento d = new jd_crear_descuento(null, true, 0);
            d.setVisible(true);
            mostrar();
        });

        JButton btnEditar = new JButton("Editar");
        btnEditar.setFont(new java.awt.Font("Tahoma", 1, 14));
        btnEditar.addActionListener(e -> editarSeleccionado());

        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.setFont(new java.awt.Font("Tahoma", 1, 14));
        btnEliminar.addActionListener(e -> eliminarSeleccionado());

        barra.add(txt_Filtro);
        barra.add(btnNuevo);
        barra.add(btnEditar);
        barra.add(btnEliminar);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(header, BorderLayout.NORTH);
        norte.add(barra, BorderLayout.SOUTH);

        jtabla = new JTable(modelo);
        jtabla.setRowHeight(28);
        jtabla.setFont(new java.awt.Font("Tahoma", 0, 14));
        jtabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    editarSeleccionado();
                }
            }
        });

        root.add(norte, BorderLayout.NORTH);
        root.add(new JScrollPane(jtabla), BorderLayout.CENTER);
        setContentPane(root);
        setSize(700, 500);
    }

    public void mostrar() {
        modelo.setRowCount(0);
        modelo.setColumnIdentifiers(new Object[]{"id", "Tipo", "Utilidad (%)", "Descuento (%)"});
        ResultSet rs = DB_consultas_R_D.getTabla("select * from descuentos order by tipo, utilidad");
        try {
            while (rs.next()) {
                modelo.addRow(new Object[]{rs.getString("id"), rs.getString("tipo"),
                    rs.getDouble("utilidad"), rs.getDouble("descuento")});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void editarSeleccionado() {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
            return;
        }
        int id = Integer.parseInt(modelo.getValueAt(fila, 0).toString());
        jd_crear_descuento d = new jd_crear_descuento(null, true, id);
        d.setVisible(true);
        mostrar();
    }

    private void eliminarSeleccionado() {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
            return;
        }
        if (!DB_consultas_R_D.validar_admin()) {
            return;
        }
        String id = modelo.getValueAt(fila, 0).toString();
        int r = JOptionPane.showConfirmDialog(this, "¿Eliminar el descuento #" + id + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION && DB_consultas_R_D.eliminar("descuentos", id)) {
            mostrar();
        }
    }
}
