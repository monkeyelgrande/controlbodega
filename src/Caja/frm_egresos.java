/*
 * Modulo Caja: registro y consulta de egresos de dinero.
 * Rediseno "SaaS claro" (mismo lenguaje visual que frm_ingresos) con el color
 * de identidad ROJO para distinguirlo de un vistazo del formulario de Ingresos
 * (verde). Sin abonos: el fondo va directo en egresos.id_fondo y todo egreso
 * se paga de contado. El formulario de alta esta siempre visible (panel fijo).
 */
package Caja;

import Formularios.frm_main;
import Metodos.ExportarExcel;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.AuditoriaCaja;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBEgresos;
import conexiondb.DB_transferencias;
import conexiondb.DB_Fotos_servicios;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Cuentas_Egresos;
import modelos.Egresos;
import modelos.Fondos;
import modelos.Fotos_registros;

/**
 * Formulario de Egresos de caja (rediseno SaaS claro, identidad roja).
 *
 * @author Monkeyelgrande
 */
public class frm_egresos extends javax.swing.JInternalFrame {

    public static DefaultTableModel modelo_fotos = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    /**
     * Color de identidad del modulo: ROJO = sale dinero. Ingresos usa
     * EstiloCaja.INGRESOS (verde) para que se distingan de un vistazo.
     */
    private static final EstiloCaja.Accent ACENTO = EstiloCaja.EGRESOS;

    DecimalFormat formatea = new DecimalFormat("###,###.##");
    Calendar fecha = new GregorianCalendar();
    static TableColumnModel columnModel = null;

    public String fecha_hoy = DB_consultas_R_D.obtener_fecha();

    String ruta_foto = "";
    String ruta_origen_imagen = "";
    public static String nombreArchivoImagen = "";

    // ---- Componentes (los public static son usados por otras clases) ----
    private ButtonGroup buttonGroup1;
    public static JButton btn_limpiar;
    public static JButton btn_guardar;
    private JTextField txt_total;
    private JTextArea jtxa_descripcion_crear;
    public static com.toedter.calendar.JDateChooser jdate_fecha_entrada;
    private JComboBox<Cuentas_Egresos> jbox_Cuentas;
    public static JLabel lbl_id;
    private JComboBox<Fondos> jbox_Fondos;
    public static JTextField txt_nombre_cliente;
    public static JLabel lbl_id_cliente;
    public static JButton btn_buscar_cliente;
    public static JButton btn_crear_cliente;
    private JRadioButton rbtn_F;
    private JRadioButton rbtn_R;
    public static JTable jtabla_fotos;
    public static JLabel lbl_foto_1;
    public static JButton btn_cargar_foto;
    public static JButton btn_cargar_foto1;
    public static JButton btn_eliminar_foto;
    private org.jdesktop.swingx.JXTable jtabla_gastos;
    private JTextField txt_Filtro;
    private JButton btn_total;
    private JLabel lbl_total_suma;
    private JLabel lbl_total_F;
    private JLabel lbl_total_R;
    private JLabel lblFootInfo;
    private JTable jtabla_resumen;
    private JButton btn_eliminar;
    private JButton btn_editar;
    public static JButton btn_actualizar;
    private JButton btn_imprimir;
    private JButton btn_imprimir1;
    private JButton jButton1;
    // KPIs
    private JLabel lblKpiTotal;
    private JLabel lblKpiF;
    private JLabel lblKpiR;
    private JLabel lblKpiCount;
    private JLabel lblKpiProm;

    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    /** Caja a la que pertenece este formulario: 1 = Caja, 2 = Caja Dos. */
    private final int idCaja;

    public frm_egresos() {
        this(1);
    }

