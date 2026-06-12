/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Metodos;

import Formularios.frm_main;
import conexiondb.DB_consultas_R_D;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import static java.awt.print.Printable.NO_SUCH_PAGE;
import static java.awt.print.Printable.PAGE_EXISTS;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.print.PrintService;
import modelos.Contactos;
import modelos.ProductoImprimir;

/**
 *
 */
public class ImprimirTermica80MM {

    String no_factura, fecha_hora, total, credi_contado;
    Contactos cliente;
    ArrayList<ProductoImprimir> productos;
    double middleHeight = 0.0;
    String user;
    String hora;
    String observacion;

    public ImprimirTermica80MM(String fecha, String no_factura, String total, Contactos cliente, ArrayList<ProductoImprimir> productos, String user, String hora, String credi_contado) {

        this.fecha_hora = fecha;
        this.no_factura = no_factura;
        this.total = total;
        this.cliente = cliente;
        this.productos = productos;
        this.user = user;
        this.hora = hora;
        this.credi_contado = credi_contado;
        this.observacion = observacion;

        middleHeight = productos.size() + 12.0;

    }

    public PageFormat getPageFormat(PrinterJob pj) {

        PageFormat pf = pj.defaultPage();
        Paper paper = pf.getPaper();

        double headerHeight = 2.0;
        double footerHeight = 3.0;
        double width = convert_CM_To_PPI(8);      //printer know only point per inch.default value is 72ppi aca se define el ancho de la impresion
        double height = convert_CM_To_PPI(headerHeight + middleHeight + footerHeight);
        paper.setSize(width, height);
        paper.setImageableArea(
                0,
                10,
                width,
                height - convert_CM_To_PPI(1)
        );   //define boarder size    after that print area width is about 180 points

        pf.setOrientation(PageFormat.PORTRAIT);           //select orientation portrait or landscape but for this time portrait
        pf.setPaper(paper);

        return pf;
    }

    protected static double convert_CM_To_PPI(double cm) {
        return toPPI(cm * 0.393600787);
    }

    protected static double toPPI(double inch) {
        return inch * 72d;
    }

    public static PrintService buscarImpresoraPorNombre(String nombre) {
        PrintService service = null;
        PrintService[] services = PrinterJob.lookupPrintServices();

        for (PrintService printService : services) {
            if (printService.getName().contains(nombre)) {
                service = printService;
                break;
            }
        }
        return service;
    }

    public void imprime() throws PrinterException {
        imprime(buscarImpresoraPorNombre(frm_main.impresora_ticket));
    }

