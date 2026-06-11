/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios_internos;

import Alertas.crear_nuevo_producto;
import Formularios.frm_ingreso_mercancia;
import Formularios.frm_main;
import JDBuscar.jd_buscar_producto;
import JDBuscar.jd_buscar_proveedor;
import Metodos.TextPrompt;
import Metodos.metodos;
import Metodos.ver_factura_impresion;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBingresosMercancias;
import conexiondb.DBstock_productos;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Bodegas;
import modelos.Contactos;
import modelos.IngresosMercancias;
import modelos.Tipo_ingresos_mercancia;

/**
 *
 * @author Monkeyelgrande
 */
public class jif_crear_ingreso_mercancia extends javax.swing.JDialog {

    /**
     * Creates new form jif_crear_ingreso_mecancia
     */
    public static DefaultTableModel modelo_productos = null;
    static DecimalFormat formatea = new DecimalFormat("###,###");

    public jif_crear_ingreso_mercancia() {
        initComponents();
        inicializar_modelo();
        txt_descripcion.setWrapStyleWord(true);
        metodos.EvitarTabEnJTextArea(txt_descripcion);

        Contactos proveedor = new Contactos();
        Tipo_ingresos_mercancia tipos_ingresos = new Tipo_ingresos_mercancia();
        proveedor.MostrarNombreProveedores(jbox_proveedor);
        proveedor.MostrarNombreContactos(jbox_transportador);

        // Doble clic en lbl_proveedor abre buscador de proveedores
        lbl_proveedor.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lbl_proveedor.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    jd_buscar_proveedor dialog = new jd_buscar_proveedor(
                            (java.awt.Frame) null, true);
                    dialog.setVisible(true);

                    Contactos seleccionado = dialog.getContactoSeleccionado();
                    if (seleccionado != null) {
                        // Buscar en el combo el item con el mismo ID
                        for (int i = 0; i < jbox_proveedor.getItemCount(); i++) {
                            if (jbox_proveedor.getItemAt(i).getId() == seleccionado.getId()) {
                                jbox_proveedor.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                }
            }
        });

        Bodegas bod = new Bodegas();
        bod.mostrarBodegas(jbox_bodega);

        tipos_ingresos.mostrarTipos_ingreso(jbox_tipo);
        lbl_id.setText(DB_consultas_R_D.cargarId("ingresos_mercancias_cabecera"));
        Calendar fecha = new GregorianCalendar();
        jdate_fecha_entrada.setCalendar(fecha);
        this.setLocationRelativeTo(this);
        metodos.addEscapeListenerWindowDialog(this);
        limpiar();
        actualizar_stock();
        TextPrompt pago_abono = new TextPrompt("Abono", txt_abono);
        TextPrompt cod_pago = new TextPrompt("Cod. Pago", txt_cod_pago);

        jdate_fecha_pagos.setCalendar(fecha);
        jdate_fecha_vencimiento.setCalendar(fecha);

