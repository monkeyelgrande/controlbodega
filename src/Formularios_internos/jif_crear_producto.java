/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Monkeyelgrande
 */
package Formularios_internos;

import Formularios.frm_main;
import Formularios.frm_productos;
import JDBuscar.jd_buscar_producto_padre;
import Metodos.TextPrompt;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBproductos;
import java.awt.Color;
import java.awt.event.KeyEvent;
import javax.swing.JOptionPane;
import modelos.Productos;
import modelos.Unidades;

public class jif_crear_producto extends javax.swing.JDialog {

    /**
     * Creates new form jif_crear_producto
     */
    public static String formulario = "";

    // --- Kardex ---
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JTable tablaKardex;
    private javax.swing.table.DefaultTableModel modeloKardex;
    private javax.swing.JPanel panelKardex;

    public jif_crear_producto() {
        initComponents();
        construirTabbedPane();
        this.setLocationRelativeTo(this);
        cargar_campos_defecto();
        btn_editar.setVisible(false);
        metodos.addEscapeListenerWindowDialog(this);
        holders();
        metodos.EvitarTabEnJTextArea(jtxt_descripcion);
        btn_e_cod_barras.setVisible(false);
        Unidades.mostrarUnidades(jbox_unidad);

    }

    /**
     * Reestructura el contenido con un JTabbedPane:
     *   Tab 1: "Producto" (formulario existente)
     *   Tab 2: "Kardex" (movimientos de inventario)
     */
    private void construirTabbedPane() {
        // Extraer los paneles del content pane
        getContentPane().removeAll();

        // Tab 1: Producto (paneles existentes)
        javax.swing.JPanel tabProducto = new javax.swing.JPanel(new java.awt.BorderLayout());
        tabProducto.add(jPanel2, java.awt.BorderLayout.CENTER);
        tabProducto.add(jPanel6, java.awt.BorderLayout.SOUTH);

        // Tab 2: Kardex
        panelKardex = construirPanelKardex();

        // Crear TabbedPane
        tabbedPane = new javax.swing.JTabbedPane();
        tabbedPane.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        tabbedPane.addTab("Producto", tabProducto);
        tabbedPane.addTab("Kardex", panelKardex);

        getContentPane().setLayout(new java.awt.BorderLayout());
        getContentPane().add(tabbedPane, java.awt.BorderLayout.CENTER);

        setSize(1370, 750);
        setMinimumSize(new java.awt.Dimension(650, 500));
    }

