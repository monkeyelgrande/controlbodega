/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import Formularios_internos.jif_crear_ingreso_mercancia;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import modelos.IngresosMercancias;

/**
 *
 * @author Monkeyelgrande
 */
public class DBingresosMercancias {

    public int Guardar(IngresosMercancias ingreso) {
        int resultado = 0;
        Connection con = null;
        String SSQL = "INSERT INTO ingresos_mercancias_cabecera (id,id_proveedor,id_transportador,id_user,id_tipo, fecha, hora, no_factura, estado, id_bodega, fecha_vencimiento, descripcion) "
                + "VALUES (" + ingreso.getId() + "," + ingreso.getId_proveedor() + "," + ingreso.getId_transportador() + "," + ingreso.getId_user() + "," + ingreso.getId_tipo_ingreso() + ","
                + ingreso.getFecha_ingreso() + "," + ingreso.getHora() + ",'" + ingreso.getNo_factura() + "'," + ingreso.getEstado() + ", "
                + ingreso.getId_bodega() + "," + ingreso.getFecha_vencimiento() + ",'" + ingreso.getDescripcion() + "')";
        try {
            con = DB_consultas_R_D.getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            resultado = psql.executeUpdate();
            psql.close();

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(null, "Error al intentar almacenar la información:\n"
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

    public int Actualizar(IngresosMercancias ingreso) {
        int resultado = 0;

        Connection con = null;

        String SQL = "UPDATE ingresos_mercancias_cabecera set "
                + "fecha=" + ingreso.getFecha_ingreso() + ", "
                + "fecha_vencimiento=" + ingreso.getFecha_vencimiento() + ", "
                + "hora=" + ingreso.getHora() + ", "
                + "no_factura='" + ingreso.getNo_factura() + "', "
                + "id_proveedor=" + ingreso.getId_proveedor() + ", "
                + "id_transportador=" + ingreso.getId_transportador() + ", "
                + "id_tipo=" + ingreso.getId_tipo_ingreso() + ", "
                + "estado=" + ingreso.getEstado() + ", "
                + "descripcion='" + ingreso.getDescripcion() + "', "
                + "id_bodega=" + ingreso.getId_bodega() + " "
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

    public static void ver_ingreso_mercancia(String ver_editar, String id_ingreso) {

        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator('.');
        DecimalFormat formateador = new DecimalFormat("####.##", simbolos);
        ResultSet rs = DB_consultas_R_D.getTabla("select i.id, i.descripcion, i.no_factura, i.id_transportador, tra.nombre as transportador, "
                + "p.nombre as proveedor,p.id as id_proveedor, t.nombre as tipo,t.id as id_tipo, i.total, i.fecha "
                + "from ingresos_mercancias_cabecera i, contactos p, tipo_ingreso t, contactos tra "
                + "where i.id_proveedor=p.id and i.id_tipo=t.id and i.id_transportador=tra.id and i.id=" + id_ingreso);
        jif_crear_ingreso_mercancia frm = new jif_crear_ingreso_mercancia();

        try {
            while (rs.next()) {
                jif_crear_ingreso_mercancia.lbl_id.setText(rs.getString("id"));
                jif_crear_ingreso_mercancia.jdate_fecha_entrada.setDate(rs.getDate("fecha"));
                jif_crear_ingreso_mercancia.jbox_proveedor.setSelectedItem(rs.getString("proveedor"));
                jif_crear_ingreso_mercancia.jbox_transportador.setSelectedItem(rs.getString("transportador"));
                jif_crear_ingreso_mercancia.id_proveedor = (rs.getInt("id_proveedor"));
                jif_crear_ingreso_mercancia.id_transportador = (rs.getInt("id_transportador"));
                jif_crear_ingreso_mercancia.jbox_tipo.setSelectedItem(rs.getString("tipo"));
                jif_crear_ingreso_mercancia.id_tipo = (rs.getInt("id_tipo"));
                jif_crear_ingreso_mercancia.txt_no_factura.setText(rs.getString("no_factura"));
                jif_crear_ingreso_mercancia.txt_descripcion.setText(rs.getString("descripcion"));
//                    jif_crear_ingreso_mercancia.lbl_total.setText(metodos.formateador_decimal().format(rs.getDouble("total")));
            }

            rs.close();
        } catch (SQLException ex) {
            System.out.println(ex);
        }

        rs = DB_consultas_R_D.getTabla("select p.id,p.codigo_barras,p.descripcion,i.cantidad "
                + "from ingresos_mercancias_detalle i, productos p, ingresos_mercancias_cabecera ic "
                + "where i.id_producto=p.id and i.id_ingreso_cabecera=ic.id and ic.id=" + id_ingreso + " order by id");

        DefaultTableModel modelof = new DefaultTableModel() { // modelo de la tabla
            @Override
            public boolean isCellEditable(int fila, int columna) { // solo se permiten editables la columan cantidad y precio
                if (columna == 3) { // Columna cantidad
                    return columna == 3;
                }
                return columna == 3;
            }

            @Override
            public void setValueAt(Object aValue, int row, int col) {
                super.setValueAt(aValue, row, col);
//                    frm.calcular_total();
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
            frm.modelo_productos = modelof;
            frm.jtabla.setModel(frm.modelo_productos);
        } catch (SQLException ex) {
            System.out.println(ex);
        }

        if (ver_editar.equals("ver")) {
            jif_crear_ingreso_mercancia.jbox_proveedor.setEnabled(false);
            jif_crear_ingreso_mercancia.jbox_transportador.setEnabled(false);
            jif_crear_ingreso_mercancia.jbox_tipo.setEnabled(false);
            jif_crear_ingreso_mercancia.txt_no_factura.setEnabled(false);
            jif_crear_ingreso_mercancia.txt_codigo_barras.setEnabled(false);
            jif_crear_ingreso_mercancia.btn_buscar.setEnabled(false);
            jif_crear_ingreso_mercancia.jdate_fecha_entrada.setEnabled(false);
            jif_crear_ingreso_mercancia.btn_guardar.setEnabled(false);
            jif_crear_ingreso_mercancia.btn_limpiar.setEnabled(false);
            jif_crear_ingreso_mercancia.chk_cerrar.setEnabled(false);
            jif_crear_ingreso_mercancia.jtabla.setEnabled(false);
            jif_crear_ingreso_mercancia.btn_imprimir.setVisible(true);
        } else {
            jif_crear_ingreso_mercancia.chk_cerrar.setEnabled(false);
            jif_crear_ingreso_mercancia.btn_guardar.setText("Actualizar");
        }

        frm.show();
    }

    /**
     * Elimina un ingreso de mercancía revirtiendo su efecto en el inventario.
     *
     * A diferencia de un DELETE plano (que dejaba el stock inflado y sin
     * rastro), aquí primero se lee el detalle, se descuenta de stock_productos
     * y se registra el movimiento de reversión por cada producto, y solo
     * entonces se borra la cabecera (el ON DELETE CASCADE limpia el detalle).
     *
     * El detalle se lee ANTES de borrar porque el cascade lo elimina.
     *
     * @param idIngreso ID de la cabecera del ingreso a eliminar
     * @param idUser    ID del usuario que realiza la eliminación
     * @return true si se revirtió y eliminó correctamente
     */
    public boolean eliminarConReversion(int idIngreso, int idUser) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();

            // 1. Bodega y estado del ingreso (y verificar que exista)
            int idBodega = -1;
            int estado = 0;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id_bodega, estado FROM ingresos_mercancias_cabecera WHERE id = ?")) {
                ps.setInt(1, idIngreso);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idBodega = rs.getInt("id_bodega");
                        estado = rs.getInt("estado");
                    } else {
                        JOptionPane.showMessageDialog(null, "El ingreso no existe.");
                        return false;
                    }
                }
            }

            // 2. Leer el detalle ANTES de borrar (el cascade lo elimina)
            List<Integer> productos = new ArrayList<Integer>();
            List<Double> cantidades = new ArrayList<Double>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id_producto, cantidad FROM ingresos_mercancias_detalle "
                    + "WHERE id_ingreso_cabecera = ?")) {
                ps.setInt(1, idIngreso);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        productos.add(rs.getInt("id_producto"));
                        cantidades.add(rs.getDouble("cantidad"));
                    }
                }
            }

            // 3. Revertir stock + registrar movimiento por cada producto.
            //    SOLO si el ingreso estaba RECIBIDO (estado=1): un ingreso
            //    pendiente nunca sumó al stock, así que no hay nada que revertir.
            //    Si una reversión falla, se aborta SIN borrar la cabecera para
            //    no perder el rastro de lo que aún falta revertir.
            if (estado == 1) {
                DBstock_productos dbStock = new DBstock_productos();
                for (int i = 0; i < productos.size(); i++) {
                    int idProducto = productos.get(i);
                    double cantidad = cantidades.get(i);
                    int idMov = dbStock.eliminarIngreso(
                            idProducto, idBodega, idUser, cantidad, idIngreso,
                            "Reversión por eliminación de ingreso #" + idIngreso);
                    if (idMov <= 0) {
                        JOptionPane.showMessageDialog(null,
                                "No se pudo revertir el stock del producto " + idProducto
                                + ".\nEl ingreso NO fue eliminado para no dejar inventario inconsistente.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }

            // 4. Borrar la cabecera (el cascade elimina el detalle)
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM ingresos_mercancias_cabecera WHERE id = ?")) {
                ps.setInt(1, idIngreso);
                ps.executeUpdate();
            }

            return true;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error al eliminar el ingreso de mercancía:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                System.err.println("Error al cerrar conexión: " + ex.getMessage());
            }
        }
    }

}
