/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Creditos;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author M-Work
 */
public class EstiloTablasHeaderPequeno extends DefaultTableCellRenderer {

    public EstiloTablasHeaderPequeno() {
        setHorizontalAlignment(JLabel.LEFT);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setFont(new Font("Roboto", Font.BOLD, 14)); // Fuente moderna, negrita y tamaño 16
        setBackground(new Color(33, 150, 243)); // Color azul Material Design
        setForeground(Color.BLACK);             // Texto blanco
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(25, 118, 210)), // Sombra sutil en la parte inferior
                BorderFactory.createEmptyBorder(0, 10, 0, 0) // Espacio (padding) a la izquierda
        ));

        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = 38; // Altura del encabezado estilo Material Design
        return d;
    }
    
    
}
