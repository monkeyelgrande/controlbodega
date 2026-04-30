
package modelos;



public class Traslados_productos {

    // Campos según la tabla traslados_productos
    private int id;                    // serial (PK)
    private int id_producto;           // integer (FK -> productos.id)
    private double cantidad;           // double precision
    private int id_bodega_origen;   // double precision (según tu DDL)
    private int id_bodega_destino;  // double precision (según tu DDL)
    private String fecha;           // date
    private String hora;               // varchar(8)
    private String observacion;        // varchar
    private int id_user;               // integer (FK -> users.id)

    public Traslados_productos() {
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

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public int getId_bodega_origen() {
        return id_bodega_origen;
    }

    public void setId_bodega_origen(int id_bodega_origen) {
        this.id_bodega_origen = id_bodega_origen;
    }

    public int getId_bodega_destino() {
        return id_bodega_destino;
    }

    public void setId_bodega_destino(int id_bodega_destino) {
        this.id_bodega_destino = id_bodega_destino;
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

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

}
