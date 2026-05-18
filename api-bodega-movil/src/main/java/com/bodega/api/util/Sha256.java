package com.bodega.api.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * SHA-256 en hexadecimal minuscula.
 *
 * Reproduce exactamente lo que hace la app de escritorio con
 * org.apache.commons.codec.digest.DigestUtils.sha256Hex(...), para que las
 * contrasenas guardadas en la tabla {@code users} validen igual desde aqui.
 */
public final class Sha256 {

    private Sha256() {
    }

    public static String hex(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                int v = b & 0xFF;
                if (v < 0x10) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular SHA-256", e);
        }
    }
}
