package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.zona.ZonaCreateRequest;
import com.example.gastroreservabackend1.dto.zona.ZonaResponse;
import com.example.gastroreservabackend1.dto.zona.ZonaUpdateRequest;
import com.example.gastroreservabackend1.service.ZonaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/zonas")
@Validated
public class ZonaController {

    private final ZonaService zonaService;

    public ZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    @GetMapping
    public List<ZonaResponse> listarZonas(
            @RequestParam(required = false) Boolean activa,
            @RequestParam(required = false) String nombre) {
        return zonaService.listar(activa, nombre);
    }

    @GetMapping("/{id}")
    public ZonaResponse buscarZona(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id) {
        return zonaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ZonaResponse> crearZona(@Valid @RequestBody ZonaCreateRequest request) {
        ZonaResponse response = zonaService.crear(request);
        return ResponseEntity.created(URI.create("/api/zonas/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ZonaResponse actualizarZona(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id,
            @Valid @RequestBody ZonaUpdateRequest request) {
        return zonaService.actualizar(id, request);
    }
}
