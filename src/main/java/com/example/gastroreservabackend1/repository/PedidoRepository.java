package com.example.gastroreservabackend1.repository;

import com.example.gastroreservabackend1.model.EstadoPedido;
import com.example.gastroreservabackend1.model.Pedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> findByIdForUpdate(@Param("id") Long id);

    // Sólo identificadores: no carga estado mutable antes de adquirir el bloqueo del origen.
    interface Origen {
        Long getReservaId();
        Long getMesaAbiertaId();
    }

    @Query("select p.reserva.id as reservaId, p.mesaAbierta.id as mesaAbiertaId from Pedido p where p.id = :id")
    Optional<Origen> findOrigen(@Param("id") Long id);

    boolean existsByReservaId(Long reservaId);
    boolean existsByMesaAbiertaId(Long mesaAbiertaId);
    boolean existsByReservaIdAndEstadoIn(Long reservaId, Collection<EstadoPedido> estados);
    boolean existsByMesaAbiertaIdAndEstadoIn(Long mesaAbiertaId, Collection<EstadoPedido> estados);
}
