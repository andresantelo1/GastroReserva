package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.reporte.ReporteDtos.*;
import com.example.gastroreservabackend1.service.ReporteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    private final ReporteService service;
    public ReporteController(ReporteService service) { this.service = service; }
    @GetMapping("/ocupacion-actual")
    public OcupacionActual ocupacion(Authentication auth) { return service.ocupacion(auth.getName()); }
    @GetMapping("/reservas")
    public Resumen reservas(Authentication auth, @RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        return service.resumen(auth.getName(), desde, hasta);
    }
}
