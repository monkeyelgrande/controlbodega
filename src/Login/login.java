package Login;

import Formularios.frm_main;
import conexiondb.DB_consultas_R_D;
import static conexiondb.DB_consultas_R_D.getTabla;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import modelos.Users;
import org.apache.commons.codec.digest.DigestUtils;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

/**
 * Pantalla de inicio de sesion con estetica Material / empresarial.
 *
 * @author Monkeyelgrande
 */
public class login extends JFrame {

    private static final Color PRIMARY = new Color(0x2E7D32);
    private static final Color PRIMARY_DARK = new Color(0x1B5E20);
    private static final Color PRIMARY_HOVER = new Color(0x388E3C);
    private static final Color BG_FORM = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(0x212121);
    private static final Color TEXT_SECONDARY = new Color(0x757575);
    private static final Color FIELD_BORDER = new Color(0xBDBDBD);

    private static final int WINDOW_W = 920;
    private static final int WINDOW_H = 560;
    private static final int ARC = 24;

    Users users = new Users();
    public int id_u;
    public int per;

    private JTextField txt_user;
    private JPasswordField jpassword;
    private JButton btn_inicio;
    private JButton btn_close;
    private JButton btn_minimize;

    public login() {
        initUI();
        applyAutoComplete();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent e) {
                txt_user.requestFocusInWindow();
            }
        });
    }

    private void initUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setUndecorated(true);
        setSize(WINDOW_W, WINDOW_H);
        setLocationRelativeTo(null);
        setShape(new RoundRectangle2D.Double(0, 0, WINDOW_W, WINDOW_H, ARC, ARC));
        try {
            setIconImage(loadLogo().getImage());
        } catch (Exception ignored) {
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_FORM);
        root.add(buildBrandPanel(), BorderLayout.WEST);
        root.add(buildFormPanel(), BorderLayout.CENTER);
        setContentPane(root);

        attachWindowDragger(root);
    }

    private JPanel buildBrandPanel() {
        JPanel brand = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, PRIMARY_DARK, w, h, PRIMARY));
                g2.fillRect(0, 0, w, h);
                // Decoracion: circulos translucidos
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.10f));
                g2.setColor(Color.WHITE);
                g2.fillOval(-80, h - 220, 320, 320);
                g2.fillOval(w - 160, -120, 280, 280);
                g2.dispose();
            }
        };
        brand.setOpaque(false);
        brand.setPreferredSize(new Dimension(380, WINDOW_H));

        // Logo
        JLabel logo = new JLabel();
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            Image scaled = loadLogo().getImage().getScaledInstance(110, 110, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            logo.setIcon(new FlatSVGIcon("imagenes/svg/leaf.svg", 96, 96));
        }

        JLabel title = new JLabel("CONTROL BODEGA");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel tagline = new JLabel("Sistema de gestion de bodegas");
        tagline.setForeground(new Color(255, 255, 255, 200));
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tagline.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel separator = new JLabel();
        separator.setOpaque(true);
        separator.setBackground(new Color(255, 255, 255, 80));
        separator.setPreferredSize(new Dimension(60, 2));

        JLabel footer = new JLabel("\u00A9 " + java.time.Year.now() + " Monkeyelgrande");
        footer.setForeground(new Color(255, 255, 255, 160));
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 1.0;
        gc.insets = new Insets(0, 30, 16, 30);

        gc.gridy = 0;
        gc.weighty = 1.0;
        gc.anchor = GridBagConstraints.SOUTH;
        brand.add(logo, gc);

        gc.gridy = 1;
        gc.weighty = 0;
        gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(0, 30, 6, 30);
        brand.add(title, gc);

        gc.gridy = 2;
        gc.insets = new Insets(0, 30, 18, 30);
        brand.add(tagline, gc);

        gc.gridy = 3;
        gc.insets = new Insets(0, 30, 24, 30);
        brand.add(separator, gc);

        gc.gridy = 4;
        gc.weighty = 1.0;
        gc.anchor = GridBagConstraints.SOUTH;
        gc.insets = new Insets(0, 30, 24, 30);
        brand.add(footer, gc);

        return brand;
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_FORM);
        form.setBorder(BorderFactory.createEmptyBorder(0, 50, 30, 50));

        // Barra superior con minimizar / cerrar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JPanel windowButtons = new JPanel();
        windowButtons.setOpaque(false);

        btn_minimize = createWindowIconButton("imagenes/svg/minimize.svg", new Color(0x424242));
        btn_minimize.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { setState(JFrame.ICONIFIED); }
        });
        btn_close = createWindowIconButton("imagenes/svg/close-dark.svg", new Color(0xE53935));
        btn_close.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { System.exit(0); }
        });
        windowButtons.add(btn_minimize);
        windowButtons.add(btn_close);
        topBar.add(windowButtons, BorderLayout.EAST);

        // Titulos
        JLabel hello = new JLabel("Bienvenido");
        hello.setFont(new Font("Segoe UI", Font.BOLD, 26));
        hello.setForeground(TEXT_PRIMARY);

        JLabel sub = new JLabel("Inicia sesion para continuar");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_SECONDARY);

        // Campo usuario
        JLabel lblUser = new JLabel("Usuario");
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblUser.setForeground(TEXT_SECONDARY);

        txt_user = new JTextField();
        styleTextField(txt_user, "Ingresa tu usuario", "imagenes/svg/user.svg");
        txt_user.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    jpassword.requestFocus();
                }
            }
        });

        // Campo password
        JLabel lblPwd = new JLabel("Contrasena");
        lblPwd.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPwd.setForeground(TEXT_SECONDARY);

        jpassword = new JPasswordField();
        jpassword.setEchoChar('\u2022');
        stylePasswordField(jpassword, "Ingresa tu contrasena", "imagenes/svg/lock.svg");
        jpassword.putClientProperty("JPasswordField.showRevealButton", true);
        jpassword.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btn_inicioActionPerformed(null);
                }
            }
        });

        // Boton principal
        btn_inicio = new JButton("INICIAR SESION");
        btn_inicio.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn_inicio.setForeground(Color.WHITE);
        btn_inicio.setBackground(PRIMARY);
        btn_inicio.setFocusPainted(false);
        btn_inicio.setBorderPainted(false);
        btn_inicio.setOpaque(true);
        btn_inicio.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        btn_inicio.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn_inicio.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn_inicio.setBackground(PRIMARY_HOVER); }
            @Override public void mouseExited(MouseEvent e) { btn_inicio.setBackground(PRIMARY); }
        });
        btn_inicio.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { btn_inicioActionPerformed(e); }
        });

        JLabel hint = new JLabel("Si olvidaste tu contrasena contacta al administrador");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(TEXT_SECONDARY);
        hint.setHorizontalAlignment(SwingConstants.CENTER);

        // Layout vertical centrado
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        gc.gridy = 0;
        gc.weighty = 0;
        gc.insets = new Insets(10, 0, 0, 0);
        form.add(topBar, gc);

        gc.gridy = 1;
        gc.weighty = 1.0;
        form.add(spacer(), gc);

        gc.weighty = 0;
        gc.gridy = 2;
        gc.insets = new Insets(0, 0, 4, 0);
        form.add(hello, gc);

        gc.gridy = 3;
        gc.insets = new Insets(0, 0, 28, 0);
        form.add(sub, gc);

        gc.gridy = 4;
        gc.insets = new Insets(0, 0, 6, 0);
        form.add(lblUser, gc);

        gc.gridy = 5;
        gc.insets = new Insets(0, 0, 16, 0);
        form.add(txt_user, gc);

        gc.gridy = 6;
        gc.insets = new Insets(0, 0, 6, 0);
        form.add(lblPwd, gc);

        gc.gridy = 7;
        gc.insets = new Insets(0, 0, 24, 0);
        form.add(jpassword, gc);

        gc.gridy = 8;
        gc.insets = new Insets(0, 0, 18, 0);
        form.add(btn_inicio, gc);

        gc.gridy = 9;
        gc.weighty = 1.0;
        form.add(spacer(), gc);

        gc.gridy = 10;
        gc.weighty = 0;
        form.add(hint, gc);

        return form;
    }

    private JComponent spacer() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    private JButton createWindowIconButton(final String svgPath, final Color hoverColor) {
        final FlatSVGIcon baseIcon = new FlatSVGIcon(svgPath, 16, 16);
        final FlatSVGIcon hoverIcon = new FlatSVGIcon(svgPath, 16, 16)
                .setColorFilter(new FlatSVGIcon.ColorFilter() {
                    @Override public Color filter(Color color) { return hoverColor; }
                });
        final JButton b = new JButton(baseIcon);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setIcon(hoverIcon); }
            @Override public void mouseExited(MouseEvent e) { b.setIcon(baseIcon); }
        });
        return b;
    }

    private void styleTextField(JTextField field, String placeholder, String svgIconPath) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT_PRIMARY);
        field.setPreferredSize(new Dimension(0, 42));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon(svgIconPath, 18, 18));
    }

    private void stylePasswordField(JPasswordField field, String placeholder, String svgIconPath) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT_PRIMARY);
        field.setPreferredSize(new Dimension(0, 42));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon(svgIconPath, 18, 18));
    }

    private void attachWindowDragger(JComponent root) {
        final java.awt.Point[] origin = new java.awt.Point[1];
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { origin[0] = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (origin[0] == null) return;
                java.awt.Point p = e.getLocationOnScreen();
                setLocation(p.x - origin[0].x, p.y - origin[0].y);
            }
            @Override public void mouseReleased(MouseEvent e) { origin[0] = null; }
        };
        root.addMouseListener(ma);
        root.addMouseMotionListener(ma);
    }

    private ImageIcon loadLogo() {
        java.io.File f = new java.io.File("logo.png");
        if (f.isFile()) {
            return new ImageIcon(f.getAbsolutePath());
        }
        return new ImageIcon(getClass().getResource("/imagenes/logo.png"));
    }

    private void applyAutoComplete() {
        try {
            ArrayList<String> lista_users = users.MostrarUserName();
            AutoCompleteDecorator.decorate(txt_user, lista_users, true);
        } catch (Exception ignored) {
        }
    }

    private void btn_inicioActionPerformed(ActionEvent evt) {
        frm_main main = new frm_main();
        if (txt_user.getText().isEmpty()) {
            JOptionPane.showMessageDialog(main, "No ha ingresado un nombre de usuario");
            txt_user.requestFocus();
        } else if (jpassword.getPassword().length == 0) {
            JOptionPane.showMessageDialog(main, "No ha ingresado un password");
            jpassword.requestFocus();
        } else {
            String pwd = new String(jpassword.getPassword());
            if (DB_consultas_R_D.Login(txt_user.getText(), DigestUtils.sha256Hex(pwd)) == 1) {

                id_u = DB_consultas_R_D.TraerIdUser(txt_user.getText());
                main.id_user = id_u;
                main.nombre_usuario = txt_user.getText();

                String cadena = "select u.id_bodega, b.nombre as bodega  from users u, bodegas b where u.id_bodega=b.id and u.id= " + id_u + "";
                ResultSet rs = getTabla(cadena);
                try {
                    while (rs.next()) {
                        main.id_bodega = rs.getInt("id_bodega");
                        main.bodega = rs.getString("bodega");
                        main.lbl_bodega_user.setText(rs.getString("bodega"));
                    }
                    rs.close();
                } catch (Exception e) {
                    System.out.println(e);
                }

                rs = DB_consultas_R_D.getTabla("select * from configuraciones where id = 1");
                try {
                    while (rs.next()) {
                        main.impresora_ticket = rs.getString("impresora_venta_80mm");
                    }
                    rs.close();
                } catch (SQLException ex) {
                    System.out.println(ex);
                }

                ResultSet rsU = getTabla(
                        "SELECT imprime_ordenes, nombre_impresora, imp_ticket_bodega_asignada, barra_notificaciones, panel_notificaciones, aprueba_compras, coalesce(rol_precios,0) as rol_precios FROM users WHERE id = " + id_u);
                try {
                    if (rsU.next()) {
                        frm_main.imprime_ordenes = rsU.getBoolean("imprime_ordenes");
                        frm_main.nombre_impresora_user = rsU.getString("nombre_impresora");
                        frm_main.imp_ticket_bodega_asignada = rsU.getBoolean("imp_ticket_bodega_asignada");
                        frm_main.barra_notificaciones = rsU.getBoolean("barra_notificaciones");
                        frm_main.panel_notificaciones = rsU.getBoolean("panel_notificaciones");
                        frm_main.aprueba_compras = rsU.getBoolean("aprueba_compras");
                        frm_main.rol_precios = rsU.getInt("rol_precios");
                    }
                    rsU.close();
                } catch (SQLException ex) {
                    System.out.println(ex);
                }

                if (frm_main.imprime_ordenes) {
                    frm_main.print_service_user = Metodos.ImprimirTermica80MMOrden
                            .buscarImpresoraPorNombre(frm_main.nombre_impresora_user);
                    if (frm_main.print_service_user == null) {
                        JOptionPane.showMessageDialog(main,
                                "Auto-impresion activa, pero no se encontro la impresora '"
                                + frm_main.nombre_impresora_user
                                + "' en este equipo.\nLas ordenes nuevas no se imprimiran automaticamente.",
                                "Aviso", JOptionPane.WARNING_MESSAGE);
                    } else {
                        Metodos.AutoImpresionOrdenesService.getInstance().iniciar();
                    }
                }

                main.lbl_user.setText(txt_user.getText());
                per = DB_consultas_R_D.TraerIdPerfil(DB_consultas_R_D.TraerIdUser(txt_user.getText()));
                main.lbl_perfil.setText(DB_consultas_R_D.TraerNombrePerfil(per));

                main.perfil = per;

                // Permisos administrables en BD; si la carga falla, permisos()
                // cae al switch por perfil anterior.
                Metodos.Permisos.cargar(id_u, per);

                // Roles del módulo Precios: un usuario puede tener varios.
                frm_main.roles_precios = conexiondb.DBpermisos.rolesPrecios(id_u);
                if (frm_main.roles_precios.isEmpty() && frm_main.rol_precios > 0) {
                    // Transición: BD sin la migración de permisos -> users.rol_precios.
                    frm_main.roles_precios.add(frm_main.rol_precios);
                }
                if (per == 1 && frm_main.roles_precios.isEmpty()) {
                    // Admin sin rol asignado: acceso total al módulo Precios.
                    frm_main.roles_precios.add(4);
                }
                frm_main.rol_precios = frm_main.roles_precios.isEmpty()
                        ? 0 : java.util.Collections.max(frm_main.roles_precios);

                main.permisos();
                main.actualizarMenuPrecios();
                main.montarBarraNotificaciones();
                main.montarPanelNotificaciones();

                String nombrePerfil = DB_consultas_R_D.TraerNombrePerfil(per);
                if (nombrePerfil != null && nombrePerfil.trim().equalsIgnoreCase("bodeguero")) {
                    Formularios_internos.jd_SeleccionModoBodega selector =
                            new Formularios_internos.jd_SeleccionModoBodega(
                                    this, txt_user.getText(), main.bodega);
                    selector.setVisible(true);

                    Formularios_internos.jd_SeleccionModoBodega.Modo modo = selector.getModo();
                    if (modo == Formularios_internos.jd_SeleccionModoBodega.Modo.ENTREGAS_RAPIDAS) {
                        Formularios.frm_EntregasQR vent = new Formularios.frm_EntregasQR(
                                main.id_user, txt_user.getText(),
                                main.id_bodega, main.bodega);
                        vent.setVisible(true);
                        this.dispose();
                        return;
                    } else if (modo == Formularios_internos.jd_SeleccionModoBodega.Modo.CANCELADO) {
                        // Vuelve al login: limpia credenciales y aborta el flujo
                        txt_user.setText("");
                        jpassword.setText("");
                        txt_user.requestFocus();
                        return;
                    }
                    // ACCESO_NORMAL cae al flujo clasico
                }

                main.show();

                this.dispose();
            } else {
                JOptionPane.showMessageDialog(main, "Error en la autenticacion", "Error", 0);
                txt_user.setText("");
                jpassword.setText("");
                txt_user.requestFocus();
            }
        }
    }

    public static void muestraContenido(String archivo) throws FileNotFoundException, IOException {
        String cadena;
        FileReader f = new FileReader(archivo);
        BufferedReader b = new BufferedReader(f);
        while ((cadena = b.readLine()) != null) {
            System.out.println(cadena);
        }
        b.close();
    }

    public static void main(String args[]) {
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("ProgressBar.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.thumbArc", 10);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("Component.focusColor", PRIMARY);
            UIManager.put("Component.focusedBorderColor", PRIMARY);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(login.class.getName())
                    .log(java.util.logging.Level.SEVERE, "No se pudo aplicar FlatLaf", ex);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override public void run() {
                new login().setVisible(true);
            }
        });
    }
}
