package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.turno.TurnoCreateRequest;
import com.example.gastroreservabackend1.dto.turno.TurnoResponse;
import com.example.gastroreservabackend1.dto.turno.TurnoUpdateRequest;
import com.example.gastroreservabackend1.service.TurnoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@Validated
public class TurnoController {

    private final TurnoService turnoService;

    public TurnoController(TurnoService turnoService) {
        this.turnoService = turnoService;
    }

    @GetMapping
    public List<TurnoResponse> listarTurnos(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String nombre) {
        return turnoService.listar(activo, nombre);
    }

    @GetMapping("/{id}")
    public TurnoResponse buscarTurno(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id) {
        return turnoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<TurnoResponse> crearTurno(@Valid @RequestBody TurnoCreateRequest request) {
        TurnoResponse response = turnoService.crear(request);
        return ResponseEntity.created(URI.create("/api/turnos/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public TurnoResponse actualizarTurno(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id,
            @Valid @RequestBody TurnoUpdateRequest request) {
        return turnoService.actualizar(id, request);
    }
}
