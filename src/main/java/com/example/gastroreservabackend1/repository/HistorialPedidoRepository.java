package com.example.gastroreservabackend1.repository;

import com.example.gastroreservabackend1.model.HistorialPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialPedidoRepository extends JpaRepository<HistorialPedido, Long> {
    List<HistorialPedido> findByPedidoIdOrderByIdAsc(Long pedidoId);
}
