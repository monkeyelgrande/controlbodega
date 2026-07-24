/*
 * Modelo Egresos del modulo Caja (portado desde cajadiaria).
 * Rediseño: sin abonos_egresos, el fondo va directo en egresos.id_fondo.
 */
package modelos;

import Caja.jd_ver_in_egre;
import conexiondb.DB_consultas_R_D;
import static conexiondb.DB_consultas_R_D.getConexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JTable;

/**
 *
 * @author Monkeyelgrande
 */
public class Egresos {

    String fecha, hora, descripcion, nombre_user, nombre_cuenta, nombre_fondo;
    int id, id_user, id_cuenta, id_cliente, id_fondo, factura_remision;
    double total;
    DecimalFormat formatea = new DecimalFormat("###,###.##");

    public int getId_cliente() {
        return id_cliente;
    }

    public int getFactura_remision() {
        return factura_remision;
    }

    public void setFactura_remision(int factura_remision) {
        this.factura_remision = factura_remision;
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

    public static Egresos traer_egreso(String id) {
        Egresos g = new Egresos();

        ResultSet rs = DB_consultas_R_D.getTabla("select g.id, u.nombre as user, cu.nombre as cuenta, g.descripcion, g.total, g.fecha, g.hora, "
                + "coalesce(f.nombre,'Pendiente') as fondo "
                + "from egresos g left join fondos f on g.id_fondo=f.id, users u, cuentas_egresos cu "
                + "where g.id_user=u.id and g.id_cuenta=cu.id and g.id=" + id);

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

    public void VerOtrosEgresos(JTable tabla) {
        jd_ver_in_egre frm = new jd_ver_in_egre();

        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            String id = "" + tabla.getValueAt(fila, 0);
            ResultSet rs;
            rs = DB_consultas_R_D.getTabla("select u.nombre as user, e.descripcion, e.fecha, e.hora, e.total, cu.nombre as cuenta "
                    + "from cuentas_egresos cu, users u, egresos e where e.id_cuenta=cu.id and e.id_user=u.id and e.id=" + id);
            try {
                while (rs.next()) {
                    jd_ver_in_egre.lbl_user.setText(rs.getString("user"));
                    jd_ver_in_egre.lbl_total.setText("$ " + formatea.format(rs.getDouble("total")));
                    jd_ver_in_egre.lbl_fecha.setText(rs.getString("fecha"));
                    jd_ver_in_egre.lbl_hora.setText(rs.getString("hora"));
                    jd_ver_in_egre.jtxa_descripcion.setText(rs.getString("descripcion"));
                    jd_ver_in_egre.lbl_cuenta_nombre.setVisible(true);
                    jd_ver_in_egre.lbl_cuenta_nombre.setText(rs.getString("cuenta"));
                    jd_ver_in_egre.lbl_cuenta_titulo.setVisible(true);
                }
                rs.close();

            } catch (SQLException ex) {
                Logger.getLogger(Egresos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        frm.show();
    }

    /**
     * Rediseño: marcar el egreso como pagado apuntando el fondo directamente
     * en la cabecera (antes insertaba en abonos_egresos).
     */
    public static boolean actualizar_estado(String id, String id_fondo) {

        Connection con = null;
        String SSQL = "update egresos set id_fondo=? where id=?";
        try {
            con = getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.setInt(1, Integer.parseInt(id_fondo.trim()));
            psql.setInt(2, Integer.parseInt(id.trim()));
            psql.executeUpdate();
            psql.close();
            con.close();
        } catch (SQLException | NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "No se puede actualizar: \n" + e);
            return false;
        }
        return true;
    }
}
