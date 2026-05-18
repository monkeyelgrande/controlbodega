package com.bodega.api.entregas;

import com.bodega.api.entregas.dto.EntregaRequest;
import com.bodega.api.entregas.dto.EntregaResponse;
import com.bodega.api.security.AuthInterceptor;
import com.bodega.api.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * POST /api/entregas
 *
 * Ejecuta una entrega completa o parcial sobre una orden. El usuario y la
 * bodega se toman del token (no del cuerpo).
 *
 * Requiere cabecera: Authorization: Bearer &lt;token&gt;
 */
@RestController
@RequestMapping("/api/entregas")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @PostMapping
    public EntregaResponse entregar(
            @RequestBody EntregaRequest req,
            @RequestAttribute(AuthInterceptor.SESION_ATTR) TokenService.Sesion sesion) {

        return entregaService.ejecutar(req, sesion.idUser, sesion.idBodega);
    }

    @ExceptionHandler(EntregaInvalidaException.class)
    public ResponseEntity<Object> manejarInvalida(EntregaInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Collections.singletonMap("error", ex.getMessage()));
    }
}
