package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.model.RolUsuario;
import com.example.gastroreservabackend1.model.Usuario;
import com.example.gastroreservabackend1.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;

@Service
@Transactional(readOnly = true)
public class ActorService {
    private final UsuarioRepository usuarios;
    public ActorService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    public Usuario exigir(String email, RolUsuario... roles) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new AccessDeniedException("El usuario no está activo"));
        if (Arrays.stream(roles).noneMatch(rol -> rol == usuario.getRol())) {
            throw new AccessDeniedException("El rol no permite esta operación");
        }
        return usuario;
    }
}
