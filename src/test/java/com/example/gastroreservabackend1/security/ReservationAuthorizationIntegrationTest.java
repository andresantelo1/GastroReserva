package com.example.gastroreservabackend1.security;

import com.example.gastroreservabackend1.dto.auth.LoginRequest;
import com.example.gastroreservabackend1.dto.auth.RegisterRequest;
import com.example.gastroreservabackend1.dto.mesa.MesaCreateRequest;
import com.example.gastroreservabackend1.dto.turno.TurnoCreateRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioCreateRequest;
import com.example.gastroreservabackend1.dto.zona.ZonaCreateRequest;
import com.example.gastroreservabackend1.model.RolUsuario;
import com.example.gastroreservabackend1.repository.ClienteRepository;
import com.example.gastroreservabackend1.repository.HistorialReservaRepository;
import com.example.gastroreservabackend1.repository.MesaRepository;
import com.example.gastroreservabackend1.repository.ReservaRepository;
import com.example.gastroreservabackend1.repository.TurnoRepository;
import com.example.gastroreservabackend1.repository.UsuarioRepository;
import com.example.gastroreservabackend1.repository.ZonaRepository;
import com.example.gastroreservabackend1.service.AuthService;
import com.example.gastroreservabackend1.service.MesaService;
import com.example.gastroreservabackend1.service.TurnoService;
import com.example.gastroreservabackend1.service.UsuarioService;
import com.example.gastroreservabackend1.service.ZonaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationAuthorizationIntegrationTest {

    private static final String PASSWORD = "Password-segura-123";

    @Autowired
    private MockMvc mockMvc;

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
    void enforcesReservationPermissionsAcrossAllRoles() throws Exception {
        String clientEmail = "cliente@example.com";
        authService.register(new RegisterRequest("Cliente", clientEmail, PASSWORD));
        String clientToken = token(clientEmail);
        String adminToken = createOperationalUser("admin@example.com", RolUsuario.ADMINISTRADOR);
        String hostToken = createOperationalUser("host@example.com", RolUsuario.HOST);
        String waiterToken = createOperationalUser("mesero@example.com", RolUsuario.MESERO);

        Long zonaId = zonaService.crear(new ZonaCreateRequest("Salón", null)).id();
        Long mesaId = mesaService.crear(new MesaCreateRequest(1, 4, zonaId)).id();
        Long segundaMesaId = mesaService.crear(new MesaCreateRequest(2, 6, zonaId)).id();
        Long turnoId = turnoService.crear(new TurnoCreateRequest(
                "Almuerzo", LocalTime.of(12, 0), LocalTime.of(15, 0), 20)).id();
        Long clienteId = clienteRepository.findByUsuarioEmailIgnoreCase(clientEmail).orElseThrow().getId();

        String availabilityUrl = "/api/disponibilidad?fecha=2030-01-15&turnoId="
                + turnoId + "&cantidadPersonas=4";
        mockMvc.perform(get(availabilityUrl))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(availabilityUrl)
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mesas[0].id").value(mesaId));
        mockMvc.perform(get("/api/disponibilidad?fecha=fecha-invalida&turnoId="
                        + turnoId + "&cantidadPersonas=4")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_PARAMETER"));

        mockMvc.perform(post("/api/reservas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(turnoId, mesaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("SOLICITADA"))
                .andExpect(jsonPath("$.cliente.email").value(clientEmail));

        Long reservaId = reservaRepository.findAll().getFirst().getId();
        mockMvc.perform(get("/api/reservas/mias")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reservaId));
        mockMvc.perform(get("/api/reservas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reservas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(waiterToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reservaId));

        mockMvc.perform(patch("/api/reservas/" + reservaId + "/estado")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hostToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estado": "CONFIRMADA",
                                  "motivo": "Confirmada por recepción"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        mockMvc.perform(patch("/api/reservas/" + reservaId + "/cancelar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        mockMvc.perform(post("/api/reservas/operativas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(waiterToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operationalReservationBody(clienteId, turnoId, mesaId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/reservas/operativas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operationalReservationBody(clienteId, turnoId, mesaId)))
                .andExpect(status().isCreated());

        Long reservaCheckInId = reservaRepository.findAll().stream()
                .map(reserva -> reserva.getId())
                .max(Long::compareTo)
                .orElseThrow();
        mockMvc.perform(patch("/api/reservas/" + reservaCheckInId + "/estado")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hostToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estado": "CONFIRMADA",
                                  "motivo": "Confirmada para check-in"
                                }
                                """))
                .andExpect(status().isOk());

        String checkInBody = """
                {
                  "mesaId": %d,
                  "motivo": "Cliente recibido"
                }
                """.formatted(mesaId);
        mockMvc.perform(patch("/api/reservas/" + reservaCheckInId + "/check-in")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/reservas/" + reservaCheckInId + "/check-in")
                        .header(HttpHeaders.AUTHORIZATION, bearer(waiterToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/reservas/" + reservaCheckInId + "/check-in")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hostToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkInBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SENTADA"))
                .andExpect(jsonPath("$.mesa.id").value(mesaId));

        String reassignmentBody = """
                {
                  "mesaId": %d,
                  "motivo": "Unir al grupo familiar"
                }
                """.formatted(segundaMesaId);
        mockMvc.perform(patch("/api/reservas/" + reservaCheckInId + "/mesa")
                        .header(HttpHeaders.AUTHORIZATION, bearer(waiterToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reassignmentBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/reservas/" + reservaCheckInId + "/mesa")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reassignmentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SENTADA"))
                .andExpect(jsonPath("$.mesa.id").value(segundaMesaId));

        mockMvc.perform(get("/api/reservas/" + reservaCheckInId + "/historial")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].tipoEvento").value("CHECK_IN"))
                .andExpect(jsonPath("$[2].mesaAnterior.id").value(mesaId))
                .andExpect(jsonPath("$[2].mesaNueva.id").value(mesaId))
                .andExpect(jsonPath("$[3].tipoEvento").value("REASIGNACION"))
                .andExpect(jsonPath("$[3].mesaAnterior.id").value(mesaId))
                .andExpect(jsonPath("$[3].mesaNueva.id").value(segundaMesaId));

        mockMvc.perform(get("/api/reservas/" + reservaId + "/historial")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoNuevo").value("SOLICITADA"))
                .andExpect(jsonPath("$[1].estadoNuevo").value("CONFIRMADA"))
                .andExpect(jsonPath("$[2].estadoNuevo").value("CANCELADA"));
    }

    private String createOperationalUser(String email, RolUsuario rol) {
        usuarioService.crear(new UsuarioCreateRequest("Usuario " + rol, email, PASSWORD, rol));
        return token(email);
    }

    private String token(String email) {
        return authService.login(new LoginRequest(email, PASSWORD)).accessToken();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String reservationBody(Long turnoId, Long mesaId) {
        return """
                {
                  "fecha": "2030-01-15",
                  "turnoId": %d,
                  "mesaId": %d,
                  "cantidadPersonas": 4,
                  "observaciones": "Mesa tranquila"
                }
                """.formatted(turnoId, mesaId);
    }

    private String operationalReservationBody(Long clienteId, Long turnoId, Long mesaId) {
        return """
                {
                  "clienteId": %d,
                  "fecha": "2030-01-15",
                  "turnoId": %d,
                  "mesaId": %d,
                  "cantidadPersonas": 4,
                  "observaciones": "Reserva telefónica"
                }
                """.formatted(clienteId, turnoId, mesaId);
    }
}