        modelo_pagos = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false; //Con esto conseguimos que la tabla no se pueda editar
            }

            @Override
            public void setValueAt(Object aValue, int row, int col) {
                super.setValueAt(aValue, row, col);
                calcular_total();
                fireTableDataChanged();
            }
        };
        modelo_pagos.setColumnIdentifiers(new Object[]{"ID", "Total", "Fecha", "Cod Pago"});

        jtabla.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_DELETE) {
                    quitar_productos(modelo_productos);
                }
            }
        });
        jtabla_Pagos.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_DELETE) {

                    int fila = jtabla_Pagos.getSelectedRow();
                    if (fila == -1) {
                        JOptionPane.showMessageDialog(null, "Seleccione un registro");
                    } else {
                        int dialogButton = JOptionPane.YES_NO_OPTION;
                        int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar este pago de factura? "
                                + "\nAl aceptar este mensaje se eliminara el registro seleccionado y es IRREVERSIBLE", "Alerta", dialogButton);
                        if (dialogResult == JOptionPane.YES_OPTION) {

                            try {
                                String id = jtabla_Pagos.getValueAt(fila, 0) + "";//suponiendo que el id lo muestras en la primera columna
                                if (DB_consultas_R_D.eliminar("pagos_ingresos", id)) {
                                    for (int i = 0; i < modelo_pagos.getRowCount(); i++) {
                                        if (modelo_pagos.getValueAt(i, 0).toString().equals(id)) {
                                            modelo_pagos.removeRow(i);
                                            calcular_total();
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });

        btn_imprimir.setVisible(false);

    }

    public static void inicializar_modelo() {

        modelo_productos = new DefaultTableModel() { // modelo de la tabla
            @Override
            public boolean isCellEditable(int fila, int columna) { // solo se permiten editables la columan cantidad y precio
                if (columna == 3) { // Columna cantidad
                    return columna == 3;
                }
                if (columna == 6) { // Columna cantidad
                    return columna == 6;
                }
                if (columna == 7) { // Columna cantidad
                    return columna == 7;
                }

                return columna == 3;
            }

            @Override
            public Object getValueAt(int row, int col) { // Sobre escritura del metodo getValue
                if (col == 5) { // digo que la columna 5 (Total) sera igual a la siguiente opreacion
                    Double Agregar; // i sera igual a la cantidad
                    try {
                        Agregar = Double.parseDouble((String) getValueAt(row, 3)); // capturo la cantidad
                    } catch (Exception e) {
                        Agregar = 1.0;
                    }

//                System.out.println(getValueAt(row, 4).toString());
                    Double Actual = Double.parseDouble(getValueAt(row, 4).toString()); // d sera igual al precio
                    if (Agregar != null && Actual != null) {
                        return metodos.formateador_decimal_punto_para_decimal().format(Agregar + Actual); // regreso el resultado de multiplicar la cantidad por el valor
                    } else {
                        return 0d;
                    }
                }

                return super.getValueAt(row, col);

            }

            @Override
            public void setValueAt(Object aValue, int row, int col) {
                super.setValueAt(aValue, row, col);
                calcular_total();
                fireTableDataChanged();
            }

        };
        modelo_productos.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANT. AGREGAR", "ACTUAL", "NUEVO TOTAL", "PRECIO COSTO", "PRECIO VENTA"});

    }
    public static double total_costo = 0;
    public static double total_efectivo = 0;

    public static double saldo = 0;
    public static double total_utilidad = 0;

    public static void calcular_total() {

        try {

            total_costo = 0;
            total_efectivo = 0;

            for (int i = 0; i < jif_crear_ingreso_mercancia.modelo_productos.getRowCount(); i++) {
                double precio = Double.parseDouble(metodos.EliminaCaracteres(jif_crear_ingreso_mercancia.modelo_productos.getValueAt(i, 6).toString(), "."));
                double cantidad = Double.parseDouble(jif_crear_ingreso_mercancia.modelo_productos.getValueAt(i, 3).toString());
                total_costo += (precio * cantidad);
            }
            try {

                for (int i = 0; i < jif_crear_ingreso_mercancia.modelo_pagos.getRowCount(); i++) {
                    total_efectivo += Double.parseDouble(metodos.EliminaCaracteres(jif_crear_ingreso_mercancia.modelo_pagos.getValueAt(i, 1).toString(), "."));
                }
            } catch (Exception e) {
                System.out.println("Tabla de pagos vacia");
            }

            saldo = total_costo - total_efectivo;

            lbl_total_costo.setText(formatea.format(total_costo) + "");
            lbl_efectivo.setText(formatea.format(total_efectivo) + "");
            lbl_saldo.setText(formatea.format(saldo) + "");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(jtabla, "Por favor verifique los precios y cantidades en la tabla");
        }

    }

    public void quitar_productos(DefaultTableModel model) {
        if (model.getRowCount() > 0) {

            int fila = this.jtabla.getSelectedRow();
            if (this.jtabla.getSelectedRowCount() < 1) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro");
            } else {
                model.removeRow(fila);
//                calcular_total();
            }
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        pnl_ingreso = new javax.swing.JPanel();
        btn_limpiar = new javax.swing.JButton();
        btn_guardar = new javax.swing.JButton();
        chk_cerrar = new javax.swing.JCheckBox();
        rbtn_pendiente = new javax.swing.JRadioButton();
        rbtn_recibido = new javax.swing.JRadioButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        txt_descripcion = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txt_codigo_barras = new javax.swing.JTextField();
        btn_buscar = new javax.swing.JButton();
        btn_imprimir = new javax.swing.JButton();
        btn_crear_producto = new javax.swing.JButton();
        btn_importar = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jbox_proveedor = new javax.swing.JComboBox<>();
        lbl_proveedor = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jbox_tipo = new javax.swing.JComboBox<>();
        jLabel23 = new javax.swing.JLabel();
        jdate_fecha_entrada = new com.toedter.calendar.JDateChooser();
        lbl_id = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        txt_no_factura = new javax.swing.JTextField();
        jbox_transportador = new javax.swing.JComboBox<>();
        jLabel22 = new javax.swing.JLabel();
        jbox_bodega = new javax.swing.JComboBox<>();
        jLabel24 = new javax.swing.JLabel();
        btn_buscar_proveedor = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();
        jpanel_dinero = new javax.swing.JPanel();
        txt_abono = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla_Pagos = new javax.swing.JTable();
        txt_cod_pago = new javax.swing.JTextField();
        btn_agregar_pago = new javax.swing.JButton();
        jdate_fecha_pagos = new com.toedter.calendar.JDateChooser();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel27 = new javax.swing.JLabel();
        lbl_total_costo = new javax.swing.JLabel();
        lbl_efectivo = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        lbl_saldo = new javax.swing.JLabel();
        jpanel_vencimiento = new javax.swing.JPanel();
        jbox_plazo = new javax.swing.JComboBox<>();
        jdate_fecha_vencimiento = new com.toedter.calendar.JDateChooser();

        setTitle("Ingreso de mercancia");
        setBackground(new java.awt.Color(0, 51, 153));
        setModal(true);
        setResizable(false);

        pnl_ingreso.setBackground(new java.awt.Color(255, 255, 255));
        pnl_ingreso.setForeground(new java.awt.Color(204, 0, 51));

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

        buttonGroup1.add(rbtn_pendiente);
        rbtn_pendiente.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        rbtn_pendiente.setForeground(new java.awt.Color(153, 0, 0));
        rbtn_pendiente.setText("Pendiente");

        buttonGroup1.add(rbtn_recibido);
        rbtn_recibido.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        rbtn_recibido.setForeground(new java.awt.Color(0, 153, 51));
        rbtn_recibido.setSelected(true);
        rbtn_recibido.setText("Recibido");

        txt_descripcion.setColumns(20);
        txt_descripcion.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_descripcion.setLineWrap(true);
        txt_descripcion.setRows(5);
        jScrollPane3.setViewportView(txt_descripcion);

        javax.swing.GroupLayout pnl_ingresoLayout = new javax.swing.GroupLayout(pnl_ingreso);
        pnl_ingreso.setLayout(pnl_ingresoLayout);
        pnl_ingresoLayout.setHorizontalGroup(
            pnl_ingresoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_ingresoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_ingresoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnl_ingresoLayout.createSequentialGroup()
                        .addGroup(pnl_ingresoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chk_cerrar)
                            .addGroup(pnl_ingresoLayout.createSequentialGroup()
                                .addComponent(btn_guardar, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btn_limpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(pnl_ingresoLayout.createSequentialGroup()
                                .addComponent(rbtn_pendiente)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(rbtn_recibido)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 551, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnl_ingresoLayout.setVerticalGroup(
            pnl_ingresoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnl_ingresoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnl_ingresoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btn_limpiar)
                    .addComponent(btn_guardar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnl_ingresoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbtn_pendiente)
                    .addComponent(rbtn_recibido))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chk_cerrar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel1.setBackground(new java.awt.Color(225, 233, 236));
        jPanel1.setForeground(new java.awt.Color(225, 233, 236));

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(57, 75, 85));
        jLabel1.setText("Codigo de barras");

        txt_codigo_barras.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        txt_codigo_barras.setForeground(new java.awt.Color(57, 75, 85));
        txt_codigo_barras.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_codigo_barrasFocusLost(evt);
            }
        });
        txt_codigo_barras.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_codigo_barrasKeyPressed(evt);
            }
        });

        btn_buscar.setBackground(new java.awt.Color(250, 171, 26));
        btn_buscar.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        btn_buscar.setForeground(new java.awt.Color(0, 51, 51));
        btn_buscar.setText("Buscar producto");
        btn_buscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscarActionPerformed(evt);
            }
        });

        btn_imprimir.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_imprimir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Print24.png"))); // NOI18N
        btn_imprimir.setText("Imprimir");
        btn_imprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimirActionPerformed(evt);
            }
        });

        btn_crear_producto.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_crear_producto.setText("Crear Producto");
        btn_crear_producto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_crear_productoActionPerformed(evt);
            }
        });

        btn_importar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_importar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Invoice.png"))); // NOI18N
        btn_importar.setText("Importar");
        btn_importar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_importarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txt_codigo_barras, javax.swing.GroupLayout.PREFERRED_SIZE, 304, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_buscar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_crear_producto)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btn_importar, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_imprimir, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(txt_codigo_barras, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btn_buscar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btn_crear_producto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_imprimir, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_importar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jPanel2.setBackground(new java.awt.Color(84, 104, 120));

        jbox_proveedor.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jbox_proveedor.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jbox_proveedorKeyPressed(evt);
            }
        });

        lbl_proveedor.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_proveedor.setForeground(new java.awt.Color(246, 248, 243));
        lbl_proveedor.setText("Proveedor");

        jLabel21.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel21.setForeground(new java.awt.Color(246, 248, 243));
        jLabel21.setText("Tipo ingreso");

        jbox_tipo.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jbox_tipo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jbox_tipoKeyPressed(evt);
            }
        });

        jLabel23.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel23.setForeground(new java.awt.Color(246, 248, 243));
        jLabel23.setText("Fecha entrada");

        jdate_fecha_entrada.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N

        lbl_id.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_id.setForeground(new java.awt.Color(255, 153, 153));
        lbl_id.setText("numero");

        jLabel26.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel26.setForeground(new java.awt.Color(246, 248, 243));
        jLabel26.setText("Número de factura");

        txt_no_factura.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        txt_no_factura.setForeground(new java.awt.Color(57, 75, 85));
        txt_no_factura.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_no_facturaFocusLost(evt);
            }
        });
        txt_no_factura.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_no_facturaKeyPressed(evt);
            }
        });

        jbox_transportador.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jbox_transportador.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jbox_transportadorKeyPressed(evt);
            }
        });

        jLabel22.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel22.setForeground(new java.awt.Color(246, 248, 243));
        jLabel22.setText("Transportador");

        jbox_bodega.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jbox_bodega.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jbox_bodegaKeyPressed(evt);
            }
        });

        jLabel24.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(255, 255, 255));
        jLabel24.setText("Bodega");

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
                        .addComponent(lbl_proveedor)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jbox_proveedor, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_buscar_proveedor))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jbox_transportador, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel23)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jdate_fecha_entrada, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel21)
                        .addGap(18, 18, 18)
                        .addComponent(jbox_tipo, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel24)
                    .addComponent(jLabel26))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jbox_bodega, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txt_no_factura, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_id)
                .addContainerGap(82, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel21)
                            .addComponent(jbox_tipo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel26)
                            .addComponent(txt_no_factura, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lbl_id))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel23)
                                .addComponent(jdate_fecha_entrada, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel24)
                                .addComponent(jbox_bodega, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_buscar_proveedor, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(lbl_proveedor)
                                .addComponent(jbox_proveedor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel22)
                            .addComponent(jbox_transportador, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jtabla.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jtabla.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jtabla.setRowHeight(23);
        jScrollPane1.setViewportView(jtabla);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
        );

        jpanel_dinero.setBackground(new java.awt.Color(255, 255, 255));

        txt_abono.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        txt_abono.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_abonoFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_abonoFocusLost(evt);
            }
        });
        txt_abono.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_abonoKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_abonoKeyTyped(evt);
            }
        });

        jtabla_Pagos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_Pagos.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtabla_PagosKeyPressed(evt);
            }
        });
        jScrollPane2.setViewportView(jtabla_Pagos);

        txt_cod_pago.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        txt_cod_pago.setForeground(new java.awt.Color(57, 75, 85));
        txt_cod_pago.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_cod_pagoFocusLost(evt);
            }
        });
        txt_cod_pago.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_cod_pagoKeyPressed(evt);
            }
        });

        btn_agregar_pago.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_agregar_pago.setMnemonic('l');
        btn_agregar_pago.setText("Agregar pago");
        btn_agregar_pago.setToolTipText("Para agregar un pago debe primero guardar la factura");
        btn_agregar_pago.setBorder(null);
        btn_agregar_pago.setEnabled(false);
        btn_agregar_pago.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_agregar_pagoActionPerformed(evt);
            }
        });

        jdate_fecha_pagos.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jLabel27.setFont(new java.awt.Font("Tahoma", 1, 20)); // NOI18N
        jLabel27.setForeground(new java.awt.Color(51, 51, 51));
        jLabel27.setText("Total costo $:");

        lbl_total_costo.setFont(new java.awt.Font("Tahoma", 1, 20)); // NOI18N
        lbl_total_costo.setForeground(new java.awt.Color(0, 153, 0));
        lbl_total_costo.setText("0.0");

        lbl_efectivo.setFont(new java.awt.Font("Tahoma", 1, 20)); // NOI18N
        lbl_efectivo.setForeground(new java.awt.Color(0, 102, 255));
        lbl_efectivo.setText("0.0");

        jLabel30.setFont(new java.awt.Font("Tahoma", 1, 20)); // NOI18N
        jLabel30.setForeground(new java.awt.Color(51, 51, 51));
        jLabel30.setText("Total efectivo $:");

        jLabel31.setFont(new java.awt.Font("Tahoma", 1, 20)); // NOI18N
        jLabel31.setForeground(new java.awt.Color(51, 51, 51));
        jLabel31.setText("Total saldo $:");

        lbl_saldo.setFont(new java.awt.Font("Tahoma", 1, 20)); // NOI18N
        lbl_saldo.setForeground(new java.awt.Color(153, 0, 0));
        lbl_saldo.setText("0.0");

        javax.swing.GroupLayout jpanel_dineroLayout = new javax.swing.GroupLayout(jpanel_dinero);
        jpanel_dinero.setLayout(jpanel_dineroLayout);
        jpanel_dineroLayout.setHorizontalGroup(
            jpanel_dineroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpanel_dineroLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpanel_dineroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jpanel_dineroLayout.createSequentialGroup()
                        .addComponent(txt_cod_pago, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jdate_fecha_pagos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpanel_dineroLayout.createSequentialGroup()
                        .addComponent(txt_abono, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_agregar_pago, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 349, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpanel_dineroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpanel_dineroLayout.createSequentialGroup()
                        .addComponent(jLabel31)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_saldo))
                    .addGroup(jpanel_dineroLayout.createSequentialGroup()
                        .addComponent(jLabel30)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_efectivo))
                    .addGroup(jpanel_dineroLayout.createSequentialGroup()
                        .addComponent(jLabel27)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_costo)))
                .addContainerGap(227, Short.MAX_VALUE))
        );
        jpanel_dineroLayout.setVerticalGroup(
            jpanel_dineroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpanel_dineroLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpanel_dineroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpanel_dineroLayout.createSequentialGroup()
                        .addGroup(jpanel_dineroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel27)
                            .addComponent(lbl_total_costo))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpanel_dineroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel30)
                            .addComponent(lbl_efectivo))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpanel_dineroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel31)
                            .addComponent(lbl_saldo))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jpanel_dineroLayout.createSequentialGroup()
                        .addComponent(jSeparator1)
                        .addContainerGap())
                    .addGroup(jpanel_dineroLayout.createSequentialGroup()
                        .addGroup(jpanel_dineroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txt_cod_pago)
                            .addComponent(jdate_fecha_pagos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jpanel_dineroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_agregar_pago, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jpanel_dineroLayout.createSequentialGroup()
                                .addComponent(txt_abono, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(8, 8, 8))))
        );

        jpanel_vencimiento.setBackground(new java.awt.Color(0, 153, 153));

        jbox_plazo.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jbox_plazo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Vencimiento Manual", "8 días", "15 días", "30 días", "60 días" }));
        jbox_plazo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbox_plazoActionPerformed(evt);
            }
        });

        jdate_fecha_vencimiento.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N

        javax.swing.GroupLayout jpanel_vencimientoLayout = new javax.swing.GroupLayout(jpanel_vencimiento);
        jpanel_vencimiento.setLayout(jpanel_vencimientoLayout);
        jpanel_vencimientoLayout.setHorizontalGroup(
            jpanel_vencimientoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpanel_vencimientoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpanel_vencimientoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jbox_plazo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jdate_fecha_vencimiento, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(124, Short.MAX_VALUE))
        );
        jpanel_vencimientoLayout.setVerticalGroup(
            jpanel_vencimientoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpanel_vencimientoLayout.createSequentialGroup()
                .addContainerGap(10, Short.MAX_VALUE)
                .addComponent(jbox_plazo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jdate_fecha_vencimiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(13, 13, 13))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnl_ingreso, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpanel_dinero, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpanel_vencimiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpanel_vencimiento, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnl_ingreso, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jpanel_dinero, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_limpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_limpiarActionPerformed
        limpiar();
    }//GEN-LAST:event_btn_limpiarActionPerformed

    public void actualizar_stock() {
        // Stock is now managed via stock_productos table
    }

    public void limpiar() {
        lbl_id.setText(DB_consultas_R_D.cargarId("ingresos_mercancias_cabecera"));
        txt_codigo_barras.setText("");
        for (int i = 0; i < modelo_productos.getRowCount(); i++) {
            modelo_productos.removeRow(i);
            i -= 1;

        }

    }

    public static int id_tipo, id_proveedor, id_transportador, id_bodega;

    private void btn_guardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_guardarActionPerformed
        boolean actualizar = false;

        if (jtabla.getRowCount() < 1) {
            JOptionPane.showMessageDialog(this, "Por favor agregue al menos un producto");
            txt_codigo_barras.requestFocus();
        } else {
            int guardo_cabecera = 0;
            DBingresosMercancias dbingreso = new DBingresosMercancias();
            DBstock_productos dbStock = new DBstock_productos();
            IngresosMercancias ingreso_cabecera = new IngresosMercancias();

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
            try {
                ingreso_cabecera.setId_tipo_ingreso(jbox_tipo.getItemAt(jbox_tipo.getSelectedIndex()).getId());
            } catch (Exception e) {
                ingreso_cabecera.setId_tipo_ingreso(id_tipo);
            }

            // Bodega NUEVA (la seleccionada actualmente)
            int idBodegaNueva = 0;
            try {
                idBodegaNueva = jbox_bodega.getItemAt(jbox_bodega.getSelectedIndex()).getId();
                ingreso_cabecera.setId_bodega(idBodegaNueva);
            } catch (Exception e) {
                idBodegaNueva = id_bodega;
                ingreso_cabecera.setId_bodega(id_bodega);
            }

            int estadoNuevo = rbtn_recibido.isSelected() ? 1 : 0;
            int estadoAnterior = 0;
            int idBodegaAnterior = 0;

            if (rbtn_pendiente.isSelected()) {
                ingreso_cabecera.setEstado(0);
            } else {
                ingreso_cabecera.setEstado(1);
            }

            ingreso_cabecera.setId_user(frm_main.id_user);
            int hora, minutos, segundos;
            int dia, mes, ano;
            ano = jdate_fecha_entrada.getCalendar().get(Calendar.YEAR);
            mes = jdate_fecha_entrada.getCalendar().get(Calendar.MARCH) + 1;
            dia = jdate_fecha_entrada.getCalendar().get(Calendar.DAY_OF_MONTH);
            hora = jdate_fecha_entrada.getCalendar().get(Calendar.HOUR_OF_DAY);
            minutos = jdate_fecha_entrada.getCalendar().get(Calendar.MINUTE);
            segundos = jdate_fecha_entrada.getCalendar().get(Calendar.SECOND);
            ingreso_cabecera.setFecha_ingreso("'" + ano + "-" + mes + "-" + dia + "'");
            ingreso_cabecera.setHora("'" + hora + ":" + minutos + ":" + segundos + "'");
            ingreso_cabecera.setId(Integer.parseInt(lbl_id.getText()));
            ingreso_cabecera.setNo_factura(txt_no_factura.getText());
            ingreso_cabecera.setDescripcion(txt_descripcion.getText());

            ano = jdate_fecha_vencimiento.getCalendar().get(Calendar.YEAR);
            mes = jdate_fecha_vencimiento.getCalendar().get(Calendar.MARCH) + 1;
            dia = jdate_fecha_vencimiento.getCalendar().get(Calendar.DAY_OF_MONTH);
            ingreso_cabecera.setFecha_vencimiento("'" + ano + "-" + mes + "-" + dia + "'");

            int idIngresoCabecera = Integer.parseInt(lbl_id.getText());

            // ════════════════════════════════════════════════════════════════════
            // ◄── CORREGIDO: Obtener datos anteriores ANTES de actualizar
            // ════════════════════════════════════════════════════════════════════
            boolean esActualizacion = DB_consultas_R_D.consultarId(lbl_id.getText(), "ingresos_mercancias_cabecera") == 1;

            if (esActualizacion) {
                // PRIMERO: Obtener datos anteriores (antes de modificar la BD)
                int[] datosAnteriores = obtenerDatosAnteriores(idIngresoCabecera);
                estadoAnterior = datosAnteriores[0];
                idBodegaAnterior = datosAnteriores[1];

                // DESPUÉS: Actualizar la cabecera
                guardo_cabecera = dbingreso.Actualizar(ingreso_cabecera);
                actualizar = true;
            } else {
                guardo_cabecera = dbingreso.Guardar(ingreso_cabecera);
            }

            if (guardo_cabecera > 0) {
                Connection con = null;
                con = DB_consultas_R_D.getConexion();
                PreparedStatement psql = null;
                String sql_ingreso_detalle = "";

                if (actualizar) {
                    sql_ingreso_detalle = "DELETE FROM ingresos_mercancias_detalle WHERE id_ingreso_cabecera=" + lbl_id.getText() + ";";

                    // Reversar en la bodega ANTERIOR (ya la obtuvimos antes del update)
                    if (estadoAnterior == 1) {
                        reversarStockIngresoAnterior(idIngresoCabecera, idBodegaAnterior, dbStock);
                    }
                }

                String updates_precios_productos = "";

                for (int i = 0; i < modelo_productos.getRowCount(); i++) {
                    String can = "";
                    try {
                        can = "" + modelo_productos.getValueAt(i, 3);
                    } catch (Exception e) {
                        System.out.println(e);
                        can = "1";
                    }

                    int idProducto = Integer.parseInt(modelo_productos.getValueAt(i, 0).toString());
                    double cantidad = Double.parseDouble(can);
                    double costoUnitario = Double.parseDouble(
                            metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 6).toString(), ".")
                    );

                    sql_ingreso_detalle += "INSERT INTO ingresos_mercancias_detalle (id,id_producto,cantidad,id_ingreso_cabecera, precio_costo) "
                            + "VALUES ((select COALESCE(max(id),0)+1 from ingresos_mercancias_detalle)," + modelo_productos.getValueAt(i, 0).toString() + "," + can + ","
                            + lbl_id.getText() + "," + metodos.EliminaCaracteres(modelo_productos.getValueAt(i, 6).toString(), ".") + ");\n";

                    updates_precios_productos += "update productos set precio_costo=" + metodos.EliminaCaracteres("" + modelo_productos.getValueAt(i, 6).toString(), ".")
                            + ",precio_venta=" + metodos.EliminaCaracteres("" + modelo_productos.getValueAt(i, 7).toString(), ".") + " "
                            + "where codigo_barras='" + modelo_productos.getValueAt(i, 1).toString() + "';\n";

                    // Nuevo ingreso en la bodega NUEVA
                    if (estadoNuevo == 1) {
                        dbStock.ingreso(
                                idProducto,
                                idBodegaNueva,
                                frm_main.id_user,
                                cantidad,
                                costoUnitario,
                                idIngresoCabecera,
                                "Ingreso de mercancía - Factura: " + txt_no_factura.getText()
                        );
                    }
                }

                try {
                    psql = con.prepareStatement(sql_ingreso_detalle);
                    psql.executeUpdate();

                    psql = con.prepareStatement(updates_precios_productos);
                    psql.executeUpdate();

                    psql.close();
                    con.close();

                    btn_agregar_pago.setEnabled(true);
                } catch (SQLException ex) {
                    Logger.getLogger(frm_ingreso_mercancia.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            frm_ingreso_mercancia.btn_actualizar.doClick();
            limpiar();

            if (chk_cerrar.isSelected()) {
                this.dispose();
            } else {
                lbl_id.setText(DB_consultas_R_D.cargarId("ingresos_mercancias_cabecera"));
            }
        }
    }//GEN-LAST:event_btn_guardarActionPerformed

    /**
     * Obtiene el estado y bodega anterior de un ingreso
     *
     * @return int[] donde [0] = estado, [1] = id_bodega
     */
    private int[] obtenerDatosAnteriores(int idIngreso) {
        int[] datos = new int[]{0, 0};  // [estado, id_bodega]
        String sql = "SELECT estado, id_bodega FROM ingresos_mercancias_cabecera WHERE id = " + idIngreso;

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            if (rs.next()) {
                datos[0] = rs.getInt("estado");
                datos[1] = rs.getInt("id_bodega");
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error al obtener datos anteriores: " + e.getMessage());
        }

        return datos;
    }

    /**
     * Reversa el stock de un ingreso anterior (cuando se edita o elimina)
     */
    private void reversarStockIngresoAnterior(int idIngreso, int idBodega, DBstock_productos dbStock) {
        String sql = "SELECT imd.id_producto, imd.cantidad, imd.precio_costo "
                + "FROM ingresos_mercancias_detalle imd "
                + "WHERE imd.id_ingreso_cabecera = " + idIngreso;

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                int idProducto = rs.getInt("id_producto");
                double cantidad = rs.getDouble("cantidad");

                // Reversar el ingreso anterior
                dbStock.eliminarIngreso(
                        idProducto,
                        idBodega,
                        frm_main.id_user,
                        cantidad,
                        idIngreso,
                        "Reversión por edición de ingreso"
                );
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error al reversar stock: " + e.getMessage());
        }
    }
    private void btn_buscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_buscarActionPerformed
        jd_buscar_producto buscar_producto = new jd_buscar_producto(null, rootPaneCheckingEnabled);
        buscar_producto.show();
    }//GEN-LAST:event_btn_buscarActionPerformed

    private void txt_codigo_barrasKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_codigo_barrasKeyPressed
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

                agregar_cod(codigo_barras, cantidad, modelo_productos);
                calcular_total();
            }
        }
    }//GEN-LAST:event_txt_codigo_barrasKeyPressed
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
        int total = 0;
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

    public void agregar_cod(String codigo_barras, double cantidad, DefaultTableModel model) {

        if (DB_consultas_R_D.consultar_existencia_campo_String("codigo_barras", codigo_barras, "productos") == 1) {

            if (existe_en_tabla(codigo_barras)) {
                double actuvalor = Double.parseDouble(extraer_cantidad_actual_by_codigo(codigo_barras));
                actuvalor += cantidad;
                model.setValueAt("" + actuvalor, posicion_en_jtable(codigo_barras), 3);
                txt_codigo_barras.setText("");

            } else {
                String consulta = "select p.id, p.codigo_barras, p.descripcion, p.precio_costo, p.precio_venta, u.nombre as unidad, COALESCE(sp.stock, 0) as stock "
                        + "from productos p LEFT JOIN (SELECT id_producto, SUM(cantidad) as stock FROM stock_productos GROUP BY id_producto) sp ON sp.id_producto = p.id, unidades_medidas u where p.id_unidad=u.id "
                        + "and COALESCE(p.estado, true) = true and p.codigo_barras ='" + codigo_barras + "'";

//                System.out.println(consulta);
                ResultSet rs = DB_consultas_R_D.getTabla(consulta);
                try {
                    while (rs.next()) {
                        double stock = rs.getDouble("stock");

                        model.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"), cantidad, stock, null,
                            metodos.formateador_dinero().format(rs.getDouble("precio_costo")), metodos.formateador_dinero().format(rs.getDouble("precio_venta"))});

                    }
                    rs.close();

                    jtabla.setModel(model);

                } catch (SQLException ex) {
                    Logger.getLogger(frm_ingreso_mercancia.class
                            .getName()).log(Level.SEVERE, null, ex);
                }

//                calcular_total();
                TamanosTabla();

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
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(400);
        columnModel.getColumn(3).setPreferredWidth(100);

    }
    private void txt_codigo_barrasFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_codigo_barrasFocusLost

    }//GEN-LAST:event_txt_codigo_barrasFocusLost

    private void txt_no_facturaFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_no_facturaFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_no_facturaFocusLost

    private void txt_no_facturaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_no_facturaKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            jbox_tipo.requestFocus();
        }
    }//GEN-LAST:event_txt_no_facturaKeyPressed

    private void jbox_proveedorKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jbox_proveedorKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            txt_no_factura.requestFocus();
        }
    }//GEN-LAST:event_jbox_proveedorKeyPressed

    private void jbox_tipoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jbox_tipoKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            txt_codigo_barras.requestFocus();
        }
    }//GEN-LAST:event_jbox_tipoKeyPressed

    private void jbox_transportadorKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jbox_transportadorKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jbox_transportadorKeyPressed

    private void btn_imprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imprimirActionPerformed

        String[] botones = {"Media carta", "Carta completa"};
        ImageIcon icono = new ImageIcon("src/imagenes/page.png");
        int variable = JOptionPane.showOptionDialog(null, "Seleccione tamaño de impresión", "Tamaño hoja",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, icono, botones, null);
        if (variable >= 0) {

            if (variable == 0) {
                ver_factura_impresion imp = new ver_factura_impresion(); // opcion media carta
                imp.imprimir_ingreso_media_carta(lbl_id.getText());
            }
            if (variable == 1) {
                ver_factura_impresion imp = new ver_factura_impresion(); // opcion carta completa
                imp.imprimir_ingreso_carta_completa(lbl_id.getText());
            }
        }


    }//GEN-LAST:event_btn_imprimirActionPerformed

    private void jbox_bodegaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jbox_bodegaKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jbox_bodegaKeyPressed

    private void txt_abonoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_abonoFocusGained
        if (!txt_abono.getText().equals("")) {
            String texto = metodos.EliminaCaracteres(txt_abono.getText(), ".");
            txt_abono.setText(texto);
        }
    }//GEN-LAST:event_txt_abonoFocusGained

    private void txt_abonoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_abonoFocusLost
        if (!txt_abono.getText().equals("")) {
            double to = Double.parseDouble(txt_abono.getText());
            String nuevo = formatea.format(to);
            txt_abono.setText(nuevo);
        }
    }//GEN-LAST:event_txt_abonoFocusLost

    private void txt_abonoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_abonoKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            btn_agregar_pago.doClick();
        }
        if ((num == KeyEvent.VK_SPACE)) {
            txt_abono.setText(metodos.EliminaCaracteres(lbl_saldo.getText(), "."));
        }
    }//GEN-LAST:event_txt_abonoKeyPressed

    private void txt_abonoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_abonoKeyTyped
        char num = evt.getKeyChar();
        DB_consultas_R_D.validar_numeros(evt, num);
    }//GEN-LAST:event_txt_abonoKeyTyped

    private void jtabla_PagosKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtabla_PagosKeyPressed
        int key = evt.getKeyCode();
    }//GEN-LAST:event_jtabla_PagosKeyPressed

    private void txt_cod_pagoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_cod_pagoFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_cod_pagoFocusLost

    private void txt_cod_pagoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_cod_pagoKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_cod_pagoKeyPressed
    public static DefaultTableModel modelo_pagos = null;

    private void btn_agregar_pagoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_agregar_pagoActionPerformed

        if (modelo_productos.getRowCount() < 1) {
            JOptionPane.showMessageDialog(this, "Debe agregar al menos un producto");

        } else {
            if (jdate_fecha_pagos.getDate().before(jdate_fecha_entrada.getDate())) {
                JOptionPane.showMessageDialog(this, "La fecha de pago o abono de anticipo no puede ser menor que la fecha de entrada");
            } else {
                int id_pagos_ingresos = Integer.parseInt(DB_consultas_R_D.cargarId("pagos_ingresos"));
                try {

                    Double saldo = 0.0;
                    total_efectivo = 0.0;
                    saldo = Double.parseDouble(metodos.EliminaCaracteres(lbl_saldo.getText(), "."));
                    total_efectivo = Double.parseDouble(metodos.EliminaCaracteres(txt_abono.getText(), "."));

                    if (total_efectivo > saldo) {
                        JOptionPane.showMessageDialog(this, "El total de efectivo no debe ser mayor al saldo", "ALERTA", JOptionPane.WARNING_MESSAGE);

                    } else {

                        int dia, mes, ano;
                        ano = jdate_fecha_pagos.getCalendar().get(Calendar.YEAR);
                        mes = jdate_fecha_pagos.getCalendar().get(Calendar.MARCH) + 1;
                        dia = jdate_fecha_pagos.getCalendar().get(Calendar.DAY_OF_MONTH);
                        String cod_pago = txt_cod_pago.getText();
                        if (txt_cod_pago.getText().equals("")) {
                            cod_pago = "-";
                        }
                        modelo_pagos.addRow(new Object[]{id_pagos_ingresos, metodos.formateador_decimal().format(total_efectivo), ano + "-" + mes + "-" + dia, cod_pago});
                        jtabla_Pagos.setModel(jif_crear_ingreso_mercancia.modelo_pagos);

                        calcular_total();

                        Connection con = null;

                        con = DB_consultas_R_D.getConexion();
                        PreparedStatement psql = null;

                        String insert_pagos_ingresos = "INSERT INTO pagos_ingresos (id, id_ingresos_mercancias_cabecera, total, fecha, hora, cod_pago) VALUES ";

                        cod_pago = txt_cod_pago.getText();
                        double abono = Double.parseDouble(metodos.EliminaCaracteres(txt_abono.getText(), "."));

                        ano = jdate_fecha_pagos.getCalendar().get(Calendar.YEAR);
                        mes = jdate_fecha_pagos.getCalendar().get(Calendar.MARCH) + 1;
                        dia = jdate_fecha_pagos.getCalendar().get(Calendar.DAY_OF_MONTH);

                        insert_pagos_ingresos += "(" + id_pagos_ingresos + "," + lbl_id.getText()
                                + "," + abono + ",'" + ano + "-" + mes + "-" + dia + "','"
                                + DB_consultas_R_D.obtener_hora() + "','" + cod_pago + "');\n";

                        try {
                            psql = con.prepareStatement(insert_pagos_ingresos);
                            psql.executeUpdate();
                        } catch (SQLException e) {
                            System.out.println("Error al intentar almacenar la información PAGOS FACTURAS: " + e + " \nposiblemente en blanco");
                        }

                        try {
                            psql.close();
                            con.close();
                            frm_ingreso_mercancia.btn_actualizar.doClick();
                            txt_abono.setText("");
                            txt_cod_pago.setText("");

                        } catch (SQLException ex) {
                            System.out.println(ex);
                        }
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "ERROR AL INGRESAR LA EL EFECTIVO SOLO NÚMEROS");
                }
            }
        }
    }//GEN-LAST:event_btn_agregar_pagoActionPerformed

    private void jbox_plazoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbox_plazoActionPerformed
        int pos = jbox_plazo.getSelectedIndex();
        Calendar fecha = jdate_fecha_entrada.getCalendar();

        switch (pos) {
            case 0:
                jdate_fecha_vencimiento.setCalendar(fecha);
                break;
            case 1:
                fecha.add(Calendar.DAY_OF_YEAR, 8);
                jdate_fecha_vencimiento.setCalendar(fecha);
                break;
            case 2:
                fecha.add(Calendar.DAY_OF_YEAR, 15);
                jdate_fecha_vencimiento.setCalendar(fecha);
                break;
            case 3:
                fecha.add(Calendar.DAY_OF_YEAR, 30);
                jdate_fecha_vencimiento.setCalendar(fecha);
                break;
            case 4:
                fecha.add(Calendar.DAY_OF_YEAR, 60);
                jdate_fecha_vencimiento.setCalendar(fecha);
                break;
        }
    }//GEN-LAST:event_jbox_plazoActionPerformed

    private void btn_crear_productoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_crear_productoActionPerformed
        jif_crear_producto j = new jif_crear_producto();

        j.formulario = "ingreso_mercancia";
        j.jtxt_descripcion.requestFocus();
        j.chk_cerrar.setEnabled(false);

        j.show();
    }//GEN-LAST:event_btn_crear_productoActionPerformed

    private void btn_importarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_importarActionPerformed
        jif_importar_ingreso_productos jd = new jif_importar_ingreso_productos();
        jd.show();
    }//GEN-LAST:event_btn_importarActionPerformed

    private void btn_buscar_proveedorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_buscar_proveedorActionPerformed
        jd_buscar_proveedor dialog = new jd_buscar_proveedor((java.awt.Frame) null, true);
        dialog.setVisible(true);

        Contactos seleccionado = dialog.getContactoSeleccionado();
        if (seleccionado != null) {
            for (int i = 0; i < jbox_proveedor.getItemCount(); i++) {
                if (jbox_proveedor.getItemAt(i).getId() == seleccionado.getId()) {
                    jbox_proveedor.setSelectedIndex(i);
                    break;
                }
            }
        }
    }//GEN-LAST:event_btn_buscar_proveedorActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    public static javax.swing.JButton btn_agregar_pago;
    public static javax.swing.JButton btn_buscar;
    public static javax.swing.JButton btn_buscar_proveedor;
    public static javax.swing.JButton btn_crear_producto;
    public static javax.swing.JButton btn_guardar;
    public static javax.swing.JButton btn_importar;
    public static javax.swing.JButton btn_imprimir;
    public static javax.swing.JButton btn_limpiar;
    private javax.swing.ButtonGroup buttonGroup1;
    public static javax.swing.JCheckBox chk_cerrar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    public static javax.swing.JComboBox<Bodegas> jbox_bodega;
    public static javax.swing.JComboBox<String> jbox_plazo;
    public static javax.swing.JComboBox<Contactos> jbox_proveedor;
    public static javax.swing.JComboBox<Tipo_ingresos_mercancia> jbox_tipo;
    public static javax.swing.JComboBox<Contactos> jbox_transportador;
    public static com.toedter.calendar.JDateChooser jdate_fecha_entrada;
    public static com.toedter.calendar.JDateChooser jdate_fecha_pagos;
    public static com.toedter.calendar.JDateChooser jdate_fecha_vencimiento;
    private javax.swing.JPanel jpanel_dinero;
    private javax.swing.JPanel jpanel_vencimiento;
    public static javax.swing.JTable jtabla;
    public static javax.swing.JTable jtabla_Pagos;
    public static javax.swing.JLabel lbl_efectivo;
    public static javax.swing.JLabel lbl_id;
    private javax.swing.JLabel lbl_proveedor;
    public static javax.swing.JLabel lbl_saldo;
    public static javax.swing.JLabel lbl_total_costo;
    public static javax.swing.JPanel pnl_ingreso;
    public static javax.swing.JRadioButton rbtn_pendiente;
    public static javax.swing.JRadioButton rbtn_recibido;
    public static javax.swing.JTextField txt_abono;
    public static javax.swing.JTextField txt_cod_pago;
    public static javax.swing.JTextField txt_codigo_barras;
    public static javax.swing.JTextArea txt_descripcion;
    public static javax.swing.JTextField txt_no_factura;
    // End of variables declaration//GEN-END:variables

}
