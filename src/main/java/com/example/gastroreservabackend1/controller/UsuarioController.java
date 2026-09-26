package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.usuario.PasswordUpdateRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioCreateRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioResponse;
import com.example.gastroreservabackend1.dto.usuario.UsuarioUpdateRequest;
import com.example.gastroreservabackend1.model.RolUsuario;
import com.example.gastroreservabackend1.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Validated
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar(
            @RequestParam(required = false) RolUsuario rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String email) {
        return usuarioService.listar(rol, activo, email);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id) {
        return usuarioService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioCreateRequest request) {
        UsuarioResponse response = usuarioService.crear(request);
        return ResponseEntity.created(URI.create("/api/usuarios/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id,
            @Valid @RequestBody UsuarioUpdateRequest request) {
        return usuarioService.actualizar(id, request);
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> actualizarPassword(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id,
            @Valid @RequestBody PasswordUpdateRequest request) {
        usuarioService.actualizarPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
