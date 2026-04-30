/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Metodos;

import Formularios.frm_main;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Monkeyelgrande
 */
public class CellRendererIngresos extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String valor = "" + value;
        if (!table.isRowSelected(row)) {
            if (column == 5) {
                if (valor.equals("Pendiente")) {
                    c.setBackground(Color.RED);
                }
                if (valor.equals("Recibido")) {
                    c.setBackground(Color.green);
                }

            } else if (column == 9) {
                if (valor.contains("-")) {
                    c.setBackground(Color.YELLOW);
                }
                if (valor.equals("Pagado")) {
                    c.setBackground(Color.green);
                }
                if (valor.contains("+")) {
                    c.setBackground(Color.RED);
                }

            } else {
                c.setBackground(table.getBackground());
            }
        }
        return c;
    }

}
