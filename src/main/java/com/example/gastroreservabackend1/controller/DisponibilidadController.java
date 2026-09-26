package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.reserva.DisponibilidadResponse;
import com.example.gastroreservabackend1.service.DisponibilidadService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/disponibilidad")
@Validated
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping
    public DisponibilidadResponse consultar(
            @RequestParam
            @NotNull(message = "La fecha es obligatoria")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha,
            @RequestParam
            @NotNull(message = "El turno es obligatorio")
            @Positive(message = "El identificador del turno debe ser mayor que cero")
            Long turnoId,
            @RequestParam
            @NotNull(message = "La cantidad de personas es obligatoria")
            @Positive(message = "La cantidad de personas debe ser mayor que cero")
            Integer cantidadPersonas,
            @RequestParam(required = false)
            @Positive(message = "El identificador de zona debe ser mayor que cero")
            Long zonaId) {
        return disponibilidadService.consultar(fecha, turnoId, cantidadPersonas, zonaId);
    }
}
