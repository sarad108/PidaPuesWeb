package com.pidapues.excepcion;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Error de la API con su código HTTP. Se responde como JSON {"error": "...", ...extras},
 * el mismo formato que usaba la versión en Node.
 */
public class ApiException extends RuntimeException {

    private final int estado;
    private final Map<String, Object> extras = new LinkedHashMap<>();

    public ApiException(int estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    /** Agrega un campo extra al JSON del error (por ejemplo intentosRestantes). */
    public ApiException con(String clave, Object valor) {
        extras.put(clave, valor);
        return this;
    }

    public int getEstado() {
        return estado;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }
}
