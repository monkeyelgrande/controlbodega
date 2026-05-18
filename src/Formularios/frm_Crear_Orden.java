/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios;

import Formularios_internos.jd_Relacionar;
import JDBuscar.jd_buscar_contacto;
import Metodos.TextPrompt;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBcontactos;
import conexiondb.DBfacturas_cabeceras;
import conexiondb.DBstock_productos;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
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
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Bodegas;
import modelos.Contactos;
import modelos.Facturas_cabeceras;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;

/**
 *
 * @author Monkeyelgrande
 */
public class frm_Crear_Orden extends javax.swing.JInternalFrame {

    /**
     * Creates new form frm_facturacion
     */
    public static DecimalFormat formatea = new DecimalFormat("###,###");
    public static DefaultTableModel modelo_ventas = null;

    static DefaultTableModel modeloProductos = new DefaultTableModel() {

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };
    public static String fecha_vencimiento = "";
    public static boolean tipo_fac_cre_apart = false;
    public static double abono = 0;
    public static int id_bodega = 0;

    int imprimirSiNo;
    int imprimirBodegaSiNo;
    int productos_repetidos;
    Calendar fecha = new GregorianCalendar();
    static TableColumnModel columnModel = null;

    public frm_Crear_Orden() throws IOException {
        initComponents();
        configurarAtajosTeclado();

        inicializar_modelo();
        TextPrompt orden = new TextPrompt("No. Factura", txt_codigo);
        TextPrompt busqueda = new TextPrompt("Busqueda por descripción", txt_Filtro);
        TextPrompt observacion = new TextPrompt("Observaciones", txt_observaciones);
        TextPrompt cantidad = new TextPrompt("Cant.", txt_cantidad);
        TextPrompt busqueda_codigo = new TextPrompt("Buscar codigo", txt_codigo_barras);
        metodos.EstiloTablaMaterialGlobal(jtabla_Ventas);

        Bodegas bod = new Bodegas();
        bod.mostrarBodegas(jbox_bodega);

        int total_bodegas = jbox_bodega.getItemCount();
        if (frm_main.perfil == 2 || frm_main.perfil == 3 || frm_main.perfil == 4) {
            // Perfiles operativos: fijar combo a la bodega del usuario en sesion.
            for (int i = 0; i < total_bodegas; i++) {
                if (jbox_bodega.getItemAt(i).getId() == frm_main.id_bodega) {
                    jbox_bodega.setSelectedIndex(i);
                    break;
                }
            }
            jbox_bodega.setEnabled(false);
        } else {
            // Perfiles 1 (admin) y 5: combo libre.
            // Selecciona la 3a bodega como predeterminada (legado);
            // si hay menos, cae a la última disponible para no reventar.
            if (total_bodegas > 2) {
                jbox_bodega.setSelectedIndex(2);
            } else if (total_bodegas > 0) {
                jbox_bodega.setSelectedIndex(total_bodegas - 1);
            }
        }

        lbl_id_cliente.setText("1");
        lbl_numerofactura.setText(DB_consultas_R_D.cargarId("facturas_cabeceras"));
        jdate_fecha.setCalendar(fecha);

        imprimirSiNo = DB_consultas_R_D.Imprimir_si_no();
        imprimirBodegaSiNo = DB_consultas_R_D.Imprimir_Bodega_si_no(jbox_bodega.getSelectedItem().toString());
        productos_repetidos = DB_consultas_R_D.productos_repetidos();
        consulta();
        columnModel = jtabla_filtro.getColumnModel();
        TamanosTablaConsulta();

        txt_observaciones.setWrapStyleWord(true);
        metodos.EvitarTabEnJTextArea(txt_observaciones);
        metodos.BuscarEnTabla(txt_Filtro, jtabla_filtro);

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
    }

