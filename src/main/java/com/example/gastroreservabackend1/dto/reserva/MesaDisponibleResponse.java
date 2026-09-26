package com.example.gastroreservabackend1.dto.reserva;

import com.example.gastroreservabackend1.dto.zona.ZonaSummaryResponse;

public record MesaDisponibleResponse(
        Long id,
        Integer numero,
        Integer capacidad,
        ZonaSummaryResponse zona
) {
}
