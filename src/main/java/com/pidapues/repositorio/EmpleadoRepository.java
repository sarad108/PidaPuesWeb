package com.pidapues.repositorio;

import com.pidapues.modelo.Empleado;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    Optional<Empleado> findByCedulaAndActivoTrue(String cedula);

    Optional<Empleado> findByCedula(String cedula);

    List<Empleado> findAllByOrderByIdEmpleado();
}
