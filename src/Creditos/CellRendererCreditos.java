/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Monkeyelgrande
 */
public class CellRendererCreditos extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String valor = (String) value;
        
        if (!table.isRowSelected(row)) {
            if (column == 12) {
                if (valor.equals("Pagado")) {
                    c.setBackground(Color.green);
                }
                if (valor.equals("Vencido")) {
                    c.setBackground(Color.RED);
                    c.setForeground(Color.WHITE);
                    
                }
                if (valor.equals("Pendiente")) {
                    c.setBackground(Color.YELLOW);
                }
            } else {
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }
        }
        return c;
    }

}
