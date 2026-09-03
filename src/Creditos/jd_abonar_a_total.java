/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos;

import Creditos.frm_Creditos;
import Formularios.frm_main;
import Creditos.CellRendererAbonoATotal;
import Metodos.TextPrompt;
import Metodos.metodos;
import com.ezware.oxbow.swingbits.table.filter.TableRowFilterSupport;
import conexiondb.DB_consultas_R_D;
import Creditos.db.DBabonos;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import Creditos.modelos.Abonos;
import Creditos.modelos.AbonosCabecera;
import conexiondb.AuditoriaCaja;
import conexiondb.DBIngresos;
import modelos.Fondos;
import modelos.Ingresos;
import Creditos.modelos.Tipos_abonos;

/**
 *
 * @author Monkeyelgrande
 */
public class jd_abonar_a_total extends javax.swing.JDialog {

    /**
     * Creates new form jd_ver_devolucion
     */
    public static DefaultTableModel modelo_tabla = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };
    CellRendererAbonoATotal myRenderer = new CellRendererAbonoATotal();

    public static int id_tipo_abono;
    public static String tipo;
    public static String nombreArchivoPDF = "";
    public static String ruta_destino_nombre_archivo_PDF = "";

    String ruta_origen_imagen = "";
    public static String nombreArchivoImagen = "";
    String ruta_destino_nombre_archivo_imagen = "";

    public static String ruta_ver_PDF = "";
    public static String ruta_ver_imagen = "";

    public jd_abonar_a_total(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        // Estado limpio: este dialogo nace para un abono NUEVO. Los flujos de
        // ver/editar sobreescriben estos estaticos DESPUES de construirlo.
        // Sin este reset, un "editar" previo dejaba tipo="editar" pegado y el
        // siguiente abono nuevo se convertia en un UPDATE a un id inexistente
        // (se perdia en silencio hasta reiniciar la aplicacion).
        tipo = "nuevo";
        nombreArchivoImagen = "";
        nombreArchivoPDF = "";
        cargoPDF = false;
        ruta_origen_PDF = "";
        ruta_destino_nombre_archivo_PDF = "";
        ruta_ver_PDF = "";
        ruta_ver_imagen = "";

        chk_anticipo.setVisible(false);

        jtabla.setDefaultRenderer(Object.class, myRenderer);
        TableRowFilterSupport.forTable(jtabla).searchable(true).apply();

        this.setLocationRelativeTo(parent);
        metodos.addEscapeListenerWindowDialog(this);
        lbl_id_abono.setText(DB_consultas_R_D.cargarId("abonos_cabeceras"));
        TextPrompt prompt = new TextPrompt("Valor abono", txt_abono);
        if (frm_main.perfil != 1) {

        }
        Calendar fecha = new GregorianCalendar();
        jdate_fecha_creacion.setCalendar(fecha);

        Tipos_abonos ta = new Tipos_abonos();
        ta.mostrarAbonos(jbox_tipo_abono);
        Fondos.mostrarFondos(jbox_Fondos, CAJA_CREDITOS);

        txt_abono.requestFocus();

        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 1) {
                    int fila = jtabla.getSelectedRow();
                    if (fila < 0) {
                        return;
                    }
                    if (chk_abono_espesifico.isSelected()) {
                        // Obtener el valor actual de la columna 8
                        String abono = jtabla.getValueAt(fila, 8).toString();
                        int colCount = modelo_tabla.getColumnCount();

                        // Extraer los datos de la fila en un arreglo de Objects
                        Object[] rowData = new Object[colCount];
                        for (int i = 0; i < colCount; i++) {
                            rowData[i] = modelo_tabla.getValueAt(fila, i);
                        }

                        // Eliminar la fila actual del modelo
                        modelo_tabla.removeRow(fila);

                        if (abono.equals("Global")) {
                            // Cambiar a "Abonar"
                            rowData[8] = "Abonar";

                            // Calcular el índice de inserción: justo después de la última fila con "Abonar"
                            int insertIndex = 0;
                            int totalFilas = modelo_tabla.getRowCount();
                            for (int i = 0; i < totalFilas; i++) {
                                if ("Abonar".equals(modelo_tabla.getValueAt(i, 8))) {
                                    insertIndex = i + 1;
                                }
                            }

                            // Insertar la fila en la posición determinada
                            modelo_tabla.insertRow(insertIndex, rowData);
                        } else {
                            // Si ya era "Abonar", se cambia a "Global" y se mueve al final
                            rowData[8] = "Global";
                            modelo_tabla.addRow(rowData);
                        }

                        sumar_seleccionados();
                    }
                }
            }
        });

    }

    public void ver_imagen_de_abono() {

        String id = lbl_id_abono.getText();

        jif_ver_imagen_abono frm = new jif_ver_imagen_abono();
        String consulta = "select foto, observacion from abonos_cabeceras where id =" + id;

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

        buttonGroup1 = new javax.swing.ButtonGroup();
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
        chk_abono_espesifico = new javax.swing.JCheckBox();
        chk_anticipo = new javax.swing.JCheckBox();
        jSeparator1 = new javax.swing.JSeparator();
        btn_imprimir = new javax.swing.JButton();
        btn_ver_pdf = new javax.swing.JButton();
        btn_imagen = new javax.swing.JButton();
        btn_guardar = new javax.swing.JButton();
        btn_Eliminar_Imagen = new javax.swing.JButton();
        btn_Eliminar_PDF = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jdate_fecha_creacion = new com.toedter.calendar.JDateChooser();
        jLabel7 = new javax.swing.JLabel();
        lbl_id_abono = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();
        lbl_total_seleccionado = new javax.swing.JLabel();
        lbl_total_titulo = new javax.swing.JLabel();
        btn_seleccionar_todas_las_facturas = new javax.swing.JButton();
        lbl_notificacion = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Abonos");
        setModal(true);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(51, 51, 51));
        jLabel4.setText("Cliente");

        lbl_cliente_nombre.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_cliente_nombre.setForeground(new java.awt.Color(255, 51, 0));
        lbl_cliente_nombre.setText("Cliente");

        lbl_total_saldo.setFont(new java.awt.Font("Tahoma", 1, 20)); // NOI18N
        lbl_total_saldo.setForeground(new java.awt.Color(204, 0, 0));
        lbl_total_saldo.setText("total");
        lbl_total_saldo.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lbl_total_saldoMouseClicked(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(51, 51, 51));
        jLabel10.setText("Total saldo:");

        lbl_id_cliente.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_id_cliente.setForeground(new java.awt.Color(255, 255, 242));
        lbl_id_cliente.setText("id_cliente");

        lbl_cedula.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
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
                        .addComponent(lbl_cliente_nombre, javax.swing.GroupLayout.PREFERRED_SIZE, 330, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_cedula, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_saldo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_id_cliente, javax.swing.GroupLayout.PREFERRED_SIZE, 11, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(lbl_cliente_nombre)
                    .addComponent(jLabel6)
                    .addComponent(lbl_cedula))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_total_saldo)
                    .addComponent(jLabel10)
                    .addComponent(lbl_id_cliente))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jPanel2.setBackground(new java.awt.Color(45, 54, 76));

        txt_abono.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
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

        chk_abono_espesifico.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        chk_abono_espesifico.setForeground(new java.awt.Color(255, 255, 255));
        chk_abono_espesifico.setText("Abonar a créditos especificos");
        chk_abono_espesifico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chk_abono_espesificoActionPerformed(evt);
            }
        });

        chk_anticipo.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        chk_anticipo.setForeground(new java.awt.Color(255, 255, 255));
        chk_anticipo.setText("¿Anticipo?");
        chk_anticipo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chk_anticipoActionPerformed(evt);
            }
        });

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(chk_anticipo))
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jbox_tipo_abono, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jbox_Fondos, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addComponent(chk_abono_espesifico))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 421, Short.MAX_VALUE)
                        .addComponent(btnAgregarImagen, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnAgregarImagen1, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lbl_foto, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton1)
                            .addComponent(jlabelabonar))
                        .addGap(9, 9, 9)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jbox_tipo_abono)
                            .addComponent(jbox_Fondos)
                            .addComponent(txt_abono, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jlabelabonar1)
                            .addComponent(chk_anticipo))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(chk_abono_espesifico))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnAgregarImagen1)
                            .addComponent(btnAgregarImagen))
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

        btn_ver_pdf.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_ver_pdf.setText("VER PDF");
        btn_ver_pdf.setEnabled(false);
        btn_ver_pdf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ver_pdfActionPerformed(evt);
            }
        });

        btn_imagen.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_imagen.setText("VER IMAGEN");
        btn_imagen.setEnabled(false);
        btn_imagen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imagenActionPerformed(evt);
            }
        });

        btn_guardar.setBackground(new java.awt.Color(0, 204, 204));
        btn_guardar.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_guardar.setForeground(new java.awt.Color(51, 51, 51));
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

        btn_Eliminar_Imagen.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_Eliminar_Imagen.setText("Eliminar Imágen");
        btn_Eliminar_Imagen.setEnabled(false);
        btn_Eliminar_Imagen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Eliminar_ImagenActionPerformed(evt);
            }
        });

        btn_Eliminar_PDF.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_Eliminar_PDF.setText("Eliminar PDF");
        btn_Eliminar_PDF.setEnabled(false);
        btn_Eliminar_PDF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_Eliminar_PDFActionPerformed(evt);
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_ver_pdf)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Eliminar_PDF)
                        .addGap(18, 18, 18)
                        .addComponent(btn_imagen)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Eliminar_Imagen)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_imprimir))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_guardar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btn_ver_pdf)
                        .addComponent(btn_Eliminar_PDF)
                        .addComponent(btn_imagen)
                        .addComponent(btn_Eliminar_Imagen)
                        .addComponent(btn_imprimir, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(204, 255, 204));
        jPanel4.setForeground(new java.awt.Color(255, 255, 204));

        jdate_fecha_creacion.setForeground(new java.awt.Color(0, 153, 51));
        jdate_fecha_creacion.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(51, 51, 51));
        jLabel7.setText("Fecha de registro");

        lbl_id_abono.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lbl_id_abono.setForeground(new java.awt.Color(51, 51, 51));
        lbl_id_abono.setText("-");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jdate_fecha_creacion, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lbl_id_abono)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addComponent(lbl_id_abono))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel7)
                            .addComponent(jdate_fecha_creacion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));

        jtabla.setFont(new java.awt.Font("Yu Gothic Medium", 0, 14)); // NOI18N
        jtabla.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla.setRowHeight(30);
        jtabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jtabla);

        lbl_total_seleccionado.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_total_seleccionado.setForeground(new java.awt.Color(204, 0, 0));
        lbl_total_seleccionado.setText("total");
        lbl_total_seleccionado.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lbl_total_seleccionadoMouseClicked(evt);
            }
        });

        lbl_total_titulo.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_total_titulo.setForeground(new java.awt.Color(51, 51, 51));
        lbl_total_titulo.setText("Total seleccionado:");

        btn_seleccionar_todas_las_facturas.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_seleccionar_todas_las_facturas.setText("Seleccionar totdas las facturas");
        btn_seleccionar_todas_las_facturas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_seleccionar_todas_las_facturasActionPerformed(evt);
            }
        });

        lbl_notificacion.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_notificacion.setForeground(new java.awt.Color(153, 0, 0));
        lbl_notificacion.setText("-");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1261, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btn_seleccionar_todas_las_facturas)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_titulo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_seleccionado))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(lbl_notificacion)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_total_seleccionado)
                    .addComponent(lbl_total_titulo)
                    .addComponent(btn_seleccionar_todas_las_facturas))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 291, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_notificacion)
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
        if (txt_abono.getText().isEmpty()) {
            txt_abono.setBackground(Color.pink);
            JOptionPane.showMessageDialog(this, "El valor ingresado no puede estar vacio");
            return;
        }

        double abono_dinero = Double.parseDouble(metodos.EliminaCaracteres(txt_abono.getText(), "."));
        if (abono_dinero <= 0) {
            JOptionPane.showMessageDialog(this, "El valor del abono debe ser mayor a cero");
            return;
        }

        DBabonos dbabono = new DBabonos();

        AbonosCabecera cabecera = new AbonosCabecera();
        cabecera.setId_user(frm_main.id_user);
        cabecera.setId_contacto(Integer.parseInt(lbl_id_cliente.getText()));
        cabecera.setTotal(abono_dinero);

        if (jbox_tipo_abono.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay tipos de abono creados. Cree al menos uno en Creditos > Tipos de abonos antes de registrar pagos.");
            return;
        }
        if (jbox_tipo_abono.getSelectedIndex() < 0) {
            jbox_tipo_abono.setSelectedIndex(0);
        }
        try {
            cabecera.setId_tipo_abono(jbox_tipo_abono.getItemAt(jbox_tipo_abono.getSelectedIndex()).getId());
        } catch (Exception e) {
            cabecera.setId_tipo_abono(id_tipo_abono);
        }

        // fecha en la que se recibio el pago
        int dia, mes, ano;
        ano = jdate_fecha_creacion.getCalendar().get(Calendar.YEAR);
        mes = jdate_fecha_creacion.getCalendar().get(Calendar.MONTH) + 1;
        dia = jdate_fecha_creacion.getCalendar().get(Calendar.DAY_OF_MONTH);
        cabecera.setFecha(ano + "-" + mes + "-" + dia);
        cabecera.setHora(DB_consultas_R_D.obtener_hora());
        cabecera.setObservacion(txt_observacion.getText());

        // imagen
        cabecera.setFoto(nombreArchivoImagen);
        if (!nombreArchivoImagen.equals("")) {
            metodos.copyFile_Java7(ruta_origen_imagen, ruta_destino_nombre_archivo_imagen);
        }

        // PDF: copia a carpeta de red
        if (cargoPDF) {
            nombreArchivoPDF = lbl_id_abono.getText() + "_" + DB_consultas_R_D.obtener_fecha() + "_" + DB_consultas_R_D.obtener_hora_con_guiones() + nombreArchivoPDF;
            ruta_destino_nombre_archivo_PDF = DB_consultas_R_D.Ruta_Imagenes() + nombreArchivoPDF;
            metodos.copyFile_Java7(ruta_origen_PDF, ruta_destino_nombre_archivo_PDF);
        }
        cabecera.setPDF(nombreArchivoPDF == null ? "" : nombreArchivoPDF);

        // EDICION: solo se actualizan los datos de la cabecera, el reparto no se toca
        if ("editar".equals(tipo)) {
            cabecera.setId(Integer.parseInt(lbl_id_abono.getText()));
            if (dbabono.ActualizarCabecera(cabecera) == 0) {
                JOptionPane.showMessageDialog(this, "No se encontró el pago a actualizar (id "
                        + cabecera.getId() + "). No se guardó nada.");
                return;
            }
            sincronizar_ingreso(cabecera);
            actualizarVentanas();
            this.dispose();
            return;
        }

        // NUEVO PAGO: repartir el dinero entre las facturas de la tabla.
        // Con anticipo marcado no se aplica a facturas: todo queda como saldo a favor.
        boolean especifico = chk_abono_espesifico.isSelected();

        if (especifico) {
            if (ids_seleccionados(jtabla).equals("-")) {
                JOptionPane.showMessageDialog(this, "Ha seleccionado la opción de abonos especificos\nDebe selecionar al menos un crédito");
                return;
            }
            double total_seleccionado = Double.parseDouble(metodos.EliminaCaracteres(lbl_total_seleccionado.getText(), "."));
            if (abono_dinero > total_seleccionado) {
                JOptionPane.showMessageDialog(this, "No se puede realizar un abono mayor a los creditos seleccionados\n\n"
                        + metodos.formateador_dinero().format(total_seleccionado));
                return;
            }
        }

        java.util.List<Abonos> detalles = new java.util.ArrayList<>();
        double restante = abono_dinero;

        if (!chk_anticipo.isSelected()) {
            for (int i = 0; i < jtabla.getRowCount() && restante > 0; i++) {
                if (especifico && !jtabla.getValueAt(i, 8).toString().equals("Abonar")) {
                    continue;
                }
                if (!jtabla.getValueAt(i, 2).toString().equals("CREDITO")) {
                    continue;
                }
                double deuda = Double.parseDouble(metodos.EliminaCaracteres(jtabla.getValueAt(i, 7).toString(), "."));
                if (deuda <= 0) {
                    continue;
                }
                double aplica = Math.min(deuda, restante);
                detalles.add(new Abonos(Integer.parseInt(jtabla.getValueAt(i, 0).toString()),
                        aplica, cabecera.getFecha(), cabecera.getHora()));
                restante -= aplica;
            }
        }

        if (restante > 0.009) {
            int r = JOptionPane.showConfirmDialog(this, "Quedarán " + metodos.formateador_dinero().format(restante)
                    + " sin aplicar a facturas.\nEse valor quedará como SALDO A FAVOR del cliente.\n¿Desea continuar?",
                    "Saldo a favor", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) {
                return;
            }
        }

        int idCabecera = dbabono.GuardarPago(cabecera, detalles);
        if (idCabecera == 0) {
            return; // no se guardo nada; DBabonos ya mostro el error
        }
        lbl_id_abono.setText(String.valueOf(idCabecera));

        // El dinero recibido entra a Caja si el tipo de abono asi lo indica.
        guardar_ingreso(cabecera);

        int dialogButton = JOptionPane.YES_NO_OPTION;
        int dialogResult = JOptionPane.showConfirmDialog(null, "Desea imprimir?", "Alerta", dialogButton);
        if (dialogResult == JOptionPane.YES_OPTION) {
            btn_imprimir.setEnabled(true);
            btn_imprimir.doClick();
        }

        actualizarVentanas();
        this.dispose();
    }

    /**
     * Caja a la que entran los abonos de cartera. La app maneja dos cajas; los
     * pagos de credito van siempre a la principal, igual que la cuenta de
     * ingresos "abono a credito" que crea la migracion.
     */
    public static final int CAJA_CREDITOS = 1;

    /** Texto para lbl_notificacion: si el pago entra a Caja y dónde. */
    private static String textoDestinoCaja() {
        if (!DBabonos.agregar_a_ingreso(id_tipo_abono)) {
            return "Este tipo de abono NO entra a Caja (solo queda en cartera).";
        }
        if (DBIngresos.cuentaAbonoACredito() <= 0) {
            return "ATENCION: entrara a Caja pero no hay cuenta de ingresos marcada como abono a credito.";
        }
        return "El pago entrará a Caja como ingreso; elija el fondo que lo recibe.";
    }

    /**
     * Registra en Caja el ingreso que genera este abono, si el tipo de abono
     * asi lo pide. El abono ya quedó guardado antes de llegar aqui: si el
     * ingreso falla se avisa, pero no se deshace la cartera.
     */
    private void guardar_ingreso(AbonosCabecera cabecera) {
        if (!DBabonos.agregar_a_ingreso(cabecera.getId_tipo_abono())) {
            return;
        }
        AuditoriaCaja.setOrigen("Creditos - abono a total (ingreso a caja)");
        try {
            new DBIngresos().Guardar_desde_abono_credito(ingresoDe(cabecera), cabecera.getId());
        } finally {
            AuditoriaCaja.limpiar();
        }
    }

    /**
     * Deja el ingreso de Caja en linea con un abono que se acaba de editar:
     * corrige el valor, lo crea si el tipo de abono paso a entrar a Caja, o lo
     * borra si dejo de hacerlo.
     */
    private void sincronizar_ingreso(AbonosCabecera cabecera) {
        AuditoriaCaja.setOrigen("Creditos - editar abono (ingreso a caja)");
        try {
            new DBIngresos().Sincronizar_ingreso_de_abono(ingresoDe(cabecera), cabecera.getId(),
                    DBabonos.agregar_a_ingreso(cabecera.getId_tipo_abono()));
        } finally {
            AuditoriaCaja.limpiar();
        }
    }

    /** Traduce la cabecera del pago al ingreso de Caja que le corresponde. */
    private Ingresos ingresoDe(AbonosCabecera cabecera) {
        Ingresos obj = new Ingresos();
        obj.setDescripcion("Abono a credito #" + cabecera.getId()
                + (cabecera.getObservacion() == null || cabecera.getObservacion().isEmpty()
                        ? "" : " - " + cabecera.getObservacion()));
        obj.setFecha(cabecera.getFecha());
        obj.setHora(DB_consultas_R_D.obtener_hora());
        obj.setId_user(frm_main.id_user);
        obj.setTotal(cabecera.getTotal());
        obj.setId_cliente(cabecera.getId_contacto());
        obj.setId_caja(CAJA_CREDITOS);
        obj.setFactura_remision(0);

        // fondo elegido en la ventana; si no hay eleccion, el predeterminado
        try {
            obj.setId_fondo(jbox_Fondos.getItemAt(jbox_Fondos.getSelectedIndex()).getId());
        } catch (Exception e) {
            obj.setId_fondo(Fondos.TraerPredeterminado(CAJA_CREDITOS));
        }

        // vendedor: el del credito al que se abonó. Con un pago repartido entre
        // varios creditos se toma el del primero; con anticipo puro no hay.
        obj.setId_vendedor(vendedorDelPago(cabecera.getId()));

        return obj;
    }

    /**
     * Vendedor al que se le acredita el pago: el del primer credito abonado.
     * Devuelve 0 si el pago no toca ningun credito (anticipo puro) o si el
     * credito no tiene vendedor asignado.
     */
    private static int vendedorDelPago(int idCabecera) {
        try (ResultSet rs = DB_consultas_R_D.getTabla(
                "select c.id_empleado from abonos a join creditos c on c.id = a.id_credito "
                + "where a.id_cabecera = " + idCabecera + " and c.id_empleado is not null "
                + "order by a.id limit 1")) {
            if (rs.next()) {
                return rs.getInt("id_empleado");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return 0;
    }

    /**
     * Refresca las ventanas que muestran creditos, si estan abiertas.
     */
    private void actualizarVentanas() {
        try {
            jd_ver_creditos_cliente.btn_actualizar.doClick();
        } catch (Exception e) {
        }
        try {
            frm_Creditos.btn_actualizar.doClick();
        } catch (Exception e) {
        }
    }

    public static String ids_seleccionados(JTable tabla) {

        StringBuilder sb = new StringBuilder();
        if (chk_abono_espesifico.isSelected()) {

            DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
            boolean entro = true;
            // Construimos el String con todos los valores de "ID factura"
            for (int fila = 0; fila < modelo.getRowCount(); fila++) {
                if (modelo.getValueAt(fila, 8).toString().equals("Abonar")) {

                    Object valorId = modelo.getValueAt(fila, 0);
                    if (valorId != null) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(valorId.toString());
                        entro = false;
                    }
                }
            }

            if (entro) {
                sb.append("-");
            }
        } else {
            sb.append("-");
        }
        return sb.toString();
    }
    private void btn_guardarKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_btn_guardarKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
//            btn_guardar.doClick();
        }
    }//GEN-LAST:event_btn_guardarKeyPressed
    private void btn_imprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imprimirActionPerformed
        try {
            new Creditos.ImprimirReciboPDF().imprimir(Integer.parseInt(lbl_id_abono.getText()));
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
        ver_pdf.tipo = "abonar_total_credito";
        ver_pdf.show();
    }//GEN-LAST:event_btnAgregarImagen1ActionPerformed

    private void jbox_tipo_abonoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbox_tipo_abonoActionPerformed
        clic_en_jbox();

    }//GEN-LAST:event_jbox_tipo_abonoActionPerformed
    public static void clic_en_jbox() {
        if (jbox_tipo_abono.getItemCount() == 0) {
            // no hay tipos de abono creados todavia
            id_tipo_abono = 0;
            lbl_notificacion.setText("No hay tipos de abono creados. Cree uno en Creditos > Tipos de abonos");
            chk_anticipo.setSelected(false);
            chk_anticipo.setVisible(false);
            jbox_Fondos.setEnabled(false);
            return;
        }
        try {
            if (jbox_tipo_abono.getSelectedIndex() < 0) {
                jbox_tipo_abono.setSelectedIndex(0);
            }
            id_tipo_abono = jbox_tipo_abono.getItemAt(jbox_tipo_abono.getSelectedIndex()).getId();
        } catch (Exception e) {
            System.out.println("Por favor seleccione un tipo de abono");
        }

        // El fondo solo importa si este tipo de abono entra a Caja.
        jbox_Fondos.setEnabled(DBabonos.agregar_a_ingreso(id_tipo_abono));

        ResultSet rs = DB_consultas_R_D.getTabla("select anticipo from tipos_abonos where id=" + id_tipo_abono);

        try {
            while (rs.next()) {
                if (rs.getInt("anticipo") == 1) {
                    lbl_notificacion.setText("Ha seleccionado Anticipo: el pago NO se aplicará a facturas, quedará como saldo a favor. "
                            + textoDestinoCaja());
                    chk_anticipo.setVisible(true);
                    chk_anticipo.setSelected(true);
                } else {
                    lbl_notificacion.setText(textoDestinoCaja());
                    chk_anticipo.setSelected(false);
                    chk_anticipo.setVisible(false);
                }
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    private void btn_Eliminar_ImagenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_Eliminar_ImagenActionPerformed

        try {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar esta imagen de este credito?\n" + ruta_ver_imagen, "Alerta", dialogButton);
            if (dialogResult == JOptionPane.YES_OPTION) {
                DB_consultas_R_D.Eliminar_Archivo(ruta_ver_imagen);
                DB_consultas_R_D.Actualizar_Campo_String("abonos_cabeceras", "foto", lbl_id_abono.getText(), "");
                lbl_foto.setIcon(null);
                nombreArchivoImagen = "";
                ruta_destino_nombre_archivo_imagen = "";
                ruta_origen_imagen = "";
                btn_Eliminar_Imagen.setEnabled(false);
            }
        } catch (SQLException ex) {
            Logger.getLogger(jd_abonar_a_total.class.getName()).log(Level.SEVERE, null, ex);
        }


    }//GEN-LAST:event_btn_Eliminar_ImagenActionPerformed

    private void btn_Eliminar_PDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_Eliminar_PDFActionPerformed
        try {

            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar el PDF de este credito?\n" + ruta_ver_PDF, "Alerta", dialogButton);
            if (dialogResult == JOptionPane.YES_OPTION) {
                DB_consultas_R_D.Eliminar_Archivo(ruta_ver_PDF);
                DB_consultas_R_D.Actualizar_Campo_String("abonos_cabeceras", "pdf", lbl_id_abono.getText(), "");
                nombreArchivoPDF = "";
                btn_Eliminar_PDF.setEnabled(false);

            }
        } catch (SQLException ex) {
            Logger.getLogger(jd_abonar_a_total.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btn_Eliminar_PDFActionPerformed

    private void chk_abono_espesificoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chk_abono_espesificoActionPerformed
        if (chk_abono_espesifico.isSelected()) {
            modelo_tabla.setColumnIdentifiers(new Object[]{"ID factura", "Fecha Creación", "Tipo", "Cuenta", "# factura", "Total", "Cancelado", "Saldo", "Abonar"});

            for (int i = 0; i < modelo_tabla.getRowCount(); i++) {
                modelo_tabla.setValueAt("Global", i, 8);
            }
            btn_seleccionar_todas_las_facturas.setVisible(true);
            lbl_total_seleccionado.setVisible(true);
            lbl_total_titulo.setVisible(true);
            sumar_seleccionados();
        } else {
            modelo_tabla.setColumnIdentifiers(new Object[]{"ID factura", "Fecha Creación", "Tipo", "Cuenta", "# factura", "Total", "Cancelado", "Saldo"});

            try {
                for (int i = 0; i < modelo_tabla.getRowCount(); i++) {
                    modelo_tabla.removeRow(i);
                    i -= 1;
                }
            } catch (Exception e) {
            }

            for (int i = 0; i < jd_ver_creditos_cliente.modelo_facturas.getRowCount(); i++) {

                double saldo_fila = 0;
                try {
                    saldo_fila = Double.parseDouble(metodos.EliminaCaracteres(jd_ver_creditos_cliente.modelo_facturas.getValueAt(i, 7).toString(), "."));
                } catch (Exception e) {
                }
                if (jd_ver_creditos_cliente.modelo_facturas.getValueAt(i, 2).toString().equals("CREDITO") && saldo_fila > 0.009) {

                    modelo_tabla.addRow(new Object[]{jd_ver_creditos_cliente.modelo_facturas.getValueAt(i, 0).toString(), jd_ver_creditos_cliente.modelo_facturas.getValueAt(i, 1).toString(),
                        jd_ver_creditos_cliente.modelo_facturas.getValueAt(i, 2).toString(), jd_ver_creditos_cliente.modelo_facturas.getValueAt(i, 3).toString(),
                        jd_ver_creditos_cliente.modelo_facturas.getValueAt(i, 4).toString(), jd_ver_creditos_cliente.modelo_facturas.getValueAt(i, 5).toString(),
                        jd_ver_creditos_cliente.modelo_facturas.getValueAt(i, 6).toString(), jd_ver_creditos_cliente.modelo_facturas.getValueAt(i, 7).toString()});
                }
            }
            jtabla.setModel(modelo_tabla);
            ///////////////

            btn_seleccionar_todas_las_facturas.setVisible(false);
            lbl_total_seleccionado.setVisible(false);
            lbl_total_titulo.setVisible(false);
        }
    }//GEN-LAST:event_chk_abono_espesificoActionPerformed
    private void lbl_total_seleccionadoMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_total_seleccionadoMouseClicked
        txt_abono.setText(lbl_total_seleccionado.getText());
    }//GEN-LAST:event_lbl_total_seleccionadoMouseClicked

    private void btn_seleccionar_todas_las_facturasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_seleccionar_todas_las_facturasActionPerformed
        try {
            for (int i = 0; i < modelo_tabla.getRowCount(); i++) {
                modelo_tabla.setValueAt("Abonar", i, 8);

            }
            sumar_seleccionados();

        } catch (Exception e) {
        }
    }//GEN-LAST:event_btn_seleccionar_todas_las_facturasActionPerformed

    private void lbl_total_saldoMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lbl_total_saldoMouseClicked
        txt_abono.setText(lbl_total_saldo.getText());

    }//GEN-LAST:event_lbl_total_saldoMouseClicked

    private void chk_anticipoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chk_anticipoActionPerformed
        if (chk_anticipo.isSelected()) {
//            modelo_tabla.setRowCount(0);
        }
    }//GEN-LAST:event_chk_anticipoActionPerformed
    public static void sumar_seleccionados() {
        double total = 0;
        for (int i = 0; i < modelo_tabla.getRowCount(); i++) {
            if (modelo_tabla.getValueAt(i, 8).equals("Abonar")) {
                total += Double.parseDouble(metodos.EliminaCaracteres(modelo_tabla.getValueAt(i, 7).toString(), "."));
            }
        }
        lbl_total_seleccionado.setText(metodos.formateador_dinero().format(total));
    }

    public static void sumar_total() {
        double total = 0;
        for (int i = 0; i < modelo_tabla.getRowCount(); i++) {
            total += Double.parseDouble(metodos.EliminaCaracteres(modelo_tabla.getValueAt(i, 5).toString(), "."));
        }
        lbl_total_seleccionado.setText(metodos.formateador_dinero().format(total));
    }

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
            java.util.logging.Logger.getLogger(jd_abonar_a_total.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(jd_abonar_a_total.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(jd_abonar_a_total.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(jd_abonar_a_total.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                jd_abonar_a_total dialog = new jd_abonar_a_total(new javax.swing.JFrame(), true);
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
    public static javax.swing.JButton btn_guardar;
    public static javax.swing.JButton btn_imagen;
    public static javax.swing.JButton btn_imprimir;
    public static javax.swing.JButton btn_seleccionar_todas_las_facturas;
    public static javax.swing.JButton btn_ver_pdf;
    private javax.swing.ButtonGroup buttonGroup1;
    public static javax.swing.JCheckBox chk_abono_espesifico;
    public static javax.swing.JCheckBox chk_anticipo;
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
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    public static javax.swing.JComboBox<Fondos> jbox_Fondos;
    public static javax.swing.JComboBox<Tipos_abonos> jbox_tipo_abono;
    public static com.toedter.calendar.JDateChooser jdate_fecha_creacion;
    public static javax.swing.JLabel jlabelabonar;
    public static javax.swing.JLabel jlabelabonar1;
    public static javax.swing.JTable jtabla;
    public static javax.swing.JLabel lbl_cedula;
    public static javax.swing.JLabel lbl_cliente_nombre;
    public static javax.swing.JLabel lbl_foto;
    public static javax.swing.JLabel lbl_id_abono;
    public static javax.swing.JLabel lbl_id_cliente;
    public static javax.swing.JLabel lbl_notificacion;
    public static javax.swing.JLabel lbl_total_saldo;
    public static javax.swing.JLabel lbl_total_seleccionado;
    public static javax.swing.JLabel lbl_total_titulo;
    public static javax.swing.JTextField txt_abono;
    public static javax.swing.JTextArea txt_observacion;
    // End of variables declaration//GEN-END:variables
}
