/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios;

import Formularios_internos.jif_crear_producto;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

/**
 *
 * @author Monkeyelgrande
 */
public class frm_productos extends javax.swing.JInternalFrame {

    /**
     * Creates new form frm_clientes
     */
    boolean ver = false;
    DecimalFormat formatDecimal = new DecimalFormat("#");
    TableColumnModel columnModel = null;

    public frm_productos() {
        initComponents();

        actualizar();
        columnModel = jtabla.getColumnModel();
        TamanosTablaAbonos();
        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    btn_verActionPerformed(null);
                }
            }
        });

        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        metodos.EstiloTablaMaterialGlobal(jtabla);

    }

    private void deshabilitarProducto() {
        int fila = jtabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto.");
            return;
        }

        String idProducto = jtabla.getValueAt(fila, 0).toString();
        String descripcion = jtabla.getValueAt(fila, 2).toString();
        String estadoActual = jtabla.getValueAt(fila, 8).toString();

        if ("Deshabilitado".equalsIgnoreCase(estadoActual)) {
            int respH = JOptionPane.showConfirmDialog(this,
                    "Desea habilitar nuevamente el producto?\n\n"
                    + "ID: " + idProducto + "\n"
                    + "Descripcion: " + descripcion,
                    "Confirmar habilitacion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (respH != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                java.sql.Connection con = DB_consultas_R_D.getConexion();
                java.sql.PreparedStatement ps = con.prepareStatement(
                        "UPDATE productos SET estado = true WHERE id = ?");
                ps.setInt(1, Integer.parseInt(idProducto));
                ps.executeUpdate();
                ps.close();
                con.close();

                JOptionPane.showMessageDialog(this,
                        "Producto habilitado correctamente.",
                        "Producto habilitado", JOptionPane.INFORMATION_MESSAGE);

                actualizar();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error al habilitar el producto:\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        // Verificar stock en TODAS las bodegas desde stock_productos
        String sql = "SELECT b.nombre AS bodega, "
                + "COALESCE(sp.cantidad, 0) AS cantidad, "
                + "COALESCE(sp.pendientes, 0) AS pendientes "
                + "FROM bodegas b "
                + "LEFT JOIN stock_productos sp ON sp.id_bodega = b.id "
                + "  AND sp.id_producto = " + idProducto + " "
                + "ORDER BY b.nombre";

        boolean puedeDeshabilitarse = true;
        java.util.List<Object[]> detalleBodegas = new java.util.ArrayList<Object[]>();

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                String bodega = rs.getString("bodega");
                double cantidad = rs.getDouble("cantidad");
                double pendientes = rs.getDouble("pendientes");

                detalleBodegas.add(new Object[]{bodega, cantidad, pendientes});

                if (cantidad != 0 || pendientes != 0) {
                    puedeDeshabilitarse = false;
                }
            }
            rs.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al consultar stock del producto:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!puedeDeshabilitarse) {
            // Mostrar tabla con existencias por bodega
            String[] columnas = {"Bodega", "Cantidad", "Pendientes"};
            Object[][] datos = new Object[detalleBodegas.size()][3];
            for (int i = 0; i < detalleBodegas.size(); i++) {
                Object[] row = detalleBodegas.get(i);
                datos[i][0] = row[0];
                datos[i][1] = row[1];
                datos[i][2] = row[2];
            }

            javax.swing.JTable tablaStock = new javax.swing.JTable(datos, columnas);
            tablaStock.setEnabled(false);
            tablaStock.setFont(new java.awt.Font("Tahoma", 0, 13));
            tablaStock.setRowHeight(24);
            metodos.EstiloTablaMaterialGlobal(tablaStock);

            javax.swing.JScrollPane scrollStock = new javax.swing.JScrollPane(tablaStock);
            scrollStock.setPreferredSize(new java.awt.Dimension(400, Math.min(200, 30 + detalleBodegas.size() * 30)));

            javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
            javax.swing.JLabel lblMsg = new javax.swing.JLabel(
                    "<html><b>No se puede deshabilitar \"" + descripcion + "\"</b><br>"
                    + "El producto tiene existencias o pendientes en las siguientes bodegas:</html>");
            lblMsg.setFont(new java.awt.Font("Tahoma", 0, 13));
            panel.add(lblMsg, java.awt.BorderLayout.NORTH);
            panel.add(scrollStock, java.awt.BorderLayout.CENTER);

            JOptionPane.showMessageDialog(this, panel,
                    "Producto con existencias", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirmar deshabilitacion
        int resp = JOptionPane.showConfirmDialog(this,
                "Esta seguro que desea deshabilitar el producto?\n\n"
                + "ID: " + idProducto + "\n"
                + "Descripcion: " + descripcion + "\n\n"
                + "El producto no aparecera en consultas ni operaciones.\n"
                + "Podra habilitarlo nuevamente editandolo y guardandolo.",
                "Confirmar deshabilitacion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (resp != JOptionPane.YES_OPTION) {
            return;
        }

        // Ejecutar UPDATE estado = false
        try {
            java.sql.Connection con = DB_consultas_R_D.getConexion();
            java.sql.PreparedStatement ps = con.prepareStatement(
                    "UPDATE productos SET estado = false WHERE id = ?");
            ps.setInt(1, Integer.parseInt(idProducto));
            ps.executeUpdate();
            ps.close();
            con.close();

            JOptionPane.showMessageDialog(this,
                    "Producto deshabilitado correctamente.",
                    "Producto deshabilitado", JOptionPane.INFORMATION_MESSAGE);

            actualizar();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al deshabilitar el producto:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void TamanosTablaAbonos() {
        columnModel.getColumn(0).setPreferredWidth(10);    // id
        columnModel.getColumn(1).setPreferredWidth(10);    // codigo
        columnModel.getColumn(2).setPreferredWidth(360);   // descripcion
        columnModel.getColumn(3).setPreferredWidth(80);    // stock minimo
        columnModel.getColumn(4).setPreferredWidth(80);    // stock ideal
        columnModel.getColumn(5).setPreferredWidth(90);    // precio costo
        columnModel.getColumn(6).setPreferredWidth(90);    // precio venta
        columnModel.getColumn(7).setPreferredWidth(70);    // cant. paquete
        columnModel.getColumn(8).setPreferredWidth(80);    // estado
    }
    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lbl_cant_clientes = new javax.swing.JPanel();
        txt_Filtro = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        btn_crear = new javax.swing.JButton();
        btn_eliminar = new javax.swing.JButton();
        btn_editar = new javax.swing.JButton();
        btn_actualizar = new javax.swing.JButton();
        btn_ver = new javax.swing.JButton();
        btn_deshabilitar = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Productos");

        lbl_cant_clientes.setBackground(new java.awt.Color(33, 33, 33));

        txt_Filtro.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        txt_Filtro.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_FiltroFocusGained(evt);
            }
        });

        jtabla.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jtabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jtabla);

        javax.swing.GroupLayout lbl_cant_clientesLayout = new javax.swing.GroupLayout(lbl_cant_clientes);
        lbl_cant_clientes.setLayout(lbl_cant_clientesLayout);
        lbl_cant_clientesLayout.setHorizontalGroup(
            lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                        .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, 464, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 413, Short.MAX_VALUE)))
                .addContainerGap())
        );
        lbl_cant_clientesLayout.setVerticalGroup(
            lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 610, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3.setBackground(new java.awt.Color(33, 33, 33));

        btn_crear.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_crear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/nuevo.png"))); // NOI18N
        btn_crear.setMnemonic('n');
        btn_crear.setText("Nuevo");
        btn_crear.setBorder(null);
        btn_crear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_crearActionPerformed(evt);
            }
        });

        btn_eliminar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_eliminar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/eliminar.png"))); // NOI18N
        btn_eliminar.setMnemonic('d');
        btn_eliminar.setText("Eliminar");
        btn_eliminar.setBorder(null);
        btn_eliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_eliminarActionPerformed(evt);
            }
        });

        btn_editar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_editar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/editar.png"))); // NOI18N
        btn_editar.setMnemonic('e');
        btn_editar.setText("Editar");
        btn_editar.setBorder(null);
        btn_editar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_editarActionPerformed(evt);
            }
        });

        btn_actualizar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_actualizar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/actualizar.png"))); // NOI18N
        btn_actualizar.setText("Actualizar");
        btn_actualizar.setBorder(null);
        btn_actualizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_actualizarActionPerformed(evt);
            }
        });

        btn_ver.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_ver.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/ver.png"))); // NOI18N
        btn_ver.setMnemonic('e');
        btn_ver.setText("Ver");
        btn_ver.setBorder(null);
        btn_ver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_verActionPerformed(evt);
            }
        });

        btn_deshabilitar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_deshabilitar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Pause Squared.png"))); // NOI18N
        btn_deshabilitar.setMnemonic('n');
        btn_deshabilitar.setText("Deshabilitar");
        btn_deshabilitar.setBorder(null);
        btn_deshabilitar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_deshabilitarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_deshabilitar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btn_ver, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btn_actualizar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_crear, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_eliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_editar, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_crear)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_eliminar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_editar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_actualizar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_ver)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_deshabilitar)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(0, 116, 214));
        jPanel4.setPreferredSize(new java.awt.Dimension(146, 80));

        jLabel13.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("Productos");

        jButton2.setBackground(new java.awt.Color(102, 0, 0));
        jButton2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jButton2.setForeground(new java.awt.Color(255, 255, 255));
        jButton2.setMnemonic('w');
        jButton2.setText("Cerrar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton2)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton2)
                    .addComponent(jLabel13))
                .addGap(26, 26, 26))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 1056, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_cant_clientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_cant_clientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_crearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_crearActionPerformed
        jif_crear_producto frm = new jif_crear_producto();
        frm.formulario = "crear";

        frm.show();
        jif_crear_producto.txt_cod_barras.requestFocus();
    }//GEN-LAST:event_btn_crearActionPerformed

    public int consultarId(String id) {
        ResultSet rs = DB_consultas_R_D.getTabla("select count(id) as id from clientes where id = " + id + "");
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return Integer.parseInt(rs.getString("id"));
            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla.setModel(modelo);
        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }
    private void btn_eliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_eliminarActionPerformed
        int fila = jtabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar este producto?", "Alerta", dialogButton);
            if (dialogResult == JOptionPane.YES_OPTION) {

                try {
                    DefaultTableModel modelo = (DefaultTableModel) jtabla.getModel();
                    String id = (String) jtabla.getValueAt(fila, 0);//suponiendo que el id lo muestras en la primera columna
                    DB_consultas_R_D.eliminar("productos", id);
                    for (int i = 0; i < modelo.getRowCount(); i++) {
                        if (modelo.getValueAt(i, 0).equals(id)) {
                            modelo.removeRow(i);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }//GEN-LAST:event_btn_eliminarActionPerformed

    public void validar_numeros(java.awt.event.KeyEvent evt, char car) {
        if ((car < '0' || car > '9')) {
            evt.consume();
        }
    }

    private void btn_editarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_editarActionPerformed
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
        } else {
            String id = (String) jtabla.getValueAt(fila, 0);
            ResultSet rs = DB_consultas_R_D.getTabla("select p.id,codigo_barras,descripcion,stock_minimo,stock_ideal,id_unidad,p.cant_paquete, p.precio_costo, p.precio_venta, p.precio_venta2, p.precio_venta3, "
                    + "id_padre, p.tipo, COALESCE((select descripcion from productos where id=p.id_padre),'-')  as descripcion_padre, u.nombre as nombre_unidad "
                    + "from productos p, unidades_medidas u "
                    + "where p.id_unidad=u.id and p.id =" + id);
            jif_crear_producto frm = new jif_crear_producto();
            try {
                while (rs.next()) {
                    jif_crear_producto.txt_id.setText(rs.getString("id"));
                    jif_crear_producto.txt_cod_barras.setText(rs.getString("codigo_barras"));
                    jif_crear_producto.jtxt_descripcion.setText(rs.getString("descripcion"));
                    jif_crear_producto.txt_stock_minimo.setText(rs.getString("stock_minimo"));
                    jif_crear_producto.txt_stock_ideal.setText(rs.getString("stock_ideal"));
                    jif_crear_producto.jbox_unidad.setSelectedItem(rs.getString("nombre_unidad"));
                    jif_crear_producto.lbl_producto_padre.setText(rs.getString("descripcion_padre"));
                    jif_crear_producto.lbl_id_producto_padre.setText(rs.getString("id_padre"));
                    jif_crear_producto.txt_cant_paquete.setText(rs.getString("cant_paquete"));
                    jif_crear_producto.txt_pcosto.setText(Metodos.metodos.formateador_dinero().format(rs.getDouble("precio_costo")));
                    jif_crear_producto.txt_pventa.setText(Metodos.metodos.formateador_dinero().format(rs.getDouble("precio_venta")));
                    jif_crear_producto.txt_pventa2.setText(Metodos.metodos.formateador_dinero().format(rs.getDouble("precio_venta2")));
                    jif_crear_producto.txt_pventa3.setText(Metodos.metodos.formateador_dinero().format(rs.getDouble("precio_venta3")));
                    jif_crear_producto.id_unidad = rs.getInt("id_unidad");
                    jif_crear_producto.jbox_tipo.setSelectedIndex(rs.getInt("tipo"));

                }
                rs.close();
            } catch (SQLException ex) {
                Logger.getLogger(frm_productos.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm.txt_cod_barras.setEnabled(false);
            frm.btn_e_cod_barras.setVisible(true);

            if (ver) {
                frm.btn_e_cod_barras.setVisible(false);
                frm.txt_cod_barras.setEnabled(false);
                frm.txt_stock_minimo.setEnabled(false);
                frm.txt_stock_ideal.setEnabled(false);
                frm.txt_pcosto.setEnabled(false);
                frm.txt_pventa.setEnabled(false);
                frm.txt_pventa2.setEnabled(false);
                frm.txt_pventa3.setEnabled(false);
                frm.jtxt_descripcion.setEnabled(false);
                frm.jbox_unidad.setEnabled(false);
                frm.jbox_tipo.setEnabled(false);
                frm.btn_guardar.setEnabled(false);
                frm.btn_limpiar.setEnabled(false);
                frm.chk_cerrar.setEnabled(false);
                frm.btn_editar.setVisible(true);

                // Cargar kardex de movimientos del producto
                frm.cargarKardex(id);
            }
            frm.show();
            jif_crear_producto.txt_cod_barras.requestFocus();
            ver = false;
        }

    }//GEN-LAST:event_btn_editarActionPerformed

    private void btn_actualizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_actualizarActionPerformed
        actualizar();
    }//GEN-LAST:event_btn_actualizarActionPerformed

    private void btn_verActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_verActionPerformed
        ver = true;
        btn_editarActionPerformed(evt);


    }//GEN-LAST:event_btn_verActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void btn_deshabilitarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_deshabilitarActionPerformed
        deshabilitarProducto();

    }//GEN-LAST:event_btn_deshabilitarActionPerformed

    private void txt_FiltroFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_FiltroFocusGained
        btn_actualizar.doClick();
    }//GEN-LAST:event_txt_FiltroFocusGained

    public void actualizar() {
        try {
            for (int i = 0; i < jtabla.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        ResultSet rs = DB_consultas_R_D.getTabla("select p.id, codigo_barras, descripcion, stock_minimo, stock_ideal, "
                + "COALESCE(precio_costo, 0) as precio_costo, COALESCE(precio_venta, 0) as precio_venta, "
                + "COALESCE(cant_paquete, 0) as cant_paquete, COALESCE(estado, true) as estado "
                + "from productos p order by p.id");
        modelo.setColumnIdentifiers(new Object[]{"id", "Codigo barras", "Descripci\u00f3n", "Stock minimo", "Stock ideal",
            "Precio costo", "Precio venta", "Cant. paquete", "Estado"});
        try {
            while (rs.next()) {
                // a\u00f1ade los resultado a al modelo de tabla
                String estado = rs.getBoolean("estado") ? "Habilitado" : "Deshabilitado";
                modelo.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                    formatDecimal.format(rs.getDouble("stock_minimo")), formatDecimal.format(rs.getDouble("stock_ideal")),
                    Metodos.metodos.formateador_dinero().format(rs.getDouble("precio_costo")),
                    Metodos.metodos.formateador_dinero().format(rs.getDouble("precio_venta")),
                    formatDecimal.format(rs.getDouble("cant_paquete")), estado});
            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla.setModel(modelo);
            TamanosTablaAbonos();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void cargarFiltros(String consulta, JComboBox jbox) {
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        DefaultComboBoxModel modelosmarca = new DefaultComboBoxModel();

        modelosmarca.addElement("");
        try {
            while (rs.next()) {
                modelosmarca.addElement(rs.getString("nombre"));
            }
            rs.close();
            jbox.setModel(modelosmarca);
        } catch (Exception e) {
            System.out.println(e);
        }
        AutoCompleteDecorator.decorate(jbox);
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
            java.util.logging.Logger.getLogger(frm_productos.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(frm_productos.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(frm_productos.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(frm_productos.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new frm_productos().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public static javax.swing.JButton btn_actualizar;
    private javax.swing.JButton btn_crear;
    private javax.swing.JButton btn_deshabilitar;
    private javax.swing.JButton btn_editar;
    private javax.swing.JButton btn_eliminar;
    private javax.swing.JButton btn_ver;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    public javax.swing.JTable jtabla;
    private javax.swing.JPanel lbl_cant_clientes;
    private javax.swing.JTextField txt_Filtro;
    // End of variables declaration//GEN-END:variables
}
