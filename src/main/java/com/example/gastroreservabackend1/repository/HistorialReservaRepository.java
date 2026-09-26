package com.example.gastroreservabackend1.repository;

import com.example.gastroreservabackend1.model.HistorialReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialReservaRepository extends JpaRepository<HistorialReserva, Long> {

    List<HistorialReserva> findByReservaIdOrderByCreadoEnAsc(Long reservaId);
}
