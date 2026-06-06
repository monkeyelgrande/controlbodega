package Formularios_internos;

import Formularios.jif_users;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBusers;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import modelos.Bodegas;
import modelos.Perfiles;
import modelos.Users;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Formulario Material/empresarial para crear o editar un usuario.
 *
 * @author Monkeyelgrande
 */
public class jif_crear_user extends JDialog {

    private static final Color PRIMARY = new Color(0x2E7D32);
    private static final Color PRIMARY_DARK = new Color(0x1B5E20);
    private static final Color PRIMARY_HOVER = new Color(0x388E3C);
    private static final Color BG_FORM = Color.WHITE;
    private static final Color BG_SECTION = new Color(0xFAFAFA);
    private static final Color TEXT_PRIMARY = new Color(0x212121);
    private static final Color TEXT_SECONDARY = new Color(0x757575);
    private static final Color DANGER_BG = new Color(0xFFEBEE);
    private static final Color DIVIDER = new Color(0xE0E0E0);

    private static final int WINDOW_W = 760;
    private static final int WINDOW_H = 800;

    public static JTextField txt_nombre;
    public static JTextField txt_user_name;
    public static JTextField txt_contacto;
    public static JTextField txt_contacto2;
    public static JTextField txt_direccion;
    public static JTextField txt_email;
    public static JTextField txt_id;
    public static JPasswordField txt_password;
    public static JComboBox jbox_estado;
    public static JComboBox<Perfiles> jbox_perfil;
    public static JComboBox<Bodegas> jbox_bodega;
    public static JCheckBox chk_cerrar;
    public static JCheckBox chk_imprime_ordenes;
    public static JComboBox<String> cmb_impresora;
    public static JCheckBox chk_imp_ticket_bodega;
    public static JCheckBox chk_barra_notificaciones;
    public static JCheckBox chk_panel_notificaciones;
    public static JCheckBox chk_aprueba_compras;
    public static JButton btn_guardar;
    public static JButton btn_limpiar;
    public static JButton btn_editar;

    public jif_crear_user() {
        initUI();
        cargar_jBox();
        txt_id.setText(DB_consultas_R_D.cargarId("users"));
        btn_editar.setVisible(false);
        metodos.addEscapeListenerWindowDialog(this);
        Bodegas bod = new Bodegas();
        bod.mostrarBodegas(jbox_bodega);
        setLocationRelativeTo(null);
    }

    private void initUI() {
        setModal(true);
        setUndecorated(true);
        setSize(WINDOW_W, WINDOW_H);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_FORM);
        root.setBorder(BorderFactory.createLineBorder(DIVIDER, 1));

        root.add(buildHeader(), BorderLayout.NORTH);

