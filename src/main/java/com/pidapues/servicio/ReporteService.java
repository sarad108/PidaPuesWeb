package com.pidapues.servicio;

import com.pidapues.excepcion.ApiException;
import com.pidapues.repositorio.InventarioRepository;
import com.pidapues.repositorio.ProductoRepository;
import com.pidapues.repositorio.VistaRepository;
import com.pidapues.util.Datos;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Reportes de administración (ventas, inventario, tiempos de servicio) y ajuste de existencias. */
@Service
public class ReporteService {

    private static final Pattern FORMATO_FECHA = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private final VistaRepository vistas;
    private final ProductoRepository productos;
    private final InventarioRepository inventarios;

    public ReporteService(VistaRepository vistas, ProductoRepository productos, InventarioRepository inventarios) {
        this.vistas = vistas;
        this.productos = productos;
        this.inventarios = inventarios;
    }

    // Rango del reporte: por defecto los últimos 30 días.
    private static LocalDate[] rangoFechas(String desdeTexto, String hastaTexto) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.minusDays(29);
        LocalDate hasta = hoy;
        try {
            if (desdeTexto != null && FORMATO_FECHA.matcher(desdeTexto).matches()) {
                desde = LocalDate.parse(desdeTexto);
            }
            if (hastaTexto != null && FORMATO_FECHA.matcher(hastaTexto).matches()) {
                hasta = LocalDate.parse(hastaTexto);
            }
        } catch (java.time.format.DateTimeParseException e) {
            // fecha imposible (por ejemplo 2026-02-31): se usan los valores por defecto
            desde = hoy.minusDays(29);
            hasta = hoy;
        }
        if (desde.isAfter(hasta)) {
            LocalDate cambio = desde;
            desde = hasta;
            hasta = cambio;
        }
        return new LocalDate[] {desde, hasta};
    }

    private List<Map<String, Object>> productosVendidos(LocalDate desde, LocalDate hasta) {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> f : vistas.productosVendidos(java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta))) {
            salida.add(Datos.mapa(
                    "id", f.get("id_producto"),
                    "nombre", f.get("nombre_producto"),
                    "categoria", f.get("nombre_categoria"),
                    "unidades", Datos.numero(f.get("unidades")),
                    "ingresos", Datos.numero(f.get("ingresos"))));
        }
        return salida;
    }

    public Map<String, Object> ventas(String desdeTexto, String hastaTexto) {
        LocalDate[] rango = rangoFechas(desdeTexto, hastaTexto);
        java.sql.Date desde = java.sql.Date.valueOf(rango[0]);
        java.sql.Date hasta = java.sql.Date.valueOf(rango[1]);

        List<Map<String, Object>> porDia = new ArrayList<>();
        BigDecimal totalVentas = BigDecimal.ZERO;
        long totalPedidos = 0;
        for (Map<String, Object> d : vistas.ventasPorDia(desde, hasta)) {
            BigDecimal total = new BigDecimal(String.valueOf(d.get("total")));
            long pedidos = ((Number) d.get("pedidos")).longValue();
            totalVentas = totalVentas.add(total);
            totalPedidos += pedidos;
            porDia.add(Datos.mapa("fecha", String.valueOf(d.get("fecha")), "total", Datos.numero(total), "pedidos", pedidos));
        }

        List<Map<String, Object>> porMetodo = new ArrayList<>();
        for (Map<String, Object> m : vistas.ventasPorMetodo(desde, hasta)) {
            porMetodo.add(Datos.mapa(
                    "metodo", m.get("metodo_pago"),
                    "total", Datos.numero(m.get("total")),
                    "pedidos", Datos.numero(m.get("pedidos"))));
        }

        long ticketPromedio = totalPedidos == 0 ? 0
                : totalVentas.divide(BigDecimal.valueOf(totalPedidos), 0, RoundingMode.HALF_UP).longValue();

        return Datos.mapa(
                "desde", rango[0].toString(),
                "hasta", rango[1].toString(),
                "resumen", Datos.mapa(
                        "totalVentas", Datos.numero(totalVentas),
                        "pedidosPagados", totalPedidos,
                        "ticketPromedio", ticketPromedio),
                "porDia", porDia,
                "porMetodo", porMetodo,
                "porProducto", productosVendidos(rango[0], rango[1]));
    }

    public Map<String, Object> inventario(String desdeTexto, String hastaTexto) {
        LocalDate[] rango = rangoFechas(desdeTexto, hastaTexto);
        Map<Object, Object> vendidos = new HashMap<>();
        for (Map<String, Object> p : productosVendidos(rango[0], rango[1])) {
            vendidos.put(p.get("id"), p.get("unidades"));
        }

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Map<String, Object> f : vistas.inventario()) {
            Object vendido = vendidos.get(f.get("id_producto"));
            filas.add(Datos.mapa(
                    "id", f.get("id_producto"),
                    "nombre", f.get("nombre_producto"),
                    "categoria", f.get("nombre_categoria"),
                    "disponible", Datos.bool(f.get("disponible")),
                    "stockActual", Datos.numero(f.get("stock_actual")),
                    "stockMinimo", Datos.numero(f.get("stock_minimo")),
                    "bajoMinimo", Datos.bool(f.get("bajo_minimo")),
                    "unidadesVendidas", vendido == null ? 0 : vendido));
        }
        return Datos.mapa("desde", rango[0].toString(), "hasta", rango[1].toString(), "productos", filas);
    }

    public List<Map<String, Object>> tiemposServicio() {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> t : vistas.tiemposServicio()) {
            salida.add(Datos.mapa(
                    "id", t.get("id_pedido"),
                    "mesa", t.get("num_mesa"),
                    "fechaHoraRegistro", Datos.iso(t.get("fecha_pedido")),
                    "fechaHoraInicioPreparacion", Datos.iso(t.get("hora_inicio_prep")),
                    "fechaHoraListo", Datos.iso(t.get("hora_fin")),
                    "fechaHoraEntregado", Datos.iso(t.get("fecha_entrega")),
                    "minutosTotales", Datos.numero(t.get("minutos_totales"))));
        }
        return salida;
    }

    private static boolean valido(Integer n) {
        return n != null && n >= 0 && n <= 100000;
    }

    public Map<String, Object> actualizarInventario(int id, Map<String, Object> body) {
        Integer stockActual = Datos.entero(body == null ? null : body.get("stockActual"));
        Integer stockMinimo = Datos.entero(body == null ? null : body.get("stockMinimo"));
        if (!valido(stockActual) || !valido(stockMinimo)) {
            throw new ApiException(400, "Las existencias y el mínimo deben ser números enteros entre 0 y 100000.");
        }
        if (!productos.existsById(id)) {
            throw new ApiException(404, "Producto no encontrado.");
        }
        inventarios.guardar(id, stockActual, stockMinimo);
        return Datos.mapa("ok", true, "id", id, "stockActual", stockActual, "stockMinimo", stockMinimo);
    }
}
