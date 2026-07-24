package Creditos;

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
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Genera el recibo de abono en PDF (media carta) con iText 2.1.7 y ZXing para
 * el QR. Reemplaza al reporte Jasper Recibo_pago manteniendo todos los campos:
 * encabezado con datos del negocio, ID recibo, tipo de pago, fecha, cliente,
 * observacion, valor en letras, total y firma del cliente.
 */
public class ImprimirReciboPDF {

    // ==================== Paleta de marca ====================
    private static final Color BRAND_DARK   = new Color(20, 45, 85);
    private static final Color BRAND_ACCENT = new Color(212, 175, 55);   // acento dorado sutil
    private static final Color HEADER_SOFT  = new Color(238, 242, 248);
    private static final Color TABLE_HEAD   = new Color(33, 55, 92);
    private static final Color BORDER_GRAY  = new Color(210, 214, 220);
    private static final Color TEXT_MUTED   = new Color(105, 110, 120);

    // ==================== Tipografias ====================
    private final Font fBrandTitle     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, BRAND_DARK);
    private final Font fLabelSmall     = FontFactory.getFont(FontFactory.HELVETICA,       6, TEXT_MUTED);
    private final Font fLabel          = FontFactory.getFont(FontFactory.HELVETICA,       7, TEXT_MUTED);
    private final Font fValue          = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  8, Color.BLACK);
    private final Font fValueBig       = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private final Font fValueMid       = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BRAND_DARK);
    private final Font fSectionHeader  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  9, Color.WHITE);
    private final Font fBody           = FontFactory.getFont(FontFactory.HELVETICA,       8, Color.BLACK);
    private final Font fBodyBig        = FontFactory.getFont(FontFactory.HELVETICA,      10, Color.BLACK);
    private final Font fBodyBold       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  8, Color.BLACK);
    private final Font fLetras         = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  9, BRAND_DARK);
    private final Font fTotal          = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BRAND_DARK);
    private final Font fFooter         = FontFactory.getFont(FontFactory.HELVETICA,       6, TEXT_MUTED);
    private final Font fFooterBold     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  6, BRAND_DARK);

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
    public void imprimir(int idAbono) {
        try {
            DatosRecibo datos;
            try (Connection cn = DB_consultas_R_D.getConexion()) {
                datos = cargarDatos(cn, idAbono);
            }
            if (datos == null) {
                JOptionPane.showMessageDialog(null,
                        "No se encontro el abono #" + idAbono,
                        "Impresion", JOptionPane.WARNING_MESSAGE);
                return;
            }
            File pdf = File.createTempFile("recibo_" + idAbono + "_", ".pdf");
            pdf.deleteOnExit();
            generarPDF(datos, pdf);

            int opcion = mostrarDialogoOpciones(idAbono);
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
                    "Error al generar el recibo:\n" + ex.getMessage(),
                    "Impresion", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Muestra el dialogo con tres botones: Imprimir, Ver, Cancelar.
     * @return 0 = Imprimir, 1 = Ver, 2 = Cancelar (o cerrar dialogo).
     */
    private int mostrarDialogoOpciones(int idAbono) {
        Object[] opciones = { "Imprimir", "Ver", "Cancelar" };
        int sel = JOptionPane.showOptionDialog(null,
                "Que deseas hacer con el recibo?",
                "Recibo #" + idAbono,
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
            job.setJobName("Recibo " + pdf.getName());
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

    private void generarPDF(DatosRecibo d, File archivo) throws Exception {
        Rectangle pageSize = PageSize.LETTER;
        float bottomMargin = pageSize.getHeight() - MEDIA_CARTA_ALTO + 20f;
        Document doc = new Document(pageSize, 20, 20, 14, bottomMargin);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(archivo));

        PageFooterEvent footer = new PageFooterEvent(d);
        writer.setPageEvent(footer);

        doc.open();

        agregarEncabezado(doc, d);
        agregarBarraCliente(doc, d);
        agregarDetalleAbono(doc, d);
        agregarResumenFinal(doc, d);

        doc.close();
    }

    // -------- Encabezado (logo + datos negocio + ID recibo/tipo/fecha + QR) --------
    private void agregarEncabezado(Document doc, DatosRecibo d) throws Exception {
        PdfPTable outer = new PdfPTable(new float[]{ 42f, 33f, 25f });
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(4f);
        outer.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // ---- Columna 1: Logo ----
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
            cLogo.addElement(new Phrase(nz(d.nombreNegocio, "RECIBO"), fBrandTitle));
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

        tInfo.addCell(celdaSimple(nz(d.nombreNegocio, ""), fBrandTitle, Element.ALIGN_LEFT));
        if (noVacio(d.nitNegocio)) {
            tInfo.addCell(celdaLabelValor("NIT:", d.nitNegocio));
        }
        if (noVacio(d.direccionNegocio)) {
            tInfo.addCell(celdaSimple(d.direccionNegocio, fValue, Element.ALIGN_LEFT));
        }
        if (noVacio(d.telefonoNegocio)) {
            tInfo.addCell(celdaLabelValor("Tel:", d.telefonoNegocio));
        }
        if (noVacio(d.telefono2Negocio)) {
            tInfo.addCell(celdaLabelValor("Tel 2:", d.telefono2Negocio));
        }
        cInfo.addElement(tInfo);
        outer.addCell(cInfo);

        // ---- Columna 3: ID recibo, tipo de pago, fecha y QR ----
        PdfPCell cMeta = new PdfPCell();
        cMeta.setBorder(Rectangle.BOX);
        cMeta.setBorderColor(BORDER_GRAY);
        cMeta.setPadding(0f);

        PdfPTable tMeta = new PdfPTable(new float[]{ 65f, 35f });
        tMeta.setWidthPercentage(100);
        tMeta.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Fila 1: ID recibo (izq) + QR (der, rowspan 3)
        PdfPCell cIdRecibo = new PdfPCell();
        cIdRecibo.setBorder(Rectangle.BOTTOM);
        cIdRecibo.setBorderColor(BORDER_GRAY);
        cIdRecibo.setPadding(2f);
        cIdRecibo.setBackgroundColor(HEADER_SOFT);
        cIdRecibo.addElement(new Phrase("RECIBO No.", fLabelSmall));
        cIdRecibo.addElement(new Phrase(String.valueOf(d.idAbono), fValueBig));
        tMeta.addCell(cIdRecibo);

        PdfPCell cQr = new PdfPCell();
        cQr.setBorder(Rectangle.BOTTOM);
        cQr.setBorderColor(BORDER_GRAY);
        cQr.setPadding(2f);
        cQr.setHorizontalAlignment(Element.ALIGN_CENTER);
        cQr.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cQr.setRowspan(3);
        Image qr = generarQR("RECIBO-" + d.idAbono, 220);
        if (qr != null) {
            qr.scaleToFit(55f, 55f);
            qr.setAlignment(Image.ALIGN_CENTER);
            cQr.addElement(qr);
        }
        tMeta.addCell(cQr);

        // Fila 2: Tipo de pago
        PdfPCell cTipo = new PdfPCell();
        cTipo.setBorder(Rectangle.BOTTOM);
        cTipo.setBorderColor(BORDER_GRAY);
        cTipo.setPadding(2f);
        cTipo.addElement(new Phrase("TIPO DE PAGO", fLabelSmall));
        cTipo.addElement(new Phrase(nz(d.tipoAbono, "-").toUpperCase(Locale.ROOT), fValueMid));
        tMeta.addCell(cTipo);

        // Fila 3: Fecha y hora del abono
        PdfPCell cFecha = new PdfPCell();
        cFecha.setBorder(Rectangle.NO_BORDER);
        cFecha.setPadding(2f);
        cFecha.addElement(new Phrase("FECHA", fLabelSmall));
        cFecha.addElement(new Phrase(formatearFechaYHora(d.fechaAbono, d.horaAbono), fValue));
        tMeta.addCell(cFecha);

        cMeta.addElement(tMeta);
        outer.addCell(cMeta);

        doc.add(outer);
    }

    // -------- Barra cliente --------
    private void agregarBarraCliente(Document doc, DatosRecibo d) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{ 15f, 45f, 15f, 25f });
        t.setWidthPercentage(100);
        t.setSpacingAfter(4f);

        t.addCell(celdaCliente("Nombre Cliente:", fLabel));
        t.addCell(celdaCliente(nz(d.nombreCliente, "-"), fBodyBold));
        t.addCell(celdaCliente("CC/NIT:", fLabel));
        t.addCell(celdaCliente(nz(d.cedulaCliente, "-"), fBodyBold));

        t.addCell(celdaCliente("Direccion:", fLabel));
        t.addCell(celdaCliente(nz(d.direccionCliente, "-"), fBodyBold));
        t.addCell(celdaCliente("Telefono:", fLabel));
        t.addCell(celdaCliente(nz(d.telefonoCliente, "-"), fBodyBold));

        doc.add(t);
    }

    // -------- Detalle del abono (concepto + valor + letras) --------
    private void agregarDetalleAbono(Document doc, DatosRecibo d) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{ 72f, 28f });
        t.setWidthPercentage(100);
        t.setSpacingAfter(2f);

        // Encabezado de seccion
        t.addCell(celdaHead("CONCEPTO / OBSERVACION", Element.ALIGN_LEFT));
        t.addCell(celdaHead("VALOR ABONADO", Element.ALIGN_RIGHT));

        // Cuerpo: observacion (izq) + total destacado (der)
        PdfPCell cObs = new PdfPCell();
        cObs.setBorder(Rectangle.BOTTOM | Rectangle.LEFT);
        cObs.setBorderColor(BORDER_GRAY);
        cObs.setPadding(6f);
        cObs.setMinimumHeight(52f);
        cObs.addElement(new Phrase(nz(d.observacion, ""), fBodyBig));
        t.addCell(cObs);

        DecimalFormat df = new DecimalFormat("$ ###,###,##0");
        PdfPCell cValor = new PdfPCell(new Phrase(df.format(d.abono), fTotal));
        cValor.setBorder(Rectangle.BOTTOM | Rectangle.RIGHT);
        cValor.setBorderColor(BORDER_GRAY);
        cValor.setBackgroundColor(HEADER_SOFT);
        cValor.setPadding(6f);
        cValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cValor.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(cValor);

        // Valor en letras
        PdfPCell cLetras = new PdfPCell();
        cLetras.setColspan(2);
        cLetras.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
        cLetras.setBorderColor(BORDER_GRAY);
        cLetras.setPadding(5f);
        Phrase pLetras = new Phrase();
        pLetras.add(new com.lowagie.text.Chunk("SON (LETRAS): ", fLabel));
        pLetras.add(new com.lowagie.text.Chunk(nz(d.letras, ""), fLetras));
        cLetras.addElement(pLetras);
        t.addCell(cLetras);

        doc.add(t);
    }

    // -------- Resumen final (firma + total) --------
    private void agregarResumenFinal(Document doc, DatosRecibo d) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{ 50f, 28f, 22f });
        t.setWidthPercentage(100);
        t.setSpacingBefore(10f);

        // Firma
        PdfPCell cFirma = new PdfPCell();
        cFirma.setBorder(Rectangle.NO_BORDER);
        cFirma.setPaddingTop(14f);
        PdfPTable tFirma = new PdfPTable(1);
        tFirma.setWidthPercentage(100);
        tFirma.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell linea = new PdfPCell(new Phrase(" ", fBody));
        linea.setBorder(Rectangle.BOTTOM);
        linea.setBorderColor(Color.BLACK);
        linea.setFixedHeight(14f);
        tFirma.addCell(linea);
        tFirma.addCell(celdaSimple("Firma Cliente: " + nz(d.nombreCliente, ""), fFooterBold, Element.ALIGN_LEFT));
        tFirma.addCell(celdaSimple("C.C / NIT: " + nz(d.cedulaCliente, ""), fFooterBold, Element.ALIGN_LEFT));

        cFirma.addElement(tFirma);
        t.addCell(cFirma);

        // Espacio central
        PdfPCell cVacio = new PdfPCell(new Phrase(" ", fBody));
        cVacio.setBorder(Rectangle.NO_BORDER);
        t.addCell(cVacio);

        // Total abonado
        PdfPCell cTotal = new PdfPCell();
        cTotal.setBorder(Rectangle.BOX);
        cTotal.setBorderColor(BRAND_DARK);
        cTotal.setBackgroundColor(HEADER_SOFT);
        cTotal.setPadding(4f);
        cTotal.setHorizontalAlignment(Element.ALIGN_CENTER);
        cTotal.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cTotal.addElement(alinear(new Phrase("Total abonado", fLabel), Element.ALIGN_CENTER));
        DecimalFormat dfTot = new DecimalFormat("$ ###,###,##0");
        cTotal.addElement(alinear(new Phrase(dfTot.format(d.abono), fValueBig), Element.ALIGN_CENTER));
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

    private PdfPCell celdaHead(String texto, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fSectionHeader));
        c.setBackgroundColor(TABLE_HEAD);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5f);
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
            int black = BRAND_DARK.getRGB();
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
        String raiz = new File("").getAbsolutePath();
        File[] candidatos = {
            new File(raiz, "logo.png"),
            new File(raiz, "logo.jpg"),
            new File("C:\\logo.jpg"),
            new File("C:\\logo.png")
        };
        for (File f : candidatos) {
            if (f.exists()) {
                try { return Image.getInstance(f.getAbsolutePath()); } catch (Exception ignore) {}
            }
        }
        return null;
    }

    // ==================== Helpers ====================
    private static boolean noVacio(String s) { return s != null && !s.trim().isEmpty(); }
    private static String nz(String s, String def) { return s == null ? def : s; }

    private static String formatearFechaLarga(java.util.Date d) {
        if (d == null) return "-";
        return new SimpleDateFormat("EEEE dd 'de' MMMM yyyy", new Locale("es", "CO")).format(d);
    }

    private static String formatearFechaYHora(Date fecha, String hora) {
        if (fecha == null) return "-";
        String f = new SimpleDateFormat("dd/MM/yyyy").format(fecha);
        if (hora == null || hora.trim().isEmpty()) return f;
        String h = hora.trim();
        if (h.length() >= 5) h = h.substring(0, 5);
        return f + " " + h;
    }

    // ==================== Datos ====================

    private DatosRecibo cargarDatos(Connection cn, int idAbono) throws Exception {
        String sql =
            "SELECT a.total AS abono, a.fecha, a.hora, a.observacion, " +
            "       c.nombre AS nombre_cliente, c.direccion AS direccion_cliente, " +
            "       coalesce(nullif(c.contacto,''), c.contacto2) AS telefono_cliente, c.cedula, " +
            "       t.nombre AS tipo, " +
            "       u.user_name, " +
            "       co.nombre_negocio, co.nit_negocio, co.direccion, co.contacto_negocio AS telefono, " +
            "       co.contacto2_negocio AS telefono_2, co.nombre_impresora " +
            "FROM abonos_cabeceras a " +
            "JOIN contactos c          ON a.id_contacto   = c.id " +
            "LEFT JOIN tipos_abonos t  ON a.id_tipo_abono = t.id " +
            "LEFT JOIN users u         ON a.id_user       = u.id " +
            "CROSS JOIN configuraciones co " +
            "WHERE a.id = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idAbono);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                DatosRecibo d = new DatosRecibo();
                d.idAbono          = idAbono;
                d.abono            = rs.getDouble("abono");
                d.fechaAbono       = rs.getDate("fecha");
                d.horaAbono        = rs.getString("hora");
                d.observacion      = rs.getString("observacion");
                d.nombreCliente    = rs.getString("nombre_cliente");
                d.direccionCliente = rs.getString("direccion_cliente");
                d.telefonoCliente  = rs.getString("telefono_cliente");
                d.cedulaCliente    = rs.getString("cedula");
                d.tipoAbono        = rs.getString("tipo");
                d.userName         = rs.getString("user_name");
                d.nombreNegocio    = rs.getString("nombre_negocio");
                d.nitNegocio       = rs.getString("nit_negocio");
                d.direccionNegocio = rs.getString("direccion");
                d.telefonoNegocio  = rs.getString("telefono");
                d.telefono2Negocio = rs.getString("telefono_2");
                d.nombreImpresora  = rs.getString("nombre_impresora");

                long valor = Math.round(d.abono);
                d.letras = "$ " + new Numero_a_Letra()
                        .Convertir(String.valueOf(valor), true) + " PESOS";
                return d;
            }
        }
    }

    // ==================== Modelos internos ====================
    private static class DatosRecibo {
        int idAbono;
        double abono;
        Date fechaAbono;
        String horaAbono, observacion;
        String nombreCliente, direccionCliente, telefonoCliente, cedulaCliente;
        String tipoAbono, userName, letras;
        String nombreNegocio, nitNegocio, direccionNegocio, telefonoNegocio, telefono2Negocio;
        String nombreImpresora;
    }

    // ==================== Page event para pie ==================
    private class PageFooterEvent extends PdfPageEventHelper {
        private final DatosRecibo datos;
        private PdfTemplate totalPages;

        PageFooterEvent(DatosRecibo d) { this.datos = d; }

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
}