    public frm_egresos(int idCaja) {
        this.idCaja = idCaja;
        buildUI();
        if (idCaja == 2) {
            setTitle("Egresos - Caja Dos");
        }

        Fondos.mostrarFondos(jbox_Fondos, idCaja);
        modelo_fotos.setColumnIdentifiers(new Object[]{"Ruta", "Nombre", "id_foto"});
        jtabla_fotos.setModel(modelo_fotos);

        EstiloCaja.styleTable(jtabla_gastos, ACENTO);
        EstiloCaja.styleTable(jtabla_resumen, ACENTO);

        jtxa_descripcion_crear.setWrapStyleWord(true);
        try {
            for (int i = 0; i < jtabla_gastos.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

        actualizar();
        calcular_total();
        actualizarResumen();

        lbl_id.setText("Nuevo");
        Cuentas_Egresos.mostrarCuentas(jbox_Cuentas, idCaja);
        jdate_fecha_entrada.setCalendar(fecha);
        metodos.EvitarTabEnJTextArea(jtxa_descripcion_crear);

        TamanosTablaAbonos();

        jtabla_gastos.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    try {
                        verEgreso();
                    } catch (SQLException ex) {
                        Logger.getLogger(frm_egresos.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        Metodos.metodos.TablaAptaParaBusquedaAndSSM(jtabla_gastos);
        metodos.BuscarEnTabla(txt_Filtro, jtabla_gastos);

        if (frm_main.perfil != 1) {
            btn_eliminar.setEnabled(false);
            btn_editar.setEnabled(false);
        }

        jtabla_fotos.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 1) {
                    int fila = jtabla_fotos.getSelectedRow();
                    if (fila >= 0) {
                        String ruta = (String) jtabla_fotos.getValueAt(fila, 0);
                        foto_a_label(ruta, lbl_foto_1);
                        ruta_foto = ruta;
                    }
                }
            }
        });

        jtabla_fotos.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent me) {
                char num = me.getKeyChar();
                if (num == KeyEvent.VK_DELETE) {
                    quitar_fila(jtabla_fotos, modelo_fotos);
                }
            }
        });
    }

    // =====================================================================
    //  Construccion de la interfaz (hand-coded, FlatLaf + EstiloCaja)
    // =====================================================================
    private void buildUI() {
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Egresos");

        buttonGroup1 = new ButtonGroup();
        btn_limpiar = EstiloCaja.ghost("Limpiar", null);
        btn_guardar = EstiloCaja.primary("Guardar egreso", FontAwesome.CHECK, ACENTO);
        txt_total = new JTextField();
        jtxa_descripcion_crear = new JTextArea(3, 20);
        jdate_fecha_entrada = new com.toedter.calendar.JDateChooser();
        jbox_Cuentas = new JComboBox<>();
        lbl_id = new JLabel("Nuevo");
        jbox_Fondos = new JComboBox<>();
        txt_nombre_cliente = new JTextField("0000");
        lbl_id_cliente = new JLabel("1"); // portador del id, no se muestra
        btn_buscar_cliente = EstiloCaja.plusButton(FontAwesome.SEARCH, "Buscar contacto", ACENTO);
        btn_crear_cliente = EstiloCaja.plusButton(FontAwesome.PLUS, "Crear contacto", ACENTO);
        rbtn_F = new JRadioButton("Empresa (E)", true);
        rbtn_R = new JRadioButton("Personal (P)");
        jtabla_fotos = new JTable();
        lbl_foto_1 = new JLabel();
        btn_cargar_foto = EstiloCaja.ghost("Cargar", FontAwesome.UPLOAD);
        btn_cargar_foto1 = EstiloCaja.ghost("Capturar", FontAwesome.CAMERA);
        btn_eliminar_foto = EstiloCaja.ghost("Quitar", FontAwesome.TRASH);
        jtabla_gastos = new org.jdesktop.swingx.JXTable();
        txt_Filtro = new JTextField();
        btn_total = EstiloCaja.iconButton(FontAwesome.CALCULATOR, "Sumar total de la tabla");
        lbl_total_suma = new JLabel("0");
        lbl_total_F = new JLabel("0");
        lbl_total_R = new JLabel("0");
        lblFootInfo = EstiloCaja.title("", 12, Font.PLAIN, EstiloCaja.TEXT_3);
        jtabla_resumen = new JTable();
        btn_eliminar = EstiloCaja.iconButton(FontAwesome.TRASH_ALT, "Eliminar");
        btn_editar = EstiloCaja.iconButton(FontAwesome.EDIT, "Editar");
        btn_actualizar = EstiloCaja.iconButton(FontAwesome.SYNC, "Actualizar");
        btn_imprimir = EstiloCaja.iconButton(FontAwesome.PRINTER, "Imprimir recibo (térmica)");
        btn_imprimir1 = EstiloCaja.iconButton(FontAwesome.IMAGE, "Exportar recibo PNG");
        jButton1 = EstiloCaja.ghost("Exportar Excel", FontAwesome.FILE_EXCEL);
        lblKpiTotal = new JLabel("$ 0");
        lblKpiF = new JLabel("$ 0");
        lblKpiR = new JLabel("$ 0");
        lblKpiCount = new JLabel("0");
        lblKpiProm = EstiloCaja.title("Ticket promedio  $0", 12, Font.PLAIN, EstiloCaja.TEXT_3);

        buttonGroup1.add(rbtn_F);
        buttonGroup1.add(rbtn_R);
        rbtn_F.setFont(EstiloCaja.font(Font.PLAIN, 13));
        rbtn_R.setFont(EstiloCaja.font(Font.PLAIN, 13));
        rbtn_F.setOpaque(false);
        rbtn_R.setOpaque(false);

        EstiloCaja.styleCombo(jbox_Cuentas, ACENTO);
        EstiloCaja.styleCombo(jbox_Fondos, ACENTO);
        EstiloCaja.styleField(txt_total, "Valor del egreso", null, ACENTO);
        txt_total.setFont(EstiloCaja.font(Font.BOLD, 15));
        EstiloCaja.styleField(txt_nombre_cliente, "Contacto", null, ACENTO);
        EstiloCaja.styleField(txt_Filtro, "Buscar egreso…", FontAwesome.SEARCH, ACENTO);
        EstiloCaja.styleArea(jtxa_descripcion_crear);

        lbl_foto_1.setBorder(BorderFactory.createLineBorder(EstiloCaja.LINE_2, 1));
        lbl_foto_1.setHorizontalAlignment(JLabel.CENTER);

        columnModel = jtabla_gastos.getColumnModel();

        // ---- listeners ----
        btn_limpiar.addActionListener(e -> limpiar());
        btn_guardar.addActionListener(e -> btn_guardarActionPerformed());
        btn_buscar_cliente.addActionListener(e -> btn_buscar_clienteActionPerformed());
        btn_crear_cliente.addActionListener(e -> btn_crear_clienteActionPerformed());
        btn_cargar_foto.addActionListener(e -> btn_cargar_fotoActionPerformed());
        btn_cargar_foto1.addActionListener(e -> btn_cargar_foto1ActionPerformed());
        btn_eliminar_foto.addActionListener(e -> btn_eliminar_fotoActionPerformed());
        btn_editar.addActionListener(e -> btn_editarActionPerformed());
        btn_eliminar.addActionListener(e -> btn_eliminarActionPerformed());
        btn_actualizar.addActionListener(e -> actualizar());
        btn_imprimir.addActionListener(e -> btn_imprimirActionPerformed());
        btn_imprimir1.addActionListener(e -> btn_imprimir1ActionPerformed());
        btn_total.addActionListener(e -> btn_totalActionPerformed());
        jButton1.addActionListener(e -> jButton1ActionPerformed());

        lbl_foto_1.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                try {
                    DB_consultas_R_D.Abrir_Archivo(ruta_foto);
                } catch (Exception e) {
                }
            }
        });

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
                        txt_total.setText(metodos.formateador_dinero().format(Double.parseDouble(txt_total.getText())));
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
                    btn_guardar.requestFocus();
                }
            }
        });

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

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel h1 = EstiloCaja.title("Egresos", 22, Font.BOLD, ACENTO.base);
        JLabel sub = EstiloCaja.title("Registro y consulta de egresos de caja", 13, Font.PLAIN, EstiloCaja.TEXT_3);
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(h1);
        titles.add(Box.createVerticalStrut(2));
        titles.add(sub);

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.X_AXIS));
        titleWrap.add(EstiloCaja.accentBar(ACENTO.base, 5, 46));
        titleWrap.add(Box.createHorizontalStrut(12));
        titleWrap.add(titles);
        titleRow.add(titleWrap, BorderLayout.WEST);

        JPanel headActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headActions.setOpaque(false);
        headActions.add(jButton1);
        titleRow.add(headActions, BorderLayout.EAST);

        header.add(titleRow);
        header.add(Box.createVerticalStrut(16));

        JPanel kpis = new JPanel(new GridLayout(1, 4, 14, 0));
        kpis.setOpaque(false);
        kpis.setAlignmentX(Component.LEFT_ALIGNMENT);
        kpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        kpis.add(kpiCard("Total del día", FontAwesome.CALCULATOR, lblKpiTotal,
                EstiloCaja.title("Suma de egresos de hoy", 12, Font.PLAIN, EstiloCaja.TEXT_3), true));
        kpis.add(kpiCard("Empresa (E)", FontAwesome.FILE_INVOICE, lblKpiF,
                EstiloCaja.title("Egresos de empresa", 12, Font.PLAIN, EstiloCaja.TEXT_3)));
        kpis.add(kpiCard("Personal (P)", FontAwesome.CLIPBOARD, lblKpiR,
                EstiloCaja.title("Egresos personales", 12, Font.PLAIN, EstiloCaja.TEXT_3)));
        kpis.add(kpiCard("Egresos registrados", FontAwesome.LIST, lblKpiCount, lblKpiProm));
        header.add(kpis);

        return header;
    }

    private JPanel kpiCard(String label, String glyph, JLabel value, JLabel foot) {
        return kpiCard(label, glyph, value, foot, false);
    }

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
        card.setAccentTop(ACENTO.base);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, EstiloCaja.LINE),
                EstiloCaja.pad(16, 16, 14, 16)));
        JPanel hl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        hl.setOpaque(false);
        JLabel ic = new JLabel(FontAwesome.icon(FontAwesome.MINUS, 15f, ACENTO.base));
        JPanel ht = new JPanel();
        ht.setOpaque(false);
        ht.setLayout(new BoxLayout(ht, BoxLayout.Y_AXIS));
        ht.add(EstiloCaja.title("Nuevo egreso", 15, Font.BOLD, EstiloCaja.TEXT));
        ht.add(EstiloCaja.title("Registra una salida de caja", 12, Font.PLAIN, EstiloCaja.TEXT_3));
        hl.add(ic);
        hl.add(ht);
        head.add(hl, BorderLayout.WEST);
        head.add(new EstiloCaja.ChipLabel("EGRESO", ACENTO.base, ACENTO.tint), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        EstiloCaja.FormPanel form = new EstiloCaja.FormPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(EstiloCaja.pad(16, 16, 16, 16));

        // Contacto: campo + buscar + crear
        JPanel contactoRow = new JPanel(new BorderLayout(8, 0));
        contactoRow.setOpaque(false);
        contactoRow.add(txt_nombre_cliente, BorderLayout.CENTER);
        JPanel contactoBtns = new JPanel(new GridLayout(1, 2, 6, 0));
        contactoBtns.setOpaque(false);
        btn_buscar_cliente.setPreferredSize(new Dimension(44, 36));
        btn_crear_cliente.setPreferredSize(new Dimension(44, 36));
        contactoBtns.add(btn_buscar_cliente);
        contactoBtns.add(btn_crear_cliente);
        contactoRow.add(contactoBtns, BorderLayout.EAST);
        addField(form, labeled("Contacto", contactoRow, 62));

        addField(form, row2(labeled("Cuenta", jbox_Cuentas, 62), labeled("Fecha", jdate_fecha_entrada, 62)));
        addField(form, labeled("Valor del egreso", txt_total, 62));

        JScrollPane descScroll = new JScrollPane(jtxa_descripcion_crear);
        descScroll.setBorder(BorderFactory.createLineBorder(EstiloCaja.LINE_2, 1));
        addField(form, labeled("Descripción", descScroll, 82));

        addField(form, labeled("Fondo", jbox_Fondos, 62));

        JPanel tipoPanel = new JPanel();
        tipoPanel.setOpaque(false);
        tipoPanel.setLayout(new BoxLayout(tipoPanel, BoxLayout.X_AXIS));
        tipoPanel.add(rbtn_F);
        tipoPanel.add(Box.createHorizontalStrut(22));
        tipoPanel.add(rbtn_R);
        tipoPanel.add(Box.createHorizontalGlue());
        addField(form, labeled("Tipo de soporte", tipoPanel, 58));

        form.add(Box.createVerticalStrut(4));
        JLabel sopl = EstiloCaja.sectionLabel("Soportes");
        sopl.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sopl);
        form.add(Box.createVerticalStrut(8));

        JPanel photoTools = new JPanel(new GridLayout(1, 3, 7, 0));
        photoTools.setOpaque(false);
        photoTools.add(btn_cargar_foto);
        photoTools.add(btn_cargar_foto1);
        photoTools.add(btn_eliminar_foto);
        addRaw(form, photoTools, 40);
        form.add(Box.createVerticalStrut(10));

        JScrollPane fotosScroll = new JScrollPane(jtabla_fotos);
        fotosScroll.setBorder(BorderFactory.createLineBorder(EstiloCaja.LINE_2, 1));
        addRaw(form, fotosScroll, 70);
        form.add(Box.createVerticalStrut(10));

        lbl_foto_1.setPreferredSize(new Dimension(10, 150));
        addRaw(form, lbl_foto_1, 150);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(formScroll, BorderLayout.CENTER);

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
        actions.add(btn_editar);
        actions.add(btn_eliminar);
        actions.add(btn_imprimir);
        actions.add(btn_imprimir1);
        actions.add(btn_total);
        actions.add(btn_actualizar);
        toolbar.add(actions, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(jtabla_gastos);
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

        JPanel totals = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        totals.setOpaque(false);
        totals.add(totalChunk("E", lbl_total_F, EstiloCaja.INFO, Font.BOLD, 17));
        totals.add(totalChunk("P", lbl_total_R, EstiloCaja.TEXT_2, Font.BOLD, 17));
        totals.add(totalChunk("Total", lbl_total_suma, ACENTO.base, Font.BOLD, 22));
        foot.add(totals, BorderLayout.EAST);
        card.add(foot, BorderLayout.SOUTH);

        return card;
    }

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
        head.add(EstiloCaja.title("Resumen por fondo · hoy", 13, Font.BOLD, EstiloCaja.TEXT), BorderLayout.WEST);
        card.add(head, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(jtabla_resumen);
        sp.setBorder(null);
        sp.getViewport().setBackground(EstiloCaja.SURFACE);
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    // ---- helpers de layout ----
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
    //  Logica (portada del formulario original)
    // =====================================================================
    public void quitar_fila(JTable tabla, DefaultTableModel modelo) {
        if (modelo.getRowCount() > 0) {
            int fila = tabla.getSelectedRow();
            if (tabla.getSelectedRowCount() < 1) {
                JOptionPane.showMessageDialog(this, "Seleccione un registro");
            } else {
                modelo.removeRow(fila);
            }
        }
    }

    public static void TamanosTablaAbonos() {
        if (columnModel == null || columnModel.getColumnCount() < 8) {
            return;
        }
        columnModel.getColumn(0).setPreferredWidth(50);   // id
        columnModel.getColumn(1).setPreferredWidth(120);  // cuenta
        columnModel.getColumn(2).setPreferredWidth(300);  // descripcion
        columnModel.getColumn(3).setPreferredWidth(80);   // fecha
        columnModel.getColumn(4).setPreferredWidth(70);   // hora
        columnModel.getColumn(5).setPreferredWidth(120);  // total
        columnModel.getColumn(6).setPreferredWidth(120);  // fondo
        columnModel.getColumn(7).setPreferredWidth(60);   // tipo

        columnModel.getColumn(0).setCellRenderer(new EstiloCaja.MutedRenderer());
        columnModel.getColumn(3).setCellRenderer(new EstiloCaja.MutedRenderer());
        columnModel.getColumn(4).setCellRenderer(new EstiloCaja.MutedRenderer());
        columnModel.getColumn(5).setCellRenderer(new EstiloCaja.MoneyRenderer());
        columnModel.getColumn(7).setCellRenderer(new EstiloCaja.TipoRenderer(ACENTO));
    }

    // Ajusta la foto proporcionalmente y centrada dentro del JLabel
    private static void foto_a_label(String ruta, final JLabel lbl_foto) {
        try {
            final Image img = new ImageIcon(ruta).getImage();
            int originalWidth = img.getWidth(null);
            int originalHeight = img.getHeight(null);
            double scaleFactor = Math.min(1.0 * lbl_foto.getWidth() / originalWidth, 1.0 * lbl_foto.getHeight() / originalHeight);
            int scaledWidth = (int) (originalWidth * scaleFactor);
            int scaledHeight = (int) (originalHeight * scaleFactor);
            final Image newimg = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_DEFAULT);

            ImageIcon newicon = new ImageIcon(newimg) {
                @Override
                public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
                    int offsetX = (lbl_foto.getWidth() - getIconWidth()) / 2;
                    int offsetY = (lbl_foto.getHeight() - getIconHeight()) / 2;
                    g.drawImage(newimg, x + offsetX, y + offsetY, c);
                }
            };
            lbl_foto.setIcon(newicon);
        } catch (Exception e) {
            System.out.println("NO se cargo la imagen");
        }
    }

    public void verEgreso() throws SQLException {
        int fila = jtabla_gastos.getSelectedRow();
        jd_ver_in_egre frm = new jd_ver_in_egre();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
            return;
        }
        String id = "" + jtabla_gastos.getValueAt(fila, 0);
        Egresos egreso = Egresos.traer_egreso(id);

        jd_ver_in_egre.lbl_user.setText(egreso.getNombre_user());
        jd_ver_in_egre.lbl_total.setText("$ " + metodos.formateador_dinero().format(egreso.getTotal()));
        jd_ver_in_egre.lbl_fecha.setText(egreso.getFecha());
        jd_ver_in_egre.lbl_hora.setText(egreso.getHora());
        jd_ver_in_egre.jtxa_descripcion.setText(egreso.getDescripcion());
        jd_ver_in_egre.lbl_cuenta_nombre.setText(egreso.getNombre_cuenta());
        jd_ver_in_egre.lbl_cuenta_nombre.setVisible(true);
        jd_ver_in_egre.lbl_cuenta_titulo.setVisible(true);

        try {
            for (int i = 0; i < jd_ver_in_egre.modelo_fotos.getRowCount(); i++) {
                jd_ver_in_egre.modelo_fotos.removeRow(i);
                i -= 1;
            }
        } catch (Exception m) {
            System.out.println(m);
        }

        // tipo_registro=2 es egreso en el esquema nuevo
        String consulta = "select * from fotos_registros where tipo_registro=2 and id_registro=" + id;
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        jd_ver_in_egre.modelo_fotos.setColumnIdentifiers(new Object[]{"Ruta", "Nombre", "id_foto"});
        while (rs.next()) {
            jd_ver_in_egre.modelo_fotos.addRow(new Object[]{DB_consultas_R_D.Ruta_Imagenes() + rs.getString("nombre"), rs.getString("nombre"), rs.getString("id")});
            jd_ver_in_egre.jtabla_fotos.setModel(jd_ver_in_egre.modelo_fotos);
            foto_a_label(DB_consultas_R_D.Ruta_Imagenes() + rs.getString("nombre"), jd_ver_in_egre.lbl_foto_1);
        }
        rs.close();
        frm.show();
    }

    public void actualizarResumen() {
        DefaultTableModel modelo_resumen = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        modelo_resumen.setColumnIdentifiers(new Object[]{"Fondo", "Tipo", "Total"});

        String consulta = "SELECT coalesce(f.nombre, 'Sin fondo') as fondo, "
                + "CASE WHEN e.factura_remision = 1 THEN 'E' ELSE 'P' END as tipo, "
                + "SUM(e.total) as total "
                + "FROM egresos e "
                + "LEFT JOIN fondos f ON e.id_fondo = f.id "
                + "WHERE e.fecha = CURRENT_DATE AND e.id_caja = " + idCaja + " "
                + "GROUP BY coalesce(f.nombre, 'Sin fondo'), e.factura_remision "
                + "ORDER BY fondo, tipo";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);
            while (rs.next()) {
                modelo_resumen.addRow(new Object[]{
                    rs.getString("fondo"),
                    rs.getString("tipo"),
                    metodos.formateador_dinero().format(rs.getDouble("total"))
                });
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("Error resumen: " + e);
        }
        jtabla_resumen.setModel(modelo_resumen);
    }

    public void calcular_total() {
        double total_general = 0, total_F = 0, total_R = 0;
        int cnt = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(
                    "select coalesce((select sum(total) from egresos where fecha='" + fecha_hoy + "' and id_caja=" + idCaja + "),0) as total,"
                    + "coalesce((select sum(total) from egresos where fecha='" + fecha_hoy + "' and id_caja=" + idCaja + " and factura_remision=1),0) as total_f,"
                    + "coalesce((select sum(total) from egresos where fecha='" + fecha_hoy + "' and id_caja=" + idCaja + " and factura_remision=0),0) as total_r,"
                    + "coalesce((select count(*) from egresos where fecha='" + fecha_hoy + "' and id_caja=" + idCaja + "),0) as cnt");
            while (rs.next()) {
                total_general = rs.getDouble("total");
                total_F = rs.getDouble("total_f");
                total_R = rs.getDouble("total_r");
                cnt = rs.getInt("cnt");
            }
            rs.close();
        } catch (Exception e) {
        }

        DecimalFormat f = metodos.formateador_dinero();
        lbl_total_suma.setText(f.format(total_general));
        lbl_total_F.setText(f.format(total_F));
        lbl_total_R.setText(f.format(total_R));

        lblKpiTotal.setText("$ " + f.format(total_general));
        lblKpiF.setText("$ " + f.format(total_F));
        lblKpiR.setText("$ " + f.format(total_R));
        lblKpiCount.setText(String.valueOf(cnt));
        double prom = cnt > 0 ? total_general / cnt : 0;
        lblKpiProm.setText("Ticket promedio  $" + f.format(prom));
    }

    public void validar_numeros(java.awt.event.KeyEvent evt, char car) {
        if ((car < '0' || car > '9')) {
            evt.consume();
        }
    }

    int id_cuenta = 0;
    int id_fondo = 0;

    private void btn_guardarActionPerformed() {
        if (jtxa_descripcion_crear.getText().isEmpty() || txt_total.getText().isEmpty()) {
            jtxa_descripcion_crear.setBackground(Color.pink);
            txt_total.setBackground(Color.pink);
            return;
        }
        DBEgresos dbegreso = new DBEgresos();
        Egresos obj = new Egresos();

        obj.setDescripcion(jtxa_descripcion_crear.getText());
        int dia, mes, ano;
        ano = jdate_fecha_entrada.getCalendar().get(Calendar.YEAR);
        mes = jdate_fecha_entrada.getCalendar().get(Calendar.MONTH) + 1;
        dia = jdate_fecha_entrada.getCalendar().get(Calendar.DAY_OF_MONTH);
        obj.setFecha(ano + "-" + mes + "-" + dia);
        obj.setHora(DB_consultas_R_D.obtener_hora());

        try {
            obj.setId_cuenta(jbox_Cuentas.getItemAt(jbox_Cuentas.getSelectedIndex()).getId());
        } catch (Exception e) {
            obj.setId_cuenta(id_cuenta);
        }
        try {
            obj.setId_fondo(jbox_Fondos.getItemAt(jbox_Fondos.getSelectedIndex()).getId());
        } catch (Exception e) {
            obj.setId_fondo(Fondos.TraerPredeterminado(idCaja));
        }
        try {
            obj.setId_cliente(Integer.parseInt(lbl_id_cliente.getText()));
        } catch (Exception e) {
            obj.setId_cliente(1);
        }

        obj.setId_user(frm_main.id_user);
        obj.setId_caja(idCaja);
        obj.setTotal(Double.parseDouble(metodos.EliminaCaracteres(txt_total.getText(), ".")));
        obj.setFactura_remision(rbtn_F.isSelected() ? 1 : 0);

        // Todo egreso se paga de contado: dinero entregado = true.
        // El origen queda en la auditoria de caja (trigger en base de datos).
        try {
            if (edita && DB_consultas_R_D.consultarId(lbl_id.getText(), "egresos") == 1) {
                AuditoriaCaja.setOrigen("Egresos - editar");
                obj.setId(Integer.parseInt(lbl_id.getText()));
                dbegreso.Actualizar(obj, true);
                edita = false;
            } else {
                AuditoriaCaja.setOrigen("Egresos - guardar");
                if (dbegreso.Guardar(obj, true) > 0) {
                    if (modelo_fotos.getRowCount() > 0) {
                        for (int i = 0; i < modelo_fotos.getRowCount(); i++) {
                            if (jtabla_fotos.getValueAt(i, 2).toString().equals("0")) {
                                metodos.copyFile_Java7(jtabla_fotos.getValueAt(i, 0).toString(), DB_consultas_R_D.Ruta_Imagenes() + jtabla_fotos.getValueAt(i, 1).toString());
                                // tipo_registro 2 = egreso
                                Fotos_registros foto = new Fotos_registros(jtabla_fotos.getValueAt(i, 1).toString(), obj.getId(), 0, 2);
                                DB_Fotos_servicios db = new DB_Fotos_servicios();
                                db.Guardar(foto);
                            }
                        }
                    }
                }
            }
        } finally {
            AuditoriaCaja.limpiar();
        }
        limpiar();
        act = 0;
        actualizar();
        actualizarResumen();
        txt_total.requestFocus();
    }

    public void limpiar() {
        jtxa_descripcion_crear.setText("");
        txt_total.setText("");
        txt_total.setBackground(Color.white);
        jtxa_descripcion_crear.setBackground(Color.white);
        txt_nombre_cliente.setText("0000");
        lbl_id_cliente.setText("1");
        lbl_id.setText("Nuevo");
        edita = false;
        jbox_Cuentas.setSelectedItem(Cuentas_Egresos.TraerPredeterminadoNombre(idCaja));
        jbox_Fondos.setSelectedItem(Fondos.TraerPredeterminadoNombre(idCaja));
        rbtn_F.setSelected(true);
        id_cuenta = Cuentas_Egresos.TraerPredeterminadoID(idCaja);
        lbl_foto_1.setIcon(null);
        nombreArchivoImagen = "";
        ruta_origen_imagen = "";
        ruta_foto = "";
        try {
            for (int i = 0; i < modelo_fotos.getRowCount(); i++) {
                modelo_fotos.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }
    }

    public boolean edita = false;

    private void btn_editarActionPerformed() {
        int fila = jtabla_gastos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
            return;
        }
        String id = "" + jtabla_gastos.getValueAt(fila, 0);
        if (DB_transferencias.esTraslado("egresos", id)) {
            JOptionPane.showMessageDialog(this, "Este egreso proviene de un traslado entre fondos y no se puede editar.\n"
                    + "Modifique o elimine el traslado desde el modulo de Traslados.", "Operacion no permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String consulta = "select i.*, u.nombre as user, cu.nombre as cuenta, cu.id as id_cuenta, coalesce(f.nombre,'default') as fondo, coalesce(i.id_fondo,0) as id_fondo_sel, co.nombre as cliente \n"
                + "from egresos i \n"
                + "left join fondos f on i.id_fondo=f.id, users u, cuentas_egresos cu, contactos co\n"
                + "where i.id_cuenta=cu.id and i.id_user=u.id and i.id_cliente=co.id and i.id=" + id;
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                lbl_id.setText(rs.getString("id"));
                txt_nombre_cliente.setText(rs.getString("cliente"));
                lbl_id_cliente.setText(rs.getString("id_cliente"));
                jtxa_descripcion_crear.setText(rs.getString("descripcion"));
                txt_total.setText(metodos.formateador_dinero().format(rs.getDouble("total")));
                jbox_Cuentas.setSelectedItem(rs.getString("cuenta"));
                id_cuenta = rs.getInt("id_cuenta");
                id_fondo = rs.getInt("id_fondo_sel");
                jdate_fecha_entrada.setDate(rs.getDate("fecha"));
                if (rs.getInt("factura_remision") == 1) {
                    rbtn_F.setSelected(true);
                } else {
                    rbtn_R.setSelected(true);
                }
                if (rs.getString("fondo").equals("default")) {
                    jbox_Fondos.setSelectedItem(Fondos.TraerPredeterminadoNombre(idCaja));
                } else {
                    jbox_Fondos.setSelectedItem(rs.getString("fondo"));
                }
                edita = true;
            }
            rs.close();
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        txt_total.requestFocus();
    }

    private void btn_eliminarActionPerformed() {
        int fila = jtabla_gastos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
            return;
        }
        String idSel = "" + jtabla_gastos.getValueAt(fila, 0);
        if (DB_transferencias.esTraslado("egresos", idSel)) {
            JOptionPane.showMessageDialog(this, "Este egreso proviene de un traslado entre fondos y no se puede eliminar.\n"
                    + "Elimine el traslado desde el modulo de Traslados.", "Operacion no permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar este egreso?", "Alerta", JOptionPane.YES_NO_OPTION);
        if (dialogResult == JOptionPane.YES_OPTION) {
            try {
                DefaultTableModel m = (DefaultTableModel) jtabla_gastos.getModel();
                String id = (String) jtabla_gastos.getValueAt(fila, 0);
                AuditoriaCaja.setOrigen("Egresos - eliminar");
                DB_consultas_R_D.eliminar("egresos", id);
                for (int i = 0; i < m.getRowCount(); i++) {
                    if (m.getValueAt(i, 0).equals(id)) {
                        m.removeRow(i);
                        btn_totalActionPerformed();
                        limpiar();
                        actualizarResumen();
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                AuditoriaCaja.limpiar();
            }
        }
    }

    private void btn_totalActionPerformed() {
        double total = 0;
        for (int i = 0; i < this.jtabla_gastos.getRowCount(); i++) {
            total += Double.parseDouble(metodos.EliminaCaracteres(this.jtabla_gastos.getValueAt(i, 5).toString(), "."));
        }
        lbl_total_suma.setText(metodos.formateador_dinero().format(total) + "");
        actualizarResumen();
    }

    private void jButton1ActionPerformed() {
        try {
            ExportarExcel obj = new ExportarExcel();
            obj.exportarExcel(jtabla_gastos);
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
    }

    private void btn_buscar_clienteActionPerformed() {
        jd_buscar_contacto_servicio buscar = new jd_buscar_contacto_servicio(null, rootPaneCheckingEnabled);
        buscar.formulario = "egreso";
        buscar.show();
    }

    private void btn_crear_clienteActionPerformed() {
        jif_crear_contactos frm = new jif_crear_contactos();
        jif_crear_contactos.formulario = "egreso";
        jif_crear_contactos.txt_nombre.requestFocus();
        frm.show();
    }

    private void btn_cargar_fotoActionPerformed() {
        ruta_origen_imagen = "";
        nombreArchivoImagen = "";
        lbl_foto_1.setIcon(null);

        JFileChooser j = new JFileChooser();
        j.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filtroImagen = new FileNameExtensionFilter("JPG, PNG & GIF", "jpg", "png", "gif");
        j.setFileFilter(filtroImagen);

        int estado = j.showOpenDialog(null);
        if (estado == JFileChooser.APPROVE_OPTION) {
            File fichero = j.getSelectedFile();
            ruta_origen_imagen = fichero.getAbsolutePath();
            nombreArchivoImagen = "egr_" + DB_consultas_R_D.obtener_fecha() + DB_consultas_R_D.obtener_hora_con_guiones() + "_" + fichero.getName();
            foto_a_label(ruta_origen_imagen, lbl_foto_1);
            modelo_fotos.addRow(new Object[]{ruta_origen_imagen, nombreArchivoImagen, 0});
            jtabla_fotos.setModel(modelo_fotos);
        }
    }

    private void btn_eliminar_fotoActionPerformed() {
        int fila = jtabla_fotos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
            return;
        }
        try {
            int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar esta imagen de este egreso?\n", "Alerta", JOptionPane.YES_NO_OPTION);
            if (dialogResult == JOptionPane.YES_OPTION) {
                String id_foto = jtabla_fotos.getValueAt(fila, 2).toString();
                String ruta = jtabla_fotos.getValueAt(fila, 0).toString();
                AuditoriaCaja.setOrigen("Egresos - eliminar soporte");
                DB_consultas_R_D.eliminar("fotos_registros", id_foto);
                AuditoriaCaja.limpiar();
                DB_consultas_R_D.Eliminar_Archivo(ruta);
                for (int i = 0; i < modelo_fotos.getRowCount(); i++) {
                    if (modelo_fotos.getValueAt(i, 2).toString().equals(id_foto)) {
                        modelo_fotos.removeRow(i);
                        break;
                    }
                }
                jtabla_fotos.setModel(modelo_fotos);
                lbl_foto_1.setIcon(null);
                nombreArchivoImagen = "";
                ruta_origen_imagen = "";
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudo realizar la eliminacion");
        }
    }

    private void btn_cargar_foto1ActionPerformed() {
        CapturaMejorada captura = new CapturaMejorada();
        String idUsuario = lbl_id.getText();
        captura.lanzar_camara(idUsuario, modelo_fotos, this);
    }

    private void btn_imprimirActionPerformed() {
        int fila = jtabla_gastos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
            return;
        }
        int dialogResult = JOptionPane.showConfirmDialog(null, "¿Desea imprimir este egreso?", "Alerta", JOptionPane.YES_NO_OPTION);
        if (dialogResult == JOptionPane.YES_OPTION) {
            String id = jtabla_gastos.getValueAt(fila, 0).toString();
            String consulta = "select i.id, i.fecha, i.hora, i.descripcion, i.total, co.nombre as contacto, cu.nombre as cuenta\n"
                    + "from egresos i inner join contactos co on i.id_cliente=co.id inner join cuentas_egresos cu on i.id_cuenta=cu.id\n"
                    + "where i.id = " + id;
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);

            ImprimirTermica80MM_caja_diaria imprimir = null;
            try {
                while (rs.next()) {
                    imprimir = new ImprimirTermica80MM_caja_diaria(rs.getString("fecha") + " / " + rs.getString("hora"), rs.getString("id"),
                            rs.getString("contacto"), metodos.formateador_dinero().format(rs.getDouble("total")),
                            jtabla_gastos.getValueAt(fila, 6).toString(), rs.getString("cuenta"), rs.getString("descripcion"), "EGRESO");
                }
                rs.close();
            } catch (SQLException ex) {
                System.out.println("");
            }
            try {
                imprimir.imprime();
            } catch (PrinterException ex) {
                System.out.println(ex);
            }
        }
    }

    private void btn_imprimir1ActionPerformed() {
        int fila = jtabla_gastos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
            return;
        }
        int dialogResult = JOptionPane.showConfirmDialog(null, "¿Desea imprimir este egreso?", "Alerta", JOptionPane.YES_NO_OPTION);
        if (dialogResult == JOptionPane.YES_OPTION) {
            String id = jtabla_gastos.getValueAt(fila, 0).toString();
            String consulta = "select i.id, i.fecha, i.hora, i.descripcion, i.total, co.nombre as contacto, cu.nombre as cuenta\n"
                    + "from egresos i inner join contactos co on i.id_cliente=co.id inner join cuentas_egresos cu on i.id_cuenta=cu.id\n"
                    + "where i.id = " + id;
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);

            ImprimirPNG_caja_diaria imprimir = null;
            try {
                while (rs.next()) {
                    imprimir = new ImprimirPNG_caja_diaria(rs.getString("fecha") + " / " + rs.getString("hora"), rs.getString("id"),
                            rs.getString("contacto"), metodos.formateador_dinero().format(rs.getDouble("total")),
                            jtabla_gastos.getValueAt(fila, 6).toString(), rs.getString("cuenta"), rs.getString("descripcion"), "EGRESO");
                }
                rs.close();
            } catch (SQLException ex) {
                System.out.println("");
            }
            imprimir.generarYGuardarImagen();
        }
    }

    int act = 0;

    public void actualizar() {
        try {
            for (int i = 0; i < jtabla_gastos.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

        String base = "SELECT e.id, e.factura_remision, c.nombre, e.total, e.descripcion, e.fecha, e.hora,\n"
                + "  coalesce(f.nombre,'Sin fondo') AS fondo\n"
                + "FROM egresos e\n"
                + "LEFT JOIN cuentas_egresos c ON e.id_cuenta = c.id\n"
                + "LEFT JOIN fondos f ON e.id_fondo = f.id\n"
                + "WHERE e.id_caja = " + idCaja + "\n";
        String consulta;
        if (act == 0) {
            consulta = base + "AND e.fecha = CURRENT_DATE\nORDER BY e.fecha DESC, e.id DESC";
            act = 1;
        } else {
            consulta = base + "ORDER BY e.fecha DESC, e.id DESC";
            act = 0;
        }

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        modelo.setColumnIdentifiers(new Object[]{"id", "Cuenta", "Descripcion", "Fecha", "Hora", "Total", "Fondo", "Tipo"});
        try {
            int filas = 0;
            while (rs.next()) {
                // E = Empresa (antes Factura), P = Personal (antes Remision)
                String tipo = rs.getInt("factura_remision") == 1 ? "E" : "P";
                modelo.addRow(new Object[]{
                    rs.getString("id"), rs.getString("nombre"), rs.getString("descripcion"),
                    rs.getDate("fecha"), rs.getString("hora"), metodos.formateador_dinero().format(rs.getDouble("total")),
                    rs.getString("fondo"), tipo});
                filas++;
            }
            rs.close();
            jtabla_gastos.setModel(modelo);
            TamanosTablaAbonos();
            calcular_total();
            actualizarResumen();
            lblFootInfo.setText("Mostrando " + filas + (act == 0 ? " (todos)" : " del día"));
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
