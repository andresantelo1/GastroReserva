package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.zona.ZonaCreateRequest;
import com.example.gastroreservabackend1.dto.zona.ZonaResponse;
import com.example.gastroreservabackend1.dto.zona.ZonaUpdateRequest;
import com.example.gastroreservabackend1.exception.ResourceConflictException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.model.Zona;
import com.example.gastroreservabackend1.repository.ZonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ZonaService {

    private final ZonaRepository zonaRepository;

    public ZonaService(ZonaRepository zonaRepository) {
        this.zonaRepository = zonaRepository;
    }

    public List<ZonaResponse> listar(Boolean activa, String nombre) {
        String nombreNormalizado = normalizarFiltro(nombre);
        List<Zona> zonas;

        if (activa != null && nombreNormalizado != null) {
            zonas = zonaRepository.findByActivaAndNombreContainingIgnoreCaseOrderByNombreAsc(
                    activa, nombreNormalizado);
        } else if (activa != null) {
            zonas = zonaRepository.findByActivaOrderByNombreAsc(activa);
        } else if (nombreNormalizado != null) {
            zonas = zonaRepository.findByNombreContainingIgnoreCaseOrderByNombreAsc(nombreNormalizado);
        } else {
            zonas = zonaRepository.findAllByOrderByNombreAsc();
        }

        return zonas.stream().map(this::toResponse).toList();
    }

    public ZonaResponse buscarPorId(Long id) {
        return toResponse(requireZona(id));
    }

    @Transactional
    public ZonaResponse crear(ZonaCreateRequest request) {
        String nombre = normalizarTexto(request.nombre());
        if (zonaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResourceConflictException("Ya existe una zona con el nombre '" + nombre + "'");
        }

        Zona zona = new Zona();
        zona.setNombre(nombre);
        zona.setDescripcion(normalizarDescripcion(request.descripcion()));
        zona.setActiva(true);
        return toResponse(zonaRepository.save(zona));
    }

    @Transactional
    public ZonaResponse actualizar(Long id, ZonaUpdateRequest request) {
        Zona zona = requireZona(id);
        String nombre = normalizarTexto(request.nombre());
        if (zonaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ResourceConflictException("Ya existe una zona con el nombre '" + nombre + "'");
        }

        zona.setNombre(nombre);
        zona.setDescripcion(normalizarDescripcion(request.descripcion()));
        zona.setActiva(request.activa());
        return toResponse(zona);
    }

    Zona requireZona(Long id) {
        return zonaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la zona con id " + id));
    }

    private ZonaResponse toResponse(Zona zona) {
        return new ZonaResponse(
                zona.getId(),
                zona.getNombre(),
                zona.getDescripcion(),
                zona.isActiva()
        );
    }

    private String normalizarFiltro(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizarDescripcion(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizarTexto(String value) {
        return value.trim();
    }
}
