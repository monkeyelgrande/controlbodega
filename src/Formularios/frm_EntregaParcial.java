package Formularios;

import Metodos.EntregaQRService.ItemEntrega;
import Metodos.EntregaQRService.OrdenInfo;
import Metodos.EntregaQRService.ProductoPendiente;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Dialogo tactil para definir cantidades a entregar producto por producto.
 * Layout: tabla a la izquierda + teclado numerico a la derecha.
 *
 * Uso:
 *   frm_EntregaParcial dlg = new frm_EntregaParcial(parent, ordenActiva);
 *   dlg.setVisible(true);
 *   if (dlg.fueConfirmado()) {
 *       List<ItemEntrega> items = dlg.getItems();
 *       ...
 *   }
 */
public class frm_EntregaParcial extends JDialog {

    private static final Color BG          = new Color(0xF4F6F8);
    private static final Color HEADER      = new Color(0x0D47A1);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color OK_GREEN    = new Color(0x2E7D32);
    private static final Color ERROR_RED   = new Color(0xC62828);
    private static final Color KEY_BG      = new Color(0xECEFF1);
    private static final Color KEY_FG      = new Color(0x212121);
    private static final Color KEY_HOVER   = new Color(0xCFD8DC);
    private static final Color KEY_ACCENT  = new Color(0xFF7043);
    private static final Color KEY_DANGER  = new Color(0xE57373);

    private final OrdenInfo orden;
    private boolean confirmado = false;

    private DefaultTableModel modelo;
    private JTable tabla;
    private JLabel lblTotal;
    private JButton btnConfirmar;

    /** Indice de la fila actualmente seleccionada para teclear cantidad. */
    private int filaActiva = -1;

    public frm_EntregaParcial(Window parent, OrdenInfo orden) {
        super(parent, "Entrega parcial", ModalityType.APPLICATION_MODAL);
        this.orden = orden;
        initUI();
    }

    public boolean fueConfirmado() { return confirmado; }

