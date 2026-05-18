package com.bodega.api.ordenes;

import com.bodega.api.ordenes.dto.ResultadoEscaneo;
import com.bodega.api.security.TokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bodega.api.security.AuthInterceptor;

/**
 * GET /api/ordenes?qr=ORDEN-123
 *
 * Procesa la lectura del QR usando el usuario y la bodega que vienen dentro
 * del token (no se confia en lo que mande el cliente). Siempre registra el
 * escaneo y devuelve el resultado + la orden con sus pendientes.
 *
 * Requiere cabecera: Authorization: Bearer &lt;token&gt;
 */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenService ordenService;

    public OrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    @GetMapping
    public ResultadoEscaneo procesar(
            @RequestParam("qr") String qr,
            @RequestAttribute(AuthInterceptor.SESION_ATTR) TokenService.Sesion sesion,
            @RequestHeader(value = "X-Dispositivo", required = false) String dispositivo) {

        String pcOrigen = (dispositivo == null || dispositivo.trim().isEmpty())
                ? "APP-MOVIL" : "APP-" + dispositivo.trim();

        return ordenService.procesarLectura(
                qr, sesion.idUser, sesion.idBodega, pcOrigen);
    }
}
