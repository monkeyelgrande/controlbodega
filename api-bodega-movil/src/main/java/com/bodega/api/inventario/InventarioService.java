package com.bodega.api.inventario;

import com.bodega.api.inventario.dto.BodegaStock;
import com.bodega.api.inventario.dto.ProductoInventario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Busca productos por codigo de barras o descripcion (parcial) y devuelve
 * su stock en TODAS las bodegas donde tiene registro, con cantidad,
 * pendientes y disponible (= cantidad - pendientes).
 *
 * Solo lectura: no modifica nada.
 */
@Service
public class InventarioService {

    private static final int MAX_FILAS = 500;

    private final JdbcTemplate jdbc;

    public InventarioService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProductoInventario> buscar(String texto) {
        String like = "%" + texto.trim() + "%";

        String sql =
                "SELECT p.id, p.codigo_barras, p.descripcion, "
                + "       b.id AS id_bodega, b.nombre AS bodega, "
                + "       COALESCE(sp.cantidad, 0)   AS cantidad, "
                + "       COALESCE(sp.pendientes, 0) AS pendientes "
                + "FROM productos p "
                + "JOIN stock_productos sp ON sp.id_producto = p.id "
                + "JOIN bodegas b ON b.id = sp.id_bodega "
                + "WHERE COALESCE(p.estado, true) = true "
                + "  AND (p.codigo_barras ILIKE ? OR p.descripcion ILIKE ?) "
                + "ORDER BY p.descripcion, b.nombre "
                + "LIMIT " + MAX_FILAS;

        // Conserva el orden de aparicion (ORDER BY descripcion) por producto.
        Map<Integer, ProductoInventario> porProducto = new LinkedHashMap<>();

        jdbc.query(sql, new Object[]{like, like}, rs -> {
            int idProd = rs.getInt("id");
            ProductoInventario prod = porProducto.get(idProd);
            if (prod == null) {
                prod = new ProductoInventario();
                prod.idProducto = idProd;
                prod.codigo = rs.getString("codigo_barras");
                prod.descripcion = rs.getString("descripcion");
                porProducto.put(idProd, prod);
            }
            double cant = rs.getDouble("cantidad");
            double pend = rs.getDouble("pendientes");

            BodegaStock bs = new BodegaStock();
            bs.idBodega = rs.getInt("id_bodega");
            bs.bodega = rs.getString("bodega");
            bs.cantidad = cant;
            bs.pendientes = pend;
            bs.disponible = cant - pend;
            prod.bodegas.add(bs);

            prod.totalCantidad += cant;
            prod.totalPendientes += pend;
            prod.totalDisponible += (cant - pend);
        });

        return new ArrayList<>(porProducto.values());
    }
}
