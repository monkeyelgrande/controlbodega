/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

import conexiondb.DB_consultas_R_D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

/**
 *
 * @author Monkeyelgrande
 */
public class verificar_inventario {

    int id, id_producto;
    String codigo_barras, descripcion, observaciones, observaciones2, observaciones3;
    double cantidad_actual, cantidad_real;

    public verificar_inventario() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_producto() {
        return id_producto;
    }

    public void setId_producto(int id_producto) {
        this.id_producto = id_producto;
    }

    public String getCodigo_barras() {
        return codigo_barras;
    }

    public void setCodigo_barras(String codigo_barras) {
        this.codigo_barras = codigo_barras;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getObservaciones2() {
        return observaciones2;
    }

    public void setObservaciones2(String observaciones2) {
        this.observaciones2 = observaciones2;
    }

    public String getObservaciones3() {
        return observaciones3;
    }

    public void setObservaciones3(String observaciones3) {
        this.observaciones3 = observaciones3;
    }

    public double getCantidad_actual() {
        return cantidad_actual;
    }

    public void setCantidad_actual(double cantidad_actual) {
        this.cantidad_actual = cantidad_actual;
    }

    public double getCantidad_real() {
        return cantidad_real;
    }

    public void setCantidad_real(double cantidad_real) {
        this.cantidad_real = cantidad_real;
    }

   
    @Override
    public String toString() {
        return descripcion;
    }

}
