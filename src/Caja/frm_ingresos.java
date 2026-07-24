/*
 * Modulo Caja: registro y consulta de ingresos de dinero.
 * Rediseno "SaaS claro" (Stripe/Linear/Vercel): hand-coded con FlatLaf + Font
 * Awesome via el helper EstiloCaja. Sin abonos (todo se recibe de contado):
 * la tabla no muestra Estado ni Fecha pagado y no existe el selector
 * "Dinero recibido". El formulario de alta esta siempre visible (panel fijo),
 * no como ventana emergente. Toda la funcionalidad previa se conserva.
 */
package Caja;

import Formularios.frm_main;
import Metodos.ExportarExcel;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBIngresos;
import conexiondb.DB_Fotos_servicios;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Color;
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
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Contactos;
import modelos.Cuentas_Ingresos;
import modelos.Fondos;
import modelos.Fotos_registros;
import modelos.Ingresos;

/**
 * Formulario de Ingresos de caja (rediseno SaaS claro).
 *
 * @author Monkeyelgrande
 */
public class frm_ingresos extends javax.swing.JInternalFrame {

    public static DefaultTableModel modelo_fotos = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    /**
     * Color de identidad del modulo: VERDE = entra dinero. Egresos usara
     * EstiloCaja.EGRESOS (rojo) para que se distingan de un vistazo.
     */
    private static final EstiloCaja.Accent ACENTO = EstiloCaja.INGRESOS;

    DecimalFormat formatea = new DecimalFormat("###,###.##");
    Calendar fecha = new GregorianCalendar();
    static TableColumnModel columnModel = null;

    public String fecha_hoy = DB_consultas_R_D.obtener_fecha();

    // ---- Componentes (los public static son usados por otras clases) ----
    private ButtonGroup buttonGroup1;
    public static JButton btn_limpiar;
    public static JButton btn_guardar;
    private JTextField txt_total;
    private JTextArea jtxa_descripcion_crear;
    public static com.toedter.calendar.JDateChooser jdate_fecha_entrada;
    private JComboBox<Cuentas_Ingresos> jbox_Cuentas;
    public static JLabel lbl_id;
    private JComboBox<Fondos> jbox_Fondos;
    public static JComboBox<Contactos> jbox_contacto;
    private JRadioButton rbtn_F;
    private JRadioButton rbtn_R;
    public static JTable jtabla_fotos;
    public static JLabel lbl_foto_1;
    public static JButton btn_cargar_foto;
    public static JButton btn_cargar_foto1;
    public static JButton btn_eliminar_foto;
    public static JButton btn_crear_cliente;
    private JTable jtabla;
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
    private JButton btn_comparar;
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

