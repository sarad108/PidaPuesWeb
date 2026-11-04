package com.pidapues.servicio;

import com.pidapues.repositorio.VistaRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

/**
 * Revisa que la base de datos tenga las tablas y vistas que usa la API y, si no las tiene, corre el
 * schema.sql. (La base de datos en sí la crea el driver con createDatabaseIfNotExist=true.)
 */
@Service
public class EsquemaService {

    private static final Logger LOG = LoggerFactory.getLogger(EsquemaService.class);

    public static final List<String> TABLAS_NECESARIAS = Arrays.asList(
            "rol", "usuario", "empleado", "intento_acceso", "categoria", "producto", "inventario",
            "cliente", "mesa", "carrito", "carrito_productos", "pedido", "detalle_pedido", "pago",
            "factura", "negocio", "resena");

    public static final List<String> VISTAS_NECESARIAS = Arrays.asList(
            "vista_productos_menu", "vista_pedidos_completos", "vista_detalle_pedido", "vista_cocina_pedidos",
            "vista_mesero_pedidos", "vista_admin_historial", "vista_admin_mesas", "vista_admin_tiempos_servicio",
            "vista_reporte_pagos", "vista_reporte_ventas_producto", "vista_reporte_inventario");

    // Tablas del esquema viejo: ya no se usan y no se borran.
    private static final List<String> TABLAS_ANTIGUAS =
            Arrays.asList("pedido_items", "pedidos", "mesas", "productos", "intentos_acceso");

    /** Resultado de revisar la base de datos. */
    public static class Diagnostico {
        public final List<String> tablasFaltantes = new ArrayList<>();
        public final List<String> vistasFaltantes = new ArrayList<>();
        public boolean completo;
        public boolean tieneEsquemaAntiguo;
        public boolean hayPedidosRelacionales;
    }

    private final VistaRepository vistas;
    private final DataSource dataSource;
    private final Environment entorno;

    public EsquemaService(VistaRepository vistas, DataSource dataSource, Environment entorno) {
        this.vistas = vistas;
        this.dataSource = dataSource;
        this.entorno = entorno;
    }

    public String nombreBd() {
        return entorno.getProperty("DB_NAME", "pidapues");
    }

    /** Mira qué tablas y vistas hay y cuáles faltan. */
    public Diagnostico diagnosticar() {
        Set<String> existentes = vistas.tablasYVistas();
        Diagnostico d = new Diagnostico();
        for (String t : TABLAS_NECESARIAS) {
            if (!existentes.contains(t)) {
                d.tablasFaltantes.add(t);
            }
        }
        for (String v : VISTAS_NECESARIAS) {
            if (!existentes.contains(v)) {
                d.vistasFaltantes.add(v);
            }
        }
        d.completo = d.tablasFaltantes.isEmpty() && d.vistasFaltantes.isEmpty();
        for (String t : TABLAS_ANTIGUAS) {
            if (existentes.contains(t)) {
                d.tieneEsquemaAntiguo = true;
            }
        }
        d.hayPedidosRelacionales = existentes.contains("pedido");
        return d;
    }

    private String leerSchema(boolean conservarTablasAntiguas) throws IOException {
        String sql;
        try (InputStream in = new ClassPathResource("db/schema.sql").getInputStream()) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        sql = Pattern.compile("CREATE DATABASE[^;]*;", Pattern.CASE_INSENSITIVE).matcher(sql).replaceAll("");
        sql = Pattern.compile("^\\s*USE\\s+\\w+\\s*;", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(sql).replaceFirst("");
        if (conservarTablasAntiguas) {
            for (String tabla : TABLAS_ANTIGUAS) {
                sql = Pattern.compile("^\\s*DROP TABLE IF EXISTS " + tabla + ";\\s*$",
                        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(sql).replaceAll("");
            }
        }
        return sql;
    }

    /** Corre el schema.sql. Con conservarTablasAntiguas=false borra todo y lo crea de nuevo. */
    public void aplicarEsquema(boolean conservarTablasAntiguas) throws IOException {
        String sql = leerSchema(conservarTablasAntiguas);
        ResourceDatabasePopulator poblador = new ResourceDatabasePopulator();
        poblador.addScript(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
        poblador.setSqlScriptEncoding("UTF-8");
        poblador.execute(dataSource);
    }

    /**
     * Se llama al arrancar el servidor. Solo crea las tablas si todavía no existe la tabla pedido,
     * así nunca se borran pedidos de verdad. Devuelve "ninguna", "creado" o "pendiente".
     */
    public String prepararEsquema() throws IOException {
        Diagnostico d = diagnosticar();
        if (d.completo) {
            return "ninguna";
        }
        boolean autoCrear = !"0".equals(entorno.getProperty("AUTO_CREAR_BD"));
        if (autoCrear && !d.hayPedidosRelacionales) {
            LOG.info("La base de datos '{}' no tiene el modelo relacional que usa la API. Creándolo ahora "
                    + "(tus tablas antiguas no se tocan)...", nombreBd());
            aplicarEsquema(true);
            d = diagnosticar();
            if (d.completo) {
                LOG.info("Modelo relacional creado: tablas, vistas, productos, mesas y usuarios de ejemplo.");
                return "creado";
            }
        }
        return "pendiente";
    }

    /** Mensajes para los errores más comunes al arrancar. */
    public static List<String> explicarError(Throwable error) {
        StringBuilder cadena = new StringBuilder();
        Throwable actual = error;
        String motivo = error == null ? "" : String.valueOf(error.getMessage());
        while (actual != null) {
            cadena.append(actual.getMessage()).append(' ');
            actual = actual.getCause();
        }
        String todo = cadena.toString();
        List<String> lineas = new ArrayList<>();
        lineas.add("Motivo: " + motivo);
        if (todo.contains("Communications link failure") || todo.contains("Connection refused")
                || todo.contains("UnknownHost") || todo.contains("timed out")) {
            lineas.add("No se pudo llegar a MySQL: ¿está encendido y son correctos DB_HOST y DB_PORT en el archivo .env?");
        } else if (todo.contains("Access denied")) {
            lineas.add("Usuario o contraseña de MySQL incorrectos: revisa DB_USER y DB_PASSWORD en el archivo .env.");
        } else if (todo.contains("Unknown database")) {
            lineas.add("La base de datos no existe: revisa DB_NAME en el archivo .env.");
        } else {
            lineas.add("Corre con --bd=revisar para ver qué tablas faltan, o con --bd=init para recrear la base desde cero.");
        }
        return lineas;
    }
}
