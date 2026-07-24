package Creditos.modelos;

/**
 * Cabecera de un pago (abono) de un cliente. Guarda el TOTAL pagado; el
 * reparto a cada factura vive en el detalle (modelos.Abonos). El saldo a
 * favor del pago es implícito: total - suma del detalle aplicado.
 *
 * @author Monkeyelgrande
 */
public class AbonosCabecera {

    int id, id_contacto, id_user, id_tipo_abono;
    double total;
    String fecha, hora, observacion, foto, PDF;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_contacto() {
        return id_contacto;
    }

    public void setId_contacto(int id_contacto) {
        this.id_contacto = id_contacto;
    }

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    public int getId_tipo_abono() {
        return id_tipo_abono;
    }

    public void setId_tipo_abono(int id_tipo_abono) {
        this.id_tipo_abono = id_tipo_abono;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getPDF() {
        return PDF;
    }

    public void setPDF(String PDF) {
        this.PDF = PDF;
    }
}
