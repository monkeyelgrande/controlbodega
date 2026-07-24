package Creditos;

import Creditos.jif_crear_credito;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import Creditos.modelos.Contactos;

public class jd_buscar_cliente_credito extends javax.swing.JDialog {

    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public jd_buscar_cliente_credito(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        mostrar();
        this.setLocationRelativeTo(parent);
        txt_Filtro.requestFocus();
        metodos.addEscapeListenerWindowDialog(this);

        metodos.BuscarEnTabla(txt_Filtro, jtabla_clientes);

        // Doble clic en la tabla selecciona el cliente
        jtabla_clientes.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                JTable table = (JTable) me.getSource();
                Point p = me.getPoint();
                int row = table.rowAtPoint(p);
                if (me.getClickCount() == 2 && row >= 0) {
                    btn_agregarActionPerformed(null);
                }
            }
        });

        // Enter en la tabla selecciona el cliente
        jtabla_clientes.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btn_agregarActionPerformed(null);
                }
            }
        });

        // Enter en el textfield pasa a la tabla o selecciona si solo hay una fila
        txt_Filtro.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (jtabla_clientes.getRowCount() == 1) {
                        jtabla_clientes.setRowSelectionInterval(0, 0);
                        btn_agregarActionPerformed(null);
                    } else if (jtabla_clientes.getRowCount() > 0) {
                        jtabla_clientes.requestFocus();
                        jtabla_clientes.setRowSelectionInterval(0, 0);
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla_clientes = new org.jdesktop.swingx.JXTable();
        txt_Filtro = new javax.swing.JTextField();
        btn_agregar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Buscar cliente");
        setResizable(false);

        jtabla_clientes.setModel(new javax.swing.table.DefaultTableModel(
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
        jtabla_clientes.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jtabla_clientes);

        txt_Filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_FiltroKeyTyped(evt);
            }
        });

        btn_agregar.setText("Seleccionar cliente");
        btn_agregar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_agregarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 826, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, 390, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btn_agregar)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_agregar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 422, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txt_FiltroKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_FiltroKeyTyped

    }//GEN-LAST:event_txt_FiltroKeyTyped

    private void btn_agregarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_agregarActionPerformed
        int fila = jtabla_clientes.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente");
            return;
        }
        int filaModelo = jtabla_clientes.convertRowIndexToModel(fila);
        String id = (String) modelo.getValueAt(filaModelo, 0);
        seleccionarEnCombo(Integer.parseInt(id));
        this.dispose();
    }//GEN-LAST:event_btn_agregarActionPerformed

    private void seleccionarEnCombo(int idContacto) {
        javax.swing.JComboBox<Contactos> combo = jif_crear_credito.cbx_contacto;
        if (combo == null) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            Contactos c = combo.getItemAt(i);
            if (c != null && c.getId() == idContacto) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        // Si no estaba en el combo (p. ej. cliente nuevo), se recarga y se intenta de nuevo
        new Contactos().MostrarNombreContactos(combo);
        for (int i = 0; i < combo.getItemCount(); i++) {
            Contactos c = combo.getItemAt(i);
            if (c != null && c.getId() == idContacto) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    public void mostrar() {
        modelo.setRowCount(0);
        modelo.setColumnIdentifiers(new Object[]{"id", "Nombre", "Cedula", "Celular"});
        ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre,cedula,contacto from contactos order by nombre");
        try {
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getString("id"),
                    rs.getString("nombre"),
                    rs.getString("cedula"),
                    rs.getString("contacto")
                });
            }
            rs.close();
            jtabla_clientes.setModel(modelo);
            metodos.BuscarEnTabla(txt_Filtro, jtabla_clientes);
        } catch (Exception e) {
            System.out.println("Error al cargar clientes: " + e);
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_agregar;
    private javax.swing.JScrollPane jScrollPane1;
    private org.jdesktop.swingx.JXTable jtabla_clientes;
    private javax.swing.JTextField txt_Filtro;
    // End of variables declaration//GEN-END:variables
}
