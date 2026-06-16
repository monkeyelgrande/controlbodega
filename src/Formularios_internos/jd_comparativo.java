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
import conexiondb.DBcomparativos;
import conexiondb.DBordenes_compra;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import modelos.Contactos;

/**
 * Comparativo de cotizaciones (RF-04): matriz productos × proveedores con
 * descuento pronto pago, IVA, flete, precio mínimo neto, mejor proveedor, peso
 * total y planificación de viajes. Réplica de la plantilla Excel del cliente.
 *
 * @author Monkeyelgrande
 */
public class jd_comparativo extends JDialog {

    private final int idComparativo;
    private boolean recomputing = false;

    private JTextField txt_iva;
    private JTextField txt_camion;
    private JComboBox<Contactos> jbox_prov;
    private JTable tblProv;       // proveedores (columnas)
    private JTable tblMatriz;     // matriz
    private DefaultTableModel mProv;
    private DefaultTableModel mMatriz;
    private JLabel lbl_totales;
    private JButton btn_emitir;

    // filas de productos: {idCompProducto, idProducto, codigo, desc, peso, cant}
    private final List<Object[]> prods = new ArrayList<>();
    // proveedores (columnas)
    private final List<DBcomparativos.Proveedor> provs = new ArrayList<>();

    // columnas fijas de la matriz antes de los proveedores
    private static final int M_IDCP = 0;
    private static final int M_COD = 1;
    private static final int M_DESC = 2;
    private static final int M_PESO = 3;
    private static final int M_CANT = 4;
    private static final int M_FIJAS = 5; // primera columna de proveedor

    public jd_comparativo(int idComparativo) {
        this.idComparativo = idComparativo;
        initUI();
        cargar();
        reconstruirMatriz();
        recalcular();
        setLocationRelativeTo(null);
        metodos.addEscapeListenerWindowDialog(this);
    }

