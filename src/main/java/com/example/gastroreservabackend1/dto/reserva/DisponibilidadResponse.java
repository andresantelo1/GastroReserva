package com.example.gastroreservabackend1.dto.reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DisponibilidadResponse(
        LocalDate fecha,
        LocalDateTime inicio,
        LocalDateTime fin,
        Integer cantidadPersonas,
        TurnoReservaSummaryResponse turno,
        Integer capacidadReservadaTurno,
        Integer capacidadDisponibleTurno,
        List<MesaDisponibleResponse> mesas
) {
}
