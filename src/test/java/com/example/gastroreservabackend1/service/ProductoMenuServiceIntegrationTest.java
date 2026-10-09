package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.producto.ProductoMenuCreateRequest;
import com.example.gastroreservabackend1.dto.producto.ProductoMenuResponse;
import com.example.gastroreservabackend1.dto.producto.ProductoMenuUpdateRequest;
import com.example.gastroreservabackend1.exception.ResourceConflictException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.model.ProductoMenu;
import com.example.gastroreservabackend1.repository.ProductoMenuRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ProductoMenuServiceIntegrationTest {

    @Autowired private ProductoMenuService service;
    @Autowired private ProductoMenuRepository repository;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void cleanProducts() {
        repository.deleteAll();
    }

    @Test
    void createsNormalizesAndPersistsAnAvailableProductWithExactDecimalPrice() {
        ProductoMenuResponse creado = service.crear(new ProductoMenuCreateRequest(
                "  Hamburguesa  ", "  Con papas  ", new BigDecimal("35.50")));
        entityManager.clear();
        ProductoMenuResponse guardado = service.buscarPorId(creado.id());
        assertThat(guardado.nombre()).isEqualTo("Hamburguesa");
        assertThat(guardado.descripcion()).isEqualTo("Con papas");
        assertThat(guardado.precio()).isEqualByComparingTo("35.50");
        assertThat(guardado.disponible()).isTrue();
        assertThat(guardado.creadoEn()).isNotNull();
        assertThat(guardado.actualizadoEn()).isNotNull();
    }

    @Test
    void updatesPriceAndAvailabilityWithoutDeletingAndCanRestoreAvailability() {
        ProductoMenuResponse creado = crear("Limonada");
        ProductoMenuResponse actualizado = service.actualizar(creado.id(), new ProductoMenuUpdateRequest(
                "Limonada", " ", new BigDecimal("12.75"), false));
        entityManager.clear();
        assertThat(actualizado.id()).isEqualTo(creado.id());
        assertThat(actualizado.creadoEn()).isEqualTo(creado.creadoEn());
        assertThat(actualizado.actualizadoEn()).isAfterOrEqualTo(creado.actualizadoEn());
        assertThat(actualizado.descripcion()).isNull();
        assertThat(service.buscarPorId(creado.id()).precio()).isEqualByComparingTo("12.75");
        assertThat(service.listar(true, null)).isEmpty();
        assertThatThrownBy(() -> service.buscarDisponible(creado.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(repository.count()).isEqualTo(1);
        service.actualizar(creado.id(), new ProductoMenuUpdateRequest(
                "Limonada", null, new BigDecimal("12.75"), true));
        assertThat(service.buscarDisponible(creado.id()).disponible()).isTrue();
    }

    @Test
    void rejectsDuplicateNamesIgnoringCaseAndOuterWhitespaceEvenWhenUnavailable() {
        ProductoMenuResponse primero = crear("Pizza");
        ProductoMenuResponse segundo = crear("Pasta");
        service.actualizar(primero.id(), new ProductoMenuUpdateRequest(
                "Pizza", null, new BigDecimal("20"), false));
        assertThatThrownBy(() -> crear(" pIzZa ")).isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> service.actualizar(segundo.id(), new ProductoMenuUpdateRequest(
                " PIZZA ", null, new BigDecimal("21"), true)))
                .isInstanceOf(ResourceConflictException.class);
        assertThat(service.actualizar(primero.id(), new ProductoMenuUpdateRequest(
                "PIZZA", null, new BigDecimal("22"), true)).nombre()).isEqualTo("PIZZA");
    }

    @Test
    void databaseAlsoEnforcesNormalizedNameUniqueness() {
        crear("Pizza");
        ProductoMenu duplicado = new ProductoMenu();
        duplicado.setNombre(" pIzZa ");
        duplicado.setPrecio(new BigDecimal("20.00"));
        assertThatThrownBy(() -> repository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-1", "1.001", "10000000000"})
    void rejectsInvalidPricesEvenWhenCalledDirectly(String precio) {
        assertThatThrownBy(() -> service.crear(new ProductoMenuCreateRequest(
                "Inválido", null, precio == null ? null : new BigDecimal(precio))))
                .isInstanceOf(ConstraintViolationException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void acceptsPriceLimitsWithoutRounding() {
        assertThat(service.crear(new ProductoMenuCreateRequest("Mínimo", null,
                new BigDecimal("0.01"))).precio()).isEqualByComparingTo("0.01");
        assertThat(service.crear(new ProductoMenuCreateRequest("Máximo", null,
                new BigDecimal("9999999999.99"))).precio()).isEqualByComparingTo("9999999999.99");
    }

    @Test
    void filtersByAvailabilityAndLiteralNameAndReturnsEmptyResults() {
        crear("Jugo 100% natural");
        crear("Jugo especial");
        crear("Combo_uno");
        ProductoMenuResponse agotado = crear("Pizza");
        service.actualizar(agotado.id(), new ProductoMenuUpdateRequest(
                "Pizza", null, new BigDecimal("20"), false));
        assertThat(service.listar(true, " JUGO ")).hasSize(2);
        assertThat(service.listar(null, "%")).extracting(ProductoMenuResponse::nombre)
                .containsExactly("Jugo 100% natural");
        assertThat(service.listar(null, "_")).extracting(ProductoMenuResponse::nombre)
                .containsExactly("Combo_uno");
        assertThat(service.listar(false, null)).extracting(ProductoMenuResponse::id)
                .containsExactly(agotado.id());
        assertThat(service.listar(true, "Pizza")).isEmpty();
        assertThat(service.listar(null, "Inexistente")).isEmpty();
    }

    @Test
    void rejectsMissingProductsOnReadAndUpdate() {
        assertThatThrownBy(() -> service.buscarPorId(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.actualizar(Long.MAX_VALUE, new ProductoMenuUpdateRequest(
                "Inexistente", null, BigDecimal.ONE, true)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ProductoMenuResponse crear(String nombre) {
        return service.crear(new ProductoMenuCreateRequest(nombre, null, new BigDecimal("20")));
    }
}
