package com.pidapues.controlador;

import com.pidapues.servicio.ResenaService;
import com.pidapues.util.Datos;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reseñas: se ven en el inicio y se dejan al ver la factura. */
@RestController
@RequestMapping("/api")
public class ResenaController {

    private final ResenaService resenas;

    public ResenaController(ResenaService resenas) {
        this.resenas = resenas;
    }

    @GetMapping("/resenas")
    public Map<String, Object> publicas() {
        return resenas.publicas();
    }

    @PostMapping("/pedidos/{id}/resena")
    public ResponseEntity<Map<String, Object>> crear(@PathVariable("id") int id,
                                                     @RequestBody(required = false) Map<String, Object> body) {
        resenas.crear(id, body);
        return ResponseEntity.status(201).body(Datos.mapa("ok", true));
    }
}
