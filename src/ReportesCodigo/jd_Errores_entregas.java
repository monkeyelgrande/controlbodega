/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ReportesCodigo;

import Formularios.frm_main;
import Formularios.frm_ver_orden;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Monkeyelgrande
 */
public class jd_Errores_entregas extends javax.swing.JDialog {

    public static DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    // Instancia del formulario de ver factura
    private frm_ver_orden frm;

    public jd_Errores_entregas(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        this.setLocationRelativeTo(parent);
        metodos.addEscapeListenerWindowDialog(this);
        consulta();
        metodos.BuscarEnTabla(txt_Filtro_codigo, jtabla);
        metodos.EstiloTablaMaterialGlobal(jtabla);

        // Agregar el popup menu a la tabla
        jtabla.setComponentPopupMenu(jpop_1);

        // Inicializar el formulario de ver factura
        frm = new frm_ver_orden();
    }

    public void consulta() {
        // Limpiar el modelo antes de cargar nuevos datos
        modelo.setRowCount(0);

        // Definimos las columnas que queremos ver en la tabla
        modelo.setColumnIdentifiers(new Object[]{
            "Id entrega",
            "Id factura",
            "Código factura",
            "Tipo factura",
            "Fecha factura",
            "Id entrega cabecera",
            "Id entrega detalle",
            "Id producto",
            "Producto",
            "Cantidad entregada"
        });

        String sql
                = "SELECT\n"
                + "    ep.id        AS id_entrega,\n"
                + "    fc.id        AS id_factura,\n"
                + "    fc.codigo    AS codigo_factura,\n"
                + "    fc.tipo_factura,\n"
                + "    fc.fecha,\n"
                + "    epc.id       AS id_entrega_cabecera,\n"
                + "    ep.id        AS id_entrega_detalle,\n"
                + "    ep.id_producto,\n"
                + "    p.descripcion AS producto,\n"
                + "    ep.cantidad  AS cantidad_entregada\n"
                + "FROM entregas_productos ep\n"
                + "JOIN entregas_productos_cabecera epc\n"
                + "    ON ep.id_cabecera = epc.id\n"
                + "JOIN facturas_cabeceras fc\n"
                + "    ON epc.id_factura = fc.id\n"
                + "LEFT JOIN facturas_detalles fd\n"
                + "    ON fd.id_cabecera = fc.id\n"
                + "   AND fd.id_producto  = ep.id_producto\n"
                + "LEFT JOIN productos p\n"
                + "    ON p.id = ep.id_producto\n"
                + "WHERE fd.id IS NULL\n"
                + "ORDER BY fc.id, epc.id, ep.id;";

        System.out.println(sql);

        ResultSet rs = DB_consultas_R_D.getTabla(sql);
        try {
            try {
                while (rs.next()) {
                    modelo.addRow(new Object[]{
                        rs.getInt("id_entrega"),
                        rs.getInt("id_factura"),
                        rs.getString("codigo_factura"),
                        rs.getString("tipo_factura"),
                        rs.getString("fecha"),
                        rs.getInt("id_entrega_cabecera"),
                        rs.getInt("id_entrega_detalle"),
                        rs.getInt("id_producto"),
                        rs.getString("producto"),
                        rs.getDouble("cantidad_entregada")
                    });
                }
                jtabla.setModel(modelo);
                rs.close();
            } catch (Exception e) {
                System.out.println(e);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al ejecutar la consulta", "Alerta", WIDTH);
        }
    }

    // MÉTODO PARA ELIMINAR REGISTRO
    private void eliminarRegistro() {
        int filaSeleccionada = jtabla.getSelectedRow();

        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this,
                    "Por favor seleccione un registro para eliminar",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Obtener el ID de la entrega (columna 0)
        int idEntrega = (int) jtabla.getValueAt(filaSeleccionada, 0);
        String producto = (String) jtabla.getValueAt(filaSeleccionada, 8);

        // Confirmar eliminación
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Desea eliminar este registro de la tabla entregas_productos?\n\n"
                + "ID Entrega: " + idEntrega + "\n"
                + "Producto: " + producto,
                "Confirmar eliminación",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirmacion == JOptionPane.OK_OPTION) {
            // Ejecutar el DELETE

            try {
                boolean resultado = DB_consultas_R_D.eliminar("entregas_productos", "" + idEntrega);

                if (resultado) {
                    JOptionPane.showMessageDialog(this,
                            "Registro eliminado correctamente",
                            "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Recargar la tabla
                    consulta();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Error al eliminar el registro",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error al eliminar: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                System.out.println(e);
            }
        }
    }

    // MÉTODO PARA VER FACTURA (adaptado del código que compartiste)
    public void cargar_facturas() {
        try {
            for (int i = 0; i < frm_ver_orden.modelo_productos.getRowCount(); i++) {
                frm_ver_orden.modelo_productos.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
        }
        try {
            for (int i = 0; i < frm_ver_orden.modelo_entregados_cabecera.getRowCount(); i++) {
                frm_ver_orden.modelo_entregados_cabecera.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
        }
        try {
            for (int i = 0; i < frm_ver_orden.modelo_entregados_detalle.getRowCount(); i++) {
                frm_ver_orden.modelo_entregados_detalle.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
        }

        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            // Obtener el ID de la factura (columna 1)
            String id = jtabla.getValueAt(fila, 1).toString();

            ResultSet rs = DB_consultas_R_D.getTabla(
                    "SELECT "
                    + "   f.id AS id_f, "
                    + "   f.fecha, "
                    + "   f.hora, "
                    + "   c.id AS id_c, "
                    + "   c.cedula, "
                    + "   c.nombre, "
                    + "   c.direccion, "
                    + "   c.contacto, "
                    + "   f.tipo_factura, "
                    + "   u.user_name, "
                    + "   f.codigo, "
                    + "   f.observacion, "
                    + "   f.observacion_entrega, "
                    + "   f.id_bodega, "
                    + "   b.nombre AS bodega "
                    + "FROM facturas_cabeceras f "
                    + "JOIN contactos c ON f.id_contacto = c.id "
                    + "JOIN users u ON f.id_user = u.id "
                    + "LEFT JOIN bodegas b ON b.id = f.id_bodega "
                    + "WHERE f.id = " + id
            );

            try {
                while (rs.next()) {
                    frm.lbl_numerofactura.setText(rs.getString("id_f"));
                    frm.lbl_fecha.setText(rs.getString("fecha"));
                    frm.lbl_id_cliente.setText(rs.getString("id_c"));
                    frm.lbl_nombre_cliente.setText(rs.getString("nombre"));
                    frm.lbl_cedula_cliente.setText(rs.getString("cedula"));
                    frm.lbl_direccion_cliente.setText(rs.getString("direccion"));
                    frm.lbl_celular_cliente.setText(rs.getString("contacto"));
                    frm.lbl_tipo_factura.setText(rs.getString("tipo_factura"));
                    frm.lbl_user.setText(rs.getString("user_name"));
                    frm.lbl_hora.setText(rs.getString("hora"));
                    frm.txt_codigo.setText(rs.getString("codigo"));
                    frm.txt_observaciones.setText(rs.getString("observacion"));
                    frm.txt_observacion_entrega.setText(rs.getString("observacion_entrega"));
                    frm.jbox_bodega.setSelectedItem(rs.getString("bodega"));
                    frm.id_bodega = rs.getInt("id_bodega");
                }

                rs.close();

            } catch (SQLException ex) {
                Logger.getLogger(frm_ver_orden.class.getName()).log(Level.SEVERE, null, ex);
            }

            frm_ver_orden.modelo_productos.setColumnIdentifiers(new Object[]{"id_fac_det", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "ENTREGADO", "SALDO", "PRECIO", "TOTAL"});

            String consulta = "WITH consulta AS (\n"
                    + "  SELECT \n"
                    + "    fd.id,\n"
                    + "    p.id,\n"
                    + "    p.codigo_barras,\n"
                    + "    p.descripcion,\n"
                    + "    fd.cantidad::numeric(18,4)          AS cantidad,\n"
                    + "    fd.subtotal::numeric(18,4)          AS subtotal,\n"
                    + "    (fd.cantidad::numeric(18,4) \n"
                    + "     * fd.subtotal::numeric(18,4))      AS total,\n"
                    + "    COALESCE((\n"
                    + "      SELECT SUM(e.cantidad)::numeric(18,4)\n"
                    + "      FROM entregas_productos e\n"
                    + "      WHERE e.id_factura = fd.id_cabecera\n"
                    + "        AND e.id_producto = p.id\n"
                    + "    ), 0)::numeric(18,4)                 AS entrega\n"
                    + "  FROM productos p\n"
                    + "  INNER JOIN facturas_detalles fd ON fd.id_producto = p.id \n"
                    + "  WHERE fd.id_cabecera = " + id + "\n"
                    + "  GROUP BY fd.id, p.id, p.codigo_barras, p.descripcion, fd.cantidad, fd.subtotal\n"
                    + ")\n"
                    + "SELECT\n"
                    + "  *,\n"
                    + "  CASE \n"
                    + "    WHEN ABS(cantidad - entrega) < 0.00005 THEN 0::numeric(18,4)\n"
                    + "    ELSE ROUND(cantidad - entrega, 2)\n"
                    + "  END AS saldo\n"
                    + "FROM consulta;";
            System.out.println(consulta);
            rs = DB_consultas_R_D.getTabla(consulta);

            try {
                while (rs.next()) {
                    double cantidad = rs.getDouble("cantidad");
                    double entrega = rs.getDouble("entrega");
                    double saldo = rs.getDouble("saldo");

                    frm_ver_orden.modelo_productos.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                        metodos.formateador_decimal_punto_para_decimal().format(cantidad), (entrega), saldo, metodos.formateador_dinero().format(rs.getDouble("subtotal")),
                        metodos.formateador_dinero().format(rs.getDouble("total"))});

                }
                rs.close();
                frm.jtabla_productos.setModel(frm_ver_orden.modelo_productos);
                frm.TamanosTablaProductos();
            } catch (SQLException ex) {
                Logger.getLogger(frm_ver_orden.class.getName()).log(Level.SEVERE, null, ex);
            }

            // MODELO DE PRODUCTOS ENTREGADOS CABECERA
            frm_ver_orden.modelo_entregados_cabecera.setColumnIdentifiers(new Object[]{"Id Entrega", "USER", "FECHA", "HORA", "BODEGA"});

            consulta = "select e.id, u.nombre as usuario, e.fecha_entrega, e.hora_entrega, b.nombre as bodega \n"
                    + "from entregas_productos_cabecera e, users u, bodegas b\n"
                    + "where e.id_user=u.id and e.id_bodega=b.id and e.id_factura=" + id;

            rs = DB_consultas_R_D.getTabla(consulta);

            try {
                while (rs.next()) {
                    frm_ver_orden.modelo_entregados_cabecera.addRow(new Object[]{rs.getString("id"),
                        rs.getString("usuario"), rs.getString("fecha_entrega"), rs.getString("hora_entrega"), rs.getString("bodega")});
                }
                rs.close();
                frm.jtabla_entregados_cabecera.setModel(frm_ver_orden.modelo_entregados_cabecera);
                frm.TamanosTablaEntregadosCabecera();
            } catch (SQLException ex) {
                Logger.getLogger(frm_ver_orden.class.getName()).log(Level.SEVERE, null, ex);
            }

            // BLOQUEAR TODOS LOS CAMPOS PARA SOLO VISUALIZACIÓN
            frm.btn_entregar.setEnabled(false);
            frm.btn_llenar.setEnabled(false);
            frm.txt_observaciones.setEnabled(false);
            frm.jtabla_productos.setEnabled(false);
//            frm.jtabla_entregados_cabecera.setEnabled(false);
            frm.txt_observacion_entrega.setEnabled(false);
            frm.txt_codigo.setEnabled(false);
            frm.jbox_bodega.setEnabled(false);
            frm.btn_verFactura.setEnabled(false);
            frm.btn_verFactura1.setEnabled(false);
            frm.btn_verFactura2.setEnabled(false);

            frm.setVisible(true);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jpop_1 = new javax.swing.JPopupMenu();
        jmenu_VerFactura1 = new javax.swing.JMenuItem();
        jmenu_Eliminar = new javax.swing.JMenuItem();
        jpop_2 = new javax.swing.JPopupMenu();
        jmenu_2 = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();
        txt_Filtro_codigo = new javax.swing.JTextField();

        jmenu_VerFactura1.setText("Ver factura");
        jmenu_VerFactura1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_VerFactura1ActionPerformed(evt);
            }
        });
        jpop_1.add(jmenu_VerFactura1);

        jmenu_Eliminar.setText("Eliminar registro");
        jmenu_Eliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_EliminarActionPerformed(evt);
            }
        });
        jpop_1.add(jmenu_Eliminar);

        jmenu_2.setText("jMenuItem1");
        jpop_2.add(jmenu_2);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Errores en entregas de productos");
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jtabla.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jtabla.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{
                    {},
                    {},
                    {},
                    {}
                },
                new String[]{}
        ));
        jtabla.setRowHeight(35);
        jtabla.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jScrollPane2.setViewportView(jtabla);

        txt_Filtro_codigo.setFont(new java.awt.Font("Tahoma", 0, 28)); // NOI18N
        txt_Filtro_codigo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_Filtro_codigoFocusGained(evt);
            }
        });
        txt_Filtro_codigo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_Filtro_codigoKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1476, Short.MAX_VALUE)
                                        .addComponent(txt_Filtro_codigo))
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(txt_Filtro_codigo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 603, Short.MAX_VALUE)
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
    }// </editor-fold>                        

    private void txt_Filtro_codigoFocusGained(java.awt.event.FocusEvent evt) {

    }

    private void txt_Filtro_codigoKeyTyped(java.awt.event.KeyEvent evt) {

    }

    // MÉTODO PARA EL ACTION DEL MENU ITEM ELIMINAR
    private void jmenu_EliminarActionPerformed(java.awt.event.ActionEvent evt) {
        eliminarRegistro();
    }

    // MÉTODO PARA EL ACTION DEL MENU ITEM VER FACTURA
    private void jmenu_VerFactura1ActionPerformed(java.awt.event.ActionEvent evt) {
        cargar_facturas();
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
            java.util.logging.Logger.getLogger(jd_Errores_entregas.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(jd_Errores_entregas.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(jd_Errores_entregas.class.getName()).log(Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(jd_Errores_entregas.class.getName()).log(Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                jd_Errores_entregas dialog = new jd_Errores_entregas(new javax.swing.JFrame(), true);
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

    // Variables declaration - do not modify                     
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JMenuItem jmenu_2;
    private javax.swing.JMenuItem jmenu_VerFactura1;
    private javax.swing.JMenuItem jmenu_Eliminar;
    private javax.swing.JPopupMenu jpop_1;
    private javax.swing.JPopupMenu jpop_2;
    private javax.swing.JTable jtabla;
    private javax.swing.JTextField txt_Filtro_codigo;
    // End of variables declaration                   
}
