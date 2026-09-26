package com.example.gastroreservabackend1.config;

import com.example.gastroreservabackend1.dto.usuario.UsuarioCreateRequest;
import com.example.gastroreservabackend1.model.RolUsuario;
import com.example.gastroreservabackend1.repository.UsuarioRepository;
import com.example.gastroreservabackend1.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final String nombre;
    private final String email;
    private final String password;

    public AdminBootstrap(UsuarioRepository usuarioRepository,
                          UsuarioService usuarioService,
                          @Value("${app.bootstrap.admin.nombre:Administrador}") String nombre,
                          @Value("${app.bootstrap.admin.email:}") String email,
                          @Value("${app.bootstrap.admin.password:}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) && !StringUtils.hasText(password)) {
            return;
        }
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("ADMIN_EMAIL y ADMIN_PASSWORD deben configurarse juntos");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener entre 8 y 72 caracteres");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email.trim())) {
            return;
        }

        usuarioService.crear(new UsuarioCreateRequest(
                StringUtils.hasText(nombre) ? nombre : "Administrador",
                email,
                password,
                RolUsuario.ADMINISTRADOR
        ));
        LOGGER.info("Usuario administrador inicial creado para {}", email.trim().toLowerCase());
    }
}
