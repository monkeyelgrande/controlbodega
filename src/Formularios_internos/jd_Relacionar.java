/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios_internos;

import Formularios.frm_Crear_Orden;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBstock_productos;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author Monkeyelgrande
 */
public class jd_Relacionar extends javax.swing.JDialog {

    /**
     * Creates new form jd_ver_devolucion
     */
    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    public jd_Relacionar(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setLocationRelativeTo(parent);
        metodos.addEscapeListenerWindowDialog(this);
        metodos.EstiloTablaMaterialGlobal(jtabla_factura);
        metodos.EstiloTablaMaterialGlobal(jtabla);
        metodos.BuscarEnTabla(txt_Filtro, jtabla);

        actualizar();
        TamanosTablaAbonos();

        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    cargar_facturas(jtabla);

                }
            }
        });
        jtabla_factura.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    int fila = jtabla_factura.getSelectedRow();

                    frm_Crear_Orden.modelo_ventas.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "R", "BODEGA", "IDBOD"});

                    double cantRel;
                    try {
                        // La columna 3 viene formateada (separador de miles ','): se limpia para parsear.
                        cantRel = Double.parseDouble(jtabla_factura.getValueAt(fila, 3).toString().replace(",", ""));
                    } catch (Exception ex) {
                        cantRel = -1; // sin cantidad valida: omite configuracion por rangos
                    }
                    int idBodAuto = DBstock_productos.seleccionarBodegaDescarga(Integer.parseInt(jtabla_factura.getValueAt(fila, 0).toString()), cantRel);
                    frm_Crear_Orden.modelo_ventas.addRow(new Object[]{jtabla_factura.getValueAt(fila, 0).toString(), jtabla_factura.getValueAt(fila, 1).toString(),
                        jtabla_factura.getValueAt(fila, 2).toString(), jtabla_factura.getValueAt(fila, 3).toString(), lbl_id_factura.getText(),
                        DBstock_productos.nombreBodega(idBodAuto), idBodAuto});

                    frm_Crear_Orden.jtabla_Ventas.setModel(frm_Crear_Orden.modelo_ventas);
                }
            }
        });

    }

    public void cargar_facturas(JTable jtabla) {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            String id = (String) jtabla.getValueAt(fila, 0);

            ResultSet rs = DB_consultas_R_D.getTabla("select f.id as id_f, fecha,f.hora, c.id as id_c, c.cedula, c.nombre, c.direccion, "
                    + "c.contacto,f.tipo_factura, u.user_name, f.codigo "
                    + "from facturas_cabeceras f,contactos c, users u where f.id_user=u.id and f.id_contacto=c.id and f.id =" + id);
            try {
                while (rs.next()) {
                    lbl_id_factura.setText(rs.getString("id_f"));
                    lbl_fecha.setText(rs.getString("fecha"));
                    lbl_cliente.setText(rs.getString("nombre"));
                    lbl_hora.setText(rs.getString("hora"));
                    lbl_codigo.setText(rs.getString("codigo"));
                }
                rs.close();

            } catch (SQLException ex) {
                System.out.println(ex);
            }

            rs = DB_consultas_R_D.getTabla("select f.id, p.id as id_producto, p.codigo_barras, p.descripcion, f.cantidad "
                    + "from facturas_detalles f, productos p where f.id_producto=p.id and id_cabecera=" + id);
            DefaultTableModel modelof = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false; //Con esto conseguimos que la tabla no se pueda editar
                }
            };
            modelof.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD",});
            try {
                while (rs.next()) {
                    modelof.addRow(new Object[]{rs.getString("id_producto"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                        metodos.formateador_un_decimal().format(rs.getDouble("cantidad"))});
                }
                rs.close();
                jtabla_factura.setModel(modelof);
            } catch (SQLException ex) {
                System.out.println(ex);
            }

        }
    }

    public void TamanosTablaAbonos() {
        jtabla.getTableHeader().setReorderingAllowed(false);
        TableColumnModel columnModel = jtabla.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(100);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(300);

    }

    public void actualizar() {
        try {
            for (int i = 0; i < jtabla.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

        ResultSet rs = DB_consultas_R_D.getTabla("SELECT \n"
                + "    fc.id AS id_factura,\n"
                + "    fc.codigo,\n"
                + "    c.nombre AS nombre_cliente\n"
                + "FROM \n"
                + "    facturas_cabeceras fc\n"
                + "JOIN \n"
                + "    contactos c ON fc.id_contacto = c.id\n"
                + "WHERE \n"
                + "    fc.tipo_factura = 'Venta';");

        modelo.setColumnIdentifiers(new Object[]{"# Orden", "#Factura", "Cliente"});
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                modelo.addRow(new Object[]{rs.getString("id_factura"), rs.getString("codigo"), rs.getString("nombre_cliente")});
            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla.setModel(modelo);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        txt_Filtro = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        lbl_cliente = new javax.swing.JLabel();
        lbl_fecha = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        lbl_hora = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        lbl_codigo = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        lbl_id_factura = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla_factura = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(244, 245, 251));

        txt_Filtro.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_Filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_FiltroKeyTyped(evt);
            }
        });

        jtabla.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jtabla);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 477, Short.MAX_VALUE)
                    .addComponent(txt_Filtro))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel2.setBackground(new java.awt.Color(255, 239, 255));

        lbl_cliente.setBackground(new java.awt.Color(15, 23, 42));
        lbl_cliente.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        lbl_cliente.setForeground(new java.awt.Color(15, 23, 42));
        lbl_cliente.setText("Cliente");

        lbl_fecha.setBackground(new java.awt.Color(15, 23, 42));
        lbl_fecha.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        lbl_fecha.setForeground(new java.awt.Color(15, 23, 42));
        lbl_fecha.setText("Fecha");

        jLabel6.setBackground(new java.awt.Color(15, 23, 42));
        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(15, 23, 42));
        jLabel6.setText("Fecha");

        lbl_hora.setBackground(new java.awt.Color(15, 23, 42));
        lbl_hora.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        lbl_hora.setForeground(new java.awt.Color(15, 23, 42));
        lbl_hora.setText("Hora");

        jLabel7.setBackground(new java.awt.Color(15, 23, 42));
        jLabel7.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(15, 23, 42));
        jLabel7.setText("Hora");

        lbl_codigo.setBackground(new java.awt.Color(15, 23, 42));
        lbl_codigo.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        lbl_codigo.setForeground(new java.awt.Color(15, 23, 42));
        lbl_codigo.setText("ID");

        jLabel3.setBackground(new java.awt.Color(15, 23, 42));
        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(15, 23, 42));
        jLabel3.setText("Cod Fact");

        jLabel4.setBackground(new java.awt.Color(15, 23, 42));
        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(15, 23, 42));
        jLabel4.setText("Cliente");

        jLabel2.setBackground(new java.awt.Color(15, 23, 42));
        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(15, 23, 42));
        jLabel2.setText("ID");

        lbl_id_factura.setBackground(new java.awt.Color(15, 23, 42));
        lbl_id_factura.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        lbl_id_factura.setForeground(new java.awt.Color(15, 23, 42));
        lbl_id_factura.setText("ID");

        jtabla_factura.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jScrollPane2.setViewportView(jtabla_factura);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbl_codigo))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbl_id_factura)))
                        .addGap(64, 64, 64)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_cliente)
                            .addComponent(lbl_fecha))
                        .addGap(228, 228, 228)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lbl_hora)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 863, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(lbl_id_factura))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(lbl_codigo)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(lbl_cliente))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel6)
                                .addComponent(lbl_fecha))
                            .addComponent(lbl_hora, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(2, 2, 2)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txt_FiltroKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_FiltroKeyTyped

    }//GEN-LAST:event_txt_FiltroKeyTyped

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(jd_Relacionar.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(jd_Relacionar.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(jd_Relacionar.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(jd_Relacionar.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                jd_Relacionar dialog = new jd_Relacionar(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    public static javax.swing.JTable jtabla;
    public static javax.swing.JTable jtabla_factura;
    public static javax.swing.JLabel lbl_cliente;
    public static javax.swing.JLabel lbl_codigo;
    public static javax.swing.JLabel lbl_fecha;
    public static javax.swing.JLabel lbl_hora;
    public static javax.swing.JLabel lbl_id_factura;
    private javax.swing.JTextField txt_Filtro;
    // End of variables declaration//GEN-END:variables
}
