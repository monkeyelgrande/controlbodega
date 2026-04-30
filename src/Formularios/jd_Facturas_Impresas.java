package Formularios;

import conexiondb.DB_consultas_R_D;
import conexiondb.DBcontactos;
import modelos.Contactos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class jd_Facturas_Impresas extends javax.swing.JDialog {

    private JTextField txtBuscar;
    private JTable tablaFacturas;
    private JTable tablaProductos;
    private DefaultTableModel modeloFacturas;
    private DefaultTableModel modeloProductos;

    private boolean contactoResuelto = false;

    public jd_Facturas_Impresas(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponentes();
        setLocationRelativeTo(parent);
    }

    private void initComponentes() {
        setTitle("Facturas Impresas - Importar Productos");
        setSize(850, 600);
        setMinimumSize(new Dimension(700, 450));
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout(5, 5));

        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panelBusqueda.setBackground(new Color(24, 30, 80));

        JLabel lblBuscar = new JLabel("N\u00famero factura:");
        lblBuscar.setForeground(Color.WHITE);
        lblBuscar.setFont(new Font("Yu Gothic Medium", Font.BOLD, 14));
        panelBusqueda.add(lblBuscar);

        txtBuscar = new JTextField(20);
        txtBuscar.setFont(new Font("Tahoma", Font.BOLD, 16));
        panelBusqueda.add(txtBuscar);

        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.setFont(new Font("Yu Gothic Medium", Font.BOLD, 14));
        panelBusqueda.add(btnBuscar);

        JLabel lblInfo = new JLabel("   Doble clic en factura = importar todo | Doble clic en producto = importar uno");
        lblInfo.setForeground(new Color(254, 201, 45));
        lblInfo.setFont(new Font("Yu Gothic Medium", Font.ITALIC, 11));
        panelBusqueda.add(lblInfo);

        add(panelBusqueda, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.4);
        splitPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        modeloFacturas = new DefaultTableModel(
                new Object[]{"N\u00famero", "Cliente", "Fecha Factura", "Fecha Impresi\u00f3n", "Concepto"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaFacturas = new JTable(modeloFacturas);
        tablaFacturas.setFont(new Font("Tahoma", Font.PLAIN, 14));
        tablaFacturas.setRowHeight(25);
        tablaFacturas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableColumnModel cmFacturas = tablaFacturas.getColumnModel();
        cmFacturas.getColumn(0).setPreferredWidth(120);
        cmFacturas.getColumn(1).setPreferredWidth(300);
        cmFacturas.getColumn(2).setPreferredWidth(120);
        cmFacturas.getColumn(3).setPreferredWidth(160);
        cmFacturas.getColumn(4).setMinWidth(0);
        cmFacturas.getColumn(4).setMaxWidth(0);
        cmFacturas.getColumn(4).setPreferredWidth(0);

        JPanel panelFacturas = new JPanel(new BorderLayout());
        panelFacturas.setBorder(BorderFactory.createTitledBorder("Facturas Encontradas"));
        panelFacturas.add(new JScrollPane(tablaFacturas), BorderLayout.CENTER);
        splitPane.setTopComponent(panelFacturas);

        modeloProductos = new DefaultTableModel(
                new Object[]{"C\u00f3digo", "Descripci\u00f3n", "Cantidad"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaProductos = new JTable(modeloProductos);
        tablaProductos.setFont(new Font("Tahoma", Font.PLAIN, 14));
        tablaProductos.setRowHeight(25);
        TableColumnModel cmProductos = tablaProductos.getColumnModel();
        cmProductos.getColumn(0).setPreferredWidth(120);
        cmProductos.getColumn(1).setPreferredWidth(450);
        cmProductos.getColumn(2).setPreferredWidth(100);

        JPanel panelProductos = new JPanel(new BorderLayout());
        panelProductos.setBorder(BorderFactory.createTitledBorder("Productos de la Factura (doble clic para importar uno)"));
        panelProductos.add(new JScrollPane(tablaProductos), BorderLayout.CENTER);
        splitPane.setBottomComponent(panelProductos);

        add(splitPane, BorderLayout.CENTER);

        // === Eventos ===

        btnBuscar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { buscarFacturas(); }
        });

        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) { buscarFacturas(); }
            }
        });

        tablaFacturas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tablaFacturas.getSelectedRow();
                if (row >= 0) {
                    String numero = (String) modeloFacturas.getValueAt(row, 0);
                    cargarProductos(numero);
                    contactoResuelto = false;
                }
            }
        });

        tablaFacturas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) { importarTodosLosProductos(); }
            }
        });

        tablaProductos.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) { importarProductoIndividual(); }
            }
        });
    }

    // ================================================================
    // Busqueda
    // ================================================================

    private void buscarFacturas() {
        String busqueda = txtBuscar.getText().trim();
        if (busqueda.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese un n\u00famero de factura");
            return;
        }

        modeloFacturas.setRowCount(0);
        modeloProductos.setRowCount(0);
        contactoResuelto = false;

        String sql = "SELECT f.numero_factura, f.cliente, f.fecha_factura, f.fecha_impresion, "
                   + "  (SELECT l.concepto FROM log_impresiones l "
                   + "   WHERE l.numero_factura = f.numero_factura AND l.concepto IS NOT NULL "
                   + "   LIMIT 1) as concepto "
                   + "FROM facturas_impresas f "
                   + "WHERE f.numero_factura ILIKE '%" + busqueda + "%' "
                   + "ORDER BY f.fecha_impresion DESC LIMIT 50";

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            int count = 0;
            while (rs.next()) {
                modeloFacturas.addRow(new Object[]{
                    rs.getString("numero_factura"),
                    rs.getString("cliente") != null ? rs.getString("cliente") : "",
                    rs.getDate("fecha_factura") != null ? rs.getDate("fecha_factura").toString() : "",
                    rs.getTimestamp("fecha_impresion") != null ? rs.getTimestamp("fecha_impresion").toString() : "",
                    rs.getString("concepto") != null ? rs.getString("concepto") : ""
                });
                count++;
            }
            rs.close();

            if (count == 0) {
                JOptionPane.showMessageDialog(this, "No se encontraron facturas con '" + busqueda + "'");
            }
        } catch (Exception e) {
            System.err.println("Error buscando facturas: " + e.getMessage());
        }
    }

    private void cargarProductos(String numeroFactura) {
        modeloProductos.setRowCount(0);

        String sql = "SELECT d.codigo_producto, d.descripcion, d.cantidad "
                   + "FROM detalle_factura d "
                   + "JOIN facturas_impresas f ON d.factura_id = f.id "
                   + "WHERE f.numero_factura = '" + numeroFactura + "' "
                   + "ORDER BY d.id";

        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            while (rs.next()) {
                modeloProductos.addRow(new Object[]{
                    rs.getString("codigo_producto"),
                    rs.getString("descripcion") != null ? rs.getString("descripcion") : "",
                    rs.getDouble("cantidad")
                });
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error cargando productos: " + e.getMessage());
        }
    }

    // ================================================================
    // Contacto - Resolucion
    // ================================================================

    private String extraerCedulaDeConcepto(String concepto) {
        if (concepto == null || concepto.isEmpty()) return null;
        Pattern pattern = Pattern.compile("\\*(\\d+)");
        Matcher matcher = pattern.matcher(concepto);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String[] buscarContactoPorNombre(String nombreCliente) {
        if (nombreCliente == null || nombreCliente.trim().isEmpty()) return null;
        String sql = "SELECT id, nombre, cedula FROM contactos "
                   + "WHERE TRIM(nombre) ILIKE TRIM('" + nombreCliente.trim().replace("'", "''") + "') LIMIT 1";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            if (rs.next()) {
                String[] resultado = new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("nombre"),
                    rs.getString("cedula") != null ? rs.getString("cedula") : ""
                };
                rs.close();
                return resultado;
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error buscando contacto por nombre: " + e.getMessage());
        }
        return null;
    }

    private String[] buscarContactoPorCedula(String cedula) {
        if (cedula == null || cedula.trim().isEmpty()) return null;
        String sql = "SELECT id, nombre, cedula FROM contactos "
                   + "WHERE TRIM(cedula) = '" + cedula.trim() + "' LIMIT 1";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            if (rs.next()) {
                String[] resultado = new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("nombre"),
                    rs.getString("cedula") != null ? rs.getString("cedula") : ""
                };
                rs.close();
                return resultado;
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error buscando contacto por cedula: " + e.getMessage());
        }
        return null;
    }

    private String[] buscarContactoPorId(int id) {
        String sql = "SELECT id, nombre, cedula FROM contactos WHERE id = " + id + " LIMIT 1";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla(sql);
            if (rs.next()) {
                String[] resultado = new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("nombre"),
                    rs.getString("cedula") != null ? rs.getString("cedula") : ""
                };
                rs.close();
                return resultado;
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error buscando contacto por ID: " + e.getMessage());
        }
        return null;
    }

    private String[] crearContacto(String nombre, String cedula) {
        try {
            DBcontactos dbcontactos = new DBcontactos();
            Contactos contacto = new Contactos();
            String idNuevo = DB_consultas_R_D.TraerIdMaximoNuevoContacto();
            contacto.setId(Integer.parseInt(idNuevo));
            contacto.setNombre(nombre.trim());
            contacto.setCedula(cedula != null ? cedula.trim() : "");
            if (dbcontactos.Guardar(contacto) > 0) {
                return new String[]{String.valueOf(contacto.getId()), contacto.getNombre(), contacto.getCedula()};
            }
        } catch (Exception e) {
            System.err.println("Error creando contacto: " + e.getMessage());
        }
        return null;
    }

    /**
     * Flujo de resolucion de contacto:
     * 1. Buscar por nombre (trim + ILIKE)
     * 2. Si no lo encuentra, extraer cedula del concepto (*NUMERO)
     * 3. Si NO hay cedula -> asignar contacto ID = 1 (por defecto)
     * 4. Si HAY cedula, buscar por cedula
     * 5. Si la cedula existe -> usar ese contacto
     * 6. Si la cedula no existe -> crear contacto nuevo con nombre + cedula
     */
    private String[] resolverContacto(String nombreCliente, String concepto) {
        // 1. Buscar por nombre
        String[] contacto = buscarContactoPorNombre(nombreCliente);
        if (contacto != null) {
            System.out.println("[CONTACTO] Encontrado por nombre: " + contacto[1] + " (ID: " + contacto[0] + ")");
            return contacto;
        }

        // 2. Extraer cedula del concepto
        String cedula = extraerCedulaDeConcepto(concepto);

        // 3. Si NO hay cedula -> contacto por defecto ID = 1
        if (cedula == null) {
            System.out.println("[CONTACTO] Sin cedula en concepto, usando contacto por defecto ID=1");
            contacto = buscarContactoPorId(1);
            if (contacto != null) {
                return contacto;
            }
            // Si ni el ID 1 existe, retornar un fallback manual
            return new String[]{"1", "CONSUMIDOR FINAL", ""};
        }

        // 4. Hay cedula, buscar por cedula
        contacto = buscarContactoPorCedula(cedula);
        if (contacto != null) {
            System.out.println("[CONTACTO] Encontrado por cedula " + cedula + ": " + contacto[1] + " (ID: " + contacto[0] + ")");
            return contacto;
        }

        // 5. Cedula no existe, crear contacto nuevo
        contacto = crearContacto(nombreCliente, cedula);
        if (contacto != null) {
            System.out.println("[CONTACTO] Creado nuevo: " + contacto[1] + " (ID: " + contacto[0] + ") CC: " + cedula);
            return contacto;
        }

        // Fallback final
        return new String[]{"1", "CONSUMIDOR FINAL", ""};
    }

    /**
     * Asigna el contacto al formulario (id, nombre, cedula).
     * Tambien carga el concepto en observaciones y el numero de factura en codigo.
     * Solo se ejecuta una vez por sesion de importacion.
     */
    private void asignarContactoAlFormulario() {
        if (contactoResuelto) return;

        int facRow = tablaFacturas.getSelectedRow();
        if (facRow < 0) return;

        String nombreCliente = (String) modeloFacturas.getValueAt(facRow, 1);
        String concepto = (String) modeloFacturas.getValueAt(facRow, 4);
        String numeroFactura = (String) modeloFacturas.getValueAt(facRow, 0);

        // Resolver y asignar contacto
        String[] contacto = resolverContacto(nombreCliente, concepto);
        if (contacto != null) {
            frm_Crear_Orden.lbl_id_cliente.setText(contacto[0]);
            frm_Crear_Orden.lbl_nombre_cliente.setText(contacto[1]);
            if (contacto.length > 2) {
                frm_Crear_Orden.lbl_cedula.setText(contacto[2]);
            }
        }

        // Cargar concepto en observaciones (sin la cedula embebida)
        if (concepto != null && !concepto.isEmpty()) {
            String conceptoLimpio = concepto.replaceAll("\\*\\d+", "").trim();
            // Limpiar espacios dobles que puedan quedar
            conceptoLimpio = conceptoLimpio.replaceAll("\\s{2,}", " ").trim();
            frm_Crear_Orden.txt_observaciones.setText(conceptoLimpio);
        }

        // Cargar numero de factura completo en txt_codigo
        if (numeroFactura != null && !numeroFactura.isEmpty()) {
            frm_Crear_Orden.txt_codigo.setText(numeroFactura);
        }

        contactoResuelto = true;
    }

    // ================================================================
    // Obtener formulario
    // ================================================================

    private frm_Crear_Orden obtenerFormulario() {
        for (JInternalFrame frame : frm_main.escritorio.getAllFrames()) {
            if (frame instanceof frm_Crear_Orden) {
                return (frm_Crear_Orden) frame;
            }
        }
        return null;
    }

    // ================================================================
    // Importar TODOS los productos (doble clic en factura)
    // ================================================================

    private void importarTodosLosProductos() {
        int row = tablaFacturas.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una factura");
            return;
        }

        if (modeloProductos.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "La factura seleccionada no tiene productos");
            return;
        }

        String numeroFactura = (String) modeloFacturas.getValueAt(row, 0);
        String nombreCliente = (String) modeloFacturas.getValueAt(row, 1);

        int resp = JOptionPane.showConfirmDialog(this,
                "Importar " + modeloProductos.getRowCount() + " productos de la factura " + numeroFactura + "?\n"
                + "Cliente: " + nombreCliente,
                "Confirmar importaci\u00f3n", JOptionPane.YES_NO_OPTION);

        if (resp != JOptionPane.YES_OPTION) return;

        frm_Crear_Orden formulario = obtenerFormulario();
        if (formulario == null) {
            JOptionPane.showMessageDialog(this, "No se encontr\u00f3 el formulario de \u00f3rdenes abierto");
            return;
        }

        // Resolver contacto
        asignarContactoAlFormulario();

        // Importar productos acumulando errores
        int importados = 0;
        List<String> noImportados = new ArrayList<String>();
        List<String> duplicados = new ArrayList<String>();

        for (int i = 0; i < modeloProductos.getRowCount(); i++) {
            String codigo = (String) modeloProductos.getValueAt(i, 0);
            double cantidad = 0;
            try {
                cantidad = Double.parseDouble(modeloProductos.getValueAt(i, 2).toString());
            } catch (Exception e) {
                cantidad = 1;
            }

            if (formulario.existe_en_tabla(codigo)) {
                duplicados.add(codigo);
                continue;
            }

            if (DB_consultas_R_D.consultar_existencia_campo_String("codigo_barras", codigo, "productos") == 1) {
                try {
                    formulario.agregar_cod(codigo, cantidad);
                    importados++;
                } catch (Exception e) {
                    noImportados.add(codigo);
                }
            } else {
                noImportados.add(codigo);
            }
        }

        if (!duplicados.isEmpty()) {
            StringBuilder dup = new StringBuilder();
            dup.append(duplicados.size()).append(" productos ya estaban en la orden (omitidos):\n");
            for (int i = 0; i < duplicados.size() && i < 20; i++) {
                dup.append("  - ").append(duplicados.get(i)).append("\n");
            }
            if (duplicados.size() > 20) dup.append("  ... y ").append(duplicados.size() - 20).append(" m\u00e1s");
            JOptionPane.showMessageDialog(this, dup.toString(), "Productos duplicados", JOptionPane.INFORMATION_MESSAGE);
        }

        // Mensaje final unico
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Importaci\u00f3n completada:\n");
        mensaje.append(importados).append(" productos agregados");

        if (!noImportados.isEmpty()) {
            mensaje.append("\n\n").append(noImportados.size()).append(" productos NO importados (no existen en BD):\n");
            for (int i = 0; i < noImportados.size(); i++) {
                mensaje.append("  - ").append(noImportados.get(i));
                if (i < noImportados.size() - 1) mensaje.append("\n");
                if (i >= 19) {
                    mensaje.append("\n  ... y ").append(noImportados.size() - 20).append(" m\u00e1s");
                    break;
                }
            }
        }

        mensaje.append("\nCliente: ").append(frm_Crear_Orden.lbl_nombre_cliente.getText())
               .append(" (ID: ").append(frm_Crear_Orden.lbl_id_cliente.getText()).append(")");

        JOptionPane.showMessageDialog(this, mensaje.toString(), "Resultado",
                noImportados.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

        dispose();
    }

    // ================================================================
    // Importar UN producto (doble clic en producto)
    // ================================================================

    private void importarProductoIndividual() {
        int prodRow = tablaProductos.getSelectedRow();
        if (prodRow < 0) return;

        int facRow = tablaFacturas.getSelectedRow();
        if (facRow < 0) {
            JOptionPane.showMessageDialog(this, "No hay factura seleccionada");
            return;
        }

        frm_Crear_Orden formulario = obtenerFormulario();
        if (formulario == null) {
            JOptionPane.showMessageDialog(this, "No se encontr\u00f3 el formulario de \u00f3rdenes abierto");
            return;
        }

        // Resolver contacto en el primer doble clic
        asignarContactoAlFormulario();

        String codigo = (String) modeloProductos.getValueAt(prodRow, 0);
        double cantidad = 0;
        try {
            cantidad = Double.parseDouble(modeloProductos.getValueAt(prodRow, 2).toString());
        } catch (Exception e) {
            cantidad = 1;
        }

        if (formulario.existe_en_tabla(codigo)) {
            JOptionPane.showMessageDialog(this,
                    "El producto " + codigo + " ya fue agregado a la orden",
                    "Producto duplicado", JOptionPane.WARNING_MESSAGE);
            modeloProductos.setValueAt("YA EN ORDEN", prodRow, 1);
            return;
        }

        if (DB_consultas_R_D.consultar_existencia_campo_String("codigo_barras", codigo, "productos") == 1) {
            formulario.agregar_cod(codigo, cantidad);
            modeloProductos.setValueAt("IMPORTADO", prodRow, 1);
        } else {
            JOptionPane.showMessageDialog(this,
                    "El producto " + codigo + " no existe en la base de datos",
                    "Producto no encontrado", JOptionPane.WARNING_MESSAGE);
        }
    }
}