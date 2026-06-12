package modelos;

/**
 * Opcion gobernable del sistema de permisos (tabla opciones): un menu, boton o
 * accion logica que puede concederse por perfil (perfil_opciones) o por
 * usuario (usuario_opciones).
 *
 * @author Monkeyelgrande
 */
public class Opcion {

    private int id;
    private String clave;
    private String nombre;
    private String modulo;
    private String componente;
    private int orden;

    public Opcion() {
    }

    public Opcion(int id, String clave, String nombre, String modulo, String componente, int orden) {
        this.id = id;
        this.clave = clave;
        this.nombre = nombre;
        this.modulo = modulo;
        this.componente = componente;
        this.orden = orden;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public String getComponente() {
        return componente;
    }

    public void setComponente(String componente) {
        this.componente = componente;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
