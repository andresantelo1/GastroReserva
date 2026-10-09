package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.auth.LoginRequest;
import com.example.gastroreservabackend1.dto.auth.LoginResponse;
import com.example.gastroreservabackend1.dto.auth.RegisterRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioResponse;
import com.example.gastroreservabackend1.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UsuarioResponse actual(Authentication authentication) {
        return authService.actual(authentication.getName());
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegisterRequest request) {
        UsuarioResponse response = authService.register(request);
        return ResponseEntity.created(URI.create("/api/clientes/me")).body(response);
    }
}
