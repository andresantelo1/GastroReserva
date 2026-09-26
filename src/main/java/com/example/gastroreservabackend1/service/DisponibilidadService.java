package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.reserva.DisponibilidadResponse;
import com.example.gastroreservabackend1.dto.reserva.MesaDisponibleResponse;
import com.example.gastroreservabackend1.dto.reserva.TurnoReservaSummaryResponse;
import com.example.gastroreservabackend1.dto.zona.ZonaSummaryResponse;
import com.example.gastroreservabackend1.exception.BusinessRuleException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.model.Mesa;
import com.example.gastroreservabackend1.model.Turno;
import com.example.gastroreservabackend1.repository.MesaRepository;
import com.example.gastroreservabackend1.repository.ReservaRepository;
import com.example.gastroreservabackend1.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DisponibilidadService {

    private final TurnoRepository turnoRepository;
    private final MesaRepository mesaRepository;
    private final ReservaRepository reservaRepository;

    public DisponibilidadService(TurnoRepository turnoRepository,
                                 MesaRepository mesaRepository,
                                 ReservaRepository reservaRepository) {
        this.turnoRepository = turnoRepository;
        this.mesaRepository = mesaRepository;
        this.reservaRepository = reservaRepository;
    }

    public DisponibilidadResponse consultar(LocalDate fecha,
                                             Long turnoId,
                                             Integer cantidadPersonas,
                                             Long zonaId) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el turno con id " + turnoId));
        if (!turno.isActivo()) {
            throw new BusinessRuleException("El turno seleccionado no está activo");
        }

        ReservaPolicy.Intervalo intervalo = ReservaPolicy.calcularIntervalo(fecha, turno);
        long reservada = reservaRepository.sumPersonasActivas(
                turnoId, fecha, ReservaPolicy.ESTADOS_QUE_BLOQUEAN);
        long disponible = Math.max(0L, (long) turno.getCapacidadMaxima() - reservada);

        List<MesaDisponibleResponse> mesas = disponible < cantidadPersonas
                ? List.of()
                : mesaRepository.findDisponibles(
                                cantidadPersonas,
                                zonaId,
                                intervalo.inicio(),
                                intervalo.fin(),
                                ReservaPolicy.ESTADOS_QUE_BLOQUEAN)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return new DisponibilidadResponse(
                fecha,
                intervalo.inicio(),
                intervalo.fin(),
                cantidadPersonas,
                toTurnoResponse(turno),
                Math.toIntExact(reservada),
                Math.toIntExact(disponible),
                mesas
        );
    }

    private MesaDisponibleResponse toResponse(Mesa mesa) {
        return new MesaDisponibleResponse(
                mesa.getId(),
                mesa.getNumero(),
                mesa.getCapacidad(),
                new ZonaSummaryResponse(
                        mesa.getZona().getId(),
                        mesa.getZona().getNombre(),
                        mesa.getZona().isActiva())
        );
    }

    private TurnoReservaSummaryResponse toTurnoResponse(Turno turno) {
        return new TurnoReservaSummaryResponse(
                turno.getId(),
                turno.getNombre(),
                turno.getHoraInicio(),
                turno.getHoraFin(),
                turno.getCapacidadMaxima()
        );
    }
}
