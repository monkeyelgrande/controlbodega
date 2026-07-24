/*
 * Modulo Caja: captura de imagen desde webcam para adjuntar a ingresos/egresos.
 * Portado desde cajadiaria; usa la DB_consultas_R_D real de bodega.
 */
package Caja;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import conexiondb.DB_consultas_R_D;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author M-Work
 */
public class CapturaMejorada {

    private Webcam webcamActiva;
    private WebcamPanel panelCamara;
    private JPanel panelContenedorCamara;

    /**
     * Lanza una ventana de diálogo para capturar una imagen desde una webcam.
     *
     * @param idUsuario El ID del usuario (proveniente de lbl_id.getText()).
     * @param tableModel El modelo de la tabla a actualizar (modelo_fotos).
     * @param parentComponent El componente (JInternalFrame) que llama al diálogo.
     */
    public void lanzar_camara(String idUsuario, DefaultTableModel tableModel, Component parentComponent) {

        List<Webcam> webcams = Webcam.getWebcams();
        if (webcams.isEmpty()) {
            JOptionPane.showMessageDialog(parentComponent, "No se encontró ninguna cámara web.", "Error de Cámara", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Window parentWindow = SwingUtilities.getWindowAncestor(parentComponent);
        JDialog dialog;
        if (parentWindow instanceof Frame) {
            dialog = new JDialog((Frame) parentWindow, "CAPTURAR IMAGEN", true);
        } else {
            dialog = new JDialog((Dialog) parentWindow, "CAPTURAR IMAGEN", true);
        }

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // --- Interfaz gráfica ---
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel panelSelector = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelSelector.add(new JLabel("Seleccionar cámara:"));
        JComboBox<Webcam> comboBoxCamaras = new JComboBox<>(webcams.toArray(new Webcam[0]));
        panelSelector.add(comboBoxCamaras);
        panelContenedorCamara = new JPanel(new BorderLayout());
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCaptura = new JButton("Capturar");
        JButton btnCancelar = new JButton("Cancelar");
        panelBotones.add(btnCaptura);
        panelBotones.add(btnCancelar);
        panelPrincipal.add(panelSelector, BorderLayout.NORTH);
        panelPrincipal.add(panelContenedorCamara, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);
        dialog.add(panelPrincipal);

        // --- Lógica de la cámara ---
        final Runnable cambiarCamara = () -> {
            if (webcamActiva != null && webcamActiva.isOpen()) {
                webcamActiva.close();
            }
            webcamActiva = (Webcam) comboBoxCamaras.getSelectedItem();
            Dimension hdSize = WebcamResolution.HD720.getSize();
            webcamActiva.setCustomViewSizes(new Dimension[]{hdSize});
            webcamActiva.setViewSize(hdSize);
            webcamActiva.open();
            panelCamara = new WebcamPanel(webcamActiva, false);
            panelCamara.setFPSDisplayed(true);
            panelCamara.start();
            panelContenedorCamara.removeAll();
            panelContenedorCamara.add(panelCamara, BorderLayout.CENTER);
            panelContenedorCamara.revalidate();
            panelContenedorCamara.repaint();
            dialog.pack();
        };
        comboBoxCamaras.addActionListener(e -> cambiarCamara.run());
        cambiarCamara.run();

        // --- Listener del botón Capturar con la lógica de guardado ---
        btnCaptura.addActionListener(e -> {
            BufferedImage imagen = webcamActiva.getImage();
            btnCaptura.setEnabled(false);
            btnCancelar.setEnabled(false);

            // 1. Crear el directorio si no existe
            File directorio = new File("c:/temp_camera/");
            if (!directorio.exists()) {
                boolean creado = directorio.mkdirs();
                if (!creado) {
                    JOptionPane.showMessageDialog(dialog, "No se pudo crear el directorio c:/temp_camera/.\nVerifique los permisos.", "Error de Directorio", JOptionPane.ERROR_MESSAGE);
                    webcamActiva.close();
                    dialog.dispose();
                    return;
                }
            }

            // 2. Construir el nombre del archivo
            String nombreArchivo = idUsuario + "_" + DB_consultas_R_D.obtener_fecha() + "_" + DB_consultas_R_D.obtener_hora_con_guiones() + "_camara.png";
            File archivoImagen = new File(directorio, nombreArchivo);

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws IOException {
                    // Guardar la imagen en un hilo de fondo
                    ImageIO.write(imagen, "PNG", archivoImagen);
                    return archivoImagen.getAbsolutePath();
                }

                @Override
                protected void done() {
                    try {
                        String rutaAbsoluta = get();
                        System.out.println("Imagen guardada exitosamente en: " + rutaAbsoluta);

                        // Actualizar el modelo de la tabla (hilo de la UI)
                        tableModel.addRow(new Object[]{rutaAbsoluta, archivoImagen.getName(), 0});

                        JOptionPane.showMessageDialog(dialog, "Imagen guardada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "Error al guardar la imagen: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    } finally {
                        // Cerrar la cámara y el diálogo independientemente del resultado
                        webcamActiva.close();
                        dialog.dispose();
                    }
                }
            }.execute();
        });

        btnCancelar.addActionListener(e -> {
            webcamActiva.close();
            dialog.dispose();
        });

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (webcamActiva != null && webcamActiva.isOpen()) {
                    webcamActiva.close();
                }
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(parentComponent);
        dialog.setVisible(true);
    }
}
