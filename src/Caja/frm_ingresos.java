/*
 * Modulo Caja: registro y consulta de ingresos de dinero.
 * Portado de cajadiaria con rediseno: sin abonos_ingresos (id_fondo directo),
 * ids serial (INSERT ... RETURNING id) y sin comparativo World Office.
 */
package Caja;

import Formularios.frm_main;
import Metodos.ExportarExcel;
import Metodos.TextPrompt;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBIngresos;
import conexiondb.DB_Fotos_servicios;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Contactos;
import modelos.Cuentas_Ingresos;
import modelos.Fondos;
import modelos.Fotos_registros;
import modelos.Ingresos;

/**
 *
 * @author Monkeyelgrande
 */
public class frm_ingresos extends javax.swing.JInternalFrame {

    /**
     * Creates new form frm_clientes
     */
    public static DefaultTableModel modelo_fotos = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    DecimalFormat formatea = new DecimalFormat("###,###.##");
    Calendar fecha = new GregorianCalendar();
    static TableColumnModel columnModel = null;

    public frm_ingresos() {
        initComponents();
        Fondos.mostrarFondos(jbox_Fondos);
        modelo_fotos.setColumnIdentifiers(new Object[]{"Ruta", "Nombre", "id_foto"});
        jtabla_fotos.setModel(modelo_fotos);
        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        metodos.EstiloTablaMaterialGlobal(jtabla);
        Contactos empleado = new Contactos();
        empleado.MostrarNombreContactos(jbox_contacto);

        jbox_contacto.setSelectedItem(traerContactoPredeterminadoNombre());
        if (frm_main.ingreso_dinero == 0) {
            chk_dinero_recibido.setVisible(false);
            btn_actualizar_pago.setVisible(false);
            btn_ver_pendiente.setVisible(false);
        }
        jtxa_descripcion_crear.setWrapStyleWord(true);
        try {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

        actualizar();
        calcular_total();

        actualizarResumen();
        lbl_id.setText("Nuevo"); // id serial: se conoce al guardar

        Cuentas_Ingresos.mostrarCuentas(jbox_Cuentas);
        jbox_Cuentas.setSelectedItem(Cuentas_Ingresos.TraerPredeterminadoNombre());

        jdate_fecha_entrada.setCalendar(fecha);
        metodos.EvitarTabEnJTextArea(jtxa_descripcion_crear);
        TextPrompt prompt = new TextPrompt("Valor ingreso", txt_total);

        columnModel = jtabla.getColumnModel();
        TamanosTablaAbonos();
        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    try {
                        verIngreso();
                    } catch (SQLException ex) {
                        Logger.getLogger(frm_ingresos.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        jtabla_fotos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 1) {
                    int fila = jtabla_fotos.getSelectedRow();

                    String ruta = (String) jtabla_fotos.getValueAt(fila, 0);
                    jd_ver_in_egre.foto_a_label(ruta, lbl_foto_1);

                    ruta_foto = ruta;
                }
            }
        });

