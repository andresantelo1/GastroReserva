package com.example.gastroreservabackend1.controller;

import com.example.gastroreservabackend1.dto.zona.ZonaCreateRequest;
import com.example.gastroreservabackend1.dto.zona.ZonaResponse;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.service.ZonaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ZonaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ZonaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZonaService zonaService;

    @Test
    void createsZonaUsingAnExplicitApiContract() throws Exception {
        when(zonaService.crear(any(ZonaCreateRequest.class)))
                .thenReturn(new ZonaResponse(1L, "Terraza", "Zona exterior", true));

        mockMvc.perform(post("/api/zonas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Terraza",
                                  "descripcion": "Zona exterior"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/zonas/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Terraza"))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void rejectsZonaWithoutNameWithFieldDetails() throws Exception {
        mockMvc.perform(post("/api/zonas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "  ",
                                  "descripcion": "Sin nombre"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.nombre")
                        .value("El nombre de la zona es obligatorio"));
    }

    @Test
    void reportsMissingZonaAsNotFound() throws Exception {
        when(zonaService.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("No existe la zona con id 99"));

        mockMvc.perform(get("/api/zonas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No existe la zona con id 99"));
    }
}
