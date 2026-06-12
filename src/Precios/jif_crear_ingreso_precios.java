package Precios;

import Formularios.frm_main;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBingresosPrecios;
import conexiondb.DBpreciosProductos;
import conexiondb.ReplicaIngresoOrden;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Contactos;
import modelos.IngresosProductos;

/**
 * Creacion/edicion de ingresos del modulo Precios (portado del
 * jif_crear_ingreso_productos de productos-agroinsumos).
 *
 * Flujo por roles (users.rol_precios):
 *   2 = captura cantidades (ex perfil Bodega)   -> estado 0 (Recibido)
 *   3 = captura costos/IVA (ex Contadora)       -> estado 1 (Ingresado) y
 *       actualiza productos.precio_costo
 *   4 = fija precios de venta (ex Precios)      -> estado 2 (Precios) y
 *       actualiza venta/desc1/desc2/S&T/credito del producto
 *
 * El boton "Enviar a bodega" (rol 2) crea ingresos de mercancia PENDIENTES en
 * el modulo clasico de controlbodega via ReplicaIngresoOrden (puente local).
 *
 * @author Monkeyelgrande
 */
public class jif_crear_ingreso_precios extends javax.swing.JDialog {

    public static DefaultTableModel modelo_productos = null;

    // ---- Puente con ingresos de mercancia (rol 2) ----
    // Indice de la columna BODEGA en la tabla del rol 2.
    public static final int COL_BODEGA = 5;
    // Lista de bodegas de controlbodega.
    public static java.util.List<Bodega> BODEGAS = null;
    // Selector general "aplicar a todos".
    public static javax.swing.JComboBox<Bodega> cmb_bodega_general = null;
    // true cuando rol_precios==2 y hay bodegas (el puente ahora es local: siempre activo).
    public static boolean replicaBodegaActiva = false;
    // Boton "Enviar a bodega".
    public static javax.swing.JButton btn_enviar_bodega = null;
    // true cuando el guardado en curso debe ademas enviar el ingreso a bodega.
    private boolean enviarTrasGuardar = false;

    /** Bodega de controlbodega (id + nombre). */
    public static class Bodega {

        public final int id;
        public final String nombre;

