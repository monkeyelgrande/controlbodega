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
public class CellRendererBalanceProductos extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String valor = ""+ value;
        if (!table.isRowSelected(row)) {
            if (column == 6) {
                valor = metodos.EliminaCaracteres(valor, ",");
                double num = Double.parseDouble(valor);
                if (num <= 0) {
                    c.setBackground(Color.red);
                }
            } else {
                c.setBackground(table.getBackground());
            }
        }
        return c;
    }

}
