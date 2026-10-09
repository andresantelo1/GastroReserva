package com.example.gastroreservabackend1.repository;

import com.example.gastroreservabackend1.model.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {
    List<ItemPedido> findByPedidoIdOrderByIdAsc(Long pedidoId);
    Optional<ItemPedido> findByIdAndPedidoId(Long id, Long pedidoId);
    boolean existsByPedidoId(Long pedidoId);
}
