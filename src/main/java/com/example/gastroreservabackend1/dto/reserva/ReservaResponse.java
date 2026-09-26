package com.example.gastroreservabackend1.dto.reserva;

import com.example.gastroreservabackend1.model.EstadoReserva;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,
        ClienteReservaSummaryResponse cliente,
        TurnoReservaSummaryResponse turno,
        MesaReservaSummaryResponse mesa,
        LocalDate fecha,
        LocalDateTime inicio,
        LocalDateTime fin,
        Integer cantidadPersonas,
        EstadoReserva estado,
        String observaciones,
        Long creadoPorUsuarioId,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
