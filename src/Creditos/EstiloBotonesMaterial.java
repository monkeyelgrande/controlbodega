/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Creditos;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;

/**
 *
 * @author M-Work
 */
public class EstiloBotonesMaterial extends JFrame {

    public EstiloBotonesMaterial() {
        // Crear un JButton con estilo Material Design
        JButton materialButton = new JButton("Material Button");
        materialButton.setFont(new Font("Roboto", Font.BOLD, 14));
        materialButton.setBackground(new Color(33, 150, 243)); // Azul Material Design
        materialButton.setForeground(Color.WHITE); // Texto blanco
        materialButton.setFocusPainted(false);
        materialButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Efecto de hover (opcional)
        materialButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                materialButton.setBackground(new Color(30, 136, 229)); // Azul más oscuro
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                materialButton.setBackground(new Color(33, 150, 243)); // Azul original
            }
        });

        // Configuración del JFrame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(materialButton);
        setSize(300, 200);
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
