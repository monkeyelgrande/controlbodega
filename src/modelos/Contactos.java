/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

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

    String nombre, cedula, direccion, contacto, contacto2, email, ciudad, formapago, cuenta, tipo_cuenta, numero_cuenta, observaciones, cliente;
    int id, descuento, proveedor;

    public Contactos() {
    }

    public int getProveedor() {
        return proveedor;
    }

    public void setProveedor(int proveedor) {
        this.proveedor = proveedor;
    }

    public Contactos(int id, String nombre) {
        this.nombre = nombre;
        this.id = id;
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

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public String getContacto2() {
        return contacto2;
    }

    public void setContacto2(String contacto2) {
        this.contacto2 = contacto2;
    }

    public int getDescuento() {
        return descuento;
    }

    public void setDescuento(int descuento) {
        this.descuento = descuento;
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

    public String getFormapago() {
        return formapago;
    }

    public void setFormapago(String formapago) {
        this.formapago = formapago;
    }

    public String getCuenta() {
        return cuenta;
    }

    public void setCuenta(String cuenta) {
        this.cuenta = cuenta;
    }

    public String getTipo_cuenta() {
        return tipo_cuenta;
    }

    public void setTipo_cuenta(String tipo_cuenta) {
        this.tipo_cuenta = tipo_cuenta;
    }

    public String getNumero_cuenta() {
        return numero_cuenta;
    }

    public void setNumero_cuenta(String numero_cuenta) {
        this.numero_cuenta = numero_cuenta;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public static String nombreDefectoVentaDiaria() {
        String nombre = "";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select nombre from contactos where id=1");
            while (rs.next()) {
                nombre = (rs.getString("nombre"));
            }
        } catch (Exception e) {
        }
        return nombre;
    }

    public void MostrarNombreContactos(JComboBox<Contactos> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre, cedula from contactos order by nombre");
            while (rs.next()) {
                modeloCombo.addElement(new Contactos(rs.getInt("id"), rs.getString("nombre") + " - " + rs.getString("cedula")));
                jbox.setModel(modeloCombo);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(jbox, "No se pudieron mostras los proveedores: " + e);
        }
//        AutoCompleteDecorator.decorate(jbox);
    }

    public void MostrarNombreProveedores(JComboBox<Contactos> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from contactos where proveedor=1 order by nombre");
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
//        AutoCompleteDecorator.decorate(txt_campo, lista_cedulas, editable);
    }

    public static void mostrarContactosCiudad(JTextField txt_cedula_contacto, boolean editable) {
        ArrayList<String> lista_ciudad = new ArrayList<String>();

        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select ciudad from contactos");
            while (rs.next()) {
                lista_ciudad.add(rs.getString("ciudad"));
            }
        } catch (Exception e) {
        }
//        AutoCompleteDecorator.decorate(txt_cedula_contacto, lista_ciudad, editable);
    }

    @Override
    public String toString() {
        return nombre;
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
//        AutoCompleteDecorator.decorate(txt_campo, lista_cedulas, editable);
    }

    public Contactos TraerContacto(String id) {
        Contactos contacto = new Contactos();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre, cedula, direccion, contacto from contactos where id=" + id);
            while (rs.next()) {
                contacto.setId(rs.getInt("id"));
                contacto.setNombre(rs.getString("nombre"));
                contacto.setCedula(rs.getString("cedula"));
                contacto.setDireccion(rs.getString("direccion"));
                contacto.setContacto(rs.getString("contacto"));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return contacto;
    }

}
