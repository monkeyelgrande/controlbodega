/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Metodos;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Monkeyelgrande
 */
public class CellRendererFacturas extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String valor = "" + value;
        if (!table.isRowSelected(row)) {
            if (column == 4) {
                if (valor.equals("Salida")) {
                    c.setBackground(Color.YELLOW);
                }
                if (valor.equals("Eliminación")) {
                    c.setBackground(Color.PINK);
                }
                if (valor.equals("Préstamo")) {
                    c.setBackground(Color.CYAN);
                }

            } else if (column == 5) {
                if (valor.equals("Pendiente")) {
                    c.setBackground(Color.RED);
                }
                if (valor.equals("Entregado")) {
                    c.setBackground(Color.GREEN);
                }
            } else {
                c.setBackground(table.getBackground());
            }
        }
        return c;
    }

}
