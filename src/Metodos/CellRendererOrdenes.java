/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Metodos;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;

/**
 * Renderer de la tabla de ordenes (frm_Ordenes). Reutiliza el coloreado de
 * CellRendererFacturas (columna Tipo orden y Estado) y, ademas, pinta la columna
 * "Bodega" (indice 6) con el color asignado a cada bodega y la columna "Imp."
 * (indice 7) como un pequeno indicador de color: azul si la orden ya fue impresa
 * por el vendedor (impreso_vendedor = 1) y rosa si aun no se ha impreso.
 *
 * @author Monkeyelgrande
 */
public class CellRendererOrdenes extends CellRendererFacturas {

    private static final int COL_BODEGA = 6;
    private static final int COL_IMPRESA = 7;

    // Azul = impresa, Rosa = sin imprimir.
    private static final Color AZUL_IMPRESA = new Color(52, 152, 219);
    private static final Color ROSA_NO_IMPRESA = new Color(255, 153, 187);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (column == COL_IMPRESA) {
            // Indicador de color sin texto; centrado.
            if (c instanceof JLabel) {
                ((JLabel) c).setText("");
                ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
            }
            if (!table.isRowSelected(row)) {
                boolean impresa = "1".equals("" + value);
                c.setBackground(impresa ? AZUL_IMPRESA : ROSA_NO_IMPRESA);
                c.setForeground(table.getForeground());
            }
            return c;
        }
        // Otras columnas vuelven a mostrar su texto (el componente se reutiliza).
        if (c instanceof JLabel) {
            ((JLabel) c).setHorizontalAlignment(JLabel.LEFT);
        }
        if (!table.isRowSelected(row)) {
            if (column == COL_BODEGA) {
                Color cb = ColoresBodega.get("" + value);
                if (cb != null) {
                    c.setBackground(cb);
                    c.setForeground(ColoresBodega.textoContraste(cb));
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
            } else {
                // El componente se reutiliza entre celdas; restaurar el color de
                // texto para que no quede el blanco/negro de una celda Bodega.
                c.setForeground(table.getForeground());
            }
        }
        return c;
    }
}