    private javax.swing.JPanel construirPanelKardex() {
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
        panel.setBackground(java.awt.Color.WHITE);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Info superior
        javax.swing.JLabel lblInfo = new javax.swing.JLabel("Historial de movimientos de inventario del producto");
        lblInfo.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        lblInfo.setForeground(new java.awt.Color(40, 53, 147));
        panel.add(lblInfo, java.awt.BorderLayout.NORTH);

        // Tabla kardex
        modeloKardex = new javax.swing.table.DefaultTableModel(
                new Object[]{"Fecha", "Hora", "Tipo", "Bodega", "Valor", "Cant. Anterior", "Cant. Nueva", "Pend. Anterior", "Pend. Nuevo", "Referencia", "Usuario", "Observaci\u00f3n"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tablaKardex = new javax.swing.JTable(modeloKardex);
        tablaKardex.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        tablaKardex.setRowHeight(28);
        tablaKardex.setAutoCreateRowSorter(true);
        tablaKardex.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablaKardex.setShowGrid(false);
        tablaKardex.setIntercellSpacing(new java.awt.Dimension(0, 0));
        tablaKardex.setFillsViewportHeight(true);
        tablaKardex.setSelectionBackground(new java.awt.Color(197, 202, 233));

        // Header estilo Material
        tablaKardex.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                javax.swing.JLabel l = (javax.swing.JLabel) super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                l.setBackground(new java.awt.Color(40, 53, 147));
                l.setForeground(java.awt.Color.WHITE);
                l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 11));
                l.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
                l.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                return l;
            }
        });
        tablaKardex.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 36));
        tablaKardex.getTableHeader().setReorderingAllowed(false);

        // Body renderer con colores por tipo
        tablaKardex.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                javax.swing.JLabel l = (javax.swing.JLabel) super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
                l.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 6));
                l.setOpaque(true);

                if (sel) {
                    l.setBackground(new java.awt.Color(197, 202, 233));
                    l.setForeground(new java.awt.Color(33, 33, 33));
                } else {
                    l.setBackground(row % 2 == 0 ? java.awt.Color.WHITE : new java.awt.Color(245, 247, 251));
                    l.setForeground(new java.awt.Color(33, 33, 33));
                }

                // Colorear tipo de movimiento
                if (col == 2 && value != null && !sel) {
                    String tipo = value.toString().toUpperCase();
                    if (tipo.contains("INGRESO") || tipo.contains("POSITIVO") || tipo.contains("ENTRADA")) {
                        l.setForeground(new java.awt.Color(67, 160, 71));
                        l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
                    } else if (tipo.contains("VENTA") || tipo.contains("NEGATIVO") || tipo.contains("SALIDA") || tipo.contains("ORDEN") || tipo.contains("ANULACION")) {
                        l.setForeground(new java.awt.Color(229, 57, 53));
                        l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
                    }
                }

                // Numeros alineados a la derecha
                if (col >= 4 && col <= 8) {
                    l.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                } else {
                    l.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                }

                return l;
            }
        });

        // Anchos
        javax.swing.table.TableColumnModel cm = tablaKardex.getColumnModel();
        cm.getColumn(0).setPreferredWidth(85);   // Fecha
        cm.getColumn(1).setPreferredWidth(60);   // Hora
        cm.getColumn(2).setPreferredWidth(130);  // Tipo
        cm.getColumn(3).setPreferredWidth(120);  // Bodega
        cm.getColumn(4).setPreferredWidth(70);   // Valor
        cm.getColumn(5).setPreferredWidth(90);   // Cant anterior
        cm.getColumn(6).setPreferredWidth(90);   // Cant nueva
        cm.getColumn(7).setPreferredWidth(90);   // Pend anterior
        cm.getColumn(8).setPreferredWidth(90);   // Pend nuevo
        cm.getColumn(9).setPreferredWidth(100);  // Referencia
        cm.getColumn(10).setPreferredWidth(100); // Usuario
        cm.getColumn(11).setPreferredWidth(200); // Observacion

        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(tablaKardex);
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(224, 224, 224)));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        panel.add(scroll, java.awt.BorderLayout.CENTER);

        return panel;
    }

    /**
     * Carga el kardex (movimientos de inventario) de un producto.
     * Debe llamarse desde frm_productos al abrir en modo "ver".
     *
     * @param idProducto ID del producto
     */
    public void cargarKardex(String idProducto) {
        modeloKardex.setRowCount(0);

        String sql = "SELECT m.fecha, m.hora, m.tipo, b.nombre as bodega, "
                + "m.valor, m.cantidad_anterior, m.cantidad_nueva, "
                + "m.pendientes_anterior, m.pendientes_nuevo, "
                + "CASE WHEN m.tabla_referencia IS NOT NULL "
                + "     THEN m.tabla_referencia || ' #' || m.id_referencia "
                + "     ELSE '' END as referencia, "
                + "u.nombre as usuario, "
                + "COALESCE(m.observacion, '') as observacion "
                + "FROM movimientos_inventario m "
                + "JOIN bodegas b ON b.id = m.id_bodega "
                + "JOIN users u ON u.id = m.id_user "
                + "WHERE m.id_producto = " + idProducto + " "
                + "ORDER BY m.fecha DESC, m.id DESC";

        java.text.DecimalFormat df = new java.text.DecimalFormat("###,###.##");

        try {
            java.sql.ResultSet rs = conexiondb.DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                modeloKardex.addRow(new Object[]{
                    rs.getString("fecha"),
                    rs.getString("hora") != null ? rs.getString("hora") : "",
                    rs.getString("tipo"),
                    rs.getString("bodega"),
                    df.format(rs.getDouble("valor")),
                    df.format(rs.getDouble("cantidad_anterior")),
                    df.format(rs.getDouble("cantidad_nueva")),
                    df.format(rs.getDouble("pendientes_anterior")),
                    df.format(rs.getDouble("pendientes_nuevo")),
                    rs.getString("referencia"),
                    rs.getString("usuario"),
                    rs.getString("observacion")
                });
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error cargando kardex: " + e.getMessage());
        }

        // Actualizar titulo del tab
        tabbedPane.setTitleAt(1, "Kardex (" + modeloKardex.getRowCount() + ")");
    }

    public void holders() {
        TextPrompt cod = new TextPrompt("Obligatorio", txt_cod_barras);
        TextPrompt desc = new TextPrompt("Obligatorio", jtxt_descripcion);

    }

    public void cargar_campos_defecto() {

        txt_id.setText(DB_consultas_R_D.cargarId("productos"));

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel6 = new javax.swing.JPanel();
        btn_guardar = new javax.swing.JButton();
        btn_limpiar = new javax.swing.JButton();
        chk_cerrar = new javax.swing.JCheckBox();
        btn_editar = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txt_cod_barras = new javax.swing.JTextField();
        txt_id = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtxt_descripcion = new javax.swing.JTextArea();
        btn_e_cod_barras = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        txt_stock_minimo = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txt_stock_ideal = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        lbl_producto_padre = new javax.swing.JLabel();
        lbl_id_producto_padre = new javax.swing.JLabel();
        btn_buscar_padre = new javax.swing.JButton();
        btn_quitar_padre = new javax.swing.JButton();
        txt_cant_paquete = new javax.swing.JTextField();
        lbl_cant_paquete = new javax.swing.JLabel();
        jbox_unidad = new javax.swing.JComboBox<>();
        txt_pcosto = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        txt_pventa = new javax.swing.JTextField();
        jbox_tipo = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel17 = new javax.swing.JLabel();
        txt_pventa2 = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        txt_pventa3 = new javax.swing.JTextField();

        setModal(true);

        jPanel6.setBackground(new java.awt.Color(33, 33, 33));

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

        chk_cerrar.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        chk_cerrar.setForeground(new java.awt.Color(255, 255, 255));
        chk_cerrar.setSelected(true);
        chk_cerrar.setText("Cerrar formulario al guardar");

        btn_editar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_editar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/editar.png"))); // NOI18N
        btn_editar.setText("Editar");
        btn_editar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_editarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chk_cerrar)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(btn_guardar, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_limpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_editar)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btn_limpiar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_editar, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addComponent(btn_guardar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chk_cerrar)
                .addGap(0, 9, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        jPanel2.setEnabled(false);

        jLabel3.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jLabel3.setText("Código");

        jLabel6.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jLabel6.setText("Descripción");

        txt_cod_barras.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_cod_barras.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_cod_barrasFocusLost(evt);
            }
        });

        txt_id.setEditable(false);
        txt_id.setBackground(new java.awt.Color(204, 204, 204));
        txt_id.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N

        jLabel12.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        jLabel12.setText("ID");

        jtxt_descripcion.setColumns(20);
        jtxt_descripcion.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jtxt_descripcion.setLineWrap(true);
        jtxt_descripcion.setRows(5);
        jScrollPane1.setViewportView(jtxt_descripcion);

        btn_e_cod_barras.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        btn_e_cod_barras.setText("Editar");
        btn_e_cod_barras.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_e_cod_barrasActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jLabel4.setText("Stock mínimo");

        txt_stock_minimo.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_stock_minimo.setText("0");
        txt_stock_minimo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_stock_minimoFocusLost(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jLabel5.setText("Stock ideal");

        txt_stock_ideal.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_stock_ideal.setText("0");
        txt_stock_ideal.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_stock_idealFocusLost(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jLabel7.setText("Unidad");

        jLabel8.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jLabel8.setText("Producto padre");

        lbl_producto_padre.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        lbl_producto_padre.setForeground(new java.awt.Color(204, 0, 102));
        lbl_producto_padre.setText("-");

        lbl_id_producto_padre.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        lbl_id_producto_padre.setText("0");

        btn_buscar_padre.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        btn_buscar_padre.setText("Buscar");
        btn_buscar_padre.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscar_padreActionPerformed(evt);
            }
        });

        btn_quitar_padre.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        btn_quitar_padre.setText("Quitar padre");
        btn_quitar_padre.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_quitar_padreActionPerformed(evt);
            }
        });

        txt_cant_paquete.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_cant_paquete.setText("1");

        lbl_cant_paquete.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        lbl_cant_paquete.setText("Cant x paquete");

        jbox_unidad.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N

        txt_pcosto.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_pcosto.setText("0");
        txt_pcosto.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_pcostoFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_pcostoFocusLost(evt);
            }
        });
        txt_pcosto.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_pcostoKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_pcostoKeyTyped(evt);
            }
        });

        jLabel11.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jLabel11.setText("Precio costo");

        jLabel16.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jLabel16.setText("Precio venta 1");

        txt_pventa.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_pventa.setText("0");
        txt_pventa.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_pventaFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_pventaFocusLost(evt);
            }
        });
        txt_pventa.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_pventaKeyPressed(evt);
            }
        });

        jbox_tipo.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jbox_tipo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "FV", "FR" }));

        jLabel9.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jLabel9.setText("Tipo");

        jLabel17.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jLabel17.setText("Precio venta 2");

        txt_pventa2.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_pventa2.setText("0");
        txt_pventa2.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_pventa2FocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_pventa2FocusLost(evt);
            }
        });
        txt_pventa2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_pventa2KeyPressed(evt);
            }
        });

        jLabel18.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        jLabel18.setText("Precio venta 3");

        txt_pventa3.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_pventa3.setText("0");
        txt_pventa3.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_pventa3FocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_pventa3FocusLost(evt);
            }
        });
        txt_pventa3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_pventa3KeyPressed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel12)
                            .addComponent(jLabel3)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(lbl_producto_padre, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lbl_id_producto_padre)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_buscar_padre, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_quitar_padre, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(txt_stock_minimo, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txt_stock_ideal, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jbox_unidad, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lbl_cant_paquete)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txt_cant_paquete, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(txt_id, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jScrollPane1)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(txt_cod_barras, javax.swing.GroupLayout.PREFERRED_SIZE, 677, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_e_cod_barras, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel17)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txt_pventa2, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel11)
                                    .addComponent(jLabel8))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txt_pcosto, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel18)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txt_pventa3, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txt_pventa, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jbox_tipo, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txt_id, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(txt_cod_barras, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_e_cod_barras))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_quitar_padre)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel8)
                                .addComponent(lbl_producto_padre)
                                .addComponent(lbl_id_producto_padre)
                                .addComponent(btn_buscar_padre)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txt_pcosto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel11)
                            .addComponent(jLabel16)
                            .addComponent(txt_pventa, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jbox_tipo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel9)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(txt_pventa2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18)
                    .addComponent(txt_pventa3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lbl_cant_paquete)
                        .addComponent(txt_cant_paquete, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jbox_unidad, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel5)
                        .addComponent(txt_stock_ideal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel7))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(txt_stock_minimo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public boolean validaciones() {
        if (txt_cod_barras.getText().isEmpty()) {
            txt_cod_barras.setBackground(Color.pink);
            return false;
        } else {
            txt_cod_barras.setBackground(Color.white);
        }

        if (jtxt_descripcion.getText().isEmpty()) {
            jtxt_descripcion.setBackground(Color.pink);
            return false;
        } else {
            jtxt_descripcion.setBackground(Color.white);
        }

        return true;
    }
    public static int id_unidad;
    private void btn_guardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_guardarActionPerformed

        if (!validaciones()) {
        } else {
            DBproductos dbproductos = new DBproductos();

            Productos producto = new Productos();
            try {
                producto.setId(Integer.parseInt(txt_id.getText()));

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e);
            }
            producto.setCodigo_barras(txt_cod_barras.getText());
            producto.setDescripcion(jtxt_descripcion.getText());
            try {
                producto.setStock_minimo(Integer.parseInt(txt_stock_minimo.getText()));
            } catch (Exception e) {
                producto.setStock_minimo(0);
            }
            try {
                producto.setStock_ideal(Integer.parseInt(txt_stock_ideal.getText()));
            } catch (Exception e) {
                producto.setStock_ideal(0);
            }
            try {
                producto.setId_unidad(jbox_unidad.getItemAt(jbox_unidad.getSelectedIndex()).getId());
            } catch (Exception e) {
                producto.setId_unidad(id_unidad);
            }
            try {
                producto.setTipo(jbox_tipo.getSelectedIndex());
            } catch (Exception e) {
                producto.setTipo(0);
            }
            producto.setCant_paquete(Integer.parseInt(txt_cant_paquete.getText()));
            producto.setId_padre(Integer.parseInt(lbl_id_producto_padre.getText()));
            producto.setPrecio_costo(Double.parseDouble(metodos.EliminaCaracteres(txt_pcosto.getText(), ".")));
            producto.setPrecio_venta(Double.parseDouble(metodos.EliminaCaracteres(txt_pventa.getText(), ".")));
            producto.setPrecio_venta2(Double.parseDouble(metodos.EliminaCaracteres(txt_pventa2.getText(), ".")));
            producto.setPrecio_venta3(Double.parseDouble(metodos.EliminaCaracteres(txt_pventa3.getText(), ".")));

            if (DB_consultas_R_D.consultarId(txt_id.getText(), "productos") == 1) {
                dbproductos.Actualizar(producto);
            } else {
                dbproductos.Guardar(producto);
            }

            switch (formulario) {
                case "crear":
                    frm_productos.btn_actualizar.doClick();
                    break;

                case "ingreso_mercancia":

                    JOptionPane.showMessageDialog(this, "Se ha creado el producto");
                    jif_crear_ingreso_mercancia.txt_codigo_barras.setText(txt_cod_barras.getText());

                    break;
            }
            limpiar();
            txt_id.setText(DB_consultas_R_D.cargarId("productos"));
            if (chk_cerrar.isSelected()) {
                this.dispose();
            }

        }
    }//GEN-LAST:event_btn_guardarActionPerformed

    private void btn_limpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_limpiarActionPerformed
        limpiar();
    }//GEN-LAST:event_btn_limpiarActionPerformed
    public void limpiar() {
        txt_cod_barras.setText("");
        jtxt_descripcion.setText("");
        txt_cod_barras.setBackground(Color.white);
        jtxt_descripcion.setBackground(Color.white);

    }


    private void btn_editarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_editarActionPerformed
