package com.pidapues.servicio;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Estado temporal en memoria de la simulación de pasarela: el código de verificación de cada pedido
 * y los pedidos cuyo pago ya se avisó a cocina. El modelo relacional no tiene dónde guardarlos y
 * ambos son temporales (se pierden si el servidor se reinicia).
 */
@Component
public class EstadoPagos {

    public static class CodigoPago {
        public final String codigo;
        public int intentos = 0;
        public boolean bloqueado = false;
        final long creado;

        CodigoPago(String codigo, long creado) {
            this.codigo = codigo;
            this.creado = creado;
        }
    }

    private static final long SEIS_HORAS = 6L * 60 * 60 * 1000;

    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<Integer, CodigoPago> codigos = new ConcurrentHashMap<>();
    private final Set<Integer> notificados = ConcurrentHashMap.newKeySet();

    /** Genera un código de 6 dígitos para el pedido y lo guarda. */
    public String guardarCodigo(int idPedido) {
        long ahora = System.currentTimeMillis();
        codigos.values().removeIf(c -> ahora - c.creado > SEIS_HORAS);
        String codigo = String.valueOf(100000 + aleatorio.nextInt(900000));
        codigos.put(idPedido, new CodigoPago(codigo, ahora));
        return codigo;
    }

    public CodigoPago obtener(int idPedido) {
        return codigos.get(idPedido);
    }

    public void quitarCodigo(int idPedido) {
        codigos.remove(idPedido);
    }

    public void marcarNotificado(int idPedido) {
        notificados.add(idPedido);
    }

    public boolean fueNotificado(int idPedido) {
        return notificados.contains(idPedido);
    }

    public void limpiarTodo() {
        codigos.clear();
        notificados.clear();
    }
}
