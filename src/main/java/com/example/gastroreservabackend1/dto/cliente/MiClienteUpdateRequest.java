package com.example.gastroreservabackend1.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MiClienteUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String telefono
) {
}
