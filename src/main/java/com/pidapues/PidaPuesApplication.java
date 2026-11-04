package com.pidapues;

import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PidaPues: API REST en Java (Spring Boot) que también sirve el frontend estático.
 *
 * Capas: Controlador (HTTP) -> Servicio (reglas y validaciones) -> Repositorio (JPA) -> MySQL.
 */
@SpringBootApplication
@EnableScheduling
public class PidaPuesApplication {

    public static void main(String[] args) {
        // Los comandos de consola (--empleados=..., --bd=...) corren sin levantar el servidor web.
        boolean comando = Arrays.stream(args).anyMatch(a -> a.startsWith("--empleados") || a.startsWith("--bd"));
        SpringApplication app = new SpringApplication(PidaPuesApplication.class);
        if (comando) {
            app.setWebApplicationType(WebApplicationType.NONE);
        }
        app.run(args);
    }
}
