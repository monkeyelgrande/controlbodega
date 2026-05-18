package com.bodega.api.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoints simples para probar conectividad SIN necesitar usuario:
 *
 *   GET /api/health      -> la API responde (probar desde el celular/navegador)
 *   GET /api/health/db   -> la API puede hablar con PostgreSQL
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status", "UP");
        r.put("servicio", "api-bodega-movil");
        return r;
    }

    @GetMapping("/db")
    public Map<String, Object> db() {
        Map<String, Object> r = new LinkedHashMap<>();
        try {
            Integer uno = jdbc.queryForObject("SELECT 1", Integer.class);
            Integer usuarios = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            r.put("status", "UP");
            r.put("conexion", uno != null && uno == 1 ? "OK" : "RARO");
            r.put("totalUsuarios", usuarios);
        } catch (Exception e) {
            r.put("status", "DOWN");
            r.put("error", e.getMessage());
        }
        return r;
    }
}
