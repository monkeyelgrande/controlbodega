/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos;

import Creditos.frm_Creditos;
import Formularios.frm_main;
import Metodos.TextPrompt;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import Creditos.db.DBcontactos;
import Creditos.db.DBfacturas;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import Creditos.modelos.Contactos;
import Creditos.modelos.Facturas;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import Creditos.modelos.Cuentas;
import javax.swing.TransferHandler;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import com.formdev.flatlaf.FlatLightLaf;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.SwingUtilities;

/**
 *
 * @author Monkeyelgrande
 */
public class jif_crear_credito extends javax.swing.JDialog {

    /**
     * Creates new form jif_crear_marca
     */
    private int x; // variables x y y para arrastrar ventana desde jpanel
    private int y;
    public static String id_categoria;
    public static String id_sub_categoria;
    public static String id_item = "";
    public static Facturas factura = new Facturas();
    FileInputStream fis;
    int longitudBytes;
    public static String tipo = "";
    public static int id_cliente = 0;
    public static boolean editar = false;

    public jif_crear_credito() {
        initComponents();
        nombreArchivoPDF = "";
        btn_editar.setVisible(false);
        lbl_id_credito.setText(DB_consultas_R_D.cargarId("creditos"));
        this.setLocationRelativeTo(this);
        metodos.addEscapeListenerWindowDialog(this);

        Cuentas cuenta = new Cuentas();
        cuenta.mostrarCuentas(jbox_Cuentas);

        Contactos contratante = new Contactos();
        contratante.MostrarNombreContactos(cbx_contacto);
        txt_descripcion.setWrapStyleWord(true);
        metodos.EvitarTabEnJTextArea(txt_descripcion);

        TextPrompt precio = new TextPrompt("$ Obligatorio", txt_total);
        TextPrompt interes = new TextPrompt("Obligatorio", txt_interes);
        TextPrompt descripcion = new TextPrompt("Descripcion de la factura", txt_descripcion);
        TextPrompt recibo = new TextPrompt("Numero de factura (opcional)", txt_codigo);
        poner_fechas();
        cbx_contacto.requestFocus();

        txt_cupo_aprobado.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {

                    int dialogButton = JOptionPane.YES_NO_OPTION;
                    int dialogResult = JOptionPane.showConfirmDialog(null, "Desea modificar el cupo aprobado?", "Alerta", dialogButton);
                    if (dialogResult == JOptionPane.YES_OPTION) {
                        txt_cupo_aprobado.setEditable(true);
                    }

                }
            }
        });

        lbl_foto.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                // Verifica si se está arrastrando una lista de archivos
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    Transferable t = support.getTransferable();
                    // Obtenemos la lista de archivos arrastrados
                    @SuppressWarnings("unchecked")
                    java.util.List<File> archivos = (java.util.List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

                    if (!archivos.isEmpty()) {
                        // Tomamos el primer archivo (o recorre la lista si quieres manejar varios)
                        File fichero = archivos.get(0);

                        // Aquí replicas la lógica de tu JFileChooser
                        ruta_origen_imagen = fichero.getAbsolutePath();
                        nombreArchivoImagen = lbl_id_credito.getText() + "_"
                                + DB_consultas_R_D.obtener_fecha()
                                + DB_consultas_R_D.obtener_hora_con_guiones()
                                + fichero.getName();
                        ruta_destino_nombre_archivo_imagen = DB_consultas_R_D.Ruta_Imagenes()
                                + nombreArchivoImagen;
                        longitudBytes = (int) fichero.length();

                        // Carga y muestra la imagen en el label
                        Image imagen = ImageIO.read(fichero).getScaledInstance(
                                lbl_foto.getWidth(),
                                lbl_foto.getHeight(),
                                Image.SCALE_DEFAULT
                        );
                        lbl_foto.setIcon(new ImageIcon(imagen));
                        lbl_foto.updateUI();
                    }
                    return true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        });

    }

    public void poner_fechas() {
        Calendar fecha = new GregorianCalendar();
        jdate_fecha_creacion.setCalendar(fecha);
        try {
            String fecha_mes = DB_consultas_R_D.obtener_fecha_seguiente_mes();
            Date date2 = new SimpleDateFormat("yyyy-MM-dd").parse(fecha_mes);
            jdate_fecha_vencimiento.setDate(date2);
        } catch (Exception e) {
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panel_titulo = new javax.swing.JPanel();
        lbl_titulo = new javax.swing.JLabel();
        lbl_id_credito = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        txt_total = new javax.swing.JTextField();
        btn_guardar = new javax.swing.JButton();
        btn_limpiar = new javax.swing.JButton();
        chk_cerrar = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        txt_codigo = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jdate_fecha_creacion = new com.toedter.calendar.JDateChooser();
        cbx_contacto = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        txt_descripcion = new javax.swing.JTextArea();
        btn_editar = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        jdate_fecha_vencimiento = new com.toedter.calendar.JDateChooser();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        txt_interes = new javax.swing.JTextField();
        jbox_Cuentas = new javax.swing.JComboBox<>();
        btn_agregar = new javax.swing.JButton();
        btn_sumar30Dias = new javax.swing.JButton();
        btn_buscar_cliente = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        lbl_foto = new javax.swing.JLabel();
        btnAgregarImagen = new javax.swing.JButton();
        btnAgregarImagen1 = new javax.swing.JButton();
        btn_Eliminar_Imagen = new javax.swing.JButton();
        btn_Eliminar_PDF = new javax.swing.JButton();
        panel_titulo1 = new javax.swing.JPanel();
        lbl_titulo1 = new javax.swing.JLabel();
        lbl_cupo_usado = new javax.swing.JLabel();
        lbl_titulo4 = new javax.swing.JLabel();
        lbl_titulo5 = new javax.swing.JLabel();
        lbl_cupo_disponible = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        txt_cupo_aprobado = new javax.swing.JTextField();

        setTitle("CREAR CRÉDITO");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setModal(true);
        setResizable(false);

        panel_titulo.setBackground(new java.awt.Color(241, 200, 11));
        panel_titulo.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                panel_tituloMouseDragged(evt);
            }
        });
        panel_titulo.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                panel_tituloMousePressed(evt);
            }
        });

        lbl_titulo.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_titulo.setForeground(new java.awt.Color(45, 54, 76));
        lbl_titulo.setText("Nuevo credito");

        lbl_id_credito.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_id_credito.setForeground(new java.awt.Color(255, 255, 255));
        lbl_id_credito.setText("id");

        javax.swing.GroupLayout panel_tituloLayout = new javax.swing.GroupLayout(panel_titulo);
        panel_titulo.setLayout(panel_tituloLayout);
        panel_tituloLayout.setHorizontalGroup(
            panel_tituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_tituloLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbl_titulo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lbl_id_credito)
                .addContainerGap())
        );
        panel_tituloLayout.setVerticalGroup(
            panel_tituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_tituloLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panel_tituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_titulo)
                    .addComponent(lbl_id_credito))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new java.awt.Color(53, 63, 89));
        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        jPanel2.setEnabled(false);
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Total");
        jPanel2.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 118, -1, -1));

        txt_total.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        txt_total.setForeground(new java.awt.Color(153, 0, 0));
        txt_total.setNextFocusableComponent(txt_codigo);
        txt_total.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_totalFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_totalFocusLost(evt);
            }
        });
        txt_total.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_totalKeyTyped(evt);
            }
        });
        jPanel2.add(txt_total, new org.netbeans.lib.awtextra.AbsoluteConstraints(177, 115, 300, -1));

        btn_guardar.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        btn_guardar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/guardar.png"))); // NOI18N
        btn_guardar.setMnemonic('g');
        btn_guardar.setText("Guardar");
        btn_guardar.setBorder(null);
        btn_guardar.setNextFocusableComponent(jdate_fecha_creacion);
        btn_guardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_guardarActionPerformed(evt);
            }
        });
        jPanel2.add(btn_guardar, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 388, 150, 56));

        btn_limpiar.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        btn_limpiar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/limpiar.png"))); // NOI18N
        btn_limpiar.setMnemonic('l');
        btn_limpiar.setText("Limpiar");
        btn_limpiar.setBorder(null);
        btn_limpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_limpiarActionPerformed(evt);
            }
        });
        jPanel2.add(btn_limpiar, new org.netbeans.lib.awtextra.AbsoluteConstraints(163, 388, 146, 56));

        chk_cerrar.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        chk_cerrar.setForeground(new java.awt.Color(255, 255, 255));
        chk_cerrar.setSelected(true);
        chk_cerrar.setText("Cerrar formulario al guardar");
        jPanel2.add(chk_cerrar, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 456, -1, -1));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("# Factura");
        jPanel2.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 155, -1, -1));

        txt_codigo.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        txt_codigo.setNextFocusableComponent(txt_interes);
        jPanel2.add(txt_codigo, new org.netbeans.lib.awtextra.AbsoluteConstraints(177, 152, 300, -1));

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Fecha inicio ");
        jPanel2.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 10, -1, -1));

        jdate_fecha_creacion.setForeground(new java.awt.Color(0, 153, 51));
        jdate_fecha_creacion.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jdate_fecha_creacion.setNextFocusableComponent(jdate_fecha_vencimiento);
        jdate_fecha_creacion.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jdate_fecha_creacionFocusLost(evt);
            }
        });
        jPanel2.add(jdate_fecha_creacion, new org.netbeans.lib.awtextra.AbsoluteConstraints(177, 7, 252, 31));

        cbx_contacto.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        cbx_contacto.setNextFocusableComponent(txt_total);
        cbx_contacto.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                cbx_contactoFocusLost(evt);
            }
        });
        cbx_contacto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbx_contactoActionPerformed(evt);
            }
        });
        jPanel2.add(cbx_contacto, new org.netbeans.lib.awtextra.AbsoluteConstraints(177, 78, 230, -1));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Cliente");
        jPanel2.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 78, -1, -1));

        txt_descripcion.setColumns(20);
        txt_descripcion.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_descripcion.setForeground(new java.awt.Color(0, 51, 51));
        txt_descripcion.setLineWrap(true);
        txt_descripcion.setRows(5);
        txt_descripcion.setNextFocusableComponent(btn_guardar);
        jScrollPane7.setViewportView(txt_descripcion);

        jPanel2.add(jScrollPane7, new org.netbeans.lib.awtextra.AbsoluteConstraints(177, 226, 300, 118));

        btn_editar.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        btn_editar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/editar.png"))); // NOI18N
        btn_editar.setText("Editar");
        btn_editar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_editarActionPerformed(evt);
            }
        });
        jPanel2.add(btn_editar, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 492, -1, -1));

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(255, 255, 255));
        jLabel10.setText("Fecha vencimiento ");
        jPanel2.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 47, -1, -1));

        jdate_fecha_vencimiento.setForeground(new java.awt.Color(153, 0, 0));
        jdate_fecha_vencimiento.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jdate_fecha_vencimiento.setNextFocusableComponent(cbx_contacto);
        jPanel2.add(jdate_fecha_vencimiento, new org.netbeans.lib.awtextra.AbsoluteConstraints(177, 44, 300, 28));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Descripcion");
        jPanel2.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 226, -1, -1));

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("Interes");
        jPanel2.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 192, -1, -1));

        txt_interes.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        txt_interes.setText("0");
        txt_interes.setNextFocusableComponent(txt_descripcion);
        txt_interes.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_interesFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_interesFocusLost(evt);
            }
        });
        txt_interes.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_interesKeyTyped(evt);
            }
        });
        jPanel2.add(txt_interes, new org.netbeans.lib.awtextra.AbsoluteConstraints(177, 189, 300, -1));

        jbox_Cuentas.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jbox_Cuentas.setForeground(new java.awt.Color(51, 51, 51));
        jPanel2.add(jbox_Cuentas, new org.netbeans.lib.awtextra.AbsoluteConstraints(177, 350, 300, -1));

        btn_agregar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/contacto_peq.png"))); // NOI18N
        btn_agregar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_agregarActionPerformed(evt);
            }
        });
        jPanel2.add(btn_agregar, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 80, 30, 31));

        btn_sumar30Dias.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/transfer-24.png"))); // NOI18N
        btn_sumar30Dias.setToolTipText("sumar 30 dias a la fecha de vencimiento");
        btn_sumar30Dias.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_sumar30DiasActionPerformed(evt);
            }
        });
        jPanel2.add(btn_sumar30Dias, new org.netbeans.lib.awtextra.AbsoluteConstraints(435, 7, 42, -1));

        btn_buscar_cliente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/bucar.png"))); // NOI18N
        btn_buscar_cliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscar_clienteActionPerformed(evt);
            }
        });
        jPanel2.add(btn_buscar_cliente, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 80, 30, 31));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        lbl_foto.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));

        btnAgregarImagen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/clientes_pequeno.png"))); // NOI18N
        btnAgregarImagen.setText("Cargar Imágen");
        btnAgregarImagen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarImagenActionPerformed(evt);
            }
        });

        btnAgregarImagen1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/pdf.png"))); // NOI18N
        btnAgregarImagen1.setText("Cargar PFD");
        btnAgregarImagen1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarImagen1ActionPerformed(evt);
            }
        });

        btn_Eliminar_Imagen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/ID not Verified.png"))); // NOI18N
        btn_Eliminar_Imagen.setText("Eliminar Imágen");
        btn_Eliminar_Imagen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Eliminar_ImagenActionPerformed(evt);
            }
        });

        btn_Eliminar_PDF.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Remove.png"))); // NOI18N
        btn_Eliminar_PDF.setText("Eliminar PDF");
        btn_Eliminar_PDF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Eliminar_PDFActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_foto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnAgregarImagen, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Eliminar_Imagen)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnAgregarImagen1, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Eliminar_PDF)
                        .addGap(0, 157, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btn_Eliminar_Imagen, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnAgregarImagen)
                        .addComponent(btnAgregarImagen1))
                    .addComponent(btn_Eliminar_PDF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_foto, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
                .addContainerGap())
        );

        panel_titulo1.setBackground(new java.awt.Color(0, 195, 55));
        panel_titulo1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                panel_titulo1MouseDragged(evt);
            }
        });
        panel_titulo1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                panel_titulo1MousePressed(evt);
            }
        });

        lbl_titulo1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_titulo1.setForeground(new java.awt.Color(45, 54, 76));
        lbl_titulo1.setText("Cupo aprobado");

        lbl_cupo_usado.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_cupo_usado.setForeground(new java.awt.Color(255, 0, 0));
        lbl_cupo_usado.setText("0");

        lbl_titulo4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_titulo4.setForeground(new java.awt.Color(255, 0, 0));
        lbl_titulo4.setText("Cupo Usado");

        lbl_titulo5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_titulo5.setForeground(new java.awt.Color(255, 255, 0));
        lbl_titulo5.setText("Cupo Disponible");

        lbl_cupo_disponible.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_cupo_disponible.setForeground(new java.awt.Color(255, 255, 0));
        lbl_cupo_disponible.setText("0");

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        txt_cupo_aprobado.setEditable(false);
        txt_cupo_aprobado.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txt_cupo_aprobado.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_cupo_aprobadoKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_cupo_aprobadoKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout panel_titulo1Layout = new javax.swing.GroupLayout(panel_titulo1);
        panel_titulo1.setLayout(panel_titulo1Layout);
        panel_titulo1Layout.setHorizontalGroup(
            panel_titulo1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_titulo1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbl_titulo1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txt_cupo_aprobado, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(lbl_titulo4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_cupo_usado)
                .addGap(12, 12, 12)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_titulo5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_cupo_disponible)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panel_titulo1Layout.setVerticalGroup(
            panel_titulo1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel_titulo1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panel_titulo1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2)
                    .addGroup(panel_titulo1Layout.createSequentialGroup()
                        .addGroup(panel_titulo1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(panel_titulo1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(lbl_titulo1)
                                .addComponent(lbl_titulo4)
                                .addComponent(lbl_cupo_usado)
                                .addComponent(lbl_titulo5)
                                .addComponent(lbl_cupo_disponible)
                                .addComponent(txt_cupo_aprobado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 495, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(panel_titulo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel_titulo1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panel_titulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panel_titulo1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 571, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public static int id_cuenta = 0;
    private void btn_guardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_guardarActionPerformed

        try {
            id_cliente = cbx_contacto.getItemAt(cbx_contacto.getSelectedIndex()).getId();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Por favor confirme la selección del cliente");
            return;
        }

        if (txt_total.getText().equals("") || txt_interes.getText().equals("")) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese una total y un interes para la factura");
            txt_total.requestFocus();
        } else {
            if (Double.parseDouble(metodos.EliminaCaracteres(txt_total.getText(), ".")) > Double.parseDouble(metodos.EliminaCaracteres(lbl_cupo_disponible.getText(), "."))) {

                int dialogButton = JOptionPane.YES_NO_OPTION;
                int dialogResult = JOptionPane.showConfirmDialog(null, "El total de este crédito supera el cupo disponible del cliente\n"
                        + "¿Desea continuar de todas maneras?\n\n"
                        + "EN CASO DE SER UNA ACTUALIACION SE CONTINUARA CON EL PROCESO", "Alerta", dialogButton);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    editar = true;

                }

                if (editar) {
                    if (guardar_credito()) {
                        if (chk_cerrar.isSelected() && condicion) {
                            this.dispose();
                        } else {
                            if (condicion) {
                                limpiar();
                                lbl_id_credito.setText(DB_consultas_R_D.cargarId("creditos"));
                                lbl_titulo.setText("Nueva factura");
                            }
                        }
                    }
                }

            } else {
                if (guardar_credito()) {
                    if (chk_cerrar.isSelected() && condicion) {
                        this.dispose();
                    } else {
                        if (condicion) {
                            limpiar();
                            lbl_id_credito.setText(DB_consultas_R_D.cargarId("creditos"));
                            lbl_titulo.setText("Nueva factura");
                        }

                    }
                }

            }
        }
    }//GEN-LAST:event_btn_guardarActionPerformed
    public static boolean condicion = true;

    public static boolean guardar_credito() {
        DBfacturas db_factura = new DBfacturas();
        condicion = true;

        if (btn_guardar.getText().equals("Actualizar")) {
            factura.setId(Integer.parseInt(lbl_id_credito.getText()));
        } else {
            lbl_id_credito.setText(DB_consultas_R_D.cargarId("creditos"));
            factura.setId(Integer.parseInt(DB_consultas_R_D.cargarId("creditos")));
        }

        factura.setTotal(Double.parseDouble(metodos.EliminaCaracteres(txt_total.getText(), ".")));
        factura.setInteres(Double.parseDouble(txt_interes.getText()));
        factura.setCodigo(txt_codigo.getText());
        factura.setDescripcion(txt_descripcion.getText());

        //cuenta
        try {
            factura.setId_cuenta(jbox_Cuentas.getItemAt(jbox_Cuentas.getSelectedIndex()).getId());

        } catch (Exception e) {
            factura.setId_cuenta(id_cuenta);
        }

        try {
            factura.setId_contacto(cbx_contacto.getItemAt(cbx_contacto.getSelectedIndex()).getId());
        } catch (Exception e) {
            factura.setId_contacto(id_cliente);
        }

        int dia, mes, ano;
        ano = jdate_fecha_creacion.getCalendar().get(Calendar.YEAR);
        mes = jdate_fecha_creacion.getCalendar().get(Calendar.MARCH) + 1;
        dia = jdate_fecha_creacion.getCalendar().get(Calendar.DAY_OF_MONTH);
        factura.setFecha_creacion(ano + "-" + mes + "-" + dia);

        ano = jdate_fecha_vencimiento.getCalendar().get(Calendar.YEAR);
        mes = jdate_fecha_vencimiento.getCalendar().get(Calendar.MARCH) + 1;
        dia = jdate_fecha_vencimiento.getCalendar().get(Calendar.DAY_OF_MONTH);
        factura.setFecha_vencimiento(ano + "-" + mes + "-" + dia);

        factura.setHora(DB_consultas_R_D.obtener_hora());
        factura.setEstado(1);
        factura.setId_user(frm_main.id_user);

        File folder = new File(DB_consultas_R_D.Ruta_Imagenes());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // Copia imagen a carpeta de red
        if (!nombreArchivoImagen.equals("")) {
            metodos.copyFile_Java7(ruta_origen_imagen, ruta_destino_nombre_archivo_imagen);
        }

        // Copia PDF a carpeta de red
        if (cargoPDF) {
            nombreArchivoPDF = lbl_id_credito.getText() + "_" + DB_consultas_R_D.obtener_fecha() + DB_consultas_R_D.obtener_hora_con_guiones() + nombreArchivoPDF;
            ruta_destino_nombre_archivo_PDF = DB_consultas_R_D.Ruta_Imagenes() + nombreArchivoPDF;
            metodos.copyFile_Java7(ruta_origen_PDF, ruta_destino_nombre_archivo_PDF);
        }

        factura.setFoto(nombreArchivoImagen);
        factura.setPDF(nombreArchivoPDF);

        if (DB_consultas_R_D.consultarId(lbl_id_credito.getText(), "creditos") == 1) {
            db_factura.Actualizar(factura);
        } else {
            db_factura.Guardar(factura);
        }
        if (tipo.equals("editar")) {
            jd_ver_creditos_cliente.btn_actualizar.doClick();
        } else {

            frm_Creditos.btn_actualizar.doClick();
        }

        return true;

    }
    private void btn_limpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_limpiarActionPerformed
        limpiar();
    }//GEN-LAST:event_btn_limpiarActionPerformed

    private void txt_totalFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_totalFocusGained
        if (!txt_total.getText().equals("")) {
            String texto = metodos.EliminaCaracteres(txt_total.getText(), ".");
            txt_total.setText(texto);
        }
    }//GEN-LAST:event_txt_totalFocusGained

    private void txt_totalFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_totalFocusLost
        if (!txt_total.getText().equals("")) {
            double valor = Double.parseDouble(txt_total.getText());
            String nuevo = metodos.formateador_dinero().format(valor);
            txt_total.setText(nuevo);
        }
    }//GEN-LAST:event_txt_totalFocusLost

    private void txt_totalKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_totalKeyTyped
        char num = evt.getKeyChar();
        DB_consultas_R_D.validar_numeros(evt, num);
        if ((num == KeyEvent.VK_ENTER)) {
            txt_total.requestFocus();
        }
    }//GEN-LAST:event_txt_totalKeyTyped

    private void panel_tituloMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panel_tituloMouseDragged
        this.this_mouseDragged(evt);
    }//GEN-LAST:event_panel_tituloMouseDragged

    private void panel_tituloMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panel_tituloMousePressed
        this.this_mouseDragged(evt);
    }//GEN-LAST:event_panel_tituloMousePressed

    private void txt_interesKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_interesKeyTyped
        char num = evt.getKeyChar();
        DB_consultas_R_D.validar_numeros(evt, num);
        if ((num == KeyEvent.VK_ENTER)) {
            txt_descripcion.requestFocus();
        }
    }//GEN-LAST:event_txt_interesKeyTyped

    private void txt_interesFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_interesFocusGained
        if (!txt_interes.getText().equals("")) {
            String texto = metodos.EliminaCaracteres(txt_interes.getText(), ".");
            txt_interes.setText(texto);
        }
    }//GEN-LAST:event_txt_interesFocusGained

    private void txt_interesFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_interesFocusLost
        if (!txt_interes.getText().equals("")) {
            double valor = Double.parseDouble(txt_interes.getText());
            String nuevo = metodos.formateador_un_decimal().format(valor);
            txt_interes.setText(nuevo);
        }
    }//GEN-LAST:event_txt_interesFocusLost
    public static String ruta_origen_imagen = "";
    public static String nombreArchivoImagen = "";
    public static String ruta_destino_nombre_archivo_imagen = "";
    public static String ruta_origen_PDF = "";
    public static String nombreArchivoPDF = "";
    public static String ruta_destino_nombre_archivo_PDF = "";
    public static boolean cargoPDF = false;


    private void btnAgregarImagenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarImagenActionPerformed

        ruta_origen_imagen = "";
        nombreArchivoImagen = "";
        ruta_destino_nombre_archivo_imagen = "";

        lbl_foto.setIcon(null);
