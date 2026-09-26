package com.example.gastroreservabackend1.dto.zona;

public record ZonaResponse(
        Long id,
        String nombre,
        String descripcion,
        boolean activa
) {
}
