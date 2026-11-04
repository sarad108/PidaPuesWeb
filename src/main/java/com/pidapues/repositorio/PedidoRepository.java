package com.pidapues.repositorio;

import com.pidapues.modelo.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    /** Último pedido creado en la mesa (null si no tiene). */
    @Query("select max(p.idPedido) from Pedido p where p.idMesa = :idMesa")
    Integer ultimoDeMesa(@Param("idMesa") Integer idMesa);

    /** Pago aprobado: el pedido pasa a la cola de cocina. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE pedido SET estado = 'pendiente' WHERE id_pedido = :id AND estado = 'pendiente_pago'",
           nativeQuery = true)
    int pasarAPendiente(@Param("id") Integer id);

    /**
     * Cocina empieza a preparar. UPDATE atómico: solo se aplica si el pago está aprobado y el pedido
     * sigue en "pendiente", así dos pantallas de cocina no pueden pisarse.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE pedido p JOIN pago pg ON pg.id_pedido = p.id_pedido "
            + "SET p.estado = 'en_preparacion', p.hora_inicio_prep = NOW(), p.id_cocina = COALESCE(p.id_cocina, :idEmpleado) "
            + "WHERE p.id_pedido = :id AND pg.estado_pago = 'aprobado' AND p.estado = 'pendiente'",
           nativeQuery = true)
    int iniciarPreparacion(@Param("id") Integer id, @Param("idEmpleado") Integer idEmpleado);

    /** Cocina termina: solo si el pedido sigue en "en_preparacion". */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE pedido p JOIN pago pg ON pg.id_pedido = p.id_pedido "
            + "SET p.estado = 'listo', p.hora_fin = NOW(), p.id_cocina = COALESCE(p.id_cocina, :idEmpleado) "
            + "WHERE p.id_pedido = :id AND pg.estado_pago = 'aprobado' AND p.estado = 'en_preparacion'",
           nativeQuery = true)
    int marcarListo(@Param("id") Integer id, @Param("idEmpleado") Integer idEmpleado);

    /** El mesero entrega: solo si en este momento sigue en "listo". */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE pedido SET estado = 'entregado', fecha_entrega = NOW(), id_mesero = :idEmpleado "
            + "WHERE id_pedido = :id AND estado = 'listo'",
           nativeQuery = true)
    int entregar(@Param("id") Integer id, @Param("idEmpleado") Integer idEmpleado);

    /** Borra todos los pedidos; detalle, pago, factura y reseña se limpian en cascada. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Pedido p")
    int borrarTodos();
}
