/*
 * Modulo Caja: Reportes propios de caja (no cuelgan del menu general de
 * Reportes). Mismo lenguaje visual SaaS claro de Ingresos/Egresos, con acento
 * indigo porque cubre las dos caras del dinero.
 *
 * Todo es HISTORICO: el saldo de un fondo = suma de ingresos - suma de egresos
 * de ese fondo desde el inicio. Los traslados entre fondos ya quedan incluidos
 * de forma natural porque generan un par egreso(origen) + ingreso(destino)
 * marcados con transferencia=1.
 */
package Caja;

import Metodos.ExportarExcel;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Reportes de caja: detalle de ingresos, detalle de egresos, movimientos de un
 * fondo con saldo acumulado, balance por fondo y balance general.
 *
 * @author Monkeyelgrande
 */
public class frm_reportes_caja extends javax.swing.JInternalFrame {

    private static final EstiloCaja.Accent ACENTO = EstiloCaja.REPORTES;

    // Tipos de reporte
    private static final int R_INGRESOS = 0;
    private static final int R_EGRESOS = 1;
    private static final int R_MOVIMIENTOS = 2;
    private static final int R_BALANCE_FONDO = 3;
    private static final int R_BALANCE_GENERAL = 4;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private final DecimalFormat fmt = metodos.formateador_dinero();

    /** Item id+nombre para los combos de filtro. */
    private static class Opcion {

        final int id;
        final String nombre;

        Opcion(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    // ---- Componentes ----
    private JComboBox<String> jbox_reporte;
    private JComboBox<Opcion> jbox_fondo;
    private JComboBox<Opcion> jbox_cuenta;
    private JComboBox<String> jbox_tipo;
    private JCheckBox chk_traslados;
    private com.toedter.calendar.JDateChooser date_desde;
    private com.toedter.calendar.JDateChooser date_hasta;
    private JButton btn_generar;
    private JButton btn_limpiar;
    private JButton btn_excel;
    private JButton btn_pdf;
    private JTable jtabla;
    private JLabel lblKpiIngresos;
    private JLabel lblKpiEgresos;
    private JLabel lblKpiBalance;
    private JLabel lblKpiSaldo;
    private JLabel lblKpiSaldoSub;
    private JLabel lblFootInfo;
    private JLabel lblTituloTabla;

    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int f, int c) {
            return false;
        }
    };

    /** Caja de la que se reporta: 1 = Caja, 2 = Caja Dos. */
    private final int idCaja;

    public frm_reportes_caja() {
        this(1);
    }

    public frm_reportes_caja(int idCaja) {
        this.idCaja = idCaja;
        buildUI();
        if (idCaja == 2) {
            setTitle("Reportes - Caja Dos");
        }
        EstiloCaja.styleTable(jtabla, ACENTO);
        cargarFondos();
        cargarCuentas();
        generar();
    }

