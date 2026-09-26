package com.example.gastroreservabackend1.dto.cliente;

import jakarta.validation.constraints.Size;

public record MiClienteCreateRequest(
        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String telefono
) {
}
