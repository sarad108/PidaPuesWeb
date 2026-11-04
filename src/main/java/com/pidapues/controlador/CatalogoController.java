package com.pidapues.controlador;

import com.pidapues.servicio.AuthService;
import com.pidapues.servicio.CatalogoService;
import com.pidapues.servicio.MesaService;
import com.pidapues.servicio.ReporteService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Menú, datos del negocio, mesas y tiempos de servicio. */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final CatalogoService catalogo;
    private final MesaService mesas;
    private final ReporteService reportes;
    private final AuthService auth;

    public CatalogoController(CatalogoService catalogo, MesaService mesas, ReporteService reportes, AuthService auth) {
        this.catalogo = catalogo;
        this.mesas = mesas;
        this.reportes = reportes;
        this.auth = auth;
    }

    @GetMapping("/productos")
    public List<Map<String, Object>> productos() {
        return catalogo.productos();
    }

    @GetMapping("/negocio")
    public Map<String, Object> negocio() {
        return catalogo.negocio();
    }

    @GetMapping("/mesas")
    public List<Map<String, Object>> mesas() {
        return mesas.listar();
    }

    @GetMapping("/tiempos-servicio")
    public List<Map<String, Object>> tiemposServicio(HttpServletRequest peticion) {
        auth.exigir(peticion, "admin");
        return reportes.tiemposServicio();
    }
}
