package com.example.gastroreservabackend1.dto.reporte;

import com.example.gastroreservabackend1.model.EstadoReserva;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class ReporteDtos {
    private ReporteDtos() {}
    public record OcupacionActual(long mesasActivas, long mesasOcupadas, BigDecimal porcentaje, Instant consultadoEn) {}
    public record TurnoDia(LocalDate fecha, Long turnoId, String turnoNombre, int capacidadActualTurno,
                           Map<EstadoReserva, Long> reservasPorEstado, long personasReservadasActivas,
                           BigDecimal porcentajeCapacidadReservada) {}
    public record Resumen(LocalDate desde, LocalDate hasta, long reservas, long atendidas, long noShow,
                          long canceladas, BigDecimal tasaNoShow, List<TurnoDia> porFechaTurno) {}
}
