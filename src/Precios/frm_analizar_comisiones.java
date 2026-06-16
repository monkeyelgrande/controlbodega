/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Precios;

import Metodos.ExportarExcel;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.File;
import java.io.FileOutputStream;
import java.awt.Desktop;

/**
 *
 * @author Monkeyelgrande
 */
public class frm_analizar_comisiones extends javax.swing.JInternalFrame {

    /**
     * Creates new form frm_clientes
     */
    DefaultTableModel modelVentas = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };
    DefaultTableModel model_costo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };
    DefaultTableModel modelComisiones = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            // Columna 5 = "% Com. WO" (antes era 4)
            if (columna == 5 && fila < getRowCount() - 1) {
                String nombre = getValueAt(fila, 0).toString();
                if (nombre.equalsIgnoreCase("TOTALES")) {
                    return false;
                }
                return true;
            }
            return false;
        }
    };

    boolean ver = false;
    double porcentaje_opeacional = 0;
    boolean recalculando = false;

    public frm_analizar_comisiones() {
        initComponents();
        ResultSet rs = DB_consultas_R_D.getTabla("select porcentaje_operacion from configuraciones where id=1");

        try {
            while (rs.next()) {
                porcentaje_opeacional = rs.getDouble("porcentaje_operacion") / 100;
            }

            rs.close();
        } catch (SQLException ex) {
            System.out.println(ex);
        }

        modelComisiones.addTableModelListener(new javax.swing.event.TableModelListener() {
            @Override
            public void tableChanged(javax.swing.event.TableModelEvent e) {
                if (recalculando) {
                    return;
                }
                int fila = e.getFirstRow();
                int columna = e.getColumn();

                // Columna 5 = "% Com. WO" (antes era 4)
                if (columna == 5 && fila >= 0 && fila < modelComisiones.getRowCount()) {
                    recalculando = true;
                    try {
                        recalcularComisionFila(fila);
                        recalcularFilaTotales();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        recalculando = false;
                    }
                }
            }
        });

    }

    private void recalcularComisionFila(int fila) {
        try {
            // Columna 5 = % Com. WO
            String porcentajeStr = modelComisiones.getValueAt(fila, 5).toString()
                    .replace(",", ".").replace("%", "").trim();
            double porcentaje = Double.parseDouble(porcentajeStr) / 100;

            // Columna 3 = utilidad total WO
            double utilidad = Double.parseDouble(
                    metodos.EliminaCaracteres(modelComisiones.getValueAt(fila, 3).toString().trim(), "."));

            // Columna 6 = Comisión WO contado
            double comisionWO = utilidad * porcentaje;
            modelComisiones.setValueAt(metodos.formateador_dinero().format(comisionWO), fila, 6);

            // Columna 8 = Comision Otros
            double comisionOtros = Double.parseDouble(
                    metodos.EliminaCaracteres(modelComisiones.getValueAt(fila, 8).toString().trim(), "."));

            // Columna 9 = Total Comisión
            double totalComision = comisionWO + comisionOtros;
            modelComisiones.setValueAt(metodos.formateador_dinero().format(totalComision), fila, 9);

        } catch (NumberFormatException ex) {
            System.out.println("Valor no válido en porcentaje de comisión: " + ex.getMessage());
        }
    }

    /**
     * Recalcula la fila de TOTALES al final de la tabla.
     */
    private void recalcularFilaTotales() {
        int totalRows = modelComisiones.getRowCount();
        if (totalRows < 2) {
            return;
        }

        int filaTotales = totalRows - 1;
        String nombre = modelComisiones.getValueAt(filaTotales, 0).toString();
        if (!nombre.equalsIgnoreCase("TOTALES")) {
            return;
        }

        double total_ventas = 0, total_costo = 0, total_utilidad_WO = 0;
        double total_comision_WO = 0, total_otros = 0, total_comision_otros = 0, total_comision_general = 0;

        for (int i = 0; i < filaTotales; i++) {
            total_ventas += Double.parseDouble(metodos.EliminaCaracteres(modelComisiones.getValueAt(i, 1).toString(), "."));
            total_costo += Double.parseDouble(metodos.EliminaCaracteres(modelComisiones.getValueAt(i, 2).toString(), "."));
            total_utilidad_WO += Double.parseDouble(metodos.EliminaCaracteres(modelComisiones.getValueAt(i, 3).toString(), "."));
            total_comision_WO += Double.parseDouble(metodos.EliminaCaracteres(modelComisiones.getValueAt(i, 6).toString(), "."));
            total_otros += Double.parseDouble(metodos.EliminaCaracteres(modelComisiones.getValueAt(i, 7).toString(), "."));
            total_comision_otros += Double.parseDouble(metodos.EliminaCaracteres(modelComisiones.getValueAt(i, 8).toString(), "."));
            total_comision_general += Double.parseDouble(metodos.EliminaCaracteres(modelComisiones.getValueAt(i, 9).toString(), "."));
        }

        modelComisiones.setValueAt(metodos.formateador_dinero().format(total_ventas), filaTotales, 1);
        modelComisiones.setValueAt(metodos.formateador_dinero().format(total_costo), filaTotales, 2);
        modelComisiones.setValueAt(metodos.formateador_dinero().format(total_utilidad_WO), filaTotales, 3);
        modelComisiones.setValueAt(metodos.formateador_dinero().format(total_comision_WO), filaTotales, 6);
        modelComisiones.setValueAt(metodos.formateador_dinero().format(total_otros), filaTotales, 7);
        modelComisiones.setValueAt(metodos.formateador_dinero().format(total_comision_otros), filaTotales, 8);
        modelComisiones.setValueAt(metodos.formateador_dinero().format(total_comision_general), filaTotales, 9);
    }

    public void TamanosTablaAbonos() {
        jtabla_ventas.getTableHeader().setReorderingAllowed(false);
        TableColumnModel columnModel = jtabla_ventas.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(40);
        columnModel.getColumn(1).setPreferredWidth(200);

    }

    public void TamanosTablaComision() {
        jtabla_comisiones.getTableHeader().setReorderingAllowed(false);
        TableColumnModel columnModel = jtabla_comisiones.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(200);
        columnModel.getColumn(1).setPreferredWidth(100);

    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpnl_tabla = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtabla_ventas = new org.jdesktop.swingx.JXTable();
        btn_importar_ventas = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtabla_costos = new org.jdesktop.swingx.JXTable();
        btn_importar_ventas1 = new javax.swing.JButton();
        btn_unir = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jtabla_comisiones = new org.jdesktop.swingx.JXTable();
        btn_analizar = new javax.swing.JButton();
        txt_comision_wo = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        txt_rem_credito = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        txt_rem_contado = new javax.swing.JTextField();
        txt_recibos_wo = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        lbl_total_credito = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel19 = new javax.swing.JLabel();
        txt_comision_otros = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        lbl_total_wo = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        txt_Porcentaje_rem_Credito = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        txt_porcentaje_rem_contado = new javax.swing.JTextField();
        jalbel11 = new javax.swing.JLabel();
        lbl_total_otros = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        lbl_total_contado = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jlabel12 = new javax.swing.JLabel();
        txt_porcentaje_recibos_caja = new javax.swing.JTextField();
        lbl_total_recibos = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        btn_pdf = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Bodegas");

        jpnl_tabla.setBackground(new java.awt.Color(33, 33, 33));

        jtabla_ventas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_ventas.setFont(new java.awt.Font("Yu Gothic Medium", 0, 14)); // NOI18N
        jtabla_ventas.setRowHeight(20);
        jtabla_ventas.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jScrollPane2.setViewportView(jtabla_ventas);

        btn_importar_ventas.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_importar_ventas.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/bodega.png"))); // NOI18N
        btn_importar_ventas.setMnemonic('n');
        btn_importar_ventas.setText("Importar Ventas");
        btn_importar_ventas.setBorder(null);
        btn_importar_ventas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_importar_ventasActionPerformed(evt);
            }
        });

        jtabla_costos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_costos.setFont(new java.awt.Font("Yu Gothic Medium", 0, 14)); // NOI18N
        jtabla_costos.setRowHeight(20);
        jtabla_costos.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jScrollPane3.setViewportView(jtabla_costos);

        btn_importar_ventas1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_importar_ventas1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/gastos.png"))); // NOI18N
        btn_importar_ventas1.setMnemonic('n');
        btn_importar_ventas1.setText("Importar Precios de costo");
        btn_importar_ventas1.setBorder(null);
        btn_importar_ventas1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_importar_ventas1ActionPerformed(evt);
            }
        });

        btn_unir.setBackground(new java.awt.Color(0, 153, 51));
        btn_unir.setFont(new java.awt.Font("Arial Black", 1, 24)); // NOI18N
        btn_unir.setText("<<");
        btn_unir.setEnabled(false);
        btn_unir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_unirActionPerformed(evt);
            }
        });

        jButton1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jButton1.setText("Exportar a excel");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jpnl_tablaLayout = new javax.swing.GroupLayout(jpnl_tabla);
        jpnl_tabla.setLayout(jpnl_tablaLayout);
        jpnl_tablaLayout.setHorizontalGroup(
            jpnl_tablaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpnl_tablaLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpnl_tablaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jpnl_tablaLayout.createSequentialGroup()
                        .addComponent(btn_importar_ventas, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 823, Short.MAX_VALUE)
                        .addComponent(jButton1))
                    .addComponent(jScrollPane2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_unir)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpnl_tablaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 598, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_importar_ventas1, javax.swing.GroupLayout.PREFERRED_SIZE, 218, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jpnl_tablaLayout.setVerticalGroup(
            jpnl_tablaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jpnl_tablaLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jpnl_tablaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btn_importar_ventas, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_importar_ventas1, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jpnl_tablaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(btn_unir, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel4.setBackground(new java.awt.Color(51, 51, 51));
        jPanel4.setPreferredSize(new java.awt.Dimension(146, 80));

        jLabel13.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("Comisiones");

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

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton2)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel13)
                    .addComponent(jButton2))
                .addGap(27, 27, 27))
        );

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jtabla_comisiones.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_comisiones.setFont(new java.awt.Font("Yu Gothic Medium", 0, 14)); // NOI18N
        jtabla_comisiones.setRowHeight(20);
        jtabla_comisiones.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jScrollPane4.setViewportView(jtabla_comisiones);

        btn_analizar.setBackground(new java.awt.Color(0, 204, 204));
        btn_analizar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_analizar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/bodega.png"))); // NOI18N
        btn_analizar.setMnemonic('n');
        btn_analizar.setText("ANALIZAR");
        btn_analizar.setBorder(null);
        btn_analizar.setEnabled(false);
        btn_analizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_analizarActionPerformed(evt);
            }
        });

        txt_comision_wo.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txt_comision_wo.setText("5");

        jLabel14.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(51, 51, 51));
        jLabel14.setText("% Comisión WO");

        jLabel15.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(51, 102, 0));
        jLabel15.setText("Rem credito");

        txt_rem_credito.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txt_rem_credito.setText("0");
        txt_rem_credito.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_rem_creditoFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_rem_creditoFocusLost(evt);
            }
        });

        jLabel16.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(153, 0, 0));
        jLabel16.setText("Rem Contado");

        txt_rem_contado.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txt_rem_contado.setText("0");
        txt_rem_contado.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_rem_contadoFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_rem_contadoFocusLost(evt);
            }
        });

        txt_recibos_wo.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txt_recibos_wo.setText("0");
        txt_recibos_wo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_recibos_woFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_recibos_woFocusLost(evt);
            }
        });

        jLabel17.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(153, 153, 0));
        jLabel17.setText("Recibos de caja WO");

        jLabel18.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(51, 51, 51));
        jLabel18.setText("Total otros:");

        lbl_total_credito.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_total_credito.setForeground(new java.awt.Color(51, 153, 0));
        lbl_total_credito.setText("0.0");
        lbl_total_credito.setToolTipText("Este valor se multiplica por el porcentaje que esta al frente\npara calcular la utilidad que se destinara a comisiones");

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jLabel19.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(51, 51, 51));
        jLabel19.setText("% Comisión OTROS");

        txt_comision_otros.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txt_comision_otros.setText("1");

        jLabel20.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(51, 51, 51));
        jLabel20.setText("Total Ventas WO:");

        lbl_total_wo.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_total_wo.setForeground(new java.awt.Color(51, 153, 0));
        lbl_total_wo.setText("0.0");

        jButton3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jButton3.setText("Exportar a excel");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        txt_Porcentaje_rem_Credito.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txt_Porcentaje_rem_Credito.setText("0");
        txt_Porcentaje_rem_Credito.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_Porcentaje_rem_CreditoFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_Porcentaje_rem_CreditoFocusLost(evt);
            }
        });
        txt_Porcentaje_rem_Credito.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_Porcentaje_rem_CreditoKeyPressed(evt);
            }
        });

        jLabel21.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel21.setForeground(new java.awt.Color(51, 102, 0));
        jLabel21.setText("% Util.");

        txt_porcentaje_rem_contado.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txt_porcentaje_rem_contado.setText("0");
        txt_porcentaje_rem_contado.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_porcentaje_rem_contadoFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_porcentaje_rem_contadoFocusLost(evt);
            }
        });
        txt_porcentaje_rem_contado.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_porcentaje_rem_contadoKeyPressed(evt);
            }
        });

        jalbel11.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jalbel11.setForeground(new java.awt.Color(153, 0, 0));
        jalbel11.setText("% Util.");

        lbl_total_otros.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_total_otros.setForeground(new java.awt.Color(51, 153, 0));
        lbl_total_otros.setText("0.0");

        jLabel22.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel22.setForeground(new java.awt.Color(51, 102, 0));
        jLabel22.setText("T. Credito");

        lbl_total_contado.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_total_contado.setForeground(new java.awt.Color(51, 153, 0));
        lbl_total_contado.setText("0.0");
        lbl_total_contado.setToolTipText("Este valor se multiplica por el porcentaje que esta al frente\npara calcular la utilidad que se destinara a comisiones");

        jLabel23.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel23.setForeground(new java.awt.Color(153, 0, 0));
        jLabel23.setText("T. Contado");
        jLabel23.setToolTipText("Este valor se calcula con el rem contado y se saca el porcentaje espesificado al frente");

        jlabel12.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jlabel12.setForeground(new java.awt.Color(153, 153, 0));
        jlabel12.setText("% Util.");

        txt_porcentaje_recibos_caja.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txt_porcentaje_recibos_caja.setText("0");
        txt_porcentaje_recibos_caja.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txt_porcentaje_recibos_cajaFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txt_porcentaje_recibos_cajaFocusLost(evt);
            }
        });
        txt_porcentaje_recibos_caja.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_porcentaje_recibos_cajaKeyPressed(evt);
            }
        });

        lbl_total_recibos.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_total_recibos.setForeground(new java.awt.Color(51, 153, 0));
        lbl_total_recibos.setText("0.0");
        lbl_total_recibos.setToolTipText("El valor de recibos se divide en 1.19 para sacar el antes de iva \ny este valor se multiplica por el porcentaje que esta al frente\npara calcular la utilidad que se destinara a comisiones");

        jLabel24.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(153, 153, 0));
        jLabel24.setText("T. Recibos");
        jLabel24.setToolTipText("Este valor se calcula con el rem contado y se saca el porcentaje espesificado al frente");

        btn_pdf.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_pdf.setText("Exportar a PDF");
        btn_pdf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_pdfActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel23)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_contado))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel17)
                            .addComponent(txt_recibos_wo, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jlabel12)
                            .addComponent(txt_porcentaje_recibos_caja, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_recibos))
                    .addComponent(jLabel18)
                    .addComponent(lbl_total_otros)
                    .addComponent(jSeparator3)
                    .addComponent(jLabel19)
                    .addComponent(jLabel14)
                    .addComponent(txt_comision_otros)
                    .addComponent(txt_comision_wo)
                    .addComponent(btn_analizar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel15)
                                .addGap(66, 66, 66))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabel22)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(lbl_total_credito)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addComponent(txt_rem_credito))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel21)
                            .addComponent(txt_Porcentaje_rem_Credito, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txt_rem_contado, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel16))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jalbel11)
                            .addComponent(txt_porcentaje_rem_contado, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jSeparator2, javax.swing.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
                    .addComponent(jSeparator4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 149, Short.MAX_VALUE)
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_wo)
                        .addGap(1020, 1020, 1020)
                        .addComponent(btn_pdf)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3))
                    .addComponent(jScrollPane4))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btn_analizar, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txt_comision_wo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txt_comision_otros, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel15)
                            .addComponent(jLabel21))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txt_rem_credito, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txt_Porcentaje_rem_Credito, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbl_total_credito)
                            .addComponent(jLabel22))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel16)
                            .addComponent(jalbel11))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txt_rem_contado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txt_porcentaje_rem_contado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(2, 2, 2)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbl_total_contado)
                            .addComponent(jLabel23))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel17)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txt_recibos_wo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jlabel12)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txt_porcentaje_recibos_caja, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lbl_total_recibos)
                            .addComponent(jLabel24))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel18)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_otros)
                        .addGap(0, 10, Short.MAX_VALUE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 513, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(lbl_total_wo)
                    .addComponent(jButton3)
                    .addComponent(btn_pdf))
                .addContainerGap())
            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 1890, Short.MAX_VALUE)
            .addComponent(jpnl_tabla, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpnl_tabla, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
private boolean esFecha(String valor) {
        return valor.matches("\\d{2}/\\d{2}/\\d{4}");
    }

    private void btn_importar_ventasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_importar_ventasActionPerformed
// Asumiendo que modelVentas y jtabla_ventas ya han sido definidos

        try {
            for (int i = 0; i < modelVentas.getRowCount(); i++) {
                modelVentas.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
        modelVentas.setColumnIdentifiers(new Object[]{"Nombres", "Código", "Cantidad", "Total_Ingresos", "Costo U", "Costo Total", "Utilidad", "% Util."});

        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filtro = new FileNameExtensionFilter("Archivos CSV", "csv");
        fileChooser.setFileFilter(filtro);

        int estado = fileChooser.showOpenDialog(this);
        if (estado == JFileChooser.APPROVE_OPTION) {
            File csvFile = fileChooser.getSelectedFile();

            // Variable para almacenar el último vendedor (en caso de que la celda esté vacía)
            String ultimoVendedor = "";

            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                String line;
                int lineIndex = 0;
                int expectedColumns = 15;  // Número de columnas esperadas según el CSV

                while ((line = br.readLine()) != null) {
                    line = line.replaceAll(";+$", "");

                    // Saltamos las 3 primeras líneas (títulos y otros metadatos)
                    if (lineIndex < 3) {
                        lineIndex++;
                        continue;
                    }

                    // Si la línea es el encabezado (comienza con "Vendedor;"), la saltamos
                    if (line.toLowerCase().startsWith("vendedor;")) {
                        lineIndex++;
                        continue;
                    }

                    // Separamos la línea por ";" y preservamos los campos vacíos (-1)
                    String[] splitted = line.split(";", -1);
                    List<String> parts = new ArrayList<>(Arrays.asList(splitted));

                    // Si existen más columnas de las esperadas,
                    // es probable que el campo "Descripción" se haya partido por contener ";"
                    if (parts.size() > expectedColumns) {
                        // Los primeros 5 campos son fijos: [0] Vendedor, [1] FormaDePago, [2] Documento, [3] Fecha, [4] Tercero.
                        // El campo Descripción debería estar en la posición 5, pero si se partió, se extiende hasta:
                        //   descriptionEndIndex = parts.size() - 9
                        // (los últimos 9 campos corresponden a: [6] Dias, [7] Cantidad, [8] Valor Unit, [9] Venta Bruta,
                        // [10] Descuento, [11] Venta Neta, [12] ImpoConsumo, [13] IVA, [14] Valor Total)
                        int descriptionEndIndex = parts.size() - 9;
                        StringBuilder descripcionMerged = new StringBuilder();
                        for (int i = 5; i < descriptionEndIndex; i++) {
                            if (i > 5) {
                                descripcionMerged.append(";");
                            }
                            descripcionMerged.append(parts.get(i));
                        }
                        // Se eliminan las columnas extra que corresponden al campo Descripción
                        for (int i = 5; i < descriptionEndIndex; i++) {
                            parts.remove(5); // Siempre se elimina en la misma posición ya que la lista se desplaza
                        }
                        // Se agrega la descripción combinada en la posición 5
                        parts.add(5, descripcionMerged.toString());
                    }

                    // Si la cantidad de columnas es menor a la esperada, se omite la fila
                    if (parts.size() < expectedColumns) {
                        lineIndex++;
                        continue;
                    }

                    // Extraemos los datos de interés:
                    String vendedor = parts.get(0).trim();        // Columna 0: Vendedor
                    String descripcion = parts.get(5).trim();       // Columna 5: Descripción
                    String cantidad = parts.get(7).trim();          // Columna 7: Cantidad
                    String valorTotal = parts.get(9).trim();       // Columna 9: Valor bruto

                    // Si el campo vendedor está vacío, reutilizamos el último valor leído
                    if (vendedor.isEmpty()) {
                        vendedor = ultimoVendedor;
                    } else {
                        ultimoVendedor = vendedor;
                    }

                    // Se omiten filas que sean totales
                    if (vendedor.toLowerCase().contains("total")) {
                        lineIndex++;
                        continue;
                    }

                    // Se extrae el código del producto desde la descripción: se toma el primer token (hasta el primer espacio)
                    String codigo = "";
                    String[] partesDescripcion = descripcion.split("\\s+");
                    if (partesDescripcion.length > 0) {
                        codigo = partesDescripcion[0].replaceAll("^\"+", "").replaceAll("\"+$", "");
                    }

                    // Convertir los valores numéricos: 
                    // Se eliminan los puntos (separador de miles) y se reemplaza la coma decimal por punto
                    cantidad = cantidad.replace(".", "").replace(",", ".");
                    valorTotal = valorTotal.replace(".", "").replace(",", ".");

                    double total = Double.parseDouble(valorTotal);

                    // Agregamos la fila al modelo
                    modelVentas.addRow(new Object[]{
                        vendedor,
                        codigo,
                        cantidad,
                        metodos.formateador_dinero().format(total)
                    });

                    lineIndex++;
                } // fin del while

                // Se asigna el modelo a la JTable
                jtabla_ventas.setModel(modelVentas);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        this,
                        "Error al leer el CSV: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }

        }


    }//GEN-LAST:event_btn_importar_ventasActionPerformed
    private String obtenerValorCelda(Cell cell) {
        if (cell == null) {
            return "";
        }
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    public void validar_numeros(java.awt.event.KeyEvent evt, char car) {
        if ((car < '0' || car > '9')) {
            evt.consume();
        }
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        this.dispose();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void btn_importar_ventas1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_importar_ventas1ActionPerformed
        // Configuramos el modelo para la tabla de costos con las columnas "Código" y "Costo Prom"
        model_costo.setColumnIdentifiers(new Object[]{"Código", "Costo Prom"});

        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filtro = new FileNameExtensionFilter("Archivos CSV", "csv");
        fileChooser.setFileFilter(filtro);

        int estado = fileChooser.showOpenDialog(this);
        if (estado == JFileChooser.APPROVE_OPTION) {
            File csvFile = fileChooser.getSelectedFile();

            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                String line;
                int lineIndex = 0;
                while ((line = br.readLine()) != null) {
                    // Omitir las primeras 3 líneas (empresa, título y encabezado)
                    if (lineIndex < 3) {
                        lineIndex++;
                        continue;
                    }

                    // Separamos la línea usando el delimitador ";"
                    String[] parts = line.split(";");
                    // Se espera tener al menos 9 columnas. Si se tiene más, es porque la descripción incluía ";".
                    if (parts.length < 9) {
                        lineIndex++;
                        continue;
                    }

                    // Las últimas 8 columnas corresponden a los datos fijos: 
                    // [Cant Vend, Venta Total, Venta Prom, Costo Total, Costo Prom, Rent Prom, Rent Total, %]
                    // Por ello, el número de partes que corresponden a la descripción es:
                    int descriptionPartsCount = parts.length - 8;
                    // Reconstruimos la descripción concatenando todas las partes correspondientes
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < descriptionPartsCount; i++) {
                        if (i > 0) {
                            sb.append(";");
                        }
                        sb.append(parts[i]);
                    }
                    String descripcion = sb.toString().trim();

                    // Extraemos el código: se asume que es la primera palabra de la descripción.
                    String codigo = "";
                    if (!descripcion.isEmpty()) {
                        String[] descParts = descripcion.split("\\s+");
                        if (descParts.length > 0) {
                            codigo = descParts[0]
                                    .replaceAll("^\"+", "")
                                    .replaceAll("\"+$", "");
                        }
                    }

                    // Se extrae "Costo Prom": en el CSV es la 6ª columna (contando desde 0 en la fila reconstruida)
                    // Por lo tanto, al tener las últimas 8 columnas fijas, "Costo Prom" está en:
                    // índice = (parts.length - 8) + 5 = parts.length - 3
                    String costoProm = parts[parts.length - 4].trim();

                    double costo = Double.parseDouble(metodos.ReemplazarCaracteres(metodos.EliminaCaracteres(costoProm, "."), ",", "."));
                    // Agregamos la fila al modelo de costos
                    model_costo.addRow(new Object[]{codigo, metodos.formateador_dinero().format(costo + (costo * porcentaje_opeacional))});

                    lineIndex++;
                } // fin del while

                // Asignamos el modelo a la JTable de costos
                jtabla_costos.setModel(model_costo);
                btn_unir.setEnabled(true);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        this,
                        "Error al leer el CSV: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }


    }//GEN-LAST:event_btn_importar_ventas1ActionPerformed

    private void btn_unirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_unirActionPerformed

        // Obtenemos los modelos de las tablas
        // Recorremos cada fila de la tabla de ventas
        for (int i = 0; i < modelVentas.getRowCount(); i++) {
            // Obtenemos el código de la fila de ventas (segunda columna, índice 1)
            String codigoVenta = modelVentas.getValueAt(i, 1).toString().trim();
            double costoProm = 0;
            boolean encontrado = false;

            // Buscamos el código en la tabla de costos
            for (int j = 0; j < model_costo.getRowCount(); j++) {
                String codigoCosto = model_costo.getValueAt(j, 0).toString().trim();
                if (codigoCosto.equalsIgnoreCase(codigoVenta)) {
                    try {
                        // Obtenemos el costo prom (columna 1 de jtabla_costos)
                        costoProm = Double.parseDouble(metodos.EliminaCaracteres(model_costo.getValueAt(j, 1).toString().trim(), "."));
                        encontrado = true;
                    } catch (NumberFormatException ex) {
                        ex.printStackTrace();
                    }
                    break;
                }
            }

            // Si se encontró el costo para el código
            if (encontrado) {
                // Actualizamos la columna "Costo U" (índice 4) en jtabla_ventas
                modelVentas.setValueAt(metodos.formateador_dinero().format(costoProm), i, 4);

                try {
                    // Obtenemos la cantidad (columna 2) y calculamos el costo total
                    double cantidad = Double.parseDouble(modelVentas.getValueAt(i, 2).toString().trim());
                    double costoTotal = costoProm * cantidad;
                    modelVentas.setValueAt(metodos.formateador_dinero().format(costoTotal), i, 5);

                    // Obtenemos el total de ingresos (columna 3) y calculamos la utilidad
                    double totalIngresos = Double.parseDouble(metodos.ReemplazarCaracteres(metodos.EliminaCaracteres(modelVentas.getValueAt(i, 3).toString().trim(), "."), ",", "."));
//                    double precio_venta_unitario = Double.parseDouble(metodos.ReemplazarCaracteres(metodos.EliminaCaracteres(modelVentas.getValueAt(i, 3).toString().trim(), "."), ",", ".")) / cantidad;

                    double utilidad = totalIngresos - costoTotal;
                    double porcentaje_utilidad = ((totalIngresos - costoTotal) / costoTotal) * 100;

                    modelVentas.setValueAt(metodos.formateador_dinero().format(utilidad), i, 6);
                    modelVentas.setValueAt(metodos.formateador_un_decimal().format(porcentaje_utilidad), i, 7);

                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }

            } else {
                modelVentas.setValueAt(metodos.formateador_dinero().format(costoProm), i, 4);
                modelVentas.setValueAt(metodos.formateador_dinero().format(costoProm), i, 5);
                modelVentas.setValueAt(metodos.formateador_dinero().format(costoProm), i, 6);
                modelVentas.setValueAt(metodos.formateador_dinero().format(0.0), i, 7);

                System.out.println("Código " + codigoVenta + " no encontrado en la tabla de costos.");
            }

        }
        btn_analizar.setEnabled(true);
    }//GEN-LAST:event_btn_unirActionPerformed

    private void btn_analizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_analizarActionPerformed

        consolidarComisiones(sumar_total());
    }//GEN-LAST:event_btn_analizarActionPerformed
    public double sumar_total() {
        double total = 0;
        double rem_credito = Double.parseDouble(metodos.EliminaCaracteres(lbl_total_credito.getText(), "."));
        double rem_contado = Double.parseDouble(metodos.EliminaCaracteres(lbl_total_contado.getText(), "."));
        double recibo_WO = Double.parseDouble(metodos.EliminaCaracteres(lbl_total_recibos.getText(), "."));

        total = rem_contado + rem_credito + recibo_WO;
        lbl_total_otros.setText(metodos.formateador_dinero().format(total));

        return total;

    }
    private void txt_rem_creditoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_rem_creditoFocusGained

        metodos.eliminar_puntos_focus_gained(txt_rem_credito);
    }//GEN-LAST:event_txt_rem_creditoFocusGained

    private void txt_rem_contadoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_rem_contadoFocusGained
        metodos.eliminar_puntos_focus_gained(txt_rem_contado);

    }//GEN-LAST:event_txt_rem_contadoFocusGained

    private void txt_recibos_woFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_recibos_woFocusGained
        metodos.eliminar_puntos_focus_gained(txt_recibos_wo);

    }//GEN-LAST:event_txt_recibos_woFocusGained

    private void txt_rem_creditoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_rem_creditoFocusLost
        metodos.formateo_dinero_en_jtextfield_fucus_lost(txt_rem_credito);

        sumar_total();
    }//GEN-LAST:event_txt_rem_creditoFocusLost

    private void txt_rem_contadoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_rem_contadoFocusLost
        metodos.formateo_dinero_en_jtextfield_fucus_lost(txt_rem_contado);
        sumar_total();

    }//GEN-LAST:event_txt_rem_contadoFocusLost

    private void txt_recibos_woFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_recibos_woFocusLost
        metodos.formateo_dinero_en_jtextfield_fucus_lost(txt_recibos_wo);
        sumar_total();

    }//GEN-LAST:event_txt_recibos_woFocusLost

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        ExportarExcel obj;

        try {
            obj = new ExportarExcel();
            obj.exportarExcel(jtabla_ventas);
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        ExportarExcel obj;

        try {
            obj = new ExportarExcel();
            obj.exportarExcel(jtabla_comisiones);
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void txt_Porcentaje_rem_CreditoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_Porcentaje_rem_CreditoFocusGained
        txt_Porcentaje_rem_Credito.selectAll();

    }//GEN-LAST:event_txt_Porcentaje_rem_CreditoFocusGained

    private void txt_Porcentaje_rem_CreditoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_Porcentaje_rem_CreditoFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_Porcentaje_rem_CreditoFocusLost

    private void txt_porcentaje_rem_contadoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_porcentaje_rem_contadoFocusGained
        txt_porcentaje_rem_contado.selectAll();

    }//GEN-LAST:event_txt_porcentaje_rem_contadoFocusGained

    private void txt_porcentaje_rem_contadoFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_porcentaje_rem_contadoFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_porcentaje_rem_contadoFocusLost

    private void txt_porcentaje_recibos_cajaFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_porcentaje_recibos_cajaFocusGained
        txt_porcentaje_recibos_caja.selectAll();

    }//GEN-LAST:event_txt_porcentaje_recibos_cajaFocusGained

    private void txt_porcentaje_recibos_cajaFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txt_porcentaje_recibos_cajaFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_txt_porcentaje_recibos_cajaFocusLost

    private void txt_Porcentaje_rem_CreditoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_Porcentaje_rem_CreditoKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            double porcentaje = Double.parseDouble(txt_Porcentaje_rem_Credito.getText());
            double valor = Double.parseDouble(metodos.EliminaCaracteres(txt_rem_credito.getText(), "."));

            lbl_total_credito.setText(metodos.formateador_dinero().format((valor * (porcentaje / 100))));
            sumar_total();
        }
    }//GEN-LAST:event_txt_Porcentaje_rem_CreditoKeyPressed

    private void txt_porcentaje_rem_contadoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_porcentaje_rem_contadoKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            double porcentaje = Double.parseDouble(txt_porcentaje_rem_contado.getText());
            double valor = Double.parseDouble(metodos.EliminaCaracteres(txt_rem_contado.getText(), "."));

            lbl_total_contado.setText(metodos.formateador_dinero().format((valor * (porcentaje / 100))));
            sumar_total();
        }
    }//GEN-LAST:event_txt_porcentaje_rem_contadoKeyPressed

    private void txt_porcentaje_recibos_cajaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_porcentaje_recibos_cajaKeyPressed
        char num = evt.getKeyChar();
        if ((num == KeyEvent.VK_ENTER)) {
            double porcentaje = Double.parseDouble(txt_porcentaje_recibos_caja.getText());
            double valor = Double.parseDouble(metodos.EliminaCaracteres(txt_recibos_wo.getText(), "."));

            valor = valor / 1.19;

            lbl_total_recibos.setText(metodos.formateador_dinero().format((valor * (porcentaje / 100))));
            sumar_total();
        }
    }//GEN-LAST:event_txt_porcentaje_recibos_cajaKeyPressed

    private void btn_pdfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_pdfActionPerformed
        if (jtabla_comisiones.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para generar el reporte.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Reporte PDF");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivo PDF", "pdf"));
        fileChooser.setSelectedFile(new java.io.File("Reporte_Comisiones.pdf"));

        int estado = fileChooser.showSaveDialog(this);
        if (estado == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".pdf")) {
                archivo = new File(archivo.getAbsolutePath() + ".pdf");
            }
            generarReportePDF(archivo);
        }
    }//GEN-LAST:event_btn_pdfActionPerformed
    private void generarReportePDF(File archivo) {
        try {
            // ── Configuración del documento (horizontal por la cantidad de columnas) ──
            Document documento = new Document(PageSize.LETTER.rotate(), 30, 30, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(documento, new FileOutputStream(archivo));
            documento.open();

            // ── Colores corporativos ──
            BaseColor azulOscuro = new BaseColor(25, 42, 86);
            BaseColor azulMedio = new BaseColor(41, 65, 122);
            BaseColor grisFondo = new BaseColor(245, 247, 250);
            BaseColor grisLinea = new BaseColor(200, 205, 215);
            BaseColor blancoTexto = BaseColor.WHITE;
            BaseColor textoOscuro = new BaseColor(33, 37, 41);
            BaseColor verdeSutil = new BaseColor(39, 174, 96);
            BaseColor rojoSutil = new BaseColor(192, 57, 43);

            // ── Fuentes ──
            Font fTitulo = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, azulOscuro);
            Font fSubtitulo = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, azulMedio);
            Font fEncabezado = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, blancoTexto);
            Font fCelda = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, textoOscuro);
            Font fCeldaBold = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, textoOscuro);
            Font fTotales = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, blancoTexto);
            Font fPie = new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC, azulMedio);

            // ══════════════════════════════════════════════════════
            //  ENCABEZADO DEL REPORTE
            // ══════════════════════════════════════════════════════
            // Línea decorativa superior
            PdfPTable lineaTop = new PdfPTable(1);
            lineaTop.setWidthPercentage(100);
            PdfPCell celdaLinea = new PdfPCell();
            celdaLinea.setFixedHeight(4f);
            celdaLinea.setBackgroundColor(azulOscuro);
            celdaLinea.setBorder(Rectangle.NO_BORDER);
            lineaTop.addCell(celdaLinea);
            documento.add(lineaTop);
            documento.add(new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, 6)));

            // Título y subtítulo
            Paragraph titulo = new Paragraph("REPORTE DE COMISIONES", fTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);

            // Fecha de generación
            String fechaActual = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
            Paragraph subtitulo = new Paragraph("Generado el " + fechaActual, fSubtitulo);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(5f);
            documento.add(subtitulo);

            // Línea separadora
            PdfPTable lineaSep = new PdfPTable(1);
            lineaSep.setWidthPercentage(100);
            PdfPCell celdaSep = new PdfPCell();
            celdaSep.setFixedHeight(1.5f);
            celdaSep.setBackgroundColor(grisLinea);
            celdaSep.setBorder(Rectangle.NO_BORDER);
            lineaSep.addCell(celdaSep);
            documento.add(lineaSep);
            documento.add(new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, 8)));

            // ══════════════════════════════════════════════════════
            //  RESUMEN EJECUTIVO (tarjetas de resumen)
            // ══════════════════════════════════════════════════════
            int totalFilas = jtabla_comisiones.getRowCount();
            int filasTotales = -1;

            // Buscar la fila "TOTALES"
            for (int i = 0; i < totalFilas; i++) {
                if (jtabla_comisiones.getValueAt(i, 0).toString().equalsIgnoreCase("TOTALES")) {
                    filasTotales = i;
                    break;
                }
            }

            if (filasTotales >= 0) {
                String totalVentas = jtabla_comisiones.getValueAt(filasTotales, 1).toString();
                String totalCosto = jtabla_comisiones.getValueAt(filasTotales, 2).toString();
                String totalUtilidad = jtabla_comisiones.getValueAt(filasTotales, 3).toString();
                String totalComision = jtabla_comisiones.getValueAt(filasTotales, 9).toString();

                PdfPTable resumen = new PdfPTable(4);
                resumen.setWidthPercentage(100);
                resumen.setWidths(new float[]{1, 1, 1, 1});

                String[][] tarjetas = {
                    {"TOTAL VENTAS", totalVentas},
                    {"TOTAL COSTO", totalCosto},
                    {"UTILIDAD TOTAL", totalUtilidad},
                    {"TOTAL COMISIÓN", totalComision}
                };

                BaseColor[] coloresTarjeta = {azulOscuro, azulMedio, verdeSutil, rojoSutil};

                for (int t = 0; t < tarjetas.length; t++) {
                    PdfPTable tarjeta = new PdfPTable(1);

                    // Etiqueta
                    PdfPCell etiqueta = new PdfPCell(new Phrase(tarjetas[t][0],
                            new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, blancoTexto)));
                    etiqueta.setBackgroundColor(coloresTarjeta[t]);
                    etiqueta.setBorder(Rectangle.NO_BORDER);
                    etiqueta.setHorizontalAlignment(Element.ALIGN_CENTER);
                    etiqueta.setPaddingTop(8f);
                    etiqueta.setPaddingBottom(3f);
                    tarjeta.addCell(etiqueta);

                    // Valor
                    PdfPCell valor = new PdfPCell(new Phrase(tarjetas[t][1],
                            new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, coloresTarjeta[t])));
                    valor.setBackgroundColor(grisFondo);
                    valor.setBorder(Rectangle.NO_BORDER);
                    valor.setHorizontalAlignment(Element.ALIGN_CENTER);
                    valor.setPaddingTop(5f);
                    valor.setPaddingBottom(8f);
                    tarjeta.addCell(valor);

                    PdfPCell contenedor = new PdfPCell(tarjeta);
                    contenedor.setBorderColor(grisLinea);
                    contenedor.setBorderWidth(0.5f);
                    contenedor.setPadding(0f);
                    if (t < tarjetas.length - 1) {
                        contenedor.setPaddingRight(6f);
                    }
                    resumen.addCell(contenedor);
                }

                documento.add(resumen);
                documento.add(new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, 10)));
            }

            // ══════════════════════════════════════════════════════
            //  TABLA DE DETALLE
            // ══════════════════════════════════════════════════════
            int numColumnas = modelComisiones.getColumnCount();
            PdfPTable tabla = new PdfPTable(numColumnas);
            tabla.setWidthPercentage(100);

            // Anchos relativos de cada columna
            float[] anchos = new float[]{
                2.8f, // nombre
                1.5f, // total ventas
                1.3f, // total costo
                1.3f, // utilidad total WO
                0.8f, // % Venta
                0.8f, // % Com. WO
                1.3f, // Comisión WO contado
                1.3f, // Otros
                1.3f, // Comision Otros
                1.3f, // Total Comisión
                0.9f // % Ventas Totales
            };
            tabla.setWidths(anchos);

            // ── Encabezados ──
            String[] encabezados = new String[numColumnas];
            for (int c = 0; c < numColumnas; c++) {
                encabezados[c] = modelComisiones.getColumnName(c);
            }

            for (String enc : encabezados) {
                PdfPCell celda = new PdfPCell(new Phrase(enc.toUpperCase(), fEncabezado));
                celda.setBackgroundColor(azulOscuro);
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
                celda.setPaddingTop(6f);
                celda.setPaddingBottom(6f);
                celda.setPaddingLeft(3f);
                celda.setPaddingRight(3f);
                celda.setBorderColor(azulMedio);
                celda.setBorderWidth(0.5f);
                tabla.addCell(celda);
            }
            tabla.setHeaderRows(1);

            // ── Filas de datos ──
            for (int i = 0; i < totalFilas; i++) {
                boolean esTotales = jtabla_comisiones.getValueAt(i, 0).toString().equalsIgnoreCase("TOTALES");
                boolean filaAlterna = (i % 2 == 0);

                for (int j = 0; j < numColumnas; j++) {
                    String valorStr = jtabla_comisiones.getValueAt(i, j) != null
                            ? jtabla_comisiones.getValueAt(i, j).toString() : "";

                    Font fuente;
                    if (esTotales) {
                        fuente = fTotales;
                    } else if (j == 0) {
                        fuente = fCeldaBold;
                    } else {
                        fuente = fCelda;
                    }

                    PdfPCell celda = new PdfPCell(new Phrase(valorStr, fuente));

                    // Alineación: nombre a la izquierda, el resto centrado
                    if (j == 0) {
                        celda.setHorizontalAlignment(Element.ALIGN_LEFT);
                        celda.setPaddingLeft(6f);
                    } else {
                        celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        celda.setPaddingRight(5f);
                    }

                    celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    celda.setPaddingTop(5f);
                    celda.setPaddingBottom(5f);

                    // Colores de fondo
                    if (esTotales) {
                        celda.setBackgroundColor(azulOscuro);
                    } else if (filaAlterna) {
                        celda.setBackgroundColor(grisFondo);
                    } else {
                        celda.setBackgroundColor(BaseColor.WHITE);
                    }

                    celda.setBorderColor(grisLinea);
                    celda.setBorderWidth(0.3f);

                    tabla.addCell(celda);
                }
            }

            documento.add(tabla);

            // ══════════════════════════════════════════════════════
            //  PIE DE PÁGINA
            // ══════════════════════════════════════════════════════
            documento.add(new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, 12)));

            PdfPTable lineaBottom = new PdfPTable(1);
            lineaBottom.setWidthPercentage(100);
            PdfPCell celdaLineaBottom = new PdfPCell();
            celdaLineaBottom.setFixedHeight(1f);
            celdaLineaBottom.setBackgroundColor(grisLinea);
            celdaLineaBottom.setBorder(Rectangle.NO_BORDER);
            lineaBottom.addCell(celdaLineaBottom);
            documento.add(lineaBottom);

            Paragraph pie = new Paragraph(
                    "Documento generado automáticamente · Monkeys Technology · Tecnología en Evolución",
                    fPie
            );
            pie.setAlignment(Element.ALIGN_CENTER);
            pie.setSpacingBefore(5f);
            documento.add(pie);

            // ── Cerrar documento ──
            documento.close();

            JOptionPane.showMessageDialog(this,
                    "Reporte PDF generado exitosamente.\n" + archivo.getAbsolutePath(),
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);

            // Abrir automáticamente el PDF
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivo);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al generar el PDF: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void consolidarComisiones(double total_otros) {
        recalculando = true;

        try {
            for (int i = modelComisiones.getRowCount() - 1; i >= 0; i--) {
                modelComisiones.removeRow(i);
            }
        } catch (Exception e) {
        }

        modelComisiones.setColumnIdentifiers(new Object[]{
            "nombre", "total ventas", "total costo", "utilidad total WO",
            "% Venta", // ← NUEVA columna informativa (posición 4)
            "% Com. WO", // ← ahora posición 5
            "Comisión WO contado", "Otros", "Comision Otros",
            "Total Comisión", "% Ventas Totales"
        });

        // Map para agrupar los totales por empleado
        Map<String, double[]> agregados = new HashMap<>();

        int filas = modelVentas.getRowCount();
        for (int i = 0; i < filas; i++) {
            String nombre = modelVentas.getValueAt(i, 0).toString().trim();

            double totalVentas = 0, totalCosto = 0, utilidad = 0;
            try {
                totalVentas = Double.parseDouble(metodos.EliminaCaracteres(modelVentas.getValueAt(i, 3).toString().trim(), "."));
            } catch (Exception ex) {
            }
            try {
                totalCosto = Double.parseDouble(metodos.EliminaCaracteres(modelVentas.getValueAt(i, 5).toString().trim(), "."));
            } catch (Exception ex) {
            }
            try {
                utilidad = Double.parseDouble(metodos.EliminaCaracteres(modelVentas.getValueAt(i, 6).toString().trim(), "."));
            } catch (Exception ex) {
            }

            double[] totales = agregados.getOrDefault(nombre, new double[3]);
            totales[0] += totalVentas;
            totales[1] += totalCosto;
            totales[2] += utilidad;
            agregados.put(nombre, totales);
        }

        // Calcular total ventas general ANTES de agregar filas
        double total_ventas_general = 0;
        for (double[] totales : agregados.values()) {
            total_ventas_general += totales[0];
        }

        // Agregar filas
        for (Map.Entry<String, double[]> entrada : agregados.entrySet()) {
            String nombreEmpleado = entrada.getKey();
            double[] totales = entrada.getValue();
            double totalVentasEmpleado = totales[0];
            double totalCostoEmpleado = totales[1];
            double utilidadTotal = totales[2];

            double porcentajeVenta = (total_ventas_general > 0)
                    ? (totalVentasEmpleado / total_ventas_general) * 100 : 0;

            modelComisiones.addRow(new Object[]{
                nombreEmpleado,
                metodos.formateador_dinero().format(totalVentasEmpleado),
                metodos.formateador_dinero().format(totalCostoEmpleado),
                metodos.formateador_dinero().format(utilidadTotal),
                metodos.formateador_dos_decimales().format(porcentajeVenta), // % Venta
                "0", // % Com. WO
                metodos.formateador_dinero().format(0), // Comisión WO contado
            });
        }

        jtabla_comisiones.setModel(modelComisiones);
        TamanosTablaComision();

        double total_ventas = 0;
        for (int i = 0; i < modelComisiones.getRowCount(); i++) {
            total_ventas += Double.parseDouble(metodos.EliminaCaracteres(modelComisiones.getValueAt(i, 1).toString(), "."));
        }
        lbl_total_wo.setText(metodos.formateador_dinero().format(total_ventas));

        double porcentajeComisionOtros = Double.parseDouble(txt_comision_otros.getText().trim()) / 100;

        for (int i = 0; i < modelComisiones.getRowCount(); i++) {
            double porcentaje_otros = Double.parseDouble(metodos.EliminaCaracteres(modelComisiones.getValueAt(i, 1).toString(), ".")) / total_ventas;
            double otros = total_otros * porcentaje_otros;

            modelComisiones.setValueAt(metodos.formateador_dinero().format(otros), i, 7);        // Otros
            modelComisiones.setValueAt(metodos.formateador_dinero().format(otros * porcentajeComisionOtros), i, 8); // Comision Otros

            double comisionOtros = otros * porcentajeComisionOtros;
            modelComisiones.setValueAt(metodos.formateador_dinero().format(comisionOtros), i, 9); // Total Comisión
            modelComisiones.setValueAt(metodos.formateador_dos_decimales().format(porcentaje_otros * 100), i, 10); // % Ventas Totales
        }

        calcular_totales();
        recalculando = false;
    }

    public void calcular_totales() {
        double total_ventas = 0;
        double total_costo = 0;
        double total_utilidad_WO = 0;
        double total_comision_WO = 0;
        double total_otros = 0;
        double total_comision_otros = 0;
        double total_comision_general = 0;
        for (int i = 0; i < jtabla_comisiones.getRowCount(); i++) {
            total_ventas += Double.parseDouble(metodos.EliminaCaracteres(jtabla_comisiones.getValueAt(i, 1).toString(), "."));
            total_costo += Double.parseDouble(metodos.EliminaCaracteres(jtabla_comisiones.getValueAt(i, 2).toString(), "."));
            total_utilidad_WO += Double.parseDouble(metodos.EliminaCaracteres(jtabla_comisiones.getValueAt(i, 3).toString(), "."));
            total_comision_WO += Double.parseDouble(metodos.EliminaCaracteres(jtabla_comisiones.getValueAt(i, 6).toString(), "."));
            total_otros += Double.parseDouble(metodos.EliminaCaracteres(jtabla_comisiones.getValueAt(i, 7).toString(), "."));
            total_comision_otros += Double.parseDouble(metodos.EliminaCaracteres(jtabla_comisiones.getValueAt(i, 8).toString(), "."));
            total_comision_general += Double.parseDouble(metodos.EliminaCaracteres(jtabla_comisiones.getValueAt(i, 9).toString(), "."));
        }

        modelComisiones.addRow(new Object[]{"TOTALES",
            metodos.formateador_dinero().format(total_ventas),
            metodos.formateador_dinero().format(total_costo),
            metodos.formateador_dinero().format(total_utilidad_WO),
            "100,00", // % Venta total
            "0",
            metodos.formateador_dinero().format(total_comision_WO),
            metodos.formateador_dinero().format(total_otros),
            metodos.formateador_dinero().format(total_comision_otros),
            metodos.formateador_dinero().format(total_comision_general),
            "0"
        });
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
            java.util.logging.Logger.getLogger(frm_analizar_comisiones.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(frm_analizar_comisiones.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(frm_analizar_comisiones.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(frm_analizar_comisiones.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new frm_analizar_comisiones().setVisible(true);

            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_analizar;
    private javax.swing.JButton btn_importar_ventas;
    private javax.swing.JButton btn_importar_ventas1;
    private javax.swing.JButton btn_pdf;
    private javax.swing.JButton btn_unir;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JLabel jalbel11;
    private javax.swing.JLabel jlabel12;
    private javax.swing.JPanel jpnl_tabla;
    private org.jdesktop.swingx.JXTable jtabla_comisiones;
    private org.jdesktop.swingx.JXTable jtabla_costos;
    private org.jdesktop.swingx.JXTable jtabla_ventas;
    private javax.swing.JLabel lbl_total_contado;
    private javax.swing.JLabel lbl_total_credito;
    private javax.swing.JLabel lbl_total_otros;
    private javax.swing.JLabel lbl_total_recibos;
    private javax.swing.JLabel lbl_total_wo;
    private javax.swing.JTextField txt_Porcentaje_rem_Credito;
    private javax.swing.JTextField txt_comision_otros;
    private javax.swing.JTextField txt_comision_wo;
    private javax.swing.JTextField txt_porcentaje_recibos_caja;
    private javax.swing.JTextField txt_porcentaje_rem_contado;
    private javax.swing.JTextField txt_recibos_wo;
    private javax.swing.JTextField txt_rem_contado;
    private javax.swing.JTextField txt_rem_credito;
    // End of variables declaration//GEN-END:variables
}
