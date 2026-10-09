package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.auth.RegisterRequest;
import com.example.gastroreservabackend1.dto.feedback.FeedbackDtos;
import com.example.gastroreservabackend1.dto.mesa.MesaCreateRequest;
import com.example.gastroreservabackend1.dto.pedido.PedidoDtos.*;
import com.example.gastroreservabackend1.dto.producto.ProductoMenuCreateRequest;
import com.example.gastroreservabackend1.dto.reserva.*;
import com.example.gastroreservabackend1.dto.turno.TurnoCreateRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioCreateRequest;
import com.example.gastroreservabackend1.dto.zona.ZonaCreateRequest;
import com.example.gastroreservabackend1.exception.*;
import com.example.gastroreservabackend1.model.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.assertThat;

// Esquema exclusivo: las operaciones se confirman realmente, no comparten la transacción del test.
@SpringBootTest(properties = {
        "spring.flyway.schemas=concurrency_test", "spring.flyway.default-schema=concurrency_test",
        "spring.jpa.properties.hibernate.default_schema=concurrency_test"
})
class ConcurrencyIntegrationTest {
    static final String ADMIN = "race-admin@example.com", CLIENT = "race-client@example.com";
    static final LocalDate DATE = LocalDate.of(2030, 1, 15);
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager em;
    @Autowired AuthService auth;
    @Autowired UsuarioService usuarios;
    @Autowired ZonaService zonas;
    @Autowired MesaService mesas;
    @Autowired TurnoService turnos;
    @Autowired ProductoMenuService productos;
    @Autowired ReservaService reservas;
    @Autowired PedidoService pedidos;
    @Autowired MesaAbiertaService abiertas;
    @Autowired FeedbackService feedback;
    @Autowired DisponibilidadService disponibilidad;
    @Autowired Clock clock;
    Long mesa, otraMesa, turno, producto;

    @BeforeEach
    void escenarioConfirmado() {
        limpiar();
        tx(() -> {
            usuarios.crear(new UsuarioCreateRequest("Admin concurrente", ADMIN, "Password-prueba-123", RolUsuario.ADMINISTRADOR));
            auth.register(new RegisterRequest("Cliente concurrente", CLIENT, "Password-prueba-123"));
            Long zona = zonas.crear(new ZonaCreateRequest("Zona concurrencia", null)).id();
            mesa = mesas.crear(new MesaCreateRequest(701, 4, zona)).id();
            otraMesa = mesas.crear(new MesaCreateRequest(702, 4, zona)).id();
            turno = turnos.crear(new TurnoCreateRequest("Turno concurrencia", LocalTime.NOON, LocalTime.of(15, 0), 4)).id();
            producto = productos.crear(new ProductoMenuCreateRequest("Producto concurrente", null, new BigDecimal("10.50"))).id();
            return null;
        });
    }

    @AfterEach
    void limpiar() {
        tx(() -> {
            // JPQL se limita al schema concurrency_test configurado arriba; no toca datos de la aplicación.
            for (String entidad : List.of("HistorialPedido", "ItemPedido", "Feedback", "Pedido", "MesaAbierta",
                    "HistorialReserva", "Reserva", "ProductoMenu", "Cliente", "Turno", "Mesa", "Zona", "Usuario")) {
                em.createQuery("delete from " + entidad).executeUpdate();
            }
            return null;
        });
    }

    @RepeatedTest(3)
    void mismaMesaNoAceptaDosReservasSimultaneas() throws Exception {
        unoSolo(carrera(() -> crear(mesa, 2), () -> crear(mesa, 2)));
        assertThat(reservas.listar(null, null, null, null, null, null)).hasSize(1);
    }

    @RepeatedTest(3)
    void mesasDistintasNoSuperanCapacidadDelTurno() throws Exception {
        unoSolo(carrera(() -> crear(mesa, 4), () -> crear(otraMesa, 4)));
        assertThat(disponibilidad.consultar(DATE, turno, 1, null).capacidadDisponibleTurno()).isZero();
    }

