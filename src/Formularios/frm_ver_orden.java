/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios;

import Metodos.CellRendererVerFactura;
import Metodos.metodos;
import Metodos.ver_factura_impresion;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBstock_productos;
import java.awt.HeadlessException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Bodegas;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author Monkeyelgrande
 */
public class frm_ver_orden extends javax.swing.JDialog {

    /**
     * Creates new form frm_facturacion
     */
    public static DefaultTableModel modelo_productos = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) { // solo se permiten editables la columan cantidad y precio
//            if (columna == 4) { // Columna ENTREGA
//                return columna == 4;
//            }
//            return columna == 4;
            return false;
        }

        @Override
        public Object getValueAt(int row, int col) { // Sobre escritura del metodo getValue

            if (col == 5) { // digo que la columna 5 (Total) sera igual a la siguiente opreacion
                Double i; // i sera igual a la cantidad
                try {
                    i = Double.parseDouble(metodos.ReemplazarCaracteres(getValueAt(row, 3).toString(), ",", ".")); // capturo la cantidad
                } catch (Exception e) {
                    i = 1.0;
                }
//                        System.out.println(i);
                Double d = Double.parseDouble(metodos.ReemplazarCaracteres(getValueAt(row, 4).toString(), ",", "."));  // d sera igual al precio
                if (i != null && d != null) {
                    return metodos.formateador_decimal_punto_para_decimal().format(i - d); // regreso el resultado de multiplicar la cantidad por el valor
                } else {
                    return 0d;
                }
            }

            return super.getValueAt(row, col);

        }

        @Override
        public void setValueAt(Object aValue, int row, int col) {
            super.setValueAt(aValue, row, col);
            fireTableDataChanged();
        }

    };
    public static DefaultTableModel modelo_entregados_cabecera = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) { // solo se permiten editables la columan cantidad y precio
//            if (columna == 4) { // Columna ENTREGA
//                return columna == 4;
//            }
//            return columna == 4;
            return false;
        }
    };
    public static DefaultTableModel modelo_entregados_detalle = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) { // solo se permiten editables la columan cantidad y precio
//            if (columna == 4) { // Columna ENTREGA
//                return columna == 4;
//            }
//            return columna == 4;
            return false;
        }
    };

    String nombre_impresora;
    public static int id_bodega;

    Calendar fecha_calendario = new GregorianCalendar();
    CellRendererVerFactura myRenderer = new CellRendererVerFactura();
    static TableColumnModel columnModelProductos = null;
    static TableColumnModel columnModelEntregadosCabecera = null;
    static TableColumnModel columnModelEntregadosDetalle = null;
    int id_entrega_cabecera;

    public frm_ver_orden() {
        initComponents();
        this.setLocationRelativeTo(this);
        metodos.addEscapeListenerWindowDialog(this);
        jtabla_productos.setDefaultRenderer(Object.class, myRenderer);
        columnModelProductos = jtabla_productos.getColumnModel();
        columnModelEntregadosCabecera = jtabla_entregados_cabecera.getColumnModel();
        columnModelEntregadosDetalle = jtabla_entregados_detalle.getColumnModel();

        Bodegas bod = new Bodegas();
        bod.mostrarBodegas(jbox_bodega);

        nombre_impresora = DB_consultas_R_D.ImpresoraPredeterminada();
        txt_observaciones.setWrapStyleWord(true);
        metodos.EvitarTabEnJTextArea(txt_observaciones);
        jdate_fecha.setCalendar(fecha_calendario);
        id_entrega_cabecera = Integer.parseInt(DB_consultas_R_D.cargarId("entregas_productos_cabecera"));
        Doble_clic_tablas_CC();
        permisos();
    }

    public static void permisos() {
        switch (frm_main.perfil) {
            case 2: // BODEGUERO
//                btn_entregar.setEnabled(false);
//                btn_llenar.setEnabled(false);
//                btn_editar_serial.setEnabled(false);
//                btn_verFactura1.setEnabled(false);
//                btn_verFactura.setEnabled(false);
//                btn_verFactura2.setEnabled(false);
//                jbox_bodega.setEnabled(false);
                break;
            case 3: // VENDEDOR
                btn_entregar.setEnabled(false);
                btn_llenar.setEnabled(false);
                btn_editar_serial.setEnabled(false);
                btn_verFactura1.setEnabled(false);
                btn_verFactura.setEnabled(true);
                btn_verFactura2.setEnabled(false);
                break;

        }
    }

    public static void TamanosTablaProductos() {
        columnModelProductos.getColumn(0).setPreferredWidth(10);   // id_producto
        columnModelProductos.getColumn(1).setPreferredWidth(50);   // CODIGO
        columnModelProductos.getColumn(2).setPreferredWidth(500);  // DESCRIPCIÓN
        columnModelProductos.getColumn(3).setPreferredWidth(80);   // CANTIDAD
        columnModelProductos.getColumn(4).setPreferredWidth(80);   // ENTREGADO
        columnModelProductos.getColumn(5).setPreferredWidth(80);   // SALDO
        columnModelProductos.getColumn(6).setPreferredWidth(80);   // PRECIO
        columnModelProductos.getColumn(7).setPreferredWidth(80);   // TOTAL
        columnModelProductos.getColumn(8).setPreferredWidth(0);    // R (oculta)
        columnModelProductos.getColumn(8).setMinWidth(0);
        columnModelProductos.getColumn(8).setMaxWidth(0);
    }

    public static void TamanosTablaEntregadosCabecera() {
        columnModelEntregadosCabecera.getColumn(0).setPreferredWidth(80);
        columnModelEntregadosCabecera.getColumn(1).setPreferredWidth(150);
        columnModelEntregadosCabecera.getColumn(2).setPreferredWidth(100);
        columnModelEntregadosCabecera.getColumn(3).setPreferredWidth(90);
        columnModelEntregadosCabecera.getColumn(4).setPreferredWidth(100);
    }

    public static void TamanosTablaEntregadosDetalle() {
        columnModelEntregadosDetalle.getColumn(0).setPreferredWidth(80);
        columnModelEntregadosDetalle.getColumn(1).setPreferredWidth(80);
        columnModelEntregadosDetalle.getColumn(2).setPreferredWidth(600);
        columnModelEntregadosDetalle.getColumn(3).setPreferredWidth(150);
    }

    boolean cabecera = true;

    public void Doble_clic_tablas_CC() {
        jtabla_productos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    try {
                        int fila = jtabla_productos.getSelectedRow();

                        double cantidad_entregar = 0.0;
                        double cantidad_pendiente = Double.parseDouble(
                                metodos.ReemplazarCaracteres(jtabla_productos.getValueAt(fila, 5).toString(), ",", "."));
                        double cantidad_entregada = Double.parseDouble(
                                metodos.ReemplazarCaracteres(jtabla_productos.getValueAt(fila, 4).toString(), ",", "."));

                        cantidad_entregar = Double.parseDouble(JOptionPane.showInputDialog(
                                "Ingrese la cantidad que desea entregar\n\nNO PUEDE SER MAYOR QUE EL SALDO"));

                        String codigo_barras = jtabla_productos.getValueAt(fila, 1).toString();

                        // Obtener bodega seleccionada para la entrega
                        int id_bodega_entrega = 1;
                        try {
                            id_bodega_entrega = jbox_bodega.getItemAt(jbox_bodega.getSelectedIndex()).getId();
                        } catch (Exception e) {
                            id_bodega_entrega = 1;
                        }

                        // Validar stock disponible
                        double stock = DB_consultas_R_D.consultar_stock_x_bodega(codigo_barras, id_bodega_entrega);
                        if (stock < cantidad_entregar) {
                            JOptionPane.showMessageDialog(rootPane,
                                    "Este producto no cuenta con la existencia suficiente en esta bodega\n"
                                    + "Se procederá a realizar la entrega pero esto generará un valor NEGATIVO\n"
                                    + "Código: " + codigo_barras + "\n"
                                    + "Cantidad actual: " + stock);
                        }

                        if (cantidad_entregar > cantidad_pendiente) {
                            JOptionPane.showMessageDialog(rootPane, "No se puede entregar una cantidad mayor al saldo actual");
                        } else {
                            String fecha = "";
                            int dia, mes, ano;
                            ano = jdate_fecha.getCalendar().get(Calendar.YEAR);
                            mes = jdate_fecha.getCalendar().get(Calendar.MONTH) + 1;
                            dia = jdate_fecha.getCalendar().get(Calendar.DAY_OF_MONTH);
                            fecha = (ano + "-" + mes + "-" + dia);

                            String SQL = "";
                            if (cabecera) {
                                modelo_entregados_cabecera.addRow(new Object[]{
                                    id_entrega_cabecera, frm_main.lbl_user.getText(), fecha,
                                    DB_consultas_R_D.obtener_hora(), jbox_bodega.getSelectedItem().toString()});

                                SQL = "insert into entregas_productos_cabecera (id, id_factura, id_user, fecha_entrega, hora_entrega, id_bodega) "
                                        + "values (" + id_entrega_cabecera + "," + lbl_numerofactura.getText() + ","
                                        + frm_main.id_user + ",'" + fecha + "','" + DB_consultas_R_D.obtener_hora() + "'," + id_bodega_entrega + ");\n";
                                cabecera = false;

                                try {
                                    for (int i = 0; i < modelo_entregados_detalle.getRowCount(); i++) {
                                        modelo_entregados_detalle.removeRow(i);
                                        i -= 1;
                                    }
                                } catch (Exception m) {
                                }
                            }

                            String id_detalle = DB_consultas_R_D.cargarId("entregas_productos");
                            modelo_entregados_detalle.setColumnIdentifiers(new Object[]{"Id entrega", "CODIGO", "DESCRIPCIÓN", "CANTIDAD"});
                            modelo_entregados_detalle.addRow(new Object[]{
                                id_detalle,
                                jtabla_productos.getValueAt(fila, 1),
                                jtabla_productos.getValueAt(fila, 2),
                                cantidad_entregar});

                            jtabla_entregados_detalle.setModel(modelo_entregados_detalle);
                            TamanosTablaEntregadosDetalle();
                            double nuevo_total_entregado = cantidad_entregar + cantidad_entregada;
                            modelo_productos.setValueAt(nuevo_total_entregado, fila, 4);

                            try {
                                SQL += "insert into entregas_productos (id, id_cabecera, id_producto, cantidad, id_factura) values "
                                        + "((select coalesce(max(id),0)+1 from entregas_productos)," + id_entrega_cabecera
                                        + ",(select id from productos where codigo_barras='" + codigo_barras + "'),"
                                        + cantidad_entregar + "," + lbl_numerofactura.getText() + ");\n";

                                Connection con = DB_consultas_R_D.getConexion();
                                PreparedStatement psql = con.prepareStatement(SQL);
                                psql.executeUpdate();
                                psql.close();

                                // ════════════════════════════════════════════════════════════
                                // INTEGRACIÓN STOCK: Registrar entrega
                                // ════════════════════════════════════════════════════════════
                                int idProducto = Integer.parseInt(jtabla_productos.getValueAt(fila, 0).toString());
                                int idOrden = Integer.parseInt(lbl_numerofactura.getText());
                                int idFacturaRef = 0;
                                try {
                                    idFacturaRef = Integer.parseInt(jtabla_productos.getValueAt(fila, 8).toString());
                                } catch (Exception e) {
                                    idFacturaRef = 0;
                                }

                                DBstock_productos dbStock = new DBstock_productos();
                                String obsEntrega = "Entrega parcial - Orden #" + idOrden;
                                if (idFacturaRef > 0) {
                                    obsEntrega += " (Ref WO: " + idFacturaRef + ")";
                                }

                                dbStock.entrega(
                                        idProducto,
                                        id_bodega_entrega,
                                        frm_main.id_user,
                                        cantidad_entregar,
                                        idOrden,
                                        obsEntrega
                                );
                                // ════════════════════════════════════════════════════════════

                            } catch (SQLException e) {
                                JOptionPane.showMessageDialog(null,
                                        "Error al intentar guardar la entrega del producto:\n" + e,
                                        "Error en la operación", JOptionPane.ERROR_MESSAGE);
                            }
                        }

                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(rootPane,
                                "Solo se admite el ingreso de numeros enteros o decimales separados por punto (.)");
                    }
                }
            }
        });
        jtabla_entregados_detalle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    if (DB_consultas_R_D.validar_admin()) {
                        try {
                            int fila = jtabla_entregados_detalle.getSelectedRow();
                            String codigo = jtabla_entregados_detalle.getValueAt(fila, 1).toString();

                            double cantidad_a_entregar = Double.parseDouble(
                                    jtabla_productos.getValueAt(fila_en_tabla(codigo), 3).toString());

                            String id = jtabla_entregados_detalle.getValueAt(fila, 0).toString();

                            double cantidad_nueva = Double.parseDouble(JOptionPane.showInputDialog(
                                    "Ingrese la cantidad que desea actualizar\n\nNO PUEDE SER MAYOR QUE EL SALDO"));

                            if (cantidad_nueva > cantidad_a_entregar) {
                                JOptionPane.showMessageDialog(rootPane,
                                        "No se puede entregar una cantidad mayor a la de la orden generada");
                            } else {
                                String SQL = "update entregas_productos set cantidad=" + cantidad_nueva + " where id=" + id;

                                Connection con = DB_consultas_R_D.getConexion();
                                PreparedStatement psql = con.prepareStatement(SQL);
                                psql.executeUpdate();
                                psql.close();

                                dispose();
                                frm_Ordenes.btn_ver.doClick();
                            }
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(rootPane,
                                    "Solo se admite el ingreso de numeros enteros o decimales separados por punto (.)");
                        }
                    }
                }
            }
        });

    }

    private int fila_en_tabla(String codigo) {
        for (int i = 0; i < jtabla_productos.getRowCount(); i++) {
            if (this.jtabla_productos.getValueAt(i, 1).toString().equals(codigo)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla_productos = new javax.swing.JTable();
        btn_llenar = new javax.swing.JButton();
        btn_entregar = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        txt_observacion_entrega = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        jtabla_entregados_cabecera = new javax.swing.JTable();
        btn_verFactura = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        jtabla_entregados_detalle = new javax.swing.JTable();
        btn_verFactura1 = new javax.swing.JButton();
        btn_verFactura2 = new javax.swing.JButton();
        btn_imprimir_termica80 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        lbl_cedula_cliente = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        lbl_direccion_cliente = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        lbl_celular_cliente = new javax.swing.JLabel();
        lbl_id_cliente = new javax.swing.JLabel();
        lbl_nombre_cliente = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        lbl_fecha = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        lbl_numerofactura = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        lbl_tipo_factura = new javax.swing.JLabel();
        lbl_user = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        lbl_hora = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txt_observaciones = new javax.swing.JTextArea();
        jPanel6 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        btn_editar_serial = new javax.swing.JButton();
        txt_codigo = new javax.swing.JTextField();
        jbox_bodega = new javax.swing.JComboBox<>();
        jdate_fecha = new com.toedter.calendar.JDateChooser();

        setTitle("Ver factura");
        setModal(true);
        setName(""); // NOI18N

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jPanel7.setBackground(new java.awt.Color(58, 159, 171));

        jtabla_productos.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jtabla_productos.setForeground(new java.awt.Color(0, 51, 51));
        jtabla_productos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jtabla_productos.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jtabla_productos.setDoubleBuffered(true);
        jtabla_productos.setSelectionBackground(new java.awt.Color(0, 153, 255));
        jtabla_productos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jtabla_productos.getTableHeader().setReorderingAllowed(false);
        jtabla_productos.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtabla_productosKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(jtabla_productos);

        btn_llenar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_llenar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/ingreso_productos.png"))); // NOI18N
        btn_llenar.setMnemonic('e');
        btn_llenar.setText("Entregar todos los productos");
        btn_llenar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_llenarActionPerformed(evt);
            }
        });

        btn_entregar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_entregar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/cambio.png"))); // NOI18N
        btn_entregar.setMnemonic('e');
        btn_entregar.setText("Actualizar observacion");
        btn_entregar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_entregarActionPerformed(evt);
            }
        });

        txt_observacion_entrega.setColumns(20);
        txt_observacion_entrega.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        txt_observacion_entrega.setRows(5);
        jScrollPane3.setViewportView(txt_observacion_entrega);

        jtabla_entregados_cabecera.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jtabla_entregados_cabecera.setForeground(new java.awt.Color(0, 51, 51));
        jtabla_entregados_cabecera.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jtabla_entregados_cabecera.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jtabla_entregados_cabecera.setDoubleBuffered(true);
        jtabla_entregados_cabecera.setSelectionBackground(new java.awt.Color(0, 153, 255));
        jtabla_entregados_cabecera.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jtabla_entregados_cabecera.getTableHeader().setReorderingAllowed(false);
        jtabla_entregados_cabecera.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jtabla_entregados_cabeceraMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jtabla_entregados_cabeceraMouseExited(evt);
            }
        });
        jtabla_entregados_cabecera.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtabla_entregados_cabeceraKeyPressed(evt);
            }
        });
        jScrollPane4.setViewportView(jtabla_entregados_cabecera);

        btn_verFactura.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        btn_verFactura.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Print24.png"))); // NOI18N
        btn_verFactura.setText("Imprimir Orden");
        btn_verFactura.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_verFacturaActionPerformed(evt);
            }
        });

        jtabla_entregados_detalle.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jtabla_entregados_detalle.setForeground(new java.awt.Color(0, 51, 51));
        jtabla_entregados_detalle.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jtabla_entregados_detalle.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jtabla_entregados_detalle.setDoubleBuffered(true);
        jtabla_entregados_detalle.setSelectionBackground(new java.awt.Color(0, 153, 255));
        jtabla_entregados_detalle.getTableHeader().setReorderingAllowed(false);
        jtabla_entregados_detalle.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jtabla_entregados_detalleMouseClicked(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                jtabla_entregados_detalleMouseExited(evt);
            }
        });
        jtabla_entregados_detalle.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtabla_entregados_detalleKeyPressed(evt);
            }
        });
        jScrollPane5.setViewportView(jtabla_entregados_detalle);

        btn_verFactura1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        btn_verFactura1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Print24.png"))); // NOI18N
        btn_verFactura1.setText("Entrega");
        btn_verFactura1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_verFactura1ActionPerformed(evt);
            }
        });

        btn_verFactura2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        btn_verFactura2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Flash24.png"))); // NOI18N
        btn_verFactura2.setText("Impresion Directa");
        btn_verFactura2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_verFactura2ActionPerformed(evt);
            }
        });

        btn_imprimir_termica80.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        btn_imprimir_termica80.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Invoice.png"))); // NOI18N
        btn_imprimir_termica80.setText("Tirilla");
        btn_imprimir_termica80.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimir_termica80ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1266, Short.MAX_VALUE)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(btn_llenar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_entregar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addComponent(btn_verFactura, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_imprimir_termica80)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_verFactura1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_verFactura2))
                            .addComponent(jScrollPane4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3)
                            .addComponent(jScrollPane5))))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_llenar)
                            .addComponent(btn_entregar))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_verFactura)
                            .addComponent(btn_verFactura1)
                            .addComponent(btn_verFactura2)
                            .addComponent(btn_imprimir_termica80)))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(58, 159, 171), 3));

        jLabel3.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(58, 159, 171));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Cliente");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(58, 159, 171));
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Cédula");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(58, 159, 171));
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("Nombre");

        lbl_cedula_cliente.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        lbl_cedula_cliente.setForeground(new java.awt.Color(58, 159, 171));
        lbl_cedula_cliente.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_cedula_cliente.setText("-");

        jLabel10.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(58, 159, 171));
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText("Dirección");

        lbl_direccion_cliente.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        lbl_direccion_cliente.setForeground(new java.awt.Color(58, 159, 171));
        lbl_direccion_cliente.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_direccion_cliente.setText("-");

        jLabel12.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(58, 159, 171));
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("Celular");

        lbl_celular_cliente.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        lbl_celular_cliente.setForeground(new java.awt.Color(58, 159, 171));
        lbl_celular_cliente.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_celular_cliente.setText("-");

        lbl_id_cliente.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        lbl_id_cliente.setForeground(new java.awt.Color(58, 159, 171));
        lbl_id_cliente.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_id_cliente.setText("-");

        lbl_nombre_cliente.setFont(new java.awt.Font("Segoe UI Black", 0, 14)); // NOI18N
        lbl_nombre_cliente.setForeground(new java.awt.Color(58, 159, 171));
        lbl_nombre_cliente.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_nombre_cliente.setText("-");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10)
                    .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(jLabel3)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_nombre_cliente, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(lbl_id_cliente, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_celular_cliente, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lbl_direccion_cliente, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                            .addComponent(lbl_cedula_cliente, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addGap(48, 48, 48))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lbl_id_cliente))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(lbl_cedula_cliente))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(lbl_nombre_cliente))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lbl_direccion_cliente, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(lbl_celular_cliente, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 102, 102));
        jLabel1.setText("Fecha");

        lbl_fecha.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lbl_fecha.setForeground(new java.awt.Color(153, 0, 0));
        lbl_fecha.setText("Fecha");

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(58, 159, 171), 3));

        jLabel6.setFont(new java.awt.Font("Segoe UI Black", 0, 20)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(0, 102, 102));
        jLabel6.setText("N° ORDEN");

        lbl_numerofactura.setFont(new java.awt.Font("Segoe UI Black", 0, 24)); // NOI18N
        lbl_numerofactura.setForeground(new java.awt.Color(153, 0, 102));
        lbl_numerofactura.setText("N° orden");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(lbl_numerofactura))
                .addContainerGap(93, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lbl_numerofactura))
        );

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(0, 102, 102));
        jLabel2.setText("Tipo factura");

        lbl_tipo_factura.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lbl_tipo_factura.setForeground(new java.awt.Color(153, 0, 0));
        lbl_tipo_factura.setText("Tipo factura");

        lbl_user.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lbl_user.setForeground(new java.awt.Color(153, 0, 0));
        lbl_user.setText("User");

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(0, 102, 102));
        jLabel4.setText("User");

        lbl_hora.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lbl_hora.setForeground(new java.awt.Color(153, 0, 0));
        lbl_hora.setText("hora");

        jLabel9.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(0, 102, 102));
        jLabel9.setText("Hora");

        txt_observaciones.setEditable(false);
        txt_observaciones.setColumns(20);
        txt_observaciones.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        txt_observaciones.setRows(5);
        jScrollPane2.setViewportView(txt_observaciones);

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));
        jPanel6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(58, 159, 171), 3));

        jLabel14.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(0, 102, 102));
        jLabel14.setText("N° factura");

        btn_editar_serial.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btn_editar_serial.setText("Edit");
        btn_editar_serial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_editar_serialActionPerformed(evt);
            }
        });

        txt_codigo.setEditable(false);
        txt_codigo.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        txt_codigo.setForeground(new java.awt.Color(204, 0, 0));
        txt_codigo.setText("-");

        jbox_bodega.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jbox_bodega.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jbox_bodegaKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txt_codigo)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 24, Short.MAX_VALUE)
                        .addComponent(btn_editar_serial))
                    .addComponent(jbox_bodega, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_editar_serial, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txt_codigo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbox_bodega, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5))
        );

        jdate_fecha.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbl_tipo_factura)
                                    .addComponent(lbl_user))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(lbl_fecha)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbl_hora)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jdate_fecha, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(jScrollPane2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel9)
                                .addComponent(lbl_hora))
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel1)
                                .addComponent(lbl_fecha))
                            .addComponent(jdate_fecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(lbl_tipo_factura))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(lbl_user))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_llenarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_llenarActionPerformed
        try {
            int id_detalle = Integer.parseInt(DB_consultas_R_D.cargarId("entregas_productos"));
            String SQL = "";

            // Obtener bodega de entrega
            int id_bodega_entrega = 1;
            try {
                id_bodega_entrega = jbox_bodega.getItemAt(jbox_bodega.getSelectedIndex()).getId();
            } catch (Exception e) {
                id_bodega_entrega = 1;
            }

            int idOrden = Integer.parseInt(lbl_numerofactura.getText());
            DBstock_productos dbStock = new DBstock_productos();

            for (int i = 0; i < modelo_productos.getRowCount(); i++) {

                double cantidad_entregar = 0.0;
                double cantidad_entregada = Double.parseDouble(
                        metodos.ReemplazarCaracteres(jtabla_productos.getValueAt(i, 4).toString(), ",", "."));
                cantidad_entregar = Double.parseDouble(
                        metodos.ReemplazarCaracteres((jtabla_productos.getValueAt(i, 5).toString()), ",", "."));

                if (cantidad_entregar == 0) {
                    System.out.println("El producto ya esta en ceros");
                } else {
                    String fecha = "";
                    int dia, mes, ano;
                    ano = jdate_fecha.getCalendar().get(Calendar.YEAR);
                    mes = jdate_fecha.getCalendar().get(Calendar.MONTH) + 1;
                    dia = jdate_fecha.getCalendar().get(Calendar.DAY_OF_MONTH);
                    fecha = (ano + "-" + mes + "-" + dia);

                    if (cabecera) {
                        modelo_entregados_cabecera.addRow(new Object[]{
                            id_entrega_cabecera, frm_main.lbl_user.getText(), fecha,
                            DB_consultas_R_D.obtener_hora(), jbox_bodega.getSelectedItem().toString()});

                        SQL = "insert into entregas_productos_cabecera (id, id_factura, id_user, fecha_entrega, hora_entrega, id_bodega) "
                                + "values (" + id_entrega_cabecera + "," + lbl_numerofactura.getText() + ","
                                + frm_main.id_user + ",'" + fecha + "','" + DB_consultas_R_D.obtener_hora() + "'," + id_bodega_entrega + ");\n";
                        cabecera = false;

                        try {
                            for (int k = 0; k < modelo_entregados_detalle.getRowCount(); k++) {
                                modelo_entregados_detalle.removeRow(k);
                                k -= 1;
                            }
                        } catch (Exception m) {
                        }
                    }

                    modelo_entregados_detalle.setColumnIdentifiers(new Object[]{"id detalle", "CODIGO", "DESCRIPCIÓN", "CANTIDAD"});
                    modelo_entregados_detalle.addRow(new Object[]{
                        id_detalle,
                        jtabla_productos.getValueAt(i, 1),
                        jtabla_productos.getValueAt(i, 2),
                        cantidad_entregar});
                    id_detalle++;

                    jtabla_entregados_detalle.setModel(modelo_entregados_detalle);
                    TamanosTablaEntregadosDetalle();
                    double nuevo_total_entregado = cantidad_entregar + cantidad_entregada;
                    modelo_productos.setValueAt(nuevo_total_entregado, i, 4);

                    String codigo_barras = jtabla_productos.getValueAt(i, 1).toString();
                    SQL += "insert into entregas_productos (id, id_cabecera, id_producto, cantidad, id_factura) values "
                            + "((select coalesce(max(id),0)+1 from entregas_productos)," + id_entrega_cabecera
                            + ",(select id from productos where codigo_barras='" + codigo_barras + "'),"
                            + cantidad_entregar + "," + lbl_numerofactura.getText() + ");\n";

                    // ════════════════════════════════════════════════════════════
                    // INTEGRACIÓN STOCK: Registrar entrega por cada producto
                    // ════════════════════════════════════════════════════════════
                    int idProducto = Integer.parseInt(jtabla_productos.getValueAt(i, 0).toString());
                    int idFacturaRef = 0;
                    try {
                        idFacturaRef = Integer.parseInt(jtabla_productos.getValueAt(i, 8).toString());
                    } catch (Exception e) {
                        idFacturaRef = 0;
                    }

                    String obsEntrega = "Entrega completa - Orden #" + idOrden;
                    if (idFacturaRef > 0) {
                        obsEntrega += " (Ref WO: " + idFacturaRef + ")";
                    }

                    dbStock.entrega(
                            idProducto,
                            id_bodega_entrega,
                            frm_main.id_user,
                            cantidad_entregar,
                            idOrden,
                            obsEntrega
                    );
                    // ════════════════════════════════════════════════════════════
                }
            }

            try {
                Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement psql = con.prepareStatement(SQL);
                psql.executeUpdate();
                psql.close();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null,
                        "Error al intentar guardar la entrega del producto:\n" + e,
                        "Error en la operación", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(rootPane, e);
        }

    }//GEN-LAST:event_btn_llenarActionPerformed

    private void btn_entregarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_entregarActionPerformed
        modelo_productos = (DefaultTableModel) jtabla_productos.getModel();

        Connection con = DB_consultas_R_D.getConexion();
        PreparedStatement psql = null;
        String SSQL = "";

        SSQL += "update facturas_cabeceras set observacion_entrega='" + txt_observacion_entrega.getText() + "' "
                + "where id = " + lbl_numerofactura.getText() + ";";

        try {
            psql = con.prepareStatement(SSQL);
            psql.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al intentar almacenar la información factura detallas:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
        }
        try {
            psql.close();
            con.close();

        } catch (SQLException ex) {
            System.out.println(ex);
        }


    }//GEN-LAST:event_btn_entregarActionPerformed

    private void jtabla_productosKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtabla_productosKeyPressed

    }//GEN-LAST:event_jtabla_productosKeyPressed
    boolean edit_codigo = true;
    private void btn_editar_serialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_editar_serialActionPerformed
        if (DB_consultas_R_D.validar_admin()) {

            if (edit_codigo) {
                if (DB_consultas_R_D.validar_admin()) {
                    txt_codigo.setEditable(true);
                    edit_codigo = false;
                }
            } else {

                Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement psql = null;

                String sql = "update facturas_cabeceras set "
                        + "codigo='" + txt_codigo.getText() + "' "
                        + "where id=" + lbl_numerofactura.getText();

                try {

                    psql = con.prepareStatement(sql);
                    psql.executeUpdate();
                    psql.close();
                    con.close();

                    txt_codigo.setEditable(false);

                } catch (Exception e) {
                    System.out.println(e);
                }

            }
        }
    }//GEN-LAST:event_btn_editar_serialActionPerformed

    private void jtabla_entregados_cabeceraMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jtabla_entregados_cabeceraMouseClicked

        try {
            try {
                for (int i = 0; i < modelo_entregados_detalle.getRowCount(); i++) {
                    modelo_entregados_detalle.removeRow(i);
                    i -= 1;
                }
            } catch (Exception m) {
            }
            modelo_entregados_detalle.setColumnIdentifiers(new Object[]{"Id entrega", "CODIGO", "DESCRIPCIÓN", "CANTIDAD"});
            int fila = jtabla_entregados_cabecera.getSelectedRow();
            String id = jtabla_entregados_cabecera.getValueAt(fila, 0).toString();

            String consulta = "select e.id, p.codigo_barras, p.descripcion, e.cantidad from entregas_productos e, productos p "
                    + "where e.id_producto=p.id and e.id_cabecera=" + id;
//                        System.out.println(consulta);
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);
            try {
                while (rs.next()) {
                    modelo_entregados_detalle.addRow(new Object[]{rs.getInt("id"), rs.getString("codigo_barras"), rs.getString("descripcion"), rs.getDouble("cantidad")});

                }
                rs.close();
                jtabla_entregados_detalle.setModel(frm_ver_orden.modelo_entregados_detalle);
                TamanosTablaEntregadosDetalle();
            } catch (SQLException ex) {
                Logger.getLogger(frm_ver_orden.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(rootPane, e);
        }

    }//GEN-LAST:event_jtabla_entregados_cabeceraMouseClicked

    private void jtabla_entregados_cabeceraMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jtabla_entregados_cabeceraMouseExited
        // TODO add your handling code here:
    }//GEN-LAST:event_jtabla_entregados_cabeceraMouseExited

    private void jtabla_entregados_cabeceraKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtabla_entregados_cabeceraKeyPressed
        int key = evt.getKeyCode();
        if ((key == KeyEvent.VK_DELETE)) {
            if (DB_consultas_R_D.validar_admin()) {
                int fila = jtabla_entregados_cabecera.getSelectedRow();
                if (fila == -1) {
                    JOptionPane.showMessageDialog(null, "Seleccione un registro");
                } else {
                    int dialogButton = JOptionPane.YES_NO_OPTION;
                    int dialogResult = JOptionPane.showConfirmDialog(null,
                            "¿Desea eliminar esta entrega?\n\n"
                            + "Se revertirá el stock de los productos entregados.",
                            "Alerta", dialogButton);

                    if (dialogResult == JOptionPane.YES_OPTION) {
                        try {
                            String idEntregaCabecera = jtabla_entregados_cabecera.getValueAt(fila, 0).toString();
                            int idOrden = Integer.parseInt(lbl_numerofactura.getText());

                            // ════════════════════════════════════════════════════════════
                            // INTEGRACIÓN STOCK: Reversar stock antes de eliminar
                            // ════════════════════════════════════════════════════════════
                            reversarStockEntregaEliminada(Integer.parseInt(idEntregaCabecera), idOrden);

                            // Eliminar de la base de datos
                            DB_consultas_R_D.eliminar("entregas_productos_cabecera", idEntregaCabecera);

                            // Quitar de la tabla visual
                            for (int i = 0; i < modelo_entregados_cabecera.getRowCount(); i++) {
                                if (modelo_entregados_cabecera.getValueAt(i, 0).equals(idEntregaCabecera)) {
                                    modelo_entregados_cabecera.removeRow(i);

                                    // Limpiar tabla de detalles
                                    try {
                                        for (int j = modelo_entregados_detalle.getRowCount() - 1; j >= 0; j--) {
                                            modelo_entregados_detalle.removeRow(j);
                                        }
                                    } catch (Exception m) {
                                    }

                                    break;
                                }
                            }

                            // Refrescar la tabla de productos para mostrar saldos actualizados
                            JOptionPane.showMessageDialog(null,
                                    "Entrega eliminada. Los saldos se actualizarán al reabrir la orden.");

                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(null,
                                    "Error al eliminar la entrega: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }//GEN-LAST:event_jtabla_entregados_cabeceraKeyPressed

// ════════════════════════════════════════════════════════════════════════════
// MÉTODO AUXILIAR: Reversar stock de entrega eliminada
// ════════════════════════════════════════════════════════════════════════════
    private void reversarStockEntregaEliminada(int idEntregaCabecera, int idOrden) {
        DBstock_productos dbStock = new DBstock_productos();

        // Obtener bodega de la entrega
        int idBodegaEntrega = 1;
        String sqlBodega = "SELECT id_bodega FROM entregas_productos_cabecera WHERE id = " + idEntregaCabecera;
        ResultSet rsBodega = DB_consultas_R_D.getTabla(sqlBodega);
        try {
            if (rsBodega.next()) {
                idBodegaEntrega = rsBodega.getInt("id_bodega");
            }
            rsBodega.close();
        } catch (SQLException e) {
            System.out.println("Error obteniendo bodega de entrega: " + e);
        }

        // Obtener productos entregados y reversar cada uno
        String sql = "SELECT ep.id_producto, ep.cantidad, fd.id_factura "
                + "FROM entregas_productos ep "
                + "LEFT JOIN facturas_detalles fd ON fd.id_cabecera = ep.id_factura AND fd.id_producto = ep.id_producto "
                + "WHERE ep.id_cabecera = " + idEntregaCabecera;

        ResultSet rs = DB_consultas_R_D.getTabla(sql);
        try {
            while (rs.next()) {
                int idProducto = rs.getInt("id_producto");
                double cantidad = rs.getDouble("cantidad");
                int idFacturaRef = rs.getInt("id_factura"); // Puede ser 0 o el id de WO

                String obsAnulacion = "Anulación entrega - Orden #" + idOrden;
                if (idFacturaRef > 0) {
                    obsAnulacion += " (Ref WO: " + idFacturaRef + ")";
                }

                dbStock.anularEntrega(
                        idProducto,
                        idBodegaEntrega,
                        frm_main.id_user,
                        cantidad,
                        idEntregaCabecera,
                        obsAnulacion
                );
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error reversando stock de entrega eliminada: " + e);
        }
    }
    private void btn_verFacturaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_verFacturaActionPerformed
        try {
            int idFactura = Integer.parseInt(lbl_numerofactura.getText());

            if (frm_main.perfil == 3) { // VENDEDOR: una sola impresion
                Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT COALESCE(impreso_vendedor, 0) FROM facturas_cabeceras WHERE id = ?");
                ps.setInt(1, idFactura);
                ResultSet rs = ps.executeQuery();
                int impreso = 0;
                if (rs.next()) {
                    impreso = rs.getInt(1);
                }
                rs.close();
                ps.close();

                if (impreso == 1) {
                    JOptionPane.showMessageDialog(this,
                            "No puede volver a imprimir esta orden porque ya fue impresa.");
                    con.close();
                    return;
                }

                new Metodos.ImprimirFacturaPDF().imprimir(idFactura);

                PreparedStatement psu = con.prepareStatement(
                        "UPDATE facturas_cabeceras SET impreso_vendedor = 1 WHERE id = ?");
                psu.setInt(1, idFactura);
                psu.executeUpdate();
                psu.close();
                con.close();
            } else if (DB_consultas_R_D.validar_admin()) {
                new Metodos.ImprimirFacturaPDF().imprimir(idFactura);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "No se pudo imprimir: " + e.getMessage());
        }
    }//GEN-LAST:event_btn_verFacturaActionPerformed

    private void jtabla_entregados_detalleMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jtabla_entregados_detalleMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_jtabla_entregados_detalleMouseClicked

    private void jtabla_entregados_detalleMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jtabla_entregados_detalleMouseExited
        // TODO add your handling code here:
    }//GEN-LAST:event_jtabla_entregados_detalleMouseExited

    private void jtabla_entregados_detalleKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtabla_entregados_detalleKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jtabla_entregados_detalleKeyPressed

    private void btn_verFactura1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_verFactura1ActionPerformed
        int fila = jtabla_entregados_cabecera.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            String id_factura = (String) jtabla_entregados_cabecera.getValueAt(fila, 0);//suponiendo que el id lo muestras en la primera columna

            Connection cn = DB_consultas_R_D.getConexion();
            JasperReport report = null;
            Map p = new HashMap();
            p.put("id_factura", Integer.parseInt(id_factura));
            p.put("SUBREPORT_DIR", new File("").getAbsolutePath() + "/src/reportes/");

            try {
                try {
                    String cad = new File("").getAbsolutePath() + "/src/reportes/Imprimir_Recibo_Entrega_MC.jrxml";
                    report = JasperCompileManager.compileReport(cad);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, e);
                }
                JasperPrint jasperPrint = JasperFillManager.fillReport(report, p, cn);
                JasperViewer view = new JasperViewer(jasperPrint, false);
                cn.close();
                JDialog dialog = new JDialog(this);//the owner
                dialog.setContentPane(view.getContentPane());
                dialog.setSize(view.getSize());
                dialog.setModal(true);
                dialog.setLocationRelativeTo(this);
                dialog.setTitle("Impresión de enrega bodega numero " + id_factura);
                metodos.addEscapeListenerWindowDialog(dialog);
                dialog.setVisible(true);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }//GEN-LAST:event_btn_verFactura1ActionPerformed

    private void btn_verFactura2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_verFactura2ActionPerformed
        int fila = jtabla_entregados_cabecera.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            String id_factura = jtabla_entregados_cabecera.getValueAt(fila, 0).toString();//suponiendo que el id lo muestras en la primera columna

            Connection cn = DB_consultas_R_D.getConexion();
            JasperReport report = null;
            Map p = new HashMap();
            p.put("id_factura", Integer.parseInt(id_factura));
            p.put("SUBREPORT_DIR", new File("").getAbsolutePath() + "/src/reportes/");
            List<JasperPrint> jasperPrints = new ArrayList<JasperPrint>();

            try {
                try {
                    String cad = new File("").getAbsolutePath() + "/src/reportes/Imprimir_Recibo_Entrega_MC.jrxml";
                    report = JasperCompileManager.compileReport(cad);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, e);
                }
                JasperPrint jasperPrint = JasperFillManager.fillReport(report, p, cn);
                jasperPrints.add(jasperPrint);

                JasperPrintManager.printReport(jasperPrint, false);

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }//GEN-LAST:event_btn_verFactura2ActionPerformed

    private void jbox_bodegaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jbox_bodegaKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jbox_bodegaKeyPressed

    private void btn_imprimir_termica80ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imprimir_termica80ActionPerformed
        ver_factura_impresion imp = new ver_factura_impresion();
        imp.imprimir_termica_80mm(lbl_numerofactura.getText());
    }//GEN-LAST:event_btn_imprimir_termica80ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    public static javax.swing.JButton btn_editar_serial;
    public static javax.swing.JButton btn_entregar;
    public static javax.swing.JButton btn_imprimir_termica80;
    public static javax.swing.JButton btn_llenar;
    public static javax.swing.JButton btn_verFactura;
    public static javax.swing.JButton btn_verFactura1;
    public static javax.swing.JButton btn_verFactura2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    public static javax.swing.JComboBox<Bodegas> jbox_bodega;
    private com.toedter.calendar.JDateChooser jdate_fecha;
    public static javax.swing.JTable jtabla_entregados_cabecera;
    public static javax.swing.JTable jtabla_entregados_detalle;
    public static javax.swing.JTable jtabla_productos;
    public static javax.swing.JLabel lbl_cedula_cliente;
    public static javax.swing.JLabel lbl_celular_cliente;
    public static javax.swing.JLabel lbl_direccion_cliente;
    public static javax.swing.JLabel lbl_fecha;
    public static javax.swing.JLabel lbl_hora;
    public static javax.swing.JLabel lbl_id_cliente;
    public static javax.swing.JLabel lbl_nombre_cliente;
    public static javax.swing.JLabel lbl_numerofactura;
    public static javax.swing.JLabel lbl_tipo_factura;
    public static javax.swing.JLabel lbl_user;
    public static javax.swing.JTextField txt_codigo;
    public static javax.swing.JTextArea txt_observacion_entrega;
    public static javax.swing.JTextArea txt_observaciones;
    // End of variables declaration//GEN-END:variables

}
