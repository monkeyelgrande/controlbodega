/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios_internos;

import Formularios.frm_main;
import Metodos.ComprasConsultas;
import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBsugeridos;
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
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modelos.Bodegas;
import modelos.Sugerido_cabecera;
import modelos.Sugerido_detalle;

/**
 * Crear / editar un sugerido de pedidos. Al agregar un producto muestra
 * existencia, rotación mensual y última compra, y propone una cantidad sugerida
 * automática que el usuario puede ajustar (RF-01).
 *
 * @author Monkeyelgrande
 */
public class jif_crear_sugerido extends JDialog {

    private int idEdicion = 0;
    private boolean soloVer = false;

    private JLabel lbl_titulo;
    private JLabel lbl_numero;
    private JComboBox<Bodegas> jbox_bodega;
    private JTextField txt_meses;
    private JTextField txt_observacion;
    private JTextField txt_Filtro;
    private JTable jtabla_filtro;
    private JTable jtabla_sug;
    private JButton btn_guardar;
    private JButton btn_bloquear;
    private JButton btn_quitar;
    private JButton btn_excel;

    private DefaultTableModel modeloProductos;
    private DefaultTableModel modeloSug;

    // Columnas de la tabla del sugerido
    private static final int C_IDPROD = 0;
    private static final int C_COD = 1;
    private static final int C_DESC = 2;
    private static final int C_EXIST = 3;
    private static final int C_ROT = 4;
    private static final int C_ULT = 5;
    private static final int C_CANT = 6;

    public jif_crear_sugerido() {
        initUI();
        cargarProductos();
        Bodegas bod = new Bodegas();
        bod.mostrarBodegas(jbox_bodega);
        seleccionarBodega(frm_main.id_bodega);
        lbl_numero.setText("SUG-" + DB_consultas_R_D.cargarId("sugeridos_cabecera"));
        setLocationRelativeTo(null);
        metodos.addEscapeListenerWindowDialog(this);
    }

