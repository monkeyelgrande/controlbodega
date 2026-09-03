/*
 * Modulo Caja: modelo de ingresos de dinero.
 * Rediseno: sin abonos, el fondo va directo en ingresos.id_fondo.
 */
package modelos;

import conexiondb.DB_consultas_R_D;
import static conexiondb.DB_consultas_R_D.getConexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 *
 * @author Monkeyelgrande
 */
public class Ingresos {

    String fecha, hora, descripcion, nombre_user, nombre_cuenta, nombre_fondo;
    int id, id_user, id_cuenta, id_cliente, id_fondo, factura_remision;
    /** Caja a la que pertenece el ingreso: 1 = Caja, 2 = Caja Dos. */
    int id_caja = 1;
    /** 1 = el ingreso es un recibo de caja, 0 = no lo es (informativo). */
    int recibo_caja;
    /**
     * Vendedor al que se le acredita el dinero. Solo lo usan los ingresos que
     * vienen de un abono a credito; 0 = sin vendedor.
     */
    int id_vendedor;
    double total;

    public int getId_vendedor() {
        return id_vendedor;
    }

    public void setId_vendedor(int id_vendedor) {
        this.id_vendedor = id_vendedor;
    }

    public int getId_caja() {
        return id_caja;
    }

    public void setId_caja(int id_caja) {
        this.id_caja = id_caja;
    }

    public int getFactura_remision() {
        return factura_remision;
    }

    public void setFactura_remision(int factura_remision) {
        this.factura_remision = factura_remision;
    }

    public int getRecibo_caja() {
        return recibo_caja;
    }

    public void setRecibo_caja(int recibo_caja) {
        this.recibo_caja = recibo_caja;
    }

    public int getId_cliente() {
        return id_cliente;
    }

    public void setId_cliente(int id_cliente) {
        this.id_cliente = id_cliente;
    }

    public int getId_fondo() {
        return id_fondo;
    }

    public void setId_fondo(int id_fondo) {
        this.id_fondo = id_fondo;
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

    public int getId_cuenta() {
        return id_cuenta;
    }

    public void setId_cuenta(int id_cuenta) {
        this.id_cuenta = id_cuenta;
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

    public String getNombre_fondo() {
        return nombre_fondo;
    }

    public void setNombre_fondo(String nombre_fondo) {
        this.nombre_fondo = nombre_fondo;
    }

    public static Ingresos traer_ingreso(String id) {
        Ingresos g = new Ingresos();

        ResultSet rs = DB_consultas_R_D.getTabla("select i.id, u.nombre as user, cu.nombre as cuenta, i.descripcion, i.total, i.fecha, i.hora, "
                + "coalesce(f.nombre,'Pendiente') as fondo "
                + "from ingresos i left join fondos f on i.id_fondo=f.id, users u, cuentas_ingresos cu "
                + "where i.id_user=u.id and i.id_cuenta=cu.id and i.id=" + id);

        try {
            while (rs.next()) {
                g.setId(rs.getInt("id"));
                g.setNombre_user(rs.getString("user"));
                g.setNombre_cuenta(rs.getString("cuenta"));
                g.setDescripcion(rs.getString("descripcion"));
                g.setTotal(rs.getDouble("total"));
                g.setFecha(rs.getString("fecha"));
                g.setHora(rs.getString("hora"));
                g.setNombre_fondo(rs.getString("fondo"));

            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
        }

        return g;
    }

    /**
     * Marca el ingreso como pagado asignando el fondo directamente sobre la fila
     * (rediseno: ya no existen abonos_ingresos).
     */
    public static boolean actualizar_estado(String id, String id_fondo) {

        Connection con = null;
        String SSQL = "update ingresos set id_fondo=? where id=?";
        try {
            con = getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, Integer.parseInt(id_fondo.trim()));
            psql.setInt(2, Integer.parseInt(id.trim()));
            psql.executeUpdate();
            psql.close();
            con.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "No se puede actualizar el pago: \n" + e);
            return false;
        }
        return true;
    }

}
