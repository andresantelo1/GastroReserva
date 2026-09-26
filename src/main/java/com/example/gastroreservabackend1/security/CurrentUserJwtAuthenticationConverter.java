package com.example.gastroreservabackend1.security;

import com.example.gastroreservabackend1.model.Usuario;
import com.example.gastroreservabackend1.repository.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

public class CurrentUserJwtAuthenticationConverter
        implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserJwtAuthenticationConverter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
        Long userId;
        try {
            userId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new BadCredentialsException("El token no identifica un usuario válido", exception);
        }

        Usuario usuario = usuarioRepository.findById(userId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new BadCredentialsException("El usuario del token no está activo"));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name());
        return new JwtAuthenticationToken(jwt, List.of(authority), usuario.getEmail());
    }
}