        jtabla_fotos.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_DELETE) {

                    quitar_fila(jtabla_fotos, modelo_fotos);

                }
            }
        });

        if (frm_main.perfil != 1) {
            btn_eliminar.setEnabled(false);
            btn_editar.setEnabled(false);
        }
    }

    // La Contactos de bodega no trae TraerPredeterminadoNombre: consulta local
    // sobre la columna predeterminado agregada por el modulo Caja.
    private String traerContactoPredeterminadoNombre() {
        String name = "";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select nombre from contactos where predeterminado=1");
            while (rs.next()) {
                name = rs.getString("nombre");
            }
            rs.close();
        } catch (Exception e) {
        }
        return name;
    }

    public void actualizarResumen() {
        DefaultTableModel modelo_resumen = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        modelo_resumen.setColumnIdentifiers(new Object[]{"Fondo", "Tipo", "Total"});

        String consulta = "SELECT coalesce(f.nombre, 'Sin fondo') as fondo, "
                + "CASE WHEN i.factura_remision = 1 THEN 'F' ELSE 'R' END as tipo, "
                + "SUM(i.total) as total "
                + "FROM ingresos i "
                + "LEFT JOIN fondos f ON i.id_fondo = f.id "
                + "WHERE i.fecha = CURRENT_DATE "
                + "GROUP BY coalesce(f.nombre, 'Sin fondo'), i.factura_remision "
                + "ORDER BY fondo, tipo";

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);
            while (rs.next()) {
                modelo_resumen.addRow(new Object[]{
                    rs.getString("fondo"),
                    rs.getString("tipo"),
                    formatea.format(rs.getDouble("total"))
                });
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("Error resumen: " + e);
        }

        jtabla_resumen.setModel(modelo_resumen);
    }

    public void quitar_fila(JTable tabla, DefaultTableModel modelo) {
        if (modelo.getRowCount() > 0) {

            int fila = tabla.getSelectedRow();
            if (tabla.getSelectedRowCount() < 1) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro");
            } else {
                modelo.removeRow(fila);

            }
        }
    }

    public String fecha_hoy = DB_consultas_R_D.obtener_fecha();

    public void calcular_total() {
        double total_general = 0;
        double total_F = 0;
        double total_R = 0;

        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select coalesce((select sum(total) as total from ingresos where fecha='" + fecha_hoy + "'),0) as total,\n"
                    + "coalesce((select sum(total) as total from ingresos where fecha='" + fecha_hoy + "' and factura_remision=1),0) as total_f,\n"
                    + "coalesce((select sum(total) as total from ingresos where fecha='" + fecha_hoy + "' and factura_remision=0),0) as total_r");
            while (rs.next()) {
                total_general = rs.getDouble("total");
                total_F = rs.getDouble("total_f");
                total_R = rs.getDouble("total_r");

            }
            rs.close();
        } catch (Exception e) {
        }

        lbl_total_suma.setText(metodos.formateador_dinero().format(total_general));
        lbl_total_F.setText(metodos.formateador_dinero().format(total_F));
        lbl_total_R.setText(metodos.formateador_dinero().format(total_R));
    }

    public static void TamanosTablaAbonos() {
        columnModel.getColumn(0).setPreferredWidth(20);
        columnModel.getColumn(1).setPreferredWidth(80);
        columnModel.getColumn(2).setPreferredWidth(250);
        columnModel.getColumn(3).setPreferredWidth(100);
        columnModel.getColumn(4).setPreferredWidth(100);
        columnModel.getColumn(5).setPreferredWidth(100);
        columnModel.getColumn(6).setPreferredWidth(100);

        if (frm_main.ingreso_dinero == 1) {
            columnModel.getColumn(7).setPreferredWidth(100);
            columnModel.getColumn(8).setPreferredWidth(20);
        }

    }

    public void verIngreso() throws SQLException {
        int fila = jtabla.getSelectedRow();
        jd_ver_in_egre frm = new jd_ver_in_egre();
        String id = "" + jtabla.getValueAt(fila, 0);
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
        } else {
            Ingresos ingreso = new Ingresos();
            ingreso = Ingresos.traer_ingreso(id);

            jd_ver_in_egre.lbl_user.setText(ingreso.getNombre_user());
            jd_ver_in_egre.lbl_total.setText("$ " + formatea.format(ingreso.getTotal()));
            jd_ver_in_egre.lbl_fecha.setText(ingreso.getFecha());
            jd_ver_in_egre.lbl_hora.setText(ingreso.getHora());
            jd_ver_in_egre.jtxa_descripcion.setText(ingreso.getDescripcion());
            jd_ver_in_egre.lbl_cuenta_nombre.setText(ingreso.getNombre_cuenta());

        }
        jd_ver_in_egre.lbl_cuenta_nombre.setVisible(true);
        jd_ver_in_egre.lbl_cuenta_titulo.setVisible(true);

        // MODELOS DE FOTOS
        try {
            for (int i = 0; i < jd_ver_in_egre.modelo_fotos.getRowCount(); i++) {
                jd_ver_in_egre.modelo_fotos.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
            System.out.println(m);
        }

        String consulta = "select * from fotos_registros where tipo_registro=1 and id_registro=" + id;
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        jd_ver_in_egre.modelo_fotos.setColumnIdentifiers(new Object[]{"Ruta", "Nombre", "id_foto"});
        while (rs.next()) {

            jd_ver_in_egre.modelo_fotos.addRow(new Object[]{DB_consultas_R_D.Ruta_Imagenes() + rs.getString("nombre"), rs.getString("nombre"), rs.getString("id")});
            jd_ver_in_egre.jtabla_fotos.setModel(jd_ver_in_egre.modelo_fotos);

            jd_ver_in_egre.foto_a_label(DB_consultas_R_D.Ruta_Imagenes() + rs.getString("nombre"), jd_ver_in_egre.lbl_foto_1);

        }
        rs.close();

        frm.show();
    }
    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        btn_limpiar = new javax.swing.JButton();
        btn_guardar = new javax.swing.JButton();
        txt_total = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtxa_descripcion_crear = new javax.swing.JTextArea();
        jdate_fecha_entrada = new com.toedter.calendar.JDateChooser();
        jbox_Cuentas = new javax.swing.JComboBox<>();
        jLabel20 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        lbl_id = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        jLabel26 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        btn_crear_cliente = new javax.swing.JButton();
        jbox_Fondos = new javax.swing.JComboBox<>();
        chk_dinero_recibido = new javax.swing.JCheckBox();
        jbox_contacto = new javax.swing.JComboBox<>();
        rbtn_F = new javax.swing.JRadioButton();
        rbtn_R = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane4 = new javax.swing.JScrollPane();
        jtabla_fotos = new javax.swing.JTable();
        lbl_foto_1 = new javax.swing.JLabel();
        btn_cargar_foto = new javax.swing.JButton();
        btn_eliminar_foto = new javax.swing.JButton();
        btn_cargar_foto1 = new javax.swing.JButton();
        jLabel27 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        lbl_cant_clientes = new javax.swing.JPanel();
        lbl_total_suma = new javax.swing.JLabel();
        lbl_text = new javax.swing.JLabel();
        txt_Filtro = new javax.swing.JTextField();
        btn_total = new javax.swing.JButton();
        lbl_text1 = new javax.swing.JLabel();
        lbl_total_F = new javax.swing.JLabel();
        lbl_text2 = new javax.swing.JLabel();
        lbl_total_R = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        btn_crear = new javax.swing.JButton();
        btn_eliminar = new javax.swing.JButton();
        btn_editar = new javax.swing.JButton();
        btn_actualizar = new javax.swing.JButton();
        btn_imprimir = new javax.swing.JButton();
        btn_comparar = new javax.swing.JButton();
        btn_imprimir1 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        btn_actualizar_pago = new javax.swing.JButton();
        btn_ver_pendiente = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla_resumen = new javax.swing.JTable();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Ingresos");

        jPanel1.setBackground(new java.awt.Color(153, 255, 153));

        btn_limpiar.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_limpiar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Broom_1.png"))); // NOI18N
        btn_limpiar.setMnemonic('l');
        btn_limpiar.setText("Limpiar");
        btn_limpiar.setBorder(null);
        btn_limpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_limpiarActionPerformed(evt);
            }
        });

        btn_guardar.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_guardar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Save_1.png"))); // NOI18N
        btn_guardar.setMnemonic('g');
        btn_guardar.setText("Guardar");
        btn_guardar.setBorder(null);
        btn_guardar.setNextFocusableComponent(jbox_Cuentas);
        btn_guardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_guardarActionPerformed(evt);
            }
        });
        btn_guardar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btn_guardarKeyPressed(evt);
            }
        });

        txt_total.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        txt_total.setNextFocusableComponent(btn_guardar);
        txt_total.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_totalFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_totalFocusLost(evt);
            }
        });
        txt_total.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_totalKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_totalKeyTyped(evt);
            }
        });

        jtxa_descripcion_crear.setColumns(20);
        jtxa_descripcion_crear.setLineWrap(true);
        jtxa_descripcion_crear.setRows(5);
        jtxa_descripcion_crear.setNextFocusableComponent(txt_total);
        jScrollPane3.setViewportView(jtxa_descripcion_crear);

        jdate_fecha_entrada.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N

        jbox_Cuentas.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jbox_Cuentas.setForeground(new java.awt.Color(51, 51, 51));

        jLabel20.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel20.setText("Cuenta");

        jLabel23.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel23.setText("Fecha ingreso");

        jLabel25.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel25.setText("Descripción");

        jLabel24.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel24.setText("Crear ingreso");

        lbl_id.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lbl_id.setText("lbl_id");

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

        jLabel26.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel26.setText("Total");

        jLabel3.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel3.setText("Contacto");

        btn_crear_cliente.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        btn_crear_cliente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/mas.png"))); // NOI18N
        btn_crear_cliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_crear_clienteActionPerformed(evt);
            }
        });

        jbox_Fondos.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jbox_Fondos.setForeground(new java.awt.Color(51, 51, 51));

        chk_dinero_recibido.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        chk_dinero_recibido.setSelected(true);
        chk_dinero_recibido.setText("Dinero Recibido");

        jbox_contacto.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jbox_contactoFocusLost(evt);
            }
        });
        jbox_contacto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbox_contactoActionPerformed(evt);
            }
        });

        buttonGroup1.add(rbtn_F);
        rbtn_F.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        rbtn_F.setSelected(true);
        rbtn_F.setText("F");

        buttonGroup1.add(rbtn_R);
        rbtn_R.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        rbtn_R.setText("R");

        jtabla_fotos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_fotos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jtabla_fotos.getTableHeader().setReorderingAllowed(false);
        jScrollPane4.setViewportView(jtabla_fotos);

        lbl_foto_1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_foto_1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        lbl_foto_1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lbl_foto_1MouseClicked(evt);
            }
        });

        btn_cargar_foto.setFont(new java.awt.Font("Yu Gothic Medium", 1, 12)); // NOI18N
        btn_cargar_foto.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Photo Gallery_1.png"))); // NOI18N
        btn_cargar_foto.setText("Cargar Foto");
        btn_cargar_foto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_cargar_fotoActionPerformed(evt);
            }
        });

        btn_eliminar_foto.setFont(new java.awt.Font("Yu Gothic Medium", 1, 12)); // NOI18N
        btn_eliminar_foto.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Remove_2.png"))); // NOI18N
        btn_eliminar_foto.setText("Eliminar Foto");
        btn_eliminar_foto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_eliminar_fotoActionPerformed(evt);
            }
        });

        btn_cargar_foto1.setFont(new java.awt.Font("Yu Gothic Medium", 1, 12)); // NOI18N
        btn_cargar_foto1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Camera_3.png"))); // NOI18N
        btn_cargar_foto1.setText("Capturar Foto");
        btn_cargar_foto1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_cargar_foto1ActionPerformed(evt);
            }
        });

        jLabel27.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jLabel27.setText("Fondo");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_foto_1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btn_cargar_foto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_eliminar_foto)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_cargar_foto1))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel25)
                            .addComponent(jLabel26)
                            .addComponent(jLabel23)
                            .addComponent(jLabel20)
                            .addComponent(jLabel24)
                            .addComponent(jLabel27))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(220, 220, 220)
                                .addComponent(jButton3)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                        .addComponent(jbox_contacto, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btn_crear_cliente, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addComponent(lbl_id))
                                    .addComponent(jScrollPane3)
                                    .addComponent(jdate_fecha_entrada, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(txt_total)
                                    .addComponent(jbox_Cuentas, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jbox_Fondos, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(rbtn_F)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(rbtn_R))
                                    .addComponent(chk_dinero_recibido)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(btn_guardar, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btn_limpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)))))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24)
                    .addComponent(lbl_id)
                    .addComponent(jButton3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(jbox_contacto))
                    .addComponent(btn_crear_cliente))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(jbox_Cuentas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addComponent(jLabel23))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jdate_fecha_entrada, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel25)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txt_total, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel26))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbox_Fondos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel27))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_limpiar)
                    .addComponent(btn_guardar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chk_dinero_recibido)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rbtn_F)
                    .addComponent(rbtn_R))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_cargar_foto, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_eliminar_foto, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_cargar_foto1, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_foto_1, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        lbl_cant_clientes.setBackground(new java.awt.Color(33, 33, 33));

        lbl_total_suma.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_total_suma.setForeground(new java.awt.Color(255, 255, 255));
        lbl_total_suma.setText("0");

        lbl_text.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_text.setForeground(new java.awt.Color(255, 255, 255));
        lbl_text.setText("$");

        txt_Filtro.setFont(new java.awt.Font("Yu Gothic Medium", 0, 14)); // NOI18N
        txt_Filtro.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txt_FiltroActionPerformed(evt);
            }
        });
        txt_Filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_FiltroKeyTyped(evt);
            }
        });

        btn_total.setBackground(new java.awt.Color(0, 153, 51));
        btn_total.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_total.setForeground(new java.awt.Color(255, 255, 255));
        btn_total.setText("Sumar");
        btn_total.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_totalActionPerformed(evt);
            }
        });

        lbl_text1.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_text1.setForeground(new java.awt.Color(255, 255, 255));
        lbl_text1.setText("F: $");

        lbl_total_F.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_total_F.setForeground(new java.awt.Color(255, 255, 255));
        lbl_total_F.setText("0");

        lbl_text2.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_text2.setForeground(new java.awt.Color(255, 255, 255));
        lbl_text2.setText("R: $");

        lbl_total_R.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        lbl_total_R.setForeground(new java.awt.Color(255, 255, 255));
        lbl_total_R.setText("0");

        jtabla.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jtabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jtabla);

        javax.swing.GroupLayout lbl_cant_clientesLayout = new javax.swing.GroupLayout(lbl_cant_clientes);
        lbl_cant_clientes.setLayout(lbl_cant_clientesLayout);
        lbl_cant_clientesLayout.setHorizontalGroup(
            lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                        .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                                .addComponent(lbl_text1)
                                .addGap(8, 8, 8)
                                .addComponent(lbl_total_F)
                                .addGap(223, 223, 223)
                                .addComponent(lbl_text2)
                                .addGap(8, 8, 8)
                                .addComponent(lbl_total_R))
                            .addComponent(txt_Filtro))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_total)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 291, Short.MAX_VALUE)
                        .addComponent(lbl_text)
                        .addGap(8, 8, 8)
                        .addComponent(lbl_total_suma)))
                .addContainerGap())
        );
        lbl_cant_clientesLayout.setVerticalGroup(
            lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_total)
                    .addComponent(lbl_text)
                    .addComponent(lbl_total_suma))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_text1)
                    .addComponent(lbl_total_F)
                    .addComponent(lbl_text2)
                    .addComponent(lbl_total_R))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1)
                .addContainerGap())
        );

        jPanel3.setBackground(new java.awt.Color(33, 33, 33));

        btn_crear.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_crear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/nuevo.png"))); // NOI18N
        btn_crear.setMnemonic('n');
        btn_crear.setText("Nuevo");
        btn_crear.setBorder(null);
        btn_crear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_crearActionPerformed(evt);
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

        btn_editar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_editar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/editar.png"))); // NOI18N
        btn_editar.setMnemonic('e');
        btn_editar.setText("Editar");
        btn_editar.setBorder(null);
        btn_editar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_editarActionPerformed(evt);
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

        btn_imprimir.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_imprimir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Printer.png"))); // NOI18N
        btn_imprimir.setMnemonic('d');
        btn_imprimir.setText("Imprimir");
        btn_imprimir.setBorder(null);
        btn_imprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimirActionPerformed(evt);
            }
        });

        btn_comparar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_comparar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Compare.png"))); // NOI18N
        btn_comparar.setMnemonic('d');
        btn_comparar.setText("Comparar WO");
        btn_comparar.setBorder(null);
        btn_comparar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_compararActionPerformed(evt);
            }
        });

        btn_imprimir1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_imprimir1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/PNG.png"))); // NOI18N
        btn_imprimir1.setMnemonic('d');
        btn_imprimir1.setText("Exportar PNG");
        btn_imprimir1.setBorder(null);
        btn_imprimir1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimir1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_actualizar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_editar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_eliminar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_crear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_imprimir, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_comparar, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
                    .addComponent(btn_imprimir1, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_crear)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_eliminar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_editar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_actualizar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_imprimir1, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btn_imprimir)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_comparar)
                .addContainerGap())
        );

        jPanel4.setBackground(new java.awt.Color(0, 102, 51));
        jPanel4.setPreferredSize(new java.awt.Dimension(146, 80));

        jLabel13.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("Ingresos");

        btn_actualizar_pago.setBackground(new java.awt.Color(0, 102, 204));
        btn_actualizar_pago.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        btn_actualizar_pago.setForeground(new java.awt.Color(255, 255, 255));
        btn_actualizar_pago.setText("Actualizar Pago");
        btn_actualizar_pago.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_actualizar_pagoActionPerformed(evt);
            }
        });

        btn_ver_pendiente.setBackground(new java.awt.Color(204, 0, 0));
        btn_ver_pendiente.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        btn_ver_pendiente.setForeground(new java.awt.Color(255, 255, 255));
        btn_ver_pendiente.setText("Ver Pendientes");
        btn_ver_pendiente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ver_pendienteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 463, Short.MAX_VALUE)
                .addComponent(btn_ver_pendiente)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_actualizar_pago)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(btn_actualizar_pago)
                    .addComponent(btn_ver_pendiente))
                .addGap(18, 18, 18))
        );

        jButton1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/excel.png"))); // NOI18N
        jButton1.setText("Exportar a excel");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jtabla_resumen.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jScrollPane2.setViewportView(jtabla_resumen);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 948, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_cant_clientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton1))
                    .addComponent(lbl_cant_clientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public void validar_numeros(java.awt.event.KeyEvent evt, char car) {
        if ((car < '0' || car > '9')) {
            evt.consume();
        }
    }
    int id_cuenta = 0;
    int id_fondo = 0;
    private void btn_limpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_limpiarActionPerformed
        limpiar();
    }//GEN-LAST:event_btn_limpiarActionPerformed

    private void btn_guardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_guardarActionPerformed
        if (txt_total.getText().isEmpty()) {

            txt_total.setBackground(Color.pink);
        } else {
            DBIngresos db_ingreso = new DBIngresos();

            Ingresos obj = new Ingresos();

            obj.setDescripcion(jtxa_descripcion_crear.getText());
            int dia, mes, ano;
            ano = jdate_fecha_entrada.getCalendar().get(Calendar.YEAR);
            mes = jdate_fecha_entrada.getCalendar().get(Calendar.MARCH) + 1;
            dia = jdate_fecha_entrada.getCalendar().get(Calendar.DAY_OF_MONTH);
            obj.setFecha(ano + "-" + mes + "-" + dia); // sin comillas: va por parametro

            obj.setHora(DB_consultas_R_D.obtener_hora());

            try {
                obj.setId_cuenta(jbox_Cuentas.getItemAt(jbox_Cuentas.getSelectedIndex()).getId());
            } catch (Exception e) {
                obj.setId_cuenta(id_cuenta);
            }
            // fondo
            try {
                obj.setId_fondo(jbox_Fondos.getItemAt(jbox_Fondos.getSelectedIndex()).getId());
            } catch (Exception e) {
                obj.setId_fondo(Fondos.TraerPredeterminado());
            }
            // cliente
            try {
                obj.setId_cliente(jbox_contacto.getItemAt(jbox_contacto.getSelectedIndex()).getId());
            } catch (Exception e) {
                obj.setId_cliente(1);
            }

            obj.setId_user(frm_main.id_user);
            obj.setTotal(Double.parseDouble(metodos.EliminaCaracteres(txt_total.getText(), ".")));

            if (rbtn_F.isSelected()) {
                obj.setFactura_remision(1);
            } else {
                obj.setFactura_remision(0);
            }

            if (edita) {
                // Edicion: el id de la fila viene de btn_editar
                obj.setId(Integer.parseInt(lbl_id.getText()));
                db_ingreso.Actualizar(obj, chk_dinero_recibido.isSelected());
                edita = false;
            } else {
                // Nuevo: el id serial lo devuelve el INSERT (RETURNING) y queda en obj
                if (db_ingreso.Guardar(obj, chk_dinero_recibido.isSelected()) > 0) {
// FOTOS
                    if (modelo_fotos.getRowCount() > 0) {
                        for (int i = 0; i < modelo_fotos.getRowCount(); i++) {

                            if (jtabla_fotos.getValueAt(i, 2).toString().equals("0")) {

                                metodos.copyFile_Java7(jtabla_fotos.getValueAt(i, 0).toString(), DB_consultas_R_D.Ruta_Imagenes() + jtabla_fotos.getValueAt(i, 1).toString());

                                Fotos_registros foto = new Fotos_registros(jtabla_fotos.getValueAt(i, 1).toString(), obj.getId(), 0, 1);

                                DB_Fotos_servicios db = new DB_Fotos_servicios();
                                db.Guardar(foto);
                            } else {

                            }
                        }

                    }
                }

            }
            limpiar();
            act = 0;
            actualizar();
            actualizarResumen();

            txt_total.requestFocus();
        }
    }//GEN-LAST:event_btn_guardarActionPerformed
    public void limpiar() {
        jtxa_descripcion_crear.setText("");
        txt_total.setText("");
//        jdate_fecha_entrada.setCalendar(fecha);
        txt_total.setBackground(Color.white);
        jtxa_descripcion_crear.setBackground(Color.white);
        lbl_id.setText("Nuevo"); // id serial: se asigna al guardar
        edita = false;
        jbox_Fondos.setSelectedItem(Fondos.TraerPredeterminadoNombre());
        jbox_Cuentas.setSelectedItem(Cuentas_Ingresos.TraerPredeterminadoNombre());
        jbox_contacto.setSelectedItem(traerContactoPredeterminadoNombre());
        rbtn_F.setSelected(true);
        id_cuenta = Cuentas_Ingresos.TraerPredeterminadoID();
        lbl_foto_1.setIcon(null);
        nombreArchivoImagen = "";
        ruta_origen_imagen = "";
        ruta_foto = "";

        try {
            for (int i = 0; i < modelo_fotos.getRowCount(); i++) {
                modelo_fotos.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

        if (frm_main.ingreso_dinero == 1) {
            chk_dinero_recibido.setSelected(true);
        }
    }


    private void txt_totalFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_totalFocusLost
        if (!txt_total.getText().equals("")) {
            double to = Double.parseDouble(txt_total.getText());
            String nuevo = formatea.format(to);
            txt_total.setText(nuevo);
        }
    }//GEN-LAST:event_txt_totalFocusLost

    private void txt_totalFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_totalFocusGained
        if (!txt_total.getText().equals("")) {
            String texto = metodos.EliminaCaracteres(txt_total.getText(), ".");
            txt_total.setText(texto);
        }
    }//GEN-LAST:event_txt_totalFocusGained

    private void txt_totalKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_totalKeyTyped
        char num = evt.getKeyChar();
        DB_consultas_R_D.validar_numeros(evt, num);
    }//GEN-LAST:event_txt_totalKeyTyped

    private void txt_totalKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_totalKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            btn_guardar.requestFocus();
        }
    }//GEN-LAST:event_txt_totalKeyPressed

    private void btn_guardarKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_btn_guardarKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            btn_guardarActionPerformed(null);
        }
    }//GEN-LAST:event_btn_guardarKeyPressed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void btn_actualizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_actualizarActionPerformed
        actualizar();
    }//GEN-LAST:event_btn_actualizarActionPerformed
    public boolean edita = false;
    private void btn_editarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_editarActionPerformed
        int fila = jtabla.getSelectedRow();

        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
        } else {
            String id = "" + jtabla.getValueAt(fila, 0);
            // Rediseno: el fondo se lee directo de ingresos.id_fondo (sin abonos)
            String consulta = "select i.id, i.descripcion, i.total, i.fecha, i.factura_remision, "
                    + "u.nombre as user, cu.nombre as cuenta, cu.id as id_cuenta, "
                    + "coalesce(f.nombre,'default') as fondo, coalesce(f.id,0) as id_fondo, co.nombre as cliente \n"
                    + "from ingresos i \n"
                    + "left join fondos f on i.id_fondo=f.id, users u, cuentas_ingresos cu, contactos co\n"
                    + "where i.id_cuenta=cu.id and i.id_user=u.id and i.id_cliente=co.id and i.id=" + id;
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);
            try {
                while (rs.next()) {
                    lbl_id.setText(rs.getString("id"));
                    jtxa_descripcion_crear.setText(rs.getString("descripcion"));
                    txt_total.setText(formatea.format(rs.getDouble("total")));
                    jbox_Cuentas.setSelectedItem(rs.getString("cuenta"));
                    jbox_contacto.setSelectedItem(rs.getString("cliente"));
                    id_cuenta = rs.getInt("id_cuenta");
                    id_fondo = rs.getInt("id_fondo");
                    jdate_fecha_entrada.setDate(rs.getDate("fecha"));

                    if (rs.getInt("factura_remision") == 1) {
                        rbtn_F.setSelected(true);
                    } else {
                        rbtn_R.setSelected(true);
                    }

                    if (rs.getInt("id_fondo") == 0) {
                        chk_dinero_recibido.setSelected(false);
                    } else {
                        chk_dinero_recibido.setSelected(true);
                    }
                    if (rs.getString("fondo").equals("default")) {
                        jbox_Fondos.setSelectedItem(Fondos.TraerPredeterminadoNombre());
                    } else {
                        jbox_Fondos.setSelectedItem(rs.getString("fondo"));

                    }
                    edita = true;
                }
                rs.close();
            } catch (SQLException ex) {
                System.out.println(ex);
            }

            txt_total.requestFocus();
        }
    }//GEN-LAST:event_btn_editarActionPerformed

    private void btn_eliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_eliminarActionPerformed
        int fila = jtabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar este ingreso?", "Alerta", dialogButton);
            if (dialogResult == JOptionPane.YES_OPTION) {

                try {
                    DefaultTableModel modelo = (DefaultTableModel) jtabla.getModel();
                    String id = (String) jtabla.getValueAt(fila, 0);//suponiendo que el id lo muestras en la primera columna
                    DB_consultas_R_D.eliminar("ingresos", id);
                    for (int i = 0; i < modelo.getRowCount(); i++) {
                        if (modelo.getValueAt(i, 0).equals(id)) {
                            modelo.removeRow(i);
                            btn_totalActionPerformed(null);
                            limpiar();
                            actualizarResumen();

                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }//GEN-LAST:event_btn_eliminarActionPerformed

    private void btn_crearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_crearActionPerformed
        limpiar();
        jbox_Cuentas.requestFocus();
    }//GEN-LAST:event_btn_crearActionPerformed

    private void btn_totalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_totalActionPerformed
        double total = 0;
        for (int i = 0; i < this.jtabla.getRowCount(); i++) {
            total += Double.parseDouble(metodos.EliminaCaracteres(this.jtabla.getValueAt(i, 5).toString(), "."));
        }
        lbl_total_suma.setText(formatea.format(total) + "");
        actualizarResumen();

    }//GEN-LAST:event_btn_totalActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        ExportarExcel obj;

        try {
            obj = new ExportarExcel();
            obj.exportarExcel(jtabla);
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void btn_crear_clienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_crear_clienteActionPerformed
        jif_crear_contactos frm = new jif_crear_contactos();
        jif_crear_contactos.formulario = "ingreso";
        jif_crear_contactos.txt_nombre.requestFocus();
        frm.show();
    }//GEN-LAST:event_btn_crear_clienteActionPerformed

    private void btn_actualizar_pagoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_actualizar_pagoActionPerformed
        int fila = jtabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            String estado = jtabla.getValueAt(fila, 6).toString();

            String id = jtabla.getValueAt(fila, 0).toString();
            String abono = jtabla.getValueAt(fila, 5).toString();
            if (estado.equals("Pendiente")) {

                jd_actualizar_pago jd = new jd_actualizar_pago(null, false);
                jd.id_cabecera = id;

                int dia, mes, ano;
                ano = jdate_fecha_entrada.getCalendar().get(Calendar.YEAR);
                mes = jdate_fecha_entrada.getCalendar().get(Calendar.MARCH) + 1;
                dia = jdate_fecha_entrada.getCalendar().get(Calendar.DAY_OF_MONTH);
                jd.fecha = (ano + "-" + mes + "-" + dia);
                jd.abono = abono;
                jd.formulario = "ingreso";

                jd.show();
            } else {
                JOptionPane.showMessageDialog(this, "No se puede actualiar un pago que ya se ha realizado con anterioridad");
            }
        }
    }//GEN-LAST:event_btn_actualizar_pagoActionPerformed

    private void jbox_contactoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jbox_contactoFocusLost
    }//GEN-LAST:event_jbox_contactoFocusLost

    private void jbox_contactoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbox_contactoActionPerformed

    }//GEN-LAST:event_jbox_contactoActionPerformed

    private void btn_imprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imprimirActionPerformed

        int fila = jtabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(null, "¿Desea imrpimir este ingreso?", "Alerta", dialogButton);
            if (dialogResult == JOptionPane.YES_OPTION) {
                String id = (String) jtabla.getValueAt(fila, 0);//suponiendo que el id lo muestras en la primera columna

                String consulta = "select i.id, i.fecha, i.hora, i.descripcion, i.total, co.nombre as contacto, cu.nombre as cuenta\n"
                        + "from ingresos i inner join contactos co on i.id_cliente=co.id inner join cuentas_ingresos cu on i.id_cuenta=cu.id\n"
                        + "where i.id = " + id;
                ResultSet rs = DB_consultas_R_D.getTabla(consulta);

                ImprimirTermica80MM imprimir = null;
                try {
                    while (rs.next()) {
                        imprimir = new ImprimirTermica80MM(rs.getString("fecha") + " / " + rs.getString("hora"), rs.getString("id"), rs.getString("contacto"), metodos.formateador_dinero().format(rs.getDouble("total")), "", rs.getString("cuenta"), rs.getString("descripcion"));
                    }
                    rs.close();

                } catch (SQLException ex) {
                    System.out.println("");
                }
                try {
                    imprimir.imprime();
                } catch (PrinterException ex) {
                    System.out.println(ex);
                }
            }
        }

    }//GEN-LAST:event_btn_imprimirActionPerformed

    private void btn_ver_pendienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ver_pendienteActionPerformed
        try {
            for (int i = 0; i < jtabla.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        String consulta = "";

        // Rediseno: pendiente = id_fondo IS NULL (sin abonos)
        consulta = "select i.id, c.nombre, i.total, i.descripcion, i.fecha, i.hora, 0 as abono, 'Pendiente' as fecha_pago\n"
                + "from ingresos i, cuentas_ingresos c \n"
                + "where i.id_cuenta=c.id and i.id_fondo is null \n"
                + "order by i.fecha desc, i.id desc";

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        modelo.setColumnIdentifiers(new Object[]{"id", "Cuenta", "Descripcion", "Fecha Creado", "Hora", "Total", "Estado", "Fecha Pagado"});

        try {
            while (rs.next()) {
                String estado = "Cobrado";
                if (rs.getDouble("abono") == 0) {
                    estado = "Pendiente";
                }
                if (frm_main.ingreso_dinero == 0) {

                    modelo.addRow(new Object[]{rs.getString("id"), rs.getString("nombre"),
                        rs.getString("descripcion"), rs.getDate("fecha"), rs.getString("hora"), formatea.format(rs.getDouble("total"))});
                } else {

                    modelo.addRow(new Object[]{rs.getString("id"), rs.getString("nombre"),
                        rs.getString("descripcion"), rs.getDate("fecha"), rs.getString("hora"), formatea.format(rs.getDouble("total")), estado, rs.getString("fecha_pago")});
                }

            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla.setModel(modelo);
            TamanosTablaAbonos();
        } catch (Exception e) {
            System.out.println(e);
        }
    }//GEN-LAST:event_btn_ver_pendienteActionPerformed

    private void lbl_foto_1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_foto_1MouseClicked
        try {
            DB_consultas_R_D.Abrir_Archivo(ruta_foto);
        } catch (Exception e) {
        }
    }//GEN-LAST:event_lbl_foto_1MouseClicked
    String ruta_foto = "";
    String ruta_origen_imagen = "";
    public static String nombreArchivoImagen = "";


    private void btn_cargar_fotoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_cargar_fotoActionPerformed

        ruta_origen_imagen = "";
        nombreArchivoImagen = "";

        lbl_foto_1.setIcon(null);

        JFileChooser j = new JFileChooser();
        j.setFileSelectionMode(JFileChooser.FILES_ONLY);//solo archivos y no carpetas
        FileNameExtensionFilter filtroImagen = new FileNameExtensionFilter("JPG, PNG & GIF", "jpg", "png", "gif");
        j.setFileFilter(filtroImagen);

        int estado = j.showOpenDialog(null);
        if (estado == JFileChooser.APPROVE_OPTION) {
            File fichero = j.getSelectedFile();
            ruta_origen_imagen = fichero.getAbsolutePath();
            // El id serial aun no existe al elegir la foto: se nombra con fecha/hora
            nombreArchivoImagen = "ing_" + DB_consultas_R_D.obtener_fecha() + DB_consultas_R_D.obtener_hora_con_guiones() + "_" + fichero.getName();

            jd_ver_in_egre.foto_a_label(ruta_origen_imagen, lbl_foto_1);

            modelo_fotos.addRow(new Object[]{ruta_origen_imagen, nombreArchivoImagen, 0});

            jtabla_fotos.setModel(modelo_fotos);
        }
    }//GEN-LAST:event_btn_cargar_fotoActionPerformed

    private void btn_eliminar_fotoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_eliminar_fotoActionPerformed

        int fila = jtabla_fotos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            try {

                int dialogButton = JOptionPane.YES_NO_OPTION;
                int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar esta imagen de este servicio?\n", "Alerta", dialogButton);
                if (dialogResult == JOptionPane.YES_OPTION) {

                    String id_foto = jtabla_fotos.getValueAt(fila, 2).toString();
                    String ruta_foto = jtabla_fotos.getValueAt(fila, 0).toString();

                    // La tabla del modulo Caja es fotos_registros (el original borraba en fotos_ordenes)
                    DB_consultas_R_D.eliminar("fotos_registros", id_foto);
                    DB_consultas_R_D.Eliminar_Archivo(ruta_foto);

                    for (int i = 0; i < modelo_fotos.getRowCount(); i++) {
                        if (modelo_fotos.getValueAt(i, 2).toString().equals(id_foto)) {
                            modelo_fotos.removeRow(i);
                            break;
                        }
                    }
                    jtabla_fotos.setModel(modelo_fotos);
                    lbl_foto_1.setIcon(null);
                    nombreArchivoImagen = "";
                    ruta_origen_imagen = "";

                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "No se pudo realizar la eliminacion");
            }
        }
    }//GEN-LAST:event_btn_eliminar_fotoActionPerformed

    private void btn_cargar_foto1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_cargar_foto1ActionPerformed
        CapturaMejorada captura = new CapturaMejorada();
        String idUsuario = lbl_id.getText();

        // Aquí está la clave: pasamos 'this' como el componente padre.
        // 'this' se refiere a la instancia actual de frm_ingresos.
        captura.lanzar_camara(idUsuario, modelo_fotos, this);
    }//GEN-LAST:event_btn_cargar_foto1ActionPerformed

    private void txt_FiltroKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_FiltroKeyTyped

    }//GEN-LAST:event_txt_FiltroKeyTyped

    private void txt_FiltroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txt_FiltroActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_FiltroActionPerformed

    private void btn_compararActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_compararActionPerformed
        jd_comparar_ingresos frm = new jd_comparar_ingresos(null, false);
        frm.setVisible(true);
    }//GEN-LAST:event_btn_compararActionPerformed

    private void btn_imprimir1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imprimir1ActionPerformed
        int fila = jtabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(null, "¿Desea imrpimir este ingreso?", "Alerta", dialogButton);
            if (dialogResult == JOptionPane.YES_OPTION) {
                String id = jtabla.getValueAt(fila, 0).toString();//suponiendo que el id lo muestras en la primera columna

                String consulta = "select i.id, i.fecha, i.hora, i.descripcion, i.total, co.nombre as contacto, cu.nombre as cuenta\n"
                        + "from ingresos i inner join contactos co on i.id_cliente=co.id inner join cuentas_ingresos cu on i.id_cuenta=cu.id\n"
                        + "where i.id = " + id;
                ResultSet rs = DB_consultas_R_D.getTabla(consulta);

                ImprimirPNG_caja_diaria imprimir = null;
                try {
                    while (rs.next()) {
                        imprimir = new ImprimirPNG_caja_diaria(rs.getString("fecha") + " / " + rs.getString("hora"), rs.getString("id"),
                                rs.getString("contacto"), metodos.formateador_dinero().format(rs.getDouble("total")), jtabla.getValueAt(fila, 6).toString(), rs.getString("cuenta"), rs.getString("descripcion"), "INGRESO");
                    }
                    rs.close();

                } catch (SQLException ex) {
                    System.out.println("");
                }
                imprimir.generarYGuardarImagen();
            }
        }
    }//GEN-LAST:event_btn_imprimir1ActionPerformed

    int act = 0;

    public void actualizar() {
        try {
            for (int i = 0; i < jtabla.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        String consulta = "";

        // Rediseno: abono/fecha_pago/fondo se derivan de ingresos.id_fondo (sin abonos)
        if (act == 0) {
            consulta = "select i.id, i.factura_remision, c.nombre, i.total, i.descripcion, i.fecha, i.hora, "
                    + "case when i.id_fondo is not null then i.total else 0 end as abono, "
                    + "case when i.id_fondo is not null then i.fecha || '' else 'Pendiente' end as fecha_pago, "
                    + "f.nombre as fondo\n"
                    + "from ingresos i \n"
                    + "left join cuentas_ingresos c on i.id_cuenta=c.id \n"
                    + "left join fondos f on i.id_fondo=f.id \n"
                    + "where i.fecha=CURRENT_DATE \n"
                    + "order by i.fecha desc, i.id desc";
            act = 1;

        } else {
            consulta = "select i.id, i.factura_remision, c.nombre, i.total, i.descripcion, i.fecha, i.hora, "
                    + "case when i.id_fondo is not null then i.total else 0 end as abono, "
                    + "case when i.id_fondo is not null then i.fecha || '' else 'Pendiente' end as fecha_pago, "
                    + "f.nombre as fondo\n"
                    + "from ingresos i \n"
                    + "left join cuentas_ingresos c on i.id_cuenta=c.id \n"
                    + "left join fondos f on i.id_fondo=f.id\n"
                    + "order by i.fecha desc, i.id desc";
            act = 0;

        }
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        if (frm_main.ingreso_dinero == 0) {

            modelo.setColumnIdentifiers(new Object[]{"id", "Cuenta", "Descripcion", "Fecha", "Hora", "Total", "Fondo", "Tipo"});
        } else {
            modelo.setColumnIdentifiers(new Object[]{"id", "Cuenta", "Descripcion", "Fecha Creado", "Hora", "Total", "Estado", "Fecha Pagado", "Fondo", "Tipo"});

        }
        try {
            while (rs.next()) {
                String estado = "Cobrado";
                if (rs.getDouble("abono") == 0) {
                    estado = "Pendiente";
                }

                String tipo = "";
                if (rs.getInt("factura_remision") == 1) {
                    tipo = "F";
                } else {
                    tipo = "R";
                }

                if (frm_main.ingreso_dinero == 0) {

                    modelo.addRow(new Object[]{rs.getString("id"), rs.getString("nombre"),
                        rs.getString("descripcion"), rs.getDate("fecha"), rs.getString("hora"), formatea.format(rs.getDouble("total")), rs.getString("fondo"), tipo});
                } else {

                    modelo.addRow(new Object[]{rs.getString("id"), rs.getString("nombre"),
                        rs.getString("descripcion"), rs.getDate("fecha"), rs.getString("hora"), formatea.format(rs.getDouble("total")), estado, rs.getString("fecha_pago"), rs.getString("fondo"), tipo});
                }

            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla.setModel(modelo);
            TamanosTablaAbonos();
            calcular_total();
            actualizarResumen();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public static javax.swing.JButton btn_actualizar;
    private javax.swing.JButton btn_actualizar_pago;
    public static javax.swing.JButton btn_cargar_foto;
    public static javax.swing.JButton btn_cargar_foto1;
    private javax.swing.JButton btn_crear;
    public static javax.swing.JButton btn_crear_cliente;
    private javax.swing.JButton btn_editar;
    private javax.swing.JButton btn_eliminar;
    public static javax.swing.JButton btn_eliminar_foto;
    public static javax.swing.JButton btn_guardar;
    private javax.swing.JButton btn_comparar;
    private javax.swing.JButton btn_imprimir;
    private javax.swing.JButton btn_imprimir1;
    public static javax.swing.JButton btn_limpiar;
    private javax.swing.JButton btn_total;
    private javax.swing.JButton btn_ver_pendiente;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox chk_dinero_recibido;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JComboBox<modelos.Cuentas_Ingresos> jbox_Cuentas;
    private javax.swing.JComboBox<modelos.Fondos> jbox_Fondos;
    public static javax.swing.JComboBox<Contactos> jbox_contacto;
    public static com.toedter.calendar.JDateChooser jdate_fecha_entrada;
    private javax.swing.JTable jtabla;
    public static javax.swing.JTable jtabla_fotos;
    private javax.swing.JTable jtabla_resumen;
    private javax.swing.JTextArea jtxa_descripcion_crear;
    private javax.swing.JPanel lbl_cant_clientes;
    public static javax.swing.JLabel lbl_foto_1;
    public static javax.swing.JLabel lbl_id;
    private javax.swing.JLabel lbl_text;
    private javax.swing.JLabel lbl_text1;
    private javax.swing.JLabel lbl_text2;
    private javax.swing.JLabel lbl_total_F;
    private javax.swing.JLabel lbl_total_R;
    private javax.swing.JLabel lbl_total_suma;
    private javax.swing.JRadioButton rbtn_F;
    private javax.swing.JRadioButton rbtn_R;
    private javax.swing.JTextField txt_Filtro;
    private javax.swing.JTextField txt_total;
    // End of variables declaration//GEN-END:variables
}
