package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.auth.LoginRequest;
import com.example.gastroreservabackend1.dto.auth.LoginResponse;
import com.example.gastroreservabackend1.dto.auth.RegisterRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioResponse;
import com.example.gastroreservabackend1.exception.InvalidCredentialsException;
import com.example.gastroreservabackend1.model.Usuario;
import com.example.gastroreservabackend1.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UsuarioService usuarioService;
    private final ClienteService clienteService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       UsuarioService usuarioService,
                       ClienteService clienteService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.usuarioService = usuarioService;
        this.clienteService = clienteService;
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!usuario.isActivo() || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        TokenService.TokenResult token = tokenService.emitir(usuario);
        return new LoginResponse(
                token.value(),
                "Bearer",
                token.expiresAt(),
                usuarioService.toResponse(usuario)
        );
    }

    public UsuarioResponse actual(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).filter(Usuario::isActivo)
                .orElseThrow(InvalidCredentialsException::new);
        return usuarioService.toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse register(RegisterRequest request) {
        UsuarioResponse usuario = usuarioService.registrarCliente(request);
        clienteService.asegurarPerfilUsuario(usuario.id(), null);
        return usuario;
    }
}
