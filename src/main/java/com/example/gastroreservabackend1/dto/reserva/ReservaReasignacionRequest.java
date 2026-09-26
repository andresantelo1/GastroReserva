package com.example.gastroreservabackend1.dto.reserva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReservaReasignacionRequest(
        @NotNull(message = "La mesa nueva es obligatoria")
        @Positive(message = "El identificador de mesa debe ser mayor que cero")
        Long mesaId,

        @NotBlank(message = "El motivo de la reasignación es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {
}
