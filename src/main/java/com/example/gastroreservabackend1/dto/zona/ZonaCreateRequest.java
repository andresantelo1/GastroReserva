package com.example.gastroreservabackend1.dto.zona;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ZonaCreateRequest(
        @NotBlank(message = "El nombre de la zona es obligatorio")
        @Size(max = 255, message = "El nombre de la zona no puede superar 255 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String descripcion
) {
}
