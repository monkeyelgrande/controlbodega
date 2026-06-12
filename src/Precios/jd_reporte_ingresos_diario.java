package Precios;

import Metodos.CellRendererIngresoProductos;
import Metodos.ExportarExcel;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBingresosPrecios;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * Reporte: ingresos de productos de un dia (modulo Precios; portado del
 * jd_Ingreso_productosDiario de productos-agroinsumos, adaptado a las tablas
 * ingresos_productos_* — la consulta original apuntaba a tablas que no
 * existian en agro).
 *
 * @author Monkeyelgrande
 */
public class jd_reporte_ingresos_diario extends javax.swing.JDialog {

    static DefaultTableModel modeloBalance = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    DecimalFormat formatea = new DecimalFormat("###,###.##");
    TableColumnModel columnModelBalance = null;
    CellRendererIngresoProductos myRenderer = new CellRendererIngresoProductos();

    public jd_reporte_ingresos_diario(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setLocationRelativeTo(parent);
        jdate_fecha1.setCalendar(new GregorianCalendar());
        columnModelBalance = jtable_productos.getColumnModel();
        metodos.addEscapeListenerWindowDialog(this);

        jtable_productos.setDefaultRenderer(Object.class, myRenderer);
        metodos.BuscarEnTabla(jTextField1, jtable_productos);
        jtable_productos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    int fila = jtable_productos.getSelectedRow();
                    String id = "" + jtable_productos.getValueAt(fila, 0);
                    DBingresosPrecios.ver_ingreso_productos("ver", id);
                }
            }
        });
        btn_consultar.doClick();
    }

    public void LimpiarModelos() {
        try {
            for (int i = 0; i < jtable_productos.getRowCount(); i++) {
                modeloBalance.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
    }

    public void TamanosTablaVentas(TableColumnModel cm) {
        cm.getColumn(0).setPreferredWidth(40);
        cm.getColumn(1).setPreferredWidth(100);
        cm.getColumn(2).setPreferredWidth(500);
        cm.getColumn(3).setPreferredWidth(200);
        cm.getColumn(4).setPreferredWidth(80);
        cm.getColumn(5).setPreferredWidth(80);
        cm.getColumn(6).setPreferredWidth(80);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        btn_consultar = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtable_productos = new javax.swing.JTable();
        jTextField1 = new javax.swing.JTextField();
        jdate_fecha1 = new com.toedter.calendar.JDateChooser();
        jLabel3 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Ingresos de productos del día");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        btn_consultar.setBackground(new java.awt.Color(37, 116, 169));
        btn_consultar.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_consultar.setForeground(new java.awt.Color(255, 255, 255));
        btn_consultar.setMnemonic('r');
        btn_consultar.setText("Consultar");
        btn_consultar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_consultarActionPerformed(evt);
            }
        });

        jPanel2.setBackground(new java.awt.Color(34, 49, 63));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Ingresos del día", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 14), new java.awt.Color(255, 255, 255))); // NOI18N

        jtable_productos.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jtable_productos.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{}
        ));
        jtable_productos.setRowHeight(35);
        jtable_productos.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtable_productos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(jtable_productos);

        jTextField1.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1400, Short.MAX_VALUE)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 615, Short.MAX_VALUE)
                                .addContainerGap())
        );

        jdate_fecha1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

        jLabel3.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        jLabel3.setText("Fecha:");

        jButton1.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jButton1.setText("Exportar a excel");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jdate_fecha1, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_consultar, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton1)
                                .addContainerGap())
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(jdate_fecha1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn_consultar)
                                        .addComponent(jButton1))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

    private void btn_consultarActionPerformed(java.awt.event.ActionEvent evt) {

        LimpiarModelos();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fecha1;
        try {
            fecha1 = sdf.format(jdate_fecha1.getDate());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Seleccione una fecha");
            return;
        }

        modeloBalance.setColumnIdentifiers(new Object[]{"# ingreso", "Código", "Descripción", "Proveedor", "Fecha", "Cantidad", "Estado"});
        String consulta = "select ic.id as id_ingreso, p.codigo_barras, p.descripcion, c.nombre as proveedor, "
                + "ic.fecha, i.cantidad, ic.estado "
                + "from ingresos_productos_detalle i, ingresos_productos_cabecera ic, productos p, contactos c "
                + "where i.id_ingreso_cabecera=ic.id and i.id_producto=p.id and ic.id_proveedor=c.id "
                + "and ic.fecha = '" + fecha1 + "' order by ic.id, p.id";

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        try {
            while (rs.next()) {
                String estado = "";
                switch (rs.getInt("estado")) {
                    case 0:
                        estado = "Recibido";
                        break;
                    case 1:
                        estado = "Ingresado";
                        break;
                    case 2:
                        estado = "Precios";
                        break;
                }
                modeloBalance.addRow(new Object[]{
                    rs.getString("id_ingreso"),
                    rs.getString("codigo_barras"),
                    rs.getString("descripcion"),
                    rs.getString("proveedor"),
                    rs.getString("fecha"),
                    formatea.format(rs.getDouble("cantidad")),
                    estado
                });
            }
            rs.close();
            jtable_productos.setModel(modeloBalance);
            TamanosTablaVentas(columnModelBalance);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            ExportarExcel obj = new ExportarExcel();
            obj.exportarExcel(jtable_productos);
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
    }

    // Variables declaration
    private javax.swing.JButton btn_consultar;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField jTextField1;
    private com.toedter.calendar.JDateChooser jdate_fecha1;
    private javax.swing.JTable jtable_productos;
}
