/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos;

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
public class CellRendererAbonoATotal extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, column);

        // Por seguridad, valida que no sea null
        if (value == null) {
            return c;
        }
        String valor = value.toString();

        // 1. Si la celda está seleccionada, personalizamos el color de selección
        if (isSelected) {
            // Si es la columna 8 y el texto contiene "Abonar", forzamos verde con letra negra
            if (column == 8 && valor.contains("Abonar")) {
                c.setBackground(Color.GREEN);
                c.setForeground(Color.BLACK);
            } else {
                // En caso contrario, usa los colores de selección predeterminados de la tabla
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            }
        } // 2. Si la celda NO está seleccionada, aplicamos tu lógica actual
        else {
            if (column == 2) {
                // Palabra_a_Color() es tu método para asignar color
                Color col = MetodosCreditos.Palabra_a_Color(valor);
                c.setBackground(col);
                c.setForeground(table.getForeground());
            } else if (column == 8 && valor.contains("Abonar")) {
                // Fondo verde para "Abonar"
                c.setBackground(Color.GREEN);
                c.setForeground(Color.BLACK);
            } else {
                // Colores normales si no cae en las condiciones anteriores
                c.setBackground(table.getBackground());
                c.setForeground(table.getForeground());
            }
        }

        return c;
    }

}
