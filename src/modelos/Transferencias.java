/*
 * Modulo Caja (portado de cajadiaria): traslado de dinero entre fondos.
 * Cada traslado genera un par egreso (fondo origen) + ingreso (fondo destino)
 * marcados con transferencia=1; ver conexiondb.DB_transferencias.GuardarTraslado.
 */
package modelos;

import conexiondb.DB_consultas_R_D;
import java.sql.ResultSet;

/**
 *
 * @author Monkeyelgrande
 */
public class Transferencias {

    String fecha, hora, descripcion, nombre_user, nombre_cuenta;
    int id, id_user, id_fondo_origen, id_fondo_destino, id_ingreso, id_egreso;
    // 1=Factura, 0=Remision: se replica en el par ingreso/egreso generado
    int factura_remision;
    double total;

    public int getFactura_remision() {
        return factura_remision;
    }

    public void setFactura_remision(int factura_remision) {
        this.factura_remision = factura_remision;
    }

    public int getId_ingreso() {
        return id_ingreso;
    }

    public void setId_ingreso(int id_ingreso) {
        this.id_ingreso = id_ingreso;
    }

    public int getId_egreso() {
        return id_egreso;
    }

    public void setId_egreso(int id_egreso) {
        this.id_egreso = id_egreso;
    }

    public int getId_fondo_origen() {
        return id_fondo_origen;
    }

    public void setId_fondo_origen(int id_fondo_origen) {
        this.id_fondo_origen = id_fondo_origen;
    }

    public int getId_fondo_destino() {
        return id_fondo_destino;
    }

    public void setId_fondo_destino(int id_fondo_destino) {
        this.id_fondo_destino = id_fondo_destino;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getNombre_user() {
        return nombre_user;
    }

    public void setNombre_user(String nombre_user) {
        this.nombre_user = nombre_user;
    }

    public String getNombre_cuenta() {
        return nombre_cuenta;
    }

    public void setNombre_cuenta(String nombre_cuenta) {
        this.nombre_cuenta = nombre_cuenta;
    }

    public static Transferencias traer_ingreso(String id) {
        Transferencias g = new Transferencias();

        // Esquema nuevo: la columna de contacto en ingresos es id_cliente y
        // puede venir nula, por eso los left join.
        ResultSet rs = DB_consultas_R_D.getTabla("select i.id, u.nombre as usuario, c.nombre as cliente, i.descripcion, i.total, i.fecha, i.hora "
                + "from ingresos i "
                + "left join users u on i.id_user=u.id "
                + "left join contactos c on i.id_cliente=c.id "
                + "where i.id=" + id);

        try {
            while (rs.next()) {
                g.setId(rs.getInt("id"));
                g.setNombre_user(rs.getString("usuario"));
                g.setNombre_cuenta(rs.getString("cliente"));
                g.setDescripcion(rs.getString("descripcion"));
                g.setTotal(rs.getDouble("total"));
                g.setFecha(rs.getString("fecha"));
                g.setHora(rs.getString("hora"));

            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
        }

        return g;
    }

}
