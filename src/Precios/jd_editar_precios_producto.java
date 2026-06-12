package Precios;

import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBpreciosProductos;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.ResultSet;
import javax.swing.*;

/**
 * Editor de los precios estilo agro de UN producto existente (modulo
 * Precios). No crea productos ni toca precio_venta/2/3 de bodega.
 *
 * @author Monkeyelgrande
 */
public class jd_editar_precios_producto extends JDialog {

    private final String codigoBarras;
    private JTextField txt_venta, txt_desc1, txt_desc2, txt_syt, txt_credito, txt_utilidad, txt_iva;

    public jd_editar_precios_producto(java.awt.Frame parent, boolean modal, String codigoBarras) {
        super(parent, modal);
        this.codigoBarras = codigoBarras;
        setTitle("Precios del producto " + codigoBarras);
        construir();
        cargar();
        pack();
        setLocationRelativeTo(parent);
        metodos.addEscapeListenerWindowDialog(this);
    }

    private JTextField campo() {
        JTextField t = new JTextField(14);
        t.setFont(new java.awt.Font("Tahoma", 0, 14));
        return t;
    }

    private void construir() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(java.awt.Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        txt_venta = campo();
        txt_desc1 = campo();
        txt_desc2 = campo();
        txt_syt = campo();
        txt_credito = campo();
        txt_utilidad = campo();
        txt_iva = campo();

        String[] etiquetas = {"Venta:", "Valor desc. N1:", "Valor desc. N2:", "Valor S y T:",
            "Valor crédito:", "% Utilidad:", "IVA (%):"};
        JTextField[] campos = {txt_venta, txt_desc1, txt_desc2, txt_syt, txt_credito, txt_utilidad, txt_iva};

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
        ResultSet rs = DB_consultas_R_D.getTabla(
                "select coalesce(venta,0) as venta, coalesce(valor_desc_1,0) as valor_desc_1, "
                + "coalesce(valor_desc_2,0) as valor_desc_2, coalesce(valor_s_y_t,0) as valor_s_y_t, "
                + "coalesce(valor_credito,0) as valor_credito, coalesce(porcentaje_utilidad,0) as porcentaje_utilidad, "
                + "coalesce(iva,0) as iva from productos where codigo_barras = '" + codigoBarras + "'");
        try {
            if (rs.next()) {
                txt_venta.setText("" + rs.getDouble("venta"));
                txt_desc1.setText("" + rs.getDouble("valor_desc_1"));
                txt_desc2.setText("" + rs.getDouble("valor_desc_2"));
                txt_syt.setText("" + rs.getDouble("valor_s_y_t"));
                txt_credito.setText("" + rs.getDouble("valor_credito"));
                txt_utilidad.setText("" + rs.getDouble("porcentaje_utilidad"));
                txt_iva.setText("" + rs.getDouble("iva"));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private double leer(JTextField campo, String nombre) {
        try {
            return Double.parseDouble(metodos.EliminaCaracteres(campo.getText().trim(), ","));
        } catch (Exception e) {
            throw new NumberFormatException(nombre);
        }
    }

    private void guardar() {
        try {
            double venta = leer(txt_venta, "Venta");
            double d1 = leer(txt_desc1, "Valor desc. N1");
            double d2 = leer(txt_desc2, "Valor desc. N2");
            double syt = leer(txt_syt, "Valor S y T");
            double credito = leer(txt_credito, "Valor crédito");
            double util = leer(txt_utilidad, "% Utilidad");
            double iva = leer(txt_iva, "IVA");

            if (DBpreciosProductos.actualizarPrecios(codigoBarras, venta, d1, d2, syt, credito, util, iva)) {
                JOptionPane.showMessageDialog(this, "Precios actualizados");
                dispose();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido en el campo: " + e.getMessage());
        }
    }
}