    /**
     * Variante silenciosa: imprime directo al PrintService indicado (sin diálogo
     * ni búsqueda por nombre). Usada por la copia automática de venta del
     * Listener, que imprime en la impresora propia del usuario (print_service_user).
     */
    public void imprime(PrintService service) throws PrinterException {
        if (service != null) {
            PrinterJob pj = PrinterJob.getPrinterJob();
            pj.setPrintService(service);

            pj.setCopies(1);
            pj.setPrintable(new Printables(), getPageFormat(pj));
            try {
                pj.print();
            } catch (PrinterException ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("No se encontró la impresora con el nombre especificado.");
        }
    }

    public class Printables implements Printable {

        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
                throws PrinterException {

            int result = NO_SUCH_PAGE;
            if (pageIndex == 0) {

                Graphics2D g2d = (Graphics2D) graphics;

                double width = pageFormat.getImageableWidth();
                g2d.translate((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY());

                FontMetrics metrics = g2d.getFontMetrics(new Font("Arial", Font.BOLD, 10));
//
//                int nombrenegocio_posicion = (int) ((width - metrics.stringWidth(nombre_negocio)) / 2);;
//                int nit_posicion = (int) ((width - metrics.stringWidth("NIT/RUT. " + nit)) / 2);;
                int fecha_hora_posicion = (int) ((width - metrics.stringWidth(fecha_hora)) / 2);;
                int no_fac_posicion = (int) ((width - metrics.stringWidth("Factura No. " + no_factura)) / 2);;

                try {
                    /*Draw Header*/
                    int y = 20;
                    int yShift = 15;
                    int yProducto = 7;

                    g2d.setFont(new Font("Arial", Font.BOLD, 10));
                    y += yShift;
                    g2d.drawString("ORDEN No. " + no_factura, no_fac_posicion, y);
                    y += yShift;
                    g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                    g2d.drawString("-------------------------------------------------------", 12, y);
                    y += yShift;
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    g2d.drawString("Cliente: " + cliente.getNombre(), 12, y);
                    y += yShift;
                    g2d.drawString("Asesor: " + user, 12, y);
                    y += yShift;
                    g2d.drawString("Hora: " + hora + " / " + fecha_hora, 12, y);
                    y += yShift;
                    g2d.drawString("Pago: " + credi_contado, 12, y);
                    y += yShift;
                    g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                    g2d.drawString("_____________________________________", 12, y);
                    g2d.setFont(new Font("Arial", Font.BOLD, 8));
                    y += yShift;
                    g2d.drawString("Artículo", 12, y);
                    g2d.drawString("Precio", 110, y);
                    g2d.drawString("Cant.", 145, y);
                    g2d.drawString("Total", 175, y);
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString("_____________________________________", 10, y);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 7)); // aca se define el tamaño del font
                    y += yShift;
                    for (int i = 0; i < productos.size(); i++) {
                        // Obtenemos el nombre completo del producto
                        String nombre = productos.get(i).getNombre().toString();

                        // Definimos las tres líneas (máximo 20 caracteres cada una)
                        String nombreL1 = "";
                        String nombreL2 = "";
                        String nombreL3 = "";

                        // Primera línea: siempre se imprimen los primeros 20 caracteres o el nombre completo si es menor
                        nombreL1 = nombre.substring(0, Math.min(20, nombre.length()));

                        // Segunda línea: si el nombre tiene más de 20 caracteres
                        if (nombre.length() > 20) {
                            nombreL2 = nombre.substring(20, Math.min(40, nombre.length()));
                        }

                        // Tercera línea: si el nombre tiene más de 40 caracteres
                        if (nombre.length() > 40) {
                            nombreL3 = nombre.substring(40, Math.min(60, nombre.length()));
                        }

                        // Imprime la primera línea en la posición actual (x=12, y)
                        g2d.drawString(nombreL1, 12, y);

                        // Si existe la segunda línea, incrementamos y y la imprimimos
                        if (!nombreL2.isEmpty()) {
                            y += yProducto;  // yProducto es el salto vertical para las líneas adicionales
                            g2d.drawString(nombreL2, 12, y);
                        }

                        // Si existe la tercera línea, se incrementa nuevamente y y se imprime
                        if (!nombreL3.isEmpty()) {
                            y += yProducto;
                            g2d.drawString(nombreL3, 12, y);
                        }

                        // Se imprimen los demás datos del producto en la misma línea donde terminó el nombre
                        g2d.drawString(productos.get(i).getPunitario(), 110, y);
                        g2d.drawString(productos.get(i).getCantidad(), 148, y);
                        g2d.drawString(productos.get(i).getPtotal(), 175, y);

                        // Se incrementa la posición vertical para el siguiente producto
                        y += yShift;
                    }

                    g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                    g2d.drawString("_____________________________________", 12, y);

                    g2d.setFont(new Font("Arial", Font.BOLD, 8));
                    g2d.drawString("TOTAL ORDEN", 80, y);
                    g2d.setFont(new Font("Arial", Font.BOLD, 10));
                    g2d.drawString("$ " + total, 160, y);
                    y += yShift;

                    g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                    y += yShift;
                    g2d.drawString("***********************************************", 10, y);
                    y += yShift;
                    g2d.drawString("GRACIAS POR SU VISITA", 35, y);
                    y += yShift;
                    g2d.drawString("***********************************************", 10, y);
                    y += yShift;
                    y += yShift;

                } catch (Exception r) {
                    r.printStackTrace();
                }

                result = PAGE_EXISTS;
            }
            return result;
        }
    }
}