    public frm_ingresos() {
        buildUI();

        Fondos.mostrarFondos(jbox_Fondos);
        modelo_fotos.setColumnIdentifiers(new Object[]{"Ruta", "Nombre", "id_foto"});
        jtabla_fotos.setModel(modelo_fotos);
        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        EstiloCaja.styleTable(jtabla, ACENTO);
        EstiloCaja.styleTable(jtabla_resumen, ACENTO);

        Contactos empleado = new Contactos();
        empleado.MostrarNombreContactos(jbox_contacto);
        jbox_contacto.setSelectedItem(traerContactoPredeterminadoNombre());

        jtxa_descripcion_crear.setWrapStyleWord(true);
        try {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

        actualizar();
        calcular_total();
        actualizarResumen();
        lbl_id.setText("Nuevo");

        Cuentas_Ingresos.mostrarCuentas(jbox_Cuentas);
        jbox_Cuentas.setSelectedItem(Cuentas_Ingresos.TraerPredeterminadoNombre());

        jdate_fecha_entrada.setCalendar(fecha);
        metodos.EvitarTabEnJTextArea(jtxa_descripcion_crear);

        TamanosTablaAbonos();

        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    try {
                        verIngreso();
                    } catch (SQLException ex) {
                        Logger.getLogger(frm_ingresos.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        jtabla_fotos.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 1) {
                    int fila = jtabla_fotos.getSelectedRow();
                    if (fila >= 0) {
                        String ruta = (String) jtabla_fotos.getValueAt(fila, 0);
                        jd_ver_in_egre.foto_a_label(ruta, lbl_foto_1);
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

        if (frm_main.perfil != 1) {
            btn_eliminar.setEnabled(false);
            btn_editar.setEnabled(false);
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
        setTitle("Ingresos");

        // ---- instanciar componentes ----
        buttonGroup1 = new ButtonGroup();
        btn_limpiar = EstiloCaja.ghost("Limpiar", null);
        btn_guardar = EstiloCaja.primary("Guardar ingreso", FontAwesome.CHECK, ACENTO);
        txt_total = new JTextField();
        jtxa_descripcion_crear = new JTextArea(3, 20);
        jdate_fecha_entrada = new com.toedter.calendar.JDateChooser();
        jbox_Cuentas = new JComboBox<>();
        lbl_id = new JLabel("Nuevo");
        jbox_Fondos = new JComboBox<>();
        jbox_contacto = new JComboBox<>();
        rbtn_F = new JRadioButton("Factura (F)", true);
        rbtn_R = new JRadioButton("Remisión (R)");
        jtabla_fotos = new JTable();
        lbl_foto_1 = new JLabel();
        btn_cargar_foto = EstiloCaja.ghost("Cargar", FontAwesome.UPLOAD);
        btn_cargar_foto1 = EstiloCaja.ghost("Capturar", FontAwesome.CAMERA);
        btn_eliminar_foto = EstiloCaja.ghost("Quitar", FontAwesome.TRASH);
        btn_crear_cliente = EstiloCaja.plusButton(FontAwesome.PLUS, "Crear contacto", ACENTO);
        jtabla = new JTable();
        txt_Filtro = new JTextField();
        btn_total = EstiloCaja.iconButton(FontAwesome.CALCULATOR, "Sumar seleccion / total");
        lbl_total_suma = new JLabel("0");
        lbl_total_F = new JLabel("0");
        lbl_total_R = new JLabel("0");
        lblFootInfo = EstiloCaja.title("", 12, Font.PLAIN, EstiloCaja.TEXT_3);
        jtabla_resumen = new JTable();
        btn_eliminar = EstiloCaja.iconButton(FontAwesome.TRASH_ALT, "Eliminar");
        btn_editar = EstiloCaja.iconButton(FontAwesome.EDIT, "Editar");
        btn_actualizar = EstiloCaja.iconButton(FontAwesome.SYNC, "Actualizar");
        btn_imprimir = EstiloCaja.iconButton(FontAwesome.PRINTER, "Imprimir recibo (térmica)");
        btn_comparar = EstiloCaja.iconButton(FontAwesome.BALANCE, "Comparar WO");
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

        EstiloCaja.styleCombo(jbox_contacto, ACENTO);
        EstiloCaja.styleCombo(jbox_Cuentas, ACENTO);
        EstiloCaja.styleCombo(jbox_Fondos, ACENTO);
        EstiloCaja.styleField(txt_total, "Valor del ingreso", null, ACENTO);
        txt_total.setFont(EstiloCaja.font(Font.BOLD, 15));
        EstiloCaja.styleField(txt_Filtro, "Buscar ingreso…", FontAwesome.SEARCH, ACENTO);
        EstiloCaja.styleArea(jtxa_descripcion_crear);

        lbl_foto_1.setBorder(BorderFactory.createLineBorder(EstiloCaja.LINE_2, 1));
        lbl_foto_1.setHorizontalAlignment(JLabel.CENTER);

        columnModel = jtabla.getColumnModel();

        // ---- listeners ----
        btn_limpiar.addActionListener(e -> limpiar());
        btn_guardar.addActionListener(e -> btn_guardarActionPerformed());
        btn_crear_cliente.addActionListener(e -> btn_crear_clienteActionPerformed());
        btn_cargar_foto.addActionListener(e -> btn_cargar_fotoActionPerformed());
        btn_cargar_foto1.addActionListener(e -> btn_cargar_foto1ActionPerformed());
        btn_eliminar_foto.addActionListener(e -> btn_eliminar_fotoActionPerformed());
        btn_editar.addActionListener(e -> btn_editarActionPerformed());
        btn_eliminar.addActionListener(e -> btn_eliminarActionPerformed());
        btn_actualizar.addActionListener(e -> actualizar());
        btn_imprimir.addActionListener(e -> btn_imprimirActionPerformed());
        btn_imprimir1.addActionListener(e -> btn_imprimir1ActionPerformed());
        btn_comparar.addActionListener(e -> btn_compararActionPerformed());
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
                    btn_guardar.requestFocus();
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
        JLabel h1 = EstiloCaja.title("Ingresos", 22, Font.BOLD, ACENTO.base);
        JLabel sub = EstiloCaja.title("Registro y consulta de ingresos de caja", 13, Font.PLAIN, EstiloCaja.TEXT_3);
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(h1);
        titles.add(Box.createVerticalStrut(2));
        titles.add(sub);

        // Barra vertical con el color de identidad del modulo (verde = entra dinero)
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

        // Fila de KPIs (bento)
        JPanel kpis = new JPanel(new GridLayout(1, 4, 14, 0));
        kpis.setOpaque(false);
        kpis.setAlignmentX(Component.LEFT_ALIGNMENT);
        kpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        kpis.add(kpiCard("Total del día", FontAwesome.CALCULATOR, lblKpiTotal,
                EstiloCaja.title("Suma de ingresos de hoy", 12, Font.PLAIN, EstiloCaja.TEXT_3), true));
        kpis.add(kpiCard("Facturas (F)", FontAwesome.FILE_INVOICE, lblKpiF,
                EstiloCaja.title("Ingresos con factura", 12, Font.PLAIN, EstiloCaja.TEXT_3)));
        kpis.add(kpiCard("Remisiones (R)", FontAwesome.CLIPBOARD, lblKpiR,
                EstiloCaja.title("Ingresos con remisión", 12, Font.PLAIN, EstiloCaja.TEXT_3)));
        kpis.add(kpiCard("Ingresos registrados", FontAwesome.LIST, lblKpiCount, lblKpiProm));
        header.add(kpis);

        return header;
    }

    private JPanel kpiCard(String label, String glyph, JLabel value, JLabel foot) {
        return kpiCard(label, glyph, value, foot, false);
    }

    /**
     * @param destacado si es true, pinta la franja superior y el valor con el
     * color de identidad del modulo (verde en Ingresos, rojo en Egresos).
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
        JLabel ic = new JLabel(FontAwesome.icon(FontAwesome.PLUS, 15f, ACENTO.base));
        JPanel ht = new JPanel();
        ht.setOpaque(false);
        ht.setLayout(new BoxLayout(ht, BoxLayout.Y_AXIS));
        ht.add(EstiloCaja.title("Nuevo ingreso", 15, Font.BOLD, EstiloCaja.TEXT));
        ht.add(EstiloCaja.title("Registra un ingreso de caja", 12, Font.PLAIN, EstiloCaja.TEXT_3));
        hl.add(ic);
        hl.add(ht);
        head.add(hl, BorderLayout.WEST);
        EstiloCaja.ChipLabel pill = new EstiloCaja.ChipLabel("INGRESO", ACENTO.base, ACENTO.tint);
        head.add(pill, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        // Contenido del formulario (FormPanel: se ajusta al ancho del viewport)
        EstiloCaja.FormPanel form = new EstiloCaja.FormPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(EstiloCaja.pad(16, 16, 16, 16));

        JPanel contactoRow = new JPanel(new BorderLayout(8, 0));
        contactoRow.setOpaque(false);
        contactoRow.add(jbox_contacto, BorderLayout.CENTER);
        btn_crear_cliente.setPreferredSize(new Dimension(44, 36));
        contactoRow.add(btn_crear_cliente, BorderLayout.EAST);
        addField(form, labeled("Contacto", contactoRow, 62));

        addField(form, row2(labeled("Cuenta", jbox_Cuentas, 62), labeled("Fecha", jdate_fecha_entrada, 62)));
        addField(form, labeled("Valor del ingreso", txt_total, 62));

        JScrollPane descScroll = new JScrollPane(jtxa_descripcion_crear);
        descScroll.setBorder(BorderFactory.createLineBorder(EstiloCaja.LINE_2, 1));
        descScroll.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc:10;");
        addField(form, labeled("Descripción", descScroll, 82));

        // Fondo y Tipo en filas completas: media tarjeta no alcanza para los
        // dos radios y "Remisión (R)" se envolvia a una segunda linea.
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
        actions.add(btn_editar);
        actions.add(btn_eliminar);
        actions.add(btn_imprimir);
        actions.add(btn_imprimir1);
        actions.add(btn_comparar);
        actions.add(btn_total);
        actions.add(btn_actualizar);
        toolbar.add(actions, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);

        // Tabla
        JScrollPane sp = new JScrollPane(jtabla);
        sp.setBorder(null);
        sp.getViewport().setBackground(EstiloCaja.SURFACE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        card.add(sp, BorderLayout.CENTER);

        // Footer con totales
        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCaja.LINE),
                EstiloCaja.pad(11, 16, 11, 16)));
        foot.add(lblFootInfo, BorderLayout.WEST);

        JPanel totals = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        totals.setOpaque(false);
        totals.add(totalChunk("F", lbl_total_F, EstiloCaja.INFO, Font.BOLD, 17));
        totals.add(totalChunk("R", lbl_total_R, EstiloCaja.TEXT_2, Font.BOLD, 17));
        totals.add(totalChunk("Total", lbl_total_suma, ACENTO.base, Font.BOLD, 22));
        foot.add(totals, BorderLayout.EAST);
        card.add(foot, BorderLayout.SOUTH);

        return card;
    }

    /** Bloque "Etiqueta: valor" para el pie de la tabla. */
    private JPanel totalChunk(String label, JLabel value, Color valueColor) {
        return totalChunkImpl(label, value, valueColor, Font.BOLD, 13);
    }

    private JPanel totalChunk(String label, JLabel value, Color valueColor, int style, int size) {
        return totalChunkImpl(label, value, valueColor, style, size);
    }

    private JPanel totalChunkImpl(String label, JLabel value, Color valueColor, int style, int size) {
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
    private String traerContactoPredeterminadoNombre() {
        String name = "";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select nombre from contactos where predeterminado=1");
            while (rs.next()) {
                name = rs.getString("nombre");
            }
            rs.close();
        } catch (Exception e) {
        }
        return name;
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
                + "CASE WHEN i.factura_remision = 1 THEN 'F' ELSE 'R' END as tipo, "
                + "SUM(i.total) as total "
                + "FROM ingresos i "
                + "LEFT JOIN fondos f ON i.id_fondo = f.id "
                + "WHERE i.fecha = CURRENT_DATE "
                + "GROUP BY coalesce(f.nombre, 'Sin fondo'), i.factura_remision "
                + "ORDER BY fondo, tipo";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);
            while (rs.next()) {
                modelo_resumen.addRow(new Object[]{
                    rs.getString("fondo"),
                    rs.getString("tipo"),
                    formatea.format(rs.getDouble("total"))
                });
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("Error resumen: " + e);
        }
        jtabla_resumen.setModel(modelo_resumen);
    }

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

    public void calcular_total() {
        double total_general = 0, total_F = 0, total_R = 0;
        int cnt = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(
                    "select coalesce((select sum(total) from ingresos where fecha='" + fecha_hoy + "'),0) as total,"
                    + "coalesce((select sum(total) from ingresos where fecha='" + fecha_hoy + "' and factura_remision=1),0) as total_f,"
                    + "coalesce((select sum(total) from ingresos where fecha='" + fecha_hoy + "' and factura_remision=0),0) as total_r,"
                    + "coalesce((select count(*) from ingresos where fecha='" + fecha_hoy + "'),0) as cnt");
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

    public void verIngreso() throws SQLException {
        int fila = jtabla.getSelectedRow();
        jd_ver_in_egre frm = new jd_ver_in_egre();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
            return;
        }
        String id = "" + jtabla.getValueAt(fila, 0);
        Ingresos ingreso = Ingresos.traer_ingreso(id);

        jd_ver_in_egre.lbl_user.setText(ingreso.getNombre_user());
        jd_ver_in_egre.lbl_total.setText("$ " + formatea.format(ingreso.getTotal()));
        jd_ver_in_egre.lbl_fecha.setText(ingreso.getFecha());
        jd_ver_in_egre.lbl_hora.setText(ingreso.getHora());
        jd_ver_in_egre.jtxa_descripcion.setText(ingreso.getDescripcion());
        jd_ver_in_egre.lbl_cuenta_nombre.setText(ingreso.getNombre_cuenta());
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

        String consulta = "select * from fotos_registros where tipo_registro=1 and id_registro=" + id;
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        jd_ver_in_egre.modelo_fotos.setColumnIdentifiers(new Object[]{"Ruta", "Nombre", "id_foto"});
        while (rs.next()) {
            jd_ver_in_egre.modelo_fotos.addRow(new Object[]{DB_consultas_R_D.Ruta_Imagenes() + rs.getString("nombre"), rs.getString("nombre"), rs.getString("id")});
            jd_ver_in_egre.jtabla_fotos.setModel(jd_ver_in_egre.modelo_fotos);
            jd_ver_in_egre.foto_a_label(DB_consultas_R_D.Ruta_Imagenes() + rs.getString("nombre"), jd_ver_in_egre.lbl_foto_1);
        }
        rs.close();
        frm.show();
    }

    public void validar_numeros(java.awt.event.KeyEvent evt, char car) {
        if ((car < '0' || car > '9')) {
            evt.consume();
        }
    }

    int id_cuenta = 0;
    int id_fondo = 0;

    private void btn_guardarActionPerformed() {
        if (txt_total.getText().isEmpty()) {
            txt_total.setBackground(Color.pink);
            return;
        }
        DBIngresos db_ingreso = new DBIngresos();
        Ingresos obj = new Ingresos();

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
            obj.setId_fondo(Fondos.TraerPredeterminado());
        }
        try {
            obj.setId_cliente(jbox_contacto.getItemAt(jbox_contacto.getSelectedIndex()).getId());
        } catch (Exception e) {
            obj.setId_cliente(1);
        }

        obj.setId_user(frm_main.id_user);
        obj.setTotal(Double.parseDouble(metodos.EliminaCaracteres(txt_total.getText(), ".")));
        obj.setFactura_remision(rbtn_F.isSelected() ? 1 : 0);

        // Todos los ingresos se reciben de contado: dinero recibido = true.
        if (edita) {
            obj.setId(Integer.parseInt(lbl_id.getText()));
            db_ingreso.Actualizar(obj, true);
            edita = false;
        } else {
            if (db_ingreso.Guardar(obj, true) > 0) {
                if (modelo_fotos.getRowCount() > 0) {
                    for (int i = 0; i < modelo_fotos.getRowCount(); i++) {
                        if (jtabla_fotos.getValueAt(i, 2).toString().equals("0")) {
                            metodos.copyFile_Java7(jtabla_fotos.getValueAt(i, 0).toString(), DB_consultas_R_D.Ruta_Imagenes() + jtabla_fotos.getValueAt(i, 1).toString());
                            Fotos_registros foto = new Fotos_registros(jtabla_fotos.getValueAt(i, 1).toString(), obj.getId(), 0, 1);
                            DB_Fotos_servicios db = new DB_Fotos_servicios();
                            db.Guardar(foto);
                        }
                    }
                }
            }
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
        lbl_id.setText("Nuevo");
        edita = false;
        jbox_Fondos.setSelectedItem(Fondos.TraerPredeterminadoNombre());
        jbox_Cuentas.setSelectedItem(Cuentas_Ingresos.TraerPredeterminadoNombre());
        jbox_contacto.setSelectedItem(traerContactoPredeterminadoNombre());
        rbtn_F.setSelected(true);
        id_cuenta = Cuentas_Ingresos.TraerPredeterminadoID();
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
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
            return;
        }
        String id = "" + jtabla.getValueAt(fila, 0);
        String consulta = "select i.id, i.descripcion, i.total, i.fecha, i.factura_remision, "
                + "u.nombre as user, cu.nombre as cuenta, cu.id as id_cuenta, "
                + "coalesce(f.nombre,'default') as fondo, coalesce(f.id,0) as id_fondo, co.nombre as cliente \n"
                + "from ingresos i \n"
                + "left join fondos f on i.id_fondo=f.id, users u, cuentas_ingresos cu, contactos co\n"
                + "where i.id_cuenta=cu.id and i.id_user=u.id and i.id_cliente=co.id and i.id=" + id;
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                lbl_id.setText(rs.getString("id"));
                jtxa_descripcion_crear.setText(rs.getString("descripcion"));
                txt_total.setText(formatea.format(rs.getDouble("total")));
                jbox_Cuentas.setSelectedItem(rs.getString("cuenta"));
                jbox_contacto.setSelectedItem(rs.getString("cliente"));
                id_cuenta = rs.getInt("id_cuenta");
                id_fondo = rs.getInt("id_fondo");
                jdate_fecha_entrada.setDate(rs.getDate("fecha"));
                if (rs.getInt("factura_remision") == 1) {
                    rbtn_F.setSelected(true);
                } else {
                    rbtn_R.setSelected(true);
                }
                if (rs.getString("fondo").equals("default")) {
                    jbox_Fondos.setSelectedItem(Fondos.TraerPredeterminadoNombre());
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
        int fila = jtabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
            return;
        }
        int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar este ingreso?", "Alerta", JOptionPane.YES_NO_OPTION);
        if (dialogResult == JOptionPane.YES_OPTION) {
            try {
                DefaultTableModel m = (DefaultTableModel) jtabla.getModel();
                String id = (String) jtabla.getValueAt(fila, 0);
                DB_consultas_R_D.eliminar("ingresos", id);
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
            }
        }
    }

    private void btn_totalActionPerformed() {
        double total = 0;
        for (int i = 0; i < this.jtabla.getRowCount(); i++) {
            total += Double.parseDouble(metodos.EliminaCaracteres(this.jtabla.getValueAt(i, 5).toString(), "."));
        }
        lbl_total_suma.setText(formatea.format(total) + "");
        actualizarResumen();
    }

    private void jButton1ActionPerformed() {
        try {
            ExportarExcel obj = new ExportarExcel();
            obj.exportarExcel(jtabla);
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
    }

    private void btn_crear_clienteActionPerformed() {
        jif_crear_contactos frm = new jif_crear_contactos();
        jif_crear_contactos.formulario = "ingreso";
        jif_crear_contactos.txt_nombre.requestFocus();
        frm.show();
    }

    private void btn_imprimirActionPerformed() {
        int fila = jtabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
            return;
        }
        int dialogResult = JOptionPane.showConfirmDialog(null, "¿Desea imprimir este ingreso?", "Alerta", JOptionPane.YES_NO_OPTION);
        if (dialogResult == JOptionPane.YES_OPTION) {
            String id = (String) jtabla.getValueAt(fila, 0);
            String consulta = "select i.id, i.fecha, i.hora, i.descripcion, i.total, co.nombre as contacto, cu.nombre as cuenta\n"
                    + "from ingresos i inner join contactos co on i.id_cliente=co.id inner join cuentas_ingresos cu on i.id_cuenta=cu.id\n"
                    + "where i.id = " + id;
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);
            ImprimirTermica80MM imprimir = null;
            try {
                while (rs.next()) {
                    imprimir = new ImprimirTermica80MM(rs.getString("fecha") + " / " + rs.getString("hora"), rs.getString("id"), rs.getString("contacto"), metodos.formateador_dinero().format(rs.getDouble("total")), "", rs.getString("cuenta"), rs.getString("descripcion"));
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

    String ruta_foto = "";
    String ruta_origen_imagen = "";
    public static String nombreArchivoImagen = "";

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
            nombreArchivoImagen = "ing_" + DB_consultas_R_D.obtener_fecha() + DB_consultas_R_D.obtener_hora_con_guiones() + "_" + fichero.getName();
            jd_ver_in_egre.foto_a_label(ruta_origen_imagen, lbl_foto_1);
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
            int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar esta imagen de este servicio?\n", "Alerta", JOptionPane.YES_NO_OPTION);
            if (dialogResult == JOptionPane.YES_OPTION) {
                String id_foto = jtabla_fotos.getValueAt(fila, 2).toString();
                String ruta = jtabla_fotos.getValueAt(fila, 0).toString();
                DB_consultas_R_D.eliminar("fotos_registros", id_foto);
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

    private void btn_compararActionPerformed() {
        jd_comparar_ingresos frm = new jd_comparar_ingresos(null, false);
        frm.setVisible(true);
    }

    private void btn_imprimir1ActionPerformed() {
        int fila = jtabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
            return;
        }
        int dialogResult = JOptionPane.showConfirmDialog(null, "¿Desea imprimir este ingreso?", "Alerta", JOptionPane.YES_NO_OPTION);
        if (dialogResult == JOptionPane.YES_OPTION) {
            String id = jtabla.getValueAt(fila, 0).toString();
            String consulta = "select i.id, i.fecha, i.hora, i.descripcion, i.total, co.nombre as contacto, cu.nombre as cuenta\n"
                    + "from ingresos i inner join contactos co on i.id_cliente=co.id inner join cuentas_ingresos cu on i.id_cuenta=cu.id\n"
                    + "where i.id = " + id;
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);
            ImprimirPNG_caja_diaria imprimir = null;
            try {
                while (rs.next()) {
                    imprimir = new ImprimirPNG_caja_diaria(rs.getString("fecha") + " / " + rs.getString("hora"), rs.getString("id"),
                            rs.getString("contacto"), metodos.formateador_dinero().format(rs.getDouble("total")), "", rs.getString("cuenta"), rs.getString("descripcion"), "INGRESO");
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
            for (int i = 0; i < jtabla.getRowCount(); i++) {
                modelo.removeRow(i);
                i -= 1;
            }
        } catch (Exception e) {
        }

        String base = "select i.id, i.factura_remision, c.nombre, i.total, i.descripcion, i.fecha, i.hora, f.nombre as fondo\n"
                + "from ingresos i \n"
                + "left join cuentas_ingresos c on i.id_cuenta=c.id \n"
                + "left join fondos f on i.id_fondo=f.id \n";
        String consulta;
        if (act == 0) {
            consulta = base + "where i.fecha=CURRENT_DATE \norder by i.fecha desc, i.id desc";
            act = 1;
        } else {
            consulta = base + "order by i.fecha desc, i.id desc";
            act = 0;
        }

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        modelo.setColumnIdentifiers(new Object[]{"id", "Cuenta", "Descripcion", "Fecha", "Hora", "Total", "Fondo", "Tipo"});
        try {
            int filas = 0;
            while (rs.next()) {
                String tipo = rs.getInt("factura_remision") == 1 ? "F" : "R";
                modelo.addRow(new Object[]{
                    rs.getString("id"), rs.getString("nombre"), rs.getString("descripcion"),
                    rs.getDate("fecha"), rs.getString("hora"), formatea.format(rs.getDouble("total")),
                    rs.getString("fondo"), tipo});
                filas++;
            }
            rs.close();
            jtabla.setModel(modelo);
            TamanosTablaAbonos();
            calcular_total();
            actualizarResumen();
            lblFootInfo.setText("Mostrando " + filas + (act == 0 ? " (todos)" : " del día"));
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
