package Creditos;

import Creditos.db.DBabonos;
import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.AuditoriaCaja;
import conexiondb.DB_consultas_R_D;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Cruce de un saldo a favor contra los créditos pendientes del mismo cliente.
 *
 * Se abre desde "Créditos del cliente" con el botón Cruzar, teniendo
 * seleccionada la fila de un pago (abono) que todavía tiene saldo sin aplicar.
 * Muestra únicamente los créditos de ese cliente con saldo pendiente; el
 * usuario marca uno o varios y el diálogo reparte el saldo disponible entre
 * ellos. El monto de cada crédito se puede ajustar a mano para cruces
 * parciales.
 *
 * El guardado va en una sola transacción (DBabonos.CruzarSaldoAFavor), que
 * revalida contra la base que no se sobregire ni el abono ni el crédito.
 *
 * @author Monkeyelgrande
 */
public class jd_cruzar_abono extends JDialog {

    /** Columnas de la tabla de créditos pendientes. */
    private static final int COL_SEL = 0;
    private static final int COL_ID = 1;
    private static final int COL_FECHA = 2;
    private static final int COL_CODIGO = 3;
    private static final int COL_TOTAL = 4;
    private static final int COL_CANCELADO = 5;
    private static final int COL_SALDO = 6;
    private static final int COL_APLICAR = 7;

    private final int idCabecera;
    private final int idContacto;
    private final double saldoDisponible;

    /** Queda en true cuando el cruce se guardó, para que el form padre refresque. */
    private boolean cruceAplicado = false;

    private DefaultTableModel modelo;
    private JTable tabla;
    private JLabel chipDisponible;
    private JLabel chipACruzar;
    private JLabel chipRestante;
    private JLabel lblAviso;

    /** Evita que el listener del modelo se dispare mientras el código lo edita. */
    private boolean recalculando = false;

    public jd_cruzar_abono(Dialog parent, int idCabecera, int idContacto, double saldoDisponible,
            String nombreCliente) {
        super(parent, "Cruzar saldo a favor", true);
        this.idCabecera = idCabecera;
        this.idContacto = idContacto;
        this.saldoDisponible = saldoDisponible;
        construir(nombreCliente);
        cargarCreditosPendientes();
        setLocationRelativeTo(parent);
    }

    public jd_cruzar_abono(Frame parent, int idCabecera, int idContacto, double saldoDisponible,
            String nombreCliente) {
        super(parent, "Cruzar saldo a favor", true);
        this.idCabecera = idCabecera;
        this.idContacto = idContacto;
        this.saldoDisponible = saldoDisponible;
        construir(nombreCliente);
        cargarCreditosPendientes();
        setLocationRelativeTo(parent);
    }

    public boolean isCruceAplicado() {
        return cruceAplicado;
    }

    // ================================================================
    // Interfaz
    // ================================================================
    private void construir(String nombreCliente) {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(980, 640);
        setLayout(new BorderLayout());
        getContentPane().setBackground(EstiloCompras.BG_FORM);

        add(EstiloCompras.header(FontAwesome.EXCHANGE, "Cruzar saldo a favor", new Runnable() {
            @Override
            public void run() {
                dispose();
            }
        }), BorderLayout.NORTH);

        // ---------- centro: tabla de créditos pendientes ----------
        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setBackground(EstiloCompras.BG_FORM);
        centro.setBorder(new EmptyBorder(16, 20, 10, 20));

        JLabel lblTitulo = new JLabel("Pago #" + idCabecera + "  ·  " + nombreCliente
                + "  ·  marque los créditos con los que desea cruzar este abono");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitulo.setForeground(EstiloCompras.TEXT_PRIMARY);
        centro.add(lblTitulo, BorderLayout.NORTH);

        modelo = new DefaultTableModel(
                new Object[]{"", "ID", "Fecha", "# Crédito", "Total", "Cancelado", "Saldo pendiente", "A aplicar"}, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == COL_SEL ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                // Solo se edita la marca y, si la fila está marcada, el monto.
                if (col == COL_SEL) {
                    return true;
                }
                return col == COL_APLICAR && Boolean.TRUE.equals(getValueAt(row, COL_SEL));
            }
        };

