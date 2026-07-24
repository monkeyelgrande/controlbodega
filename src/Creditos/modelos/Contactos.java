/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos.modelos;

import conexiondb.DB_consultas_R_D;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

/**
 *
 * @author Monkeyelgrande
 */
public class Contactos {

    String nombre, cedula, direccion, celular, telefono, email, ciudad, observaciones;
    int id, empleado, antiguo;
    double cupo, interes;
    
    public double getInteres() {
        return interes;
    }

    public void setInteres(double interes) {
        this.interes = interes;
    }

    public int getAntiguo() {
        return antiguo;
    }

    public void setAntiguo(int antiguo) {
        this.antiguo = antiguo;
    }

 

    public int getEmpleado() {
        return empleado;
    }

    public void setEmpleado(int empleado) {
        this.empleado = empleado;
    }

    public Contactos() {
    }

    public Contactos(int id, String nombre) {
        this.nombre = nombre;
        this.id = id;
    }

    public double getCupo() {
        return cupo;
    }

    public void setCupo(double cupo) {
        this.cupo = cupo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String contacto) {
        this.celular = contacto;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String contacto2) {
        this.telefono = contacto2;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public static void MostrarNombreContactos(JComboBox<Contactos> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from contactos order by nombre");
            while (rs.next()) {
                modeloCombo.addElement(new Contactos(rs.getInt("id"), rs.getString("nombre")));
                jbox.setModel(modeloCombo);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(jbox, "No se pudieron mostras los proveedores: " + e);
        }
        AutoCompleteDecorator.decorate(jbox);
    }

    public void MostrarNombreEmpleados(JComboBox<Contactos> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from contactos where empleado=1 order by nombre");
            while (rs.next()) {
                modeloCombo.addElement(new Contactos(rs.getInt("id"), rs.getString("nombre")));
                jbox.setModel(modeloCombo);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(jbox, "No se pudieron mostras los proveedores: " + e);
        }
        AutoCompleteDecorator.decorate(jbox);
    }



    public static void mostrarContactosCedula(JTextField txt_campo, boolean editable) {
        ArrayList<String> lista_cedulas = new ArrayList<String>();

        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select cedula from contactos");
            while (rs.next()) {
                lista_cedulas.add(rs.getString("cedula"));
            }
        } catch (Exception e) {
        }
        AutoCompleteDecorator.decorate(txt_campo, lista_cedulas, editable);
    }

    public static void mostrarContactosNombre(JTextField txt_campo, boolean editable) {
        ArrayList<String> lista_cedulas = new ArrayList<String>();

        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select nombre from contactos");
            while (rs.next()) {
                lista_cedulas.add(rs.getString("nombre"));
            }
        } catch (Exception e) {
        }
        AutoCompleteDecorator.decorate(txt_campo, lista_cedulas, editable);
    }

    public static void mostrarContactosCiudad(JTextField txt_cedula_contacto, boolean editable) {
        ArrayList<String> lista_cedulas = new ArrayList<String>();

        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select ciudad from contactos");
            while (rs.next()) {
                lista_cedulas.add(rs.getString("ciudad"));
            }
        } catch (Exception e) {
        }
        AutoCompleteDecorator.decorate(txt_cedula_contacto, lista_cedulas, editable);
    }

    public static double traerCupo(int id) {
        double cupo = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select cupo from contactos where id=" + id);
            while (rs.next()) {
                cupo = rs.getDouble("cupo");
            }
        } catch (Exception e) {
        }
        return cupo;
    }

    @Override
    public String toString() {
        return nombre;
    }

}
