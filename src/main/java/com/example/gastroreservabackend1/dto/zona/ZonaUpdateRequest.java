package com.example.gastroreservabackend1.dto.zona;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ZonaUpdateRequest(
        @NotBlank(message = "El nombre de la zona es obligatorio")
        @Size(max = 255, message = "El nombre de la zona no puede superar 255 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String descripcion,

        @NotNull(message = "El estado activo de la zona es obligatorio")
        Boolean activa
) {
}
