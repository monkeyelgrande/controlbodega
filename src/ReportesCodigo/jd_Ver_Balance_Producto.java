/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ReportesCodigo;

import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Monkeyelgrande
 */
public class jd_Ver_Balance_Producto extends javax.swing.JDialog {

    public static DefaultTableModel modeloBodegas = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    String id_producto, fecha;

    public jd_Ver_Balance_Producto(java.awt.Frame parent, boolean modal, String id_producto, String fecha) {
        super(parent, modal);
        initComponents();
        this.id_producto = id_producto;
        this.fecha = fecha;
        modeloBodegas.setColumnIdentifiers(new Object[]{"Bodega", "Cantidad"});

        this.setLocationRelativeTo(parent);
        metodos.addEscapeListenerWindowDialog(this);
        consulta();
    }

    public void consulta() {
        try {
            for (int i = 0; i < modeloBodegas.getRowCount(); i++) {
                modeloBodegas.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        String sql = "WITH movimientos AS (\n"
                + "    -- 1) ENTRADAS: ingresos de mercancía (solo estado = 1 = recibido)\n"
                + "    SELECT\n"
                + "        imd.id_producto,\n"
                + "        imc.id_bodega,\n"
                + "        imd.cantidad::double precision AS qty\n"
                + "    FROM ingresos_mercancias_detalle imd\n"
                + "    JOIN ingresos_mercancias_cabecera imc\n"
                + "        ON imd.id_ingreso_cabecera = imc.id\n"
                + "    WHERE imc.estado = 1\n"
                + "\n"
                + "    UNION ALL\n"
                + "\n"
                + "    -- 2) ENTRADAS: devoluciones a bodega\n"
                + "    SELECT\n"
                + "        dd.id_producto,\n"
                + "        d.id_bodega,\n"
                + "        dd.cantidad::double precision AS qty\n"
                + "    FROM devoluciones_detalles dd\n"
                + "    JOIN devoluciones d\n"
                + "        ON dd.id_cabecera_devolucion = d.id\n"
                + "\n"
                + "    UNION ALL\n"
                + "\n"
                + "    -- 3) ENTRADAS: traslados (bodega destino)\n"
                + "    SELECT\n"
                + "        t.id_producto,\n"
                + "        t.id_bodega_destino::integer AS id_bodega,\n"
                + "        t.cantidad::double precision AS qty\n"
                + "    FROM traslados_productos t\n"
                + "\n"
                + "    UNION ALL\n"
                + "\n"
                + "    -- 4) SALIDAS: facturas tipo \"Venta\"\n"
                + "    SELECT\n"
                + "        fd.id_producto,\n"
                + "        fc.id_bodega,\n"
                + "        -fd.cantidad::double precision AS qty\n"
                + "    FROM facturas_detalles fd\n"
                + "    JOIN facturas_cabeceras fc\n"
                + "        ON fd.id_cabecera = fc.id\n"
                + "    WHERE fc.anulado = 1\n"
                + "      AND fc.tipo_factura = 'Venta'\n"
                + "\n"
                + "    UNION ALL\n"
                + "\n"
                + "    -- 5) SALIDAS: entregas de facturas tipo \"Salida\", \"Prestamo\", \"Eliminacion\"\n"
                + "    SELECT\n"
                + "        ep.id_producto,\n"
                + "        epc.id_bodega,\n"
                + "        -ep.cantidad::double precision AS qty\n"
                + "    FROM entregas_productos ep\n"
                + "    JOIN entregas_productos_cabecera epc\n"
                + "        ON ep.id_cabecera = epc.id\n"
                + "    JOIN facturas_cabeceras fc\n"
                + "        ON epc.id_factura = fc.id\n"
                + "    WHERE fc.anulado = 1\n"
                + "      AND fc.tipo_factura IN ('Salida','Prestamo','Eliminacion')\n"
                + "\n"
                + "    UNION ALL\n"
                + "\n"
                + "    -- 6) SALIDAS: traslados (bodega origen)\n"
                + "    SELECT\n"
                + "        t.id_producto,\n"
                + "        t.id_bodega_origen::integer AS id_bodega,\n"
                + "        -t.cantidad::double precision AS qty\n"
                + "    FROM traslados_productos t\n"
                + "),\n"
                + "\n"
                + "stock_por_bodega AS (\n"
                + "    SELECT\n"
                + "        id_producto,\n"
                + "        id_bodega,\n"
                + "        SUM(qty) AS cantidad\n"
                + "    FROM movimientos\n"
                + "    GROUP BY id_producto, id_bodega\n"
                + ")\n"
                + "\n"
                + "SELECT\n"
                + "    b.id        AS id_bodega,\n"
                + "    b.nombre    AS nombre_bodega,\n"
                + "    COALESCE(s.cantidad, 0) AS cantidad\n"
                + "FROM bodegas b\n"
                + "JOIN stock_por_bodega s\n"
                + "    ON s.id_bodega = b.id\n"
                + "WHERE s.id_producto = " + id_producto + "\n"
                + "  AND s.cantidad <> 0\n"
                + "ORDER BY b.nombre;";

        System.out.println(sql);

        ResultSet rs = DB_consultas_R_D.getTabla(sql);
        try {

            try {
                while (rs.next()) {
                    modeloBodegas.addRow(new Object[]{rs.getString("nombre_bodega"), rs.getDouble("cantidad")});
                }
                jtabla.setModel(modeloBodegas);
                rs.close();
            } catch (Exception e) {
                System.out.println(e);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Verifique las fechas", "Alerta", WIDTH);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpop_1 = new javax.swing.JPopupMenu();
        jmenu_VerFactura1 = new javax.swing.JMenuItem();
        jpop_2 = new javax.swing.JPopupMenu();
        jmenu_2 = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        lbl_descripcion = new javax.swing.JLabel();
        lbl_codigo = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();
        jlabel = new javax.swing.JLabel();
        lbl_total_bodegas = new javax.swing.JLabel();
        jlabel1 = new javax.swing.JLabel();
        lbl_pendientes_entrega = new javax.swing.JLabel();
        jlabel2 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        lbl_total_disponibles = new javax.swing.JLabel();
        jlabel3 = new javax.swing.JLabel();

        jmenu_VerFactura1.setText("Ver factura");
        jpop_1.add(jmenu_VerFactura1);

        jmenu_2.setText("jMenuItem1");
        jpop_2.add(jmenu_2);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Balance productos entre fechas");
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        lbl_descripcion.setFont(new java.awt.Font("Yu Gothic Medium", 1, 24)); // NOI18N
        lbl_descripcion.setForeground(new java.awt.Color(153, 0, 0));
        lbl_descripcion.setText("lbl_descripcion");

        lbl_codigo.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        lbl_codigo.setText("lbl_codigo");

        jtabla.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
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
        jtabla.setRowHeight(35);
        jtabla.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jScrollPane2.setViewportView(jtabla);

        jlabel.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jlabel.setText("Total en bodegas:");

        lbl_total_bodegas.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        lbl_total_bodegas.setForeground(new java.awt.Color(0, 102, 51));
        lbl_total_bodegas.setText("bodegas");

        jlabel1.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jlabel1.setText("Pendientes de entrega:");

        lbl_pendientes_entrega.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        lbl_pendientes_entrega.setForeground(new java.awt.Color(153, 0, 0));
        lbl_pendientes_entrega.setText("entrega");

        jlabel2.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jlabel2.setText("Cantidad actual en cada bodega");

        lbl_total_disponibles.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        lbl_total_disponibles.setForeground(new java.awt.Color(102, 102, 0));
        lbl_total_disponibles.setText("entrega");

        jlabel3.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jlabel3.setText("Disponibles:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1114, Short.MAX_VALUE)
                    .addComponent(jSeparator1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(lbl_descripcion)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lbl_codigo))
                            .addComponent(jlabel2)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jlabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbl_total_bodegas)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jlabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbl_pendientes_entrega)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jlabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbl_total_disponibles)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_descripcion)
                    .addComponent(lbl_codigo))
                .addGap(30, 30, 30)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jlabel3)
                        .addComponent(lbl_total_disponibles))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jlabel)
                        .addComponent(lbl_total_bodegas)
                        .addComponent(jlabel1)
                        .addComponent(lbl_pendientes_entrega)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jlabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
                .addContainerGap())
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
    }// </editor-fold>//GEN-END:initComponents
    public Date sumarRestarDiasFecha(Date fecha, int dias) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fecha); // Configuramos la fecha que se recibe
        calendar.add(Calendar.DAY_OF_YEAR, dias); // numero de días a añadir, o restar en caso de días<0
        return calendar.getTime(); // Devuelve el objeto Date con los nuevos días añadidos

    }

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
            java.util.logging.Logger.getLogger(jd_Ver_Balance_Producto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(jd_Ver_Balance_Producto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(jd_Ver_Balance_Producto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(jd_Ver_Balance_Producto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                jd_Ver_Balance_Producto dialog = new jd_Ver_Balance_Producto(new javax.swing.JFrame(), true, "", "");
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
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel jlabel;
    public static javax.swing.JLabel jlabel1;
    private javax.swing.JLabel jlabel2;
    public static javax.swing.JLabel jlabel3;
    private javax.swing.JMenuItem jmenu_2;
    private javax.swing.JMenuItem jmenu_VerFactura1;
    private javax.swing.JPopupMenu jpop_1;
    private javax.swing.JPopupMenu jpop_2;
    private javax.swing.JTable jtabla;
    public static javax.swing.JLabel lbl_codigo;
    public static javax.swing.JLabel lbl_descripcion;
    public static javax.swing.JLabel lbl_pendientes_entrega;
    public static javax.swing.JLabel lbl_total_bodegas;
    public static javax.swing.JLabel lbl_total_disponibles;
    // End of variables declaration//GEN-END:variables
}
