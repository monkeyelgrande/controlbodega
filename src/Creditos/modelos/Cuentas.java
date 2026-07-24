/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos.modelos;

import conexiondb.DB_consultas_R_D;
import java.sql.ResultSet;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

/**
 *
 * @author Monkeyelgrande
 */
public class Cuentas {

    String nombre;
    int id;

    public Cuentas() {
    }

    public Cuentas(int id, String nombre) {
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

    public static void mostrarCuentas(JComboBox<Cuentas> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from cuentas order by nombre");
            while (rs.next()) {
                modeloCombo.addElement(new Cuentas(rs.getInt("id"), rs.getString("nombre")));
                jbox.setModel(modeloCombo);
            }
        } catch (Exception e) {
        }
        AutoCompleteDecorator.decorate(jbox);
    }

    public static void mostrarCuentasConTodas(JComboBox<Cuentas> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        modeloCombo.addElement(new Cuentas(0, "TOTAS LAS CUENTAS"));

        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from cuentas order by nombre");
            while (rs.next()) {
                modeloCombo.addElement(new Cuentas(rs.getInt("id"), rs.getString("nombre")));
                jbox.setModel(modeloCombo);
            }
        } catch (Exception e) {
        }
        AutoCompleteDecorator.decorate(jbox);
    }

    public static int Traer_cuentas_modelo_lista(JList jlist) {
        DefaultListModel modelo = new DefaultListModel();
//        modeloCombo.addElement(new Cuentas(0, ""));
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from cuentas order by nombre");
            while (rs.next()) {
                modelo.addElement(new Cuentas(rs.getInt("id"), rs.getString("nombre")));
                jlist.setModel(modelo);
            }
        } catch (Exception e) {
        }
        return modelo.size();
    }

    @Override
    public String toString() {
        return nombre;
    }
}
