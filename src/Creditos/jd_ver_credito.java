/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos;

import Creditos.frm_Creditos;
import Formularios.frm_main;
import Creditos.Numero_a_Letra;
import Metodos.TextPrompt;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author Monkeyelgrande
 */
public class jd_ver_credito extends javax.swing.JDialog {

    private static final Logger LOGGER = Logger.getLogger(jd_ver_credito.class.getName());
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    private JTable jtabla_abonos;
    private DefaultTableModel modelo_abonos;

    /**
     * Creates new form jd_ver_devolucion
     */
    public jd_ver_credito(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setLocationRelativeTo(parent);
        metodos.addEscapeListenerWindowDialog(this);

        txt_descripcion.setWrapStyleWord(true);

        inicializarTablaAbonos();
    }

    private void inicializarTablaAbonos() {
        modelo_abonos = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        modelo_abonos.setColumnIdentifiers(new Object[]{"ID", "Fecha", "Tipo", "Valor", "Observación", "Pago (cabecera)"});

        jtabla_abonos = new JTable(modelo_abonos);
        jtabla_abonos.getTableHeader().setReorderingAllowed(false);
        MetodosCreditos.EstiloTablaMaterialGlobalPequeno(jtabla_abonos);

        TableColumnModel cm = jtabla_abonos.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(1).setPreferredWidth(90);
        cm.getColumn(2).setPreferredWidth(140);
        cm.getColumn(3).setPreferredWidth(110);
        cm.getColumn(4).setPreferredWidth(220);

        panel_abonos.setLayout(new java.awt.BorderLayout());
        panel_abonos.add(new JScrollPane(jtabla_abonos), java.awt.BorderLayout.CENTER);

        jtabla_abonos.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() != 2) {
                    return;
                }
                int fila = jtabla_abonos.getSelectedRow();
                if (fila < 0) {
                    return;
                }
                String idCabecera = jtabla_abonos.getValueAt(fila, 5).toString();
                String valor = jtabla_abonos.getValueAt(fila, 3).toString();

