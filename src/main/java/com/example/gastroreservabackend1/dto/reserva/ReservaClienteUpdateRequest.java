package com.example.gastroreservabackend1.dto.reserva;

import jakarta.validation.constraints.*;

/** Corrección administrativa de la relación, no reprogramación ni cambio de estado. */
public record ReservaClienteUpdateRequest(
        @NotNull @Positive Long clienteId,
        @Size(max = 500) String observaciones,
        @NotBlank @Size(max = 500) String motivo,
        @NotNull @PositiveOrZero Long version
) { }
