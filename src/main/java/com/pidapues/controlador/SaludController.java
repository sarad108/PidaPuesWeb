package com.pidapues.controlador;

import com.pidapues.servicio.EsquemaService;
import com.pidapues.util.Datos;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sirve para ver si la API llega a la base y si tiene las tablas: http://localhost:3000/api/salud */
@RestController
@RequestMapping("/api")
public class SaludController {

    private final EsquemaService esquema;

    public SaludController(EsquemaService esquema) {
        this.esquema = esquema;
    }

    @GetMapping("/salud")
    public ResponseEntity<Map<String, Object>> salud() {
        EsquemaService.Diagnostico d = esquema.diagnosticar();
        Map<String, Object> cuerpo = Datos.mapa(
                "baseDeDatos", esquema.nombreBd(),
                "esquemaCompleto", d.completo,
                "tablasFaltantes", d.tablasFaltantes,
                "vistasFaltantes", d.vistasFaltantes,
                "esquemaAntiguoDetectado", d.tieneEsquemaAntiguo);
        return ResponseEntity.status(d.completo ? 200 : 503).body(cuerpo);
    }
}
