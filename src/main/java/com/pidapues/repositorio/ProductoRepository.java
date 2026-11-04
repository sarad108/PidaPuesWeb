package com.pidapues.repositorio;

import com.pidapues.modelo.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    List<Producto> findByDisponibleTrue();
}
