package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.mesa.MesaCreateRequest;
import com.example.gastroreservabackend1.dto.mesa.MesaResponse;
import com.example.gastroreservabackend1.dto.mesa.MesaUpdateRequest;
import com.example.gastroreservabackend1.dto.zona.ZonaSummaryResponse;
import com.example.gastroreservabackend1.exception.ResourceConflictException;
import com.example.gastroreservabackend1.exception.BusinessRuleException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.model.Mesa;
import com.example.gastroreservabackend1.model.Zona;
import com.example.gastroreservabackend1.repository.MesaRepository;
import com.example.gastroreservabackend1.repository.ZonaRepository;
import com.example.gastroreservabackend1.repository.ReservaRepository;
import com.example.gastroreservabackend1.repository.MesaAbiertaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class MesaService {

    private final MesaRepository mesaRepository;
    private final ZonaRepository zonaRepository;
    private final ReservaRepository reservaRepository;
    private final MesaAbiertaRepository mesaAbiertaRepository;

    public MesaService(MesaRepository mesaRepository,
                       ZonaRepository zonaRepository,
                       ReservaRepository reservaRepository,
                       MesaAbiertaRepository mesaAbiertaRepository) {
        this.mesaRepository = mesaRepository;
        this.zonaRepository = zonaRepository;
        this.reservaRepository = reservaRepository;
        this.mesaAbiertaRepository = mesaAbiertaRepository;
    }

    public List<MesaResponse> listar(Long zonaId, Boolean activa, Integer capacidadMinima, String estado) {
        Specification<Mesa> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (zonaId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("zona").get("id"), zonaId));
        }
        if (activa != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("activa"), activa));
        }
        if (capacidadMinima != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("capacidad"), capacidadMinima));
        }
        if (StringUtils.hasText(estado)) {
            String estadoNormalizado = estado.trim().toUpperCase(Locale.ROOT);
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(criteriaBuilder.upper(root.get("estado")), estadoNormalizado));
        }

        return mesaRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "numero"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MesaResponse buscarPorId(Long id) {
        return toResponse(requireMesa(id));
    }

    @Transactional
    public MesaResponse crear(MesaCreateRequest request) {
        if (mesaRepository.existsByNumero(request.numero())) {
            throw new ResourceConflictException("Ya existe la mesa número " + request.numero());
        }

        Zona zona = requireZona(request.zonaId());
        Mesa mesa = new Mesa();
        mesa.setNumero(request.numero());
        mesa.setCapacidad(request.capacidad());
        mesa.setEstado(Mesa.ESTADO_DISPONIBLE);
        mesa.setActiva(true);
        mesa.setZona(zona);
        return toResponse(mesaRepository.save(mesa));
    }

    @Transactional
    public MesaResponse actualizar(Long id, MesaUpdateRequest request) {
        Mesa mesa = mesaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la mesa con id " + id));
        if (mesaAbiertaRepository.existsByMesaIdAndMesaActivaIdIsNotNullAndCantidadPersonasGreaterThan(id, request.capacidad())) {
            throw new BusinessRuleException("La capacidad no puede ser menor que una atención abierta de la mesa");
        }
        if (mesaRepository.existsByNumeroAndIdNot(request.numero(), id)) {
            throw new ResourceConflictException("Ya existe la mesa número " + request.numero());
        }
        if (reservaRepository.existsByMesaIdAndEstadoInAndCantidadPersonasGreaterThan(
                id, ReservaPolicy.ESTADOS_QUE_BLOQUEAN, request.capacidad())) {
            throw new BusinessRuleException(
                    "La capacidad no puede ser menor que una reserva activa de la mesa");
        }

        Zona zona = requireZona(request.zonaId());
        mesa.setNumero(request.numero());
        mesa.setCapacidad(request.capacidad());
        mesa.setActiva(request.activa());
        mesa.setZona(zona);
        return toResponse(mesa);
    }

    private Mesa requireMesa(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la mesa con id " + id));
    }

    private Zona requireZona(Long id) {
        return zonaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la zona con id " + id));
    }

    private MesaResponse toResponse(Mesa mesa) {
        Zona zona = mesa.getZona();
        return new MesaResponse(
                mesa.getId(),
                mesa.getNumero(),
                mesa.getCapacidad(),
                mesa.getEstado(),
                mesa.isActiva(),
                new ZonaSummaryResponse(zona.getId(), zona.getNombre(), zona.isActiva())
        );
    }
}
