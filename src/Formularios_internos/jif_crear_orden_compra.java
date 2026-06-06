/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios_internos;

import Formularios.frm_main;
import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBordenes_compra;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import modelos.Bodegas;
import modelos.Ordenes_compra_cabecera;
import modelos.Ordenes_compra_detalle;

/**
 * Crear / editar / ver una orden de compra. Cualquier usuario puede agregar
 * productos y cantidades, sin proveedor ni precios (eso se asigna luego en el
 * análisis/aprobación).
 *
 * @author Monkeyelgrande
 */
public class jif_crear_orden_compra extends JDialog {

    private int idOrdenEdicion = 0;     // 0 = orden nueva
    private boolean soloVer = false;

    private JLabel lbl_titulo;
    private JLabel lbl_numero;
    private JComboBox<Bodegas> jbox_bodega;
    private javax.swing.JTextField txt_observacion;
    private javax.swing.JTextField txt_Filtro;
    private JTable jtabla_filtro;
    private JTable jtabla_orden;
    private JButton btn_guardar_borrador;
    private JButton btn_enviar_pendiente;
    private JButton btn_quitar;

    private DefaultTableModel modeloProductos;
    private DefaultTableModel modeloOrden;

    public jif_crear_orden_compra() {
        initUI();
        cargarProductos();
        Bodegas bod = new Bodegas();
        bod.mostrarBodegas(jbox_bodega);
        seleccionarBodega(frm_main.id_bodega);
        lbl_numero.setText("OC-" + DB_consultas_R_D.cargarId("ordenes_compra_cabecera"));
        setLocationRelativeTo(null);
        metodos.addEscapeListenerWindowDialog(this);
    }

