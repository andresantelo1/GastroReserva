package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.turno.TurnoCreateRequest;
import com.example.gastroreservabackend1.dto.turno.TurnoResponse;
import com.example.gastroreservabackend1.dto.turno.TurnoUpdateRequest;
import com.example.gastroreservabackend1.exception.BusinessRuleException;
import com.example.gastroreservabackend1.exception.ResourceConflictException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.model.Turno;
import com.example.gastroreservabackend1.repository.ReservaRepository;
import com.example.gastroreservabackend1.repository.TurnoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final ReservaRepository reservaRepository;

    public TurnoService(TurnoRepository turnoRepository, ReservaRepository reservaRepository) {
        this.turnoRepository = turnoRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<TurnoResponse> listar(Boolean activo, String nombre) {
        Specification<Turno> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (activo != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("activo"), activo));
        }
        if (StringUtils.hasText(nombre)) {
            String filter = "%" + nombre.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), filter));
        }

        return turnoRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "horaInicio"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TurnoResponse buscarPorId(Long id) {
        return toResponse(requireTurno(id));
    }

    @Transactional
    public TurnoResponse crear(TurnoCreateRequest request) {
        String nombre = request.nombre().trim();
        if (turnoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResourceConflictException("Ya existe un turno con el nombre '" + nombre + "'");
        }
        validarHorario(request.horaInicio(), request.horaFin());

        Turno turno = new Turno();
        aplicar(turno, nombre, request.horaInicio(), request.horaFin(), request.capacidadMaxima(), true);
        return toResponse(turnoRepository.save(turno));
    }

    @Transactional
    public TurnoResponse actualizar(Long id, TurnoUpdateRequest request) {
        Turno turno = requireTurno(id);
        String nombre = request.nombre().trim();
        if (turnoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ResourceConflictException("Ya existe un turno con el nombre '" + nombre + "'");
        }
        validarHorario(request.horaInicio(), request.horaFin());
        boolean superaCapacidad = reservaRepository.findCapacidadesReservadasPorFecha(
                        id, ReservaPolicy.ESTADOS_QUE_BLOQUEAN)
                .stream()
                .anyMatch(reservada -> reservada > request.capacidadMaxima());
        if (superaCapacidad) {
            throw new BusinessRuleException(
                    "La capacidad máxima no puede ser menor que las reservas activas del turno");
        }
        aplicar(turno, nombre, request.horaInicio(), request.horaFin(),
                request.capacidadMaxima(), request.activo());
        return toResponse(turno);
    }

    private void aplicar(Turno turno,
                         String nombre,
                         LocalTime horaInicio,
                         LocalTime horaFin,
                         Integer capacidadMaxima,
                         boolean activo) {
        turno.setNombre(nombre);
        turno.setHoraInicio(horaInicio);
        turno.setHoraFin(horaFin);
        turno.setCapacidadMaxima(capacidadMaxima);
        turno.setActivo(activo);
    }

    private void validarHorario(LocalTime horaInicio, LocalTime horaFin) {
        if (horaInicio.equals(horaFin)) {
            throw new BusinessRuleException("La hora de inicio y la hora de fin del turno deben ser diferentes");
        }
    }

    private Turno requireTurno(Long id) {
        return turnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el turno con id " + id));
    }

    private TurnoResponse toResponse(Turno turno) {
        return new TurnoResponse(
                turno.getId(),
                turno.getNombre(),
                turno.getHoraInicio(),
                turno.getHoraFin(),
                turno.getCapacidadMaxima(),
                turno.isActivo(),
                turno.getCreadoEn(),
                turno.getActualizadoEn()
        );
    }
}
