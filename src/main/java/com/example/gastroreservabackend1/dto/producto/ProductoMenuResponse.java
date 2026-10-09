package com.example.gastroreservabackend1.dto.producto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductoMenuResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        boolean disponible,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
