package com.pidapues.repositorio;

import com.pidapues.modelo.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegocioRepository extends JpaRepository<Negocio, Integer> {
}
