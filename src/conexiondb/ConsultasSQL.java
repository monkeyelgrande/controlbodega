/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

/**
 *
 * @author Monkeyelgrande
 */
public class ConsultasSQL {



    public static String ConsultaFacturasAbonos() {
        return "select ca.id as id_crap, f.id as id_factura, c.nombre as cliente,c.cedula, f.total as total_factura, ca.abono as abono_total, "
                + "(f.total-ca.abono) as saldo, ca.fecha_plazo_final as fecha_fin, f.tipo_factura, (current_date-ca.fecha_plazo_final) as dias_vence  "
                + "from facturas_cabeceras f, creditos_apartados ca, contactos c "
                + "where ca.id_factura=f.id and f.id_contacto=c.id and f.estado='1' and (f.total-ca.abono)>0 order by ca.fecha_plazo_final desc";
    }

    public static String ConsultaFacturasAbonosConPagado() {
        return "select ca.id as id_crap, f.id as id_factura, c.nombre as cliente,c.cedula, f.total as total_factura, ca.abono as abono_total, "
                + "(f.total-ca.abono) as saldo, ca.fecha_plazo_final as fecha_fin, f.tipo_factura, (current_date-ca.fecha_plazo_final) as dias_vence  "
                + "from facturas_cabeceras f, creditos_apartados ca, contactos c "
                + "where ca.id_factura=f.id and f.id_contacto=c.id and f.estado='1' order by ca.fecha_plazo_final";
    }

    public static String ConsultaFacturasAbonosCreditos() {
        return "select ca.id as id_crap, f.id as id_factura, c.nombre as cliente, f.total as total_factura, ca.abono as abono_total, "
                + "(f.total-ca.abono) as saldo, ca.fecha_plazo_final as fecha_fin, f.tipo_factura from facturas_cabeceras f, creditos_apartados ca, contactos c "
                + "where ca.id_factura=f.id and f.id_contacto=c.id and f.tipo_factura='Credito' and f.estado='1' and (f.total-ca.abono)>0 order by id_factura";
    }

    public static String ConsultaFacturasAbonosApartados() {
        return "select ca.id as id_crap, f.id as id_factura, c.nombre as cliente, f.total as total_factura, ca.abono as abono_total, "
                + "(f.total-ca.abono) as saldo, ca.fecha_plazo_final as fecha_fin, f.tipo_factura from facturas_cabeceras f, creditos_apartados ca, contactos c "
                + "where ca.id_factura=f.id and f.id_contacto=c.id and f.tipo_factura='Apartado' and f.estado='1' and (f.total-ca.abono)>0 order by id_factura";
    }

    public static String ConsultaFacturasPorFecha_reporte(String fecha) {
        return "select f.id, c.nombre, f.total from facturas_cabeceras f, contactos c where f.id_contacto=c.id and f.tipo_factura='Venta' and f.estado='1' "
                + "and f.fecha='" + fecha + "'";
    }

    public static String ConsultaFacturasEntreFechas_reporte(String fecha1, String fecha2) {
        return "select f.id, c.nombre, f.total, f.fecha from facturas_cabeceras f, contactos c "
                + "where f.id_contacto=c.id and f.tipo_factura='Venta' and f.estado='1' and f.fecha between'" + fecha1 + "' and '" + fecha2 + "' order by f.fecha";
    }

    public static String ConsultaIngresosProductosEntreFecha_reporte(String fecha1, String fecha2) {
        return "select i.id, p.codigo_barras, p.descripcion, "
                + "c.nombre as proveedor, u.user_name as user, i.fecha, i.cantidad "
                + "from ingresos_mercancias i, contactos c, productos p, users u where i.id_producto = p.id and i.cantidad > 0 and "
                + " c.id = i.id_contacto and u.id = i.id_user "
                + "and i.fecha between '" + fecha1 + "' and '" + fecha2 + "' order by i.fecha";
    }

    public static String ConsultaSalidaProductosEntreFecha_reporte(String fecha1, String fecha2) {
        return "select fc.id as id_factura, p.codigo_barras, p.descripcion, fc.fecha, c.nombre as contacto, fd.cantidad "
                + "from facturas_detalles fd, facturas_cabeceras fc, productos p, contactos c "
                + "where fd.id_cabecera=fc.id and fd.id_producto=p.id and fc.id_contacto=c.id and fc.estado='1' and fc.fecha between '" + fecha1 + "' and '" + fecha2 + "' order by fc.fecha";
    }


    public static String ConsultaSalidaProductosEntreFechaXProductoYContacto_reporte(String fecha1, String fecha2, String codigo_producto, String codigo_contacto) {
        return "select fc.id as id_factura, fc.fecha, c.nombre as contacto, fd.cantidad "
                + "from facturas_detalles fd, facturas_cabeceras fc, productos p, contactos c "
                + "where fd.id_cabecera=fc.id and fd.id_producto=p.id and fc.id_contacto=c.id and fd.id_producto=" + codigo_producto + " and "
                + "fc.estado='1' and c.id ="+ codigo_contacto +" and "
                + "fc.fecha between '" + fecha1 + "' and '" + fecha2 + "' order by fc.fecha";
    }

    public static String ConsultaClientes() {
        return "select distinct c.id,c.nombre,c.cedula,c.contacto,c.ciudad,c.direccion "
                + "from contactos c, facturas_cabeceras f where f.id_contacto=c.id order by c.id";
    }

    public static String ConsultaProveedores() {
        return "select distinct c.id,c.nombre,c.cedula,c.contacto,c.ciudad,c.direccion "
                + "from contactos c, egresos e where e.id_contacto=c.id order by c.id";
    }

}
