package com.bodega.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Token de sesion compacto y firmado con HMAC-SHA256 (sin dependencias
 * externas, valido en Java 8).
 *
 * Formato:  base64url(payload) + "." + base64url(firma)
 * Payload:  idUser|idPerfil|idBodega|expEpochMillis
 *
 * El token es "stateless": no se guarda en base de datos. El servidor solo
 * verifica que la firma sea valida y que no haya expirado. Asi cada endpoint
 * sabe que usuario / perfil / bodega hace la peticion sin volver a pedir
 * usuario y contrasena.
 */
@Service
public class TokenService {

    private final byte[] secret;
    private final long ttlMillis;

    public TokenService(
            @Value("${app.token.secret}") String secret,
            @Value("${app.token.ttl-minutes}") long ttlMinutes) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlMillis = ttlMinutes * 60L * 1000L;
    }

    /** Datos que viajan dentro del token. */
    public static final class Sesion {
        public final int idUser;
        public final int idPerfil;
        public final int idBodega;

        public Sesion(int idUser, int idPerfil, int idBodega) {
            this.idUser = idUser;
            this.idPerfil = idPerfil;
            this.idBodega = idBodega;
        }
    }

    public String generar(int idUser, int idPerfil, int idBodega) {
        long exp = System.currentTimeMillis() + ttlMillis;
        String payload = idUser + "|" + idPerfil + "|" + idBodega + "|" + exp;
        String payloadB64 = base64url(payload.getBytes(StandardCharsets.UTF_8));
        String firma = base64url(hmac(payloadB64));
        return payloadB64 + "." + firma;
    }

    /**
     * Devuelve la sesion si el token es valido y no expiro; null en cualquier
     * otro caso (firma invalida, manipulado, vencido, mal formado).
     */
    public Sesion verificar(String token) {
        if (token == null || token.indexOf('.') < 0) {
            return null;
        }
        int punto = token.indexOf('.');
        String payloadB64 = token.substring(0, punto);
        String firmaRecibida = token.substring(punto + 1);

        String firmaEsperada = base64url(hmac(payloadB64));
        if (!constantTimeEquals(firmaEsperada, firmaRecibida)) {
            return null;
        }

        try {
            String payload = new String(
                    Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            String[] p = payload.split("\\|");
            if (p.length != 4) {
                return null;
            }
            int idUser = Integer.parseInt(p[0]);
            int idPerfil = Integer.parseInt(p[1]);
            int idBodega = Integer.parseInt(p[2]);
            long exp = Long.parseLong(p[3]);
            if (System.currentTimeMillis() > exp) {
                return null;
            }
            return new Sesion(idUser, idPerfil, idBodega);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }

    private static String base64url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }
}
