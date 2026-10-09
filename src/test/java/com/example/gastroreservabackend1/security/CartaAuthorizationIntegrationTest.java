package com.example.gastroreservabackend1.security;

import com.example.gastroreservabackend1.dto.auth.LoginRequest;
import com.example.gastroreservabackend1.dto.producto.ProductoMenuCreateRequest;
import com.example.gastroreservabackend1.dto.producto.ProductoMenuResponse;
import com.example.gastroreservabackend1.dto.producto.ProductoMenuUpdateRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioCreateRequest;
import com.example.gastroreservabackend1.model.RolUsuario;
import com.example.gastroreservabackend1.repository.ProductoMenuRepository;
import com.example.gastroreservabackend1.service.AuthService;
import com.example.gastroreservabackend1.service.ProductoMenuService;
import com.example.gastroreservabackend1.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CartaAuthorizationIntegrationTest {

    private static final String VALID_BODY = """
            {"nombre":"Hamburguesa","descripcion":"Con papas","precio":35.50}
            """;

    @Autowired private MockMvc mvc;
    @Autowired private ProductoMenuService service;
    @Autowired private ProductoMenuRepository repository;
    @Autowired private UsuarioService usuarioService;
    @Autowired private AuthService authService;

    @BeforeEach
    void cleanProducts() {
        repository.deleteAll();
    }

    @Test
    void administratorCanCreateUsingARealJwtAndReceivesAnExplicitDto() throws Exception {
        String password = "Password-prueba-123";
        usuarioService.crear(new UsuarioCreateRequest("Admin carta", "admin-carta@example.com",
                password, RolUsuario.ADMINISTRADOR));
        String token = authService.login(new LoginRequest("admin-carta@example.com", password)).accessToken();
        mvc.perform(post("/api/productos-menu").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/productos-menu/")))
                .andExpect(jsonPath("$.nombre").value("Hamburguesa"))
                .andExpect(jsonPath("$.precio").value(35.5))
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.creadoEn").isNotEmpty())
                .andExpect(jsonPath("$.nombreNormalizado").doesNotExist());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administratorCanUpdateAndFilterUnavailableProducts() throws Exception {
        ProductoMenuResponse producto = crear("Pizza", true);
        mvc.perform(put("/api/productos-menu/{id}", producto.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Pizza familiar","precio":50.25,"disponible":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(50.25))
                .andExpect(jsonPath("$.disponible").value(false));
        mvc.perform(get("/api/productos-menu?disponible=false&nombre=FAMILIAR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/productos-menu/{id}", producto.id()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Pizza familiar"));
        mvc.perform(get("/api/productos-menu?disponible=true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "MESERO", "HOST"})
    void readersCanOnlySeeAvailableProductsAndCannotManageTheCatalog(String rol) throws Exception {
        ProductoMenuResponse visible = crear("Pizza", true);
        ProductoMenuResponse oculto = crear("Limonada", false);
        mvc.perform(get("/api/carta?disponible=false").with(user("lector").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(visible.id()));
        mvc.perform(get("/api/carta/{id}", visible.id()).with(user("lector").roles(rol)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/carta/{id}", oculto.id()).with(user("lector").roles(rol)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(get("/api/productos-menu").with(user("lector").roles(rol)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/productos-menu/{id}", oculto.id()).with(user("lector").roles(rol)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/productos-menu").with(user("lector").roles(rol))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(put("/api/productos-menu/{id}", visible.id()).with(user("lector").roles(rol))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Alterado","precio":1,"disponible":false}
                                """))
                .andExpect(status().isForbidden());
        assertThat(service.buscarPorId(visible.id()).nombre()).isEqualTo("Pizza");
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void anonymousRequestsReceiveJson401() throws Exception {
        for (String ruta : new String[]{"/api/carta", "/api/carta/1", "/api/productos-menu", "/api/productos-menu/1"}) {
            mvc.perform(get(ruta)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
        mvc.perform(post("/api/productos-menu").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "12.345", "10000000000", "null"})
    @WithMockUser(roles = "ADMINISTRADOR")
    void invalidPricesReturn400WithoutSaving(String precio) throws Exception {
        mvc.perform(post("/api/productos-menu").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Inválido\",\"precio\":" + precio + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.precio").isNotEmpty());
        assertThat(repository.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void validatesRequiredFieldsLengthsAndUpdateBeforeChangingData() throws Exception {
        mvc.perform(post("/api/productos-menu").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" \",\"precio\":10}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.nombre").isNotEmpty());
        mvc.perform(post("/api/productos-menu").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + "a".repeat(121) + "\",\"precio\":10,\"descripcion\":\""
                                + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nombre").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.descripcion").isNotEmpty());
        ProductoMenuResponse producto = crear("Pizza", true);
        mvc.perform(put("/api/productos-menu/{id}", producto.id())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.disponible").isNotEmpty());
        mvc.perform(put("/api/productos-menu/{id}", producto.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Pizza\",\"precio\":-1,\"disponible\":false}"))
                .andExpect(status().isBadRequest());
        assertThat(service.buscarPorId(producto.id()).disponible()).isTrue();
        assertThat(service.buscarPorId(producto.id()).precio()).isEqualByComparingTo("20");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void duplicateNamesReturn409OnCreateAndUpdate() throws Exception {
        crear("Hamburguesa", true);
        ProductoMenuResponse otro = crear("Pizza", true);
        mvc.perform(post("/api/productos-menu").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" HAMBURGUESA \",\"precio\":30}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
        mvc.perform(put("/api/productos-menu/{id}", otro.id()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"hamburguesa\",\"precio\":30,\"disponible\":true}"))
                .andExpect(status().isConflict());
        assertThat(service.buscarPorId(otro.id()).nombre()).isEqualTo("Pizza");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void missingResourcesAndInvalidIdsAndFiltersHaveControlledErrors() throws Exception {
        mvc.perform(get("/api/productos-menu/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(put("/api/productos-menu/{id}", Long.MAX_VALUE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Pizza\",\"precio\":30,\"disponible\":true}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/carta/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/productos-menu/-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/productos-menu?disponible=no-es-booleano"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_PARAMETER"));
        mvc.perform(post("/api/productos-menu").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void cartaSupportsNameSearchEmptyResultsAndNeverAllowsWrites() throws Exception {
        crear("Pizza", true);
        mvc.perform(get("/api/carta?nombre=PIZ"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/carta?nombre=Inexistente"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(post("/api/carta").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    private ProductoMenuResponse crear(String nombre, boolean disponible) {
        ProductoMenuResponse producto = service.crear(new ProductoMenuCreateRequest(nombre, null, new BigDecimal("20")));
        if (!disponible) {
            return service.actualizar(producto.id(), new ProductoMenuUpdateRequest(nombre, null, producto.precio(), false));
        }
        return producto;
    }
}
