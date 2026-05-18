package com.bodega.api.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Genera el siguiente id de una tabla con {@code select max(id)+1}.
 *
 * Replica exactamente {@code DB_consultas_R_D.cargarId(tabla)} de la app de
 * escritorio (incluido el comportamiento de devolver 1 cuando la tabla esta
 * vacia) para que las dos aplicaciones convivan sin chocar.
 */
@Component
public class IdGenerator {

    private final JdbcTemplate jdbc;

    public IdGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int siguiente(String tabla) {
        Integer id = jdbc.queryForObject(
                "select max(id)+1 as id from " + tabla, Integer.class);
        return id == null ? 1 : id;
    }
}
