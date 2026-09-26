package com.example.gastroreservabackend1.dto.mesa;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MesaUpdateRequest(
        @NotNull(message = "El número de mesa es obligatorio")
        @Positive(message = "El número de mesa debe ser mayor que cero")
        Integer numero,

        @NotNull(message = "La capacidad es obligatoria")
        @Positive(message = "La capacidad debe ser mayor que cero")
        Integer capacidad,

        @NotNull(message = "El estado activo de la mesa es obligatorio")
        Boolean activa,

        @NotNull(message = "La zona es obligatoria")
        @Positive(message = "El identificador de zona debe ser mayor que cero")
        Long zonaId
) {
}
