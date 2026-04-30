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
public class Tipo_ingresos_mercancia {

    String nombre;
    int id;

    public Tipo_ingresos_mercancia() {
    }

    public Tipo_ingresos_mercancia(int id, String nombre) {
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

    public void mostrarTipos_ingreso(JComboBox<Tipo_ingresos_mercancia> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from tipo_ingreso order by nombre");
            while (rs.next()) {
                modeloCombo.addElement(new Tipo_ingresos_mercancia(rs.getInt("id"), rs.getString("nombre")));
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
