package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.cliente.ClienteCreateRequest;
import com.example.gastroreservabackend1.dto.cliente.ClienteResponse;
import com.example.gastroreservabackend1.dto.cliente.ClienteUpdateRequest;
import com.example.gastroreservabackend1.dto.cliente.MiClienteCreateRequest;
import com.example.gastroreservabackend1.dto.cliente.MiClienteUpdateRequest;
import com.example.gastroreservabackend1.exception.BusinessRuleException;
import com.example.gastroreservabackend1.exception.ResourceConflictException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.model.Cliente;
import com.example.gastroreservabackend1.model.RolUsuario;
import com.example.gastroreservabackend1.model.Usuario;
import com.example.gastroreservabackend1.repository.ClienteRepository;
import com.example.gastroreservabackend1.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    public ClienteService(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<ClienteResponse> listar(Boolean activo, String nombre, String email) {
        Specification<Cliente> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (activo != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("activo"), activo));
        }
        if (StringUtils.hasText(nombre)) {
            String filter = "%" + nombre.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), filter));
        }
        if (StringUtils.hasText(email)) {
            String filter = "%" + email.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), filter));
        }

        return clienteRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "nombre"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ClienteResponse buscarPorId(Long id) {
        return toResponse(requireCliente(id));
    }

    public ClienteResponse buscarActual(String userEmail) {
        return toResponse(requireClienteActual(userEmail));
    }

    @Transactional
    public ClienteResponse crear(ClienteCreateRequest request) {
        String email = normalizarEmail(request.email());
        validarEmailDisponible(email, null);

        Cliente cliente = new Cliente();
        cliente.setNombre(request.nombre().trim());
        cliente.setEmail(email);
        cliente.setTelefono(normalizarTelefono(request.telefono()));
        cliente.setActivo(true);
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteUpdateRequest request) {
        Cliente cliente = requireCliente(id);
        String email = normalizarEmail(request.email());
        validarEmailDisponible(email, id);

        cliente.setNombre(request.nombre().trim());
        cliente.setEmail(email);
        cliente.setTelefono(normalizarTelefono(request.telefono()));
        cliente.setActivo(request.activo());
        sincronizarUsuarioVinculado(cliente);
        return toResponse(cliente);
    }

    @Transactional
    public ClienteResponse asegurarPerfilActual(String userEmail, MiClienteCreateRequest request) {
        Usuario usuario = requireUsuarioCliente(userEmail);
        return asegurarPerfilUsuario(usuario.getId(), request.telefono());
    }

    @Transactional
    public ClienteResponse asegurarPerfilUsuario(Long usuarioId, String telefono) {
        return clienteRepository.findByUsuarioId(usuarioId)
                .map(this::toResponse)
                .orElseGet(() -> crearOVincularPerfil(usuarioId, telefono));
    }

    @Transactional
    public ClienteResponse actualizarActual(String userEmail, MiClienteUpdateRequest request) {
        Cliente cliente = requireClienteActual(userEmail);
        cliente.setNombre(request.nombre().trim());
        cliente.setTelefono(normalizarTelefono(request.telefono()));
        cliente.getUsuario().setNombre(cliente.getNombre());
        return toResponse(cliente);
    }

    private ClienteResponse crearOVincularPerfil(Long usuarioId, String telefono) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario con id " + usuarioId));
        if (usuario.getRol() != RolUsuario.CLIENTE) {
            throw new BusinessRuleException("Sólo un usuario con rol CLIENTE puede tener un perfil de cliente");
        }

        Cliente cliente = clienteRepository.findByEmailIgnoreCase(usuario.getEmail()).orElseGet(Cliente::new);
        if (cliente.getUsuario() != null && !cliente.getUsuario().getId().equals(usuarioId)) {
            throw new ResourceConflictException("El perfil de cliente ya está vinculado a otro usuario");
        }
        if (cliente.getId() == null) {
            cliente.setNombre(usuario.getNombre());
            cliente.setEmail(usuario.getEmail());
            cliente.setTelefono(normalizarTelefono(telefono));
            cliente.setActivo(true);
        } else if (!StringUtils.hasText(cliente.getTelefono()) && StringUtils.hasText(telefono)) {
            cliente.setTelefono(normalizarTelefono(telefono));
        }
        cliente.setUsuario(usuario);
        return toResponse(clienteRepository.save(cliente));
    }

    private void sincronizarUsuarioVinculado(Cliente cliente) {
        Usuario usuario = cliente.getUsuario();
        if (usuario == null) {
            return;
        }
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(cliente.getEmail(), usuario.getId())) {
            throw new ResourceConflictException("Ya existe un usuario con el correo '" + cliente.getEmail() + "'");
        }
        usuario.setNombre(cliente.getNombre());
        usuario.setEmail(cliente.getEmail());
    }

    private Usuario requireUsuarioCliente(String email) {
        return usuarioRepository.findByEmailIgnoreCase(normalizarEmail(email))
                .filter(usuario -> usuario.getRol() == RolUsuario.CLIENTE && usuario.isActivo())
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario cliente activo para el perfil"));
    }

    private Cliente requireClienteActual(String email) {
        return clienteRepository.findByUsuarioEmailIgnoreCase(normalizarEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene un perfil de cliente"));
    }

    private Cliente requireCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el cliente con id " + id));
    }

    private void validarEmailDisponible(String email, Long currentId) {
        boolean exists = currentId == null
                ? clienteRepository.existsByEmailIgnoreCase(email)
                : clienteRepository.existsByEmailIgnoreCaseAndIdNot(email, currentId);
        if (exists) {
            throw new ResourceConflictException("Ya existe un cliente con el correo '" + email + "'");
        }
    }

    private ClienteResponse toResponse(Cliente cliente) {
        Long usuarioId = cliente.getUsuario() == null ? null : cliente.getUsuario().getId();
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.isActivo(),
                usuarioId,
                cliente.getCreadoEn(),
                cliente.getActualizadoEn()
        );
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarTelefono(String telefono) {
        return StringUtils.hasText(telefono) ? telefono.trim() : null;
    }
}
