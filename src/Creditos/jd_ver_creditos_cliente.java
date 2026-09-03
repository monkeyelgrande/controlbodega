/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Creditos;

import Creditos.frm_Creditos;
import Formularios.frm_main;
import Creditos.CellRendererCreditosCliente;
import Metodos.ExportarExcel;
import Metodos.metodos;
import Creditos.db.DBabonos;
import conexiondb.DB_consultas_R_D;
import java.awt.Color;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import Creditos.modelos.Abonos;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author Monkeyelgrande
 */
public class jd_ver_creditos_cliente extends javax.swing.JDialog {

    private static final Logger LOGGER = Logger.getLogger(jd_ver_creditos_cliente.class.getName());
    private static JasperReport reporteEstadoCuentaCache;

    /**
     * Creates new form jd_ver_devolucion
     */
    CellRendererCreditosCliente myRenderer = new CellRendererCreditosCliente();
    public static DefaultTableModel modelo_facturas = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false; //Con esto conseguimos que la tabla no se pueda editar
        }
    };

    public static String ventana = "";
    static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    public static String id_cliente;

    public jd_ver_creditos_cliente(java.awt.Frame parent, boolean modal, String id_cliente) {
        super(parent, modal);
        initComponents();
        this.id_cliente = id_cliente;
        permisos(frm_main.perfil);
        this.setLocationRelativeTo(parent);
        metodos.addEscapeListenerWindowDialog(this);
        MetodosCreditos.EstiloTablaMaterialGlobalPequeno(jtabla_creditos);

        jtabla_creditos.setDefaultRenderer(Object.class, myRenderer);
        Calendar fecha = new GregorianCalendar();
        jtabla_creditos.setModel(modelo_facturas);
        Doble_clic_tablas_CC();
        actualizar(false);
        traer_cupo();
    }

    public void permisos(int perfil) {
        switch (perfil) {
            case 2:
                btn_abonar.setEnabled(false);
                break;
            default:
                break;
        }
    }
    public void actualizar(boolean soloPendientes) {

        String consulta = "select id, cedula, nombre from contactos where id=" + id_cliente;

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                jd_ver_creditos_cliente.lbl_cliente_nombre.setText(rs.getString("nombre"));
                jd_ver_creditos_cliente.lbl_cedula.setText(rs.getString("cedula"));
                jd_ver_creditos_cliente.lbl_id_cliente.setText(rs.getString("id"));
            }
            rs.close();

        } catch (SQLException ex) {
            Logger.getLogger(frm_Creditos.class.getName()).log(Level.SEVERE, null, ex);
        }
        consulta = sqlMovimientosCliente(id_cliente, soloPendientes);

        jd_ver_creditos_cliente.modelo_facturas.setRowCount(0);

        jd_ver_creditos_cliente.modelo_facturas.setColumnIdentifiers(new Object[]{"ID factura", "Fecha Creación", "Tipo", "Cuenta", "# factura", "Total", "Cancelado", "Saldo", "Deuda Actual", "General"});

        rs = DB_consultas_R_D.getTabla(consulta);
        double total_deuda = 0;
        try {
            while (rs.next()) {
                // "General" = Si cuando la fila es la cabecera de un pago
                String general = rs.getString("tipo").equals("CREDITO") ? "No" : "Si";
                total_deuda = rs.getDouble("deuda_actual");

                jd_ver_creditos_cliente.modelo_facturas.addRow(new Object[]{rs.getString("id"), sdf.format(rs.getDate("fecha_creacion")), rs.getString("tipo"), rs.getString("cuenta"),
                    rs.getString("codigo"), metodos.formateador_dinero().format(rs.getDouble("total")),
                    metodos.formateador_dinero().format(rs.getDouble("abono")),
                    metodos.formateador_dinero().format(rs.getDouble("saldo")),
                    metodos.formateador_dinero().format(total_deuda), general});
            }
            rs.close();
            lbl_total_saldo.setText(metodos.formateador_dinero().format(total_deuda));
            TamanosTablaAbonos();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, null, e);
        }
    }

    public static void traer_cupo() {
        try {

            String consulta = sqlCupoCliente(id_cliente);

//            System.out.println(consulta);
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);

            try {
                while (rs.next()) {
                    txt_cupo_aprobado.setText(metodos.formateador_dinero().format(rs.getDouble("cupo")));
                    lbl_cupo_usado.setText(metodos.formateador_dinero().format(rs.getDouble("debe")));
                    lbl_cupo_disponible.setText(metodos.formateador_dinero().format(rs.getDouble("cupo_disponible")));
                }
                rs.close();

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, null, e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, null, e);
        }
    }

    /**
     * Selecciona (y deja en azul / a la vista) la fila de la tabla de créditos
     * cuyo "# factura" (columna 4) coincide con el código recibido. Se usa para
     * que, al abrir este diálogo desde un doble clic, quede preseleccionado el
     * crédito de la factura sobre la que se hizo doble clic.
     */
    public void seleccionarFacturaPorCodigo(String codigo) {
        if (codigo == null) {
            return;
        }
        for (int i = 0; i < jtabla_creditos.getRowCount(); i++) {
            Object val = jtabla_creditos.getValueAt(i, 4);
            if (val != null && val.toString().equals(codigo)) {
                jtabla_creditos.setRowSelectionInterval(i, i);
                jtabla_creditos.scrollRectToVisible(jtabla_creditos.getCellRect(i, 0, true));
                jtabla_creditos.requestFocusInWindow();
                break;
            }
        }
    }

    public void TamanosTablaAbonos() {
        jtabla_creditos.getTableHeader().setReorderingAllowed(false);
        TableColumnModel columnModel = jtabla_creditos.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(40);
        columnModel.getColumn(1).setPreferredWidth(80);
        columnModel.getColumn(2).setPreferredWidth(200);
        columnModel.getColumn(3).setPreferredWidth(100);
        columnModel.getColumn(4).setPreferredWidth(100);
        columnModel.getColumn(5).setPreferredWidth(100);
        columnModel.getColumn(6).setPreferredWidth(100);
        columnModel.getColumn(7).setPreferredWidth(100);
        columnModel.getColumn(8).setPreferredWidth(100);
        columnModel.getColumn(9).setPreferredWidth(40);

    }

    private static void cargarFotoYPdfAbono(ResultSet rs, jd_abonar_a_total frm) throws SQLException {
        String foto = rs.getString("foto");
        if (foto == null || foto.equals("")) {
            jd_abonar_a_total.btn_imagen.setEnabled(false);
            jd_abonar_a_total.btn_Eliminar_Imagen.setEnabled(false);
        } else {
            String ruta = DB_consultas_R_D.Ruta_Imagenes() + foto;
            Image img = new ImageIcon(ruta).getImage();
            Image newimg = img.getScaledInstance(frm.lbl_foto.getWidth(), frm.lbl_foto.getHeight(), java.awt.Image.SCALE_SMOOTH);
            frm.lbl_foto.setIcon(new ImageIcon(newimg));
            jd_abonar_a_total.ruta_ver_imagen = ruta;
            jd_abonar_a_total.btn_imagen.setEnabled(true);
            jd_abonar_a_total.btn_Eliminar_Imagen.setEnabled(true);
        }

        String pdf = rs.getString("pdf");
        if (pdf == null || pdf.equals("null") || pdf.equals("")) {
            jd_abonar_a_total.btn_ver_pdf.setEnabled(false);
            jd_abonar_a_total.btn_Eliminar_PDF.setEnabled(false);
        } else {
            jd_abonar_a_total.ruta_ver_PDF = DB_consultas_R_D.Ruta_Imagenes() + pdf;
            jd_abonar_a_total.btn_ver_pdf.setEnabled(true);
            jd_abonar_a_total.btn_Eliminar_PDF.setEnabled(true);
        }
    }

    static void cargarAbonoEnDialog(jd_abonar_a_total frm, String idCabecera, String saldo, boolean modoVisualizacion) {
        String consulta = "select ca.id, ca.fecha, c.id as id_cliente, c.nombre as cliente, c.cedula, ca.total as abono, ca.observacion, "
                + "ca.foto, coalesce(ca.pdf,'null') as pdf, t.nombre as tipo_abono, t.id as id_tipo_abono \n"
                + "from abonos_cabeceras ca, contactos c, tipos_abonos t \n"
                + "where ca.id_contacto=c.id and ca.id_tipo_abono=t.id and ca.id=" + idCabecera;

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {
                try {
                    cargarFotoYPdfAbono(rs, frm);
                    if (modoVisualizacion) {
                        jd_abonar_a_total.jbox_tipo_abono.setEnabled(false);
                        jd_abonar_a_total.txt_abono.setEnabled(false);
                        jd_abonar_a_total.txt_observacion.setEnabled(false);
                        jd_abonar_a_total.btn_guardar.setEnabled(false);
                        jd_abonar_a_total.btnAgregarImagen.setEnabled(false);
                        jd_abonar_a_total.btnAgregarImagen1.setEnabled(false);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "No se cargo la imagen", e);
                }

                jd_abonar_a_total.lbl_id_abono.setText(rs.getString("id"));
                jd_abonar_a_total.jdate_fecha_creacion.setDate(rs.getDate("fecha"));
                jd_abonar_a_total.lbl_cliente_nombre.setText(rs.getString("cliente"));
                jd_abonar_a_total.lbl_cedula.setText(rs.getString("cedula"));
                jd_abonar_a_total.txt_observacion.setText(rs.getString("observacion"));
                jd_abonar_a_total.lbl_id_cliente.setText(rs.getString("id_cliente"));
                jd_abonar_a_total.txt_abono.setText(metodos.formateador_dinero().format(rs.getDouble("abono")));
                jd_abonar_a_total.jbox_tipo_abono.setSelectedItem(rs.getString("tipo_abono"));
                jd_abonar_a_total.nombreArchivoImagen = rs.getString("foto");
                jd_abonar_a_total.nombreArchivoPDF = rs.getString("pdf");
                jd_abonar_a_total.id_tipo_abono = rs.getInt("id_tipo_abono");
                jd_abonar_a_total.lbl_total_saldo.setText(saldo);
            }
            rs.close();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
    }

    static void cargarAbonosHijosEnDialog(String idCabecera) {
        String consulta = "SELECT\n"
                + "    f.id,\n"
                + "    f.fecha_creacion,\n"
                + "    cu.nombre AS cuenta,\n"
                + "    f.codigo,\n"
                + "    f.total,\n"
                + "    a.abono,\n"
                + "    (f.total - (SELECT COALESCE(SUM(a2.abono),0) FROM abonos a2 WHERE a2.id_credito=f.id)) AS saldo,\n"
                + "    a.id AS id_abono_hijo\n"
                + "FROM abonos a\n"
                + "JOIN creditos f ON a.id_credito = f.id\n"
                + "LEFT JOIN cuentas cu ON f.id_cuenta = cu.id\n"
                + "WHERE a.id_cabecera = " + idCabecera + "\n"
                + "ORDER BY a.id";

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        jd_abonar_a_total.modelo_tabla.setRowCount(0);
        jd_abonar_a_total.modelo_tabla.setColumnIdentifiers(new Object[]{"ID factura", "Fecha Creación", "Cuenta", "# factura", "Total", "Abonado", "Saldo", "Id abono hijo"});
        try {
            while (rs.next()) {
                try {
                    jd_abonar_a_total.modelo_tabla.addRow(new Object[]{rs.getString("id"), sdf.format(rs.getDate("fecha_creacion")), rs.getString("cuenta"),
                        rs.getString("codigo"), metodos.formateador_dinero().format(rs.getDouble("total")), metodos.formateador_dinero().format(rs.getDouble("abono")),
                        metodos.formateador_dinero().format(rs.getDouble("saldo")), rs.getString("id_abono_hijo")});
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, null, e);
                }
            }
            rs.close();
            jd_abonar_a_total.jtabla.setModel(jd_abonar_a_total.modelo_tabla);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
    }

    public void Doble_clic_tablas_CC() {
        jtabla_creditos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() != 2) {
                    return;
                }
                int fila = jtabla_creditos.getSelectedRow();
                if (fila < 0) {
                    return;
                }
                String tipo = jtabla_creditos.getValueAt(fila, 2).toString();

                if (tipo.equals("CREDITO")) {
                    if (jtabla_creditos.getSelectedColumn() == 5) {
                        VerAbonos_credito();
                    } else {
                        ver_credito();
                    }
                    return;
                }

                // cabecera de un pago: se muestra con su detalle de facturas
                jd_abonar_a_total frm = new jd_abonar_a_total(null, true);
                String id = jtabla_creditos.getValueAt(fila, 0).toString();
                String saldo = jtabla_creditos.getValueAt(fila, 8).toString();
                cargarAbonoEnDialog(frm, id, saldo, true);
                cargarAbonosHijosEnDialog(id);
                jd_abonar_a_total.jdate_fecha_creacion.setEnabled(false);
                jd_abonar_a_total.btn_guardar.setText("Actualizar");
                jd_abonar_a_total.tipo = "editar";
                jd_abonar_a_total.sumar_total();
                frm.show();
            }
        }
        );
    }

    public void AbonarATotal() {
        jd_abonar_a_total frm = new jd_abonar_a_total(null, true);

        String id = lbl_id_cliente.getText();
        String saldo = lbl_total_saldo.getText();

        jd_abonar_a_total.lbl_cliente_nombre.setText(lbl_cliente_nombre.getText());
        jd_abonar_a_total.lbl_cedula.setText(lbl_cedula.getText());
        jd_abonar_a_total.lbl_id_cliente.setText(lbl_id_cliente.getText());
        jd_abonar_a_total.lbl_total_saldo.setText(saldo);
        jd_abonar_a_total.btn_imprimir.setEnabled(false);

        btn_actualizar.doClick();

        jd_abonar_a_total.modelo_tabla.setColumnIdentifiers(new Object[]{"ID factura", "Fecha Creación", "Tipo", "Cuenta", "# factura", "Total", "Cancelado", "Saldo"});

        jd_abonar_a_total.modelo_tabla.setRowCount(0);

        for (int i = 0; i < modelo_facturas.getRowCount(); i++) {

            double saldo_fila = 0;
            try {
                saldo_fila = Double.parseDouble(metodos.EliminaCaracteres(modelo_facturas.getValueAt(i, 7).toString(), "."));
            } catch (Exception e) {
            }
            if (modelo_facturas.getValueAt(i, 2).toString().equals("CREDITO") && saldo_fila > 0.009) {

                jd_abonar_a_total.modelo_tabla.addRow(new Object[]{modelo_facturas.getValueAt(i, 0).toString(), modelo_facturas.getValueAt(i, 1).toString(),
                    modelo_facturas.getValueAt(i, 2).toString(), modelo_facturas.getValueAt(i, 3).toString(),
                    modelo_facturas.getValueAt(i, 4).toString(), modelo_facturas.getValueAt(i, 5).toString(),
                    modelo_facturas.getValueAt(i, 6).toString(), modelo_facturas.getValueAt(i, 7).toString()});
            }
        }
        jd_abonar_a_total.jtabla.setModel(jd_abonar_a_total.modelo_tabla);

        jd_abonar_a_total.btn_seleccionar_todas_las_facturas.setVisible(false);
        jd_abonar_a_total.lbl_total_seleccionado.setVisible(false);
        jd_abonar_a_total.lbl_total_titulo.setVisible(false);
        jd_abonar_a_total.clic_en_jbox();
        frm.show();
        jd_abonar_a_total.txt_abono.requestFocus();

    }

    public void AbonarACredito() {
        jd_abonar_a_credito.modelo_facturas.setRowCount(0);
        jd_abonar_a_credito frm = new jd_abonar_a_credito(null, true);
        int fila = jtabla_creditos.getSelectedRow();

        String saldo = "" + jtabla_creditos.getValueAt(fila, 7);

        jd_abonar_a_credito.lbl_cliente_nombre.setText(lbl_cliente_nombre.getText());
        jd_abonar_a_credito.lbl_cedula.setText(lbl_cedula.getText());
        jd_abonar_a_credito.lbl_id_cliente.setText(lbl_id_cliente.getText());
        jd_abonar_a_credito.lbl_total_saldo.setText(saldo);
        jd_abonar_a_credito.btn_imprimir.setEnabled(false);
        jd_abonar_a_credito.btn_editar.setVisible(false);

        String id_credito = "" + jtabla_creditos.getValueAt(fila, 0);

        jd_abonar_a_credito.lbl_id_factura.setText(id_credito);

        ResultSet rs;

        String consulta = "SELECT a.id, a.fecha, a.hora, a.abono, a.id_cabecera, t.nombre as tipo \n"
                + "from abonos a \n"
                + "join abonos_cabeceras ca on a.id_cabecera=ca.id \n"
                + "join tipos_abonos t on ca.id_tipo_abono=t.id \n"
                + "where a.id_credito=" + id_credito + "\n"
                + "ORDER BY a.fecha, CAST(a.hora AS TIME) ASC";

        jd_abonar_a_credito.modelo_facturas.setColumnIdentifiers(new Object[]{"Id", "Fecha registro", "Tipo", "Total", "Id cabecera"});

        rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {

                jd_abonar_a_credito.modelo_facturas.addRow(new Object[]{rs.getString("id"), sdf.format(rs.getDate("fecha")), rs.getString("tipo"),
                    metodos.formateador_dinero().format(rs.getDouble("abono")), rs.getString("id_cabecera")});
            }
            rs.close();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, null, e);
        }
        jd_abonar_a_credito.jtabla_abonos.setModel(jd_abonar_a_credito.modelo_facturas);

        frm.show();
        jd_abonar_a_credito.txt_abono.requestFocus();

    }

    public void VerAbonos_credito() {
        jd_Ver_abonos_a_credito.modelo_facturas.setRowCount(0);
        jd_Ver_abonos_a_credito frm = new jd_Ver_abonos_a_credito(null, true);
        int fila = jtabla_creditos.getSelectedRow();

        String saldo = "" + jtabla_creditos.getValueAt(fila, 7);

        jd_Ver_abonos_a_credito.lbl_cliente_nombre.setText(lbl_cliente_nombre.getText());
        jd_Ver_abonos_a_credito.lbl_cedula.setText(lbl_cedula.getText());
        jd_Ver_abonos_a_credito.lbl_id_cliente.setText(lbl_id_cliente.getText());
        jd_Ver_abonos_a_credito.lbl_total_saldo.setText(saldo);

        String id_credito = "" + jtabla_creditos.getValueAt(fila, 0);

        jd_Ver_abonos_a_credito.lbl_id_factura.setText(id_credito);

        String consulta = "SELECT a.id, a.fecha, a.hora, a.abono, a.id_cabecera, t.nombre as tipo \n"
                + "from abonos a \n"
                + "join abonos_cabeceras ca on a.id_cabecera=ca.id \n"
                + "join tipos_abonos t on ca.id_tipo_abono=t.id \n"
                + "where a.id_credito=" + id_credito + "\n"
                + "ORDER BY a.fecha, CAST(a.hora AS TIME) ASC";

        jd_Ver_abonos_a_credito.modelo_facturas.setColumnIdentifiers(new Object[]{"Id", "Fecha registro", "Tipo", "Total", "Id cabecera"});

        ResultSet rs = DB_consultas_R_D.getTabla(consulta);
        try {
            while (rs.next()) {

                jd_Ver_abonos_a_credito.modelo_facturas.addRow(new Object[]{rs.getString("id"), sdf.format(rs.getDate("fecha")), rs.getString("tipo"),
                    metodos.formateador_dinero().format(rs.getDouble("abono")), rs.getString("id_cabecera")});
            }
            rs.close();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, null, e);
        }
        jd_Ver_abonos_a_credito.jtabla_abonos.setModel(jd_Ver_abonos_a_credito.modelo_facturas);

        frm.show();

    }

    public void ver_credito() {
        jd_ver_credito frm = new jd_ver_credito(null, true);

        int fila = jtabla_creditos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un registro");
        } else {
            String id = "" + jtabla_creditos.getValueAt(fila, 0);

            String consulta = "select f.*,  0 as saldo, c.nombre as contacto_nombre, c.cedula, c.direccion, c.contacto as celular, u.nombre as usuario, \n"
                    + "1 as abono, f.foto, f.pdf \n"
                    + "\n"
                    + "from users u,contactos c, creditos f \n"
                    + "\n"
                    + "where f.id_contacto=c.id and f.id_user=u.id and f.id=" + id;

//            System.out.println(consulta);
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);
            try {
                while (rs.next()) {
                    jd_ver_credito.lbl_id_factura.setText(rs.getString("id"));

                    jd_ver_credito.lbl_cliente.setText(rs.getString("contacto_nombre"));
                    jd_ver_credito.lbl_documento.setText(rs.getString("cedula"));
                    jd_ver_credito.lbl_direccion.setText(rs.getString("direccion"));
                    jd_ver_credito.lbl_telefono.setText(rs.getString("celular"));
                    jd_ver_credito.lbl_codigo.setText(rs.getString("codigo"));
                    jd_ver_credito.lbl_user.setText(rs.getString("usuario"));
                    jd_ver_credito.lbl_total_factura.setText(metodos.formateador_dinero().format(rs.getDouble("total")));
                    jd_ver_credito.lbl_interes.setText(rs.getDouble("interes") + "");
                    jd_ver_credito.lbl_fecha_factura.setText(sdf.format(rs.getDate("fecha_creacion")));
                    jd_ver_credito.lbl_fecha_vencimiento.setText(sdf.format(rs.getDate("fecha_vencimiento")));
                    jd_ver_credito.txt_descripcion.setText(rs.getString("descripcion"));
                    if (rs.getString("foto").equals("")) {
                        jd_ver_credito.btn_imagen.setEnabled(false);
                    }
                    if (rs.getString("pdf").equals("")) {
                        jd_ver_credito.btn_ver_pdf.setEnabled(false);
                    }
                }
                rs.close();

            } catch (SQLException ex) {
                Logger.getLogger(frm_Creditos.class.getName()).log(Level.SEVERE, null, ex);
            }

            frm.cargarAbonosCredito(id);
            frm.show();
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        lbl_cliente_nombre = new javax.swing.JLabel();
        lbl_cedula = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        btn_imprimir_oi = new javax.swing.JButton();
        lbl_id_cliente = new javax.swing.JLabel();
        lbl_titulo1 = new javax.swing.JLabel();
        txt_cupo_aprobado = new javax.swing.JTextField();
        lbl_titulo4 = new javax.swing.JLabel();
        lbl_titulo5 = new javax.swing.JLabel();
        lbl_cupo_disponible = new javax.swing.JLabel();
        lbl_cupo_usado = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        btn_interes = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        btn_abonar = new javax.swing.JButton();
        btn_cruzar = new javax.swing.JButton();
        btn_actualizar = new javax.swing.JButton();
        lbl_cliente1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtabla_creditos = new javax.swing.JTable();
        btn_VerFactura_editar = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        lbl_total_saldo = new javax.swing.JLabel();
        btn_VerFactura1 = new javax.swing.JButton();
        btn_eliminar = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();

        jLabel1.setText("jLabel1");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Abonos");
        setModal(true);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel4.setForeground(new java.awt.Color(51, 51, 51));
        jLabel4.setText("Cliente");

        lbl_cliente_nombre.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_cliente_nombre.setForeground(new java.awt.Color(0, 102, 102));
        lbl_cliente_nombre.setText("Cliente");

        lbl_cedula.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_cedula.setForeground(new java.awt.Color(0, 102, 102));
        lbl_cedula.setText("cedula");

        jLabel6.setForeground(new java.awt.Color(51, 51, 51));
        jLabel6.setText("Cédula");

        btn_imprimir_oi.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_imprimir_oi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/print_pequeno.png"))); // NOI18N
        btn_imprimir_oi.setMnemonic('p');
        btn_imprimir_oi.setText("Imprimir Estado Actual");
        btn_imprimir_oi.setToolTipText("ATL+P");
        btn_imprimir_oi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_imprimir_oiActionPerformed(evt);
            }
        });

        lbl_id_cliente.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_id_cliente.setForeground(new java.awt.Color(255, 255, 242));
        lbl_id_cliente.setText("id_cliente");

        lbl_titulo1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_titulo1.setForeground(new java.awt.Color(45, 54, 76));
        lbl_titulo1.setText("Cupo aprobado");

        txt_cupo_aprobado.setEditable(false);
        txt_cupo_aprobado.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txt_cupo_aprobado.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txt_cupo_aprobadoKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txt_cupo_aprobadoKeyTyped(evt);
            }
        });

        lbl_titulo4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_titulo4.setForeground(new java.awt.Color(255, 0, 0));
        lbl_titulo4.setText("Cupo Usado");

        lbl_titulo5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_titulo5.setForeground(new java.awt.Color(0, 153, 51));
        lbl_titulo5.setText("Cupo Disponible");

        lbl_cupo_disponible.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_cupo_disponible.setForeground(new java.awt.Color(0, 153, 51));
        lbl_cupo_disponible.setText("0");

        lbl_cupo_usado.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbl_cupo_usado.setForeground(new java.awt.Color(255, 0, 0));
        lbl_cupo_usado.setText("0");

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        btn_interes.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        btn_interes.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/gastos.png"))); // NOI18N
        btn_interes.setText("Interes");
        btn_interes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_interesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_cliente_nombre)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_cedula)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_id_cliente))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(lbl_titulo1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txt_cupo_aprobado, javax.swing.GroupLayout.PREFERRED_SIZE, 156, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15)
                        .addComponent(lbl_titulo4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_cupo_usado)
                        .addGap(12, 12, 12)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_titulo5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_cupo_disponible)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btn_interes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btn_imprimir_oi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6)
                        .addComponent(lbl_cedula)
                        .addComponent(lbl_id_cliente))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(lbl_cliente_nombre))
                    .addComponent(btn_imprimir_oi))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lbl_titulo1)
                        .addComponent(lbl_titulo4)
                        .addComponent(lbl_cupo_usado)
                        .addComponent(lbl_titulo5)
                        .addComponent(lbl_cupo_disponible)
                        .addComponent(txt_cupo_aprobado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btn_interes)))
                .addGap(5, 5, 5))
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jPanel2.setBackground(new java.awt.Color(45, 54, 76));

        btn_abonar.setBackground(new java.awt.Color(204, 255, 204));
        btn_abonar.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        btn_abonar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/abonar.png"))); // NOI18N
        btn_abonar.setMnemonic('c');
        btn_abonar.setText("Abonar");
        btn_abonar.setBorder(null);
        btn_abonar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_abonarActionPerformed(evt);
            }
        });

        btn_cruzar.setBackground(new java.awt.Color(255, 229, 204));
        btn_cruzar.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        btn_cruzar.setMnemonic('z');
        btn_cruzar.setText("Cruzar saldo a favor");
        btn_cruzar.setToolTipText("Aplica el saldo a favor de un pago a los creditos pendientes del cliente");
        btn_cruzar.setBorder(null);
        btn_cruzar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_cruzarActionPerformed(evt);
            }
        });

        btn_actualizar.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        btn_actualizar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/actualizar.png"))); // NOI18N
        btn_actualizar.setMnemonic('c');
        btn_actualizar.setText("Actualizar");
        btn_actualizar.setBorder(null);
        btn_actualizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_actualizarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btn_abonar, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btn_cruzar, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(769, 769, 769)
                .addComponent(btn_actualizar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_abonar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_cruzar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_actualizar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        lbl_cliente1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbl_cliente1.setForeground(new java.awt.Color(0, 102, 102));
        lbl_cliente1.setText("Facturas");

        jtabla_creditos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jtabla_creditos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jtabla_creditos);

        btn_VerFactura_editar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_VerFactura_editar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/edit_24px.png"))); // NOI18N
        btn_VerFactura_editar.setText("Editar");
        btn_VerFactura_editar.setBorder(null);
        btn_VerFactura_editar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_VerFactura_editarActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(51, 51, 51));
        jLabel10.setText("Total saldo:");

        lbl_total_saldo.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_total_saldo.setForeground(new java.awt.Color(204, 0, 0));
        lbl_total_saldo.setText("total");

        btn_VerFactura1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_VerFactura1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/eye_24px_1.png"))); // NOI18N
        btn_VerFactura1.setText("Solo pendientes");
        btn_VerFactura1.setBorder(null);
        btn_VerFactura1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_VerFactura1ActionPerformed(evt);
            }
        });

        btn_eliminar.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        btn_eliminar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/shutdown.png"))); // NOI18N
        btn_eliminar.setMnemonic('d');
        btn_eliminar.setText("Eliminar");
        btn_eliminar.setBorder(null);
        btn_eliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_eliminarActionPerformed(evt);
            }
        });

        jButton1.setFont(new java.awt.Font("Yu Gothic Medium", 1, 14)); // NOI18N
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/excel.png"))); // NOI18N
        jButton1.setText("Exportar a excel");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(lbl_cliente1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(473, 473, 473)
                        .addComponent(btn_eliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_VerFactura1, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_VerFactura_editar, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbl_total_saldo)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lbl_cliente1)
                    .addComponent(btn_VerFactura_editar)
                    .addComponent(btn_VerFactura1)
                    .addComponent(btn_eliminar)
                    .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(lbl_total_saldo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void btn_imprimir_oiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_imprimir_oiActionPerformed
        try {

            DefaultTableModel de = (DefaultTableModel) jtabla_creditos.getModel();
            JRTableModelDataSource datasource = new JRTableModelDataSource(de);

            JasperReport report = null;
            Map params = new HashMap();

            params.put("total", "$ " + lbl_total_saldo.getText());

            ResultSet rs = DB_consultas_R_D.getTabla("select * from contactos where id=" + lbl_id_cliente.getText());

            try {
                while (rs.next()) {
                    params.put("nombre", rs.getString("nombre"));
                    params.put("direccion", rs.getString("direccion"));
                    params.put("celular", rs.getString("celular"));
                    params.put("telefono", rs.getString("telefono"));
                    params.put("cedula", rs.getString("cedula"));

                }

                rs.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, null, e);
            }

            params.put("SUBREPORT_DIR", new File("").getAbsolutePath() + "/src/reportes/");

            try {
                try {
                    if (reporteEstadoCuentaCache == null) {
                        String cad = new File("").getAbsolutePath() + "/src/reportes/estado_cuenta.jrxml";
                        reporteEstadoCuentaCache = JasperCompileManager.compileReport(cad);
                    }
                    report = reporteEstadoCuentaCache;
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, e);
                }
                JasperPrint jasperPrint = JasperFillManager.fillReport(report, params, datasource);

                JasperViewer view = new JasperViewer(jasperPrint, false);

                JDialog dialog = new JDialog(this);//the owner
                metodos.addEscapeListenerWindowDialog(dialog);

                dialog.setContentPane(view.getContentPane());
                dialog.setSize(view.getSize());
                dialog.setModal(true);
                dialog.setLocationRelativeTo(this);
                dialog.setTitle("Impresión de servicios");
                dialog.setVisible(true);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, null, e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, null, e);
        }
    }//GEN-LAST:event_btn_imprimir_oiActionPerformed

    private void btn_VerFactura_editarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_VerFactura_editarActionPerformed

        int fila = jtabla_creditos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {

            String id = (String) jtabla_creditos.getValueAt(fila, 0);
            if (DB_consultas_R_D.validar_admin("Creditos", "Editar", "Se edito el registro con el id: " + id)) {

                String tipo = jtabla_creditos.getValueAt(fila, 2).toString();
                if (tipo.equals("CREDITO")) { // CREDITO
                    jif_crear_credito frm = new jif_crear_credito();
                    frm.editar = true;

                    String consulta = "select f.*, c.nombre as contacto, c.id as id_contacto, f.foto, f.pdf, cu.nombre as cuenta, f.id_cuenta "
                            + "from creditos f,contactos c, cuentas cu where f.id_cuenta=cu.id and f.id_contacto=c.id and f.id =" + id;
                    ResultSet rs = DB_consultas_R_D.getTabla(consulta);
                    try {
                        while (rs.next()) {
                            try {

                                if (rs.getString("foto").equals("")) {
                                    frm.btn_Eliminar_Imagen.setEnabled(false);
                                }
                                if (rs.getString("pdf").equals("")) {
                                    frm.btn_Eliminar_PDF.setEnabled(false);
                                }

                                String ruta = DB_consultas_R_D.Ruta_Imagenes() + rs.getString("foto");

                                Image img = new ImageIcon(ruta).getImage();

                                Image newimg = img.getScaledInstance(frm.lbl_foto.getWidth(), frm.lbl_foto.getHeight(), java.awt.Image.SCALE_SMOOTH);
                                ImageIcon newicon = new ImageIcon(newimg);

                                frm.lbl_foto.setIcon(newicon);
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, "No se cargo la imagen", e);
                            }

                            jif_crear_credito.jdate_fecha_creacion.setDate(rs.getDate("fecha_creacion"));
                            jif_crear_credito.jdate_fecha_vencimiento.setDate(rs.getDate("fecha_vencimiento"));
                            jif_crear_credito.txt_codigo.setText(rs.getString("codigo"));
                            jif_crear_credito.txt_descripcion.setText(rs.getString("descripcion"));
                            jif_crear_credito.lbl_id_credito.setText(rs.getString("id"));
                            jif_crear_credito.txt_total.setText(metodos.formateador_dinero().format(rs.getDouble("total")));
                            jif_crear_credito.txt_interes.setText(metodos.formateador_un_decimal().format(rs.getDouble("interes")));
                            jif_crear_credito.factura.setId_contacto(rs.getInt("id_contacto"));
                            jif_crear_credito.nombreArchivoImagen = rs.getString("foto");
                            jif_crear_credito.nombreArchivoPDF = rs.getString("pdf");
                            jif_crear_credito.jbox_Cuentas.setSelectedItem(rs.getString("cuenta"));
                            jif_crear_credito.id_cuenta = (rs.getInt("id_cuenta"));
                            jif_crear_credito.id_cliente = rs.getInt("id_contacto");
                            jif_crear_credito.cbx_contacto.setSelectedItem(rs.getString("contacto"));
                            jif_crear_credito.chk_comisionable.setSelected(rs.getBoolean("comisionable"));
                            // El combo de vendedor se posiciona por id: los nombres
                            // pueden repetirse entre contactos.
                            int idVendedor = rs.getInt("id_empleado");
                            for (int k = 0; k < jif_crear_credito.cbx_vendedor.getItemCount(); k++) {
                                if (jif_crear_credito.cbx_vendedor.getItemAt(k).getId() == idVendedor) {
                                    jif_crear_credito.cbx_vendedor.setSelectedIndex(k);
                                    break;
                                }
                            }

                        }
                        rs.close();

                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, null, ex);
                    }
                    frm.lbl_titulo.setText("Editar factura");

                    jif_crear_credito.btn_guardar.setText("Actualizar");
                    jif_crear_credito.cbx_contacto.setEnabled(true);
                    jif_crear_credito.traer_cupo();
                    jif_crear_credito.tipo = "editar";

                    frm.show();
                } else { // ABONOS (cabecera de pago)
                    jd_abonar_a_total frm = new jd_abonar_a_total(null, true);
                    id = jtabla_creditos.getValueAt(fila, 0).toString();
                    String saldo = jtabla_creditos.getValueAt(fila, 8).toString();
                    cargarAbonoEnDialog(frm, id, saldo, false);
                    cargarAbonosHijosEnDialog(id);
                    jd_abonar_a_total.btn_guardar.setText("Actualizar");
                    jd_abonar_a_total.tipo = "editar";
                    frm.show();
                }

            }
        }
    }//GEN-LAST:event_btn_VerFactura_editarActionPerformed

    private void btn_abonarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_abonarActionPerformed
        AbonarATotal();
    }//GEN-LAST:event_btn_abonarActionPerformed

    private void btn_cruzarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_cruzarActionPerformed
        CruzarAbono();
    }//GEN-LAST:event_btn_cruzarActionPerformed

    /**
     * Aplica el saldo a favor de un pago a los créditos pendientes del cliente.
     *
     * Se trabaja sobre la fila seleccionada en la tabla de movimientos, que
     * debe ser un ABONO (no un CREDITO). El saldo se relee de la base y no de
     * la pantalla: otro usuario pudo haber aplicado parte del abono mientras
     * esta ventana estaba abierta.
     */
    public void CruzarAbono() {
        int fila = jtabla_creditos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione primero la fila del abono que desea cruzar.",
                    "Sin selección", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String tipo = jtabla_creditos.getValueAt(fila, 2).toString();
        if (tipo.equals("CREDITO")) {
            JOptionPane.showMessageDialog(this,
                    "La fila seleccionada es un crédito, no un abono.\n\n"
                    + "Seleccione la fila de un pago (ABONO) que tenga saldo disponible\n"
                    + "y vuelva a pulsar Cruzar saldo a favor.",
                    "Seleccione un abono", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idCabecera;
        try {
            idCabecera = Integer.parseInt(jtabla_creditos.getValueAt(fila, 0).toString());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "No se pudo leer el id del abono seleccionado.",
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double saldoDisponible = Creditos.db.DBabonos.SaldoAFavorCabecera(idCabecera);
        if (saldoDisponible <= 0.009) {
            JOptionPane.showMessageDialog(this,
                    "El abono #" + idCabecera + " no tiene saldo disponible para cruzar.\n\n"
                    + "Su valor ya se aplicó por completo a créditos del cliente.",
                    "Sin saldo por cruzar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        jd_cruzar_abono dlg = new jd_cruzar_abono(this, idCabecera,
                Integer.parseInt(id_cliente), saldoDisponible, lbl_cliente_nombre.getText());
        dlg.setVisible(true);

        if (dlg.isCruceAplicado()) {
            btn_actualizar.doClick();
        }
    }

    private void btn_actualizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_actualizarActionPerformed
        actualizar(false);
    }//GEN-LAST:event_btn_actualizarActionPerformed

    private void btn_VerFactura1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_VerFactura1ActionPerformed
        actualizar(true);
    }//GEN-LAST:event_btn_VerFactura1ActionPerformed

    private void btn_eliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_eliminarActionPerformed

        int fila = jtabla_creditos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un registro");
        } else {
            String tipo = jtabla_creditos.getValueAt(fila, 2).toString();
            String id = (String) jtabla_creditos.getValueAt(fila, 0);//suponiendo que el id lo muestras en la primera columna
            if (DB_consultas_R_D.validar_admin("Creditos", "Eliminar", "Se elimino el registro con el id: " + id)) {

                if (tipo.equals("CREDITO")) {
                    int dialogButton = JOptionPane.YES_NO_OPTION;
                    int dialogResult = JOptionPane.showConfirmDialog(null, "Desea eliminar este credito?\n"
                            + "Los abonos que se le aplicaron quedarán como saldo a favor de sus pagos", "Alerta", dialogButton);
                    if (dialogResult == JOptionPane.YES_OPTION) {
                        try {
                            // primero se libera el detalle de abonos aplicado a la factura
                            // (el dinero vuelve como saldo a favor de sus cabeceras)
                            DB_consultas_R_D.getTabla("delete from abonos where id_credito=" + id + " returning id");
                            DB_consultas_R_D.eliminar("creditos", id);
                            btn_actualizar.doClick();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    int dialogButton = JOptionPane.YES_NO_OPTION;
                    int dialogResult = JOptionPane.showConfirmDialog(null, "¿Desea eliminar este pago?\n"
                            + "Se eliminará la cabecera con todo su detalle de abonos", "Alerta", dialogButton);
                    if (dialogResult == JOptionPane.YES_OPTION) {
                        DBabonos dbabonos = new DBabonos();
                        if (dbabonos.EliminarPago(Integer.parseInt(id))) {
                            JOptionPane.showMessageDialog(this, "Se eliminó el pago con todo su detalle");
                        }
                        btn_actualizar.doClick();
                    }
                }
            }

        }
    }//GEN-LAST:event_btn_eliminarActionPerformed

    private void txt_cupo_aprobadoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_cupo_aprobadoKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            String sql = "update contactos set "
                    + "cupo=" + metodos.EliminaCaracteres(txt_cupo_aprobado.getText(), ".") + " "
                    + "where id=" + id_cliente;

            try (Connection con = DB_consultas_R_D.getConexion(); PreparedStatement psql = con.prepareStatement(sql)) {

                psql.executeUpdate();

                txt_cupo_aprobado.setEditable(false);
                JOptionPane.showMessageDialog(this, "Se actualizo el cupo para este cliente");
                traer_cupo();

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, null, e);
            }
        }
    }//GEN-LAST:event_txt_cupo_aprobadoKeyPressed

    private void txt_cupo_aprobadoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txt_cupo_aprobadoKeyTyped
        char num = evt.getKeyChar();
        DB_consultas_R_D.validar_numeros(evt, num);
    }//GEN-LAST:event_txt_cupo_aprobadoKeyTyped

    private void btn_interesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_interesActionPerformed
        jd_Calcular_interes_cliente j = new jd_Calcular_interes_cliente(null, true);

        String consulta = sqlInteresCliente(lbl_cedula.getText());
        ResultSet rs = DB_consultas_R_D.getTabla(consulta);

        try {
            while (rs.next()) {
                j.lbl_Estado.setText(rs.getString("estado"));
                j.lbl_Cedula.setText(rs.getString("cedula"));
                j.txt_Dinero_adeudado.setText(metodos.formateador_dinero().format(rs.getDouble("saldo")));
                j.lbl_Porcentaje_interes.setText("" + rs.getDouble("porcentaje"));
                j.lbl_InteresMensual.setText(metodos.formateador_dinero().format(rs.getDouble("interes")));
                j.lbl_Dias_vencidosFactura.setText(metodos.formateador_dinero().format(rs.getDouble("dias_vencidos_ultima_factura")));
                j.lbl_Dias_vencidosAbono.setText(metodos.formateador_dinero().format(rs.getDouble("dias_vencidos_ultimo_abono")));
                j.lbl_InteresCalculado.setText(metodos.formateador_dinero().format(rs.getDouble("interes_calculado")));
                j.lbl_InteresMasCapital.setText(metodos.formateador_dinero().format(rs.getDouble("interes_calculado") + rs.getDouble("saldo")));
            }
            rs.close();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, null, e);
        }

        j.show();

    }//GEN-LAST:event_btn_interesActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        ExportarExcel obj;

        try {
            obj = new ExportarExcel();
            obj.exportarExcel(jtabla_creditos);
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * Movimientos del cliente: facturas (CREDITO) y cabeceras de pagos, en
     * orden cronológico. La columna deuda_actual es el acumulado: cada factura
     * suma su saldo y cada cabecera resta su saldo a favor (total - aplicado).
     * Por defecto se muestra TODO el historial; con soloPendientes=true solo
     * se listan facturas con saldo pendiente y cabeceras con saldo a favor.
     */
    private static String sqlMovimientosCliente(String idCliente, boolean soloPendientes) {
        String filtro = soloPendientes ? "WHERE saldo > 0.009 OR saldo_favor > 0.009 \n" : "";
        return "WITH consulta AS (\n"
                + "  SELECT \n"
                + "    f.id, \n"
                + "    f.codigo, \n"
                + "    f.total, \n"
                + "    COALESCE(SUM(a.abono), 0) AS abono, \n"
                + "    (f.total - COALESCE(SUM(a.abono),0)) AS saldo, \n"
                + "    0 AS saldo_favor, \n"
                + "    f.fecha_creacion, \n"
                + "    f.hora, \n"
                + "    'CREDITO' AS tipo, \n"
                + "    c.nombre AS cuenta \n"
                + "  FROM creditos f \n"
                + "    JOIN cuentas c ON f.id_cuenta = c.id \n"
                + "    LEFT JOIN abonos a ON a.id_credito = f.id \n"
                + "  WHERE f.id_contacto = " + idCliente + " \n"
                + "  GROUP BY f.id, f.codigo, f.total, f.fecha_creacion, f.hora, c.nombre \n"
                + "\n"
                + "  UNION ALL\n"
                + "\n"
                + "  SELECT \n"
                + "    ca.id, \n"
                + "    '-' AS codigo, \n"
                + "    ca.total, \n"
                + "    COALESCE(SUM(a.abono),0) AS abono, \n"
                + "    0 AS saldo, \n"
                + "    (ca.total - COALESCE(SUM(a.abono),0)) AS saldo_favor, \n"
                + "    ca.fecha AS fecha_creacion, \n"
                + "    ca.hora, \n"
                + "    t.nombre AS tipo, \n"
                + "    '-' AS cuenta \n"
                + "  FROM abonos_cabeceras ca \n"
                + "    JOIN tipos_abonos t ON ca.id_tipo_abono = t.id \n"
                + "    LEFT JOIN abonos a ON a.id_cabecera = ca.id \n"
                + "  WHERE ca.id_contacto = " + idCliente + " \n"
                + "  GROUP BY ca.id, ca.total, ca.fecha, ca.hora, t.nombre \n"
                + ")\n"
                + "SELECT *, \n"
                + "  SUM(saldo - saldo_favor) OVER (ORDER BY fecha_creacion, CAST(hora AS TIME) ASC, tipo DESC, id) AS deuda_actual \n"
                + "FROM consulta \n"
                + filtro
                + "ORDER BY fecha_creacion, CAST(hora AS TIME) ASC, tipo DESC, id;";
    }

    private static String sqlCupoCliente(String idCliente) {
        return "WITH sum_facturas AS (\n"
                + "    SELECT id_contacto, SUM(total) AS total_facturas\n"
                + "    FROM creditos\n"
                + "    GROUP BY id_contacto\n"
                + "),\n"
                + "sum_abonos AS (\n"
                + "    SELECT id_contacto, SUM(total) AS total_abonos\n"
                + "    FROM abonos_cabeceras\n"
                + "    GROUP BY id_contacto\n"
                + ")\n"
                + "SELECT\n"
                + "    c.id AS id_cliente,\n"
                + "    c.nombre,\n"
                + "    c.cupo,\n"
                + "    COALESCE(sf.total_facturas, 0) AS creditos,\n"
                + "    (COALESCE(sf.total_facturas, 0) - COALESCE(sa.total_abonos, 0)) AS debe,\n"
                + "    (\n"
                + "        c.cupo\n"
                + "        - (COALESCE(sf.total_facturas, 0) - COALESCE(sa.total_abonos, 0))\n"
                + "    ) AS cupo_disponible\n"
                + "FROM contactos c\n"
                + "LEFT JOIN sum_facturas sf ON sf.id_contacto = c.id\n"
                + "LEFT JOIN sum_abonos sa ON sa.id_contacto = c.id\n"
                + "WHERE c.id = " + idCliente + "\n"
                + "ORDER BY c.nombre;";
    }

    private static String sqlInteresCliente(String cedula) {
        return "with resultado as (\n"
                + "	with consulta as (\n"
                + "		select c.id as id_cliente, c.nombre, c.cedula, c.contacto as celular, c.interes, sum(f.total) as total, \n"
                + "		(select max(fecha_vencimiento) from creditos where id_contacto=c.id) as ultima_factura, (select max(fecha) from abonos_cabeceras where id_contacto=c.id) as ultimo_abono,\n"
                + "		coalesce((select sum(total) from abonos_cabeceras where id_contacto=c.id and fecha < current_date),0) as abonos, \n"
                + "		(sum(f.total) - coalesce((select sum(total) from abonos_cabeceras where id_contacto=c.id and fecha < current_date),0)) as saldo \n"
                + "\n"
                + "		from creditos f, contactos c \n"
                + "\n"
                + "		where f.id_contacto=c.id and f.fecha_creacion < current_date and c.cedula='" + cedula + "'\n"
                + "\n"
                + "		group by c.id, c.nombre\n"
                + "\n"
                + "		order by c.nombre\n"
                + "	)\n"
                + "	select c.id as id_cliente, c.antiguo,  c.nombre, c.cedula, c.contacto as celular, co.ultima_factura, coalesce(co.ultimo_abono||'','No abonado') as ultimo_abono, \n"
                + "	(co.ultima_factura-current_date)*-1 as dias_vencidos_ultima_factura, \n"
                + "	(+coalesce(co.ultimo_abono-current_date,0))*-1 as dias_vencidos_ultimo_abono, \n"
                + "	c.interes as porcentaje, co.saldo, (co.saldo*(c.interes/100)) as interes\n"
                + "	from contactos c left join consulta co on co.id_cliente=c.id\n"
                + "	where saldo != 0 and c.cedula='" + cedula + "'\n"
                + "	order by c.nombre\n"
                + ")\n"
                + "select r.*, \n"
                + "case \n"
                + "	when dias_vencidos_ultima_factura <= 0 \n"
                + "	then 'Vigente' \n"
                + "	else \n"
                + "		case \n"
                + "			when dias_vencidos_ultima_factura > dias_vencidos_ultimo_abono \n"
                + "			then \n"
                + "				case \n"
                + "					when dias_vencidos_ultimo_abono = 0\n"
                + "					then 'se calcula con dias de factura' \n"
                + "					else 'se calcula con dias de abono' \n"
                + "				end\n"
                + "			else 'se calcula con dias de factura' \n"
                + "		end\n"
                + "\n"
                + "end as estado,\n"
                + "\n"
                + "case \n"
                + "	when dias_vencidos_ultima_factura <= 0 \n"
                + "	then 0 \n"
                + "	else \n"
                + "		case \n"
                + "			when dias_vencidos_ultima_factura > dias_vencidos_ultimo_abono \n"
                + "			then \n"
                + "				case \n"
                + "					when dias_vencidos_ultimo_abono = 0\n"
                + "					then (interes/30)* dias_vencidos_ultima_factura\n"
                + "					else (interes/30)*  dias_vencidos_ultimo_abono\n"
                + "				end\n"
                + "			else (interes/30)* dias_vencidos_ultima_factura\n"
                + "		end\n"
                + "\n"
                + "end as interes_calculado\n"
                + "\n"
                + "from resultado r;";
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_VerFactura1;
    private javax.swing.JButton btn_VerFactura_editar;
    private javax.swing.JButton btn_abonar;
    private javax.swing.JButton btn_cruzar;
    public static javax.swing.JButton btn_actualizar;
    private javax.swing.JButton btn_eliminar;
    private javax.swing.JButton btn_imprimir_oi;
    public static javax.swing.JButton btn_interes;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator2;
    public static javax.swing.JTable jtabla_creditos;
    public static javax.swing.JLabel lbl_cedula;
    public static javax.swing.JLabel lbl_cliente1;
    public static javax.swing.JLabel lbl_cliente_nombre;
    public static javax.swing.JLabel lbl_cupo_disponible;
    public static javax.swing.JLabel lbl_cupo_usado;
    public static javax.swing.JLabel lbl_id_cliente;
    public static javax.swing.JLabel lbl_titulo1;
    public static javax.swing.JLabel lbl_titulo4;
    public static javax.swing.JLabel lbl_titulo5;
    public static javax.swing.JLabel lbl_total_saldo;
    public static javax.swing.JTextField txt_cupo_aprobado;
    // End of variables declaration//GEN-END:variables
}
