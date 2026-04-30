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
public class Productos {

    int id, id_padre, cant_paquete, id_unidad, tipo;
    String codigo_barras, descripcion;
    double stock_minimo, stock_ideal, precio_costo, precio_venta, precio_venta2, precio_venta3;
    boolean estado = true; // true = habilitado, false = deshabilitado

    public Productos() {
    }

    public Productos(int id, String descripcion, String codigo_barras) {
        this.id = id;
        this.descripcion = descripcion;
        this.codigo_barras = codigo_barras;
    }

    public double getPrecio_venta2() {
        return precio_venta2;
    }

    public void setPrecio_venta2(double precio_venta2) {
        this.precio_venta2 = precio_venta2;
    }

    public double getPrecio_venta3() {
        return precio_venta3;
    }

    public void setPrecio_venta3(double precio_venta3) {
        this.precio_venta3 = precio_venta3;
    }

    public int getTipo() {
        return tipo;
    }

    public void setTipo(int tipo) {
        this.tipo = tipo;
    }

    public double getPrecio_costo() {
        return precio_costo;
    }

    public void setPrecio_costo(double precio_costo) {
        this.precio_costo = precio_costo;
    }

    public double getPrecio_venta() {
        return precio_venta;
    }

    public void setPrecio_venta(double precio_venta) {
        this.precio_venta = precio_venta;
    }

    public int getId_padre() {
        return id_padre;
    }

    public void setId_padre(int id_padre) {
        this.id_padre = id_padre;
    }

    public int getCant_paquete() {
        return cant_paquete;
    }

    public void setCant_paquete(int cant_paquete) {
        this.cant_paquete = cant_paquete;
    }

    public int getId_unidad() {
        return id_unidad;
    }

    public void setId_unidad(int id_unidad) {
        this.id_unidad = id_unidad;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public double getStock_minimo() {
        return stock_minimo;
    }

    public void setStock_minimo(double stock_minimo) {
        this.stock_minimo = stock_minimo;
    }

    public double getStock_ideal() {
        return stock_ideal;
    }

    public void setStock_ideal(double stock_ideal) {
        this.stock_ideal = stock_ideal;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    /*
    * cuando el valor SumaResta esta en true se realiza la suma, y cuando es false se realizar una resta en el stock
     */
    public static int ActualizaStock(String id, int cantidad, boolean SumaResta) {
        int resultado = 0;
        Connection con = null;
        String SQL = "";
        if (SumaResta) {
            SQL = "UPDATE productos set "
                    + "stock=stock+" + cantidad
                    + "where id=" + id;
        } else {
            SQL = "UPDATE productos set "
                    + "stock=stock-" + cantidad
                    + "where id=" + id;
        }

        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SQL);
            resultado = psql.executeUpdate();
            psql.close();

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(null, "Error al intentar actualizar la información:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error al intentar cerrar la conexión:\n"
                        + ex, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            }
        }
        return resultado;
    }

    public void mostrarProductos_descripcion(JComboBox<Productos> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        modeloCombo.addElement(new Productos(0, "", ""));
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,descripcion,codigo_barras from productos WHERE COALESCE(estado, true) = true");
            while (rs.next()) {
                modeloCombo.addElement(new Productos(rs.getInt("id"), rs.getString("descripcion"), rs.getString("codigo_barras")));
                jbox.setModel(modeloCombo);
            }
        } catch (Exception e) {
        }
        AutoCompleteDecorator.decorate(jbox);
    }

    @Override
    public String toString() {
        return descripcion;
    }

}
