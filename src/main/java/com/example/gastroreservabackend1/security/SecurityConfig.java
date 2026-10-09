package com.example.gastroreservabackend1.security;

import com.example.gastroreservabackend1.repository.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            RestAuthenticationEntryPoint authenticationEntryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler,
                                            CurrentUserJwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/health", "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/reportes/**").hasAnyRole("ADMINISTRADOR", "HOST")
                        .requestMatchers("/api/reportes/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/notificaciones").hasRole("CLIENTE")
                        .requestMatchers("/api/notificaciones/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/feedback").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/feedback/mios").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/feedback").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/feedback/*").hasAnyRole("ADMINISTRADOR", "CLIENTE")
                        .requestMatchers("/api/feedback/**").denyAll()
                        .requestMatchers("/api/mesas-abiertas/**").hasAnyRole("ADMINISTRADOR", "HOST", "MESERO")
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/**").hasAnyRole("ADMINISTRADOR", "HOST", "MESERO")
                        .requestMatchers(HttpMethod.PATCH, "/api/pedidos/*/responsable").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/pedidos/**").hasAnyRole("ADMINISTRADOR", "MESERO")
                        .requestMatchers("/api/productos-menu/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/carta/**")
                        .hasAnyRole("ADMINISTRADOR", "HOST", "MESERO", "CLIENTE")
                        .requestMatchers("/api/carta/**").denyAll()
                        .requestMatchers("/api/clientes/me", "/api/clientes/me/**").hasRole("CLIENTE")
                        .requestMatchers("/api/clientes/**").hasAnyRole("ADMINISTRADOR", "HOST")
                        .requestMatchers("/api/disponibilidad/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/reservas/mias").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/reservas").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.PATCH, "/api/reservas/*/cancelar").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/reservas/operativas")
                        .hasAnyRole("ADMINISTRADOR", "HOST")
                        .requestMatchers(HttpMethod.PUT, "/api/reservas/*/datos-cliente")
                        .hasAnyRole("ADMINISTRADOR", "HOST")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/**")
                        .hasAnyRole("ADMINISTRADOR", "HOST", "MESERO")
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/reservas/*/check-in", "/api/reservas/*/mesa")
                        .hasAnyRole("ADMINISTRADOR", "HOST")
                        .requestMatchers(HttpMethod.PATCH, "/api/reservas/*/estado")
                        .hasAnyRole("ADMINISTRADOR", "HOST", "MESERO")
                        .requestMatchers("/api/reservas/**").hasAnyRole("ADMINISTRADOR", "HOST")
                        .requestMatchers(HttpMethod.GET, "/api/turnos/**")
                        .hasAnyRole("ADMINISTRADOR", "HOST", "MESERO")
                        .requestMatchers("/api/turnos/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/zonas/**", "/api/mesas/**")
                        .hasAnyRole("ADMINISTRADOR", "HOST", "MESERO")
                        .requestMatchers("/api/zonas/**", "/api/mesas/**").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey jwtSecretKey(JwtProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey) {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    CurrentUserJwtAuthenticationConverter currentUserJwtAuthenticationConverter(
            UsuarioRepository usuarioRepository) {
        return new CurrentUserJwtAuthenticationConverter(usuarioRepository);
    }

}
