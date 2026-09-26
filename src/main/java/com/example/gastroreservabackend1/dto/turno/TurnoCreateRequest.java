package com.example.gastroreservabackend1.dto.turno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record TurnoCreateRequest(
        @NotBlank(message = "El nombre del turno es obligatorio")
        @Size(max = 100, message = "El nombre del turno no puede superar 100 caracteres")
        String nombre,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        @NotNull(message = "La capacidad máxima es obligatoria")
        @Positive(message = "La capacidad máxima debe ser mayor que cero")
        Integer capacidadMaxima
) {
}
