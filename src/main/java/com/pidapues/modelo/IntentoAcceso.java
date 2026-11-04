package com.pidapues.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla `intento_acceso`. */
@Entity
@Table(name = "intento_acceso")
public class IntentoAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_intento")
    private Integer idIntento;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "exitoso")
    private Boolean exitoso;

    public IntentoAcceso() {
    }

    public Integer getIdIntento() {
        return idIntento;
    }

    public void setIdIntento(Integer idIntento) {
        this.idIntento = idIntento;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Boolean getExitoso() {
        return exitoso;
    }

    public void setExitoso(Boolean exitoso) {
        this.exitoso = exitoso;
    }
}
