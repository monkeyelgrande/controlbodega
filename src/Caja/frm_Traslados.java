/*
 * Modulo Caja: traslados de dinero entre fondos.
 * Rediseno "SaaS claro" (mismo lenguaje visual de Ingresos/Egresos): hand-coded
 * con FlatLaf + Font Awesome via el helper EstiloCaja. Color de identidad AZUL:
 * en un traslado el dinero no entra ni sale, solo se mueve de un fondo a otro.
 * El par ingreso/egreso lo sigue creando DB_transferencias.GuardarTraslado en
 * una sola transaccion. Toda la funcionalidad previa se conserva.
 */
package Caja;

import Formularios.frm_main;
import Metodos.ExportarExcel;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.AuditoriaCaja;
import conexiondb.DB_consultas_R_D;
import conexiondb.DB_transferencias;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Cuentas_Egresos;
import modelos.Cuentas_Ingresos;
import modelos.Fondos;
import modelos.Transferencias;

/**
 * Formulario de Traslados de dinero entre fondos (rediseno SaaS claro).
 *
 * @author Monkeyelgrande
 */
public class frm_Traslados extends javax.swing.JInternalFrame {

    /**
     * Color de identidad del modulo: AZUL = el dinero se mueve entre fondos
     * (verde = Ingresos, rojo = Egresos).
     */
    private static final EstiloCaja.Accent ACENTO = EstiloCaja.TRASLADOS;

    /** Indices de las columnas del modelo de la tabla. */
    private static final int COL_ID = 0;
    private static final int COL_ORIGEN = 1;
    private static final int COL_DESTINO = 2;
    private static final int COL_TOTAL = 3;
    private static final int COL_DESCRIPCION = 4;
    private static final int COL_FECHA = 5;
    private static final int COL_ID_INGRESO = 6;
    private static final int COL_ID_EGRESO = 7;

    DecimalFormat formatea = new DecimalFormat("###,###.##");
    Calendar fecha = new GregorianCalendar();

    // ---- Componentes ----
    private ButtonGroup buttonGroup1;
    public static JButton btn_limpiar;
    public static JButton btn_guardar;
    public static JLabel lbl_id;
    public static com.toedter.calendar.JDateChooser jdate_fecha_entrada;
    private JTextField txt_total;
    private JTextArea txt_descripcion_crear;
    private JComboBox<Fondos> jbox_Fondos_Origen;
    private JComboBox<Fondos> jbox_Fondo_Destino;
    private JRadioButton rbtn_F;
    private JRadioButton rbtn_R;

    private JTable jtabla_gastos;
    private JTextField txt_Filtro;
    private JButton btn_crear;
    private JButton btn_eliminar;
    private JButton btn_actualizar;
    private JButton btn_imprimir1;
    private JButton btn_excel;
    private JButton btn_cerrar;
    private JLabel lblFootInfo;
    private JLabel lbl_total_suma;
    private JTable jtabla_resumen;

    // KPIs
    private JLabel lblKpiTotalHoy;
    private JLabel lblKpiCantHoy;
    private JLabel lblKpiProm;
    private JLabel lblKpiMes;
    private JLabel lblKpiUltimo;
    private JLabel lblKpiUltimoPie;

    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    /** Caja a la que pertenece este formulario: 1 = Caja, 2 = Caja Dos. */
    private final int idCaja;

    public frm_Traslados() {
        this(1);
    }

