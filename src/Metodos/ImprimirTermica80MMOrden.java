package Metodos;

import Formularios.frm_main;
import conexiondb.DB_consultas_R_D;
import java.awt.print.PrinterException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import modelos.Contactos;
import modelos.ProductoImprimirOrden;

public class ImprimirTermica80MMOrden {

    String nombre_negocio, nit, direccion, telefono, no_orden, codigo_orden, fecha_hora,
            nombre_vendedor, total_articulos, tipo, observacion;
    String concepto;
    Contactos cliente;
    ArrayList<ProductoImprimirOrden> productos;

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    // === Configuración ESC/POS ===
    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;
    private static final int LINE_CHARS = 48;

    // Distribución de columnas: Código(10) | Descripción(30) | Cant.(8) = 48
    private static final int COL_CODIGO = 10;
    private static final int COL_DESC = 30;
    private static final int COL_CANT = 8;

    private static final String ENCODING = "windows-1252";
    private static final int CODEPAGE_N = 16;

    public ImprimirTermica80MMOrden(String fecha_hora, String no_orden, String codigo_orden,
            String nombre_vendedor, String total_articulos,
            Contactos cliente, ArrayList<ProductoImprimirOrden> productos,
            String tipo, String observacion) {

        this.fecha_hora = fecha_hora;
        this.no_orden = no_orden;
        this.codigo_orden = codigo_orden;
        this.nombre_vendedor = nombre_vendedor;
        this.total_articulos = total_articulos;
        this.cliente = cliente;
        this.productos = productos;
        this.tipo = tipo;
        this.observacion = observacion;

        // Carga de datos de negocio
        String cadena = "select * from configuraciones";
        ResultSet rs = DB_consultas_R_D.getTabla(cadena);
        try {
            while (rs.next()) {
                this.nombre_negocio = rs.getString("nombre_negocio");
                this.nit = rs.getString("nit_negocio");
                this.telefono = rs.getString("contacto_negocio");
                this.direccion = rs.getString("direccion");
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static PrintService buscarImpresoraPorNombre(String nombre) {
        PrintService service = null;
        PrintService[] services = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService ps : services) {
            if (ps.getName().contains(nombre)) {
                service = ps;
                break;
            }
        }
        return service;
    }

    public void imprime() throws PrinterException {
        imprime(buscarImpresoraPorNombre(frm_main.impresora_ticket));
    }

    public void imprime(PrintService service) throws PrinterException {
        if (service == null) {
            System.out.println("No se encontró la impresora con el nombre especificado.");
            return;
        }

        try {
            byte[] data = buildTicketBytes();
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(data, flavor, null);
            DocPrintJob job = service.createPrintJob();
            job.print(doc, new HashPrintRequestAttributeSet());
        } catch (Exception ex) {
            throw new PrinterException("Error imprimiendo ESC/POS: " + ex.getMessage());
        }
    }

    private byte[] buildTicketBytes() throws UnsupportedEncodingException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Init
        write(out, ESC, '@');
        write(out, ESC, 'M', 0);
        write(out, ESC, 't', (byte) CODEPAGE_N);
        write(out, ESC, 'a', 0);
        write(out, ESC, 'E', 0);
        write(out, GS, '!', 0);

        // ======= Encabezado =======
        center(out);
        bold(out, true);
        line(out, nombre_negocio);
        bold(out, false);

        center(out);
        line(out, "NIT: " + nullSafe(nit));
        line(out, nullSafe(direccion));
        line(out, "Tel. bodega: " + nullSafe(telefono));
        feed(out, 1);

        left(out);
        bold(out, true);
        line(out, "ID orden: " + nullSafe(no_orden));
        line(out, "# FACTURA: " + nullSafe(codigo_orden));
        bold(out, false);
        line(out, "FECHA: " + nullSafe(fecha_hora));

        if (concepto != null && !concepto.trim().isEmpty()) {
            ArrayList<String> lineasConcepto = wrap("Concepto: " + concepto, LINE_CHARS);
            for (String lc : lineasConcepto) {
                line(out, lc);
            }
        }

        sep(out);

        // Cliente
        line(out, "Nombre Cliente: " + nullSafe(cliente != null ? cliente.getNombre() : ""));
        line(out, "CC/NIT: " + nullSafe(cliente != null ? cliente.getCedula() : ""));
        line(out, nullSafe(tipo));

        sep(out);

        // Encabezado detalles
        bold(out, true);
        line(out, columnas("CODIGO", "DESCRIPCION", "CANT."));
        bold(out, false);
        sep(out);

        // Interlineado moderado para productos. La separación visible entre
        // ítems se logra con un feed(1) explícito después de cada producto;
        // este interlineado afecta sólo a continuaciones de descripción larga.
        write(out, ESC, '3', (byte) 50);

        // Productos agrupados por bodega + sección "sin bodega" para novedades
        double totalCantidad = 0.0;
        if (productos != null) {
            // 1) Productos con bodega, agrupados por nombre de bodega
            java.util.LinkedHashMap<String, java.util.List<ProductoImprimirOrden>> porBodega
                    = new java.util.LinkedHashMap<>();
            java.util.List<ProductoImprimirOrden> sinBodega = new java.util.ArrayList<>();

            for (ProductoImprimirOrden p : productos) {
                String b = p.getBodega();
                if (b == null || b.trim().isEmpty()) {
                    sinBodega.add(p);
                } else {
                    java.util.List<ProductoImprimirOrden> ls = porBodega.get(b);
                    if (ls == null) {
                        ls = new java.util.ArrayList<>();
                        porBodega.put(b, ls);
                    }
                    ls.add(p);
                }
            }

            // Imprimir cada bodega
            boolean primeraSeccion = true;
            for (java.util.Map.Entry<String, java.util.List<ProductoImprimirOrden>> e : porBodega.entrySet()) {
                if (!primeraSeccion) {
                    feed(out, 1);
                }
                primeraSeccion = false;
                center(out);
                bold(out, true);
                line(out, ">> BODEGA: " + e.getKey().toUpperCase() + " <<");
                bold(out, false);
                left(out);
                for (ProductoImprimirOrden p : e.getValue()) {
                    appendProducto(out, p);
                    feed(out, 1); // salto de línea entre productos
                    try {
                        totalCantidad += Double.parseDouble(p.getCantidad());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Sección de productos sin bodega (novedades de wo-printer)
            if (!sinBodega.isEmpty()) {
                if (!primeraSeccion) {
                    feed(out, 1);
                }
                center(out);
                bold(out, true);
                line(out, ">> PRODUCTOS SIN BODEGA <<");
                bold(out, false);
                left(out);
                for (ProductoImprimirOrden p : sinBodega) {
                    appendProducto(out, p);
                    feed(out, 1); // salto de línea entre productos
                    try {
                        totalCantidad += Double.parseDouble(p.getCantidad());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // Restaurar interlineado por defecto (ESC 2)
        write(out, ESC, '2');

        sep(out);

        // Total de artículos (ahora calculado correctamente)
        right(out);
        bold(out, true);
        line(out, "Total de articulos: " + String.format("%.1f", totalCantidad));
        bold(out, false);
        left(out);

        // Observación
        if (observacion != null && !observacion.trim().isEmpty()) {
            feed(out, 1);
            line(out, "Observacion:");
            ArrayList<String> lineasObs = wrap(observacion, LINE_CHARS);
            for (String lineaObs : lineasObs) {
                line(out, lineaObs);
            }
        }

        feed(out, 1);
        line(out, "USER: " + nullSafe(nombre_vendedor));

        feed(out, 2);
        center(out);
        line(out, "________________________________________");
        line(out, "Nombre Cliente");
        feed(out, 3);

        // Corte
        cut(out);

        return out.toByteArray();
    }

    private void appendProducto(ByteArrayOutputStream out, ProductoImprimirOrden p) throws UnsupportedEncodingException, IOException {
        String codigo = nullSafe(p.getCodigo());
        String descripcion = nullSafe(p.getDescripcion());
        String cantidad = nullSafe(p.getCantidad());

        // Si la descripción cabe en una línea, imprimir todo en una línea
        if (descripcion.length() <= COL_DESC) {
            String linea = padRight(crop(codigo, COL_CODIGO), COL_CODIGO)
                    + padRight(descripcion, COL_DESC)
                    + padLeft(crop(cantidad, COL_CANT), COL_CANT);
            line(out, linea);
        } else {
            // Dividir la descripción en múltiples líneas
            ArrayList<String> lineasDesc = wrap(descripcion, COL_DESC);

            // Primera línea: código + primera parte descripción + cantidad
            String primeraLinea = padRight(crop(codigo, COL_CODIGO), COL_CODIGO)
                    + padRight(lineasDesc.get(0), COL_DESC)
                    + padLeft(crop(cantidad, COL_CANT), COL_CANT);
            line(out, primeraLinea);

            // Líneas siguientes: solo continuar la descripción en su columna
            for (int i = 1; i < lineasDesc.size(); i++) {
                String lineaContinuacion = repeat(' ', COL_CODIGO)
                        + padRight(lineasDesc.get(i), COL_DESC);
                line(out, lineaContinuacion);
            }
        }
    }

    private String columnas(String a, String b, String c) {
        return padRight(crop(a, COL_CODIGO), COL_CODIGO)
                + padRight(crop(b, COL_DESC), COL_DESC)
                + padLeft(crop(c, COL_CANT), COL_CANT);
    }

    private static ArrayList<String> wrap(String text, int width) {
        ArrayList<String> lines = new ArrayList<>();
        if (text == null) {
            lines.add("");
            return lines;
        }
        text = text.replace("\r", "");
        int idx = 0;
        while (idx < text.length()) {
            int end = Math.min(idx + width, text.length());
            int cut = end;
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end - 1);
                if (lastSpace >= idx && (end - lastSpace) < 10) {
                    cut = lastSpace;
                }
            }
            if (cut <= idx) {
                cut = end;
            }
            lines.add(text.substring(idx, cut).trim());
            idx = (text.charAt(Math.min(cut, text.length() - 1)) == ' ') ? cut + 1 : cut;
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private static String crop(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String padRight(String s, int n) {
        if (s == null) {
            s = "";
        }
        int len = s.length();
        if (len >= n) {
            return s;
        }
        return s + repeat(' ', n - len);
    }

    private static String padLeft(String s, int n) {
        if (s == null) {
            s = "";
        }
        int len = s.length();
        if (len >= n) {
            return s.substring(len - n);
        }
        return repeat(' ', n - len) + s;
    }

    private static String repeat(char c, int n) {
        if (n <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private void line(ByteArrayOutputStream out, String s) throws UnsupportedEncodingException, IOException {
        text(out, s);
        lf(out);
    }

    private void text(ByteArrayOutputStream out, String s) throws UnsupportedEncodingException, IOException {
        if (s == null) {
            s = "";
        }
        out.write(s.getBytes(ENCODING));
    }

    private void lf(ByteArrayOutputStream out) {
        out.write('\n');
    }

    private void feed(ByteArrayOutputStream out, int lines) {
        for (int i = 0; i < lines; i++) {
            out.write('\n');
        }
    }

    private void sep(ByteArrayOutputStream out) throws UnsupportedEncodingException, IOException {
        line(out, repeat('_', LINE_CHARS));
    }

    private void center(ByteArrayOutputStream out) {
        write(out, ESC, 'a', 1);
    }

    private void left(ByteArrayOutputStream out) {
        write(out, ESC, 'a', 0);
    }

    private void right(ByteArrayOutputStream out) {
        write(out, ESC, 'a', 2);
    }

    private void bold(ByteArrayOutputStream out, boolean on) {
        write(out, ESC, 'E', (byte) (on ? 1 : 0));
    }

    private void cut(ByteArrayOutputStream out) {
        feed(out, 4);
        write(out, GS, 'V', 0);
    }

    private void write(ByteArrayOutputStream out, int... bytes) {
        for (int b : bytes) {
            out.write((byte) b);
        }
    }

    private void write(ByteArrayOutputStream out, byte... bytes) {
        out.write(bytes, 0, bytes.length);
    }
}
