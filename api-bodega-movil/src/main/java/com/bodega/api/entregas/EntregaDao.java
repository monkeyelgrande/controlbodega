package com.bodega.api.entregas;

import com.bodega.api.common.Horas;
import com.bodega.api.common.IdGenerator;
import com.bodega.api.entregas.dto.ItemEntrega;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Inserta la cabecera de entrega y sus detalles en UNA transaccion, que se
 * confirma al volver del metodo.
 *
 * Se hace antes de los movimientos de stock (igual que el escritorio: la
 * cabecera ya existe cuando movimientos_inventario la referencia).
 */
@Repository
public class EntregaDao {

    private final JdbcTemplate jdbc;
    private final IdGenerator idGen;

    public EntregaDao(JdbcTemplate jdbc, IdGenerator idGen) {
        this.jdbc = jdbc;
        this.idGen = idGen;
    }

    @Transactional
    public int crearCabeceraConDetalles(int idFactura, int idUser, int idBodega,
                                        List<ItemEntrega> items) {

        int idCab = idGen.siguiente("entregas_productos_cabecera");
        java.sql.Date fecha = new java.sql.Date(System.currentTimeMillis());
        String hora = Horas.horaEntrega();

        jdbc.update(
                "INSERT INTO entregas_productos_cabecera "
                + "(id, id_factura, id_user, fecha_entrega, hora_entrega, id_bodega) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
                idCab, idFactura, idUser, fecha, hora, idBodega);

        int idDet = idGen.siguiente("entregas_productos");
        for (ItemEntrega it : items) {
            jdbc.update(
                    "INSERT INTO entregas_productos "
                    + "(id, id_cabecera, id_producto, cantidad, id_factura) "
                    + "VALUES (?, ?, ?, ?, ?)",
                    idDet++, idCab, it.getIdProducto(), it.getCantidad(), idFactura);
        }

        return idCab;
    }
}
