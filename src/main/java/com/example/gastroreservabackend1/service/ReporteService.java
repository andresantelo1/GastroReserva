package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.reporte.ReporteDtos.*;
import com.example.gastroreservabackend1.exception.BusinessRuleException;
import com.example.gastroreservabackend1.model.EstadoReserva;
import com.example.gastroreservabackend1.repository.MesaRepository;
import com.example.gastroreservabackend1.repository.ReservaRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.example.gastroreservabackend1.model.RolUsuario.*;

@Service
@Validated
@Transactional(readOnly = true)
public class ReporteService {
    private final ReservaRepository reservas;
    private final MesaRepository mesas;
    private final ActorService actores;
    private final Clock clock;
    public ReporteService(ReservaRepository reservas, MesaRepository mesas, ActorService actores, Clock clock) {
        this.reservas = reservas; this.mesas = mesas; this.actores = actores; this.clock = clock;
    }

    public OcupacionActual ocupacion(String email) {
        actores.exigir(email, ADMINISTRADOR, HOST);
        var conteo = mesas.contarOcupacion();
        long activas = conteo.getActivas();
        long ocupadas = conteo.getOcupadas();
        return new OcupacionActual(activas, ocupadas, porcentaje(ocupadas, activas), Instant.now(clock));
    }

    public Resumen resumen(String email, @NotNull LocalDate desde, @NotNull LocalDate hasta) {
        actores.exigir(email, ADMINISTRADOR, HOST);
        if (desde.isAfter(hasta) || ChronoUnit.DAYS.between(desde, hasta) > 365)
            throw new BusinessRuleException("Use un rango ordenado de hasta 366 días inclusive");
        var filas = reservas.contarPorFechaTurnoEstado(desde, hasta);
        Map<String, List<ReservaRepository.ConteoEstado>> grupos = new LinkedHashMap<>();
        long total = 0, atendidas = 0, noShow = 0, canceladas = 0;
        for (var fila : filas) {
            grupos.computeIfAbsent(fila.getFecha() + "/" + fila.getTurnoId(), key -> new ArrayList<>()).add(fila);
            total += fila.getReservas();
            if (fila.getEstado() == EstadoReserva.SENTADA || fila.getEstado() == EstadoReserva.FINALIZADA) atendidas += fila.getReservas();
            if (fila.getEstado() == EstadoReserva.NO_SHOW) noShow += fila.getReservas();
            if (fila.getEstado() == EstadoReserva.CANCELADA) canceladas += fila.getReservas();
        }
        List<TurnoDia> detalle = new ArrayList<>();
        for (var grupo : grupos.values()) {
            Map<EstadoReserva, Long> estados = new EnumMap<>(EstadoReserva.class);
            for (var estado : EstadoReserva.values()) estados.put(estado, 0L);
            long personas = 0;
            for (var fila : grupo) {
                estados.put(fila.getEstado(), fila.getReservas());
                if (ReservaPolicy.ESTADOS_QUE_BLOQUEAN.contains(fila.getEstado())) personas += fila.getPersonas();
            }
            var primera = grupo.getFirst();
            detalle.add(new TurnoDia(primera.getFecha(), primera.getTurnoId(), primera.getTurnoNombre(),
                    primera.getCapacidad(), estados, personas, porcentaje(personas, primera.getCapacidad())));
        }
        return new Resumen(desde, hasta, total, atendidas, noShow, canceladas, porcentaje(noShow, noShow + atendidas), detalle);
    }

    private BigDecimal porcentaje(long numerador, long denominador) {
        return denominador == 0 ? new BigDecimal("0.00") : BigDecimal.valueOf(numerador)
                .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominador), 2, RoundingMode.HALF_UP);
    }
}
