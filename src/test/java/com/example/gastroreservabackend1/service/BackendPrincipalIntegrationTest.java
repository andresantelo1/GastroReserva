package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.auth.RegisterRequest;
import com.example.gastroreservabackend1.dto.feedback.FeedbackDtos;
import com.example.gastroreservabackend1.dto.mesa.*;
import com.example.gastroreservabackend1.dto.pedido.PedidoDtos.*;
import com.example.gastroreservabackend1.dto.producto.*;
import com.example.gastroreservabackend1.dto.reserva.*;
import com.example.gastroreservabackend1.dto.turno.TurnoCreateRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioCreateRequest;
import com.example.gastroreservabackend1.dto.zona.*;
import com.example.gastroreservabackend1.exception.*;
import com.example.gastroreservabackend1.model.*;
import com.example.gastroreservabackend1.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;

import static com.example.gastroreservabackend1.model.RolUsuario.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(BackendPrincipalIntegrationTest.FixedClock.class)
@Transactional
class BackendPrincipalIntegrationTest {
    static final String ADMIN = "core-admin@example.com", HOST = "core-host@example.com",
            MESERO = "core-mesero@example.com", OTRO_MESERO = "core-otro-mesero@example.com",
            CLIENTE = "core-cliente@example.com", OTRO_CLIENTE = "core-otro-cliente@example.com";
    static final LocalDate FECHA = LocalDate.of(2030, 1, 15);
    @TestConfiguration
    static class FixedClock {
        @Bean @Primary Clock testClock() {
            return Clock.fixed(Instant.parse("2030-01-15T16:00:00Z"), ZoneId.of("America/La_Paz"));
        }
    }

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
    @Autowired ReporteService reportes;
    @Autowired NotificacionService notificaciones;
    @Autowired DisponibilidadService disponibilidad;
    @Autowired ReservaRepository reservaRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PedidoRepository pedidoRepository;
    @Autowired EntityManager em;
    Long zonaId, mesaId, otraMesaId, terceraMesaId, cuartaMesaId, turnoId, productoId;

    @BeforeEach
    void scenario() {
        usuarios.crear(new UsuarioCreateRequest("Admin core", ADMIN, "Password-prueba-123", ADMINISTRADOR));
        usuarios.crear(new UsuarioCreateRequest("Host core", HOST, "Password-prueba-123", RolUsuario.HOST));
        usuarios.crear(new UsuarioCreateRequest("Mesero core", MESERO, "Password-prueba-123", RolUsuario.MESERO));
        usuarios.crear(new UsuarioCreateRequest("Otro mesero", OTRO_MESERO, "Password-prueba-123", RolUsuario.MESERO));
        auth.register(new RegisterRequest("Cliente core", CLIENTE, "Password-prueba-123"));
        auth.register(new RegisterRequest("Otro cliente", OTRO_CLIENTE, "Password-prueba-123"));
        zonaId = zonas.crear(new ZonaCreateRequest("Zona core", null)).id();
        mesaId = mesas.crear(new MesaCreateRequest(901, 4, zonaId)).id();
        otraMesaId = mesas.crear(new MesaCreateRequest(902, 4, zonaId)).id();
        terceraMesaId = mesas.crear(new MesaCreateRequest(903, 4, zonaId)).id();
        cuartaMesaId = mesas.crear(new MesaCreateRequest(904, 4, zonaId)).id();
        turnoId = turnos.crear(new TurnoCreateRequest("Almuerzo core", LocalTime.NOON, LocalTime.of(15, 0), 16)).id();
        productoId = productos.crear(new ProductoMenuCreateRequest("Hamburguesa core", null, new BigDecimal("35.50"))).id();
    }

