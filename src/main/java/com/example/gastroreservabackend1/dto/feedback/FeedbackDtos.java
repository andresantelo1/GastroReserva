package com.example.gastroreservabackend1.dto.feedback;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class FeedbackDtos {
    private FeedbackDtos() {}
    public record Crear(@NotNull @Positive Long reservaId, @NotNull @Min(1) @Max(5) Integer puntuacion,
                        @NotBlank @Size(max = 1000) String comentario) {}
    public record Respuesta(Long id, Long reservaId, Long autorId, String autorNombre, Integer puntuacion,
                            String comentario, Instant creadoEn) {}
}
