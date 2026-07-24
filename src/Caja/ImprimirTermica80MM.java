/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Caja;

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
import javax.print.PrintService;

/**
 *
 * @author mktec
 */
public class ImprimirTermica80MM {

    String nombre_negocio, no_factura, fecha_hora, nombre_vendedor, total, fondo, cuenta, descripcion;
    double middleHeight = 0.0;

    public ImprimirTermica80MM(String fecha, String no_factura, String nombre_vendedor, String total, String fondo, String cuenta, String descripcion) {

        this.fecha_hora = fecha;
        this.no_factura = no_factura;
        this.nombre_vendedor = nombre_vendedor;
        this.total = total;
        this.fondo = fondo;
        this.cuenta = cuenta;
        this.descripcion = descripcion;

        middleHeight = 12.0;
        String cadena = "select * from configuraciones";
        ResultSet rs = DB_consultas_R_D.getTabla(cadena);
        try {
            while (rs.next()) {
                this.nombre_negocio = rs.getString("nombre_negocio");
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
        }

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

        PrintService service = buscarImpresoraPorNombre(frm_main.impresora_ticket);
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

                int nombrenegocio_posicion = (int) ((width - metrics.stringWidth(nombre_negocio)) / 2);;
                int fecha_hora_posicion = (int) ((width - metrics.stringWidth(fecha_hora)) / 2);;
                int no_fac_posicion = (int) ((width - metrics.stringWidth("Recibo No. " + no_factura)) / 2);;

                try {
                    /*Draw Header*/
                    int y = 20;
                    int yShift = 15;
                    int yProducto = 7;

                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString("_____________________________________", 12, y);
                    y += yShift;
                    g2d.drawString(nombre_negocio, nombrenegocio_posicion, y);
                    y += yShift;
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    g2d.drawString(fecha_hora, fecha_hora_posicion, y);
                    g2d.setFont(new Font("Arial", Font.BOLD, 10));
                    y += yShift;
                    g2d.drawString("Recibo No. " + no_factura, no_fac_posicion, y);
                    y += yShift;
                    g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                    g2d.drawString("-------------------------------------------------------", 12, y);
                    y += yShift;
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    g2d.drawString("Empleado: " + nombre_vendedor, 12, y);
                    y += yShift;
                    g2d.setFont(new Font("Arial", Font.BOLD, 10));
                    g2d.drawString("CUENTA:     " + cuenta, 12, y);
                    y += yShift;
                    g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                    g2d.drawString("_____________________________________", 12, y);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    g2d.drawString("Descripción", 12, y);
                    y += yShift;

                    int maxLineLength = 39; // Longitud máxima de caracteres por línea
                    int largo = descripcion.length();
                    int start = 0;

                    while (start < largo) {
                        // Calcular el final de la línea
                        int end = Math.min(start + maxLineLength, largo);

                        // Ajustar el final para evitar dividir palabras
                        if (end < largo && descripcion.charAt(end) != ' ') {
                            int lastSpace = descripcion.lastIndexOf(' ', end);
                            if (lastSpace > start) {
                                end = lastSpace;
                            }
                        }

                        // Dibujar la línea
                        g2d.drawString(descripcion.substring(start, end), 12, y);
                        y += yShift;

                        // Actualizar el inicio para la próxima iteración
                        start = end;

                        // Saltar los espacios al principio de la siguiente línea
                        while (start < largo && descripcion.charAt(start) == ' ') {
                            start++;
                        }
                    }

                    g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                    g2d.drawString("_____________________________________", 12, y);
                    y += yShift;
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString("TOTAL: ", 12, y);
                    y += yShift;
                    g2d.drawString("$ " + total, 12, y);
                    y += yShift;
                    g2d.drawString("***********************************************", 10, y);
                    y += yShift;
                    g2d.drawString("------COMPROBANTE------", 35, y);
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
