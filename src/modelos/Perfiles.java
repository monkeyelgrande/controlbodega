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
public class Perfiles {
    String perfil;
    int id;

    public Perfiles() {
    }

    public Perfiles(int id, String perfil) {
        this.perfil = perfil;
        this.id = id;
    }

       
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }
    
    public void mostrar_perfiles(JComboBox<Perfiles> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,perfil from perfiles order by perfil");
            while (rs.next()) {
                modeloCombo.addElement(new Perfiles(rs.getInt("id"), rs.getString("perfil")));
                jbox.setModel(modeloCombo);
            }
        } catch (Exception e) {
        }
//        AutoCompleteDecorator.decorate(jbox);
    }

    @Override
    public String toString() {
        return perfil;
    }
}
