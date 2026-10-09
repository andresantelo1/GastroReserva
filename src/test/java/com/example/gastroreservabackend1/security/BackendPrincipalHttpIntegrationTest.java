package com.example.gastroreservabackend1.security;

import com.example.gastroreservabackend1.dto.auth.*;
import com.example.gastroreservabackend1.dto.mesa.MesaCreateRequest;
import com.example.gastroreservabackend1.dto.producto.ProductoMenuCreateRequest;
import com.example.gastroreservabackend1.dto.reserva.*;
import com.example.gastroreservabackend1.dto.turno.TurnoCreateRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioCreateRequest;
import com.example.gastroreservabackend1.dto.zona.ZonaCreateRequest;
import com.example.gastroreservabackend1.model.*;
import com.example.gastroreservabackend1.repository.UsuarioRepository;
import com.example.gastroreservabackend1.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BackendPrincipalHttpIntegrationTest {
    static final String PASSWORD = "Password-prueba-123";
    static final LocalDate FECHA = LocalDate.of(2030, 1, 15);
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthService auth;
    @Autowired UsuarioService usuarios;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ZonaService zonas;
    @Autowired MesaService mesas;
    @Autowired TurnoService turnos;
    @Autowired ProductoMenuService productos;
    @Autowired ReservaService reservas;
    @Autowired com.example.gastroreservabackend1.repository.ReservaRepository reservaRepository;
    @Autowired Clock clock;
    final Map<String, String> tokens = new HashMap<>();
    Long mesaId, turnoId, productoId;

    private String email(String actor) { return "http-" + actor + "@example.com"; }

    @BeforeEach
    void escenario() {
        for (var rol : List.of(RolUsuario.ADMINISTRADOR, RolUsuario.HOST, RolUsuario.MESERO)) {
            usuarios.crear(new UsuarioCreateRequest(rol.name(), email(rol.name()), PASSWORD, rol));
        }
        usuarios.crear(new UsuarioCreateRequest("Otro mesero", email("otro"), PASSWORD, RolUsuario.MESERO));
        auth.register(new RegisterRequest("Cliente", email("CLIENTE"), PASSWORD));
        auth.register(new RegisterRequest("Otro cliente", email("ajeno"), PASSWORD));
        Long zona = zonas.crear(new ZonaCreateRequest("Zona HTTP core", null)).id();
        mesaId = mesas.crear(new MesaCreateRequest(801, 4, zona)).id();
        turnoId = turnos.crear(new TurnoCreateRequest("Turno HTTP core", LocalTime.NOON, LocalTime.of(15, 0), 20)).id();
        productoId = productos.crear(new ProductoMenuCreateRequest("Producto HTTP", null, new BigDecimal("12.50"))).id();
    }

    @Test
    void rutaDesconocidaDevuelve404SinEliminarLaAutenticacion() throws Exception {
        call(get("/api/clientes-inexistentes"), "ADMINISTRADOR", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/clientes-inexistentes"));
        mvc.perform(get("/api/clientes-inexistentes")).andExpect(status().isUnauthorized());
    }

    @Test
    void flujoCompletoPorHttpConJwtYContratosDto() throws Exception {
        call(get("/api/disponibilidad/turnos"), "CLIENTE", null).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(turnoId));
        call(get("/api/disponibilidad/zonas"), "CLIENTE", null).andExpect(status().isOk());
        Long reserva = id(call(post("/api/reservas"), "CLIENTE",
                "{\"fecha\":\"2030-01-15\",\"turnoId\":" + turnoId + ",\"mesaId\":" + mesaId + ",\"cantidadPersonas\":2}")
                .andExpect(status().isCreated()));
        call(patch("/api/reservas/" + reserva + "/estado"), "HOST", "{\"estado\":\"CONFIRMADA\"}").andExpect(status().isOk());
        call(patch("/api/reservas/" + reserva + "/check-in"), "HOST", "{\"mesaId\":" + mesaId + "}").andExpect(status().isOk());
        Long pedido = id(call(post("/api/pedidos"), "MESERO", "{\"reservaId\":" + reserva + "}")
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.responsable.passwordHash").doesNotExist()));
        call(post("/api/pedidos/" + pedido + "/items"), "MESERO", "{\"productoId\":" + productoId + ",\"cantidad\":2}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalOperativo").value(25.0))
                .andExpect(jsonPath("$.items[0].precioUnitarioHistorico").value(12.5));
        call(patch("/api/reservas/" + reserva + "/estado"), "HOST", "{\"estado\":\"FINALIZADA\"}")
                .andExpect(status().isUnprocessableEntity());
        for (String estado : List.of("EN_PREPARACION", "SERVIDO", "CERRADO")) {
            call(patch("/api/pedidos/" + pedido + "/estado"), "MESERO", "{\"estado\":\"" + estado + "\"}")
                    .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value(estado));
        }
        call(patch("/api/reservas/" + reserva + "/estado"), "MESERO", "{\"estado\":\"FINALIZADA\"}").andExpect(status().isOk());
        call(post("/api/feedback"), "CLIENTE", "{\"reservaId\":" + reserva + ",\"puntuacion\":5,\"comentario\":\"Muy bien\"}")
                .andExpect(status().isCreated()).andExpect(header().exists("Location"));
        call(get("/api/pedidos/" + pedido + "/historial"), "HOST", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        call(get("/api/reportes/reservas?desde=2030-01-15&hasta=2030-01-15"), "ADMINISTRADOR", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.atendidas").value(1));
        call(get("/api/notificaciones"), "CLIENTE", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos.length()").value(4))
                .andExpect(jsonPath("$.eventos[0].motivo").doesNotExist());
    }

    @Test
    void rutasNuevasRechazanAnonimos() throws Exception {
        for (String ruta : List.of("/api/auth/me", "/api/pedidos", "/api/pedidos/1/historial", "/api/mesas-abiertas",
                "/api/feedback", "/api/notificaciones", "/api/reportes/ocupacion-actual", "/api/disponibilidad/turnos")) {
            mvc.perform(get(ruta)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"HOST", "CLIENTE"})
    void noPermiteEscrituraDePedidosARolesNoAutorizados(String actor) throws Exception {
        Long pedido = pedidoSentado();
        for (var req : List.of(post("/api/pedidos"), post("/api/pedidos/" + pedido + "/items"),
                put("/api/pedidos/" + pedido + "/items/1"), delete("/api/pedidos/" + pedido + "/items/1"),
                patch("/api/pedidos/" + pedido + "/estado"), patch("/api/pedidos/" + pedido + "/responsable"))) {
            call(req, actor, "{}").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
        call(get("/api/pedidos"), actor, null).andExpect(actor.equals("HOST") ? status().isOk() : status().isForbidden());
    }

    @Test
    void protegeResponsableValidaItemsYConservaHistorial() throws Exception {
        Long pedido = pedidoSentado();
        String itemBody = "{\"productoId\":" + productoId + ",\"cantidad\":1}";
        call(post("/api/pedidos/" + pedido + "/items"), "otro", itemBody).andExpect(status().isForbidden());
        var res = call(post("/api/pedidos/" + pedido + "/items"), "MESERO", itemBody).andExpect(status().isOk()).andReturn();
        long item = json.readTree(res.getResponse().getContentAsString()).get("items").get(0).get("id").asLong();
        call(put("/api/pedidos/" + pedido + "/items/" + item), "MESERO", "{\"cantidad\":3}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalOperativo").value(37.5));
        call(delete("/api/pedidos/" + pedido + "/items/" + item), "MESERO", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        call(patch("/api/pedidos/" + pedido + "/estado"), "MESERO", "{\"estado\":\"EN_PREPARACION\"}")
                .andExpect(status().isUnprocessableEntity());
        Long otro = usuarioRepository.findByEmailIgnoreCase(email("otro")).orElseThrow().getId();
        call(patch("/api/pedidos/" + pedido + "/responsable"), "MESERO", "{\"usuarioId\":" + otro + "}").andExpect(status().isForbidden());
        call(patch("/api/pedidos/" + pedido + "/responsable"), "ADMINISTRADOR", "{\"usuarioId\":" + otro + "}").andExpect(status().isOk());
        call(post("/api/pedidos/" + pedido + "/items"), "MESERO", itemBody).andExpect(status().isForbidden());
        call(post("/api/pedidos/" + pedido + "/items"), "otro", itemBody).andExpect(status().isOk());
        call(get("/api/pedidos/mios"), "MESERO", null).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        call(get("/api/pedidos?mesaId=" + mesaId), "HOST", null).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void pedidosDevuelven400404409422Controlados() throws Exception {
        call(post("/api/pedidos"), "MESERO", "{}").andExpect(status().isBadRequest());
        call(post("/api/pedidos"), "MESERO", "{\"reservaId\":1,\"mesaAbiertaId\":2}").andExpect(status().isBadRequest());
        call(post("/api/pedidos"), "MESERO", "{\"reservaId\":9223372036854775807}").andExpect(status().isNotFound());
        Long reserva = reservaSentada();
        String body = "{\"reservaId\":" + reserva + "}";
        Long pedido = id(call(post("/api/pedidos"), "MESERO", body).andExpect(status().isCreated()));
        call(post("/api/pedidos"), "MESERO", body).andExpect(status().isConflict());
        call(post("/api/pedidos/" + pedido + "/items"), "MESERO", "{\"productoId\":" + productoId + ",\"cantidad\":0}").andExpect(status().isBadRequest());
        call(patch("/api/pedidos/" + pedido + "/estado"), "MESERO", "{\"estado\":\"CERRADO\"}").andExpect(status().isUnprocessableEntity());
        call(get("/api/pedidos?estado=INEXISTENTE"), "HOST", null).andExpect(status().isBadRequest());
        call(get("/api/pedidos/0"), "HOST", null).andExpect(status().isBadRequest());
        call(post("/api/pedidos"), "MESERO", "{").andExpect(status().isBadRequest());
    }

    @Test
    void atencionSinReservaPuedeAbrirseYFinalizarPeroClienteNoTieneAcceso() throws Exception {
        String body = "{\"mesaId\":" + mesaId + ",\"cantidadPersonas\":2,\"finPrevisto\":\"" + LocalDateTime.now(clock).plusHours(2) + "\"}";
        call(post("/api/mesas-abiertas"), "CLIENTE", body).andExpect(status().isForbidden());
        Long apertura = id(call(post("/api/mesas-abiertas"), "HOST", body).andExpect(status().isCreated()));
        call(get("/api/mesas-abiertas?abierta=true"), "MESERO", null).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        call(get("/api/mesas-abiertas/" + apertura), "HOST", null).andExpect(status().isOk());
        call(post("/api/mesas-abiertas"), "HOST", body).andExpect(status().isUnprocessableEntity());
        Long pedido = id(call(post("/api/pedidos"), "MESERO", "{\"mesaAbiertaId\":" + apertura + "}").andExpect(status().isCreated()));
        call(patch("/api/mesas-abiertas/" + apertura + "/finalizar"), "HOST", null).andExpect(status().isUnprocessableEntity());
        call(patch("/api/pedidos/" + pedido + "/estado"), "MESERO", "{\"estado\":\"CANCELADO\",\"motivo\":\"No consume\"}").andExpect(status().isOk());
        call(patch("/api/mesas-abiertas/" + apertura + "/finalizar"), "HOST", null).andExpect(status().isOk()).andExpect(jsonPath("$.abierta").value(false));
        call(patch("/api/mesas-abiertas/" + apertura + "/finalizar"), "HOST", null).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void feedbackAplicaPropiedadEstadoUnicidadYPermisosDeLectura() throws Exception {
        Long reserva = reservaSentada();
        String body = "{\"reservaId\":" + reserva + ",\"puntuacion\":4,\"comentario\":\"Bien\"}";
        call(post("/api/feedback"), "CLIENTE", body).andExpect(status().isUnprocessableEntity());
        reservas.cambiarEstado(reserva, email("HOST"), new ReservaEstadoUpdateRequest(EstadoReserva.FINALIZADA, null));
        call(post("/api/feedback"), "ajeno", body).andExpect(status().isNotFound());
        Long opinion = id(call(post("/api/feedback"), "CLIENTE", body).andExpect(status().isCreated()));
        call(post("/api/feedback"), "CLIENTE", body).andExpect(status().isConflict());
        call(get("/api/feedback/" + opinion), "ajeno", null).andExpect(status().isNotFound());
        call(get("/api/feedback/" + opinion), "CLIENTE", null).andExpect(status().isOk());
        call(get("/api/feedback/mios"), "ajeno", null).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        call(get("/api/feedback?puntuacion=4"), "ADMINISTRADOR", null).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        call(get("/api/feedback"), "CLIENTE", null).andExpect(status().isForbidden());
        call(get("/api/feedback/" + opinion), "HOST", null).andExpect(status().isForbidden());
        call(post("/api/feedback"), "ADMINISTRADOR", body).andExpect(status().isForbidden());
        call(post("/api/feedback"), "CLIENTE", "{\"reservaId\":" + reserva + ",\"puntuacion\":6,\"comentario\":\" \"}").andExpect(status().isBadRequest());
        call(get("/api/feedback?puntuacion=0"), "ADMINISTRADOR", null).andExpect(status().isBadRequest());
    }

    @Test
    void reportesNotificacionesYFiltrosTienenPermisosYErroresControlados() throws Exception {
        for (String actor : List.of("CLIENTE", "MESERO")) {
            call(get("/api/reportes/ocupacion-actual"), actor, null).andExpect(status().isForbidden());
        }
        call(get("/api/reportes/ocupacion-actual"), "HOST", null).andExpect(status().isOk()).andExpect(jsonPath("$.porcentaje").value(0));
        call(get("/api/reportes/reservas"), "HOST", null).andExpect(status().isBadRequest());
        call(get("/api/reportes/reservas?desde=2030-02-01&hasta=2030-01-01"), "HOST", null).andExpect(status().isUnprocessableEntity());
        call(get("/api/reportes/reservas?desde=mal&hasta=2030-01-01"), "HOST", null).andExpect(status().isBadRequest());
        call(get("/api/notificaciones"), "HOST", null).andExpect(status().isForbidden());
        call(get("/api/notificaciones?despuesDeId=-1"), "CLIENTE", null).andExpect(status().isBadRequest());
        call(get("/api/notificaciones?limite=101"), "CLIENTE", null).andExpect(status().isBadRequest());
        reservaSentada();
        call(get("/api/notificaciones"), "ajeno", null).andExpect(status().isOk()).andExpect(jsonPath("$.eventos").isEmpty());
    }

    @Test
    void sesionDevuelveRolVigenteSinClavesYRechazaCuentaDesactivada() throws Exception {
        call(get("/api/auth/me"), "MESERO", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("MESERO")).andExpect(jsonPath("$.passwordHash").doesNotExist());
        var cuenta = usuarioRepository.findByEmailIgnoreCase(email("MESERO")).orElseThrow();
        cuenta.setRol(RolUsuario.CLIENTE);
        usuarioRepository.flush();
        call(get("/api/pedidos"), "MESERO", null).andExpect(status().isForbidden());
        cuenta.setActivo(false);
        usuarioRepository.flush();
        call(get("/api/auth/me"), "MESERO", null).andExpect(status().isUnauthorized());
    }

    @Test
    void corsPermiteFrontendLocalPeroNoOrigenDesconocido() throws Exception {
        mvc.perform(options("/api/pedidos").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/pedidos").header("Origin", "https://desconocido.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        call(get("/api/auth/me").header("Origin", "http://localhost:5173"), "CLIENTE", null)
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "HOST"})
    void crudClienteConBajaLogicaYReactivacion(String actor) throws Exception {
        Long cliente = id(call(post("/api/clientes"), actor,
                "{\"nombre\":\"Cliente CRUD\",\"email\":\"crud@example.com\",\"telefono\":null}")
                .andExpect(status().isCreated()));
        call(get("/api/clientes/" + cliente), actor, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
        String update = "{\"nombre\":\"Cliente editado\",\"email\":\"crud@example.com\",\"telefono\":\"70010009\",\"activo\":true}";
        for (int i = 0; i < 2; i++) {
            call(put("/api/clientes/" + cliente), actor, update)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(cliente))
                    .andExpect(jsonPath("$.nombre").value("Cliente editado"));
        }
        for (int i = 0; i < 2; i++) {
            call(delete("/api/clientes/" + cliente), actor, null)
                    .andExpect(status().isNoContent()).andExpect(content().string(""));
        }
        call(get("/api/clientes/" + cliente), actor, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.telefono").value("70010009"));
        call(get("/api/clientes?activo=true&email=crud@example.com"), actor, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        call(get("/api/clientes?activo=false&email=crud@example.com"), actor, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        call(put("/api/clientes/" + cliente), actor, update)
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void bajaClienteConservaReservaHistorialYUsuarioVinculado() throws Exception {
        Long reserva = reservas.crearParaClienteActual(email("CLIENTE"),
                new ReservaCreateRequest(FECHA, turnoId, mesaId, 2, null)).id();
        var perfil = call(get("/api/clientes/me"), "CLIENTE", null).andExpect(status().isOk());
        Long cliente = id(perfil);
        call(delete("/api/clientes/" + cliente), "HOST", null).andExpect(status().isNoContent());
        call(get("/api/reservas?clienteId=" + cliente), "HOST", null).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reserva))
                .andExpect(jsonPath("$[0].cliente.id").value(cliente));
        call(get("/api/reservas/" + reserva + "/historial"), "HOST", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        call(get("/api/auth/me"), "CLIENTE", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        call(post("/api/reservas/operativas"), "HOST",
                "{\"clienteId\":" + cliente + ",\"fecha\":\"2030-01-16\",\"turnoId\":" + turnoId + ",\"mesaId\":" + mesaId + ",\"cantidadPersonas\":2}")
                .andExpect(status().isUnprocessableEntity());
    }

    @ParameterizedTest
    @ValueSource(strings = {"MESERO", "CLIENTE"})
    void bajaClienteNoPermiteRolesNoAutorizados(String actor) throws Exception {
        call(delete("/api/clientes/1"), actor, null).andExpect(status().isForbidden());
        call(put("/api/clientes/1"), actor, "{}").andExpect(status().isForbidden());
    }

    @Test
    void crudClienteDevuelve400404409SinCambiarDatos() throws Exception {
        mvc.perform(delete("/api/clientes/1")).andExpect(status().isUnauthorized());
        call(delete("/api/clientes/0"), "HOST", null).andExpect(status().isBadRequest());
        for (var req : List.of(get("/api/clientes/9223372036854775807"),
                delete("/api/clientes/9223372036854775807"))) {
            call(req, "HOST", null).andExpect(status().isNotFound());
        }
        String valid = "{\"nombre\":\"Persistido\",\"email\":\"crud-errores@example.com\",\"telefono\":null,\"activo\":true}";
        call(put("/api/clientes/9223372036854775807"), "HOST", valid).andExpect(status().isNotFound());
        Long cliente = id(call(post("/api/clientes"), "HOST",
                "{\"nombre\":\"Persistido\",\"email\":\"crud-errores@example.com\"}").andExpect(status().isCreated()));
        call(put("/api/clientes/" + cliente), "HOST", "{\"nombre\":\"\",\"email\":\"mal\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.activo").exists());
        call(put("/api/clientes/" + cliente), "HOST",
                valid.replace("crud-errores@example.com", email("CLIENTE")))
                .andExpect(status().isConflict());
        call(get("/api/clientes/" + cliente), "HOST", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("crud-errores@example.com"))
                .andExpect(jsonPath("$.nombre").value("Persistido"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "HOST"})
    void corrigeClienteReservaConAuditoriaSinCambiarHorario(String actor) throws Exception {
        var original = reservas.crearParaClienteActual(email("CLIENTE"), new ReservaCreateRequest(FECHA, turnoId, mesaId, 2, "Antes"));
        Long nuevo = id(call(get("/api/clientes/me"), "ajeno", null).andExpect(status().isOk()));
        String url = "/api/reservas/" + original.id() + "/datos-cliente";
        String body = json.writeValueAsString(new ReservaClienteUpdateRequest(nuevo, " Después ", "Corrección solicitada", original.version()));
        call(put(url), actor, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.id").value(nuevo))
                .andExpect(jsonPath("$.observaciones").value("Después"))
                .andExpect(jsonPath("$.estado").value("SOLICITADA"))
                .andExpect(jsonPath("$.mesa.id").value(mesaId))
                .andExpect(jsonPath("$.turno.id").value(turnoId))
                .andExpect(jsonPath("$.cantidadPersonas").value(2))
                .andExpect(jsonPath("$.creadoPorUsuarioId").value(original.creadoPorUsuarioId()));
        var actual = reservas.buscarPorId(original.id());
        assertThat(actual.version()).isGreaterThan(original.version());
        assertThat(actual.inicio()).isEqualTo(original.inicio());
        assertThat(actual.fin()).isEqualTo(original.fin());
        call(put(url), actor, body).andExpect(status().isOk());
        call(get("/api/reservas/" + original.id() + "/historial"), actor, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].tipoEvento").value("CORRECCION_CLIENTE"))
                .andExpect(jsonPath("$[1].clienteAnteriorId").value(original.cliente().id()))
                .andExpect(jsonPath("$[1].clienteNuevoId").value(nuevo))
                .andExpect(jsonPath("$[1].observacionesAnteriores").value("Antes"))
                .andExpect(jsonPath("$[1].observacionesNuevas").value("Después"));
        call(get("/api/reservas/mias"), "CLIENTE", null).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        call(get("/api/reservas/mias"), "ajeno", null).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(original.id()));
        call(get("/api/reservas?clienteId=" + original.cliente().id()), actor, null).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        call(get("/api/reservas?clienteId=" + nuevo), actor, null).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(original.id()));
    }

    @Test
    void correccionReservaRechazaReferenciaInvalidaInactivaYVersionVieja() throws Exception {
        var reserva = reservas.crearParaClienteActual(email("CLIENTE"), new ReservaCreateRequest(FECHA, turnoId, mesaId, 2, null));
        Long nuevo = id(call(get("/api/clientes/me"), "ajeno", null).andExpect(status().isOk()));
        String url = "/api/reservas/" + reserva.id() + "/datos-cliente";
        call(put(url), "HOST", "{\"clienteId\":0,\"motivo\":\" \",\"version\":-1}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.clienteId").exists());
        call(put(url), "HOST", json.writeValueAsString(new ReservaClienteUpdateRequest(Long.MAX_VALUE, null, "Corregir", reserva.version())))
                .andExpect(status().isNotFound());
        call(delete("/api/clientes/" + nuevo), "HOST", null).andExpect(status().isNoContent());
        call(put(url), "HOST", json.writeValueAsString(new ReservaClienteUpdateRequest(nuevo, null, "Corregir", reserva.version())))
                .andExpect(status().isUnprocessableEntity());
        String body = json.writeValueAsString(new ReservaClienteUpdateRequest(reserva.cliente().id(), "Nuevo texto", "Corregir", reserva.version()));
        call(put("/api/reservas/9223372036854775807/datos-cliente"), "HOST", body).andExpect(status().isNotFound());
        call(put(url), "HOST", body).andExpect(status().isOk());
        call(put(url), "HOST", body.replace("Nuevo texto", "Texto obsoleto")).andExpect(status().isConflict());
        assertThat(reservas.buscarPorId(reserva.id()).observaciones()).isEqualTo("Nuevo texto");
        assertThat(reservas.listarHistorial(reserva.id())).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONFIRMADA", "SENTADA", "FINALIZADA", "CANCELADA", "NO_SHOW"})
    void correccionNoCambiaPropietarioEnOtrosEstados(String estado) throws Exception {
        var original = reservas.crearParaClienteActual(email("CLIENTE"), new ReservaCreateRequest(FECHA, turnoId, mesaId, 2, null));
        var entidad = reservaRepository.findById(original.id()).orElseThrow();
        entidad.setEstado(EstadoReserva.valueOf(estado));
        reservaRepository.flush();
        Long nuevo = id(call(get("/api/clientes/me"), "ajeno", null).andExpect(status().isOk()));
        call(put("/api/reservas/" + original.id() + "/datos-cliente"), "HOST",
                json.writeValueAsString(new ReservaClienteUpdateRequest(nuevo, null, "Corregir", entidad.getVersion())))
                .andExpect(status().isUnprocessableEntity());
        assertThat(entidad.getCliente().getId()).isEqualTo(original.cliente().id());
        assertThat(reservas.listarHistorial(original.id())).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MESERO", "CLIENTE"})
    void correccionClienteReservaExigeRolOperativo(String actor) throws Exception {
        call(put("/api/reservas/1/datos-cliente"), actor, "{}").andExpect(status().isForbidden());
        mvc.perform(put("/api/reservas/1/datos-cliente").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String actor, String body) throws Exception {
        String token = tokens.computeIfAbsent(actor, a -> auth.login(new LoginRequest(email(a), PASSWORD)).accessToken());
        request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(request);
    }
    private Long id(ResultActions result) throws Exception {
        long value = json.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asLong();
        assertThat(value).isPositive();
        return value;
    }
    private Long reservaSentada() {
        Long reserva = reservas.crearParaClienteActual(email("CLIENTE"), new ReservaCreateRequest(FECHA, turnoId, mesaId, 2, null)).id();
        reservas.cambiarEstado(reserva, email("HOST"), new ReservaEstadoUpdateRequest(EstadoReserva.CONFIRMADA, null));
        reservas.realizarCheckIn(reserva, email("HOST"), new ReservaCheckInRequest(mesaId, null));
        return reserva;
    }
    private Long pedidoSentado() throws Exception {
        return id(call(post("/api/pedidos"), "MESERO", "{\"reservaId\":" + reservaSentada() + "}").andExpect(status().isCreated()));
    }
}
