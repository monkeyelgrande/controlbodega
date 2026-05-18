package com.bodega.api.inventario;

import com.bodega.api.inventario.dto.ProductoInventario;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * GET /api/inventario?q=texto
 *
 * Busca por codigo de barras o nombre (parcial) y devuelve el stock del
 * producto en todas las bodegas. Requiere token (Authorization: Bearer).
 */
@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping
    public ResponseEntity<?> buscar(@RequestParam("q") String q) {
        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error",
                            "Escribe al menos 2 caracteres para buscar."));
        }
        List<ProductoInventario> r = inventarioService.buscar(q);
        return ResponseEntity.ok(r);
    }
}
