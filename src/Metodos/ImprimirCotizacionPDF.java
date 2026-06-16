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
import javax.swing.JOptionPane;

/**
 * Genera en PDF (OpenPDF) la Solicitud de Cotización / RFQ (RF-03): encabezado
 * de la empresa, consecutivo y fecha, datos del proveedor, detalle por producto
 * con espacios para que el proveedor diligencie precio, IVA y plazo, y un pie
 * con condiciones de pago y validez.
 *
 * @author Monkeyelgrande
 */
public class ImprimirCotizacionPDF {

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
    private final Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private final Font fBody = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

    private final DecimalFormat cant = new DecimalFormat("###,###,##0.##");

    public void imprimir(int idCotiz) {
        try {
            Datos d;
            try (Connection cn = DB_consultas_R_D.getConexion()) {
                d = cargar(cn, idCotiz);
            }
            if (d == null) {
                JOptionPane.showMessageDialog(null, "No se encontró la cotización #" + idCotiz);
                return;
            }
            File pdf = File.createTempFile("rfq_" + idCotiz + "_", ".pdf");
            pdf.deleteOnExit();
            generar(d, pdf);
            abrir(pdf);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al generar el PDF de la cotización:\n" + ex.getMessage(),
                    "PDF", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generar(Datos d, File archivo) throws Exception {
        Document doc = new Document(PageSize.LETTER, 28, 28, 24, 28);
        PdfWriter.getInstance(doc, new FileOutputStream(archivo));
        doc.open();
        encabezado(doc, d);
        barraProveedor(doc, d);
        tabla(doc, d);
        pie(doc, d);
        doc.close();
    }

    private void encabezado(Document doc, Datos d) throws Exception {
        PdfPTable outer = new PdfPTable(new float[]{60f, 40f});
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(8f);
        outer.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell cInfo = new PdfPCell();
        cInfo.setBorder(Rectangle.NO_BORDER);
        Image logo = cargarLogo();
        if (logo != null) {
            logo.scaleToFit(170f, 60f);
            cInfo.addElement(logo);
        }
        cInfo.addElement(new Phrase(nz(d.negocio, "BODEGA"), fBrandTitle));
        if (noVacio(d.nit)) {
            cInfo.addElement(new Phrase("NIT: " + d.nit, fValue));
        }
        if (noVacio(d.direccion)) {
            cInfo.addElement(new Phrase(d.direccion, fValue));
        }
        if (noVacio(d.telNegocio)) {
            cInfo.addElement(new Phrase("Tel: " + d.telNegocio, fLabel));
        }
        outer.addCell(cInfo);

        PdfPCell cBox = new PdfPCell();
        cBox.setBorder(Rectangle.BOX);
        cBox.setBorderColor(BORDER_GRAY);
        cBox.setBackgroundColor(BRAND_SOFT);
        cBox.setPadding(8f);
        cBox.addElement(alinear(new Phrase("SOLICITUD DE COTIZACIÓN", fLabel), Element.ALIGN_RIGHT));
        cBox.addElement(alinear(new Phrase(nz(d.numero, "-"), fValueBig), Element.ALIGN_RIGHT));
        cBox.addElement(alinear(new Phrase("Fecha: " + fecha(d.fecha), fValue), Element.ALIGN_RIGHT));
        if (noVacio(d.fechaLimite)) {
            cBox.addElement(alinear(new Phrase("Responder antes de: " + d.fechaLimite, fValue), Element.ALIGN_RIGHT));
        }
        outer.addCell(cBox);
        doc.add(outer);
    }

    private void barraProveedor(Document doc, Datos d) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{18f, 52f, 12f, 18f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(6f);
        t.addCell(celda("Proveedor:", fLabel, false));
        t.addCell(celda(nz(d.proveedor, "-"), fValue, false));
        t.addCell(celda("Celular:", fLabel, false));
        t.addCell(celda(nz(d.celular, "-"), fValue, false));
        doc.add(t);
    }

    private void tabla(Document doc, Datos d) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{12f, 38f, 10f, 10f, 12f, 8f, 10f});
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        t.addCell(head("CÓDIGO", Element.ALIGN_LEFT));
        t.addCell(head("DESCRIPCIÓN", Element.ALIGN_LEFT));
        t.addCell(head("UNIDAD", Element.ALIGN_LEFT));
        t.addCell(head("CANT.", Element.ALIGN_RIGHT));
        t.addCell(head("PRECIO UNIT", Element.ALIGN_RIGHT));
        t.addCell(head("IVA %", Element.ALIGN_RIGHT));
        t.addCell(head("PLAZO", Element.ALIGN_LEFT));

        int i = 0;
        for (Item it : d.items) {
            boolean alt = (i++ % 2 == 1);
            t.addCell(body(nz(it.codigo, ""), Element.ALIGN_LEFT, alt));
            t.addCell(body(nz(it.descripcion, ""), Element.ALIGN_LEFT, alt));
            t.addCell(body(nz(it.unidad, ""), Element.ALIGN_LEFT, alt));
            t.addCell(body(cant.format(it.cantidad), Element.ALIGN_RIGHT, alt));
            t.addCell(body(it.precio == null ? "" : cant.format(it.precio), Element.ALIGN_RIGHT, alt));
            t.addCell(body(it.iva == null ? "" : cant.format(it.iva * 100), Element.ALIGN_RIGHT, alt));
            t.addCell(body(nz(it.plazo, ""), Element.ALIGN_LEFT, alt));
        }
        doc.add(t);
    }

