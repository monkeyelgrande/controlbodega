package Precios;

import Formularios.frm_main;
import Formularios_internos.jif_crear_producto;
import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import com.formdev.flatlaf.FlatClientProperties;
import conexiondb.DB_consultas_R_D;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

    // Referencia al ingreso abierto: la usa jif_crear_producto para agregar a la
    // tabla, de inmediato, el producto recien creado desde "Crear producto".
    public static jif_crear_ingreso_precios instancia = null;

    public jif_crear_ingreso_precios() {
        instancia = this;
        initComponents();
        inicializar_modelo();
        configurarColumnasYBodega();
        btn_calcular_utildiad_porcentaje.setVisible(false);

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
        setTitle("Ingreso de productos (Precios)");
        setModal(true);

        // ---------------- Documento ----------------
        jbox_proveedor = new JComboBox<>();
        EstiloCompras.styleCombo(jbox_proveedor);
        jbox_proveedor.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jbox_proveedorKeyPressed(evt);
            }
        });

        jbox_transportador = new JComboBox<>();
        EstiloCompras.styleCombo(jbox_transportador);

        jdate_fecha_entrada = new com.toedter.calendar.JDateChooser();
        jdate_fecha_entrada.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        jdate_fecha_entrada.setPreferredSize(new Dimension(0, 38));

        txt_no_factura = new javax.swing.JTextField();
        estiloCampo(txt_no_factura);

        txt_redondear = new javax.swing.JTextField("2");
        estiloCampo(txt_redondear);

        lbl_id = new JLabel("0");
        lbl_id.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lbl_id.setForeground(EstiloCompras.PRIMARY);

        btn_buscar_proveedor = new JButton(FontAwesome.icon(FontAwesome.SEARCH, 14f, EstiloCompras.PRIMARY));
        EstiloCompras.secondaryButton(btn_buscar_proveedor);
        btn_buscar_proveedor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscar_proveedorActionPerformed(evt);
            }
        });

        btn_buscar_transportador = new JButton(FontAwesome.icon(FontAwesome.SEARCH, 14f, EstiloCompras.PRIMARY));
        EstiloCompras.secondaryButton(btn_buscar_transportador);
        btn_buscar_transportador.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jd_buscar_transportador_precios d = new jd_buscar_transportador_precios(null, true);
                d.setVisible(true);
            }
        });

        // ---------------- Barra de captura ----------------
        txt_codigo_barras = new javax.swing.JTextField();
        estiloCampo(txt_codigo_barras);
        txt_codigo_barras.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                FontAwesome.icon(FontAwesome.BARCODE, 15f, EstiloCompras.TEXT_SECONDARY));
        txt_codigo_barras.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_codigo_barrasKeyPressed(evt);
            }
        });

        btn_buscar = new JButton("Buscar producto", FontAwesome.icon(FontAwesome.SEARCH, 14f, Color.WHITE));
        EstiloCompras.primaryButton(btn_buscar);
        btn_buscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscarActionPerformed(evt);
            }
        });

        btn_crear_producto = new JButton("Crear producto", FontAwesome.icon(FontAwesome.PLUS, 14f, Color.WHITE));
        EstiloCompras.successButton(btn_crear_producto);
        btn_crear_producto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jif_crear_producto fp = new jif_crear_producto();
                jif_crear_producto.formulario = "precios";
                fp.setVisible(true);
            }
        });

        btn_calcular_costos = new JButton("Calcular costos", FontAwesome.icon(FontAwesome.SYNC, 14f, Color.WHITE));
        EstiloCompras.primaryButton(btn_calcular_costos);
        btn_calcular_costos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_calcular_costosActionPerformed(evt);
            }
        });

        btn_precargar = new JButton("Precargar costos e IVA", FontAwesome.icon(FontAwesome.ARROW_DOWN, 14f, EstiloCompras.PRIMARY));
        EstiloCompras.secondaryButton(btn_precargar);
        btn_precargar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_precargarActionPerformed(evt);
            }
        });

        btn_Exportar_Worold = new JButton("Exportar a World Office", FontAwesome.icon(FontAwesome.FILE_INVOICE, 14f, EstiloCompras.PRIMARY));
        EstiloCompras.secondaryButton(btn_Exportar_Worold);
        btn_Exportar_Worold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Exportar_WoroldActionPerformed(evt);
            }
        });

        btn_calcular_precios_venta = new JButton("Calcular P. Venta", FontAwesome.icon(FontAwesome.SYNC, 14f, Color.WHITE));
        EstiloCompras.primaryButton(btn_calcular_precios_venta);
        btn_calcular_precios_venta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_calcular_precios_ventaActionPerformed(evt);
            }
        });

        btn_calcular_utildiad_porcentaje = new JButton("Calcular utilidad", FontAwesome.icon(FontAwesome.SYNC, 14f, EstiloCompras.PRIMARY));
        EstiloCompras.secondaryButton(btn_calcular_utildiad_porcentaje);
        btn_calcular_utildiad_porcentaje.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_calcular_utildiad_porcentajeActionPerformed(evt);
            }
        });

        // ---------------- Tabla ----------------
        jtabla = new javax.swing.JTable();
        jtabla.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{}, new String[]{}));
        EstiloCompras.styleTable(jtabla);
        jtabla.setRowHeight(40);
        jtabla.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        jScrollPane1 = new JScrollPane(jtabla);
        jScrollPane1.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));

        jPanel3 = new JPanel(new BorderLayout());
        jPanel3.setBackground(EstiloCompras.BG_FORM);
        jPanel3.add(jScrollPane1, BorderLayout.CENTER);

        // ---------------- Observacion ----------------
        txt_observacion = new javax.swing.JTextArea();
        txt_observacion.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt_observacion.setLineWrap(true);
        txt_observacion.setWrapStyleWord(true);
        jScrollPane2 = new JScrollPane(txt_observacion);
        jScrollPane2.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));

        // ---------------- Totales ----------------
        lbl_sub_total = valorTotal(EstiloCompras.PRIMARY);
        lbl_descuento = valorTotal(new Color(0x9A7D0A));
        lbl_iva = valorTotal(EstiloCompras.DANGER);
        lbl_total_factura = valorTotal(EstiloCompras.SUCCESS);
        lbl_total_items = new JLabel("0");
        lbl_total_items.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lbl_total_items.setForeground(EstiloCompras.SUCCESS);

        // ---------------- Acciones ----------------
        btn_guardar = new JButton("Guardar", FontAwesome.icon(FontAwesome.SAVE, 15f, Color.WHITE));
        EstiloCompras.successButton(btn_guardar);
        btn_guardar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn_guardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_guardarActionPerformed(evt);
            }
        });

        btn_limpiar = new JButton("Limpiar", FontAwesome.icon(FontAwesome.SYNC, 15f, EstiloCompras.PRIMARY));
        EstiloCompras.secondaryButton(btn_limpiar);
        btn_limpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_limpiarActionPerformed(evt);
            }
        });

        btn_imprimir = new JButton("Imprimir tickets", FontAwesome.icon(FontAwesome.LIST, 15f, EstiloCompras.PRIMARY));
        EstiloCompras.secondaryButton(btn_imprimir);
        btn_imprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimirActionPerformed(evt);
            }
        });

        // Actualiza SOLO la observacion del ingreso, en cualquier momento y con
        // cualquier usuario (no depende del rol ni del estado del ingreso).
        btn_actualizar_obs = new JButton("Actualizar", FontAwesome.icon(FontAwesome.SAVE, 13f, EstiloCompras.PRIMARY));
        EstiloCompras.secondaryButton(btn_actualizar_obs);
        btn_actualizar_obs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_actualizar_obsActionPerformed(evt);
            }
        });

        chk_cerrar = new javax.swing.JCheckBox("Cerrar al guardar", true);
        chk_cerrar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chk_cerrar.setOpaque(false);
        chk_cerrar.setForeground(EstiloCompras.TEXT_SECONDARY);
        chk_cerrar.setFocusPainted(false);

        // ---------------- Ensamblado ----------------
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(EstiloCompras.header(FontAwesome.FILE_INVOICE, "Ingreso de productos (Precios)", new Runnable() {
            public void run() {
                dispose();
            }
        }), BorderLayout.NORTH);

        JPanel cards = new JPanel();
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        cards.setBackground(EstiloCompras.BG_FORM);
        cards.setBorder(BorderFactory.createEmptyBorder(16, 22, 8, 22));

        JPanel fila1 = filaHorizontal();
        fila1.add(EstiloCompras.labeled("Proveedor", comboConBoton(jbox_proveedor, btn_buscar_proveedor), 0));
        fila1.add(Box.createHorizontalStrut(14));
        fila1.add(EstiloCompras.labeled("Transportador", comboConBoton(jbox_transportador, btn_buscar_transportador), 0));
        cards.add(fila1);
        cards.add(Box.createVerticalStrut(10));

        JPanel fila2 = filaHorizontal();
        fila2.add(EstiloCompras.labeled("Fecha de entrada", jdate_fecha_entrada, 220));
        fila2.add(Box.createHorizontalStrut(14));
        fila2.add(EstiloCompras.labeled("Numero de factura", txt_no_factura, 0));
        fila2.add(Box.createHorizontalStrut(14));
        fila2.add(EstiloCompras.labeled("Redondear", txt_redondear, 110));
        fila2.add(Box.createHorizontalStrut(14));
        fila2.add(EstiloCompras.labeled("Consecutivo", lbl_id, 150));
        cards.add(fila2);
        cards.add(Box.createVerticalStrut(12));

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setBackground(EstiloCompras.BG_SECTION);
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JPanel toolLeft = new JPanel();
        toolLeft.setOpaque(false);
        toolLeft.setLayout(new BoxLayout(toolLeft, BoxLayout.X_AXIS));
        txt_codigo_barras.setPreferredSize(new Dimension(320, 38));
        txt_codigo_barras.setMaximumSize(new Dimension(320, 38));
        toolLeft.add(txt_codigo_barras);
        toolLeft.add(Box.createHorizontalStrut(8));
        toolLeft.add(btn_buscar);
        toolLeft.add(Box.createHorizontalStrut(8));
        toolLeft.add(btn_crear_producto);

        JPanel toolRight = new JPanel();
        toolRight.setOpaque(false);
        toolRight.setLayout(new BoxLayout(toolRight, BoxLayout.X_AXIS));
        toolRight.add(btn_calcular_utildiad_porcentaje);
        toolRight.add(Box.createHorizontalStrut(8));
        toolRight.add(btn_Exportar_Worold);
        toolRight.add(Box.createHorizontalStrut(8));
        toolRight.add(btn_precargar);
        toolRight.add(Box.createHorizontalStrut(8));
        toolRight.add(btn_calcular_costos);
        toolRight.add(Box.createHorizontalStrut(8));
        toolRight.add(btn_calcular_precios_venta);

        toolbar.add(toolLeft, BorderLayout.WEST);
        toolbar.add(toolRight, BorderLayout.EAST);
        cards.add(toolbar);

        north.add(cards, BorderLayout.CENTER);
        root.add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(EstiloCompras.BG_FORM);
        center.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 22));
        center.add(jPanel3, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(16, 0));
        south.setBackground(EstiloCompras.BG_SECTION);
        south.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCompras.DIVIDER),
                BorderFactory.createEmptyBorder(12, 22, 12, 22)));

        JPanel obs = new JPanel(new BorderLayout(0, 4));
        obs.setOpaque(false);
        obs.setPreferredSize(new Dimension(420, 150));
        JLabel lblObs = new JLabel("Observacion");
        lblObs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblObs.setForeground(EstiloCompras.TEXT_SECONDARY);
        JPanel obsHead = new JPanel(new BorderLayout());
        obsHead.setOpaque(false);
        obsHead.add(lblObs, BorderLayout.WEST);
        obsHead.add(btn_actualizar_obs, BorderLayout.EAST);
        obs.add(obsHead, BorderLayout.NORTH);
        obs.add(jScrollPane2, BorderLayout.CENTER);
        jpanel_dinero = obs;

        jpanel_dinero1 = new JPanel();
        jpanel_dinero1.setOpaque(false);
        jpanel_dinero1.setLayout(new java.awt.GridLayout(4, 2, 16, 6));
        jpanel_dinero1.add(etiquetaTotal("Sub Total:"));
        jpanel_dinero1.add(lbl_sub_total);
        jpanel_dinero1.add(etiquetaTotal("Descuento:"));
        jpanel_dinero1.add(lbl_descuento);
        jpanel_dinero1.add(etiquetaTotal("IVA:"));
        jpanel_dinero1.add(lbl_iva);
        jpanel_dinero1.add(etiquetaTotal("Total:"));
        jpanel_dinero1.add(lbl_total_factura);
        JPanel totalesWrap = new JPanel(new java.awt.GridBagLayout());
        totalesWrap.setOpaque(false);
        totalesWrap.add(jpanel_dinero1);

        pnl_ingreso = new JPanel();
        pnl_ingreso.setOpaque(false);
        pnl_ingreso.setLayout(new BoxLayout(pnl_ingreso, BoxLayout.Y_AXIS));

        JPanel itemsRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        itemsRow.setOpaque(false);
        JLabel lblItems = new JLabel("Total items:");
        lblItems.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblItems.setForeground(EstiloCompras.TEXT_PRIMARY);
        itemsRow.add(lblItems);
        itemsRow.add(lbl_total_items);
        itemsRow.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel botonesRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        botonesRow.setOpaque(false);
        botonesRow.add(btn_imprimir);
        botonesRow.add(btn_limpiar);
        botonesRow.add(btn_guardar);
        botonesRow.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel chkRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        chkRow.setOpaque(false);
        chkRow.add(chk_cerrar);
        chkRow.setAlignmentX(Component.RIGHT_ALIGNMENT);

        pnl_ingreso.add(itemsRow);
        pnl_ingreso.add(Box.createVerticalStrut(8));
        pnl_ingreso.add(botonesRow);
        pnl_ingreso.add(Box.createVerticalStrut(4));
        pnl_ingreso.add(chkRow);

        south.add(jpanel_dinero, BorderLayout.WEST);
        south.add(totalesWrap, BorderLayout.CENTER);
        south.add(pnl_ingreso, BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(1500, 860);
        setMinimumSize(new Dimension(1100, 700));
    }

    // ---------------- Helpers de estilo ----------------
    private void estiloCampo(javax.swing.JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setPreferredSize(new Dimension(0, 38));
    }

    private JPanel comboConBoton(JComboBox combo, JButton boton) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        p.add(combo, BorderLayout.CENTER);
        p.add(boton, BorderLayout.EAST);
        p.setPreferredSize(new Dimension(0, 38));
        return p;
    }

    private JPanel filaHorizontal() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        return row;
    }

    private JLabel valorTotal(Color c) {
        JLabel l = new JLabel("0");
        l.setFont(new Font("Segoe UI", Font.BOLD, 15));
        l.setForeground(c);
        return l;
    }

    private JLabel etiquetaTotal(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(EstiloCompras.TEXT_PRIMARY);
        return l;
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
            int dia, mes, ano;
            ano = jdate_fecha_entrada.getCalendar().get(Calendar.YEAR);
            mes = jdate_fecha_entrada.getCalendar().get(Calendar.MONTH) + 1;
            dia = jdate_fecha_entrada.getCalendar().get(Calendar.DAY_OF_MONTH);
            ingreso_cabecera.setFecha_ingreso("'" + ano + "-" + mes + "-" + dia + "'");
            // La HORA es la del reloj del sistema al momento de registrar (antes
            // se tomaba del selector de fecha, que queda en 00:00:00). El flujo
            // completo con cada cambio de estado queda ademas en auditoria_ingresos.
            Calendar ahora = Calendar.getInstance();
            ingreso_cabecera.setHora(String.format("'%02d:%02d:%02d'",
                    ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE), ahora.get(Calendar.SECOND)));
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

    /**
     * Actualiza SOLO la observacion del ingreso abierto, sin importar el rol del
     * usuario ni el estado del ingreso. Escribe directo en la cabecera por id.
     */
    private void btn_actualizar_obsActionPerformed(java.awt.event.ActionEvent evt) {
        int id;
        try {
            id = Integer.parseInt(lbl_id.getText().trim());
        } catch (Exception e) {
            id = 0;
        }
        if (id <= 0 || es_nuevo) {
            JOptionPane.showMessageDialog(this,
                    "Primero debe guardar el ingreso para poder actualizar la observacion.");
            return;
        }
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE ingresos_productos_cabecera SET observacion=? WHERE id=?")) {
            ps.setString(1, txt_observacion.getText());
            ps.setInt(2, id);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Observacion actualizada.");
            try {
                frm_ingresos_precios.btn_actualizar.doClick();
            } catch (Exception ignored) {
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar la observacion:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
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
    public static javax.swing.JButton btn_actualizar_obs;
    public static javax.swing.JButton btn_buscar_proveedor;
    public static javax.swing.JButton btn_buscar_transportador;
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
