package Precios;

import Metodos.metodos;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.view.JasperViewer;

/**
 * Impresion de etiquetas/codigos de barras en dos tamanos (25x35 y 100x50 mm)
 * con JasperReports (portado del jd_Productos_a_imprimir de
 * productos-agroinsumos). Requiere src/reportes/Codigos25x35.jrxml y
 * Codigos100x50.jrxml.
 *
 * @author Monkeyelgrande
 */
public class jd_productos_a_imprimir extends javax.swing.JDialog {

    public static DefaultTableModel modeloProductos = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return columna == 2; // solo cantidad
        }
    };
    public static DefaultTableModel modeloImprimir = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public jd_productos_a_imprimir(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setLocationRelativeTo(parent);

        metodos.addEscapeListenerWindowDialog(this);

        jtabla_productos.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_DELETE) {
                    quitar_productos(modeloProductos);
                }
            }
        });
    }

    public void quitar_productos(DefaultTableModel model) {
        if (model.getRowCount() > 0) {

            int fila = jtabla_productos.getSelectedRow();
            if (jtabla_productos.getSelectedRowCount() < 1) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro");
            } else {
                model.removeRow(fila);
            }
        }
    }

    public void TamanosTablaVentas(TableColumnModel cm) {
        cm.getColumn(0).setPreferredWidth(15);
        cm.getColumn(1).setPreferredWidth(300);
        cm.getColumn(2).setPreferredWidth(80);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla_productos = new javax.swing.JTable();
        btn_buscar = new javax.swing.JButton();
        btn_buscar1 = new javax.swing.JButton();
        btn_imprimir_peque = new javax.swing.JButton();
        btn_imprimir_grande = new javax.swing.JButton();
        jbox_tamano_peque = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jbox_tamano_grande = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Impresión de etiquetas");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jPanel2.setBackground(new java.awt.Color(34, 49, 63));

        jtabla.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jtabla.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{}
        ));
        jtabla.setRowHeight(32);
        jtabla.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jtabla.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jtabla);

        jtabla_productos.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jtabla_productos.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{}
        ));
        jtabla_productos.setRowHeight(32);
        jtabla_productos.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtabla_productos.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(jtabla_productos);

        btn_buscar.setBackground(new java.awt.Color(0, 204, 204));
        btn_buscar.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_buscar.setForeground(new java.awt.Color(0, 51, 51));
        btn_buscar.setText("Buscar producto");
        btn_buscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscarActionPerformed(evt);
            }
        });

        btn_buscar1.setBackground(new java.awt.Color(255, 0, 0));
        btn_buscar1.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_buscar1.setForeground(new java.awt.Color(255, 255, 255));
        btn_buscar1.setText("Procesar");
        btn_buscar1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscar1ActionPerformed(evt);
            }
        });

        btn_imprimir_peque.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_imprimir_peque.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/print_24.png"))); // NOI18N
        btn_imprimir_peque.setText("Peq 25*35");
        btn_imprimir_peque.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimir_pequeActionPerformed(evt);
            }
        });

        btn_imprimir_grande.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_imprimir_grande.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/print_24.png"))); // NOI18N
        btn_imprimir_grande.setText("Grande 100*50");
        btn_imprimir_grande.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimir_grandeActionPerformed(evt);
            }
        });

        jbox_tamano_peque.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jbox_tamano_peque.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"}));
        jbox_tamano_peque.setSelectedIndex(6);

        jLabel1.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Tamaño Letra");

        jbox_tamano_grande.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jbox_tamano_grande.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "35", "40"}));
        jbox_tamano_grande.setSelectedIndex(16);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(btn_buscar)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 338, Short.MAX_VALUE)
                                                .addComponent(btn_buscar1)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 769, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                                .addComponent(jLabel1)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jbox_tamano_grande, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(btn_imprimir_grande, javax.swing.GroupLayout.Alignment.TRAILING))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(btn_imprimir_peque, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jbox_tamano_peque, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(btn_buscar)
                                                .addComponent(btn_buscar1))
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(jbox_tamano_peque, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel1)
                                                .addComponent(jbox_tamano_grande, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 752, Short.MAX_VALUE)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(btn_imprimir_peque)
                                                        .addComponent(btn_imprimir_grande))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jScrollPane1)))
                                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    private void btn_imprimir_pequeActionPerformed(java.awt.event.ActionEvent evt) {
        imprimir("Codigos25x35.jrxml", jbox_tamano_peque.getSelectedItem().toString());
    }

    private void btn_imprimir_grandeActionPerformed(java.awt.event.ActionEvent evt) {
        imprimir("Codigos100x50.jrxml", jbox_tamano_grande.getSelectedItem().toString());
    }

    private void imprimir(String plantilla, String fontSize) {
        try {
            DefaultTableModel de = (DefaultTableModel) jtabla.getModel();
            JRTableModelDataSource datasource = new JRTableModelDataSource(de);

            JasperReport report = JasperCompileManager.compileReport(
                    new File("").getAbsolutePath() + "/src/reportes/" + plantilla);
            Map<String, Object> params = new HashMap<>();
            params.put("SUBREPORT_DIR", new File("").getAbsolutePath() + "/src/reportes/");
            params.put("FONT_SIZE", fontSize);

            JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, datasource);

            JasperViewer view = new JasperViewer(jasperPrint, false);

            JDialog dialog = new JDialog(this);
            dialog.setContentPane(view.getContentPane());
            dialog.setSize(view.getSize());
            dialog.setModal(true);
            dialog.setLocationRelativeTo(this);
            dialog.setTitle("Impresión de etiquetas");
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al generar las etiquetas:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void btn_buscarActionPerformed(java.awt.event.ActionEvent evt) {
        jd_buscar_producto_precios buscar_producto = new jd_buscar_producto_precios(null, rootPaneCheckingEnabled);
        jd_buscar_producto_precios.formulario = "imprimir";
        buscar_producto.show();
    }

    private void btn_buscar1ActionPerformed(java.awt.event.ActionEvent evt) {

        try {
            for (int i = 0; i < modeloImprimir.getRowCount(); i++) {
                modeloImprimir.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        modeloImprimir.setColumnIdentifiers(new Object[]{"Código", "Descripcion"});

        for (int i = 0; i < modeloProductos.getRowCount(); i++) {

            int cantidad = (int) Double.parseDouble(modeloProductos.getValueAt(i, 2).toString());

            for (int j = 0; j < cantidad; j++) {
                String nombre = modeloProductos.getValueAt(i, 1).toString();
                modeloImprimir.addRow(new Object[]{modeloProductos.getValueAt(i, 0).toString(), nombre});
            }
        }

        jtabla.setModel(modeloImprimir);
    }

    // Variables declaration
    public static javax.swing.JButton btn_buscar;
    public static javax.swing.JButton btn_buscar1;
    private javax.swing.JButton btn_imprimir_grande;
    private javax.swing.JButton btn_imprimir_peque;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JComboBox<String> jbox_tamano_grande;
    private javax.swing.JComboBox<String> jbox_tamano_peque;
    public static javax.swing.JTable jtabla;
    public static javax.swing.JTable jtabla_productos;
}
