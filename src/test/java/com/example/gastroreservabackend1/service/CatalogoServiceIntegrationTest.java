package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.mesa.MesaCreateRequest;
import com.example.gastroreservabackend1.dto.mesa.MesaResponse;
import com.example.gastroreservabackend1.dto.mesa.MesaUpdateRequest;
import com.example.gastroreservabackend1.dto.zona.ZonaCreateRequest;
import com.example.gastroreservabackend1.dto.zona.ZonaResponse;
import com.example.gastroreservabackend1.dto.zona.ZonaUpdateRequest;
import com.example.gastroreservabackend1.exception.ResourceConflictException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.repository.MesaRepository;
import com.example.gastroreservabackend1.repository.ZonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CatalogoServiceIntegrationTest {

    @Autowired
    private ZonaService zonaService;

    @Autowired
    private MesaService mesaService;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private ZonaRepository zonaRepository;

    @BeforeEach
    void cleanCatalogs() {
        mesaRepository.deleteAll();
        zonaRepository.deleteAll();
    }

    @Test
    void createsAndFiltersZones() {
        zonaService.crear(new ZonaCreateRequest("Salón principal", "Interior"));
        zonaService.crear(new ZonaCreateRequest("Terraza", "Exterior"));

        assertThat(zonaService.listar(true, "terra"))
                .extracting(ZonaResponse::nombre)
                .containsExactly("Terraza");
    }

    @Test
    void rejectsDuplicateZoneNamesIgnoringCase() {
        zonaService.crear(new ZonaCreateRequest("Terraza", null));

        assertThatThrownBy(() -> zonaService.crear(new ZonaCreateRequest(" terraza ", null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Ya existe una zona con el nombre 'terraza'");
    }

    @Test
    void createsAndFiltersTablesWithAZoneSummary() {
        ZonaResponse zona = zonaService.crear(new ZonaCreateRequest("Terraza", "Exterior"));
        MesaResponse created = mesaService.crear(new MesaCreateRequest(10, 6, zona.id()));

        assertThat(created.estado()).isEqualTo("DISPONIBLE");
        assertThat(created.zona().nombre()).isEqualTo("Terraza");
        assertThat(mesaService.listar(zona.id(), true, 4, "disponible"))
                .extracting(MesaResponse::numero)
                .containsExactly(10);
    }

    @Test
    void rejectsTableAssignedToMissingZone() {
        assertThatThrownBy(() -> mesaService.crear(new MesaCreateRequest(10, 4, 999L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe la zona con id 999");
    }

    @Test
    void updatesAdministrativeZoneAndTableData() {
        ZonaResponse terraza = zonaService.crear(new ZonaCreateRequest("Terraza", "Exterior"));
        ZonaResponse salon = zonaService.crear(new ZonaCreateRequest("Salón", "Interior"));
        MesaResponse mesa = mesaService.crear(new MesaCreateRequest(10, 4, terraza.id()));

        ZonaResponse zonaActualizada = zonaService.actualizar(
                terraza.id(), new ZonaUpdateRequest("Patio", "Exterior renovado", false));
        MesaResponse mesaActualizada = mesaService.actualizar(
                mesa.id(), new MesaUpdateRequest(11, 8, false, salon.id()));

        assertThat(zonaActualizada.nombre()).isEqualTo("Patio");
        assertThat(zonaActualizada.activa()).isFalse();
        assertThat(mesaActualizada.numero()).isEqualTo(11);
        assertThat(mesaActualizada.capacidad()).isEqualTo(8);
        assertThat(mesaActualizada.activa()).isFalse();
        assertThat(mesaActualizada.zona().id()).isEqualTo(salon.id());
    }
}
