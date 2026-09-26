package com.example.gastroreservabackend1.dto.mesa;

import com.example.gastroreservabackend1.dto.zona.ZonaSummaryResponse;

public record MesaResponse(
        Long id,
        Integer numero,
        Integer capacidad,
        String estado,
        boolean activa,
        ZonaSummaryResponse zona
) {
}