        // Body envuelto en JScrollPane: el formulario es alto y puede no caber
        // en pantallas pequenas. Header (NORTH) y footer (SOUTH) quedan fijos
        // — solo el cuerpo hace scroll. Asi los botones siempre estan visibles.
        JPanel body = buildBody();
        JScrollPane scrollBody = new JScrollPane(body,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollBody.setBorder(null);
        scrollBody.getViewport().setBackground(BG_FORM);
        scrollBody.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scrollBody, BorderLayout.CENTER);

        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, PRIMARY_DARK, getWidth(), 0, PRIMARY));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(WINDOW_W, 64));
        header.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 12));

        JLabel icon = new JLabel(new FlatSVGIcon("imagenes/svg/user.svg", 24, 24)
                .setColorFilter(new FlatSVGIcon.ColorFilter() {
                    @Override public Color filter(Color c) { return Color.WHITE; }
                }));

        JLabel title = new JLabel("Gestion de usuario");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.X_AXIS));
        titleWrap.add(icon);
        titleWrap.add(title);

        JButton close = createHeaderIconButton("imagenes/svg/close.svg");
        close.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });

        header.add(titleWrap, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_FORM);
        body.setBorder(BorderFactory.createEmptyBorder(20, 28, 12, 28));

        // ---------- DATOS PERSONALES ----------
        body.add(sectionTitle("Datos personales"));
        body.add(Box.createVerticalStrut(10));

        txt_id = newField(false);
        txt_id.setEnabled(false);
        txt_id.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "ID");
        txt_id.setPreferredSize(new Dimension(90, 38));

        txt_nombre = newField(true);
        txt_nombre.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre y apellido");
        txt_nombre.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("imagenes/svg/user.svg", 16, 16));

        body.add(rowTwo(
                labeled("ID", txt_id, 100),
                labeled("Nombre y apellido", txt_nombre, 0)));
        body.add(Box.createVerticalStrut(12));

        txt_email = newField(true);
        txt_email.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "correo@ejemplo.com");
        txt_email.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("imagenes/svg/mail.svg", 16, 16));
        body.add(labeled("Email", txt_email, 0));
        body.add(Box.createVerticalStrut(12));

        txt_contacto = newField(true);
        txt_contacto.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Numero celular");
        txt_contacto.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("imagenes/svg/phone.svg", 16, 16));
        txt_contacto.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent evt) {
                DB_consultas_R_D.validar_numeros(evt, evt.getKeyChar());
            }
        });

        txt_contacto2 = newField(true);
        txt_contacto2.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Telefono fijo");
        txt_contacto2.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("imagenes/svg/phone.svg", 16, 16));
        txt_contacto2.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent evt) {
                DB_consultas_R_D.validar_numeros(evt, evt.getKeyChar());
            }
        });

        body.add(rowTwo(
                labeled("Celular", txt_contacto, 0),
                labeled("Telefono", txt_contacto2, 0)));
        body.add(Box.createVerticalStrut(12));

        txt_direccion = newField(true);
        txt_direccion.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Direccion");
        txt_direccion.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("imagenes/svg/map-pin.svg", 16, 16));
        body.add(labeled("Direccion", txt_direccion, 0));
        body.add(Box.createVerticalStrut(20));

        // ---------- ACCESO ----------
        body.add(sectionTitle("Acceso"));
        body.add(Box.createVerticalStrut(10));

        txt_user_name = newField(true);
        txt_user_name.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre de usuario");
        txt_user_name.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("imagenes/svg/user.svg", 16, 16));

        txt_password = new JPasswordField();
        txt_password.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt_password.setPreferredSize(new Dimension(0, 38));
        txt_password.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Contrasena");
        txt_password.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("imagenes/svg/lock.svg", 16, 16));
        txt_password.putClientProperty("JPasswordField.showRevealButton", true);

        body.add(rowTwo(
                labeled("Usuario", txt_user_name, 0),
                labeled("Contrasena", txt_password, 0)));
        body.add(Box.createVerticalStrut(12));

        jbox_perfil = new JComboBox<>();
        styleCombo(jbox_perfil);

        jbox_estado = new JComboBox(new String[]{"Activo", "Inactivo"});
        styleCombo(jbox_estado);

        body.add(rowTwo(
                labeled("Perfil", jbox_perfil, 0),
                labeled("Estado", jbox_estado, 0)));
        body.add(Box.createVerticalStrut(20));

        // ---------- ASIGNACION ----------
        body.add(sectionTitle("Asignacion"));
        body.add(Box.createVerticalStrut(10));

        jbox_bodega = new JComboBox<>();
        styleCombo(jbox_bodega);
        jbox_bodega.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        body.add(labeled("Bodega", jbox_bodega, 0));
        body.add(Box.createVerticalStrut(20));

        // ---------- IMPRESION ----------
        body.add(sectionTitle("Impresion"));
        body.add(Box.createVerticalStrut(10));

        chk_imprime_ordenes = new JCheckBox("Imprimir ordenes automaticamente");
        chk_imprime_ordenes.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chk_imprime_ordenes.setBackground(BG_FORM);
        chk_imprime_ordenes.setFocusPainted(false);

        cmb_impresora = new JComboBox<>();
        javax.print.PrintService[] services =
                javax.print.PrintServiceLookup.lookupPrintServices(null, null);
        for (javax.print.PrintService ps : services) {
            cmb_impresora.addItem(ps.getName());
        }
        cmb_impresora.setEditable(true);
        cmb_impresora.setEnabled(false);
        styleCombo(cmb_impresora);

        chk_imprime_ordenes.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                cmb_impresora.setEnabled(chk_imprime_ordenes.isSelected());
            }
        });

        JPanel chkRow = new JPanel();
        chkRow.setLayout(new BoxLayout(chkRow, BoxLayout.X_AXIS));
        chkRow.setBackground(BG_FORM);
        chkRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkRow.add(chk_imprime_ordenes);
        chkRow.add(Box.createHorizontalGlue());
        body.add(chkRow);
        body.add(Box.createVerticalStrut(8));
        body.add(labeled("Impresora", cmb_impresora, 0));
        body.add(Box.createVerticalStrut(8));

        // Copia adicional filtrada por la bodega del usuario
        chk_imp_ticket_bodega = new JCheckBox("Imprimir ademas copia con solo los productos de su bodega");
        chk_imp_ticket_bodega.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chk_imp_ticket_bodega.setBackground(BG_FORM);
        chk_imp_ticket_bodega.setFocusPainted(false);
        chk_imp_ticket_bodega.setSelected(false);

        JPanel chkRow2 = new JPanel();
        chkRow2.setLayout(new BoxLayout(chkRow2, BoxLayout.X_AXIS));
        chkRow2.setBackground(BG_FORM);
        chkRow2.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkRow2.add(chk_imp_ticket_bodega);
        chkRow2.add(Box.createHorizontalGlue());
        body.add(chkRow2);
        body.add(Box.createVerticalStrut(20));

        // ---------- NOTIFICACIONES ----------
        body.add(sectionTitle("Notificaciones"));
        body.add(Box.createVerticalStrut(10));

        chk_barra_notificaciones = new JCheckBox("Mostrar barra lateral de notificaciones de ordenes");
        chk_barra_notificaciones.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chk_barra_notificaciones.setBackground(BG_FORM);
        chk_barra_notificaciones.setFocusPainted(false);
        chk_barra_notificaciones.setSelected(false);

        JPanel chkRow3 = new JPanel();
        chkRow3.setLayout(new BoxLayout(chkRow3, BoxLayout.X_AXIS));
        chkRow3.setBackground(BG_FORM);
        chkRow3.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkRow3.add(chk_barra_notificaciones);
        chkRow3.add(Box.createHorizontalGlue());
        body.add(chkRow3);
        body.add(Box.createVerticalStrut(8));

        chk_panel_notificaciones = new JCheckBox("Mostrar panel de productos agotados / en negativo");
        chk_panel_notificaciones.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chk_panel_notificaciones.setBackground(BG_FORM);
        chk_panel_notificaciones.setFocusPainted(false);
        chk_panel_notificaciones.setSelected(false);

        JPanel chkRow4 = new JPanel();
        chkRow4.setLayout(new BoxLayout(chkRow4, BoxLayout.X_AXIS));
        chkRow4.setBackground(BG_FORM);
        chkRow4.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkRow4.add(chk_panel_notificaciones);
        chkRow4.add(Box.createHorizontalGlue());
        body.add(chkRow4);
        body.add(Box.createVerticalStrut(8));

        chk_aprueba_compras = new JCheckBox("Puede analizar histórico y aprobar órdenes de compra");
        chk_aprueba_compras.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chk_aprueba_compras.setBackground(BG_FORM);
        chk_aprueba_compras.setFocusPainted(false);
        chk_aprueba_compras.setSelected(false);

        JPanel chkRow5 = new JPanel();
        chkRow5.setLayout(new BoxLayout(chkRow5, BoxLayout.X_AXIS));
        chkRow5.setBackground(BG_FORM);
        chkRow5.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkRow5.add(chk_aprueba_compras);
        chkRow5.add(Box.createHorizontalGlue());
        body.add(chkRow5);
        body.add(Box.createVerticalStrut(8));

        return body;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG_SECTION);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)));
        footer.setPreferredSize(new Dimension(WINDOW_W, 70));

        chk_cerrar = new JCheckBox("Cerrar al guardar");
        chk_cerrar.setSelected(true);
        chk_cerrar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chk_cerrar.setOpaque(false);
        chk_cerrar.setForeground(TEXT_SECONDARY);
        chk_cerrar.setFocusPainted(false);

        btn_limpiar = new JButton("Limpiar", new FlatSVGIcon("imagenes/svg/refresh.svg", 16, 16));
        styleSecondaryButton(btn_limpiar);
        btn_limpiar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { limpiar(); }
        });

        btn_editar = new JButton("Editar", new FlatSVGIcon("imagenes/svg/edit.svg", 16, 16));
        styleSecondaryButton(btn_editar);
        btn_editar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { habilitarEdicion(); }
        });

        btn_guardar = new JButton("Guardar", new FlatSVGIcon("imagenes/svg/save.svg", 16, 16));
        stylePrimaryButton(btn_guardar);
        btn_guardar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { guardar(); }
        });

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(btn_editar);
        right.add(Box.createHorizontalStrut(8));
        right.add(btn_limpiar);
        right.add(Box.createHorizontalStrut(8));
        right.add(btn_guardar);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.add(chk_cerrar);

        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    // ---------- HELPERS UI ----------

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(PRIMARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
                BorderFactory.createEmptyBorder(0, 0, 6, 0)));
        return l;
    }

    private JComponent labeled(String label, JComponent field, int fixedWidth) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(field);

        if (fixedWidth > 0) {
            Dimension d = new Dimension(fixedWidth, 70);
            p.setMaximumSize(d);
            p.setPreferredSize(d);
        } else {
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        }
        return p;
    }

    private JComponent rowTwo(JComponent a, JComponent b) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(a);
        row.add(Box.createHorizontalStrut(14));
        row.add(b);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        return row;
    }

    private JTextField newField(boolean enabled) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setPreferredSize(new Dimension(0, 38));
        f.setEnabled(enabled);
        return f;
    }

    private void styleCombo(JComboBox combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(0, 38));
    }

    private void stylePrimaryButton(JButton b) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(PRIMARY);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setIconTextGap(8);
        final Color base = PRIMARY;
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(PRIMARY_HOVER); }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(base); }
        });
    }

    private void styleSecondaryButton(JButton b) {
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setForeground(PRIMARY);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIVIDER, 1),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setIconTextGap(8);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(BG_SECTION); }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(Color.WHITE); }
        });
    }

    private JButton createHeaderIconButton(String svg) {
        final FlatSVGIcon ic = new FlatSVGIcon(svg, 18, 18)
                .setColorFilter(new FlatSVGIcon.ColorFilter() {
                    @Override public Color filter(Color c) { return Color.WHITE; }
                });
        final JButton b = new JButton(ic);
        b.setOpaque(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------- LOGICA ----------

    public void cargar_jBox() {
        Perfiles perfiles = new Perfiles();
        perfiles.mostrar_perfiles(jbox_perfil);
    }

    private void guardar() {
        boolean nombreVacio = txt_nombre.getText().isEmpty();
        boolean userVacio = txt_user_name.getText().isEmpty();
        if (nombreVacio || userVacio) {
            txt_nombre.setBackground(nombreVacio ? DANGER_BG : Color.WHITE);
            txt_user_name.setBackground(userVacio ? DANGER_BG : Color.WHITE);
            JOptionPane.showMessageDialog(this,
                    "Nombre y usuario son obligatorios.",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DBusers dbusers = new DBusers();
        Users user = new Users();
        try {
            user.setId(Integer.parseInt(txt_id.getText()));
        } catch (Exception ignored) {
        }
        user.setNombre(txt_nombre.getText());
        user.setPassword(DigestUtils.sha256Hex(new String(txt_password.getPassword())));
        user.setUser_name(txt_user_name.getText());
        user.setDireccion(txt_direccion.getText());
        user.setTelefono(txt_contacto.getText());
        user.setTelefono2(txt_contacto2.getText());
        user.setEstado((String) jbox_estado.getItemAt(jbox_estado.getSelectedIndex()));
        user.setId_perfil(jbox_perfil.getItemAt(jbox_perfil.getSelectedIndex()).getId());

        try {
            user.setId_bodega(jbox_bodega.getItemAt(jbox_bodega.getSelectedIndex()).getId());
        } catch (Exception e) {
            user.setId_bodega(1);
        }

        user.setEmail(txt_email.getText());
        if (new String(txt_password.getPassword()).equals("")) {
            user.setPassword("");
        }

        user.setImprime_ordenes(chk_imprime_ordenes.isSelected());
        if (chk_imprime_ordenes.isSelected()) {
            Object sel = cmb_impresora.getSelectedItem();
            user.setNombre_impresora(sel == null ? null : sel.toString());
        } else {
            user.setNombre_impresora(null);
        }
        user.setImp_ticket_bodega_asignada(
                chk_imp_ticket_bodega != null && chk_imp_ticket_bodega.isSelected());
        user.setBarra_notificaciones(
                chk_barra_notificaciones != null && chk_barra_notificaciones.isSelected());
        user.setPanel_notificaciones(
                chk_panel_notificaciones != null && chk_panel_notificaciones.isSelected());
        user.setAprueba_compras(
                chk_aprueba_compras != null && chk_aprueba_compras.isSelected());

        if (DB_consultas_R_D.consultarId(txt_id.getText(), "users") == 1) {
            dbusers.Actualizar(user);
        } else {
            dbusers.Guardar(user);
        }
        limpiar();
        txt_id.setText(DB_consultas_R_D.cargarId("users"));
        if (chk_cerrar.isSelected()) {
            dispose();
        }
        jif_users.btn_actualizar.doClick();
    }

    public void limpiar() {
        txt_nombre.setText("");
        txt_nombre.setBackground(Color.WHITE);
        txt_user_name.setText("");
        txt_user_name.setBackground(Color.WHITE);
        txt_direccion.setText("");
        txt_contacto.setText("");
        txt_contacto2.setText("");
        txt_email.setText("");
        txt_password.setText("");
        if (chk_imprime_ordenes != null) {
            chk_imprime_ordenes.setSelected(false);
            cmb_impresora.setEnabled(false);
            cmb_impresora.setSelectedIndex(-1);
        }
        if (chk_imp_ticket_bodega != null) {
            chk_imp_ticket_bodega.setSelected(false);
        }
        if (chk_barra_notificaciones != null) {
            chk_barra_notificaciones.setSelected(false);
        }
        if (chk_panel_notificaciones != null) {
            chk_panel_notificaciones.setSelected(false);
        }
        if (chk_aprueba_compras != null) {
            chk_aprueba_compras.setSelected(false);
        }
        txt_nombre.requestFocus();
        txt_id.setText(DB_consultas_R_D.cargarId("users"));
    }

    private void habilitarEdicion() {
        txt_nombre.setEnabled(true);
        txt_user_name.setEnabled(true);
        txt_contacto.setEnabled(true);
        txt_contacto2.setEnabled(true);
        txt_direccion.setEnabled(true);
        txt_email.setEnabled(true);
        txt_password.setEnabled(true);
        jbox_estado.setEnabled(true);
        jbox_perfil.setEnabled(true);
        btn_guardar.setEnabled(true);
        btn_limpiar.setEnabled(true);
        chk_cerrar.setEnabled(true);
    }
}
