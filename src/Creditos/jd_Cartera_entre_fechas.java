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
import Creditos.modelos.Cuentas;
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
public class jd_Cartera_entre_fechas extends javax.swing.JDialog {

    /**
     * Creates new form jd_Ventas_diarias
     */
    static DefaultTableModel modelo_ingresos = new DefaultTableModel() {
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

    static DefaultTableModel modelo_creditos = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    TableColumnModel columnModelVentas = null;
    TableColumnModel columnModelCreditos = null;

    public jd_Cartera_entre_fechas(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setTitle("Ingresos");
        columnModelVentas = jtabla_ingresos.getColumnModel();
        columnModelCreditos = jtabla.getColumnModel();

        metodos.EstiloTablaMaterialGlobal(jtabla);
        metodos.EstiloTablaMaterialGlobal(jtabla_ingresos);
        Cuentas cuenta = new Cuentas();
        cuenta.mostrarCuentasConTodas(jbox_Cuentas);
        this.setLocationRelativeTo(parent);
        poner_fechas();
        metodos.addEscapeListenerWindowDialog(this);
        doble_clic_tablas();
        mostrar_tipos_de_abonos();
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
        jtabla_ingresos.addMouseListener(new java.awt.event.MouseAdapter() {
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
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla_ingresos = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        lbl_Total_3 = new javax.swing.JLabel();
        lbl_Total_Ingresos = new javax.swing.JLabel();
        jdate_fecha2 = new com.toedter.calendar.JDateChooser();
        jLabel8 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla_tipos_abonos = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();
        jLabel6 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        lbl_Total_4 = new javax.swing.JLabel();
        lbl_Total_Creditos_1 = new javax.swing.JLabel();
        jbox_Cuentas = new javax.swing.JComboBox<>();

        jmenu_VerFactura1.setText("Ver factura");
        jpop_1.add(jmenu_VerFactura1);

        jmenu_2.setText("jMenuItem1");
        jpop_2.add(jmenu_2);

        jPasswordField1.setText("jPasswordField1");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        jPanel1.setBackground(new java.awt.Color(204, 255, 204));

        jdate_fecha1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel1.setText("Fecha inicio:");

        btn_consultar.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        btn_consultar.setMnemonic('r');
        btn_consultar.setText("Consultar");
        btn_consultar.setToolTipText("ATL+R");
        btn_consultar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_consultarActionPerformed(evt);
            }
        });

        jtabla_ingresos.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtabla_ingresos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_ingresos.setComponentPopupMenu(jpop_1);
        jtabla_ingresos.setRowHeight(22);
        jtabla_ingresos.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtabla_ingresos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jtabla_ingresos);

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel2.setText("Ingresos por abonos a créditos");

