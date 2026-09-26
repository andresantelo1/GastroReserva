package com.example.gastroreservabackend1.dto.reserva;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ReservaOperativaCreateRequest(
        @NotNull(message = "El cliente es obligatorio")
        @Positive(message = "El identificador del cliente debe ser mayor que cero")
        Long clienteId,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "El turno es obligatorio")
        @Positive(message = "El identificador del turno debe ser mayor que cero")
        Long turnoId,

        @NotNull(message = "La mesa es obligatoria")
        @Positive(message = "El identificador de la mesa debe ser mayor que cero")
        Long mesaId,

        @NotNull(message = "La cantidad de personas es obligatoria")
        @Positive(message = "La cantidad de personas debe ser mayor que cero")
        Integer cantidadPersonas,

        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones
) {
}
