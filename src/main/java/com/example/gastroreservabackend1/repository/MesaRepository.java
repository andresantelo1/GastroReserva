package com.example.gastroreservabackend1.repository;

import com.example.gastroreservabackend1.model.Mesa;
import com.example.gastroreservabackend1.model.EstadoReserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, Long>, JpaSpecificationExecutor<Mesa> {

    boolean existsByNumero(Integer numero);

    boolean existsByNumeroAndIdNot(Integer numero, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mesa m join fetch m.zona where m.id = :id")
    Optional<Mesa> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select m from Mesa m
            join fetch m.zona z
            where m.activa = true
              and z.activa = true
              and m.capacidad >= :personas
              and (:zonaId is null or z.id = :zonaId)
              and not exists (
                  select r.id from Reserva r
                  where r.mesa = m
                    and r.estado in :estados
                    and r.inicio < :fin
                    and r.fin > :inicio
              )
            order by m.capacidad asc, m.numero asc
            """)
    List<Mesa> findDisponibles(@Param("personas") Integer personas,
                               @Param("zonaId") Long zonaId,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fin") LocalDateTime fin,
                               @Param("estados") Collection<EstadoReserva> estados);
}
