/*
 * Modulo Caja: exportacion a PDF de los reportes de caja.
 * Diseno "empresarial": banda de encabezado con los datos del negocio, titulo
 * y rango del reporte, tarjetas de totales grandes, tabla con encabezado
 * oscuro y filas cebra, y pie con paginacion.
 *
 * Usa iText 2.1.7 (com.lowagie.text), la misma libreria que ya usa
 * Creditos/ImprimirReciboPDF, para no meter otra dependencia.
 */
package Caja;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import conexiondb.DB_consultas_R_D;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Genera el PDF de un reporte de caja a partir de la tabla en pantalla.
 *
 * @author Monkeyelgrande
 */
public final class ReportePDFCaja {

    // Paleta corporativa (coherente con EstiloCaja)
    private static final Color TINTA = new Color(0x16181D);
    private static final Color GRIS = new Color(0x5B616E);
    private static final Color GRIS_SUAVE = new Color(0x8A909C);
    private static final Color LINEA = new Color(0xD8DBE0);
    private static final Color CEBRA = new Color(0xF7F8FA);

    private static final Font F_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, TINTA);
    private static final Font F_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, GRIS);
    private static final Font F_NEGOCIO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TINTA);
    private static final Font F_NEGOCIO_SUB = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, GRIS_SUAVE);
    private static final Font F_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font F_TD = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TINTA);
    private static final Font F_TD_NEG = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TINTA);
    private static final Font F_KPI_LBL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, GRIS);
    private static final Font F_PIE = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, GRIS_SUAVE);

    private ReportePDFCaja() {
    }

    /** Una tarjeta de total del bloque superior. */
    public static class Kpi {

        final String etiqueta;
        final String valor;
        final Color color;

        public Kpi(String etiqueta, String valor, Color color) {
            this.etiqueta = etiqueta;
            this.valor = valor;
            this.color = color;
        }
    }

    /**
     * Exporta la tabla a PDF pidiendo la ruta al usuario.
     *
     * @param tabla tabla en pantalla (se exporta tal cual se ve)
     * @param titulo titulo del reporte
     * @param subtitulo rango de fechas / filtros aplicados
     * @param kpis tarjetas de totales (puede ser null)
     * @param acento color de identidad para las franjas
     * @param padre componente para los dialogos
     */
    public static void exportar(JTable tabla, String titulo, String subtitulo,
            Kpi[] kpis, Color acento, java.awt.Component padre) {

        if (tabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(padre, "No hay datos para exportar.");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar reporte en PDF");
        fc.setFileFilter(new FileNameExtensionFilter("Archivo PDF", "pdf"));
        String sugerido = titulo.replaceAll("[^a-zA-Z0-9]+", "_")
                + "_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf";
        fc.setSelectedFile(new File(sugerido));
        if (fc.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File destino = fc.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".pdf")) {
            destino = new File(destino.getAbsolutePath() + ".pdf");
        }

        // Horizontal si la tabla tiene muchas columnas
        boolean horizontal = tabla.getColumnCount() > 6;
        Document doc = new Document(horizontal ? PageSize.LETTER.rotate() : PageSize.LETTER,
                36, 36, 92, 46);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(destino));
            String[] negocio = datosNegocio();
            writer.setPageEvent(new Encabezado(negocio, titulo, subtitulo, acento));
            doc.open();

            if (kpis != null && kpis.length > 0) {
                doc.add(bloqueKpis(kpis));
                doc.add(espacio(14));
            }
            doc.add(tablaDatos(tabla, acento));
            doc.close();

            int r = JOptionPane.showConfirmDialog(padre,
                    "PDF generado correctamente.\n¿Desea abrirlo ahora?",
                    "Reporte exportado", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                try {
                    java.awt.Desktop.getDesktop().open(destino);
                } catch (Exception ex) {
                    System.out.println("No se pudo abrir el PDF: " + ex);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(padre, "No se pudo generar el PDF:\n" + e,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Tarjetas de totales, grandes y legibles. */
    private static PdfPTable bloqueKpis(Kpi[] kpis) throws DocumentException {
        PdfPTable t = new PdfPTable(kpis.length);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);
        for (Kpi k : kpis) {
            PdfPTable interna = new PdfPTable(1);
            interna.setWidthPercentage(100);

            PdfPCell lbl = new PdfPCell(new Phrase(k.etiqueta.toUpperCase(), F_KPI_LBL));
            lbl.setBorder(Rectangle.NO_BORDER);
            lbl.setPaddingBottom(3);
            lbl.setPaddingLeft(10);
            lbl.setPaddingTop(9);
            interna.addCell(lbl);

            Font fv = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17,
                    k.color != null ? k.color : TINTA);
            PdfPCell val = new PdfPCell(new Phrase(k.valor, fv));
            val.setBorder(Rectangle.NO_BORDER);
            val.setPaddingLeft(10);
            val.setPaddingBottom(10);
            interna.addCell(val);

            PdfPCell cont = new PdfPCell(interna);
            cont.setBackgroundColor(Color.WHITE);
            cont.setBorderColor(LINEA);
            cont.setBorderWidth(0.8f);
            // franja de color arriba
            cont.setBorderWidthTop(3f);
            cont.setBorderColorTop(k.color != null ? k.color : LINEA);
            cont.setPadding(0);
            t.addCell(cont);
        }
        return t;
    }

    /** Tabla de datos con encabezado oscuro y filas cebra. */
    private static PdfPTable tablaDatos(JTable tabla, Color acento) throws DocumentException {
        int cols = tabla.getColumnCount();
        PdfPTable t = new PdfPTable(cols);
        t.setWidthPercentage(100);
        t.setHeaderRows(1);

        // Anchos proporcionales a los de pantalla
        float[] w = new float[cols];
        for (int c = 0; c < cols; c++) {
            w[c] = Math.max(30, tabla.getColumnModel().getColumn(c).getPreferredWidth());
        }
        t.setWidths(w);

        for (int c = 0; c < cols; c++) {
            PdfPCell h = new PdfPCell(new Phrase(tabla.getColumnName(c).toUpperCase(), F_TH));
            h.setBackgroundColor(acento);
            h.setBorder(Rectangle.NO_BORDER);
            h.setPadding(6);
            h.setHorizontalAlignment(esNumerica(tabla, c) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            t.addCell(h);
        }

        for (int f = 0; f < tabla.getRowCount(); f++) {
            boolean cebra = f % 2 == 1;
            // Fila de totales (TOTAL / SALDO) en negrita
            String prim = valor(tabla, f, 0).trim().toUpperCase();
            boolean esTotal = prim.equals("TOTAL") || prim.equals("SALDO") || prim.equals("BALANCE");

            for (int c = 0; c < cols; c++) {
                PdfPCell cell = new PdfPCell(new Phrase(valor(tabla, f, c), esTotal ? F_TD_NEG : F_TD));
                cell.setBorder(Rectangle.BOTTOM);
                cell.setBorderColor(LINEA);
                cell.setBorderWidthBottom(0.5f);
                cell.setPadding(5);
                cell.setHorizontalAlignment(esNumerica(tabla, c) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                if (esTotal) {
                    cell.setBackgroundColor(new Color(0xEDEFF3));
                } else if (cebra) {
                    cell.setBackgroundColor(CEBRA);
                }
                t.addCell(cell);
            }
        }
        return t;
    }

    /** Heuristica: columnas de dinero/valor se alinean a la derecha. */
    private static boolean esNumerica(JTable tabla, int col) {
        String n = tabla.getColumnName(col).toLowerCase();
        return n.contains("total") || n.contains("valor") || n.contains("saldo")
                || n.contains("entrada") || n.contains("salida")
                || n.contains("ingresos") || n.contains("egresos") || n.contains("neto");
    }

    private static String valor(JTable t, int f, int c) {
        Object o = t.getValueAt(f, c);
        return o == null ? "" : o.toString();
    }

    private static Paragraph espacio(float alto) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(alto);
        return p;
    }

    private static String[] datosNegocio() {
        String[] d = {"", "", ""};
        try (ResultSet rs = DB_consultas_R_D.getTabla("select * from configuraciones")) {
            if (rs.next()) {
                d[0] = str(rs.getString("nombre_negocio"));
                d[1] = str(rs.getString("nit_negocio"));
                d[2] = str(rs.getString("direccion"));
            }
        } catch (Exception e) {
            System.out.println("datosNegocio: " + e);
        }
        return d;
    }

    private static String str(String s) {
        return s == null ? "" : s;
    }

    /** Encabezado y pie repetidos en cada pagina. */
    private static class Encabezado extends PdfPageEventHelper {

        private final String[] negocio;
        private final String titulo;
        private final String subtitulo;
        private final Color acento;
        private final String generado;

        Encabezado(String[] negocio, String titulo, String subtitulo, Color acento) {
            this.negocio = negocio;
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.acento = acento;
            this.generado = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        }

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            try {
                Rectangle p = doc.getPageSize();
                float izq = doc.leftMargin();
                float der = p.getWidth() - doc.rightMargin();
                float top = p.getHeight() - 30;

                // Encabezado: negocio a la izquierda, titulo a la derecha
                PdfPTable head = new PdfPTable(2);
                head.setTotalWidth(der - izq);
                head.setWidths(new float[]{1.1f, 1f});

                PdfPCell izqC = new PdfPCell();
                izqC.setBorder(Rectangle.NO_BORDER);
                izqC.addElement(new Paragraph(negocio[0], F_NEGOCIO));
                if (!negocio[1].isEmpty()) {
                    izqC.addElement(new Paragraph("NIT " + negocio[1], F_NEGOCIO_SUB));
                }
                if (!negocio[2].isEmpty()) {
                    izqC.addElement(new Paragraph(negocio[2], F_NEGOCIO_SUB));
                }
                head.addCell(izqC);

                PdfPCell derC = new PdfPCell();
                derC.setBorder(Rectangle.NO_BORDER);
                derC.setHorizontalAlignment(Element.ALIGN_RIGHT);
                Paragraph pt = new Paragraph(titulo, F_TITULO);
                pt.setAlignment(Element.ALIGN_RIGHT);
                derC.addElement(pt);
                Paragraph ps = new Paragraph(subtitulo, F_SUBTITULO);
                ps.setAlignment(Element.ALIGN_RIGHT);
                derC.addElement(ps);
                head.addCell(derC);

                head.writeSelectedRows(0, -1, izq, top, writer.getDirectContent());

                // Linea de acento bajo el encabezado
                writer.getDirectContent().setColorFill(acento);
                writer.getDirectContent().rectangle(izq, p.getHeight() - 86, der - izq, 2.5f);
                writer.getDirectContent().fill();

                // Pie: generado + pagina
                PdfPTable pie = new PdfPTable(2);
                pie.setTotalWidth(der - izq);
                pie.setWidths(new float[]{1f, 1f});
                PdfPCell a = new PdfPCell(new Phrase("Generado el " + generado, F_PIE));
                a.setBorder(Rectangle.TOP);
                a.setBorderColor(LINEA);
                a.setPaddingTop(4);
                pie.addCell(a);
                PdfPCell b = new PdfPCell(new Phrase("Página " + writer.getPageNumber(), F_PIE));
                b.setBorder(Rectangle.TOP);
                b.setBorderColor(LINEA);
                b.setPaddingTop(4);
                b.setHorizontalAlignment(Element.ALIGN_RIGHT);
                pie.addCell(b);
                pie.writeSelectedRows(0, -1, izq, doc.bottomMargin() - 6, writer.getDirectContent());
            } catch (Exception e) {
                System.out.println("Encabezado PDF: " + e);
            }
        }
    }
}
