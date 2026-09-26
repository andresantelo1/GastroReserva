package com.example.gastroreservabackend1.dto.reserva;

import java.time.LocalTime;

public record TurnoReservaSummaryResponse(
        Long id,
        String nombre,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer capacidadMaxima
) {
}