        jButton1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
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
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 926, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton1)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addContainerGap())
        );

        lbl_Total_3.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lbl_Total_3.setText("Total ingresos:");

        lbl_Total_Ingresos.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lbl_Total_Ingresos.setForeground(new java.awt.Color(0, 153, 0));
        lbl_Total_Ingresos.setText("0");

        jdate_fecha2.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N

        jLabel8.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel8.setText("Fecha fin:");

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
                .addComponent(jLabel4)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE)
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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jdate_fecha1, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jdate_fecha2, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_consultar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbl_Total_3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_Total_Ingresos)))
                .addContainerGap())
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
                        .addComponent(lbl_Total_Ingresos))
                    .addComponent(jdate_fecha1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jdate_fecha2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel4.setBackground(new java.awt.Color(255, 204, 204));

        jtabla.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
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
        jtabla.setRowHeight(22);
        jtabla.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(jtabla);

        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel6.setText("Creditos creados entre fechas");

        jButton2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jButton2.setText("Exportar a excel");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 1179, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton2)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton2)
                .addContainerGap())
        );

        lbl_Total_4.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lbl_Total_4.setText("Total Créditos:");

        lbl_Total_Creditos_1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lbl_Total_Creditos_1.setForeground(new java.awt.Color(0, 153, 0));
        lbl_Total_Creditos_1.setText("0");

        jbox_Cuentas.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jbox_Cuentas.setForeground(new java.awt.Color(51, 51, 51));
        jbox_Cuentas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbox_CuentasActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jbox_Cuentas, javax.swing.GroupLayout.PREFERRED_SIZE, 258, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lbl_Total_4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_Total_Creditos_1)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbox_Cuentas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lbl_Total_4)
                        .addComponent(lbl_Total_Creditos_1)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    public void LimpiarModelos() {
        try {
            for (int i = 0; i < modelo_ingresos.getRowCount(); i++) {
                modelo_ingresos.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        try {
            for (int i = 0; i < modelo_creditos.getRowCount(); i++) {
                modelo_creditos.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

    }

    public void consultas_creditos(int id_cuenta) {
        String fecha1 = "";
        String fecha2 = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MMM-yyyy");
        fecha1 = sdf.format(jdate_fecha1.getDate());
        fecha2 = sdf.format(jdate_fecha2.getDate());

        String consulta = "select f.id as id_credito, f.codigo, c.nombre as cliente, f.fecha_creacion, f.total, cu.nombre as cuenta\n"
                + "from creditos f, contactos c, cuentas cu \n"
                + "where f.id_cuenta=cu.id and f.id_contacto=c.id and "
                + "cu.id=" + id_cuenta + " and f.fecha_creacion between '" + fecha1 + "' and '" + fecha2 + "' order by fecha_creacion";
        if (id_cuenta == 0) {
            consulta = "select f.id as id_credito, f.codigo, c.nombre as cliente, f.fecha_creacion, f.total, cu.nombre as cuenta\n"
                    + "from creditos f, contactos c, cuentas cu \n"
                    + "where f.id_cuenta=cu.id and f.id_contacto=c.id and "
                    + "f.fecha_creacion between '" + fecha1 + "' and '" + fecha2 + "' order by fecha_creacion";
        }

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        modelo_creditos.setColumnIdentifiers(new Object[]{"Id credito", "Código", "Cliente", "Total", "Fecha creacion", "Cuenta"});
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                modelo_creditos.addRow(new Object[]{rs.getString("id_credito"), rs.getString("codigo"), rs.getString("cliente"),
                    metodos.formateador_dinero().format(rs.getDouble("total")), sdf2.format(rs.getDate("fecha_creacion")), rs.getString("cuenta")});
            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla.setModel(modelo_creditos);
            TamanosTablaVentas(columnModelCreditos);
        } catch (Exception e) {
            System.out.println(e);
        }

        sumar_totales();
    }

    public void sumar_totales() {
        double total_facturas = 0;

        for (int i = 0; i < jtabla.getRowCount(); i++) {
            total_facturas += Double.parseDouble(metodos.EliminaCaracteres(jtabla.getValueAt(i, 3).toString(), "."));

        }
        lbl_Total_Creditos_1.setText(metodos.formateador_dinero().format(total_facturas));

    }
    private void btn_consultarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_consultarActionPerformed
        LimpiarModelos();

        consultas_ingresos();
        consultas_creditos(0);

    }//GEN-LAST:event_btn_consultarActionPerformed
    public void consultas_ingresos() {

        String fecha1 = "";
        String fecha2 = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MMM-yyyy");
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

        String consulta = "select ca.id, c.nombre as cliente, ca.total, ca.fecha, t.nombre as tipo, \n"
                + "coalesce((select string_agg(distinct f.codigo, ', ') from abonos a join creditos f on a.id_credito=f.id where a.id_cabecera=ca.id), '-') as codigo, \n"
                + "coalesce((select string_agg(distinct cu.nombre, ', ') from abonos a join creditos f on a.id_credito=f.id join cuentas cu on f.id_cuenta=cu.id where a.id_cabecera=ca.id), '-') as cuenta \n"
                + "from abonos_cabeceras ca \n"
                + "join contactos c on ca.id_contacto=c.id \n"
                + "join tipos_abonos t on ca.id_tipo_abono=t.id \n"
                + "where coalesce(ca.total,0)>0 and ca.id_tipo_abono in(" + ids + ") and \n"
                + "ca.fecha between '" + fecha1 + "' and '" + fecha2 + "' order by ca.fecha";

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        modelo_ingresos.setColumnIdentifiers(new Object[]{"ID abono", "Nombre cliente", "Codigo Factura", "Total", "Fecha", "Tipo", "Cuenta"});
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                modelo_ingresos.addRow(new Object[]{rs.getString("id"), rs.getString("cliente"), rs.getString("codigo"), metodos.formateador_dinero().format(rs.getDouble("total")),
                    sdf2.format(rs.getDate("fecha")), rs.getString("tipo"), rs.getString("cuenta")});
            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla_ingresos.setModel(modelo_ingresos);
            TamanosTablaVentas(columnModelVentas);
            calcular_total();

        } catch (Exception e) {
            System.out.println(e);
        }

    }
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        ExportarExcel obj;

        try {
            obj = new ExportarExcel();
            obj.exportarExcel(jtabla_ingresos);
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        ExportarExcel obj;

        try {
            obj = new ExportarExcel();
            obj.exportarExcel(jtabla);
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jbox_CuentasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbox_CuentasActionPerformed
        int id_cu = jbox_Cuentas.getItemAt(jbox_Cuentas.getSelectedIndex()).getId();
        consultas_creditos(id_cu);
    }//GEN-LAST:event_jbox_CuentasActionPerformed

    public void calcular_total() {
        double total_cantidad = 0;

        for (int i = 0; i < this.jtabla_ingresos.getRowCount(); i++) {
            total_cantidad += Double.parseDouble(metodos.EliminaCaracteres(this.jtabla_ingresos.getValueAt(i, 3).toString(), "."));
        }

        lbl_Total_Ingresos.setText("$ " + metodos.formateador_dinero().format(total_cantidad));
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
            java.util.logging.Logger.getLogger(jd_Cartera_entre_fechas.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(jd_Cartera_entre_fechas.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(jd_Cartera_entre_fechas.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(jd_Cartera_entre_fechas.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                jd_Cartera_entre_fechas dialog = new jd_Cartera_entre_fechas(new javax.swing.JFrame(), true);
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
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    public static javax.swing.JComboBox<Creditos.modelos.Cuentas> jbox_Cuentas;
    private com.toedter.calendar.JDateChooser jdate_fecha1;
    private com.toedter.calendar.JDateChooser jdate_fecha2;
    private javax.swing.JMenuItem jmenu_2;
    private javax.swing.JMenuItem jmenu_VerFactura1;
    private javax.swing.JPopupMenu jpop_1;
    private javax.swing.JPopupMenu jpop_2;
    private javax.swing.JTable jtabla;
    private javax.swing.JTable jtabla_ingresos;
    private javax.swing.JTable jtabla_tipos_abonos;
    private javax.swing.JLabel lbl_Total_3;
    private javax.swing.JLabel lbl_Total_4;
    private javax.swing.JLabel lbl_Total_Creditos_1;
    private javax.swing.JLabel lbl_Total_Ingresos;
    // End of variables declaration//GEN-END:variables
}