//        SwingUtilities.invokeLater(() -> {

        JFileChooser j = new JFileChooser();
        j.setFileSelectionMode(JFileChooser.FILES_ONLY);//solo archivos y no carpetas
        FileNameExtensionFilter filtroImagen = new FileNameExtensionFilter("JPG, PNG & GIF", "jpg", "png", "gif");
        j.setFileFilter(filtroImagen);

        int estado = j.showOpenDialog(null);
        if (estado == JFileChooser.APPROVE_OPTION) {
            File fichero = j.getSelectedFile();
            ruta_origen_imagen = fichero.getAbsolutePath();
            nombreArchivoImagen = lbl_id_credito.getText() + "_" + DB_consultas_R_D.obtener_fecha() + DB_consultas_R_D.obtener_hora_con_guiones() + fichero.getName();
            ruta_destino_nombre_archivo_imagen = DB_consultas_R_D.Ruta_Imagenes() + nombreArchivoImagen;
            //                fis = new FileInputStream(j.getSelectedFile());
            //necesitamos saber la cantidad de bytes
            this.longitudBytes = (int) j.getSelectedFile().length();
            try {
                Image icono = ImageIO.read(j.getSelectedFile()).getScaledInstance(lbl_foto.getWidth(), lbl_foto.getHeight(), Image.SCALE_DEFAULT);
                lbl_foto.setIcon(new ImageIcon(icono));
                lbl_foto.updateUI();

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(rootPane, "imagen: " + ex);
            }
        }
