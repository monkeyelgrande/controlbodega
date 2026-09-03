package Creditos;

import Creditos.db.DB_Porcentajes_comision;
import Creditos.modelos.Porcentajes_comision;
import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.AuditoriaCaja;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Escala de comisiones: "cobrado hasta X días -> Y % sobre el abono".
 *
 * El reporte de comisiones lee esta tabla ordenada por días y aplica el PRIMER
 * escalón cuyo tope cubra los días que tardó el cobro; los anticipos siempre
 * toman el primero. Si la escala está vacía nadie comisiona, y el reporte lo
 * avisa.
 *
 * @author Monkeyelgrande
 */
public class frm_Porcentajes_comision extends JDialog {

    private DefaultTableModel modelo;
    private JTable tabla;
    private JTextField txtDias;
    private JTextField txtPorcentaje;
    private JLabel lblAviso;

    /** id que se está editando; 0 = alta nueva. */
    private int idEnEdicion = 0;

    public frm_Porcentajes_comision(Frame parent) {
        super(parent, "Porcentajes de comisión", true);
        construir();
        cargar();
        setLocationRelativeTo(parent);
    }

    private void construir() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(760, 560);
        setLayout(new BorderLayout());
        getContentPane().setBackground(EstiloCompras.BG_FORM);

        add(EstiloCompras.header(FontAwesome.CALCULATOR, "Porcentajes de comisión", new Runnable() {
            @Override
            public void run() {
                dispose();
            }
        }), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setBackground(EstiloCompras.BG_FORM);
        centro.setBorder(new EmptyBorder(16, 20, 10, 20));

        JLabel explicacion = new JLabel("<html>La comisión se calcula sobre el valor del abono, sin IVA. "
                + "Se aplica el <b>primer escalón</b> cuyo tope de días cubra los días que tardó el cobro "
                + "(por ejemplo: 30 días → 3%, 60 días → 2%). Los anticipos toman siempre el primer escalón.</html>");
        explicacion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        explicacion.setForeground(EstiloCompras.TEXT_SECONDARY);
        centro.add(explicacion, BorderLayout.NORTH);

        modelo = new DefaultTableModel(new Object[]{"id", "Hasta (días)", "Porcentaje"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        EstiloCompras.styleTable(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        EstiloCompras.anchoColumnas(tabla, 60, 200, 200);
        centro.add(EstiloCompras.scroll(tabla), BorderLayout.CENTER);

        // ---------- formulario de alta/edicion ----------
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        form.setBackground(EstiloCompras.BG_FORM);

        txtDias = EstiloCompras.field("Hasta cuántos días", null);
        txtDias.setPreferredSize(new Dimension(180, 38));
        txtPorcentaje = EstiloCompras.field("Porcentaje (ej: 3.5)", null);
        txtPorcentaje.setPreferredSize(new Dimension(180, 38));

        JButton btnGuardar = EstiloCompras.successBtn("Guardar", FontAwesome.SAVE);
        btnGuardar.addActionListener(e -> guardar());
        JButton btnNuevo = EstiloCompras.secondaryBtn("Nuevo", FontAwesome.PLUS);
        btnNuevo.addActionListener(e -> limpiar());
        JButton btnEliminar = EstiloCompras.dangerBtn("Eliminar", FontAwesome.TRASH);
        btnEliminar.addActionListener(e -> eliminar());

        form.add(etiqueta("Hasta (días)"));
        form.add(txtDias);
        form.add(etiqueta("Porcentaje %"));
        form.add(txtPorcentaje);
        form.add(btnGuardar);
        form.add(btnNuevo);
        form.add(btnEliminar);

        JPanel sur = new JPanel(new BorderLayout());
        sur.setBackground(EstiloCompras.BG_FORM);
        sur.setBorder(new EmptyBorder(0, 20, 16, 20));
        sur.add(form, BorderLayout.CENTER);

        lblAviso = new JLabel(" ", SwingConstants.LEFT);
        lblAviso.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAviso.setForeground(EstiloCompras.DANGER);
        lblAviso.setPreferredSize(new Dimension(0, 20));
        sur.add(lblAviso, BorderLayout.SOUTH);

        add(centro, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);

        // doble clic sobre una fila la carga para editar
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    cargarSeleccion();
                }
            }
        });