//        txt_cod_barras.setEnabled(true);
        btn_e_cod_barras.setVisible(true);
        jtxt_descripcion.setEnabled(true);
        txt_stock_minimo.setEnabled(true);
        txt_stock_ideal.setEnabled(true);
        txt_pcosto.setEnabled(true);
        txt_pventa.setEnabled(true);
        txt_pventa2.setEnabled(true);
        txt_pventa3.setEnabled(true);
        jbox_unidad.setEnabled(true);
        btn_guardar.setEnabled(true);
        btn_limpiar.setEnabled(true);
        chk_cerrar.setEnabled(true);
        jbox_tipo.setEnabled(true);

    }//GEN-LAST:event_btn_editarActionPerformed

    private void txt_cod_barrasFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_cod_barrasFocusLost
        if (DB_consultas_R_D.consultar_existencia_campo_String("codigo_barras", txt_cod_barras.getText(), "productos") == 1) {
            JOptionPane.showMessageDialog(this, "El código de barras ingresado ya esta registrado.\nIngrese un código distinto");
            txt_cod_barras.setText("");
            txt_cod_barras.requestFocus();
        }
    }//GEN-LAST:event_txt_cod_barrasFocusLost

    private void btn_e_cod_barrasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_e_cod_barrasActionPerformed
        txt_cod_barras.setEnabled(true);
    }//GEN-LAST:event_btn_e_cod_barrasActionPerformed

    private void txt_stock_minimoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_stock_minimoFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_stock_minimoFocusLost

    private void txt_stock_idealFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_stock_idealFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_stock_idealFocusLost

    private void btn_buscar_padreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_buscar_padreActionPerformed
        jd_buscar_producto_padre buscar_producto = new jd_buscar_producto_padre(null, rootPaneCheckingEnabled);
        buscar_producto.show();
    }//GEN-LAST:event_btn_buscar_padreActionPerformed

    private void btn_quitar_padreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_quitar_padreActionPerformed
        lbl_id_producto_padre.setText("0");
        lbl_producto_padre.setText("-");
    }//GEN-LAST:event_btn_quitar_padreActionPerformed

    private void txt_pcostoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_pcostoFocusGained
        if (!txt_pcosto.getText().equals("")) {
            String texto = metodos.EliminaCaracteres(txt_pcosto.getText(), ".");
            txt_pcosto.setText(texto);
        }
        txt_pcosto.selectAll();
    }//GEN-LAST:event_txt_pcostoFocusGained

    private void txt_pcostoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_pcostoFocusLost
        if (!txt_pcosto.getText().equals("")) {
            double to = Double.parseDouble(txt_pcosto.getText());
            String nuevo = metodos.formateador_decimal().format(to);
            txt_pcosto.setText(nuevo);
        }
        txt_pventa.setText(txt_pcosto.getText());
    }//GEN-LAST:event_txt_pcostoFocusLost

    private void txt_pcostoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_pcostoKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {

        }
    }//GEN-LAST:event_txt_pcostoKeyPressed

    private void txt_pcostoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_pcostoKeyTyped
        char num = evt.getKeyChar();
        DB_consultas_R_D.validar_numeros(evt, num);
    }//GEN-LAST:event_txt_pcostoKeyTyped

    private void txt_pventaFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_pventaFocusGained
        if (!txt_pventa.getText().equals("")) {
            String texto = metodos.EliminaCaracteres(txt_pventa.getText(), ".");
            txt_pventa.setText(texto);
        }
        txt_pventa.selectAll();
    }//GEN-LAST:event_txt_pventaFocusGained

    private void txt_pventaFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_pventaFocusLost

        if (!txt_pventa.getText().equals("")) {
            double to = Double.parseDouble(txt_pventa.getText());
            String nuevo = metodos.formateador_dinero().format(to);
            txt_pventa.setText(nuevo);
        }
    }//GEN-LAST:event_txt_pventaFocusLost

    private void txt_pventaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_pventaKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
        }
    }//GEN-LAST:event_txt_pventaKeyPressed

    private void txt_pventa2FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_pventa2FocusGained
        if (!txt_pventa2.getText().equals("")) {
            String texto = metodos.EliminaCaracteres(txt_pventa2.getText(), ".");
            txt_pventa2.setText(texto);
        }
        txt_pventa2.selectAll();
    }//GEN-LAST:event_txt_pventa2FocusGained

    private void txt_pventa2FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_pventa2FocusLost
        if (!txt_pventa2.getText().equals("")) {
            double to = Double.parseDouble(txt_pventa2.getText());
            String nuevo = metodos.formateador_dinero().format(to);
            txt_pventa2.setText(nuevo);
        }    }//GEN-LAST:event_txt_pventa2FocusLost

    private void txt_pventa2KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_pventa2KeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
        }
    }//GEN-LAST:event_txt_pventa2KeyPressed

    private void txt_pventa3FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_pventa3FocusGained
        if (!txt_pventa3.getText().equals("")) {
            String texto = metodos.EliminaCaracteres(txt_pventa3.getText(), ".");
            txt_pventa3.setText(texto);
        }
        txt_pventa3.selectAll();
    }//GEN-LAST:event_txt_pventa3FocusGained

    private void txt_pventa3FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_pventa3FocusLost
        if (!txt_pventa3.getText().equals("")) {
            double to = Double.parseDouble(txt_pventa3.getText());
            String nuevo = metodos.formateador_dinero().format(to);
            txt_pventa3.setText(nuevo);
        }    }//GEN-LAST:event_txt_pventa3FocusLost

    private void txt_pventa3KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_pventa3KeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
        }
    }//GEN-LAST:event_txt_pventa3KeyPressed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    public static javax.swing.JButton btn_buscar_padre;
    public static javax.swing.JButton btn_e_cod_barras;
    public static javax.swing.JButton btn_editar;
    public static javax.swing.JButton btn_guardar;
    public static javax.swing.JButton btn_limpiar;
    public static javax.swing.JButton btn_quitar_padre;
    public static javax.swing.JCheckBox chk_cerrar;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    public static javax.swing.JComboBox jbox_tipo;
    public static javax.swing.JComboBox<Unidades> jbox_unidad;
    public static javax.swing.JTextArea jtxt_descripcion;
    private javax.swing.JLabel lbl_cant_paquete;
    public static javax.swing.JLabel lbl_id_producto_padre;
    public static javax.swing.JLabel lbl_producto_padre;
    public static javax.swing.JTextField txt_cant_paquete;
    public static javax.swing.JTextField txt_cod_barras;
    public static javax.swing.JTextField txt_id;
    public static javax.swing.JTextField txt_pcosto;
    public static javax.swing.JTextField txt_pventa;
    public static javax.swing.JTextField txt_pventa2;
    public static javax.swing.JTextField txt_pventa3;
    public static javax.swing.JTextField txt_stock_ideal;
    public static javax.swing.JTextField txt_stock_minimo;
    // End of variables declaration//GEN-END:variables
}
