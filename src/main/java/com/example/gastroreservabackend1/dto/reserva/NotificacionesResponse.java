package com.example.gastroreservabackend1.dto.reserva;

import com.example.gastroreservabackend1.model.EstadoReserva;
import com.example.gastroreservabackend1.model.TipoEventoReserva;
import java.time.Instant;
import java.util.List;

public record NotificacionesResponse(List<Evento> eventos, long ultimoId, boolean hayMas) {
    public record Evento(Long id, Long reservaId, TipoEventoReserva tipo, EstadoReserva estado, Instant creadoEn) {}
}