    private void initUI() {
        setModal(true);
        setUndecorated(true);
        setSize(1320, 680);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));

        // ---- Header ----
        JPanel header = EstiloCompras.gradientBar(64);
        header.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 12));
        JLabel icon = new JLabel(FontAwesome.icon(FontAwesome.CLIPBOARD, 22f, Color.WHITE));
        lbl_titulo = new JLabel("Crear orden de compra");
        lbl_titulo.setForeground(Color.WHITE);
        lbl_titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl_titulo.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        JPanel tw = new JPanel();
        tw.setOpaque(false);
        tw.setLayout(new BoxLayout(tw, BoxLayout.X_AXIS));
        tw.add(icon);
        tw.add(lbl_titulo);
        header.add(tw, BorderLayout.WEST);
        JButton close = EstiloCompras.headerIconButton(FontAwesome.CLOSE);
        close.addActionListener(e -> dispose());
        header.add(close, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ---- Cuerpo ----
        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setBackground(EstiloCompras.BG_FORM);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        cuerpo.add(buildCabecera(), BorderLayout.NORTH);
        cuerpo.add(buildSplit(), BorderLayout.CENTER);

        root.add(cuerpo, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JComponent buildCabecera() {
        lbl_numero = new JLabel("-");
        lbl_numero.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl_numero.setForeground(EstiloCompras.PRIMARY);
        lbl_numero.setPreferredSize(new Dimension(120, 38));

        jbox_bodega = new JComboBox<>();
        EstiloCompras.styleCombo(jbox_bodega);

        txt_observacion = EstiloCompras.field("Observación de la solicitud (opcional)", FontAwesome.CLIPBOARD);

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(EstiloCompras.labeled("Orden N°", lbl_numero, 120));
        row.add(Box.createHorizontalStrut(14));
        row.add(EstiloCompras.labeled("Bodega", jbox_bodega, 240));
        row.add(Box.createHorizontalStrut(14));
        row.add(EstiloCompras.labeled("Observación", txt_observacion, 0));
        return row;
    }

    private JComponent buildSplit() {
        // ---- Productos disponibles ----
        modeloProductos = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        modeloProductos.setColumnIdentifiers(new Object[]{"ID", "CÓDIGO", "DESCRIPCIÓN"});
        jtabla_filtro = new JTable(modeloProductos);
        EstiloCompras.styleTable(jtabla_filtro);
        // ID y CÓDIGO estrechos; DESCRIPCIÓN amplia.
        EstiloCompras.anchoColumnas(jtabla_filtro, 45, 110, 620);
        jtabla_filtro.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent me) {
                if (me.getClickCount() == 2) {
                    agregarProductoSeleccionado();
                }
            }
        });
        jtabla_filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    agregarProductoSeleccionado();
                }
            }
        });

        txt_Filtro = EstiloCompras.field("Buscar producto...", FontAwesome.SEARCH);
        metodos.BuscarEnTabla(txt_Filtro, jtabla_filtro);

        JPanel pnlIzq = new JPanel(new BorderLayout(0, 6));
        pnlIzq.setOpaque(false);
        pnlIzq.add(tituloPanel("Productos (doble clic o ENTER para agregar)"), BorderLayout.NORTH);
        JPanel izqCont = new JPanel(new BorderLayout(0, 6));
        izqCont.setOpaque(false);
        izqCont.add(txt_Filtro, BorderLayout.NORTH);
        izqCont.add(EstiloCompras.scroll(jtabla_filtro), BorderLayout.CENTER);
        pnlIzq.add(izqCont, BorderLayout.CENTER);

        // ---- Orden (productos a pedir) ----
        modeloOrden = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return columna == 3; // solo CANTIDAD
            }
        };
        modeloOrden.setColumnIdentifiers(new Object[]{"ID_PROD", "CÓDIGO", "DESCRIPCIÓN", "CANTIDAD"});
        jtabla_orden = new JTable(modeloOrden);
        EstiloCompras.styleTable(jtabla_orden);
        // ID_PROD oculto; CÓDIGO estrecho; DESCRIPCIÓN amplia; CANTIDAD pequeña.
        EstiloCompras.ocultarColumna(jtabla_orden, 0);
        EstiloCompras.anchoColumnas(jtabla_orden, 0, 110, 520, 90);
        jtabla_orden.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    quitarSeleccionado();
                }
            }
        });

        JPanel pnlDer = new JPanel(new BorderLayout(0, 6));
        pnlDer.setOpaque(false);
        pnlDer.add(tituloPanel("Productos a pedir"), BorderLayout.NORTH);
        pnlDer.add(EstiloCompras.scroll(jtabla_orden), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlIzq, pnlDer);
        split.setBorder(null);
        split.setResizeWeight(0.5);
        split.setDividerLocation(630);
        split.setOpaque(false);
        return split;
    }

    private JLabel tituloPanel(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(EstiloCompras.TEXT_SECONDARY);
        return l;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(EstiloCompras.BG_SECTION);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCompras.DIVIDER),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        btn_quitar = EstiloCompras.secondaryBtn("Quitar producto", FontAwesome.TRASH);
        btn_quitar.addActionListener(e -> quitarSeleccionado());

        btn_guardar_borrador = EstiloCompras.secondaryBtn("Guardar borrador", FontAwesome.SAVE);
        btn_guardar_borrador.addActionListener(e -> guardar(Ordenes_compra_cabecera.ESTADO_BORRADOR));

        btn_enviar_pendiente = EstiloCompras.primaryBtn("Enviar a pendiente", FontAwesome.CHECK);
        btn_enviar_pendiente.addActionListener(e -> guardar(Ordenes_compra_cabecera.ESTADO_PENDIENTE));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.add(btn_quitar);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(btn_guardar_borrador);
        right.add(Box.createHorizontalStrut(8));
        right.add(btn_enviar_pendiente);

        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void cargarProductos() {
        modeloProductos.setRowCount(0);
        String sql = "SELECT p.id, p.codigo_barras, p.descripcion FROM productos p "
                + "WHERE COALESCE(p.estado, true) = true ORDER BY p.descripcion";
        ResultSet rs = DB_consultas_R_D.getTabla(sql);
        try {
            while (rs.next()) {
                modeloProductos.addRow(new Object[]{
                    rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion")});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void agregarProductoSeleccionado() {
        int fila = jtabla_filtro.getSelectedRow();
        if (fila < 0) {
            return;
        }
        String idProd = jtabla_filtro.getValueAt(fila, 0).toString();
        String codigo = String.valueOf(jtabla_filtro.getValueAt(fila, 1));
        String desc = String.valueOf(jtabla_filtro.getValueAt(fila, 2));

        for (int i = 0; i < modeloOrden.getRowCount(); i++) {
            if (modeloOrden.getValueAt(i, 0).toString().equals(idProd)) {
                jtabla_orden.setRowSelectionInterval(i, i);
                return;
            }
        }
        modeloOrden.addRow(new Object[]{idProd, codigo, desc, "1"});
    }

    private void quitarSeleccionado() {
        int fila = jtabla_orden.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto de la orden");
            return;
        }
        if (jtabla_orden.isEditing()) {
            jtabla_orden.getCellEditor().stopCellEditing();
        }
        modeloOrden.removeRow(fila);
    }

    private void seleccionarBodega(int idBodega) {
        for (int i = 0; i < jbox_bodega.getItemCount(); i++) {
            if (jbox_bodega.getItemAt(i).getId() == idBodega) {
                jbox_bodega.setSelectedIndex(i);
                return;
            }
        }
    }

    private void guardar(int estado) {
        if (soloVer) {
            return;
        }
        if (jtabla_orden.isEditing()) {
            jtabla_orden.getCellEditor().stopCellEditing();
        }
        if (modeloOrden.getRowCount() < 1) {
            JOptionPane.showMessageDialog(this, "Agregue al menos un producto a la orden");
            return;
        }

        List<Ordenes_compra_detalle> detalles = new ArrayList<>();
        for (int i = 0; i < modeloOrden.getRowCount(); i++) {
            double cant;
            try {
                cant = Double.parseDouble(modeloOrden.getValueAt(i, 3).toString().replace(",", "."));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Cantidad inválida en la fila " + (i + 1));
                return;
            }
            if (cant <= 0) {
                JOptionPane.showMessageDialog(this,
                        "La cantidad debe ser mayor a 0 (fila " + (i + 1) + ")");
                return;
            }
            Ordenes_compra_detalle d = new Ordenes_compra_detalle();
            d.setId_producto(Integer.parseInt(modeloOrden.getValueAt(i, 0).toString()));
            d.setCantidad(cant);
            detalles.add(d);
        }

        Ordenes_compra_cabecera oc = new Ordenes_compra_cabecera();
        oc.setEstado(estado);
        oc.setId_user_crea(frm_main.id_user);
        oc.setObservacion(txt_observacion.getText());
        try {
            oc.setId_bodega(jbox_bodega.getItemAt(jbox_bodega.getSelectedIndex()).getId());
        } catch (Exception e) {
            oc.setId_bodega(frm_main.id_bodega);
        }
        Calendar c = new GregorianCalendar();
        oc.setFecha(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
        oc.setHora(new SimpleDateFormat("HH:mm:ss").format(c.getTime()));

        DBordenes_compra dao = new DBordenes_compra();
        boolean ok;
        if (idOrdenEdicion > 0) {
            oc.setId(idOrdenEdicion);
            ok = dao.ActualizarOrdenCompleta(oc, detalles);
        } else {
            ok = dao.GuardarOrdenCompleta(oc, detalles);
        }

        if (ok) {
            String msg = estado == Ordenes_compra_cabecera.ESTADO_PENDIENTE
                    ? "Orden enviada a PENDIENTE correctamente."
                    : "Borrador guardado correctamente.";
            JOptionPane.showMessageDialog(this, msg + "\nN°: " + oc.getNumero());
            dispose();
        }
    }

    /**
     * Carga una orden existente para ver o editar.
     */
    public void cargarOrden(int idOrden, boolean soloVer) {
        this.idOrdenEdicion = idOrden;
        this.soloVer = soloVer;

        ResultSet rsc = DBordenes_compra.cargarCabecera(idOrden);
        try {
            if (rsc.next()) {
                lbl_numero.setText(rsc.getString("numero"));
                txt_observacion.setText(rsc.getString("observacion"));
            }
            rsc.close();
        } catch (Exception e) {
            System.out.println(e);
        }

        ResultSet rsb = DB_consultas_R_D.getTabla(
                "SELECT id_bodega FROM ordenes_compra_cabecera WHERE id = " + idOrden);
        try {
            if (rsb.next()) {
                seleccionarBodega(rsb.getInt("id_bodega"));
            }
            rsb.close();
        } catch (Exception e) {
            System.out.println(e);
        }

        modeloOrden.setRowCount(0);
        ResultSet rsd = DBordenes_compra.cargarDetalles(idOrden);
        try {
            while (rsd.next()) {
                modeloOrden.addRow(new Object[]{
                    rsd.getString("id_producto"), rsd.getString("codigo_barras"),
                    rsd.getString("descripcion"),
                    metodos.formateador_decimal().format(rsd.getDouble("cantidad"))});
            }
            rsd.close();
        } catch (Exception e) {
            System.out.println(e);
        }

        if (soloVer) {
            lbl_titulo.setText("Ver orden de compra " + lbl_numero.getText());
            jbox_bodega.setEnabled(false);
            txt_observacion.setEditable(false);
            jtabla_orden.setEnabled(false);
            btn_guardar_borrador.setEnabled(false);
            btn_enviar_pendiente.setEnabled(false);
            btn_quitar.setEnabled(false);
        } else {
            lbl_titulo.setText("Editar orden de compra " + lbl_numero.getText());
        }
    }
}
