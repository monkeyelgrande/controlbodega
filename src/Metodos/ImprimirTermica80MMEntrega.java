package Metodos;

import Formularios.frm_main;
import Metodos.EntregaQRService.ItemEntrega;
import Metodos.EntregaQRService.OrdenInfo;
import conexiondb.DB_consultas_R_D;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;

/**
 * Comprobante 80mm para una ENTREGA puntual realizada desde el modulo
 * Entregas Rapidas. Imprime SOLO los items entregados en el evento (no la
 * orden completa) y el saldo pendiente al cierre del comprobante.
 *
 * Reutiliza la misma maquetacion ESC/POS de ImprimirTermica80MMOrden.
 */
public class ImprimirTermica80MMEntrega {

    private static final byte ESC = 0x1B;
    private static final byte GS  = 0x1D;
    private static final int LINE_CHARS = 48;

    private static final int COL_CODIGO = 10;
    private static final int COL_DESC   = 30;
    private static final int COL_CANT   = 8;

    private static final String ENCODING    = "windows-1252";
    private static final int    CODEPAGE_N  = 16;

    /**
     * Punto de entrada. Toma los datos del comprobante y dispara la impresion
     * usando la impresora configurada (frm_main.impresora_ticket).
     * Si la impresora no esta disponible, no lanza excepcion (silenciosa).
     */
    public static void imprimirComprobante(int idEntregaCab,
                                           OrdenInfo orden,
                                           List<ItemEntrega> items,
                                           String nombreUsuario,
                                           String nombreBodega) {
        try {
            PrintService service = ImprimirTermica80MMOrden
                    .buscarImpresoraPorNombre(frm_main.impresora_ticket);
            if (service == null) {
                System.out.println("Comprobante entrega: impresora no encontrada ('"
                        + frm_main.impresora_ticket + "')");
                return;
            }

            // Datos de negocio
            String nombreNegocio = "", nit = "", direccion = "", telefono = "";
            try (ResultSet rs = DB_consultas_R_D.getTabla(
                    "select nombre_negocio, nit_negocio, contacto_negocio, direccion "
                    + "from configuraciones limit 1")) {
                if (rs != null && rs.next()) {
                    nombreNegocio = nullSafe(rs.getString("nombre_negocio"));
                    nit           = nullSafe(rs.getString("nit_negocio"));
                    telefono      = nullSafe(rs.getString("contacto_negocio"));
                    direccion     = nullSafe(rs.getString("direccion"));
                }
            } catch (Exception e) {
                System.out.println("Comprobante entrega: lectura configuraciones fallo: " + e);
            }

            String fechaHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            // Calcular saldo pendiente DESPUES de esta entrega (pendiente original - cantidad entregada)
            double pendienteRestante = calcularPendienteRestante(orden, items);
            double totalEntregado = 0.0;
            for (ItemEntrega it : items) totalEntregado += it.cantidad;

            byte[] data = construirTicket(
                    nombreNegocio, nit, direccion, telefono,
                    idEntregaCab, orden, items,
                    nombreUsuario, nombreBodega, fechaHora,
                    totalEntregado, pendienteRestante);

            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(data, flavor, null);
            DocPrintJob job = service.createPrintJob();
            job.print(doc, new HashPrintRequestAttributeSet());
        } catch (Exception ex) {
            System.out.println("Comprobante entrega: error imprimiendo: " + ex);
        }
    }

    private static double calcularPendienteRestante(OrdenInfo orden, List<ItemEntrega> items) {
        // pendiente total inicial - lo que se entrego ahora
        double pendInicial = orden.getTotalPendiente();
        double entregado = 0.0;
        for (ItemEntrega it : items) entregado += it.cantidad;
        double restante = pendInicial - entregado;
        return restante < 0 ? 0 : restante;
    }

    private static byte[] construirTicket(String nombreNegocio, String nit, String direccion, String telefono,
                                          int idEntregaCab, OrdenInfo orden, List<ItemEntrega> items,
                                          String nombreUsuario, String nombreBodega, String fechaHora,
                                          double totalEntregado, double pendienteRestante)
            throws UnsupportedEncodingException, IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Init
        write(out, ESC, '@');
        write(out, ESC, 'M', 0);
        write(out, ESC, 't', (byte) CODEPAGE_N);
        write(out, ESC, 'a', 0);
        write(out, ESC, 'E', 0);
        write(out, GS, '!', 0);

        // ===== Encabezado negocio =====
        center(out);
        bold(out, true);
        line(out, nombreNegocio);
        bold(out, false);
        line(out, "NIT: " + nit);
        line(out, direccion);
        line(out, "Tel.: " + telefono);
        feed(out, 1);

        // ===== Titulo del comprobante =====
        center(out);
        bold(out, true);
        line(out, "COMPROBANTE DE ENTREGA");
        bold(out, false);
        feed(out, 1);

        // ===== Datos de la entrega =====
        left(out);
        bold(out, true);
        line(out, "ID Entrega: " + idEntregaCab);
        line(out, "ID Orden:   " + orden.idFactura);
        bold(out, false);
        line(out, "# Factura:  " + nullSafe(orden.codigoFactura));
        line(out, "Fecha:      " + fechaHora);
        line(out, "Bodeguero:  " + nullSafe(nombreUsuario));
        line(out, "Bodega:     " + nullSafe(nombreBodega));
        sep(out);

