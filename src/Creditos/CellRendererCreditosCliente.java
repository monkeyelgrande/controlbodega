/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos;

import Metodos.metodos;
import Formularios.frm_main;
import conexiondb.DB_consultas_R_D;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Monkeyelgrande
 */
public class CellRendererCreditosCliente extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String valor = (String) value;
        Color col = MetodosCreditos.Palabra_a_Color(valor);
        if (!table.isRowSelected(row)) {

            if (column == 2) {
                c.setBackground(col);
            } else if (column == 6) {
                // credito totalmente pagado: saldo (columna 7) en cero
                try {
                    if ("CREDITO".equals(table.getValueAt(row, 2).toString())
                            && Double.parseDouble(metodos.EliminaCaracteres(table.getValueAt(row, 7).toString(), ".")) == 0) {
                        c.setBackground(Color.green);
                    }
                } catch (Exception e) {
                }
            } else {
                if (row % 2 == 0) {
                    c.setBackground(new Color(240, 240, 240)); // Fondo gris claro para filas pares
                } else {
                    c.setBackground(new Color(255, 255, 255)); // Fondo blanco para filas impares
                }
                c.setForeground(new Color(33, 33, 33)); // Texto gris oscuro para filas normales
            }
        }

        return c;
    }

}
