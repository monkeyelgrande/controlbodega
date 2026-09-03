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
import Creditos.db.DBabonos;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import Creditos.modelos.Abonos;
import Creditos.modelos.AbonosCabecera;
import Creditos.modelos.Tipos_abonos;
import conexiondb.AuditoriaCaja;
import conexiondb.DBIngresos;
import modelos.Fondos;
import modelos.Ingresos;
import javax.swing.TransferHandler;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.io.File;

/**
 *
 * @author Monkeyelgrande
 */
public class jd_abonar_a_credito extends javax.swing.JDialog {

    /**
     * Creates new form jd_ver_devolucion
     */
    public static DefaultTableModel modelo_facturas = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };
    public static int id_tipo_abono;
    public static int id_cabecera_cargada = 0;
    public static String tipo;
    public static String nombreArchivoPDF = "";
    public static String ruta_destino_nombre_archivo_PDF = "";

    String ruta_origen_imagen = "";
    public static String nombreArchivoImagen = "";
    String ruta_destino_nombre_archivo_imagen = "";

    public static String ruta_ver_PDF = "";
    public static String ruta_ver_imagen = "";
    int longitudBytes;

    public jd_abonar_a_credito(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        // Estado limpio: el dialogo nace para un abono NUEVO; Cargar_abono
        // (ver/editar) sobreescribe estos estaticos despues de construirlo.
        tipo = "nuevo";
        id_cabecera_cargada = 0;
        nombreArchivoImagen = "";
        nombreArchivoPDF = "";
        cargoPDF = false;
        ruta_destino_nombre_archivo_PDF = "";
        ruta_ver_PDF = "";
        ruta_ver_imagen = "";

        this.setLocationRelativeTo(parent);
        metodos.addEscapeListenerWindowDialog(this);
        MetodosCreditos.EstiloTablaMaterialGlobalPequeno(jtabla_abonos);

        lbl_id_abono.setText(DB_consultas_R_D.cargarId("abonos"));
        TextPrompt prompt = new TextPrompt("Valor abono", txt_abono);
        if (frm_main.perfil != 1) {

        }
        Calendar fecha = new GregorianCalendar();
        jdate_fecha_creacion.setCalendar(fecha);

        Tipos_abonos ta = new Tipos_abonos();
        ta.mostrarAbonos(jbox_tipo_abono);
        Fondos.mostrarFondos(jbox_Fondos, jd_abonar_a_total.CAJA_CREDITOS);

        txt_abono.requestFocus();

        jtabla_abonos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    Cargar_abono();
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
                        nombreArchivoImagen = lbl_id_abono.getText() + "_"
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

    public static void Cargar_abono() {
        int fila = jtabla_abonos.getSelectedRow();
        String id = jtabla_abonos.getValueAt(fila, 0).toString();
        lbl_id_factura.setText(id);

        String consulta = "select a.id, a.fecha, a.abono, ca.id as id_cabecera, ca.observacion, ca.foto, coalesce(ca.pdf,'null') as pdf, \n"
                + "c.id as id_cliente, c.nombre as cliente, c.cedula, t.nombre as tipo_abono, t.id as id_tipo_abono \n"
                + "from abonos a \n"
                + "join abonos_cabeceras ca on a.id_cabecera=ca.id \n"
                + "join contactos c on ca.id_contacto=c.id \n"
                + "join tipos_abonos t on ca.id_tipo_abono=t.id \n"
                + "where a.id=" + id;
        System.out.println(consulta);
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                try {
                    jbox_tipo_abono.setSelectedItem(rs.getString("tipo_abono"));
                    if (rs.getString("foto").equals("")) {
                        btn_imagen.setEnabled(false);
                        btn_Eliminar_Imagen.setEnabled(false);
                        System.out.println("foto esta vacio");
                    } else {
                        String ruta = DB_consultas_R_D.Ruta_Imagenes() + rs.getString("foto");

                        Image img = new ImageIcon(ruta).getImage();

                        Image newimg = img.getScaledInstance(lbl_foto.getWidth(), lbl_foto.getHeight(), java.awt.Image.SCALE_SMOOTH);
                        ImageIcon newicon = new ImageIcon(newimg);

                        lbl_foto.setIcon(newicon);

                        ruta_ver_imagen = ruta;
                        btn_imagen.setEnabled(true);
                        btn_Eliminar_Imagen.setEnabled(true);
                    }

                    if (rs.getString("pdf").equals("null") || rs.getString("pdf").equals("")) {
                        btn_ver_pdf.setEnabled(false);
                        btn_Eliminar_PDF.setEnabled(false);
                        System.out.println("PDF esta vacio");
                    } else {
                        String ruta = DB_consultas_R_D.Ruta_Imagenes() + rs.getString("pdf");
                        ruta_ver_PDF = ruta;
                        btn_ver_pdf.setEnabled(true);
                        btn_Eliminar_PDF.setEnabled(true);

                    }
                    jbox_tipo_abono.setEnabled(false);
                    txt_abono.setEnabled(false);
                    btn_imprimir.setEnabled(true);
                    txt_observacion.setEnabled(false);
                    btn_guardar.setEnabled(false);
                    btnAgregarImagen.setEnabled(false);
                    btnAgregarImagen1.setEnabled(false);
                    btn_editar.setVisible(true);

                } catch (Exception e) {
                    System.out.println("NO se cargo la imagen");
                }

                lbl_id_abono.setText(rs.getString("id"));
                id_cabecera_cargada = rs.getInt("id_cabecera");
                jdate_fecha_creacion.setDate(rs.getDate("fecha"));
                lbl_cliente_nombre.setText(rs.getString("cliente"));
                lbl_cedula.setText(rs.getString("cedula"));
                txt_observacion.setText(rs.getString("observacion"));
                lbl_id_cliente.setText(rs.getString("id_cliente"));
                txt_abono.setText(metodos.formateador_dinero().format(rs.getDouble("abono")));
                jbox_tipo_abono.setSelectedItem(rs.getString("tipo_abono"));
                nombreArchivoImagen = rs.getString("foto");
                nombreArchivoPDF = rs.getString("pdf");
                id_tipo_abono = rs.getInt("id_tipo_abono");
//                                jd_abonar_a_total.lbl_total_saldo.setText(jtabla_creditos.getValueAt(fila, 6).toString());

            }
            rs.close();

        } catch (Exception ex) {
            System.out.println(ex);
        }

        btn_guardar.setText("Actualizar");
        tipo = "editar";

    }

    public void ver_imagen_de_abono() {

        String id = lbl_id_abono.getText();

        jif_ver_imagen_abono frm = new jif_ver_imagen_abono();
        String consulta = "select ca.foto, ca.observacion from abonos a join abonos_cabeceras ca on a.id_cabecera=ca.id where a.id =" + id;

        System.out.println(consulta);
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                try {

                    String ruta = DB_consultas_R_D.Ruta_Imagenes() + rs.getString("foto");

                    Image img = new ImageIcon(ruta).getImage();

                    Image newimg = img.getScaledInstance(frm.lbl_foto.getWidth(), frm.lbl_foto.getHeight(), java.awt.Image.SCALE_SMOOTH);
                    ImageIcon newicon = new ImageIcon(newimg);

                    frm.txt_observacion.setText(rs.getString("observacion"));

                    if (rs.getString("foto").equals("")) {

                        frm.lbl_foto.setText("NO HAY UNA IMÁGEN ASOCIADA A ESTE ABONO");
                    } else {

                        frm.lbl_foto.setIcon(newicon);
                    }
                    frm.ruta = ruta;
                } catch (Exception e) {
                    System.out.println("NO se cargo la imagen");
                }

            }
            rs.close();

        } catch (Exception ex) {
            Logger.getLogger(frm_Creditos.class.getName()).log(Level.SEVERE, null, ex);
        }
        frm.show();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        lbl_cliente_nombre = new javax.swing.JLabel();
        lbl_total_saldo = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        lbl_id_cliente = new javax.swing.JLabel();
        lbl_cedula = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        txt_abono = new javax.swing.JTextField();
        jlabelabonar = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        txt_observacion = new javax.swing.JTextArea();
        jlabelabonar1 = new javax.swing.JLabel();
        btnAgregarImagen = new javax.swing.JButton();
        lbl_foto = new javax.swing.JLabel();
        btnAgregarImagen1 = new javax.swing.JButton();
        jbox_tipo_abono = new javax.swing.JComboBox<>();
        jbox_Fondos = new javax.swing.JComboBox<>();
        btn_imprimir = new javax.swing.JButton();
        btn_ver_pdf = new javax.swing.JButton();
        btn_imagen = new javax.swing.JButton();
        btn_guardar = new javax.swing.JButton();
        btn_Eliminar_Imagen = new javax.swing.JButton();
        btn_Eliminar_PDF = new javax.swing.JButton();
        btn_editar = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jdate_fecha_creacion = new com.toedter.calendar.JDateChooser();
        jLabel7 = new javax.swing.JLabel();
        lbl_id_abono = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla_abonos = new javax.swing.JTable();
        lbl_cliente_nombre1 = new javax.swing.JLabel();
        lbl_id_factura = new javax.swing.JLabel();
        btn_eliminar = new javax.swing.JButton();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(jTable1);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Abonos");
        setModal(true);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(51, 51, 51));
        jLabel4.setText("Cliente");

        lbl_cliente_nombre.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lbl_cliente_nombre.setForeground(new java.awt.Color(0, 102, 102));
        lbl_cliente_nombre.setText("Cliente");

        lbl_total_saldo.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_total_saldo.setForeground(new java.awt.Color(204, 0, 0));
        lbl_total_saldo.setText("total");

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(51, 51, 51));
        jLabel10.setText("Total saldo:");

        lbl_id_cliente.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_id_cliente.setForeground(new java.awt.Color(255, 255, 242));
        lbl_id_cliente.setText("id_cliente");

        lbl_cedula.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lbl_cedula.setForeground(new java.awt.Color(0, 102, 102));
        lbl_cedula.setText("cedula");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(51, 51, 51));
        jLabel6.setText("Cédula");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_cliente_nombre))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_saldo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_id_cliente, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_cedula)))
                .addContainerGap(359, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(lbl_cliente_nombre))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(lbl_cedula))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_total_saldo)
                    .addComponent(jLabel10)
                    .addComponent(lbl_id_cliente))
                .addContainerGap())
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jPanel2.setBackground(new java.awt.Color(45, 54, 76));

        txt_abono.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        txt_abono.setForeground(new java.awt.Color(0, 153, 153));
        txt_abono.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_abonoFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_abonoFocusLost(evt);
            }
        });
        txt_abono.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_abonoKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_abonoKeyTyped(evt);
            }
        });

        jlabelabonar.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jlabelabonar.setForeground(new java.awt.Color(255, 255, 255));
        jlabelabonar.setText("Abonar");

        jButton1.setText("Abonar todo");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jScrollPane3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        txt_observacion.setColumns(20);
        txt_observacion.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txt_observacion.setLineWrap(true);
        txt_observacion.setRows(5);
        jScrollPane3.setViewportView(txt_observacion);

        jlabelabonar1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jlabelabonar1.setForeground(new java.awt.Color(255, 255, 255));
        jlabelabonar1.setText("Observación:");

        btnAgregarImagen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/clientes_pequeno.png"))); // NOI18N
        btnAgregarImagen.setText("Cargar Imágen");
        btnAgregarImagen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarImagenActionPerformed(evt);
            }
        });

        lbl_foto.setBackground(new java.awt.Color(255, 255, 255));
        lbl_foto.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255), 3));

        btnAgregarImagen1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/pdf.png"))); // NOI18N
        btnAgregarImagen1.setText("Cargar PFD");
        btnAgregarImagen1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarImagen1ActionPerformed(evt);
            }
        });

        jbox_Fondos.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jbox_Fondos.setToolTipText("Fondo de caja al que entra el dinero de este abono");

        jbox_tipo_abono.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jbox_tipo_abono.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbox_tipo_abonoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane3)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jlabelabonar)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jlabelabonar1)
                            .addComponent(txt_abono, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jbox_tipo_abono, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jbox_Fondos, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(btnAgregarImagen, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 244, Short.MAX_VALUE)
                        .addComponent(btnAgregarImagen1, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lbl_foto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton1)
                            .addComponent(jlabelabonar))
                        .addGap(10, 10, 10)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jbox_tipo_abono)
                            .addComponent(jbox_Fondos)
                            .addComponent(txt_abono, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jlabelabonar1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnAgregarImagen)
                            .addComponent(btnAgregarImagen1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_foto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        btn_imprimir.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_imprimir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/print_pequeno.png"))); // NOI18N
        btn_imprimir.setMnemonic('p');
        btn_imprimir.setText("Imprimir");
        btn_imprimir.setToolTipText("ATL+P");
        btn_imprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimirActionPerformed(evt);
            }
        });

        btn_ver_pdf.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        btn_ver_pdf.setText("VER PDF");
        btn_ver_pdf.setEnabled(false);
        btn_ver_pdf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ver_pdfActionPerformed(evt);
            }
        });

        btn_imagen.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        btn_imagen.setText("VER IMAGEN");
        btn_imagen.setEnabled(false);
        btn_imagen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imagenActionPerformed(evt);
            }
        });

        btn_guardar.setFont(new java.awt.Font("Tahoma", 0, 36)); // NOI18N
        btn_guardar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/guardar.png"))); // NOI18N
        btn_guardar.setMnemonic('g');
        btn_guardar.setText("Guardar");
        btn_guardar.setBorder(null);
        btn_guardar.setPreferredSize(new java.awt.Dimension(85, 30));
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

        btn_Eliminar_Imagen.setText("Eliminar Imágen");
        btn_Eliminar_Imagen.setEnabled(false);
        btn_Eliminar_Imagen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Eliminar_ImagenActionPerformed(evt);
            }
        });

        btn_Eliminar_PDF.setText("Eliminar PDF");
        btn_Eliminar_PDF.setEnabled(false);
        btn_Eliminar_PDF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Eliminar_PDFActionPerformed(evt);
            }
        });

        btn_editar.setText("Editar");
        btn_editar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_editarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(btn_guardar, javax.swing.GroupLayout.PREFERRED_SIZE, 431, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_ver_pdf, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_Eliminar_PDF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btn_Eliminar_Imagen, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_imagen))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btn_imprimir, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_editar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(btn_guardar, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_ver_pdf)
                            .addComponent(btn_imprimir, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_imagen))
                        .addGap(4, 4, 4)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_Eliminar_Imagen)
                            .addComponent(btn_Eliminar_PDF)
                            .addComponent(btn_editar))
                        .addContainerGap())))
        );

        jPanel4.setBackground(new java.awt.Color(204, 255, 204));
        jPanel4.setForeground(new java.awt.Color(255, 255, 204));

        jdate_fecha_creacion.setForeground(new java.awt.Color(0, 153, 51));
        jdate_fecha_creacion.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(51, 51, 51));
        jLabel7.setText("Fecha del abono");

        lbl_id_abono.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lbl_id_abono.setForeground(new java.awt.Color(51, 51, 51));
        lbl_id_abono.setText("-");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jdate_fecha_creacion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(lbl_id_abono)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jdate_fecha_creacion, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lbl_id_abono))
        );

        jPanel5.setBackground(new java.awt.Color(255, 255, 204));

        jtabla_abonos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_abonos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(jtabla_abonos);

        lbl_cliente_nombre1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_cliente_nombre1.setForeground(new java.awt.Color(0, 102, 102));
        lbl_cliente_nombre1.setText("Abonos realizados a la factura");

        lbl_id_factura.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_id_factura.setForeground(new java.awt.Color(51, 51, 51));
        lbl_id_factura.setText("-");

        btn_eliminar.setText("Eliminar abono");
        btn_eliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_eliminarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 968, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(lbl_cliente_nombre1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_id_factura)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_eliminar)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_cliente_nombre1)
                    .addComponent(lbl_id_factura)
                    .addComponent(btn_eliminar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 285, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txt_abonoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_abonoFocusGained
        if (!txt_abono.getText().equals("")) {
            String texto = metodos.EliminaCaracteres(txt_abono.getText(), ".");
            txt_abono.setText(texto);
        }
    }//GEN-LAST:event_txt_abonoFocusGained

    private void txt_abonoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_abonoFocusLost
        if (!txt_abono.getText().equals("")) {
            try {
                double to = Double.parseDouble(txt_abono.getText());
                String nuevo = metodos.formateador_dinero().format(to);
                txt_abono.setText(nuevo);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Por favor verifique se que halla ingresado un valor correcto");
            }
        }
    }//GEN-LAST:event_txt_abonoFocusLost

    private void txt_abonoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_abonoKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            btn_guardar.requestFocus();
        }
    }//GEN-LAST:event_txt_abonoKeyPressed

    private void txt_abonoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_abonoKeyTyped
        char num = evt.getKeyChar();
        DB_consultas_R_D.validar_numeros(evt, num);
    }//GEN-LAST:event_txt_abonoKeyTyped

    private void btn_guardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_guardarActionPerformed
        // el boton se bloquea mientras se procesa: un doble clic aqui creaba
        // pagos duplicados
        btn_guardar.setEnabled(false);
        try {
            guardarAbono();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el abono:\n" + e,
                    "Error al guardar", JOptionPane.ERROR_MESSAGE);
        } finally {
            btn_guardar.setEnabled(true);
        }
    }//GEN-LAST:event_btn_guardarActionPerformed

    private void guardarAbono() {
        if (txt_abono.getText().isEmpty() || txt_abono.getText().equals("0")) {
            txt_abono.setBackground(Color.pink);
            JOptionPane.showMessageDialog(this, "El valor ingresado no puede estar vacio");
            return;
        }
        if (!validaSaldo()) {
            return;
        }

        double abono_dinero = Double.parseDouble(metodos.EliminaCaracteres(txt_abono.getText(), "."));
        DBabonos dbabono = new DBabonos();

        if (jbox_tipo_abono.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay tipos de abono creados. Cree al menos uno en Creditos > Tipos de abonos antes de registrar pagos.");
            return;
        }
        if (jbox_tipo_abono.getSelectedIndex() < 0) {
            jbox_tipo_abono.setSelectedIndex(0);
        }
        int id_tipo;
        try {
            id_tipo = jbox_tipo_abono.getItemAt(jbox_tipo_abono.getSelectedIndex()).getId();
        } catch (Exception e) {
            id_tipo = id_tipo_abono;
            JOptionPane.showMessageDialog(this, "Por favor seleccione un tipo de abono");
            return;
        }

        // fecha en la que se recibio el pago
        int dia, mes, ano;
        ano = jdate_fecha_creacion.getCalendar().get(Calendar.YEAR);
        mes = jdate_fecha_creacion.getCalendar().get(Calendar.MONTH) + 1;
        dia = jdate_fecha_creacion.getCalendar().get(Calendar.DAY_OF_MONTH);
        String fecha = ano + "-" + mes + "-" + dia;
        String hora = DB_consultas_R_D.obtener_hora();

        // imagen
        if (!nombreArchivoImagen.equals("")) {
            metodos.copyFile_Java7(ruta_origen_imagen, ruta_destino_nombre_archivo_imagen);
        }

        // PDF: copia a carpeta de red
        if (cargoPDF) {
            nombreArchivoPDF = lbl_id_abono.getText() + "_" + DB_consultas_R_D.obtener_fecha() + "_" + DB_consultas_R_D.obtener_hora_con_guiones() + nombreArchivoPDF;
            ruta_destino_nombre_archivo_PDF = DB_consultas_R_D.Ruta_Imagenes() + nombreArchivoPDF;
            metodos.copyFile_Java7(ruta_origen_PDF, ruta_destino_nombre_archivo_PDF);
        }
        if (nombreArchivoPDF == null) {
            nombreArchivoPDF = "";
        }

        if ("editar".equals(tipo)) {
            // edicion de un abono existente: ajusta el detalle y su cabecera
            int filas = dbabono.ActualizarDetalleYCabecera(Integer.parseInt(lbl_id_abono.getText()), abono_dinero, fecha, hora,
                    id_tipo, txt_observacion.getText(), nombreArchivoImagen, nombreArchivoPDF);
            if (filas == 0) {
                JOptionPane.showMessageDialog(this, "No se encontró el abono a actualizar (id "
                        + lbl_id_abono.getText() + "). No se guardó nada.");
                return;
            }

            // El ingreso de Caja tiene que seguir al abono editado. El total se
            // relee de la base: ActualizarDetalleYCabecera ajusta la cabecera
            // por la diferencia, asi que no tiene por que valer lo mismo que el
            // detalle que se acaba de editar (la cabecera puede repartirse entre
            // varios creditos o dejar saldo a favor).
            AbonosCabecera editada = new AbonosCabecera();
            editada.setId(id_cabecera_cargada);
            editada.setId_contacto(Integer.parseInt(lbl_id_cliente.getText()));
            editada.setId_tipo_abono(id_tipo);
            editada.setTotal(abono_dinero);
            editada.setFecha(fecha);
            editada.setHora(hora);
            editada.setObservacion(txt_observacion.getText());
            try (java.sql.ResultSet rsc = DB_consultas_R_D.getTabla(
                    "select total, fecha, observacion, id_tipo_abono from abonos_cabeceras where id = "
                    + id_cabecera_cargada)) {
                if (rsc.next()) {
                    editada.setTotal(rsc.getDouble("total"));
                    editada.setFecha(rsc.getString("fecha"));
                    editada.setObservacion(rsc.getString("observacion"));
                    editada.setId_tipo_abono(rsc.getInt("id_tipo_abono"));
                }
            } catch (Exception e) {
                System.out.println(e);
            }
            sincronizar_ingreso(editada, vendedorDelCredito(Integer.parseInt(lbl_id_factura.getText())));
        } else {
            // nuevo abono a este credito: cabecera con un solo detalle
            AbonosCabecera cabecera = new AbonosCabecera();
            cabecera.setId_user(frm_main.id_user);
            cabecera.setId_contacto(Integer.parseInt(lbl_id_cliente.getText()));
            cabecera.setId_tipo_abono(id_tipo);
            cabecera.setTotal(abono_dinero);
            cabecera.setFecha(fecha);
            cabecera.setHora(hora);
            cabecera.setObservacion(txt_observacion.getText());
            cabecera.setFoto(nombreArchivoImagen);
            cabecera.setPDF(nombreArchivoPDF);

            java.util.List<Abonos> detalles = new java.util.ArrayList<>();
            detalles.add(new Abonos(Integer.parseInt(lbl_id_factura.getText()), abono_dinero, fecha, hora));

            int idCabecera = dbabono.GuardarPago(cabecera, detalles);
            if (idCabecera == 0) {
                return;
            }
            id_cabecera_cargada = idCabecera;

            // El dinero recibido entra a Caja si el tipo de abono asi lo indica.
            guardar_ingreso(cabecera, vendedorDelCredito(Integer.parseInt(lbl_id_factura.getText())));
        }

        int dialogButton = JOptionPane.YES_NO_OPTION;
        int dialogResult = JOptionPane.showConfirmDialog(null, "Desea imprimir?", "Alerta", dialogButton);
        if (dialogResult == JOptionPane.YES_OPTION) {
            btn_imprimir.setEnabled(true);
            btn_imprimir.doClick();
        }

        try {
            frm_Creditos.btn_actualizar.doClick();
        } catch (Exception e) {
        }
        try {
            jd_ver_creditos_cliente.btn_actualizar.doClick();
        } catch (Exception e) {
        }
        this.dispose();
    }

    public boolean validaSaldo() {
        double saldo = Double.parseDouble(metodos.EliminaCaracteres(lbl_total_saldo.getText(), "."));
        double abono = Double.parseDouble(metodos.EliminaCaracteres(txt_abono.getText(), "."));
        if (abono > saldo) {
            if ("editar".equals(tipo)) {

                return true;
            } else {
                JOptionPane.showMessageDialog(this, "El abono ingresado supera el saldo de la factura \nVerifique los valores ingresados");
                txt_abono.setText("");
                txt_abono.requestFocus();
                return false;
            }
        }
        return true;
    }
    private void btn_guardarKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_btn_guardarKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            btn_guardarActionPerformed(null);
        }
    }//GEN-LAST:event_btn_guardarKeyPressed

    private void btn_imprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imprimirActionPerformed
        try {
            new Creditos.ImprimirReciboPDF().imprimir(id_cabecera_cargada);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo imprimir el recibo:\n" + e,
                    "Impresion", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btn_imprimirActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        txt_abono.setText(lbl_total_saldo.getText());
    }//GEN-LAST:event_jButton1ActionPerformed

    private void btn_imagenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imagenActionPerformed

        jif_ver_imagen frm = new jif_ver_imagen();
        String consulta = "select f.foto from creditos f where f.id =" + lbl_id_abono.getText();

        System.out.println(consulta);
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                try {

                    Image img = new ImageIcon(ruta_ver_imagen).getImage();

                    Image newimg = img.getScaledInstance(frm.lbl_foto.getWidth(), frm.lbl_foto.getHeight(), java.awt.Image.SCALE_SMOOTH);
                    ImageIcon newicon = new ImageIcon(newimg);

                    frm.lbl_foto.setIcon(newicon);
                    frm.ruta = ruta_ver_imagen;
                } catch (Exception e) {
                    System.out.println("NO se cargo la imagen");
                }

            }
            rs.close();

        } catch (Exception ex) {
            Logger.getLogger(frm_Creditos.class.getName()).log(Level.SEVERE, null, ex);
        }
        frm.show();
    }//GEN-LAST:event_btn_imagenActionPerformed

    private void btn_ver_pdfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ver_pdfActionPerformed
        try {
            DB_consultas_R_D.Abrir_Archivo(ruta_ver_PDF);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se ha podido cargar ningun PDF");
        }


    }//GEN-LAST:event_btn_ver_pdfActionPerformed

    private void btnAgregarImagenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarImagenActionPerformed

        ruta_origen_imagen = "";
        nombreArchivoImagen = "";
        ruta_destino_nombre_archivo_imagen = "";

        lbl_foto.setIcon(null);

        JFileChooser j = new JFileChooser();
        j.setFileSelectionMode(JFileChooser.FILES_ONLY);//solo archivos y no carpetas
        FileNameExtensionFilter filtroImagen = new FileNameExtensionFilter("JPG, PNG & GIF", "jpg", "png", "gif");
        j.setFileFilter(filtroImagen);

        int estado = j.showOpenDialog(null);
        if (estado == JFileChooser.APPROVE_OPTION) {
            File fichero = j.getSelectedFile();
            ruta_origen_imagen = fichero.getAbsolutePath();
            nombreArchivoImagen = DB_consultas_R_D.obtener_fecha() + DB_consultas_R_D.obtener_hora_con_guiones() + fichero.getName();
            ruta_destino_nombre_archivo_imagen = DB_consultas_R_D.Ruta_Imagenes() + nombreArchivoImagen;

            try {
                Image icono = ImageIO.read(j.getSelectedFile()).getScaledInstance(lbl_foto.getWidth(), lbl_foto.getHeight(), Image.SCALE_DEFAULT);
                lbl_foto.setIcon(new ImageIcon(icono));
                lbl_foto.updateUI();

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(rootPane, "imagen: " + ex);
            }
        }
    }//GEN-LAST:event_btnAgregarImagenActionPerformed
    public static String ruta_origen_PDF = "";
    public static boolean cargoPDF = false;

    private void btnAgregarImagen1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarImagen1ActionPerformed
        jd_Ver_PDF ver_pdf = new jd_Ver_PDF();
        ver_pdf.show();
    }//GEN-LAST:event_btnAgregarImagen1ActionPerformed

    private void jbox_tipo_abonoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbox_tipo_abonoActionPerformed
        // El fondo solo importa si este tipo de abono entra a Caja.
        try {
            jbox_Fondos.setEnabled(DBabonos.agregar_a_ingreso(
                    jbox_tipo_abono.getItemAt(jbox_tipo_abono.getSelectedIndex()).getId()));
        } catch (Exception e) {
            jbox_Fondos.setEnabled(false);
        }
    }//GEN-LAST:event_jbox_tipo_abonoActionPerformed

    /**
     * Registra en Caja el ingreso que genera este abono, si el tipo de abono
     * asi lo pide. El abono ya quedo guardado antes de llegar aqui: si el
     * ingreso falla se avisa, pero no se deshace la cartera.
     */
    private void guardar_ingreso(AbonosCabecera cabecera, int idVendedor) {
        if (!DBabonos.agregar_a_ingreso(cabecera.getId_tipo_abono())) {
            return;
        }
        AuditoriaCaja.setOrigen("Creditos - abono a credito (ingreso a caja)");
        try {
            new DBIngresos().Guardar_desde_abono_credito(ingresoDe(cabecera, idVendedor), cabecera.getId());
        } finally {
            AuditoriaCaja.limpiar();
        }
    }

    /**
     * Deja el ingreso de Caja en linea con un abono que se acaba de editar:
     * corrige el valor, lo crea si el tipo de abono paso a entrar a Caja, o lo
     * borra si dejo de hacerlo.
     */
    private void sincronizar_ingreso(AbonosCabecera cabecera, int idVendedor) {
        AuditoriaCaja.setOrigen("Creditos - editar abono (ingreso a caja)");
        try {
            new DBIngresos().Sincronizar_ingreso_de_abono(ingresoDe(cabecera, idVendedor), cabecera.getId(),
                    DBabonos.agregar_a_ingreso(cabecera.getId_tipo_abono()));
        } finally {
            AuditoriaCaja.limpiar();
        }
    }

    /** Traduce la cabecera del pago al ingreso de Caja que le corresponde. */
    private Ingresos ingresoDe(AbonosCabecera cabecera, int idVendedor) {
        Ingresos obj = new Ingresos();
        obj.setDescripcion("Abono a credito #" + cabecera.getId()
                + (cabecera.getObservacion() == null || cabecera.getObservacion().isEmpty()
                        ? "" : " - " + cabecera.getObservacion()));
        obj.setFecha(cabecera.getFecha());
        obj.setHora(DB_consultas_R_D.obtener_hora());
        obj.setId_user(frm_main.id_user);
        obj.setTotal(cabecera.getTotal());
        obj.setId_cliente(cabecera.getId_contacto());
        obj.setId_caja(jd_abonar_a_total.CAJA_CREDITOS);
        obj.setFactura_remision(0);
        obj.setId_vendedor(idVendedor);

        try {
            obj.setId_fondo(jbox_Fondos.getItemAt(jbox_Fondos.getSelectedIndex()).getId());
        } catch (Exception e) {
            obj.setId_fondo(Fondos.TraerPredeterminado(jd_abonar_a_total.CAJA_CREDITOS));
        }

        return obj;
    }

    /** Vendedor del credito al que se esta abonando; 0 si no tiene. */
    private static int vendedorDelCredito(int idCredito) {
        try (java.sql.ResultSet rs = DB_consultas_R_D.getTabla(
                "select id_empleado from creditos where id = " + idCredito)) {
            if (rs.next()) {
                return rs.getInt("id_empleado");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return 0;
    }

    private void btn_Eliminar_ImagenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_Eliminar_ImagenActionPerformed

        try {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar esta imagen de este credito?\n" + ruta_ver_imagen, "Alerta", dialogButton);
            if (dialogResult == JOptionPane.YES_OPTION) {
                DB_consultas_R_D.Eliminar_Archivo(ruta_ver_imagen);
                DB_consultas_R_D.Actualizar_Campo_String("abonos_cabeceras", "foto", String.valueOf(id_cabecera_cargada), "");
                lbl_foto.setIcon(null);
                nombreArchivoImagen = "";
                ruta_destino_nombre_archivo_imagen = "";
                ruta_origen_imagen = "";
                btn_Eliminar_Imagen.setEnabled(false);
            }
        } catch (SQLException ex) {
            Logger.getLogger(jd_abonar_a_credito.class.getName()).log(Level.SEVERE, null, ex);
        }


    }//GEN-LAST:event_btn_Eliminar_ImagenActionPerformed

    private void btn_Eliminar_PDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_Eliminar_PDFActionPerformed
        try {

            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar el PDF de este credito?\n" + ruta_ver_PDF, "Alerta", dialogButton);
            if (dialogResult == JOptionPane.YES_OPTION) {
                DB_consultas_R_D.Eliminar_Archivo(ruta_ver_PDF);
                DB_consultas_R_D.Actualizar_Campo_String("abonos_cabeceras", "pdf", String.valueOf(id_cabecera_cargada), "");
                nombreArchivoPDF = "";
                btn_Eliminar_PDF.setEnabled(false);

            }
        } catch (SQLException ex) {
            Logger.getLogger(jd_abonar_a_credito.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btn_Eliminar_PDFActionPerformed

    private void btn_editarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_editarActionPerformed

        jbox_tipo_abono.setEnabled(true);
        txt_abono.setEnabled(true);
        btn_imprimir.setEnabled(false);
        txt_observacion.setEnabled(true);
        btn_guardar.setEnabled(true);
        btnAgregarImagen.setEnabled(true);
        btnAgregarImagen1.setEnabled(true);
        btn_editar.setVisible(false);
    }//GEN-LAST:event_btn_editarActionPerformed

    private void btn_eliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_eliminarActionPerformed
        int fila = jtabla_abonos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
            return;
        }

        int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar este abono?\n"
                + "El valor eliminado quedará como saldo a favor del pago (cabecera) al que pertenece", "Alerta", JOptionPane.YES_NO_OPTION);
        if (dialogResult != JOptionPane.YES_OPTION) {
            return;
        }

        DefaultTableModel modelo = (DefaultTableModel) jtabla_abonos.getModel();
        String id = (String) jtabla_abonos.getValueAt(fila, 0);

        if (!DB_consultas_R_D.validar_admin("Creditos", "Eliminar", "Se elimino el abono con el id: " + id)) {
            return;
        }

        try {
            DB_consultas_R_D.eliminar("abonos", id);
            for (int i = 0; i < modelo.getRowCount(); i++) {
                if (modelo.getValueAt(i, 0).equals(id)) {
                    modelo.removeRow(i);
                    break;
                }
            }
            jd_ver_creditos_cliente.btn_actualizar.doClick();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_btn_eliminarActionPerformed
    public void limpiar() {
        txt_abono.setText("");
        txt_abono.setBackground(Color.white);
        txt_abono.requestFocus();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(jd_abonar_a_credito.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(jd_abonar_a_credito.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(jd_abonar_a_credito.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(jd_abonar_a_credito.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>


        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                jd_abonar_a_credito dialog = new jd_abonar_a_credito(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public static javax.swing.JButton btnAgregarImagen;
    public static javax.swing.JButton btnAgregarImagen1;
    public static javax.swing.JButton btn_Eliminar_Imagen;
    public static javax.swing.JButton btn_Eliminar_PDF;
    public static javax.swing.JButton btn_editar;
    public static javax.swing.JButton btn_eliminar;
    public static javax.swing.JButton btn_guardar;
    public static javax.swing.JButton btn_imagen;
    public static javax.swing.JButton btn_imprimir;
    public static javax.swing.JButton btn_ver_pdf;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel4;
    public static javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    public static javax.swing.JComboBox<Fondos> jbox_Fondos;
    public static javax.swing.JComboBox<Tipos_abonos> jbox_tipo_abono;
    public static com.toedter.calendar.JDateChooser jdate_fecha_creacion;
    public static javax.swing.JLabel jlabelabonar;
    public static javax.swing.JLabel jlabelabonar1;
    public static javax.swing.JTable jtabla_abonos;
    public static javax.swing.JLabel lbl_cedula;
    public static javax.swing.JLabel lbl_cliente_nombre;
    public static javax.swing.JLabel lbl_cliente_nombre1;
    public static javax.swing.JLabel lbl_foto;
    public static javax.swing.JLabel lbl_id_abono;
    public static javax.swing.JLabel lbl_id_cliente;
    public static javax.swing.JLabel lbl_id_factura;
    public static javax.swing.JLabel lbl_total_saldo;
    public static javax.swing.JTextField txt_abono;
    public static javax.swing.JTextArea txt_observacion;
    // End of variables declaration//GEN-END:variables
}
