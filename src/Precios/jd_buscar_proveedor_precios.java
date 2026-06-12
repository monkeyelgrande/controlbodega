package Precios;

import Metodos.TextPrompt;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Contactos;

/**
 * Busqueda de proveedores para el modulo Precios (portado del
 * jd_buscar_proveedor de productos-agroinsumos).
 */
public class jd_buscar_proveedor_precios extends javax.swing.JDialog {

    static TableColumnModel columnModel = null;

    private JTextField txt_Filtro;
    private JButton btn_seleccionar;
    private JPanel jPanel1;
    private JScrollPane jScrollPane1;
    private JTable jtabla;

    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public jd_buscar_proveedor_precios(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        columnModel = jtabla.getColumnModel();
        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        mostrar();
        new TextPrompt("Buscar proveedor por nombre...", txt_Filtro);
        this.setLocationRelativeTo(parent);
        txt_Filtro.requestFocus();
        metodos.addEscapeListenerWindowDialog(this);

        jtabla.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    seleccionarProveedor();
                }
            }
        });

        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    seleccionarProveedor();
                }
            }
        });

        txt_Filtro.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    jtabla.requestFocus();
                    if (jtabla.getRowCount() > 0) {
                        jtabla.getSelectionModel().setSelectionInterval(0, 0);
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (jtabla.getRowCount() > 0) {
                        jtabla.getSelectionModel().setSelectionInterval(0, 0);
                        seleccionarProveedor();
                    }
                }
            }
        });
    }

    private void initComponents() {
        txt_Filtro = new JTextField();
        btn_seleccionar = new JButton();
        jPanel1 = new JPanel();
        jScrollPane1 = new JScrollPane();
        jtabla = new JTable();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Buscar Proveedor");
        setResizable(false);

        txt_Filtro.setFont(new java.awt.Font("Tahoma", 0, 20));

        btn_seleccionar.setFont(new java.awt.Font("Tahoma", 1, 16));
        btn_seleccionar.setText("Seleccionar");
        btn_seleccionar.addActionListener(e -> seleccionarProveedor());

        jPanel1.setBackground(new java.awt.Color(34, 49, 63));
        jPanel1.setBorder(BorderFactory.createTitledBorder(
                null, "Proveedores",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new java.awt.Font("Tahoma", 1, 14),
                new java.awt.Color(255, 255, 255)));

        jtabla.setFont(new java.awt.Font("Tahoma", 0, 14));
        jtabla.setRowHeight(25);
        jtabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jtabla);

        GroupLayout panelLayout = new GroupLayout(jPanel1);
        jPanel1.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
                panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1)
                                .addContainerGap()));
        panelLayout.setVerticalGroup(
                panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                                .addContainerGap()));

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(txt_Filtro, GroupLayout.PREFERRED_SIZE, 500, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btn_seleccionar, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)))
                                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(txt_Filtro, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn_seleccionar))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap()));

        pack();
    }

    private void mostrar() {
        try {
            for (int i = modelo.getRowCount() - 1; i >= 0; i--) {
                modelo.removeRow(i);
            }
        } catch (Exception e) {
        }

        modelo.setColumnIdentifiers(new Object[]{"ID", "Nombre", "Cedula/NIT", "Contacto", "Ciudad"});
        ResultSet rs = DB_consultas_R_D.getTabla(
                "select id, nombre, cedula, contacto, ciudad from contactos where proveedor=1 order by nombre");
        try {
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("cedula"),
                    rs.getString("contacto"),
                    rs.getString("ciudad")
                });
            }
            rs.close();
            jtabla.setModel(modelo);
            ajustarColumnas();
        } catch (Exception e) {
            System.out.println("Error cargando proveedores: " + e);
        }
    }

    private void ajustarColumnas() {
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(0).setMaxWidth(60);
        columnModel.getColumn(1).setPreferredWidth(300);
        columnModel.getColumn(2).setPreferredWidth(120);
        columnModel.getColumn(3).setPreferredWidth(120);
        columnModel.getColumn(4).setPreferredWidth(120);
    }

    private void seleccionarProveedor() {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un proveedor");
            return;
        }

        int id = Integer.parseInt(jtabla.getValueAt(fila, 0).toString());

        JComboBox<Contactos> combo = jif_crear_ingreso_precios.jbox_proveedor;
        for (int i = 0; i < combo.getItemCount(); i++) {
            Contactos c = combo.getItemAt(i);
            if (c.getId() == id) {
                combo.setSelectedIndex(i);
                break;
            }
        }

        this.dispose();
    }
}
