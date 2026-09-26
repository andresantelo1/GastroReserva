package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.mesa.MesaCreateRequest;
import com.example.gastroreservabackend1.dto.mesa.MesaResponse;
import com.example.gastroreservabackend1.dto.zona.ZonaSummaryResponse;
import com.example.gastroreservabackend1.service.MesaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MesaController.class)
@AutoConfigureMockMvc(addFilters = false)
class MesaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MesaService mesaService;

    @Test
    void createsMesaUsingZonaIdInsteadOfJpaEntities() throws Exception {
        ZonaSummaryResponse zona = new ZonaSummaryResponse(1L, "Terraza", true);
        when(mesaService.crear(any(MesaCreateRequest.class)))
                .thenReturn(new MesaResponse(2L, 2, 4, "DISPONIBLE", true, zona));

        mockMvc.perform(post("/api/mesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numero": 2,
                                  "capacidad": 4,
                                  "zonaId": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/mesas/2"))
                .andExpect(jsonPath("$.numero").value(2))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.zona.id").value(1))
                .andExpect(jsonPath("$.zona.nombre").value("Terraza"));
    }

    @Test
    void rejectsMesaWithInvalidCapacity() throws Exception {
        mockMvc.perform(post("/api/mesas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "numero": 2,
                                  "capacidad": 0,
                                  "zonaId": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.capacidad")
                        .value("La capacidad debe ser mayor que cero"));
    }
}