    private void initUI() {
        setModal(true);
        setUndecorated(true);
        setSize(1340, 700);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));

        JPanel header = EstiloCompras.gradientBar(64);
        header.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 12));
        JLabel icon = new JLabel(FontAwesome.icon(FontAwesome.CLIPBOARD, 22f, Color.WHITE));
        lbl_titulo = new JLabel("Nuevo sugerido de pedido");
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

        txt_meses = EstiloCompras.field("1", null);
        txt_meses.setText("1");

        txt_observacion = EstiloCompras.field("Observación (opcional)", FontAwesome.CLIPBOARD);

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(EstiloCompras.labeled("Sugerido N°", lbl_numero, 120));
        row.add(Box.createHorizontalStrut(14));
        row.add(EstiloCompras.labeled("Bodega", jbox_bodega, 220));
        row.add(Box.createHorizontalStrut(14));
        row.add(EstiloCompras.labeled("Meses de cobertura", txt_meses, 140));
        row.add(Box.createHorizontalStrut(14));
        row.add(EstiloCompras.labeled("Observación", txt_observacion, 0));
        return row;
    }

    private JComponent buildSplit() {
        modeloProductos = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int f, int c) {
                return false;
            }
        };
        modeloProductos.setColumnIdentifiers(new Object[]{"ID", "CÓDIGO", "DESCRIPCIÓN"});
        jtabla_filtro = new JTable(modeloProductos);
        EstiloCompras.styleTable(jtabla_filtro);
        EstiloCompras.anchoColumnas(jtabla_filtro, 45, 110, 460);
        jtabla_filtro.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent me) {
                if (me.getClickCount() == 2) {
                    agregarProducto();
                }
            }
        });
        jtabla_filtro.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    agregarProducto();
                }
            }
        });
        txt_Filtro = EstiloCompras.field("Buscar producto...", FontAwesome.SEARCH);
        metodos.BuscarEnTabla(txt_Filtro, jtabla_filtro);

        JPanel izq = new JPanel(new BorderLayout(0, 6));
        izq.setOpaque(false);
        izq.add(tituloPanel("Catálogo (doble clic o ENTER para agregar)"), BorderLayout.NORTH);
        JPanel izqCont = new JPanel(new BorderLayout(0, 6));
        izqCont.setOpaque(false);
        izqCont.add(txt_Filtro, BorderLayout.NORTH);
        izqCont.add(EstiloCompras.scroll(jtabla_filtro), BorderLayout.CENTER);
        izq.add(izqCont, BorderLayout.CENTER);

        modeloSug = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int f, int c) {
                return c == C_CANT;
            }
        };
        modeloSug.setColumnIdentifiers(new Object[]{
            "ID_PROD", "CÓDIGO", "DESCRIPCIÓN", "EXIST.", "ROT./MES", "ÚLT. COMPRA", "CANT. SUGERIDA"});
        jtabla_sug = new JTable(modeloSug);
        EstiloCompras.styleTable(jtabla_sug);
        EstiloCompras.ocultarColumna(jtabla_sug, C_IDPROD);
        EstiloCompras.anchoColumnas(jtabla_sug, 0, 90, 340, 80, 80, 90, 110);
        jtabla_sug.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    quitar();
                }
            }
        });

        JPanel der = new JPanel(new BorderLayout(0, 6));
        der.setOpaque(false);
        der.add(tituloPanel("Productos sugeridos a pedir"), BorderLayout.NORTH);
        der.add(EstiloCompras.scroll(jtabla_sug), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izq, der);
        split.setBorder(null);
        split.setResizeWeight(0.42);
        split.setDividerLocation(560);
        split.setOpaque(false);
        return split;
    }

    private JLabel tituloPanel(String t) {
        JLabel l = new JLabel(t);
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

        btn_quitar = EstiloCompras.secondaryBtn("Quitar", FontAwesome.TRASH);
        btn_quitar.addActionListener(e -> quitar());
        btn_excel = EstiloCompras.secondaryBtn("Exportar Excel", FontAwesome.LIST);
        btn_excel.addActionListener(e -> exportarExcel());
        btn_guardar = EstiloCompras.secondaryBtn("Guardar", FontAwesome.SAVE);
        btn_guardar.addActionListener(e -> guardar(false));
        btn_bloquear = EstiloCompras.primaryBtn("Cerrar y bloquear", FontAwesome.CHECK);
        btn_bloquear.addActionListener(e -> bloquear());

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.add(btn_quitar);
        left.add(Box.createHorizontalStrut(8));
        left.add(btn_excel);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(btn_guardar);
        right.add(Box.createHorizontalStrut(8));
        right.add(btn_bloquear);

        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private void cargarProductos() {
        modeloProductos.setRowCount(0);
        ResultSet rs = DB_consultas_R_D.getTabla(
                "SELECT id, codigo_barras, descripcion FROM productos "
                + "WHERE COALESCE(estado,true)=true ORDER BY descripcion");
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

    private double meses() {
        try {
            return Double.parseDouble(txt_meses.getText().replace(",", "."));
        } catch (Exception e) {
            return 1;
        }
    }

    private void agregarProducto() {
        int fila = jtabla_filtro.getSelectedRow();
        if (fila < 0) {
            return;
        }
        String idProd = jtabla_filtro.getValueAt(fila, 0).toString();
        for (int i = 0; i < modeloSug.getRowCount(); i++) {
            if (modeloSug.getValueAt(i, C_IDPROD).toString().equals(idProd)) {
                jtabla_sug.setRowSelectionInterval(i, i);
                return;
            }
        }
        int idp = Integer.parseInt(idProd);
        double exist = ComprasConsultas.existencia(idp);
        double rot = ComprasConsultas.rotacionMensual(idp);
        double ult = ComprasConsultas.ultimaCompra(idp);
        double trans = ComprasConsultas.transito(idp);
        double auto = ComprasConsultas.sugeridoAutomatico(rot, exist, trans, meses());
        modeloSug.addRow(new Object[]{
            idProd, jtabla_filtro.getValueAt(fila, 1), jtabla_filtro.getValueAt(fila, 2),
            fmt(exist), fmt(rot), fmt(ult), fmt(auto)});
    }

    private String fmt(double v) {
        return metodos.formateador_decimal().format(v);
    }

    private void quitar() {
        int fila = jtabla_sug.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto");
            return;
        }
        if (jtabla_sug.isEditing()) {
            jtabla_sug.getCellEditor().stopCellEditing();
        }
        modeloSug.removeRow(fila);
    }

    private void seleccionarBodega(int idBodega) {
        for (int i = 0; i < jbox_bodega.getItemCount(); i++) {
            if (jbox_bodega.getItemAt(i).getId() == idBodega) {
                jbox_bodega.setSelectedIndex(i);
                return;
            }
        }
    }

    private double num(Object o) {
        try {
            return Double.parseDouble(o.toString().replace(".", "").replace(",", "."));
        } catch (Exception e) {
            try {
                return Double.parseDouble(o.toString());
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    private List<Sugerido_detalle> leerDetalles() {
        if (jtabla_sug.isEditing()) {
            jtabla_sug.getCellEditor().stopCellEditing();
        }
        List<Sugerido_detalle> dets = new ArrayList<>();
        for (int i = 0; i < modeloSug.getRowCount(); i++) {
            Sugerido_detalle d = new Sugerido_detalle();
            d.setId_producto(Integer.parseInt(modeloSug.getValueAt(i, C_IDPROD).toString()));
            d.setExistencia(num(modeloSug.getValueAt(i, C_EXIST)));
            d.setRotacion_mensual(num(modeloSug.getValueAt(i, C_ROT)));
            d.setUltima_compra(num(modeloSug.getValueAt(i, C_ULT)));
            double cant = num(modeloSug.getValueAt(i, C_CANT));
            d.setCantidad_sugerida(cant);
            d.setCantidad_final(cant);
            dets.add(d);
        }
        return dets;
    }

    private boolean guardar(boolean bloquear) {
        if (soloVer) {
            return false;
        }
        if (modeloSug.getRowCount() < 1) {
            JOptionPane.showMessageDialog(this, "Agregue al menos un producto");
            return false;
        }
        List<Sugerido_detalle> dets = leerDetalles();
        Sugerido_cabecera cab = new Sugerido_cabecera();
        cab.setId_user_crea(frm_main.id_user);
        cab.setObservacion(txt_observacion.getText());
        cab.setMeses_cobertura(meses());
        cab.setEstado(bloquear ? Sugerido_cabecera.ESTADO_BLOQUEADO : Sugerido_cabecera.ESTADO_ABIERTO);
        try {
            cab.setId_bodega(jbox_bodega.getItemAt(jbox_bodega.getSelectedIndex()).getId());
        } catch (Exception e) {
            cab.setId_bodega(frm_main.id_bodega);
        }
        Calendar c = new GregorianCalendar();
        cab.setFecha(new SimpleDateFormat("yyyy-MM-dd").format(c.getTime()));
        cab.setHora(new SimpleDateFormat("HH:mm:ss").format(c.getTime()));

        DBsugeridos dao = new DBsugeridos();
        boolean ok;
        if (idEdicion > 0) {
            cab.setId(idEdicion);
            ok = dao.ActualizarCompleto(cab, dets);
            if (ok && bloquear) {
                dao.ActualizarEstado(idEdicion, Sugerido_cabecera.ESTADO_BLOQUEADO);
            }
        } else {
            ok = dao.GuardarCompleto(cab, dets);
        }
        return ok;
    }

    private void bloquear() {
        int r = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de que esto es todo? El sugerido quedará bloqueado y no editable.",
                "Cerrar sugerido", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        if (guardar(true)) {
            JOptionPane.showMessageDialog(this, "Sugerido cerrado y bloqueado. Notifique al equipo de almacén.");
            dispose();
        }
    }

    private void exportarExcel() {
        try {
            new Metodos.ExportarExcel().exportarExcel(jtabla_sug);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar: " + e.getMessage());
        }
    }

    public void cargarSugerido(int id, boolean soloVer) {
        this.idEdicion = id;
        this.soloVer = soloVer;
        ResultSet rs = DBsugeridos.cargarCabecera(id);
        try {
            if (rs.next()) {
                lbl_numero.setText(rs.getString("numero"));
                txt_observacion.setText(rs.getString("observacion"));
                txt_meses.setText(metodos.formateador_decimal().format(rs.getDouble("meses_cobertura")));
                seleccionarBodega(rs.getInt("id_bodega"));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        modeloSug.setRowCount(0);
        rs = DBsugeridos.cargarDetalles(id);
        try {
            while (rs.next()) {
                modeloSug.addRow(new Object[]{
                    rs.getString("id_producto"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                    fmt(rs.getDouble("existencia")), fmt(rs.getDouble("rotacion_mensual")),
                    fmt(rs.getDouble("ultima_compra")), fmt(rs.getDouble("cantidad_sugerida"))});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        if (soloVer) {
            lbl_titulo.setText("Ver sugerido " + lbl_numero.getText());
            jbox_bodega.setEnabled(false);
            txt_meses.setEditable(false);
            txt_observacion.setEditable(false);
            jtabla_sug.setEnabled(false);
            btn_guardar.setEnabled(false);
            btn_bloquear.setEnabled(false);
            btn_quitar.setEnabled(false);
        } else {
            lbl_titulo.setText("Editar sugerido " + lbl_numero.getText());
        }
    }
}
