/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

import conexiondb.DB_consultas_R_D;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 *
 * @author Monkeyelgrande
 */
public class Users {

    int id, id_perfil, id_bodega;

    public int getId_bodega() {
        return id_bodega;
    }

    public void setId_bodega(int id_bodega) {
        this.id_bodega = id_bodega;
    }
    String nombre, user_name, password, direccion, telefono, telefono2, estado, email;
    boolean imprime_ordenes;
    String nombre_impresora;
    boolean imp_ticket_bodega_asignada;
    boolean barra_notificaciones;

    public boolean isBarra_notificaciones() {
        return barra_notificaciones;
    }

    public void setBarra_notificaciones(boolean barra_notificaciones) {
        this.barra_notificaciones = barra_notificaciones;
    }

    public boolean isImprime_ordenes() {
        return imprime_ordenes;
    }

    public void setImprime_ordenes(boolean imprime_ordenes) {
        this.imprime_ordenes = imprime_ordenes;
    }

    public String getNombre_impresora() {
        return nombre_impresora;
    }

    public void setNombre_impresora(String nombre_impresora) {
        this.nombre_impresora = nombre_impresora;
    }

    public boolean isImp_ticket_bodega_asignada() {
        return imp_ticket_bodega_asignada;
    }

    public void setImp_ticket_bodega_asignada(boolean imp_ticket_bodega_asignada) {
        this.imp_ticket_bodega_asignada = imp_ticket_bodega_asignada;
    }

    public Users() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_perfil() {
        return id_perfil;
    }

    public void setId_perfil(int id_perfil) {
        this.id_perfil = id_perfil;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getTelefono2() {
        return telefono2;
    }

    public void setTelefono2(String telefono2) {
        this.telefono2 = telefono2;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static ArrayList MostrarUserName() {
        ArrayList<String> lista = new ArrayList<String>();

        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select user_name from users");
            while (rs.next()) {
                lista.add(rs.getString("user_name"));
            }
        } catch (Exception e) {
        }
        return lista;
    }

}
