package Creditos;

import Creditos.db.DBabonos;
import Creditos.db.DB_Porcentajes_comision;
import Creditos.modelos.Porcentajes_comision;
import Metodos.EstiloCompras;
import Metodos.FontAwesome;
import Metodos.metodos;
import conexiondb.AuditoriaCaja;
import conexiondb.DB_consultas_R_D;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Comisiones de vendedor sobre los abonos a crédito.
 *
 * Qué entra al cálculo:
 * <ul>
 * <li>Abonos aplicados a un crédito, cuando el tipo de abono es comisionable y
 * la comisión todavía no se ha liquidado. El vendedor es el del crédito.</li>
 * <li>Anticipos comisionables que todavía no se aplicaron a ningún crédito. No
 * cuelgan de un crédito, así que el vendedor sale del propio cliente.</li>
 * </ul>
 *
 * Cómo se calcula: base = abono sin IVA (se usa configuraciones.iva); la escala
 * de porcentajes_comision da el porcentaje según los días que tardó el cobro
 * (fecha del abono menos fecha del crédito). Un crédito marcado como NO
 * comisionable aparece con comisión cero para que se vea, no se esconde.
 *
 * "Liquidar" marca las filas mostradas como comisión pagada: dejan de aparecer
 * en las siguientes consultas.
 *
 * @author Monkeyelgrande
 */
public class jd_Comisiones extends JDialog {

    // Columnas del detalle
    private static final int D_ORIGEN = 0;   // "detalle" | "cabecera"
    private static final int D_ID = 1;       // id de abonos / abonos_cabeceras
    private static final int D_VENDEDOR = 2;
    private static final int D_CREDITO = 3;
    private static final int D_FECHA = 4;
    private static final int D_ABONO = 5;
    private static final int D_DIAS = 6;
    private static final int D_PORCENTAJE = 7;
    private static final int D_COMISION = 8;
    private static final int D_NOTA = 9;

    private com.toedter.calendar.JDateChooser jdate_desde;
    private com.toedter.calendar.JDateChooser jdate_hasta;
    private DefaultTableModel modeloGeneral;
    private DefaultTableModel modeloDetalle;
    private JTable tablaGeneral;
    private JTable tablaDetalle;
    private JLabel lblTotal;
    private JLabel lblAviso;

    /** IVA con el que se descuenta el impuesto del abono antes de comisionar. */
    private final double iva = ivaConfigurado();

    public jd_Comisiones(Frame parent, boolean modal) {
        super(parent, "Comisiones de vendedores", modal);
        construir();
        ponerFechasDelMes();
        consultar();
        setLocationRelativeTo(parent);
    }

    // ================================================================
    // Interfaz
    // ================================================================
    private void construir() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(1180, 700);
        setLayout(new BorderLayout());
        getContentPane().setBackground(EstiloCompras.BG_FORM);

        add(EstiloCompras.header(FontAwesome.BALANCE, "Comisiones de vendedores", new Runnable() {
            @Override
            public void run() {
                dispose();
            }
        }), BorderLayout.NORTH);

        // ---------- filtros ----------
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filtros.setBackground(EstiloCompras.BG_FORM);

        jdate_desde = new com.toedter.calendar.JDateChooser();
        jdate_desde.setPreferredSize(new Dimension(160, 34));
        jdate_hasta = new com.toedter.calendar.JDateChooser();
        jdate_hasta.setPreferredSize(new Dimension(160, 34));

        JButton btnConsultar = EstiloCompras.primaryBtn("Consultar", FontAwesome.SEARCH);
        btnConsultar.addActionListener(e -> consultar());

        JButton btnLiquidar = EstiloCompras.successBtn("Liquidar lo mostrado", FontAwesome.CHECK);
        btnLiquidar.addActionListener(e -> liquidar());

        filtros.add(etiqueta("Desde"));
        filtros.add(jdate_desde);
        filtros.add(etiqueta("Hasta"));
        filtros.add(jdate_hasta);
        filtros.add(btnConsultar);
        filtros.add(btnLiquidar);

