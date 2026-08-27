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
 * Editor de los precios de UN producto existente (modulo Precios). Los campos
 * dependen del modo de la instalacion (ModoPrecios):
 *   AGRO : Venta, Desc. N1/N2, S&T, Credito, % Utilidad, IVA
 *   TECNI: Precio 1/2/3 con sus tres margenes, S&T, IVA (sin credito)
 * No crea productos; el guardado aplica tambien el puente precio_venta/2/3.
 *
 * @author Monkeyelgrande
 */
public class jd_editar_precios_producto extends JDialog {

    private final String codigoBarras;
    private JTextField txt_venta, txt_desc1, txt_desc2, txt_syt, txt_credito, txt_iva;
    private JTextField txt_utilidad, txt_utilidad2, txt_utilidad3;

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
        txt_utilidad2 = campo();
        txt_utilidad3 = campo();
        txt_iva = campo();

        String[] etiquetas;
        JTextField[] campos;
        if (ModoPrecios.esTecni()) {
            etiquetas = new String[]{"Precio 1:", "% P1:", "Precio 2:", "% P2:", "Precio 3:", "% P3:",
                "Valor S y T:", "IVA (%):"};
            campos = new JTextField[]{txt_venta, txt_utilidad, txt_desc1, txt_utilidad2,
                txt_desc2, txt_utilidad3, txt_syt, txt_iva};
        } else {
            etiquetas = new String[]{"Venta:", "Valor desc. N1:", "Valor desc. N2:", "Valor S y T:",
                "Valor crédito:", "% Utilidad:", "IVA (%):"};
            campos = new JTextField[]{txt_venta, txt_desc1, txt_desc2, txt_syt, txt_credito, txt_utilidad, txt_iva};
        }

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
                + "coalesce(porcentaje_utilidad2,0) as porcentaje_utilidad2, "
                + "coalesce(porcentaje_utilidad3,0) as porcentaje_utilidad3, "
                + "coalesce(iva,0) as iva from productos where codigo_barras = '" + codigoBarras + "'");
        try {
            if (rs.next()) {
                txt_venta.setText("" + rs.getDouble("venta"));
                txt_desc1.setText("" + rs.getDouble("valor_desc_1"));
                txt_desc2.setText("" + rs.getDouble("valor_desc_2"));
                txt_syt.setText("" + rs.getDouble("valor_s_y_t"));
                txt_credito.setText("" + rs.getDouble("valor_credito"));
                txt_utilidad.setText("" + rs.getDouble("porcentaje_utilidad"));
                txt_utilidad2.setText("" + rs.getDouble("porcentaje_utilidad2"));
                txt_utilidad3.setText("" + rs.getDouble("porcentaje_utilidad3"));
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
            boolean ok;
            if (ModoPrecios.esTecni()) {
                ok = DBpreciosProductos.actualizarPreciosTecni(codigoBarras,
                        leer(txt_venta, "Precio 1"), leer(txt_desc1, "Precio 2"), leer(txt_desc2, "Precio 3"),
                        leer(txt_syt, "Valor S y T"),
                        leer(txt_utilidad, "% P1"), leer(txt_utilidad2, "% P2"), leer(txt_utilidad3, "% P3"),
                        leer(txt_iva, "IVA"));
            } else {
                ok = DBpreciosProductos.actualizarPreciosAgro(codigoBarras,
                        leer(txt_venta, "Venta"), leer(txt_desc1, "Valor desc. N1"), leer(txt_desc2, "Valor desc. N2"),
                        leer(txt_syt, "Valor S y T"), leer(txt_credito, "Valor crédito"),
                        leer(txt_utilidad, "% Utilidad"), leer(txt_iva, "IVA"));
            }
            if (ok) {
                JOptionPane.showMessageDialog(this, "Precios actualizados");
                dispose();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido en el campo: " + e.getMessage());
        }
    }
}
