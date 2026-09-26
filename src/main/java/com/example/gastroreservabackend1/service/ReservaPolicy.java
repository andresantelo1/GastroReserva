package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.model.EstadoReserva;
import com.example.gastroreservabackend1.model.Turno;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

final class ReservaPolicy {

    static final Set<EstadoReserva> ESTADOS_QUE_BLOQUEAN = Collections.unmodifiableSet(
            EnumSet.of(EstadoReserva.SOLICITADA, EstadoReserva.CONFIRMADA, EstadoReserva.SENTADA));

    private static final Map<EstadoReserva, Set<EstadoReserva>> TRANSICIONES = Map.of(
            EstadoReserva.SOLICITADA,
            Collections.unmodifiableSet(EnumSet.of(EstadoReserva.CONFIRMADA, EstadoReserva.CANCELADA)),
            EstadoReserva.CONFIRMADA,
            Collections.unmodifiableSet(EnumSet.of(
                    EstadoReserva.SENTADA, EstadoReserva.CANCELADA, EstadoReserva.NO_SHOW)),
            EstadoReserva.SENTADA,
            Collections.unmodifiableSet(EnumSet.of(EstadoReserva.FINALIZADA)),
            EstadoReserva.FINALIZADA, Collections.emptySet(),
            EstadoReserva.CANCELADA, Collections.emptySet(),
            EstadoReserva.NO_SHOW, Collections.emptySet()
    );

    private ReservaPolicy() {
    }

    static Intervalo calcularIntervalo(LocalDate fecha, Turno turno) {
        LocalDateTime inicio = fecha.atTime(turno.getHoraInicio());
        LocalDateTime fin = fecha.atTime(turno.getHoraFin());
        if (!fin.isAfter(inicio)) {
            fin = fin.plusDays(1);
        }
        return new Intervalo(inicio, fin);
    }

    static boolean permite(EstadoReserva actual, EstadoReserva nuevo) {
        return TRANSICIONES.getOrDefault(actual, Collections.emptySet()).contains(nuevo);
    }

    record Intervalo(LocalDateTime inicio, LocalDateTime fin) {
    }
}
