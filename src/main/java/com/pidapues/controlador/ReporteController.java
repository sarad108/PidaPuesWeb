package com.pidapues.controlador;

import com.pidapues.servicio.AuthService;
import com.pidapues.servicio.PedidoService;
import com.pidapues.servicio.ReporteService;
import com.pidapues.util.Datos;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reportes de ventas e inventario, ajuste de existencias y limpieza de la demo (solo admin). */
@RestController
@RequestMapping("/api")
public class ReporteController {

    private final ReporteService reportes;
    private final PedidoService pedidos;
    private final AuthService auth;

    public ReporteController(ReporteService reportes, PedidoService pedidos, AuthService auth) {
        this.reportes = reportes;
        this.pedidos = pedidos;
        this.auth = auth;
    }

    @GetMapping("/reportes/ventas")
    public Map<String, Object> ventas(@RequestParam(name = "desde", required = false) String desde,
                                      @RequestParam(name = "hasta", required = false) String hasta,
                                      HttpServletRequest peticion) {
        auth.exigir(peticion, "admin");
        return reportes.ventas(desde, hasta);
    }

    @GetMapping("/reportes/inventario")
    public Map<String, Object> inventario(@RequestParam(name = "desde", required = false) String desde,
                                          @RequestParam(name = "hasta", required = false) String hasta,
                                          HttpServletRequest peticion) {
        auth.exigir(peticion, "admin");
        return reportes.inventario(desde, hasta);
    }

    @PatchMapping("/inventario/{id}")
    public Map<String, Object> actualizarInventario(@PathVariable("id") int id,
                                                    @RequestBody(required = false) Map<String, Object> body,
                                                    HttpServletRequest peticion) {
        auth.exigir(peticion, "admin");
        return reportes.actualizarInventario(id, body);
    }

    @DeleteMapping("/demo")
    public Map<String, Object> limpiarDemo(HttpServletRequest peticion) {
        auth.exigir(peticion, "admin");
        pedidos.limpiarDemo();
        return Datos.mapa("ok", true);
    }
}
