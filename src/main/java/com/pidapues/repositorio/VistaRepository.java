package com.pidapues.repositorio;

import java.sql.Date;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Lecturas sobre las VISTAS del schema.sql (menú, pedidos por rol, mesas, reportes).
 * Las vistas no son tablas, por eso se consultan con SQL y no con entidades JPA.
 */
@Repository
public class VistaRepository {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate nombrado;

    public VistaRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate nombrado) {
        this.jdbc = jdbc;
        this.nombrado = nombrado;
    }

    // ---------------- menú y pedidos ----------------

    public List<Map<String, Object>> productosMenu() {
        return jdbc.queryForList("SELECT * FROM vista_productos_menu");
    }

    /** Una fila de vista_pedidos_completos, o null si el pedido no existe. */
    public Map<String, Object> pedidoCompleto(int idPedido) {
        List<Map<String, Object>> filas =
                jdbc.queryForList("SELECT * FROM vista_pedidos_completos WHERE id_pedido = ?", idPedido);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /** Pedidos que ve cada rol: cocina, mesero o admin. */
    public List<Map<String, Object>> pedidosPorRol(String rol) {
        String vista;
        if ("cocina".equals(rol)) {
            vista = "vista_cocina_pedidos";
        } else if ("mesero".equals(rol)) {
            vista = "vista_mesero_pedidos";
        } else {
            vista = "vista_admin_historial";
        }
        return jdbc.queryForList("SELECT * FROM " + vista);
    }

    /** Líneas de varios pedidos con una sola consulta. */
    public List<Map<String, Object>> lineasDe(Collection<Integer> idsPedido) {
        if (idsPedido.isEmpty()) {
            return List.of();
        }
        return nombrado.queryForList(
                "SELECT * FROM vista_detalle_pedido WHERE id_pedido IN (:ids) ORDER BY id_detalle_pedido",
                new MapSqlParameterSource("ids", idsPedido));
    }

    public List<Integer> idsPedidosEnCocina() {
        return jdbc.queryForList("SELECT id_pedido FROM vista_cocina_pedidos", Integer.class);
    }

    // ---------------- mesas y tiempos ----------------

    public List<Map<String, Object>> mesasAdmin() {
        return jdbc.queryForList("SELECT * FROM vista_admin_mesas");
    }

    /** Mesas ocupadas cuya última interacción supera los minutos dados. */
    public List<Integer> mesasInactivas(int minutos) {
        return jdbc.queryForList(
                "SELECT id_mesa FROM vista_admin_mesas "
                        + "WHERE estado <> 'libre' AND ultima_interaccion IS NOT NULL "
                        + "AND ultima_interaccion < (NOW() - INTERVAL ? MINUTE)",
                Integer.class, minutos);
    }

    public List<Map<String, Object>> tiemposServicio() {
        return jdbc.queryForList("SELECT * FROM vista_admin_tiempos_servicio");
    }

    // ---------------- reportes ----------------

    public List<Map<String, Object>> ventasPorDia(Date desde, Date hasta) {
        return jdbc.queryForList(
                "SELECT fecha, SUM(valor_total) AS total, COUNT(*) AS pedidos FROM vista_reporte_pagos "
                        + "WHERE fecha BETWEEN ? AND ? GROUP BY fecha ORDER BY fecha",
                desde, hasta);
    }

    public List<Map<String, Object>> ventasPorMetodo(Date desde, Date hasta) {
        return jdbc.queryForList(
                "SELECT metodo_pago, SUM(valor_total) AS total, COUNT(*) AS pedidos FROM vista_reporte_pagos "
                        + "WHERE fecha BETWEEN ? AND ? GROUP BY metodo_pago ORDER BY total DESC",
                desde, hasta);
    }

    public List<Map<String, Object>> productosVendidos(Date desde, Date hasta) {
        return jdbc.queryForList(
                "SELECT id_producto, nombre_producto, nombre_categoria, "
                        + "SUM(cantidad) AS unidades, SUM(subtotal) AS ingresos "
                        + "FROM vista_reporte_ventas_producto WHERE fecha BETWEEN ? AND ? "
                        + "GROUP BY id_producto, nombre_producto, nombre_categoria "
                        + "ORDER BY unidades DESC, ingresos DESC",
                desde, hasta);
    }

    public List<Map<String, Object>> inventario() {
        return jdbc.queryForList("SELECT * FROM vista_reporte_inventario");
    }

    // ---------------- reseñas ----------------

    /** {total, promedio} de todas las reseñas. */
    public Map<String, Object> resumenResenas() {
        return jdbc.queryForMap("SELECT COUNT(*) AS total, AVG(calificacion) AS promedio FROM resena");
    }

    /** Últimas 9 reseñas con comentario. */
    public List<Map<String, Object>> ultimasResenas() {
        return jdbc.queryForList(
                "SELECT nombre_publico, calificacion, comentario, fecha FROM resena "
                        + "WHERE comentario IS NOT NULL AND comentario <> '' "
                        + "ORDER BY fecha DESC, id_resena DESC LIMIT 9");
    }

    // ---------------- esquema y demo ----------------

    /** Nombres (en minúscula) de todas las tablas y vistas de la base actual. */
    public Set<String> tablasYVistas() {
        List<String> nombres = jdbc.queryForList(
                "SELECT table_name AS nombre FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class);
        Set<String> r = new HashSet<>();
        for (String n : nombres) {
            r.add(n.toLowerCase());
        }
        return r;
    }

    public int borrarClientesExceptoGenerico(int idGenerico) {
        return jdbc.update("DELETE FROM cliente WHERE id_cliente <> ?", idGenerico);
    }

    public void probarConexion() {
        jdbc.queryForObject("SELECT 1", Integer.class);
    }
}
