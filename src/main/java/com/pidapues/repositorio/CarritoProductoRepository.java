package com.pidapues.repositorio;

import com.pidapues.modelo.CarritoProducto;
import com.pidapues.modelo.CarritoProductoId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarritoProductoRepository extends JpaRepository<CarritoProducto, CarritoProductoId> {
}