    public frm_Traslados(int idCaja) {
        this.idCaja = idCaja;
        buildUI();
        if (idCaja == 2) {
            setTitle("Traslados - Caja Dos");
        }

        Fondos.mostrarFondos(jbox_Fondos_Origen, idCaja);
        Fondos.mostrarFondos(jbox_Fondo_Destino, idCaja);
        preseleccionarFondos();

        EstiloCaja.styleTable(jtabla_gastos, ACENTO);
        EstiloCaja.styleTable(jtabla_resumen, ACENTO);
        metodos.BuscarEnTabla(txt_Filtro, jtabla_gastos);

        txt_descripcion_crear.setWrapStyleWord(true);
        metodos.EvitarTabEnJTextArea(txt_descripcion_crear);

        try {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

        actualizar("hoy");
        actualizarResumen();
        calcular_total();

        // el id ahora es serial: no se muestra hasta que exista
        lbl_id.setText("Nuevo");
        jdate_fecha_entrada.setCalendar(fecha);

        if (frm_main.perfil != 1) {
            btn_eliminar.setEnabled(false);
        }
    }

    // =====================================================================
    //  Construccion de la interfaz (hand-coded, FlatLaf + EstiloCaja)
    // =====================================================================
    private void buildUI() {
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Traslados");

        // ---- instanciar componentes ----
        buttonGroup1 = new ButtonGroup();
        btn_limpiar = EstiloCaja.ghost("Limpiar", null);
        btn_guardar = EstiloCaja.primary("Guardar", FontAwesome.CHECK, ACENTO);
        btn_guardar.setMnemonic('g');
        btn_limpiar.setMnemonic('l');
        lbl_id = new JLabel("Nuevo");
        jdate_fecha_entrada = new com.toedter.calendar.JDateChooser();
        txt_total = new JTextField();
        txt_descripcion_crear = new JTextArea(3, 20);
        jbox_Fondos_Origen = new JComboBox<>();
        jbox_Fondo_Destino = new JComboBox<>();
        rbtn_F = new JRadioButton("Empresa (E)", true);
        rbtn_R = new JRadioButton("Personal (P)");

        jtabla_gastos = new JTable();
        jtabla_resumen = new JTable();
        txt_Filtro = new JTextField();
        btn_crear = EstiloCaja.iconButton(FontAwesome.PLUS, "Nuevo traslado (limpiar formulario)");
        btn_eliminar = EstiloCaja.iconButton(FontAwesome.TRASH_ALT, "Eliminar traslado");
        btn_actualizar = EstiloCaja.iconButton(FontAwesome.SYNC, "Actualizar (alterna hoy / todos)");
        btn_imprimir1 = EstiloCaja.iconButton(FontAwesome.IMAGE, "Exportar comprobante PNG");
        btn_excel = EstiloCaja.ghost("Exportar Excel", FontAwesome.FILE_EXCEL);
        btn_cerrar = EstiloCaja.ghost("Cerrar", FontAwesome.CLOSE);
        btn_cerrar.setMnemonic('w');
        lblFootInfo = EstiloCaja.title("", 12, Font.PLAIN, EstiloCaja.TEXT_3);
        lbl_total_suma = new JLabel("0");

        lblKpiTotalHoy = new JLabel("$ 0");
        lblKpiCantHoy = new JLabel("0");
        lblKpiProm = EstiloCaja.title("Traslado promedio  $0", 12, Font.PLAIN, EstiloCaja.TEXT_3);
        lblKpiMes = new JLabel("$ 0");
        lblKpiUltimo = new JLabel("$ 0");
        lblKpiUltimoPie = EstiloCaja.title("Sin traslados registrados", 12, Font.PLAIN, EstiloCaja.TEXT_3);

        buttonGroup1.add(rbtn_F);
        buttonGroup1.add(rbtn_R);
        rbtn_F.setFont(EstiloCaja.font(Font.PLAIN, 13));
        rbtn_R.setFont(EstiloCaja.font(Font.PLAIN, 13));
        rbtn_F.setOpaque(false);
        rbtn_R.setOpaque(false);

        lbl_id.setFont(EstiloCaja.font(Font.PLAIN, 12));
        lbl_id.setForeground(EstiloCaja.TEXT_3);

        EstiloCaja.styleCombo(jbox_Fondos_Origen, ACENTO);
        EstiloCaja.styleCombo(jbox_Fondo_Destino, ACENTO);
        EstiloCaja.styleField(txt_total, "Valor de la transferencia", null, ACENTO);
        txt_total.setFont(EstiloCaja.font(Font.BOLD, 15));
        EstiloCaja.styleField(txt_Filtro, "Buscar traslado…", FontAwesome.SEARCH, ACENTO);
        EstiloCaja.styleArea(txt_descripcion_crear);

        // ---- listeners ----
        btn_limpiar.addActionListener(e -> limpiar());
        btn_guardar.addActionListener(e -> btn_guardarActionPerformed());
        btn_crear.addActionListener(e -> limpiar());
        btn_eliminar.addActionListener(e -> btn_eliminarActionPerformed());
        btn_actualizar.addActionListener(e -> {
            actualizar("");
            actualizarResumen();
        });
        btn_imprimir1.addActionListener(e -> btn_imprimir1ActionPerformed());
        btn_excel.addActionListener(e -> btn_excelActionPerformed());
        btn_cerrar.addActionListener(e -> dispose());

        btn_guardar.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyChar() == KeyEvent.VK_ENTER) {
                    btn_guardarActionPerformed();
                }
            }
        });

        txt_total.setNextFocusableComponent(btn_guardar);
        txt_total.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (!txt_total.getText().equals("")) {
                    txt_total.setText(metodos.EliminaCaracteres(txt_total.getText(), "."));
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (!txt_total.getText().equals("")) {
                    try {
                        txt_total.setText(formatea.format(Double.parseDouble(txt_total.getText())));
                    } catch (Exception e) {
                    }
                }
            }
        });
        txt_total.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent evt) {
                DB_consultas_R_D.validar_numeros(evt, evt.getKeyChar());
            }

            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyChar() == KeyEvent.VK_ENTER) {
                    txt_descripcion_crear.requestFocus();
                }
            }
        });

        // ---- ensamblado ----
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(EstiloCaja.BG);
        root.setBorder(EstiloCaja.pad(18, 20, 20, 20));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildWorkspace(), BorderLayout.CENTER);

        setContentPane(root);
        setSize(1260, 800);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        // Fila titulo + accion exportar
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel h1 = EstiloCaja.title("Traslados", 22, Font.BOLD, ACENTO.base);
        JLabel sub = EstiloCaja.title("Movimientos de dinero entre fondos", 13, Font.PLAIN, EstiloCaja.TEXT_3);
        // margen a la derecha: sin el, la ultima letra del subtitulo se corta
        titles.setBorder(EstiloCaja.pad(0, 0, 0, 12));
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(h1);
        titles.add(Box.createVerticalStrut(2));
        titles.add(sub);

        // Barra vertical con el color de identidad del modulo (azul = traslado)
        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.X_AXIS));
        titleWrap.add(EstiloCaja.accentBar(ACENTO.base, 5, 46));
        titleWrap.add(Box.createHorizontalStrut(12));
        titleWrap.add(titles);
        titleRow.add(titleWrap, BorderLayout.WEST);

        JPanel headActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headActions.setOpaque(false);
        headActions.add(btn_excel);
        headActions.add(btn_cerrar);
        titleRow.add(headActions, BorderLayout.EAST);

        header.add(titleRow);
        header.add(Box.createVerticalStrut(16));

        // Fila de KPIs (bento)
        JPanel kpis = new JPanel(new GridLayout(1, 4, 14, 0));
        kpis.setOpaque(false);
        kpis.setAlignmentX(Component.LEFT_ALIGNMENT);
        kpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        kpis.add(kpiCard("Trasladado hoy", FontAwesome.EXCHANGE, lblKpiTotalHoy,
                EstiloCaja.title("Suma de traslados de hoy", 12, Font.PLAIN, EstiloCaja.TEXT_3), true));
        kpis.add(kpiCard("Traslados de hoy", FontAwesome.LIST, lblKpiCantHoy, lblKpiProm));
        kpis.add(kpiCard("Trasladado en el mes", FontAwesome.MONEY_BILL, lblKpiMes,
                EstiloCaja.title("Mes en curso", 12, Font.PLAIN, EstiloCaja.TEXT_3)));
        kpis.add(kpiCard("Último traslado", FontAwesome.CLOCK, lblKpiUltimo, lblKpiUltimoPie));
        header.add(kpis);

        return header;
    }

    private JPanel kpiCard(String label, String glyph, JLabel value, JLabel foot) {
        return kpiCard(label, glyph, value, foot, false);
    }

    /**
     * @param destacado si es true, pinta la franja superior y el valor con el
     * color de identidad del modulo (azul en Traslados).
     */
    private JPanel kpiCard(String label, String glyph, JLabel value, JLabel foot, boolean destacado) {
        EstiloCaja.Card c = EstiloCaja.card(new BorderLayout());
        c.setBorder(EstiloCaja.pad(destacado ? 16 : 14, 16, 12, 16));
        if (destacado) {
            c.setAccentTop(ACENTO.base);
        }

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(EstiloCaja.title(label, 12, Font.PLAIN, EstiloCaja.TEXT_3), BorderLayout.WEST);
        top.add(new JLabel(FontAwesome.icon(glyph, 15f, destacado ? ACENTO.base : EstiloCaja.TEXT_3)), BorderLayout.EAST);

        value.setFont(EstiloCaja.font(Font.BOLD, 25));
        value.setForeground(destacado ? ACENTO.base : EstiloCaja.TEXT);
        value.setBorder(EstiloCaja.pad(10, 0, 4, 0));

        c.add(top, BorderLayout.NORTH);
        c.add(value, BorderLayout.CENTER);
        if (foot != null) {
            c.add(foot, BorderLayout.SOUTH);
        }
        return c;
    }

    private JPanel buildWorkspace() {
        JPanel ws = new JPanel(new BorderLayout(16, 0));
        ws.setOpaque(false);
        ws.add(buildFormCard(), BorderLayout.WEST);
        ws.add(buildRightColumn(), BorderLayout.CENTER);
        return ws;
    }

    private EstiloCaja.Card buildFormCard() {
        EstiloCaja.Card card = EstiloCaja.card(new BorderLayout());
        card.setPreferredSize(new Dimension(374, 10));
        card.setAccentTop(ACENTO.base); // franja de identidad del modulo

        // Header de la tarjeta
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, EstiloCaja.LINE),
                EstiloCaja.pad(16, 16, 14, 16)));
        JPanel hl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        hl.setOpaque(false);
        JLabel ic = new JLabel(FontAwesome.icon(FontAwesome.EXCHANGE, 15f, ACENTO.base));
        JPanel ht = new JPanel();
        ht.setOpaque(false);
        ht.setLayout(new BoxLayout(ht, BoxLayout.Y_AXIS));
        ht.add(EstiloCaja.title("Nuevo traslado", 15, Font.BOLD, EstiloCaja.TEXT));
        JPanel subRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        subRow.setOpaque(false);
        subRow.add(EstiloCaja.title("Mueve dinero entre fondos ·", 12, Font.PLAIN, EstiloCaja.TEXT_3));
        subRow.add(lbl_id);
        ht.add(subRow);
        hl.add(ic);
        hl.add(ht);
        head.add(hl, BorderLayout.WEST);
        head.add(new EstiloCaja.ChipLabel("TRASLADO", ACENTO.base, ACENTO.tint), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        // Contenido del formulario (FormPanel: se ajusta al ancho del viewport)
        EstiloCaja.FormPanel form = new EstiloCaja.FormPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(EstiloCaja.pad(16, 16, 16, 16));

        addField(form, labeled("Fecha del traslado", jdate_fecha_entrada, 62));
        addField(form, labeled("Fondo origen (sale el dinero)", jbox_Fondos_Origen, 62));

        // Indicador visual del sentido del traslado (origen -> destino)
        JPanel flecha = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        flecha.setOpaque(false);
        flecha.add(new JLabel(FontAwesome.icon(FontAwesome.ARROW_DOWN, 13f, ACENTO.base)));
        addRaw(form, flecha, 22);
        form.add(Box.createVerticalStrut(6));

        addField(form, labeled("Fondo destino (entra el dinero)", jbox_Fondo_Destino, 62));
        addField(form, labeled("Valor de la transferencia", txt_total, 62));

        JPanel tipoPanel = new JPanel();
        tipoPanel.setOpaque(false);
        tipoPanel.setLayout(new BoxLayout(tipoPanel, BoxLayout.X_AXIS));
        tipoPanel.add(rbtn_F);
        tipoPanel.add(Box.createHorizontalStrut(22));
        tipoPanel.add(rbtn_R);
        tipoPanel.add(Box.createHorizontalGlue());
        addField(form, labeled("Tipo de soporte", tipoPanel, 58));

        JScrollPane descScroll = new JScrollPane(txt_descripcion_crear);
        descScroll.setBorder(BorderFactory.createLineBorder(EstiloCaja.LINE_2, 1));
        addField(form, labeled("Descripción", descScroll, 108));

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(formScroll, BorderLayout.CENTER);

        // Footer con acciones
        JPanel foot = new JPanel(new GridLayout(1, 2, 10, 0));
        foot.setOpaque(false);
        foot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCaja.LINE),
                EstiloCaja.pad(12, 16, 14, 16)));
        foot.add(btn_limpiar);
        foot.add(btn_guardar);
        card.add(foot, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildRightColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 14));
        col.setOpaque(false);
        col.add(buildTableCard(), BorderLayout.CENTER);
        col.add(buildResumenCard(), BorderLayout.SOUTH);
        return col;
    }

    private EstiloCaja.Card buildTableCard() {
        EstiloCaja.Card card = EstiloCaja.card(new BorderLayout());

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, EstiloCaja.LINE),
                EstiloCaja.pad(12, 14, 12, 14)));
        toolbar.add(EstiloCaja.title("Movimientos", 14, Font.BOLD, EstiloCaja.TEXT), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        txt_Filtro.setPreferredSize(new Dimension(210, 34));
        actions.add(txt_Filtro);
        actions.add(btn_crear);
        actions.add(btn_eliminar);
        actions.add(btn_imprimir1);
        actions.add(btn_actualizar);
        toolbar.add(actions, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);

        // Tabla
        JScrollPane sp = new JScrollPane(jtabla_gastos);
        sp.setBorder(null);
        sp.getViewport().setBackground(EstiloCaja.SURFACE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        card.add(sp, BorderLayout.CENTER);

        // Footer con el total de lo mostrado
        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCaja.LINE),
                EstiloCaja.pad(11, 16, 11, 16)));
        foot.add(lblFootInfo, BorderLayout.WEST);

        JPanel totals = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        totals.setOpaque(false);
        totals.add(totalChunk("Total trasladado", lbl_total_suma, ACENTO.base, Font.BOLD, 22));
        foot.add(totals, BorderLayout.EAST);
        card.add(foot, BorderLayout.SOUTH);

        return card;
    }

    /** Bloque "Etiqueta: valor" para el pie de la tabla. */
    private JPanel totalChunk(String label, JLabel value, Color valueColor, int style, int size) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        value.setFont(EstiloCaja.font(style, size));
        value.setForeground(valueColor);
        p.add(EstiloCaja.title(label + ":", 13, Font.PLAIN, EstiloCaja.TEXT_3));
        p.add(value);
        return p;
    }

    private EstiloCaja.Card buildResumenCard() {
        EstiloCaja.Card card = EstiloCaja.card(new BorderLayout());
        card.setPreferredSize(new Dimension(10, 200));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, EstiloCaja.LINE),
                EstiloCaja.pad(13, 16, 13, 16)));
        head.add(EstiloCaja.title("Efecto por fondo · hoy", 13, Font.BOLD, EstiloCaja.TEXT), BorderLayout.WEST);
        card.add(head, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(jtabla_resumen);
        sp.setBorder(null);
        sp.getViewport().setBackground(EstiloCaja.SURFACE);
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    // ---- helpers de layout del formulario ----
    private void addField(JPanel form, JComponent comp) {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(comp);
        form.add(Box.createVerticalStrut(12));
    }

    private void addRaw(JPanel form, JComponent comp, int height) {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        form.add(comp);
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

    // =====================================================================
    //  Logica (portada del formulario original)
    // =====================================================================
    public void validar_numeros(java.awt.event.KeyEvent evt, char car) {
        if ((car < '0' || car > '9')) {
            evt.consume();
        }
    }

    private void btn_guardarActionPerformed() {
        if (txt_descripcion_crear.getText().isEmpty() || txt_total.getText().isEmpty()) {
            txt_descripcion_crear.setBackground(Color.pink);
            txt_total.setBackground(Color.pink);
            return;
        }

        DB_transferencias db_transferencias = new DB_transferencias();
        Transferencias obj = new Transferencias();

        obj.setDescripcion(txt_descripcion_crear.getText());
        int dia, mes, ano;
        ano = jdate_fecha_entrada.getCalendar().get(Calendar.YEAR);
        mes = jdate_fecha_entrada.getCalendar().get(Calendar.MONTH) + 1;
        dia = jdate_fecha_entrada.getCalendar().get(Calendar.DAY_OF_MONTH);
        obj.setFecha(String.format("%04d-%02d-%02d", ano, mes, dia));
        obj.setHora(DB_consultas_R_D.obtener_hora());

        // fondo origen
        try {
            obj.setId_fondo_origen(jbox_Fondos_Origen.getItemAt(jbox_Fondos_Origen.getSelectedIndex()).getId());
        } catch (Exception e) {
            obj.setId_fondo_origen(Fondos.TraerPredeterminado(idCaja));
        }
        // fondo destino
        try {
            obj.setId_fondo_destino(jbox_Fondo_Destino.getItemAt(jbox_Fondo_Destino.getSelectedIndex()).getId());
        } catch (Exception e) {
            obj.setId_fondo_destino(Fondos.TraerPredeterminado(idCaja));
        }

        if (obj.getId_fondo_origen() == obj.getId_fondo_destino()) {
            JOptionPane.showMessageDialog(this, "El fondo de origen y destino no pueden ser el mismo");
            return;
        }

        obj.setId_user(frm_main.id_user);
        obj.setId_caja(idCaja);
        obj.setTotal(Double.parseDouble(metodos.EliminaCaracteres(txt_total.getText(), ".")));
        obj.setFactura_remision(rbtn_F.isSelected() ? 1 : 0);

        // El par ingreso/egreso y la transferencia se crean en una sola
        // transaccion; las cuentas del par son las predeterminadas DE ESTA CAJA.
        // El origen queda en la auditoria de caja (trigger en base de datos).
        int idGenerado;
        try {
            AuditoriaCaja.setOrigen("Traslados - guardar");
            idGenerado = db_transferencias.GuardarTraslado(obj,
                    Cuentas_Ingresos.TraerPredeterminadoID(idCaja),
                    Cuentas_Egresos.TraerPredeterminadoID(idCaja));
        } finally {
            AuditoriaCaja.limpiar();
        }

        if (idGenerado > 0) {
            limpiar();
            actualizar("hoy");
            actualizarResumen();
            txt_total.requestFocus();
        }
    }

    public void limpiar() {
        txt_descripcion_crear.setText("");
        txt_total.setText("");
        jdate_fecha_entrada.setCalendar(fecha);
        txt_total.setBackground(Color.white);
        txt_descripcion_crear.setBackground(Color.white);
        rbtn_F.setSelected(true);
        preseleccionarFondos();
        lbl_id.setText("Nuevo");
    }

    /**
     * Origen = fondo predeterminado (primero de la lista) y destino = el
     * siguiente, para que la ventana no arranque con origen y destino iguales
     * (combinacion que el sistema rechaza).
     */
    private void preseleccionarFondos() {
        if (jbox_Fondos_Origen.getItemCount() > 0) {
            jbox_Fondos_Origen.setSelectedIndex(0);
        }
        if (jbox_Fondo_Destino.getItemCount() > 1) {
            jbox_Fondo_Destino.setSelectedIndex(1);
        }
    }

    private void btn_eliminarActionPerformed() {
        int fila = jtabla_gastos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
            return;
        }
        int filaModelo = jtabla_gastos.convertRowIndexToModel(fila);
        int dialogResult = JOptionPane.showConfirmDialog(null,
                "¿Desea eliminar este traslado?\nSe eliminarán también el ingreso y el egreso que generó.",
                "Alerta", JOptionPane.YES_NO_OPTION);
        if (dialogResult == JOptionPane.YES_OPTION) {
            try {
                String id = modelo.getValueAt(filaModelo, COL_ID).toString();
                String id_ingreso = modelo.getValueAt(filaModelo, COL_ID_INGRESO).toString();
                String id_egreso = modelo.getValueAt(filaModelo, COL_ID_EGRESO).toString();
                // primero la transferencia (tiene FK hacia el par)
                AuditoriaCaja.setOrigen("Traslados - eliminar");
                DB_consultas_R_D.eliminar("transferencias", id);
                DB_consultas_R_D.eliminar("ingresos", id_ingreso);
                DB_consultas_R_D.eliminar("egresos", id_egreso);
                modelo.removeRow(filaModelo);
                limpiar();
                calcular_total();
                actualizarResumen();
                sumarTablaYPie();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                AuditoriaCaja.limpiar();
            }
        }
    }

    private void btn_imprimir1ActionPerformed() {
        int fila = jtabla_gastos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
            return;
        }
        int filaModelo = jtabla_gastos.convertRowIndexToModel(fila);
        int dialogResult = JOptionPane.showConfirmDialog(null, "¿Desea imprimir este traslado?", "Alerta", JOptionPane.YES_NO_OPTION);
        if (dialogResult == JOptionPane.YES_OPTION) {
            String id = modelo.getValueAt(filaModelo, COL_ID).toString();
            String origen = "" + modelo.getValueAt(filaModelo, COL_ORIGEN);
            String destino = "" + modelo.getValueAt(filaModelo, COL_DESTINO);

            String consulta = "select t.id, t.fecha, t.hora, t.descripcion, t.total \n"
                    + "from transferencias t \n"
                    + "where t.id = " + id;
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);

            ImprimirPNG_traslado imprimir = null;
            try {
                while (rs.next()) {
                    imprimir = new ImprimirPNG_traslado(rs.getString("fecha") + " / " + rs.getString("hora"), rs.getString("id"),
                            origen, destino,
                            metodos.formateador_dinero().format(rs.getDouble("total")), rs.getString("descripcion"), "TRASLADO");
                }
                rs.close();
            } catch (SQLException ex) {
                System.out.println("");
            }
            if (imprimir != null) {
                imprimir.generarYGuardarImagen();
            }
        }
    }

    private void btn_excelActionPerformed() {
        try {
            ExportarExcel obj = new ExportarExcel();
            obj.exportarExcel(jtabla_gastos);
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
    }

    /** Ancho de columnas, renderers y ocultamiento de los ids del par. */
    private void ajustarTabla() {
        TableColumnModel cm = jtabla_gastos.getColumnModel();
        if (cm.getColumnCount() < 8) {
            return;
        }
        cm.getColumn(COL_ID).setPreferredWidth(55);
        cm.getColumn(COL_ORIGEN).setPreferredWidth(150);
        cm.getColumn(COL_DESTINO).setPreferredWidth(150);
        cm.getColumn(COL_TOTAL).setPreferredWidth(120);
        cm.getColumn(COL_DESCRIPCION).setPreferredWidth(320);
        cm.getColumn(COL_FECHA).setPreferredWidth(90);

        cm.getColumn(COL_ID).setCellRenderer(new EstiloCaja.MutedRenderer());
        cm.getColumn(COL_FECHA).setCellRenderer(new EstiloCaja.MutedRenderer());
        cm.getColumn(COL_TOTAL).setCellRenderer(new EstiloCaja.MoneyRenderer());

        // Los ids del par ingreso/egreso siguen en el modelo (los usa eliminar)
        // pero no se muestran: son ruido para el usuario.
        cm.removeColumn(cm.getColumn(COL_ID_EGRESO));
        cm.removeColumn(cm.getColumn(COL_ID_INGRESO));
    }

    /** Suma los traslados cargados en la tabla y actualiza el pie. */
    private void sumarTablaYPie() {
        double total = 0;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            try {
                total += Double.parseDouble(metodos.EliminaCaracteres(modelo.getValueAt(i, COL_TOTAL).toString(), "."));
            } catch (Exception e) {
            }
        }
        lbl_total_suma.setText(metodos.formateador_dinero().format(total));
        lblFootInfo.setText("Mostrando " + modelo.getRowCount() + (act == 0 ? " (todos)" : " del día"));
    }

    /** KPIs del encabezado. */
    public void calcular_total() {
        double total_hoy = 0, total_mes = 0;
        int cnt_hoy = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(
                    "select coalesce((select sum(total) from transferencias where fecha=CURRENT_DATE and id_caja=" + idCaja + "),0) as total_hoy,"
                    + "coalesce((select count(*) from transferencias where fecha=CURRENT_DATE and id_caja=" + idCaja + "),0) as cnt_hoy,"
                    + "coalesce((select sum(total) from transferencias where fecha>=date_trunc('month',CURRENT_DATE) and id_caja=" + idCaja + "),0) as total_mes");
            while (rs.next()) {
                total_hoy = rs.getDouble("total_hoy");
                cnt_hoy = rs.getInt("cnt_hoy");
                total_mes = rs.getDouble("total_mes");
            }
            rs.close();
        } catch (Exception e) {
        }

        DecimalFormat f = metodos.formateador_dinero();
        lblKpiTotalHoy.setText("$ " + f.format(total_hoy));
        lblKpiCantHoy.setText(String.valueOf(cnt_hoy));
        lblKpiMes.setText("$ " + f.format(total_mes));
        double prom = cnt_hoy > 0 ? total_hoy / cnt_hoy : 0;
        lblKpiProm.setText("Traslado promedio  $" + f.format(prom));

        // Ultimo traslado registrado
        lblKpiUltimo.setText("$ 0");
        lblKpiUltimoPie.setText("Sin traslados registrados");
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(
                    "select t.total, t.fecha, fo.nombre as origen, fd.nombre as destino "
                    + "from transferencias t "
                    + "inner join fondos fo on t.id_fondo_origen=fo.id "
                    + "inner join fondos fd on t.id_fondo_destino=fd.id "
                    + "where t.id_caja=" + idCaja + " "
                    + "order by t.id desc limit 1");
            while (rs.next()) {
                lblKpiUltimo.setText("$ " + f.format(rs.getDouble("total")));
                lblKpiUltimoPie.setText(rs.getString("origen") + " → " + rs.getString("destino"));
            }
            rs.close();
        } catch (Exception e) {
        }
    }

    /**
     * Resumen del dia: cuanto entro y cuanto salio de cada fondo por traslados.
     */
    public void actualizarResumen() {
        DefaultTableModel modelo_resumen = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        modelo_resumen.setColumnIdentifiers(new Object[]{"Fondo", "Entró", "Salió", "Neto"});

        String consulta = "SELECT f.nombre as fondo, "
                + "coalesce(SUM(CASE WHEN t.id_fondo_destino=f.id THEN t.total ELSE 0 END),0) as entro, "
                + "coalesce(SUM(CASE WHEN t.id_fondo_origen=f.id THEN t.total ELSE 0 END),0) as salio "
                + "FROM fondos f "
                + "INNER JOIN transferencias t ON (t.id_fondo_origen=f.id OR t.id_fondo_destino=f.id) "
                + "WHERE t.fecha=CURRENT_DATE AND t.id_caja=" + idCaja + " "
                + "GROUP BY f.nombre "
                + "ORDER BY f.nombre";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);
            while (rs.next()) {
                double entro = rs.getDouble("entro");
                double salio = rs.getDouble("salio");
                modelo_resumen.addRow(new Object[]{
                    rs.getString("fondo"),
                    formatea.format(entro),
                    formatea.format(salio),
                    formatea.format(entro - salio)
                });
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("Error resumen traslados: " + e);
        }
        jtabla_resumen.setModel(modelo_resumen);
    }

    int act = 0;

    /**
     * Carga la tabla. Con "hoy" muestra solo los traslados del dia; con
     * cualquier otro valor alterna entre el dia y el historico completo (es el
     * comportamiento del boton Actualizar).
     */
    public void actualizar(String fecha) {
        try {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

        // act: 1 = la tabla muestra solo el dia, 0 = muestra el historico.
        // El boton Actualizar alterna entre los dos modos (como en Ingresos).
        boolean soloHoy = fecha.equals("hoy") || act != 1;

        String base = "select i.id, fo.nombre as nombre_origen, fd.nombre as nombre_destino, i.total, i.descripcion, i.fecha, i.id_ingreso, i.id_egreso "
                + "from transferencias i, fondos fo, fondos fd "
                + "where i.id_fondo_origen=fo.id and i.id_fondo_destino=fd.id and i.id_caja=" + idCaja + " ";
        String consulta = soloHoy
                ? base + "and i.fecha=CURRENT_DATE order by i.fecha desc, i.id desc"
                : base + "order by i.fecha desc, i.id desc";

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        modelo.setColumnIdentifiers(new Object[]{"id", "Fondo origen", "Fondo destino", "Total", "Descripcion", "Fecha", "Id ingreso", "Id Egreso"});
        try {
            while (rs.next()) {
                modelo.addRow(new Object[]{rs.getString("id"), rs.getString("nombre_origen"), rs.getString("nombre_destino"),
                    formatea.format(rs.getDouble("total")), rs.getString("descripcion"), rs.getDate("fecha"),
                    rs.getInt("id_ingreso"), rs.getInt("id_egreso")});
            }
            rs.close();
            // asigna el modelo a la tabla
            jtabla_gastos.setModel(modelo);
            ajustarTabla();
            calcular_total();
            act = soloHoy ? 1 : 0;
            sumarTablaYPie();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