        metodos.addEscapeListenerWindowDialog(this);
    }

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(EstiloCompras.TEXT_PRIMARY);
        return l;
    }

    private void cargar() {
        modelo.setRowCount(0);
        List<Porcentajes_comision> escala = DB_Porcentajes_comision.listar();
        for (Porcentajes_comision pc : escala) {
            modelo.addRow(new Object[]{String.valueOf(pc.getId()), String.valueOf(pc.getDias()),
                metodos.formateador_un_decimal().format(pc.getPorcentaje()) + " %"});
        }
        if (escala.isEmpty()) {
            lblAviso.setText("La escala está vacía: mientras no tenga escalones nadie genera comisión.");
        } else {
            lblAviso.setText(" ");
        }
    }

    private void limpiar() {
        idEnEdicion = 0;
        txtDias.setText("");
        txtPorcentaje.setText("");
        tabla.clearSelection();
        txtDias.requestFocus();
    }

    private void cargarSeleccion() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        idEnEdicion = Integer.parseInt(modelo.getValueAt(fila, 0).toString());
        txtDias.setText(modelo.getValueAt(fila, 1).toString());
        txtPorcentaje.setText(modelo.getValueAt(fila, 2).toString().replace("%", "").trim());
    }

    private void guardar() {
        int dias;
        double porcentaje;
        try {
            dias = Integer.parseInt(txtDias.getText().trim());
        } catch (NumberFormatException e) {
            lblAviso.setText("Los días deben ser un número entero.");
            return;
        }
        try {
            porcentaje = Double.parseDouble(txtPorcentaje.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            lblAviso.setText("El porcentaje debe ser un número (use punto para los decimales).");
            return;
        }
        if (dias <= 0) {
            lblAviso.setText("Los días deben ser mayores que cero.");
            return;
        }
        if (porcentaje < 0 || porcentaje > 100) {
            lblAviso.setText("El porcentaje debe estar entre 0 y 100.");
            return;
        }
        // Dos escalones con el mismo tope hacen ambiguo el calculo.
        for (Porcentajes_comision pc : DB_Porcentajes_comision.listar()) {
            if (pc.getDias() == dias && pc.getId() != idEnEdicion) {
                lblAviso.setText("Ya existe un escalón de " + dias + " días; edítelo en vez de crear otro.");
                return;
            }
        }

        Porcentajes_comision obj = new Porcentajes_comision(idEnEdicion, dias, porcentaje);
        DB_Porcentajes_comision db = new DB_Porcentajes_comision();

        AuditoriaCaja.setOrigen("Creditos - porcentajes de comision");
        try {
            int filas = idEnEdicion > 0 ? db.Actualizar(obj) : db.Guardar(obj);
            if (filas == 0) {
                lblAviso.setText("No se guardó nada.");
                return;
            }
        } finally {
            AuditoriaCaja.limpiar();
        }

        limpiar();
        cargar();
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            lblAviso.setText("Seleccione primero el escalón que desea eliminar.");
            return;
        }
        int id = Integer.parseInt(modelo.getValueAt(fila, 0).toString());
        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el escalón de hasta " + modelo.getValueAt(fila, 1) + " días?\n\n"
                + "Las comisiones que ya se liquidaron no cambian, pero las pendientes\n"
                + "se recalcularán con la escala que quede.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }

        AuditoriaCaja.setOrigen("Creditos - porcentajes de comision (eliminar)");
        try {
            new DB_Porcentajes_comision().Eliminar(id);
        } finally {
            AuditoriaCaja.limpiar();
        }
        limpiar();
        cargar();
    }
}
