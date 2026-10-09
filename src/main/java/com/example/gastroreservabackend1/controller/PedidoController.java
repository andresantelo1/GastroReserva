package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.pedido.PedidoDtos.*;
import com.example.gastroreservabackend1.model.EstadoPedido;
import com.example.gastroreservabackend1.service.PedidoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@Validated
public class PedidoController {
    private final PedidoService service;
    public PedidoController(PedidoService service) { this.service = service; }

    @GetMapping
    public List<Respuesta> listar(Authentication auth, @RequestParam(required = false) EstadoPedido estado,
                                  @RequestParam(required = false) @Positive Long mesaId,
                                  @RequestParam(required = false) @Positive Long reservaId) {
        return service.listar(auth.getName(), estado, mesaId, reservaId, false);
    }
    @GetMapping("/mios")
    public List<Respuesta> mios(Authentication auth, @RequestParam(required = false) EstadoPedido estado) {
        return service.listar(auth.getName(), estado, null, null, true);
    }
    @GetMapping("/{id}")
    public Respuesta buscar(@PathVariable @Positive Long id, Authentication auth) { return service.buscar(id, auth.getName()); }
    @GetMapping("/{id}/historial")
    public List<Historial> historial(@PathVariable @Positive Long id, Authentication auth) { return service.historial(id, auth.getName()); }
    @PostMapping
    public ResponseEntity<Respuesta> crear(Authentication auth, @Valid @RequestBody Crear request) {
        Respuesta response = service.crear(auth.getName(), request);
        return ResponseEntity.created(URI.create("/api/pedidos/" + response.id())).body(response);
    }
    @PostMapping("/{id}/items")
    public Respuesta agregar(@PathVariable @Positive Long id, Authentication auth, @Valid @RequestBody AgregarItem request) {
        return service.agregar(id, auth.getName(), request);
    }
    @PutMapping("/{id}/items/{itemId}")
    public Respuesta cantidad(@PathVariable @Positive Long id, @PathVariable @Positive Long itemId,
                               Authentication auth, @Valid @RequestBody CambiarCantidad request) {
        return service.cantidad(id, itemId, auth.getName(), request);
    }
    @DeleteMapping("/{id}/items/{itemId}")
    public Respuesta quitar(@PathVariable @Positive Long id, @PathVariable @Positive Long itemId, Authentication auth) {
        return service.quitar(id, itemId, auth.getName());
    }
    @PatchMapping("/{id}/estado")
    public Respuesta estado(@PathVariable @Positive Long id, Authentication auth, @Valid @RequestBody CambiarEstado request) {
        return service.cambiarEstado(id, auth.getName(), request);
    }
    @PatchMapping("/{id}/responsable")
    public Respuesta responsable(@PathVariable @Positive Long id, Authentication auth, @Valid @RequestBody AsignarMesero request) {
        return service.asignar(id, auth.getName(), request);
    }
}