                jd_abonar_a_total frm = new jd_abonar_a_total(null, true);
                jd_ver_creditos_cliente.cargarAbonoEnDialog(frm, idCabecera, valor, true);
                jd_ver_creditos_cliente.cargarAbonosHijosEnDialog(idCabecera);
                jd_abonar_a_total.jdate_fecha_creacion.setEnabled(false);
                jd_abonar_a_total.btn_guardar.setText("Actualizar");
                jd_abonar_a_total.tipo = "editar";
                frm.show();
            }
        });
    }

    public void cargarAbonosCredito(String idCredito) {
        modelo_abonos.setRowCount(0);

        String consulta = "SELECT a.id, a.fecha, a.abono, ca.observacion, t.nombre AS tipo_abono, a.id_cabecera "
                + "FROM abonos a "
                + "JOIN abonos_cabeceras ca ON a.id_cabecera = ca.id "
                + "JOIN tipos_abonos t ON ca.id_tipo_abono = t.id "
                + "WHERE a.id_credito = " + idCredito + " "
                + "ORDER BY a.fecha, CAST(a.hora AS TIME) ASC, a.id";

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                modelo_abonos.addRow(new Object[]{
                    rs.getString("id"),
                    sdf.format(rs.getDate("fecha")),
                    rs.getString("tipo_abono"),
                    metodos.formateador_dinero().format(rs.getDouble("abono")),
                    rs.getString("observacion"),
                    rs.getString("id_cabecera")
                });
            }
            rs.close();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
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
        lbl_fecha_vencimiento = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        lbl_total_factura = new javax.swing.JLabel();
        lbl_id_factura = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        lbl_user = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        lbl_fecha_factura = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        lbl_codigo = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txt_descripcion = new javax.swing.JTextArea();
        jLabel14 = new javax.swing.JLabel();
        lbl_interes = new javax.swing.JLabel();
        btn_imagen = new javax.swing.JButton();
        btn_ver_pdf = new javax.swing.JButton();
        btn_imprimir_oi = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        lbl_cliente = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        lbl_documento = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        lbl_telefono = new javax.swing.JLabel();
        lbl_direccion = new javax.swing.JLabel();
        panel_abonos = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Abonos");
        setModal(true);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        lbl_fecha_vencimiento.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_fecha_vencimiento.setForeground(new java.awt.Color(0, 51, 51));
        lbl_fecha_vencimiento.setText("Fecha v");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(51, 51, 51));
        jLabel6.setText("Fecha vencimiento");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(51, 51, 51));
        jLabel5.setText("Total factura");

        lbl_total_factura.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_total_factura.setForeground(new java.awt.Color(0, 102, 204));
        lbl_total_factura.setText("total");

        lbl_id_factura.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_id_factura.setForeground(new java.awt.Color(0, 51, 51));
        lbl_id_factura.setText("ID");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(51, 51, 51));
        jLabel3.setText("ID Factura");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(51, 51, 51));
        jLabel8.setText("Usuario");

        lbl_user.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_user.setForeground(new java.awt.Color(0, 51, 51));
        lbl_user.setText("user");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(51, 51, 51));
        jLabel7.setText("Fecha creación");

        lbl_fecha_factura.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_fecha_factura.setForeground(new java.awt.Color(0, 51, 51));
        lbl_fecha_factura.setText("Fecha c");

        jLabel11.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(51, 51, 51));
        jLabel11.setText("Codigo");

        lbl_codigo.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_codigo.setForeground(new java.awt.Color(0, 51, 51));
        lbl_codigo.setText("cod");

        jLabel13.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(51, 51, 51));
        jLabel13.setText("Descripción");

        jScrollPane2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        txt_descripcion.setEditable(false);
        txt_descripcion.setColumns(20);
        txt_descripcion.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txt_descripcion.setLineWrap(true);
        txt_descripcion.setRows(5);
        jScrollPane2.setViewportView(txt_descripcion);

        jLabel14.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(51, 51, 51));
        jLabel14.setText("Interes ");

        lbl_interes.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_interes.setForeground(new java.awt.Color(204, 51, 0));
        lbl_interes.setText("Interes");

        btn_imagen.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        btn_imagen.setText("VER IMAGEN");
        btn_imagen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imagenActionPerformed(evt);
            }
        });

        btn_ver_pdf.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        btn_ver_pdf.setText("VER PDF");
        btn_ver_pdf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ver_pdfActionPerformed(evt);
            }
        });

        btn_imprimir_oi.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_imprimir_oi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/print_pequeno.png"))); // NOI18N
        btn_imprimir_oi.setMnemonic('p');
        btn_imprimir_oi.setText("Imprimir");
        btn_imprimir_oi.setToolTipText("ATL+P");
        btn_imprimir_oi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimir_oiActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel13)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel11))
                                .addGap(115, 115, 115)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbl_id_factura)
                                    .addComponent(lbl_codigo)))
                            .addComponent(jLabel8)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel5)
                                    .addComponent(jLabel14))
                                .addGap(49, 49, 49)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lbl_total_factura)
                                    .addComponent(lbl_fecha_factura)
                                    .addComponent(lbl_fecha_vencimiento)
                                    .addComponent(lbl_user)
                                    .addComponent(lbl_interes))))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btn_imagen, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_ver_pdf)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_imprimir_oi, javax.swing.GroupLayout.DEFAULT_SIZE, 184, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lbl_id_factura))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(lbl_codigo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(lbl_user))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(lbl_fecha_factura))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(lbl_fecha_vencimiento))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(lbl_total_factura))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel14)
                    .addComponent(lbl_interes))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_imagen)
                    .addComponent(btn_ver_pdf, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_imprimir_oi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel2.setBackground(new java.awt.Color(204, 255, 255));

        lbl_cliente.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_cliente.setForeground(new java.awt.Color(0, 102, 102));
        lbl_cliente.setText("Cliente");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(51, 51, 51));
        jLabel4.setText("Cliente");

        jLabel9.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(51, 51, 51));
        jLabel9.setText("Documento");

        jLabel10.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(51, 51, 51));
        jLabel10.setText("Telefono");

        lbl_documento.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_documento.setForeground(new java.awt.Color(51, 51, 51));
        lbl_documento.setText("no doc");

        jLabel15.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(51, 51, 51));
        jLabel15.setText("Dirección");

        lbl_telefono.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_telefono.setForeground(new java.awt.Color(51, 51, 51));
        lbl_telefono.setText("no telefono");

        lbl_direccion.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_direccion.setForeground(new java.awt.Color(51, 51, 51));
        lbl_direccion.setText("Dirección");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lbl_cliente))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_documento)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_telefono))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_direccion)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(lbl_cliente))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(lbl_documento)
                    .addComponent(jLabel10)
                    .addComponent(lbl_telefono))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(lbl_direccion))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout panel_abonosLayout = new javax.swing.GroupLayout(panel_abonos);
        panel_abonos.setLayout(panel_abonosLayout);
        panel_abonosLayout.setHorizontalGroup(
            panel_abonosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 610, Short.MAX_VALUE)
        );
        panel_abonosLayout.setVerticalGroup(
            panel_abonosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panel_abonos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(panel_abonos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void btn_imagenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imagenActionPerformed

        jif_ver_imagen frm = new jif_ver_imagen();
        String consulta = "select f.foto from creditos f where f.id =" + lbl_id_factura.getText();

        System.out.println(consulta);
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                try {

                    String ruta = DB_consultas_R_D.Ruta_Imagenes() + rs.getString("foto");

                    Image img = new ImageIcon(ruta).getImage();

                    Image newimg = img.getScaledInstance(frm.lbl_foto.getWidth(), frm.lbl_foto.getHeight(), java.awt.Image.SCALE_SMOOTH);
                    ImageIcon newicon = new ImageIcon(newimg);

                    frm.lbl_foto.setIcon(newicon);
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
    }//GEN-LAST:event_btn_imagenActionPerformed

    private void btn_ver_pdfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ver_pdfActionPerformed

        String consulta = "select f.pdf from creditos f where f.id =" + lbl_id_factura.getText();

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                try {

                    String ruta = DB_consultas_R_D.Ruta_Imagenes() + rs.getString("pdf");

                    DB_consultas_R_D.Abrir_Archivo(ruta);

                } catch (Exception e) {
                    System.out.println("NO se cargo el PDF");
                }

            }
            rs.close();

        } catch (Exception ex) {
            Logger.getLogger(frm_Creditos.class.getName()).log(Level.SEVERE, null, ex);
        }


    }//GEN-LAST:event_btn_ver_pdfActionPerformed

    private void btn_imprimir_oiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imprimir_oiActionPerformed
        String cad = new File("").getAbsolutePath() + "/src/reportes/Credito.jrxml";
        if (!cad.equals("")) {

            Numero_a_Letra d = new Numero_a_Letra();
            int numero = Integer.parseInt(metodos.EliminaCaracteres(lbl_total_factura.getText(), "."));
            String letras = d.Convertir(String.valueOf(numero), true) + " PESOS";

            Connection cn = DB_consultas_R_D.getConexion();
            JasperReport report = null;
            Map p = new HashMap();
            p.put("SUBREPORT_DIR", new File("").getAbsolutePath() + "/src/reportes/");
            p.put("id_factura", Integer.parseInt(lbl_id_factura.getText()));
            p.put("letras", "$ " + letras);
            try {
                try {
                    report = JasperCompileManager.compileReport(cad);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, e);
                }
                JasperPrint print = JasperFillManager.fillReport(report, p, cn);
                JasperViewer view = new JasperViewer(print, false);
                cn.close();
                JDialog dialog = new JDialog(this);//the owner
                metodos.addEscapeListenerWindowDialog(dialog);

                dialog.setContentPane(view.getContentPane());
                dialog.setSize(view.getSize());
                dialog.setModal(true);
                dialog.setLocationRelativeTo(this);
                dialog.setTitle("Reporte abonos");
                dialog.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_btn_imprimir_oiActionPerformed
    public static String nombreArchivoImagen = "";

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
            java.util.logging.Logger.getLogger(jd_ver_credito.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(jd_ver_credito.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(jd_ver_credito.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(jd_ver_credito.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                jd_ver_credito dialog = new jd_ver_credito(new javax.swing.JFrame(), true);
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
    public static javax.swing.JButton btn_imagen;
    private javax.swing.JButton btn_imprimir_oi;
    public static javax.swing.JButton btn_ver_pdf;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    public static javax.swing.JLabel lbl_cliente;
    public static javax.swing.JLabel lbl_codigo;
    public static javax.swing.JLabel lbl_direccion;
    public static javax.swing.JLabel lbl_documento;
    public static javax.swing.JLabel lbl_fecha_factura;
    public static javax.swing.JLabel lbl_fecha_vencimiento;
    public static javax.swing.JLabel lbl_id_factura;
    public static javax.swing.JLabel lbl_interes;
    public static javax.swing.JLabel lbl_telefono;
    public static javax.swing.JLabel lbl_total_factura;
    public static javax.swing.JLabel lbl_user;
    private javax.swing.JPanel panel_abonos;
    public static javax.swing.JTextArea txt_descripcion;
    // End of variables declaration//GEN-END:variables
}
