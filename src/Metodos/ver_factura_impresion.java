/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Metodos;

import conexiondb.DB_consultas_R_D;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.print.PrintService;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import modelos.Contactos;
import modelos.ProductoImprimir;
import modelos.ProductoImprimirOrden;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author mktec
 */
public class ver_factura_impresion extends javax.swing.JDialog {

    public void imprimir(String id_factura) {
        Connection cn = DB_consultas_R_D.getConexion();
        JasperReport report = null;
        Map p = new HashMap();
        p.put("id_factura", Integer.parseInt(id_factura));
        p.put("SUBREPORT_DIR", new File("").getAbsolutePath() + "/src/reportes/");

        try {
            try {
                String cad = new File("").getAbsolutePath() + "/src/reportes/ImprimirFactura_MC.jrxml";
                report = JasperCompileManager.compileReport(cad);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e);
            }
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, p, cn);
            JasperViewer view = new JasperViewer(jasperPrint, false);
            cn.close();
            JDialog dialog = new JDialog(this);//the owner
            dialog.setContentPane(view.getContentPane());
            dialog.setSize(view.getSize());
            dialog.setModal(true);
            dialog.setLocationRelativeTo(this);
            dialog.setTitle("Impresión de orden bodega numero " + id_factura);
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void imprimir_ingreso_media_carta(String id_factura) {
        Connection cn = DB_consultas_R_D.getConexion();
        JasperReport report = null;
        Map p = new HashMap();
        p.put("id_factura", Integer.parseInt(id_factura));
        p.put("SUBREPORT_DIR", new File("").getAbsolutePath() + "/src/reportes/");

        try {
            try {
                String cad = new File("").getAbsolutePath() + "/src/reportes/ImprimirIngreso_MC.jrxml";
                report = JasperCompileManager.compileReport(cad);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e);
            }
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, p, cn);
            JasperViewer view = new JasperViewer(jasperPrint, false);
            cn.close();
            JDialog dialog = new JDialog(this);//the owner
            dialog.setContentPane(view.getContentPane());
            dialog.setSize(view.getSize());
            dialog.setModal(true);
            dialog.setLocationRelativeTo(this);
            dialog.setTitle("Impresión de ingreso numero " + id_factura);
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void imprimir_ingreso_carta_completa(String id_factura) {
        Connection cn = DB_consultas_R_D.getConexion();
        JasperReport report = null;
        Map p = new HashMap();
        p.put("id_factura", Integer.parseInt(id_factura));
        p.put("SUBREPORT_DIR", new File("").getAbsolutePath() + "/src/reportes/");

        try {
            try {
                String cad = new File("").getAbsolutePath() + "/src/reportes/ImprimirIngreso_carta.jrxml";
                report = JasperCompileManager.compileReport(cad);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e);
            }
            JasperPrint jasperPrint = JasperFillManager.fillReport(report, p, cn);
            JasperViewer view = new JasperViewer(jasperPrint, false);
            cn.close();
            JDialog dialog = new JDialog(this);//the owner
            dialog.setContentPane(view.getContentPane());
            dialog.setSize(view.getSize());
            dialog.setModal(true);
            dialog.setLocationRelativeTo(this);
            dialog.setTitle("Impresión tamaño carta de ingreso numero " + id_factura);
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void imprimir_termica_80mm(String id_factura) {
        try {
            ImprimirTermica80MMOrden impresion = construirImpresion(id_factura, null);
            impresion.imprime();

            JOptionPane.showMessageDialog(null, "Impresión enviada correctamente",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al imprimir: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public void imprimir_termica_80mm_silencioso(String id_factura, PrintService service) throws Exception {
        ImprimirTermica80MMOrden impresion = construirImpresion(id_factura, null);
        impresion.imprime(service);
    }

    /**
     * Variante silenciosa que imprime SÓLO los productos asignados a la
     * bodega indicada (la sección "sin bodega" se omite). Pensado para la
     * copia adicional del bodeguero.
     */
    public void imprimir_termica_80mm_silencioso(String id_factura, PrintService service,
            Integer idBodegaFiltro) throws Exception {
        ImprimirTermica80MMOrden impresion = construirImpresion(id_factura, idBodegaFiltro);
        impresion.imprime(service);
    }

    /**
     * Caso especial: factura donde TODOS los ítems son novedades. wo-printer
     * no creó facturas_cabeceras (no había bodega que asignar), así que la
     * data viene 100% de facturas_impresas + detalle_factura.
     */
    public void imprimir_termica_80mm_silencioso_solo_novedad(String numeroFactura,
            PrintService service) throws Exception {
        if (numeroFactura == null || numeroFactura.isEmpty()) {
            return;
        }
        String numEsc = numeroFactura.replace("'", "''");

        // Cabecera desde facturas_impresas
        int idImpresa = -1;
        String cliente = "", fecha = "", vendedor = "", concepto = "";
        try (ResultSet rs = DB_consultas_R_D.getTabla(
                "SELECT id, cliente, fecha_factura, vendedor, concepto "
                + "FROM facturas_impresas WHERE numero_factura = '" + numEsc + "' LIMIT 1")) {
            if (rs != null && rs.next()) {
                idImpresa = rs.getInt("id");
                cliente = nz(rs.getString("cliente"));
                fecha = nz(rs.getString("fecha_factura"));
                vendedor = nz(rs.getString("vendedor"));
                concepto = nz(rs.getString("concepto"));
            }
        }
        if (idImpresa < 0) {
            return; // factura no encontrada
        }

        // Productos (todas son novedades porque no hay cabeceras)
        ArrayList<ProductoImprimirOrden> productos = new ArrayList<>();
        try (ResultSet rs = DB_consultas_R_D.getTabla(
                "SELECT codigo_producto, descripcion, cantidad "
                + "FROM detalle_factura "
                + "WHERE factura_id = " + idImpresa + " "
                + "ORDER BY codigo_producto")) {
            while (rs != null && rs.next()) {
                String cod = nz(rs.getString("codigo_producto"));
                String desc = nz(rs.getString("descripcion"));
                String cant = rs.getString("cantidad") != null ? rs.getString("cantidad") : "0";
                productos.add(new ProductoImprimirOrden(cod, desc, cant, "", null));
            }
        }

        double totalArticulos = 0.0;
        for (ProductoImprimirOrden p : productos) {
            try {
                totalArticulos += Double.parseDouble(p.getCantidad());
            } catch (NumberFormatException ignored) {
            }
        }

        Contactos clienteObj = new Contactos();
        clienteObj.setNombre(cliente);

        ImprimirTermica80MMOrden impresion = new ImprimirTermica80MMOrden(
                fecha, "0", numeroFactura, vendedor,
                String.format(java.util.Locale.US, "%.1f", totalArticulos),
                clienteObj, productos, "Salida", ""
        );
        impresion.setConcepto(concepto);
        impresion.imprime(service);
    }

    /**
     * Copia automática del recibo de VENTA para el usuario con la opción de
     * copia (imp_ticket_bodega_asignada). Se arma desde las SALIDAS de la venta
     * (que sí llevan la bodega por producto) agrupadas por bodega y CON precios,
     * usando el formato de tirilla de venta ya existente. Se imprime en silencio
     * al PrintService indicado (la impresora propia del usuario).
     *
     * Debe llamarse cuando las salidas YA existen (NOTIFY 'VENTACOPIA' que
     * GeneradorOrdenAuto emite al terminar de crearlas), no en el NOTIFY de la
     * venta (que llega antes de que haya salidas).
     *
     * @param idCabeceraVenta id de la cabecera tipo 'Venta'
     * @param service         impresora destino (print_service_user)
     */
    public void imprimir_termica_80mm_venta_copia_silencioso(String idCabeceraVenta,
            PrintService service) throws Exception {
        // 1. Cabecera de la venta
        String codigo = "", fecha = "", hora = "", tipoPago = "";
        String nombreCliente = "", cedula = "", nombreVendedor = "";
        try (ResultSet rs = DB_consultas_R_D.getTabla(
                "SELECT fc.codigo, fc.fecha, fc.hora, fc.tipo_pago, "
                + "c.nombre AS nombre_cliente, c.cedula, u.nombre AS nombre_vendedor "
                + "FROM facturas_cabeceras fc "
                + "LEFT JOIN contactos c ON fc.id_contacto = c.id "
                + "LEFT JOIN users u ON fc.id_user = u.id "
                + "WHERE fc.id = " + idCabeceraVenta)) {
            if (rs != null && rs.next()) {
                codigo = nz(rs.getString("codigo"));
                fecha = nz(rs.getString("fecha"));
                hora = nz(rs.getString("hora"));
                tipoPago = nz(rs.getString("tipo_pago"));
                nombreCliente = nz(rs.getString("nombre_cliente"));
                cedula = nz(rs.getString("cedula"));
                nombreVendedor = nz(rs.getString("nombre_vendedor"));
            }
        }

        // 2. Productos de las SALIDAS de esta venta, agrupados por bodega + precios
        ArrayList<ProductoImprimirOrden> agrupados =
                cargarProductosAgrupados(idCabeceraVenta, codigo, fecha, hora, null);
        if (agrupados.isEmpty()) {
            System.out.println("[ver_factura_impresion] Copia de venta sin productos: " + codigo);
            return;
        }

        // 3. Convertir a líneas de tirilla, insertando un encabezado por bodega
        ArrayList<ProductoImprimir> productos = new ArrayList<>();
        double total = 0.0;
        String bodegaActual = null;
        for (ProductoImprimirOrden po : agrupados) {
            String bod = po.getBodega();
            String etiquetaBodega = (bod == null || bod.trim().isEmpty())
                    ? "SIN BODEGA / NOVEDAD" : bod;
            if (!etiquetaBodega.equals(bodegaActual)) {
                bodegaActual = etiquetaBodega;
                ProductoImprimir header = new ProductoImprimir();
                header.setNombre(">> " + etiquetaBodega.toUpperCase());
                header.setPunitario("");
                header.setCantidad("");
                header.setPtotal("");
                productos.add(header);
            }
            ProductoImprimir pi = new ProductoImprimir();
            pi.setNombre(nz(po.getDescripcion()));
            pi.setPunitario(nz(po.getPrecioUnitario()));
            pi.setCantidad(nz(po.getCantidad()));
            pi.setPtotal(nz(po.getPrecioTotal()));
            productos.add(pi);
            try {
                total += metodos.formateador_dinero().parse(nz(po.getPrecioTotal())).doubleValue();
            } catch (Exception ignored) {
            }
        }

        String credi_contado = "0".equals(tipoPago) ? "CONTADO" : "CREDITO";
        Contactos cliente = new Contactos();
        cliente.setNombre(nombreCliente);
        cliente.setCedula(cedula);

        ImprimirTermica80MM tirilla = new ImprimirTermica80MM(
                fecha, codigo, metodos.formateador_dinero().format(total),
                cliente, productos, nombreVendedor, hora, credi_contado);
        tirilla.imprime(service);
        System.out.println("[ver_factura_impresion] Copia de venta impresa: " + codigo);
    }

    /**
     * Arma el objeto de impresión a partir del id de facturas_cabeceras.
     * Trae cabecera + cliente + vendedor (de facturas_impresas si existe) +
     * productos agrupados por bodega (incluida la sección "sin bodega" para
     * novedades de wo-printer).
     *
     * @param idBodegaFiltro si es != null, sólo carga productos asignados a
     *                       esa bodega y omite la sección de novedades.
     */
    private ImprimirTermica80MMOrden construirImpresion(String id_factura,
            Integer idBodegaFiltro) throws Exception {
        // 1. Cabecera + cliente
        String consulta = "SELECT fc.id, fc.codigo, fc.fecha, fc.hora, fc.tipo_factura, fc.observacion, "
                + "c.nombre as nombre_cliente, c.cedula, c.direccion, c.contacto, "
                + "u.nombre as nombre_vendedor_local "
                + "FROM facturas_cabeceras fc "
                + "LEFT JOIN contactos c ON fc.id_contacto = c.id "
                + "LEFT JOIN users u ON fc.id_user = u.id "
                + "WHERE fc.id = " + id_factura;

        String codigo = "", fecha = "", hora = "", tipo_factura = "", observacion = "";
        String nombre_cliente = "", cedula = "", direccion = "", contacto = "", nombre_vendedor_local = "";

        try (ResultSet rs = DB_consultas_R_D.getTabla(consulta)) {
            if (rs != null && rs.next()) {
                codigo = nz(rs.getString("codigo"));
                fecha = nz(rs.getString("fecha"));
                hora = nz(rs.getString("hora"));
                tipo_factura = nz(rs.getString("tipo_factura"));
                observacion = nz(rs.getString("observacion"));
                nombre_cliente = nz(rs.getString("nombre_cliente"));
                cedula = nz(rs.getString("cedula"));
                direccion = nz(rs.getString("direccion"));
                contacto = nz(rs.getString("contacto"));
                nombre_vendedor_local = nz(rs.getString("nombre_vendedor_local"));
            }
        }

        // 2. Vendedor + concepto desde facturas_impresas (wo-printer)
        String[] datosImpresa = obtenerDatosFacturaImpresa(codigo);
        String nombre_vendedor = datosImpresa != null && datosImpresa[0] != null && !datosImpresa[0].isEmpty()
                ? datosImpresa[0] : nombre_vendedor_local;
        String concepto = datosImpresa != null ? datosImpresa[1] : null;

        // 3. Productos agrupados por bodega + novedades sin bodega
        ArrayList<ProductoImprimirOrden> productos = cargarProductosAgrupados(id_factura, codigo, fecha, hora, idBodegaFiltro);

        // Total artículos
        double totalArticulos = 0.0;
        for (ProductoImprimirOrden p : productos) {
            try {
                totalArticulos += Double.parseDouble(p.getCantidad());
            } catch (NumberFormatException ignored) {
            }
        }

        Contactos cliente = new Contactos();
        cliente.setNombre(nombre_cliente);
        cliente.setCedula(cedula);
        cliente.setDireccion(direccion);
        cliente.setContacto(contacto);

        String fechaHora = fecha + " " + hora;
        ImprimirTermica80MMOrden impresion = new ImprimirTermica80MMOrden(
                fechaHora, id_factura, codigo, nombre_vendedor,
                String.format(java.util.Locale.US, "%.1f", totalArticulos),
                cliente, productos, tipo_factura, observacion
        );
        impresion.setConcepto(concepto);
        return impresion;
    }

    /**
     * Trae los productos de la factura.
     * La sección 3a (productos asignados) filtra por {@code idFactura} directamente
     * (la llave primaria de facturas_cabeceras), lo que funciona tanto para órdenes
     * generadas internamente (codigo vacío) como para las importadas de wo-printer.
     * La sección 3b (novedades wo-printer) sí necesita el {@code numeroFactura}
     * porque es el enlace con la tabla facturas_impresas — se omite si está vacío.
     * Si {@code idBodegaFiltro} es null, devuelve todos los productos más las
     * novedades sin bodega. Si es != null, devuelve sólo los productos asignados
     * a esa bodega y omite la sección de novedades.
     */
    private ArrayList<ProductoImprimirOrden> cargarProductosAgrupados(String idFactura,
            String numeroFactura, String fecha, String hora, Integer idBodegaFiltro) {
        ArrayList<ProductoImprimirOrden> lista = new ArrayList<>();
        if (idFactura == null || idFactura.isEmpty()) {
            return lista;
        }

        // 3a. Productos asignados a alguna bodega (uno por línea de facturas_detalles).
        //
        // Una orden con varias bodegas se guarda como VARIAS cabeceras (una por
        // bodega), todas con el mismo codigo/fecha/hora. Por eso:
        //   - Copia filtrada (idBodegaFiltro != null): solo ESTA cabecera (la de
        //     la bodega del bodeguero).
        //   - Recibo completo (idBodegaFiltro == null): TODAS las cabeceras
        //     hermanas de la orden, para incluir los productos de las demás
        //     bodegas. Se agrupa por codigo+fecha+hora (las hermanas comparten
        //     las tres) — inmune al reúso del mismo codigo en otra fecha/hora.
        //     Si no hay codigo (orden interna sin número), se cae a esta cabecera.
        String filtroCabeceras;
        if (idBodegaFiltro != null) {
            filtroCabeceras = "  fc.id = " + idFactura + " "
                    + "  AND fc.id_bodega = " + idBodegaFiltro + " ";
        } else if (numeroFactura != null && !numeroFactura.isEmpty()
                && fecha != null && !fecha.isEmpty() && hora != null && !hora.isEmpty()) {
            String codEsc = numeroFactura.replace("'", "''");
            filtroCabeceras = "  fc.codigo = '" + codEsc + "' "
                    + "  AND fc.fecha = '" + fecha.replace("'", "''") + "' "
                    + "  AND fc.hora = '" + hora.replace("'", "''") + "' ";
        } else {
            filtroCabeceras = "  fc.id = " + idFactura + " ";
        }
        String sqlAsignados = "SELECT bod.nombre AS bodega, "
                + "       p.codigo_barras AS codigo, "
                + "       p.descripcion, "
                + "       fd.cantidad, "
                + "       fd.subtotal, "
                + "       COALESCE(um.nombre, 'UNIDAD') AS unidad "
                + "FROM facturas_cabeceras fc "
                + "JOIN facturas_detalles fd ON fd.id_cabecera = fc.id "
                + "JOIN productos p ON p.id = fd.id_producto "
                + "LEFT JOIN unidades_medidas um ON um.id = p.id_unidad "
                + "LEFT JOIN bodegas bod ON bod.id = fc.id_bodega "
                + "WHERE " + filtroCabeceras
                + "  AND fc.tipo_factura <> 'Venta' "
                + "ORDER BY bod.nombre NULLS LAST, p.descripcion";

        try (ResultSet rs = DB_consultas_R_D.getTabla(sqlAsignados)) {
            while (rs != null && rs.next()) {
                String bodega = rs.getString("bodega");
                String cod = nz(rs.getString("codigo"));
                String desc = nz(rs.getString("descripcion"));
                String cant = rs.getString("cantidad") != null ? rs.getString("cantidad") : "0";
                String unidad = nz(rs.getString("unidad"));
                if (unidad.isEmpty()) {
                    unidad = "UNIDAD";
                }
                ProductoImprimirOrden po = new ProductoImprimirOrden(cod, desc, cant, unidad, bodega);
                // Precios (usados solo por la copia de venta; el recibo de orden los ignora)
                double subtotal = rs.getDouble("subtotal");
                double cantNum;
                try {
                    cantNum = Double.parseDouble(cant);
                } catch (NumberFormatException nfe) {
                    cantNum = 0.0;
                }
                double unitario = (cantNum != 0.0) ? subtotal / cantNum : subtotal;
                po.setPrecioUnitario(metodos.formateador_dinero().format(unitario));
                po.setPrecioTotal(metodos.formateador_dinero().format(subtotal));
                lista.add(po);
            }
        } catch (Exception ex) {
            System.out.println("[ver_factura_impresion] Error productos asignados: " + ex.getMessage());
        }

        // Si estamos imprimiendo la copia para una bodega específica, no
        // tiene sentido incluir las novedades sin bodega.
        if (idBodegaFiltro != null) {
            return lista;
        }

        // 3b. Novedades de wo-printer (productos no asignados a bodega) — solo si
        // existe numero_factura WO (las órdenes internas no aplican).
        if (numeroFactura == null || numeroFactura.isEmpty()) {
            return lista;
        }
        String numEsc = numeroFactura.replace("'", "''");
        String sqlNovedades = "SELECT df.codigo_producto AS codigo, "
                + "       df.descripcion, "
                + "       df.cantidad "
                + "FROM facturas_impresas fi "
                + "JOIN detalle_factura df ON df.factura_id = fi.id "
                + "WHERE fi.numero_factura = '" + numEsc + "' "
                + "  AND df.es_novedad = TRUE "
                + "ORDER BY df.codigo_producto";

        try (ResultSet rs = DB_consultas_R_D.getTabla(sqlNovedades)) {
            while (rs != null && rs.next()) {
                String cod = nz(rs.getString("codigo"));
                String desc = nz(rs.getString("descripcion"));
                String cant = rs.getString("cantidad") != null ? rs.getString("cantidad") : "0";
                lista.add(new ProductoImprimirOrden(cod, desc, cant, "", null));
            }
        } catch (Exception ex) {
            // Si las tablas de wo-printer no existen aún, no es error fatal.
            System.out.println("[ver_factura_impresion] Sin novedades / tabla wo-printer no disponible: "
                    + ex.getMessage());
        }
        return lista;
    }

    /**
     * Vendedor + concepto desde facturas_impresas (lo escribe wo-printer).
     * Retorna [vendedor, concepto] o null si la tabla no existe / no hay registro.
     */
    private String[] obtenerDatosFacturaImpresa(String numeroFactura) {
        if (numeroFactura == null || numeroFactura.isEmpty()) {
            return null;
        }
        String numEsc = numeroFactura.replace("'", "''");
        String sql = "SELECT vendedor, concepto FROM facturas_impresas "
                + "WHERE numero_factura = '" + numEsc + "' LIMIT 1";
        try (ResultSet rs = DB_consultas_R_D.getTabla(sql)) {
            if (rs != null && rs.next()) {
                return new String[] { rs.getString("vendedor"), rs.getString("concepto") };
            }
        } catch (Exception ex) {
            // Tabla puede no existir si wo-printer no aplicó migración aún.
        }
        return null;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
