package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.auth.RegisterRequest;
import com.example.gastroreservabackend1.dto.cliente.ClienteCreateRequest;
import com.example.gastroreservabackend1.dto.cliente.ClienteResponse;
import com.example.gastroreservabackend1.dto.cliente.MiClienteUpdateRequest;
import com.example.gastroreservabackend1.dto.turno.TurnoCreateRequest;
import com.example.gastroreservabackend1.dto.turno.TurnoResponse;
import com.example.gastroreservabackend1.dto.turno.TurnoUpdateRequest;
import com.example.gastroreservabackend1.exception.BusinessRuleException;
import com.example.gastroreservabackend1.exception.ResourceConflictException;
import com.example.gastroreservabackend1.repository.ClienteRepository;
import com.example.gastroreservabackend1.repository.TurnoRepository;
import com.example.gastroreservabackend1.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ClienteTurnoServiceIntegrationTest {

    private static final String PASSWORD = "Password-segura-123";

    @Autowired
    private AuthService authService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private TurnoService turnoService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void cleanDatabase() {
        clienteRepository.deleteAll();
        turnoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void registrationCreatesALinkedBusinessProfile() {
        var usuario = authService.register(new RegisterRequest(
                "Cliente registrado", "CLIENTE@example.com", PASSWORD));

        ClienteResponse cliente = clienteService.buscarActual("cliente@example.com");

        assertThat(cliente.usuarioId()).isEqualTo(usuario.id());
        assertThat(cliente.nombre()).isEqualTo("Cliente registrado");
        assertThat(cliente.email()).isEqualTo("cliente@example.com");
        assertThat(cliente.activo()).isTrue();
    }

    @Test
    void registrationLinksAnExistingWalkInClientByEmail() {
        ClienteResponse existing = clienteService.crear(new ClienteCreateRequest(
                "Cliente presencial", "cliente@example.com", "70000000"));

        var usuario = authService.register(new RegisterRequest(
                "Nombre de la cuenta", "cliente@example.com", PASSWORD));
        ClienteResponse linked = clienteService.buscarPorId(existing.id());

        assertThat(linked.usuarioId()).isEqualTo(usuario.id());
        assertThat(linked.telefono()).isEqualTo("70000000");
        assertThat(clienteRepository.count()).isOne();
    }

    @Test
    void clientCanUpdateOwnProfileAndTheLinkedUserName() {
        authService.register(new RegisterRequest("Nombre inicial", "cliente@example.com", PASSWORD));

        ClienteResponse updated = clienteService.actualizarActual(
                "cliente@example.com", new MiClienteUpdateRequest("Nombre nuevo", "76543210"));

        assertThat(updated.nombre()).isEqualTo("Nombre nuevo");
        assertThat(updated.telefono()).isEqualTo("76543210");
        assertThat(usuarioRepository.findByEmailIgnoreCase("cliente@example.com").orElseThrow().getNombre())
                .isEqualTo("Nombre nuevo");
    }

    @Test
    void rejectsDuplicateClientEmail() {
        clienteService.crear(new ClienteCreateRequest("Uno", "cliente@example.com", null));

        assertThatThrownBy(() -> clienteService.crear(
                new ClienteCreateRequest("Dos", "CLIENTE@example.com", null)))
                .isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void createsFiltersAndUpdatesShifts() {
        TurnoResponse lunch = turnoService.crear(new TurnoCreateRequest(
                "Almuerzo", LocalTime.of(12, 0), LocalTime.of(15, 30), 30));
        turnoService.crear(new TurnoCreateRequest(
                "Cena", LocalTime.of(19, 0), LocalTime.of(23, 30), 40));

        assertThat(turnoService.listar(true, "muer"))
                .extracting(TurnoResponse::nombre)
                .containsExactly("Almuerzo");

        TurnoResponse updated = turnoService.actualizar(lunch.id(), new TurnoUpdateRequest(
                "Almuerzo principal", LocalTime.of(11, 30), LocalTime.of(16, 0), 35, false));
        assertThat(updated.capacidadMaxima()).isEqualTo(35);
        assertThat(updated.activo()).isFalse();
    }

    @Test
    void supportsOvernightShiftsButRejectsAnEmptyTimeWindow() {
        TurnoResponse overnight = turnoService.crear(new TurnoCreateRequest(
                "Madrugada", LocalTime.of(22, 0), LocalTime.of(2, 0), 20));
        assertThat(overnight.horaFin()).isBefore(overnight.horaInicio());

        assertThatThrownBy(() -> turnoService.crear(new TurnoCreateRequest(
                "Inválido", LocalTime.NOON, LocalTime.NOON, 10)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deben ser diferentes");
    }
}
