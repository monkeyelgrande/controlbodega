/*
 * MODIFICADO: La resolución de la imagen ha sido aumentada a 200 PPI para mayor nitidez.
 * Los elementos gráficos han sido escalados para mantener el diseño.
 */
package Caja;

import conexiondb.DB_consultas_R_D;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author mktec
 * @modifier Gemini
 */
public class ImprimirPNG_traslado {

    // --- Variables de datos del recibo ---
    private final String nombre_negocio, nit, direccion, no_factura, fecha_hora, origen, total, destino, descripcion, tipo;

    // --- Constantes de diseño para ALTA RESOLUCIÓN (200 PPI) ---
    private static final double ANCHO_CM = 8.0;
    private static final double RESOLUCION_PPI = 200.0; // Aumentado de 72 a 200 para mayor calidad

    // Tamaños escalados para 200 PPI (aprox. 2.77x el tamaño original)
    private static final int MARGEN_IZQUIERDO = 33;
    private static final int INTERLINEADO = 42;
    private static final int MAX_CARACTERES_DESCRIPCION = 30;

    // Fuentes escaladas
    private static final Font FUENTE_CABECERA = new Font("Arial", Font.BOLD, 33);
    private static final Font FUENTE_NORMAL_BOLD = new Font("Arial", Font.BOLD, 28);
    private static final Font FUENTE_NORMAL = new Font("Arial", Font.PLAIN, 28);
    private static final Font FUENTE_SEPARADOR = new Font("Arial", Font.PLAIN, 33);

    public ImprimirPNG_traslado(String fecha, String no_factura, String origen, String destino, String total, String descripcion, String tipo) {
        this.fecha_hora = fecha;
        this.no_factura = no_factura;
        this.origen = origen;
        this.destino = destino;
        this.total = total;
        this.descripcion = descripcion;
        this.tipo = tipo;

        // Carga de datos del negocio desde la DB
        String temp_nombre = "Nombre no encontrado";
        String temp_nit = "NIT no encontrado";
        String temp_direccion = "Dirección no encontrada";

        String cadena = "select * from configuraciones";
        try (ResultSet rs = DB_consultas_R_D.getTabla(cadena)) {
            if (rs.next()) {
                temp_nombre = rs.getString("nombre_negocio");
                temp_nit = rs.getString("nit_negocio");
                temp_direccion = rs.getString("direccion");
            }
        } catch (Exception e) {
            System.err.println("Error al cargar configuración del negocio: " + e.getMessage());
        }
        this.nombre_negocio = temp_nombre;
        this.nit = temp_nit;
        this.direccion = temp_direccion;
    }

