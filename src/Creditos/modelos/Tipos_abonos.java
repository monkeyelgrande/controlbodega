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
public class Tipos_abonos {

    String nombre, color;
    int id, anticipo;
    /** 1 = un pago de este tipo entra a Caja como ingreso. */
    int agregar_a_ingreso = 1;
    /** 1 = un pago de este tipo genera comision para el vendedor. */
    int comisionable;

    public int getAgregar_a_ingreso() {
        return agregar_a_ingreso;
    }

    public void setAgregar_a_ingreso(int agregar_a_ingreso) {
        this.agregar_a_ingreso = agregar_a_ingreso;
    }

    public int getComisionable() {
        return comisionable;
    }

    public void setComisionable(int comisionable) {
        this.comisionable = comisionable;
    }

    public int getAnticipo() {
        return anticipo;
    }

    public void setAnticipo(int anticipo) {
        this.anticipo = anticipo;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Tipos_abonos() {
    }

    public Tipos_abonos(int id, String nombre) {
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

    public static void mostrarAbonos(JComboBox<Tipos_abonos> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from tipos_abonos order by nombre");
            while (rs.next()) {
                modeloCombo.addElement(new Tipos_abonos(rs.getInt("id"), rs.getString("nombre")));
            }
            rs.close();
        } catch (Exception e) {
        }
        jbox.setModel(modeloCombo);
        AutoCompleteDecorator.decorate(jbox);
    }

    @Override
    public String toString() {
        return nombre;
    }

}
