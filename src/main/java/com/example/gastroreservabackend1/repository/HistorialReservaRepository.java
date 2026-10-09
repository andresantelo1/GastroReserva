package com.example.gastroreservabackend1.repository;

import com.example.gastroreservabackend1.model.HistorialReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistorialReservaRepository extends JpaRepository<HistorialReserva, Long> {

    List<HistorialReserva> findByReservaIdOrderByCreadoEnAsc(Long reservaId);

    @Query("""
            select h from HistorialReserva h join fetch h.reserva r
            where r.cliente.usuario.id = :usuarioId and h.id > :despuesDeId order by h.id
            """)
    List<HistorialReserva> findNotificaciones(@Param("usuarioId") Long usuarioId,
                                            @Param("despuesDeId") long despuesDeId, Pageable pageable);
}
