package Precios;

import Metodos.metodos;
import conexiondb.DB_Descuentos;
import conexiondb.DB_consultas_R_D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.ResultSet;
import javax.swing.*;
import modelos.Descuentos;

/**
 * Crear/editar un descuento escalonado (modulo Precios, portado del
 * jif_CrearDescuentos de productos-agroinsumos).
 *
 * @author Monkeyelgrande
 */
public class jd_crear_descuento extends JDialog {

    private final int idEditar; // 0 = nuevo
    private JComboBox<String> cmb_tipo;
    private JTextField txt_utilidad, txt_descuento;

    public jd_crear_descuento(java.awt.Frame parent, boolean modal, int idEditar) {
        super(parent, modal);
        this.idEditar = idEditar;
        setTitle(idEditar > 0 ? "Editar descuento #" + idEditar : "Nuevo descuento");
        construir();
        if (idEditar > 0) {
            cargar();
        }
        pack();
        setLocationRelativeTo(parent);
        metodos.addEscapeListenerWindowDialog(this);
    }

    private void construir() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(java.awt.Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        cmb_tipo = new JComboBox<>(new String[]{"1", "2"});
        cmb_tipo.setFont(new java.awt.Font("Tahoma", 0, 14));
        txt_utilidad = new JTextField(12);
        txt_utilidad.setFont(new java.awt.Font("Tahoma", 0, 14));
        txt_descuento = new JTextField(12);
        txt_descuento.setFont(new java.awt.Font("Tahoma", 0, 14));

        String[] etiquetas = {"Tipo (nivel):", "Utilidad hasta (%):", "Descuento (%):"};
        java.awt.Component[] campos = {cmb_tipo, txt_utilidad, txt_descuento};

        for (int i = 0; i < etiquetas.length; i++) {
            gc.gridx = 0;
            gc.gridy = i;
            JLabel l = new JLabel(etiquetas[i]);
            l.setFont(new java.awt.Font("Tahoma", 1, 14));
            root.add(l, gc);
            gc.gridx = 1;
            root.add(campos[i], gc);
        }

        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.setFont(new java.awt.Font("Tahoma", 1, 14));
        btnGuardar.setBackground(new java.awt.Color(46, 125, 50));
        btnGuardar.setForeground(java.awt.Color.WHITE);
        btnGuardar.addActionListener(e -> guardar());

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new java.awt.Font("Tahoma", 1, 14));
        btnCancelar.addActionListener(e -> dispose());

        gc.gridx = 0;
        gc.gridy = etiquetas.length;
        root.add(btnGuardar, gc);
        gc.gridx = 1;
        root.add(btnCancelar, gc);

        setContentPane(root);
    }

    private void cargar() {
        ResultSet rs = DB_consultas_R_D.getTabla("select * from descuentos where id = " + idEditar);
        try {
            if (rs.next()) {
                cmb_tipo.setSelectedItem(rs.getString("tipo"));
                txt_utilidad.setText("" + rs.getDouble("utilidad"));
                txt_descuento.setText("" + rs.getDouble("descuento"));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void guardar() {
        double utilidad, descuento;
        try {
            utilidad = Double.parseDouble(txt_utilidad.getText().trim());
            descuento = Double.parseDouble(txt_descuento.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Utilidad y descuento deben ser numéricos");
            return;
        }

        Descuentos obj = new Descuentos();
        obj.setTipo(Integer.parseInt(cmb_tipo.getSelectedItem().toString()));
        obj.setUtilidad(utilidad);
        obj.setDescuento(descuento);

        DB_Descuentos db = new DB_Descuentos();
        int resultado;
        if (idEditar > 0) {
            obj.setId(idEditar);
            resultado = db.Actualizar(obj);
        } else {
            obj.setId(Integer.parseInt(DB_consultas_R_D.cargarId("descuentos")));
            resultado = db.Guardar(obj);
        }
        if (resultado > 0) {
            dispose();
        }
    }
}
