package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.auth.RegisterRequest;
import com.example.gastroreservabackend1.dto.mesa.MesaCreateRequest;
import com.example.gastroreservabackend1.dto.mesa.MesaUpdateRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaCreateRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaCheckInRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaEstadoUpdateRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaReasignacionRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaResponse;
import com.example.gastroreservabackend1.dto.turno.TurnoCreateRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioCreateRequest;
import com.example.gastroreservabackend1.dto.zona.ZonaCreateRequest;
import com.example.gastroreservabackend1.exception.BusinessRuleException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.model.EstadoReserva;
import com.example.gastroreservabackend1.model.Mesa;
import com.example.gastroreservabackend1.model.RolUsuario;
import com.example.gastroreservabackend1.model.TipoEventoReserva;
import com.example.gastroreservabackend1.repository.ClienteRepository;
import com.example.gastroreservabackend1.repository.HistorialReservaRepository;
import com.example.gastroreservabackend1.repository.MesaRepository;
import com.example.gastroreservabackend1.repository.ReservaRepository;
import com.example.gastroreservabackend1.repository.TurnoRepository;
import com.example.gastroreservabackend1.repository.UsuarioRepository;
import com.example.gastroreservabackend1.repository.ZonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReservaServiceIntegrationTest {

    private static final String PASSWORD = "Password-segura-123";
    private static final LocalDate FECHA = LocalDate.of(2030, 1, 15);

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ZonaService zonaService;

    @Autowired
    private MesaService mesaService;

    @Autowired
    private TurnoService turnoService;

    @Autowired
    private DisponibilidadService disponibilidadService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private HistorialReservaRepository historialRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private ZonaRepository zonaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void cleanDatabase() {
        historialRepository.deleteAll();
        reservaRepository.deleteAll();
        mesaRepository.deleteAll();
        zonaRepository.deleteAll();
        clienteRepository.deleteAll();
        turnoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void createsAReservationAndRemovesTheTableFromAvailability() {
        Scenario scenario = scenario(10, 4);

        var before = disponibilidadService.consultar(FECHA, scenario.turnoId(), 4, null);
        assertThat(before.mesas()).extracting("id").containsExactly(scenario.mesaId());

        ReservaResponse reserva = reservaService.crearParaClienteActual(
                scenario.clientEmail(), request(scenario, 4));

        assertThat(reserva.estado()).isEqualTo(EstadoReserva.SOLICITADA);
        assertThat(reserva.inicio()).isEqualTo(FECHA.atTime(12, 0));
        assertThat(historialRepository.findByReservaIdOrderByCreadoEnAsc(reserva.id()))
                .hasSize(1)
                .first()
                .extracting("estadoNuevo")
                .isEqualTo(EstadoReserva.SOLICITADA);

        var after = disponibilidadService.consultar(FECHA, scenario.turnoId(), 4, null);
        assertThat(after.capacidadReservadaTurno()).isEqualTo(4);
        assertThat(after.capacidadDisponibleTurno()).isEqualTo(6);
        assertThat(after.mesas()).isEmpty();
    }

    @Test
    void enforcesTableAndShiftCapacity() {
        Scenario scenario = scenario(6, 4);

        assertThatThrownBy(() -> reservaService.crearParaClienteActual(
                scenario.clientEmail(), request(scenario, 5)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("admite como máximo 4");

        reservaService.crearParaClienteActual(scenario.clientEmail(), request(scenario, 4));
        Long secondMesaId = mesaService.crear(new MesaCreateRequest(2, 4, scenario.zonaId())).id();
        authService.register(new RegisterRequest("Segundo cliente", "segundo@example.com", PASSWORD));

        assertThatThrownBy(() -> reservaService.crearParaClienteActual(
                "segundo@example.com",
                new ReservaCreateRequest(FECHA, scenario.turnoId(), secondMesaId, 3, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("turno no tiene capacidad suficiente");
    }

    @Test
    void preventsAdministrativeCapacityChangesThatInvalidateActiveReservations() {
        Scenario scenario = scenario(10, 4);
        reservaService.crearParaClienteActual(scenario.clientEmail(), request(scenario, 4));

        assertThatThrownBy(() -> mesaService.actualizar(
                scenario.mesaId(), new MesaUpdateRequest(1, 3, true, scenario.zonaId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reserva activa");

        assertThatThrownBy(() -> turnoService.actualizar(
                scenario.turnoId(),
                new com.example.gastroreservabackend1.dto.turno.TurnoUpdateRequest(
                        "Almuerzo", LocalTime.of(12, 0), LocalTime.of(15, 0), 3, true)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reservas activas");
    }

    @Test
    void rejectsOverlappingReservationsAndAllowsTheTableAfterCancellation() {
        Scenario scenario = scenario(20, 4);
        ReservaResponse first = reservaService.crearParaClienteActual(
                scenario.clientEmail(), request(scenario, 4));
        authService.register(new RegisterRequest("Segundo cliente", "segundo@example.com", PASSWORD));

        assertThatThrownBy(() -> reservaService.crearParaClienteActual(
                "segundo@example.com", request(scenario, 2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("se solapa");

        reservaService.cancelarActual(first.id(), scenario.clientEmail());

        ReservaResponse second = reservaService.crearParaClienteActual(
                "segundo@example.com", request(scenario, 2));
        assertThat(second.estado()).isEqualTo(EstadoReserva.SOLICITADA);
    }

    @Test
    void onlyTheOwnerCanCancelThroughTheClientOperation() {
        Scenario scenario = scenario(20, 4);
        ReservaResponse reserva = reservaService.crearParaClienteActual(
                scenario.clientEmail(), request(scenario, 2));
        authService.register(new RegisterRequest("Segundo cliente", "segundo@example.com", PASSWORD));

        assertThatThrownBy(() -> reservaService.cancelarActual(reserva.id(), "segundo@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void appliesTheStateMachineAndAuditsEveryTransition() {
        Scenario scenario = scenario(20, 4);
        ReservaResponse reserva = reservaService.crearParaClienteActual(
                scenario.clientEmail(), request(scenario, 2));
        crearAdministrador();

        reservaService.cambiarEstado(reserva.id(), "admin@example.com",
                new ReservaEstadoUpdateRequest(EstadoReserva.CONFIRMADA, "Confirmada por recepción"));

        assertThatThrownBy(() -> reservaService.cambiarEstado(reserva.id(), "admin@example.com",
                new ReservaEstadoUpdateRequest(EstadoReserva.SENTADA, "Intento directo")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("operación de check-in");

        ReservaResponse sentada = reservaService.realizarCheckIn(
                reserva.id(),
                "admin@example.com",
                new ReservaCheckInRequest(scenario.mesaId(), "Cliente recibido"));
        assertThat(sentada.estado()).isEqualTo(EstadoReserva.SENTADA);
        assertThat(mesaRepository.findById(scenario.mesaId()).orElseThrow().getEstado())
                .isEqualTo(Mesa.ESTADO_OCUPADA);

        ReservaResponse finished = reservaService.cambiarEstado(reserva.id(), "admin@example.com",
                new ReservaEstadoUpdateRequest(EstadoReserva.FINALIZADA, "Visita finalizada"));

        assertThat(finished.estado()).isEqualTo(EstadoReserva.FINALIZADA);
        assertThat(mesaRepository.findById(scenario.mesaId()).orElseThrow().getEstado())
                .isEqualTo(Mesa.ESTADO_DISPONIBLE);
        assertThat(reservaService.listarHistorial(reserva.id()))
                .extracting("estadoNuevo")
                .containsExactly(
                        EstadoReserva.SOLICITADA,
                        EstadoReserva.CONFIRMADA,
                        EstadoReserva.SENTADA,
                        EstadoReserva.FINALIZADA);
        assertThat(reservaService.listarHistorial(reserva.id()))
                .extracting("tipoEvento")
                .containsExactly(
                        TipoEventoReserva.CREACION,
                        TipoEventoReserva.CAMBIO_ESTADO,
                        TipoEventoReserva.CHECK_IN,
                        TipoEventoReserva.CAMBIO_ESTADO);

        assertThatThrownBy(() -> reservaService.cambiarEstado(reserva.id(), "admin@example.com",
                new ReservaEstadoUpdateRequest(EstadoReserva.CANCELADA, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No se permite");
    }

    @Test
    void checkInCanChangeTheTableAndRecordsBothAssignments() {
        Scenario scenario = scenario(20, 4);
        Long mesaNuevaId = mesaService.crear(
                new MesaCreateRequest(2, 6, scenario.zonaId())).id();
        ReservaResponse reserva = reservaService.crearParaClienteActual(
                scenario.clientEmail(), request(scenario, 4));
        crearAdministrador();
        reservaService.cambiarEstado(reserva.id(), "admin@example.com",
                new ReservaEstadoUpdateRequest(EstadoReserva.CONFIRMADA, "Confirmada"));

        ReservaResponse sentada = reservaService.realizarCheckIn(
                reserva.id(),
                "admin@example.com",
                new ReservaCheckInRequest(mesaNuevaId, "Cliente solicita una mesa más amplia"));

        assertThat(sentada.estado()).isEqualTo(EstadoReserva.SENTADA);
        assertThat(sentada.mesa().id()).isEqualTo(mesaNuevaId);
        assertThat(mesaRepository.findById(scenario.mesaId()).orElseThrow().getEstado())
                .isEqualTo(Mesa.ESTADO_DISPONIBLE);
        assertThat(mesaRepository.findById(mesaNuevaId).orElseThrow().getEstado())
                .isEqualTo(Mesa.ESTADO_OCUPADA);

        var evento = historialRepository.findByReservaIdOrderByCreadoEnAsc(reserva.id()).getLast();
        assertThat(evento.getTipoEvento()).isEqualTo(TipoEventoReserva.CHECK_IN);
        assertThat(evento.getMesaAnterior().getId()).isEqualTo(scenario.mesaId());
        assertThat(evento.getMesaNueva().getId()).isEqualTo(mesaNuevaId);
        assertThat(evento.getMotivo()).isEqualTo("Cliente solicita una mesa más amplia");
        assertThat(evento.getCambiadoPor().getEmail()).isEqualTo("admin@example.com");
        assertThat(evento.getCreadoEn()).isNotNull();
    }

    @Test
    void rejectsInactiveInsufficientAndOverlappingReassignmentTargets() {
        Scenario scenario = scenario(20, 4);
        ReservaResponse reserva = reservaService.crearParaClienteActual(
                scenario.clientEmail(), request(scenario, 4));
        crearAdministrador();
        reservaService.cambiarEstado(reserva.id(), "admin@example.com",
                new ReservaEstadoUpdateRequest(EstadoReserva.CONFIRMADA, "Confirmada"));

        Long mesaPequenaId = mesaService.crear(
                new MesaCreateRequest(2, 2, scenario.zonaId())).id();
        assertThatThrownBy(() -> reservaService.reasignarMesa(
                reserva.id(),
                "admin@example.com",
                new ReservaReasignacionRequest(mesaPequenaId, "Cambio de mesa")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("admite como máximo 2");

        Long mesaInactivaId = mesaService.crear(
                new MesaCreateRequest(3, 4, scenario.zonaId())).id();
        mesaService.actualizar(
                mesaInactivaId,
                new MesaUpdateRequest(3, 4, false, scenario.zonaId()));
        assertThatThrownBy(() -> reservaService.reasignarMesa(
                reserva.id(),
                "admin@example.com",
                new ReservaReasignacionRequest(mesaInactivaId, "Cambio de mesa")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no están activas");

        Long mesaSolapadaId = mesaService.crear(
                new MesaCreateRequest(4, 4, scenario.zonaId())).id();
        authService.register(new RegisterRequest("Segundo cliente", "segundo@example.com", PASSWORD));
        reservaService.crearParaClienteActual(
                "segundo@example.com",
                new ReservaCreateRequest(
                        FECHA, scenario.turnoId(), mesaSolapadaId, 2, "Otra reserva"));

        assertThatThrownBy(() -> reservaService.reasignarMesa(
                reserva.id(),
                "admin@example.com",
                new ReservaReasignacionRequest(mesaSolapadaId, "Cambio de mesa")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("se solapa");
    }

    @Test
    void rejectsAReassignmentWithoutReasonAtTheServiceBoundary() {
        Scenario scenario = scenario(20, 4);
        Long mesaNuevaId = mesaService.crear(
                new MesaCreateRequest(2, 4, scenario.zonaId())).id();
        ReservaResponse reserva = reservaService.crearParaClienteActual(
                scenario.clientEmail(), request(scenario, 2));
        crearAdministrador();
        reservaService.cambiarEstado(reserva.id(), "admin@example.com",
                new ReservaEstadoUpdateRequest(EstadoReserva.CONFIRMADA, "Confirmada"));

        assertThatThrownBy(() -> reservaService.reasignarMesa(
                reserva.id(),
                "admin@example.com",
                new ReservaReasignacionRequest(mesaNuevaId, "   ")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("motivo de la reasignación es obligatorio");

        assertThat(reservaRepository.findById(reserva.id()).orElseThrow().getMesa().getId())
                .isEqualTo(scenario.mesaId());
        assertThat(historialRepository.findByReservaIdOrderByCreadoEnAsc(reserva.id()))
                .noneMatch(evento -> evento.getTipoEvento() == TipoEventoReserva.REASIGNACION);
    }

    @Test
    void reassignsASeatedReservationAndKeepsAnAppendOnlyAuditTrail() {
        Scenario scenario = scenario(20, 4);
        Long segundaMesaId = mesaService.crear(
                new MesaCreateRequest(2, 4, scenario.zonaId())).id();
        Long terceraMesaId = mesaService.crear(
                new MesaCreateRequest(3, 6, scenario.zonaId())).id();
        ReservaResponse reserva = reservaService.crearParaClienteActual(
                scenario.clientEmail(), request(scenario, 2));
        crearAdministrador();
        reservaService.cambiarEstado(reserva.id(), "admin@example.com",
                new ReservaEstadoUpdateRequest(EstadoReserva.CONFIRMADA, "Confirmada"));
        reservaService.realizarCheckIn(
                reserva.id(), "admin@example.com", new ReservaCheckInRequest(scenario.mesaId(), null));

        ReservaResponse primeraReasignacion = reservaService.reasignarMesa(
                reserva.id(),
                "admin@example.com",
                new ReservaReasignacionRequest(segundaMesaId, "Alejar de la puerta"));
        assertThat(primeraReasignacion.estado()).isEqualTo(EstadoReserva.SENTADA);
        assertThat(primeraReasignacion.mesa().id()).isEqualTo(segundaMesaId);
        assertThat(mesaRepository.findById(scenario.mesaId()).orElseThrow().getEstado())
                .isEqualTo(Mesa.ESTADO_DISPONIBLE);
        assertThat(mesaRepository.findById(segundaMesaId).orElseThrow().getEstado())
                .isEqualTo(Mesa.ESTADO_OCUPADA);

        reservaService.reasignarMesa(
                reserva.id(),
                "admin@example.com",
                new ReservaReasignacionRequest(terceraMesaId, "Unir al grupo familiar"));

        var reasignaciones = historialRepository.findByReservaIdOrderByCreadoEnAsc(reserva.id())
                .stream()
                .filter(evento -> evento.getTipoEvento() == TipoEventoReserva.REASIGNACION)
                .toList();
        assertThat(reasignaciones).hasSize(2);
        assertThat(reasignaciones.getFirst().getMesaAnterior().getId()).isEqualTo(scenario.mesaId());
        assertThat(reasignaciones.getFirst().getMesaNueva().getId()).isEqualTo(segundaMesaId);
        assertThat(reasignaciones.getFirst().getMotivo()).isEqualTo("Alejar de la puerta");
        assertThat(reasignaciones.getLast().getMesaAnterior().getId()).isEqualTo(segundaMesaId);
        assertThat(reasignaciones.getLast().getMesaNueva().getId()).isEqualTo(terceraMesaId);
    }

    @Test
    void calculatesAnOvernightReservationEndingTheNextDay() {
        authService.register(new RegisterRequest("Cliente", "cliente@example.com", PASSWORD));
        Long zonaId = zonaService.crear(new ZonaCreateRequest("Salón", null)).id();
        Long mesaId = mesaService.crear(new MesaCreateRequest(1, 4, zonaId)).id();
        Long turnoId = turnoService.crear(new TurnoCreateRequest(
                "Nocturno", LocalTime.of(22, 0), LocalTime.of(2, 0), 10)).id();

        ReservaResponse reserva = reservaService.crearParaClienteActual(
                "cliente@example.com",
                new ReservaCreateRequest(FECHA, turnoId, mesaId, 2, null));

        assertThat(reserva.inicio()).isEqualTo(FECHA.atTime(22, 0));
        assertThat(reserva.fin()).isEqualTo(FECHA.plusDays(1).atTime(2, 0));
    }

    private Scenario scenario(int capacidadTurno, int capacidadMesa) {
        String email = "cliente@example.com";
        authService.register(new RegisterRequest("Cliente", email, PASSWORD));
        Long zonaId = zonaService.crear(new ZonaCreateRequest("Salón", null)).id();
        Long mesaId = mesaService.crear(new MesaCreateRequest(1, capacidadMesa, zonaId)).id();
        Long turnoId = turnoService.crear(new TurnoCreateRequest(
                "Almuerzo", LocalTime.of(12, 0), LocalTime.of(15, 0), capacidadTurno)).id();
        return new Scenario(email, zonaId, mesaId, turnoId);
    }

    private ReservaCreateRequest request(Scenario scenario, int personas) {
        return new ReservaCreateRequest(
                FECHA, scenario.turnoId(), scenario.mesaId(), personas, "Sin alergias");
    }

    private void crearAdministrador() {
        usuarioService.crear(new UsuarioCreateRequest(
                "Administrador", "admin@example.com", PASSWORD, RolUsuario.ADMINISTRADOR));
    }

    private record Scenario(String clientEmail, Long zonaId, Long mesaId, Long turnoId) {
    }
}
