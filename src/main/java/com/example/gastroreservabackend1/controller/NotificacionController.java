package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.reserva.NotificacionesResponse;
import com.example.gastroreservabackend1.service.NotificacionService;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificaciones")
@Validated
public class NotificacionController {
    private final NotificacionService service;
    public NotificacionController(NotificacionService service) { this.service = service; }
    @GetMapping
    public NotificacionesResponse consultar(Authentication auth, @RequestParam(defaultValue = "0") @Min(0) long despuesDeId,
                                            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limite) {
        return service.consultar(auth.getName(), despuesDeId, limite);
    }
}
