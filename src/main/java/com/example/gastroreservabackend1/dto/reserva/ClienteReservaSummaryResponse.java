package com.example.gastroreservabackend1.dto.reserva;

public record ClienteReservaSummaryResponse(
        Long id,
        String nombre,
        String email,
        String telefono
) {
}
