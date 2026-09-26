package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.auth.RegisterRequest;
import com.example.gastroreservabackend1.dto.usuario.PasswordUpdateRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioCreateRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioResponse;
import com.example.gastroreservabackend1.dto.usuario.UsuarioUpdateRequest;
import com.example.gastroreservabackend1.exception.ResourceConflictException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.model.RolUsuario;
import com.example.gastroreservabackend1.model.Usuario;
import com.example.gastroreservabackend1.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponse> listar(RolUsuario rol, Boolean activo, String email) {
        Specification<Usuario> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if (rol != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("rol"), rol));
        }
        if (activo != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("activo"), activo));
        }
        if (StringUtils.hasText(email)) {
            String filter = "%" + email.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), filter));
        }

        return usuarioRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "email"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UsuarioResponse buscarPorId(Long id) {
        return toResponse(requireUsuario(id));
    }

    @Transactional
    public UsuarioResponse registrarCliente(RegisterRequest request) {
        return crearInterno(request.nombre(), request.email(), request.password(), RolUsuario.CLIENTE);
    }

    @Transactional
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        return crearInterno(request.nombre(), request.email(), request.password(), request.rol());
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = requireUsuario(id);
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ResourceConflictException("Ya existe un usuario con el correo '" + email + "'");
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setRol(request.rol());
        usuario.setActivo(request.activo());
        return toResponse(usuario);
    }

    @Transactional
    public void actualizarPassword(Long id, PasswordUpdateRequest request) {
        Usuario usuario = requireUsuario(id);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.isActivo(),
                usuario.getCreadoEn(),
                usuario.getActualizadoEn()
        );
    }

    private UsuarioResponse crearInterno(String nombre, String rawEmail, String password, RolUsuario rol) {
        String email = normalizarEmail(rawEmail);
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResourceConflictException("Ya existe un usuario con el correo '" + email + "'");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre.trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setRol(rol);
        usuario.setActivo(true);
        return toResponse(usuarioRepository.save(usuario));
    }

    private Usuario requireUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario con id " + id));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
