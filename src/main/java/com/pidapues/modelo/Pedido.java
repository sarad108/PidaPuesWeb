package com.pidapues.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Tabla `pedido`. */
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer idPedido;

    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "id_mesa")
    private Integer idMesa;

    @Column(name = "id_mesero")
    private Integer idMesero;

    @Column(name = "id_cocina")
    private Integer idCocina;

    @Column(name = "fecha_pedido", insertable = false, updatable = false)
    private LocalDateTime fechaPedido;

    @Column(name = "hora_inicio_prep", insertable = false, updatable = false)
    private LocalDateTime horaInicioPrep;

    @Column(name = "hora_fin", insertable = false, updatable = false)
    private LocalDateTime horaFin;

    @Column(name = "fecha_entrega", insertable = false, updatable = false)
    private LocalDateTime fechaEntrega;

    @Column(name = "total")
    private BigDecimal total;

    @Column(name = "estado")
    private String estado;

    public Pedido() {
    }

    public Integer getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(Integer idPedido) {
        this.idPedido = idPedido;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public Integer getIdMesa() {
        return idMesa;
    }

    public void setIdMesa(Integer idMesa) {
        this.idMesa = idMesa;
    }

    public Integer getIdMesero() {
        return idMesero;
    }

    public void setIdMesero(Integer idMesero) {
        this.idMesero = idMesero;
    }

    public Integer getIdCocina() {
        return idCocina;
    }

    public void setIdCocina(Integer idCocina) {
        this.idCocina = idCocina;
    }

    public LocalDateTime getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(LocalDateTime fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public LocalDateTime getHoraInicioPrep() {
        return horaInicioPrep;
    }

    public void setHoraInicioPrep(LocalDateTime horaInicioPrep) {
        this.horaInicioPrep = horaInicioPrep;
    }

    public LocalDateTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalDateTime horaFin) {
        this.horaFin = horaFin;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