        public Bodega(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    // true = ingreso nuevo; false = editando uno existente.
    public static boolean es_nuevo = true;

    public jif_crear_ingreso_precios() {
        initComponents();
        inicializar_modelo();
        configurarColumnasYBodega();
        btn_calcular_utildiad_porcentaje.setVisible(false);
        // La creacion de productos se hace en el modulo Productos de
        // controlbodega (tabla unificada).
        btn_crear_producto.setVisible(false);

        Contactos proveedor = new Contactos();
        proveedor.MostrarNombreProveedores(jbox_proveedor);
        proveedor.MostrarNombreContactos(jbox_transportador);

        lbl_id.setText(DB_consultas_R_D.cargarId("ingresos_productos_cabecera"));
        es_nuevo = true;
        Calendar fecha = new GregorianCalendar();
        jdate_fecha_entrada.setCalendar(fecha);
        this.setLocationRelativeTo(this);
        metodos.addEscapeListenerWindowDialog(this);
        limpiar();

        jtabla.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_DELETE) {
                    quitar_productos(modelo_productos);
                }
            }
        });

        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    int fila = jtabla.getSelectedRow();

                    if (jtabla.getSelectedColumn() == 1) {
                        metodos.dinero_a_porta_papeles(jtabla.getValueAt(fila, 1).toString());
                    } else {
                        // Calcular solo la fila seleccionada (rol Precios)
                        if (frm_main.rol_precios == 4) {

                            ResultSet rs = DB_consultas_R_D.getTabla("select * from configuraciones where id = 1");
                            try {
                                while (rs.next()) {
                                    porcentaje_s_y_t = rs.getDouble("porcentaje_s_y_t");
                                    porcentaje_credito = rs.getDouble("porcentaje_credito");
                                }
                                rs.close();
                            } catch (SQLException ex) {
                                System.out.println(ex);
                            }

                            try {
                                double costo_iva_descuento,
                                        costo_iva_descuento_gasto,
                                        porcentaje_utilidad = 0,
                                        venta,
                                        valor_desc_1,
                                        valor_desc_2,
                                        valor_s_y_t,
                                        valor_credito;

                                try {
                                    porcentaje_utilidad = Double.parseDouble(
                                            modelo_productos.getValueAt(fila, 9).toString());
                                } catch (Exception e) {
                                    porcentaje_utilidad = 0;
                                }

                                costo_iva_descuento = Double.parseDouble(
                                        metodos.EliminaCaracteres(modelo_productos.getValueAt(fila, 7).toString(), "."));
                                costo_iva_descuento_gasto = Double.parseDouble(
                                        metodos.EliminaCaracteres(modelo_productos.getValueAt(fila, 8).toString(), "."));

                                venta = (costo_iva_descuento_gasto / ((100 - porcentaje_utilidad) / 100));

                                valor_desc_1 = calcularDescuento(venta, costo_iva_descuento_gasto, 1);
                                valor_desc_2 = calcularDescuento(venta, costo_iva_descuento_gasto, 2);

                                valor_s_y_t = costo_iva_descuento / (porcentaje_s_y_t);
                                valor_credito = venta + (venta * (porcentaje_credito / 100));

                                int redondear = Integer.parseInt(txt_redondear.getText());

                                modelo_productos.setValueAt(metodos.formateador_dinero().format(
                                        metodos.redondearNumero(venta, redondear)), fila, 10);
                                modelo_productos.setValueAt(metodos.formateador_dinero().format(
                                        metodos.redondearNumero(valor_desc_1, redondear)), fila, 11);
                                modelo_productos.setValueAt(metodos.formateador_dinero().format(
                                        metodos.redondearNumero(valor_desc_2, redondear)), fila, 12);
                                modelo_productos.setValueAt(metodos.formateador_dinero().format(
                                        metodos.redondearNumero(valor_s_y_t, redondear)), fila, 13);
                                modelo_productos.setValueAt(metodos.formateador_dinero().format(
                                        metodos.redondearNumero(valor_credito, redondear)), fila, 14);

                            } catch (Exception e) {
                                System.out.println(e);
                                modelo_productos.setValueAt("0", fila, 6);
                            }
                        }
                    }
                }
            }
        });
    }

    public static void inicializar_modelo() {

        switch (frm_main.rol_precios) {
            case 2:
                modelo_productos = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int fila, int columna) {
                        if (columna == 3) { // cantidad
                            return true;
                        }
                        if (columna == 4) { // etiquetas
                            return true;
                        }
                        if (columna == 5) { // BODEGA
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void setValueAt(Object aValue, int row, int col) {
                        super.setValueAt(aValue, row, col);
                        calcular_total();
                        fireTableDataChanged();
                    }
                };
                break;
            case 3:
                modelo_productos = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int fila, int columna) {
                        return columna == 5 || columna == 6 || columna == 7;
                    }

                    @Override
                    public void setValueAt(Object aValue, int row, int col) {
                        super.setValueAt(aValue, row, col);
                        calcular_total();
                        fireTableDataChanged();
                    }
                };
                break;
            default: // 4 (y admin)
                modelo_productos = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int fila, int columna) {
                        return columna >= 9 && columna <= 14;
                    }

                    @Override
                    public void setValueAt(Object aValue, int row, int col) {
                        super.setValueAt(aValue, row, col);
                        calcular_total();
                        fireTableDataChanged();
                    }
                };
                break;
        }
    }
    public static double total_items = 0, subtotal = 0, iva_global = 0, descuento_global = 0, total_global = 0;

    public static void calcular_total() {

        try {
            double por_iva = 0, por_descuento = 0;
            switch (frm_main.rol_precios) {
                case 3:

                    total_items = 0;
                    subtotal = 0;
                    iva_global = 0;
                    descuento_global = 0;
                    total_global = 0;

                    for (int i = 0; i < jif_crear_ingreso_precios.modelo_productos.getRowCount(); i++) {
                        por_descuento = (Double.parseDouble(jif_crear_ingreso_precios.modelo_productos.getValueAt(i, 7).toString()) / 100);
                        por_iva = (Double.parseDouble(jif_crear_ingreso_precios.modelo_productos.getValueAt(i, 6).toString()) / 100);

                        total_items += Double.parseDouble(jif_crear_ingreso_precios.modelo_productos.getValueAt(i, 3).toString());
                        try {

                            subtotal += Double.parseDouble(metodos.EliminaCaracteres(jif_crear_ingreso_precios.modelo_productos.getValueAt(i, 5).toString(), ".")) * Double.parseDouble(jif_crear_ingreso_precios.modelo_productos.getValueAt(i, 3).toString());

                            descuento_global += ((Double.parseDouble(metodos.EliminaCaracteres(jif_crear_ingreso_precios.modelo_productos.getValueAt(i, 5).toString(), ".")) * por_descuento))
                                    * Double.parseDouble(jif_crear_ingreso_precios.modelo_productos.getValueAt(i, 3).toString());

                            iva_global += ((Double.parseDouble(metodos.EliminaCaracteres(jif_crear_ingreso_precios.modelo_productos.getValueAt(i, 5).toString(), ".")) * Double.parseDouble(jif_crear_ingreso_precios.modelo_productos.getValueAt(i, 3).toString()))
                                    - ((Double.parseDouble(metodos.EliminaCaracteres(jif_crear_ingreso_precios.modelo_productos.getValueAt(i, 5).toString(), ".")) * Double.parseDouble(jif_crear_ingreso_precios.modelo_productos.getValueAt(i, 3).toString()) * por_descuento))) * por_iva;

                        } catch (Exception e) {
                            subtotal = 0;
                        }

                        try {
                            total_global += Double.parseDouble(metodos.EliminaCaracteres(jif_crear_ingreso_precios.modelo_productos.getValueAt(i, 9).toString(), "."));
                        } catch (Exception e) {
                            total_global = 0;
                        }
                    }
                    lbl_total_items.setText((total_items) + "");
                    lbl_sub_total.setText(metodos.formateador_dinero().format(subtotal));
                    lbl_iva.setText(metodos.formateador_dinero().format(iva_global));
                    lbl_descuento.setText(metodos.formateador_dinero().format(descuento_global));
                    lbl_total_factura.setText(metodos.formateador_dinero().format(total_global));
                    break;
                case 4:
                    break;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(jtabla, "Por favor verifique los precios y cantidades en la tabla\n" + e);
        }
    }

    public void quitar_productos(DefaultTableModel model) {
        if (model.getRowCount() > 0) {

            int fila = this.jtabla.getSelectedRow();
            if (this.jtabla.getSelectedRowCount() < 1) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro");
            } else {
                model.removeRow(fila);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        pnl_ingreso = new javax.swing.JPanel();
        btn_limpiar = new javax.swing.JButton();
        btn_guardar = new javax.swing.JButton();
        chk_cerrar = new javax.swing.JCheckBox();
        jLabel27 = new javax.swing.JLabel();
        lbl_total_items = new javax.swing.JLabel();
        btn_imprimir = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txt_codigo_barras = new javax.swing.JTextField();
        btn_buscar = new javax.swing.JButton();
        btn_crear_producto = new javax.swing.JButton();
        btn_calcular_costos = new javax.swing.JButton();
        btn_precargar = new javax.swing.JButton();
        btn_Exportar_Worold = new javax.swing.JButton();
        btn_calcular_precios_venta = new javax.swing.JButton();
        btn_calcular_utildiad_porcentaje = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jbox_proveedor = new javax.swing.JComboBox<>();
        jLabel20 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jdate_fecha_entrada = new com.toedter.calendar.JDateChooser();
        lbl_id = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        txt_no_factura = new javax.swing.JTextField();
        jbox_transportador = new javax.swing.JComboBox<>();
        jLabel22 = new javax.swing.JLabel();
        txt_redondear = new javax.swing.JTextField();
        jLabel32 = new javax.swing.JLabel();
        btn_buscar_proveedor = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();
        jpanel_dinero = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txt_observacion = new javax.swing.JTextArea();
        jpanel_dinero1 = new javax.swing.JPanel();
        jLabel28 = new javax.swing.JLabel();
        lbl_sub_total = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        lbl_iva = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        lbl_total_factura = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        lbl_descuento = new javax.swing.JLabel();

        setTitle("Ingreso de productos (Precios)");
        setModal(true);

        pnl_ingreso.setBackground(new java.awt.Color(255, 255, 255));

        btn_limpiar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_limpiar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/limpiar.png"))); // NOI18N
        btn_limpiar.setMnemonic('l');
        btn_limpiar.setText("Limpiar");
        btn_limpiar.setBorder(null);
        btn_limpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_limpiarActionPerformed(evt);
            }
        });

        btn_guardar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_guardar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/guardar.png"))); // NOI18N
        btn_guardar.setMnemonic('g');
        btn_guardar.setText("Guardar");
        btn_guardar.setBorder(null);
        btn_guardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_guardarActionPerformed(evt);
            }
        });

        chk_cerrar.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        chk_cerrar.setForeground(new java.awt.Color(51, 51, 51));
        chk_cerrar.setSelected(true);
        chk_cerrar.setText("Cerrar formulario al guardar");

        jLabel27.setFont(new java.awt.Font("Tahoma", 1, 20)); // NOI18N
        jLabel27.setForeground(new java.awt.Color(51, 51, 51));
        jLabel27.setText("Total Items:");

        lbl_total_items.setFont(new java.awt.Font("Tahoma", 1, 20)); // NOI18N
        lbl_total_items.setForeground(new java.awt.Color(0, 153, 0));
        lbl_total_items.setText("0.0");

        btn_imprimir.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_imprimir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/print_24.png"))); // NOI18N
        btn_imprimir.setText("Imprimir Tickets");
        btn_imprimir.setBorder(null);
        btn_imprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimirActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnl_ingresoLayout = new javax.swing.GroupLayout(pnl_ingreso);
        pnl_ingreso.setLayout(pnl_ingresoLayout);
        pnl_ingresoLayout.setHorizontalGroup(
            pnl_ingresoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_ingresoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_ingresoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_ingresoLayout.createSequentialGroup()
                        .addComponent(chk_cerrar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 150, Short.MAX_VALUE)
                        .addComponent(jLabel27)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_items))
                    .addGroup(pnl_ingresoLayout.createSequentialGroup()
                        .addComponent(btn_guardar, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_limpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_imprimir)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pnl_ingresoLayout.setVerticalGroup(
            pnl_ingresoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_ingresoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_ingresoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_guardar)
                    .addComponent(btn_limpiar)
                    .addComponent(btn_imprimir))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_ingresoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_ingresoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel27)
                        .addComponent(lbl_total_items))
                    .addComponent(chk_cerrar))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.setBackground(new java.awt.Color(225, 233, 236));

        jLabel1.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(57, 75, 85));
        jLabel1.setText("Codigo de barras");

        txt_codigo_barras.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        txt_codigo_barras.setForeground(new java.awt.Color(57, 75, 85));
        txt_codigo_barras.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_codigo_barrasKeyPressed(evt);
            }
        });

        btn_buscar.setBackground(new java.awt.Color(250, 171, 26));
        btn_buscar.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_buscar.setForeground(new java.awt.Color(0, 51, 51));
        btn_buscar.setText("Buscar producto");
        btn_buscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscarActionPerformed(evt);
            }
        });

        btn_crear_producto.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_crear_producto.setText("Crear Producto");

        btn_calcular_costos.setBackground(new java.awt.Color(255, 51, 51));
        btn_calcular_costos.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_calcular_costos.setForeground(new java.awt.Color(255, 255, 255));
        btn_calcular_costos.setText("Calcular Costos");
        btn_calcular_costos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_calcular_costosActionPerformed(evt);
            }
        });

        btn_precargar.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_precargar.setText("Precargar Costos  e IVA");
        btn_precargar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_precargarActionPerformed(evt);
            }
        });

        btn_Exportar_Worold.setBackground(new java.awt.Color(78, 205, 196));
        btn_Exportar_Worold.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_Exportar_Worold.setText("Exportar A World Office");
        btn_Exportar_Worold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Exportar_WoroldActionPerformed(evt);
            }
        });

        btn_calcular_precios_venta.setBackground(new java.awt.Color(153, 0, 153));
        btn_calcular_precios_venta.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_calcular_precios_venta.setForeground(new java.awt.Color(255, 255, 255));
        btn_calcular_precios_venta.setText("Calcular P. Venta");
        btn_calcular_precios_venta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_calcular_precios_ventaActionPerformed(evt);
            }
        });

        btn_calcular_utildiad_porcentaje.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_calcular_utildiad_porcentaje.setText("Calcular Utilidad");
        btn_calcular_utildiad_porcentaje.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_calcular_utildiad_porcentajeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txt_codigo_barras, javax.swing.GroupLayout.PREFERRED_SIZE, 304, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_buscar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_crear_producto)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 372, Short.MAX_VALUE)
                .addComponent(btn_calcular_utildiad_porcentaje)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_Exportar_Worold)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_precargar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_calcular_costos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_calcular_precios_venta)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txt_codigo_barras, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_buscar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_crear_producto)
                    .addComponent(btn_calcular_precios_venta)
                    .addComponent(btn_calcular_costos)
                    .addComponent(btn_precargar, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_Exportar_Worold)
                    .addComponent(btn_calcular_utildiad_porcentaje))
                .addContainerGap())
        );

        jPanel2.setBackground(new java.awt.Color(84, 104, 120));

        jbox_proveedor.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jbox_proveedor.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jbox_proveedorKeyPressed(evt);
            }
        });

        jLabel20.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(246, 248, 243));
        jLabel20.setText("Proveedor");

        jLabel23.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel23.setForeground(new java.awt.Color(246, 248, 243));
        jLabel23.setText("Fecha entrada");

        jdate_fecha_entrada.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N

        lbl_id.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_id.setForeground(new java.awt.Color(255, 153, 153));
        lbl_id.setText("numero");

        jLabel26.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel26.setForeground(new java.awt.Color(246, 248, 243));
        jLabel26.setText("Redondear");

        txt_no_factura.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        txt_no_factura.setForeground(new java.awt.Color(57, 75, 85));

        jbox_transportador.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N

        jLabel22.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel22.setForeground(new java.awt.Color(246, 248, 243));
        jLabel22.setText("Transportador");

        txt_redondear.setText("2");

        jLabel32.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel32.setForeground(new java.awt.Color(246, 248, 243));
        jLabel32.setText("Número de factura");

        btn_buscar_proveedor.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_buscar_proveedor.setForeground(new java.awt.Color(0, 51, 51));
        btn_buscar_proveedor.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/bucar.png"))); // NOI18N
        btn_buscar_proveedor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscar_proveedorActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jbox_proveedor, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_buscar_proveedor))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jbox_transportador, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel32)
                    .addComponent(jLabel23))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jdate_fecha_entrada, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)
                    .addComponent(txt_no_factura))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 1157, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lbl_id)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel26)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txt_redondear, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(lbl_id)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txt_redondear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel26)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_buscar_proveedor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel20)
                                .addComponent(jbox_proveedor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel32)
                                .addComponent(txt_no_factura, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jdate_fecha_entrada, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel22)
                                .addComponent(jbox_transportador, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel23)))))
                .addContainerGap(10, Short.MAX_VALUE))
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jtabla.setFont(new java.awt.Font("Yu Gothic Medium", 0, 24)); // NOI18N
        jtabla.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][]{},
            new String[]{}
        ));
        jtabla.setRowHeight(50);
        jtabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jtabla);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE)
        );

        jpanel_dinero.setBackground(new java.awt.Color(255, 255, 255));

        txt_observacion.setColumns(20);
        txt_observacion.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_observacion.setRows(5);
        jScrollPane2.setViewportView(txt_observacion);

        javax.swing.GroupLayout jpanel_dineroLayout = new javax.swing.GroupLayout(jpanel_dinero);
        jpanel_dinero.setLayout(jpanel_dineroLayout);
        jpanel_dineroLayout.setHorizontalGroup(
            jpanel_dineroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpanel_dineroLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 829, Short.MAX_VALUE)
                .addContainerGap())
        );
        jpanel_dineroLayout.setVerticalGroup(
            jpanel_dineroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpanel_dineroLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        jpanel_dinero1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel28.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel28.setForeground(new java.awt.Color(51, 51, 51));
        jLabel28.setText("Sub Total:");

        lbl_sub_total.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_sub_total.setForeground(new java.awt.Color(102, 0, 102));
        lbl_sub_total.setText("0.0");

        jLabel29.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel29.setForeground(new java.awt.Color(51, 51, 51));
        jLabel29.setText("IVA:");

        lbl_iva.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_iva.setForeground(new java.awt.Color(153, 0, 0));
        lbl_iva.setText("0.0");

        jLabel30.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel30.setForeground(new java.awt.Color(51, 51, 51));
        jLabel30.setText("Total :");

        lbl_total_factura.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_total_factura.setForeground(new java.awt.Color(0, 153, 0));
        lbl_total_factura.setText("0.0");

        jLabel31.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel31.setForeground(new java.awt.Color(51, 51, 51));
        jLabel31.setText("Descuento:");

        lbl_descuento.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_descuento.setForeground(new java.awt.Color(153, 153, 0));
        lbl_descuento.setText("0.0");

        javax.swing.GroupLayout jpanel_dinero1Layout = new javax.swing.GroupLayout(jpanel_dinero1);
        jpanel_dinero1.setLayout(jpanel_dinero1Layout);
        jpanel_dinero1Layout.setHorizontalGroup(
            jpanel_dinero1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpanel_dinero1Layout.createSequentialGroup()
                .addContainerGap(458, Short.MAX_VALUE)
                .addGroup(jpanel_dinero1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpanel_dinero1Layout.createSequentialGroup()
                        .addComponent(jLabel28)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_sub_total))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpanel_dinero1Layout.createSequentialGroup()
                        .addComponent(jLabel29)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_iva))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpanel_dinero1Layout.createSequentialGroup()
                        .addComponent(jLabel30)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_factura))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpanel_dinero1Layout.createSequentialGroup()
                        .addComponent(jLabel31)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_descuento)))
                .addContainerGap())
        );
        jpanel_dinero1Layout.setVerticalGroup(
            jpanel_dinero1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpanel_dinero1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpanel_dinero1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel28)
                    .addComponent(lbl_sub_total))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpanel_dinero1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel31)
                    .addComponent(lbl_descuento))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpanel_dinero1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel29)
                    .addComponent(lbl_iva))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpanel_dinero1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel30)
                    .addComponent(lbl_total_factura))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnl_ingreso, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpanel_dinero, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpanel_dinero1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(pnl_ingreso, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpanel_dinero, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpanel_dinero1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        pack();
    }

    private void btn_limpiarActionPerformed(java.awt.event.ActionEvent evt) {
        limpiar();
    }

    public void limpiar() {
        lbl_id.setText(DB_consultas_R_D.cargarId("ingresos_productos_cabecera"));
        es_nuevo = true;
        txt_codigo_barras.setText("");
        for (int i = 0; i < modelo_productos.getRowCount(); i++) {
            modelo_productos.removeRow(i);
            i -= 1;
        }
    }

    public static int id_proveedor, id_transportador;

    // ====================================================================
    // Puente con ingresos de mercancia: columna/selector de bodega (rol 2)
    // ====================================================================
    private void configurarColumnasYBodega() {
        replicaBodegaActiva = false;
        if (frm_main.rol_precios == 2) {
            cargarBodegas();
            if (BODEGAS != null && !BODEGAS.isEmpty()) {
                replicaBodegaActiva = true;
            }
        }

        if (replicaBodegaActiva) {
            modelo_productos.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "ETIQUETAS", "BODEGA"});
            construirBarraBodega();
            jtabla.setModel(modelo_productos);
            aplicarEditorBodega();
        } else {
            modelo_productos.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "ETIQUETAS"});
        }
    }

    /** Carga las bodegas de controlbodega (misma base). */
    private void cargarBodegas() {
        BODEGAS = new java.util.ArrayList<>();
        ResultSet rs = DB_consultas_R_D.getTabla("select id, nombre from bodegas order by nombre");
        try {
            while (rs != null && rs.next()) {
                BODEGAS.add(new Bodega(rs.getInt("id"), rs.getString("nombre")));
            }
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            System.out.println("Error al cargar bodegas: " + e);
        }
    }

    /** Inserta una barra superior con el selector general de bodega sobre la tabla. */
    private void construirBarraBodega() {
        cmb_bodega_general = new javax.swing.JComboBox<>();
        for (Bodega b : BODEGAS) {
            cmb_bodega_general.addItem(b);
        }
        cmb_bodega_general.setFont(new java.awt.Font("Tahoma", 0, 16));

        javax.swing.JButton btnAplicar = new javax.swing.JButton("Aplicar a todos");
        btnAplicar.setFont(new java.awt.Font("Tahoma", 1, 14));
        btnAplicar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                aplicarBodegaATodos();
            }
        });

        btn_enviar_bodega = new javax.swing.JButton("Enviar a bodega");
        btn_enviar_bodega.setFont(new java.awt.Font("Tahoma", 1, 14));
        btn_enviar_bodega.setBackground(new java.awt.Color(0, 153, 0));
        btn_enviar_bodega.setForeground(new java.awt.Color(255, 255, 255));
        btn_enviar_bodega.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                enviarABodega();
            }
        });

        javax.swing.JLabel lbl = new javax.swing.JLabel("Bodega destino:");
        lbl.setFont(new java.awt.Font("Tahoma", 1, 14));

        javax.swing.JPanel barra = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 6));
        barra.setBackground(new java.awt.Color(255, 255, 255));
        barra.add(lbl);
        barra.add(cmb_bodega_general);
        barra.add(btnAplicar);
        barra.add(btn_enviar_bodega);

        jPanel3.setLayout(new java.awt.BorderLayout());
        jPanel3.add(barra, java.awt.BorderLayout.NORTH);
        jPanel3.add(jScrollPane1, java.awt.BorderLayout.CENTER);
    }

    /** (Re)aplica el editor de combo de bodega a la columna BODEGA de la tabla. */
    public static void aplicarEditorBodega() {
        if (!replicaBodegaActiva) {
            return;
        }
        try {
            if (jtabla.getColumnModel().getColumnCount() > COL_BODEGA) {
                javax.swing.JComboBox<Bodega> editorCombo = new javax.swing.JComboBox<>();
                for (Bodega b : BODEGAS) {
                    editorCombo.addItem(b);
                }
                jtabla.getColumnModel().getColumn(COL_BODEGA).setCellEditor(new javax.swing.DefaultCellEditor(editorCombo));
            }
        } catch (Exception e) {
            System.out.println("Error al aplicar editor de bodega: " + e);
        }
    }

    /** @return la Bodega de la lista cargada con ese id, o null si no existe. */
    public static Bodega bodegaPorId(int id) {
        if (BODEGAS == null || id <= 0) {
            return null;
        }
        for (Bodega b : BODEGAS) {
            if (b.getId() == id) {
                return b;
            }
        }
        return null;
    }

    /** Bodega por defecto para nuevas filas: la seleccionada en el selector general. */
    public static Bodega bodegaPorDefecto() {
        if (cmb_bodega_general != null && cmb_bodega_general.getSelectedItem() instanceof Bodega) {
            return (Bodega) cmb_bodega_general.getSelectedItem();
        }
        if (BODEGAS != null && !BODEGAS.isEmpty()) {
            return BODEGAS.get(0);
        }
        return null;
    }

    /** Asigna la bodega del selector general a todas las filas de la tabla. */
    public void aplicarBodegaATodos() {
        if (!replicaBodegaActiva) {
            return;
        }
        Bodega seleccionada = bodegaPorDefecto();
        if (seleccionada == null) {
            return;
        }
        for (int i = 0; i < modelo_productos.getRowCount(); i++) {
            modelo_productos.setValueAt(seleccionada, i, COL_BODEGA);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /**
     * Boton "Enviar a bodega": guarda y ademas crea los ingresos de mercancia
     * pendientes. Si ya fue enviado, no permite reenviar.
     */
    private void enviarABodega() {
        if (!replicaBodegaActiva) {
            return;
        }
        if (!es_nuevo && yaEnviado(lbl_id.getText())) {
            JOptionPane.showMessageDialog(this,
                    "Este ingreso YA fue enviado a bodega.\n"
                    + "Solo se puede enviar una vez; no es posible reenviarlo.",
                    "Ingreso ya enviado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        enviarTrasGuardar = true;
        btn_guardarActionPerformed(null);
    }

    /** @return true si el ingreso (id) ya fue marcado como enviado a bodega. */
    private boolean yaEnviado(String idIngreso) {
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(
                    "select enviado_control_bodega from ingresos_productos_cabecera where id = " + idIngreso);
            if (rs != null && rs.next()) {
                boolean v = rs.getBoolean("enviado_control_bodega");
                rs.close();
                return v;
            }
            if (rs != null) {
                rs.close();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }

    /** Marca el ingreso (id) como enviado a bodega. */
    private void marcarEnviado(String idIngreso) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement ps = con.prepareStatement(
                    "update ingresos_productos_cabecera set enviado_control_bodega = true where id = " + idIngreso);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            System.out.println("Error al marcar ingreso como enviado: " + e);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex);
            }
        }
    }

    /**
     * @return indice de la primera fila SIN bodega valida, o -1 si todas
     * tienen bodega.
     */
    public static int primeraFilaSinBodega() {
        if (modelo_productos == null || modelo_productos.getColumnCount() <= COL_BODEGA) {
            return 0;
        }
        for (int i = 0; i < modelo_productos.getRowCount(); i++) {
            Object bod = modelo_productos.getValueAt(i, COL_BODEGA);
            if (!(bod instanceof Bodega) || ((Bodega) bod).getId() <= 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Busca dos filas con el MISMO codigo de barras asignadas a la MISMA
     * bodega.
     */
    public static int[] duplicadoMismaBodega() {
        if (modelo_productos == null || modelo_productos.getColumnCount() <= COL_BODEGA) {
            return null;
        }
        for (int i = 0; i < modelo_productos.getRowCount(); i++) {
            String codI = "" + modelo_productos.getValueAt(i, 1);
            Object bodI = modelo_productos.getValueAt(i, COL_BODEGA);
            int idBodI = (bodI instanceof Bodega) ? ((Bodega) bodI).getId() : -1;
            for (int j = i + 1; j < modelo_productos.getRowCount(); j++) {
                String codJ = "" + modelo_productos.getValueAt(j, 1);
                Object bodJ = modelo_productos.getValueAt(j, COL_BODEGA);
                int idBodJ = (bodJ instanceof Bodega) ? ((Bodega) bodJ).getId() : -2;
                if (codI.equals(codJ) && idBodI == idBodJ) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }

    /**
     * Crea los ingresos de mercancia pendientes (uno por bodega) a partir de
     * la tabla. Tablas unificadas: se referencian los ids de producto y
     * proveedor directamente.
     */
    private boolean enviarIngresoABodega(IngresosProductos ingreso_cabecera) {
        try {
            java.util.List<ReplicaIngresoOrden.Linea> lineas = new java.util.ArrayList<>();
            for (int i = 0; i < modelo_productos.getRowCount(); i++) {
                int idProducto = Integer.parseInt("" + modelo_productos.getValueAt(i, 0));
                double cant;
                try {
                    cant = Double.parseDouble("" + modelo_productos.getValueAt(i, 3));
                } catch (Exception e) {
                    cant = 1;
                }
                int idBodega = 1;
                Object bod = modelo_productos.getValueAt(i, COL_BODEGA);
                if (bod instanceof Bodega) {
                    idBodega = ((Bodega) bod).getId();
                } else {
                    Bodega d = bodegaPorDefecto();
                    if (d != null) {
                        idBodega = d.getId();
                    }
                }
                lineas.add(new ReplicaIngresoOrden.Linea(idProducto, cant, idBodega));
            }

            String nombreUsuario = nz(frm_main.nombre_usuario);

            String fechaB = ingreso_cabecera.getFecha_ingreso().replace("'", "");
            String horaB = ingreso_cabecera.getHora().replace("'", "");

            ReplicaIngresoOrden replica = new ReplicaIngresoOrden();
            ReplicaIngresoOrden.Resultado resR = replica.enviar(
                    lineas, ingreso_cabecera.getId_proveedor(), txt_no_factura.getText(),
                    fechaB, horaB, nombreUsuario, txt_observacion.getText(), frm_main.id_user);

            StringBuilder msg = new StringBuilder();
            msg.append("Envío a bodega:\n");
            msg.append("• Ingresos de mercancía (pendientes) creados: ").append(resR.cabecerasCreadas).append("\n");
            if (!resR.errores.isEmpty()) {
                msg.append("\nAdvertencias:\n");
                for (String er : resR.errores) {
                    msg.append("  - ").append(er).append("\n");
                }
                JOptionPane.showMessageDialog(this, msg.toString(), "Envío a bodega", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, msg.toString(), "Envío a bodega", JOptionPane.INFORMATION_MESSAGE);
            }
            return resR.ok && resR.cabecerasCreadas > 0 && resR.errores.isEmpty();
        } catch (Exception e) {
            System.out.println("Error en envío a bodega: " + e);
            JOptionPane.showMessageDialog(this,
                    "El ingreso se guardó, pero falló el envío a bodega:\n" + e,
                    "Envío a bodega", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    private void btn_guardarActionPerformed(java.awt.event.ActionEvent evt) {
        boolean actualizar = false;
        final boolean enviar = enviarTrasGuardar;
        enviarTrasGuardar = false;
        if (jtabla.getRowCount() < 1) {
            JOptionPane.showMessageDialog(this, "Por favor agregue al menos un producto");
            txt_codigo_barras.requestFocus();
        } else if (enviar && replicaBodegaActiva && primeraFilaSinBodega() >= 0) {
            int fila = primeraFilaSinBodega();
            try {
                jtabla.setRowSelectionInterval(fila, fila);
                jtabla.scrollRectToVisible(jtabla.getCellRect(fila, COL_BODEGA, true));
            } catch (Exception ignored) {
            }
            JOptionPane.showMessageDialog(this,
                    "Debe seleccionar una BODEGA para cada producto antes de guardar.\n"
                    + "Falta la bodega en la fila " + (fila + 1) + ".",
                    "Bodega obligatoria", JOptionPane.WARNING_MESSAGE);
        } else if (enviar && replicaBodegaActiva && duplicadoMismaBodega() != null) {
            int[] dup = duplicadoMismaBodega();
            String cod = "" + modelo_productos.getValueAt(dup[1], 1);
            try {
                jtabla.setRowSelectionInterval(dup[1], dup[1]);
                jtabla.scrollRectToVisible(jtabla.getCellRect(dup[1], COL_BODEGA, true));
            } catch (Exception ignored) {
            }
            JOptionPane.showMessageDialog(this,
                    "El producto " + cod + " está agregado más de una vez en la MISMA bodega (filas "
                    + (dup[0] + 1) + " y " + (dup[1] + 1) + ").\n"
                    + "Debe unificarlo en una sola fila o asignarle bodegas diferentes.",
                    "Producto repetido en la misma bodega", JOptionPane.WARNING_MESSAGE);
        } else {

            int guardo_cabecera = 0;
            DBingresosPrecios dbingreso = new DBingresosPrecios();
            IngresosProductos ingreso_cabecera = new IngresosProductos();
            try {
                ingreso_cabecera.setId_proveedor(jbox_proveedor.getItemAt(jbox_proveedor.getSelectedIndex()).getId());
            } catch (Exception e) {
                ingreso_cabecera.setId_proveedor(id_proveedor);
            }
            try {
                ingreso_cabecera.setId_transportador(jbox_transportador.getItemAt(jbox_transportador.getSelectedIndex()).getId());
            } catch (Exception e) {
                ingreso_cabecera.setId_transportador(id_transportador);
            }

            switch (frm_main.rol_precios) {
                case 2:
                    ingreso_cabecera.setEstado(0);
                    break;
                case 3:
                    ingreso_cabecera.setEstado(1);
                    break;
                default:
                    ingreso_cabecera.setEstado(2);
                    break;
            }
            ingreso_cabecera.setObservacion(txt_observacion.getText());

            ingreso_cabecera.setId_user(frm_main.id_user);
            int hora, minutos, segundos;
            int dia, mes, ano;
            ano = jdate_fecha_entrada.getCalendar().get(Calendar.YEAR);
            mes = jdate_fecha_entrada.getCalendar().get(Calendar.MONTH) + 1;
            dia = jdate_fecha_entrada.getCalendar().get(Calendar.DAY_OF_MONTH);
            hora = jdate_fecha_entrada.getCalendar().get(Calendar.HOUR_OF_DAY);
            minutos = jdate_fecha_entrada.getCalendar().get(Calendar.MINUTE);
            segundos = jdate_fecha_entrada.getCalendar().get(Calendar.SECOND);
            ingreso_cabecera.setFecha_ingreso("'" + ano + "-" + mes + "-" + dia + "'");
            ingreso_cabecera.setHora("'" + hora + ":" + minutos + ":" + segundos + "'");
            ingreso_cabecera.setId(Integer.parseInt(lbl_id.getText()));
            ingreso_cabecera.setNo_factura(txt_no_factura.getText());

            if (!es_nuevo) {
                guardo_cabecera = dbingreso.Actualizar(ingreso_cabecera);
                actualizar = true;
            } else {
                int idAsignado = dbingreso.GuardarConReintento(ingreso_cabecera, 5);
                if (idAsignado > 0) {
                    guardo_cabecera = 1;
                    lbl_id.setText(String.valueOf(idAsignado));
                } else {
                    guardo_cabecera = 0;
                }
            }

            if (guardo_cabecera > 0) {
                Connection con = null;
                PreparedStatement psql = null;
                String sql_ingreso_detalle = "";
                String updates_precios_productos = "";

                switch (frm_main.rol_precios) {
                    case 2:
                        con = DB_consultas_R_D.getConexion();
                        sql_ingreso_detalle = "";

                        if (actualizar) {
                            sql_ingreso_detalle = "DELETE FROM ingresos_productos_detalle WHERE id_ingreso_cabecera=" + lbl_id.getText() + ";";
                        }

                        for (int i = 0; i < modelo_productos.getRowCount(); i++) {
                            String can = "";
                            try {
                                can = "" + modelo_productos.getValueAt(i, 3);
                            } catch (Exception e) {
                                System.out.println(e);
                                can = "1";
                            }
                            String etiqueta = "";
                            try {
                                etiqueta = modelo_productos.getValueAt(i, 4).toString();
                            } catch (Exception e) {
                                System.out.println(e);
                                etiqueta = "1";
                            }
                            if (replicaBodegaActiva) {
                                // Bodega seleccionada por fila (se persiste para poder
                                // continuar la edicion antes de enviar a bodega).
                                String idBodegaCol = "NULL";
                                try {
                                    Object bod = modelo_productos.getValueAt(i, COL_BODEGA);
                                    if (bod instanceof Bodega && ((Bodega) bod).getId() > 0) {
                                        idBodegaCol = String.valueOf(((Bodega) bod).getId());
                                    }
                                } catch (Exception e) {
                                    idBodegaCol = "NULL";
                                }
                                sql_ingreso_detalle += "INSERT INTO ingresos_productos_detalle (id,id_producto,cantidad, etiquetas, id_ingreso_cabecera, id_bodega_control) "
                                        + "VALUES ((select COALESCE(max(id),0)+1 from ingresos_productos_detalle)," + modelo_productos.getValueAt(i, 0).toString() + "," + can + ",'" + etiqueta + "',"
                                        + lbl_id.getText() + "," + idBodegaCol + ");\n";
                            } else {
                                sql_ingreso_detalle += "INSERT INTO ingresos_productos_detalle (id,id_producto,cantidad, etiquetas, id_ingreso_cabecera) "
                                        + "VALUES ((select COALESCE(max(id),0)+1 from ingresos_productos_detalle)," + modelo_productos.getValueAt(i, 0).toString() + "," + can + ",'" + etiqueta + "',"
                                        + lbl_id.getText() + ");\n";
                            }
                        }
                        try {
                            psql = con.prepareStatement(sql_ingreso_detalle);
                            psql.executeUpdate();

                            psql.close();
                            con.close();

                        } catch (SQLException ex) {
                            Logger.getLogger(jif_crear_ingreso_precios.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        // Envio a bodega: SOLO cuando el usuario pulsa "Enviar a bodega".
                        if (enviar && replicaBodegaActiva) {
                            boolean enviado = enviarIngresoABodega(ingreso_cabecera);
                            if (enviado) {
                                marcarEnviado(lbl_id.getText());
                            }
                        }
                        break;
                    case 3: // CONTADOR
                        con = DB_consultas_R_D.getConexion();
                        sql_ingreso_detalle = "";

                        if (actualizar) {
                            sql_ingreso_detalle = "DELETE FROM ingresos_productos_detalle WHERE id_ingreso_cabecera=" + lbl_id.getText() + ";";
                        }
                        updates_precios_productos = "";

                        for (int i = 0; i < modelo_productos.getRowCount(); i++) {
                            String can = "";
                            try {
                                can = "" + modelo_productos.getValueAt(i, 3);
                            } catch (Exception e) {
                                System.out.println(e);
                                can = "1";
                            }
                            String etiqueta = "";
                            try {
                                etiqueta = modelo_productos.getValueAt(i, 4).toString();
                            } catch (Exception e) {
                                System.out.println(e);
                                etiqueta = "1";
                            }

                            sql_ingreso_detalle += "INSERT INTO ingresos_productos_detalle (id,id_producto,cantidad, etiquetas, id_ingreso_cabecera, iva, precio_costo, descuento) "
                                    + "VALUES ((select COALESCE(max(id),0)+1 from ingresos_productos_detalle)," + modelo_productos.getValueAt(i, 0).toString() + "," + can + ",'" + etiqueta + "',"
                                    + lbl_id.getText() + "," + modelo_productos.getValueAt(i, 6).toString() + ", " + metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 5).toString(), ".") + ","
                                    + modelo_productos.getValueAt(i, 7).toString() + ");\n";

                            double costo = Double.parseDouble(metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 5).toString(), "."));
                            double descuento = Double.parseDouble(modelo_productos.getValueAt(i, 7).toString());

                            double costo_con_descuento = costo - (costo * (descuento / 100));

                            // Costo real de compra: compartido con el resto de
                            // controlbodega (decision del negocio).
                            updates_precios_productos += "update productos set precio_costo=" + costo_con_descuento
                                    + ",iva=" + modelo_productos.getValueAt(i, 6).toString() + " "
                                    + "where codigo_barras='" + modelo_productos.getValueAt(i, 1).toString() + "';\n";
                        }
                        try {

                            psql = con.prepareStatement(sql_ingreso_detalle);
                            psql.executeUpdate();

                            psql = con.prepareStatement(updates_precios_productos);
                            psql.executeUpdate();

                            psql.close();
                            con.close();

                        } catch (SQLException ex) {
                            Logger.getLogger(jif_crear_ingreso_precios.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                    default: // 4 PRECIOS
                        con = DB_consultas_R_D.getConexion();
                        sql_ingreso_detalle = "";

                        if (actualizar) {
                            sql_ingreso_detalle = "DELETE FROM ingresos_productos_detalle WHERE id_ingreso_cabecera=" + lbl_id.getText() + ";";
                        }
                        updates_precios_productos = "";

                        for (int i = 0; i < modelo_productos.getRowCount(); i++) {
                            String can = "";
                            try {
                                can = "" + modelo_productos.getValueAt(i, 3);
                            } catch (Exception e) {
                                System.out.println(e);
                                can = "1";
                            }

                            sql_ingreso_detalle += "INSERT INTO ingresos_productos_detalle (id,  id_ingreso_cabecera, id_producto, cantidad, precio_costo, iva, descuento, porcentaje_utilidad, venta, valor_desc_1, valor_desc_2, valor_s_y_t,"
                                    + "valor_credito, desc_n_1, desc_n_2, etiquetas) "
                                    + "VALUES ((select COALESCE(max(id),0)+1 from ingresos_productos_detalle), " + lbl_id.getText() + "," + modelo_productos.getValueAt(i, 0).toString() + "," + can + ", "
                                    + metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 4).toString(), ".") + ","
                                    + modelo_productos.getValueAt(i, 5).toString() + ","
                                    + modelo_productos.getValueAt(i, 6).toString() + ","
                                    + modelo_productos.getValueAt(i, 9).toString() + ","
                                    + metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 10).toString(), ".") + ", "
                                    + metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 11).toString(), ".") + ", "
                                    + metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 12).toString(), ".") + ", "
                                    + metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 13).toString(), ".") + ", "
                                    + metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 14).toString(), ".") + ", "
                                    + (Double.parseDouble(metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 10).toString(), ".")) - Double.parseDouble(metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 11).toString(), "."))) + ","
                                    + (Double.parseDouble(metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 10).toString(), ".")) - Double.parseDouble(metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 12).toString(), "."))) + ","
                                    + "'" + modelo_productos.getValueAt(i, 15).toString() + "'"
                                    + ");\n";

                            // SOLO columnas del modulo Precios: nunca toca
                            // precio_venta/2/3 de bodega.
                            updates_precios_productos += "update productos set "
                                    + "venta=" + metodos.EliminaCaracteres("" + modelo_productos.getValueAt(i, 10).toString(), ".") + ", "
                                    + "valor_desc_1=" + metodos.EliminaCaracteres("" + modelo_productos.getValueAt(i, 11).toString(), ".") + ", "
                                    + "valor_desc_2=" + metodos.EliminaCaracteres("" + modelo_productos.getValueAt(i, 12).toString(), ".") + ", "
                                    + "valor_s_y_t=" + metodos.EliminaCaracteres("" + modelo_productos.getValueAt(i, 13).toString(), ".") + ", "
                                    + "valor_credito=" + metodos.EliminaCaracteres("" + modelo_productos.getValueAt(i, 14).toString(), ".") + ", "
                                    + "porcentaje_utilidad=" + modelo_productos.getValueAt(i, 9).toString() + ", "
                                    + "iva=" + modelo_productos.getValueAt(i, 5).toString() + " "
                                    + "where codigo_barras='" + modelo_productos.getValueAt(i, 1).toString() + "';\n";
                        }
                        try {

                            psql = con.prepareStatement(sql_ingreso_detalle);
                            psql.executeUpdate();

                            psql = con.prepareStatement(updates_precios_productos);
                            psql.executeUpdate();

                            psql.close();
                            con.close();

                        } catch (SQLException ex) {
                            Logger.getLogger(jif_crear_ingreso_precios.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                }
            }
            try {
                frm_ingresos_precios.btn_actualizar.doClick();
            } catch (Exception ignored) {
            }
            limpiar();
            if (chk_cerrar.isSelected()) {
                this.dispose();
            } else {
                lbl_id.setText(DB_consultas_R_D.cargarId("ingresos_productos_cabecera"));
                es_nuevo = true;
                btn_guardar.setText("Guardar");
            }

        }
    }

    private void btn_buscarActionPerformed(java.awt.event.ActionEvent evt) {
        jd_buscar_producto_precios buscar_producto = new jd_buscar_producto_precios(null, rootPaneCheckingEnabled);
        jd_buscar_producto_precios.formulario = "ingreso";
        buscar_producto.show();
    }

    /** Pasa los productos de la tabla a la impresion de etiquetas. */
    private void btn_imprimirActionPerformed(java.awt.event.ActionEvent evt) {
        jd_productos_a_imprimir j = new jd_productos_a_imprimir(null, rootPaneCheckingEnabled);

        try {
            for (int i = 0; i < jd_productos_a_imprimir.modeloProductos.getRowCount(); i++) {
                jd_productos_a_imprimir.modeloProductos.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

        jd_productos_a_imprimir.modeloProductos.setColumnIdentifiers(new Object[]{"codigo", "descripcion", "Cantidad"});

        for (int i = 0; i < modelo_productos.getRowCount(); i++) {
            jd_productos_a_imprimir.modeloProductos.addRow(new Object[]{modelo_productos.getValueAt(i, 1).toString(), modelo_productos.getValueAt(i, 2).toString(), modelo_productos.getValueAt(i, 3)});
        }
        jd_productos_a_imprimir.jtabla_productos.setModel(jd_productos_a_imprimir.modeloProductos);

        j.show();
    }

    private void txt_codigo_barrasKeyPressed(java.awt.event.KeyEvent evt) {
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            if (txt_codigo_barras.getText().equals("")) {
                JOptionPane.showMessageDialog(this, "Ingrese un codigo de barras");
            } else {

                int posicion_asterisco = txt_codigo_barras.getText().indexOf("*") + 1;
                String codigo_barras = txt_codigo_barras.getText().substring(posicion_asterisco);
                double cantidad = 1.0;
                try {
                    cantidad = Double.parseDouble(txt_codigo_barras.getText().substring(0, posicion_asterisco - 1));
                } catch (Exception e) {
                    cantidad = 1;
                }

                agregar_cod(codigo_barras, cantidad);
                calcular_total();
            }
        }
    }

    private boolean existe_en_tabla(String codigo) {
        try {
            for (int i = 0; i < this.jtabla.getRowCount(); i++) {
                if (this.jtabla.getValueAt(i, 1).toString().equals(codigo)) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    private int posicion_en_jtable(String codigo) {
        for (int i = 0; i < this.jtabla.getRowCount(); i++) {
            if (this.jtabla.getValueAt(i, 1).toString().equals(codigo)) {
                return i;
            }
        }
        return 0;
    }

    private String extraer_cantidad_actual_by_codigo(String codigo) {
        String regresa = "";
        for (int i = 0; i < this.jtabla.getRowCount(); i++) {
            if (this.jtabla.getValueAt(i, 1).toString().equals(codigo)) {
                regresa = "" + jtabla.getValueAt(i, 3);
            }
        }
        return regresa;
    }

    public void agregar_cod(String codigo_barras, double cantidad) {
        if (DB_consultas_R_D.consultar_existencia_campo_String("codigo_barras", codigo_barras, "productos") == 1) {

            // Verificar si el producto esta deshabilitado (estado boolean en bodega)
            ResultSet rsEstado = DB_consultas_R_D.getTabla("SELECT coalesce(estado,true) as estado FROM productos WHERE codigo_barras = '" + codigo_barras + "'");
            try {
                if (rsEstado.next()) {
                    boolean estado = rsEstado.getBoolean("estado");
                    if (!estado) {
                        int respuesta = JOptionPane.showConfirmDialog(this,
                                "Este producto está DESHABILITADO.\n¿Desea habilitarlo y agregarlo?",
                                "Producto deshabilitado",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);

                        if (respuesta == JOptionPane.YES_OPTION) {
                            DBpreciosProductos.habilitarProducto(codigo_barras);
                        } else {
                            txt_codigo_barras.setText("");
                            return;
                        }
                    }
                }
                rsEstado.close();
            } catch (Exception e) {
                System.out.println(e);
            }

            // El mismo producto puede ir a varias bodegas en rol 2: no se fusiona.
            if (existe_en_tabla(codigo_barras) && !replicaBodegaActiva) {
                double actuvalor = Double.parseDouble(extraer_cantidad_actual_by_codigo(codigo_barras));
                actuvalor += cantidad;
                modelo_productos.setValueAt("" + actuvalor, posicion_en_jtable(codigo_barras), 3);
                txt_codigo_barras.setText("");
            } else {
                if (frm_main.rol_precios == 4) {
                    // Rol Precios: agregar con 16 columnas
                    String consulta = "select p.id, p.codigo_barras, p.descripcion, p.precio_costo, p.iva, p.venta, "
                            + "p.valor_desc_1, p.valor_desc_2, p.valor_s_y_t, p.valor_credito, p.porcentaje_utilidad, "
                            + "c.porcentaje_operacion "
                            + "from productos p, configuraciones c "
                            + "where c.id=1 and p.codigo_barras ='" + codigo_barras + "'";
                    ResultSet rs = DB_consultas_R_D.getTabla(consulta);
                    try {
                        while (rs.next()) {
                            double costo = rs.getDouble("precio_costo");
                            double iva = rs.getDouble("iva");
                            double descuento = 0;
                            double porcentaje_operacion = rs.getDouble("porcentaje_operacion");

                            double costo_iva_descuento = (costo + (costo * (iva / 100))) - ((costo + (costo * (iva / 100))) * (descuento / 100));
                            double costo_iva_descuento_gasto = costo_iva_descuento + (costo_iva_descuento * (porcentaje_operacion / 100));

                            modelo_productos.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "COSTO", "IVA", "DESCUENTO", "COSTO+IVA-DESC", "COSTO+IVA+GASTO",
                                "% UTIL.", "VENTA", "VALOR DES. N1", "VALOR DES. N2", "VALOR S Y T", "VALOR CRED.", "E"});

                            modelo_productos.addRow(new Object[]{
                                rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                                cantidad, metodos.formateador_dinero().format(costo), iva, descuento,
                                metodos.formateador_dinero().format(costo_iva_descuento),
                                metodos.formateador_dinero().format(costo_iva_descuento_gasto),
                                rs.getDouble("porcentaje_utilidad"),
                                metodos.formateador_dinero().format(rs.getDouble("venta")),
                                metodos.formateador_dinero().format(rs.getDouble("valor_desc_1")),
                                metodos.formateador_dinero().format(rs.getDouble("valor_desc_2")),
                                metodos.formateador_dinero().format(rs.getDouble("valor_s_y_t")),
                                metodos.formateador_dinero().format(rs.getDouble("valor_credito")),
                                "0"
                            });
                        }
                        rs.close();
                        jtabla.setModel(modelo_productos);
                    } catch (SQLException ex) {
                        Logger.getLogger(jif_crear_ingreso_precios.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    TamanosTablaPrecios();
                    btn_calcular_utildiad_porcentaje.setVisible(true);
                } else {
                    String consulta = "select p.id, p.codigo_barras, p.descripcion, p.precio_costo, u.nombre as unidad "
                            + "from productos p, unidades_medidas u where p.id_unidad=u.id "
                            + "and p.codigo_barras ='" + codigo_barras + "'";
                    ResultSet rs = DB_consultas_R_D.getTabla(consulta);
                    try {
                        while (rs.next()) {
                            if (replicaBodegaActiva) {
                                modelo_productos.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"), cantidad, 0, bodegaPorDefecto()});
                            } else {
                                modelo_productos.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"), cantidad, 0});
                            }
                        }
                        rs.close();
                        jtabla.setModel(modelo_productos);
                        aplicarEditorBodega();
                    } catch (SQLException ex) {
                        Logger.getLogger(jif_crear_ingreso_precios.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    TamanosTabla();
                }
                txt_codigo_barras.setText("");
            }
        } else {
            JOptionPane.showMessageDialog(this, "El codigo de barras ingresado no se encuentra en la base de datos");
            txt_codigo_barras.setText("");
        }
    }

    public static void TamanosTabla() {
        jtabla.getTableHeader().setReorderingAllowed(false);
        TableColumnModel columnModel = jtabla.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(60);
        columnModel.getColumn(1).setPreferredWidth(80);
        columnModel.getColumn(2).setPreferredWidth(450);
        columnModel.getColumn(3).setPreferredWidth(100);
    }

    public static void TamanosTablaContador() {
        jtabla.getTableHeader().setReorderingAllowed(false);
        TableColumnModel columnModel = jtabla.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(60);
        columnModel.getColumn(1).setPreferredWidth(80);
        columnModel.getColumn(2).setPreferredWidth(450);
        columnModel.getColumn(3).setPreferredWidth(100);
    }

    public static void TamanosTablaPrecios() {
        jtabla.getTableHeader().setReorderingAllowed(false);
        TableColumnModel columnModel = jtabla.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(10);
        columnModel.getColumn(1).setPreferredWidth(50);
        columnModel.getColumn(2).setPreferredWidth(350);
        columnModel.getColumn(3).setPreferredWidth(70);
        columnModel.getColumn(4).setPreferredWidth(70);
        columnModel.getColumn(5).setPreferredWidth(30);
        columnModel.getColumn(6).setPreferredWidth(5);
        columnModel.getColumn(7).setPreferredWidth(100);
        columnModel.getColumn(8).setPreferredWidth(100);
        columnModel.getColumn(9).setPreferredWidth(100);
        columnModel.getColumn(10).setPreferredWidth(100);
        columnModel.getColumn(11).setPreferredWidth(100);
        columnModel.getColumn(12).setPreferredWidth(100);
        columnModel.getColumn(13).setPreferredWidth(100);
        columnModel.getColumn(14).setPreferredWidth(100);
        columnModel.getColumn(15).setPreferredWidth(1);
    }

    private void jbox_proveedorKeyPressed(java.awt.event.KeyEvent evt) {
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            txt_no_factura.requestFocus();
        }
    }

    private void btn_calcular_costosActionPerformed(java.awt.event.ActionEvent evt) {

        switch (frm_main.rol_precios) {
            case 3:

                for (int i = 0; i < jtabla.getRowCount(); i++) {
                    try {
                        double costo, iva, cantidad, descuento;
                        cantidad = Double.parseDouble(modelo_productos.getValueAt(i, 3).toString());

                        costo = Double.parseDouble(metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 5).toString(), "."));
                        iva = Double.parseDouble(modelo_productos.getValueAt(i, 6).toString());
                        descuento = Double.parseDouble(modelo_productos.getValueAt(i, 7).toString());

                        costo = costo - (costo * (descuento / 100));

                        modelo_productos.setValueAt(metodos.formateador_dinero().format(((iva / 100) * costo) + costo), i, 8);

                        modelo_productos.setValueAt(metodos.formateador_dinero().format((((iva / 100) * costo) + costo) * cantidad), i, 9);
                    } catch (Exception e) {
                        modelo_productos.setValueAt("0", i, 7);
                    }
                }
                break;

            case 4:

                for (int i = 0; i < jtabla.getRowCount(); i++) {
                    try {
                        double costo, costo_final, iva, cantidad, descuento;

                        cantidad = Double.parseDouble(modelo_productos.getValueAt(i, 3).toString());

                        costo = Double.parseDouble(metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 4).toString(), "."));
                        iva = Double.parseDouble(modelo_productos.getValueAt(i, 5).toString());
                        descuento = Double.parseDouble(modelo_productos.getValueAt(i, 6).toString());

                        costo = costo - (costo * (descuento / 100));

                        costo_final = ((iva / 100) * costo) + costo;

                        modelo_productos.setValueAt(metodos.formateador_dinero().format(costo_final), i, 7);

                    } catch (Exception e) {
                        System.out.println(e);
                        modelo_productos.setValueAt("0", i, 6);
                    }
                }

                break;
        }
    }

    private void btn_precargarActionPerformed(java.awt.event.ActionEvent evt) {
        String cod = "";
        switch (frm_main.rol_precios) {
            case 3:
                for (int i = 0; i < modelo_productos.getRowCount(); i++) {
                    cod = modelo_productos.getValueAt(i, 1).toString();

                    String consulta = "select p.precio_costo, p.iva "
                            + "from productos p "
                            + "where p.codigo_barras ='" + cod + "'";

                    ResultSet rs = DB_consultas_R_D.getTabla(consulta);
                    try {
                        while (rs.next()) {
                            modelo_productos.setValueAt(metodos.formateador_dinero().format(rs.getDouble("precio_costo")), i, 5);
                            modelo_productos.setValueAt(rs.getDouble("iva"), i, 6);
                        }
                        rs.close();

                        jtabla.setModel(modelo_productos);

                    } catch (SQLException ex) {
                        Logger.getLogger(jif_crear_ingreso_precios.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;
            case 4:
                cod = "";
                for (int i = 0; i < modelo_productos.getRowCount(); i++) {
                    cod = modelo_productos.getValueAt(i, 1).toString();

                    String consulta = "SELECT \n"
                            + "    p.codigo_barras,\n"
                            + "    p.descripcion,\n"
                            + "    COALESCE(d.venta, 0) AS venta,\n"
                            + "    COALESCE(d.valor_desc_1, 0) AS valor_desc_1,\n"
                            + "    COALESCE(d.valor_desc_2, 0) AS valor_desc_2,\n"
                            + "    COALESCE(d.valor_s_y_t, 0) AS valor_s_y_t,\n"
                            + "    COALESCE(d.valor_credito, 0) AS valor_credito,\n"
                            + "    COALESCE(d.porcentaje_utilidad, p.porcentaje_utilidad) AS porcentaje_utilidad,\n"
                            + "    COALESCE(d.desc_n_1, 0) AS desc_n_1,\n"
                            + "    COALESCE(d.desc_n_2, 0) AS desc_n_2\n"
                            + "FROM \n"
                            + "    productos p\n"
                            + "JOIN \n"
                            + "    ingresos_productos_detalle d ON p.id = d.id_producto\n"
                            + "JOIN \n"
                            + "    ingresos_productos_cabecera c ON d.id_ingreso_cabecera = c.id\n"
                            + "WHERE \n"
                            + "    p.codigo_barras = '" + cod + "'\n"
                            + "ORDER BY \n"
                            + "    c.fecha DESC, c.hora DESC\n"
                            + "LIMIT 1;";

                    ResultSet rs = DB_consultas_R_D.getTabla(consulta);
                    try {
                        while (rs.next()) {
                            modelo_productos.setValueAt(rs.getDouble("porcentaje_utilidad"), i, 9);
                            modelo_productos.setValueAt(metodos.formateador_dinero().format(rs.getDouble("venta")), i, 10);
                            modelo_productos.setValueAt(metodos.formateador_dinero().format(rs.getDouble("valor_desc_1")), i, 11);
                            modelo_productos.setValueAt(metodos.formateador_dinero().format(rs.getDouble("valor_desc_2")), i, 12);
                            modelo_productos.setValueAt(metodos.formateador_dinero().format(rs.getDouble("valor_s_y_t")), i, 13);
                            modelo_productos.setValueAt(metodos.formateador_dinero().format(rs.getDouble("valor_credito")), i, 14);
                        }
                        rs.close();

                        jtabla.setModel(modelo_productos);

                    } catch (SQLException ex) {
                        Logger.getLogger(jif_crear_ingreso_precios.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;
        }
    }

    double porcentaje_s_y_t = 0,
            porcentaje_credito = 0;

    private void btn_calcular_precios_ventaActionPerformed(java.awt.event.ActionEvent evt) {
        switch (frm_main.rol_precios) {

            case 4:

                ResultSet rs = DB_consultas_R_D.getTabla("select * from configuraciones where id = 1");
                try {
                    while (rs.next()) {
                        porcentaje_s_y_t = rs.getDouble("porcentaje_s_y_t");
                        porcentaje_credito = rs.getDouble("porcentaje_credito");
                    }
                    rs.close();
                } catch (SQLException ex) {
                    System.out.println(ex);
                }

                for (int i = 0; i < jtabla.getRowCount(); i++) {
                    try {
                        double costo_iva_descuento, costo_iva_descuento_gasto, porcentaje_utilidad = 0, venta, valor_desc_1, valor_desc_2, valor_s_y_t, valor_credito;
                        try {
                            porcentaje_utilidad = Double.parseDouble(modelo_productos.getValueAt(i, 9).toString());
                        } catch (Exception e) {
                            porcentaje_utilidad = 0;
                        }
                        costo_iva_descuento = Double.parseDouble(metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 7).toString(), "."));
                        costo_iva_descuento_gasto = Double.parseDouble(metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 8).toString(), "."));

                        venta = (costo_iva_descuento_gasto / ((100 - porcentaje_utilidad) / 100));

                        valor_desc_1 = calcularDescuento(venta, costo_iva_descuento_gasto, 1);
                        valor_desc_2 = calcularDescuento(venta, costo_iva_descuento_gasto, 2);

                        valor_s_y_t = costo_iva_descuento / (porcentaje_s_y_t);
                        valor_credito = venta + (venta * (porcentaje_credito / 100));

                        int redondear = Integer.parseInt(txt_redondear.getText());

                        modelo_productos.setValueAt(metodos.formateador_dinero().format(metodos.redondearNumero(venta, redondear)), i, 10);
                        modelo_productos.setValueAt(metodos.formateador_dinero().format(metodos.redondearNumero(valor_desc_1, redondear)), i, 11);
                        modelo_productos.setValueAt(metodos.formateador_dinero().format(metodos.redondearNumero(valor_desc_2, redondear)), i, 12);
                        modelo_productos.setValueAt(metodos.formateador_dinero().format(metodos.redondearNumero(valor_s_y_t, redondear)), i, 13);
                        modelo_productos.setValueAt(metodos.formateador_dinero().format(metodos.redondearNumero(valor_credito, redondear)), i, 14);

                    } catch (Exception e) {
                        System.out.println(e);
                        modelo_productos.setValueAt("0", i, 6);
                    }
                }

                break;
        }
    }

    private void btn_calcular_utildiad_porcentajeActionPerformed(java.awt.event.ActionEvent evt) {
        int fila = jtabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
            DecimalFormat df = new DecimalFormat("0.00", symbols);

            try {
                double costo = Double.parseDouble(metodos.EliminaCaracteres(modelo_productos.getValueAt(fila, 8).toString(), "."));
                double venta = Double.parseDouble(metodos.EliminaCaracteres(modelo_productos.getValueAt(fila, 10).toString(), "."));

                if (costo > 0) {
                    double porcentaje_utilidad = ((venta - costo) / costo) * 100;
                    modelo_productos.setValueAt(df.format(porcentaje_utilidad), fila, 9);
                } else {
                    modelo_productos.setValueAt("0.00", fila, 9);
                }
            } catch (Exception ex) {
                modelo_productos.setValueAt("0.00", fila, 9);
            }
        }
    }

    private void btn_buscar_proveedorActionPerformed(java.awt.event.ActionEvent evt) {
        jd_buscar_proveedor_precios buscar = new jd_buscar_proveedor_precios(null, true);
        buscar.setVisible(true);
    }

    /**
     * Exporta la tabla del ingreso en edicion al formato World Office
     * (FECHA, CODIGO, UNIDAD, CANTIDAD, IVA, COSTO). Flujo del rol 3:
     * columnas 5=COSTO y 6=IVA.
     */
    private void btn_Exportar_WoroldActionPerformed(java.awt.event.ActionEvent evt) {
        jd_exportar_world_office j = new jd_exportar_world_office(null, false);
        j.LimpiarModelos();
        int dia, mes, ano;
        ano = jdate_fecha_entrada.getCalendar().get(Calendar.YEAR);
        mes = jdate_fecha_entrada.getCalendar().get(Calendar.MONTH) + 1;
        dia = jdate_fecha_entrada.getCalendar().get(Calendar.DAY_OF_MONTH);
        String fecha = (ano + "-" + mes + "-" + dia);

        jd_exportar_world_office.modelo.setColumnIdentifiers(new Object[]{"FECHA", "CODIGO", "UNIDAD", "CANTIDAD", "IVA", "COSTO"});

        for (int i = 0; i < modelo_productos.getRowCount(); i++) {
            String unidad = DBpreciosProductos.traerUnidad(modelo_productos.getValueAt(i, 1).toString());

            jd_exportar_world_office.modelo.addRow(new Object[]{fecha, modelo_productos.getValueAt(i, 1).toString(), unidad, modelo_productos.getValueAt(i, 3).toString(),
                metodos.ReemplazarCaracteres("" + (Double.parseDouble(modelo_productos.getValueAt(i, 6).toString()) / 100), ".", ","),
                metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 5).toString(), ".")});
        }

        jd_exportar_world_office.jtable_productos.setModel(jd_exportar_world_office.modelo);
        j.show();
    }

    public double calcularDescuento(double venta, double costo, int tipo) {
        double valor_descuento = 0.0;

        double porcentaje_utilidad = 0.0;
        if (costo > 0) {
            porcentaje_utilidad = ((venta - costo) / costo) * 100;
        } else {
            System.out.println("El costo es 0, no se puede calcular el porcentaje de utilidad.");
            return venta;
        }

        String consulta = "SELECT utilidad, descuento FROM descuentos WHERE tipo = " + tipo + " ORDER BY utilidad ASC";
        try (Connection con = DB_consultas_R_D.getConexion(); PreparedStatement ps = con.prepareStatement(consulta); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                double utilidad = rs.getDouble("utilidad");
                double descuento = rs.getDouble("descuento");

                if (porcentaje_utilidad <= utilidad) {
                    double utilidad_monetaria = venta - costo;
                    valor_descuento = utilidad_monetaria * (descuento / 100);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return venta - valor_descuento;
    }

    // Variables declaration
    public static javax.swing.JButton btn_Exportar_Worold;
    public static javax.swing.JButton btn_buscar;
    public static javax.swing.JButton btn_buscar_proveedor;
    public static javax.swing.JButton btn_calcular_costos;
    public static javax.swing.JButton btn_calcular_precios_venta;
    public static javax.swing.JButton btn_calcular_utildiad_porcentaje;
    public static javax.swing.JButton btn_crear_producto;
    public static javax.swing.JButton btn_guardar;
    public static javax.swing.JButton btn_imprimir;
    public static javax.swing.JButton btn_limpiar;
    public static javax.swing.JButton btn_precargar;
    public static javax.swing.JCheckBox chk_cerrar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    public static javax.swing.JComboBox<Contactos> jbox_proveedor;
    public static javax.swing.JComboBox<Contactos> jbox_transportador;
    public static com.toedter.calendar.JDateChooser jdate_fecha_entrada;
    private javax.swing.JPanel jpanel_dinero;
    private javax.swing.JPanel jpanel_dinero1;
    public static javax.swing.JTable jtabla;
    public static javax.swing.JLabel lbl_descuento;
    public static javax.swing.JLabel lbl_id;
    public static javax.swing.JLabel lbl_iva;
    public static javax.swing.JLabel lbl_sub_total;
    public static javax.swing.JLabel lbl_total_factura;
    public static javax.swing.JLabel lbl_total_items;
    public static javax.swing.JPanel pnl_ingreso;
    public static javax.swing.JTextField txt_codigo_barras;
    public static javax.swing.JTextField txt_no_factura;
    public static javax.swing.JTextArea txt_observacion;
    private javax.swing.JTextField txt_redondear;
}