    @Test
    void unPedidoPorVisitaInclusoConDosSolicitudes() throws Exception {
        Long reserva = sentar();
        unoSolo(carrera(() -> pedidos.crear(ADMIN, new Crear(reserva, null)), () -> pedidos.crear(ADMIN, new Crear(reserva, null))));
        assertThat(pedidos.listar(ADMIN, null, null, reserva, false)).hasSize(1);
    }

    @Test
    void agregarItemsSimultaneosNoPierdeLineasNiTotal() throws Exception {
        Long pedido = pedidos.crear(ADMIN, new Crear(sentar(), null)).id();
        assertThat(carrera(() -> pedidos.agregar(pedido, ADMIN, new AgregarItem(producto, 2)),
                () -> pedidos.agregar(pedido, ADMIN, new AgregarItem(producto, 3)))).containsOnly("OK");
        assertThat(pedidos.buscar(pedido, ADMIN).totalOperativo()).isEqualByComparingTo("52.50");
        assertThat(pedidos.buscar(pedido, ADMIN).items()).hasSize(2);
        assertThat(pedidos.historial(pedido, ADMIN)).hasSize(3);
    }

    @Test
    void noSePuedeAbrirDosVecesLaMismaMesa() throws Exception {
        var request = new AbrirMesa(mesa, 2, LocalDateTime.now(clock).plusHours(1));
        unoSolo(carrera(() -> abiertas.abrir(ADMIN, request), () -> abiertas.abrir(ADMIN, request)));
        assertThat(abiertas.listar(ADMIN, true, mesa)).hasSize(1);
        assertThat(mesas.buscarPorId(mesa).estado()).isEqualTo(Mesa.ESTADO_OCUPADA);
    }

    @Test
    void feedbackUnicoBajoConcurrencia() throws Exception {
        Long reserva = sentar();
        reservas.cambiarEstado(reserva, ADMIN, new ReservaEstadoUpdateRequest(EstadoReserva.FINALIZADA, null));
        var request = new FeedbackDtos.Crear(reserva, 5, "Excelente");
        unoSolo(carrera(() -> feedback.crear(CLIENT, request), () -> feedback.crear(CLIENT, request)));
        assertThat(feedback.listar(CLIENT, true, null, null, null)).hasSize(1);
    }

    @RepeatedTest(3)
    void crearPedidoYFinalizarVisitaNoDejanPedidoActivoEnVisitaFinalizada() throws Exception {
        Long reserva = sentar();
        unoSolo(carrera(() -> pedidos.crear(ADMIN, new Crear(reserva, null)),
                () -> reservas.cambiarEstado(reserva, ADMIN, new ReservaEstadoUpdateRequest(EstadoReserva.FINALIZADA, null))));
        var lista = pedidos.listar(ADMIN, null, null, reserva, false);
        var estado = reservas.listar(null, null, null, null, null, null).getFirst().estado();
        assertThat(estado == EstadoReserva.FINALIZADA ? lista.isEmpty() : lista.size() == 1).isTrue();
    }

    private Long crear(Long mesaId, int personas) {
        return reservas.crearParaClienteActual(CLIENT, new ReservaCreateRequest(DATE, turno, mesaId, personas, null)).id();
    }
    private Long sentar() {
        Long reserva = crear(mesa, 2);
        reservas.cambiarEstado(reserva, ADMIN, new ReservaEstadoUpdateRequest(EstadoReserva.CONFIRMADA, null));
        reservas.realizarCheckIn(reserva, ADMIN, new ReservaCheckInRequest(mesa, null));
        return reserva;
    }
    private <T> T tx(Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }
    private List<String> carrera(Supplier<?> first, Supplier<?> second) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<String>> results = new ArrayList<>();
            for (var action : List.of(first, second)) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("No iniciaron los dos actores");
                    try { action.get(); return "OK"; }
                    catch (BusinessRuleException | ResourceConflictException e) { return "RECHAZADO"; }
                }));
            }
            try {
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            } finally { start.countDown(); }
            try {
                return List.of(results.get(0).get(30, TimeUnit.SECONDS), results.get(1).get(30, TimeUnit.SECONDS));
            } finally { pool.shutdownNow(); }
        }
    }
    private void unoSolo(List<String> results) {
        assertThat(results).containsExactlyInAnyOrder("OK", "RECHAZADO");
    }
}