    @Test
    void completesReservationOrderVisitAndFeedbackWithActorHistory() {
        Long reserva = sentar(mesaId, CLIENTE);
        var pedido = pedidos.crear(MESERO, new Crear(reserva, null));
        var conItem = pedidos.agregar(pedido.id(), MESERO, new AgregarItem(productoId, 2));
        assertThat(conItem.totalOperativo()).isEqualByComparingTo("71.00");
        assertThatThrownBy(() -> finalizar(reserva)).isInstanceOf(BusinessRuleException.class);
        cerrarPedido(pedido.id());
        finalizar(reserva);
        assertThat(mesas.buscarPorId(mesaId).estado()).isEqualTo("DISPONIBLE");
        var opinion = feedback.crear(CLIENTE, new FeedbackDtos.Crear(reserva, 5, " Buena atención "));
        assertThat(opinion.comentario()).isEqualTo("Buena atención");
        assertThat(feedback.buscar(opinion.id(), CLIENTE)).isEqualTo(opinion);
        assertThat(pedidos.historial(pedido.id(), HOST)).hasSize(5)
                .allSatisfy(h -> { assertThat(h.cambiadoPorId()).isNotNull(); assertThat(h.creadoEn()).isNotNull(); });
        assertThat(pedidos.historial(pedido.id(), ADMIN)).last().extracting(Historial::estadoNuevo).isEqualTo(EstadoPedido.CERRADO);
        assertThat(notificaciones.consultar(CLIENTE, 0, 50).eventos()).hasSize(4);
        assertThat(notificaciones.consultar(OTRO_CLIENTE, 0, 50).eventos()).isEmpty();
    }

    @Test
    void preservesHistoricalPriceAndNameAndAddsNewPriceOnSeparateLine() {
        var pedido = pedidos.crear(MESERO, new Crear(sentar(mesaId, CLIENTE), null));
        var inicial = pedidos.agregar(pedido.id(), MESERO, new AgregarItem(productoId, 2));
        productos.actualizar(productoId, new ProductoMenuUpdateRequest("Hamburguesa nueva", null, new BigDecimal("40.75"), true));
        var actualizado = pedidos.agregar(pedido.id(), MESERO, new AgregarItem(productoId, 1));
        em.flush(); em.clear();
        var recargado = pedidos.buscar(pedido.id(), HOST);
        assertThat(recargado.items()).hasSize(2);
        assertThat(recargado.items().getFirst().precioUnitarioHistorico()).isEqualByComparingTo("35.50");
        assertThat(recargado.items().getFirst().nombreProducto()).isEqualTo("Hamburguesa core");
        assertThat(recargado.totalOperativo()).isEqualByComparingTo("111.75");
        var cantidad = pedidos.cantidad(pedido.id(), inicial.items().getFirst().id(), MESERO, new CambiarCantidad(3));
        assertThat(cantidad.totalOperativo()).isEqualByComparingTo("147.25");
        var quitado = pedidos.quitar(pedido.id(), actualizado.items().getLast().id(), MESERO);
        assertThat(quitado.totalOperativo()).isEqualByComparingTo("106.50");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoReserva.class, names = "SENTADA", mode = EnumSource.Mode.EXCLUDE)
    void rejectsOrdersForReservationsNotSeated(EstadoReserva estado) {
        Long reserva = crearReserva(mesaId, CLIENTE);
        reservaRepository.findById(reserva).orElseThrow().setEstado(estado);
        assertThatThrownBy(() -> pedidos.crear(MESERO, new Crear(reserva, null))).isInstanceOf(BusinessRuleException.class);
        assertThat(pedidoRepository.count()).isZero();
    }

