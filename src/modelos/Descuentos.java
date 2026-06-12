package modelos;

/**
 * Descuento escalonado por utilidad (modulo Precios, portado de
 * productos-agroinsumos).
 *
 * @author Monkeyelgrande
 */
public class Descuentos {

    int id, tipo;
    double utilidad, descuento;

    public Descuentos() {
    }

    public int getTipo() {
        return tipo;
    }

    public void setTipo(int tipo) {
        this.tipo = tipo;
    }

    public double getUtilidad() {
        return utilidad;
    }

    public void setUtilidad(double utilidad) {
        this.utilidad = utilidad;
    }

    public double getDescuento() {
        return descuento;
    }

    public void setDescuento(double descuento) {
        this.descuento = descuento;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return utilidad + "";
    }
}
