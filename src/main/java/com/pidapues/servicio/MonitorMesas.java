package com.pidapues.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Libera las mesas inactivas (30 min) cada minuto, sin depender de que haya una pantalla abierta. */
@Component
public class MonitorMesas {

    private static final Logger LOG = LoggerFactory.getLogger(MonitorMesas.class);

    private final MesaService mesas;

    public MonitorMesas(MesaService mesas) {
        this.mesas = mesas;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void liberarInactivas() {
        try {
            mesas.liberarInactivas();
        } catch (Exception e) {
            LOG.warn("Error liberando mesas: {}", e.getMessage());
        }
    }
}
