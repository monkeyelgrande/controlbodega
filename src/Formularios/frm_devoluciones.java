/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios;

import static Formularios.frm_main.escritorio;
import Formularios_internos.jd_ver_devolucion;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBdevoluciones_cabeceras;
import conexiondb.DBdevoluciones_detalles;
import conexiondb.DBfacturas_cabeceras;
import conexiondb.DBstock_productos;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import modelos.Bodegas;
import modelos.Devoluciones_Cabeceras;
import modelos.Devoluciones_detalles;

/**
 *
 * @author Monkeyelgrande
 */
public class frm_devoluciones extends javax.swing.JInternalFrame {

    /**
     * Creates new form frm_devoluciones
     */
    private TableRowSorter trsFiltro;
    DecimalFormat formatea = new DecimalFormat("###,###.##");
    DecimalFormatSymbols simbolos = new DecimalFormatSymbols();

    DecimalFormat formatDecimal;

    DefaultTableModel modelo_factura = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }

    };
    DefaultTableModel modelo_devolver = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (col == 5) { // digo que la columna 5 (Total) sera igual a la siguiente opreacion
                Double i; // i sera igual a la cantidad
                try {
                    i = Double.parseDouble(getValueAt(row, 3).toString()); // capturo la cantidad
                } catch (Exception e) {
                    System.out.println(e);
                    i = 1.0;
                }

                Double d = Double.parseDouble(metodos.EliminaCaracteres((String) getValueAt(row, 4), ".")); // d sera igual al precio
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

    public frm_devoluciones() {
        initComponents();
        simbolos.setDecimalSeparator('.');
        formatDecimal = new DecimalFormat("#.0", simbolos);
        actualizar();
        listeners();
        metodos.EstiloTablaMaterialGlobal(jtabla_factura);
        Bodegas bod = new Bodegas();
        bod.mostrarBodegas(jbox_bodega);
        lbl_id_cabecera_devolucion.setText(DB_consultas_R_D.cargarId("devoluciones"));
    }

    public void listeners() {

        modelo_devolver.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "PRECIO", "TOTAL"});

        jtabla_factura.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                Point p = me.getPoint();
                if (me.getClickCount() == 2) {
                    pasar_a_tabla_devolucion();
                }
            }
        });
        jtabla_factura.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_ENTER) {
                    pasar_a_tabla_devolucion();
                }
            }
        });
        jtabla_devoluciones.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                Point p = me.getPoint();
                if (me.getClickCount() == 2) {
                    int fila = jtabla_devoluciones.getSelectedRow();
                    modelo_devolver.removeRow(fila);
                    calcular_total();

                }
            }
        });
        jtabla_devoluciones.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_ENTER) {
                    int fila = jtabla_devoluciones.getSelectedRow();
                    modelo_devolver.removeRow(fila);
                    calcular_total();
                }
            }
        });
        jtabla_dev_principal.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                Point p = me.getPoint();
                if (me.getClickCount() == 2) {
                    btn_verActionPerformed(null);

                }
            }
        });
        jtabla_dev_principal.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_ENTER) {
                    btn_verActionPerformed(null);

                }
            }
        });
    }

    public void pasar_a_tabla_devolucion() {

        int fila = jtabla_factura.getSelectedRow();

        String canti = "" + jtabla_factura.getValueAt(fila, 3);
        double can = Double.parseDouble(jtabla_factura.getValueAt(fila, 3) + "");
        String cod = "" + jtabla_factura.getValueAt(fila, 1);
        if (existe_en_tabla_devoluciones(cod)) {    //aca evaluo si el producto ya fue agregado con anterioridad a la tabla de devoluciones
            double cantidad = 1;
            if (can != 1) {
                int flag = 1;
                do {
                    try {
                        cantidad = Double.parseDouble(JOptionPane.showInputDialog("ingrese la cantidad que desea devolver \n la cantidad no puede ser superior a " + can));
                        flag = 1;
                    } catch (Exception e) {
                        flag = 0;
                    }
                } while (cantidad > can || cantidad <= 0 || flag == 0);
            }

            Double can_devolver = Double.parseDouble("" + jtabla_devoluciones.getValueAt(posicion_en_jtable(cod), 3));
            double suma = can_devolver + cantidad;
            modelo_devolver.setValueAt(suma, posicion_en_jtable(cod), 3);

            if (cantidad == can) {
                modelo_factura.removeRow(fila);
            } else {
                double resta = can - cantidad;
                modelo_factura.setValueAt(formatDecimal.format(resta), fila, 3);

            }
            calcular_total();

        } else {
            double cantidad = 1;
            if (can != 1) {
                int flag = 1;
                do {
                    try {
                        cantidad = Double.parseDouble(JOptionPane.showInputDialog("ingrese la cantidad que desea devolver \n la cantidad no puede ser superior a " + can));
                        flag = 1;
                    } catch (Exception e) {
                        flag = 0;
                    }
                } while (cantidad > can || cantidad <= 0 || flag == 0);
            }

            modelo_devolver.addRow(new Object[]{(String) modelo_factura.getValueAt(fila, 0), (String) modelo_factura.getValueAt(fila, 1), (String) modelo_factura.getValueAt(fila, 2),
                cantidad, (String) modelo_factura.getValueAt(fila, 4)});
            jtabla_devoluciones.setModel(modelo_devolver);

            if (cantidad == can) {
                modelo_factura.removeRow(fila);
            } else {
                double resta = can - cantidad;

                modelo_factura.setValueAt(formatDecimal.format(resta), fila, 3);

            }
            calcular_total();
        }
    }

    private boolean existe_en_tabla_devoluciones(String codigo) {
        try {
            for (int i = 0; i < this.jtabla_devoluciones.getRowCount(); i++) {
                if (this.jtabla_devoluciones.getValueAt(i, 1).toString().equals(codigo)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }

    }

    private int posicion_en_jtable(String codigo) {
        for (int i = 0; i < this.jtabla_devoluciones.getRowCount(); i++) {
            if (this.jtabla_devoluciones.getValueAt(i, 1).toString().equals(codigo)) {
                return i;
            }
        }
        return 0;
    }

    public void calcular_total() {
        double total = 0;
        for (int i = 0; i < this.jtabla_devoluciones.getRowCount(); i++) {
            total += Double.parseDouble(metodos.EliminaCaracteres(this.jtabla_devoluciones.getValueAt(i, 5).toString(), "."));
        }
        txt_total_devolver.setText(formatea.format(total) + "");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jtab_devoluciones = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        lbl_cant_clientes = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla_dev_principal = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        txt_Filtro = new javax.swing.JTextField();
        comboFiltro = new javax.swing.JComboBox();
        jButton1 = new javax.swing.JButton();
        btn_ver = new javax.swing.JButton();
        btn_actualizar = new javax.swing.JButton();
        btn_eliminar = new javax.swing.JButton();
        btn_crear = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txt_numero_factura = new javax.swing.JTextField();
        btn_buscar = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        lbl_id_factura = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        lbl_cliente = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        lbl_fecha = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla_factura = new javax.swing.JTable();
        jlabel15 = new javax.swing.JLabel();
        lbl_total_factura = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtabla_devoluciones = new javax.swing.JTable();
        btn_guardar = new javax.swing.JButton();
        jlabel16 = new javax.swing.JLabel();
        txt_total_devolver = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        btn_limpiar = new javax.swing.JButton();
        lbl_id_cabecera_devolucion = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jbox_bodega = new javax.swing.JComboBox<>();

        setClosable(true);
        setMaximizable(true);
        setTitle("Devoluciones");

        jtab_devoluciones.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jtab_devolucionesMouseClicked(evt);
            }
        });

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        lbl_cant_clientes.setBackground(new java.awt.Color(33, 33, 33));

        jtabla_dev_principal.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jtabla_dev_principal.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_dev_principal.setGridColor(new java.awt.Color(255, 255, 255));
        jtabla_dev_principal.setRowHeight(25);
        jtabla_dev_principal.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtabla_dev_principal.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jtabla_dev_principal.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jtabla_dev_principal);

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Buscar:");

        txt_Filtro.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_FiltroFocusGained(evt);
            }
        });
        txt_Filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_FiltroKeyTyped(evt);
            }
        });

        comboFiltro.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ID factura", "Cliente", "Fecha" }));
        comboFiltro.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboFiltroActionPerformed(evt);
            }
        });

        jButton1.setBackground(new java.awt.Color(102, 0, 0));
        jButton1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 255, 255));
        jButton1.setMnemonic('w');
        jButton1.setText("Cerrar");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout lbl_cant_clientesLayout = new javax.swing.GroupLayout(lbl_cant_clientes);
        lbl_cant_clientes.setLayout(lbl_cant_clientesLayout);
        lbl_cant_clientesLayout.setHorizontalGroup(
            lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 975, Short.MAX_VALUE)
                    .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                        .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                                .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton1)))
                .addContainerGap())
        );
        lbl_cant_clientesLayout.setVerticalGroup(
            lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton1)
                    .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 566, Short.MAX_VALUE)
                .addContainerGap())
        );

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

        btn_actualizar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_actualizar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/actualizar.png"))); // NOI18N
        btn_actualizar.setText("Actualizar");
        btn_actualizar.setBorder(null);
        btn_actualizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_actualizarActionPerformed(evt);
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

        btn_crear.setFont(btn_crear.getFont().deriveFont(btn_crear.getFont().getStyle() | java.awt.Font.BOLD, 14));
        btn_crear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/nuevo.png"))); // NOI18N
        btn_crear.setMnemonic('n');
        btn_crear.setText("Nuevo");
        btn_crear.setBorder(null);
        btn_crear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_crearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_eliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_crear, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_actualizar, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_ver, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addComponent(lbl_cant_clientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lbl_cant_clientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_crear)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btn_eliminar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_actualizar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_ver)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jtab_devoluciones.addTab("Lista de devoluciones", jPanel2);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel1.setText("Numero de factura");

        txt_numero_factura.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        txt_numero_factura.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_numero_facturaKeyPressed(evt);
            }
        });

        btn_buscar.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btn_buscar.setMnemonic('b');
        btn_buscar.setText("Buscar");
        btn_buscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscarActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel2.setText("ID");

        lbl_id_factura.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        lbl_id_factura.setText("ID");

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel4.setText("Cliente");

        lbl_cliente.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        lbl_cliente.setText("Cliente");

        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel6.setText("Fecha");

        lbl_fecha.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        lbl_fecha.setText("Fecha");

        jtabla_factura.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jScrollPane2.setViewportView(jtabla_factura);

        jlabel15.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jlabel15.setText("Total Facturado");

        lbl_total_factura.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        lbl_total_factura.setText("Total");

        jPanel3.setBackground(new java.awt.Color(98, 192, 255));

        jtabla_devoluciones.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_devoluciones.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(jtabla_devoluciones);

        btn_guardar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_guardar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/guardar.png"))); // NOI18N
        btn_guardar.setMnemonic('g');
        btn_guardar.setText("Guardar");
        btn_guardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_guardarActionPerformed(evt);
            }
        });

        jlabel16.setFont(new java.awt.Font("Tahoma", 3, 14)); // NOI18N
        jlabel16.setText("Total dinero a devolver");

        txt_total_devolver.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel7.setText("Productos a devolver");

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

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(btn_guardar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_limpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 350, Short.MAX_VALUE)
                        .addComponent(jlabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txt_total_devolver, javax.swing.GroupLayout.PREFERRED_SIZE, 334, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(btn_guardar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_limpiar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txt_total_devolver, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jlabel16)))
                .addContainerGap(27, Short.MAX_VALUE))
        );

        lbl_id_cabecera_devolucion.setForeground(new java.awt.Color(255, 255, 255));

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

        jbox_bodega.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jbox_bodega.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jbox_bodegaKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1147, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jlabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_factura))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbl_id_factura))
                            .addComponent(lbl_id_cabecera_devolucion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(33, 33, 33)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl_cliente)
                            .addComponent(lbl_fecha))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txt_numero_factura, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_buscar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jbox_bodega, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton2)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txt_numero_factura, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_buscar)
                    .addComponent(jButton2)
                    .addComponent(jbox_bodega, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(lbl_id_factura))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(lbl_cliente))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(lbl_fecha)
                            .addComponent(lbl_id_cabecera_devolucion, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 197, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jlabel15)
                    .addComponent(lbl_total_factura))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jtab_devoluciones.addTab("Nueva devolucion", jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jtab_devoluciones)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jtab_devoluciones)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txt_FiltroKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_FiltroKeyTyped
        txt_Filtro.addKeyListener(new KeyAdapter() {
            public void keyReleased(final KeyEvent e) {
                String cadena = (txt_Filtro.getText());
                repaint();
                filtro();
            }
        });
        trsFiltro = new TableRowSorter(jtabla_dev_principal.getModel());
        jtabla_dev_principal.setRowSorter(trsFiltro);
    }//GEN-LAST:event_txt_FiltroKeyTyped
    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    public void filtro() {
        int columnaABuscar = 1;
        switch (comboFiltro.getSelectedItem() + "") {
            case "Id factura":
                columnaABuscar = 1;
                break;
            case "Cliente":
                columnaABuscar = 2;
                break;
            case "Fecha":
                columnaABuscar = 5;
                break;
        }
        trsFiltro.setRowFilter(RowFilter.regexFilter(txt_Filtro.getText(), columnaABuscar));
    }
    private void comboFiltroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboFiltroActionPerformed
        txt_Filtro.setText("");
        txt_Filtro.requestFocus();
    }//GEN-LAST:event_comboFiltroActionPerformed

    private void btn_verActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_verActionPerformed
        jd_ver_devolucion frm = new jd_ver_devolucion(null, closable);

        int fila = jtabla_dev_principal.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
        } else {
            String id = "" + jtabla_dev_principal.getValueAt(fila, 0);
            ResultSet rs;
            rs = DB_consultas_R_D.getTabla("select d.id,id_factura,c.nombre as cliente,u.nombre,d.Total,d.Fecha,d.Hora "
                    + "from devoluciones d, users u,contactos c, facturas_cabeceras f "
                    + "where d.id_user=u.id and f.id_contacto=c.id and d.id_factura=f.id and d.id=" + id);
            try {
                while (rs.next()) {
                    jd_ver_devolucion.lbl_cliente.setText(rs.getString("cliente"));
                    jd_ver_devolucion.lbl_fecha.setText(rs.getString("fecha"));
                    jd_ver_devolucion.lbl_hora.setText(rs.getString("hora"));
                    jd_ver_devolucion.lbl_id_devolucion.setText(rs.getString("id"));
                    jd_ver_devolucion.lbl_id_factura.setText(rs.getString("id_factura"));
                    jd_ver_devolucion.lbl_total_devuelto.setText(formatea.format(rs.getDouble("total")));
                    jd_ver_devolucion.lbl_user_devolucion.setText(rs.getString("nombre"));
                }
                rs.close();

            } catch (SQLException ex) {
                System.out.println(ex);
            }

            rs = DB_consultas_R_D.getTabla("select p.codigo_barras, p.descripcion, d.cantidad, d.valor_unitario, d.total "
                    + "from devoluciones_detalles d, productos p where d.id_producto=p.id and d.id_cabecera_devolucion=" + jd_ver_devolucion.lbl_id_devolucion.getText());

            DefaultTableModel modelo_ver_devo = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false; //Con esto conseguimos que la tabla no se pueda editar
                }
            };
            modelo_ver_devo.setColumnIdentifiers(new Object[]{"CODIGO", "DESCRIPCIÓN", "CANTIDAD", "PRECIO", "TOTAL"});
            try {
                while (rs.next()) {
                    modelo_ver_devo.addRow(new Object[]{rs.getString("codigo_barras"), rs.getString("descripcion"),
                        rs.getString("cantidad"), "" + formatea.format(rs.getDouble("valor_unitario")), formatea.format(rs.getDouble("total"))});
                }
                rs.close();
                jd_ver_devolucion.jtabla_ver_devoluciones.setModel(modelo_ver_devo);
            } catch (SQLException ex) {
                System.out.println(ex);
            }

        }
        frm.show();


    }//GEN-LAST:event_btn_verActionPerformed

    private void btn_actualizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_actualizarActionPerformed
        actualizar();
    }//GEN-LAST:event_btn_actualizarActionPerformed
    public void actualizar() {
        try {
            for (int i = 0; i < jtabla_dev_principal.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        ResultSet rs = null;

        rs = DB_consultas_R_D.getTabla("select d.id,id_factura,c.nombre as cliente,u.user_name,d.Total,d.Fecha,d.Hora "
                + "from devoluciones d, users u,contactos c, facturas_cabeceras f "
                + "where d.id_user=u.id and f.id_contacto=c.id and d.id_factura=f.id order by fecha desc");

        modelo.setColumnIdentifiers(new Object[]{"id", "Id factura", "Cliente", "Usuario", "total", "fecha", "hora"});
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                modelo.addRow(new Object[]{rs.getString("id"), rs.getString("id_factura"), rs.getString("cliente"), rs.getString("user_name"), formatea.format(rs.getDouble("total")), rs.getString("fecha"), rs.getString("hora")});
            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla_dev_principal.setModel(modelo);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void btn_eliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_eliminarActionPerformed
        int fila = jtabla_dev_principal.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(null,
                    "¿Desea eliminar esta devolución?\n\n"
                    + "Se revertirá el stock de los productos devueltos.",
                    "Alerta", dialogButton);

            if (dialogResult == JOptionPane.YES_OPTION) {
                try {
                    DefaultTableModel modelo = (DefaultTableModel) jtabla_dev_principal.getModel();
                    String id = (String) modelo.getValueAt(fila, 0);
                    int idDevolucion = Integer.parseInt(id);

                    // ════════════════════════════════════════════════════════════
                    // INTEGRACIÓN STOCK: Reversar stock antes de eliminar
                    // ════════════════════════════════════════════════════════════
                    reversarStockDevolucionEliminada(idDevolucion);

                    // Eliminar de la base de datos
                    DB_consultas_R_D.eliminar("devoluciones", id);

                    // Quitar de la tabla visual
                    modelo.removeRow(fila);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }//GEN-LAST:event_btn_eliminarActionPerformed

// ════════════════════════════════════════════════════════════════════════════
// MÉTODO AUXILIAR: Reversar stock de devolución eliminada
// ════════════════════════════════════════════════════════════════════════════
    private void reversarStockDevolucionEliminada(int idDevolucion) {
        DBstock_productos dbStock = new DBstock_productos();

        // Obtener bodega e id_factura de la devolución
        int idBodega = 1;
        int idFactura = 0;
        String sqlCabecera = "SELECT id_bodega, id_factura FROM devoluciones WHERE id = " + idDevolucion;
        ResultSet rsCabecera = DB_consultas_R_D.getTabla(sqlCabecera);
        try {
            if (rsCabecera.next()) {
                idBodega = rsCabecera.getInt("id_bodega");
                idFactura = rsCabecera.getInt("id_factura");
            }
            rsCabecera.close();
        } catch (SQLException e) {
            System.out.println("Error obteniendo cabecera devolución: " + e);
        }

        // Obtener productos devueltos y reversar cada uno
        String sql = "SELECT id_producto, cantidad FROM devoluciones_detalles WHERE id_cabecera_devolucion = " + idDevolucion;
        ResultSet rs = DB_consultas_R_D.getTabla(sql);
        try {
            while (rs.next()) {
                int idProducto = rs.getInt("id_producto");
                double cantidad = rs.getDouble("cantidad");

                dbStock.anularDevolucion(
                        idProducto,
                        idBodega,
                        frm_main.id_user,
                        cantidad,
                        idDevolucion,
                        "Anulación devolución - Factura #" + idFactura
                );
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error reversando stock devolución: " + e);
        }
    }
    private void btn_crearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_crearActionPerformed
        jtab_devoluciones.setSelectedIndex(1);
    }//GEN-LAST:event_btn_crearActionPerformed

    private void txt_numero_facturaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_numero_facturaKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            consulta_factura();
            try {
                for (int i = 0; i < jtabla_devoluciones.getRowCount(); i++) {
                    modelo_devolver.removeRow(i);
                    i -= 1;
                }
            } catch (Exception e) {
            }

        }
    }//GEN-LAST:event_txt_numero_facturaKeyPressed

    private void btn_buscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_buscarActionPerformed
        if (metodos.estacerrado(frm_main.frm_factura_ventas)) {
//            frm_main.tipo_factura = "Venta'";
            frm_main.frm_factura_ventas = new frm_facturas_ventas();
            escritorio.add(frm_main.frm_factura_ventas);
            frm_main.frm_factura_ventas.lbl_titulo.setText("Devoluciones");
            frm_main.frm_factura_ventas.show();
            frm_facturas_ventas.btn_agregar.setVisible(true);
            frm_facturas_ventas.btn_anular.setEnabled(false);
            frm_facturas_ventas.btn_editar.setEnabled(false);
        } else {
            frm_main.frm_factura_ventas.lbl_titulo.setText("Devoluciones");
            frm_facturas_ventas.btn_agregar.setVisible(true);
            frm_main.tipo_factura = "Venta','Apartado','Credito";
            frm_main.frm_factura_ventas.actualizar("");
            frm_main.frm_factura_ventas.toFront();
        }
    }//GEN-LAST:event_btn_buscarActionPerformed

    private void btn_guardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_guardarActionPerformed
        Devoluciones_Cabeceras dc = new Devoluciones_Cabeceras();
        DBdevoluciones_cabeceras dbDevoluciones_cabecera = new DBdevoluciones_cabeceras();

        if (txt_numero_factura.getText().equals("") || txt_total_devolver.getText().equals("") || jtabla_devoluciones.getRowCount() < 1) {
            JOptionPane.showMessageDialog(this,
                    "Por favor verifique que ha selecionado una factura, \n"
                    + "contiene un valor de reembolso o\n"
                    + "hay productos a devolver");
        } else {
            dc.setId(Integer.parseInt(lbl_id_cabecera_devolucion.getText()));
            dc.setId_factura(Integer.parseInt(lbl_id_factura.getText()));
            dc.setId_user(frm_main.id_user);
            dc.setTotal(Double.parseDouble(metodos.EliminaCaracteres(txt_total_devolver.getText(), ".")));
            dc.setFecha(DB_consultas_R_D.obtener_fecha());
            dc.setHora(DB_consultas_R_D.obtener_hora());

            int id_bodega = 1;
            try {
                id_bodega = jbox_bodega.getItemAt(jbox_bodega.getSelectedIndex()).getId();
            } catch (Exception e) {
                id_bodega = 1;
            }
            dc.setId_bodega(id_bodega);

            if (dbDevoluciones_cabecera.Guardar(dc) == 1) {
                DBdevoluciones_detalles dbfacturadetalle = new DBdevoluciones_detalles();
                Devoluciones_detalles dd = new Devoluciones_detalles();

                // ════════════════════════════════════════════════════════════
                // INTEGRACIÓN STOCK: Preparar para registrar devoluciones
                // ════════════════════════════════════════════════════════════
                DBstock_productos dbStock = new DBstock_productos();
                int idDevolucion = Integer.parseInt(lbl_id_cabecera_devolucion.getText());

                for (int i = 0; i < jtabla_devoluciones.getRowCount(); i++) {
                    dd.setId_cabecera(idDevolucion);

                    String codigoBarras = (String) modelo_devolver.getValueAt(i, 1);
                    int idProducto = DB_consultas_R_D.traer_id_con_cod_barras(codigoBarras);
                    dd.setId_producto(idProducto);

                    double cantidad = 1;
                    try {
                        cantidad = Double.parseDouble(modelo_devolver.getValueAt(i, 3).toString());
                    } catch (Exception e) {
                        cantidad = 1;
                    }
                    dd.setCantidad(cantidad);

                    dd.setValor_unitario(Double.parseDouble(
                            metodos.EliminaCaracteres((String) modelo_devolver.getValueAt(i, 4), ".")));
                    dd.setTotal(Double.parseDouble(
                            metodos.EliminaCaracteres((String) modelo_devolver.getValueAt(i, 5), ".")));

                    dbfacturadetalle.Guardar(dd);

                    // ════════════════════════════════════════════════════════════
                    // INTEGRACIÓN STOCK: Registrar devolución (+cantidad)
                    // ════════════════════════════════════════════════════════════
                    dbStock.devolucion(
                            idProducto,
                            id_bodega,
                            frm_main.id_user,
                            cantidad,
                            idDevolucion,
                            "Devolución - Factura #" + lbl_id_factura.getText()
                    );
                }
            }
            limpiar();
            actualizar();
        }
    }//GEN-LAST:event_btn_guardarActionPerformed

    private void btn_limpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_limpiarActionPerformed
        limpiar();
    }//GEN-LAST:event_btn_limpiarActionPerformed

    private void jtab_devolucionesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jtab_devolucionesMouseClicked
        txt_numero_factura.requestFocus();
    }//GEN-LAST:event_jtab_devolucionesMouseClicked

    private void txt_FiltroFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_FiltroFocusGained
        txt_Filtro.selectAll();
    }//GEN-LAST:event_txt_FiltroFocusGained

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jbox_bodegaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jbox_bodegaKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jbox_bodegaKeyPressed
    public void limpiar() {
        txt_numero_factura.setText("");
        txt_total_devolver.setText("");
        lbl_cliente.setText("");
        lbl_fecha.setText("");
        lbl_id_factura.setText("");
        lbl_total_factura.setText("");

        try {
            for (int i = 0; i < jtabla_devoluciones.getRowCount(); i++) {
                modelo_devolver.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        try {
            for (int i = 0; i < jtabla_factura.getRowCount(); i++) {
                modelo_factura.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        txt_numero_factura.requestFocus();
        lbl_id_cabecera_devolucion.setText(DB_consultas_R_D.cargarId("devoluciones"));
    }

    private void consulta_factura() {
        if (txt_numero_factura.getText().equals("")) {
            JOptionPane.showMessageDialog(this, "Ingrese un codigo de factura");
        } else {
            if (DB_consultas_R_D.consultar_existencia_campo_String("id", txt_numero_factura.getText(), "facturas_cabeceras") == 1) {
                String id_factura = txt_numero_factura.getText();

                // ════════════════════════════════════════════════════════════
                // VALIDACIÓN: Solo facturas tipo "Venta" pueden tener devolución
                // ════════════════════════════════════════════════════════════
                String tipoFactura = obtenerTipoFactura(id_factura);
                if (!tipoFactura.equalsIgnoreCase("Venta")) {
                    JOptionPane.showMessageDialog(this,
                            "Solo se pueden realizar devoluciones a facturas de tipo 'Venta'.\n\n"
                            + "La factura #" + id_factura + " es de tipo: " + tipoFactura,
                            "Tipo de factura no válido",
                            JOptionPane.WARNING_MESSAGE);
                    txt_numero_factura.setText("");
                    txt_numero_factura.requestFocus();
                    return;
                }

                ResultSet rs = DB_consultas_R_D.getTabla(
                        "select f.id, c.nombre, "
                        + "(select sum(cantidad*subtotal) from facturas_detalles where id_cabecera=f.id) as total, fecha "
                        + "from facturas_cabeceras f, contactos c "
                        + "where f.id_contacto=c.id and f.id =" + id_factura);

                try {
                    while (rs.next()) {
                        lbl_id_factura.setText(rs.getString("id"));
                        lbl_cliente.setText(rs.getString("nombre"));
                        lbl_fecha.setText(rs.getString("fecha"));
                        lbl_total_factura.setText(formatea.format(rs.getDouble("total")));
                    }
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(frm_devoluciones.class.getName()).log(Level.SEVERE, null, ex);
                }

                rs = DB_consultas_R_D.getTabla(
                        "select f.id, p.codigo_barras, p.descripcion, f.cantidad, f.subtotal "
                        + "from facturas_detalles f, productos p "
                        + "where f.id_producto=p.id and id_cabecera=" + lbl_id_factura.getText());

                modelo_factura = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int fila, int columna) {
                        return false;
                    }
                };
                modelo_factura.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "PRECIO", "TOTAL"});
                try {
                    while (rs.next()) {
                        modelo_factura.addRow(new Object[]{
                            rs.getString("id"),
                            rs.getString("codigo_barras"),
                            rs.getString("descripcion"),
                            rs.getDouble("cantidad"),
                            "" + formatea.format(rs.getDouble("subtotal")),
                            formatea.format(rs.getInt("cantidad") * rs.getDouble("subtotal"))
                        });
                    }
                    rs.close();
                    jtabla_factura.setModel(modelo_factura);
                } catch (SQLException ex) {
                    Logger.getLogger(frm_facturas_ventas.class.getName()).log(Level.SEVERE, null, ex);
                }

                jtabla_factura.requestFocus();

            } else {
                JOptionPane.showMessageDialog(this,
                        "La factura ingresada no se encuentra en la base de datos\no esta anulada");
                txt_numero_factura.setText("");
            }
        }
    }

