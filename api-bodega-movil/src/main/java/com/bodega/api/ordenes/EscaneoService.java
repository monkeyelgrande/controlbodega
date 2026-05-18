package com.bodega.api.ordenes;

import com.bodega.api.common.IdGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Types;

/**
 * Registro de lecturas QR en {@code escaneos_qr_ordenes}, portado de
 * {@code EntregaQRService.registrarEscaneo / actualizarAccionEscaneo}.
 *
 * Se registra SIEMPRE una lectura (valida o no) para poder auditar despues.
 */
@Service
public class EscaneoService {

    private final JdbcTemplate jdbc;
    private final IdGenerator idGen;

    public EscaneoService(JdbcTemplate jdbc, IdGenerator idGen) {
        this.jdbc = jdbc;
        this.idGen = idGen;
    }

    public int registrar(Integer idFactura, int idUser, int idBodega,
                         String qrLeido, String resultado, String accion,
                         Integer idEntregaCab, String pcOrigen) {

        int id = idGen.siguiente("escaneos_qr_ordenes");
        long now = System.currentTimeMillis();
        java.sql.Date fecha = new java.sql.Date(now);
        java.sql.Time hora = new java.sql.Time(now);
        String qr = qrLeido == null ? null
                : (qrLeido.length() > 150 ? qrLeido.substring(0, 150) : qrLeido);

        jdbc.update(con -> {
            java.sql.PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO escaneos_qr_ordenes "
                    + "(id, id_factura, id_user, id_bodega, fecha_escaneo, "
                    + " hora_escaneo, qr_leido, resultado, accion, id_entrega_cab, "
                    + " pc_origen) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            int i = 1;
            ps.setInt(i++, id);
            if (idFactura == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, idFactura);
            }
            ps.setInt(i++, idUser);
            ps.setInt(i++, idBodega);
            ps.setDate(i++, fecha);
            ps.setTime(i++, hora);
            if (qr == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, qr);
            }
            ps.setString(i++, resultado);
            ps.setString(i++, accion);
            if (idEntregaCab == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, idEntregaCab);
            }
            if (pcOrigen == null) {
                ps.setNull(i++, Types.VARCHAR);
            } else {
                ps.setString(i++, pcOrigen.length() > 80
                        ? pcOrigen.substring(0, 80) : pcOrigen);
            }
            return ps;
        });

        return id;
    }

    public void actualizarAccion(int idEscaneo, String accion, Integer idEntregaCab) {
        if (idEscaneo <= 0) {
            return;
        }
        jdbc.update(con -> {
            java.sql.PreparedStatement ps = con.prepareStatement(
                    "UPDATE escaneos_qr_ordenes SET accion = ?, id_entrega_cab = ? "
                    + "WHERE id = ?");
            ps.setString(1, accion);
            if (idEntregaCab == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, idEntregaCab);
            }
            ps.setInt(3, idEscaneo);
            return ps;
        });
    }
}
