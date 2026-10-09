package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.producto.ProductoMenuCreateRequest;
import com.example.gastroreservabackend1.dto.producto.ProductoMenuResponse;
import com.example.gastroreservabackend1.dto.producto.ProductoMenuUpdateRequest;
import com.example.gastroreservabackend1.exception.ResourceConflictException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.model.ProductoMenu;
import com.example.gastroreservabackend1.repository.ProductoMenuRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
@Validated
@Transactional(readOnly = true)
public class ProductoMenuService {

    private final ProductoMenuRepository productoRepository;

    public ProductoMenuService(ProductoMenuRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<ProductoMenuResponse> listar(Boolean disponible, String nombre) {
        Specification<ProductoMenu> filtro = (root, query, cb) -> cb.conjunction();
        if (disponible != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("disponible"), disponible));
        }
        if (StringUtils.hasText(nombre)) {
            // El filtro busca texto literal, no patrones SQL suministrados por el cliente.
            String texto = nombre.trim().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_");
            filtro = filtro.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("nombre")), "%" + texto + "%", '!'));
        }
        return productoRepository.findAll(filtro, Sort.by("nombre").and(Sort.by("id")))
                .stream().map(this::toResponse).toList();
    }

    public ProductoMenuResponse buscarPorId(Long id) {
        return toResponse(requireProducto(id));
    }

    public ProductoMenuResponse buscarDisponible(Long id) {
        ProductoMenu producto = requireProducto(id);
        if (!producto.isDisponible()) {
            throw new ResourceNotFoundException("No existe un producto disponible con id " + id);
        }
        return toResponse(producto);
    }

    @Transactional
    public ProductoMenuResponse crear(@NotNull @Valid ProductoMenuCreateRequest request) {
        String nombre = request.nombre().trim();
        if (productoRepository.existsByNombreNormalizado(nombre.toLowerCase(Locale.ROOT))) {
            throw nombreDuplicado();
        }
        ProductoMenu producto = new ProductoMenu();
        aplicar(producto, nombre, request.descripcion(), request.precio(), true);
        return toResponse(productoRepository.saveAndFlush(producto));
    }

    @Transactional
    public ProductoMenuResponse actualizar(Long id, @NotNull @Valid ProductoMenuUpdateRequest request) {
        ProductoMenu producto = requireProducto(id);
        String nombre = request.nombre().trim();
        if (productoRepository.existsByNombreNormalizadoAndIdNot(nombre.toLowerCase(Locale.ROOT), id)) {
            throw nombreDuplicado();
        }
        aplicar(producto, nombre, request.descripcion(), request.precio(), request.disponible());
        // Flush aplica la restricción única y actualiza la fecha antes de construir el DTO.
        return toResponse(productoRepository.saveAndFlush(producto));
    }

    private void aplicar(ProductoMenu producto, String nombre, String descripcion,
                         BigDecimal precio, boolean disponible) {
        producto.setNombre(nombre);
        producto.setDescripcion(StringUtils.hasText(descripcion) ? descripcion.trim() : null);
        producto.setPrecio(precio.setScale(2, RoundingMode.UNNECESSARY));
        producto.setDisponible(disponible);
    }

    private ProductoMenu requireProducto(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto con id " + id));
    }

    private ResourceConflictException nombreDuplicado() {
        return new ResourceConflictException("Ya existe un producto con ese nombre");
    }

    private ProductoMenuResponse toResponse(ProductoMenu producto) {
        return new ProductoMenuResponse(producto.getId(), producto.getNombre(), producto.getDescripcion(),
                producto.getPrecio(), producto.isDisponible(), producto.getCreadoEn(), producto.getActualizadoEn());
    }
}
