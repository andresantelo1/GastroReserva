package com.example.gastroreservabackend1.dto.turno;

import java.time.Instant;
import java.time.LocalTime;

public record TurnoResponse(
        Long id,
        String nombre,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer capacidadMaxima,
        boolean activo,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
