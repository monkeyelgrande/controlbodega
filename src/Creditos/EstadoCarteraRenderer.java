/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Creditos;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.*;
import java.awt.*;

public class EstadoCarteraRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Obtener el valor de la primera columna para identificar el tipo de fila
        Object col0 = table.getValueAt(row, 0);
        Object col3 = table.getValueAt(row, 3);

        String col0Str = (col0 != null) ? col0.toString() : "";
        String col3Str = (col3 != null) ? col3.toString() : "";

        // Resetear formato por defecto
        setFont(table.getFont());
        setBackground(Color.WHITE);
        setForeground(Color.BLACK);

        // Detectar encabezados de cliente (primera columna = "ID" y tercera = "CELULAR")
        if ("ID".equals(col0Str) && "CELULAR".equals(col3Str)) {
            setBackground(new Color(135, 206, 250)); // Azul claro
            setFont(table.getFont().deriveFont(Font.BOLD));
            setForeground(Color.BLACK);
        } // Detectar encabezados de créditos (primera columna = "ID" y cuarta = "F. VENCIMIENTO")
        else if ("ID".equals(col0Str) && "F. VENCIMIENTO".equals(col3Str)) {
            setBackground(new Color(255, 200, 100)); // Naranja claro
            setFont(table.getFont().deriveFont(Font.BOLD));
            setForeground(Color.BLACK);
        } // Detectar fila de total (cuarta columna = "TOTAL")
        else if ("TOTAL".equals(col3Str)) {
            setFont(table.getFont().deriveFont(Font.BOLD));
            setBackground(new Color(220, 220, 220)); // Gris claro
            setForeground(Color.BLACK);
        } // Filas normales - mantener fondo blanco
        else {
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
        }

        // Si la fila está seleccionada, usar color de selección
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        }

        return c;
    }
}
