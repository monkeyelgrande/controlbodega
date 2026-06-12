package Precios;

import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * Gestion de precios del producto (modulo Precios): lista los productos con
 * sus precios estilo agro y abre el editor. La creacion/deshabilitacion de
 * productos sigue siendo del modulo Productos de controlbodega.
 *
 * @author Monkeyelgrande
 */
public class frm_precios_productos extends javax.swing.JInternalFrame {

    private JTable jtabla;
    private JTextField txt_Filtro;

    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public frm_precios_productos() {
        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Precios de productos");
        construir();
        mostrar();
        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        txt_Filtro.requestFocus();
    }

    private void construir() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.setBackground(java.awt.Color.WHITE);

        // Encabezado
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new java.awt.Color(46, 125, 50));
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel titulo = new JLabel("Precios de productos");
        titulo.setFont(new java.awt.Font("Tahoma", 1, 22));
        titulo.setForeground(java.awt.Color.WHITE);
        header.add(titulo, BorderLayout.WEST);

        // Barra de acciones
        JPanel barra = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4));
        barra.setOpaque(false);
        txt_Filtro = new JTextField(35);
        txt_Filtro.setFont(new java.awt.Font("Tahoma", 0, 16));
        new Metodos.TextPrompt("Buscar por código o descripción...", txt_Filtro);

        JButton btnEditar = new JButton("Editar precios");
        btnEditar.setFont(new java.awt.Font("Tahoma", 1, 14));
        btnEditar.setBackground(new java.awt.Color(46, 125, 50));
        btnEditar.setForeground(java.awt.Color.WHITE);
        btnEditar.addActionListener(e -> editarSeleccionado());

        JButton btnActualizar = new JButton("Actualizar");
        btnActualizar.setFont(new java.awt.Font("Tahoma", 1, 14));
        btnActualizar.addActionListener(e -> mostrar());

        barra.add(txt_Filtro);
        barra.add(btnEditar);
        barra.add(btnActualizar);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(header, BorderLayout.NORTH);
        norte.add(barra, BorderLayout.SOUTH);

        // Tabla
        jtabla = new JTable(modelo);
        jtabla.setRowHeight(26);
        jtabla.setFont(new java.awt.Font("Tahoma", 0, 13));
        jtabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    editarSeleccionado();
                }
            }
        });

        root.add(norte, BorderLayout.NORTH);
        root.add(new JScrollPane(jtabla), BorderLayout.CENTER);
        setContentPane(root);
        setSize(1200, 650);
    }

    public void mostrar() {
        modelo.setRowCount(0);
        modelo.setColumnIdentifiers(new Object[]{"Código", "Descripción", "Costo", "IVA",
            "Venta", "Desc. N1", "Desc. N2", "S y T", "Crédito", "% Util.", "Cant. paquete"});
        ResultSet rs = DB_consultas_R_D.getTabla(
                "select codigo_barras, descripcion, coalesce(precio_costo,0) as precio_costo, coalesce(iva,0) as iva, "
                + "coalesce(venta,0) as venta, coalesce(valor_desc_1,0) as valor_desc_1, coalesce(valor_desc_2,0) as valor_desc_2, "
                + "coalesce(valor_s_y_t,0) as valor_s_y_t, coalesce(valor_credito,0) as valor_credito, "
                + "coalesce(porcentaje_utilidad,0) as porcentaje_utilidad, coalesce(cant_paquete,0) as cant_paquete "
                + "from productos where coalesce(estado,true)=true order by descripcion");
        try {
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getString("codigo_barras"),
                    rs.getString("descripcion"),
                    metodos.formateador_dinero().format(rs.getDouble("precio_costo")),
                    rs.getDouble("iva"),
                    metodos.formateador_dinero().format(rs.getDouble("venta")),
                    metodos.formateador_dinero().format(rs.getDouble("valor_desc_1")),
                    metodos.formateador_dinero().format(rs.getDouble("valor_desc_2")),
                    metodos.formateador_dinero().format(rs.getDouble("valor_s_y_t")),
                    metodos.formateador_dinero().format(rs.getDouble("valor_credito")),
                    rs.getDouble("porcentaje_utilidad"),
                    rs.getInt("cant_paquete")
                });
            }
            rs.close();
            ajustarColumnas();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void ajustarColumnas() {
        TableColumnModel cm = jtabla.getColumnModel();
        cm.getColumn(0).setPreferredWidth(110);
        cm.getColumn(1).setPreferredWidth(420);
        for (int i = 2; i < cm.getColumnCount(); i++) {
            cm.getColumn(i).setPreferredWidth(90);
        }
    }

    private void editarSeleccionado() {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto");
            return;
        }
        String codigo = jtabla.getValueAt(fila, 0).toString();
        jd_editar_precios_producto dialogo = new jd_editar_precios_producto(null, true, codigo);
        dialogo.setVisible(true);
        mostrar();
    }
}
