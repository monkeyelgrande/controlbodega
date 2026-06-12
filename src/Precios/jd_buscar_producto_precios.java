package Precios;

import Metodos.TextPrompt;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * Busqueda de productos para el modulo Precios (portado del jd_buscar_producto
 * de productos-agroinsumos). Agrega el producto seleccionado a la tabla de
 * jif_crear_ingreso_precios segun el rol activo.
 *
 * @author Monkeyelgrande
 */
public class jd_buscar_producto_precios extends javax.swing.JDialog {

    static TableColumnModel columnModel = null;
    /** "ingreso" (por defecto) o "imprimir" (etiquetas). */
    public static String formulario = "ingreso";

    public jd_buscar_producto_precios(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        columnModel = jtabla.getColumnModel();
        metodos.BuscarEnTabla(txt_Filtro, jtabla);

        mostrar();
        TextPrompt descripcion = new TextPrompt("Descripción del producto", txt_Filtro);

        this.setLocationRelativeTo(parent);
        txt_Filtro.requestFocus();
        metodos.addEscapeListenerWindowDialog(this);

        jtabla.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_ENTER) {
                    btn_agregar.doClick();
                }
            }
        });
        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table = (JTable) me.getSource();
                Point p = me.getPoint();
                int row = table.rowAtPoint(p);
                if (me.getClickCount() == 2) {
                    btn_agregarActionPerformed(null);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        txt_Filtro = new javax.swing.JTextField();
        btn_agregar = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Buscar producto");
        setResizable(false);

        txt_Filtro.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        txt_Filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_FiltroKeyPressed(evt);
            }
        });

        btn_agregar.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        btn_agregar.setText("Agregar producto");
        btn_agregar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_agregarActionPerformed(evt);
            }
        });

        jPanel2.setBackground(new java.awt.Color(34, 49, 63));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Consulta de productos", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 14), new java.awt.Color(255, 255, 255))); // NOI18N

        jtabla.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{}
        ));
        jScrollPane1.setViewportView(jtabla);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1)
                                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 473, Short.MAX_VALUE)
                                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, 1015, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btn_agregar, javax.swing.GroupLayout.DEFAULT_SIZE, 536, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btn_agregar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGap(2, 2, 2)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    private boolean existe_en_tabla(String codigo) {
        try {
            for (int i = 0; i < jif_crear_ingreso_precios.jtabla.getRowCount(); i++) {
                if (jif_crear_ingreso_precios.jtabla.getValueAt(i, 1).toString().equals(codigo)) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    private void btn_agregarActionPerformed(java.awt.event.ActionEvent evt) {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
        } else {

            String codigo = jtabla.getValueAt(fila, 0).toString();

            if ("imprimir".equals(formulario)) {
                double cantidadImp;
                try {
                    cantidadImp = Double.parseDouble(JOptionPane.showInputDialog("Ingrese la cantidad de etiquetas, dejar en blanco para igual a 1.\nSOLO NÚMEROS"));
                } catch (Exception e) {
                    cantidadImp = 1.0;
                }
                jd_productos_a_imprimir.modeloProductos.setColumnIdentifiers(new Object[]{"codigo", "descripcion", "Cantidad"});
                jd_productos_a_imprimir.modeloProductos.addRow(new Object[]{
                    jtabla.getValueAt(fila, 0).toString(), jtabla.getValueAt(fila, 1).toString(), cantidadImp});
                jd_productos_a_imprimir.jtabla_productos.setModel(jd_productos_a_imprimir.modeloProductos);
                txt_Filtro.requestFocus();
                txt_Filtro.selectAll();
                return;
            }

            // El mismo producto puede ir a varias bodegas: no se bloquea el
            // duplicado en rol 2 (la validacion de "mismo producto en la misma
            // bodega" se hace al guardar).
            boolean permitirRepetidoBodega = jif_crear_ingreso_precios.replicaBodegaActiva;
            if (existe_en_tabla(codigo) && !permitirRepetidoBodega) {
                JOptionPane.showMessageDialog(this, "El producto que intenta agregar ya se encuentra agregado en la tabla");
            } else {
                double cantidad = 0;
                try {
                    cantidad = Double.parseDouble(JOptionPane.showInputDialog("Ingrese la cantidad, dejar en blanco para igual a 1.\nSOLO NÚMEROS"));
                } catch (Exception e) {
                    cantidad = 1.0;
                }

                if (Formularios.frm_main.rol_precios == 4) {
                    // Rol Precios: agregar con 16 columnas
                    String consultaPrecios = "select p.id, p.codigo_barras, p.descripcion, p.precio_costo, p.iva, p.venta, "
                            + "p.valor_desc_1, p.valor_desc_2, p.valor_s_y_t, p.valor_credito, p.porcentaje_utilidad, "
                            + "c.porcentaje_operacion "
                            + "from productos p, configuraciones c "
                            + "where c.id=1 and p.codigo_barras ='" + codigo + "'";
                    ResultSet rsP = DB_consultas_R_D.getTabla(consultaPrecios);
                    try {
                        while (rsP.next()) {
                            double costo = rsP.getDouble("precio_costo");
                            double iva = rsP.getDouble("iva");
                            double descuento = 0;
                            double porcentaje_operacion = rsP.getDouble("porcentaje_operacion");

                            double costo_iva_descuento = (costo + (costo * (iva / 100))) - ((costo + (costo * (iva / 100))) * (descuento / 100));
                            double costo_iva_descuento_gasto = costo_iva_descuento + (costo_iva_descuento * (porcentaje_operacion / 100));

                            jif_crear_ingreso_precios.modelo_productos.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "COSTO", "IVA", "DESCUENTO", "COSTO+IVA-DESC", "COSTO+IVA+GASTO",
                                "% UTIL.", "VENTA", "VALOR DES. N1", "VALOR DES. N2", "VALOR S Y T", "VALOR CRED.", "E"});

                            jif_crear_ingreso_precios.modelo_productos.addRow(new Object[]{
                                rsP.getString("id"), rsP.getString("codigo_barras"), rsP.getString("descripcion"),
                                cantidad, metodos.formateador_dinero().format(costo), iva, descuento,
                                metodos.formateador_dinero().format(costo_iva_descuento),
                                metodos.formateador_dinero().format(costo_iva_descuento_gasto),
                                rsP.getDouble("porcentaje_utilidad"),
                                metodos.formateador_dinero().format(rsP.getDouble("venta")),
                                metodos.formateador_dinero().format(rsP.getDouble("valor_desc_1")),
                                metodos.formateador_dinero().format(rsP.getDouble("valor_desc_2")),
                                metodos.formateador_dinero().format(rsP.getDouble("valor_s_y_t")),
                                metodos.formateador_dinero().format(rsP.getDouble("valor_credito")),
                                "0"
                            });
                        }
                        rsP.close();
                        jif_crear_ingreso_precios.jtabla.setModel(jif_crear_ingreso_precios.modelo_productos);
                        jif_crear_ingreso_precios.TamanosTablaPrecios();
                        jif_crear_ingreso_precios.btn_calcular_utildiad_porcentaje.setVisible(true);
                    } catch (SQLException ex) {
                        Logger.getLogger(jd_buscar_producto_precios.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    String consulta = "select p.id, p.codigo_barras, p.descripcion, u.nombre as unidad "
                            + "from productos p, unidades_medidas u where p.id_unidad=u.id "
                            + "and p.codigo_barras ='" + codigo + "'";

                    ResultSet rs = DB_consultas_R_D.getTabla(consulta);
                    try {
                        while (rs.next()) {
                            if (jif_crear_ingreso_precios.replicaBodegaActiva) {
                                jif_crear_ingreso_precios.modelo_productos.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"), cantidad + "", 0, jif_crear_ingreso_precios.bodegaPorDefecto()});
                            } else {
                                jif_crear_ingreso_precios.modelo_productos.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"), cantidad + "", 0});
                            }
                        }
                        rs.close();

                        jif_crear_ingreso_precios.jtabla.setModel(jif_crear_ingreso_precios.modelo_productos);
                        jif_crear_ingreso_precios.aplicarEditorBodega();
                        jif_crear_ingreso_precios.calcular_total();
                        jif_crear_ingreso_precios.TamanosTabla();
                        jif_crear_ingreso_precios.txt_codigo_barras.setText("");
                    } catch (SQLException ex) {
                        Logger.getLogger(jd_buscar_producto_precios.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                txt_Filtro.requestFocus();
                txt_Filtro.selectAll();
            }
        }
    }

    private void txt_FiltroKeyPressed(java.awt.event.KeyEvent evt) {
        int key = evt.getKeyCode();
        if ((key == KeyEvent.VK_DOWN)) {
            jtabla.requestFocus();
            jtabla.getSelectionModel().setSelectionInterval(0, 0);
        }
    }

    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public void mostrar() {
        try {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
        }
        modelo.setColumnIdentifiers(new Object[]{"Código", "Descripción", "Unidad"});
        // productos.estado en controlbodega es boolean
        ResultSet rs = DB_consultas_R_D.getTabla("select p.codigo_barras, p.descripcion, u.nombre as unidad "
                + "from productos p, unidades_medidas u where p.id_unidad=u.id and coalesce(p.estado,true)=true");

        try {
            while (rs.next()) {
                modelo.addRow(new Object[]{rs.getString("codigo_barras"), rs.getString("descripcion"), rs.getString("unidad")});
            }
            rs.close();
            jtabla.setModel(modelo);
            TamanosTablaConsulta();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void TamanosTablaConsulta() {
        columnModel.getColumn(0).setPreferredWidth(100);
        columnModel.getColumn(1).setPreferredWidth(800);
        columnModel.getColumn(2).setPreferredWidth(80);
    }

    // Variables declaration
    private javax.swing.JButton btn_agregar;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jtabla;
    private javax.swing.JTextField txt_Filtro;
}