// ════════════════════════════════════════════════════════════════════════════
// MÉTODO AUXILIAR: Obtener tipo de factura
// ════════════════════════════════════════════════════════════════════════════
    private String obtenerTipoFactura(String idFactura) {
        String tipo = "";
        String sql = "SELECT tipo_factura FROM facturas_cabeceras WHERE id = " + idFactura;
        ResultSet rs = DB_consultas_R_D.getTabla(sql);
        try {
            if (rs.next()) {
                tipo = rs.getString("tipo_factura");
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error obteniendo tipo de factura: " + e);
        }
        return tipo != null ? tipo : "";
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_actualizar;
    private javax.swing.JButton btn_buscar;
    private javax.swing.JButton btn_crear;
    private javax.swing.JButton btn_eliminar;
    private javax.swing.JButton btn_guardar;
    private javax.swing.JButton btn_limpiar;
    private javax.swing.JButton btn_ver;
    private javax.swing.JComboBox comboFiltro;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    public static javax.swing.JComboBox<Bodegas> jbox_bodega;
    private javax.swing.JLabel jlabel15;
    private javax.swing.JLabel jlabel16;
    private javax.swing.JTabbedPane jtab_devoluciones;
    private javax.swing.JTable jtabla_dev_principal;
    private javax.swing.JTable jtabla_devoluciones;
    public static javax.swing.JTable jtabla_factura;
    private javax.swing.JPanel lbl_cant_clientes;
    public static javax.swing.JLabel lbl_cliente;
    public static javax.swing.JLabel lbl_fecha;
    private javax.swing.JLabel lbl_id_cabecera_devolucion;
    public static javax.swing.JLabel lbl_id_factura;
    public static javax.swing.JLabel lbl_total_factura;
    private javax.swing.JTextField txt_Filtro;
    public static javax.swing.JTextField txt_numero_factura;
    private javax.swing.JTextField txt_total_devolver;
    // End of variables declaration//GEN-END:variables

}
