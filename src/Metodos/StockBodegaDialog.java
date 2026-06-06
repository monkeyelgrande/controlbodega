package Metodos;

import conexiondb.DB_consultas_R_D;
import java.awt.Component;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * Muestra el stock de un producto desglosado por bodega, leyendo en tiempo real
 * de {@code stock_productos} (no de la tabla agregada {@code stock}, que se
 * desactualiza). Por cada bodega donde el producto tenga registro de inventario
 * —aunque sea 0 o negativo— se muestra una fila con cantidad, pendientes de
 * entrega y disponible (cantidad - pendientes). Las bodegas donde el producto
 * nunca tuvo inventario (sin fila en stock_productos) no aparecen.
 *
 * Reutilizable desde los formularios de crear ordenes, facturas y cotizaciones
 * (tecla X sobre la tabla de productos).
 */
public class StockBodegaDialog {

    private static final DecimalFormat FMT = new DecimalFormat("#,##0.##");

    // Paleta para el disponible
    private static final String COL_NEG = "#C62828"; // negativo
    private static final String COL_CERO = "#9E6F00"; // cero / agotado
    private static final String COL_POS = "#2E7D32"; // positivo

    private StockBodegaDialog() {
    }

    /**
     * Diálogo completo (título + tabla por bodega). Usado por el formulario de
     * órdenes, donde la tecla X solo mostraba el stock.
     */
    public static void mostrar(Component parent, String codigoBarras) {
        String html = "<html><div style='font-family:Segoe UI;'>"
                + "<b>Existencias por bodega</b><br>"
                + "<span style='color:#616161;'>Producto: " + escape(codigoBarras) + "</span>"
                + "<hr>"
                + tablaHtml(codigoBarras)
                + "</div></html>";
        JLabel label = new JLabel(html);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        JOptionPane.showMessageDialog(parent, label, "Stock por bodega",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Devuelve solo el fragmento HTML de la tabla por bodega, para componerlo
     * dentro de otro mensaje (p. ej. el de facturas que también muestra precios).
     * No incluye etiquetas &lt;html&gt;.
     */
    public static String tablaHtml(String codigoBarras) {
        StringBuilder filas = new StringBuilder();
        int n = 0;
        Connection c = null;
        String sql = "SELECT b.nombre AS bodega, "
                + "       sp.cantidad, "
                + "       sp.pendientes, "
                + "       (sp.cantidad - sp.pendientes) AS disponible "
                + "FROM productos p "
                + "JOIN stock_productos sp ON sp.id_producto = p.id "
                + "JOIN bodegas b ON b.id = sp.id_bodega "
                + "WHERE p.codigo_barras = ? "
                + "ORDER BY b.nombre";
        try {
            c = DB_consultas_R_D.getConexion();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, codigoBarras);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        n++;
                        double disponible = rs.getDouble("disponible");
                        String colDisp = disponible < 0 ? COL_NEG
                                : (disponible == 0 ? COL_CERO : COL_POS);
                        filas.append("<tr>")
                                .append("<td style='padding:3px 10px 3px 0;'>")
                                .append(escape(rs.getString("bodega"))).append("</td>")
                                .append("<td align='right' style='padding:3px 10px;'>")
                                .append(FMT.format(rs.getDouble("cantidad"))).append("</td>")
                                .append("<td align='right' style='padding:3px 10px;'>")
                                .append(FMT.format(rs.getDouble("pendientes"))).append("</td>")
                                .append("<td align='right' style='padding:3px 0 3px 10px; color:")
                                .append(colDisp).append("; font-weight:bold;'>")
                                .append(FMT.format(disponible)).append("</td>")
                                .append("</tr>");
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("[StockBodegaDialog] Error: " + ex.getMessage());
            return "<span style='color:#C62828;'>No se pudo consultar el stock.</span>";
        } finally {
            try { if (c != null) c.close(); } catch (Exception ignored) {}
        }

        if (n == 0) {
            return "<span style='color:#616161;'>Este producto no tiene "
                    + "registros de inventario en ninguna bodega.</span>";
        }

        return "<table cellspacing='0' cellpadding='0'>"
                + "<tr style='color:#9E9E9E; font-size:11px;'>"
                + "<td style='padding:0 10px 4px 0;'>BODEGA</td>"
                + "<td align='right' style='padding:0 10px 4px;'>CANTIDAD</td>"
                + "<td align='right' style='padding:0 10px 4px;'>PENDIENTES</td>"
                + "<td align='right' style='padding:0 0 4px 10px;'>DISPONIBLE</td>"
                + "</tr>"
                + filas
                + "</table>";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
