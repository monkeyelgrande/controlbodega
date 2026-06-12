package Formularios_internos;

import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import conexiondb.DBpermisos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import conexiondb.DBperfiles;
import modelos.Opcion;
import modelos.Perfiles;

/**
 * Administración de permisos de la aplicación.
 *
 * Pestaña "Perfiles": qué opciones concede cada perfil por defecto
 * (perfil_opciones). Pestaña "Usuarios": excepciones por usuario sobre su
 * perfil (usuario_opciones) — casillas en azul difieren de lo que su perfil
 * concede. El perfil Admin siempre tiene acceso total y no se edita.
 *
 * Los cambios aplican en el próximo inicio de sesión de cada usuario.
 *
 * @author Monkeyelgrande
 */
public class jif_permisos extends JDialog {

    private static final int WINDOW_W = 860;
    private static final int WINDOW_H = 720;

    private final List<Opcion> opciones;

    // --- pestaña Perfiles ---
    private JComboBox<Perfiles> jbox_perfil;
    private final Map<Integer, JCheckBox> chkPerfil = new LinkedHashMap<>();
    private JButton btn_guardar_perfil;
    private JLabel lbl_hint_perfil;

    // --- pestaña Crear perfiles ---
    private JTable jtabla_perfiles;
    private DefaultTableModel modelo_perfiles;
    private JTextField txt_nombre_perfil;
    private JCheckBox chk_copiar_permisos;
    private JComboBox<Perfiles> jbox_copiar_de;

    // --- pestaña Usuarios ---
    private JComboBox<UsuarioItem> jbox_usuario;
    private final Map<Integer, JCheckBox> chkUsuario = new LinkedHashMap<>();
    private JButton btn_guardar_usuario;
    private JLabel lbl_hint_usuario;
    // opciones que concede el perfil del usuario seleccionado (para marcar diferencias)
    private Set<Integer> baseUsuario = new HashSet<>();

    /** Usuario para el combo de la pestaña Usuarios. */
    private static class UsuarioItem {

        final int id;
        final String nombre;
        final int idPerfil;
        final String perfil;

        UsuarioItem(int id, String nombre, int idPerfil, String perfil) {
            this.id = id;
            this.nombre = nombre;
            this.idPerfil = idPerfil;
            this.perfil = perfil;
        }

        @Override
        public String toString() {
            return nombre + "  (" + (perfil == null ? "sin perfil" : perfil) + ")";
        }
    }