    private void configurarAtajosTeclado() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        // F12 → ir a jbox_bodega
        im.put(KeyStroke.getKeyStroke("F12"), "focusBodega");
        am.put("focusBodega", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jbox_bodega.requestFocusInWindow();
                jbox_bodega.showPopup();
            }
        });

        // F4 → ir a jbox_bodega
        im.put(KeyStroke.getKeyStroke("F4"), "focusImportar");
        am.put("focusImportar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importar_impresas.doClick();
            }
        });

        // F9 → ir al combo de cliente
        im.put(KeyStroke.getKeyStroke("F9"), "focusCliente");
        am.put("focusCliente", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btn_buscar_cliente.doClick();
            }
        });

        // F8 → ir al campo código de barras
        im.put(KeyStroke.getKeyStroke("F8"), "focusFiltro");
        am.put("focusFiltro", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txt_Filtro.requestFocusInWindow();
                txt_Filtro.selectAll();
            }
        });

        // F7 → ir a la tabla de ventas
        im.put(KeyStroke.getKeyStroke("F7"), "fucus_codigo");
        am.put("fucus_codigo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txt_codigo.requestFocusInWindow();

            }
        });

        // F6 → ir al textarea de observaciones
        im.put(KeyStroke.getKeyStroke("F6"), "focusObservaciones");
        am.put("focusObservaciones", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txt_observaciones.requestFocusInWindow();
            }
        });
        // F2 → ir VENDER
        im.put(KeyStroke.getKeyStroke("F2"), "btn_facturar");
        am.put("btn_facturar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btn_facturar.doClick();
            }
        });

        im.put(KeyStroke.getKeyStroke("F1"), "mostrarAyuda");
        am.put("mostrarAyuda", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btn_help.doClick();   // ← Ejecuta el botón
            }
        });

        im.put(KeyStroke.getKeyStroke("F5"), "relacionar");
        am.put("relacionar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btn_relacionar.doClick();   // ← Ejecuta el botón
            }
        });
    }

    private void mostrarAyuda() {
        String mensaje
                = "<html>"
                + "<body style='font-family:Segoe UI; font-size:12px;'>"
                + "<h2 style='color:#1A73E8;'>Atajos de Teclado</h2>"
                + "<hr>"
                + "<p>Estos son los comandos disponibles para navegar rápidamente:</p>"
                + "<ul>"
                + "  <li><b>F12:</b> Ir a <span style='color:#388E3C;'>Bodega</span></li>"
                + "  <li><b>F9:</b> Ir a <span style='color:#388E3C;'>Cliente</span></li>"
                + "  <li><b>F8:</b> Ir a <span style='color:#388E3C;'>Código de Barras</span></li>"
                + "  <li><b>F7:</b> Ir a <span style='color:#388E3C;'>Código</span></li>"
                + "  <li><b>F6:</b> Ir a <span style='color:#388E3C;'>Observaciones</span></li>"
                + "  <li><b>F5:</b> <span style='color:#D32F2F;'>Relacionar</span></li>"
                + "  <li><b>F2:</b> <span style='color:#D32F2F;'>Vender / Facturar</span></li>"
                + "  <li><b>F1:</b> Mostrar esta ventana de ayuda</li>"
                + "</ul>"
                + "<br>"
                + "<p style='font-size:11px; color:#555;'>"
                + "Puedes usar estos accesos sin importar dónde tengas el foco."
                + "</p>"
                + "</body>"
                + "</html>";

        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Ayuda del Sistema",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void inicializar_modelo() {

        modelo_ventas = new DefaultTableModel() { // modelo de la tabla
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
        };
        modelo_ventas.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "R"});

    }

    public void consulta() {
        try {
            for (int i = 0; i < modeloProductos.getRowCount(); i++) {
                modeloProductos.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
        }
        modeloProductos.setColumnIdentifiers(new Object[]{"Código", "Descripción", "Unidad", "stock"});
        String consulta;
        if (frm_main.perfil == 2 || frm_main.perfil == 3 || frm_main.perfil == 4) {
            // Perfiles operativos: stock solo de la bodega del usuario.
            consulta = "select p.codigo_barras, p.descripcion, u.nombre as unidad, COALESCE(sp.stock, 0) as stock "
                    + "from productos p LEFT JOIN (SELECT id_producto, cantidad as stock FROM stock_productos WHERE id_bodega = " + frm_main.id_bodega + ") sp ON sp.id_producto = p.id, unidades_medidas u where p.id_unidad=u.id AND COALESCE(p.estado, true) = true";
        } else {
            // Perfiles 1 (admin) y 5: stock total sumado de todas las bodegas.
            consulta = "select p.codigo_barras, p.descripcion, u.nombre as unidad, COALESCE(sp.stock, 0) as stock "
                    + "from productos p LEFT JOIN (SELECT id_producto, SUM(cantidad) as stock FROM stock_productos GROUP BY id_producto) sp ON sp.id_producto = p.id, unidades_medidas u where p.id_unidad=u.id AND COALESCE(p.estado, true) = true";
        }
//        System.out.println(consulta);
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        try {
            while (rs.next()) {

                modeloProductos.addRow(new Object[]{rs.getString("codigo_barras"), rs.getString("descripcion"),
                    rs.getString("unidad"), metodos.formateador_decimal().format(rs.getDouble("stock"))});
            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla_filtro.setModel(modeloProductos);
            TamanosTablaConsulta();
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public void TamanosTablaConsulta() {
        columnModel.getColumn(0).setPreferredWidth(100);
        columnModel.getColumn(1).setPreferredWidth(700);
        columnModel.getColumn(2).setPreferredWidth(80);
        columnModel.getColumn(3).setPreferredWidth(80);
    }

    public void TamanosTablaVentas() {
        columnModel.getColumn(0).setPreferredWidth(30);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(600);
        columnModel.getColumn(3).setPreferredWidth(100);
//        columnModel.getColumn(4).setPreferredWidth(10);
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
        lbl_descuento = new javax.swing.JLabel();
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
        jSeparator1 = new javax.swing.JSeparator();
        rb_salida = new javax.swing.JRadioButton();
        rb_prestamo = new javax.swing.JRadioButton();
        rb_eliminacion = new javax.swing.JRadioButton();
        btn_help = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jbox_bodega = new javax.swing.JComboBox<>();
        jButton2 = new javax.swing.JButton();
        btn_relacionar = new javax.swing.JButton();
        importar_impresas = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        txt_observaciones = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        lbl_id_cliente = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        btn_buscar_cliente = new javax.swing.JButton();
        lbl_nombre_cliente = new javax.swing.JLabel();
        lbl_cedula = new javax.swing.JLabel();

        setClosable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Salidas");
        setName(""); // NOI18N

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jPanel7.setBackground(new java.awt.Color(24, 30, 80));

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
        btn_facturar.setForeground(new java.awt.Color(51, 51, 51));
        btn_facturar.setMnemonic('f');
        btn_facturar.setText("Facturar");
        btn_facturar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_facturarActionPerformed(evt);
            }
        });

        btn_limpiar.setFont(new java.awt.Font("Yu Gothic Medium", 1, 24)); // NOI18N
        btn_limpiar.setForeground(new java.awt.Color(51, 51, 51));
        btn_limpiar.setMnemonic('l');
        btn_limpiar.setText("Limpiar");
        btn_limpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_limpiarActionPerformed(evt);
            }
        });

        lbl_descuento.setBackground(new java.awt.Color(255, 255, 255));
        lbl_descuento.setFont(new java.awt.Font("Yu Gothic Medium", 1, 24)); // NOI18N
        lbl_descuento.setForeground(new java.awt.Color(255, 255, 255));
        lbl_descuento.setText("0");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1649, Short.MAX_VALUE)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(btn_facturar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_limpiar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbl_descuento)))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_descuento)
                    .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btn_facturar, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btn_limpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));
        jPanel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(254, 201, 45), 3));

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

        jPanel2.setBackground(new java.awt.Color(244, 67, 54));
        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(24, 30, 80), 3));

        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("N° Salida");

        lbl_numerofactura.setFont(new java.awt.Font("Segoe UI Black", 0, 24)); // NOI18N
        lbl_numerofactura.setForeground(new java.awt.Color(255, 255, 255));
        lbl_numerofactura.setText("N° Factura");

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        rgroup_tipo_factura.add(rb_salida);
        rb_salida.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        rb_salida.setForeground(new java.awt.Color(255, 255, 255));
        rb_salida.setSelected(true);
        rb_salida.setText("Salida");
        rb_salida.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rb_salidaActionPerformed(evt);
            }
        });

        rgroup_tipo_factura.add(rb_prestamo);
        rb_prestamo.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        rb_prestamo.setForeground(new java.awt.Color(255, 255, 255));
        rb_prestamo.setText("Préstamo");
        rb_prestamo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rb_prestamoActionPerformed(evt);
            }
        });

        rgroup_tipo_factura.add(rb_eliminacion);
        rb_eliminacion.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        rb_eliminacion.setForeground(new java.awt.Color(255, 255, 255));
        rb_eliminacion.setText("Eliminación");
        rb_eliminacion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rb_eliminacionActionPerformed(evt);
            }
        });

        btn_help.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Help.png"))); // NOI18N
        btn_help.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_helpActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rb_salida)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rb_prestamo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rb_eliminacion)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btn_help)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_numerofactura)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btn_help))
                    .addComponent(jSeparator1)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(rb_salida)
                                .addComponent(rb_prestamo)
                                .addComponent(rb_eliminacion))
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel6)
                                .addComponent(lbl_numerofactura)))
                        .addGap(0, 6, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));
        jPanel6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(24, 30, 80), 3));

        jLabel8.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(0, 102, 102));
        jLabel8.setText("Bodega");

        jbox_bodega.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jbox_bodega.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbox_bodegaActionPerformed(evt);
            }
        });
        jbox_bodega.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jbox_bodegaKeyPressed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Yu Gothic Medium", 1, 12)); // NOI18N
        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Available Updates.png"))); // NOI18N
        jButton2.setText("Actualizar Stock");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        btn_relacionar.setBackground(new java.awt.Color(244, 67, 54));
        btn_relacionar.setFont(new java.awt.Font("Yu Gothic Medium", 1, 12)); // NOI18N
        btn_relacionar.setForeground(new java.awt.Color(255, 255, 255));
        btn_relacionar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Chain.png"))); // NOI18N
        btn_relacionar.setText("Relacionar Factura");
        btn_relacionar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_relacionarActionPerformed(evt);
            }
        });

        importar_impresas.setText("Fac. Impresas");
        importar_impresas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importar_impresasActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jbox_bodega, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_relacionar, javax.swing.GroupLayout.DEFAULT_SIZE, 227, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(importar_impresas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jbox_bodega, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importar_impresas)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_relacionar)
                .addContainerGap())
        );

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel4.setPreferredSize(new java.awt.Dimension(370, 232));

        txt_observaciones.setColumns(20);
        txt_observaciones.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        txt_observaciones.setLineWrap(true);
        txt_observaciones.setRows(5);
        jScrollPane4.setViewportView(txt_observaciones);

        jLabel3.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(58, 159, 171));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Cliente");

        lbl_id_cliente.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_id_cliente.setForeground(new java.awt.Color(58, 159, 171));
        lbl_id_cliente.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_id_cliente.setText("-");

        jLabel7.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(58, 159, 171));
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Nombre:");

        jButton1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jButton1.setForeground(new java.awt.Color(51, 204, 0));
        jButton1.setMnemonic('k');
        jButton1.setText("+");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        btn_buscar_cliente.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        btn_buscar_cliente.setForeground(new java.awt.Color(51, 204, 0));
        btn_buscar_cliente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/bucar.png"))); // NOI18N
        btn_buscar_cliente.setMnemonic('k');
        btn_buscar_cliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscar_clienteActionPerformed(evt);
            }
        });

        lbl_nombre_cliente.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_nombre_cliente.setForeground(new java.awt.Color(153, 0, 0));
        lbl_nombre_cliente.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_nombre_cliente.setText("nombre cliente");

        lbl_cedula.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_cedula.setForeground(new java.awt.Color(58, 159, 171));
        lbl_cedula.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbl_cedula.setText("-");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 356, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(18, 18, 18)
                                .addComponent(lbl_cedula)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_buscar_cliente))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lbl_nombre_cliente)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lbl_id_cliente)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(lbl_cedula))
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButton1)
                        .addComponent(btn_buscar_cliente)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(lbl_nombre_cliente)
                    .addComponent(lbl_id_cliente))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 258, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

                            modelo_ventas.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"), cantidad, "0"});

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
                                modelo_ventas.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"), cantidad, "0"});

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

        } else {
            JOptionPane.showMessageDialog(this, "El codigo de barras ingresado no se encuentra en la base de datos");
            txt_codigo_barras.setText("");
        }

    }

    public boolean existe_en_tabla(String codigo) {
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
    private void rb_eliminacionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rb_eliminacionActionPerformed

    }//GEN-LAST:event_rb_eliminacionActionPerformed

    private void rb_salidaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rb_salidaActionPerformed

    }//GEN-LAST:event_rb_salidaActionPerformed

    private void rb_prestamoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rb_prestamoActionPerformed

    }//GEN-LAST:event_rb_prestamoActionPerformed

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

    private void btn_facturarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_facturarActionPerformed
        if (jtabla_Ventas.getRowCount() < 1) {
            JOptionPane.showMessageDialog(this, "Por favor agregue al menos un producto");
            txt_codigo_barras.requestFocus();
        } else {
            boolean flag = true;
            int idBodegaValidacion;
            String nombreBodegaValidacion;
            try {
                idBodegaValidacion = jbox_bodega.getItemAt(jbox_bodega.getSelectedIndex()).getId();
                nombreBodegaValidacion = jbox_bodega.getSelectedItem().toString();
            } catch (Exception e) {
                idBodegaValidacion = id_bodega;
                nombreBodegaValidacion = "asignada";
            }
            for (int i = 0; i < jtabla_Ventas.getRowCount(); i++) {
                int idFacturaRef = 0;
                try {
                    idFacturaRef = Integer.parseInt(modelo_ventas.getValueAt(i, 4).toString());
                } catch (Exception e) {
                    idFacturaRef = 0;
                }
                if (idFacturaRef != 0) {
                    continue;
                }
                double can = 0;
                try {
                    can = Double.parseDouble("" + modelo_ventas.getValueAt(i, 3));
                } catch (NumberFormatException e) {
                    can = 1;
                }
                double stock = DB_consultas_R_D.consultar_stock_x_bodega("" + modelo_ventas.getValueAt(i, 1), idBodegaValidacion);
                if (can > stock) {
                    flag = false;
                    int dialogButton = JOptionPane.YES_NO_OPTION;
                    int dialogResult = JOptionPane.showConfirmDialog(null, "El producto " + modelo_ventas.getValueAt(i, 2) + " solo cuenta con un inventario de:\n"
                            + stock + " en la bodega " + nombreBodegaValidacion + "\n¿Desea continuar esta  orden sin inventario suficiente?\n"
                            + "el balance le dara negativo", "Alerta", dialogButton);
                    if (dialogResult == JOptionPane.YES_OPTION) {
                        flag = true;
                    }
                }
            }

            if (flag) {
                lbl_numerofactura.setText(DB_consultas_R_D.cargarId("facturas_cabeceras"));
                DBfacturas_cabeceras dbfactura = new DBfacturas_cabeceras();
                DBstock_productos dbStock = new DBstock_productos();  // ◄── NUEVO
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
                    if (rb_salida.isSelected()) {
                        fc.setTipo("Salida");
                    }
                    if (rb_prestamo.isSelected()) {
                        fc.setTipo("Préstamo");
                    }
                    if (rb_eliminacion.isSelected()) {
                        fc.setTipo("Eliminación");
                    }
                    fc.setHora(DB_consultas_R_D.obtener_hora());
                    fc.setObservacion(txt_observaciones.getText());
                    fc.setAnulado(1);

                    try {
                        fc.setId_bodega(jbox_bodega.getItemAt(jbox_bodega.getSelectedIndex()).getId());
                    } catch (Exception e) {
                        fc.setId_bodega(id_bodega);
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, e);
                }

                if (dbfactura.Guardar(fc) == 1) {
                    Connection con = null;
                    con = DB_consultas_R_D.getConexion();
                    PreparedStatement psql = null;
                    String SSQL = "";

                    int idOrden = Integer.parseInt(lbl_numerofactura.getText());
                    int idBodega = fc.getId_bodega();

                    for (int i = 0; i < jtabla_Ventas.getRowCount(); i++) {
                        double can = 0;
                        try {
                            can = Double.parseDouble("" + modelo_ventas.getValueAt(i, 3));
                        } catch (Exception e) {
                            can = 1;
                        }

                        int idProducto = Integer.parseInt(modelo_ventas.getValueAt(i, 0).toString());
                        int idFacturaRef = 0;
                        try {
                            idFacturaRef = Integer.parseInt(modelo_ventas.getValueAt(i, 4).toString());
                        } catch (Exception e) {
                            idFacturaRef = 0;
                        }

                        SSQL += "INSERT INTO facturas_detalles (id,id_cabecera, id_producto, cantidad, subtotal, id_factura) "
                                + "VALUES ((select COALESCE(max(id),0)+1 from facturas_detalles),"
                                + lbl_numerofactura.getText() + "," + modelo_ventas.getValueAt(i, 0).toString() + ","
                                + can + ",0," + idFacturaRef + ");\n";

                        // ════════════════════════════════════════════════════════════════════
// INTEGRACIÓN STOCK: Registrar orden
// ════════════════════════════════════════════════════════════════════
                        if (idFacturaRef == 0) {
                            // Orden normal: solo compromete pendientes
                            dbStock.orden(
                                    idProducto,
                                    idBodega,
                                    frm_main.id_user,
                                    can,
                                    idOrden,
                                    "Orden - " + fc.getTipo()
                            );
                        } else {
                            // Orden referenciada: ingresa cantidad Y compromete pendientes
                            dbStock.ordenReferenciada(
                                    idProducto,
                                    idBodega,
                                    frm_main.id_user,
                                    can,
                                    idOrden,
                                    "Orden referenciada - Factura WO: " + idFacturaRef // ◄── idFacturaRef va aquí en observación
                            );
                        }
                    }
                    try {
                        psql = con.prepareStatement(SSQL);
                        psql.executeUpdate();
                        psql.close();
                        con.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(frm_Crear_Orden.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                if (tipo_fac_cre_apart) {
                    tipo_fac_cre_apart = false;
                }

                imprimirBodegaSiNo = DB_consultas_R_D.Imprimir_Bodega_si_no(jbox_bodega.getSelectedItem().toString());
                System.out.println(imprimirBodegaSiNo);
                if (imprimirSiNo == 1 && imprimirBodegaSiNo == 1) {
                    imprimir_factura();
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

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        try {
            String nombre = JOptionPane.showInputDialog(this, "Ingrese el nombre del nuevo cliente:");
            if (nombre == null || nombre.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre es obligatorio");
                return;
            }

            String cedula = JOptionPane.showInputDialog(this, "Ingrese la cédula del cliente:");
            if (cedula == null || cedula.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "La cédula es obligatoria");
                return;
            }

            // 🔎 Validar si la cédula ya existe
            if (DBcontactos.existeCedula(cedula)) {
                JOptionPane.showMessageDialog(this,
                        "La cédula ya está registrada.\nNo se puede crear el contacto.",
                        "Cédula duplicada",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            DBcontactos dbclientes = new DBcontactos();
            Contactos contacto = new Contactos();

            String idnuevo = DB_consultas_R_D.TraerIdMaximoNuevoContacto();
            contacto.setId(Integer.parseInt(idnuevo));
            contacto.setNombre(nombre);
            contacto.setCedula(cedula);

            if (dbclientes.Guardar(contacto) > 0) {
                lbl_id_cliente.setText(contacto.getId() + "");
                lbl_nombre_cliente.setText(contacto.getNombre());
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }//GEN-LAST:event_jButton1ActionPerformed

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
            String codigo_barras = (String) jtabla_Ventas.getValueAt(fila, 1);
            double stock = DB_consultas_R_D.consultar_stock(codigo_barras);
            JOptionPane.showConfirmDialog(rootPane, "E producto actual posee un stock de:\n" + stock);
        }

    }//GEN-LAST:event_jtabla_VentasKeyPressed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        consulta();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void btn_relacionarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_relacionarActionPerformed
        jd_Relacionar jd = new jd_Relacionar(null, true);

        jd.show();
    }//GEN-LAST:event_btn_relacionarActionPerformed

    private void jbox_bodegaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jbox_bodegaKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jbox_bodegaKeyPressed

    private void jbox_bodegaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbox_bodegaActionPerformed

        try {
            id_bodega = (jbox_bodega.getItemAt(jbox_bodega.getSelectedIndex()).getId());
        } catch (Exception e) {
            id_bodega = 1;
        }
    }//GEN-LAST:event_jbox_bodegaActionPerformed

    private void btn_helpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_helpActionPerformed
        mostrarAyuda();

    }//GEN-LAST:event_btn_helpActionPerformed

    private void btn_buscar_clienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_buscar_clienteActionPerformed
        jd_buscar_contacto buscar_cleinte = new jd_buscar_contacto(null, closable);
        buscar_cleinte.formulario = "orden";
        buscar_cleinte.show();
    }//GEN-LAST:event_btn_buscar_clienteActionPerformed

    private void importar_impresasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importar_impresasActionPerformed
        jd_Facturas_Impresas jd = new jd_Facturas_Impresas(null, true);
        metodos.addEscapeListenerWindowDialog(jd);
        jd.setVisible(true);
    }//GEN-LAST:event_importar_impresasActionPerformed

    public void limpiar() {
        jdate_fecha.setCalendar(fecha);
        txt_codigo_barras.setText("");
        txt_codigo.setText("");
        modelo_ventas.setRowCount(0);

        lbl_descuento.setText("");
        txt_observaciones.setText("");
        txt_codigo_barras.requestFocus();
        rb_salida.setSelected(true);
        lbl_numerofactura.setText(DB_consultas_R_D.cargarId("facturas_cabeceras"));
        tipo_fac_cre_apart = false;
        lbl_cedula.setText("-");
        lbl_id_cliente.setText("1");
        lbl_nombre_cliente.setText("-");
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_buscar_cliente;
    public static javax.swing.JButton btn_facturar;
    private javax.swing.JButton btn_help;
    private javax.swing.JButton btn_limpiar;
    private javax.swing.JButton btn_relacionar;
    private javax.swing.JButton importar_impresas;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
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
    private javax.swing.JSeparator jSeparator1;
    public static javax.swing.JComboBox<Bodegas> jbox_bodega;
    private com.toedter.calendar.JDateChooser jdate_fecha;
    public static javax.swing.JTable jtabla_Ventas;
    private javax.swing.JTable jtabla_filtro;
    public static javax.swing.JLabel lbl_cedula;
    public static javax.swing.JLabel lbl_descuento;
    public static javax.swing.JLabel lbl_id_cliente;
    public static javax.swing.JLabel lbl_nombre_cliente;
    public static javax.swing.JLabel lbl_numerofactura;
    private javax.swing.JRadioButton rb_eliminacion;
    private javax.swing.JRadioButton rb_prestamo;
    private javax.swing.JRadioButton rb_salida;
    private javax.swing.ButtonGroup rgroup_tipo_factura;
    public static javax.swing.JTextField txt_Filtro;
    public static javax.swing.JTextField txt_cantidad;
    public static javax.swing.JTextField txt_codigo;
    public static javax.swing.JTextField txt_codigo_barras;
    public static javax.swing.JTextArea txt_observaciones;
    // End of variables declaration//GEN-END:variables

}
