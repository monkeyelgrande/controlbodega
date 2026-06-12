package Precios;

import Metodos.CellRendererIngresoProductos;
import Metodos.metodos;
import com.ezware.oxbow.swingbits.table.filter.TableRowFilterSupport;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBingresosPrecios;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * Selector de ingresos (estado Precios) para exportar a World Office (modulo
 * Precios, portado del jd_Ver_facturas_exportar_world_office de
 * productos-agroinsumos). Permite seleccion multiple de ingresos.
 *
 * @author Monkeyelgrande
 */
public class jd_ver_ingresos_world_office extends javax.swing.JDialog {

    public static DefaultTableModel modelo_ingresos = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    TableColumnModel columnModel = null;
    CellRendererIngresoProductos myRenderer = new CellRendererIngresoProductos();

    public jd_ver_ingresos_world_office(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setLocationRelativeTo(parent);

        columnModel = jtabla.getColumnModel();
        metodos.addEscapeListenerWindowDialog(this);
        TableRowFilterSupport.forTable(jtabla).searchable(true).apply();
        jtabla.setDefaultRenderer(Object.class, myRenderer);

        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    int fila = jtabla.getSelectedRow();
                    String id = (String) jtabla.getValueAt(fila, 0);
                    DBingresosPrecios.ver_ingreso_productos("ver", id);
                }
            }
        });
    }

    public void LimpiarModelos() {
        try {
            for (int i = 0; i < modelo_ingresos.getRowCount(); i++) {
                modelo_ingresos.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
    }

    public void TamanosTabla() {
        columnModel.getColumn(0).setPreferredWidth(20);
        columnModel.getColumn(1).setPreferredWidth(300);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(3).setPreferredWidth(100);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();
        btn_Exportar_Worold = new javax.swing.JButton();
        btn_Exportar_Worold1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Exportar ingresos a World Office");
        setModal(true);
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jPanel2.setBackground(new java.awt.Color(34, 49, 63));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Seleccione los ingresos a exportar (selección múltiple)", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 14), new java.awt.Color(255, 255, 255))); // NOI18N

        jtabla.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jtabla.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{}
        ));
        jtabla.setRowHeight(35);
        jtabla.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtabla.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jScrollPane2.setViewportView(jtabla);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1200, Short.MAX_VALUE)
                                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 555, Short.MAX_VALUE)
                                .addContainerGap())
        );

        btn_Exportar_Worold.setBackground(new java.awt.Color(78, 205, 196));
        btn_Exportar_Worold.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_Exportar_Worold.setText("Exportar Precio de Venta y Descuentos A World Office");
        btn_Exportar_Worold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Exportar_WoroldActionPerformed(evt);
            }
        });

        btn_Exportar_Worold1.setBackground(new java.awt.Color(78, 205, 196));
        btn_Exportar_Worold1.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_Exportar_Worold1.setText("Exportar Precio y Código A World Office");
        btn_Exportar_Worold1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Exportar_Worold1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(btn_Exportar_Worold, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btn_Exportar_Worold1, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btn_Exportar_Worold, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)
                                        .addComponent(btn_Exportar_Worold1, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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

    private void btn_Exportar_WoroldActionPerformed(java.awt.event.ActionEvent evt) {
        if (jtabla.getSelectedRowCount() < 1) {
            JOptionPane.showMessageDialog(this, "Seleccione al menos un ingreso");
            return;
        }
        jd_exportar_world_office jd = new jd_exportar_world_office(null, false);
        jd.LimpiarModelos();

        jd.modelo.setColumnIdentifiers(new Object[]{
            "Código", "CONTADO", "DES. N°1", "DES. N°2", "CREDITO",
            "Precio 5", "Precio 6", "Precio 7", "Precio 8", "Precio 9", "Precio 10",
            "Precio 11", "Precio 12", "Precio 13", "Precio 14", "Precio 15",
            "Precio 16", "Precio 17", "Precio 18", "Precio 19", "Precio 20",
            "Precio 21", "Precio 22", "Precio 23", "Precio 24", "Precio 25",
            "Precio 26", "Precio 27", "Precio 28", "Precio 29", "Precio 30"});

        String consulta = "select p.id,p.codigo_barras,p.descripcion,i.*, c.porcentaje_operacion\n"
                + "from ingresos_productos_detalle i, productos p, ingresos_productos_cabecera ic, configuraciones c\n"
                + "where i.id_producto=p.id and i.id_ingreso_cabecera=ic.id and ic.id in " + obtenerNumerosConcatenadosSeleccionados(jtabla) + ""
                + " order by i.id";
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        try {
            while (rs.next()) {
                jd.modelo.addRow(new Object[]{
                    rs.getString("codigo_barras"), rs.getDouble("venta"),
                    rs.getDouble("valor_desc_1"),
                    rs.getDouble("valor_desc_2"),
                    rs.getDouble("valor_credito"),
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    rs.getDouble("valor_s_y_t")
                });
                jd.jtable_productos.setRowHeight(20);
            }

            jd.jtable_productos.setModel(jd.modelo);

        } catch (SQLException ex) {
            System.out.println(ex);
        }

        jd.show();
    }

    private void btn_Exportar_Worold1ActionPerformed(java.awt.event.ActionEvent evt) {
        if (jtabla.getSelectedRowCount() < 1) {
            JOptionPane.showMessageDialog(this, "Seleccione al menos un ingreso");
            return;
        }
        jd_exportar_world_office jd = new jd_exportar_world_office(null, false);
        jd.LimpiarModelos();

        jd.modelo.setColumnIdentifiers(new Object[]{"Código", "Costo"});

        String consulta = "select p.id,p.codigo_barras,p.descripcion,i.*, c.porcentaje_operacion\n"
                + "from ingresos_productos_detalle i, productos p, ingresos_productos_cabecera ic, configuraciones c\n"
                + "where i.id_producto=p.id and i.id_ingreso_cabecera=ic.id and ic.id in " + obtenerNumerosConcatenadosSeleccionados(jtabla) + ""
                + " order by i.id";
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        try {
            while (rs.next()) {
                double costo = rs.getDouble("precio_costo"),
                        descuento = rs.getDouble("descuento");

                double costo_descuento = costo - (costo * (descuento / 100));

                jd.modelo.addRow(new Object[]{rs.getString("codigo_barras"), metodos.formateador_dinero().format(costo_descuento)});
                jd.jtable_productos.setRowHeight(20);
            }

            jd.jtable_productos.setModel(jd.modelo);

        } catch (SQLException ex) {
            System.out.println(ex);
        }

        jd.show();
    }

    public String obtenerNumerosConcatenadosSeleccionados(JTable table) {
        StringBuilder resultado = new StringBuilder("(");

        int[] filasSeleccionadas = table.getSelectedRows();

        for (int fila : filasSeleccionadas) {
            Object valor = table.getValueAt(fila, 0);
            if (valor != null) {
                resultado.append(valor.toString()).append(",");
            }
        }

        if (resultado.length() > 1) {
            resultado.setLength(resultado.length() - 1);
        }
        resultado.append(")");

        return resultado.toString();
    }

    // Variables declaration
    public static javax.swing.JButton btn_Exportar_Worold;
    public static javax.swing.JButton btn_Exportar_Worold1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    public static javax.swing.JTable jtabla;
}
