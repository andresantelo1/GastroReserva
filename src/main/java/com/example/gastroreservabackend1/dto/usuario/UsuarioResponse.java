package com.example.gastroreservabackend1.dto.usuario;

import com.example.gastroreservabackend1.model.RolUsuario;

import java.time.Instant;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        RolUsuario rol,
        boolean activo,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
