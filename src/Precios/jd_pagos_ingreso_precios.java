package Precios;

import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBpagosIngresosProductos;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Pagos al proveedor de un ingreso del modulo Precios (tabla
 * pagos_ingresos_productos). Replica el patron de pagos de
 * jif_crear_ingreso_mercancia pero como dialogo independiente.
 *
 * @author Monkeyelgrande
 */
public class jd_pagos_ingreso_precios extends JDialog {

    private final int idIngreso;
    private JTable jtabla;
    private JLabel lbl_total_pagado;
    private JTextField txt_valor;
    private JTextField txt_cod_pago;

    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public jd_pagos_ingreso_precios(java.awt.Frame parent, boolean modal, int idIngreso) {
        super(parent, modal);
        this.idIngreso = idIngreso;
        setTitle("Pagos del ingreso #" + idIngreso);
        construir();
        cargar();
        setSize(700, 450);
        setLocationRelativeTo(parent);
        metodos.addEscapeListenerWindowDialog(this);
    }

    private void construir() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.setBackground(java.awt.Color.WHITE);

        // Captura de pago
        JPanel captura = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        captura.setOpaque(false);

        JLabel lblValor = new JLabel("Valor:");
        lblValor.setFont(new java.awt.Font("Tahoma", 1, 14));
        txt_valor = new JTextField(10);
        txt_valor.setFont(new java.awt.Font("Tahoma", 0, 14));

        JLabel lblCod = new JLabel("Código/referencia:");
        lblCod.setFont(new java.awt.Font("Tahoma", 1, 14));
        txt_cod_pago = new JTextField(12);
        txt_cod_pago.setFont(new java.awt.Font("Tahoma", 0, 14));

        JButton btnAgregar = new JButton("Registrar pago");
        btnAgregar.setFont(new java.awt.Font("Tahoma", 1, 14));
        btnAgregar.setBackground(new java.awt.Color(46, 125, 50));
        btnAgregar.setForeground(java.awt.Color.WHITE);
        btnAgregar.addActionListener(e -> registrarPago());

        JButton btnEliminar = new JButton("Eliminar pago");
        btnEliminar.setFont(new java.awt.Font("Tahoma", 1, 14));
        btnEliminar.addActionListener(e -> eliminarPago());

        captura.add(lblValor);
        captura.add(txt_valor);
        captura.add(lblCod);
        captura.add(txt_cod_pago);
        captura.add(btnAgregar);
        captura.add(btnEliminar);

        // Tabla
        jtabla = new JTable(modelo);
        jtabla.setRowHeight(28);
        jtabla.setFont(new java.awt.Font("Tahoma", 0, 14));
        jtabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Pie con total
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        pie.setOpaque(false);
        JLabel lblTotal = new JLabel("Total pagado:");
        lblTotal.setFont(new java.awt.Font("Tahoma", 1, 16));
        lbl_total_pagado = new JLabel("0");
        lbl_total_pagado.setFont(new java.awt.Font("Tahoma", 1, 16));
        lbl_total_pagado.setForeground(new java.awt.Color(0, 153, 0));
        pie.add(lblTotal);
        pie.add(lbl_total_pagado);

        root.add(captura, BorderLayout.NORTH);
        root.add(new JScrollPane(jtabla), BorderLayout.CENTER);
        root.add(pie, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void cargar() {
        modelo.setRowCount(0);
        modelo.setColumnIdentifiers(new Object[]{"id", "Valor", "Fecha", "Hora", "Código"});
        double total = 0;
        ResultSet rs = DB_consultas_R_D.getTabla(
                "select id, total, fecha, hora, cod_pago from pagos_ingresos_productos "
                + "where id_ingreso_productos_cabecera = " + idIngreso + " order by id");
        try {
            while (rs.next()) {
                modelo.addRow(new Object[]{rs.getString("id"),
                    metodos.formateador_dinero().format(rs.getDouble("total")),
                    rs.getDate("fecha"), rs.getString("hora"), rs.getString("cod_pago")});
                total += rs.getDouble("total");
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        lbl_total_pagado.setText(metodos.formateador_dinero().format(total));
    }

    private void registrarPago() {
        double valor;
        try {
            valor = Double.parseDouble(metodos.EliminaCaracteres(txt_valor.getText().trim(), ","));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ingrese un valor de pago válido");
            txt_valor.requestFocus();
            return;
        }
        if (valor <= 0) {
            JOptionPane.showMessageDialog(this, "El valor del pago debe ser mayor a 0");
            return;
        }
        Calendar c = new GregorianCalendar();
        String fecha = new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
        String hora = new SimpleDateFormat("HH:mm:ss").format(c.getTime());

        if (DBpagosIngresosProductos.guardar(idIngreso, valor, fecha, hora, txt_cod_pago.getText().trim())) {
            txt_valor.setText("");
            txt_cod_pago.setText("");
            cargar();
        }
    }

    private void eliminarPago() {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un pago");
            return;
        }
        if (!DB_consultas_R_D.validar_admin()) {
            return;
        }
        int idPago = Integer.parseInt(modelo.getValueAt(fila, 0).toString());
        int r = JOptionPane.showConfirmDialog(this, "¿Eliminar el pago #" + idPago + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION && DBpagosIngresosProductos.eliminar(idPago)) {
            cargar();
        }
    }
}
