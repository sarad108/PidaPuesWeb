package com.pidapues.repositorio;

import com.pidapues.modelo.Mesa;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MesaRepository extends JpaRepository<Mesa, Integer> {

    /** La mesa libre de menor número, bloqueada para que dos pedidos simultáneos no tomen la misma. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Mesa> findFirstByEstadoOrderByNumMesaAsc(String estado);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Mesa m set m.estado = :estado where m.idMesa = :idMesa")
    int cambiarEstado(@Param("idMesa") Integer idMesa, @Param("estado") String estado);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Mesa m set m.estado = 'libre' where m.idMesa in :ids")
    int liberar(@Param("ids") List<Integer> ids);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Mesa m set m.estado = 'libre'")
    int liberarTodas();
}
