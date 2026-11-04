package com.pidapues.servicio;

import com.pidapues.excepcion.ApiException;
import com.pidapues.modelo.Resena;
import com.pidapues.repositorio.ResenaRepository;
import com.pidapues.repositorio.VistaRepository;
import com.pidapues.util.Datos;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Reseñas: son opcionales, se dejan al ver la factura (no en el carrito) y hay una por pedido. */
@Service
public class ResenaService {

    private final VistaRepository vistas;
    private final ResenaRepository resenas;

    public ResenaService(VistaRepository vistas, ResenaRepository resenas) {
        this.vistas = vistas;
        this.resenas = resenas;
    }

    /** Reseñas que se ven en el inicio: promedio, total y las últimas con comentario. */
    public Map<String, Object> publicas() {
        Map<String, Object> resumen = vistas.resumenResenas();
        Object promedioBd = resumen.get("promedio");
        Object promedio = null;
        if (promedioBd != null) {
            promedio = Math.round(((Number) promedioBd).doubleValue() * 10) / 10.0;
        }
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Map<String, Object> f : vistas.ultimasResenas()) {
            lista.add(Datos.mapa(
                    "nombre", f.get("nombre_publico"),
                    "calificacion", f.get("calificacion"),
                    "comentario", f.get("comentario"),
                    "fecha", Datos.iso(f.get("fecha"))));
        }
        Number total = Datos.numero(resumen.get("total"));
        return Datos.mapa("total", total == null ? 0 : total, "promedio", promedio, "resenas", lista);
    }

    // Deja el nombre como "Juan P." (y "Consumidor final" queda como "Cliente").
    static String nombrePublico(String nombreCompleto) {
        String texto = nombreCompleto == null ? "" : nombreCompleto.trim();
        if (texto.isEmpty()) {
            return "Cliente";
        }
        String[] partes = texto.split("\\s+");
        if (String.join(" ", partes).equalsIgnoreCase("consumidor final")) {
            return "Cliente";
        }
        String primero = partes[0].length() > 20 ? partes[0].substring(0, 20) : partes[0];
        if (partes.length == 1) {
            return primero;
        }
        String ultima = partes[partes.length - 1];
        return primero + " " + Character.toUpperCase(ultima.charAt(0)) + ".";
    }

    /** Una reseña por pedido y solo si el pago ya está verificado. */
    public void crear(int idPedido, Map<String, Object> body) {
        Integer calificacion = Datos.entero(body == null ? null : body.get("calificacion"));
        if (calificacion == null || calificacion < 1 || calificacion > 5) {
            throw new ApiException(400, "Elige de 1 a 5 estrellas.");
        }
        String comentario = Datos.texto(body.get("comentario")).replaceAll("\\s+", " ");
        if (comentario.length() > 300) {
            throw new ApiException(400, "El comentario no puede superar los 300 caracteres.");
        }

        Map<String, Object> fila = vistas.pedidoCompleto(idPedido);
        if (fila == null) {
            throw new ApiException(404, "Pedido no encontrado.");
        }
        if (!Datos.bool(fila.get("pagado"))) {
            throw new ApiException(409, "Puedes dejar tu reseña cuando el pago esté verificado.");
        }

        Resena resena = new Resena();
        resena.setIdPedido(idPedido);
        resena.setNombrePublico(nombrePublico((String) fila.get("nombre_cliente")));
        resena.setCalificacion(calificacion);
        resena.setComentario(comentario.isEmpty() ? null : comentario);
        try {
            resenas.save(resena);
        } catch (DataIntegrityViolationException e) {
            String causa = String.valueOf(e.getMostSpecificCause().getMessage());
            if (causa.contains("Duplicate")) {
                throw new ApiException(409, "Este pedido ya tiene una reseña. ¡Gracias!").con("yaExiste", true);
            }
            throw e;
        }
    }

    /** Si la tabla está vacía crea 3 reseñas de ejemplo para que el inicio no se vea vacío. */
    public void sembrarEjemplos() {
        if (resenas.count() > 0) {
            return;
        }
        Object[][] ejemplos = {
            {"Camila R.", 5, "Pedí desde la mesa y la hamburguesa llegó caliente y rápido. Cero confusiones con el pedido, súper fácil."},
            {"Andrés G.", 5, "La salchipapa está buenísima y la porción es generosa. Pagar desde el celular fue muy cómodo."},
            {"Laura P.", 4, "Las alitas BBQ estuvieron deliciosas y el ambiente es muy agradable. Vamos a volver con la familia."},
        };
        for (Object[] e : ejemplos) {
            Resena r = new Resena();
            r.setNombrePublico((String) e[0]);
            r.setCalificacion((Integer) e[1]);
            r.setComentario((String) e[2]);
            resenas.save(r);
        }
    }
}
