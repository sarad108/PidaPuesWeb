package com.pidapues.repositorio;

import com.pidapues.modelo.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findFirstByCedulaOrderByIdClienteDesc(String cedula);
}
