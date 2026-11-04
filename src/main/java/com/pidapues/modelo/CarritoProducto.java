package com.pidapues.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Tabla `carrito_productos`: productos y cantidades dentro de cada carrito. */
@Entity
@Table(name = "carrito_productos")
@IdClass(CarritoProductoId.class)
public class CarritoProducto {

    @Id
    @Column(name = "id_carrito")
    private Integer idCarrito;

    @Id
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "cantidad")
    private Integer cantidad;

    public CarritoProducto() {
    }

    public CarritoProducto(Integer idCarrito, Integer idProducto, Integer cantidad) {
        this.idCarrito = idCarrito;
        this.idProducto = idProducto;
        this.cantidad = cantidad;
    }

    public Integer getIdCarrito() {
        return idCarrito;
    }

    public void setIdCarrito(Integer idCarrito) {
        this.idCarrito = idCarrito;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}
