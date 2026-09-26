package com.example.gastroreservabackend1.repository;

import com.example.gastroreservabackend1.model.Zona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZonaRepository extends JpaRepository<Zona, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Zona> findAllByOrderByNombreAsc();

    List<Zona> findByActivaOrderByNombreAsc(boolean activa);

    List<Zona> findByNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);

    List<Zona> findByActivaAndNombreContainingIgnoreCaseOrderByNombreAsc(boolean activa, String nombre);
}