        tabla = new JTable(modelo);
        EstiloCompras.styleTable(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // styleTable pone el renderer de texto en TODAS las columnas: la de la
        // marca vuelve a su renderer/editor de checkbox.
        tabla.getColumnModel().getColumn(COL_SEL).setCellRenderer(tabla.getDefaultRenderer(Boolean.class));
        tabla.getColumnModel().getColumn(COL_SEL).setCellEditor(tabla.getDefaultEditor(Boolean.class));
        EstiloCompras.anchoColumnas(tabla, 40, 60, 100, 160, 110, 110, 130, 130);
        tabla.getColumnModel().getColumn(COL_SEL).setMaxWidth(50);

        modelo.addTableModelListener(e -> {
            if (recalculando) {
                return;
            }
            int fila = e.getFirstRow();
            int col = e.getColumn();
            if (fila < 0 || col < 0) {
                return;
            }
            if (col == COL_SEL) {
                repartirDesde(fila);
            } else if (col == COL_APLICAR) {
                normalizarMonto(fila);
            }
            refrescarTotales();
        });

        JScrollPane scroll = EstiloCompras.scroll(tabla);
        centro.add(scroll, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        // ---------- sur: totales + botones ----------
        JPanel sur = new JPanel(new BorderLayout());
        sur.setBackground(EstiloCompras.BG_FORM);
        sur.setBorder(new EmptyBorder(0, 20, 16, 20));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        chips.setBackground(EstiloCompras.BG_FORM);
        chipDisponible = chip("Saldo del abono", saldoDisponible, EstiloCompras.PRIMARY);
        chipACruzar = chip("A cruzar", 0, EstiloCompras.SUCCESS);
        chipRestante = chip("Quedaría a favor", saldoDisponible, EstiloCompras.TEXT_SECONDARY);
        chips.add(chipDisponible);
        chips.add(chipACruzar);
        chips.add(chipRestante);
        sur.add(chips, BorderLayout.WEST);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        botones.setBackground(EstiloCompras.BG_FORM);

        JButton btnCancelar = EstiloCompras.secondaryBtn("Cancelar", FontAwesome.CLOSE);
        btnCancelar.addActionListener(e -> dispose());

        final JButton btnAplicar = EstiloCompras.successBtn("Aplicar cruce", FontAwesome.CHECK);
        btnAplicar.addActionListener(e -> aplicarCruce(btnAplicar));

        botones.add(btnCancelar);
        botones.add(btnAplicar);
        sur.add(botones, BorderLayout.EAST);

        lblAviso = new JLabel(" ", SwingConstants.CENTER);
        lblAviso.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAviso.setForeground(EstiloCompras.DANGER);
        lblAviso.setPreferredSize(new Dimension(0, 20));
        sur.add(lblAviso, BorderLayout.SOUTH);

        add(sur, BorderLayout.SOUTH);

        metodos.addEscapeListenerWindowDialog(this);
    }

    /** Etiqueta tipo "chip": titulo pequeño arriba y el valor en grande. */
    private JLabel chip(String titulo, double valor, Color color) {
        JLabel l = new JLabel();
        l.setOpaque(true);
        l.setBackground(EstiloCompras.BG_SECTION);
        l.setForeground(color);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstiloCompras.DIVIDER),
                new EmptyBorder(6, 12, 6, 12)));
        actualizarChip(l, titulo, valor);
        return l;
    }

    private void actualizarChip(JLabel chip, String titulo, double valor) {
        chip.setText("<html><span style='font-size:9px;color:#757575'>" + titulo
                + "</span><br>$ " + metodos.formateador_dinero().format(valor) + "</html>");
    }

    // ================================================================
    // Datos
    // ================================================================
    /**
     * Carga los créditos del cliente que todavía tienen saldo pendiente. Los
     * que ya están cancelados no se muestran: no hay nada que cruzar en ellos.
     */
    private void cargarCreditosPendientes() {
        String consulta = "SELECT f.id, f.codigo, f.fecha_creacion, f.total, \n"
                + "       COALESCE(SUM(a.abono),0) AS cancelado, \n"
                + "       (f.total - COALESCE(SUM(a.abono),0)) AS saldo \n"
                + "  FROM creditos f \n"
                + "  LEFT JOIN abonos a ON a.id_credito = f.id \n"
                + " WHERE f.id_contacto = " + idContacto + " \n"
                + " GROUP BY f.id, f.codigo, f.fecha_creacion, f.total \n"
                + "HAVING (f.total - COALESCE(SUM(a.abono),0)) > 0.009 \n"
                + " ORDER BY f.fecha_creacion, f.id";

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        modelo.setRowCount(0);

        try (ResultSet rs = DB_consultas_R_D.getTabla(consulta)) {
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    Boolean.FALSE,
                    String.valueOf(rs.getInt("id")),
                    rs.getDate("fecha_creacion") == null ? "-" : sdf.format(rs.getDate("fecha_creacion")),
                    rs.getString("codigo") == null ? "-" : rs.getString("codigo"),
                    metodos.formateador_dinero().format(rs.getDouble("total")),
                    metodos.formateador_dinero().format(rs.getDouble("cancelado")),
                    metodos.formateador_dinero().format(rs.getDouble("saldo")),
                    "0"});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los créditos pendientes:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
        }

        if (modelo.getRowCount() == 0) {
            lblAviso.setText("Este cliente no tiene créditos con saldo pendiente para cruzar.");
        }
        refrescarTotales();
    }

    // ================================================================
    // Reparto del saldo
    // ================================================================
    /**
     * Al marcar un crédito se le asigna lo máximo posible: el menor valor entre
     * su saldo pendiente y lo que todavía queda libre del abono. Al desmarcarlo
     * su monto vuelve a cero.
     */
    private void repartirDesde(int fila) {
        recalculando = true;
        try {
            if (!Boolean.TRUE.equals(modelo.getValueAt(fila, COL_SEL))) {
                modelo.setValueAt("0", fila, COL_APLICAR);
                return;
            }
            double usadoPorOtras = totalACruzar() - montoDeFila(fila);
            double libre = saldoDisponible - usadoPorOtras;
            double saldoCredito = valor(modelo.getValueAt(fila, COL_SALDO));
            double aplicar = Math.min(saldoCredito, Math.max(libre, 0));
            modelo.setValueAt(metodos.formateador_dinero().format(aplicar), fila, COL_APLICAR);
        } finally {
            recalculando = false;
        }
    }

    /**
     * Recorta el monto escrito a mano para que nunca supere el saldo del
     * crédito ni lo que queda libre del abono.
     */
    private void normalizarMonto(int fila) {
        recalculando = true;
        try {
            double escrito = montoDeFila(fila);
            double saldoCredito = valor(modelo.getValueAt(fila, COL_SALDO));
            double usadoPorOtras = 0;
            for (int i = 0; i < modelo.getRowCount(); i++) {
                if (i != fila) {
                    usadoPorOtras += montoDeFila(i);
                }
            }
            double libre = saldoDisponible - usadoPorOtras;
            double corregido = Math.max(0, Math.min(escrito, Math.min(saldoCredito, libre)));
            modelo.setValueAt(metodos.formateador_dinero().format(corregido), fila, COL_APLICAR);
            if (corregido < escrito - 0.009) {
                lblAviso.setText("El monto se ajustó al máximo posible para ese crédito.");
            } else {
                lblAviso.setText(" ");
            }
        } finally {
            recalculando = false;
        }
    }

    private double totalACruzar() {
        double total = 0;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            total += montoDeFila(i);
        }
        return total;
    }

    private double montoDeFila(int fila) {
        if (!Boolean.TRUE.equals(modelo.getValueAt(fila, COL_SEL))) {
            return 0;
        }
        return valor(modelo.getValueAt(fila, COL_APLICAR));
    }

    /** Convierte un texto de dinero ("1.429.000") a número, ignorando separadores. */
    private double valor(Object texto) {
        if (texto == null) {
            return 0;
        }
        StringBuilder limpio = new StringBuilder();
        for (char c : texto.toString().toCharArray()) {
            if (Character.isDigit(c)) {
                limpio.append(c);
            }
        }
        if (limpio.length() == 0) {
            return 0;
        }
        try {
            return Double.parseDouble(limpio.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void refrescarTotales() {
        double aCruzar = totalACruzar();
        actualizarChip(chipACruzar, "A cruzar", aCruzar);
        actualizarChip(chipRestante, "Quedaría a favor", saldoDisponible - aCruzar);
    }

    // ================================================================
    // Guardado
    // ================================================================
    private void aplicarCruce(JButton btnAplicar) {
        // Si el usuario dejó el cursor dentro de una celda, ese valor todavía no
        // esta en el modelo: se confirma antes de leerlo.
        if (tabla.isEditing()) {
            tabla.getCellEditor().stopCellEditing();
        }

        Map<Integer, Double> aplicaciones = new LinkedHashMap<>();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            double monto = montoDeFila(i);
            if (monto > 0.009) {
                aplicaciones.put(Integer.parseInt(modelo.getValueAt(i, COL_ID).toString()), monto);
            }
        }

        if (aplicaciones.isEmpty()) {
            lblAviso.setText("Marque al menos un crédito y asígnele un monto mayor que cero.");
            return;
        }

        double total = totalACruzar();
        if (total > saldoDisponible + 0.009) {
            lblAviso.setText("El total a cruzar supera el saldo disponible del abono.");
            return;
        }

        int confirmar = JOptionPane.showConfirmDialog(this,
                "Se aplicarán $ " + metodos.formateador_dinero().format(total)
                + " del pago #" + idCabecera + " a " + aplicaciones.size() + " crédito(s).\n"
                + "Quedarían $ " + metodos.formateador_dinero().format(saldoDisponible - total)
                + " como saldo a favor.\n\n¿Desea continuar?",
                "Confirmar cruce", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }

        btnAplicar.setEnabled(false);
        try {
            AuditoriaCaja.setOrigen("Creditos - cruzar saldo a favor");
            Date ahora = new Date();
            String fecha = new SimpleDateFormat("yyyy-MM-dd").format(ahora);
            String hora = new SimpleDateFormat("HH:mm:ss").format(ahora);

            String error = new DBabonos().CruzarSaldoAFavor(idCabecera, aplicaciones, fecha, hora);
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "No se pudo cruzar", JOptionPane.ERROR_MESSAGE);
                return;
            }

            cruceAplicado = true;
            JOptionPane.showMessageDialog(this,
                    "Se cruzaron $ " + metodos.formateador_dinero().format(total)
                    + " del pago #" + idCabecera + ".",
                    "Cruce aplicado", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } finally {
            AuditoriaCaja.limpiar();
            btnAplicar.setEnabled(true);
        }
    }
}
