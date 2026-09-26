package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.mesa.MesaCreateRequest;
import com.example.gastroreservabackend1.dto.mesa.MesaResponse;
import com.example.gastroreservabackend1.dto.mesa.MesaUpdateRequest;
import com.example.gastroreservabackend1.service.MesaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/mesas")
@Validated
public class MesaController {

    private final MesaService mesaService;

    public MesaController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    @GetMapping
    public List<MesaResponse> listarMesas(
            @RequestParam(required = false) @Positive(message = "El id de zona debe ser mayor que cero") Long zonaId,
            @RequestParam(required = false) Boolean activa,
            @RequestParam(required = false) @Positive(message = "La capacidad mínima debe ser mayor que cero")
            Integer capacidadMinima,
            @RequestParam(required = false) String estado) {
        return mesaService.listar(zonaId, activa, capacidadMinima, estado);
    }

    @GetMapping("/{id}")
    public MesaResponse buscarMesa(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id) {
        return mesaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<MesaResponse> crearMesa(@Valid @RequestBody MesaCreateRequest request) {
        MesaResponse response = mesaService.crear(request);
        return ResponseEntity.created(URI.create("/api/mesas/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public MesaResponse actualizarMesa(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id,
            @Valid @RequestBody MesaUpdateRequest request) {
        return mesaService.actualizar(id, request);
    }
}
