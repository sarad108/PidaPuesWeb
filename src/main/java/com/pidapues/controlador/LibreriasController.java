package com.pidapues.controlador;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Librerías de los reportes (gráficos y PDF) que se sirven desde el propio servidor, para que
 * funcionen sin internet. Vienen dentro de los WebJars del pom.xml (reemplazan a node_modules).
 */
@RestController
public class LibreriasController {

    private final PathMatchingResourcePatternResolver buscador = new PathMatchingResourcePatternResolver();

    @GetMapping("/vendor/chart.js")
    public ResponseEntity<?> chart() {
        return servir("classpath*:META-INF/resources/webjars/chart.js/*/dist/chart.umd.js",
                "classpath*:META-INF/resources/webjars/chart.js/*/dist/chart.umd.min.js");
    }

    @GetMapping("/vendor/jspdf.js")
    public ResponseEntity<?> jspdf() {
        return servir("classpath*:META-INF/resources/webjars/jspdf/*/dist/jspdf.umd.min.js",
                "classpath*:META-INF/resources/webjars/jspdf/*/dist/jspdf.umd.js");
    }

    private ResponseEntity<?> servir(String... patrones) {
        for (String patron : patrones) {
            try {
                Resource[] encontrados = buscador.getResources(patron);
                if (encontrados.length > 0) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType("application/javascript"))
                            .body(encontrados[0]);
                }
            } catch (IOException e) {
                // se prueba el siguiente patrón
            }
        }
        return ResponseEntity.status(404)
                .contentType(MediaType.TEXT_PLAIN)
                .body("No se encontró la librería. Revisa las dependencias de webjars en el pom.xml.");
    }
}
