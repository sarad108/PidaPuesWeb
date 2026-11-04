package com.pidapues.repositorio;

import com.pidapues.modelo.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    /** Aprueba el pago solo si sigue pendiente (evita aprobarlo dos veces). */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE pago SET estado_pago = 'aprobado', fecha_hora = NOW(), referencia_pasarela = :referencia "
            + "WHERE id_pedido = :idPedido AND estado_pago = 'pendiente'",
           nativeQuery = true)
    int aprobar(@Param("idPedido") Integer idPedido, @Param("referencia") String referencia);

    /** Marca un movimiento reciente para no liberar la mesa por inactividad mientras se espera el código. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE pago SET fecha_hora = NOW() WHERE id_pedido = :idPedido AND estado_pago = 'pendiente'",
           nativeQuery = true)
    int tocar(@Param("idPedido") Integer idPedido);
}
