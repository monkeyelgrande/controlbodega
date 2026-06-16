package Precios;

import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.DB_consultas_R_D;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import modelos.Contactos;

/**
 * Busqueda de transportadores para el modulo Precios. Mismo estilo Material que
 * jd_buscar_proveedor_precios. El transportador puede ser cualquier contacto
 * (igual que el combo, que se llena con MostrarNombreContactos).
 */
public class jd_buscar_transportador_precios extends javax.swing.JDialog {

    static TableColumnModel columnModel = null;

    private JTextField txt_Filtro;
    private JButton btn_seleccionar;
    private JScrollPane jScrollPane1;
    private JTable jtabla;

    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    public jd_buscar_transportador_precios(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        columnModel = jtabla.getColumnModel();
        metodos.BuscarEnTabla(txt_Filtro, jtabla);
        mostrar();
        this.setLocationRelativeTo(parent);
        txt_Filtro.requestFocus();
        metodos.addEscapeListenerWindowDialog(this);

        jtabla.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    seleccionarTransportador();
                }
            }
        });

        jtabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    seleccionarTransportador();
                }
            }
        });

        txt_Filtro.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    jtabla.requestFocus();
                    if (jtabla.getRowCount() > 0) {
                        jtabla.getSelectionModel().setSelectionInterval(0, 0);
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (jtabla.getRowCount() > 0) {
                        jtabla.getSelectionModel().setSelectionInterval(0, 0);
                        seleccionarTransportador();
                    }
                }
            }
        });
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Buscar Transportador");
        setModal(true);
        setUndecorated(true);
        setSize(760, 580);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EstiloCompras.BG_FORM);
        root.setBorder(BorderFactory.createLineBorder(EstiloCompras.DIVIDER, 1));

        root.add(EstiloCompras.header(FontAwesome.SEARCH, "Buscar transportador", new Runnable() {
            @Override
            public void run() {
                dispose();
            }
        }), BorderLayout.NORTH);

        // ----- Cuerpo: filtro + tabla -----
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(EstiloCompras.BG_FORM);
        body.setBorder(BorderFactory.createEmptyBorder(18, 22, 8, 22));

        txt_Filtro = EstiloCompras.field("Buscar transportador por nombre...", FontAwesome.SEARCH);
        body.add(txt_Filtro, BorderLayout.NORTH);

        jtabla = new JTable();
        jtabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        EstiloCompras.styleTable(jtabla);
        jScrollPane1 = EstiloCompras.scroll(jtabla);
        body.add(jScrollPane1, BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);

        // ----- Footer: seleccionar -----
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(EstiloCompras.BG_SECTION);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, EstiloCompras.DIVIDER),
                BorderFactory.createEmptyBorder(12, 22, 12, 22)));

        btn_seleccionar = EstiloCompras.primaryBtn("Seleccionar", FontAwesome.CHECK);
        btn_seleccionar.addActionListener(e -> seleccionarTransportador());

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.add(Box.createHorizontalGlue());
        right.add(btn_seleccionar);
        footer.add(right, BorderLayout.CENTER);

        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void mostrar() {
        try {
            for (int i = modelo.getRowCount() - 1; i >= 0; i--) {
                modelo.removeRow(i);
            }
        } catch (Exception e) {
        }

        modelo.setColumnIdentifiers(new Object[]{"ID", "Nombre", "Cedula/NIT", "Contacto", "Ciudad"});
        ResultSet rs = DB_consultas_R_D.getTabla(
                "select id, nombre, cedula, contacto, ciudad from contactos order by nombre");
        try {
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("cedula"),
                    rs.getString("contacto"),
                    rs.getString("ciudad")
                });
            }
            rs.close();
            jtabla.setModel(modelo);
            ajustarColumnas();
        } catch (Exception e) {
            System.out.println("Error cargando transportadores: " + e);
        }
    }

    private void ajustarColumnas() {
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(0).setMaxWidth(60);
        columnModel.getColumn(1).setPreferredWidth(300);
        columnModel.getColumn(2).setPreferredWidth(120);
        columnModel.getColumn(3).setPreferredWidth(120);
        columnModel.getColumn(4).setPreferredWidth(120);
    }

    private void seleccionarTransportador() {
        int fila = jtabla.getSelectedRow();
        if (fila < 0) {
            javax.swing.JOptionPane.showMessageDialog(this, "Seleccione un transportador");
            return;
        }
        // La tabla puede estar filtrada/ordenada: convertir a indice del modelo.
        int filaModelo = jtabla.convertRowIndexToModel(fila);
        int id = Integer.parseInt(jtabla.getModel().getValueAt(filaModelo, 0).toString());

        javax.swing.JComboBox<Contactos> combo = jif_crear_ingreso_precios.jbox_transportador;
        for (int i = 0; i < combo.getItemCount(); i++) {
            Contactos c = combo.getItemAt(i);
            if (c.getId() == id) {
                combo.setSelectedIndex(i);
                break;
            }
        }

        this.dispose();
    }
}
