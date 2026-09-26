package com.example.gastroreservabackend1.dto.cliente;

import java.time.Instant;

public record ClienteResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        boolean activo,
        Long usuarioId,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
