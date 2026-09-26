package com.example.gastroreservabackend1.security;

import com.example.gastroreservabackend1.dto.auth.LoginRequest;
import com.example.gastroreservabackend1.dto.auth.LoginResponse;
import com.example.gastroreservabackend1.dto.auth.RegisterRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioCreateRequest;
import com.example.gastroreservabackend1.dto.usuario.UsuarioResponse;
import com.example.gastroreservabackend1.dto.usuario.UsuarioUpdateRequest;
import com.example.gastroreservabackend1.model.RolUsuario;
import com.example.gastroreservabackend1.repository.ClienteRepository;
import com.example.gastroreservabackend1.repository.MesaRepository;
import com.example.gastroreservabackend1.repository.TurnoRepository;
import com.example.gastroreservabackend1.repository.UsuarioRepository;
import com.example.gastroreservabackend1.repository.ZonaRepository;
import com.example.gastroreservabackend1.service.AuthService;
import com.example.gastroreservabackend1.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationAuthorizationIntegrationTest {

    private static final String PASSWORD = "Password-segura-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AuthService authService;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private ZonaRepository zonaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @BeforeEach
    void cleanDatabase() {
        mesaRepository.deleteAll();
        zonaRepository.deleteAll();
        clienteRepository.deleteAll();
        turnoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void registersClientsAndReturnsJwtAtLogin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Cliente prueba",
                                  "email": "cliente@example.com",
                                  "password": "Password-segura-123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "CLIENTE@example.com",
                                  "password": "Password-segura-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"));
    }

    @Test
    void rejectsInvalidCredentialsWithoutRevealingAccountState() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@example.com",
                                  "password": "Password-incorrecta"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"));
    }

    @Test
    void rejectsAnonymousRequestsWithAJson401() throws Exception {
        mockMvc.perform(get("/api/zonas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void appliesCatalogPermissionsForEveryOperationalRole() throws Exception {
        String adminToken = createUserAndLogin("admin@example.com", RolUsuario.ADMINISTRADOR).accessToken();
        String hostToken = createUserAndLogin("host@example.com", RolUsuario.HOST).accessToken();
        String waiterToken = createUserAndLogin("mesero@example.com", RolUsuario.MESERO).accessToken();
        String clientToken = createUserAndLogin("cliente@example.com", RolUsuario.CLIENTE).accessToken();

        mockMvc.perform(post("/api/zonas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Terraza",
                                  "descripcion": "Exterior"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/zonas").header(HttpHeaders.AUTHORIZATION, bearer(hostToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/zonas").header(HttpHeaders.AUTHORIZATION, bearer(waiterToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/zonas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hostToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Privada",
                                  "descripcion": "No autorizada"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/zonas").header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void onlyAdministratorsCanManageUsers() throws Exception {
        String adminToken = createUserAndLogin("admin@example.com", RolUsuario.ADMINISTRADOR).accessToken();
        String hostToken = createUserAndLogin("host@example.com", RolUsuario.HOST).accessToken();

        mockMvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());

        mockMvc.perform(get("/api/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(hostToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void resolvesPermissionsFromTheUsersCurrentRole() throws Exception {
        usuarioService.crear(new UsuarioCreateRequest(
                "Host temporal", "host@example.com", PASSWORD, RolUsuario.HOST));
        LoginResponse login = authService.login(new LoginRequest("host@example.com", PASSWORD));
        UsuarioResponse usuario = login.usuario();

        mockMvc.perform(get("/api/zonas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login.accessToken())))
                .andExpect(status().isOk());

        usuarioService.actualizar(usuario.id(), new UsuarioUpdateRequest(
                usuario.nombre(), usuario.email(), RolUsuario.CLIENTE, true));

        mockMvc.perform(get("/api/zonas")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login.accessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void protectsClientManagementAndAllowsTheClientToManageOwnProfile() throws Exception {
        String adminToken = createUserAndLogin("admin@example.com", RolUsuario.ADMINISTRADOR).accessToken();
        String hostToken = createUserAndLogin("host@example.com", RolUsuario.HOST).accessToken();
        authService.register(new RegisterRequest("Cliente perfil", "cliente@example.com", PASSWORD));
        String clientToken = authService.login(new LoginRequest("cliente@example.com", PASSWORD)).accessToken();

        mockMvc.perform(get("/api/clientes/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente@example.com"))
                .andExpect(jsonPath("$.usuarioId").isNumber());

        mockMvc.perform(put("/api/clientes/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Cliente actualizado",
                                  "telefono": "+591 70000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cliente actualizado"))
                .andExpect(jsonPath("$.telefono").value("+591 70000000"));

        mockMvc.perform(get("/api/clientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());

        mockMvc.perform(post("/api/clientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Cliente presencial",
                                  "email": "presencial@example.com",
                                  "telefono": "70000001"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").doesNotExist());

        mockMvc.perform(get("/api/clientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void appliesShiftPermissionsForOperationalRoles() throws Exception {
        String adminToken = createUserAndLogin("admin@example.com", RolUsuario.ADMINISTRADOR).accessToken();
        String hostToken = createUserAndLogin("host@example.com", RolUsuario.HOST).accessToken();
        String waiterToken = createUserAndLogin("mesero@example.com", RolUsuario.MESERO).accessToken();
        String clientToken = createUserAndLogin("cliente@example.com", RolUsuario.CLIENTE).accessToken();

        mockMvc.perform(post("/api/turnos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Cena",
                                  "horaInicio": "19:00:00",
                                  "horaFin": "23:30:00",
                                  "capacidadMaxima": 40
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(get("/api/turnos?activo=true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hostToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Cena"));
        mockMvc.perform(get("/api/turnos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(waiterToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/turnos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(hostToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Almuerzo",
                                  "horaInicio": "12:00:00",
                                  "horaFin": "15:00:00",
                                  "capacidadMaxima": 30
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/turnos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private LoginResponse createUserAndLogin(String email, RolUsuario rol) {
        usuarioService.crear(new UsuarioCreateRequest("Usuario " + rol, email, PASSWORD, rol));
        return authService.login(new LoginRequest(email, PASSWORD));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
