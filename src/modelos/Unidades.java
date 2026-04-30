/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

import conexiondb.DB_consultas_R_D;
import java.sql.ResultSet;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

/**
 *
 * @author Monkeyelgrande
 */
public class Unidades {

    String nombre;
    int id;

    public Unidades() {
    }

    public Unidades(int id, String nombre) {
        this.nombre = nombre;
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public static void mostrarUnidades(JComboBox<Unidades> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from unidades_medidas order by nombre");
            while (rs.next()) {
                modeloCombo.addElement(new Unidades(rs.getInt("id"), rs.getString("nombre")));
                jbox.setModel(modeloCombo);
            }
        } catch (Exception e) {
        }
        AutoCompleteDecorator.decorate(jbox);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
