package com.bodega.api.auth;

import com.bodega.api.security.AuthInterceptor;
import com.bodega.api.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint de inicio de sesion para la app Android.
 *
 * POST /api/auth/login
 *   body: { "userName": "...", "password": "..." }
 *   200 : datos del usuario + token de sesion
 *   401 : credenciales invalidas
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int PERFIL_ADMIN = 1;

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req == null
                || req.getUserName() == null || req.getUserName().trim().isEmpty()
                || req.getPassword() == null || req.getPassword().isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Debe enviar userName y password.");
        }

        AuthService.UsuarioAutenticado u =
                authService.autenticar(req.getUserName().trim(), req.getPassword());

        if (u == null) {
            return error(HttpStatus.UNAUTHORIZED, "Usuario o contrasena incorrectos.");
        }

        LoginResponse resp = new LoginResponse();
        resp.setToken(tokenService.generar(u.idUser, u.idPerfil, u.idBodega));
        resp.setIdUser(u.idUser);
        resp.setNombre(u.nombre);
        resp.setUserName(u.userName);
        resp.setIdPerfil(u.idPerfil);
        resp.setPerfil(u.perfil);
        resp.setIdBodega(u.idBodega);
        resp.setBodega(u.bodega);
        resp.setEsAdmin(u.idPerfil == PERFIL_ADMIN);

        return ResponseEntity.ok(resp);
    }

    /**
     * Valida el token guardado en el telefono cuando la app se reabre.
     * Protegido por el interceptor: si el token es invalido o expiro,
     * responde 401 y la app manda al usuario al login.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(
            @RequestAttribute(AuthInterceptor.SESION_ATTR) TokenService.Sesion sesion) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("idUser", sesion.idUser);
        r.put("idPerfil", sesion.idPerfil);
        r.put("idBodega", sesion.idBodega);
        r.put("esAdmin", sesion.idPerfil == PERFIL_ADMIN);
        return ResponseEntity.ok(r);
    }

    private ResponseEntity<Object> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status)
                .body(Collections.singletonMap("error", mensaje));
    }
}