//        });
    }//GEN-LAST:event_btnAgregarImagenActionPerformed

    private void btnAgregarImagen1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarImagen1ActionPerformed
        jd_Ver_PDF ver_pdf = new jd_Ver_PDF();
        jd_Ver_PDF.tipo = "credito";
        ver_pdf.show();
    }//GEN-LAST:event_btnAgregarImagen1ActionPerformed

    private void btn_Eliminar_ImagenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_Eliminar_ImagenActionPerformed
        String consulta = "select f.foto from creditos f where f.id =" + lbl_id_credito.getText();

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                try {

                    String ruta = DB_consultas_R_D.Ruta_Imagenes() + rs.getString("foto");
                    int dialogButton = JOptionPane.YES_NO_OPTION;
                    int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar esta imagen de este credito?\n" + ruta, "Alerta", dialogButton);
                    if (dialogResult == JOptionPane.YES_OPTION) {
                        DB_consultas_R_D.Eliminar_Archivo(ruta);
                        DB_consultas_R_D.Actualizar_Campo_String("creditos", "foto", lbl_id_credito.getText(), "");
                        lbl_foto.setIcon(null);
                        nombreArchivoImagen = "";
                        ruta_destino_nombre_archivo_imagen = "";
                        ruta_origen_imagen = "";
                        btn_Eliminar_Imagen.setEnabled(false);

                    }

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "No se pudo realizar la eliminacion");
                }

            }
            rs.close();

        } catch (Exception ex) {
            System.out.println(ex);
        }

    }//GEN-LAST:event_btn_Eliminar_ImagenActionPerformed

    private void btn_Eliminar_PDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_Eliminar_PDFActionPerformed
        String consulta = "select f.pdf from creditos f where f.id =" + lbl_id_credito.getText();

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                try {

                    String ruta = DB_consultas_R_D.Ruta_Imagenes() + rs.getString("pdf");
                    int dialogButton = JOptionPane.YES_NO_OPTION;
                    int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar el PDF de este credito?\n" + ruta, "Alerta", dialogButton);
                    if (dialogResult == JOptionPane.YES_OPTION) {
                        DB_consultas_R_D.Eliminar_Archivo(ruta);
                        DB_consultas_R_D.Actualizar_Campo_String("creditos", "pdf", lbl_id_credito.getText(), "");
                        nombreArchivoPDF = "";
                        btn_Eliminar_PDF.setEnabled(false);
                        cargoPDF = false;
                    }

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "No se pudo realizar la eliminacion");
                }

            }
            rs.close();

        } catch (Exception ex) {
            System.out.println(ex);
        }
    }//GEN-LAST:event_btn_Eliminar_PDFActionPerformed

    private void panel_titulo1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panel_titulo1MouseDragged
        // TODO add your handling code here:
    }//GEN-LAST:event_panel_titulo1MouseDragged

    private void panel_titulo1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panel_titulo1MousePressed
        // TODO add your handling code here:
    }//GEN-LAST:event_panel_titulo1MousePressed

    private void cbx_contactoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbx_contactoActionPerformed
        traer_cupo();


    }//GEN-LAST:event_cbx_contactoActionPerformed
    public static void traer_cupo() {
        try {
            try {
                id_cliente = (cbx_contacto.getItemAt(cbx_contacto.getSelectedIndex()).getId());

            } catch (Exception e) {
                System.out.println("Error en traer el id cliente del jbox: " + e);
            }
            System.out.println("Click " + id_cliente);

            String consulta = "select c.id as id_cliente, sum(f.total) as creditos, c.cupo, \n"
                    + "((coalesce(sum(f.total),0)-coalesce((select sum(total) from abonos_cabeceras where id_contacto=c.id),0))) as debe,  \n"
                    + "(c.cupo-((coalesce(sum(f.total),0)-coalesce((select sum(total) from abonos_cabeceras where id_contacto=c.id),0)))) as cupo_disponible  \n"
                    + "\n"
                    + "from contactos c left join creditos f on f.id_contacto=c.id \n"
                    + "\n"
                    + "where c.id=" + id_cliente + "\n"
                    + "\n"
                    + "group by c.id, c.nombre\n"
                    + "\n"
                    + "order by c.nombre";

//            System.out.println(consulta);
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);

            try {
                while (rs.next()) {
                    txt_cupo_aprobado.setText(metodos.formateador_dinero().format(rs.getDouble("cupo")));
                    lbl_cupo_usado.setText(metodos.formateador_dinero().format(rs.getDouble("debe")));
                    lbl_cupo_disponible.setText(metodos.formateador_dinero().format(rs.getDouble("cupo_disponible")));
                }
                rs.close();

            } catch (Exception e) {
                System.out.println(e);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    private void btn_agregarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_agregarActionPerformed

        jif_crear_contacto_credito frm = new jif_crear_contacto_credito();
        jif_crear_contacto_credito.txt_cedula.requestFocus();
        jif_crear_contacto_credito.formulario = "credito";
        frm.show();

    }//GEN-LAST:event_btn_agregarActionPerformed

    private void cbx_contactoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_cbx_contactoFocusLost
        traer_cupo();
    }//GEN-LAST:event_cbx_contactoFocusLost

    private void txt_cupo_aprobadoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_cupo_aprobadoKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_cupo_aprobadoKeyPressed

    private void txt_cupo_aprobadoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_cupo_aprobadoKeyTyped
        char num = evt.getKeyChar();
        DB_consultas_R_D.validar_numeros(evt, num);
        if ((num == KeyEvent.VK_ENTER)) {
            Connection con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = null;

            String sql = "update contactos set "
                    + "cupo=" + metodos.EliminaCaracteres(txt_cupo_aprobado.getText(), ".") + " "
                    + "where id=" + id_cliente;

            try {

                psql = con.prepareStatement(sql);
                psql.executeUpdate();
                psql.close();
                con.close();

                txt_cupo_aprobado.setEditable(false);
                JOptionPane.showMessageDialog(this, "Se actualizo el cupo para este cliente");
                traer_cupo();

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }//GEN-LAST:event_txt_cupo_aprobadoKeyTyped

    private void jdate_fecha_creacionFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jdate_fecha_creacionFocusLost

    }//GEN-LAST:event_jdate_fecha_creacionFocusLost

    private void btn_sumar30DiasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_sumar30DiasActionPerformed
        // Obtener la fecha seleccionada
        Date fechaCreacion = jdate_fecha_creacion.getDate();

        // Usar Calendar para sumar un mes
        Calendar cal = Calendar.getInstance();
        cal.setTime(fechaCreacion);
        cal.add(Calendar.MONTH, 1);

        // Establecer la nueva fecha en jdate_fecha_vencimiento
        Date fechaVencimiento = cal.getTime();
        jdate_fecha_vencimiento.setDate(fechaVencimiento);
    }//GEN-LAST:event_btn_sumar30DiasActionPerformed

    private void btn_editarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_editarActionPerformed
        txt_total.setEnabled(true);
        txt_codigo.setEnabled(true);
        txt_interes.setEnabled(true);
        txt_descripcion.setEnabled(true);
        jdate_fecha_creacion.setEnabled(true);
        cbx_contacto.setEnabled(true);
        btn_guardar.setEnabled(true);
        btn_limpiar.setEnabled(true);
        cbx_contacto.requestFocus();
    }//GEN-LAST:event_btn_editarActionPerformed
    private void btn_buscar_clienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_buscar_clienteActionPerformed
        Creditos.jd_buscar_cliente_credito buscador
                = new Creditos.jd_buscar_cliente_credito(null, true);
        buscador.setVisible(true);
    }//GEN-LAST:event_btn_buscar_clienteActionPerformed

    public static void agregarPDF() {

    }

    public static void limpiar() {
        txt_total.setText("");
        txt_interes.setText("");
        txt_codigo.setText("");
        txt_descripcion.setText("");
        Calendar fecha = new GregorianCalendar();
        jdate_fecha_creacion.setCalendar(fecha);
        jdate_fecha_vencimiento.setCalendar(fecha);
        cbx_contacto.setSelectedIndex(0);
        txt_total.requestFocus();
    }

    protected void this_mousePressed(MouseEvent e) {
        x = e.getX();
        y = e.getY();
    }

    /**
     *
     */
    protected void this_mouseDragged(MouseEvent e) {
        Point point = MouseInfo.getPointerInfo().getLocation();
        setLocation(point.x - x, point.y - y);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgregarImagen;
    private javax.swing.JButton btnAgregarImagen1;
    public static javax.swing.JButton btn_Eliminar_Imagen;
    public static javax.swing.JButton btn_Eliminar_PDF;
    private javax.swing.JButton btn_agregar;
    private javax.swing.JButton btn_buscar_cliente;
    public static javax.swing.JButton btn_editar;
    public static javax.swing.JButton btn_guardar;
    public static javax.swing.JButton btn_limpiar;
    private javax.swing.JButton btn_sumar30Dias;
    public static javax.swing.JComboBox<Contactos> cbx_contacto;
    public static javax.swing.JCheckBox chk_cerrar;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    public static javax.swing.JComboBox<Creditos.modelos.Cuentas> jbox_Cuentas;
    public static com.toedter.calendar.JDateChooser jdate_fecha_creacion;
    public static com.toedter.calendar.JDateChooser jdate_fecha_vencimiento;
    public static javax.swing.JLabel lbl_cupo_disponible;
    public static javax.swing.JLabel lbl_cupo_usado;
    public static javax.swing.JLabel lbl_foto;
    public static javax.swing.JLabel lbl_id_credito;
    public static javax.swing.JLabel lbl_titulo;
    public static javax.swing.JLabel lbl_titulo1;
    public static javax.swing.JLabel lbl_titulo4;
    public static javax.swing.JLabel lbl_titulo5;
    private javax.swing.JPanel panel_titulo;
    private javax.swing.JPanel panel_titulo1;
    public static javax.swing.JTextField txt_codigo;
    public static javax.swing.JTextField txt_cupo_aprobado;
    public static javax.swing.JTextArea txt_descripcion;
    public static javax.swing.JTextField txt_interes;
    public static javax.swing.JTextField txt_total;
    // End of variables declaration//GEN-END:variables
}
