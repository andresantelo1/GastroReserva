package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.feedback.FeedbackDtos.*;
import com.example.gastroreservabackend1.service.FeedbackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@Validated
public class FeedbackController {
    private final FeedbackService service;
    public FeedbackController(FeedbackService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<Respuesta> crear(Authentication auth, @Valid @RequestBody Crear request) {
        Respuesta response = service.crear(auth.getName(), request);
        return ResponseEntity.created(URI.create("/api/feedback/" + response.id())).body(response);
    }
    @GetMapping
    public List<Respuesta> listar(Authentication auth, @RequestParam(required = false) @Min(1) @Max(5) Integer puntuacion,
                                 @RequestParam(required = false) LocalDate desde, @RequestParam(required = false) LocalDate hasta) {
        return service.listar(auth.getName(), false, puntuacion, desde, hasta);
    }
    @GetMapping("/mios")
    public List<Respuesta> mios(Authentication auth) { return service.listar(auth.getName(), true, null, null, null); }
    @GetMapping("/{id}")
    public Respuesta buscar(@PathVariable @Positive Long id, Authentication auth) { return service.buscar(id, auth.getName()); }
}
