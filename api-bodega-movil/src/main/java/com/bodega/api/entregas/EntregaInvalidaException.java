package com.bodega.api.entregas;

/** La entrega no se puede realizar por una regla de negocio (se devuelve 400). */
public class EntregaInvalidaException extends RuntimeException {
    public EntregaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
