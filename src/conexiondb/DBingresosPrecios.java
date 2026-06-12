package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import modelos.IngresosProductos;

/**
 * DAO de la cabecera de ingresos del modulo Precios (tabla
 * ingresos_productos_cabecera). Portado de DBingresosProductos de
 * productos-agroinsumos.
 *
 * @author Monkeyelgrande
 */
public class DBingresosPrecios {

    /**
     * Inserta la cabecera recalculando MAX(id)+1 justo antes del INSERT y
     * reintentando si otro usuario nos gano el ID (clave duplicada). Devuelve
     * el id realmente persistido o 0 si no se pudo guardar tras
     * {@code maxIntentos}.
     */
    public int GuardarConReintento(IngresosProductos ingreso, int maxIntentos) {
        for (int intento = 0; intento < maxIntentos; intento++) {
            int idCandidato;
            try {
                idCandidato = Integer.parseInt(DB_consultas_R_D.cargarId("ingresos_productos_cabecera"));
            } catch (NumberFormatException nfe) {
                idCandidato = 1;
            }
            ingreso.setId(idCandidato);

            String SSQL = "INSERT INTO ingresos_productos_cabecera (id,id_proveedor,id_transportador,id_user, fecha, hora, no_factura, estado, observacion) "
                    + "VALUES (" + ingreso.getId() + "," + ingreso.getId_proveedor() + "," + ingreso.getId_transportador() + "," + ingreso.getId_user() + ","
                    + ingreso.getFecha_ingreso() + "," + ingreso.getHora() + ",'" + ingreso.getNo_factura() + "'," + ingreso.getEstado() + ",'" + ingreso.getObservacion() + "')";

            Connection con = null;
            PreparedStatement psql = null;
            try {
                con = DB_consultas_R_D.getConexion();
                psql = con.prepareStatement(SSQL);
                psql.executeUpdate();
                return idCandidato;
            } catch (SQLException e) {
                String sqlState = e.getSQLState();
                boolean esDuplicado = (sqlState != null && sqlState.startsWith("23"));
                if (esDuplicado && intento < maxIntentos - 1) {
                    continue;
                }
                JOptionPane.showMessageDialog(null, "Error al intentar almacenar la información:\n"
                        + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
                return 0;
            } finally {
                try {
                    if (psql != null) {
                        psql.close();
                    }
                } catch (SQLException ignored) {
                }
                try {
                    if (con != null) {
                        con.close();
                    }
                } catch (SQLException ignored) {
                }
            }
        }
        return 0;
    }

    /**
     * Abre un ingreso en modo lectura/edicion simple (4 columnas). Portado del
     * ver_ingreso_productos de agro; lo usan los dialogos de World Office.
     */
    public static void ver_ingreso_productos(String ver_editar, String id_ingreso) {

        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator('.');
        DecimalFormat formateador = new DecimalFormat("####.##", simbolos);
        ResultSet rs = DB_consultas_R_D.getTabla("select i.id, i.no_factura, i.id_transportador, tra.nombre as transportador, "
                + "p.nombre as proveedor,p.id as id_proveedor, i.total, i.fecha "
                + "from ingresos_productos_cabecera i, contactos p, contactos tra "
                + "where i.id_proveedor=p.id and i.id_transportador=tra.id and i.id=" + id_ingreso);
        Precios.jif_crear_ingreso_precios frm = new Precios.jif_crear_ingreso_precios();
        Precios.jif_crear_ingreso_precios.es_nuevo = false;
        Precios.jif_crear_ingreso_precios.lbl_id.setText(id_ingreso);

        try {
            while (rs.next()) {
                Precios.jif_crear_ingreso_precios.lbl_id.setText(rs.getString("id"));
                Precios.jif_crear_ingreso_precios.jdate_fecha_entrada.setDate(rs.getDate("fecha"));
                Precios.jif_crear_ingreso_precios.jbox_proveedor.setSelectedItem(rs.getString("proveedor"));
                Precios.jif_crear_ingreso_precios.jbox_transportador.setSelectedItem(rs.getString("transportador"));
                Precios.jif_crear_ingreso_precios.id_proveedor = (rs.getInt("id_proveedor"));
                Precios.jif_crear_ingreso_precios.id_transportador = (rs.getInt("id_transportador"));
                Precios.jif_crear_ingreso_precios.txt_no_factura.setText(rs.getString("no_factura"));
            }

            rs.close();
        } catch (SQLException ex) {
            System.out.println(ex);
        }

        rs = DB_consultas_R_D.getTabla("select p.id,p.codigo_barras,p.descripcion,i.cantidad "
                + "from ingresos_productos_detalle i, productos p, ingresos_productos_cabecera ic "
                + "where i.id_producto=p.id and i.id_ingreso_cabecera=ic.id and ic.id=" + id_ingreso + " order by id");

        DefaultTableModel modelof = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return columna == 3; // solo cantidad
            }

            @Override
            public void setValueAt(Object aValue, int row, int col) {
                super.setValueAt(aValue, row, col);
                fireTableDataChanged();
            }
        };

