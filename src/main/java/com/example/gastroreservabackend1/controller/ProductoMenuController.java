package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.producto.ProductoMenuCreateRequest;
import com.example.gastroreservabackend1.dto.producto.ProductoMenuResponse;
import com.example.gastroreservabackend1.dto.producto.ProductoMenuUpdateRequest;
import com.example.gastroreservabackend1.service.ProductoMenuService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/productos-menu")
@Validated
public class ProductoMenuController {

    private final ProductoMenuService productoService;

    public ProductoMenuController(ProductoMenuService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoMenuResponse> listar(@RequestParam(required = false) Boolean disponible,
                                             @RequestParam(required = false) String nombre) {
        return productoService.listar(disponible, nombre);
    }

    @GetMapping("/{id}")
    public ProductoMenuResponse buscar(@PathVariable @Positive Long id) {
        return productoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ProductoMenuResponse> crear(@Valid @RequestBody ProductoMenuCreateRequest request) {
        ProductoMenuResponse response = productoService.crear(request);
        return ResponseEntity.created(URI.create("/api/productos-menu/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ProductoMenuResponse actualizar(@PathVariable @Positive Long id,
                                           @Valid @RequestBody ProductoMenuUpdateRequest request) {
        return productoService.actualizar(id, request);
    }
}
