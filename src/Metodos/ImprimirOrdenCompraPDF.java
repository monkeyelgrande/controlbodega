package Metodos;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import conexiondb.DB_consultas_R_D;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.print.PrintService;
import javax.swing.JOptionPane;
import modelos.Ordenes_compra_cabecera;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

/**
 * Genera en PDF (OpenPDF) una orden de compra aprobada: datos del negocio,
 * número/fecha/estado, creada/aprobada por, y la tabla de productos con
 * proveedor, precio unitario y subtotal por línea más el total general.
 *
 * @author Monkeyelgrande
 */
public class ImprimirOrdenCompraPDF {

    private static final Color BRAND_DARK = new Color(0x0D47A1);
    private static final Color BRAND_SOFT = new Color(0xE3F2FD);
    private static final Color TABLE_HEAD = new Color(0x1565C0);
    private static final Color ROW_ALT = new Color(0xF5F8FC);
    private static final Color BORDER_GRAY = new Color(0xD2D6DC);
    private static final Color TEXT_MUTED = new Color(0x69707A);

    private final Font fBrandTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, BRAND_DARK);
    private final Font fLabel = FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_MUTED);
    private final Font fLabelSmall = FontFactory.getFont(FontFactory.HELVETICA, 6, TEXT_MUTED);
    private final Font fValue = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
    private final Font fValueBig = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BRAND_DARK);
    private final Font fItemHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private final Font fItemBody = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private final Font fTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);

    private final DecimalFormat money = new DecimalFormat("###,###,##0");

    // ==================== API pública ====================
    public void imprimir(int idOrden) {
        try {
            DatosOrden datos;
            try (Connection cn = DB_consultas_R_D.getConexion()) {
                datos = cargarDatos(cn, idOrden);
            }
            if (datos == null) {
                JOptionPane.showMessageDialog(null, "No se encontró la orden #" + idOrden);
                return;
            }
            File pdf = File.createTempFile("orden_compra_" + idOrden + "_", ".pdf");
            pdf.deleteOnExit();
            generarPDF(datos, pdf);

            Object[] opciones = {"Imprimir", "Ver", "Cancelar"};
            int sel = JOptionPane.showOptionDialog(null,
                    "¿Qué deseas hacer con la orden de compra?",
                    "Orden " + datos.numero, JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[1]);
            if (sel == 0) {
                imprimirDirecto(pdf, datos.nombreImpresora);
            } else if (sel == 1) {
                abrirEnVisor(pdf);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error al generar el PDF de la orden:\n" + ex.getMessage(),
                    "PDF", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== Generación ====================
    private void generarPDF(DatosOrden d, File archivo) throws Exception {
        Document doc = new Document(PageSize.LETTER, 28, 28, 24, 28);
        PdfWriter.getInstance(doc, new FileOutputStream(archivo));
        doc.open();

        agregarEncabezado(doc, d);
        agregarInfo(doc, d);
        agregarTabla(doc, d);
        agregarTotal(doc, d);

        doc.close();
    }

    private void agregarEncabezado(Document doc, DatosOrden d) throws Exception {
        PdfPTable outer = new PdfPTable(new float[]{60f, 40f});
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(8f);
        outer.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Izquierda: negocio
        PdfPCell cInfo = new PdfPCell();
        cInfo.setBorder(Rectangle.NO_BORDER);
        cInfo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Image logo = cargarLogo();
        if (logo != null) {
            logo.scaleToFit(170f, 60f);
            cInfo.addElement(logo);
        }
        cInfo.addElement(new Phrase(nz(d.nombreNegocio, "BODEGA"), fBrandTitle));
        if (noVacio(d.direccion)) {
            cInfo.addElement(new Phrase(d.direccion, fValue));
        }
        if (noVacio(d.contactoNegocio)) {
            cInfo.addElement(new Phrase("Tel: " + d.contactoNegocio, fLabel));
        }
        outer.addCell(cInfo);

        // Derecha: caja "ORDEN DE COMPRA"
        PdfPCell cBox = new PdfPCell();
        cBox.setBorder(Rectangle.BOX);
        cBox.setBorderColor(BORDER_GRAY);
        cBox.setPadding(8f);
        cBox.setBackgroundColor(BRAND_SOFT);
        cBox.addElement(alinear(new Phrase("ORDEN DE COMPRA", fLabel), Element.ALIGN_RIGHT));
        cBox.addElement(alinear(new Phrase(nz(d.numero, "-"), fValueBig), Element.ALIGN_RIGHT));
        cBox.addElement(alinear(new Phrase("Fecha: " + formatearFecha(d.fecha, d.hora), fValue), Element.ALIGN_RIGHT));
        cBox.addElement(alinear(new Phrase("Estado: "
                + Ordenes_compra_cabecera.nombreEstado(d.estado).toUpperCase(Locale.ROOT), fValue),
                Element.ALIGN_RIGHT));
        outer.addCell(cBox);

        doc.add(outer);
    }

    private void agregarInfo(Document doc, DatosOrden d) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{25f, 25f, 25f, 25f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(6f);

        t.addCell(par("Bodega", nz(d.bodega, "-")));
        t.addCell(par("Creada por", nz(d.creador, "-")));
        t.addCell(par("Aprobada por", nz(d.aprobador, "-")));
        t.addCell(par("Fecha aprobación", nz(d.fechaAprobacion, "-")));

        PdfPCell obs = new PdfPCell();
        obs.setColspan(4);
        obs.setBorder(Rectangle.NO_BORDER);
        obs.setPaddingTop(2f);
        Phrase p = new Phrase();
        p.add(new com.lowagie.text.Chunk("Observación: ", fLabel));
        p.add(new com.lowagie.text.Chunk(nz(d.observacion, "—"), fValue));
        obs.addElement(p);
        t.addCell(obs);

        doc.add(t);
    }

    private void agregarTabla(Document doc, DatosOrden d) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{12f, 38f, 10f, 22f, 9f, 9f});
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        t.setSpacingBefore(2f);

        t.addCell(head("CÓDIGO", Element.ALIGN_LEFT));
        t.addCell(head("DESCRIPCIÓN", Element.ALIGN_LEFT));
        t.addCell(head("CANT.", Element.ALIGN_RIGHT));
        t.addCell(head("PROVEEDOR", Element.ALIGN_LEFT));
        t.addCell(head("P. UNIT", Element.ALIGN_RIGHT));
        t.addCell(head("SUBTOTAL", Element.ALIGN_RIGHT));

        DecimalFormat cant = new DecimalFormat("###,###,##0.##");
        int i = 0;
        for (ItemOrden it : d.items) {
            boolean alt = (i++ % 2 == 1);
            t.addCell(body(nz(it.codigo, ""), Element.ALIGN_LEFT, alt));
            t.addCell(body(nz(it.descripcion, ""), Element.ALIGN_LEFT, alt));
            t.addCell(body(cant.format(it.cantidad), Element.ALIGN_RIGHT, alt));
            t.addCell(body(nz(it.proveedor, "—"), Element.ALIGN_LEFT, alt));
            t.addCell(body("$ " + money.format(it.precio), Element.ALIGN_RIGHT, alt));
            t.addCell(body("$ " + money.format(it.cantidad * it.precio), Element.ALIGN_RIGHT, alt));
        }
        doc.add(t);
    }

    private void agregarTotal(Document doc, DatosOrden d) throws Exception {
        double total = 0;
        for (ItemOrden it : d.items) {
            total += it.cantidad * it.precio;
        }
        PdfPTable t = new PdfPTable(new float[]{70f, 30f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(6f);
        t.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        t.addCell(vacia());

        PdfPCell cTotal = new PdfPCell();
        cTotal.setBackgroundColor(TABLE_HEAD);
        cTotal.setBorder(Rectangle.BOX);
        cTotal.setBorderColor(BRAND_DARK);
        cTotal.setPadding(8f);
        Phrase ph = new Phrase();
        ph.add(new com.lowagie.text.Chunk("TOTAL ESTIMADO:   ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        ph.add(new com.lowagie.text.Chunk("$ " + money.format(total), fTotal));
        cTotal.addElement(alinear(ph, Element.ALIGN_RIGHT));
        t.addCell(cTotal);

        doc.add(t);
    }

    // ==================== Celdas ====================
    private PdfPCell par(String etiqueta, String valor) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(BORDER_GRAY);
        c.setPadding(5f);
        c.addElement(new Phrase(etiqueta.toUpperCase(Locale.ROOT), fLabelSmall));
        c.addElement(new Phrase(valor, fValue));
        return c;
    }

    private PdfPCell head(String texto, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fItemHeader));
        c.setBackgroundColor(TABLE_HEAD);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5f);
        return c;
    }

    private PdfPCell body(String texto, int align, boolean alt) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fItemBody));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BORDER_GRAY);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4f);
        if (alt) {
            c.setBackgroundColor(ROW_ALT);
        }
        return c;
    }

    private PdfPCell vacia() {
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private com.lowagie.text.Paragraph alinear(Phrase p, int align) {
        com.lowagie.text.Paragraph par = new com.lowagie.text.Paragraph(p);
        par.setAlignment(align);
        return par;
    }

    // ==================== Impresión / visor ====================
    private void imprimirDirecto(File pdf, String nombreImpresora) throws Exception {
        PrintService destino = buscarImpresora(nombreImpresora);
        if (destino == null) {
            abrirEnVisor(pdf);
            return;
        }
        try (PDDocument documento = PDDocument.load(pdf)) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(destino);
            job.setJobName("Orden compra " + pdf.getName());
            job.setPageable(new PDFPageable(documento));
            job.print();
        } catch (Exception ex) {
            abrirEnVisor(pdf);
        }
    }

    private PrintService buscarImpresora(String nombre) {
        if (!noVacio(nombre)) {
            return null;
        }
        String objetivo = nombre.trim();
        for (PrintService ps : PrinterJob.lookupPrintServices()) {
            if (ps.getName().equalsIgnoreCase(objetivo)) {
                return ps;
            }
        }
        return null;
    }

    private void abrirEnVisor(File pdf) throws Exception {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(pdf);
            return;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", pdf.getAbsolutePath()).start();
        } else if (os.contains("mac")) {
            new ProcessBuilder("open", pdf.getAbsolutePath()).start();
        } else {
            new ProcessBuilder("xdg-open", pdf.getAbsolutePath()).start();
        }
    }

    private Image cargarLogo() {
        String[] candidatos = {"logo.png", "logo.jpg", "banner 1.jpg"};
        String raiz = new File("").getAbsolutePath();
        for (String nombre : candidatos) {
            File f = new File(raiz, nombre);
            if (f.exists()) {
                try {
                    return Image.getInstance(f.getAbsolutePath());
                } catch (Exception ignore) {
                }
            }
        }
        return null;
    }

    // ==================== Datos ====================
    private DatosOrden cargarDatos(Connection cn, int idOrden) throws Exception {
        DatosOrden d = new DatosOrden();
        String sqlCab
                = "SELECT c.nombre_negocio, c.direccion, c.contacto_negocio, c.nombre_impresora, "
                + "       oc.numero, oc.fecha, oc.hora, oc.estado, oc.observacion, oc.fecha_aprobacion, "
                + "       COALESCE(uc.nombre,'') AS creador, COALESCE(ua.nombre,'') AS aprobador, "
                + "       COALESCE(b.nombre,'') AS bodega "
                + "FROM ordenes_compra_cabecera oc "
                + "LEFT JOIN users uc ON uc.id = oc.id_user_crea "
                + "LEFT JOIN users ua ON ua.id = oc.id_user_aprueba "
                + "LEFT JOIN bodegas b ON b.id = oc.id_bodega "
                + "CROSS JOIN configuraciones c "
                + "WHERE oc.id = ?";
        try (PreparedStatement ps = cn.prepareStatement(sqlCab)) {
            ps.setInt(1, idOrden);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                d.nombreNegocio = rs.getString("nombre_negocio");
                d.direccion = rs.getString("direccion");
                d.contactoNegocio = rs.getString("contacto_negocio");
                d.nombreImpresora = rs.getString("nombre_impresora");
                d.numero = rs.getString("numero");
                d.fecha = rs.getDate("fecha");
                d.hora = rs.getString("hora");
                d.estado = rs.getInt("estado");
                d.observacion = rs.getString("observacion");
                java.sql.Timestamp fa = rs.getTimestamp("fecha_aprobacion");
                d.fechaAprobacion = fa == null ? null
                        : new SimpleDateFormat("dd/MM/yyyy HH:mm").format(fa);
                d.creador = rs.getString("creador");
                d.aprobador = rs.getString("aprobador");
                d.bodega = rs.getString("bodega");
            }
        }

        String sqlDet
                = "SELECT p.codigo_barras, p.descripcion, d.cantidad, "
                + "       COALESCE(ct.nombre,'') AS proveedor, d.precio_unitario "
                + "FROM ordenes_compra_detalle d "
                + "JOIN productos p ON p.id = d.id_producto "
                + "LEFT JOIN contactos ct ON ct.id = d.id_proveedor "
                + "WHERE d.id_orden_cabecera = ? ORDER BY d.id";
        d.items = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sqlDet)) {
            ps.setInt(1, idOrden);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemOrden it = new ItemOrden();
                    it.codigo = rs.getString("codigo_barras");
                    it.descripcion = rs.getString("descripcion");
                    it.cantidad = rs.getDouble("cantidad");
                    it.proveedor = rs.getString("proveedor");
                    it.precio = rs.getDouble("precio_unitario");
                    d.items.add(it);
                }
            }
        }
        return d;
    }

    // ==================== Helpers ====================
    private static boolean noVacio(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String nz(String s, String def) {
        return s == null ? def : s;
    }

    private static String formatearFecha(java.sql.Date fecha, String hora) {
        if (fecha == null) {
            return "-";
        }
        String f = new SimpleDateFormat("dd/MM/yyyy").format(fecha);
        if (hora != null && hora.length() >= 5) {
            f += " " + hora.substring(0, 5);
        }
        return f;
    }

    // ==================== Modelos internos ====================
    private static class DatosOrden {
        String nombreNegocio, direccion, contactoNegocio, nombreImpresora;
        String numero, hora, observacion, creador, aprobador, bodega, fechaAprobacion;
        java.sql.Date fecha;
        int estado;
        List<ItemOrden> items;
    }

    private static class ItemOrden {
        String codigo, descripcion, proveedor;
        double cantidad, precio;
    }
}
