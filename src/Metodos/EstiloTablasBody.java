/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Metodos;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *

*/
public class EstiloTablasBody extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        c.setFont(new Font("Roboto", Font.PLAIN, 14)); // Fuente estilo Material Design

        if (isSelected) {
            c.setBackground(new Color(239, 83, 80)); // Fondo verde para selección
            c.setForeground(Color.WHITE); // Texto blanco para selección
        } else {
            if (row % 2 == 0) {
                c.setBackground(new Color(240, 240, 240)); // Fondo gris claro para filas pares
            } else {
                c.setBackground(new Color(255, 255, 255)); // Fondo blanco para filas impares
            }
            c.setForeground(new Color(33, 33, 33)); // Texto gris oscuro para filas normales
        }
            ((JComponent) c).setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        return c;
    }

}
