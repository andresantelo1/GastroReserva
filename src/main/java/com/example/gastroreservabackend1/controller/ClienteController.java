package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.cliente.ClienteCreateRequest;
import com.example.gastroreservabackend1.dto.cliente.ClienteResponse;
import com.example.gastroreservabackend1.dto.cliente.ClienteUpdateRequest;
import com.example.gastroreservabackend1.dto.cliente.MiClienteCreateRequest;
import com.example.gastroreservabackend1.dto.cliente.MiClienteUpdateRequest;
import com.example.gastroreservabackend1.service.ClienteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/clientes")
@Validated
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping("/me")
    public ClienteResponse buscarMiPerfil(Authentication authentication) {
        return clienteService.buscarActual(authentication.getName());
    }

    @PostMapping("/me")
    public ClienteResponse asegurarMiPerfil(Authentication authentication,
                                             @Valid @RequestBody MiClienteCreateRequest request) {
        return clienteService.asegurarPerfilActual(authentication.getName(), request);
    }

    @PutMapping("/me")
    public ClienteResponse actualizarMiPerfil(Authentication authentication,
                                               @Valid @RequestBody MiClienteUpdateRequest request) {
        return clienteService.actualizarActual(authentication.getName(), request);
    }

    @GetMapping
    public List<ClienteResponse> listarClientes(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String email) {
        return clienteService.listar(activo, nombre, email);
    }

    @GetMapping("/{id}")
    public ClienteResponse buscarCliente(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id) {
        return clienteService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> crearCliente(@Valid @RequestBody ClienteCreateRequest request) {
        ClienteResponse response = clienteService.crear(request);
        return ResponseEntity.created(URI.create("/api/clientes/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizarCliente(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id,
            @Valid @RequestBody ClienteUpdateRequest request) {
        return clienteService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(
            @PathVariable @Positive(message = "El id debe ser mayor que cero") Long id) {
        clienteService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
