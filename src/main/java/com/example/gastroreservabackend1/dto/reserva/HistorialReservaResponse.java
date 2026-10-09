package com.example.gastroreservabackend1.dto.reserva;

import com.example.gastroreservabackend1.model.EstadoReserva;
import com.example.gastroreservabackend1.model.TipoEventoReserva;

import java.time.Instant;

public record HistorialReservaResponse(
        Long id,
        TipoEventoReserva tipoEvento,
        EstadoReserva estadoAnterior,
        EstadoReserva estadoNuevo,
        String motivo,
        MesaReservaSummaryResponse mesaAnterior,
        MesaReservaSummaryResponse mesaNueva,
        Long cambiadoPorUsuarioId,
        String cambiadoPorNombre,
        Instant creadoEn,
        Long clienteAnteriorId,
        Long clienteNuevoId,
        String observacionesAnteriores,
        String observacionesNuevas
) {
}
