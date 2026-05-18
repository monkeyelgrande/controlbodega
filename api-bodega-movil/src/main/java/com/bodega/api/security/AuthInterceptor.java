package com.bodega.api.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Exige un token valido (cabecera {@code Authorization: Bearer <token>}) en
 * los endpoints protegidos. Si es valido, deja la sesion disponible como
 * atributo de la peticion con la clave {@link #SESION_ATTR}.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESION_ATTR = "sesion";

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String auth = request.getHeader("Authorization");
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7).trim();
        }

        TokenService.Sesion sesion = tokenService.verificar(token);
        if (sesion == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"error\":\"Token ausente, invalido o vencido. Inicie sesion de nuevo.\"}");
            return false;
        }

        request.setAttribute(SESION_ATTR, sesion);
        return true;
    }
}
