package Creditos.modelos;

/**
 * Un escalón de la escala de comisiones: "cobrado hasta X días -> Y %".
 *
 * La escala se lee ordenada por días y se toma el PRIMER escalón cuyo tope de
 * días sea mayor o igual a los días que tardó el cobro. Un anticipo siempre
 * toma el primer escalón (el mejor porcentaje).
 *
 * @author Monkeyelgrande
 */
public class Porcentajes_comision {

    int id, dias;
    double porcentaje;

    public Porcentajes_comision() {
    }

    public Porcentajes_comision(int id, int dias, double porcentaje) {
        this.id = id;
        this.dias = dias;
        this.porcentaje = porcentaje;
    }

    public int getDias() {
        return dias;
    }

    public void setDias(int dias) {
        this.dias = dias;
    }

    public double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "hasta " + dias + " dias - " + porcentaje + "%";
    }
}