    @Test
    void rejectsDuplicateOrdersAndInvalidOrigins() {
        Long reserva = sentar(mesaId, CLIENTE);
        pedidos.crear(MESERO, new Crear(reserva, null));
        assertThatThrownBy(() -> pedidos.crear(MESERO, new Crear(reserva, null))).isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> pedidos.crear(MESERO, new Crear(null, null))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> pedidos.crear(MESERO, new Crear(reserva, 1L))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> pedidos.crear(MESERO, new Crear(Long.MAX_VALUE, null))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void preventsInvalidTransitionsEmptyPreparationAndChangesAfterPreparation() {
        var p = pedidos.crear(MESERO, new Crear(sentar(mesaId, CLIENTE), null));
        assertThatThrownBy(() -> pedidos.cambiarEstado(p.id(), MESERO, new CambiarEstado(EstadoPedido.EN_PREPARACION, null)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> pedidos.cambiarEstado(p.id(), MESERO, new CambiarEstado(EstadoPedido.CERRADO, null)))
                .isInstanceOf(BusinessRuleException.class);
        var item = pedidos.agregar(p.id(), MESERO, new AgregarItem(productoId, 1)).items().getFirst();
        pedidos.cambiarEstado(p.id(), MESERO, new CambiarEstado(EstadoPedido.EN_PREPARACION, null));
        assertThatThrownBy(() -> pedidos.agregar(p.id(), MESERO, new AgregarItem(productoId, 1))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> pedidos.cantidad(p.id(), item.id(), MESERO, new CambiarCantidad(2))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> pedidos.quitar(p.id(), item.id(), MESERO)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancellationRequiresReasonAndIsTerminal() {
        Long reserva = sentar(mesaId, CLIENTE);
        var p = pedidos.crear(MESERO, new Crear(reserva, null));
        assertThatThrownBy(() -> pedidos.cambiarEstado(p.id(), MESERO, new CambiarEstado(EstadoPedido.CANCELADO, " ")))
                .isInstanceOf(BusinessRuleException.class);
        pedidos.cambiarEstado(p.id(), MESERO, new CambiarEstado(EstadoPedido.CANCELADO, "Cliente no consumirá"));
        assertThatThrownBy(() -> pedidos.cambiarEstado(p.id(), MESERO, new CambiarEstado(EstadoPedido.ABIERTO, null)))
                .isInstanceOf(BusinessRuleException.class);
        finalizar(reserva);
        assertThatThrownBy(() -> pedidos.agregar(p.id(), MESERO, new AgregarItem(productoId, 1))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsUnavailableProductsInvalidQuantitiesAndItemsFromOtherOrders() {
        var p = pedidos.crear(MESERO, new Crear(sentar(mesaId, CLIENTE), null));
        var otro = pedidos.crear(MESERO, new Crear(sentar(otraMesaId, OTRO_CLIENTE), null));
        Long itemId = pedidos.agregar(otro.id(), MESERO, new AgregarItem(productoId, 1)).items().getFirst().id();
        assertThatThrownBy(() -> pedidos.quitar(p.id(), itemId, MESERO)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> pedidos.agregar(p.id(), MESERO, new AgregarItem(productoId, 0))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> pedidos.agregar(p.id(), MESERO, new AgregarItem(productoId, 1000))).isInstanceOf(ConstraintViolationException.class);
        productos.actualizar(productoId, new ProductoMenuUpdateRequest("Hamburguesa core", null, new BigDecimal("35.50"), false));
        assertThatThrownBy(() -> pedidos.agregar(p.id(), MESERO, new AgregarItem(productoId, 1))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void waiterOwnershipCanBeTransferredOnlyToAnActiveWaiterByAdmin() {
        var p = pedidos.crear(MESERO, new Crear(sentar(mesaId, CLIENTE), null));
        assertThat(pedidos.listar(MESERO, null, null, null, true)).hasSize(1);
        assertThat(pedidos.listar(OTRO_MESERO, null, null, null, true)).isEmpty();
        assertThatThrownBy(() -> pedidos.agregar(p.id(), OTRO_MESERO, new AgregarItem(productoId, 1))).isInstanceOf(AccessDeniedException.class);
        Long nuevo = usuarioRepository.findByEmailIgnoreCase(OTRO_MESERO).orElseThrow().getId();
        assertThatThrownBy(() -> pedidos.asignar(p.id(), MESERO, new AsignarMesero(nuevo))).isInstanceOf(AccessDeniedException.class);
        pedidos.asignar(p.id(), ADMIN, new AsignarMesero(nuevo));
        assertThat(pedidos.listar(OTRO_MESERO, null, null, null, true)).hasSize(1);
        pedidos.agregar(p.id(), OTRO_MESERO, new AgregarItem(productoId, 1));
        assertThatThrownBy(() -> pedidos.agregar(p.id(), MESERO, new AgregarItem(productoId, 1))).isInstanceOf(AccessDeniedException.class);
        Long cliente = usuarioRepository.findByEmailIgnoreCase(CLIENTE).orElseThrow().getId();
        assertThatThrownBy(() -> pedidos.asignar(p.id(), ADMIN, new AsignarMesero(cliente))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void orderFollowsAuditedReservationReassignmentWithoutStaleTableId() {
        Long reserva = sentar(mesaId, CLIENTE);
        var p = pedidos.crear(MESERO, new Crear(reserva, null));
        reservas.reasignarMesa(reserva, HOST, new ReservaReasignacionRequest(otraMesaId, "Cambio solicitado"));
        em.flush(); em.clear();
        assertThat(pedidos.buscar(p.id(), MESERO).mesaId()).isEqualTo(otraMesaId);
        assertThat(pedidos.listar(HOST, null, otraMesaId, reserva, false)).hasSize(1);
        assertThat(pedidos.listar(HOST, null, mesaId, null, false)).isEmpty();
    }

    @Test
    void opensWalkInTableCreatesOrderAndClosesWithoutLosingHistory() {
        var a = abiertas.abrir(HOST, new AbrirMesa(mesaId, 4, FECHA.atTime(14, 0)));
        assertThat(mesas.buscarPorId(mesaId).estado()).isEqualTo("OCUPADA");
        var p = pedidos.crear(MESERO, new Crear(null, a.id()));
        pedidos.agregar(p.id(), MESERO, new AgregarItem(productoId, 1));
        assertThatThrownBy(() -> abiertas.finalizar(a.id(), HOST)).isInstanceOf(BusinessRuleException.class);
        cerrarPedido(p.id());
        var cerrada = abiertas.finalizar(a.id(), HOST);
        assertThat(cerrada.abierta()).isFalse();
        assertThat(cerrada.cerradaPorId()).isNotNull();
        assertThat(cerrada.cerradaEn()).isNotNull();
        assertThat(mesas.buscarPorId(mesaId).estado()).isEqualTo("DISPONIBLE");
        assertThat(abiertas.listar(HOST, false, mesaId)).hasSize(1);
        assertThat(pedidos.buscar(p.id(), HOST).totalOperativo()).isEqualByComparingTo("35.50");
        assertThatThrownBy(() -> pedidos.crear(MESERO, new Crear(null, a.id()))).isInstanceOf(BusinessRuleException.class);
        assertThat(abiertas.abrir(HOST, new AbrirMesa(mesaId, 2, FECHA.atTime(15, 0))).id()).isNotEqualTo(a.id());
    }

    @Test
    void walkInTableProtectsCapacityAvailabilityAndReservationsInBothDirections() {
        var a = abiertas.abrir(HOST, new AbrirMesa(mesaId, 4, FECHA.atTime(14, 0)));
        assertThatThrownBy(() -> abiertas.abrir(HOST, new AbrirMesa(mesaId, 1, FECHA.atTime(14, 0)))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> crearReserva(mesaId, CLIENTE)).isInstanceOf(BusinessRuleException.class);
        assertThat(disponibilidad.consultar(FECHA, turnoId, 4, null).mesas()).extracting(MesaDisponibleResponse::id).doesNotContain(mesaId);
        assertThatThrownBy(() -> mesas.actualizar(mesaId, new MesaUpdateRequest(901, 3, true, zonaId))).isInstanceOf(BusinessRuleException.class);
        crearReserva(otraMesaId, CLIENTE);
        assertThatThrownBy(() -> abiertas.abrir(HOST, new AbrirMesa(otraMesaId, 1, FECHA.atTime(14, 0)))).isInstanceOf(BusinessRuleException.class);
        abiertas.finalizar(a.id(), HOST);
        assertThat(disponibilidad.consultar(FECHA, turnoId, 4, null).mesas()).extracting(MesaDisponibleResponse::id).contains(mesaId);
    }

    @Test
    void walkInOpeningRejectsPastEndCapacityAndInactiveZone() {
        assertThatThrownBy(() -> abiertas.abrir(HOST, new AbrirMesa(mesaId, 1, FECHA.atTime(12, 0)))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> abiertas.abrir(HOST, new AbrirMesa(mesaId, 5, FECHA.atTime(14, 0)))).isInstanceOf(BusinessRuleException.class);
        zonas.actualizar(zonaId, new ZonaUpdateRequest("Zona core", null, false));
        assertThatThrownBy(() -> abiertas.abrir(HOST, new AbrirMesa(mesaId, 1, FECHA.atTime(14, 0)))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void feedbackRejectsNonFinalVisitsOtherOwnersDuplicatesAndUnauthorizedReaders() {
        Long reserva = crearReserva(mesaId, CLIENTE);
        assertThatThrownBy(() -> feedback.crear(CLIENTE, new FeedbackDtos.Crear(reserva, 5, "Bien"))).isInstanceOf(BusinessRuleException.class);
        reservas.cambiarEstado(reserva, ADMIN, new ReservaEstadoUpdateRequest(EstadoReserva.CONFIRMADA, null));
        reservas.realizarCheckIn(reserva, HOST, new ReservaCheckInRequest(mesaId, null));
        finalizar(reserva);
        assertThatThrownBy(() -> feedback.crear(OTRO_CLIENTE, new FeedbackDtos.Crear(reserva, 5, "Bien"))).isInstanceOf(ResourceNotFoundException.class);
        var f = feedback.crear(CLIENTE, new FeedbackDtos.Crear(reserva, 5, "Bien"));
        assertThatThrownBy(() -> feedback.crear(CLIENTE, new FeedbackDtos.Crear(reserva, 4, "Otro"))).isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> feedback.buscar(f.id(), OTRO_CLIENTE)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> feedback.listar(HOST, false, null, null, null)).isInstanceOf(AccessDeniedException.class);
        LocalDate fechaComentario = f.creadoEn().atZone(ZoneId.of("America/La_Paz")).toLocalDate();
        assertThat(feedback.listar(ADMIN, false, 5, fechaComentario, fechaComentario)).hasSize(1);
        assertThat(feedback.listar(CLIENTE, true, null, null, null)).hasSize(1);
        assertThat(feedback.listar(OTRO_CLIENTE, true, null, null, null)).isEmpty();
        assertThat(feedback.listar(ADMIN, false, 1, null, null)).isEmpty();
    }

    @Test
    void reportsHaveKnownCountsDateBoundariesAndExplicitNoShowDenominator() {
        sentar(mesaId, CLIENTE);
        Long noShow = crearReserva(otraMesaId, OTRO_CLIENTE);
        reservas.cambiarEstado(noShow, ADMIN, new ReservaEstadoUpdateRequest(EstadoReserva.CONFIRMADA, null));
        reservas.cambiarEstado(noShow, HOST, new ReservaEstadoUpdateRequest(EstadoReserva.NO_SHOW, "No llegó"));
        Long cancelada = crearReserva(terceraMesaId, CLIENTE);
        reservas.cancelarActual(cancelada, CLIENTE);
        crearReserva(cuartaMesaId, CLIENTE);
        var informe = reportes.resumen(ADMIN, FECHA, FECHA);
        assertThat(informe.reservas()).isEqualTo(4);
        assertThat(informe.atendidas()).isEqualTo(1);
        assertThat(informe.noShow()).isEqualTo(1);
        assertThat(informe.canceladas()).isEqualTo(1);
        assertThat(informe.tasaNoShow()).isEqualByComparingTo("50.00");
        assertThat(informe.porFechaTurno()).hasSize(1);
        assertThat(informe.porFechaTurno().getFirst().personasReservadasActivas()).isEqualTo(8);
        assertThat(informe.porFechaTurno().getFirst().porcentajeCapacidadReservada()).isEqualByComparingTo("50.00");
        assertThat(reportes.ocupacion(HOST).porcentaje()).isEqualByComparingTo("25.00");
        assertThat(reportes.resumen(ADMIN, FECHA.plusDays(1), FECHA.plusDays(1)).reservas()).isZero();
    }

    @Test
    void emptyReportsAndInvalidRangesAreControlled() {
        assertThat(reportes.resumen(HOST, FECHA, FECHA).tasaNoShow()).isEqualByComparingTo("0.00");
        assertThat(reportes.resumen(HOST, FECHA, FECHA).porFechaTurno()).isEmpty();
        zonas.actualizar(zonaId, new ZonaUpdateRequest("Zona core", null, false));
        assertThat(reportes.ocupacion(ADMIN).mesasActivas()).isZero();
        assertThat(reportes.ocupacion(ADMIN).porcentaje()).isEqualByComparingTo("0.00");
        assertThatThrownBy(() -> reportes.resumen(ADMIN, FECHA, FECHA.minusDays(1))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> reportes.resumen(ADMIN, FECHA, FECHA.plusDays(366))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> reportes.resumen(CLIENTE, FECHA, FECHA)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void notificationsAreOwnerScopedAndPaginatedWithoutLosingEvents() {
        Long reserva = crearReserva(mesaId, CLIENTE);
        reservas.cambiarEstado(reserva, ADMIN, new ReservaEstadoUpdateRequest(EstadoReserva.CONFIRMADA, null));
        crearReserva(otraMesaId, OTRO_CLIENTE);
        var primera = notificaciones.consultar(CLIENTE, 0, 1);
        assertThat(primera.hayMas()).isTrue();
        assertThat(primera.eventos()).hasSize(1);
        var segunda = notificaciones.consultar(CLIENTE, primera.ultimoId(), 1);
        assertThat(segunda.hayMas()).isFalse();
        assertThat(segunda.eventos().getFirst().estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(notificaciones.consultar(CLIENTE, segunda.ultimoId(), 1).eventos()).isEmpty();
        assertThatThrownBy(() -> notificaciones.consultar(CLIENTE, -1, 50)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> notificaciones.consultar(CLIENTE, 0, 101)).isInstanceOf(ConstraintViolationException.class);
    }

    private Long crearReserva(Long mesa, String cliente) {
        return reservas.crearParaClienteActual(cliente, new ReservaCreateRequest(FECHA, turnoId, mesa, 4, null)).id();
    }
    private Long sentar(Long mesa, String cliente) {
        Long reserva = crearReserva(mesa, cliente);
        reservas.cambiarEstado(reserva, ADMIN, new ReservaEstadoUpdateRequest(EstadoReserva.CONFIRMADA, null));
        reservas.realizarCheckIn(reserva, HOST, new ReservaCheckInRequest(mesa, null));
        return reserva;
    }
    private void cerrarPedido(Long pedido) {
        pedidos.cambiarEstado(pedido, MESERO, new CambiarEstado(EstadoPedido.EN_PREPARACION, null));
        pedidos.cambiarEstado(pedido, MESERO, new CambiarEstado(EstadoPedido.SERVIDO, null));
        pedidos.cambiarEstado(pedido, MESERO, new CambiarEstado(EstadoPedido.CERRADO, null));
    }
    private void finalizar(Long reserva) {
        reservas.cambiarEstado(reserva, MESERO, new ReservaEstadoUpdateRequest(EstadoReserva.FINALIZADA, "Visita terminada"));
    }
}
