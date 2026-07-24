/*
 * frm_egresos del modulo Caja (portado desde cajadiaria).
 * Rediseño: sin abonos_egresos (fondo directo en egresos.id_fondo),
 * ids serial (sin cargarId), fotos con tipo_registro=2 (egreso).
 */
package Caja;

import Formularios.frm_main;
import Metodos.ExportarExcel;
import Metodos.TextPrompt;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBEgresos;
import conexiondb.DB_Fotos_servicios;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Cuentas_Egresos;
import modelos.Egresos;
import modelos.Fondos;
import modelos.Fotos_registros;

/**
 *
 * @author Monkeyelgrande
 */
public class frm_egresos extends javax.swing.JInternalFrame {

    /**
     * Creates new form frm_clientes
     */
    public static DefaultTableModel modelo_fotos = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    Calendar fecha = new GregorianCalendar();
    static TableColumnModel columnModel = null;
    String ruta_foto = "";
    String ruta_origen_imagen = "";
    public static String nombreArchivoImagen = "";

    public frm_egresos() {
        initComponents();
        Fondos.mostrarFondos(jbox_Fondos);
        modelo_fotos.setColumnIdentifiers(new Object[]{"Ruta", "Nombre", "id_foto"});
        jtabla_fotos.setModel(modelo_fotos);

        chk_dinero_recibido.setVisible(false);

        jtxa_descripcion_crear.setWrapStyleWord(true);
        try {
            for (int i = 0; i < jtabla_gastos.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        actualizar();
        // id serial: se conoce al guardar (RETURNING id)
        lbl_id.setText("Nuevo");
        Cuentas_Egresos.mostrarCuentas(jbox_Cuentas);
        jdate_fecha_entrada.setCalendar(fecha);
        metodos.EvitarTabEnJTextArea(jtxa_descripcion_crear);
        TextPrompt prompt = new TextPrompt("Valor egreso", txt_total);

        columnModel = jtabla_gastos.getColumnModel();
        TamanosTablaAbonos();
        jtabla_gastos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    try {
                        verEgreso();
                    } catch (SQLException ex) {
                        Logger.getLogger(frm_egresos.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        Metodos.metodos.TablaAptaParaBusquedaAndSSM(jtabla_gastos);
        metodos.BuscarEnTabla(txt_Filtro, jtabla_gastos);

        if (frm_main.perfil != 1) {
            btn_eliminar.setEnabled(false);
            btn_editar.setEnabled(false);
        }

        jtabla_fotos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 1) {
                    int fila = jtabla_fotos.getSelectedRow();

                    String ruta = (String) jtabla_fotos.getValueAt(fila, 0);
                    foto_a_label(ruta, lbl_foto_1);

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

    public static void TamanosTablaAbonos() {
        columnModel.getColumn(0).setPreferredWidth(20);
        columnModel.getColumn(1).setPreferredWidth(60);
        columnModel.getColumn(2).setPreferredWidth(350);
        columnModel.getColumn(3).setPreferredWidth(80);
        columnModel.getColumn(4).setPreferredWidth(80);
        columnModel.getColumn(5).setPreferredWidth(100);
        columnModel.getColumn(5).setPreferredWidth(10);

    }

    // Ajusta la foto proporcionalmente y centrada dentro del JLabel
    // (en bodega Metodos.metodos no trae foto_a_label; helper local)
    private static void foto_a_label(String ruta, final JLabel lbl_foto) {
        try {

            final Image img = new ImageIcon(ruta).getImage();

            int originalWidth = img.getWidth(null);
            int originalHeight = img.getHeight(null);

            double scaleFactor = Math.min(1.0 * lbl_foto.getWidth() / originalWidth, 1.0 * lbl_foto.getHeight() / originalHeight);

            int scaledWidth = (int) (originalWidth * scaleFactor);
            int scaledHeight = (int) (originalHeight * scaleFactor);

            final Image newimg = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_DEFAULT);

            ImageIcon newicon = new ImageIcon(newimg) {
                @Override
                public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
                    int offsetX = (lbl_foto.getWidth() - getIconWidth()) / 2;
                    int offsetY = (lbl_foto.getHeight() - getIconHeight()) / 2;

                    g.drawImage(newimg, x + offsetX, y + offsetY, c);
                }
            };

            lbl_foto.setIcon(newicon);

        } catch (Exception e) {
            System.out.println("NO se cargo la imagen");
        }
    }

    public void verEgreso() throws SQLException {
        int fila = jtabla_gastos.getSelectedRow();
        jd_ver_in_egre frm = new jd_ver_in_egre();
        String id = "" + jtabla_gastos.getValueAt(fila, 0);
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
        } else {
            Egresos egreso = new Egresos();
            egreso = Egresos.traer_egreso(id);

            jd_ver_in_egre.lbl_user.setText(egreso.getNombre_user());
            jd_ver_in_egre.lbl_total.setText("$ " + metodos.formateador_dinero().format(egreso.getTotal()));
            jd_ver_in_egre.lbl_fecha.setText(egreso.getFecha());
            jd_ver_in_egre.lbl_hora.setText(egreso.getHora());
            jd_ver_in_egre.jtxa_descripcion.setText(egreso.getDescripcion());
            jd_ver_in_egre.lbl_cuenta_nombre.setText(egreso.getNombre_cuenta());

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

        // tipo_registro=2 es egreso en el esquema nuevo
        String consulta = "select * from fotos_registros where tipo_registro=2 and id_registro=" + id;
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        jd_ver_in_egre.modelo_fotos.setColumnIdentifiers(new Object[]{"Ruta", "Nombre", "id_foto"});
        while (rs.next()) {

            jd_ver_in_egre.modelo_fotos.addRow(new Object[]{DB_consultas_R_D.Ruta_Imagenes() + rs.getString("nombre"), rs.getString("nombre"), rs.getString("id")});
            jd_ver_in_egre.jtabla_fotos.setModel(jd_ver_in_egre.modelo_fotos);

            foto_a_label(DB_consultas_R_D.Ruta_Imagenes() + rs.getString("nombre"), jd_ver_in_egre.lbl_foto_1);

        }

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
        txt_nombre_cliente = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        btn_buscar_cliente = new javax.swing.JButton();
        btn_crear_cliente = new javax.swing.JButton();
        jbox_Fondos = new javax.swing.JComboBox<>();
        lbl_id_cliente = new javax.swing.JLabel();
        chk_dinero_recibido = new javax.swing.JCheckBox();
        rbtn_R = new javax.swing.JRadioButton();
        rbtn_F = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane4 = new javax.swing.JScrollPane();
        jtabla_fotos = new javax.swing.JTable();
        btn_cargar_foto = new javax.swing.JButton();
        btn_eliminar_foto = new javax.swing.JButton();
        btn_cargar_foto1 = new javax.swing.JButton();
        lbl_foto_1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        lbl_cant_clientes = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla_gastos = new org.jdesktop.swingx.JXTable();
        btn_total = new javax.swing.JButton();
        lbl_total_suma = new javax.swing.JLabel();
        lbl_total_suma1 = new javax.swing.JLabel();
        txt_Filtro = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        btn_crear = new javax.swing.JButton();
        btn_eliminar = new javax.swing.JButton();
        btn_editar = new javax.swing.JButton();
        btn_actualizar = new javax.swing.JButton();
        btn_imprimir = new javax.swing.JButton();
        btn_imprimir1 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Egresos");

        jPanel1.setBackground(new java.awt.Color(255, 102, 102));

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

        jLabel20.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel20.setText("Cuenta");

        jLabel23.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel23.setText("Fecha gasto");

        jLabel25.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel25.setText("Descripción:");

        jLabel24.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel24.setText("Crear egreso");

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

        jLabel26.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel26.setText("Total");

        txt_nombre_cliente.setEditable(false);
        txt_nombre_cliente.setFont(new java.awt.Font("Segoe UI", 3, 14)); // NOI18N
        txt_nombre_cliente.setText("0000");
        txt_nombre_cliente.setDisabledTextColor(new java.awt.Color(255, 255, 255));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setText("Cliente");

        btn_buscar_cliente.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        btn_buscar_cliente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/lupa.png"))); // NOI18N
        btn_buscar_cliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscar_clienteActionPerformed(evt);
            }
        });

        btn_crear_cliente.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        btn_crear_cliente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/mas.png"))); // NOI18N
        btn_crear_cliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_crear_clienteActionPerformed(evt);
            }
        });

        jbox_Fondos.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jbox_Fondos.setForeground(new java.awt.Color(51, 51, 51));

        lbl_id_cliente.setBackground(new java.awt.Color(73, 229, 221));
        lbl_id_cliente.setFont(new java.awt.Font("Segoe UI", 1, 8)); // NOI18N
        lbl_id_cliente.setForeground(new java.awt.Color(73, 229, 221));
        lbl_id_cliente.setText("1");

        chk_dinero_recibido.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        chk_dinero_recibido.setSelected(true);
        chk_dinero_recibido.setText("Dinero Recibido");

        buttonGroup1.add(rbtn_R);
        rbtn_R.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        rbtn_R.setText("R");

        buttonGroup1.add(rbtn_F);
        rbtn_F.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        rbtn_F.setSelected(true);
        rbtn_F.setText("F");

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

        lbl_foto_1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_foto_1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        lbl_foto_1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lbl_foto_1MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lbl_id)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_id_cliente)
                        .addGap(167, 167, 167)
                        .addComponent(jButton3)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel25)
                            .addComponent(jLabel26)
                            .addComponent(jLabel23)
                            .addComponent(jLabel20)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(btn_guardar, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btn_limpiar, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(txt_nombre_cliente)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_buscar_cliente)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_crear_cliente, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane3)
                            .addComponent(jbox_Cuentas, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jdate_fecha_entrada, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txt_total)
                            .addComponent(jbox_Fondos, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(rbtn_F)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(rbtn_R))
                                    .addComponent(chk_dinero_recibido))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jSeparator1)
                    .addComponent(lbl_foto_1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(btn_cargar_foto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_eliminar_foto)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_cargar_foto1)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24)
                    .addComponent(jButton3)
                    .addComponent(lbl_id)
                    .addComponent(lbl_id_cliente))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txt_nombre_cliente, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel3))
                    .addComponent(btn_crear_cliente)
                    .addComponent(btn_buscar_cliente))
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
                .addComponent(jbox_Fondos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                .addComponent(lbl_foto_1, javax.swing.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        lbl_cant_clientes.setBackground(new java.awt.Color(33, 33, 33));

        jtabla_gastos.setModel(new javax.swing.table.DefaultTableModel(
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
        jtabla_gastos.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jtabla_gastos.setRowHeight(25);
        jtabla_gastos.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jScrollPane2.setViewportView(jtabla_gastos);

        btn_total.setBackground(new java.awt.Color(0, 153, 51));
        btn_total.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        btn_total.setForeground(new java.awt.Color(255, 255, 255));
        btn_total.setText("Sumar Total");
        btn_total.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_totalActionPerformed(evt);
            }
        });

        lbl_total_suma.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        lbl_total_suma.setForeground(new java.awt.Color(255, 255, 255));
        lbl_total_suma.setText("0");

        lbl_total_suma1.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        lbl_total_suma1.setForeground(new java.awt.Color(255, 255, 255));
        lbl_total_suma1.setText("$");

        txt_Filtro.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        txt_Filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_FiltroKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout lbl_cant_clientesLayout = new javax.swing.GroupLayout(lbl_cant_clientes);
        lbl_cant_clientes.setLayout(lbl_cant_clientesLayout);
        lbl_cant_clientesLayout.setHorizontalGroup(
            lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 837, Short.MAX_VALUE)
                    .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                        .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, 277, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_total)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbl_total_suma1)
                        .addGap(8, 8, 8)
                        .addComponent(lbl_total_suma)))
                .addContainerGap())
        );
        lbl_cant_clientesLayout.setVerticalGroup(
            lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_total)
                    .addComponent(lbl_total_suma1)
                    .addComponent(lbl_total_suma)
                    .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 707, Short.MAX_VALUE)
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
                    .addComponent(btn_actualizar, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                    .addComponent(btn_editar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_eliminar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_crear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_imprimir, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_imprimir1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                .addComponent(btn_imprimir, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_imprimir1, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btn_actualizar)
                .addContainerGap())
        );

        jPanel4.setBackground(new java.awt.Color(153, 0, 51));
        jPanel4.setPreferredSize(new java.awt.Dimension(146, 80));

        jLabel13.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("Egresos");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jLabel13)
                .addContainerGap(900, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel13)
                .addGap(26, 26, 26))
        );

        jButton1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/excel.png"))); // NOI18N
        jButton1.setText("Exportar a excel");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
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
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton1)
                            .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_cant_clientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1024, Short.MAX_VALUE))
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
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
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
        if (jtxa_descripcion_crear.getText().isEmpty() || txt_total.getText().isEmpty()) {
            jtxa_descripcion_crear.setBackground(Color.pink);
            txt_total.setBackground(Color.pink);
        } else {

            DBEgresos dbegreso = new DBEgresos();

            Egresos obj = new Egresos();

            obj.setDescripcion(jtxa_descripcion_crear.getText());
            int dia, mes, ano;
            ano = jdate_fecha_entrada.getCalendar().get(Calendar.YEAR);
            mes = jdate_fecha_entrada.getCalendar().get(Calendar.MARCH) + 1;
            dia = jdate_fecha_entrada.getCalendar().get(Calendar.DAY_OF_MONTH);
            // sin comillas: la fecha va como parámetro de PreparedStatement
            obj.setFecha(ano + "-" + mes + "-" + dia);

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
                obj.setId_cliente(Integer.parseInt(lbl_id_cliente.getText()));
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

            if (edita && DB_consultas_R_D.consultarId(lbl_id.getText(), "egresos") == 1) {
                obj.setId(Integer.parseInt(lbl_id.getText()));
                dbegreso.Actualizar(obj, true);
                edita = false;
            } else {

                // id serial: Guardar devuelve el id generado (RETURNING id)
                if (dbegreso.Guardar(obj, true) > 0) {
// FOTOS
                    if (modelo_fotos.getRowCount() > 0) {
                        for (int i = 0; i < modelo_fotos.getRowCount(); i++) {

                            if (jtabla_fotos.getValueAt(i, 2).toString().equals("0")) {

                                metodos.copyFile_Java7(jtabla_fotos.getValueAt(i, 0).toString(), DB_consultas_R_D.Ruta_Imagenes() + jtabla_fotos.getValueAt(i, 1).toString());

                                // id 0: la BD lo genera (serial); tipo_registro 2 = egreso
                                Fotos_registros foto = new Fotos_registros(jtabla_fotos.getValueAt(i, 1).toString(), obj.getId(), 0, 2);

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
            txt_total.requestFocus();
        }
    }//GEN-LAST:event_btn_guardarActionPerformed

    public void limpiar() {
        jtxa_descripcion_crear.setText("");
        txt_total.setText("");
//        jdate_fecha_entrada.setCalendar(fecha);
        txt_total.setBackground(Color.white);
        jtxa_descripcion_crear.setBackground(Color.white);
        txt_nombre_cliente.setText("0000");
        lbl_id_cliente.setText("1");
        lbl_id.setText("Nuevo");
        jbox_Cuentas.setSelectedItem(Cuentas_Egresos.TraerPredeterminadoNombre());
        jbox_Fondos.setSelectedItem(Fondos.TraerPredeterminadoNombre());
        id_cuenta = Cuentas_Egresos.TraerPredeterminadoID();
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
            String nuevo = metodos.formateador_dinero().format(to);
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
        int fila = jtabla_gastos.getSelectedRow();

        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
        } else {
            String id = "" + jtabla_gastos.getValueAt(fila, 0);
            // rediseño: el fondo va directo en egresos.id_fondo (sin abonos_egresos)
            String consulta = "select i.*, u.nombre as user, cu.nombre as cuenta, cu.id as id_cuenta, coalesce(f.nombre,'default') as fondo, coalesce(i.id_fondo,0) as id_fondo_sel, co.nombre as cliente \n"
                    + "from egresos i \n"
                    + "left join fondos f on i.id_fondo=f.id, users u, cuentas_egresos cu, contactos co\n"
                    + "where i.id_cuenta=cu.id and i.id_user=u.id and i.id_cliente=co.id and i.id=" + id;

            ResultSet rs = DB_consultas_R_D.getTabla(consulta);
            try {
                while (rs.next()) {
                    lbl_id.setText(rs.getString("id"));
                    txt_nombre_cliente.setText(rs.getString("cliente"));
                    lbl_id_cliente.setText(rs.getString("id_cliente"));
                    jtxa_descripcion_crear.setText(rs.getString("descripcion"));
                    txt_total.setText(metodos.formateador_dinero().format(rs.getDouble("total")));
                    jbox_Cuentas.setSelectedItem(rs.getString("cuenta"));
                    id_cuenta = rs.getInt("id_cuenta");
                    id_fondo = rs.getInt("id_fondo_sel");
                    jdate_fecha_entrada.setDate(rs.getDate("fecha"));

                    if (rs.getInt("factura_remision") == 1) {
                        rbtn_F.setSelected(true);
                    } else {
                        rbtn_R.setSelected(true);
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
        int fila = jtabla_gastos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar este egreso?", "Alerta", dialogButton);
            if (dialogResult == JOptionPane.YES_OPTION) {

                try {
                    DefaultTableModel modelo = (DefaultTableModel) jtabla_gastos.getModel();
                    String id = (String) jtabla_gastos.getValueAt(fila, 0);//suponiendo que el id lo muestras en la primera columna
                    DB_consultas_R_D.eliminar("egresos", id);
                    for (int i = 0; i < modelo.getRowCount(); i++) {
                        if (modelo.getValueAt(i, 0).equals(id)) {
                            modelo.removeRow(i);
                            btn_totalActionPerformed(null);
                            limpiar();
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
        for (int i = 0; i < this.jtabla_gastos.getRowCount(); i++) {
            total += Double.parseDouble(metodos.EliminaCaracteres(this.jtabla_gastos.getValueAt(i, 5).toString(), "."));
        }
        lbl_total_suma.setText(metodos.formateador_dinero().format(total) + "");
    }//GEN-LAST:event_btn_totalActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        ExportarExcel obj;

        try {
            obj = new ExportarExcel();
            obj.exportarExcel(jtabla_gastos);
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void btn_buscar_clienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_buscar_clienteActionPerformed
        jd_buscar_contacto_servicio buscar = new jd_buscar_contacto_servicio(null, rootPaneCheckingEnabled);
        buscar.formulario = "egreso";
        buscar.show();
    }//GEN-LAST:event_btn_buscar_clienteActionPerformed

    private void btn_crear_clienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_crear_clienteActionPerformed
        jif_crear_contactos frm = new jif_crear_contactos();
        jif_crear_contactos.formulario = "egreso";
        jif_crear_contactos.txt_nombre.requestFocus();
        frm.show();
    }//GEN-LAST:event_btn_crear_clienteActionPerformed

    private void txt_FiltroKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_FiltroKeyTyped

    }//GEN-LAST:event_txt_FiltroKeyTyped

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
            nombreArchivoImagen = lbl_id.getText() + "_" + DB_consultas_R_D.obtener_fecha() + DB_consultas_R_D.obtener_hora_con_guiones() + "_" + fichero.getName();

            try {

                Image img = ImageIO.read(j.getSelectedFile());
                // Obtener el ancho y alto originales de la imagen
                int originalWidth = img.getWidth(null);
                int originalHeight = img.getHeight(null);

                // Calcular la escala para ajustar la imagen proporcionalmente
                double scaleFactor = Math.min(1.0 * lbl_foto_1.getWidth() / originalWidth, 1.0 * lbl_foto_1.getHeight() / originalHeight);

                // Redimensionar la imagen utilizando la escala calculada
                int scaledWidth = (int) (originalWidth * scaleFactor);
                int scaledHeight = (int) (originalHeight * scaleFactor);

                final Image newimg = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_DEFAULT);

                // Crear un ImageIcon personalizado para centrar la imagen
                ImageIcon newicon = new ImageIcon(newimg) {
                    @Override
                    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
                        // Calcular las coordenadas para centrar la imagen en el JLabel
                        int offsetX = (lbl_foto_1.getWidth() - getIconWidth()) / 2;
                        int offsetY = (lbl_foto_1.getHeight() - getIconHeight()) / 2;

                        // Dibujar la imagen centrada
                        g.drawImage(newimg, x + offsetX, y + offsetY, c);
                    }
                };

                lbl_foto_1.setIcon(newicon);

                modelo_fotos.addRow(new Object[]{ruta_origen_imagen, nombreArchivoImagen, 0});

                jtabla_fotos.setModel(modelo_fotos);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(rootPane, "imagen: " + ex);
            }
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

                    // las fotos del modulo Caja viven en fotos_registros
                    // (en el origen decia fotos_ordenes por error)
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

        // Pasamos 'this' como el componente padre.
        captura.lanzar_camara(idUsuario, modelo_fotos, this);
    }//GEN-LAST:event_btn_cargar_foto1ActionPerformed

    private void lbl_foto_1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_foto_1MouseClicked
        try {
            DB_consultas_R_D.Abrir_Archivo(ruta_foto);
        } catch (Exception e) {
        }
    }//GEN-LAST:event_lbl_foto_1MouseClicked

    private void btn_imprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imprimirActionPerformed

        int fila = jtabla_gastos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(null, "¿Desea imrpimir este egreso?", "Alerta", dialogButton);
            if (dialogResult == JOptionPane.YES_OPTION) {
                String id = jtabla_gastos.getValueAt(fila, 0).toString();//suponiendo que el id lo muestras en la primera columna

                String consulta = "select i.id, i.fecha, i.hora, i.descripcion, i.total, co.nombre as contacto, cu.nombre as cuenta\n"
                + "from egresos i inner join contactos co on i.id_cliente=co.id inner join cuentas_egresos cu on i.id_cuenta=cu.id\n"
                + "where i.id = " + id;
                ResultSet rs = DB_consultas_R_D.getTabla(consulta);

                ImprimirTermica80MM_caja_diaria imprimir = null;
                try {
                    while (rs.next()) {
                        imprimir = new ImprimirTermica80MM_caja_diaria(rs.getString("fecha") + " / " + rs.getString("hora"), rs.getString("id"),
                            rs.getString("contacto"), metodos.formateador_dinero().format(rs.getDouble("total")), jtabla_gastos.getValueAt(fila, 6).toString(), rs.getString("cuenta"), rs.getString("descripcion"),"EGRESO");
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

    private void btn_imprimir1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imprimir1ActionPerformed
        int fila = jtabla_gastos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(null, "¿Desea imrpimir este egreso?", "Alerta", dialogButton);
            if (dialogResult == JOptionPane.YES_OPTION) {
                String id = jtabla_gastos.getValueAt(fila, 0).toString();//suponiendo que el id lo muestras en la primera columna

                String consulta = "select i.id, i.fecha, i.hora, i.descripcion, i.total, co.nombre as contacto, cu.nombre as cuenta\n"
                + "from egresos i inner join contactos co on i.id_cliente=co.id inner join cuentas_egresos cu on i.id_cuenta=cu.id\n"
                + "where i.id = " + id;
                ResultSet rs = DB_consultas_R_D.getTabla(consulta);

                ImprimirPNG_caja_diaria imprimir = null;
                try {
                    while (rs.next()) {
                        imprimir = new ImprimirPNG_caja_diaria(rs.getString("fecha") + " / " + rs.getString("hora"), rs.getString("id"),
                            rs.getString("contacto"), metodos.formateador_dinero().format(rs.getDouble("total")), jtabla_gastos.getValueAt(fila, 6).toString(), rs.getString("cuenta"), rs.getString("descripcion"),"EGRESO");
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
            for (int i = 0; i < jtabla_gastos.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        String consulta = "";

        // rediseño: sin abonos_egresos; abono/fecha_pago se derivan de id_fondo
        if (act == 0) {
            consulta = "SELECT\n"
                    + "    e.id,\n"
                    + "    e.factura_remision,\n"
                    + "    c.nombre,\n"
                    + "    e.total,\n"
                    + "    e.descripcion,\n"
                    + "    e.fecha,\n"
                    + "    e.hora,\n"
                    + "    case when e.id_fondo is not null then e.total else 0 end AS abono,\n"
                    + "    case when e.id_fondo is not null then e.fecha || '' else 'Pendiente' end AS fecha_pago,\n"
                    + "    coalesce(f.nombre,'Pendiente') AS fondo\n"
                    + "FROM egresos e\n"
                    + "LEFT JOIN cuentas_egresos c\n"
                    + "    ON e.id_cuenta   = c.id\n"
                    + "LEFT JOIN fondos f\n"
                    + "    ON e.id_fondo    = f.id\n"
                    + "WHERE\n"
                    + "    e.fecha = CURRENT_DATE\n"
                    + "ORDER BY\n"
                    + "    e.fecha DESC,\n"
                    + "    e.id   DESC;";
            act = 1;
        } else {
            consulta = "SELECT\n"
                    + "    e.id,\n"
                    + "    e.factura_remision,\n"
                    + "    c.nombre,\n"
                    + "    e.total,\n"
                    + "    e.descripcion,\n"
                    + "    e.fecha,\n"
                    + "    e.hora,\n"
                    + "    case when e.id_fondo is not null then e.total else 0 end AS abono,\n"
                    + "    case when e.id_fondo is not null then e.fecha || '' else 'Pendiente' end AS fecha_pago,\n"
                    + "    coalesce(f.nombre,'Pendiente') AS fondo\n"
                    + "FROM egresos e\n"
                    + "LEFT JOIN cuentas_egresos c\n"
                    + "    ON e.id_cuenta   = c.id\n"
                    + "LEFT JOIN fondos f\n"
                    + "    ON e.id_fondo    = f.id\n"
                    + "ORDER BY\n"
                    + "    e.fecha DESC,\n"
                    + "    e.id   DESC;";
            act = 0;
        }
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        modelo.setColumnIdentifiers(new Object[]{"id", "Cuenta", "Descripcion", "Fecha", "Hora", "Total", "Fondo", "Tipo"});
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
                modelo.addRow(new Object[]{rs.getString("id"), rs.getString("nombre"),
                    rs.getString("descripcion"), rs.getDate("fecha"), rs.getString("hora"), metodos.formateador_dinero().format(rs.getDouble("total")), rs.getString("fondo"), tipo});
            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla_gastos.setModel(modelo);
            TamanosTablaAbonos();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public static javax.swing.JButton btn_actualizar;
    public static javax.swing.JButton btn_buscar_cliente;
    public static javax.swing.JButton btn_cargar_foto;
    public static javax.swing.JButton btn_cargar_foto1;
    private javax.swing.JButton btn_crear;
    public static javax.swing.JButton btn_crear_cliente;
    private javax.swing.JButton btn_editar;
    private javax.swing.JButton btn_eliminar;
    public static javax.swing.JButton btn_eliminar_foto;
    public static javax.swing.JButton btn_guardar;
    private javax.swing.JButton btn_imprimir;
    private javax.swing.JButton btn_imprimir1;
    public static javax.swing.JButton btn_limpiar;
    private javax.swing.JButton btn_total;
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
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JComboBox<modelos.Cuentas_Egresos> jbox_Cuentas;
    private javax.swing.JComboBox<modelos.Fondos> jbox_Fondos;
    public static com.toedter.calendar.JDateChooser jdate_fecha_entrada;
    public static javax.swing.JTable jtabla_fotos;
    private org.jdesktop.swingx.JXTable jtabla_gastos;
    private javax.swing.JTextArea jtxa_descripcion_crear;
    private javax.swing.JPanel lbl_cant_clientes;
    public static javax.swing.JLabel lbl_foto_1;
    public static javax.swing.JLabel lbl_id;
    public static javax.swing.JLabel lbl_id_cliente;
    private javax.swing.JLabel lbl_total_suma;
    private javax.swing.JLabel lbl_total_suma1;
    private javax.swing.JRadioButton rbtn_F;
    private javax.swing.JRadioButton rbtn_R;
    private javax.swing.JTextField txt_Filtro;
    public static javax.swing.JTextField txt_nombre_cliente;
    private javax.swing.JTextField txt_total;
    // End of variables declaration//GEN-END:variables
}
