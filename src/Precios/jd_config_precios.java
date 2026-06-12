package Precios;

import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;

/**
 * Configuracion del modulo Precios: porcentaje de operacion, divisor S&T y
 * porcentaje de credito (columnas nuevas de configuraciones). NO toca la
 * configuracion general de controlbodega.
 *
 * @author Monkeyelgrande
 */
public class jd_config_precios extends JDialog {

    private JTextField txt_operacion, txt_syt, txt_credito;

    public jd_config_precios(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        setTitle("Configuración de precios");
        construir();
        cargar();
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

        txt_operacion = new JTextField(12);
        txt_operacion.setFont(new java.awt.Font("Tahoma", 0, 14));
        txt_syt = new JTextField(12);
        txt_syt.setFont(new java.awt.Font("Tahoma", 0, 14));
        txt_credito = new JTextField(12);
        txt_credito.setFont(new java.awt.Font("Tahoma", 0, 14));

        String[] etiquetas = {"% Operación (gasto):", "Divisor S y T:", "% Crédito:"};
        JTextField[] campos = {txt_operacion, txt_syt, txt_credito};

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
                "select coalesce(porcentaje_operacion,0) as porcentaje_operacion, "
                + "coalesce(porcentaje_s_y_t,0) as porcentaje_s_y_t, "
                + "coalesce(porcentaje_credito,0) as porcentaje_credito "
                + "from configuraciones where id = 1");
        try {
            if (rs.next()) {
                txt_operacion.setText("" + rs.getDouble("porcentaje_operacion"));
                txt_syt.setText("" + rs.getDouble("porcentaje_s_y_t"));
                txt_credito.setText("" + rs.getDouble("porcentaje_credito"));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void guardar() {
        double operacion, syt, credito;
        try {
            operacion = Double.parseDouble(txt_operacion.getText().trim());
            syt = Double.parseDouble(txt_syt.getText().trim());
            credito = Double.parseDouble(txt_credito.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Los tres valores deben ser numéricos");
            return;
        }

        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(
                        "update configuraciones set porcentaje_operacion=?, porcentaje_s_y_t=?, porcentaje_credito=? where id=1")) {
            ps.setDouble(1, operacion);
            ps.setDouble(2, syt);
            ps.setDouble(3, credito);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Configuración guardada");
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al guardar la configuración:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