    // =====================================================================
    //  UI
    // =====================================================================
    private void buildUI() {
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Reportes de caja");

        jbox_reporte = new JComboBox<>(new String[]{
            "Ingresos (detalle)",
            "Egresos (detalle)",
            "Movimientos de un fondo",
            "Balance por fondo",
            "Balance general"
        });
        jbox_fondo = new JComboBox<>();
        jbox_cuenta = new JComboBox<>();
        jbox_tipo = new JComboBox<>(new String[]{"Todos", "Empresa (E)", "Personal (P)"});
        chk_traslados = new JCheckBox("Incluir traslados entre fondos", true);
        date_desde = new com.toedter.calendar.JDateChooser();
        date_hasta = new com.toedter.calendar.JDateChooser();
        btn_generar = EstiloCaja.primary("Generar reporte", FontAwesome.SEARCH, ACENTO);
        btn_limpiar = EstiloCaja.ghost("Limpiar", null);
        btn_excel = EstiloCaja.ghost("Excel", FontAwesome.FILE_EXCEL);
        btn_pdf = EstiloCaja.ghost("PDF", FontAwesome.PRINTER);
        jtabla = new JTable();
        lblKpiIngresos = new JLabel("$ 0");
        lblKpiEgresos = new JLabel("$ 0");
        lblKpiBalance = new JLabel("$ 0");
        lblKpiSaldo = new JLabel("$ 0");
        lblKpiSaldoSub = EstiloCaja.title("Saldo histórico acumulado", 12, Font.PLAIN, EstiloCaja.TEXT_3);
        lblFootInfo = EstiloCaja.title("", 12, Font.PLAIN, EstiloCaja.TEXT_3);
        lblTituloTabla = EstiloCaja.title("Resultados", 14, Font.BOLD, EstiloCaja.TEXT);

        EstiloCaja.styleCombo(jbox_reporte, ACENTO);
        EstiloCaja.styleCombo(jbox_fondo, ACENTO);
        EstiloCaja.styleCombo(jbox_cuenta, ACENTO);
        EstiloCaja.styleCombo(jbox_tipo, ACENTO);
        chk_traslados.setOpaque(false);
        chk_traslados.setFont(EstiloCaja.font(Font.PLAIN, 12));
        chk_traslados.setToolTipText("Los traslados generan un egreso en el fondo origen y un ingreso "
                + "en el destino. Desmárcalo para ver solo movimiento operativo.");

        // Rango por defecto: mes actual
        Calendar c = new GregorianCalendar();
        date_hasta.setDate(c.getTime());
        Calendar ini = new GregorianCalendar();
        ini.set(Calendar.DAY_OF_MONTH, 1);
        date_desde.setDate(ini.getTime());

        jbox_reporte.addActionListener(e -> {
            cargarCuentas();
            ajustarFiltros();
        });
        btn_generar.addActionListener(e -> generar());
        btn_limpiar.addActionListener(e -> limpiarFiltros());
        btn_excel.addActionListener(e -> exportarExcel());
        btn_pdf.addActionListener(e -> exportarPDF());

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(EstiloCaja.BG);
        root.setBorder(EstiloCaja.pad(18, 20, 20, 20));
        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel ws = new JPanel(new BorderLayout(16, 0));
        ws.setOpaque(false);
        ws.add(buildFiltros(), BorderLayout.WEST);
        ws.add(buildTabla(), BorderLayout.CENTER);
        root.add(ws, BorderLayout.CENTER);

        setContentPane(root);
        setSize(1260, 800);
        ajustarFiltros();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel h1 = EstiloCaja.title("Reportes de caja", 22, Font.BOLD, ACENTO.base);
        JLabel sub = EstiloCaja.title("Ingresos, egresos, balances y movimientos por fondo", 13, Font.PLAIN, EstiloCaja.TEXT_3);
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(h1);
        titles.add(Box.createVerticalStrut(2));
        titles.add(sub);

        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.X_AXIS));
        wrap.add(EstiloCaja.accentBar(ACENTO.base, 5, 46));
        wrap.add(Box.createHorizontalStrut(12));
        wrap.add(titles);
        titleRow.add(wrap, BorderLayout.WEST);

        JPanel acc = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acc.setOpaque(false);
        acc.add(EstiloCaja.title("Exportar:", 12, Font.PLAIN, EstiloCaja.TEXT_3));
        acc.add(btn_excel);
        acc.add(btn_pdf);
        titleRow.add(acc, BorderLayout.EAST);

        header.add(titleRow);
        header.add(Box.createVerticalStrut(16));

        JPanel kpis = new JPanel(new GridLayout(1, 4, 14, 0));
        kpis.setOpaque(false);
        kpis.setAlignmentX(Component.LEFT_ALIGNMENT);
        kpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        kpis.add(kpiCard("Ingresos del periodo", FontAwesome.ARROW_DOWN, lblKpiIngresos,
                EstiloCaja.title("Entradas de dinero", 12, Font.PLAIN, EstiloCaja.TEXT_3),
                EstiloCaja.INGRESOS.base));
        kpis.add(kpiCard("Egresos del periodo", FontAwesome.ARROW_UP, lblKpiEgresos,
                EstiloCaja.title("Salidas de dinero", 12, Font.PLAIN, EstiloCaja.TEXT_3),
                EstiloCaja.EGRESOS.base));
        kpis.add(kpiCard("Balance del periodo", FontAwesome.BALANCE, lblKpiBalance,
                EstiloCaja.title("Ingresos − egresos", 12, Font.PLAIN, EstiloCaja.TEXT_3), null));
        kpis.add(kpiCard("Saldo acumulado", FontAwesome.CALCULATOR, lblKpiSaldo, lblKpiSaldoSub, ACENTO.base));
        header.add(kpis);
        return header;
    }

    private JPanel kpiCard(String label, String glyph, JLabel value, JLabel foot, Color color) {
        EstiloCaja.Card c = EstiloCaja.card(new BorderLayout());
        c.setBorder(EstiloCaja.pad(color != null ? 16 : 14, 16, 12, 16));
        if (color != null) {
            c.setAccentTop(color);
        }
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(EstiloCaja.title(label, 12, Font.PLAIN, EstiloCaja.TEXT_3), BorderLayout.WEST);
        top.add(new JLabel(FontAwesome.icon(glyph, 15f, color != null ? color : EstiloCaja.TEXT_3)), BorderLayout.EAST);
        value.setFont(EstiloCaja.font(Font.BOLD, 24));
        value.setForeground(color != null ? color : EstiloCaja.TEXT);
        value.setBorder(EstiloCaja.pad(10, 0, 4, 0));
        c.add(top, BorderLayout.NORTH);
        c.add(value, BorderLayout.CENTER);
        if (foot != null) {
            c.add(foot, BorderLayout.SOUTH);
        }
        return c;
    }

    private EstiloCaja.Card buildFiltros() {
        EstiloCaja.Card card = EstiloCaja.card(new BorderLayout());
        card.setPreferredSize(new Dimension(330, 10));
        card.setAccentTop(ACENTO.base);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, EstiloCaja.LINE),
                EstiloCaja.pad(16, 16, 14, 16)));
        JPanel hl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        hl.setOpaque(false);
        hl.add(new JLabel(FontAwesome.icon(FontAwesome.LIST, 15f, ACENTO.base)));
        JPanel ht = new JPanel();
        ht.setOpaque(false);
        ht.setLayout(new BoxLayout(ht, BoxLayout.Y_AXIS));
        ht.add(EstiloCaja.title("Filtros", 15, Font.BOLD, EstiloCaja.TEXT));
        ht.add(EstiloCaja.title("Configura y genera el reporte", 12, Font.PLAIN, EstiloCaja.TEXT_3));
        hl.add(ht);
        head.add(hl, BorderLayout.WEST);
        card.add(head, BorderLayout.NORTH);

        EstiloCaja.FormPanel form = new EstiloCaja.FormPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(EstiloCaja.pad(16, 16, 16, 16));

        addField(form, labeled("Tipo de reporte", jbox_reporte, 62));
        addField(form, row2(labeled("Desde", date_desde, 62), labeled("Hasta", date_hasta, 62)));
        addField(form, labeled("Fondo", jbox_fondo, 62));
        addField(form, labeled("Cuenta", jbox_cuenta, 62));
        addField(form, labeled("Tipo (Empresa / Personal)", jbox_tipo, 62));

        JPanel tras = new JPanel();
        tras.setOpaque(false);
        tras.setLayout(new BoxLayout(tras, BoxLayout.X_AXIS));
        tras.add(chk_traslados);
        tras.add(Box.createHorizontalGlue());
        addField(form, tras);

        JLabel nota = new JLabel("<html><body style='width:250px'>"
                + "Los <b>traslados</b> mueven dinero entre fondos: restan en el origen y suman en el destino. "
                + "El <b>saldo acumulado</b> es histórico (desde el inicio hasta la fecha «Hasta»)."
                + "</body></html>");
        nota.setFont(EstiloCaja.font(Font.PLAIN, 11));
        nota.setForeground(EstiloCaja.TEXT_3);
        nota.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(nota);

        JScrollPane sc = new JScrollPane(form);
        sc.setBorder(null);
        sc.setOpaque(false);
        sc.getViewport().setOpaque(false);
        sc.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.getVerticalScrollBar().setUnitIncrement(16);
        card.add(sc, BorderLayout.CENTER);

        JPanel foot = new JPanel(new GridLayout(1, 2, 10, 0));
        foot.setOpaque(false);
        foot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCaja.LINE),
                EstiloCaja.pad(12, 16, 14, 16)));
        foot.add(btn_limpiar);
        foot.add(btn_generar);
        card.add(foot, BorderLayout.SOUTH);
        return card;
    }

    private EstiloCaja.Card buildTabla() {
        EstiloCaja.Card card = EstiloCaja.card(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, EstiloCaja.LINE),
                EstiloCaja.pad(12, 16, 12, 16)));
        top.add(lblTituloTabla, BorderLayout.WEST);
        card.add(top, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(jtabla);
        sp.setBorder(null);
        sp.getViewport().setBackground(EstiloCaja.SURFACE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        card.add(sp, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCaja.LINE),
                EstiloCaja.pad(11, 16, 11, 16)));
        foot.add(lblFootInfo, BorderLayout.WEST);
        card.add(foot, BorderLayout.SOUTH);
        return card;
    }

    // ---- helpers de layout ----
    private void addField(JPanel form, JComponent comp) {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(comp);
        form.add(Box.createVerticalStrut(12));
    }

    private JPanel labeled(String text, JComponent field, int height) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = EstiloCaja.fieldLabel(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, height - 22));
        p.add(l);
        p.add(Box.createVerticalStrut(5));
        p.add(field);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return p;
    }

    private JPanel row2(JComponent a, JComponent b) {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        p.add(a);
        p.add(b);
        return p;
    }

    // =====================================================================
    //  Filtros
    // =====================================================================
    private void cargarFondos() {
        DefaultComboBoxModel<Opcion> m = new DefaultComboBoxModel<>();
        m.addElement(new Opcion(0, "Todos los fondos"));
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id, nombre from fondos where id_caja=" + idCaja + " order by nombre");
            while (rs.next()) {
                m.addElement(new Opcion(rs.getInt("id"), rs.getString("nombre")));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("cargarFondos: " + e);
        }
        jbox_fondo.setModel(m);
    }

    private void cargarCuentas() {
        boolean egresos = jbox_reporte.getSelectedIndex() == R_EGRESOS;
        DefaultComboBoxModel<Opcion> m = new DefaultComboBoxModel<>();
        m.addElement(new Opcion(0, "Todas las cuentas"));
        String tabla = egresos ? "cuentas_egresos" : "cuentas_ingresos";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id, nombre from " + tabla + " where id_caja=" + idCaja + " order by nombre");
            while (rs.next()) {
                m.addElement(new Opcion(rs.getInt("id"), rs.getString("nombre")));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("cargarCuentas: " + e);
        }
        jbox_cuenta.setModel(m);
    }

    /** Habilita solo los filtros que aplican al reporte elegido. */
    private void ajustarFiltros() {
        int r = jbox_reporte.getSelectedIndex();
        boolean detalle = (r == R_INGRESOS || r == R_EGRESOS);
        boolean movimientos = (r == R_MOVIMIENTOS);
        boolean balanceGeneral = (r == R_BALANCE_GENERAL);

        jbox_cuenta.setEnabled(detalle);
        // El tipo (Empresa/Personal) aplica al detalle y también al balance general.
        jbox_tipo.setEnabled(detalle || balanceGeneral);
        // El fondo se puede filtrar en todos los reportes. En "Movimientos de un
        // fondo" es obligatorio (no "Todos").
        jbox_fondo.setEnabled(true);
        chk_traslados.setEnabled(r != R_BALANCE_FONDO && r != R_BALANCE_GENERAL);

        if (movimientos && jbox_fondo.getItemCount() > 1
                && ((Opcion) jbox_fondo.getSelectedItem()).id == 0) {
            jbox_fondo.setSelectedIndex(1);
        }
    }

    private void limpiarFiltros() {
        Calendar c = new GregorianCalendar();
        date_hasta.setDate(c.getTime());
        Calendar ini = new GregorianCalendar();
        ini.set(Calendar.DAY_OF_MONTH, 1);
        date_desde.setDate(ini.getTime());
        if (jbox_fondo.getItemCount() > 0) {
            jbox_fondo.setSelectedIndex(0);
        }
        if (jbox_cuenta.getItemCount() > 0) {
            jbox_cuenta.setSelectedIndex(0);
        }
        jbox_tipo.setSelectedIndex(0);
        chk_traslados.setSelected(true);
        ajustarFiltros();
    }

    private String desde() {
        return date_desde.getDate() == null ? "1900-01-01" : sdf.format(date_desde.getDate());
    }

    private String hasta() {
        return date_hasta.getDate() == null ? "2999-12-31" : sdf.format(date_hasta.getDate());
    }

    private int fondoId() {
        Object o = jbox_fondo.getSelectedItem();
        return o == null ? 0 : ((Opcion) o).id;
    }

    private int cuentaId() {
        Object o = jbox_cuenta.getSelectedItem();
        return o == null ? 0 : ((Opcion) o).id;
    }

    /** -1 = todos, 1 = Empresa, 0 = Personal. */
    private int tipoSel() {
        switch (jbox_tipo.getSelectedIndex()) {
            case 1:
                return 1;
            case 2:
                return 0;
            default:
                return -1;
        }
    }

    // =====================================================================
    //  Generacion de reportes
    // =====================================================================
    private void generar() {
        switch (jbox_reporte.getSelectedIndex()) {
            case R_INGRESOS:
                reporteDetalle(true);
                break;
            case R_EGRESOS:
                reporteDetalle(false);
                break;
            case R_MOVIMIENTOS:
                reporteMovimientosFondo();
                break;
            case R_BALANCE_FONDO:
                reporteBalancePorFondo();
                break;
            default:
                reporteBalanceGeneral();
                break;
        }
    }

    /** Filtros comunes de un detalle (fondo, cuenta, tipo, traslados). */
    private String filtrosDetalle(String alias) {
        StringBuilder w = new StringBuilder();
        if (fondoId() > 0) {
            w.append(" and ").append(alias).append(".id_fondo=").append(fondoId());
        }
        if (cuentaId() > 0) {
            w.append(" and ").append(alias).append(".id_cuenta=").append(cuentaId());
        }
        if (tipoSel() >= 0) {
            w.append(" and ").append(alias).append(".factura_remision=").append(tipoSel());
        }
        if (!chk_traslados.isSelected()) {
            w.append(" and coalesce(").append(alias).append(".transferencia,0)=0");
        }
        return w.toString();
    }

    /** Ingresos o egresos en detalle. */
    private void reporteDetalle(boolean ingresos) {
        String tabla = ingresos ? "ingresos" : "egresos";
        String cuentas = ingresos ? "cuentas_ingresos" : "cuentas_egresos";
        String sql = "select m.id, m.fecha, m.hora, coalesce(c.nombre,'') as cuenta, "
                + "coalesce(co.nombre,'') as contacto, coalesce(m.descripcion,'') as descripcion, "
                + "coalesce(f.nombre,'Sin fondo') as fondo, m.factura_remision, "
                + "coalesce(m.transferencia,0) as transferencia, m.total\n"
                + "from " + tabla + " m\n"
                + "left join " + cuentas + " c on m.id_cuenta=c.id\n"
                + "left join contactos co on m.id_cliente=co.id\n"
                + "left join fondos f on m.id_fondo=f.id\n"
                + "where m.id_caja=" + idCaja
                + " and m.fecha between cast('" + desde() + "' as date) and cast('" + hasta() + "' as date)"
                + filtrosDetalle("m")
                + "\norder by m.fecha desc, m.id desc";

        modelo.setColumnIdentifiers(new Object[]{"id", "Fecha", "Hora", "Cuenta", "Contacto",
            "Descripcion", "Fondo", "Tipo", "Traslado", "Total"});
        limpiarFilas();
        double total = 0;
        int n = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                double v = rs.getDouble("total");
                total += v;
                n++;
                modelo.addRow(new Object[]{
                    rs.getString("id"), rs.getDate("fecha"), rs.getString("hora"),
                    rs.getString("cuenta"), rs.getString("contacto"), rs.getString("descripcion"),
                    rs.getString("fondo"), rs.getInt("factura_remision") == 1 ? "E" : "P",
                    rs.getInt("transferencia") == 1 ? "Sí" : "No", fmt.format(v)});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("reporteDetalle: " + e);
        }
        jtabla.setModel(modelo);
        anchos(new int[]{50, 85, 65, 130, 160, 260, 120, 55, 75, 120});
        renderers(new int[]{9}, new int[]{0, 1, 2}, 7, 8);

        lblTituloTabla.setText(ingresos ? "Ingresos en detalle" : "Egresos en detalle");
        lblFootInfo.setText(n + " registros · " + rango());
        if (ingresos) {
            setKpis(total, 0, saldoAcumulado());
        } else {
            setKpis(0, total, saldoAcumulado());
        }
    }

    /** Movimientos de un fondo con saldo acumulado (histórico). */
    private void reporteMovimientosFondo() {
        int idF = fondoId();
        if (idF <= 0) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Seleccione un fondo específico para ver sus movimientos.");
            return;
        }
        String nombreFondo = jbox_fondo.getSelectedItem().toString();
        String extra = chk_traslados.isSelected() ? "" : " and coalesce(transferencia,0)=0";

        // Saldo anterior al rango (histórico)
        double saldo = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(
                    "select coalesce((select sum(total) from ingresos where id_fondo=" + idF
                    + " and fecha < cast('" + desde() + "' as date)" + extra + "),0)"
                    + " - coalesce((select sum(total) from egresos where id_fondo=" + idF
                    + " and fecha < cast('" + desde() + "' as date)" + extra + "),0) as saldo");
            if (rs.next()) {
                saldo = rs.getDouble("saldo");
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("saldo anterior: " + e);
        }

        String sql = "select fecha, hora, clase, descripcion, cuenta, contacto, transferencia, entrada, salida from (\n"
                + "  select i.fecha, i.hora, 'Ingreso' as clase, coalesce(i.descripcion,'') as descripcion,\n"
                + "         coalesce(c.nombre,'') as cuenta, coalesce(co.nombre,'') as contacto,\n"
                + "         coalesce(i.transferencia,0) as transferencia, i.total as entrada, 0 as salida, i.id as oid\n"
                + "  from ingresos i left join cuentas_ingresos c on i.id_cuenta=c.id\n"
                + "  left join contactos co on i.id_cliente=co.id\n"
                + "  where i.id_fondo=" + idF + " and i.fecha between cast('" + desde() + "' as date) and cast('" + hasta() + "' as date)"
                + (chk_traslados.isSelected() ? "" : " and coalesce(i.transferencia,0)=0") + "\n"
                + "  union all\n"
                + "  select e.fecha, e.hora, 'Egreso', coalesce(e.descripcion,''),\n"
                + "         coalesce(c.nombre,''), coalesce(co.nombre,''),\n"
                + "         coalesce(e.transferencia,0), 0, e.total, e.id\n"
                + "  from egresos e left join cuentas_egresos c on e.id_cuenta=c.id\n"
                + "  left join contactos co on e.id_cliente=co.id\n"
                + "  where e.id_fondo=" + idF + " and e.fecha between cast('" + desde() + "' as date) and cast('" + hasta() + "' as date)"
                + (chk_traslados.isSelected() ? "" : " and coalesce(e.transferencia,0)=0") + "\n"
                + ") mov order by fecha asc, hora asc, oid asc";

        modelo.setColumnIdentifiers(new Object[]{"Fecha", "Hora", "Movimiento", "Cuenta", "Contacto",
            "Descripcion", "Traslado", "Entrada", "Salida", "Saldo"});
        limpiarFilas();

        // Fila inicial con el saldo anterior
        modelo.addRow(new Object[]{"", "", "Saldo anterior", "", "", "Acumulado hasta " + desde(),
            "", "", "", fmt.format(saldo)});

        double totalIn = 0, totalOut = 0;
        int n = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                double in = rs.getDouble("entrada");
                double out = rs.getDouble("salida");
                saldo += in - out;
                totalIn += in;
                totalOut += out;
                n++;
                modelo.addRow(new Object[]{
                    rs.getDate("fecha"), rs.getString("hora"), rs.getString("clase"),
                    rs.getString("cuenta"), rs.getString("contacto"), rs.getString("descripcion"),
                    rs.getInt("transferencia") == 1 ? "Sí" : "No",
                    in == 0 ? "" : fmt.format(in),
                    out == 0 ? "" : fmt.format(out),
                    fmt.format(saldo)});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("reporteMovimientosFondo: " + e);
        }
        jtabla.setModel(modelo);
        anchos(new int[]{85, 65, 95, 130, 150, 240, 75, 110, 110, 125});
        renderers(new int[]{7, 8, 9}, new int[]{0, 1}, -1, 6);

        lblTituloTabla.setText("Movimientos de " + nombreFondo);
        lblFootInfo.setText(n + " movimientos · " + rango() + " · saldo final " + fmt.format(saldo));
        setKpis(totalIn, totalOut, saldo);
        lblKpiSaldoSub.setText("Saldo de " + nombreFondo);
    }

    /** Balance por fondo: movimiento del periodo + saldo histórico. */
    private void reporteBalancePorFondo() {
        String sql = "select f.id, f.nombre,\n"
                + " coalesce((select sum(i.total) from ingresos i where i.id_fondo=f.id"
                + "   and i.fecha between cast('" + desde() + "' as date) and cast('" + hasta() + "' as date)),0) as ing,\n"
                + " coalesce((select sum(e.total) from egresos e where e.id_fondo=f.id"
                + "   and e.fecha between cast('" + desde() + "' as date) and cast('" + hasta() + "' as date)),0) as egr,\n"
                + " coalesce((select sum(i2.total) from ingresos i2 where i2.id_fondo=f.id"
                + "   and i2.fecha <= cast('" + hasta() + "' as date)),0)\n"
                + " - coalesce((select sum(e2.total) from egresos e2 where e2.id_fondo=f.id"
                + "   and e2.fecha <= cast('" + hasta() + "' as date)),0) as saldo\n"
                + "from fondos f\n"
                + "where f.id_caja=" + idCaja + (fondoId() > 0 ? " and f.id=" + fondoId() : "") + "\n"
                + "order by f.nombre";

        modelo.setColumnIdentifiers(new Object[]{"Fondo", "Ingresos periodo", "Egresos periodo",
            "Neto periodo", "Saldo acumulado"});
        limpiarFilas();
        double tIn = 0, tOut = 0, tSaldo = 0;
        int n = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                double in = rs.getDouble("ing");
                double out = rs.getDouble("egr");
                double sal = rs.getDouble("saldo");
                tIn += in;
                tOut += out;
                tSaldo += sal;
                n++;
                modelo.addRow(new Object[]{rs.getString("nombre"), fmt.format(in), fmt.format(out),
                    fmt.format(in - out), fmt.format(sal)});
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("reporteBalancePorFondo: " + e);
        }
        modelo.addRow(new Object[]{"TOTAL", fmt.format(tIn), fmt.format(tOut),
            fmt.format(tIn - tOut), fmt.format(tSaldo)});
        jtabla.setModel(modelo);
        anchos(new int[]{200, 150, 150, 150, 160});
        renderers(new int[]{1, 2, 3, 4}, new int[]{}, -1, -1);

        lblTituloTabla.setText("Balance por fondo");
        lblFootInfo.setText(n + " fondos · " + rango() + " · saldo total " + fmt.format(tSaldo));
        setKpis(tIn, tOut, tSaldo);
        lblKpiSaldoSub.setText("Saldo histórico de todos los fondos");
    }

    /** Balance general: ingresos vs egresos del periodo, por tipo y por cuenta. */
    private void reporteBalanceGeneral() {
        modelo.setColumnIdentifiers(new Object[]{"Concepto", "Detalle", "Valor"});
        limpiarFilas();

        int tsel = tipoSel(); // -1 = todos, 1 = Empresa, 0 = Personal
        double ing = suma("ingresos", true);
        double egr = suma("egresos", true);
        double saldo = saldoAcumulado();

        // Cuando se filtra por un tipo, el "Total" ya viene filtrado (suma respeta
        // tipoSel), así que solo se muestra la línea de desglose correspondiente.
        modelo.addRow(new Object[]{"INGRESOS", "Total del periodo", fmt.format(ing)});
        if (tsel != 0) {
            modelo.addRow(new Object[]{"", "  Empresa (E)", fmt.format(sumaTipo("ingresos", 1))});
        }
        if (tsel != 1) {
            modelo.addRow(new Object[]{"", "  Personal (P)", fmt.format(sumaTipo("ingresos", 0))});
        }
        modelo.addRow(new Object[]{"EGRESOS", "Total del periodo", fmt.format(egr)});
        if (tsel != 0) {
            modelo.addRow(new Object[]{"", "  Empresa (E)", fmt.format(sumaTipo("egresos", 1))});
        }
        if (tsel != 1) {
            modelo.addRow(new Object[]{"", "  Personal (P)", fmt.format(sumaTipo("egresos", 0))});
        }
        modelo.addRow(new Object[]{"BALANCE", "Ingresos − egresos del periodo", fmt.format(ing - egr)});
        modelo.addRow(new Object[]{"SALDO", "Acumulado histórico hasta " + hasta(), fmt.format(saldo)});

        jtabla.setModel(modelo);
        anchos(new int[]{160, 380, 170});
        renderers(new int[]{2}, new int[]{}, -1, -1);

        lblTituloTabla.setText("Balance general");
        lblFootInfo.setText(rango());
        setKpis(ing, egr, saldo);
        lblKpiSaldoSub.setText(fondoId() > 0
                ? "Saldo histórico de " + jbox_fondo.getSelectedItem()
                : "Saldo histórico de todos los fondos");
    }

    // ---- utilidades de consulta ----
    private double suma(String tabla, boolean rango) {
        String w = "where id_caja=" + idCaja
                + " and fecha between cast('" + desde() + "' as date) and cast('" + hasta() + "' as date)";
        if (fondoId() > 0) {
            w += " and id_fondo=" + fondoId();
        }
        if (tipoSel() >= 0) {
            w += " and factura_remision=" + tipoSel();
        }
        if (!chk_traslados.isSelected()) {
            w += " and coalesce(transferencia,0)=0";
        }
        return escalar("select coalesce(sum(total),0) as v from " + tabla + " " + w);
    }

    private double sumaTipo(String tabla, int tipo) {
        String w = "where id_caja=" + idCaja
                + " and fecha between cast('" + desde() + "' as date) and cast('" + hasta() + "' as date)"
                + " and factura_remision=" + tipo;
        if (fondoId() > 0) {
            w += " and id_fondo=" + fondoId();
        }
        if (!chk_traslados.isSelected()) {
            w += " and coalesce(transferencia,0)=0";
        }
        return escalar("select coalesce(sum(total),0) as v from " + tabla + " " + w);
    }

    /** Saldo historico (todo desde el inicio hasta "Hasta"), respetando el fondo elegido. */
    private double saldoAcumulado() {
        String wf = " and id_caja=" + idCaja + (fondoId() > 0 ? " and id_fondo=" + fondoId() : "")
                + (tipoSel() >= 0 ? " and factura_remision=" + tipoSel() : "");
        return escalar("select coalesce((select sum(total) from ingresos where fecha <= cast('" + hasta()
                + "' as date)" + wf + "),0) - coalesce((select sum(total) from egresos where fecha <= cast('"
                + hasta() + "' as date)" + wf + "),0) as v");
    }

    private double escalar(String sql) {
        double v = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            if (rs.next()) {
                v = rs.getDouble("v");
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("escalar: " + e);
        }
        return v;
    }

    private String rango() {
        return "del " + desde() + " al " + hasta();
    }

    private void setKpis(double ing, double egr, double saldo) {
        lblKpiIngresos.setText("$ " + fmt.format(ing));
        lblKpiEgresos.setText("$ " + fmt.format(egr));
        double bal = ing - egr;
        lblKpiBalance.setText("$ " + fmt.format(bal));
        lblKpiBalance.setForeground(bal < 0 ? EstiloCaja.EGRESOS.base : EstiloCaja.INGRESOS.base);
        lblKpiSaldo.setText("$ " + fmt.format(saldo));
        lblKpiSaldo.setForeground(saldo < 0 ? EstiloCaja.EGRESOS.base : ACENTO.base);
    }

    private void limpiarFilas() {
        modelo.setRowCount(0);
    }

    private void anchos(int[] w) {
        for (int i = 0; i < w.length && i < jtabla.getColumnModel().getColumnCount(); i++) {
            jtabla.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }
    }

    /**
     * Reaplica los renderers despues de cada setModel.
     *
     * @param money columnas de dinero, mutedCols columnas atenuadas,
     * tipoCol columna E/P (-1 si no aplica), siNoCol columna Sí/No (-1 si no aplica)
     */
    private void renderers(int[] money, int[] mutedCols, int tipoCol, int siNoCol) {
        int n = jtabla.getColumnModel().getColumnCount();
        for (int c : money) {
            if (c < n) {
                jtabla.getColumnModel().getColumn(c).setCellRenderer(new EstiloCaja.MoneyRenderer());
            }
        }
        for (int c : mutedCols) {
            if (c < n) {
                jtabla.getColumnModel().getColumn(c).setCellRenderer(new EstiloCaja.MutedRenderer());
            }
        }
        if (tipoCol >= 0 && tipoCol < n) {
            jtabla.getColumnModel().getColumn(tipoCol).setCellRenderer(new EstiloCaja.TipoRenderer(ACENTO));
        }
        if (siNoCol >= 0 && siNoCol < n) {
            jtabla.getColumnModel().getColumn(siNoCol).setCellRenderer(new EstiloCaja.SiNoRenderer(ACENTO));
        }
    }

    private void exportarExcel() {
        try {
            new ExportarExcel().exportarExcel(jtabla);
        } catch (IOException ex) {
            System.out.println("exportarExcel: " + ex);
        }
    }

    /** Exporta el reporte actual a un PDF con presentacion empresarial. */
    private void exportarPDF() {
        // Subtitulo con el rango y los filtros que realmente se aplicaron
        StringBuilder sub = new StringBuilder(rango());
        if (fondoId() > 0) {
            sub.append("  ·  Fondo: ").append(jbox_fondo.getSelectedItem());
        }
        if (jbox_cuenta.isEnabled() && cuentaId() > 0) {
            sub.append("  ·  Cuenta: ").append(jbox_cuenta.getSelectedItem());
        }
        if (jbox_tipo.isEnabled() && tipoSel() >= 0) {
            sub.append("  ·  Tipo: ").append(jbox_tipo.getSelectedItem());
        }
        if (chk_traslados.isEnabled() && !chk_traslados.isSelected()) {
            sub.append("  ·  Sin traslados");
        }

        ReportePDFCaja.Kpi[] kpis = new ReportePDFCaja.Kpi[]{
            new ReportePDFCaja.Kpi("Ingresos del periodo", lblKpiIngresos.getText(), EstiloCaja.INGRESOS.base),
            new ReportePDFCaja.Kpi("Egresos del periodo", lblKpiEgresos.getText(), EstiloCaja.EGRESOS.base),
            new ReportePDFCaja.Kpi("Balance del periodo", lblKpiBalance.getText(), lblKpiBalance.getForeground()),
            new ReportePDFCaja.Kpi("Saldo acumulado", lblKpiSaldo.getText(), lblKpiSaldo.getForeground())
        };

        ReportePDFCaja.exportar(jtabla, lblTituloTabla.getText(), sub.toString(),
                kpis, ACENTO.base, this);
    }
}
