package com.pidapues.repositorio;

import com.pidapues.modelo.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface InventarioRepository extends JpaRepository<Inventario, Integer> {

    /** Descuenta unidades vendidas sin bajar de 0. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE inventario SET stock_actual = GREATEST(stock_actual - :unidades, 0) WHERE id_producto = :id",
           nativeQuery = true)
    int descontar(@Param("id") Integer id, @Param("unidades") Integer unidades);

    /** Crea o actualiza las existencias de un producto. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "INSERT INTO inventario (id_producto, stock_actual, stock_minimo) VALUES (:id, :actual, :minimo) "
            + "ON DUPLICATE KEY UPDATE stock_actual = :actual, stock_minimo = :minimo",
           nativeQuery = true)
    int guardar(@Param("id") Integer id, @Param("actual") Integer actual, @Param("minimo") Integer minimo);
}