        // ===== Cliente =====
        line(out, "Cliente: " + nullSafe(orden.nombreCliente));
        line(out, "CC/NIT:  " + nullSafe(orden.cedulaCliente));
        if (orden.observacion != null && !orden.observacion.trim().isEmpty()) {
            ArrayList<String> obs = wrap("Observacion: " + orden.observacion, LINE_CHARS);
            for (String l : obs) line(out, l);
        }
        sep(out);

        // ===== Detalle entregado =====
        bold(out, true);
        line(out, columnas("CODIGO", "DESCRIPCION", "CANT."));
        bold(out, false);
        sep(out);

        write(out, ESC, '3', (byte) 50); // interlineado moderado
        for (ItemEntrega it : items) {
            appendItem(out, it.codigo, it.descripcion, fmt(it.cantidad));
            feed(out, 1);
        }
        write(out, ESC, '2'); // restaurar interlineado

        sep(out);

        // ===== Totales =====
        right(out);
        bold(out, true);
        line(out, "TOTAL ENTREGADO: " + fmt(totalEntregado));
        bold(out, false);
        left(out);

        // ===== Saldo pendiente =====
        feed(out, 1);
        if (pendienteRestante > 0) {
            bold(out, true);
            line(out, "Saldo pendiente: " + fmt(pendienteRestante) + " unid.");
            bold(out, false);
        } else {
            bold(out, true);
            line(out, "*** ORDEN ENTREGADA EN SU TOTALIDAD ***");
            bold(out, false);
        }

        // ===== Firma =====
        feed(out, 2);
        center(out);
        line(out, "________________________________________");
        line(out, "Firma quien recibe");
        feed(out, 3);

        cut(out);
        return out.toByteArray();
    }

    // =====================================================================
    // ESC/POS helpers (autocontenidos)
    // =====================================================================

    private static void appendItem(ByteArrayOutputStream out, String codigo, String descripcion, String cantidad)
            throws UnsupportedEncodingException, IOException {
        codigo = nullSafe(codigo);
        descripcion = nullSafe(descripcion);
        cantidad = nullSafe(cantidad);

        if (descripcion.length() <= COL_DESC) {
            line(out, padRight(crop(codigo, COL_CODIGO), COL_CODIGO)
                    + padRight(descripcion, COL_DESC)
                    + padLeft(crop(cantidad, COL_CANT), COL_CANT));
        } else {
            ArrayList<String> wr = wrap(descripcion, COL_DESC);
            line(out, padRight(crop(codigo, COL_CODIGO), COL_CODIGO)
                    + padRight(wr.get(0), COL_DESC)
                    + padLeft(crop(cantidad, COL_CANT), COL_CANT));
            for (int i = 1; i < wr.size(); i++) {
                line(out, repeat(' ', COL_CODIGO) + padRight(wr.get(i), COL_DESC));
            }
        }
    }

    private static String columnas(String a, String b, String c) {
        return padRight(crop(a, COL_CODIGO), COL_CODIGO)
                + padRight(crop(b, COL_DESC), COL_DESC)
                + padLeft(crop(c, COL_CANT), COL_CANT);
    }

    private static ArrayList<String> wrap(String text, int width) {
        ArrayList<String> lines = new ArrayList<>();
        if (text == null) { lines.add(""); return lines; }
        text = text.replace("\r", "");
        int idx = 0;
        while (idx < text.length()) {
            int end = Math.min(idx + width, text.length());
            int cut = end;
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end - 1);
                if (lastSpace >= idx && (end - lastSpace) < 10) cut = lastSpace;
            }
            if (cut <= idx) cut = end;
            lines.add(text.substring(idx, cut).trim());
            idx = (text.charAt(Math.min(cut, text.length() - 1)) == ' ') ? cut + 1 : cut;
        }
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.format(java.util.Locale.US, "%.2f", d);
    }

    private static String crop(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
    private static String padRight(String s, int n) {
        if (s == null) s = "";
        int len = s.length();
        return (len >= n) ? s : s + repeat(' ', n - len);
    }
    private static String padLeft(String s, int n) {
        if (s == null) s = "";
        int len = s.length();
        if (len >= n) return s.substring(len - n);
        return repeat(' ', n - len) + s;
    }
    private static String repeat(char c, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
    private static String nullSafe(String s) { return s == null ? "" : s; }

    private static void line(ByteArrayOutputStream out, String s) throws UnsupportedEncodingException, IOException {
        text(out, s); lf(out);
    }
    private static void text(ByteArrayOutputStream out, String s) throws UnsupportedEncodingException, IOException {
        if (s == null) s = "";
        out.write(s.getBytes(ENCODING));
    }
    private static void lf(ByteArrayOutputStream out) { out.write('\n'); }
    private static void feed(ByteArrayOutputStream out, int n) { for (int i = 0; i < n; i++) out.write('\n'); }
    private static void sep(ByteArrayOutputStream out) throws UnsupportedEncodingException, IOException {
        line(out, repeat('_', LINE_CHARS));
    }
    private static void center(ByteArrayOutputStream out) { write(out, ESC, 'a', 1); }
    private static void left(ByteArrayOutputStream out)   { write(out, ESC, 'a', 0); }
    private static void right(ByteArrayOutputStream out)  { write(out, ESC, 'a', 2); }
    private static void bold(ByteArrayOutputStream out, boolean on) { write(out, ESC, 'E', (byte) (on ? 1 : 0)); }
    private static void cut(ByteArrayOutputStream out) {
        feed(out, 4);
        write(out, GS, 'V', 0);
    }
    private static void write(ByteArrayOutputStream out, int... bytes) {
        for (int b : bytes) out.write((byte) b);
    }

}