        modelof.setColumnIdentifiers(new Object[]{"ID", "CODIGO", "DESCRIPCIÓN", "CANTIDAD"});
        try {
            while (rs.next()) {
                modelof.addRow(new Object[]{rs.getString("id"), rs.getString("codigo_barras"), rs.getString("descripcion"),
                    formateador.format(rs.getDouble("cantidad"))});
            }
            rs.close();
            Precios.jif_crear_ingreso_precios.modelo_productos = modelof;
            Precios.jif_crear_ingreso_precios.jtabla.setModel(modelof);
        } catch (SQLException ex) {
            System.out.println(ex);
        }

        if (ver_editar.equals("ver")) {
            Precios.jif_crear_ingreso_precios.jbox_proveedor.setEnabled(false);
            Precios.jif_crear_ingreso_precios.jbox_transportador.setEnabled(false);
            Precios.jif_crear_ingreso_precios.txt_no_factura.setEnabled(false);
            Precios.jif_crear_ingreso_precios.txt_codigo_barras.setEnabled(false);
            Precios.jif_crear_ingreso_precios.btn_buscar.setEnabled(false);
            Precios.jif_crear_ingreso_precios.jdate_fecha_entrada.setEnabled(false);
            Precios.jif_crear_ingreso_precios.btn_guardar.setEnabled(false);
            Precios.jif_crear_ingreso_precios.btn_limpiar.setEnabled(false);
            Precios.jif_crear_ingreso_precios.chk_cerrar.setEnabled(false);
            Precios.jif_crear_ingreso_precios.jtabla.setEnabled(false);
        } else {
            Precios.jif_crear_ingreso_precios.chk_cerrar.setEnabled(false);
            Precios.jif_crear_ingreso_precios.btn_guardar.setText("Actualizar");
        }

        frm.show();
    }

    public int Actualizar(IngresosProductos ingreso) {
        int resultado = 0;
        Connection con = null;
        String SQL = "UPDATE ingresos_productos_cabecera set "
                + "fecha=" + ingreso.getFecha_ingreso() + ", "
                + "hora=" + ingreso.getHora() + ", "
                + "no_factura='" + ingreso.getNo_factura() + "', "
                + "id_proveedor=" + ingreso.getId_proveedor() + ", "
                + "id_transportador=" + ingreso.getId_transportador() + ", "
                + "estado=" + ingreso.getEstado() + ", "
                + "observacion='" + ingreso.getObservacion() + "' "
                + "where id=" + ingreso.getId();
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SQL);
            resultado = psql.executeUpdate();
            psql.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al intentar actualizar la información:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error al intentar cerrar la conexión:\n"
                        + ex, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            }
        }
        return resultado;
    }
}
