package Precios;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Colorea la columna ESTADO del listado de ingresos del modulo Precios
 * (portado del CellRendererIngresos de productos-agroinsumos).
 *
 * @author Monkeyelgrande
 */
public class CellRendererIngresosPrecios extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String valor = "" + value;
        if (!table.isRowSelected(row)) {
            if (column == 4) {
                if (valor.equals("Recibido")) {
                    c.setBackground(Color.GREEN);
                } else if (valor.equals("Ingresado")) {
                    c.setBackground(Color.YELLOW);
                } else if (valor.equals("Precios")) {
                    c.setBackground(Color.CYAN);
                } else {
                    c.setBackground(table.getBackground());
                }
            } else {
                c.setBackground(table.getBackground());
            }
        }
        return c;
    }
}
