package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.pedido.PedidoDtos.*;
import com.example.gastroreservabackend1.service.MesaAbiertaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/mesas-abiertas")
@Validated
public class MesaAbiertaController {
    private final MesaAbiertaService service;
    public MesaAbiertaController(MesaAbiertaService service) { this.service = service; }
    @GetMapping
    public List<MesaAbiertaRespuesta> listar(Authentication auth, @RequestParam(required = false) Boolean abierta,
                                           @RequestParam(required = false) @Positive Long mesaId) {
        return service.listar(auth.getName(), abierta, mesaId);
    }
    @GetMapping("/{id}")
    public MesaAbiertaRespuesta buscar(@PathVariable @Positive Long id, Authentication auth) {
        return service.buscar(id, auth.getName());
    }
    @PostMapping
    public ResponseEntity<MesaAbiertaRespuesta> abrir(Authentication auth, @Valid @RequestBody AbrirMesa request) {
        MesaAbiertaRespuesta response = service.abrir(auth.getName(), request);
        return ResponseEntity.created(URI.create("/api/mesas-abiertas/" + response.id())).body(response);
    }
    @PatchMapping("/{id}/finalizar")
    public MesaAbiertaRespuesta finalizar(@PathVariable @Positive Long id, Authentication auth) {
        return service.finalizar(id, auth.getName());
    }
}
