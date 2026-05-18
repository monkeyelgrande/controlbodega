package Metodos;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import conexiondb.DB_consultas_R_D;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Genera el recibo/orden de salida en PDF con iText (OpenPDF 2.1.7) y ZXing
 * para el QR. Reemplaza al reporte Jasper ImprimirFactura_MC manteniendo todos
 * los campos: encabezado con datos del negocio, ID orden, numero de factura,
 * fecha, cliente, tabla de productos, observacion, total de articulos,
 * pie legal y USER.
 */
public class ImprimirFacturaPDF {

    // ==================== Paleta de marca ====================
    private static final Color BRAND_DARK   = new Color(20, 45, 85);
    private static final Color BRAND_ACCENT = new Color(212, 175, 55);   // acento dorado sutil
    private static final Color HEADER_SOFT  = new Color(238, 242, 248);
    private static final Color TABLE_HEAD   = new Color(33, 55, 92);
    private static final Color ROW_ALT      = new Color(246, 248, 251);
    private static final Color BORDER_GRAY  = new Color(210, 214, 220);
    private static final Color TEXT_MUTED   = new Color(105, 110, 120);

    // ==================== Tipografias ====================
    private final Font fBrandTitle     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, BRAND_DARK);
    private final Font fSubtitle       = FontFactory.getFont(FontFactory.HELVETICA,       8, TEXT_MUTED);
    private final Font fLabelSmall     = FontFactory.getFont(FontFactory.HELVETICA,       6, TEXT_MUTED);
    private final Font fLabel          = FontFactory.getFont(FontFactory.HELVETICA,       7, TEXT_MUTED);
    private final Font fValue          = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  8, Color.BLACK);
    private final Font fValueBig       = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private final Font fValueMid       = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BRAND_DARK);
    private final Font fTableHeader    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  7, Color.WHITE);
    // Fuentes dedicadas para la tabla de productos (codigo, descripcion, unidad, cantidad)
    private final Font fItemHeader     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  9, Color.WHITE);
    private final Font fItemBody       = FontFactory.getFont(FontFactory.HELVETICA,      10, Color.BLACK);
    private final Font fBody           = FontFactory.getFont(FontFactory.HELVETICA,       8, Color.BLACK);
    private final Font fBodyBold       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  8, Color.BLACK);
    private final Font fFooter         = FontFactory.getFont(FontFactory.HELVETICA,       6, TEXT_MUTED);
    private final Font fFooterBold     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  6, BRAND_DARK);
    private final Font fTipo           = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  9, BRAND_ACCENT.darker());

    // ==================== API publica ====================

    /**
     * Genera el PDF en un archivo temporal y muestra un dialogo con tres
     * opciones:
     *   - Imprimir: envia el PDF directo a la impresora configurada en
     *     {@code configuraciones.nombre_impresora}. Si no esta configurada o
     *     no se encuentra en el sistema, abre el archivo como fallback.
     *   - Ver: abre el PDF en el visor predeterminado del sistema.
     *   - Cancelar: no hace nada.
     */
    public void imprimir(int idFactura) {
        try {
            DatosFactura datos;
            try (Connection cn = DB_consultas_R_D.getConexion()) {
                datos = cargarDatos(cn, idFactura);
            }
            File pdf = File.createTempFile("orden_" + idFactura + "_", ".pdf");
            pdf.deleteOnExit();
            generarPDF(datos, pdf);

            int opcion = mostrarDialogoOpciones(idFactura);
            switch (opcion) {
                case 0: // Imprimir
                    imprimirDirecto(pdf, datos.nombreImpresora);
                    break;
                case 1: // Ver
                    abrirEnVisor(pdf);
                    break;
                default: // Cancelar / cerrar dialogo
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error al generar la factura:\n" + ex.getMessage(),
                    "Impresion", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Alias de {@link #imprimir(int)} conservado por compatibilidad. */
    public void vistaPrevia(int idFactura) {
        imprimir(idFactura);
    }

    /**
     * Muestra el dialogo con tres botones: Imprimir, Ver, Cancelar.
     * @return 0 = Imprimir, 1 = Ver, 2 = Cancelar (o cerrar dialogo).
     */
    private int mostrarDialogoOpciones(int idFactura) {
        Object[] opciones = { "Imprimir", "Ver", "Cancelar" };
        int sel = JOptionPane.showOptionDialog(null,
                "Que deseas hacer con la orden?",
                "Orden #" + idFactura,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]);
        return sel < 0 ? 2 : sel;
    }

    /**
     * Imprime el PDF directamente en la impresora indicada, sin abrir visor.
     * Usa Apache PDFBox para rasterizar y java.awt.print.PrinterJob para
     * enviar al servicio de impresion. Si la impresora no esta configurada
     * o no se encuentra en el sistema, abre el PDF como fallback.
     */
    private void imprimirDirecto(File pdf, String nombreImpresora) throws Exception {
        if (!noVacio(nombreImpresora)) {
            JOptionPane.showMessageDialog(null,
                    "No hay impresora configurada en Configuraciones.\nSe abrira el archivo.",
                    "Impresion", JOptionPane.WARNING_MESSAGE);
            abrirEnVisor(pdf);
            return;
        }

        PrintService destino = buscarImpresora(nombreImpresora);
        if (destino == null) {
            JOptionPane.showMessageDialog(null,
                    "No se encontro la impresora '" + nombreImpresora
                            + "' en el sistema.\nSe abrira el archivo.",
                    "Impresion", JOptionPane.WARNING_MESSAGE);
            abrirEnVisor(pdf);
            return;
        }

        try (PDDocument documento = PDDocument.load(pdf)) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(destino);
            job.setJobName("Orden " + pdf.getName());
            job.setPageable(new PDFPageable(documento));
            job.print();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Fallo la impresion directa:\n" + ex.getMessage()
                            + "\nSe abrira el archivo.",
                    "Impresion", JOptionPane.ERROR_MESSAGE);
            abrirEnVisor(pdf);
        }
    }

    /** Busca un PrintService cuyo nombre coincida (case-insensitive). */
    private PrintService buscarImpresora(String nombre) {
        if (!noVacio(nombre)) return null;
        String objetivo = nombre.trim();
        for (PrintService ps : PrinterJob.lookupPrintServices()) {
            if (ps.getName().equalsIgnoreCase(objetivo)) {
                return ps;
            }
        }
        // Segundo intento: coincidencia parcial (algunos drivers cuelgan sufijos
        // tipo "Canon TS3100 series (Copia 1)").
        for (PrintService ps : PrinterJob.lookupPrintServices()) {
            if (ps.getName().toLowerCase(Locale.ROOT)
                    .contains(objetivo.toLowerCase(Locale.ROOT))) {
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
        // Fallback: intentar con el shell del SO
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", pdf.getAbsolutePath()).start();
        } else if (os.contains("mac")) {
            new ProcessBuilder("open", pdf.getAbsolutePath()).start();
        } else {
            new ProcessBuilder("xdg-open", pdf.getAbsolutePath()).start();
        }
    }

    // ==================== Generacion del PDF ====================

    // Altura util de la "media carta" dentro de una hoja carta.
    // Letter = 612x792 pt; queremos que el diseno solo ocupe los 396 pt superiores.
    private static final float MEDIA_CARTA_ALTO = 396f;

    private void generarPDF(DatosFactura d, File archivo) throws Exception {
        Rectangle pageSize = PageSize.LETTER;
        float bottomMargin = pageSize.getHeight() - MEDIA_CARTA_ALTO + 20f;
        Document doc = new Document(pageSize, 20, 20, 14, bottomMargin);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(archivo));

        PageFooterEvent footer = new PageFooterEvent(d);
        writer.setPageEvent(footer);

        doc.open();

        agregarEncabezado(doc, d);
        agregarBarraCliente(doc, d);
        agregarTablaProductos(doc, d);
        agregarResumenFinal(doc, d);

        doc.close();
    }

    // -------- Encabezado (logo + datos negocio + ID orden/factura/fecha + QR) --------
    private void agregarEncabezado(Document doc, DatosFactura d) throws Exception {
        PdfPTable outer = new PdfPTable(new float[]{ 42f, 33f, 25f });
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(4f);
        outer.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // ---- Columna 1: Logo (logo.png en raiz del proyecto) ----
        PdfPCell cLogo = new PdfPCell();
        cLogo.setBorder(Rectangle.BOX);
        cLogo.setBorderColor(BORDER_GRAY);
        cLogo.setPadding(4f);
        cLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cLogo.setHorizontalAlignment(Element.ALIGN_CENTER);
        Image logo = cargarLogo();
        if (logo != null) {
            logo.scaleToFit(170f, 60f);
            logo.setAlignment(Image.ALIGN_CENTER);
            cLogo.addElement(logo);
        } else {
            cLogo.addElement(new Phrase(d.nombreNegocio == null ? "BODEGA" : d.nombreNegocio, fBrandTitle));
        }
        outer.addCell(cLogo);

        // ---- Columna 2: titulo + datos de contacto ----
        PdfPCell cInfo = new PdfPCell();
        cInfo.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cInfo.setBorderColor(BORDER_GRAY);
        cInfo.setPadding(6f);
        cInfo.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPTable tInfo = new PdfPTable(1);
        tInfo.setWidthPercentage(100);
        tInfo.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        tInfo.addCell(celdaSimple("BODEGA", fBrandTitle, Element.ALIGN_LEFT));
        if (noVacio(d.direccion)) {
            tInfo.addCell(celdaSimple(d.direccion, fValue, Element.ALIGN_LEFT));
        }
        if (noVacio(d.contactoNegocio)) {
            tInfo.addCell(celdaLabelValor("Tel. bodega:", d.contactoNegocio));
        }
        if (noVacio(d.contacto2Negocio)) {
            tInfo.addCell(celdaLabelValor("Tel. local: ", d.contacto2Negocio));
        }
        cInfo.addElement(tInfo);
        outer.addCell(cInfo);

        // ---- Columna 3: ID orden, # factura, fecha y QR ----
        PdfPCell cMeta = new PdfPCell();
        cMeta.setBorder(Rectangle.BOX);
        cMeta.setBorderColor(BORDER_GRAY);
        cMeta.setPadding(0f);

        PdfPTable tMeta = new PdfPTable(new float[]{ 65f, 35f });
        tMeta.setWidthPercentage(100);
        tMeta.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Fila 1: ID orden (izq) + QR (der, rowspan 3)
        PdfPCell cIdOrden = new PdfPCell();
        cIdOrden.setBorder(Rectangle.BOTTOM);
        cIdOrden.setBorderColor(BORDER_GRAY);
        cIdOrden.setPadding(2f);
        cIdOrden.setBackgroundColor(HEADER_SOFT);
        cIdOrden.addElement(new Phrase("ID orden", fLabelSmall));
        cIdOrden.addElement(new Phrase(String.valueOf(d.idFactura), fValueBig));
        tMeta.addCell(cIdOrden);

        PdfPCell cQr = new PdfPCell();
        cQr.setBorder(Rectangle.BOTTOM);
        cQr.setBorderColor(BORDER_GRAY);
        cQr.setPadding(2f);
        cQr.setHorizontalAlignment(Element.ALIGN_CENTER);
        cQr.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cQr.setRowspan(3);
        Image qr = generarQR("ORDEN-" + d.idFactura, 220);
        if (qr != null) {
            qr.scaleToFit(55f, 55f);
            qr.setAlignment(Image.ALIGN_CENTER);
            cQr.addElement(qr);
        }
        tMeta.addCell(cQr);

        // Fila 2: # Factura
        PdfPCell cFact = new PdfPCell();
        cFact.setBorder(Rectangle.BOTTOM);
        cFact.setBorderColor(BORDER_GRAY);
        cFact.setPadding(2f);
        cFact.addElement(new Phrase("# FACTURA", fLabelSmall));
        cFact.addElement(new Phrase(d.codigoFactura == null ? "-" : d.codigoFactura, fValueMid));
        tMeta.addCell(cFact);

        // Fila 3: Fecha y hora de creacion de la orden
        PdfPCell cFecha = new PdfPCell();
        cFecha.setBorder(Rectangle.NO_BORDER);
        cFecha.setPadding(2f);
        cFecha.addElement(new Phrase("FECHA CREACION", fLabelSmall));
        cFecha.addElement(new Phrase(formatearFechaYHora(d.fechaFactura, d.horaFactura), fValue));
        tMeta.addCell(cFecha);

        cMeta.addElement(tMeta);
        outer.addCell(cMeta);

        doc.add(outer);
    }

    // -------- Barra cliente --------
    private void agregarBarraCliente(Document doc, DatosFactura d) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{ 18f, 42f, 15f, 15f, 10f });
        t.setWidthPercentage(100);
        t.setSpacingAfter(4f);

        t.addCell(celdaCliente("Nombre Cliente:", fLabel));
        t.addCell(celdaCliente(nz(d.nombreCliente, "-"), fBodyBold));
        t.addCell(celdaCliente("CC/NIT:", fLabel));
        t.addCell(celdaCliente(nz(d.cedulaCliente, "-"), fBodyBold));
        t.addCell(celdaClienteTipo(nz(d.tipoFactura, "")));

        doc.add(t);
    }

    // -------- Tabla de productos --------
    private void agregarTablaProductos(Document doc, DatosFactura d) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{ 15f, 55f, 15f, 15f });
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        t.setSpacingAfter(2f);

        t.addCell(celdaHead("CODIGO",      Element.ALIGN_LEFT));
        t.addCell(celdaHead("DESCRIPCION", Element.ALIGN_LEFT));
        t.addCell(celdaHead("UNIDAD",      Element.ALIGN_LEFT));
        t.addCell(celdaHead("CANT.",       Element.ALIGN_RIGHT));

        DecimalFormat df = new DecimalFormat("###,###,##0.##");
        int i = 0;
        for (ItemFactura it : d.items) {
            boolean alt = (i++ % 2 == 1);
            t.addCell(celdaBody(nz(it.codigo, ""),       Element.ALIGN_LEFT,  alt));
            t.addCell(celdaBody(nz(it.descripcion, ""),  Element.ALIGN_LEFT,  alt));
            t.addCell(celdaBody(nz(it.unidad, ""),       Element.ALIGN_LEFT,  alt));
            t.addCell(celdaBody(df.format(it.cantidad),  Element.ALIGN_RIGHT, alt));
        }
        doc.add(t);
    }

    // -------- Resumen final (firma + observacion + total + pie legal) --------
    private void agregarResumenFinal(Document doc, DatosFactura d) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{ 50f, 28f, 22f });
        t.setWidthPercentage(100);
        t.setSpacingBefore(6f);

        // Firma + observacion
        PdfPCell cFirma = new PdfPCell();
        cFirma.setBorder(Rectangle.NO_BORDER);
        cFirma.setPaddingTop(14f);
        PdfPTable tFirma = new PdfPTable(1);
        tFirma.setWidthPercentage(100);
        tFirma.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell linea = new PdfPCell(new Phrase(" ", fBody));
        linea.setBorder(Rectangle.BOTTOM);
        linea.setBorderColor(Color.BLACK);
        linea.setFixedHeight(10f);
        tFirma.addCell(linea);
        tFirma.addCell(celdaSimple("Firma Cliente: " + nz(d.nombreCliente, ""), fFooterBold, Element.ALIGN_LEFT));

        PdfPCell obs = new PdfPCell();
        obs.setBorder(Rectangle.NO_BORDER);
        obs.setPaddingTop(3f);
        obs.addElement(new Phrase("Observacion:", fBodyBold));
        obs.addElement(new Phrase(nz(d.observacion, ""), fBody));
        tFirma.addCell(obs);

        cFirma.addElement(tFirma);
        t.addCell(cFirma);

        // Pie legal
        PdfPCell cPie = new PdfPCell(new Phrase(nz(d.pieLegal, ""), fBody));
        cPie.setBorder(Rectangle.NO_BORDER);
        cPie.setPaddingTop(6f);
        cPie.setVerticalAlignment(Element.ALIGN_TOP);
        t.addCell(cPie);

        // Total articulos + fecha creacion
        PdfPCell cTotal = new PdfPCell();
        cTotal.setBorder(Rectangle.BOX);
        cTotal.setBorderColor(BRAND_DARK);
        cTotal.setBackgroundColor(HEADER_SOFT);
        cTotal.setPadding(4f);
        cTotal.setHorizontalAlignment(Element.ALIGN_CENTER);
        cTotal.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cTotal.addElement(alinear(new Phrase("Total de articulos", fLabel), Element.ALIGN_CENTER));
        DecimalFormat dfTot = new DecimalFormat("###,###,##0");
        cTotal.addElement(alinear(new Phrase(dfTot.format(d.totalArticulos), fValueBig), Element.ALIGN_CENTER));
        t.addCell(cTotal);

        doc.add(t);
    }

    // ==================== Utilitarios de celda ====================

    private PdfPCell celdaSimple(String texto, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPadding(1f);
        return c;
    }

    private PdfPCell celdaLabelValor(String etiqueta, String valor) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(1f);
        Phrase p = new Phrase();
        p.add(new com.lowagie.text.Chunk(etiqueta + " ", fLabel));
        p.add(new com.lowagie.text.Chunk(valor, fValue));
        c.addElement(p);
        return c;
    }

    private PdfPCell celdaCliente(String texto, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(BORDER_GRAY);
        c.setPadding(6f);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(HEADER_SOFT);
        return c;
    }

    private PdfPCell celdaClienteTipo(String tipo) {
        PdfPCell c = new PdfPCell(new Phrase(tipo.toUpperCase(Locale.ROOT), fTipo));
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(BORDER_GRAY);
        c.setPadding(6f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(BRAND_DARK);
        Phrase ph = new Phrase(tipo.toUpperCase(Locale.ROOT),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE));
        c.setPhrase(ph);
        return c;
    }

    private PdfPCell celdaHead(String texto, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fItemHeader));
        c.setBackgroundColor(TABLE_HEAD);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5f);
        return c;
    }

    private PdfPCell celdaBody(String texto, int align, boolean alt) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fItemBody));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BORDER_GRAY);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4f);
        if (alt) c.setBackgroundColor(ROW_ALT);
        return c;
    }

    private com.lowagie.text.Paragraph alinear(Phrase p, int align) {
        com.lowagie.text.Paragraph par = new com.lowagie.text.Paragraph(p);
        par.setAlignment(align);
        return par;
    }

    // ==================== QR ====================

    private Image generarQR(String contenido, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 0);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = writer.encode(contenido, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            int black = new Color(20, 45, 85).getRGB();
            int white = 0x00FFFFFF;
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    img.setRGB(x, y, matrix.get(x, y) ? black : white);
                }
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            return Image.getInstance(bos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ==================== Imagenes ====================

    private Image cargarLogo() {
        String[] candidatos = { "logo.png", "logo.jpg", "banner 1.jpg" };
        String raiz = new File("").getAbsolutePath();
        for (String nombre : candidatos) {
            File[] intentos = {
                new File(raiz, nombre),
                new File(nombre)
            };
            for (File f : intentos) {
                if (f.exists()) {
                    try { return Image.getInstance(f.getAbsolutePath()); } catch (Exception ignore) {}
                }
            }
        }
        return null;
    }

    // ==================== Helpers ====================
    private static boolean noVacio(String s) { return s != null && !s.trim().isEmpty(); }
    private static String nz(String s, String def) { return s == null ? def : s; }

    private static String formatearFecha(Date d) {
        if (d == null) return "-";
        return new SimpleDateFormat("dd/MM/yyyy").format(d);
    }

    private static String formatearFechaLarga(java.util.Date d) {
        if (d == null) return "-";
        return new SimpleDateFormat("EEEE dd 'de' MMMM yyyy", new Locale("es", "CO")).format(d);
    }

    private static String formatearFechaHora(java.util.Date d) {
        if (d == null) return "-";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("es", "CO")).format(d);
    }

    private static String formatearFechaYHora(java.sql.Date fecha, String hora) {
        if (fecha == null) return "-";
        String f = new SimpleDateFormat("dd/MM/yyyy").format(fecha);
        if (hora == null || hora.trim().isEmpty()) return f;
        String h = hora.trim();
        if (h.length() >= 5) h = h.substring(0, 5);
        return f + " " + h;
    }

    // ==================== Datos ====================

    private DatosFactura cargarDatos(Connection cn, int idFactura) throws Exception {
        DatosFactura d = new DatosFactura();
        d.idFactura = idFactura;

        // Encabezado (negocio + factura + cliente + user)
        String sqlCab =
            "SELECT c.nombre_negocio, c.nit_negocio, c.contacto_negocio, c.contacto2_negocio, " +
            "       c.direccion, c.pie_legal, c.servicios, c.nombre_impresora, " +
            "       f.codigo, f.fecha, f.hora, f.tipo_factura, f.observacion, " +
            "       ct.cedula, ct.nombre AS nombre_cliente, ct.direccion AS direccion_cliente, ct.contacto, " +
            "       u.user_name " +
            "FROM facturas_cabeceras f " +
            "JOIN contactos ct     ON f.id_contacto = ct.id " +
            "JOIN users u          ON f.id_user     = u.id " +
            "CROSS JOIN configuraciones c " +
            "WHERE f.id = ?";
        try (PreparedStatement ps = cn.prepareStatement(sqlCab)) {
            ps.setInt(1, idFactura);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    d.nombreNegocio    = rs.getString("nombre_negocio");
                    d.nitNegocio       = rs.getString("nit_negocio");
                    d.contactoNegocio  = rs.getString("contacto_negocio");
                    d.contacto2Negocio = rs.getString("contacto2_negocio");
                    d.direccion        = rs.getString("direccion");
                    d.pieLegal         = rs.getString("pie_legal");
                    d.servicios        = rs.getString("servicios");
                    d.nombreImpresora  = rs.getString("nombre_impresora");
                    d.codigoFactura    = rs.getString("codigo");
                    d.fechaFactura     = rs.getDate("fecha");
                    d.horaFactura      = rs.getString("hora");
                    d.tipoFactura      = rs.getString("tipo_factura");
                    d.observacion      = rs.getString("observacion");
                    d.cedulaCliente    = rs.getString("cedula");
                    d.nombreCliente    = rs.getString("nombre_cliente");
                    d.direccionCliente = rs.getString("direccion_cliente");
                    d.contactoCliente  = rs.getString("contacto");
                    d.userName         = rs.getString("user_name");
                }
            }
        }

        // Detalles
        String sqlDet =
            "SELECT p.codigo_barras, p.descripcion, uni.nombre AS unidad, fd.cantidad " +
            "FROM facturas_detalles fd " +
            "JOIN productos p           ON fd.id_producto = p.id " +
            "JOIN unidades_medidas uni  ON p.id_unidad    = uni.id " +
            "WHERE fd.id_cabecera = ? " +
            "ORDER BY fd.id";
        d.items = new ArrayList<>();
        d.totalArticulos = 0.0;
        try (PreparedStatement ps = cn.prepareStatement(sqlDet)) {
            ps.setInt(1, idFactura);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemFactura it = new ItemFactura();
                    it.codigo      = rs.getString("codigo_barras");
                    it.descripcion = rs.getString("descripcion");
                    it.unidad      = rs.getString("unidad");
                    it.cantidad    = rs.getDouble("cantidad");
                    d.items.add(it);
                    d.totalArticulos += it.cantidad;
                }
            }
        }

        return d;
    }

    // ==================== Modelos internos ====================
    private static class DatosFactura {
        int idFactura;
        String nombreNegocio, nitNegocio, contactoNegocio, contacto2Negocio, direccion, pieLegal, servicios;
        String nombreImpresora;
        String codigoFactura, tipoFactura, observacion;
        Date fechaFactura;
        String horaFactura;
        String cedulaCliente, nombreCliente, direccionCliente, contactoCliente;
        String userName;
        List<ItemFactura> items;
        double totalArticulos;
    }

    private static class ItemFactura {
        String codigo, descripcion, unidad;
        double cantidad;
    }

    // ==================== Page event para pie ==================
    private class PageFooterEvent extends PdfPageEventHelper {
        private final DatosFactura datos;
        private PdfTemplate totalPages;

        PageFooterEvent(DatosFactura d) { this.datos = d; }

        @Override
        public void onOpenDocument(PdfWriter writer, Document doc) {
            totalPages = writer.getDirectContent().createTemplate(30, 10);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle r = doc.getPageSize();
            // Pie justo encima del fold de la media carta, no en el borde inferior de la hoja carta.
            float y = r.getHeight() - MEDIA_CARTA_ALTO + 14f;

            // linea separadora
            cb.setColorStroke(BORDER_GRAY);
            cb.setLineWidth(0.3f);
            cb.moveTo(doc.leftMargin(), y + 10f);
            cb.lineTo(r.getWidth() - doc.rightMargin(), y + 10f);
            cb.stroke();

            // fecha larga (izquierda)
            String fecha = formatearFechaLarga(new java.util.Date());
            Phrase pFecha = new Phrase(fecha, fFooter);
            com.lowagie.text.pdf.ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, pFecha,
                    doc.leftMargin(), y, 0);

            // pagina N de M  (centro)
            int page = writer.getPageNumber();
            Phrase pPag = new Phrase("Pagina " + page + " de ", fFooter);
            com.lowagie.text.pdf.ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, pPag,
                    r.getWidth() / 2f, y, 0);
            cb.addTemplate(totalPages, r.getWidth() / 2f + 30f, y);

            // USER (derecha)
            Phrase pUser = new Phrase();
            pUser.add(new com.lowagie.text.Chunk("USER: ", fFooter));
            pUser.add(new com.lowagie.text.Chunk(nz(datos.userName, "-"), fFooterBold));
            com.lowagie.text.pdf.ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, pUser,
                    r.getWidth() - doc.rightMargin(), y, 0);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document doc) {
            com.lowagie.text.pdf.ColumnText.showTextAligned(totalPages, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(writer.getPageNumber() - 1), fFooter),
                    2f, 2f, 0);
        }
    }

    // Referencia para evitar warnings por PageSize no usado si luego se parametriza
    @SuppressWarnings("unused")
    private static final Rectangle PAGE_SIZE_DEFAULT = PageSize.LETTER;
}
