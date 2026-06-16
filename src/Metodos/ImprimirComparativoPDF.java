package Metodos;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import conexiondb.DBcomparativos;
import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 * Genera en PDF (OpenPDF, horizontal) el Comparativo de cotizaciones (RF-04):
 * matriz productos × proveedores con descuento pronto pago, IVA, flete, precio
 * mínimo neto, mejor proveedor, totales por proveedor y recomendación.
 *
 * @author Monkeyelgrande
 */
public class ImprimirComparativoPDF {

    private static final Color BRAND_DARK = new Color(0x0D47A1);
    private static final Color BRAND_SOFT = new Color(0xE3F2FD);
    private static final Color TABLE_HEAD = new Color(0x1565C0);
    private static final Color ROW_ALT = new Color(0xF5F8FC);
    private static final Color BEST = new Color(0xE8F5E9);
    private static final Color BORDER_GRAY = new Color(0xD2D6DC);

    private final Font fTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BRAND_DARK);
    private final Font fLabel = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(0x69707A));
    private final Font fValue = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
    private final Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6, Color.WHITE);
    private final Font fBody = FontFactory.getFont(FontFactory.HELVETICA, 6, Color.BLACK);
    private final Font fBodyB = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6, Color.BLACK);

    private final DecimalFormat money = new DecimalFormat("###,###,##0");
    private final DecimalFormat dec = new DecimalFormat("###,###,##0.##");

    public void imprimir(int idComparativo) {
        try {
            Datos d = cargar(idComparativo);
            if (d == null) {
                JOptionPane.showMessageDialog(null, "No se encontró el comparativo #" + idComparativo);
                return;
            }
            File pdf = File.createTempFile("comparativo_" + idComparativo + "_", ".pdf");
            pdf.deleteOnExit();
            generar(d, pdf);
            abrir(pdf);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al generar el PDF del comparativo:\n" + ex.getMessage(),
                    "PDF", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generar(Datos d, File archivo) throws Exception {
        Document doc = new Document(PageSize.LETTER.rotate(), 24, 24, 22, 24);
        PdfWriter.getInstance(doc, new FileOutputStream(archivo));
        doc.open();
        encabezado(doc, d);
        matriz(doc, d);
        totales(doc, d);
        doc.close();
    }

    private void encabezado(Document doc, Datos d) throws Exception {
        PdfPTable t = new PdfPTable(new float[]{60f, 40f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(6f);
        t.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        PdfPCell c1 = new PdfPCell();
        c1.setBorder(Rectangle.NO_BORDER);
        c1.addElement(new Phrase("COMPARATIVO DE COTIZACIONES", fTitle));
        c1.addElement(new Phrase(nz(d.numero) + "   •   Fecha: " + fecha(d.fecha)
                + "   •   IVA: " + dec.format(d.iva * 100) + "%", fValue));
        t.addCell(c1);
        PdfPCell c2 = new PdfPCell();
        c2.setBorder(Rectangle.BOX);
        c2.setBorderColor(BORDER_GRAY);
        c2.setBackgroundColor(BRAND_SOFT);
        c2.setPadding(6f);
        String dec2 = "UNICO".equals(d.decision) ? "Todo a un proveedor (" + nz(d.provUnico) + ")"
                : ("POR_PRODUCTO".equals(d.decision) ? "El más barato de cada producto" : "Sin decidir");
        c2.addElement(alinear(new Phrase("Decisión: " + dec2, fValue), Element.ALIGN_RIGHT));
        c2.addElement(alinear(new Phrase("Autorizado por: " + nz(d.autoriza), fLabel), Element.ALIGN_RIGHT));
        t.addCell(c2);
        doc.add(t);
    }

    private void matriz(Document doc, Datos d) throws Exception {
        int nP = d.provs.size();
        // columnas: Código, Descripción, Peso, Cant, [prov x nP], Mín neto, Mejor prov
        float[] w = new float[6 + nP];
        w[0] = 10;
        w[1] = 26;
        w[2] = 6;
        w[3] = 6;
        for (int j = 0; j < nP; j++) {
            w[4 + j] = 10;
        }
        w[4 + nP] = 9;
        w[5 + nP] = 12;
        PdfPTable t = new PdfPTable(w);
        t.setWidthPercentage(100);
        t.setHeaderRows(1);

        t.addCell(head("CÓDIGO", Element.ALIGN_LEFT));
        t.addCell(head("DESCRIPCIÓN", Element.ALIGN_LEFT));
        t.addCell(head("PESO", Element.ALIGN_RIGHT));
        t.addCell(head("CANT", Element.ALIGN_RIGHT));
        for (Prov p : d.provs) {
            t.addCell(head(p.nombre, Element.ALIGN_RIGHT));
        }
        t.addCell(head("MÍN NETO", Element.ALIGN_RIGHT));
        t.addCell(head("MEJOR PROV", Element.ALIGN_LEFT));

        int i = 0;
        for (Item it : d.items) {
            boolean alt = (i++ % 2 == 1);
            t.addCell(body(nz(it.codigo), Element.ALIGN_LEFT, alt, false));
            t.addCell(body(nz(it.descripcion), Element.ALIGN_LEFT, alt, false));
            t.addCell(body(dec.format(it.peso), Element.ALIGN_RIGHT, alt, false));
            t.addCell(body(dec.format(it.cantidad), Element.ALIGN_RIGHT, alt, false));
            double minNeto = Double.NaN;
            int mejorJ = -1;
            for (int j = 0; j < nP; j++) {
                Double lista = it.precios.get(d.provs.get(j).idCv);
                if (lista == null) {
                    t.addCell(body("", Element.ALIGN_RIGHT, alt, false));
                    continue;
                }
                t.addCell(body(money.format(lista), Element.ALIGN_RIGHT, alt, false));
                double neto = lista * (1 - d.provs.get(j).descuento);
                if (Double.isNaN(minNeto) || neto < minNeto) {
                    minNeto = neto;
                    mejorJ = j;
                }
            }
            t.addCell(body(Double.isNaN(minNeto) ? "" : money.format(minNeto), Element.ALIGN_RIGHT, alt, true));
            t.addCell(body(mejorJ < 0 ? "" : d.provs.get(mejorJ).nombre, Element.ALIGN_LEFT, alt, true));
        }
        doc.add(t);
    }

    private void totales(Document doc, Datos d) throws Exception {
        int nP = d.provs.size();
        double[] bruto = new double[nP];
        int[] items = new int[nP];
        double costoMasBarato = 0, pesoTotal = 0;
        for (Item it : d.items) {
            double minNeto = Double.NaN;
            for (int j = 0; j < nP; j++) {
                Double lista = it.precios.get(d.provs.get(j).idCv);
                if (lista == null) {
                    continue;
                }
                items[j]++;
                bruto[j] += lista * it.cantidad;
                double neto = lista * (1 - d.provs.get(j).descuento);
                if (Double.isNaN(minNeto) || neto < minNeto) {
                    minNeto = neto;
                }
            }
            if (!Double.isNaN(minNeto)) {
                costoMasBarato += minNeto * it.cantidad;
            }
            pesoTotal += it.peso * it.cantidad;
        }
        double[] total = new double[nP];
        double mejorTotal = Double.NaN;
        int mejorJ = -1;
        for (int j = 0; j < nP; j++) {
            double neto = bruto[j] * (1 - d.provs.get(j).descuento);
            double iva = neto * d.iva;
            total[j] = neto + iva + d.provs.get(j).flete;
            if (items[j] > 0 && (Double.isNaN(mejorTotal) || total[j] < mejorTotal)) {
                mejorTotal = total[j];
                mejorJ = j;
            }
        }

        PdfPTable t = new PdfPTable(new float[]{26f, 10f, 16f, 16f, 12f, 10f, 16f});
        t.setWidthPercentage(100);
        t.setSpacingBefore(10f);
        t.setHeaderRows(1);
        Font h = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.WHITE);
        for (String c : new String[]{"PROVEEDOR", "ÍTEMS", "SUBTOTAL BRUTO", "SUBTOTAL NETO", "IVA", "FLETE", "TOTAL BODEGA"}) {
            PdfPCell hc = new PdfPCell(new Phrase(c, h));
            hc.setBackgroundColor(TABLE_HEAD);
            hc.setBorder(Rectangle.NO_BORDER);
            hc.setPadding(4f);
            hc.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(hc);
        }
        Font b = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);
        for (int j = 0; j < nP; j++) {
            double neto = bruto[j] * (1 - d.provs.get(j).descuento);
            double iva = neto * d.iva;
            Color bg = (j == mejorJ) ? BEST : Color.WHITE;
            t.addCell(tot(d.provs.get(j).nombre, Element.ALIGN_LEFT, bg, b));
            t.addCell(tot(String.valueOf(items[j]), Element.ALIGN_CENTER, bg, b));
            t.addCell(tot("$ " + money.format(bruto[j]), Element.ALIGN_RIGHT, bg, b));
            t.addCell(tot("$ " + money.format(neto), Element.ALIGN_RIGHT, bg, b));
            t.addCell(tot("$ " + money.format(iva), Element.ALIGN_RIGHT, bg, b));
            t.addCell(tot("$ " + money.format(d.provs.get(j).flete), Element.ALIGN_RIGHT, bg, b));
            t.addCell(tot("$ " + money.format(total[j]), Element.ALIGN_RIGHT, bg,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.BLACK)));
        }
        doc.add(t);

        double ton = pesoTotal / 1000.0;
        int viajes = d.capacidad > 0 ? (int) Math.ceil(ton / d.capacidad) : 0;
        com.lowagie.text.Paragraph p = new com.lowagie.text.Paragraph();
        p.setSpacingBefore(8f);
        p.add(new Phrase("Recomendación: ", fValue));
        if (mejorJ >= 0) {
            p.add(new Phrase("mejor proveedor único = " + d.provs.get(mejorJ).nombre
                    + " (total $ " + money.format(mejorTotal) + ").  ", fBody));
        }
        p.add(new Phrase("Costo si toma el más barato de cada producto (neto, sin flete/IVA): $ "
                + money.format(costoMasBarato) + ".  ", fBody));
        p.add(new Phrase("Peso total: " + dec.format(pesoTotal) + " kg (" + dec.format(ton)
                + " ton).  Viajes necesarios (camión " + dec.format(d.capacidad) + " ton): " + viajes + ".", fBody));
        doc.add(p);
    }

    private PdfPCell head(String t, int align) {
        PdfPCell c = new PdfPCell(new Phrase(t, fHead));
        c.setBackgroundColor(TABLE_HEAD);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPadding(3f);
        return c;
    }

    private PdfPCell body(String t, int align, boolean alt, boolean bold) {
        PdfPCell c = new PdfPCell(new Phrase(t, bold ? fBodyB : fBody));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(BORDER_GRAY);
        c.setHorizontalAlignment(align);
        c.setPadding(2.5f);
        if (alt) {
            c.setBackgroundColor(ROW_ALT);
        }
        return c;
    }

    private PdfPCell tot(String t, int align, Color bg, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(t, f));
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(BORDER_GRAY);
        c.setHorizontalAlignment(align);
        c.setPadding(3f);
        c.setBackgroundColor(bg);
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

    private Datos cargar(int id) {
        Datos d = new Datos();
        ResultSet rs = DBcomparativos.cargarCabecera(id);
        try {
            if (rs.next()) {
                d.numero = rs.getString("numero");
                d.fecha = rs.getDate("fecha");
                d.iva = rs.getDouble("iva_pct");
                d.capacidad = rs.getDouble("capacidad_camion_ton");
                d.decision = rs.getString("decision");
                d.provUnico = rs.getString("proveedor_unico");
                d.autoriza = rs.getString("autoriza");
            } else {
                rs.close();
                return null;
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
        d.provs = new ArrayList<>();
        rs = DBcomparativos.cargarProveedores(id);
        try {
            while (rs.next()) {
                Prov p = new Prov();
                p.idCv = rs.getInt("id");
                p.nombre = rs.getString("nombre");
                p.descuento = rs.getDouble("descuento_pronto_pago");
                p.flete = rs.getDouble("flete");
                d.provs.add(p);
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        Map<Integer, Item> porCp = new HashMap<>();
        d.items = new ArrayList<>();
        rs = DBcomparativos.cargarProductos(id);
        try {
            while (rs.next()) {
                Item it = new Item();
                it.idCp = rs.getInt("id");
                it.codigo = rs.getString("codigo_barras");
                it.descripcion = rs.getString("descripcion");
                it.cantidad = rs.getDouble("cantidad");
                it.peso = rs.getDouble("peso_unitario");
                it.precios = new HashMap<>();
                d.items.add(it);
                porCp.put(it.idCp, it);
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        rs = DBcomparativos.cargarPrecios(id);
        try {
            while (rs.next()) {
                Item it = porCp.get(rs.getInt("id_comp_producto"));
                if (it != null) {
                    it.precios.put(rs.getInt("id_comp_proveedor"), rs.getDouble("precio_lista"));
                }
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return d;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String fecha(java.sql.Date f) {
        return f == null ? "-" : new SimpleDateFormat("dd/MM/yyyy").format(f);
    }

    private static class Datos {
        String numero, decision, provUnico, autoriza;
        java.sql.Date fecha;
        double iva, capacidad;
        List<Prov> provs;
        List<Item> items;
    }

    private static class Prov {
        int idCv;
        String nombre;
        double descuento, flete;
    }

    private static class Item {
        int idCp;
        String codigo, descripcion;
        double cantidad, peso;
        Map<Integer, Double> precios;
    }
}
