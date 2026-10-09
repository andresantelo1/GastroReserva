package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.reserva.NotificacionesResponse;
import com.example.gastroreservabackend1.model.RolUsuario;
import com.example.gastroreservabackend1.repository.HistorialReservaRepository;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class NotificacionService {
    private final HistorialReservaRepository historial;
    private final ActorService actores;
    public NotificacionService(HistorialReservaRepository historial, ActorService actores) {
        this.historial = historial; this.actores = actores;
    }

    public NotificacionesResponse consultar(String email, @Min(0) long despuesDeId, @Min(1) @Max(100) int limite) {
        var actor = actores.exigir(email, RolUsuario.CLIENTE);
        var filas = historial.findNotificaciones(actor.getId(), despuesDeId, PageRequest.of(0, limite + 1));
        var eventos = filas.stream().limit(limite).map(h -> new NotificacionesResponse.Evento(
                h.getId(), h.getReserva().getId(), h.getTipoEvento(), h.getEstadoNuevo(), h.getCreadoEn())).toList();
        return new NotificacionesResponse(eventos, eventos.isEmpty() ? despuesDeId : eventos.getLast().id(), filas.size() > limite);
    }
}
