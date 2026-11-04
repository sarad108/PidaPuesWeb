package com.pidapues.repositorio;

import com.pidapues.modelo.DetallePedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Integer> {

    /** Unidades por producto de un pedido: cada fila es {idProducto, suma de cantidad}. */
    @Query("select d.idProducto, sum(d.cantidad) from DetallePedido d where d.idPedido = :idPedido group by d.idProducto")
    List<Object[]> consumoPorProducto(@Param("idPedido") Integer idPedido);
}
