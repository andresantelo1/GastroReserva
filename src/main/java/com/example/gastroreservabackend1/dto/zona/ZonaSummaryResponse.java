package com.example.gastroreservabackend1.dto.zona;

public record ZonaSummaryResponse(
        Long id,
        String nombre,
        boolean activa
) {
}