        // ---------- tablas ----------
        modeloGeneral = new DefaultTableModel(
                new Object[]{"id", "Vendedor", "Total abonado", "Comisión"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaGeneral = new JTable(modeloGeneral);
        EstiloCompras.styleTable(tablaGeneral);
        tablaGeneral.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        EstiloCompras.anchoColumnas(tablaGeneral, 60, 320, 180, 180);

        modeloDetalle = new DefaultTableModel(
                new Object[]{"origen", "id", "Vendedor", "Crédito", "Fecha abono", "Abono",
                    "Días", "%", "Comisión", "Nota"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaDetalle = new JTable(modeloDetalle);
        EstiloCompras.styleTable(tablaDetalle);
        tablaDetalle.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        EstiloCompras.anchoColumnas(tablaDetalle, 0, 0, 220, 130, 110, 130, 60, 60, 130, 260);
        EstiloCompras.ocultarColumna(tablaDetalle, D_ORIGEN);
        EstiloCompras.ocultarColumna(tablaDetalle, D_ID);

        JPanel tablas = new JPanel(new GridLayout(2, 1, 0, 12));
        tablas.setBackground(EstiloCompras.BG_FORM);
        tablas.setBorder(new EmptyBorder(4, 20, 4, 20));
        tablas.add(conTitulo("Resumen por vendedor", EstiloCompras.scroll(tablaGeneral)));
        tablas.add(conTitulo("Detalle de abonos que comisionan", EstiloCompras.scroll(tablaDetalle)));

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(EstiloCompras.BG_FORM);
        centro.add(filtros, BorderLayout.NORTH);
        centro.add(tablas, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);

        // ---------- pie ----------
        JPanel sur = new JPanel(new BorderLayout());
        sur.setBackground(EstiloCompras.BG_FORM);
        sur.setBorder(new EmptyBorder(0, 20, 16, 20));

        lblTotal = new JLabel(" ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTotal.setForeground(EstiloCompras.PRIMARY);
        sur.add(lblTotal, BorderLayout.WEST);

        lblAviso = new JLabel(" ");
        lblAviso.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAviso.setForeground(EstiloCompras.DANGER);
        sur.add(lblAviso, BorderLayout.SOUTH);

        add(sur, BorderLayout.SOUTH);
        metodos.addEscapeListenerWindowDialog(this);
    }

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(EstiloCompras.TEXT_PRIMARY);
        return l;
    }

    private JPanel conTitulo(String titulo, java.awt.Component c) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(EstiloCompras.BG_FORM);
        p.add(EstiloCompras.sectionTitle(titulo), BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private void ponerFechasDelMes() {
        Calendar c = new GregorianCalendar();
        c.set(Calendar.DAY_OF_MONTH, 1);
        jdate_desde.setCalendar(c);
        jdate_hasta.setCalendar(new GregorianCalendar());
    }

    // ================================================================
    // Consulta
    // ================================================================
    /**
     * IVA configurado en el negocio. Sirve para descontar el impuesto del abono
     * antes de comisionar: la comisión se paga sobre lo que realmente factura la
     * empresa. Si no hay configuración se asume 0 (no se descuenta nada).
     */
    private static double ivaConfigurado() {
        try (ResultSet rs = DB_consultas_R_D.getTabla("select coalesce(iva,0) as iva from configuraciones order by id limit 1")) {
            if (rs.next()) {
                return rs.getDouble("iva");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return 0;
    }

    private String fecha(com.toedter.calendar.JDateChooser d) {
        if (d.getDate() == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(d.getDate());
    }

    private void consultar() {
        String desde = fecha(jdate_desde);
        String hasta = fecha(jdate_hasta);
        if (desde == null || hasta == null) {
            lblAviso.setText("Indique el rango de fechas.");
            return;
        }

        List<Porcentajes_comision> escala = DB_Porcentajes_comision.listar();
        modeloGeneral.setRowCount(0);
        modeloDetalle.setRowCount(0);

        Map<Integer, String> nombrePorVendedor = new LinkedHashMap<>();
        Map<Integer, Double> abonosPorVendedor = new LinkedHashMap<>();
        Map<Integer, Double> comisionPorVendedor = new LinkedHashMap<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        double totalComision = 0;

        try (ResultSet rs = DB_consultas_R_D.getTabla(sqlComisiones(desde, hasta))) {
            while (rs.next()) {
                int idVendedor = rs.getInt("id_vendedor");
                String vendedor = rs.getString("vendedor");
                double valorAbono = rs.getDouble("valor_abono");
                boolean esAnticipo = rs.getInt("anticipo") == 1;
                boolean comisionable = rs.getBoolean("comisionable");

                int dias = rs.getInt("dias_cobro");
                if (esAnticipo) {
                    dias = 0;          // el anticipo entra antes de la deuda
                } else if (dias < 0) {
                    dias = 0;          // abono con fecha anterior al credito
                }

                double porcentaje = comisionable
                        ? DB_Porcentajes_comision.porcentajePorDias(escala, dias, esAnticipo) : 0;
                double base = valorAbono / (1 + (iva / 100.0));
                double comision = base * (porcentaje / 100.0);

                String nota;
                if (!comisionable) {
                    nota = "El crédito está marcado como NO comisionable";
                } else if (escala.isEmpty()) {
                    nota = "No hay escala de porcentajes configurada";
                } else if (esAnticipo) {
                    nota = "Anticipo sin aplicar: toma el primer escalón";
                } else {
                    nota = rs.getString("observacion") == null ? "" : rs.getString("observacion");
                }

                modeloDetalle.addRow(new Object[]{
                    rs.getString("origen"),
                    String.valueOf(rs.getInt("id_registro")),
                    vendedor == null ? "(sin vendedor)" : vendedor,
                    rs.getString("codigo_credito") == null ? "-" : rs.getString("codigo_credito"),
                    rs.getDate("fecha_abono") == null ? "-" : sdf.format(rs.getDate("fecha_abono")),
                    metodos.formateador_dinero().format(valorAbono),
                    String.valueOf(dias),
                    metodos.formateador_un_decimal().format(porcentaje),
                    metodos.formateador_dinero().format(comision),
                    nota});

                nombrePorVendedor.put(idVendedor, vendedor == null ? "(sin vendedor)" : vendedor);
                abonosPorVendedor.put(idVendedor,
                        abonosPorVendedor.getOrDefault(idVendedor, 0.0) + valorAbono);
                comisionPorVendedor.put(idVendedor,
                        comisionPorVendedor.getOrDefault(idVendedor, 0.0) + comision);
                totalComision += comision;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se pudieron calcular las comisiones:\n" + e,
                    "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (Map.Entry<Integer, Double> e : abonosPorVendedor.entrySet()) {
            modeloGeneral.addRow(new Object[]{
                String.valueOf(e.getKey()),
                nombrePorVendedor.get(e.getKey()),
                metodos.formateador_dinero().format(e.getValue()),
                metodos.formateador_dinero().format(comisionPorVendedor.getOrDefault(e.getKey(), 0.0))});
        }

        lblTotal.setText("Comisión total del periodo:  $ " + metodos.formateador_dinero().format(totalComision)
                + "     (base sin IVA del " + metodos.formateador_un_decimal().format(iva) + "%)");

        if (modeloDetalle.getRowCount() == 0) {
            lblAviso.setText("No hay abonos pendientes de comisionar en ese rango. "
                    + "Revise que los tipos de abono estén marcados como comisionables.");
        } else if (escala.isEmpty()) {
            lblAviso.setText("La escala de porcentajes está vacía: configúrela en Créditos > Porcentajes de comisión.");
        } else {
            lblAviso.setText(" ");
        }
    }

    /**
     * Une las dos fuentes de comisión: abonos aplicados a créditos y anticipos
     * comisionables que todavía no se aplicaron a ninguno.
     */
    private static String sqlComisiones(String desde, String hasta) {
        return "SELECT 'detalle' AS origen, \n"
                + "       a.id AS id_registro, \n"
                + "       c.id_empleado AS id_vendedor, \n"
                + "       e.nombre AS vendedor, \n"
                + "       c.codigo AS codigo_credito, \n"
                + "       a.abono AS valor_abono, \n"
                + "       a.fecha AS fecha_abono, \n"
                + "       (a.fecha::date - c.fecha_creacion::date) AS dias_cobro, \n"
                + "       ta.anticipo, \n"
                + "       COALESCE(c.comisionable, TRUE) AS comisionable, \n"
                + "       ca.observacion \n"
                + "  FROM abonos a \n"
                + "  JOIN abonos_cabeceras ca ON ca.id = a.id_cabecera \n"
                + "  JOIN creditos c ON c.id = a.id_credito \n"
                + "  JOIN tipos_abonos ta ON ta.id = ca.id_tipo_abono \n"
                + "  LEFT JOIN contactos e ON e.id = c.id_empleado \n"
                + " WHERE a.fecha BETWEEN '" + desde + "' AND '" + hasta + "' \n"
                + "   AND COALESCE(ta.comisionable, 0) = 1 \n"
                + "   AND COALESCE(a.comision_pagada, 0) = 0 \n"
                + "   AND COALESCE(ta.anticipo, 0) = 0 \n"
                + "\n"
                + "UNION ALL \n"
                + "\n"
                + "SELECT 'cabecera' AS origen, \n"
                + "       ca.id AS id_registro, \n"
                + "       co.id AS id_vendedor, \n"
                + "       co.nombre AS vendedor, \n"
                + "       NULL AS codigo_credito, \n"
                + "       ca.total AS valor_abono, \n"
                + "       ca.fecha AS fecha_abono, \n"
                + "       0 AS dias_cobro, \n"
                + "       ta.anticipo, \n"
                + "       TRUE AS comisionable, \n"
                + "       ca.observacion \n"
                + "  FROM abonos_cabeceras ca \n"
                + "  JOIN tipos_abonos ta ON ta.id = ca.id_tipo_abono \n"
                + "  LEFT JOIN contactos co ON co.id = ca.id_contacto \n"
                + " WHERE ca.fecha BETWEEN '" + desde + "' AND '" + hasta + "' \n"
                + "   AND COALESCE(ta.comisionable, 0) = 1 \n"
                + "   AND COALESCE(ca.comision_pagada, 0) = 0 \n"
                + "   AND COALESCE(ta.anticipo, 0) = 1 \n"
                + "   AND NOT EXISTS (SELECT 1 FROM abonos a2 WHERE a2.id_cabecera = ca.id) \n"
                + "\n"
                + "ORDER BY vendedor, fecha_abono";
    }

    // ================================================================
    // Liquidacion
    // ================================================================
    /**
     * Marca como pagadas las comisiones de todas las filas del detalle. Es la
     * accion que hace que dejen de aparecer en la proxima consulta, asi que se
     * confirma con el total a la vista.
     */
    private void liquidar() {
        if (modeloDetalle.getRowCount() == 0) {
            lblAviso.setText("No hay nada que liquidar.");
            return;
        }

        List<Integer> idsDetalle = new ArrayList<>();
        List<Integer> idsCabecera = new ArrayList<>();
        for (int i = 0; i < modeloDetalle.getRowCount(); i++) {
            int id = Integer.parseInt(modeloDetalle.getValueAt(i, D_ID).toString());
            if ("cabecera".equals(modeloDetalle.getValueAt(i, D_ORIGEN))) {
                idsCabecera.add(id);
            } else {
                idsDetalle.add(id);
            }
        }

        int r = JOptionPane.showConfirmDialog(this,
                "Se marcarán como PAGADAS las comisiones de " + modeloDetalle.getRowCount()
                + " abono(s).\n\n" + lblTotal.getText() + "\n\n"
                + "Dejarán de aparecer en este reporte. ¿Desea continuar?",
                "Liquidar comisiones", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }

        AuditoriaCaja.setOrigen("Creditos - liquidar comisiones");
        int filas;
        try {
            filas = new DBabonos().MarcarComisionesPagadas(idsDetalle, idsCabecera);
        } finally {
            AuditoriaCaja.limpiar();
        }
        if (filas < 0) {
            return;
        }

        JOptionPane.showMessageDialog(this, "Se liquidaron " + filas + " comisión(es).",
                "Comisiones liquidadas", JOptionPane.INFORMATION_MESSAGE);
        consultar();
    }
}
