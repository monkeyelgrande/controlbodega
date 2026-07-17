/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

import Formularios.frm_main;
import Formularios.frm_ver_orden;
import Formularios.frm_ver_factura_venta;
import Metodos.metodos;
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
public class Facturas_cabeceras {

    frm_ver_orden frm = new frm_ver_orden();

    int id, id_cliente, id_user, anulado, tipo_pago, id_bodega;
    String fecha, hora, tipo, codigo, observacion, observacion_entrega;

    public int getId_bodega() {
        return id_bodega;
    }

    public void setId_bodega(int id_bodega) {
        this.id_bodega = id_bodega;
    }

    public int getTipo_pago() {
        return tipo_pago;
    }

    public void setTipo_pago(int tipo_pago) {
        this.tipo_pago = tipo_pago;
    }

    public String getObservacion_entrega() {
        return observacion_entrega;
    }

    public void setObservacion_entrega(String observacion_entrega) {
        this.observacion_entrega = observacion_entrega;
    }

    public int getAnulado() {
        return anulado;
    }

    public void setAnulado(int anulado) {
        this.anulado = anulado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_cliente() {
        return id_cliente;
    }

    public void setId_cliente(int id_cliente) {
        this.id_cliente = id_cliente;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public void cargar_facturas(JTable jtabla, String ver) {
        DecimalFormatSymbols sim = new DecimalFormatSymbols();
        sim.setDecimalSeparator('.');

        try {
            frm_ver_orden.modelo_productos.setRowCount(0);
            frm_ver_orden.modelo_entregados_cabecera.setRowCount(0);
            frm_ver_orden.modelo_entregados_detalle.setRowCount(0);
        } catch (Exception m) {
        }

        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            String id = jtabla.getValueAt(fila, 0).toString();

            ResultSet rs = DB_consultas_R_D.getTabla(
                    "SELECT "
                    + "   f.id AS id_f, "
                    + "   f.fecha, "
                    + "   f.hora, "
                    + "   c.id AS id_c, "
                    + "   c.cedula, "
                    + "   c.nombre, "
                    + "   c.direccion, "
                    + "   c.contacto, "
                    + "   f.tipo_factura, "
                    + "   u.user_name, "
                    + "   f.codigo, "
                    + "   f.observacion, "
                    + "   f.observacion_entrega, "
                    + "   f.id_bodega, "
                    + "   b.nombre AS bodega "
                    + "FROM facturas_cabeceras f "
                    + "JOIN contactos c ON f.id_contacto = c.id "
                    + "JOIN users u ON f.id_user = u.id "
                    + "LEFT JOIN bodegas b ON b.id = f.id_bodega "
                    + "WHERE f.id = " + id
            );

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
                    frm.jbox_bodega.setSelectedItem(rs.getString("bodega"));
                    frm.id_bodega = rs.getInt("id_bodega");
                    frm.nombre_bodega = rs.getString("bodega");
                }
                rs.close();
            } catch (SQLException ex) {
                Logger.getLogger(frm_ver_orden.class.getName()).log(Level.SEVERE, null, ex);
            }

            // ════════════════════════════════════════════════════════════════════════════
            // MODELO DE PRODUCTOS - AHORA CON COLUMNA R (id_factura para referenciadas)
            // ════════════════════════════════════════════════════════════════════════════
            frm_ver_orden.modelo_productos.setColumnIdentifiers(new Object[]{
                "id_prod", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "ENTREGADO", "SALDO", "PRECIO", "TOTAL", "R"
            });

            String consulta = "WITH consulta AS (\n"
                    + "  SELECT \n"
                    + "    fd.id,\n"
                    + "    p.id AS id_producto,\n"
                    + "    p.codigo_barras,\n"
                    + "    p.descripcion,\n"
                    + "    fd.cantidad::numeric(18,4)          AS cantidad,\n"
                    + "    fd.subtotal::numeric(18,4)          AS subtotal,\n"
                    + "    (fd.cantidad::numeric(18,4) \n"
                    + "     * fd.subtotal::numeric(18,4))      AS total,\n"
                    + "    fd.id_factura,\n" // ← COLUMNA R (id_factura de WorldOffice)
                    + "    COALESCE((\n"
                    + "      SELECT SUM(e.cantidad)::numeric(18,4)\n"
                    + "      FROM entregas_productos e\n"
                    + "      WHERE e.id_factura = fd.id_cabecera\n"
                    + "        AND e.id_producto = p.id\n"
                    + "    ), 0)::numeric(18,4)                 AS entrega\n"
                    + "  FROM productos p\n"
                    + "  INNER JOIN facturas_detalles fd ON fd.id_producto = p.id \n"
                    + "  WHERE fd.id_cabecera = " + id + "\n"
                    + "  GROUP BY fd.id, p.id, p.codigo_barras, p.descripcion, fd.cantidad, fd.subtotal, fd.id_factura\n"
                    + ")\n"
                    + "SELECT\n"
                    + "  *,\n"
                    + "  CASE \n"
                    + "    WHEN ABS(cantidad - entrega) < 0.00005 THEN 0::numeric(18,4)\n"
                    + "    ELSE ROUND(cantidad - entrega, 2)\n"
                    + "  END AS saldo\n"
                    + "FROM consulta;";

            System.out.println(consulta);
            rs = DB_consultas_R_D.getTabla(consulta);

            try {
                while (rs.next()) {
                    double cantidad = rs.getDouble("cantidad");
                    double entrega = rs.getDouble("entrega");
                    double saldo = rs.getDouble("saldo");
                    int idFacturaRef = rs.getInt("id_factura"); // ← COLUMNA R

                    frm_ver_orden.modelo_productos.addRow(new Object[]{
                        rs.getString("id_producto"), // Columna 0: id_producto (cambiado de id detalle)
                        rs.getString("codigo_barras"),
                        rs.getString("descripcion"),
                        metodos.formateador_decimal_punto_para_decimal().format(cantidad),
                        entrega,
                        saldo,
                        metodos.formateador_dinero().format(rs.getDouble("subtotal")),
                        metodos.formateador_dinero().format(rs.getDouble("total")),
                        idFacturaRef // Columna 8: R (id_factura de WorldOffice)
                    });
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
                    + "where e.id_user=u.id and e.id_bodega=b.id and e.id_factura=" + id + " order by fecha_entrega";

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
            // La bodega de entrega NO es editable: siempre es la de la orden. Se
            // bloquea el combo para todos los perfiles (antes solo el bodeguero),
            // de modo que no se pueda entregar en una bodega distinta.
            frm.jbox_bodega.setEnabled(false);
            frm.show();
        }

    }

    public void cargar_facturas_ventas(JTable jtabla, String ver) {
        frm_ver_factura_venta frm_venta = new frm_ver_factura_venta();

        DecimalFormatSymbols sim = new DecimalFormatSymbols();
        sim.setDecimalSeparator('.');

        try {
            for (int i = 0; i < frm_ver_factura_venta.modelo_productos.getRowCount(); i++) {
                frm_ver_factura_venta.modelo_productos.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
        }

        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            String id = jtabla.getValueAt(fila, 0).toString();

            ResultSet rs = DB_consultas_R_D.getTabla("select f.id as id_f, fecha,f.hora, c.id as id_c, c.cedula, c.nombre, c.direccion, f.tipo_pago, "
                    + "c.contacto,f.tipo_factura, u.user_name, f.codigo, f.observacion, f.observacion_entrega "
                    + "from facturas_cabeceras f,contactos c, users u where f.id_user=u.id and f.id_contacto=c.id and f.id =" + id);
            try {
                while (rs.next()) {
                    String tipo_pago = "";
                    if (rs.getInt("tipo_pago") == 0) {
                        tipo_pago = "Contado";
                    } else {
                        tipo_pago = "Crédito";

                    }

                    frm_ver_factura_venta.lbl_numerofactura.setText(rs.getString("id_f"));
                    frm_ver_factura_venta.lbl_fecha.setText(rs.getString("fecha"));
                    frm_ver_factura_venta.lbl_id_cliente.setText(rs.getString("id_c"));
                    frm_ver_factura_venta.lbl_nombre_cliente.setText(rs.getString("nombre"));
                    frm_ver_factura_venta.lbl_cedula_cliente.setText(rs.getString("cedula"));
                    frm_ver_factura_venta.lbl_direccion_cliente.setText(rs.getString("direccion"));
                    frm_ver_factura_venta.lbl_celular_cliente.setText(rs.getString("contacto"));
                    frm_ver_factura_venta.lbl_tipo_factura.setText(rs.getString("tipo_factura"));
                    frm_ver_factura_venta.lbl_tipo_pago.setText(tipo_pago);
                    frm_ver_factura_venta.lbl_user.setText(rs.getString("user_name"));
                    frm_ver_factura_venta.lbl_hora.setText(rs.getString("hora"));
                    frm_ver_factura_venta.txt_codigo.setText(rs.getString("codigo"));
                    frm_ver_factura_venta.txt_observaciones.setText(rs.getString("observacion"));
                    frm_ver_factura_venta.txt_observacion_entrega.setText(rs.getString("observacion_entrega"));
                }

                rs.close();

            } catch (SQLException ex) {
                Logger.getLogger(frm_ver_factura_venta.class.getName()).log(Level.SEVERE, null, ex);
            }

//             MODELO DE PRODUCTOS EN LA FACTURA
            frm_ver_factura_venta.modelo_productos.setColumnIdentifiers(new Object[]{"id_fac_det", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "PRECIO", "TOTAL"});

            String consulta = "with consulta as (\n"
                    + "	select fd.id, p.id, p.codigo_barras, p.descripcion, fd.cantidad, fd.subtotal, (fd.cantidad*fd.subtotal) as total, "
                    + "coalesce((select sum(e.cantidad) from entregas_productos e where e.id_factura=fd.id_cabecera and e.id_producto=p.id),0) as entrega\n"
                    + "	from productos p inner join facturas_detalles fd on fd.id_producto=p.id \n"
                    + "	where fd.id_cabecera=" + id + " \n"
                    + "group by fd.id, p.id, p.codigo_barras, p.descripcion, fd.cantidad, fd.subtotal \n"
                    + ")\n"
                    + "select *, (cantidad-entrega) as saldo from consulta";
//            System.out.println(consulta);
            rs = DB_consultas_R_D.getTabla(consulta);

            try {
                while (rs.next()) {
                    double cantidad = rs.getDouble("cantidad");

                    frm_ver_factura_venta.modelo_productos.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                        (cantidad), metodos.formateador_dinero().format(rs.getDouble("subtotal")),
                        metodos.formateador_dinero().format(rs.getDouble("total"))});

                }
                rs.close();
                frm_venta.jtabla_productos.setModel(frm_ver_factura_venta.modelo_productos);
                frm_venta.TamanosTablaProductos();
            } catch (SQLException ex) {
                Logger.getLogger(frm_ver_factura_venta.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (ver.equals("ver")) {
                frm_venta.txt_observaciones.setEnabled(false);
                frm_venta.jtabla_productos.setEnabled(false);
                frm_venta.txt_observacion_entrega.setEnabled(false);
            }
            frm_venta.txt_codigo.requestFocus();
            frm_venta.calcular_total();
            frm_venta.show();
        }
    }

    public void cargar_cotizaciones(JTable jtabla, String ver) {
        frm_ver_factura_venta frm_venta = new frm_ver_factura_venta();

        DecimalFormatSymbols sim = new DecimalFormatSymbols();
        sim.setDecimalSeparator('.');

        try {
            for (int i = 0; i < frm_ver_factura_venta.modelo_productos.getRowCount(); i++) {
                frm_ver_factura_venta.modelo_productos.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
        }

        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            String id = jtabla.getValueAt(fila, 0).toString();

            ResultSet rs = DB_consultas_R_D.getTabla("select f.id as id_f, fecha,f.hora, c.id as id_c, c.cedula, c.nombre, c.direccion, f.tipo_pago, "
                    + "c.contacto,f.tipo_factura, u.user_name, f.codigo, f.observacion, f.observacion_entrega "
                    + "from cotizaciones_cabeceras f,contactos c, users u where f.id_user=u.id and f.id_contacto=c.id and f.id =" + id);
            try {
                while (rs.next()) {
                    String tipo_pago = "";
                    if (rs.getInt("tipo_pago") == 0) {
                        tipo_pago = "Contado";
                    } else {
                        tipo_pago = "Crédito";

                    }

                    frm_ver_factura_venta.lbl_numerofactura.setText(rs.getString("id_f"));
                    frm_ver_factura_venta.lbl_fecha.setText(rs.getString("fecha"));
                    frm_ver_factura_venta.lbl_id_cliente.setText(rs.getString("id_c"));
                    frm_ver_factura_venta.lbl_nombre_cliente.setText(rs.getString("nombre"));
                    frm_ver_factura_venta.lbl_cedula_cliente.setText(rs.getString("cedula"));
                    frm_ver_factura_venta.lbl_direccion_cliente.setText(rs.getString("direccion"));
                    frm_ver_factura_venta.lbl_celular_cliente.setText(rs.getString("contacto"));
                    frm_ver_factura_venta.lbl_tipo_factura.setText(rs.getString("tipo_factura"));
                    frm_ver_factura_venta.lbl_tipo_pago.setText(tipo_pago);
                    frm_ver_factura_venta.lbl_user.setText(rs.getString("user_name"));
                    frm_ver_factura_venta.lbl_hora.setText(rs.getString("hora"));
                    frm_ver_factura_venta.txt_codigo.setText(rs.getString("codigo"));
                    frm_ver_factura_venta.txt_observaciones.setText(rs.getString("observacion"));
                    frm_ver_factura_venta.txt_observacion_entrega.setText(rs.getString("observacion_entrega"));
                }

                rs.close();

            } catch (SQLException ex) {
                Logger.getLogger(frm_ver_factura_venta.class.getName()).log(Level.SEVERE, null, ex);
            }

//             MODELO DE PRODUCTOS EN LA FACTURA
            frm_ver_factura_venta.modelo_productos.setColumnIdentifiers(new Object[]{"id_fac_det", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "PRECIO", "TOTAL"});

            String consulta = "with consulta as (\n"
                    + "	select fd.id, p.id, p.codigo_barras, p.descripcion, fd.cantidad, fd.subtotal, (fd.cantidad*fd.subtotal) as total, "
                    + "coalesce((select sum(e.cantidad) from entregas_productos e where e.id_factura=fd.id_cabecera and e.id_producto=p.id),0) as entrega\n"
                    + "	from productos p inner join cotizaciones_detalles fd on fd.id_producto=p.id \n"
                    + "	where fd.id_cabecera=" + id + " \n"
                    + "group by fd.id, p.id, p.codigo_barras, p.descripcion, fd.cantidad, fd.subtotal \n"
                    + ")\n"
                    + "select *, (cantidad-entrega) as saldo from consulta";
//            System.out.println(consulta);
            rs = DB_consultas_R_D.getTabla(consulta);

            try {
                while (rs.next()) {
                    double cantidad = rs.getDouble("cantidad");

                    frm_ver_factura_venta.modelo_productos.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                        (cantidad), metodos.formateador_dinero().format(rs.getDouble("subtotal")),
                        metodos.formateador_dinero().format(rs.getDouble("total"))});

                }
                rs.close();
                frm_venta.jtabla_productos.setModel(frm_ver_factura_venta.modelo_productos);
                frm_venta.TamanosTablaProductos();
            } catch (SQLException ex) {
                Logger.getLogger(frm_ver_factura_venta.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (ver.equals("ver")) {
                frm_venta.txt_observaciones.setEnabled(false);
                frm_venta.jtabla_productos.setEnabled(false);
                frm_venta.txt_observacion_entrega.setEnabled(false);
            }
            frm_venta.txt_codigo.requestFocus();
            frm_venta.calcular_total();
            frm_venta.show();
        }
    }

}