    private void initUI() {
        setModal(true);
        setUndecorated(true);
        setSize(1320, 800);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));
        root.add(EstiloCompras.header(FontAwesome.FILE_INVOICE, "Comparativo de cotizaciones", () -> dispose()),
                BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 10));
        cuerpo.setBackground(EstiloCompras.BG_FORM);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(12, 16, 14, 16));
        cuerpo.add(buildParams(), BorderLayout.NORTH);

        // proveedores arriba, matriz abajo
        mProv = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int f, int c) {
                return c == 2 || c == 3 || c == 4; // desc%, flete, condición
            }
        };
        mProv.setColumnIdentifiers(new Object[]{"ID_CP", "PROVEEDOR", "DESC. %", "FLETE", "CONDICIÓN PAGO"});
        tblProv = new JTable(mProv);
        EstiloCompras.styleTable(tblProv);
        tblProv.setRowHeight(30);
        EstiloCompras.ocultarColumna(tblProv, 0);
        EstiloCompras.anchoColumnas(tblProv, 0, 240, 90, 110, 200);
        mProv.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (!recomputing) {
                    recalcular();
                }
            }
        });

        mMatriz = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int f, int c) {
                // editable: PESO, CANT y las columnas de precio de cada proveedor
                if (c == M_PESO || c == M_CANT) {
                    return true;
                }
                return c >= M_FIJAS && c < M_FIJAS + provs.size();
            }
        };
        tblMatriz = new JTable(mMatriz);
        EstiloCompras.styleTable(tblMatriz);
        tblMatriz.setRowHeight(28);
        tblMatriz.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        mMatriz.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (!recomputing && e.getType() == TableModelEvent.UPDATE) {
                    recalcular();
                }
            }
        });

        JPanel pProv = new JPanel(new BorderLayout(0, 4));
        pProv.setOpaque(false);
        pProv.add(titulo("Proveedores (columnas) — digite descuento %, flete y condición"), BorderLayout.NORTH);
        pProv.add(EstiloCompras.scroll(tblProv), BorderLayout.CENTER);

        JPanel pMat = new JPanel(new BorderLayout(0, 4));
        pMat.setOpaque(false);
        pMat.add(titulo("Matriz comparativa — digite el precio de lista SIN IVA de cada proveedor"), BorderLayout.NORTH);
        pMat.add(new JScrollPane(tblMatriz), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pProv, pMat);
        split.setBorder(null);
        split.setOpaque(false);
        split.setResizeWeight(0.28);
        split.setDividerLocation(190);
        cuerpo.add(split, BorderLayout.CENTER);

        lbl_totales = new JLabel();
        lbl_totales.setVerticalAlignment(JLabel.TOP);
        lbl_totales.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JScrollPane spTot = new JScrollPane(lbl_totales);
        spTot.setBorder(null);
        spTot.setPreferredSize(new Dimension(100, 150));
        cuerpo.add(spTot, BorderLayout.SOUTH);

        root.add(cuerpo, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JLabel titulo(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(EstiloCompras.TEXT_SECONDARY);
        return l;
    }

    private JPanel buildParams() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        txt_iva = EstiloCompras.field("0.19", null);
        txt_iva.setText("0.19");
        txt_camion = EstiloCompras.field("30", null);
        txt_camion.setText("30");
        jbox_prov = new JComboBox<>();
        EstiloCompras.styleCombo(jbox_prov);
        jbox_prov.setPreferredSize(new Dimension(220, 34));
        new Contactos().MostrarNombreProveedores(jbox_prov);
        JButton btnAdd = EstiloCompras.primaryBtn("Agregar proveedor", FontAwesome.PLUS);
        btnAdd.addActionListener(e -> agregarProveedor());
        JButton btnDel = EstiloCompras.secondaryBtn("Quitar proveedor", FontAwesome.TRASH);
        btnDel.addActionListener(e -> quitarProveedor());

        javax.swing.JTextField fIva = txt_iva;
        fIva.addActionListener(e -> recalcular());
        txt_camion.addActionListener(e -> recalcular());

        p.add(EstiloCompras.labeled("IVA (ej. 0.19)", txt_iva, 130));
        p.add(Box.createHorizontalStrut(12));
        p.add(EstiloCompras.labeled("Capacidad camión (ton)", txt_camion, 170));
        p.add(Box.createHorizontalStrut(20));
        p.add(EstiloCompras.labeled("Proveedor", jbox_prov, 240));
        p.add(Box.createHorizontalStrut(8));
        JPanel botones = new JPanel();
        botones.setOpaque(false);
        botones.setLayout(new BoxLayout(botones, BoxLayout.Y_AXIS));
        botones.add(Box.createVerticalStrut(18));
        JPanel brow = new JPanel();
        brow.setOpaque(false);
        brow.setLayout(new BoxLayout(brow, BoxLayout.X_AXIS));
        brow.add(btnAdd);
        brow.add(Box.createHorizontalStrut(8));
        brow.add(btnDel);
        botones.add(brow);
        p.add(botones);
        p.add(Box.createHorizontalGlue());
        return p;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(EstiloCompras.BG_SECTION);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCompras.DIVIDER),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JButton btnGuardar = EstiloCompras.secondaryBtn("Guardar", FontAwesome.SAVE);
        btnGuardar.addActionListener(e -> guardar(true));
        JButton btnExcel = EstiloCompras.secondaryBtn("Exportar Excel", FontAwesome.LIST);
        btnExcel.addActionListener(e -> {
            try {
                new Metodos.ExportarExcel().exportarExcel(tblMatriz);
            } catch (Exception ex) {
            }
        });
        JButton btnPdf = EstiloCompras.secondaryBtn("Exportar PDF", FontAwesome.FILE_INVOICE);
        btnPdf.addActionListener(e -> {
            guardar(false);
            new Metodos.ImprimirComparativoPDF().imprimir(idComparativo);
        });
        JButton btnDecidir = EstiloCompras.successBtn("Decidir y autorizar", FontAwesome.CHECK);
        btnDecidir.addActionListener(e -> decidir());
        btn_emitir = EstiloCompras.primaryBtn("Emitir orden de compra", FontAwesome.FILE_INVOICE);
        btn_emitir.addActionListener(e -> emitir());

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(btnExcel);
        right.add(Box.createHorizontalStrut(8));
        right.add(btnPdf);
        right.add(Box.createHorizontalStrut(8));
        right.add(btnGuardar);
        right.add(Box.createHorizontalStrut(8));
        right.add(btnDecidir);
        right.add(Box.createHorizontalStrut(8));
        right.add(btn_emitir);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    // ===================== carga =====================
    private final Map<String, Double> preciosCargados = new HashMap<>(); // "idCp-idCv" -> precio

    private void cargar() {
        ResultSet rs = DBcomparativos.cargarCabecera(idComparativo);
        try {
            if (rs.next()) {
                txt_iva.setText(String.valueOf(rs.getDouble("iva_pct")));
                txt_camion.setText(String.valueOf(rs.getDouble("capacidad_camion_ton")));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        prods.clear();
        rs = DBcomparativos.cargarProductos(idComparativo);
        try {
            while (rs.next()) {
                prods.add(new Object[]{rs.getInt("id"), rs.getInt("id_producto"),
                    rs.getString("codigo_barras"), rs.getString("descripcion"),
                    rs.getDouble("peso_unitario"), rs.getDouble("cantidad")});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        provs.clear();
        rs = DBcomparativos.cargarProveedores(idComparativo);
        try {
            while (rs.next()) {
                DBcomparativos.Proveedor pv = new DBcomparativos.Proveedor();
                pv.idCompProv = rs.getInt("id");
                pv.idProveedor = rs.getInt("id_proveedor");
                pv.nombre = rs.getString("nombre");
                pv.descuento = rs.getDouble("descuento_pronto_pago");
                pv.flete = rs.getDouble("flete");
                pv.condicion = rs.getString("condicion_pago");
                provs.add(pv);
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        preciosCargados.clear();
        rs = DBcomparativos.cargarPrecios(idComparativo);
        try {
            while (rs.next()) {
                preciosCargados.put(rs.getInt("id_comp_producto") + "-" + rs.getInt("id_comp_proveedor"),
                        rs.getDouble("precio_lista"));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void reconstruirMatriz() {
        recomputing = true;
        // proveedores
        mProv.setRowCount(0);
        for (DBcomparativos.Proveedor pv : provs) {
            mProv.addRow(new Object[]{pv.idCompProv, pv.nombre,
                metodos.formateador_decimal().format(pv.descuento * 100),
                metodos.formateador_decimal().format(pv.flete), pv.condicion == null ? "" : pv.condicion});
        }
        // columnas de la matriz
        List<String> cols = new ArrayList<>();
        cols.add("ID_CP");
        cols.add("CÓDIGO");
        cols.add("DESCRIPCIÓN");
        cols.add("PESO");
        cols.add("CANT");
        for (DBcomparativos.Proveedor pv : provs) {
            cols.add(pv.nombre);
        }
        cols.add("MÍN NETO");
        cols.add("MEJOR PROV");
        cols.add("COSTO LÍNEA");
        cols.add("PESO TOTAL");
        mMatriz.setColumnIdentifiers(cols.toArray());
        mMatriz.setRowCount(0);
        for (Object[] pr : prods) {
            Object[] row = new Object[cols.size()];
            row[M_IDCP] = pr[0];
            row[M_COD] = pr[2];
            row[M_DESC] = pr[3];
            row[M_PESO] = metodos.formateador_decimal().format((Double) pr[4]);
            row[M_CANT] = metodos.formateador_decimal().format((Double) pr[5]);
            for (int j = 0; j < provs.size(); j++) {
                Double pre = preciosCargados.get(pr[0] + "-" + provs.get(j).idCompProv);
                row[M_FIJAS + j] = pre == null ? "" : metodos.formateador_decimal().format(pre);
            }
            mMatriz.addRow(row);
        }
        EstiloCompras.ocultarColumna(tblMatriz, M_IDCP);
        // anchos
        tblMatriz.getColumnModel().getColumn(M_COD).setPreferredWidth(90);
        tblMatriz.getColumnModel().getColumn(M_DESC).setPreferredWidth(280);
        tblMatriz.getColumnModel().getColumn(M_PESO).setPreferredWidth(60);
        tblMatriz.getColumnModel().getColumn(M_CANT).setPreferredWidth(60);
        for (int j = 0; j < provs.size(); j++) {
            tblMatriz.getColumnModel().getColumn(M_FIJAS + j).setPreferredWidth(110);
        }
        int base = M_FIJAS + provs.size();
        for (int k = 0; k < 4; k++) {
            tblMatriz.getColumnModel().getColumn(base + k).setPreferredWidth(k == 1 ? 150 : 110);
        }
        recomputing = false;
    }

    private double num(Object o) {
        if (o == null) {
            return Double.NaN;
        }
        String s = o.toString().trim();
        if (s.isEmpty()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(s.replace(".", "").replace(",", "."));
        } catch (Exception e) {
            try {
                return Double.parseDouble(s);
            } catch (Exception e2) {
                return Double.NaN;
            }
        }
    }

    private double ivaPct() {
        try {
            return Double.parseDouble(txt_iva.getText().replace(",", "."));
        } catch (Exception e) {
            return 0.19;
        }
    }

    private double camion() {
        try {
            return Double.parseDouble(txt_camion.getText().replace(",", "."));
        } catch (Exception e) {
            return 30;
        }
    }

    // ===================== motor de cálculo =====================
    private void recalcular() {
        if (recomputing || provs.isEmpty()) {
            actualizarTotalesVacio();
            return;
        }
        recomputing = true;
        try {
            // descuentos por proveedor desde la tabla de proveedores
            double[] desc = new double[provs.size()];
            double[] flete = new double[provs.size()];
            for (int j = 0; j < provs.size(); j++) {
                double d = num(mProv.getValueAt(j, 2));
                desc[j] = Double.isNaN(d) ? 0 : d / 100.0;
                double f = num(mProv.getValueAt(j, 3));
                flete[j] = Double.isNaN(f) ? 0 : f;
            }
            double iva = ivaPct();
            int base = M_FIJAS + provs.size();

            double[] subtotalBruto = new double[provs.size()];
            int[] itemsCotizados = new int[provs.size()];
            double costoMasBarato = 0; // suma costo línea (neto)
            double pesoTotalPedido = 0;

            for (int i = 0; i < mMatriz.getRowCount(); i++) {
                double cant = num(mMatriz.getValueAt(i, M_CANT));
                if (Double.isNaN(cant)) {
                    cant = 0;
                }
                double peso = num(mMatriz.getValueAt(i, M_PESO));
                if (Double.isNaN(peso)) {
                    peso = 0;
                }
                double minNeto = Double.NaN;
                int mejorJ = -1;
                for (int j = 0; j < provs.size(); j++) {
                    double lista = num(mMatriz.getValueAt(i, M_FIJAS + j));
                    if (Double.isNaN(lista)) {
                        continue;
                    }
                    itemsCotizados[j]++;
                    subtotalBruto[j] += lista * cant;
                    double neto = lista * (1 - desc[j]);
                    if (Double.isNaN(minNeto) || neto < minNeto) {
                        minNeto = neto;
                        mejorJ = j;
                    }
                }
                double costoLinea = Double.isNaN(minNeto) ? 0 : minNeto * cant;
                double pesoLinea = peso * cant;
                costoMasBarato += costoLinea;
                pesoTotalPedido += pesoLinea;

                mMatriz.setValueAt(Double.isNaN(minNeto) ? "" : metodos.formateador_decimal().format(minNeto), i, base);
                mMatriz.setValueAt(mejorJ < 0 ? "" : provs.get(mejorJ).nombre, i, base + 1);
                mMatriz.setValueAt(metodos.formateador_decimal().format(costoLinea), i, base + 2);
                mMatriz.setValueAt(metodos.formateador_decimal().format(pesoLinea), i, base + 3);
            }

            // totales por proveedor
            double mejorTotal = Double.NaN;
            int mejorProvUnico = -1;
            double[] totalBodega = new double[provs.size()];
            for (int j = 0; j < provs.size(); j++) {
                double bruto = subtotalBruto[j];
                double descMonto = bruto * desc[j];
                double neto = bruto - descMonto;
                double ivaMonto = neto * iva;
                double total = neto + ivaMonto + flete[j];
                totalBodega[j] = total;
                if (itemsCotizados[j] > 0 && (Double.isNaN(mejorTotal) || total < mejorTotal)) {
                    mejorTotal = total;
                    mejorProvUnico = j;
                }
            }

            double ton = pesoTotalPedido / 1000.0;
            int viajes = camion() > 0 ? (int) Math.ceil(ton / camion()) : 0;

            StringBuilder sb = new StringBuilder("<html><div style='font-family:Segoe UI;font-size:11px'>");
            sb.append("<b>TOTAL PUESTO EN BODEGA POR PROVEEDOR</b><br><table cellpadding=3>");
            sb.append("<tr><td><b>Proveedor</b></td><td><b>Ítems</b></td><td><b>Subtotal bruto</b></td>"
                    + "<td><b>Neto</b></td><td><b>IVA</b></td><td><b>Flete</b></td><td><b>Total bodega</b></td></tr>");
            for (int j = 0; j < provs.size(); j++) {
                double bruto = subtotalBruto[j];
                double neto = bruto * (1 - desc[j]);
                double ivaMonto = neto * iva;
                String fila = mejorProvUnico == j ? " style='background:#E8F5E9'" : "";
                sb.append("<tr").append(fila).append(">");
                sb.append("<td>").append(provs.get(j).nombre).append("</td>");
                sb.append("<td align=center>").append(itemsCotizados[j]).append("</td>");
                sb.append("<td align=right>$ ").append(fmt(bruto)).append("</td>");
                sb.append("<td align=right>$ ").append(fmt(neto)).append("</td>");
                sb.append("<td align=right>$ ").append(fmt(ivaMonto)).append("</td>");
                sb.append("<td align=right>$ ").append(fmt(flete[j])).append("</td>");
                sb.append("<td align=right><b>$ ").append(fmt(totalBodega[j])).append("</b></td></tr>");
            }
            sb.append("</table><br>");
            sb.append("<b>Recomendación:</b> ");
            if (mejorProvUnico >= 0) {
                sb.append("Mejor proveedor único: <b>").append(provs.get(mejorProvUnico).nombre)
                        .append("</b> (total $ ").append(fmt(mejorTotal)).append(")");
            }
            sb.append("<br>Costo si toma el más barato de cada producto (neto, sin flete/IVA): <b>$ ")
                    .append(fmt(costoMasBarato)).append("</b>");
            sb.append("<br><b>Peso total:</b> ").append(fmt(pesoTotalPedido)).append(" kg  (")
                    .append(fmt(ton)).append(" ton)  →  <b>Viajes necesarios: ").append(viajes).append("</b>");
            sb.append("</div></html>");
            lbl_totales.setText(sb.toString());
        } finally {
            recomputing = false;
        }
    }

    private void actualizarTotalesVacio() {
        lbl_totales.setText("<html><div style='font-family:Segoe UI;font-size:11px'>"
                + "Agregue proveedores y digite precios para ver los totales y la recomendación.</div></html>");
    }

    private String fmt(double v) {
        return metodos.formateador_decimal().format(v);
    }

    // ===================== acciones =====================
    private void agregarProveedor() {
        Contactos prov = (Contactos) jbox_prov.getSelectedItem();
        if (prov == null) {
            return;
        }
        for (DBcomparativos.Proveedor pv : provs) {
            if (pv.idProveedor == prov.getId()) {
                JOptionPane.showMessageDialog(this, "Ese proveedor ya está en el comparativo");
                return;
            }
        }
        if (provs.size() >= 10) {
            JOptionPane.showMessageDialog(this, "Máximo 10 proveedores");
            return;
        }
        guardar(false); // persistir precios actuales antes de reconstruir
        DBcomparativos dao = new DBcomparativos();
        int idCp = dao.agregarProveedor(idComparativo, prov.getId());
        if (idCp > 0) {
            cargar();
            reconstruirMatriz();
            recalcular();
        }
    }

    private void quitarProveedor() {
        int f = tblProv.getSelectedRow();
        if (f < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un proveedor");
            return;
        }
        int idCp = Integer.parseInt(mProv.getValueAt(f, 0).toString());
        guardar(false);
        if (new DBcomparativos().quitarProveedor(idCp)) {
            cargar();
            reconstruirMatriz();
            recalcular();
        }
    }

    private boolean guardar(boolean avisar) {
        if (tblMatriz.isEditing()) {
            tblMatriz.getCellEditor().stopCellEditing();
        }
        if (tblProv.isEditing()) {
            tblProv.getCellEditor().stopCellEditing();
        }
        // proveedores params
        for (int j = 0; j < provs.size(); j++) {
            double d = num(mProv.getValueAt(j, 2));
            provs.get(j).descuento = Double.isNaN(d) ? 0 : d / 100.0;
            double fl = num(mProv.getValueAt(j, 3));
            provs.get(j).flete = Double.isNaN(fl) ? 0 : fl;
            provs.get(j).condicion = String.valueOf(mProv.getValueAt(j, 4));
        }
        // precios
        List<int[]> keys = new ArrayList<>();
        List<Double> vals = new ArrayList<>();
        for (int i = 0; i < mMatriz.getRowCount(); i++) {
            int idCp = Integer.parseInt(mMatriz.getValueAt(i, M_IDCP).toString());
            for (int j = 0; j < provs.size(); j++) {
                double v = num(mMatriz.getValueAt(i, M_FIJAS + j));
                if (!Double.isNaN(v)) {
                    keys.add(new int[]{idCp, provs.get(j).idCompProv});
                    vals.add(v);
                }
            }
        }
        int[][] kArr = keys.toArray(new int[0][]);
        double[] vArr = new double[vals.size()];
        for (int i = 0; i < vals.size(); i++) {
            vArr[i] = vals.get(i);
        }
        boolean ok = new DBcomparativos().guardarMatriz(idComparativo, ivaPct(), camion(), provs, kArr, vArr);
        if (ok && avisar) {
            JOptionPane.showMessageDialog(this, "Comparativo guardado.");
        }
        return ok;
    }

    private void decidir() {
        if (provs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agregue proveedores y precios primero");
            return;
        }
        guardar(false);
        Object[] opts = {"Todo a un proveedor", "El más barato de cada producto", "Cancelar"};
        int sel = JOptionPane.showOptionDialog(this,
                "¿Cómo desea decidir la compra?", "Decisión de compra",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);
        if (sel == 2 || sel < 0) {
            return;
        }
        Integer idProvUnico = null;
        String decision;
        if (sel == 0) {
            // elegir proveedor único
            String[] nombres = new String[provs.size()];
            for (int j = 0; j < provs.size(); j++) {
                nombres[j] = provs.get(j).nombre;
            }
            String elegido = (String) JOptionPane.showInputDialog(this, "Proveedor único:",
                    "Proveedor", JOptionPane.QUESTION_MESSAGE, null, nombres, nombres[0]);
            if (elegido == null) {
                return;
            }
            for (DBcomparativos.Proveedor pv : provs) {
                if (pv.nombre.equals(elegido)) {
                    idProvUnico = pv.idProveedor;
                }
            }
            decision = "UNICO";
        } else {
            decision = "POR_PRODUCTO";
        }
        if (new DBcomparativos().decidirYAutorizar(idComparativo, decision, idProvUnico, frm_main.id_user) > 0) {
            JOptionPane.showMessageDialog(this, "Comparativo decidido y autorizado. Ya puede emitir la orden de compra.");
        }
    }

    private void emitir() {
        guardar(false);
        ResultSet rs = DBcomparativos.cargarCabecera(idComparativo);
        String decision = null;
        int idProvUnico = 0;
        int estado = 0;
        try {
            if (rs.next()) {
                decision = rs.getString("decision");
                idProvUnico = rs.getInt("id_proveedor_unico");
                estado = rs.getInt("estado");
            }
            rs.close();
        } catch (Exception e) {
        }
        if (estado != 2 || decision == null) {
            JOptionPane.showMessageDialog(this, "Primero debe 'Decidir y autorizar' el comparativo.");
            return;
        }

        // recomputar precios netos por línea/proveedor para construir la OC
        double[] desc = new double[provs.size()];
        for (int j = 0; j < provs.size(); j++) {
            desc[j] = provs.get(j).descuento;
        }

        List<modelos.Ordenes_compra_detalle> lineas = new ArrayList<>();
        for (int i = 0; i < mMatriz.getRowCount(); i++) {
            int idProducto = (Integer) prods.get(i)[1];
            double cant = num(mMatriz.getValueAt(i, M_CANT));
            if (Double.isNaN(cant)) {
                cant = 0;
            }
            Integer idProvLinea = null;
            double precioNeto = 0;
            if ("UNICO".equals(decision)) {
                int j = indiceProveedorPorId(idProvUnico);
                if (j >= 0) {
                    double lista = num(mMatriz.getValueAt(i, M_FIJAS + j));
                    if (!Double.isNaN(lista)) {
                        idProvLinea = idProvUnico;
                        precioNeto = lista * (1 - desc[j]);
                    }
                }
            } else { // POR_PRODUCTO: mejor proveedor de la fila
                double min = Double.NaN;
                int mejorJ = -1;
                for (int j = 0; j < provs.size(); j++) {
                    double lista = num(mMatriz.getValueAt(i, M_FIJAS + j));
                    if (Double.isNaN(lista)) {
                        continue;
                    }
                    double neto = lista * (1 - desc[j]);
                    if (Double.isNaN(min) || neto < min) {
                        min = neto;
                        mejorJ = j;
                    }
                }
                if (mejorJ >= 0) {
                    idProvLinea = provs.get(mejorJ).idProveedor;
                    precioNeto = min;
                }
            }
            if (idProvLinea == null) {
                continue; // sin precio cotizado, se omite
            }
            modelos.Ordenes_compra_detalle d = new modelos.Ordenes_compra_detalle();
            d.setId_producto(idProducto);
            d.setCantidad(cant);
            d.setId_proveedor(idProvLinea);
            d.setPrecio_unitario(precioNeto);
            lineas.add(d);
        }
        if (lineas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay líneas con precio para emitir la orden.");
            return;
        }
        int idOC = new DBordenes_compra().emitirDesdeComparativo(idComparativo, frm_main.id_user, ivaPct(), lineas);
        if (idOC > 0) {
            JOptionPane.showMessageDialog(this,
                    "Orden(es) de compra emitida(s) a partir del comparativo (N° interno " + idOC + ").\n"
                    + "Quedan en estado Aprobada en el módulo de Órdenes de compra.");
            dispose();
        }
    }

    private int indiceProveedorPorId(int idProveedor) {
        for (int j = 0; j < provs.size(); j++) {
            if (provs.get(j).idProveedor == idProveedor) {
                return j;
            }
        }
        return -1;
    }
}
