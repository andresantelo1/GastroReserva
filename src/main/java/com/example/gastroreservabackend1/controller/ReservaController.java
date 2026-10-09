package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.reserva.HistorialReservaResponse;
import com.example.gastroreservabackend1.dto.reserva.ReservaCheckInRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaCreateRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaEstadoUpdateRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaOperativaCreateRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaReasignacionRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaResponse;
import com.example.gastroreservabackend1.dto.reserva.ReservaClienteUpdateRequest;
import com.example.gastroreservabackend1.service.ReservaClienteService;
import com.example.gastroreservabackend1.model.EstadoReserva;
import com.example.gastroreservabackend1.service.ReservaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@Validated
public class ReservaController {

    private final ReservaService reservaService;
    private final ReservaClienteService reservaClienteService;

    public ReservaController(ReservaService reservaService, ReservaClienteService reservaClienteService) {
        this.reservaService = reservaService;
        this.reservaClienteService = reservaClienteService;
    }

    @PutMapping("/{id}/datos-cliente")
    public ReservaResponse actualizarDatosCliente(
            @PathVariable @Positive Long id,
            Authentication authentication,
            @Valid @RequestBody ReservaClienteUpdateRequest request) {
        return reservaClienteService.actualizar(id, authentication.getName(), request);
    }

    @GetMapping("/mias")
    public List<ReservaResponse> listarMias(
            Authentication authentication,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaHasta,
            @RequestParam(required = false) EstadoReserva estado) {
        return reservaService.listarMias(
                authentication.getName(), fechaDesde, fechaHasta, estado);
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crearPropia(
            Authentication authentication,
            @Valid @RequestBody ReservaCreateRequest request) {
        ReservaResponse response = reservaService.crearParaClienteActual(authentication.getName(), request);
        return ResponseEntity.created(URI.create("/api/reservas/" + response.id())).body(response);
    }

    @PatchMapping("/{id}/cancelar")
    public ReservaResponse cancelarPropia(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id,
            Authentication authentication) {
        return reservaService.cancelarActual(id, authentication.getName());
    }

    @PostMapping("/operativas")
    public ResponseEntity<ReservaResponse> crearOperativa(
            Authentication authentication,
            @Valid @RequestBody ReservaOperativaCreateRequest request) {
        ReservaResponse response = reservaService.crearOperativa(authentication.getName(), request);
        return ResponseEntity.created(URI.create("/api/reservas/" + response.id())).body(response);
    }

    @GetMapping
    public List<ReservaResponse> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaDesde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaHasta,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false)
            @Positive(message = "El identificador del cliente debe ser mayor que cero")
            Long clienteId,
            @RequestParam(required = false)
            @Positive(message = "El identificador del turno debe ser mayor que cero")
            Long turnoId,
            @RequestParam(required = false)
            @Positive(message = "El identificador de la mesa debe ser mayor que cero")
            Long mesaId) {
        return reservaService.listar(fechaDesde, fechaHasta, estado, clienteId, turnoId, mesaId);
    }

    @GetMapping("/{id}")
    public ReservaResponse buscarPorId(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id) {
        return reservaService.buscarPorId(id);
    }

    @GetMapping("/{id}/historial")
    public List<HistorialReservaResponse> listarHistorial(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id) {
        return reservaService.listarHistorial(id);
    }

    @PatchMapping("/{id}/estado")
    public ReservaResponse cambiarEstado(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id,
            Authentication authentication,
            @Valid @RequestBody ReservaEstadoUpdateRequest request) {
        return reservaService.cambiarEstado(id, authentication.getName(), request);
    }

    @PatchMapping("/{id}/check-in")
    public ReservaResponse realizarCheckIn(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id,
            Authentication authentication,
            @Valid @RequestBody ReservaCheckInRequest request) {
        return reservaService.realizarCheckIn(id, authentication.getName(), request);
    }

    @PatchMapping("/{id}/mesa")
    public ReservaResponse reasignarMesa(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id,
            Authentication authentication,
            @Valid @RequestBody ReservaReasignacionRequest request) {
        return reservaService.reasignarMesa(id, authentication.getName(), request);
    }
}
