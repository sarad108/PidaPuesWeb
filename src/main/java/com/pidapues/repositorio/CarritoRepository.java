package com.pidapues.repositorio;

import com.pidapues.modelo.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CarritoRepository extends JpaRepository<Carrito, Integer> {

    /** Borra el carrito de la mesa; carrito_productos se limpia en cascada en la base de datos. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Carrito c where c.idMesa = :idMesa")
    int borrarPorMesa(@Param("idMesa") Integer idMesa);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Carrito c")
    int borrarTodos();
}