    public List<ItemEntrega> getItems() {
        List<ItemEntrega> items = new ArrayList<>();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            double cant = parseDouble(modelo.getValueAt(i, COL_CANT_ENTREGAR));
            if (cant > 0) {
                ProductoPendiente p = orden.productos.get(i);
                items.add(new ItemEntrega(p.idProducto, p.codigo, p.descripcion, cant));
            }
        }
        return items;
    }

    // =====================================================================
    // UI
    // =====================================================================

    private static final int COL_CODIGO       = 0;
    private static final int COL_DESCRIPCION  = 1;
    private static final int COL_PENDIENTE    = 2;
    private static final int COL_STOCK        = 3;
    private static final int COL_CANT_ENTREGAR = 4;

    private void initUI() {
        setUndecorated(false);
        // tamaño grande para tactil
        setSize(1100, 720);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(getParent());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(HEADER);
        h.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        JLabel t = new JLabel("ENTREGA PARCIAL — Orden #"
                + (orden.codigoFactura != null ? orden.codigoFactura : ("id " + orden.idFactura)));
        t.setForeground(Color.WHITE);
        t.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JLabel sub = new JLabel("Toca una fila y usa el teclado para fijar la cantidad a entregar");
        sub.setForeground(new Color(255, 255, 255, 220));
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(t, BorderLayout.NORTH);
        left.add(sub, BorderLayout.SOUTH);
        h.add(left, BorderLayout.WEST);
        return h;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBackground(BG);
        body.setBorder(BorderFactory.createEmptyBorder(16, 22, 8, 22));

        body.add(buildTablaPanel(), BorderLayout.CENTER);
        body.add(buildTeclado(), BorderLayout.EAST);
        return body;
    }

    private JPanel buildTablaPanel() {
        modelo = new DefaultTableModel(
                new Object[]{"Código", "Descripción", "Pendiente", "Stock", "Entregar"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (ProductoPendiente p : orden.productos) {
            if (p.pendiente <= 0) continue;
            modelo.addRow(new Object[]{
                    p.codigo, p.descripcion,
                    fmt(p.pendiente), fmt(p.stockBodega),
                    "0"
            });
        }
        tabla = new JTable(modelo);
        tabla.setRowHeight(50);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(0xE0E0E0));
        // Seleccion bien contrastada: fondo azul fuerte, texto blanco
        tabla.setSelectionBackground(new Color(0x1565C0));
        tabla.setSelectionForeground(Color.WHITE);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Renderer para todas las columnas: respeta seleccion y centra columnas numericas
        final Color colEntregarBg     = new Color(0xFFF3E0); // ambar muy claro
        final Color colEntregarBgSel  = new Color(0x0D47A1); // azul mas oscuro al estar seleccionado
        final Color colEntregarFg     = new Color(0xBF360C); // rojo-naranja fuerte para el numero
        final Font  colEntregarFont   = new Font("Segoe UI", Font.BOLD, 22);
        final Font  colNumFont        = new Font("Segoe UI", Font.PLAIN, 15);

        DefaultTableCellRenderer rendererEntregar = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(colEntregarFont);
                if (isSelected) {
                    c.setBackground(colEntregarBgSel);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(colEntregarBg);
                    // si el valor es > 0 lo pintamos en rojo-naranja, si es 0 en gris suave
                    double v = parseDouble(value);
                    c.setForeground(v > 0 ? colEntregarFg : new Color(0x9E9E9E));
                }
                return c;
            }
        };

        DefaultTableCellRenderer rendererNumCentro = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(colNumFont);
                return c;
            }
        };
        tabla.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                filaActiva = tabla.getSelectedRow();
            }
        });
        if (modelo.getRowCount() > 0) {
            tabla.setRowSelectionInterval(0, 0);
            filaActiva = 0;
        }

        // Anchos
        tabla.getColumnModel().getColumn(COL_CODIGO).setPreferredWidth(110);
        tabla.getColumnModel().getColumn(COL_DESCRIPCION).setPreferredWidth(340);
        tabla.getColumnModel().getColumn(COL_PENDIENTE).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(COL_STOCK).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(COL_CANT_ENTREGAR).setPreferredWidth(140);

        // Renderers
        tabla.getColumnModel().getColumn(COL_PENDIENTE).setCellRenderer(rendererNumCentro);
        tabla.getColumnModel().getColumn(COL_STOCK).setCellRenderer(rendererNumCentro);
        tabla.getColumnModel().getColumn(COL_CANT_ENTREGAR).setCellRenderer(rendererEntregar);

        JScrollPane sp = new JScrollPane(tabla);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(22, 0));
        sp.setBorder(BorderFactory.createLineBorder(new Color(0xE0E0E0), 1));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(CARD_BG);
        wrap.add(sp, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildTeclado() {
        JPanel teclado = new JPanel(new BorderLayout(0, 8));
        teclado.setOpaque(false);
        teclado.setPreferredSize(new Dimension(330, 0));

        // Etiqueta de la fila activa
        final JLabel hint = new JLabel("Selecciona una fila", SwingConstants.CENTER);
        hint.setFont(new Font("Segoe UI", Font.BOLD, 13));
        hint.setForeground(new Color(0x616161));
        hint.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        teclado.add(hint, BorderLayout.NORTH);
        // actualizar hint al cambiar fila
        tabla.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                int r = tabla.getSelectedRow();
                if (r < 0) { hint.setText("Selecciona una fila"); return; }
                hint.setText("Editando: " + modelo.getValueAt(r, COL_DESCRIPCION));
            }
        });

        JPanel keys = new JPanel(new GridLayout(5, 3, 8, 8));
        keys.setOpaque(false);

        String[] base = {"7","8","9","4","5","6","1","2","3"};
        for (String k : base) keys.add(tecla(k, KEY_BG, KEY_FG, KEY_HOVER));

        keys.add(tecla("0", KEY_BG, KEY_FG, KEY_HOVER));
        keys.add(tecla(".", KEY_BG, KEY_FG, KEY_HOVER));
        keys.add(tecla("←", KEY_DANGER, Color.WHITE, new Color(0xC62828)));    // borrar ultimo
        keys.add(tecla("MAX", KEY_ACCENT, Color.WHITE, new Color(0xE64A19)));  // poner pendiente
        keys.add(tecla("CLR", KEY_DANGER, Color.WHITE, new Color(0xC62828)));  // limpiar
        keys.add(tecla("Sig", KEY_BG, KEY_FG, KEY_HOVER));                     // siguiente fila

        teclado.add(keys, BorderLayout.CENTER);
        return teclado;
    }

    private JButton tecla(final String etiqueta, Color base, Color fg, final Color hover) {
        final JButton b = new JButton(etiqueta);
        b.setFont(new Font("Segoe UI", Font.BOLD, 24));
        b.setForeground(fg);
        b.setBackground(base);
        b.setOpaque(true);
        b.setFocusable(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));
        final Color baseFinal = base;
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(baseFinal); }
            @Override public void mouseClicked(MouseEvent e) { onTeclaClick(etiqueta); }
        });
        return b;
    }

    private void onTeclaClick(String tecla) {
        if (filaActiva < 0) return;
        String actual = String.valueOf(modelo.getValueAt(filaActiva, COL_CANT_ENTREGAR));
        if (actual == null) actual = "0";

        switch (tecla) {
            case "←": {
                String nuevo = actual.length() <= 1 ? "0" : actual.substring(0, actual.length() - 1);
                if (nuevo.isEmpty()) nuevo = "0";
                modelo.setValueAt(nuevo, filaActiva, COL_CANT_ENTREGAR);
                break;
            }
            case "CLR":
                modelo.setValueAt("0", filaActiva, COL_CANT_ENTREGAR);
                break;
            case "MAX": {
                ProductoPendiente p = orden.productos.get(filaActiva);
                modelo.setValueAt(fmt(p.pendiente), filaActiva, COL_CANT_ENTREGAR);
                break;
            }
            case "Sig": {
                int next = (filaActiva + 1) % modelo.getRowCount();
                tabla.setRowSelectionInterval(next, next);
                filaActiva = next;
                tabla.scrollRectToVisible(tabla.getCellRect(next, 0, true));
                break;
            }
            case ".": {
                if (!actual.contains(".")) {
                    String base = actual.equals("0") ? "0" : actual;
                    modelo.setValueAt(base + ".", filaActiva, COL_CANT_ENTREGAR);
                }
                break;
            }
            default: { // dígito 0-9
                String nuevo = actual.equals("0") ? tecla : actual + tecla;
                modelo.setValueAt(nuevo, filaActiva, COL_CANT_ENTREGAR);
                break;
            }
        }
        recalcularTotal();
    }

    private void recalcularTotal() {
        double total = 0;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            total += parseDouble(modelo.getValueAt(i, COL_CANT_ENTREGAR));
        }
        if (lblTotal != null) {
            lblTotal.setText("Total a entregar: " + fmt(total) + " unidades");
        }
        if (btnConfirmar != null) {
            btnConfirmar.setEnabled(total > 0);
            btnConfirmar.setBackground(total > 0 ? OK_GREEN : new Color(0xBDBDBD));
        }
    }

    private JPanel buildFooter() {
        JPanel f = new JPanel(new BorderLayout());
        f.setBackground(BG);
        f.setBorder(BorderFactory.createEmptyBorder(8, 22, 16, 22));

        lblTotal = new JLabel("Total a entregar: 0 unidades");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTotal.setForeground(new Color(0x212121));
        f.add(lblTotal, BorderLayout.WEST);

        JPanel acciones = new JPanel(new GridLayout(1, 2, 12, 0));
        acciones.setOpaque(false);
        acciones.setPreferredSize(new Dimension(520, 70));

        JButton btnCancelar = botonGrande("CANCELAR", ERROR_RED, new Color(0x8E0000));
        btnCancelar.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                confirmado = false;
                dispose();
            }
        });

        btnConfirmar = botonGrande("CONFIRMAR ENTREGA", new Color(0xBDBDBD), new Color(0x9E9E9E));
        btnConfirmar.setEnabled(false);
        btnConfirmar.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (!btnConfirmar.isEnabled()) return;
                if (!validar()) return;
                confirmado = true;
                dispose();
            }
        });

        acciones.add(btnCancelar);
        acciones.add(btnConfirmar);
        f.add(acciones, BorderLayout.EAST);
        return f;
    }

    private boolean validar() {
        // 1) cantidad no excede pendiente por fila
        StringBuilder excedePend = new StringBuilder();
        StringBuilder excedeStock = new StringBuilder();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            double cant = parseDouble(modelo.getValueAt(i, COL_CANT_ENTREGAR));
            if (cant <= 0) continue;
            ProductoPendiente p = orden.productos.get(i);
            if (cant > p.pendiente) {
                excedePend.append(" • ").append(p.descripcion)
                        .append(" (entregar=").append(fmt(cant))
                        .append(", pendiente=").append(fmt(p.pendiente)).append(")\n");
            } else if (cant > p.stockBodega) {
                excedeStock.append(" • ").append(p.descripcion)
                        .append(" (entregar=").append(fmt(cant))
                        .append(", stock=").append(fmt(p.stockBodega)).append(")\n");
            }
        }
        if (excedePend.length() > 0) {
            JOptionPane.showMessageDialog(this,
                    "No se puede entregar mas de lo pendiente:\n\n" + excedePend,
                    "Cantidad excedida", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (excedeStock.length() > 0) {
            int op = JOptionPane.showConfirmDialog(this,
                    "Los siguientes productos quedaran con STOCK NEGATIVO:\n\n"
                    + excedeStock + "\n¿Continuar de todos modos?",
                    "Stock negativo",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            return op == JOptionPane.YES_OPTION;
        }
        return true;
    }

    private JButton botonGrande(String texto, final Color base, final Color hover) {
        final JButton b = new JButton(texto);
        b.setForeground(Color.WHITE);
        b.setBackground(base);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 17));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e)  {
                if (!b.isEnabled()) return;
                b.setBackground(base);
            }
        });
        return b;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static double parseDouble(Object o) {
        if (o == null) return 0;
        try {
            String s = o.toString().trim();
            if (s.isEmpty()) return 0;
            // si termina en punto, completar para parsear
            if (s.endsWith(".")) s = s + "0";
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.format(java.util.Locale.US, "%.2f", d);
    }
}
