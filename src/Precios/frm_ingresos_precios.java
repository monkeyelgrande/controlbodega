package Precios;

import Formularios.frm_main;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * Listado de ingresos del modulo Precios (portado del frm_ingreso_productos de
 * productos-agroinsumos). Trabaja sobre ingresos_productos_cabecera/detalle.
 *
 * @author Monkeyelgrande
 */
public class frm_ingresos_precios extends javax.swing.JInternalFrame {

    TableColumnModel columnModel = null;
    CellRendererIngresosPrecios myRenderer = new CellRendererIngresosPrecios();

    public frm_ingresos_precios() {
        initComponents();
        actualizar();
        columnModel = jtabla.getColumnModel();
        TamanosTabla();
        jtabla.setDefaultRenderer(Object.class, myRenderer);

        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    btn_verActionPerformed(null);
                }
            }
        });

        // Al seleccionar una fila (un clic) se muestra su historial de cambios a
        // la derecha; el doble clic sigue abriendo el ingreso como antes.
        jtabla.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    cargarHistorial();
                }
            }
        });

        // Unión de capacidades de los roles del usuario: crear lo permiten
        // Almacenista (2) y Precios (4); eliminar, Contable (3) y Precios (4).
        // Con un solo rol equivale al switch anterior.
        java.util.List<Integer> roles = rolesUsuario();
        btn_crear.setEnabled(roles.contains(SelectorRolPrecios.ALMACENISTA)
                || roles.contains(SelectorRolPrecios.PRECIOS));
        btn_eliminar.setEnabled(roles.contains(SelectorRolPrecios.CONTABLE)
                || roles.contains(SelectorRolPrecios.PRECIOS));
        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        txt_Filtro.requestFocus();
    }

    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public void TamanosTabla() {
        // Se toma fresco: setModel() reemplaza el column model de la tabla
        columnModel = jtabla.getColumnModel();
        if (columnModel.getColumnCount() < 4) {
            return;
        }
        columnModel.getColumn(0).setPreferredWidth(20);
        columnModel.getColumn(1).setPreferredWidth(300);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(3).setPreferredWidth(100);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        btn_crear = new javax.swing.JButton();
        btn_eliminar = new javax.swing.JButton();
        btn_actualizar = new javax.swing.JButton();
        btn_ver = new javax.swing.JButton();
        btn_editar = new javax.swing.JButton();
        btn_pagos = new javax.swing.JButton();
        btn_world_office = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        btn_cerrar = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        txt_Filtro = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        jtabla = new javax.swing.JTable();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Ingresos de productos (Precios)");

        jPanel3.setBackground(new java.awt.Color(33, 33, 33));

        btn_crear.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_crear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/nuevo.png"))); // NOI18N
        btn_crear.setMnemonic('n');
        btn_crear.setText("Nuevo");
        btn_crear.setBorder(null);
        btn_crear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_crearActionPerformed(evt);
            }
        });

        btn_eliminar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_eliminar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/eliminar.png"))); // NOI18N
        btn_eliminar.setMnemonic('d');
        btn_eliminar.setText("Eliminar");
        btn_eliminar.setBorder(null);
        btn_eliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_eliminarActionPerformed(evt);
            }
        });

        btn_actualizar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_actualizar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/actualizar.png"))); // NOI18N
        btn_actualizar.setText("Actualizar");
        btn_actualizar.setBorder(null);
        btn_actualizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_actualizarActionPerformed(evt);
            }
        });

        btn_ver.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_ver.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/ver.png"))); // NOI18N
        btn_ver.setMnemonic('e');
        btn_ver.setText("Ver");
        btn_ver.setBorder(null);
        btn_ver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_verActionPerformed(evt);
            }
        });

        btn_editar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_editar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/editar.png"))); // NOI18N
        btn_editar.setMnemonic('e');
        btn_editar.setText("Editar");
        btn_editar.setBorder(null);
        btn_editar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_editarActionPerformed(evt);
            }
        });

        btn_pagos.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_pagos.setText("Pagos");
        btn_pagos.setBorder(null);
        btn_pagos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_pagosActionPerformed(evt);
            }
        });

        btn_world_office.setBackground(new java.awt.Color(78, 205, 196));
        btn_world_office.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        btn_world_office.setText("World Office");
        btn_world_office.setBorder(null);
        btn_world_office.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_world_officeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_actualizar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_crear, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_eliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_ver, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_pagos, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_world_office, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(btn_editar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_crear)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_eliminar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_actualizar, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_editar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_ver)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_pagos, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_world_office, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(46, 125, 50));
        jPanel4.setPreferredSize(new java.awt.Dimension(146, 80));

        jLabel13.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("Ingresos de productos (Precios)");

        btn_cerrar.setBackground(new java.awt.Color(102, 0, 0));
        btn_cerrar.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        btn_cerrar.setForeground(new java.awt.Color(255, 255, 255));
        btn_cerrar.setMnemonic('w');
        btn_cerrar.setText("Cerrar");
        btn_cerrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_cerrarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 858, Short.MAX_VALUE)
                .addComponent(btn_cerrar)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_cerrar)
                    .addComponent(jLabel13))
                .addGap(26, 26, 26))
        );

        jPanel1.setBackground(new java.awt.Color(51, 51, 51));

        txt_Filtro.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N

        jtabla.setFont(new java.awt.Font("Yu Gothic Medium", 0, 18)); // NOI18N
        jtabla.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][]{},
            new String[]{}
        ));
        jtabla.setRowHeight(50);
        jtabla.setSelectionBackground(new java.awt.Color(0, 153, 153));
        jtabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(jtabla);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, 427, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txt_Filtro, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 592, Short.MAX_VALUE)
                .addContainerGap())
        );

        // ---- Panel de historial (auditoria) ----------------------------------
        jPanelHistorial = new javax.swing.JPanel();
        lblHistorialTitulo = new javax.swing.JLabel();
        jScrollHistorial = new javax.swing.JScrollPane();
        jTablaHistorial = new javax.swing.JTable();

        jPanelHistorial.setBackground(new java.awt.Color(51, 51, 51));
        jPanelHistorial.setPreferredSize(new java.awt.Dimension(360, 0));
        jPanelHistorial.setLayout(new java.awt.BorderLayout(0, 6));

        lblHistorialTitulo.setFont(new java.awt.Font("Tahoma", 1, 16)); // NOI18N
        lblHistorialTitulo.setForeground(new java.awt.Color(255, 255, 255));
        lblHistorialTitulo.setText("Historial");
        lblHistorialTitulo.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 4, 10));
        jPanelHistorial.add(lblHistorialTitulo, java.awt.BorderLayout.NORTH);

        jTablaHistorial.setFont(new java.awt.Font("Yu Gothic Medium", 0, 13)); // NOI18N
        jTablaHistorial.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][]{},
            new String[]{"Fecha / Hora", "Usuario", "Cambio"}
        ));
        jTablaHistorial.setRowHeight(30);
        jTablaHistorial.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTablaHistorial.getTableHeader().setReorderingAllowed(false);
        jScrollHistorial.setViewportView(jTablaHistorial);
        jPanelHistorial.add(jScrollHistorial, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 1188, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelHistorial, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanelHistorial, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );

        pack();
    }

    /** Roles de Precios del usuario; si no hay lista (transición), el rol único actual. */
    private static java.util.List<Integer> rolesUsuario() {
        java.util.List<Integer> roles = new java.util.ArrayList<>(frm_main.roles_precios);
        if (roles.isEmpty() && frm_main.rol_precios > 0) {
            roles.add(frm_main.rol_precios);
        }
        return roles;
    }

    /**
     * Estados que cada rol puede editar (mismas reglas que las negaciones
     * anteriores): Almacenista solo Recibido; Contable Recibido/Ingresado;
     * Precios Ingresado/Precios.
     */
    private static boolean rolPuedeEditar(int rol, String estado) {
        switch (rol) {
            case SelectorRolPrecios.ALMACENISTA:
                return estado.equals("Recibido");
            case SelectorRolPrecios.CONTABLE:
                return estado.equals("Recibido") || estado.equals("Ingresado");
            case SelectorRolPrecios.PRECIOS:
                return estado.equals("Ingresado") || estado.equals("Precios");
            default:
                return false;
        }
    }

    private void btn_crearActionPerformed(java.awt.event.ActionEvent evt) {
        // Crear lo permiten Almacenista (2) y Precios (4). Si el usuario tiene
        // ambos, se le pregunta con cuál rol quiere trabajar.
        java.util.List<Integer> candidatos = new java.util.ArrayList<>();
        for (int rol : rolesUsuario()) {
            if (rol == SelectorRolPrecios.ALMACENISTA || rol == SelectorRolPrecios.PRECIOS) {
                candidatos.add(rol);
            }
        }
        int rolElegido = SelectorRolPrecios.elegir(candidatos,
                "¿Con cuál rol desea crear el ingreso?");
        if (rolElegido == -1) {
            return;
        }
        frm_main.rol_precios = rolElegido;

        jif_crear_ingreso_precios jif_crear_ingreso = new jif_crear_ingreso_precios();
        jif_crear_ingreso_precios.es_nuevo = true;
        jif_crear_ingreso_precios.btn_guardar.setText("Guardar");
        jif_crear_ingreso_precios.btn_calcular_costos.setVisible(false);
        jif_crear_ingreso_precios.btn_calcular_precios_venta.setVisible(false);
        jif_crear_ingreso_precios.btn_precargar.setVisible(false);
        jif_crear_ingreso_precios.btn_Exportar_Worold.setVisible(false);
        jif_crear_ingreso.show();
    }

    private void btn_eliminarActionPerformed(java.awt.event.ActionEvent evt) {

        if (DB_consultas_R_D.validar_admin()) {
            int fila = jtabla.getSelectedRow();
            if (fila == -1) {
                JOptionPane.showMessageDialog(null, "Seleccione un registro");
            } else {
                int dialogButton = JOptionPane.YES_NO_OPTION;
                int dialogResult = JOptionPane.showConfirmDialog(null, "¿Desea eliminar este ingreso de productos?\n"
                        + "Se eliminarán también su detalle y sus pagos.\n"
                        + "(Este módulo no afecta el stock de bodega.)", "Alerta", dialogButton);
                if (dialogResult == JOptionPane.YES_OPTION) {

                    try {
                        DefaultTableModel modelo = (DefaultTableModel) jtabla.getModel();
                        String id = (String) jtabla.getValueAt(fila, 0);
                        if (DB_consultas_R_D.eliminar("ingresos_productos_cabecera", id)) {
                            for (int i = 0; i < modelo.getRowCount(); i++) {
                                if (modelo.getValueAt(i, 0).equals(id)) {
                                    modelo.removeRow(i);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void btn_actualizarActionPerformed(java.awt.event.ActionEvent evt) {
        actualizar();
    }

    private void btn_verActionPerformed(java.awt.event.ActionEvent evt) {
        // Para ver no se pregunta: se usa el rol que corresponde al estado del
        // registro (su vista es la más completa) y, si no se tiene, el mayor.
        int fila = jtabla.getSelectedRow();
        java.util.List<Integer> roles = rolesUsuario();
        if (fila >= 0 && roles.size() > 1) {
            String estado = jtabla.getValueAt(fila, 4).toString();
            int preferido = estado.equals("Recibido") ? SelectorRolPrecios.ALMACENISTA
                    : estado.equals("Ingresado") ? SelectorRolPrecios.CONTABLE
                    : SelectorRolPrecios.PRECIOS;
            frm_main.rol_precios = roles.contains(preferido)
                    ? preferido : java.util.Collections.max(roles);
        }
        ver_ingreso("ver");
    }

    private void btn_pagosActionPerformed(java.awt.event.ActionEvent evt) {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
            return;
        }
        String id = (String) jtabla.getValueAt(fila, 0);
        jd_pagos_ingreso_precios dialogo = new jd_pagos_ingreso_precios(null, true, Integer.parseInt(id));
        dialogo.setVisible(true);
    }

    /** Lista los ingresos en estado Precios para exportarlos a World Office. */
    private void btn_world_officeActionPerformed(java.awt.event.ActionEvent evt) {
        jd_ver_ingresos_world_office jd = new jd_ver_ingresos_world_office(null, false);
        jd.LimpiarModelos();

        String consulta = "select i.id, p.nombre as proveedor, i.fecha, i.fecha_vencimiento, i.no_factura, i.estado "
                + "from ingresos_productos_cabecera i, contactos p "
                + "where i.id_proveedor=p.id  and i.estado=2 order by fecha desc,  i.id desc";

        jd_ver_ingresos_world_office.modelo_ingresos.setColumnIdentifiers(new Object[]{"id", "proveedor", "Numero factura", "Fecha", "Estado"});

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                String estado = "";
                switch (rs.getInt("estado")) {
                    case 0:
                        estado = "Recibido";
                        break;
                    case 1:
                        estado = "Ingresado";
                        break;
                    case 2:
                        estado = "Precios";
                        break;
                }

                jd_ver_ingresos_world_office.modelo_ingresos.addRow(new Object[]{rs.getString("id"), rs.getString("proveedor"), rs.getString("no_factura"), rs.getDate("fecha"), estado});
            }
            rs.close();
            jd_ver_ingresos_world_office.jtabla.setModel(jd_ver_ingresos_world_office.modelo_ingresos);
            jd.TamanosTabla();
        } catch (Exception e) {
            System.out.println(e);
        }

        jd.show();
    }

    public void ver_ingreso(String ver_editar) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator('.');
        DecimalFormat formateador = new DecimalFormat("####.##", simbolos);
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
        } else {
            String id = (String) jtabla.getValueAt(fila, 0);

            ResultSet rs = DB_consultas_R_D.getTabla("select i.id, i.*, tra.nombre as transportador, "
                    + "p.nombre as proveedor,p.id as id_proveedor, i.fecha, i.estado "
                    + "from ingresos_productos_cabecera i, contactos p, contactos tra "
                    + "where i.id_proveedor=p.id and i.id_transportador=tra.id and i.id=" + id);

            jif_crear_ingreso_precios frm = new jif_crear_ingreso_precios();
            jif_crear_ingreso_precios.es_nuevo = false;
            // Fija el id real de inmediato: si la consulta de arriba no devuelve
            // filas (joins sin coincidencia), evitaria que el guardado cree otro
            // registro con el id nuevo del constructor.
            jif_crear_ingreso_precios.lbl_id.setText(id);

            try {
                while (rs.next()) {
                    jif_crear_ingreso_precios.lbl_id.setText(rs.getString("id"));
                    jif_crear_ingreso_precios.jdate_fecha_entrada.setDate(rs.getDate("fecha"));
                    jif_crear_ingreso_precios.jbox_proveedor.setSelectedItem(rs.getString("proveedor"));
                    jif_crear_ingreso_precios.jbox_transportador.setSelectedItem(rs.getString("transportador"));
                    jif_crear_ingreso_precios.id_proveedor = (rs.getInt("id_proveedor"));
                    jif_crear_ingreso_precios.id_transportador = (rs.getInt("id_transportador"));
                    jif_crear_ingreso_precios.txt_no_factura.setText(rs.getString("no_factura"));
                    jif_crear_ingreso_precios.txt_observacion.setText(rs.getString("observacion"));
                }

                rs.close();
            } catch (SQLException ex) {
                Logger.getLogger(jif_crear_ingreso_precios.class.getName()).log(Level.SEVERE, null, ex);
            }
            String consulta = "select p.id,p.codigo_barras,p.descripcion,i.*, c.porcentaje_operacion\n"
                    + "from ingresos_productos_detalle i, productos p, ingresos_productos_cabecera ic, configuraciones c\n"
                    + "where i.id_producto=p.id and i.id_ingreso_cabecera=ic.id and ic.id=" + id + " order by i.id";
            rs = DB_consultas_R_D.getTabla(consulta);

            try {
                jif_crear_ingreso_precios.modelo_productos.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "ETIQUETAS"});
                while (rs.next()) {
                    switch (frm_main.rol_precios) {
                        case 2: // BODEGA
                            if (jif_crear_ingreso_precios.replicaBodegaActiva) {
                                jif_crear_ingreso_precios.modelo_productos.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "ETIQUETAS", "BODEGA"});
                                jif_crear_ingreso_precios.Bodega bodGuardada = null;
                                try {
                                    bodGuardada = jif_crear_ingreso_precios.bodegaPorId(rs.getInt("id_bodega_control"));
                                } catch (Exception e) {
                                    bodGuardada = null;
                                }
                                jif_crear_ingreso_precios.modelo_productos.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                                    formateador.format(rs.getDouble("cantidad")), rs.getString("etiquetas"), bodGuardada});
                            } else {
                                jif_crear_ingreso_precios.modelo_productos.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "ETIQUETAS"});
                                jif_crear_ingreso_precios.modelo_productos.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                                    formateador.format(rs.getDouble("cantidad")), rs.getString("etiquetas")});
                            }
                            break;
                        case 3: // CONTADORA
                            jif_crear_ingreso_precios.modelo_productos.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD", "ETIQUETAS", "COSTO", "IVA", "DESCUENTO", "COSTO+IVA", "TOTAL"});
                            jif_crear_ingreso_precios.modelo_productos.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                                formateador.format(rs.getDouble("cantidad")), rs.getString("etiquetas"), metodos.formateador_dinero().format(rs.getDouble("precio_costo")), rs.getDouble("iva"), rs.getDouble("descuento")});
                            break;

                        default: // 4 PRECIOS
                            jif_crear_ingreso_precios.modelo_productos.setColumnIdentifiers(jif_crear_ingreso_precios.encabezadosRol4());

                            double costo = rs.getDouble("precio_costo"),
                             descuento = rs.getDouble("descuento"),
                             iva = rs.getDouble("iva"),
                             porcentaje_operacion = rs.getDouble("porcentaje_operacion");

                            double costo_iva_descuento = (costo + (costo * (iva / 100))) - ((costo + (costo * (iva / 100))) * (descuento / 100));

                            double costo_iva_descuento_gasto = costo_iva_descuento + (costo_iva_descuento * (porcentaje_operacion / 100));

                            // En TECNI desc_n_1/desc_n_2 del detalle guardan los
                            // margenes 2 y 3 de la linea (filaRol4 los ignora en AGRO).
                            jif_crear_ingreso_precios.modelo_productos.addRow(jif_crear_ingreso_precios.filaRol4(
                                    rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                                    formateador.format(rs.getDouble("cantidad")),
                                    metodos.formateador_dinero().format(rs.getDouble("precio_costo")),
                                    rs.getDouble("iva"), rs.getDouble("descuento"),
                                    metodos.formateador_dinero().format(costo_iva_descuento),
                                    metodos.formateador_dinero().format(costo_iva_descuento_gasto),
                                    rs.getDouble("porcentaje_utilidad"),
                                    rs.getDouble("venta"),
                                    rs.getDouble("desc_n_1"),
                                    rs.getDouble("valor_desc_1"),
                                    rs.getDouble("desc_n_2"),
                                    rs.getDouble("valor_desc_2"),
                                    rs.getDouble("valor_s_y_t"),
                                    rs.getDouble("valor_credito"),
                                    rs.getString("etiquetas")
                            ));
                            jif_crear_ingreso_precios.jtabla.setRowHeight(20);
                            jif_crear_ingreso_precios.btn_calcular_utildiad_porcentaje.setVisible(true);
                            break;
                    }
                }

                jif_crear_ingreso_precios.jtabla.setModel(jif_crear_ingreso_precios.modelo_productos);
                switch (frm_main.rol_precios) {
                    case 2:
                        jif_crear_ingreso_precios.TamanosTabla();
                        jif_crear_ingreso_precios.btn_calcular_costos.setVisible(false);
                        jif_crear_ingreso_precios.btn_calcular_precios_venta.setVisible(false);
                        jif_crear_ingreso_precios.btn_precargar.setVisible(false);
                        jif_crear_ingreso_precios.btn_Exportar_Worold.setVisible(false);
                        jif_crear_ingreso_precios.aplicarEditorBodega();
                        break;
                    case 3:
                        Font font = new Font("Arial", Font.PLAIN, 14);
                        jif_crear_ingreso_precios.jtabla.setRowHeight(25);
                        jif_crear_ingreso_precios.jtabla.setFont(font);
                        jif_crear_ingreso_precios.TamanosTablaContador();
                        jif_crear_ingreso_precios.btn_buscar.setEnabled(false);
                        jif_crear_ingreso_precios.txt_codigo_barras.setEnabled(false);
                        jif_crear_ingreso_precios.btn_calcular_costos.doClick();
                        jif_crear_ingreso_precios.btn_calcular_precios_venta.setVisible(false);
                        break;

                    default:
                        jif_crear_ingreso_precios.jtabla.setRowHeight(25);
                        font = new Font("Arial", Font.PLAIN, 14);
                        jif_crear_ingreso_precios.jtabla.setFont(font);
                        jif_crear_ingreso_precios.TamanosTablaPrecios();
                        jif_crear_ingreso_precios.btn_buscar.setEnabled(true);
                        jif_crear_ingreso_precios.btn_Exportar_Worold.setEnabled(false);
                        jif_crear_ingreso_precios.txt_codigo_barras.setEnabled(true);
                        jif_crear_ingreso_precios.btn_calcular_costos.setVisible(false);
                        break;
                }

            } catch (SQLException ex) {
                System.out.println(ex);
            }

            if (ver_editar.equals("ver")) {
                jif_crear_ingreso_precios.jbox_proveedor.setEnabled(false);
                jif_crear_ingreso_precios.jbox_transportador.setEnabled(false);
                jif_crear_ingreso_precios.txt_no_factura.setEnabled(false);
                jif_crear_ingreso_precios.txt_codigo_barras.setEnabled(false);
                jif_crear_ingreso_precios.btn_buscar.setEnabled(false);
                jif_crear_ingreso_precios.btn_calcular_costos.setEnabled(false);
                jif_crear_ingreso_precios.btn_precargar.setEnabled(false);
                jif_crear_ingreso_precios.jdate_fecha_entrada.setEnabled(false);
                jif_crear_ingreso_precios.btn_guardar.setEnabled(false);
                jif_crear_ingreso_precios.btn_limpiar.setEnabled(false);
                jif_crear_ingreso_precios.chk_cerrar.setEnabled(false);
                if (jif_crear_ingreso_precios.btn_enviar_bodega != null) {
                    jif_crear_ingreso_precios.btn_enviar_bodega.setEnabled(false);
                }
            } else {
                jif_crear_ingreso_precios.chk_cerrar.setEnabled(false);
                jif_crear_ingreso_precios.btn_guardar.setText("Actualizar");
            }
            frm.calcular_total();
            frm.show();
        }
    }

    /**
     * Carga en el panel de la derecha la mini-auditoria del ingreso
     * seleccionado: fecha/hora real, usuario y cada cambio (incluidos los
     * cambios de estado Recibido -> Ingresado -> Precios). Los datos los
     * escriben los triggers de sql/migracion_auditoria_ingresos.sql.
     */
    private void cargarHistorial() {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            lblHistorialTitulo.setText("Historial");
            jTablaHistorial.setModel(new javax.swing.table.DefaultTableModel(
                    new Object[][]{}, new String[]{"Fecha / Hora", "Usuario", "Cambio"}));
            return;
        }
        try {
            String id = jtabla.getValueAt(fila, 0).toString();
            lblHistorialTitulo.setText("Historial · Ingreso #" + id);
            jTablaHistorial.setModel(conexiondb.AuditoriaIngresos.historial(Integer.parseInt(id)));
            TableColumnModel cm = jTablaHistorial.getColumnModel();
            if (cm.getColumnCount() >= 3) {
                cm.getColumn(0).setPreferredWidth(130);
                cm.getColumn(1).setPreferredWidth(90);
                cm.getColumn(2).setPreferredWidth(260);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void btn_cerrarActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void btn_editarActionPerformed(java.awt.event.ActionEvent evt) {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
            return;
        }

        String estado = jtabla.getValueAt(fila, 4).toString();

        // Roles del usuario que pueden editar un registro en este estado. Si
        // tiene más de uno se le pregunta con cuál quiere editarlo.
        java.util.List<Integer> candidatos = new java.util.ArrayList<>();
        for (int rol : rolesUsuario()) {
            if (rolPuedeEditar(rol, estado)) {
                candidatos.add(rol);
            }
        }
        if (candidatos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Este registro no esta autorizado en su rol");
            return;
        }
        int rolElegido = SelectorRolPrecios.elegir(candidatos,
                "Registro en estado \"" + estado + "\". ¿Con cuál rol desea editarlo?");
        if (rolElegido == -1) {
            return;
        }
        frm_main.rol_precios = rolElegido;

        ver_ingreso("editar");
    }

    boolean act = true;

    public void actualizar() {
        modelo.setRowCount(0);

        String consulta = "";

        if (act) {
            consulta = "select i.id, p.nombre as proveedor, i.fecha, i.fecha_vencimiento, i.no_factura, i.estado, u.nombre as usuario, i.enviado_control_bodega "
                    + "from ingresos_productos_cabecera i, contactos p, users u "
                    + "where i.id_proveedor=p.id  and i.estado<2 and i.id_user=u.id order by fecha desc,  i.id desc";
            act = false;
        } else {
            consulta = "select i.id, p.nombre as proveedor, i.fecha, i.fecha_vencimiento, i.no_factura, i.estado, u.nombre as usuario, i.enviado_control_bodega "
                    + "from ingresos_productos_cabecera i, contactos p, users u "
                    + "where i.id_proveedor=p.id and i.id_user=u.id order by fecha desc, i.id desc";
            act = true;
        }

        modelo.setColumnIdentifiers(new Object[]{"id", "proveedor", "Numero factura", "Fecha", "Estado", "Usuario", "Enviado a bodega"});

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                String estado = "";
                switch (rs.getInt("estado")) {
                    case 0:
                        estado = "Recibido";
                        break;
                    case 1:
                        estado = "Ingresado";
                        break;
                    case 2:
                        estado = "Precios";
                        break;
                }

                boolean exportado = false;
                try {
                    exportado = rs.getBoolean("enviado_control_bodega");
                } catch (Exception ex) {
                    exportado = false;
                }
                String textoExportado = exportado ? "Enviado" : "No enviado";

                modelo.addRow(new Object[]{rs.getString("id"), rs.getString("proveedor"), rs.getString("no_factura"), rs.getDate("fecha"), estado, rs.getString("usuario"), textoExportado});
            }
            rs.close();
            jtabla.setModel(modelo);
            TamanosTabla();
            aplicarRendererExportado();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Pinta la ultima columna ("Enviado a bodega") en verde/rojo.
     */
    private void aplicarRendererExportado() {
        int idx = jtabla.getColumnCount() - 1;
        if (idx < 0) {
            return;
        }
        jtabla.getColumnModel().getColumn(idx).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String v = value == null ? "" : value.toString();
                if ("Enviado".equalsIgnoreCase(v)) {
                    c.setBackground(new java.awt.Color(198, 239, 206));
                    c.setForeground(new java.awt.Color(0, 97, 0));
                } else {
                    c.setBackground(new java.awt.Color(255, 199, 206));
                    c.setForeground(new java.awt.Color(156, 0, 6));
                }
                setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                return c;
            }
        });
    }

    // Variables declaration
    public static javax.swing.JButton btn_actualizar;
    private javax.swing.JButton btn_cerrar;
    private javax.swing.JButton btn_crear;
    private javax.swing.JButton btn_editar;
    private javax.swing.JButton btn_eliminar;
    private javax.swing.JButton btn_pagos;
    private javax.swing.JButton btn_world_office;
    private javax.swing.JButton btn_ver;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jtabla;
    private javax.swing.JTextField txt_Filtro;
    // Panel de historial (auditoria) a la derecha del listado
    private javax.swing.JPanel jPanelHistorial;
    private javax.swing.JLabel lblHistorialTitulo;
    private javax.swing.JScrollPane jScrollHistorial;
    private javax.swing.JTable jTablaHistorial;
    // End of variables declaration
}
