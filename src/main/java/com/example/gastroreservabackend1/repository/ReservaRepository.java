package com.example.gastroreservabackend1.repository;

import com.example.gastroreservabackend1.model.EstadoReserva;
import com.example.gastroreservabackend1.model.Reserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {

    Optional<Reserva> findByIdAndClienteUsuarioEmailIgnoreCase(Long id, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select coalesce(sum(r.cantidadPersonas), 0)
            from Reserva r
            where r.turno.id = :turnoId
              and r.fecha = :fecha
              and r.estado in :estados
            """)
    Long sumPersonasActivas(@Param("turnoId") Long turnoId,
                            @Param("fecha") LocalDate fecha,
                            @Param("estados") Collection<EstadoReserva> estados);

    @Query("""
            select case when count(r) > 0 then true else false end
            from Reserva r
            where r.mesa.id = :mesaId
              and r.estado in :estados
              and r.inicio < :fin
              and r.fin > :inicio
            """)
    boolean existsSolapamiento(@Param("mesaId") Long mesaId,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fin") LocalDateTime fin,
                               @Param("estados") Collection<EstadoReserva> estados);

    @Query("""
            select case when count(r) > 0 then true else false end
            from Reserva r
            where r.mesa.id = :mesaId
              and r.id <> :reservaId
              and r.estado in :estados
              and r.inicio < :fin
              and r.fin > :inicio
            """)
    boolean existsSolapamientoExcluyendoReserva(@Param("mesaId") Long mesaId,
                                                 @Param("reservaId") Long reservaId,
                                                 @Param("inicio") LocalDateTime inicio,
                                                 @Param("fin") LocalDateTime fin,
                                                 @Param("estados") Collection<EstadoReserva> estados);

    boolean existsByMesaIdAndEstadoInAndCantidadPersonasGreaterThan(
            Long mesaId, Collection<EstadoReserva> estados, Integer capacidad);

    @Query("""
            select sum(r.cantidadPersonas)
            from Reserva r
            where r.turno.id = :turnoId
              and r.estado in :estados
            group by r.fecha
            """)
    List<Long> findCapacidadesReservadasPorFecha(
            @Param("turnoId") Long turnoId,
            @Param("estados") Collection<EstadoReserva> estados);
}
