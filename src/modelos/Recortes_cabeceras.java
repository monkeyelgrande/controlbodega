/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

import Formularios.frm_ver_orden;
import conexiondb.DB_consultas_R_D;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormatSymbols;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JTable;

/**
 *
 * @author Monkeyelgrande
 */
public class Recortes_cabeceras {

    frm_ver_orden frm = new frm_ver_orden();

    int id, id_producto, id_user;
    String fecha, hora, observacion;
    double cantidad;

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

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
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

    public void cargar_facturas(JTable jtabla, String ver) {
        DecimalFormatSymbols sim = new DecimalFormatSymbols();
        sim.setDecimalSeparator('.');

        try {
            for (int i = 0; i < frm_ver_orden.modelo_productos.getRowCount(); i++) {
                frm_ver_orden.modelo_productos.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
        }
        try {
            for (int i = 0; i < frm_ver_orden.modelo_entregados_cabecera.getRowCount(); i++) {
                frm_ver_orden.modelo_entregados_cabecera.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
        }
        try {
            for (int i = 0; i < frm_ver_orden.modelo_entregados_detalle.getRowCount(); i++) {
                frm_ver_orden.modelo_entregados_detalle.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
        }

        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            String id = (String) jtabla.getValueAt(fila, 0);

            ResultSet rs = DB_consultas_R_D.getTabla("select f.id as id_f, fecha,f.hora, c.id as id_c, c.cedula, c.nombre, c.direccion, "
                    + "c.contacto,f.tipo_factura, u.user_name, f.codigo, f.observacion, f.observacion_entrega "
                    + "from facturas_cabeceras f,contactos c, users u where f.id_user=u.id and f.id_contacto=c.id and f.id =" + id);
            try {
                while (rs.next()) {
                    frm.lbl_numerofactura.setText(rs.getString("id_f"));
                    frm.lbl_fecha.setText(rs.getString("fecha"));
                    frm.lbl_id_cliente.setText(rs.getString("id_c"));
                    frm.lbl_nombre_cliente.setText(rs.getString("nombre"));
                    frm.lbl_cedula_cliente.setText(rs.getString("cedula"));
                    frm.lbl_direccion_cliente.setText(rs.getString("direccion"));
                    frm.lbl_celular_cliente.setText(rs.getString("contacto"));
                    frm.lbl_tipo_factura.setText(rs.getString("tipo_factura"));
                    frm.lbl_user.setText(rs.getString("user_name"));
                    frm.lbl_hora.setText(rs.getString("hora"));
                    frm.txt_codigo.setText(rs.getString("codigo"));
                    frm.txt_observaciones.setText(rs.getString("observacion"));
                    frm.txt_observacion_entrega.setText(rs.getString("observacion_entrega"));
                }
                rs.close();

            } catch (SQLException ex) {
                Logger.getLogger(frm_ver_orden.class.getName()).log(Level.SEVERE, null, ex);
            }

//             MODELO DE PRODUCTOS EN LA FACTURA
            frm_ver_orden.modelo_productos.setColumnIdentifiers(new Object[]{"id_fac_det", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "ENTREGADO", "SALDO"});
            String consulta = "with consulta as (\n"
                    + "	select fd.id, p.id, p.codigo_barras, p.descripcion, fd.cantidad, coalesce((select sum(e.cantidad) from entregas_productos e where e.id_factura=fd.id and e.id_producto=p.id),0) as entrega\n"
                    + "	from productos p inner join facturas_detalles fd on fd.id_producto=p.id \n"
                    + "	where fd.id_cabecera=" + id + " \n"
                    + "group by fd.id, p.id, p.codigo_barras, p.descripcion, fd.cantidad\n"
                    + ")\n"
                    + "select *, (cantidad-entrega) as saldo from consulta";
            System.out.println(consulta);
            rs = DB_consultas_R_D.getTabla(consulta);

            try {
                while (rs.next()) {
                    double cantidad = rs.getDouble("cantidad");
                    double entrega = rs.getDouble("entrega");
                    double saldo = rs.getDouble("saldo");
                    frm_ver_orden.modelo_productos.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                        (cantidad), (entrega), saldo});
                }
                rs.close();
                frm.jtabla_productos.setModel(frm_ver_orden.modelo_productos);
                frm.TamanosTablaProductos();
            } catch (SQLException ex) {
                Logger.getLogger(frm_ver_orden.class.getName()).log(Level.SEVERE, null, ex);
            }

            // MODELO DE PRODUCTOS ENTREGADOS CABECERA
            frm_ver_orden.modelo_entregados_cabecera.setColumnIdentifiers(new Object[]{"Id Entrega", "USER", "FECHA", "HORA", "BODEGA"});

            consulta = "select e.id, u.nombre as usuario, e.fecha_entrega, e.hora_entrega, b.nombre as bodega \n"
                    + "from entregas_productos_cabecera e, users u, bodegas b\n"
                    + "where e.id_user=u.id and e.id_bodega=b.id and e.id_factura=" + id;
//            System.out.println(consulta);
            rs = DB_consultas_R_D.getTabla(consulta);

            try {
                while (rs.next()) {
                    frm_ver_orden.modelo_entregados_cabecera.addRow(new Object[]{rs.getString("id"),
                        rs.getString("usuario"), rs.getString("fecha_entrega"), rs.getString("hora_entrega"), rs.getString("bodega")});
                }
                rs.close();
                frm.jtabla_entregados_cabecera.setModel(frm_ver_orden.modelo_entregados_cabecera);
                frm.TamanosTablaEntregadosCabecera();
            } catch (SQLException ex) {
                Logger.getLogger(frm_ver_orden.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (ver.equals("ver")) {
                frm.btn_entregar.setEnabled(false);
                frm.btn_llenar.setEnabled(false);
                frm.txt_observaciones.setEnabled(false);
                frm.jtabla_productos.setEnabled(false);
                frm.jtabla_entregados_cabecera.setEnabled(true);
                frm.txt_observacion_entrega.setEnabled(false);
            }
            frm.txt_codigo.requestFocus();
            frm.show();
        }
    }

}