    private void pie(Document doc, Datos d) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(10f);
        t.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        t.addCell(new Phrase("Condición de pago: " + nz(d.condicion, "____________________________"), fBody));
        t.addCell(new Phrase("Validez de la oferta: " + nz(d.validez, "____________________________"), fBody));
        t.addCell(new Phrase("Observaciones: " + nz(d.observacion, ""), fBody));
        t.addCell(new Phrase(" ", fBody));
        t.addCell(new Phrase("Diligenciado por (proveedor): ____________________________   Firma: ________________", fBody));
        doc.add(t);
    }

    // ---- celdas ----
    private PdfPCell celda(String texto, Font f, boolean box) {
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setBorder(box ? Rectangle.BOX : Rectangle.NO_BORDER);
        c.setBorderColor(BORDER_GRAY);
        c.setPadding(3f);
        return c;
    }

    private PdfPCell head(String t, int align) {
        PdfPCell c = new PdfPCell(new Phrase(t, fHead));
        c.setBackgroundColor(TABLE_HEAD);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPadding(5f);
        return c;
    }

    private PdfPCell body(String t, int align, boolean alt) {
        PdfPCell c = new PdfPCell(new Phrase(t, fBody));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BORDER_GRAY);
        c.setHorizontalAlignment(align);
        c.setPadding(4f);
        c.setMinimumHeight(16f);
        if (alt) {
            c.setBackgroundColor(ROW_ALT);
        }
        return c;
    }

    private com.lowagie.text.Paragraph alinear(Phrase p, int align) {
        com.lowagie.text.Paragraph par = new com.lowagie.text.Paragraph(p);
        par.setAlignment(align);
        return par;
    }

    private void abrir(File pdf) throws Exception {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(pdf);
            return;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", pdf.getAbsolutePath()).start();
        } else {
            new ProcessBuilder("xdg-open", pdf.getAbsolutePath()).start();
        }
    }

    private Image cargarLogo() {
        String[] cand = {"logo.png", "logo.jpg", "banner 1.jpg"};
        String raiz = new File("").getAbsolutePath();
        for (String n : cand) {
            File f = new File(raiz, n);
            if (f.exists()) {
                try {
                    return Image.getInstance(f.getAbsolutePath());
                } catch (Exception ignore) {
                }
            }
        }
        return null;
    }

    private Datos cargar(Connection cn, int id) throws Exception {
        Datos d = new Datos();
        String sql = "SELECT c.nombre_negocio, c.nit_negocio, c.direccion, c.contacto_negocio, "
                + "co.numero, co.fecha, co.fecha_limite, co.condicion_pago, co.validez, co.observacion, "
                + "COALESCE(ct.nombre,'') AS proveedor, COALESCE(ct.contacto,'') AS celular "
                + "FROM cotizaciones_compra_cabecera co "
                + "LEFT JOIN contactos ct ON ct.id=co.id_proveedor "
                + "CROSS JOIN configuraciones c WHERE co.id=?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                d.negocio = rs.getString("nombre_negocio");
                d.nit = rs.getString("nit_negocio");
                d.direccion = rs.getString("direccion");
                d.telNegocio = rs.getString("contacto_negocio");
                d.numero = rs.getString("numero");
                d.fecha = rs.getDate("fecha");
                java.sql.Date fl = rs.getDate("fecha_limite");
                d.fechaLimite = fl == null ? null : fl.toString();
                d.condicion = rs.getString("condicion_pago");
                d.validez = rs.getString("validez");
                d.observacion = rs.getString("observacion");
                d.proveedor = rs.getString("proveedor");
                d.celular = rs.getString("celular");
            }
        }
        String sqlD = "SELECT p.codigo_barras, p.descripcion, COALESCE(u.nombre,'') AS unidad, "
                + "d.cantidad, d.precio_unitario, d.iva_pct, COALESCE(d.plazo_entrega,'') AS plazo "
                + "FROM cotizaciones_compra_detalle d JOIN productos p ON p.id=d.id_producto "
                + "LEFT JOIN unidades_medidas u ON u.id=p.id_unidad "
                + "WHERE d.id_cotiz_cab=? ORDER BY d.id";
        d.items = new ArrayList<>();
        try (PreparedStatement ps = cn.prepareStatement(sqlD)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Item it = new Item();
                    it.codigo = rs.getString("codigo_barras");
                    it.descripcion = rs.getString("descripcion");
                    it.unidad = rs.getString("unidad");
                    it.cantidad = rs.getDouble("cantidad");
                    it.precio = rs.getObject("precio_unitario") == null ? null : rs.getDouble("precio_unitario");
                    it.iva = rs.getObject("iva_pct") == null ? null : rs.getDouble("iva_pct");
                    it.plazo = rs.getString("plazo");
                    d.items.add(it);
                }
            }
        }
        return d;
    }

    private static boolean noVacio(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String nz(String s, String def) {
        return s == null ? def : s;
    }

    private static String fecha(java.sql.Date f) {
        return f == null ? "-" : new SimpleDateFormat("dd/MM/yyyy").format(f);
    }

    private static class Datos {
        String negocio, nit, direccion, telNegocio;
        String numero, fechaLimite, condicion, validez, observacion, proveedor, celular;
        java.sql.Date fecha;
        List<Item> items;
    }

    private static class Item {
        String codigo, descripcion, unidad, plazo;
        double cantidad;
        Double precio, iva;
    }
}
