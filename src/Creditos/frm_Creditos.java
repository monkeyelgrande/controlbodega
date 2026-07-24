/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos;

import Formularios.frm_main;
import Creditos.jd_abonar_a_total;
import Creditos.jd_buscar_no_factura;
import Creditos.jd_ver_creditos_cliente;
import Creditos.jif_crear_credito;
import Creditos.CellRendererCreditos;
import Metodos.ExportarExcel;
import Creditos.ExportarExcelCreditos;
import Metodos.metodos;
import com.ezware.oxbow.swingbits.table.filter.TableRowFilterSupport;
import conexiondb.DB_consultas_R_D;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author Monkeyelgrande
 */
public class frm_Creditos extends javax.swing.JInternalFrame {

    /**
     * Creates new form frm_clientes
     */
    Calendar fecha = new GregorianCalendar();
//    CellRendererCreditos myRenderer = new CellRendererCreditos();
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");

    public frm_Creditos() {
        initComponents();
        permisos(frm_main.perfil);
        Actualizar();
        TamanosTablaAbonos();
        sumar_totales();
//        jtabla_creditos.setDefaultRenderer(Object.class, myRenderer);
        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    AbonarCliente();
                }
            }
        });
        TableRowFilterSupport.forTable(jtabla).searchable(true).apply();

        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        metodos.EstiloTablaMaterialGlobal(jtabla);

    }

    public void permisos(int perfil) {
        switch (perfil) {
            case 2:
//                btn_crear.setEnabled(false);
//                btn_abono.setEnabled(false);
                break;
        }
    }

    public void TamanosTablaAbonos() {
        jtabla.getTableHeader().setReorderingAllowed(false);
        TableColumnModel columnModel = jtabla.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(20);
        columnModel.getColumn(1).setPreferredWidth(500);
        columnModel.getColumn(2).setPreferredWidth(200);
        columnModel.getColumn(2).setPreferredWidth(200);

    }
    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };
    DefaultTableModel modeloAbonos = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    public void Actualizar() {

        try {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

        String consulta = "WITH facturas_por_contacto AS (\n"
                + "    SELECT\n"
                + "        id_contacto,\n"
                + "        SUM(total) AS total\n"
                + "    FROM creditos\n"
                + "    GROUP BY id_contacto\n"
                + "),\n"
                + "abonos_por_contacto AS (\n"
                + "    SELECT\n"
                + "        id_contacto,\n"
                + "        SUM(total) AS abonos\n"
                + "    FROM abonos_cabeceras\n"
                + "    GROUP BY id_contacto\n"
                + ")\n"
                + "SELECT\n"
                + "    c.id AS id_cliente,\n"
                + "    c.antiguo,\n"
                + "    c.nombre,\n"
                + "    c.cedula,\n"
                + "    c.contacto as celular,\n"
                + "    COALESCE(fpc.total, 0) AS total,\n"
                + "    COALESCE(apc.abonos, 0) AS abono,\n"
                + "    (COALESCE(fpc.total, 0) - COALESCE(apc.abonos, 0)) AS saldo\n"
                + "FROM contactos c\n"
                + "LEFT JOIN facturas_por_contacto fpc\n"
                + "       ON fpc.id_contacto = c.id\n"
                + "LEFT JOIN abonos_por_contacto apc\n"
                + "       ON apc.id_contacto = c.id\n"
                + "ORDER BY c.nombre;";
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        modelo.setColumnIdentifiers(new Object[]{"ID cliente", "Nombre", "Cédula", "Telefono", "Saldo", "Archivo"});

        try {
            while (rs.next()) {
                String archivo = "Nuevo";
                if (rs.getInt("antiguo") == 1) {
                    archivo = "Antiguo";

                }
                modelo.addRow(new Object[]{rs.getString("id_cliente"), rs.getString("nombre"), rs.getString("cedula"), rs.getString("celular"),
                    metodos.formateador_dinero().format(rs.getDouble("saldo")), archivo});
            }
            rs.close();

            jtabla.setModel(modelo);
            TamanosTablaAbonos();
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpop_crap = new javax.swing.JPopupMenu();
        jpm_Abonar = new javax.swing.JMenuItem();
        jpm_verFa = new javax.swing.JMenuItem();
        jPanel2 = new javax.swing.JPanel();
        lbl_cant_clientes = new javax.swing.JPanel();
        txt_Filtro = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();
        btn_total = new javax.swing.JButton();
        jlabel = new javax.swing.JLabel();
        lbl_suma_saldo = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        btn_crear = new javax.swing.JButton();
        btn_abonas_cliente = new javax.swing.JButton();
        btn_actualizar = new javax.swing.JButton();
        btn_buscar = new javax.swing.JButton();
        jpanel_titulo = new javax.swing.JPanel();
        lbl_titulo = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        btn_imprimir = new javax.swing.JButton();

        jpm_Abonar.setText("Realiar Abono");
        jpm_Abonar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jpm_AbonarActionPerformed(evt);
            }
        });
        jpop_crap.add(jpm_Abonar);

        jpm_verFa.setText("Ver factura");
        jpm_verFa.setToolTipText("");
        jpm_verFa.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jpm_verFaActionPerformed(evt);
            }
        });
        jpop_crap.add(jpm_verFa);

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Creditos ");

        lbl_cant_clientes.setBackground(new java.awt.Color(33, 33, 33));

        txt_Filtro.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        txt_Filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_FiltroKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_FiltroKeyTyped(evt);
            }
        });

        jtabla.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
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
        jtabla.setComponentPopupMenu(jpop_crap);
        jtabla.setRowHeight(25);
        jtabla.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(jtabla);

        btn_total.setBackground(new java.awt.Color(0, 153, 51));
        btn_total.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        btn_total.setForeground(new java.awt.Color(255, 255, 255));
        btn_total.setText("Sumar Total");
        btn_total.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_totalActionPerformed(evt);
            }
        });

        jlabel.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jlabel.setForeground(new java.awt.Color(255, 255, 255));
        jlabel.setText("Saldo $");

        lbl_suma_saldo.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_suma_saldo.setForeground(new java.awt.Color(255, 0, 0));
        lbl_suma_saldo.setText("0");

        javax.swing.GroupLayout lbl_cant_clientesLayout = new javax.swing.GroupLayout(lbl_cant_clientes);
        lbl_cant_clientes.setLayout(lbl_cant_clientesLayout);
        lbl_cant_clientesLayout.setHorizontalGroup(
            lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 996, Short.MAX_VALUE)
                    .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                        .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_total)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jlabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_suma_saldo)))
                .addContainerGap())
        );
        lbl_cant_clientesLayout.setVerticalGroup(
            lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lbl_cant_clientesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jlabel)
                        .addComponent(lbl_suma_saldo))
                    .addGroup(lbl_cant_clientesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btn_total)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel3.setBackground(new java.awt.Color(13, 13, 13));

        btn_crear.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_crear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/nuevo.png"))); // NOI18N
        btn_crear.setMnemonic('n');
        btn_crear.setText("Nuevo Crédito");
        btn_crear.setBorder(null);
        btn_crear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_crearActionPerformed(evt);
            }
        });

        btn_abonas_cliente.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        btn_abonas_cliente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/users.png"))); // NOI18N
        btn_abonas_cliente.setMnemonic('c');
        btn_abonas_cliente.setText("Abonar Cliente");
        btn_abonas_cliente.setBorder(null);
        btn_abonas_cliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_abonas_clienteActionPerformed(evt);
            }
        });

        btn_actualizar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_actualizar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/actualizar.png"))); // NOI18N
        btn_actualizar.setMnemonic('z');
        btn_actualizar.setText("Actualizar");
        btn_actualizar.setBorder(null);
        btn_actualizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_actualizarActionPerformed(evt);
            }
        });

        btn_buscar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_buscar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Search More.png"))); // NOI18N
        btn_buscar.setMnemonic('z');
        btn_buscar.setText("Buscar #Factura");
        btn_buscar.setBorder(null);
        btn_buscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_buscarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_crear, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_abonas_cliente, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_actualizar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_buscar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_crear)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_abonas_cliente)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_actualizar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_buscar)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jpanel_titulo.setBackground(new java.awt.Color(13, 30, 64));
        jpanel_titulo.setPreferredSize(new java.awt.Dimension(146, 80));

        lbl_titulo.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        lbl_titulo.setForeground(new java.awt.Color(255, 255, 255));
        lbl_titulo.setText("Creditos  ");

        jButton2.setBackground(new java.awt.Color(242, 56, 39));
        jButton2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jButton2.setForeground(new java.awt.Color(255, 255, 255));
        jButton2.setMnemonic('w');
        jButton2.setText("Cerrar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpanel_tituloLayout = new javax.swing.GroupLayout(jpanel_titulo);
        jpanel_titulo.setLayout(jpanel_tituloLayout);
        jpanel_tituloLayout.setHorizontalGroup(
            jpanel_tituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpanel_tituloLayout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(lbl_titulo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jpanel_tituloLayout.setVerticalGroup(
            jpanel_tituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpanel_tituloLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jpanel_tituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jButton2)
                    .addComponent(lbl_titulo, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(24, 24, 24))
        );

        jButton1.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/excel.png"))); // NOI18N
        jButton1.setText("Exportar a excel");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        btn_imprimir.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        btn_imprimir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/print_pequeno.png"))); // NOI18N
        btn_imprimir.setText("Imprimir");
        btn_imprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimirActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_imprimir, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_cant_clientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jpanel_titulo, javax.swing.GroupLayout.DEFAULT_SIZE, 1217, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jpanel_titulo, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_cant_clientes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_imprimir)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void jpm_AbonarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jpm_AbonarActionPerformed

    }//GEN-LAST:event_jpm_AbonarActionPerformed
    public void AbonarCliente() {
        int fila = jtabla.getSelectedRow();
        jd_ver_creditos_cliente frm = new jd_ver_creditos_cliente(null, closable, jtabla.getValueAt(fila, 0).toString());
        jd_ver_creditos_cliente.ventana = "creditos";
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
        } else {
            frm.show();
        }

    }
    private void jpm_verFaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jpm_verFaActionPerformed

    }//GEN-LAST:event_jpm_verFaActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void btn_actualizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_actualizarActionPerformed
        Actualizar();
        jpanel_titulo.setBackground(Color.BLUE);
    }//GEN-LAST:event_btn_actualizarActionPerformed

    private void txt_FiltroKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_FiltroKeyTyped

    }//GEN-LAST:event_txt_FiltroKeyTyped

    private void txt_FiltroKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_FiltroKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_DELETE)) {
            txt_Filtro.setText("");
        }
        if ((num == KeyEvent.VK_ENTER)) {
            btn_totalActionPerformed(null);
        }
    }//GEN-LAST:event_txt_FiltroKeyPressed

    private void btn_totalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_totalActionPerformed
        sumar_totales();
    }//GEN-LAST:event_btn_totalActionPerformed
    public void sumar_totales() {
        double total_saldo = 0;
        for (int i = 0; i < this.jtabla.getRowCount(); i++) {
            total_saldo += Double.parseDouble(metodos.EliminaCaracteres(this.jtabla.getValueAt(i, 4).toString(), "."));
        }
        lbl_suma_saldo.setText(MetodosCreditos.formateador_tres_decimales().format(total_saldo));
    }
    private void btn_crearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_crearActionPerformed
        jif_crear_credito frm = new jif_crear_credito(); // este es un jdialog
        jif_crear_credito.tipo = "nuevo";
        jif_crear_credito.editar = false;

        frm.show();
        jif_crear_credito.txt_total.requestFocus();
    }//GEN-LAST:event_btn_crearActionPerformed

    private void btn_abonas_clienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_abonas_clienteActionPerformed
        AbonarCliente();
    }//GEN-LAST:event_btn_abonas_clienteActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        ExportarExcelCreditos obj;

        try {
            obj = new ExportarExcelCreditos();
            obj.exportarExcel(jtabla);
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void btn_imprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imprimirActionPerformed
        btn_actualizar.doClick();
        String[] opciones = {"Nuevo", "Antiguo", "Cancelar"};
        int seleccion = JOptionPane.showOptionDialog(
                null, // componente padre
                "Seleccione una opción:", // mensaje
                "Opciones", // título
                JOptionPane.DEFAULT_OPTION, // tipo de opción
                JOptionPane.QUESTION_MESSAGE, // tipo de mensaje
                null, // icono
                opciones, // botones
                opciones[0] // opción por defecto
        );

        String tipo = "";
        double total_saldo = 0;
        if (seleccion == 0) {
            System.out.println("Seleccionó: Nuevo");
            tipo = "Antiguo";
            total_saldo = 0;

            for (int i = 0; i < this.jtabla.getRowCount(); i++) {
                if (jtabla.getValueAt(i, 5).equals("Nuevo")) {
                    total_saldo += Double.parseDouble(metodos.EliminaCaracteres(this.jtabla.getValueAt(i, 4).toString(), "."));
                }
            }
            lbl_suma_saldo.setText(MetodosCreditos.formateador_tres_decimales().format(total_saldo));
        } else if (seleccion == 1) {
            System.out.println("Seleccionó: Antiguo");
            tipo = "Nuevo";
            total_saldo = 0;

            for (int i = 0; i < this.jtabla.getRowCount(); i++) {
                if (jtabla.getValueAt(i, 5).equals("Antiguo")) {
                    total_saldo += Double.parseDouble(metodos.EliminaCaracteres(this.jtabla.getValueAt(i, 4).toString(), "."));
                }
            }
            lbl_suma_saldo.setText(MetodosCreditos.formateador_tres_decimales().format(total_saldo));
        } else {
            System.out.println("Cancelado");
            return;
        }

        try {

            DefaultTableModel de = (DefaultTableModel) jtabla.getModel();

// Recorremos de la última fila a la primera
            for (int i = de.getRowCount() - 1; i >= 0; i--) {
                Object valor = de.getValueAt(i, 5);
                if (valor != null && valor.toString().equalsIgnoreCase(tipo)) {
                    de.removeRow(i);
                }
            }

            JRTableModelDataSource datasource = new JRTableModelDataSource(de);

            JasperReport report = null;
            Map params = new HashMap();
            params.put("id_factura", "1");
            params.put("total", metodos.formateador_dinero().format(total_saldo));

            params.put("SUBREPORT_DIR", new File("").getAbsolutePath() + "/src/reportes/");

            try {
                try {
                    String cad = new File("").getAbsolutePath() + "/src/reportes/Imprimir_creditos.jrxml";
                    report = JasperCompileManager.compileReport(cad);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, e);
                }
                JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, datasource);

                JasperViewer view = new JasperViewer(jasperPrint, false);

                JDialog dialog = new JDialog();//the owner
                dialog.setContentPane(view.getContentPane());
                dialog.setSize(view.getSize());
                dialog.setModal(true);
                metodos.addEscapeListenerWindowDialog(dialog);
                dialog.setLocationRelativeTo(this);
                dialog.setTitle("Reporte");
                dialog.setVisible(true);
            } catch (Exception e) {
                System.out.println(e);
            }
        } catch (Exception e) {
        }
    }//GEN-LAST:event_btn_imprimirActionPerformed

    private void btn_buscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_buscarActionPerformed
        jd_buscar_no_factura jd = new jd_buscar_no_factura(null, closable);
        jd.show();
    }//GEN-LAST:event_btn_buscarActionPerformed

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
            java.util.logging.Logger.getLogger(frm_Creditos.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(frm_Creditos.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(frm_Creditos.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(frm_Creditos.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new frm_Creditos().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_abonas_cliente;
    public static javax.swing.JButton btn_actualizar;
    public static javax.swing.JButton btn_buscar;
    private javax.swing.JButton btn_crear;
    private javax.swing.JButton btn_imprimir;
    private javax.swing.JButton btn_total;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel jlabel;
    private javax.swing.JPanel jpanel_titulo;
    private javax.swing.JMenuItem jpm_Abonar;
    private javax.swing.JMenuItem jpm_verFa;
    private javax.swing.JPopupMenu jpop_crap;
    private javax.swing.JTable jtabla;
    private javax.swing.JPanel lbl_cant_clientes;
    private javax.swing.JLabel lbl_suma_saldo;
    private javax.swing.JLabel lbl_titulo;
    private javax.swing.JTextField txt_Filtro;
    // End of variables declaration//GEN-END:variables
}
