package com.example.gastroreservabackend1.repository;

import com.example.gastroreservabackend1.model.MesaAbierta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface MesaAbiertaRepository extends JpaRepository<MesaAbierta, Long>, JpaSpecificationExecutor<MesaAbierta> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from MesaAbierta a where a.id = :id")
    Optional<MesaAbierta> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select (count(a) > 0) from MesaAbierta a
            where a.mesa.id = :mesaId and a.mesaActivaId is not null
              and a.inicio < :fin and a.finPrevisto > :inicio
            """)
    boolean existsSolapamiento(@Param("mesaId") Long mesaId, @Param("inicio") LocalDateTime inicio,
                              @Param("fin") LocalDateTime fin);

    boolean existsByMesaIdAndMesaActivaIdIsNotNullAndCantidadPersonasGreaterThan(Long mesaId, Integer capacidad);
}
