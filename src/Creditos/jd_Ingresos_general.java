/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos;

import Formularios.frm_main;
import Metodos.ExportarExcel;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTable;
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
public class jd_Ingresos_general extends javax.swing.JDialog {

    /**
     * Creates new form jd_Ventas_diarias
     */
    static DefaultTableModel modelo_creditos = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    static DefaultTableModel modelo_interes = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    static DefaultTableModel modelo_arriendos = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    DefaultTableModel modelo_tipos_abono = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };
    TableColumnModel columnModelVentas = null;

    public jd_Ingresos_general(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setTitle("Ingresos");

        // Secciones de prestamos y arriendos: herencia de sisbesol, no aplican
        // en el modulo de creditos de controlbodega (sus tablas no existen).
        jPanel5.setVisible(false);
        jPanel6.setVisible(false);

        columnModelVentas = jtabla_creditos.getColumnModel();
        this.setLocationRelativeTo(parent);
        poner_fechas();
        metodos.addEscapeListenerWindowDialog(this);
        doble_clic_tablas();
        mostrar_tipos_de_abonos();
        jtabla_tipos_abonos.selectAll();
        btn_consultar.doClick();

    }

    public void mostrar_tipos_de_abonos() {
        try {
            for (int i = 0; i < modelo_tipos_abono.getRowCount(); i++) {
                modelo_tipos_abono.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        ResultSet rs = DB_consultas_R_D.getTabla("select * from tipos_abonos order by id");
        modelo_tipos_abono.setColumnIdentifiers(new Object[]{"id", "Nombre"});
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                modelo_tipos_abono.addRow(new Object[]{rs.getString("id"), rs.getString("nombre")});
            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla_tipos_abonos.setModel(modelo_tipos_abono);
            TamanosTablaTipos();
        } catch (Exception e) {
            System.out.println(e + " cargando tipos de abonos");
        }
    }

    public void TamanosTablaTipos() {
        jtabla_tipos_abonos.getTableHeader().setReorderingAllowed(false);
        TableColumnModel columnModel = jtabla_tipos_abonos.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(20);
        columnModel.getColumn(1).setPreferredWidth(200);

    }

    public void poner_fechas() {
        try {
            String fecha1 = DB_consultas_R_D.obtener_fecha_dia1();
            Date date1 = new SimpleDateFormat("yyyy-MM-dd").parse(fecha1);
            jdate_fecha1.setDate(date1);

            String fecha2 = DB_consultas_R_D.obtener_fecha_dia_ultimo();
            Date date2 = new SimpleDateFormat("yyyy-MM-dd").parse(fecha2);
            jdate_fecha2.setDate(date2);
        } catch (Exception e) {
        }
    }

    public void TamanosTablaVentas(TableColumnModel cm) {
        cm.getColumn(0).setPreferredWidth(15);
        cm.getColumn(1).setPreferredWidth(100);
        cm.getColumn(2).setPreferredWidth(80);
    }

    public void doble_clic_tablas() {
        jtabla_creditos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table = (JTable) me.getSource();
                Point p = me.getPoint();
                if (me.getClickCount() == 2) {

                }
            }
        });

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpop_1 = new javax.swing.JPopupMenu();
        jmenu_VerFactura1 = new javax.swing.JMenuItem();
        jpop_2 = new javax.swing.JPopupMenu();
        jmenu_2 = new javax.swing.JMenuItem();
        jPasswordField1 = new javax.swing.JPasswordField();
        jPanel1 = new javax.swing.JPanel();
        jdate_fecha1 = new com.toedter.calendar.JDateChooser();
        jLabel1 = new javax.swing.JLabel();
        btn_consultar = new javax.swing.JButton();
        lbl_Total_3 = new javax.swing.JLabel();
        lbl_Total_Ingresos = new javax.swing.JLabel();
        jdate_fecha2 = new com.toedter.calendar.JDateChooser();
        jLabel8 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla_creditos = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        lbl_Total_Creditos = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla_tipos_abonos = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        lbl_Total_Interes = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtabla_interes = new javax.swing.JTable();
        jLabel6 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jtabla_arriendos = new javax.swing.JTable();
        jLabel9 = new javax.swing.JLabel();
        lbl_Total_Arriendos = new javax.swing.JLabel();
        btn_imprimir = new javax.swing.JButton();

        jmenu_VerFactura1.setText("Ver factura");
        jpop_1.add(jmenu_VerFactura1);

        jmenu_2.setText("jMenuItem1");
        jpop_2.add(jmenu_2);

        jPasswordField1.setText("jPasswordField1");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jdate_fecha1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

        jLabel1.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        jLabel1.setText("Fecha inicio:");

        btn_consultar.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        btn_consultar.setMnemonic('r');
        btn_consultar.setText("Consultar");
        btn_consultar.setToolTipText("ATL+R");
        btn_consultar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_consultarActionPerformed(evt);
            }
        });

        lbl_Total_3.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        lbl_Total_3.setText("Total ingresos:");

        lbl_Total_Ingresos.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        lbl_Total_Ingresos.setForeground(new java.awt.Color(0, 153, 0));
        lbl_Total_Ingresos.setText("0");

        jdate_fecha2.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

        jLabel8.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        jLabel8.setText("Fecha fin:");

        jPanel4.setBackground(new java.awt.Color(139, 195, 74));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jtabla_creditos.setFont(new java.awt.Font("Yu Gothic Medium", 0, 14)); // NOI18N
        jtabla_creditos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_creditos.setComponentPopupMenu(jpop_1);
        jtabla_creditos.setRowHeight(22);
        jtabla_creditos.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtabla_creditos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jtabla_creditos);

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel2.setText("Ingresos por abonos a créditos");

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel3.setText("Total créditos");

        lbl_Total_Creditos.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lbl_Total_Creditos.setText("0");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 926, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbl_Total_Creditos)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)
                .addGap(7, 7, 7)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lbl_Total_Creditos))
                .addContainerGap())
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel4.setText("Tipos de abonos");

        jtabla_tipos_abonos.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtabla_tipos_abonos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_tipos_abonos.setComponentPopupMenu(jpop_1);
        jtabla_tipos_abonos.setRowHeight(22);
        jtabla_tipos_abonos.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtabla_tipos_abonos.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jScrollPane2.setViewportView(jtabla_tipos_abonos);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel5.setBackground(new java.awt.Color(255, 255, 255));

        jLabel5.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel5.setText("Total interes:");

        lbl_Total_Interes.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lbl_Total_Interes.setText("0");

        jtabla_interes.setFont(new java.awt.Font("Yu Gothic Medium", 0, 14)); // NOI18N
        jtabla_interes.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_interes.setRowHeight(22);
        jtabla_interes.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtabla_interes.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jtabla_interes.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(jtabla_interes);

        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel6.setText("Ingresos por Interes");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lbl_Total_Interes)
                        .addGap(1053, 1053, 1053))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 1298, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(lbl_Total_Interes))
                .addContainerGap())
        );

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel7.setText("Ingresos por arriendos");

        jtabla_arriendos.setFont(new java.awt.Font("Yu Gothic Medium", 0, 14)); // NOI18N
        jtabla_arriendos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_arriendos.setRowHeight(22);
        jtabla_arriendos.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtabla_arriendos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jtabla_arriendos.getTableHeader().setReorderingAllowed(false);
        jScrollPane4.setViewportView(jtabla_arriendos);

        jLabel9.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel9.setText("Total arriendo:");

        lbl_Total_Arriendos.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lbl_Total_Arriendos.setText("0");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
            .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel6Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel6Layout.createSequentialGroup()
                            .addComponent(jLabel9)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(lbl_Total_Arriendos)
                            .addGap(1041, 1041, 1041))
                        .addComponent(jScrollPane4)
                        .addGroup(jPanel6Layout.createSequentialGroup()
                            .addComponent(jLabel7)
                            .addGap(0, 388, Short.MAX_VALUE)))
                    .addContainerGap()))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 219, Short.MAX_VALUE)
            .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel6Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel7)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 156, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel9)
                        .addComponent(lbl_Total_Arriendos))
                    .addContainerGap()))
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        btn_imprimir.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btn_imprimir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/print_pequeno.png"))); // NOI18N
        btn_imprimir.setText("Imprimir");
        btn_imprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimirActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jdate_fecha1, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel8)
                .addGap(12, 12, 12)
                .addComponent(jdate_fecha2, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btn_consultar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_Total_3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbl_Total_Ingresos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_imprimir)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel1)
                    .addComponent(jLabel8)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btn_consultar)
                        .addComponent(lbl_Total_3)
                        .addComponent(lbl_Total_Ingresos)
                        .addComponent(btn_imprimir))
                    .addComponent(jdate_fecha1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jdate_fecha2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    public void LimpiarModelos() {
        try {
            for (int i = 0; i < modelo_creditos.getRowCount(); i++) {
                modelo_creditos.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        try {
            for (int i = 0; i < modelo_interes.getRowCount(); i++) {
                modelo_interes.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        try {
            for (int i = 0; i < modelo_arriendos.getRowCount(); i++) {
                modelo_arriendos.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

    }
    private void btn_consultarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_consultarActionPerformed
        LimpiarModelos();
        String fecha1 = "";
        String fecha2 = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");
        fecha1 = sdf.format(jdate_fecha1.getDate());
        fecha2 = sdf.format(jdate_fecha2.getDate());

        String ids = "";
        int[] filas_seleccionadas = jtabla_tipos_abonos.getSelectedRows();

        for (int i = 0; i < filas_seleccionadas.length; i++) {

            ids += jtabla_tipos_abonos.getValueAt(filas_seleccionadas[i], 0) + ",";
        }
        try {
            ids = ids.substring(0, ids.length() - 1);
        } catch (Exception e) {
        }

        String consulta = "SELECT\n"
                + "    ca.id,\n"
                + "    c.nombre AS cliente,\n"
                + "    ca.total,\n"
                + "    ca.fecha,\n"
                + "    t.nombre AS tipo,\n"
                + "    coalesce((select string_agg(distinct f.codigo, ', ') from abonos a join creditos f on a.id_credito=f.id where a.id_cabecera=ca.id), '-') as codigo,\n"
                + "    coalesce((select string_agg(distinct cu.nombre, ', ') from abonos a join creditos f on a.id_credito=f.id join cuentas cu on f.id_cuenta=cu.id where a.id_cabecera=ca.id), '-') as cuenta\n"
                + "FROM abonos_cabeceras ca\n"
                + "     join contactos c on ca.id_contacto=c.id\n"
                + "     join tipos_abonos t on ca.id_tipo_abono=t.id\n"
                + "WHERE \n"
                + "    COALESCE(ca.total, 0) > 0\n"
                + "    AND ca.id_tipo_abono IN (" + ids + ")\n"
                + "    AND ca.fecha BETWEEN '" + fecha1 + "' and '" + fecha2 + "'\n "
                + "order by ca.fecha, ca.id";

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        modelo_creditos.setColumnIdentifiers(new Object[]{"ID abono", "Nombre cliente", "Total", "Fecha", "Tipo", "Código", "Cuenta"});
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                modelo_creditos.addRow(new Object[]{rs.getString("id"), rs.getString("cliente"), metodos.formateador_dinero().format(rs.getDouble("total")),
                    sdf2.format(rs.getDate("fecha")), rs.getString("tipo"), rs.getString("codigo"), rs.getString("cuenta")});
            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla_creditos.setModel(modelo_creditos);
            TamanosTablaVentas(columnModelVentas);
            calcular_total();

        } catch (Exception e) {
            System.out.println(e);
        }

    }//GEN-LAST:event_btn_consultarActionPerformed

    private void btn_imprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imprimirActionPerformed
        jd_reporte_general jd = new jd_reporte_general(null, true);

        jd.jdate_fecha1.setDate(jdate_fecha1.getDate());
        jd.jdate_fecha2.setDate(jdate_fecha2.getDate());
        jd.lbl_Total_Ingresos.setText(lbl_Total_Ingresos.getText());

        jd.modelo.setColumnIdentifiers(new Object[]{"Cliente", "Pago", "Fecha", "Tipo", "Codigo", "Cuenta"});
        for (int i = 0; i < modelo_creditos.getRowCount(); i++) {
            jd.modelo.addRow(new Object[]{modelo_creditos.getValueAt(i, 1).toString(), modelo_creditos.getValueAt(i, 2).toString(), modelo_creditos.getValueAt(i, 3).toString(), "Cré. - " + modelo_creditos.getValueAt(i, 4).toString(),
                modelo_creditos.getValueAt(i, 5).toString(), modelo_creditos.getValueAt(i, 6).toString()});
        }

        for (int i = 0; i < modelo_interes.getRowCount(); i++) {
            jd.modelo.addRow(new Object[]{modelo_interes.getValueAt(i, 1).toString(), modelo_interes.getValueAt(i, 3).toString(), modelo_interes.getValueAt(i, 4).toString(), modelo_interes.getValueAt(i, 2).toString(), "-", "-"});
        }

        for (int i = 0; i < modelo_arriendos.getRowCount(); i++) {
            jd.modelo.addRow(new Object[]{modelo_arriendos.getValueAt(i, 1).toString(), modelo_arriendos.getValueAt(i, 3).toString(), modelo_arriendos.getValueAt(i, 4).toString(), "Arriendo - " + modelo_arriendos.getValueAt(i, 5).toString(), "-", "-"});
        }

        jd.jtabla_interes.setModel(jd.modelo);
        jd.show();
    }//GEN-LAST:event_btn_imprimirActionPerformed
    public void calcular_total() {
        double total_creditos = 0, total_interes = 0, total_arriendo = 0;
        for (int i = 0; i < this.jtabla_creditos.getRowCount(); i++) {
            total_creditos += Double.parseDouble(metodos.EliminaCaracteres(this.jtabla_creditos.getValueAt(i, 2).toString(), "."));
        }
        lbl_Total_Creditos.setText("$ " + metodos.formateador_dinero().format(total_creditos));

        total_interes = 0;
        for (int i = 0; i < this.jtabla_interes.getRowCount(); i++) {
            total_interes += Double.parseDouble(metodos.EliminaCaracteres(this.jtabla_interes.getValueAt(i, 3).toString(), "."));
        }
        lbl_Total_Interes.setText("$ " + metodos.formateador_dinero().format(total_interes));

        total_arriendo = 0;
        for (int i = 0; i < this.jtabla_arriendos.getRowCount(); i++) {
            total_arriendo += Double.parseDouble(metodos.EliminaCaracteres(this.jtabla_arriendos.getValueAt(i, 3).toString(), "."));
        }
        lbl_Total_Arriendos.setText("$ " + metodos.formateador_dinero().format(total_arriendo));

        lbl_Total_Ingresos.setText("$ " + metodos.formateador_dinero().format(total_creditos + total_interes + total_arriendo));
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
            java.util.logging.Logger.getLogger(jd_Ingresos_general.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(jd_Ingresos_general.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(jd_Ingresos_general.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(jd_Ingresos_general.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                jd_Ingresos_general dialog = new jd_Ingresos_general(new javax.swing.JFrame(), true);
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
    private javax.swing.JButton btn_consultar;
    private javax.swing.JButton btn_imprimir;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private com.toedter.calendar.JDateChooser jdate_fecha1;
    private com.toedter.calendar.JDateChooser jdate_fecha2;
    private javax.swing.JMenuItem jmenu_2;
    private javax.swing.JMenuItem jmenu_VerFactura1;
    private javax.swing.JPopupMenu jpop_1;
    private javax.swing.JPopupMenu jpop_2;
    private javax.swing.JTable jtabla_arriendos;
    private javax.swing.JTable jtabla_creditos;
    private javax.swing.JTable jtabla_interes;
    private javax.swing.JTable jtabla_tipos_abonos;
    private javax.swing.JLabel lbl_Total_3;
    private javax.swing.JLabel lbl_Total_Arriendos;
    private javax.swing.JLabel lbl_Total_Creditos;
    private javax.swing.JLabel lbl_Total_Ingresos;
    private javax.swing.JLabel lbl_Total_Interes;
    // End of variables declaration//GEN-END:variables
}
