/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios_internos;

import Formularios.frm_tipo_ingresos;
import Metodos.metodos;
import conexiondb.DB_Tipos_ingresos_mercancia;
import conexiondb.DB_consultas_R_D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Tipo_ingresos_mercancia;
import Formularios.frm_main;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Monkeyelgrande
 */
public class jif_importar_ingreso_productos extends javax.swing.JDialog {

    /**
     * Creates new form jif_crear_marca
     */
    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    DefaultTableModel modeloDetalle = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    private static String database_name;
    public static String ip;

    public static String url = "";
    public static String usuario = "postgres";
    public static String contrasenia = "monkey";
    TableColumnModel columnModel = null;
    TableColumnModel columnModelDetalle = null;

    public jif_importar_ingreso_productos() {
        initComponents();
        this.setLocationRelativeTo(this);
        metodos.addEscapeListenerWindowDialog(this);
        columnModel = jtabla.getColumnModel();
        columnModelDetalle = jtabla_detalle.getColumnModel();

//        TamanosTabla();
        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        poner_fechas();
        agregarListenerTabla();

    }

    private void agregarListenerTabla() {
        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int fila = jtabla.getSelectedRow();
                if (fila >= 0) {
                    // Obtener el ID del ingreso de la fila seleccionada
                    String idIngreso = jtabla.getValueAt(fila, 0).toString();
                    cargarDetalle(idIngreso);
                }
            }
        });
    }

    public void poner_fechas() {
        try {
            String fecha1 = DB_consultas_R_D.obtener_fecha_dia1();
            Date date1 = new SimpleDateFormat("yyyy-MM-dd").parse(fecha1);
            jdate_fecha1.setDate(date1);

            String fecha2 = DB_consultas_R_D.obtener_fecha_dia_ultimo();
            Date date2 = new SimpleDateFormat("yyyy-MM-dd").parse(fecha2);
            jdate_fecha2.setDate(date2);
        } catch (Exception e) {
        }
    }

    public void cargarDetalle(String idIngreso) {
        modeloDetalle.setRowCount(0);

        String consulta = "SELECT p.codigo_barras, p.descripcion, d.cantidad, d.precio_costo, "
                + "d.iva, d.venta, d.descuento, "
                + "(d.cantidad * d.precio_costo) as total "
                + "FROM ingresos_productos_detalle d "
                + "INNER JOIN productos p ON d.id_producto = p.id "
                + "WHERE d.id_ingreso_cabecera = " + idIngreso + " "
                + "ORDER BY d.id";

        modeloDetalle.setColumnIdentifiers(new Object[]{
            "Código", "Descripción", "Cantidad", "Precio Costo",
            "IVA", "Precio Venta", "Descuento", "Total"
        });

        ResultSet rs = getTabla(consulta);
        try {
            while (rs.next()) {
                modeloDetalle.addRow(new Object[]{
                    rs.getString("codigo_barras"),
                    rs.getString("descripcion"),
                    metodos.formateador_dinero().format(rs.getDouble("cantidad")),
                    metodos.formateador_dinero().format(rs.getDouble("precio_costo")),
                    metodos.formateador_dinero().format(rs.getDouble("iva")),
                    metodos.formateador_dinero().format(rs.getDouble("venta")),
                    metodos.formateador_dinero().format(rs.getDouble("descuento")),
                    metodos.formateador_dinero().format(rs.getDouble("total"))
                });
            }
            rs.close();
            jtabla_detalle.setModel(modeloDetalle);
            TamanosTablaDetalle();
        } catch (Exception e) {
            System.out.println("Error al cargar detalle: " + e);
            JOptionPane.showMessageDialog(null, "Error al cargar detalle: " + e.getMessage());
        }
    }

    public void TamanosTablaDetalle() {
        columnModelDetalle.getColumn(0).setPreferredWidth(100);  // Código
        columnModelDetalle.getColumn(1).setPreferredWidth(300);  // Descripción
        columnModelDetalle.getColumn(2).setPreferredWidth(80);   // Cantidad
        columnModelDetalle.getColumn(3).setPreferredWidth(100);  // Precio Costo
        columnModelDetalle.getColumn(4).setPreferredWidth(60);   // IVA
        columnModelDetalle.getColumn(5).setPreferredWidth(100);  // Precio Venta
        columnModelDetalle.getColumn(6).setPreferredWidth(80);   // Descuento
        columnModelDetalle.getColumn(7).setPreferredWidth(100);  // Total
    }

    // Método para importar los productos al formulario de ingreso de mercancía
    private void importarProductosAIngreso() {
        // Validar que haya una fila seleccionada en jtabla
        int filaSeleccionada = jtabla.getSelectedRow();
        if (filaSeleccionada < 0) {
            JOptionPane.showMessageDialog(this, "Por favor seleccione un ingreso de la tabla principal",
                    "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validar que haya productos en la tabla detalle
        if (jtabla_detalle.getRowCount() < 1) {
            JOptionPane.showMessageDialog(this, "No hay productos para importar en este ingreso",
                    "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Obtener el número de factura de la fila seleccionada
            String numeroFactura = jtabla.getValueAt(filaSeleccionada, 2).toString();

            // Asignar el número de factura al formulario de ingreso
            jif_crear_ingreso_mercancia.txt_no_factura.setText(numeroFactura);

            // Limpiar la tabla de productos del formulario de ingreso antes de importar
            int confirmar = JOptionPane.showConfirmDialog(this,
                    "¿Desea limpiar la tabla actual antes de importar?\n"
                    + "Si selecciona 'No', los productos se agregarán a los existentes.",
                    "Confirmar importación",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (confirmar == JOptionPane.CANCEL_OPTION) {
                return; // Cancelar la operación
            }

            if (confirmar == JOptionPane.YES_OPTION) {
                // Limpiar la tabla
                for (int i = jif_crear_ingreso_mercancia.modelo_productos.getRowCount() - 1; i >= 0; i--) {
                    jif_crear_ingreso_mercancia.modelo_productos.removeRow(i);
                }
            }

            // Recorrer todos los productos de jtabla_detalle
            int productosImportados = 0;
            for (int i = 0; i < jtabla_detalle.getRowCount(); i++) {
                String codigoBarras = jtabla_detalle.getValueAt(i, 0).toString();
                double cantidad = Double.parseDouble(metodos.EliminaCaracteres(jtabla_detalle.getValueAt(i, 2).toString(), "."));
                double precioCosto = Double.parseDouble(metodos.EliminaCaracteres(jtabla_detalle.getValueAt(i, 3).toString(), "."));

                // Consultar datos completos del producto desde la base de datos
                String consulta = "SELECT p.id, p.codigo_barras, p.descripcion, p.precio_costo, p.precio_venta, "
                        + "COALESCE(sp.stock, 0) as stock "
                        + "FROM productos p "
                        + "LEFT JOIN (SELECT id_producto, SUM(cantidad) as stock FROM stock_productos GROUP BY id_producto) sp ON sp.id_producto = p.id "
                        + "WHERE p.codigo_barras = '" + codigoBarras + "' AND COALESCE(p.estado, true) = true";

                ResultSet rs = DB_consultas_R_D.getTabla(consulta);

                if (rs.next()) {
                    String id = rs.getString("id");
                    String descripcion = rs.getString("descripcion");
                    double stock = rs.getDouble("stock");
                    double precioVenta = rs.getDouble("precio_venta");

                    // Agregar el producto a la tabla según el modo (con o sin dinero)
                    jif_crear_ingreso_mercancia.modelo_productos.addRow(new Object[]{
                        id,
                        codigoBarras,
                        descripcion,
                        cantidad,
                        stock,
                        null,
                        metodos.formateador_dinero().format(precioCosto),
                        metodos.formateador_dinero().format(precioVenta)
                    });

                    productosImportados++;
                }
                rs.close();
            }

            // Actualizar la tabla en el formulario de ingreso
            jif_crear_ingreso_mercancia.jtabla.setModel(jif_crear_ingreso_mercancia.modelo_productos);
            jif_crear_ingreso_mercancia.TamanosTabla();
            jif_crear_ingreso_mercancia.calcular_total();

            JOptionPane.showMessageDialog(this,
                    "Se importaron " + productosImportados + " producto(s) correctamente",
                    "Importación exitosa",
                    JOptionPane.INFORMATION_MESSAGE);

            // Cerrar este diálogo
            this.dispose();

        } catch (SQLException ex) {
            Logger.getLogger(jif_importar_ingreso_productos.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this,
                    "Error al importar productos: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            Logger.getLogger(jif_importar_ingreso_productos.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this,
                    "Error inesperado: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        lbl_cant_clientes = new javax.swing.JPanel();
        txt_Filtro = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla = new org.jdesktop.swingx.JXTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtabla_detalle = new org.jdesktop.swingx.JXTable();
        btn_importar_a_ingreso = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        btn_consulta = new javax.swing.JButton();
        txt_base_de_datos = new javax.swing.JTextField();
        jdate_fecha1 = new com.toedter.calendar.JDateChooser();
        lbl_titulo1 = new javax.swing.JLabel();
        jdate_fecha2 = new com.toedter.calendar.JDateChooser();

        setModal(true);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setEnabled(false);

        lbl_cant_clientes.setBackground(new java.awt.Color(33, 33, 33));

        txt_Filtro.setFont(new java.awt.Font("Yu Gothic Medium", 1, 24)); // NOI18N
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
        jtabla.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jtabla.setRowHeight(25);
        jtabla.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jScrollPane2.setViewportView(jtabla);

        jtabla_detalle.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_detalle.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jtabla_detalle.setRowHeight(25);
        jtabla_detalle.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jScrollPane3.setViewportView(jtabla_detalle);

        btn_importar_a_ingreso.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_importar_a_ingreso.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Invoice.png"))); // NOI18N
        btn_importar_a_ingreso.setText("Importar");
        btn_importar_a_ingreso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_importar_a_ingresoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout lbl_cant_clientesLayout = new javax.swing.GroupLayout(lbl_cant_clientes);
        lbl_cant_clientes.setLayout(lbl_cant_clientesLayout);
        lbl_cant_clientesLayout.setHorizontalGroup(
            lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane2)
                    .addComponent(txt_Filtro, javax.swing.GroupLayout.DEFAULT_SIZE, 661, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lbl_cant_clientesLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btn_importar_a_ingreso, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        lbl_cant_clientesLayout.setVerticalGroup(
            lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_importar_a_ingreso, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 541, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel3.setBackground(new java.awt.Color(33, 33, 33));

        btn_consulta.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_consulta.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/bucar.png"))); // NOI18N
        btn_consulta.setMnemonic('n');
        btn_consulta.setText("Consulta");
        btn_consulta.setBorder(null);
        btn_consulta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_consultaActionPerformed(evt);
            }
        });

        txt_base_de_datos.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_base_de_datos.setText("productos");

        jdate_fecha1.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N

        lbl_titulo1.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        lbl_titulo1.setForeground(new java.awt.Color(255, 255, 255));
        lbl_titulo1.setText("A");

        jdate_fecha2.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txt_base_de_datos, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jdate_fecha1, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_titulo1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jdate_fecha2, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 722, Short.MAX_VALUE)
                .addComponent(btn_consulta, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btn_consulta, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jdate_fecha2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jdate_fecha1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txt_base_de_datos)
                            .addComponent(lbl_titulo1))))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lbl_cant_clientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_cant_clientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1419, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 659, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txt_FiltroKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_FiltroKeyTyped

    }//GEN-LAST:event_txt_FiltroKeyTyped

    private void btn_consultaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_consultaActionPerformed
        consultar();
    }//GEN-LAST:event_btn_consultaActionPerformed

    private void btn_importar_a_ingresoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_importar_a_ingresoActionPerformed
        importarProductosAIngreso();

    }//GEN-LAST:event_btn_importar_a_ingresoActionPerformed
    public void limpiar() {

    }

    public void consultar() {
        database_name = txt_base_de_datos.getText();
        String fecha1 = "";
        String fecha2 = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        fecha1 = sdf.format(jdate_fecha1.getDate());
        fecha2 = sdf.format(jdate_fecha2.getDate());
        modelo.setRowCount(0);

        String consulta = "select i.id, p.nombre as proveedor, i.fecha, i.fecha_vencimiento, i.no_factura, i.estado, u.nombre as usuario  "
                + "from ingresos_productos_cabecera i, contactos p, users u "
                + "where i.id_proveedor=p.id and i.id_user=u.id and i.fecha between '" + fecha1 + "' and '" + fecha2 + "' "
                + "order by fecha desc, i.id desc";

        modelo.setColumnIdentifiers(new Object[]{"id", "proveedor", "Numero factura", "Fecha", "Estado", "Usuario"});

//        System.out.println(consulta);
        ResultSet rs = getTabla(consulta);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
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

                modelo.addRow(new Object[]{rs.getString("id"), rs.getString("proveedor"), rs.getString("no_factura"), rs.getDate("fecha"), estado, rs.getString("usuario")});

            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla.setModel(modelo);
            TamanosTabla();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void TamanosTabla() {
        columnModel.getColumn(0).setPreferredWidth(20);
        columnModel.getColumn(1).setPreferredWidth(300);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(3).setPreferredWidth(100);

    }

    public static ResultSet getTabla(String Consulta) {
        Connection cn = getConexion();
        Statement st;
        ResultSet datos = null;
        try {
            st = cn.createStatement();
            datos = st.executeQuery(Consulta);
            cn.close();
        } catch (Exception e) {
            System.out.print(e.toString());
        }
        return datos;
    }

    private static Connection getConexion() {
        database_name = txt_base_de_datos.getText();

        try {

            consulta_database_ip(new File("").getAbsolutePath() + "/src/ip.txt");
            url = "jdbc:postgresql://" + ip + ":5432/" + database_name;
//            System.out.println(url);
        } catch (Exception e) {
            System.out.println("Error en getConexion: " + e);
        }

        Connection cn = null;
        try {
            Class.forName("org.postgresql.Driver");
            cn = DriverManager.getConnection(url, usuario, contrasenia);
        } catch (Exception e) {
            System.out.println(String.valueOf(e));
            JOptionPane.showMessageDialog(null, "Error de conexxion a la base de datos:\n " + e);
        }
        return cn;
    }

    public static void consulta_database_ip(String archivo) throws FileNotFoundException, IOException {
        String ip_consultada;
        FileReader f = new FileReader(archivo);
        BufferedReader b = new BufferedReader(f);
        while ((ip_consultada = b.readLine()) != null) {
            ip = ip_consultada;
        }
        b.close();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public static javax.swing.JButton btn_consulta;
    public static javax.swing.JButton btn_importar_a_ingreso;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private com.toedter.calendar.JDateChooser jdate_fecha1;
    private com.toedter.calendar.JDateChooser jdate_fecha2;
    private org.jdesktop.swingx.JXTable jtabla;
    private org.jdesktop.swingx.JXTable jtabla_detalle;
    private javax.swing.JPanel lbl_cant_clientes;
    private javax.swing.JLabel lbl_titulo1;
    private javax.swing.JTextField txt_Filtro;
    private static javax.swing.JTextField txt_base_de_datos;
    // End of variables declaration//GEN-END:variables
}
