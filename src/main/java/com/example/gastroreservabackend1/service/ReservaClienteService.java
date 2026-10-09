package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.reserva.ReservaClienteUpdateRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaResponse;
import com.example.gastroreservabackend1.exception.*;
import com.example.gastroreservabackend1.model.*;
import com.example.gastroreservabackend1.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

@Service
@Validated
public class ReservaClienteService {
    private final ReservaRepository reservas;
    private final ClienteRepository clientes;
    private final HistorialReservaRepository historial;
    private final ActorService actores;
    private final ReservaService consultas;

    public ReservaClienteService(ReservaRepository reservas, ClienteRepository clientes,
                                 HistorialReservaRepository historial, ActorService actores,
                                 ReservaService consultas) {
        this.reservas = reservas; this.clientes = clientes; this.historial = historial;
        this.actores = actores; this.consultas = consultas;
    }

    @Transactional
    public ReservaResponse actualizar(@Positive Long id, String email, @Valid ReservaClienteUpdateRequest request) {
        Usuario actor = actores.exigir(email, RolUsuario.ADMINISTRADOR, RolUsuario.HOST);
        Reserva reserva = reservas.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva con id " + id));
        if (reserva.getEstado() != EstadoReserva.SOLICITADA) {
            throw new BusinessRuleException("Sólo una reserva SOLICITADA permite corregir cliente y observaciones");
        }
        Cliente nuevo = clientes.findByIdForUpdate(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el cliente con id " + request.clienteId()));
        if (!nuevo.isActivo()) throw new BusinessRuleException("El cliente seleccionado no está activo");
        String observaciones = normalizar(request.observaciones());
        boolean sinCambios = reserva.getCliente().getId().equals(nuevo.getId())
                && Objects.equals(reserva.getObservaciones(), observaciones);
        // Repetir una actualización ya aplicada no duplica eventos ni cambia la versión.
        if (sinCambios) return consultas.buscarPorId(id);
        if (!Objects.equals(reserva.getVersion(), request.version())) {
            throw new ResourceConflictException("La reserva cambió desde que se abrió. Cerrá la edición y volvé a consultar");
        }
        HistorialReserva evento = new HistorialReserva();
        evento.setReserva(reserva);
        evento.setTipoEvento(TipoEventoReserva.CORRECCION_CLIENTE);
        evento.setEstadoAnterior(reserva.getEstado());
        evento.setEstadoNuevo(reserva.getEstado());
        evento.setClienteAnterior(reserva.getCliente());
        evento.setClienteNuevo(nuevo);
        evento.setObservacionesAnteriores(reserva.getObservaciones());
        evento.setObservacionesNuevas(observaciones);
        evento.setMotivo(request.motivo().trim());
        evento.setCambiadoPor(actor);
        reserva.setCliente(nuevo);
        reserva.setObservaciones(observaciones);
        historial.save(evento);
        reservas.flush(); // El DTO devuelve la nueva versión, no la del formulario anterior.
        return consultas.buscarPorId(id);
    }

    private String normalizar(String text) { return text == null || text.isBlank() ? null : text.trim(); }
}