    /**
     * Orquesta la creación y guardado de la imagen del recibo.
     */
    public void generarYGuardarImagen() {
        try {
            int ancho = (int) convert_CM_To_PPI(ANCHO_CM);
            int alto = calcularAlturaNecesaria();

            BufferedImage image = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            configurarLienzo(g2d, ancho, alto);
            dibujarRecibo(g2d, ancho);

            g2d.dispose();

            guardarImagenComoPNG(image);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Ocurrió un error al generar la imagen:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void configurarLienzo(Graphics2D g2d, int ancho, int alto) {
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, ancho, alto);
        g2d.setColor(Color.BLACK);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
    }

    private void dibujarRecibo(Graphics2D g2d, double ancho) {
        try {
            int y = 56; // Posición Y inicial, escalada

            // --- CABECERA ---
            y = dibujarLineaCentrada(g2d, nombre_negocio, FUENTE_CABECERA, y, ancho);
            y = dibujarLineaCentrada(g2d, nit, FUENTE_CABECERA, y, ancho);
            y = dibujarLineaCentrada(g2d, direccion, FUENTE_CABECERA, y, ancho);

            g2d.setFont(FUENTE_NORMAL_BOLD);
            g2d.drawString("_____________________________________", MARGEN_IZQUIERDO, y);
            y += INTERLINEADO;

            y = dibujarLineaCentrada(g2d, "Tipo: " + tipo, FUENTE_NORMAL_BOLD, y, ancho);
            y = dibujarLineaCentrada(g2d, "Recibo No. " + no_factura, FUENTE_NORMAL_BOLD, y, ancho);
            y = dibujarLineaCentrada(g2d, "Fecha", FUENTE_NORMAL_BOLD, y, ancho);
            y = dibujarLineaCentrada(g2d, fecha_hora, FUENTE_NORMAL, y, ancho);

            g2d.setFont(FUENTE_SEPARADOR);
            g2d.drawString("-------------------------------------------------------", MARGEN_IZQUIERDO, y);
            y += INTERLINEADO;

            // --- CUERPO ---
            g2d.setFont(FUENTE_NORMAL_BOLD);
            g2d.drawString("ORIGEN: " + origen, MARGEN_IZQUIERDO, y);
            y += INTERLINEADO;

            g2d.setFont(FUENTE_NORMAL_BOLD);
            g2d.drawString("DESTINO: " + destino, MARGEN_IZQUIERDO, y);
            y += INTERLINEADO;

            g2d.setFont(FUENTE_SEPARADOR);
            g2d.drawString("_____________________________________", MARGEN_IZQUIERDO, y);
            g2d.setFont(FUENTE_NORMAL);
            g2d.drawString("Concepto", MARGEN_IZQUIERDO, y);
            y += INTERLINEADO;

            y = dibujarTextoConAjuste(g2d, descripcion, FUENTE_NORMAL, y, MARGEN_IZQUIERDO);

            // --- PIE DE PÁGINA ---
            g2d.setFont(FUENTE_SEPARADOR);
            g2d.drawString("_____________________________________", MARGEN_IZQUIERDO, y);
            y += INTERLINEADO;

            g2d.setFont(FUENTE_CABECERA);
            g2d.drawString("TOTAL:     " + total, MARGEN_IZQUIERDO, y);
            y += INTERLINEADO;

            g2d.setFont(FUENTE_CABECERA);
            g2d.drawString("■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■", MARGEN_IZQUIERDO, y);
            y += INTERLINEADO;
            g2d.drawString("***********************************************", 10, y);

        } catch (Exception r) {
            r.printStackTrace();
        }
    }

    private int calcularAlturaNecesaria() {
        int altura = 56; // Margen superior escalado
        altura += INTERLINEADO * 10; // 10 líneas de contenido fijo (cabecera, cuerpo, pie)

        // Calcular líneas de la descripción
        int largo = descripcion.length();
        int start = 0;
        while (start < largo) {
            altura += INTERLINEADO;
            int end = Math.min(start + MAX_CARACTERES_DESCRIPCION, largo);
            if (end < largo && descripcion.charAt(end) != ' ') {
                int lastSpace = descripcion.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            start = end;
            while (start < largo && descripcion.charAt(start) == ' ') {
                start++;
            }
        }

        altura += INTERLINEADO * 6; // Pie de página y margen inferior
        return altura;
    }

    private void guardarImagenComoPNG(BufferedImage image) throws IOException {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Recibo como Imagen");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Imagen PNG (*.png)", "png"));
        fileChooser.setSelectedFile(new File("traslado_" + this.no_factura + ".png"));

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".png")) {
                fileToSave = new File(filePath + ".png");
            }
            ImageIO.write(image, "png", fileToSave);
            JOptionPane.showMessageDialog(null, "Imagen de alta resolución guardada en:\n" + fileToSave.getAbsolutePath(), "Guardado Completo", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "El guardado de la imagen fue cancelado.", "Cancelado", JOptionPane.WARNING_MESSAGE);
        }
    }

    // --- Métodos de ayuda para el dibujo ---
    private int dibujarLineaCentrada(Graphics2D g, String texto, Font font, int y, double ancho) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int x = (int) ((ancho - fm.stringWidth(texto)) / 2);
        g.drawString(texto, x, y);
        return y + INTERLINEADO;
    }

    private int dibujarTextoConAjuste(Graphics2D g, String texto, Font font, int y, int x) {
        g.setFont(font);
        int start = 0;
        int largo = texto.length();

        while (start < largo) {
            int end = Math.min(start + MAX_CARACTERES_DESCRIPCION, largo);
            if (end < largo && texto.charAt(end) != ' ') {
                int lastSpace = texto.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            g.drawString(texto.substring(start, end), x, y);
            y += INTERLINEADO;
            start = end;
            while (start < largo && texto.charAt(start) == ' ') {
                start++;
            }
        }
        return y;
    }

    // --- Métodos de conversión ---
    protected static double convert_CM_To_PPI(double cm) {
        return toPPI(cm * 0.393600787);
    }

    protected static double toPPI(double inch) {
        return inch * RESOLUCION_PPI; // Usa la constante de alta resolución
    }
}
