package com.example.gastroreservabackend1.dto.usuario;

import com.example.gastroreservabackend1.model.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 255, message = "El correo no puede superar 255 caracteres")
        String email,

        @NotNull(message = "El rol es obligatorio")
        RolUsuario rol,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}
