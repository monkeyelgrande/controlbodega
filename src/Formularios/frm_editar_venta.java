/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios;

import Metodos.TextPrompt;
import Metodos.metodos;
import Metodos.ver_factura_impresion;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBcontactos;
import conexiondb.DBfacturas_cabeceras;
import conexiondb.DBstock_productos;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.File;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PrinterName;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import modelos.Contactos;
import modelos.Facturas_cabeceras;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporterParameter;

/**
 *
 * @author Monkeyelgrande
 */
public class frm_editar_venta extends javax.swing.JDialog {

    /**
     * Creates new form frm_facturacion
     */
    public static DefaultTableModel modelo = new DefaultTableModel() { // modelo de la tabla
        @Override
        public boolean isCellEditable(int fila, int columna) { // solo se permiten editables la columan cantidad y precio
            if (columna == 3) { // Columna cantidad
                return columna == 3;
            }
            if (columna == 4) { // Columna precio
                return columna == 4;
            }
            return columna == 3;
        }

        @Override
        public Object getValueAt(int row, int col) { // Sobre escritura del metodo getValue

            if (col == 5) { // digo que la columna 5 (Total) sera igual a la siguiente opreacion
                Double i; // i sera igual a la cantidad
                try {
                    i = Double.parseDouble(getValueAt(row, 3).toString()); // capturo la cantidad
                } catch (Exception e) {
                    System.out.println(e);
                    i = 1.0;
                }

                Double d = Double.parseDouble(metodos.EliminaCaracteres(getValueAt(row, 4).toString(), ".")); // d sera igual al precio
                if (i != null && d != null) {
                    return metodos.formateador_dinero().format(i * d); // regreso el resultado de multiplicar la cantidad por el valor
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
    static DefaultTableModel modeloProductos = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };
    double total = 0;
    DecimalFormat formatea = new DecimalFormat("###,###.##");
    public static String fecha_vencimiento = "";
    public static boolean tipo_fac_cre_apart = false;
    public static double abono = 0;
    String nombre_impresora;
    int imprimirSiNo;
    int productos_repetidos;
    Calendar fecha = new GregorianCalendar();
    public static TableColumnModel columnModel = null;
    private TableRowSorter trsFiltro;

    public frm_editar_venta() {
        initComponents();
        TextPrompt orden = new TextPrompt("No. Factura", txt_codigo);
        TextPrompt busqueda = new TextPrompt("Busqueda por descripción", txt_Filtro);
        TextPrompt observacion = new TextPrompt("Observaciones", txt_observaciones);
        TextPrompt cantidad = new TextPrompt("Cant.", txt_cantidad);
        TextPrompt busqueda_codigo = new TextPrompt("Buscar codigo", txt_codigo_barras);
        Contactos cliente = new Contactos();

        cliente.MostrarNombreContactos(jbox_cliente);

        jbox_cliente.setSelectedItem(cliente.nombreDefectoVentaDiaria());
        lbl_id_cliente.setText("1");
        lbl_id_factura.setText(DB_consultas_R_D.cargarId("facturas_cabeceras"));
        modelo.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD"});
        jdate_fecha.setCalendar(fecha);
        nombre_impresora = DB_consultas_R_D.ImpresoraPredeterminada();
        imprimirSiNo = DB_consultas_R_D.Imprimir_si_no();
        productos_repetidos = DB_consultas_R_D.productos_repetidos();
        consulta();
        columnModel = jTable_filtro.getColumnModel();
        TamanosTablaVentas();
        txt_observaciones.setWrapStyleWord(true);
        metodos.EvitarTabEnJTextArea(txt_observaciones);
        metodos.addEscapeListenerWindowDialog(this);
        this.setLocationRelativeTo(this);

        jTable_filtro.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    int fila = jTable_filtro.getSelectedRow();
                    String codigo_barras = jTable_filtro.getValueAt(fila, 0).toString();
                    double can = 1;
                    try {
                        can = Double.parseDouble(txt_cantidad.getText());
                    } catch (Exception e) {
                        can = 1;
                    }
                    agregar_cod(codigo_barras, can);
                    txt_Filtro.setText("");
                    txt_cantidad.setText("1");
                    txt_cantidad.requestFocus();
                }
            }
        });
        jTable_filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_ENTER) {
                    int fila = jTable_filtro.getSelectedRow();
                    String codigo_barras = jTable_filtro.getValueAt(fila, 0).toString();
                    double can = 1;
                    try {
                        can = Double.parseDouble(txt_cantidad.getText());

                    } catch (Exception e) {
                        can = 1;

                    }
                    agregar_cod(codigo_barras, can);
                    txt_Filtro.setText("");
                    txt_cantidad.setText("1");
                    txt_cantidad.requestFocus();

                }
            }
        });
        jtabla_Ventas.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_DELETE) {

                    if (modelo.getRowCount() > 0) {

                        int fila = jtabla_Ventas.getSelectedRow();
                        if (jtabla_Ventas.getSelectedRowCount() < 1) {
                            JOptionPane.showMessageDialog(rootPane, "Seleccione un registro");
                        } else {
                            modelo.removeRow(fila);
                        }
                    }

                }
            }
        });
    }

    public static void calcular_total() {

        double suma = 0;
        for (int i = 0; i < jtabla_Ventas.getRowCount(); i++) {

            suma += Double.parseDouble(metodos.EliminaCaracteres(jtabla_Ventas.getValueAt(i, 5).toString(), "."));

        }
        lbl_total_factura.setText(metodos.formateador_dinero().format(suma));
    }

    public void consulta() {

        try {
            for (int i = 0; i < modeloProductos.getRowCount(); i++) {
                modeloProductos.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
        }
        modeloProductos.setColumnIdentifiers(new Object[]{"Código", "Descripción", "Unidad", "Precio"});
        ResultSet rs = DB_consultas_R_D.getTabla("select codigo_barras, descripcion, precio_venta, u.nombre as unidad from productos p, unidades_medidas u where p.id_unidad=u.id and COALESCE(p.estado, true) = true");

        try {
            while (rs.next()) {

                modeloProductos.addRow(new Object[]{rs.getString("codigo_barras"), rs.getString("descripcion"), rs.getString("unidad"), metodos.formateador_dinero().format(rs.getDouble("precio_venta"))});
            }
            rs.close();
            // asigna el modelo a la tabla
            jTable_filtro.setModel(modeloProductos);
            TamanosTablaVentas();
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public void TamanosTablaVentas() {
        columnModel.getColumn(0).setPreferredWidth(40);
        columnModel.getColumn(1).setPreferredWidth(600);
        columnModel.getColumn(2).setPreferredWidth(80);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        rgroup_tipo_factura = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla_Ventas = new javax.swing.JTable();
        btn_actualizar = new javax.swing.JButton();
        btn_limpiar = new javax.swing.JButton();
        lbl_total_factura = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        lbl_id_cliente = new javax.swing.JLabel();
        jbox_cliente = new javax.swing.JComboBox<>();
        jButton1 = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        txt_observaciones = new javax.swing.JTextArea();
        jPanel5 = new javax.swing.JPanel();
        txt_codigo_barras = new javax.swing.JTextField();
        txt_Filtro = new javax.swing.JTextField();
        txt_codigo = new javax.swing.JTextField();
        txt_cantidad = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable_filtro = new javax.swing.JTable();
        jdate_fecha = new com.toedter.calendar.JDateChooser();
        jPanel2 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        lbl_id_factura = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        rb_contado = new javax.swing.JRadioButton();
        rb_credito = new javax.swing.JRadioButton();
        chk_copia = new javax.swing.JCheckBox();
        jButton2 = new javax.swing.JButton();

        setTitle("Editar orden");
        setModal(true);
        setName(""); // NOI18N

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jPanel7.setBackground(new java.awt.Color(24, 30, 80));

        jtabla_Ventas.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jtabla_Ventas.setForeground(new java.awt.Color(0, 102, 102));
        jtabla_Ventas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jtabla_Ventas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jtabla_Ventas.setDoubleBuffered(true);
        jtabla_Ventas.setRowHeight(35);
        jtabla_Ventas.setSelectionBackground(new java.awt.Color(0, 153, 255));
        jtabla_Ventas.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jtabla_Ventas.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jtabla_Ventas);

        btn_actualizar.setFont(new java.awt.Font("Segoe UI Black", 0, 24)); // NOI18N
        btn_actualizar.setForeground(new java.awt.Color(0, 102, 102));
        btn_actualizar.setMnemonic('f');
        btn_actualizar.setText("Actualizar");
        btn_actualizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_actualizarActionPerformed(evt);
            }
        });

        btn_limpiar.setFont(new java.awt.Font("Segoe UI Black", 0, 24)); // NOI18N
        btn_limpiar.setForeground(new java.awt.Color(0, 102, 102));
        btn_limpiar.setMnemonic('l');
        btn_limpiar.setText("Limpiar");
        btn_limpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_limpiarActionPerformed(evt);
            }
        });

        lbl_total_factura.setBackground(new java.awt.Color(58, 159, 171));
        lbl_total_factura.setFont(new java.awt.Font("Yu Gothic Medium", 1, 24)); // NOI18N
        lbl_total_factura.setForeground(new java.awt.Color(255, 255, 255));
        lbl_total_factura.setText("0");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1330, Short.MAX_VALUE)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(btn_actualizar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_limpiar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbl_total_factura)))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_total_factura)
                    .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btn_actualizar, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btn_limpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(13, 13, 13))
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(58, 159, 171), 3));
        jPanel3.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel3.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(58, 159, 171));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Cliente");
        jPanel3.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(13, 14, -1, -1));

        jLabel7.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(58, 159, 171));
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Nombre");
        jPanel3.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(13, 51, -1, -1));

        lbl_id_cliente.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        lbl_id_cliente.setForeground(new java.awt.Color(58, 159, 171));
        lbl_id_cliente.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_id_cliente.setText("-");
        jPanel3.add(lbl_id_cliente, new org.netbeans.lib.awtextra.AbsoluteConstraints(87, 14, 41, -1));

        jbox_cliente.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jbox_cliente.setNextFocusableComponent(txt_codigo_barras);
        jbox_cliente.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jbox_clienteMouseClicked(evt);
            }
        });
        jbox_cliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbox_clienteActionPerformed(evt);
            }
        });
        jPanel3.add(jbox_cliente, new org.netbeans.lib.awtextra.AbsoluteConstraints(87, 45, 235, -1));

        jButton1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jButton1.setForeground(new java.awt.Color(51, 204, 0));
        jButton1.setMnemonic('k');
        jButton1.setText("+");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(328, 44, -1, -1));

        txt_observaciones.setColumns(20);
        txt_observaciones.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        txt_observaciones.setLineWrap(true);
        txt_observaciones.setRows(5);
        jScrollPane4.setViewportView(txt_observaciones);

        jPanel3.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(13, 81, 362, 190));

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));
        jPanel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(58, 159, 171), 3));

        txt_codigo_barras.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        txt_codigo_barras.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_codigo_barrasKeyPressed(evt);
            }
        });

        txt_Filtro.setBackground(new java.awt.Color(255, 204, 204));
        txt_Filtro.setFont(new java.awt.Font("Segoe UI Historic", 1, 18)); // NOI18N
        txt_Filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_FiltroKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_FiltroKeyTyped(evt);
            }
        });

        txt_codigo.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        txt_codigo.setForeground(new java.awt.Color(0, 102, 102));
        txt_codigo.setToolTipText("Precione tecla abajo para paar a busqueda por descripciíon");
        txt_codigo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_codigoFocusLost(evt);
            }
        });
        txt_codigo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_codigoKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_codigoKeyTyped(evt);
            }
        });

        txt_cantidad.setBackground(new java.awt.Color(255, 204, 204));
        txt_cantidad.setFont(new java.awt.Font("Segoe UI Historic", 1, 18)); // NOI18N
        txt_cantidad.setText("1");
        txt_cantidad.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_cantidadFocusGained(evt);
            }
        });
        txt_cantidad.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_cantidadKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_cantidadKeyTyped(evt);
            }
        });

        jTable_filtro.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jTable_filtro.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jTable_filtro.setRowHeight(32);
        jTable_filtro.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTable_filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTable_filtroKeyPressed(evt);
            }
        });
        jScrollPane3.setViewportView(jTable_filtro);

        jdate_fecha.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 714, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(txt_codigo_barras)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txt_codigo, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jdate_fecha, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(txt_cantidad, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txt_Filtro)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txt_codigo_barras, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txt_codigo, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jdate_fecha, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txt_cantidad, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 165, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(58, 159, 171), 3));

        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(0, 102, 102));
        jLabel6.setText("N° Salida");

        lbl_id_factura.setFont(new java.awt.Font("Segoe UI Black", 0, 24)); // NOI18N
        lbl_id_factura.setForeground(new java.awt.Color(153, 0, 102));
        lbl_id_factura.setText("N° Factura");

        jButton3.setBackground(new java.awt.Color(102, 0, 0));
        jButton3.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jButton3.setForeground(new java.awt.Color(255, 255, 255));
        jButton3.setMnemonic('w');
        jButton3.setText("Cerrar");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(35, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_id_factura)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(18, 18, 18)
                        .addComponent(jButton3)))
                .addGap(2, 2, 2))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel6))
                    .addComponent(jButton3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lbl_id_factura)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));
        jPanel6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(58, 159, 171), 3));

        rgroup_tipo_factura.add(rb_contado);
        rb_contado.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        rb_contado.setSelected(true);
        rb_contado.setText("Contado");
        rb_contado.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rb_contadoActionPerformed(evt);
            }
        });

        rgroup_tipo_factura.add(rb_credito);
        rb_credito.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        rb_credito.setText("Crédito");
        rb_credito.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rb_creditoActionPerformed(evt);
            }
        });

        chk_copia.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        chk_copia.setSelected(true);
        chk_copia.setText("Copia");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rb_credito)
                    .addComponent(rb_contado)
                    .addComponent(chk_copia))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rb_contado)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rb_credito)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(chk_copia)
                .addContainerGap())
        );

        jButton2.setFont(new java.awt.Font("Yu Gothic Medium", 1, 12)); // NOI18N
        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Available Updates.png"))); // NOI18N
        jButton2.setText("Actualizar Stock");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 387, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton2)))
            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

    public void TamanosTabla() {
        jtabla_Ventas.getTableHeader().setReorderingAllowed(false);
        TableColumnModel columnModel = jtabla_Ventas.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(60);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(600);
        columnModel.getColumn(3).setPreferredWidth(100);

    }

    public void agregar_cod(String codigo_barras, double cantidad) {

        if (DB_consultas_R_D.consultar_existencia_campo_String("codigo_barras", codigo_barras, "productos") == 1) {

            if (existe_en_tabla(codigo_barras) && productos_repetidos == 0) {
                double actuvalor = Double.parseDouble(extraer_cantidad_actual_by_codigo(codigo_barras));
                actuvalor += cantidad;
                modelo.setValueAt("" + actuvalor, posicion_en_jtable(codigo_barras), 3);
                txt_codigo_barras.setText("");

            } else {

                ResultSet rs = DB_consultas_R_D.getTabla("select id,codigo_barras,descripcion, precio_venta from productos where codigo_barras ='" + codigo_barras + "' AND COALESCE(estado, true) = true");

                try {
                    while (rs.next()) {
                        modelo.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"), cantidad, metodos.formateador_dinero().format(rs.getDouble("precio_venta"))});
                    }
                    rs.close();
                    jtabla_Ventas.setModel(modelo);
                } catch (SQLException ex) {
                    Logger.getLogger(frm_contactos.class.getName()).log(Level.SEVERE, null, ex);
                }

                TamanosTabla();
                calcular_total();

                txt_codigo_barras.setText("");
            }

        } else {
            JOptionPane.showMessageDialog(this, "El codigo de barras ingresado no se encuentra en la base de datos");
            txt_codigo_barras.setText("");
        }

    }

    private boolean existe_en_tabla(String codigo) {
        for (int i = 0; i < this.jtabla_Ventas.getRowCount(); i++) {
            if (this.jtabla_Ventas.getValueAt(i, 1).toString().equals(codigo)) {
                return true;
            }
        }
        return false;
    }

    private int posicion_en_jtable(String codigo) {
        int total = 0;
        for (int i = 0; i < this.jtabla_Ventas.getRowCount(); i++) {
            if (this.jtabla_Ventas.getValueAt(i, 1).toString().equals(codigo)) {
                return i;
            }
        }
        return 0;
    }

    private String extraer_cantidad_actual_by_codigo(String codigo) {
        String regresa = "";
        for (int i = 0; i < this.jtabla_Ventas.getRowCount(); i++) {
            if (this.jtabla_Ventas.getValueAt(i, 1).toString().equals(codigo)) {
                regresa = "" + jtabla_Ventas.getValueAt(i, 3);
            }
        }
        return regresa;
    }


    private void btn_limpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_limpiarActionPerformed
        limpiar();
    }//GEN-LAST:event_btn_limpiarActionPerformed

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
                agregar_cod(codigo_barras, cantidad);
            }
        }
    }//GEN-LAST:event_txt_codigo_barrasKeyPressed
    public void imprimir_factura() {
        try {
            int idFactura = Integer.parseInt(lbl_id_factura.getText());
            new Metodos.ImprimirFacturaPDF().imprimir(idFactura);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudo imprimir: " + e.getMessage());
        }
    }
    private void rb_contadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rb_contadoActionPerformed

    }//GEN-LAST:event_rb_contadoActionPerformed

    private void rb_creditoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rb_creditoActionPerformed

    }//GEN-LAST:event_rb_creditoActionPerformed

    private void txt_codigoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_codigoKeyTyped
        char c = evt.getKeyChar();
        if (Character.isLowerCase(c)) {
            String cad = ("" + c).toUpperCase();
            c = cad.charAt(0);
            evt.setKeyChar(c);
        }
    }//GEN-LAST:event_txt_codigoKeyTyped

    private void txt_codigoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_codigoFocusLost
        if (!txt_codigo.getText().equals("")) {
            if (DB_consultas_R_D.consultar_existencia_campo_String("codigo", txt_codigo.getText(), "facturas_cabeceras") >= 1) {
                JOptionPane.showMessageDialog(this, "EL codigo ingresado ya esta asosciado a otra factura");
                txt_codigo.setText("");
                txt_codigo.requestFocus();
            }
        }

    }//GEN-LAST:event_txt_codigoFocusLost

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void btn_actualizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_actualizarActionPerformed
        if (jtabla_Ventas.getRowCount() < 1) {
            JOptionPane.showMessageDialog(this, "Por favor agregue al menos un producto");
            txt_codigo_barras.requestFocus();
        } else {
            DBfacturas_cabeceras dbfactura = new DBfacturas_cabeceras();
            DBstock_productos dbStock = new DBstock_productos();  // ◄── NUEVO
            Facturas_cabeceras fc = new Facturas_cabeceras();

            int idFactura = Integer.parseInt(lbl_id_factura.getText());

            // ════════════════════════════════════════════════════════════════════
            // PRIMERO: Obtener bodega anterior ANTES de actualizar
            // ════════════════════════════════════════════════════════════════════
            int idBodegaAnterior = obtenerBodegaFactura(idFactura);

            // ════════════════════════════════════════════════════════════════════
            // Reversar stock de la venta anterior
            // ════════════════════════════════════════════════════════════════════
            reversarStockVentaAnterior(idFactura, idBodegaAnterior, dbStock);

            try {
                fc.setId(idFactura);
                fc.setId_cliente(Integer.parseInt(lbl_id_cliente.getText()));
                fc.setId_user(frm_main.id_user);

                int dia, mes, ano;
                ano = jdate_fecha.getCalendar().get(Calendar.YEAR);
                mes = jdate_fecha.getCalendar().get(Calendar.MARCH) + 1;
                dia = jdate_fecha.getCalendar().get(Calendar.DAY_OF_MONTH);
                fc.setFecha(ano + "-" + mes + "-" + dia);

                if (txt_codigo.getText().equals("")) {
                    fc.setCodigo("");
                } else {
                    fc.setCodigo(txt_codigo.getText());
                }
                fc.setTipo("Venta");

                if (rb_contado.isSelected()) {
                    fc.setTipo_pago(0);
                }
                if (rb_credito.isSelected()) {
                    fc.setTipo_pago(1);
                }

                fc.setHora(DB_consultas_R_D.obtener_hora());
                fc.setObservacion(txt_observaciones.getText());
                fc.setAnulado(1);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e);
            }

            if (dbfactura.Actualizar(fc) == 1) {
                Connection con = null;
                con = DB_consultas_R_D.getConexion();
                PreparedStatement psql = null;
                String SSQL = "delete from facturas_detalles where id_cabecera=" + lbl_id_factura.getText() + ";\n";

                int idBodegaNueva = idBodegaAnterior;  // Misma bodega (no hay selector)

                for (int i = 0; i < jtabla_Ventas.getRowCount(); i++) {
                    String can = "";
                    try {
                        can = "" + modelo.getValueAt(i, 3);
                    } catch (Exception e) {
                        can = "1";
                    }

                    int idProducto = Integer.parseInt(modelo.getValueAt(i, 0).toString());
                    double cantidad = Double.parseDouble(can);

                    SSQL += "INSERT INTO facturas_detalles (id,id_cabecera,id_producto,cantidad, subtotal, id_factura) "
                            + "VALUES ((select COALESCE(max(id),0)+1 from facturas_detalles)," + lbl_id_factura.getText() + "," + modelo.getValueAt(i, 0).toString() + "," + can + ","
                            + metodos.EliminaCaracteres(modelo.getValueAt(i, 4).toString(), ".") + ",0);\n";

                    // ════════════════════════════════════════════════════════════
                    // INTEGRACIÓN STOCK: Registrar nueva venta
                    // ════════════════════════════════════════════════════════════
                    dbStock.venta(
                            idProducto,
                            idBodegaNueva,
                            frm_main.id_user,
                            cantidad,
                            idFactura,
                            "Venta actualizada - Factura: " + lbl_id_factura.getText()
                    );
                }
                try {
                    psql = con.prepareStatement(SSQL);
                    psql.executeUpdate();
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(null, "Error al intentar almacenar la información factura detalles:\n"
                            + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
                }
                try {
                    psql.close();
                    con.close();
                } catch (SQLException ex) {
                    Logger.getLogger(frm_editar_venta.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (tipo_fac_cre_apart) {
                tipo_fac_cre_apart = false;
            }
            frm_facturas_ventas.btn_actualizar.doClick();
            this.dispose();
        }
    }//GEN-LAST:event_btn_actualizarActionPerformed
// ════════════════════════════════════════════════════════════════════════════
// MÉTODO AUXILIAR: Obtener bodega de una factura
// ════════════════════════════════════════════════════════════════════════════

    private int obtenerBodegaFactura(int idFactura) {
        int idBodega = 1; // Default
        String sql = "SELECT id_bodega FROM facturas_cabeceras WHERE id = " + idFactura;
        ResultSet rs = DB_consultas_R_D.getTabla(sql);
        try {
            if (rs.next()) {
                idBodega = rs.getInt("id_bodega");
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error obteniendo bodega: " + e);
        }
        return idBodega;
    }

// ════════════════════════════════════════════════════════════════════════════
// MÉTODO AUXILIAR: Reversar stock de venta anterior
// ════════════════════════════════════════════════════════════════════════════
    private void reversarStockVentaAnterior(int idFactura, int idBodega, DBstock_productos dbStock) {
        String sql = "SELECT id_producto, cantidad FROM facturas_detalles WHERE id_cabecera = " + idFactura;
        ResultSet rs = DB_consultas_R_D.getTabla(sql);
        try {
            while (rs.next()) {
                int idProducto = rs.getInt("id_producto");
                double cantidad = rs.getDouble("cantidad");

                dbStock.anularVenta(
                        idProducto,
                        idBodega,
                        frm_main.id_user,
                        cantidad,
                        idFactura,
                        "Anulación por edición de venta"
                );
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error reversando stock venta: " + e);
        }
    }
    private void txt_FiltroKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_FiltroKeyTyped
        txt_Filtro.addKeyListener(new KeyAdapter() {
            public void keyReleased(final KeyEvent e) {
                repaint();
                filtro();
            }
        });
        trsFiltro = new TableRowSorter(jTable_filtro.getModel());
        jTable_filtro.setRowSorter(trsFiltro);
    }//GEN-LAST:event_txt_FiltroKeyTyped

    private void txt_cantidadKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_cantidadKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            txt_Filtro.requestFocus();
        }
        if ((num == KeyEvent.VK_RIGHT)) {
            txt_Filtro.requestFocus();
        }
    }//GEN-LAST:event_txt_cantidadKeyPressed

    private void txt_cantidadKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_cantidadKeyTyped
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_cantidadKeyTyped

    private void txt_cantidadFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_cantidadFocusGained
        txt_cantidad.selectAll();
    }//GEN-LAST:event_txt_cantidadFocusGained

    private void txt_FiltroKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_FiltroKeyPressed
        int key = evt.getKeyCode();
        if ((key == KeyEvent.VK_DOWN)) {
            jTable_filtro.requestFocus();
            jTable_filtro.getSelectionModel().setSelectionInterval(0, 0);
        }
        if ((key == KeyEvent.VK_UP)) {
            txt_codigo.requestFocus();
        }
        if ((key == KeyEvent.VK_LEFT)) {
            txt_cantidad.requestFocus();
        }
    }//GEN-LAST:event_txt_FiltroKeyPressed

    private void jbox_clienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbox_clienteActionPerformed

        lbl_id_cliente.setText(DB_consultas_R_D.TraerIdCliente(jbox_cliente.getSelectedItem().toString()));

    }//GEN-LAST:event_jbox_clienteActionPerformed

    private void jbox_clienteMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jbox_clienteMouseClicked

    }//GEN-LAST:event_jbox_clienteMouseClicked

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        try {
            String name = JOptionPane.showInputDialog(this, "Ingrese el nombre del nuevo cliente:");
            if (name.equals("")) {
                JOptionPane.showMessageDialog(this, "Por favor ingrese un nombre");
            } else {

                DBcontactos dbclientes = new DBcontactos();
                Contactos contacto = new Contactos();
                String idnuevo = DB_consultas_R_D.TraerIdMaximoNuevoContacto();

                try {
                    contacto.setId(Integer.parseInt(idnuevo));
                } catch (Exception e) {

                }
                contacto.setNombre(name);

                dbclientes.Guardar(contacto);

                DefaultComboBoxModel modeloCombo = (DefaultComboBoxModel) jbox_cliente.getModel();
                modeloCombo.addElement(new Contactos(Integer.parseInt(idnuevo), name));
                jbox_cliente.setModel(modeloCombo);
                jbox_cliente.setSelectedItem(name);

            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTable_filtroKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTable_filtroKeyPressed
        int key = evt.getKeyCode();
        if ((key == KeyEvent.VK_LEFT)) {
            txt_Filtro.requestFocus();
        }
    }//GEN-LAST:event_jTable_filtroKeyPressed

    private void txt_codigoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_codigoKeyPressed
        int key = evt.getKeyCode();
        if ((key == KeyEvent.VK_DOWN)) {
            txt_Filtro.requestFocus();
        }

    }//GEN-LAST:event_txt_codigoKeyPressed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        consulta();
    }//GEN-LAST:event_jButton2ActionPerformed
    public void filtro() {
        int columnaABuscar = 1;
        String cadena = txt_Filtro.getText();
        trsFiltro.setRowFilter(RowFilter.regexFilter("(?i)" + cadena, columnaABuscar));
    }

    public void limpiar() {
        jdate_fecha.setCalendar(fecha);
        txt_codigo_barras.setText("");
        txt_codigo.setText("");
        if (jtabla_Ventas.getRowCount() < 1) {

        } else {
            for (int i = 0; i < jtabla_Ventas.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        }
        lbl_total_factura.setText("");
        txt_observaciones.setText("");
        txt_codigo_barras.requestFocus();
        rb_contado.setSelected(true);
        lbl_id_factura.setText(DB_consultas_R_D.cargarId("facturas_cabeceras"));
        tipo_fac_cre_apart = false;

    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    public static javax.swing.JButton btn_actualizar;
    private javax.swing.JButton btn_limpiar;
    private javax.swing.JCheckBox chk_copia;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTable jTable_filtro;
    public static javax.swing.JComboBox<Contactos> jbox_cliente;
    private com.toedter.calendar.JDateChooser jdate_fecha;
    public static javax.swing.JTable jtabla_Ventas;
    public static javax.swing.JLabel lbl_id_cliente;
    public static javax.swing.JLabel lbl_id_factura;
    public static javax.swing.JLabel lbl_total_factura;
    public static javax.swing.JRadioButton rb_contado;
    public static javax.swing.JRadioButton rb_credito;
    private javax.swing.ButtonGroup rgroup_tipo_factura;
    public static javax.swing.JTextField txt_Filtro;
    public static javax.swing.JTextField txt_cantidad;
    public static javax.swing.JTextField txt_codigo;
    public static javax.swing.JTextField txt_codigo_barras;
    public static javax.swing.JTextArea txt_observaciones;
    // End of variables declaration//GEN-END:variables

}
