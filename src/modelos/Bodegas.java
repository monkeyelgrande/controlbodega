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
public class Bodegas {

    String nombre;
    int id, imprime;

    public Bodegas() {
    }

    public int getImprime() {
        return imprime;
    }

    public void setImprime(int imprime) {
        this.imprime = imprime;
    }

    public Bodegas(int id, String nombre) {
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

    public void mostrarBodegas(JComboBox<Bodegas> jbox_bodegas) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();

        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from bodegas order by nombre");
            while (rs.next()) {
                modeloCombo.addElement(new Bodegas(rs.getInt("id"), rs.getString("nombre")));
                jbox_bodegas.setModel(modeloCombo);
            }
        } catch (Exception e) {
        }
        AutoCompleteDecorator.decorate(jbox_bodegas);
    }
    public void mostrarBodegasConTodas(JComboBox<Bodegas> jbox_bodegas) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        modeloCombo.addElement(new Bodegas(0, "Todas las bodegas"));

        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from bodegas order by nombre");
            while (rs.next()) {
                modeloCombo.addElement(new Bodegas(rs.getInt("id"), rs.getString("nombre")));
                jbox_bodegas.setModel(modeloCombo);
            }
        } catch (Exception e) {
        }
        AutoCompleteDecorator.decorate(jbox_bodegas);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
