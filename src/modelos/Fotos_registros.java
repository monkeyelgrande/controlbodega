/*
 * Modulo Caja: fotos adjuntas a ingresos/egresos (tipo_registro: 1=ingreso, 2=egreso).
 * Portado desde cajadiaria.
 */
package modelos;

/**
 *
 * @author Monkeyelgrande
 */
public class Fotos_registros {

    String nombre;
    int id_registro, tipo_registro;
    int id;

    public Fotos_registros() {
    }

    public int getTipo_registro() {
        return tipo_registro;
    }

    public void setTipo_registro(int tipo_registro) {
        this.tipo_registro = tipo_registro;
    }

    public int getId_registro() {
        return id_registro;
    }

    public void setId_registro(int id_orden) {
        this.id_registro = id_orden;
    }

    public Fotos_registros(String nombre, int id_registro, int id, int tipo_registro) {
        this.nombre = nombre;
        this.id_registro = id_registro;
        this.id = id;
        this.tipo_registro = tipo_registro;
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

    @Override
    public String toString() {
        return nombre;
    }
}
