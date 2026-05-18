package Formularios;

import Login.login;
import Metodos.EntregaQRService;
import Metodos.EntregaQRService.Accion;
import Metodos.EntregaQRService.ItemEntrega;
import Metodos.EntregaQRService.OrdenInfo;
import Metodos.EntregaQRService.ProductoPendiente;
import Metodos.EntregaQRService.Resultado;
import Metodos.EntregaQRService.ResultadoEscaneo;

import java.util.ArrayList;
import java.util.List;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/**
 * Ventana principal del modulo Entregas Rapidas.
 * - Pantalla completa, sin menus.
 * - Captura lector QR via KeyEventDispatcher global (no requiere foco en un campo).
 * - Muestra orden escaneada, productos pendientes, y permite entrega completa o parcial.
 */
public class frm_EntregasQR extends JFrame {

    // Paleta tactil
    private static final Color BG          = new Color(0xF4F6F8);
    private static final Color HEADER      = new Color(0x1B5E20);
    private static final Color HEADER_ALT  = new Color(0x2E7D32);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT        = new Color(0x212121);
    private static final Color TEXT_SOFT   = new Color(0x616161);
    private static final Color OK_GREEN    = new Color(0x2E7D32);
    private static final Color ERROR_RED   = new Color(0xC62828);
    private static final Color WARN_AMBER  = new Color(0xEF6C00);
    private static final Color INFO_BLUE   = new Color(0x1565C0);
    private static final Color NOTE_BG     = new Color(0xFFF8E1); // amarillo nota
    private static final Color NOTE_BORDER = new Color(0xF9A825); // ambar fuerte
    private static final Color NOTE_TEXT   = new Color(0x4E342E); // marron oscuro legible

    private final int idUser;
    private final String nombreUsuario;
    private final int idBodega;
    private final String nombreBodega;

    // Estado actual
    private OrdenInfo ordenActiva;
    private int idEscaneoActivo = -1;

    // Captura QR (buffer global)
    private final StringBuilder qrBuffer = new StringBuilder();
    private long ultimaTeclaMs = 0;
    private static final long QR_TIMEOUT_MS = 200; // si pasa mas tiempo entre teclas, se reinicia

    private KeyEventDispatcher dispatcher;

    // UI
    private JLabel lblBanner;       // mensaje superior (ej: "Esperando QR...")
    private JLabel lblOrdenCodigo;
    private JLabel lblOrdenCliente;
    private JLabel lblOrdenFecha;
    private JLabel lblOrdenTotalPend;
    private JPanel panelObservacion;
    private JTextArea txtObservacion;
    private JLabel lblObservacionTitulo;
    private DefaultTableModel modeloProductos;
    private JTable tablaProductos;
    private JButton btnEntregaCompleta;
    private JButton btnEntregaParcial;
    private JButton btnCerrarSesion;

