package com.pidapues.controlador;

import com.pidapues.servicio.AuthService;
import com.pidapues.servicio.PedidoService;
import com.pidapues.util.Datos;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Pedidos: lectura por rol, creación, pago, flujo de cocina y entrega. */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidos;
    private final AuthService auth;

    public PedidoController(PedidoService pedidos, AuthService auth) {
        this.pedidos = pedidos;
        this.auth = auth;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(name = "rol", required = false) String rol,
                                            HttpServletRequest peticion) {
        String rolPedido = rol == null || rol.isEmpty() ? "admin" : rol;
        // Exige el token del rol que se está pidiendo ver (admin puede ver todo).
        auth.exigir(peticion, rolPedido);
        return pedidos.listarPorRol(rolPedido);
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable("id") int id) {
        return pedidos.pedidoOError(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.status(201).body(pedidos.crear(body));
    }

    @PostMapping("/{id}/verificar-pago")
    public Map<String, Object> verificarPago(@PathVariable("id") int id,
                                             @RequestBody(required = false) Map<String, Object> body) {
        Object codigo = body == null ? null : body.get("codigo");
        return pedidos.verificarPago(id, codigo == null ? "" : String.valueOf(codigo));
    }

    @PostMapping("/{id}/reenviar-codigo")
    public Map<String, Object> reenviarCodigo(@PathVariable("id") int id) {
        return Datos.mapa("codigoVerificacionDemo", pedidos.reenviarCodigo(id));
    }

    @PatchMapping("/{id}/cocina")
    public Map<String, Object> cocina(@PathVariable("id") int id,
                                      @RequestBody(required = false) Map<String, Object> body,
                                      HttpServletRequest peticion) {
        AuthService.Sesion sesion = auth.exigir(peticion, "cocina");
        Object estado = body == null ? null : body.get("estado");
        return pedidos.cambiarEstadoCocina(id, estado instanceof String ? (String) estado : null, sesion.idEmpleado);
    }

    @PostMapping("/{id}/notificado")
    public Map<String, Object> notificado(@PathVariable("id") int id, HttpServletRequest peticion) {
        auth.exigir(peticion, "cocina");
        pedidos.marcarNotificado(id);
        return Datos.mapa("ok", true);
    }

    @PatchMapping("/{id}/entregar")
    public Map<String, Object> entregar(@PathVariable("id") int id, HttpServletRequest peticion) {
        AuthService.Sesion sesion = auth.exigir(peticion, "mesero");
        return pedidos.entregar(id, sesion.idEmpleado);
    }
}
