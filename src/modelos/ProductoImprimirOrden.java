package modelos;

public class ProductoImprimirOrden {

    private String codigo;
    private String descripcion;
    private String cantidad;
    private String unidad;
    /**
     * Nombre de la bodega a la que está asignado el producto.
     * null = producto sin bodega (novedad / no asignado).
     */
    private String bodega;
    /**
     * Precio unitario y total de la línea, ya formateados como dinero.
     * Solo se usan en recibos con precios (p.ej. la copia de venta); en el
     * recibo de orden quedan null y no se imprimen.
     */
    private String precioUnitario;
    private String precioTotal;

    public ProductoImprimirOrden(String codigo, String descripcion, String cantidad, String unidad) {
        this(codigo, descripcion, cantidad, unidad, null);
    }

    public ProductoImprimirOrden(String codigo, String descripcion, String cantidad, String unidad, String bodega) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.bodega = bodega;
    }

    public String getBodega() {
        return bodega;
    }

    public void setBodega(String bodega) {
        this.bodega = bodega;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCantidad() {
        return cantidad;
    }

    public void setCantidad(String cantidad) {
        this.cantidad = cantidad;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(String precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public String getPrecioTotal() {
        return precioTotal;
    }

    public void setPrecioTotal(String precioTotal) {
        this.precioTotal = precioTotal;
    }
}
