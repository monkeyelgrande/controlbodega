/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos;

import Metodos.metodos;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 *
 * @author AdminOmarGuevara
 */
public class ExportarExcelCreditos {

    public void exportarExcel(JTable t) throws IOException {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Archivos de excel", "xls");
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("Guardar archivo");
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            String ruta = chooser.getSelectedFile().toString().concat(".xls");
            try {
                File archivoXLS = new File(ruta);
                if (archivoXLS.exists()) {
                    archivoXLS.delete();
                }
                archivoXLS.createNewFile();
                Workbook libro = new HSSFWorkbook();
                FileOutputStream archivo = new FileOutputStream(archivoXLS);
                Sheet hoja = libro.createSheet("Mi hoja de trabajo 1");
                hoja.setDisplayGridlines(false);
                for (int f = 0; f < t.getRowCount(); f++) {
                    Row fila = hoja.createRow(f);
                    for (int c = 0; c < t.getColumnCount(); c++) {
                        Cell celda = fila.createCell(c);
                        if (f == 0) {
                            celda.setCellValue(t.getColumnName(c));
                        }
                    }
                }
                int filaInicio = 1;
                int filas = t.getRowCount();
                int columnas = t.getColumnCount();

                for (int f = 0; f < filas; f++) {
                    Row fila = hoja.createRow(filaInicio++);
                    for (int c = 0; c < columnas; c++) {
                        Cell celda = fila.createCell(c);
                        Object val = t.getValueAt(f, c);

                        if (val == null) {
                            celda.setCellValue("");
                            continue;
                        }

                        // Si ya es número, escribir como numérico
                        if (val instanceof Number) {
                            celda.setCellValue(((Number) val).doubleValue());
                            continue;
                        }

                        // Si es texto
                        String texto = String.valueOf(val).trim();

                        // Caso especial que tenías para la columna 5 (índice 4)
                        if (c == 4) {
                            try {
                                // Si usas tu método para limpiar miles con punto
                                double num = Double.parseDouble(metodos.EliminaCaracteres(texto, "."));
                                celda.setCellValue(num);
                                continue;
                            } catch (Exception ignore) {
                                // Si no puede parsear, lo pone como texto
                            }
                        }

                        // Intento genérico: si el texto parece número, conviértelo
                        try {
                            // Limpia símbolos comunes de moneda/espacios no separadores
                            String limpio = texto.replaceAll("[^0-9,.-]", "");
                            // Soporte a formato con coma decimal
                            if (limpio.contains(",") && !limpio.contains(".")) {
                                limpio = limpio.replace(",", ".");
                            } else if (limpio.chars().filter(ch -> ch == ',').count() > 1) {
                                limpio = limpio.replace(".", "").replace(",", ".");
                            }
                            double num = Double.parseDouble(limpio);
                            celda.setCellValue(num);
                        } catch (Exception e) {
                            celda.setCellValue(texto);
                        }
                    }
                }
                libro.write(archivo);
                archivo.close();
                Desktop.getDesktop().open(archivoXLS);
            } catch (IOException | NumberFormatException e) {
                throw e;
            }
        }
    }

}
