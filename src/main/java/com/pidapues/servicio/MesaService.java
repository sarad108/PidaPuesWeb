package com.pidapues.servicio;

import com.pidapues.repositorio.MesaRepository;
import com.pidapues.repositorio.PedidoRepository;
import com.pidapues.repositorio.VistaRepository;
import com.pidapues.util.Datos;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MesaService {

    public static final int MINUTOS_INACTIVIDAD = 30;

    private final VistaRepository vistas;
    private final MesaRepository mesas;
    private final PedidoRepository pedidos;

    public MesaService(VistaRepository vistas, MesaRepository mesas, PedidoRepository pedidos) {
        this.vistas = vistas;
        this.mesas = mesas;
        this.pedidos = pedidos;
    }

    /** Libera las mesas cuya última interacción supera 30 minutos. */
    public void liberarInactivas() {
        List<Integer> ids = vistas.mesasInactivas(MINUTOS_INACTIVIDAD);
        if (!ids.isEmpty()) {
            mesas.liberar(ids);
        }
    }

    /** Libera la mesa solo si el pedido dado sigue siendo el último de esa mesa. */
    public void liberarMesaDelPedido(Integer idMesa, int idPedido) {
        Integer ultimo = pedidos.ultimoDeMesa(idMesa);
        if (ultimo != null && ultimo.intValue() == idPedido) {
            mesas.cambiarEstado(idMesa, "libre");
        }
    }

    public List<Map<String, Object>> listar() {
        liberarInactivas();
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> m : vistas.mesasAdmin()) {
            salida.add(Datos.mapa(
                    "numero", m.get("num_mesa"),
                    "estado", m.get("estado"),
                    "pedidoId", m.get("id_pedido"),
                    "ultimaInteraccion", Datos.iso(m.get("ultima_interaccion"))));
        }
        return salida;
    }
}