    public jif_permisos() {
        setModal(true);
        setUndecorated(true);
        setSize(WINDOW_W, WINDOW_H);
        setResizable(false);

        opciones = DBpermisos.listarOpciones();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));

        root.add(EstiloCompras.header(FontAwesome.LIST, "Permisos de la aplicación",
                new Runnable() {
            @Override
            public void run() {
                dispose();
            }
        }), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(EstiloCompras.BG_FORM);
        tabs.addTab("Crear perfiles", buildTabGestionPerfiles());
        tabs.addTab("Por perfil", buildTabPerfiles());
        tabs.addTab("Por usuario", buildTabUsuarios());
        root.add(tabs, BorderLayout.CENTER);

        setContentPane(root);
        metodos.addEscapeListenerWindowDialog(this);
        setLocationRelativeTo(null);

        cargarPerfilSeleccionado();
        cargarUsuarioSeleccionado();
    }

    // ------------------------------------------------------------------
    // Pestaña Crear perfiles
    // ------------------------------------------------------------------
    private JPanel buildTabGestionPerfiles() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(EstiloCompras.BG_FORM);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

        // --- formulario superior ---
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        txt_nombre_perfil = EstiloCompras.field("Nombre del perfil", FontAwesome.PENCIL);
        txt_nombre_perfil.setMaximumSize(new Dimension(380, 38));
        txt_nombre_perfil.setPreferredSize(new Dimension(380, 38));

        chk_copiar_permisos = new JCheckBox("Copiar permisos de:");
        chk_copiar_permisos.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chk_copiar_permisos.setBackground(EstiloCompras.BG_FORM);
        chk_copiar_permisos.setFocusPainted(false);

        jbox_copiar_de = new JComboBox<>();
        EstiloCompras.styleCombo(jbox_copiar_de);
        jbox_copiar_de.setMaximumSize(new Dimension(240, 38));
        jbox_copiar_de.setPreferredSize(new Dimension(240, 38));
        jbox_copiar_de.setEnabled(false);
        new Perfiles().mostrar_perfiles(jbox_copiar_de);
        chk_copiar_permisos.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                jbox_copiar_de.setEnabled(chk_copiar_permisos.isSelected());
            }
        });

        JButton btnCrear = EstiloCompras.primaryBtn("Crear perfil", FontAwesome.PLUS);
        btnCrear.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                crearPerfil();
            }
        });

        JButton btnRenombrar = EstiloCompras.secondaryBtn("Renombrar seleccionado", FontAwesome.EDIT);
        btnRenombrar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                renombrarPerfil();
            }
        });

        JPanel fila = new JPanel();
        fila.setOpaque(false);
        fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.add(EstiloCompras.labeled("Nombre", txt_nombre_perfil, 380));
        fila.add(Box.createHorizontalStrut(14));
        JPanel botonesWrap = new JPanel();
        botonesWrap.setOpaque(false);
        botonesWrap.setLayout(new BoxLayout(botonesWrap, BoxLayout.Y_AXIS));
        botonesWrap.add(Box.createVerticalStrut(20));
        JPanel botones = new JPanel();
        botones.setOpaque(false);
        botones.setLayout(new BoxLayout(botones, BoxLayout.X_AXIS));
        botones.add(btnCrear);
        botones.add(Box.createHorizontalStrut(8));
        botones.add(btnRenombrar);
        botonesWrap.add(botones);
        fila.add(botonesWrap);
        fila.add(Box.createHorizontalGlue());
        form.add(fila);

        JPanel filaCopia = new JPanel();
        filaCopia.setOpaque(false);
        filaCopia.setLayout(new BoxLayout(filaCopia, BoxLayout.X_AXIS));
        filaCopia.setAlignmentX(Component.LEFT_ALIGNMENT);
        filaCopia.add(chk_copiar_permisos);
        filaCopia.add(Box.createHorizontalStrut(8));
        filaCopia.add(jbox_copiar_de);
        filaCopia.add(Box.createHorizontalGlue());
        form.add(filaCopia);

        JLabel hint = hintLabel(
                "El perfil nuevo inicia sin permisos (o con los copiados); asígnelos en la pestaña \"Por perfil\".");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(Box.createVerticalStrut(6));
        form.add(hint);
        form.add(Box.createVerticalStrut(10));

        tab.add(form, BorderLayout.NORTH);

        // --- tabla de perfiles existentes ---
        modelo_perfiles = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        modelo_perfiles.setColumnIdentifiers(new Object[]{"ID", "Perfil", "Usuarios"});
        jtabla_perfiles = new JTable(modelo_perfiles);
        EstiloCompras.styleTable(jtabla_perfiles);
        cargarTablaPerfiles();
        EstiloCompras.anchoColumnas(jtabla_perfiles, 60, 420, 100);
        // Al seleccionar, lleva el nombre al campo para renombrar
        jtabla_perfiles.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
            @Override
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                int fila = jtabla_perfiles.getSelectedRow();
                if (!e.getValueIsAdjusting() && fila >= 0) {
                    txt_nombre_perfil.setText(jtabla_perfiles.getValueAt(fila, 1).toString());
                }
            }
        });
        tab.add(EstiloCompras.scroll(jtabla_perfiles), BorderLayout.CENTER);

        JButton btnCerrar = EstiloCompras.secondaryBtn("Cerrar", FontAwesome.CLOSE);
        btnCerrar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        JPanel der = new JPanel();
        der.setOpaque(false);
        der.setLayout(new BoxLayout(der, BoxLayout.X_AXIS));
        der.add(btnCerrar);
        pie.add(der, BorderLayout.EAST);
        tab.add(pie, BorderLayout.SOUTH);

        return tab;
    }

    private void cargarTablaPerfiles() {
        modelo_perfiles.setRowCount(0);
        ResultSet rs = DB_consultas_R_D.getTabla(
                "select p.id, p.perfil, count(u.id) as usuarios "
                + "from perfiles p left join users u on u.id_perfil = p.id "
                + "group by p.id, p.perfil order by p.id");
        try {
            while (rs.next()) {
                modelo_perfiles.addRow(new Object[]{rs.getString("id"),
                    rs.getString("perfil"), rs.getString("usuarios")});
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    private void crearPerfil() {
        String nombre = txt_nombre_perfil.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escriba el nombre del perfil.",
                    "Permisos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        for (int i = 0; i < modelo_perfiles.getRowCount(); i++) {
            if (nombre.equalsIgnoreCase(modelo_perfiles.getValueAt(i, 1).toString())) {
                JOptionPane.showMessageDialog(this, "Ya existe un perfil con ese nombre.",
                        "Permisos", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        Perfiles nuevo = new Perfiles();
        nuevo.setId(Integer.parseInt(DB_consultas_R_D.cargarId("perfiles")));
        nuevo.setPerfil(nombre);
        if (new DBperfiles().Guardar(nuevo) <= 0) {
            return;
        }

        if (chk_copiar_permisos.isSelected() && jbox_copiar_de.getSelectedItem() != null) {
            Perfiles origen = (Perfiles) jbox_copiar_de.getSelectedItem();
            // El Admin no tiene filas en perfil_opciones (todo va en código):
            // copiar de él significa conceder todas las opciones.
            Set<Integer> ids = origen.getId() == 1
                    ? new HashSet<Integer>() : DBpermisos.opcionesPerfil(origen.getId());
            if (origen.getId() == 1) {
                for (Opcion o : opciones) {
                    ids.add(o.getId());
                }
            }
            DBpermisos.guardarOpcionesPerfil(nuevo.getId(), ids);
        }

        txt_nombre_perfil.setText("");
        chk_copiar_permisos.setSelected(false);
        jbox_copiar_de.setEnabled(false);
        refrescarPerfiles();
        JOptionPane.showMessageDialog(this,
                "Perfil \"" + nombre + "\" creado.\nAsigne sus permisos en la pestaña \"Por perfil\".",
                "Permisos", JOptionPane.INFORMATION_MESSAGE);
    }

    private void renombrarPerfil() {
        int fila = jtabla_perfiles.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un perfil en la tabla.",
                    "Permisos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String nombre = txt_nombre_perfil.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escriba el nuevo nombre del perfil.",
                    "Permisos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = Integer.parseInt(jtabla_perfiles.getValueAt(fila, 0).toString());
        if (id == 1) {
            JOptionPane.showMessageDialog(this, "El perfil Admin no se renombra.",
                    "Permisos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Perfiles p = new Perfiles(id, nombre);
        if (new DBperfiles().Actualizar(p) > 0) {
            txt_nombre_perfil.setText("");
            refrescarPerfiles();
        }
    }

    /** Recarga la tabla y los combos que muestran perfiles en las tres pestañas. */
    private void refrescarPerfiles() {
        cargarTablaPerfiles();
        new Perfiles().mostrar_perfiles(jbox_perfil);
        new Perfiles().mostrar_perfiles(jbox_copiar_de);
        jbox_usuario.removeAllItems();
        cargarUsuarios();
    }

    // ------------------------------------------------------------------
    // Pestaña Perfiles
    // ------------------------------------------------------------------
    private JPanel buildTabPerfiles() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(EstiloCompras.BG_FORM);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

        jbox_perfil = new JComboBox<>();
        EstiloCompras.styleCombo(jbox_perfil);
        new Perfiles().mostrar_perfiles(jbox_perfil);
        jbox_perfil.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cargarPerfilSeleccionado();
            }
        });

        lbl_hint_perfil = hintLabel("Lo marcado es lo que el perfil concede por defecto a sus usuarios.");

        tab.add(buildBarraSuperior("Perfil", jbox_perfil, lbl_hint_perfil, chkPerfil),
                BorderLayout.NORTH);
        tab.add(buildPanelOpciones(chkPerfil, null), BorderLayout.CENTER);

        btn_guardar_perfil = EstiloCompras.primaryBtn("Guardar perfil", FontAwesome.SAVE);
        btn_guardar_perfil.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                guardarPerfil();
            }
        });
        tab.add(buildPie(btn_guardar_perfil), BorderLayout.SOUTH);
        return tab;
    }

    private void cargarPerfilSeleccionado() {
        Perfiles p = (Perfiles) jbox_perfil.getSelectedItem();
        if (p == null) {
            return;
        }
        boolean esAdmin = p.getId() == 1;
        Set<Integer> concedidas = esAdmin ? null : DBpermisos.opcionesPerfil(p.getId());
        for (Map.Entry<Integer, JCheckBox> e : chkPerfil.entrySet()) {
            JCheckBox cb = e.getValue();
            cb.setSelected(esAdmin || concedidas.contains(e.getKey()));
            cb.setEnabled(!esAdmin);
            cb.setForeground(EstiloCompras.TEXT_PRIMARY);
        }
        btn_guardar_perfil.setEnabled(!esAdmin);
        lbl_hint_perfil.setText(esAdmin
                ? "El perfil Admin siempre tiene acceso total; no se edita."
                : "Lo marcado es lo que el perfil concede por defecto a sus usuarios.");
    }

    private void guardarPerfil() {
        Perfiles p = (Perfiles) jbox_perfil.getSelectedItem();
        if (p == null || p.getId() == 1) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (Map.Entry<Integer, JCheckBox> e : chkPerfil.entrySet()) {
            if (e.getValue().isSelected()) {
                ids.add(e.getKey());
            }
        }
        if (DBpermisos.guardarOpcionesPerfil(p.getId(), ids)) {
            JOptionPane.showMessageDialog(this,
                    "Permisos del perfil guardados.\nAplican en el próximo inicio de sesión de cada usuario.",
                    "Permisos", JOptionPane.INFORMATION_MESSAGE);
            // refresca la pestaña de usuarios: la base de comparación cambió
            cargarUsuarioSeleccionado();
        }
    }

    // ------------------------------------------------------------------
    // Pestaña Usuarios
    // ------------------------------------------------------------------
    private JPanel buildTabUsuarios() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(EstiloCompras.BG_FORM);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

        jbox_usuario = new JComboBox<>();
        EstiloCompras.styleCombo(jbox_usuario);
        cargarUsuarios();
        jbox_usuario.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cargarUsuarioSeleccionado();
            }
        });

        lbl_hint_usuario = hintLabel("Las casillas en azul difieren de lo que su perfil concede.");

        tab.add(buildBarraSuperior("Usuario", jbox_usuario, lbl_hint_usuario, chkUsuario),
                BorderLayout.NORTH);
        tab.add(buildPanelOpciones(chkUsuario, new Runnable() {
            @Override
            public void run() {
                pintarDiferenciasUsuario();
            }
        }), BorderLayout.CENTER);

        btn_guardar_usuario = EstiloCompras.primaryBtn("Guardar usuario", FontAwesome.SAVE);
        btn_guardar_usuario.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                guardarUsuario();
            }
        });
        tab.add(buildPie(btn_guardar_usuario), BorderLayout.SOUTH);
        return tab;
    }

    private void cargarUsuarios() {
        ResultSet rs = DB_consultas_R_D.getTabla(
                "select u.id, u.nombre, u.id_perfil, p.perfil "
                + "from users u left join perfiles p on p.id = u.id_perfil "
                + "order by u.nombre");
        try {
            while (rs.next()) {
                jbox_usuario.addItem(new UsuarioItem(rs.getInt("id"), rs.getString("nombre"),
                        rs.getInt("id_perfil"), rs.getString("perfil")));
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    private void cargarUsuarioSeleccionado() {
        UsuarioItem u = (UsuarioItem) jbox_usuario.getSelectedItem();
        if (u == null) {
            return;
        }
        boolean esAdmin = u.idPerfil == 1;
        baseUsuario = esAdmin ? new HashSet<Integer>() : DBpermisos.opcionesPerfil(u.idPerfil);
        Map<Integer, Boolean> excepciones = esAdmin
                ? new HashMap<Integer, Boolean>() : DBpermisos.excepcionesUsuario(u.id);

        for (Map.Entry<Integer, JCheckBox> e : chkUsuario.entrySet()) {
            JCheckBox cb = e.getValue();
            boolean base = baseUsuario.contains(e.getKey());
            Boolean exc = excepciones.get(e.getKey());
            cb.setSelected(esAdmin || (exc != null ? exc : base));
            cb.setEnabled(!esAdmin);
        }
        btn_guardar_usuario.setEnabled(!esAdmin);
        lbl_hint_usuario.setText(esAdmin
                ? "Los usuarios con perfil Admin siempre tienen acceso total; no se editan."
                : "Las casillas en azul difieren de lo que su perfil concede.");
        pintarDiferenciasUsuario();
    }

    /** Marca en azul las casillas cuyo estado difiere del perfil del usuario. */
    private void pintarDiferenciasUsuario() {
        UsuarioItem u = (UsuarioItem) jbox_usuario.getSelectedItem();
        boolean esAdmin = u != null && u.idPerfil == 1;
        for (Map.Entry<Integer, JCheckBox> e : chkUsuario.entrySet()) {
            JCheckBox cb = e.getValue();
            boolean difiere = !esAdmin && cb.isSelected() != baseUsuario.contains(e.getKey());
            cb.setForeground(difiere ? EstiloCompras.PRIMARY : EstiloCompras.TEXT_PRIMARY);
            cb.setFont(cb.getFont().deriveFont(difiere ? Font.BOLD : Font.PLAIN));
        }
    }

    private void guardarUsuario() {
        UsuarioItem u = (UsuarioItem) jbox_usuario.getSelectedItem();
        if (u == null || u.idPerfil == 1) {
            return;
        }
        // Solo se guardan las diferencias contra el perfil: así, si después se
        // cambia el perfil, el usuario hereda lo nuevo salvo sus excepciones.
        Map<Integer, Boolean> excepciones = new HashMap<>();
        for (Map.Entry<Integer, JCheckBox> e : chkUsuario.entrySet()) {
            boolean marcado = e.getValue().isSelected();
            if (marcado != baseUsuario.contains(e.getKey())) {
                excepciones.put(e.getKey(), marcado);
            }
        }
        if (DBpermisos.guardarExcepcionesUsuario(u.id, excepciones)) {
            JOptionPane.showMessageDialog(this,
                    "Permisos del usuario guardados.\nAplican en su próximo inicio de sesión.",
                    "Permisos", JOptionPane.INFORMATION_MESSAGE);
            pintarDiferenciasUsuario();
        }
    }

    // ------------------------------------------------------------------
    // Construcción compartida
    // ------------------------------------------------------------------
    private JPanel buildBarraSuperior(String etiqueta, JComboBox<?> combo, JLabel hint,
            final Map<Integer, JCheckBox> chks) {
        JPanel barra = new JPanel();
        barra.setOpaque(false);
        barra.setLayout(new BoxLayout(barra, BoxLayout.Y_AXIS));

        JPanel fila = new JPanel();
        fila.setOpaque(false);
        fila.setLayout(new BoxLayout(fila, BoxLayout.X_AXIS));
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);

        combo.setMaximumSize(new Dimension(380, 38));
        combo.setPreferredSize(new Dimension(380, 38));
        fila.add(EstiloCompras.labeled(etiqueta, combo, 380));
        fila.add(Box.createHorizontalStrut(14));

        JButton btnTodas = EstiloCompras.secondaryBtn("Todas", FontAwesome.CHECK);
        btnTodas.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                marcarTodas(chks, true);
            }
        });
        JButton btnNinguna = EstiloCompras.secondaryBtn("Ninguna", FontAwesome.CLOSE);
        btnNinguna.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                marcarTodas(chks, false);
            }
        });

        JPanel botones = new JPanel();
        botones.setOpaque(false);
        botones.setLayout(new BoxLayout(botones, BoxLayout.X_AXIS));
        botones.add(btnTodas);
        botones.add(Box.createHorizontalStrut(8));
        botones.add(btnNinguna);
        JPanel botonesWrap = new JPanel();
        botonesWrap.setOpaque(false);
        botonesWrap.setLayout(new BoxLayout(botonesWrap, BoxLayout.Y_AXIS));
        botonesWrap.add(Box.createVerticalStrut(20));
        botonesWrap.add(botones);
        fila.add(botonesWrap);
        fila.add(Box.createHorizontalGlue());

        barra.add(fila);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.add(hint);
        barra.add(Box.createVerticalStrut(10));
        return barra;
    }

    private void marcarTodas(Map<Integer, JCheckBox> chks, boolean valor) {
        for (JCheckBox cb : chks.values()) {
            if (cb.isEnabled()) {
                cb.setSelected(valor);
            }
        }
        if (chks == chkUsuario) {
            pintarDiferenciasUsuario();
        }
    }

    /**
     * Panel con las opciones agrupadas por módulo. Llena el mapa id_opcion →
     * checkbox; alCambiar (opcional) se invoca con cada clic.
     */
    private JScrollPane buildPanelOpciones(Map<Integer, JCheckBox> chks, final Runnable alCambiar) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(EstiloCompras.BG_FORM);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 12, 4));

        String moduloActual = null;
        for (Opcion o : opciones) {
            if (!o.getModulo().equals(moduloActual)) {
                moduloActual = o.getModulo();
                if (panel.getComponentCount() > 0) {
                    panel.add(Box.createVerticalStrut(14));
                }
                JLabel titulo = EstiloCompras.sectionTitle(moduloActual);
                titulo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
                panel.add(titulo);
                panel.add(Box.createVerticalStrut(6));
            }
            JCheckBox cb = new JCheckBox(o.getNombre());
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cb.setBackground(EstiloCompras.BG_FORM);
            cb.setFocusPainted(false);
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (alCambiar != null) {
                cb.addItemListener(new java.awt.event.ItemListener() {
                    @Override
                    public void itemStateChanged(java.awt.event.ItemEvent e) {
                        alCambiar.run();
                    }
                });
            }
            chks.put(o.getId(), cb);
            panel.add(cb);
        }

        JScrollPane scroll = new JScrollPane(panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));
        scroll.getViewport().setBackground(EstiloCompras.BG_FORM);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel buildPie(JButton guardar) {
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        JButton cerrar = EstiloCompras.secondaryBtn("Cerrar", FontAwesome.CLOSE);
        cerrar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
        JPanel der = new JPanel();
        der.setOpaque(false);
        der.setLayout(new BoxLayout(der, BoxLayout.X_AXIS));
        der.add(cerrar);
        der.add(Box.createHorizontalStrut(8));
        der.add(guardar);
        pie.add(der, BorderLayout.EAST);
        return pie;
    }

    private JLabel hintLabel(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(EstiloCompras.TEXT_SECONDARY);
        return l;
    }
}
