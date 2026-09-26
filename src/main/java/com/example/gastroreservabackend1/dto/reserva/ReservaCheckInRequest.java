package com.example.gastroreservabackend1.dto.reserva;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReservaCheckInRequest(
        @NotNull(message = "La mesa es obligatoria")
        @Positive(message = "El identificador de mesa debe ser mayor que cero")
        Long mesaId,

        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {
}
