package Formularios;

import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBajustes_inventario;

import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * Listado de ajustes de inventario con opciones de crear, ver y eliminar.
 *
 * @author M-Work
 */
public class frm_ajuste_inventario extends javax.swing.JInternalFrame {

    DecimalFormat formatDecimal = new DecimalFormat("###,###.##");
    TableColumnModel columnModel = null;

    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    // Componentes (creados programaticamente)
    private JTextField txt_Filtro;
    private JTable jtabla;
    private JButton btn_crear, btn_eliminar, btn_ver, btn_actualizar;

    public frm_ajuste_inventario() {
        initUI();
        actualizar();
        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        metodos.EstiloTablaMaterialGlobal(jtabla);
    }

    private void initUI() {
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Ajustes de Inventario");

        // Panel superior (titulo)
        JPanel panelTitulo = new JPanel();
        panelTitulo.setBackground(new java.awt.Color(0, 116, 214));
        panelTitulo.setPreferredSize(new java.awt.Dimension(0, 53));
        panelTitulo.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 20, 12));

        JLabel lblTitulo = new JLabel("Ajustes de Inventario");
        lblTitulo.setFont(new java.awt.Font("Tahoma", 1, 24));
        lblTitulo.setForeground(java.awt.Color.WHITE);
        panelTitulo.add(lblTitulo);

        // Panel botones
        JPanel panelBotones = new JPanel();
        panelBotones.setBackground(new java.awt.Color(33, 33, 33));
        panelBotones.setLayout(new javax.swing.BoxLayout(panelBotones, javax.swing.BoxLayout.Y_AXIS));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        btn_crear = crearBoton("Nuevo", "/imagenes/nuevo.png");
        btn_eliminar = crearBoton("Anular", "/imagenes/eliminar.png");
        btn_ver = crearBoton("Ver", "/imagenes/ver.png");
        btn_actualizar = crearBoton("Actualizar", "/imagenes/actualizar.png");

        panelBotones.add(btn_crear);
        panelBotones.add(Box.createVerticalStrut(6));
        panelBotones.add(btn_eliminar);
        panelBotones.add(Box.createVerticalStrut(6));
        panelBotones.add(btn_ver);
        panelBotones.add(Box.createVerticalStrut(6));
        panelBotones.add(btn_actualizar);

        // Panel tabla
        JPanel panelTabla = new JPanel(new java.awt.BorderLayout(5, 5));
        panelTabla.setBackground(new java.awt.Color(33, 33, 33));
        panelTabla.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        txt_Filtro = new JTextField();
        txt_Filtro.setFont(new java.awt.Font("Tahoma", 0, 18));

        jtabla = new JTable(modelo);
        jtabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(jtabla);

        panelTabla.add(txt_Filtro, java.awt.BorderLayout.NORTH);
        panelTabla.add(scroll, java.awt.BorderLayout.CENTER);

        // Layout
        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(panelTitulo, java.awt.BorderLayout.NORTH);
        getContentPane().add(panelBotones, java.awt.BorderLayout.WEST);
        getContentPane().add(panelTabla, java.awt.BorderLayout.CENTER);

        // Eventos
        btn_crear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { crearAjuste(); }
        });
        btn_eliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { anularAjuste(); }
        });
        btn_ver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { verAjuste(); }
        });
        btn_actualizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { actualizar(); }
        });
        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) verAjuste();
            }
        });

        pack();
    }

    private JButton crearBoton(String texto, String icono) {
        JButton btn = new JButton(texto);
        btn.setFont(new java.awt.Font("Tahoma", 1, 14));
        btn.setBorder(null);
        btn.setMaximumSize(new java.awt.Dimension(150, 35));
        btn.setPreferredSize(new java.awt.Dimension(150, 35));
        try {
            btn.setIcon(new ImageIcon(getClass().getResource(icono)));
        } catch (Exception ignore) {}
        return btn;
    }

    public void actualizar() {
        try {
            for (int i = 0; i < jtabla.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {}

        String sql = "SELECT a.id, a.fecha, a.hora, u.nombre as usuario, b.nombre as bodega, "
                + "a.observacion, a.estado, "
                + "(SELECT COUNT(*) FROM ajustes_inventario_detalle d WHERE d.id_ajuste_cabecera = a.id) as productos "
                + "FROM ajustes_inventario_cabecera a "
                + "JOIN users u ON u.id = a.id_user "
                + "JOIN bodegas b ON b.id = a.id_bodega "
                + "ORDER BY a.id DESC";

        modelo.setColumnIdentifiers(new Object[]{
            "ID", "Fecha", "Hora", "Usuario", "Bodega", "Productos", "Observaci\u00f3n", "Estado"
        });

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                String estado = rs.getInt("estado") == 1 ? "Activo" : "Anulado";
                modelo.addRow(new Object[]{
                    rs.getString("id"),
                    rs.getString("fecha"),
                    rs.getString("hora"),
                    rs.getString("usuario"),
                    rs.getString("bodega"),
                    rs.getString("productos"),
                    rs.getString("observacion") != null ? rs.getString("observacion") : "",
                    estado
                });
            }
            rs.close();
            jtabla.setModel(modelo);
            ajustarColumnas();
        } catch (Exception e) {
            System.err.println("Error cargando ajustes: " + e.getMessage());
        }
    }

    private void ajustarColumnas() {
        try {
            columnModel = jtabla.getColumnModel();
            columnModel.getColumn(0).setPreferredWidth(50);
            columnModel.getColumn(1).setPreferredWidth(100);
            columnModel.getColumn(2).setPreferredWidth(70);
            columnModel.getColumn(3).setPreferredWidth(150);
            columnModel.getColumn(4).setPreferredWidth(150);
            columnModel.getColumn(5).setPreferredWidth(80);
            columnModel.getColumn(6).setPreferredWidth(300);
            columnModel.getColumn(7).setPreferredWidth(80);
        } catch (Exception ignore) {}
    }

    private void crearAjuste() {
        jd_crear_ajuste_inventario dialog = new jd_crear_ajuste_inventario(null, true);
        dialog.setVisible(true);
        actualizar();
    }

    private void verAjuste() {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un ajuste.");
            return;
        }
        String id = jtabla.getValueAt(fila, 0).toString();
        jd_crear_ajuste_inventario dialog = new jd_crear_ajuste_inventario(null, true);
        dialog.cargarAjuste(Integer.parseInt(id));
        dialog.setVisible(true);
    }

    private void anularAjuste() {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un ajuste.");
            return;
        }

        String id = jtabla.getValueAt(fila, 0).toString();
        String estado = jtabla.getValueAt(fila, 7).toString();

        if ("Anulado".equals(estado)) {
            JOptionPane.showMessageDialog(this, "Este ajuste ya fue anulado.");
            return;
        }

        int resp = JOptionPane.showConfirmDialog(this,
                "Esta seguro que desea ANULAR el ajuste #" + id + "?\n"
                + "Se revertiran todos los cambios de stock.",
                "Confirmar anulacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (resp != JOptionPane.YES_OPTION) return;

        int idUser = frm_main.id_user;
        DBajustes_inventario db = new DBajustes_inventario();
        if (db.eliminar(Integer.parseInt(id), idUser)) {
            JOptionPane.showMessageDialog(this, "Ajuste #" + id + " anulado correctamente.");
            actualizar();
        }
    }
}