    public frm_EntregasQR(int idUser, String nombreUsuario, int idBodega, String nombreBodega) {
        this.idUser = idUser;
        this.nombreUsuario = nombreUsuario;
        this.idBodega = idBodega;
        this.nombreBodega = nombreBodega;
        initUI();
        instalarCapturaQR();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { desinstalarCapturaQR(); }
        });
    }

    // =====================================================================
    // UI
    // =====================================================================

    private void initUI() {
        setTitle("Control Bodega — Entregas Rápidas");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { cerrarSesion(); }
        });
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1024, 700));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);

        setContentPane(root);
        actualizarEstadoSinOrden();
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(HEADER);
        h.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        JLabel titulo = new JLabel("ENTREGAS RÁPIDAS");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JLabel sub = new JLabel("  •  " + nombreUsuario + "  •  Bodega: " + nombreBodega);
        sub.setForeground(new Color(255, 255, 255, 220));
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new VerticalLayout());
        left.add(titulo);
        left.add(sub);

        btnCerrarSesion = botonHeader("CERRAR SESIÓN");
        btnCerrarSesion.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { cerrarSesion(); }
        });

        h.add(left, BorderLayout.WEST);
        h.add(btnCerrarSesion, BorderLayout.EAST);
        return h;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(BG);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Banner de estado
        lblBanner = new JLabel(" ", SwingConstants.CENTER);
        lblBanner.setOpaque(true);
        lblBanner.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblBanner.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        body.add(lblBanner, BorderLayout.NORTH);

        // Centro: card de orden + observacion + tabla
        JPanel topStack = new JPanel(new BorderLayout(0, 12));
        topStack.setOpaque(false);
        topStack.add(buildOrdenCard(), BorderLayout.NORTH);
        topStack.add(buildObservacionPanel(), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(topStack, BorderLayout.NORTH);
        center.add(buildTablaPanel(), BorderLayout.CENTER);
        body.add(center, BorderLayout.CENTER);

        // Sur: botones grandes
        body.add(buildAcciones(), BorderLayout.SOUTH);

        return body;
    }

    private JPanel buildOrdenCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE0E0E0), 1),
                BorderFactory.createEmptyBorder(16, 22, 16, 22)));

        lblOrdenCodigo    = new JLabel("—");
        lblOrdenCliente   = new JLabel("—");
        lblOrdenFecha     = new JLabel("—");
        lblOrdenTotalPend = new JLabel("—");

        lblOrdenCodigo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblOrdenCliente.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblOrdenFecha.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblOrdenTotalPend.setFont(new Font("Segoe UI", Font.BOLD, 16));

        lblOrdenCodigo.setForeground(TEXT);
        lblOrdenCliente.setForeground(TEXT);
        lblOrdenFecha.setForeground(TEXT_SOFT);
        lblOrdenTotalPend.setForeground(INFO_BLUE);

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridx = 0;
        gc.insets = new Insets(0, 0, 4, 0);

        gc.gridy = 0; card.add(etiqueta("ORDEN", TEXT_SOFT, 11), gc);
        gc.gridy = 1; card.add(lblOrdenCodigo, gc);
        gc.gridy = 2; card.add(etiqueta("CLIENTE", TEXT_SOFT, 11), gc);
        gc.gridy = 3; card.add(lblOrdenCliente, gc);
        gc.gridy = 4; card.add(lblOrdenFecha, gc);
        gc.gridy = 5; gc.insets = new Insets(10, 0, 0, 0);
        card.add(lblOrdenTotalPend, gc);

        return card;
    }

    private JPanel buildObservacionPanel() {
        panelObservacion = new JPanel(new BorderLayout(0, 6));
        panelObservacion.setBackground(NOTE_BG);
        panelObservacion.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, NOTE_BORDER),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        lblObservacionTitulo = new JLabel("OBSERVACIÓN DE LA ORDEN");
        lblObservacionTitulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblObservacionTitulo.setForeground(NOTE_BORDER);

        txtObservacion = new JTextArea();
        txtObservacion.setEditable(false);
        txtObservacion.setLineWrap(true);
        txtObservacion.setWrapStyleWord(true);
        txtObservacion.setBackground(NOTE_BG);
        txtObservacion.setForeground(NOTE_TEXT);
        txtObservacion.setFont(new Font("Segoe UI", Font.BOLD, 17));
        txtObservacion.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        panelObservacion.add(lblObservacionTitulo, BorderLayout.NORTH);
        panelObservacion.add(txtObservacion, BorderLayout.CENTER);
        panelObservacion.setVisible(false); // se muestra solo si hay observacion
        return panelObservacion;
    }

    private JPanel buildTablaPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(CARD_BG);
        wrap.setBorder(BorderFactory.createLineBorder(new Color(0xE0E0E0), 1));

        modeloProductos = new DefaultTableModel(
                new Object[]{"Código", "Descripción", "Pedido", "Entregado", "Pendiente", "Stock"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaProductos = new JTable(modeloProductos);
        tablaProductos.setRowHeight(40);
        tablaProductos.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        tablaProductos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        tablaProductos.getTableHeader().setReorderingAllowed(false);
        tablaProductos.setShowGrid(true);
        tablaProductos.setGridColor(new Color(0xEEEEEE));
        tablaProductos.setSelectionBackground(new Color(0xE3F2FD));
        tablaProductos.setIntercellSpacing(new Dimension(0, 0));
        tablaProductos.setRowSelectionAllowed(false);
        tablaProductos.setFocusable(false);
        tablaProductos.setCellSelectionEnabled(false);

        JScrollPane sp = new JScrollPane(tablaProductos);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(20, 0));
        sp.setBorder(BorderFactory.createEmptyBorder());
        wrap.add(sp, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildAcciones() {
        JPanel p = new JPanel(new GridLayout(1, 2, 16, 0));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 110));

        btnEntregaCompleta = botonGrande("ENTREGA COMPLETA",
                "Entrega TODA la cantidad pendiente",
                OK_GREEN, new Color(0x1B5E20));
        btnEntregaCompleta.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (btnEntregaCompleta.isEnabled()) onEntregaCompleta();
            }
        });

        btnEntregaParcial = botonGrande("ENTREGA PARCIAL",
                "Especifica las cantidades a entregar",
                INFO_BLUE, new Color(0x0D47A1));
        btnEntregaParcial.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (btnEntregaParcial.isEnabled()) onEntregaParcial();
            }
        });

        p.add(btnEntregaCompleta);
        p.add(btnEntregaParcial);
        return p;
    }

    private JButton botonGrande(String titulo, String sub, final Color base, final Color hover) {
        final JButton b = new JButton("<html><div style='text-align:center;'>"
                + "<div style='font-size:18pt;font-weight:bold;'>" + titulo + "</div>"
                + "<div style='font-size:10pt;font-weight:normal;margin-top:6px;'>" + sub + "</div>"
                + "</div></html>");
        b.setForeground(Color.WHITE);
        b.setBackground(base);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e)  { if (b.isEnabled()) b.setBackground(base); }
        });
        return b;
    }

    private JButton botonHeader(String texto) {
        JButton b = new JButton(texto);
        b.setForeground(Color.WHITE);
        b.setBackground(HEADER_ALT);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JLabel etiqueta(String texto, Color color, int size) {
        JLabel l = new JLabel(texto);
        l.setForeground(color);
        l.setFont(new Font("Segoe UI", Font.PLAIN, size));
        return l;
    }

    // =====================================================================
    // Estado de UI segun orden
    // =====================================================================

    private void actualizarEstadoSinOrden() {
        ordenActiva = null;
        idEscaneoActivo = -1;
        modeloProductos.setRowCount(0);
        lblOrdenCodigo.setText("—");
        lblOrdenCliente.setText("—");
        lblOrdenFecha.setText("—");
        lblOrdenTotalPend.setText("Esperando lectura de QR…");
        if (panelObservacion != null) {
            panelObservacion.setVisible(false);
            txtObservacion.setText("");
        }
        setBanner("Escanea el QR de una orden para comenzar", INFO_BLUE);
        btnEntregaCompleta.setEnabled(false);
        btnEntregaParcial.setEnabled(false);
        btnEntregaCompleta.setBackground(new Color(0xBDBDBD));
        btnEntregaParcial.setBackground(new Color(0xBDBDBD));
    }

    private void cargarOrdenEnUI(OrdenInfo info) {
        ordenActiva = info;
        modeloProductos.setRowCount(0);
        for (ProductoPendiente p : info.productos) {
            modeloProductos.addRow(new Object[]{
                    p.codigo, p.descripcion,
                    fmt(p.pedido), fmt(p.entregado), fmt(p.pendiente), fmt(p.stockBodega)
            });
        }
        lblOrdenCodigo.setText("# " + safe(info.codigoFactura) + "   (id " + info.idFactura + ")");
        lblOrdenCliente.setText(safe(info.nombreCliente)
                + (info.cedulaCliente != null && !info.cedulaCliente.isEmpty()
                    ? "   —   " + info.cedulaCliente : ""));
        lblOrdenFecha.setText("Fecha: " + safe(info.fecha) + "   •   Bodega: " + safe(info.nombreBodegaOrden));
        double pend = info.getTotalPendiente();
        lblOrdenTotalPend.setText("Total pendiente: " + fmt(pend) + " unidades");

        // Observacion: solo visible si hay contenido
        String obs = info.observacion == null ? "" : info.observacion.trim();
        if (obs.isEmpty()) {
            panelObservacion.setVisible(false);
            txtObservacion.setText("");
        } else {
            txtObservacion.setText(obs);
            txtObservacion.setCaretPosition(0);
            panelObservacion.setVisible(true);
        }

        boolean hayPendiente = pend > 0;
        btnEntregaCompleta.setEnabled(hayPendiente);
        btnEntregaParcial.setEnabled(hayPendiente);
        if (hayPendiente) {
            btnEntregaCompleta.setBackground(OK_GREEN);
            btnEntregaParcial.setBackground(INFO_BLUE);
            setBanner("Orden cargada — selecciona el tipo de entrega", OK_GREEN);
        }
    }

    private void setBanner(String texto, Color color) {
        lblBanner.setText(texto);
        lblBanner.setBackground(color);
        lblBanner.setForeground(Color.WHITE);
    }

    // =====================================================================
    // Acciones
    // =====================================================================

    private void onEntregaCompleta() {
        if (ordenActiva == null) return;

        // 1) Construir items: todos los productos con cantidad = pendiente
        List<ItemEntrega> items = new ArrayList<>();
        List<ProductoPendiente> productosEnNegativo = new ArrayList<>();
        for (ProductoPendiente p : ordenActiva.productos) {
            if (p.pendiente <= 0) continue;
            items.add(new ItemEntrega(p.idProducto, p.codigo, p.descripcion, p.pendiente));
            if (p.pendiente > p.stockBodega) {
                productosEnNegativo.add(p);
            }
        }
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay productos pendientes para entregar.",
                    "Sin pendientes", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 2) Si hay productos que dejarian stock en negativo, alertar antes
        if (!productosEnNegativo.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><body style='font-family:Segoe UI;font-size:13pt;'>");
            sb.append("<b style='color:#C62828;'>ADVERTENCIA: stock insuficiente</b><br><br>");
            sb.append("Los siguientes productos quedarán con stock <b>NEGATIVO</b> ");
            sb.append("si confirmas la entrega completa:<br><br>");
            sb.append("<table border='1' cellpadding='4' cellspacing='0' style='border-collapse:collapse;font-size:11pt;'>");
            sb.append("<tr style='background:#FFEBEE;'>")
              .append("<th>Código</th><th>Descripción</th>")
              .append("<th>A entregar</th><th>Stock</th><th>Quedará</th></tr>");
            for (ProductoPendiente p : productosEnNegativo) {
                sb.append("<tr>")
                  .append("<td>").append(safeHtml(p.codigo)).append("</td>")
                  .append("<td>").append(safeHtml(p.descripcion)).append("</td>")
                  .append("<td align='right'>").append(fmt(p.pendiente)).append("</td>")
                  .append("<td align='right'>").append(fmt(p.stockBodega)).append("</td>")
                  .append("<td align='right' style='color:#C62828;'><b>")
                  .append(fmt(p.stockBodega - p.pendiente))
                  .append("</b></td>")
                  .append("</tr>");
            }
            sb.append("</table><br>");
            sb.append("¿Deseas continuar de todos modos?");
            sb.append("</body></html>");

            int op = JOptionPane.showConfirmDialog(this, sb.toString(),
                    "Stock negativo",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (op != JOptionPane.YES_OPTION) return;
        }

        // 3) Confirmacion final
        double totalUnid = 0.0;
        for (ItemEntrega it : items) totalUnid += it.cantidad;
        int op = JOptionPane.showConfirmDialog(this,
                "<html><body style='font-family:Segoe UI;font-size:13pt;'>"
                + "Se van a entregar <b>" + fmt(totalUnid) + "</b> unidades "
                + "en <b>" + items.size() + "</b> productos.<br><br>"
                + "¿Confirmar entrega completa?</body></html>",
                "Confirmar entrega completa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (op != JOptionPane.YES_OPTION) return;

        // 4) Ejecutar
        int idCab = EntregaQRService.ejecutarEntrega(
                ordenActiva, items, idUser, idBodega,
                Accion.ENTREGA_COMPLETA, idEscaneoActivo);

        if (idCab <= 0) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo registrar la entrega. Revisa el log.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5) Imprimir comprobante (si hay infra) y reset
        try {
            Metodos.ImprimirTermica80MMEntrega.imprimirComprobante(
                    idCab, ordenActiva, items, nombreUsuario, nombreBodega);
        } catch (Throwable t) {
            System.out.println("Impresion comprobante fallo: " + t);
        }

        setBanner("Entrega completa registrada (id " + idCab + ")", OK_GREEN);
        new Timer(2500, e -> {
            actualizarEstadoSinOrden();
            ((Timer) e.getSource()).stop();
        }).start();
    }

    private static String safeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void onEntregaParcial() {
        if (ordenActiva == null) return;

        frm_EntregaParcial dlg = new frm_EntregaParcial(this, ordenActiva);
        dlg.setVisible(true);
        if (!dlg.fueConfirmado()) return;

        List<ItemEntrega> items = dlg.getItems();
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No se especifico ninguna cantidad para entregar.",
                    "Sin cantidades", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int idCab = EntregaQRService.ejecutarEntrega(
                ordenActiva, items, idUser, idBodega,
                Accion.ENTREGA_PARCIAL, idEscaneoActivo);

        if (idCab <= 0) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo registrar la entrega. Revisa el log.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Metodos.ImprimirTermica80MMEntrega.imprimirComprobante(
                    idCab, ordenActiva, items, nombreUsuario, nombreBodega);
        } catch (Throwable t) {
            System.out.println("Impresion comprobante fallo: " + t);
        }

        setBanner("Entrega parcial registrada (id " + idCab + ")", OK_GREEN);
        new Timer(2500, e -> {
            actualizarEstadoSinOrden();
            ((Timer) e.getSource()).stop();
        }).start();
    }

    private void cerrarSesion() {
        int op = JOptionPane.showConfirmDialog(this,
                "¿Cerrar sesión y volver al login?",
                "Cerrar sesión",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (op == JOptionPane.YES_OPTION) {
            desinstalarCapturaQR();
            dispose();
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() { new login().setVisible(true); }
            });
        }
    }

    // =====================================================================
    // Captura QR global (KeyEventDispatcher)
    // =====================================================================

    private void instalarCapturaQR() {
        dispatcher = new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() != KeyEvent.KEY_PRESSED) return false;
                // Solo procesar QR si la ventana activa es ESTE frame.
                // Si hay un dialogo modal (entrega parcial, JOptionPane), las teclas
                // pertenecen a ese dialogo y no se interpretan como QR.
                Component c = e.getComponent();
                Window w = c == null ? null : SwingUtilities.getWindowAncestor(c);
                if (w != frm_EntregasQR.this) return false;
                if (!isFocused()) return false;

                long now = System.currentTimeMillis();
                if (now - ultimaTeclaMs > QR_TIMEOUT_MS && qrBuffer.length() > 0) {
                    qrBuffer.setLength(0);
                }
                ultimaTeclaMs = now;

                int code = e.getKeyCode();
                if (code == KeyEvent.VK_ENTER) {
                    final String leido = qrBuffer.toString();
                    qrBuffer.setLength(0);
                    if (leido.length() == 0) return false;
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() { procesarQR(leido); }
                    });
                    return true;
                }

                char ch = e.getKeyChar();
                if (ch != KeyEvent.CHAR_UNDEFINED && ch >= 32 && ch < 127) {
                    qrBuffer.append(ch);
                    return true;
                }
                return false;
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
    }

    private void desinstalarCapturaQR() {
        if (dispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);
            dispatcher = null;
        }
    }

    private void procesarQR(String texto) {
        ResultadoEscaneo r = EntregaQRService.procesarLectura(texto, idUser, idBodega);
        idEscaneoActivo = r.idEscaneo;

        if (r.resultado == Resultado.OK) {
            cargarOrdenEnUI(r.orden);
            return;
        }

        // No-OK: limpiar UI y mostrar mensaje segun resultado
        actualizarEstadoSinOrden();
        Color color;
        switch (r.resultado) {
            case YA_ENTREGADA: color = WARN_AMBER; break;
            case OTRA_BODEGA:  color = WARN_AMBER; break;
            case ANULADA:      color = ERROR_RED;  break;
            case ORDEN_NO_EXISTE: color = ERROR_RED; break;
            case QR_INVALIDO:  color = ERROR_RED;  break;
            default:           color = ERROR_RED;  break;
        }
        setBanner(r.mensaje != null ? r.mensaje : r.resultado.name(), color);
        // Auto-volver al estado de espera tras unos segundos si fue invalido total
        new Timer(4000, e -> {
            if (ordenActiva == null) setBanner("Escanea el QR de una orden para comenzar", INFO_BLUE);
            ((Timer) e.getSource()).stop();
        }).start();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static String safe(String s) { return s == null ? "" : s; }

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.format(java.util.Locale.US, "%.2f", d);
    }

    // Pequeño layout vertical local: apila componentes uno debajo del otro
    private static class VerticalLayout implements java.awt.LayoutManager {
        @Override public void addLayoutComponent(String name, Component comp) {}
        @Override public void removeLayoutComponent(Component comp) {}
        @Override public Dimension preferredLayoutSize(java.awt.Container parent) {
            int w = 0, h = 0;
            for (Component c : parent.getComponents()) {
                Dimension d = c.getPreferredSize();
                w = Math.max(w, d.width);
                h += d.height;
            }
            return new Dimension(w, h);
        }
        @Override public Dimension minimumLayoutSize(java.awt.Container parent) {
            return preferredLayoutSize(parent);
        }
        @Override public void layoutContainer(java.awt.Container parent) {
            int y = 0;
            int w = parent.getWidth();
            for (Component c : parent.getComponents()) {
                Dimension d = c.getPreferredSize();
                c.setBounds(0, y, Math.max(d.width, w), d.height);
                y += d.height;
            }
        }
    }
}
