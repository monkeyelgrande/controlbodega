package com.bodega.api.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el interceptor de autenticacion sobre los endpoints protegidos
 * y habilita CORS abierto (la app corre dentro de la red local de la
 * empresa; util tambien para probar desde el navegador).
 *
 * /api/auth/** y /api/health/** quedan abiertos (no requieren token).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                // Solo el login y health quedan abiertos. /api/auth/me queda
                // protegido para poder validar el token guardado al reabrir.
                .excludePathPatterns("/api/auth/login", "/api/health/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
