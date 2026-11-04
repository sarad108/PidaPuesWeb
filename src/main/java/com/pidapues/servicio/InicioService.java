package com.pidapues.servicio;

import com.pidapues.repositorio.VistaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Tareas al arrancar el servidor: revisar la base, sembrar reseñas de ejemplo y guardar los PIN del .env. */
@Component
public class InicioService implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InicioService.class);

    private final EsquemaService esquema;
    private final VistaRepository vistas;
    private final ResenaService resenas;
    private final AuthService auth;
    private final EstadoPagos estado;
    private final Environment entorno;

    public InicioService(EsquemaService esquema, VistaRepository vistas, ResenaService resenas,
                         AuthService auth, EstadoPagos estado, Environment entorno) {
        this.esquema = esquema;
        this.vistas = vistas;
        this.resenas = resenas;
        this.auth = auth;
        this.estado = estado;
        this.entorno = entorno;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("empleados") || args.containsOption("bd")) {
            return; // modo línea de comandos: no se prepara el servidor
        }
        LOG.info("PidaPues escuchando en http://localhost:{}", entorno.getProperty("PORT", "3000"));
        try {
            // primero reviso que la base tenga las tablas; si no las tiene, las crea
            String accion = esquema.prepararEsquema();
            vistas.probarConexion();
            LOG.info("Conexión a MySQL correcta (base de datos: {})", esquema.nombreBd());

            if ("pendiente".equals(accion)) {
                EsquemaService.Diagnostico d = esquema.diagnosticar();
                LOG.error("A la base de datos le faltan tablas o vistas del modelo relacional: {} {}. "
                                + "Recréala con: --bd=init (borra y vuelve a crear las tablas con datos de ejemplo)",
                        d.tablasFaltantes, d.vistasFaltantes);
                return;
            }

            if (!"0".equals(entorno.getProperty("RESENAS_EJEMPLO"))) {
                resenas.sembrarEjemplos();
            }
            auth.sincronizarPines();
            // Los pedidos que ya estaban pagados no vuelven a mostrar el aviso a cocina.
            for (Integer id : vistas.idsPedidosEnCocina()) {
                estado.marcarNotificado(id);
            }
            LOG.info("Vistas y usuarios de acceso listos");
        } catch (Exception e) {
            LOG.error("No se pudo preparar la base de datos. El sitio va a cargar pero SIN datos.");
            for (String linea : EsquemaService.explicarError(e)) {
                LOG.error("   {}", linea);
            }
        }
    }
}
