package Creditos;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;

public class ExportarExcelEstadoCartera {

    public void exportarExcel(JTable t) throws IOException {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Archivos de excel", "xls");
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Guardar archivo");
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            String ruta = chooser.getSelectedFile().toString();
            if (!ruta.toLowerCase().endsWith(".xls")) {
                ruta = ruta.concat(".xls");
            }

            try {
                File archivoXLS = new File(ruta);
                if (archivoXLS.exists()) {
                    archivoXLS.delete();
                }
                archivoXLS.createNewFile();

                Workbook libro = new HSSFWorkbook();
                FileOutputStream archivo = new FileOutputStream(archivoXLS);
                Sheet hoja = libro.createSheet("Estado de Cartera");
                hoja.setDisplayGridlines(false);

                // Crear estilos
                CellStyle estiloHeaderCliente = crearEstiloHeaderCliente(libro);
                CellStyle estiloHeaderCreditos = crearEstiloHeaderCreditos(libro);
                CellStyle estiloTotal = crearEstiloTotal(libro);
                CellStyle estiloNormal = crearEstiloNormal(libro);
                CellStyle estiloNumerico = crearEstiloNumerico(libro);

                // Recorrer filas de la tabla
                int filaExcel = 0;
                for (int f = 0; f < t.getRowCount(); f++) {
                    Row fila = hoja.createRow(filaExcel++);

                    // Obtener valores para detectar tipo de fila
                    Object col0 = t.getValueAt(f, 0);
                    Object col1 = t.getValueAt(f, 1);
                    Object col3 = t.getValueAt(f, 3);

                    String col0Str = col0 != null ? col0.toString() : "";
                    String col1Str = col1 != null ? col1.toString() : "";
                    String col3Str = col3 != null ? col3.toString() : "";

                    // Detectar tipo de fila
                    boolean esHeaderCliente = "ID".equals(col0Str) && "NOMBRE".equals(col1Str);
                    boolean esDatosCliente = !col0Str.isEmpty() && !col0Str.equals("ID")
                            && !col1Str.isEmpty() && !col1Str.equals("CODIGO")
                            && col3Str.isEmpty();
                    boolean esHeaderCreditos = "ID".equals(col0Str) && "CODIGO".equals(col1Str);
                    boolean esTotal = "TOTAL".equals(col3Str);
                    boolean esFilaCredito = !esHeaderCliente && !esDatosCliente && !esHeaderCreditos && !esTotal && !col0Str.isEmpty();

                    CellStyle estiloFila;
                    if (esHeaderCliente || esDatosCliente) {
                        estiloFila = estiloHeaderCliente;
                    } else if (esHeaderCreditos) {
                        estiloFila = estiloHeaderCreditos;
                    } else if (esTotal) {
                        estiloFila = estiloTotal;
                    } else {
                        estiloFila = estiloNormal;
                    }

                    // Crear celdas
                    int colExcel = 0;
                    for (int c = 0; c < t.getColumnCount(); c++) {
                        // Omitir columnas 2 y 3 SOLO si es header de cliente o datos de cliente
                        if ((esHeaderCliente || esDatosCliente) && (c == 2 || c == 3)) {
                            continue;
                        }

                        Cell celda = fila.createCell(colExcel++);
                        Object valor = t.getValueAt(f, c);

                        if (valor != null) {
                            String valorStr = valor.toString();

                            // Intentar convertir a número si es columna de montos (columnas 4, 5, 6)
                            if (c >= 4 && !esHeaderCliente && !esHeaderCreditos
                                    && !esDatosCliente && !valorStr.isEmpty()) {
                                try {
                                    // Remover separadores de miles y reemplazar coma decimal por punto
                                    String valorLimpio = valorStr.replace(".", "").replace(",", ".");
                                    double numero = Double.parseDouble(valorLimpio);
                                    celda.setCellValue(numero);

                                    // Aplicar estilo numérico con formato
                                    if (esTotal) {
                                        CellStyle estiloNumericoTotal = crearEstiloNumericoTotal(libro);
                                        celda.setCellStyle(estiloNumericoTotal);
                                    } else {
                                        celda.setCellStyle(estiloNumerico);
                                    }
                                    continue; // Ya aplicamos estilo, siguiente celda
                                } catch (NumberFormatException e) {
                                    // Si falla, lo dejamos como texto
                                    celda.setCellValue(valorStr);
                                }
                            } else {
                                celda.setCellValue(valorStr);
                            }
                        }

                        celda.setCellStyle(estiloFila);
                    }
                }

                // Auto-ajustar ancho de columnas
                for (int c = 0; c < 7; c++) {
                    hoja.autoSizeColumn(c);
                }

                libro.write(archivo);
                archivo.close();
                Desktop.getDesktop().open(archivoXLS);

            } catch (IOException | NumberFormatException e) {
                throw e;
            }
        }
    }

    // Métodos para crear estilos
    private CellStyle crearEstiloHeaderCliente(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font font = libro.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        estilo.setFont(font);

        // Color RGB (221, 235, 246) - Azul muy claro
        estilo.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        estilo.setFillPattern(CellStyle.SOLID_FOREGROUND);

        // Bordes
        aplicarBordes(estilo);

        return estilo;
    }

    private CellStyle crearEstiloHeaderCreditos(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font font = libro.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        estilo.setFont(font);

        // Color RGB (155, 193, 230) - Azul medio
        estilo.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        estilo.setFillPattern(CellStyle.SOLID_FOREGROUND);

        // Bordes
        aplicarBordes(estilo);

        return estilo;
    }

    private CellStyle crearEstiloTotal(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font font = libro.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        estilo.setFont(font);

        // Color RGB (246, 203, 169) - Naranja claro/durazno
        estilo.setFillForegroundColor(IndexedColors.TAN.getIndex());
        estilo.setFillPattern(CellStyle.SOLID_FOREGROUND);

        // Bordes
        aplicarBordes(estilo);

        return estilo;
    }

    private CellStyle crearEstiloNormal(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        aplicarBordes(estilo);
        return estilo;
    }

    private CellStyle crearEstiloNumerico(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        DataFormat formato = libro.createDataFormat();
        estilo.setDataFormat(formato.getFormat("#,##0.00"));
        aplicarBordes(estilo);
        return estilo;
    }

    private CellStyle crearEstiloNumericoTotal(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font font = libro.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        estilo.setFont(font);

        DataFormat formato = libro.createDataFormat();
        estilo.setDataFormat(formato.getFormat("#,##0.00"));

        // Color RGB (246, 203, 169) - Naranja claro/durazno
        estilo.setFillForegroundColor(IndexedColors.TAN.getIndex());
        estilo.setFillPattern(CellStyle.SOLID_FOREGROUND);

        aplicarBordes(estilo);
        return estilo;
    }

    private void aplicarBordes(CellStyle estilo) {
        estilo.setBorderBottom(CellStyle.BORDER_THIN);
        estilo.setBorderTop(CellStyle.BORDER_THIN);
        estilo.setBorderLeft(CellStyle.BORDER_THIN);
        estilo.setBorderRight(CellStyle.BORDER_THIN);
    }
}