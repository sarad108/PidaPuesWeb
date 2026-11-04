package com.pidapues.modelo;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de carrito_productos: (id_carrito, id_producto). */
public class CarritoProductoId implements Serializable {

    private Integer idCarrito;
    private Integer idProducto;

    public CarritoProductoId() {
    }

    public CarritoProductoId(Integer idCarrito, Integer idProducto) {
        this.idCarrito = idCarrito;
        this.idProducto = idProducto;
    }

    public Integer getIdCarrito() {
        return idCarrito;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CarritoProductoId)) {
            return false;
        }
        CarritoProductoId otro = (CarritoProductoId) o;
        return Objects.equals(idCarrito, otro.idCarrito) && Objects.equals(idProducto, otro.idProducto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCarrito, idProducto);
    }
}
