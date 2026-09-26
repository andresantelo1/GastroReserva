package com.example.gastroreservabackend1.dto.reserva;

import com.example.gastroreservabackend1.model.EstadoReserva;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReservaEstadoUpdateRequest(
        @NotNull(message = "El nuevo estado es obligatorio")
        EstadoReserva estado,

        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {
}
