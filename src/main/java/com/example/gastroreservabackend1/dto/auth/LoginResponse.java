package com.example.gastroreservabackend1.dto.auth;

import com.example.gastroreservabackend1.dto.usuario.UsuarioResponse;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UsuarioResponse usuario
) {
}
