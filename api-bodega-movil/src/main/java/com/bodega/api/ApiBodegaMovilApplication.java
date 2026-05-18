package com.bodega.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API REST de la app movil de bodega.
 *
 * Esta aplicacion se ejecuta en el mismo PC que tiene PostgreSQL y expone
 * los endpoints HTTP que consume la app Android dentro de la red WiFi de
 * la empresa.
 */
@SpringBootApplication
public class ApiBodegaMovilApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiBodegaMovilApplication.class, args);
    }
}
