package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.producto.ProductoMenuResponse;
import com.example.gastroreservabackend1.service.ProductoMenuService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/carta")
@Validated
public class CartaController {

    private final ProductoMenuService productoService;

    public CartaController(ProductoMenuService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoMenuResponse> listar(@RequestParam(required = false) String nombre) {
        // Nunca se toma la disponibilidad de la petición: la carta sólo ofrece productos disponibles.
        return productoService.listar(true, nombre);
    }

    @GetMapping("/{id}")
    public ProductoMenuResponse buscar(@PathVariable @Positive Long id) {
        return productoService.buscarDisponible(id);
    }
}
