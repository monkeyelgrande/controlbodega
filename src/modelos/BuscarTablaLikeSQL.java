/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelos;

import conexiondb.DB_consultas_R_D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author CHAPARRO
 */
public class BuscarTablaLikeSQL {

    DecimalFormat formatea = new DecimalFormat("###,###.##");

    public DefaultTableModel BuscarEnTabla(String sql, DefaultTableModel modeloImport) {

        DefaultTableModel modelo = modeloImport;


        ResultSet rs = null;
        try {
            rs = DB_consultas_R_D.getTabla(sql);

            while (rs.next()) {
                modelo.addRow(new Object[]{rs.getString("id_cabecera"), rs.getString("id_producto"), rs.getString("descripcion"), rs.getString("cliente"),
                    formatea.format(rs.getDouble("cantidad")), formatea.format(rs.getDouble("entrega")), formatea.format(rs.getDouble("pendiente"))});

            }
        } catch (SQLException e) {

            JOptionPane.showMessageDialog(null, "Error al conectar. " + e.getMessage());

        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, e);
            }
        }
        return modelo;
    }
}
