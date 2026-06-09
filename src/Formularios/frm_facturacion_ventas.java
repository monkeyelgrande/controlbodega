/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios;

import JDBuscar.jd_buscar_contacto;
import Metodos.GeneradorOrdenAuto;
import Metodos.GeneradorOrdenAuto.ItemFacturado;
import Metodos.ImprimirTermica80MM;
import Metodos.TextPrompt;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBcontactos;
import conexiondb.DBfacturas_cabeceras;
import conexiondb.DBstock_productos;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Contactos;
import modelos.Facturas_cabeceras;
import modelos.ProductoImprimir;

/**
 *
 * @author Monkeyelgrande
 */
public class frm_facturacion_ventas extends javax.swing.JInternalFrame {

    /**
     * Creates new form frm_facturacion
     */
    public static DecimalFormat formatea = new DecimalFormat("###,###");
    public static DefaultTableModel modelo_ventas = new DefaultTableModel() { // modelo de la tabla
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
                    return formatea.format(i * d); // regreso el resultado de multiplicar la cantidad por el valor
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
    static DefaultTableModel modeloProductosFiltro = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }

    };
    public static String fecha_vencimiento = "";
    public static boolean tipo_fac_cre_apart = false;

    int imprimirSiNo;
    int productos_repetidos;
    Calendar fecha = new GregorianCalendar();
    static TableColumnModel columnModel = null;
    ArrayList<ProductoImprimir> productos;

    public frm_facturacion_ventas() throws IOException {
        initComponents();

        TextPrompt orden = new TextPrompt("No. Factura", txt_codigo);
        TextPrompt busqueda = new TextPrompt("Busqueda por descripción", txt_Filtro);
        TextPrompt observacion = new TextPrompt("Observaciones", txt_observaciones);
        TextPrompt cantidad = new TextPrompt("Cant.", txt_cantidad);
        TextPrompt busqueda_codigo = new TextPrompt("Buscar codigo", txt_codigo_barras);
        productos = new ArrayList<ProductoImprimir>();
        calcular_total();
        cargar_jcombos();

        lbl_id_cliente.setText("1");
        lbl_numerofactura.setText(DB_consultas_R_D.cargarId("facturas_cabeceras"));
        jdate_fecha.setCalendar(fecha);

        imprimirSiNo = DB_consultas_R_D.Imprimir_si_no();
        productos_repetidos = DB_consultas_R_D.productos_repetidos();
        consulta();
        columnModel = jtabla_filtro.getColumnModel();
        TamanosTablaVentas();
        txt_observaciones.setWrapStyleWord(true);
        metodos.EvitarTabEnJTextArea(txt_observaciones);
        metodos.BuscarEnTabla(txt_Filtro, jtabla_filtro);
        modelo_ventas.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "PRECIO", "TOTAL"});

        jtabla_filtro.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    int fila = jtabla_filtro.getSelectedRow();
                    String codigo_barras = (String) jtabla_filtro.getValueAt(fila, 0);
                    double can = 1;
                    try {
                        can = Double.parseDouble(txt_cantidad.getText());
                    } catch (Exception e) {
                        can = 1;
                    }
                    agregar_cod(codigo_barras, can);
                    txt_Filtro.selectAll();
                    txt_cantidad.setText("1");
                    txt_cantidad.requestFocus();
                }
            }
        });
        
        jtabla_filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_ENTER) {
                    int fila = jtabla_filtro.getSelectedRow();
                    String codigo_barras = (String) jtabla_filtro.getValueAt(fila, 0);
                    double can = 1;
                    try {
                        can = Double.parseDouble(txt_cantidad.getText());
                    } catch (Exception e) {
                        can = 1;
                    }
                    agregar_cod(codigo_barras, can);
                    txt_Filtro.selectAll();
                    txt_cantidad.setText("1");
                    txt_cantidad.requestFocus();

                }
            }
        });
        jtabla_Ventas.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_DELETE) {

                    if (modelo_ventas.getRowCount() > 0) {

                        int fila = jtabla_Ventas.getSelectedRow();
                        if (jtabla_Ventas.getSelectedRowCount() < 1) {
                            JOptionPane.showMessageDialog(rootPane, "Seleccione un registro");
                        } else {
                            modelo_ventas.removeRow(fila);
                        }
                    }

                }
            }
        });

        metodos.EstiloTablaMaterialGlobal(jtabla_Ventas);
    }

    public void cargar_jcombos() {
        txt_cedula_cliente.setText("0000");
        consulta_cliente_cedula();
        Contactos.mostrarContactosCedula(txt_cedula_cliente, false);
    }

    public static void calcular_total() {

        double suma = 0;
        for (int i = 0; i < jtabla_Ventas.getRowCount(); i++) {

            suma += Double.parseDouble(metodos.EliminaCaracteres(jtabla_Ventas.getValueAt(i, 5).toString(), "."));

        }
        lbl_total_factura.setText(formatea.format(suma));
    }

    public void consulta() {
        try {
            for (int i = 0; i < modeloProductosFiltro.getRowCount(); i++) {
                modeloProductosFiltro.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
        }
        modeloProductosFiltro.setColumnIdentifiers(new Object[]{"Código", "Descripción", "Unidad", "stock", "Venta"});
        String consulta = "select p.codigo_barras, p.precio_venta, p.descripcion, u.nombre as unidad, COALESCE(sp.stock, 0) as stock "
                + "from productos p LEFT JOIN (SELECT id_producto, SUM(cantidad) as stock FROM stock_productos GROUP BY id_producto) sp ON sp.id_producto = p.id, unidades_medidas u where p.id_unidad=u.id and p.tipo=1 and COALESCE(p.estado, true) = true";
//        System.out.println(consulta);
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        try {
            while (rs.next()) {

                modeloProductosFiltro.addRow(new Object[]{rs.getString("codigo_barras"), rs.getString("descripcion"),
                    rs.getString("unidad"), metodos.formateador_decimal().format(rs.getDouble("stock")), metodos.formateador_dinero().format(rs.getDouble("precio_venta"))
                });
            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla_filtro.setModel(modeloProductosFiltro);
            TamanosTablaConsulta();
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public void TamanosTablaConsulta() {
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(1).setPreferredWidth(700);
        columnModel.getColumn(2).setPreferredWidth(80);
        columnModel.getColumn(3).setPreferredWidth(80);
    }

    public void TamanosTablaVentas() {
        columnModel.getColumn(0).setPreferredWidth(30);
        columnModel.getColumn(1).setPreferredWidth(600);
        columnModel.getColumn(2).setPreferredWidth(60);
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
        btn_facturar = new javax.swing.JButton();
        btn_limpiar = new javax.swing.JButton();
        jLabel27 = new javax.swing.JLabel();
        lbl_total_factura = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        txt_codigo_barras = new javax.swing.JTextField();
        txt_Filtro = new javax.swing.JTextField();
        txt_codigo = new javax.swing.JTextField();
        txt_cantidad = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtabla_filtro = new javax.swing.JTable();
        jdate_fecha = new com.toedter.calendar.JDateChooser();
        jPanel2 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        lbl_numerofactura = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        rb_contado = new javax.swing.JRadioButton();
        rb_credito = new javax.swing.JRadioButton();
        chk_copia = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        lbl_nombre_cliente = new javax.swing.JLabel();
        txt_cedula_cliente = new javax.swing.JTextField();
        lbl_id_cliente = new javax.swing.JLabel();
        btn_buscar_cliente = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        txt_observaciones = new javax.swing.JTextArea();
        jButton2 = new javax.swing.JButton();

        setClosable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Salidas");
        setName(""); // NOI18N

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jPanel7.setBackground(new java.awt.Color(0, 177, 157));

        jtabla_Ventas.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jtabla_Ventas.setForeground(new java.awt.Color(0, 102, 102));
        jtabla_Ventas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jtabla_Ventas.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jtabla_Ventas.setDoubleBuffered(true);
        jtabla_Ventas.setRowHeight(25);
        jtabla_Ventas.setSelectionBackground(new java.awt.Color(0, 153, 255));
        jtabla_Ventas.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jtabla_Ventas.getTableHeader().setReorderingAllowed(false);
        jtabla_Ventas.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtabla_VentasKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(jtabla_Ventas);

        btn_facturar.setFont(new java.awt.Font("Yu Gothic Medium", 1, 24)); // NOI18N
        btn_facturar.setForeground(new java.awt.Color(0, 102, 102));
        btn_facturar.setMnemonic('f');
        btn_facturar.setText("Registrar");
        btn_facturar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_facturarActionPerformed(evt);
            }
        });

        btn_limpiar.setFont(new java.awt.Font("Yu Gothic Medium", 1, 24)); // NOI18N
        btn_limpiar.setForeground(new java.awt.Color(0, 102, 102));
        btn_limpiar.setMnemonic('l');
        btn_limpiar.setText("Limpiar");
        btn_limpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_limpiarActionPerformed(evt);
            }
        });

        jLabel27.setFont(new java.awt.Font("Tahoma", 1, 20)); // NOI18N
        jLabel27.setForeground(new java.awt.Color(51, 51, 51));
        jLabel27.setText("Total $:");

        lbl_total_factura.setFont(new java.awt.Font("Yu Gothic Medium", 1, 24)); // NOI18N
        lbl_total_factura.setForeground(new java.awt.Color(255, 255, 255));
        lbl_total_factura.setText("0.0");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1462, Short.MAX_VALUE)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(btn_facturar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_limpiar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel27)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_factura)))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 385, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel27)
                        .addComponent(lbl_total_factura))
                    .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btn_facturar, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btn_limpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));
        jPanel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(239, 68, 68), 3));

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

        jtabla_filtro.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jtabla_filtro.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_filtro.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jtabla_filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtabla_filtroKeyPressed(evt);
            }
        });
        jScrollPane3.setViewportView(jtabla_filtro);

        jdate_fecha.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(txt_codigo_barras)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txt_codigo, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jdate_fecha, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(txt_cantidad, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(239, 68, 68), 3));

        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(0, 102, 102));
        jLabel6.setText("N° Salida");

        lbl_numerofactura.setFont(new java.awt.Font("Segoe UI Black", 0, 24)); // NOI18N
        lbl_numerofactura.setForeground(new java.awt.Color(153, 0, 102));
        lbl_numerofactura.setText("N° Registro");

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
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_numerofactura)
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
                .addComponent(lbl_numerofactura)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));
        jPanel6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(239, 68, 68), 3));

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
                    .addComponent(chk_copia)
                    .addComponent(rb_contado)
                    .addComponent(rb_credito))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rb_contado)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rb_credito)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chk_copia)
                .addContainerGap(7, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(239, 68, 68), 3));

        jLabel3.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(31, 31, 31));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Cliente");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(31, 31, 31));
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Cédula");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(31, 31, 31));
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("Nombre");

        lbl_nombre_cliente.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        lbl_nombre_cliente.setForeground(new java.awt.Color(31, 31, 31));
        lbl_nombre_cliente.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_nombre_cliente.setText("-");

        txt_cedula_cliente.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txt_cedula_cliente.setForeground(new java.awt.Color(29, 115, 133));
        txt_cedula_cliente.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_cedula_clienteFocusGained(evt);
            }
        });
        txt_cedula_cliente.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_cedula_clienteKeyPressed(evt);
            }
        });

        lbl_id_cliente.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        lbl_id_cliente.setForeground(new java.awt.Color(31, 31, 31));
        lbl_id_cliente.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_id_cliente.setText("-");

        btn_buscar_cliente.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btn_buscar_cliente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/bucar.png"))); // NOI18N
        btn_buscar_cliente.setText("Buscar");
        btn_buscar_cliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscar_clienteActionPerformed(evt);
            }
        });

        txt_observaciones.setColumns(20);
        txt_observaciones.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        txt_observaciones.setLineWrap(true);
        txt_observaciones.setRows(5);
        jScrollPane4.setViewportView(txt_observaciones);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel3))
                        .addGap(26, 26, 26)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(lbl_id_cliente, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lbl_nombre_cliente, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(txt_cedula_cliente)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_buscar_cliente))))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lbl_id_cliente))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(txt_cedula_cliente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_buscar_cliente))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_nombre_cliente)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4)
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
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addContainerGap())
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2))
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
            double stock = DB_consultas_R_D.consultar_stock(codigo_barras);

            if (stock > 0) { // valida existencia de stock
                if (existe_en_tabla(codigo_barras) && productos_repetidos == 0) {
                    double actuvalor = Double.parseDouble(extraer_cantidad_actual_by_codigo(codigo_barras));
                    actuvalor += cantidad;
                    modelo_ventas.setValueAt("" + actuvalor, posicion_en_jtable(codigo_barras), 3);
                    txt_codigo_barras.setText("");

                } else {

                    ResultSet rs = DB_consultas_R_D.getTabla("select id,codigo_barras,descripcion, precio_costo, precio_venta from productos where codigo_barras ='" + codigo_barras + "' AND COALESCE(estado, true) = true");

                    try {
                        while (rs.next()) {

                            modelo_ventas.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"), cantidad,
                                formatea.format(rs.getDouble("precio_venta"))});

                        }
                        rs.close();
                        jtabla_Ventas.setModel(modelo_ventas);
                    } catch (SQLException ex) {
                        Logger.getLogger(frm_contactos.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    TamanosTabla();

                    txt_codigo_barras.setText("");
                }
            } else {
                int dialogButton = JOptionPane.YES_NO_OPTION;
                int dialogResult = JOptionPane.showConfirmDialog(null, "El producto selecionado no posee inventario\nCantidad: " + stock
                        + "\n¿Desea vender este producto sin inventario?\n"
                        + "el balance le dara negativo", "Alerta", dialogButton);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    if (existe_en_tabla(codigo_barras) && productos_repetidos == 0) {
                        double actuvalor = Double.parseDouble(extraer_cantidad_actual_by_codigo(codigo_barras));
                        actuvalor += cantidad;
                        modelo_ventas.setValueAt("" + actuvalor, posicion_en_jtable(codigo_barras), 3);
                        txt_codigo_barras.setText("");

                    } else {

                        ResultSet rs = DB_consultas_R_D.getTabla("select id,codigo_barras,descripcion, precio_venta from productos where codigo_barras ='" + codigo_barras + "' AND COALESCE(estado, true) = true");

                        try {
                            while (rs.next()) {

                                modelo_ventas.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"), cantidad,
                                    formatea.format(rs.getDouble("precio_venta"))});

                            }
                            rs.close();
                            jtabla_Ventas.setModel(modelo_ventas);
                        } catch (SQLException ex) {
                            System.out.println(ex);
                            Logger.getLogger(frm_contactos.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        TamanosTabla();

                        txt_codigo_barras.setText("");
                    }
                }
            }
            calcular_total();
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
            int idFactura = Integer.parseInt(lbl_numerofactura.getText());
            new Metodos.ImprimirFacturaPDF().imprimir(idFactura);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudo imprimir: " + e.getMessage());
        }
    }
    private void rb_contadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rb_contadoActionPerformed

    }//GEN-LAST:event_rb_contadoActionPerformed

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

    private void btn_facturarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_facturarActionPerformed
        if (jtabla_Ventas.getRowCount() < 1) {
            JOptionPane.showMessageDialog(this, "Por favor agregue al menos un producto");
            txt_codigo_barras.requestFocus();
        } else {
            boolean flag = true;

            if (flag) {
                lbl_numerofactura.setText(DB_consultas_R_D.cargarId("facturas_cabeceras"));
                DBfacturas_cabeceras dbfactura = new DBfacturas_cabeceras();
                DBstock_productos dbStock = new DBstock_productos();
                Facturas_cabeceras fc = new Facturas_cabeceras();

                try {
                    fc.setId(Integer.parseInt(lbl_numerofactura.getText()));
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
                    } else {
                        fc.setTipo_pago(1);
                    }

                    fc.setHora(DB_consultas_R_D.obtener_hora());
                    fc.setObservacion(txt_observaciones.getText());
                    fc.setAnulado(1);
                    fc.setId_bodega(1);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, e);
                }

                if (dbfactura.Guardar(fc) == 1) {
                    productos = new ArrayList<ProductoImprimir>();

                    Connection con = null;
                    con = DB_consultas_R_D.getConexion();
                    PreparedStatement psql = null;
                    String SSQL = "";

                    int idFactura = Integer.parseInt(lbl_numerofactura.getText());

                    List<ItemFacturado> itemsParaOrdenAuto = new ArrayList<>();

                    for (int i = 0; i < jtabla_Ventas.getRowCount(); i++) {
                        double can = 0;
                        try {
                            can = Double.parseDouble("" + modelo_ventas.getValueAt(i, 3));
                        } catch (Exception e) {
                            can = 1;
                        }

                        int idProducto = Integer.parseInt(modelo_ventas.getValueAt(i, 0).toString());

                        // Partición de la cantidad entre bodegas según las unidades
                        // de entrega del producto (o una sola bodega por las 4 reglas
                        // si no tiene unidades configuradas).
                        java.util.List<DBstock_productos.AsignacionBodega> asignaciones =
                                DBstock_productos.asignarBodegasEntrega(idProducto, can);

                        double subtotal = Double.parseDouble(
                                metodos.EliminaCaracteres(modelo_ventas.getValueAt(i, 4).toString(), "."));

                        // La factura al cliente mantiene UNA línea por producto (cantidad total).
                        SSQL += "INSERT INTO facturas_detalles (id,id_cabecera,id_producto,cantidad, subtotal, id_factura) "
                                + "VALUES ((select COALESCE(max(id),0)+1 from facturas_detalles),"
                                + lbl_numerofactura.getText() + ",'" + modelo_ventas.getValueAt(i, 0).toString() + "',"
                                + can + ", " + subtotal + ",0);\n";

                        // ════════════════════════════════════════════════════════
                        // INTEGRACIÓN STOCK: registrar venta por cada bodega asignada
                        // (descuenta cantidad). El subtotal se reparte proporcional.
                        // ════════════════════════════════════════════════════════
                        for (DBstock_productos.AsignacionBodega asig : asignaciones) {
                            double subAsig = (can != 0) ? subtotal * (asig.cantidad / can) : subtotal;
                            dbStock.venta(
                                    idProducto,
                                    asig.idBodega,
                                    frm_main.id_user,
                                    asig.cantidad,
                                    idFactura,
                                    "Venta directa - Factura: " + lbl_numerofactura.getText()
                            );
                            itemsParaOrdenAuto.add(new ItemFacturado(idProducto, asig.idBodega, asig.cantidad, subAsig));
                        }

                        if (imprimirSiNo == 1) {
                            ProductoImprimir prod = new ProductoImprimir();
                            prod.setNombre("" + modelo_ventas.getValueAt(i, 2));
                            prod.setCantidad("" + modelo_ventas.getValueAt(i, 3));
                            prod.setPunitario(metodos.formateador_dinero().format(subtotal));
                            prod.setPtotal("" + modelo_ventas.getValueAt(i, 5));
                            productos.add(prod);
                        }
                    }
                    try {
                        psql = con.prepareStatement(SSQL);
                        psql.executeUpdate();
                        psql.close();
                        con.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(frm_facturacion_ventas.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    // Generación automática de órdenes de entrega (tipo Salida) para
                    // los productos descargados de bodegas marcadas con genera_orden_automatica=TRUE.
                    // ORDEN_REFERENCIADA revierte el descuento de VENTA y reserva pendientes,
                    // evitando el doble descuento cuando luego se haga la ENTREGA física.
                    GeneradorOrdenAuto.generarSalidasDesdeVenta(
                            idFactura,
                            frm_main.id_user,
                            fc.getId_cliente(),
                            fc.getFecha(),
                            fc.getHora(),
                            fc.getCodigo(),
                            fc.getTipo_pago(),
                            fc.getObservacion(),
                            itemsParaOrdenAuto
                    );
                }

                if (tipo_fac_cre_apart) {
                    tipo_fac_cre_apart = false;
                }
                if (imprimirSiNo == 1) {
                    int dialogButton = JOptionPane.YES_NO_OPTION;
                    int dialogResult = JOptionPane.showConfirmDialog(null, "¿Desea imprimir la factura?", "Alerta", dialogButton);
                    if (dialogResult == JOptionPane.YES_OPTION) {

                        int dia, mes, ano;
                        ano = jdate_fecha.getCalendar().get(Calendar.YEAR);
                        mes = jdate_fecha.getCalendar().get(Calendar.MARCH) + 1;
                        dia = jdate_fecha.getCalendar().get(Calendar.DAY_OF_MONTH);
                        String fecha = dia + "-" + mes + "-" + ano;
                        Contactos cliente = new Contactos();
                        cliente = cliente.TraerContacto(lbl_id_cliente.getText());
                        String credicontado = "";
                        if (rb_contado.isSelected()) {
                            credicontado = "Contado";
                        } else {
                            credicontado = "Crédito";
                        }
                        ImprimirTermica80MM imprimir = new ImprimirTermica80MM(fecha, lbl_numerofactura.getText(), lbl_total_factura.getText(), cliente, productos,
                                frm_main.nombre_usuario, DB_consultas_R_D.obtener_hora(), credicontado);
                        try {
                            imprimir.imprime();
                            if (chk_copia.isSelected()) {
                                imprimir.imprime();
                            }
                        } catch (PrinterException ex) {
                            System.out.println(ex);
                        }
                    }

//                    imprimir_factura();
                }
                limpiar();

            }

        }
    }//GEN-LAST:event_btn_facturarActionPerformed

    private void txt_FiltroKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_FiltroKeyTyped

    }//GEN-LAST:event_txt_FiltroKeyTyped

    private void txt_cantidadKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_cantidadKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            txt_Filtro.requestFocus();
        }
        if ((num == KeyEvent.VK_RIGHT)) {
            txt_Filtro.requestFocus();
        }
        if ((num == KeyEvent.VK_DOWN)) {
            System.out.println("abajo pressed");
            jtabla_filtro.requestFocus();
            jtabla_filtro.getSelectionModel().setSelectionInterval(0, 0);
        }

    }//GEN-LAST:event_txt_cantidadKeyPressed

    private void txt_cantidadKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_cantidadKeyTyped
        int key = evt.getKeyCode();

        if ((key == KeyEvent.VK_DOWN)) {
            System.out.println("abajo");
            jtabla_filtro.requestFocus();
            jtabla_filtro.getSelectionModel().setSelectionInterval(0, 0);
        }
    }//GEN-LAST:event_txt_cantidadKeyTyped

    private void txt_cantidadFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_cantidadFocusGained
        txt_cantidad.selectAll();
    }//GEN-LAST:event_txt_cantidadFocusGained

    private void txt_FiltroKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_FiltroKeyPressed
        int key = evt.getKeyCode();
        if ((key == KeyEvent.VK_DOWN)) {

            jtabla_filtro.requestFocus();
            jtabla_filtro.getSelectionModel().setSelectionInterval(0, 0);
        }
        if ((key == KeyEvent.VK_UP)) {
            txt_codigo.requestFocus();
        }
        if ((key == KeyEvent.VK_LEFT)) {
            txt_cantidad.requestFocus();
        }
    }//GEN-LAST:event_txt_FiltroKeyPressed

    private void jtabla_filtroKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtabla_filtroKeyPressed
        int key = evt.getKeyCode();
        if ((key == KeyEvent.VK_LEFT)) {
            txt_Filtro.requestFocus();
        }
    }//GEN-LAST:event_jtabla_filtroKeyPressed

    private void txt_codigoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_codigoKeyPressed
        int key = evt.getKeyCode();
        if ((key == KeyEvent.VK_DOWN)) {
            txt_Filtro.requestFocus();
        }

    }//GEN-LAST:event_txt_codigoKeyPressed

    private void jtabla_VentasKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtabla_VentasKeyPressed
        int key = evt.getKeyCode();
        if ((key == KeyEvent.VK_X)) {
            int fila = jtabla_Ventas.getSelectedRow();
            if (fila < 0) {
                return;
            }
            String codigo_barras = (String) jtabla_Ventas.getValueAt(fila, 1);
            String consulta = "select p.descripcion,p.precio_costo,p.precio_venta,p.precio_venta2,p.precio_venta3 "
                    + "from productos p "
                    + "where p.codigo_barras='" + codigo_barras + "'";
//            System.out.println(consulta);
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);
            String texto = "";
            try {
                while (rs.next()) {

                    texto = "<html>Precios del producto<br>" + rs.getString("descripcion") + "<hr>"
                            + "<br><b>Precio de venta 1:  $ " + metodos.formateador_dinero().format(rs.getDouble("precio_venta")) + "</b>"
                            + "<br><b>Precio de venta 2:  $ " + metodos.formateador_dinero().format(rs.getDouble("precio_venta2")) + "</b>"
                            + "<br><b>Precio de venta 3:  $ " + metodos.formateador_dinero().format(rs.getDouble("precio_venta3")) + "</b>" + "<hr>"
                            + "<br><b>Existencias por bodega</b><br>"
                            + Metodos.StockBodegaDialog.tablaHtml(codigo_barras)
                            + "</html>";

                }
                JLabel label = new JLabel(texto);
                label.setFont(new Font("TimesRoman", Font.PLAIN, 16));
                JOptionPane.showMessageDialog(null, label);
                rs.close();

            } catch (SQLException ex) {
                Logger.getLogger(frm_contactos.class
                        .getName()).log(Level.SEVERE, null, ex);
            }

        }

    }//GEN-LAST:event_jtabla_VentasKeyPressed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        consulta();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void rb_creditoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rb_creditoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rb_creditoActionPerformed

    private void txt_cedula_clienteFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_cedula_clienteFocusGained
        txt_cedula_cliente.selectAll();
    }//GEN-LAST:event_txt_cedula_clienteFocusGained

    private void txt_cedula_clienteKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_cedula_clienteKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            if (txt_cedula_cliente.getText().equals("")) {
                txt_cedula_cliente.setText("0000");
            }
            consulta_cliente_cedula();
        }
        if ((num == KeyEvent.VK_DELETE)) {
            txt_cedula_cliente.setText("0000");
        }
    }//GEN-LAST:event_txt_cedula_clienteKeyPressed
    public void consulta_cliente_cedula() {
        if (txt_cedula_cliente.getText().equals("")) {
            JOptionPane.showMessageDialog(this, "Ingrese un numero de cedula");
        } else {
            if (DB_consultas_R_D.consultar_existencia_campo_String("cedula", txt_cedula_cliente.getText(), "contactos") == 1) {
                String cedula = txt_cedula_cliente.getText();
                ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from contactos where cedula ='" + cedula + "'");

                try {
                    while (rs.next()) {
                        lbl_id_cliente.setText(rs.getString("id"));
                        lbl_nombre_cliente.setText(rs.getString("nombre"));
                    }
                    rs.close();

                } catch (SQLException ex) {
                    Logger.getLogger(frm_contactos.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
                txt_codigo_barras.requestFocus();

            } else {
                String nombre = "";
                String direccion = "";
                String celular = "";
                JTextField name = new JTextField();
                JTextField dir = new JTextField();
                JTextField cell = new JTextField();
                Object[] message = {
                    "Los datos con (*) son obligatorios", "",
                    "\n", "",
                    "La cedula del cliente es:", txt_cedula_cliente.getText() + "\n",
                    "*Nombre:", name,
                    "Direccion:", dir,
                    "Celular:", cell,};
                int option = JOptionPane.showConfirmDialog(null, message, "Ingrese los datos", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (option == JOptionPane.OK_OPTION) {
                    nombre = name.getText();
                    direccion = dir.getText();
                    celular = cell.getText();

                    if (nombre.equals("")) {
                        JOptionPane.showMessageDialog(null, "Por favor ingrese el nombre");
                        txt_cedula_cliente.selectAll();
                    } else {
                        Contactos contacto = new Contactos();
                        DBcontactos dbcontacto = new DBcontactos();
                        contacto.setNombre(nombre);
                        contacto.setCedula(txt_cedula_cliente.getText());
                        contacto.setDireccion(direccion);
                        contacto.setContacto(celular);
                        dbcontacto.GuardarFactura(contacto);

                        consulta_cliente_cedula();
                    }
                }
            }
        }
    }
    private void btn_buscar_clienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_buscar_clienteActionPerformed
        jd_buscar_contacto buscar_cleinte = new jd_buscar_contacto(null, closable);
        buscar_cleinte.formulario = "venta";
        buscar_cleinte.show();
    }//GEN-LAST:event_btn_buscar_clienteActionPerformed

    public void limpiar() {
        jdate_fecha.setCalendar(fecha);
        txt_codigo_barras.setText("");
        txt_codigo.setText("");
        if (jtabla_Ventas.getRowCount() < 1) {

        } else {
            for (int i = 0; i < modelo_ventas.getRowCount(); i++) {
                modelo_ventas.removeRow(i);
                i -= 1;
            }
        }

        txt_observaciones.setText("");
        txt_codigo_barras.requestFocus();
        rb_contado.setSelected(true);
        lbl_numerofactura.setText(DB_consultas_R_D.cargarId("facturas_cabeceras"));
        tipo_fac_cre_apart = false;

    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_buscar_cliente;
    public static javax.swing.JButton btn_facturar;
    private javax.swing.JButton btn_limpiar;
    private javax.swing.JCheckBox chk_copia;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private com.toedter.calendar.JDateChooser jdate_fecha;
    public static javax.swing.JTable jtabla_Ventas;
    private javax.swing.JTable jtabla_filtro;
    public static javax.swing.JLabel lbl_id_cliente;
    public static javax.swing.JLabel lbl_nombre_cliente;
    public static javax.swing.JLabel lbl_numerofactura;
    public static javax.swing.JLabel lbl_total_factura;
    private javax.swing.JRadioButton rb_contado;
    private javax.swing.JRadioButton rb_credito;
    private javax.swing.ButtonGroup rgroup_tipo_factura;
    public static javax.swing.JTextField txt_Filtro;
    public static javax.swing.JTextField txt_cantidad;
    public static javax.swing.JTextField txt_cedula_cliente;
    private javax.swing.JTextField txt_codigo;
    public static javax.swing.JTextField txt_codigo_barras;
    private javax.swing.JTextArea txt_observaciones;
    // End of variables declaration//GEN-END:variables

}
