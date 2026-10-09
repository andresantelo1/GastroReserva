package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.reserva.ClienteReservaSummaryResponse;
import com.example.gastroreservabackend1.dto.reserva.HistorialReservaResponse;
import com.example.gastroreservabackend1.dto.reserva.MesaReservaSummaryResponse;
import com.example.gastroreservabackend1.dto.reserva.ReservaCheckInRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaCreateRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaEstadoUpdateRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaOperativaCreateRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaReasignacionRequest;
import com.example.gastroreservabackend1.dto.reserva.ReservaResponse;
import com.example.gastroreservabackend1.dto.reserva.TurnoReservaSummaryResponse;
import com.example.gastroreservabackend1.dto.zona.ZonaSummaryResponse;
import com.example.gastroreservabackend1.exception.BusinessRuleException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.model.Cliente;
import com.example.gastroreservabackend1.model.EstadoReserva;
import com.example.gastroreservabackend1.model.HistorialReserva;
import com.example.gastroreservabackend1.model.Mesa;
import com.example.gastroreservabackend1.model.Reserva;
import com.example.gastroreservabackend1.model.TipoEventoReserva;
import com.example.gastroreservabackend1.model.Turno;
import com.example.gastroreservabackend1.model.Usuario;
import com.example.gastroreservabackend1.repository.ClienteRepository;
import com.example.gastroreservabackend1.repository.HistorialReservaRepository;
import com.example.gastroreservabackend1.repository.MesaRepository;
import com.example.gastroreservabackend1.repository.ReservaRepository;
import com.example.gastroreservabackend1.repository.TurnoRepository;
import com.example.gastroreservabackend1.repository.UsuarioRepository;
import com.example.gastroreservabackend1.repository.PedidoRepository;
import com.example.gastroreservabackend1.repository.MesaAbiertaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final HistorialReservaRepository historialRepository;
    private final ClienteRepository clienteRepository;
    private final TurnoRepository turnoRepository;
    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final MesaAbiertaRepository mesaAbiertaRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          HistorialReservaRepository historialRepository,
                          ClienteRepository clienteRepository,
                          TurnoRepository turnoRepository,
                          MesaRepository mesaRepository,
                          UsuarioRepository usuarioRepository,
                          PedidoRepository pedidoRepository,
                          MesaAbiertaRepository mesaAbiertaRepository) {
        this.reservaRepository = reservaRepository;
        this.historialRepository = historialRepository;
        this.clienteRepository = clienteRepository;
        this.turnoRepository = turnoRepository;
        this.mesaRepository = mesaRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.mesaAbiertaRepository = mesaAbiertaRepository;
    }

    public List<ReservaResponse> listar(LocalDate fechaDesde,
                                         LocalDate fechaHasta,
                                         EstadoReserva estado,
                                         Long clienteId,
                                         Long turnoId,
                                         Long mesaId) {
        validarRangoFechas(fechaDesde, fechaHasta);
        return reservaRepository.findAll(
                        crearFiltros(fechaDesde, fechaHasta, estado, clienteId, turnoId, mesaId),
                        Sort.by(Sort.Direction.ASC, "inicio").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReservaResponse> listarMias(String userEmail,
                                             LocalDate fechaDesde,
                                             LocalDate fechaHasta,
                                             EstadoReserva estado) {
        validarRangoFechas(fechaDesde, fechaHasta);
        Cliente cliente = requireClienteActual(userEmail);
        return reservaRepository.findAll(
                        crearFiltros(fechaDesde, fechaHasta, estado, cliente.getId(), null, null),
                        Sort.by(Sort.Direction.ASC, "inicio").and(Sort.by(Sort.Direction.ASC, "id")))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ReservaResponse buscarPorId(Long id) {
        return toResponse(requireReserva(id));
    }

    public List<HistorialReservaResponse> listarHistorial(Long reservaId) {
        requireReserva(reservaId);
        return historialRepository.findByReservaIdOrderByCreadoEnAsc(reservaId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional
    public ReservaResponse crearParaClienteActual(String userEmail, ReservaCreateRequest request) {
        Usuario actor = requireUsuario(userEmail);
        Cliente cliente = requireClienteActual(userEmail);
        return crearInterno(
                actor,
                cliente,
                request.fecha(),
                request.turnoId(),
                request.mesaId(),
                request.cantidadPersonas(),
                request.observaciones());
    }

    @Transactional
    public ReservaResponse crearOperativa(String userEmail, ReservaOperativaCreateRequest request) {
        Usuario actor = requireUsuario(userEmail);
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el cliente con id " + request.clienteId()));
        return crearInterno(
                actor,
                cliente,
                request.fecha(),
                request.turnoId(),
                request.mesaId(),
                request.cantidadPersonas(),
                request.observaciones());
    }

    @Transactional
    public ReservaResponse cancelarActual(Long id, String userEmail) {
        Usuario actor = requireUsuario(userEmail);
        Reserva reserva = requireReservaForUpdate(id);
        if (reserva.getCliente().getUsuario() == null
                || !reserva.getCliente().getUsuario().getEmail().equalsIgnoreCase(normalizarEmail(userEmail))) {
            throw new ResourceNotFoundException("No existe una reserva propia con id " + id);
        }
        if (reserva.getEstado() != EstadoReserva.SOLICITADA
                && reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new BusinessRuleException(
                    "El cliente sólo puede cancelar reservas solicitadas o confirmadas");
        }
        cambiarEstado(reserva, EstadoReserva.CANCELADA, "Cancelada por el cliente", actor);
        return toResponse(reserva);
    }

    @Transactional
    public ReservaResponse cambiarEstado(Long id, String userEmail, ReservaEstadoUpdateRequest request) {
        Usuario actor = requireUsuario(userEmail);
        Reserva reserva = requireReservaForUpdate(id);
        if (request.estado() == EstadoReserva.SENTADA) {
            throw new BusinessRuleException(
                    "Para sentar una reserva se debe utilizar la operación de check-in");
        }
        if (!ReservaPolicy.permite(reserva.getEstado(), request.estado())) {
            throw new BusinessRuleException(
                    "No se permite cambiar una reserva de " + reserva.getEstado() + " a " + request.estado());
        }
        if (reserva.getEstado() == EstadoReserva.SENTADA
                && request.estado() == EstadoReserva.FINALIZADA) {
            if (pedidoRepository.existsByReservaIdAndEstadoIn(id, PedidoService.ESTADOS_PENDIENTES)) {
                throw new BusinessRuleException("Primero debe cerrar o cancelar el pedido pendiente");
            }
            Mesa mesa = mesaRepository.findByIdForUpdate(reserva.getMesa().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No existe la mesa con id " + reserva.getMesa().getId()));
            mesa.setEstado(Mesa.ESTADO_DISPONIBLE);
        }
        cambiarEstado(reserva, request.estado(), normalizarTexto(request.motivo()), actor);
        return toResponse(reserva);
    }

    @Transactional
    public ReservaResponse realizarCheckIn(Long id,
                                           String userEmail,
                                           ReservaCheckInRequest request) {
        Usuario actor = requireUsuario(userEmail);
        Reserva reserva = requireReservaForUpdate(id);
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new BusinessRuleException(
                    "Sólo una reserva confirmada puede realizar check-in");
        }

        MesasBloqueadas mesas = bloquearMesas(reserva.getMesa(), request.mesaId());
        Mesa mesaAnterior = mesas.anterior();
        Mesa mesaNueva = mesas.nueva();
        validarMesaDestino(reserva, mesaNueva);

        boolean cambiaMesa = !mesaAnterior.getId().equals(mesaNueva.getId());
        String motivo = normalizarTexto(request.motivo());
        if (cambiaMesa && motivo == null) {
            throw new BusinessRuleException(
                    "El motivo es obligatorio cuando el check-in cambia la mesa asignada");
        }

        reserva.setMesa(mesaNueva);
        reserva.setEstado(EstadoReserva.SENTADA);
        mesaNueva.setEstado(Mesa.ESTADO_OCUPADA);
        registrarHistorial(
                reserva,
                EstadoReserva.CONFIRMADA,
                EstadoReserva.SENTADA,
                motivo != null ? motivo : "Check-in realizado",
                actor,
                TipoEventoReserva.CHECK_IN,
                mesaAnterior,
                mesaNueva);
        return toResponse(reserva);
    }

    @Transactional
    public ReservaResponse reasignarMesa(Long id,
                                         String userEmail,
                                         ReservaReasignacionRequest request) {
        Usuario actor = requireUsuario(userEmail);
        Reserva reserva = requireReservaForUpdate(id);
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA
                && reserva.getEstado() != EstadoReserva.SENTADA) {
            throw new BusinessRuleException(
                    "Sólo una reserva confirmada o sentada puede cambiar de mesa");
        }
        if (reserva.getMesa().getId().equals(request.mesaId())) {
            throw new BusinessRuleException("La mesa nueva debe ser diferente de la mesa actual");
        }
        String motivo = normalizarTexto(request.motivo());
        if (motivo == null) {
            throw new BusinessRuleException("El motivo de la reasignación es obligatorio");
        }

        MesasBloqueadas mesas = bloquearMesas(reserva.getMesa(), request.mesaId());
        Mesa mesaAnterior = mesas.anterior();
        Mesa mesaNueva = mesas.nueva();
        validarMesaDestino(reserva, mesaNueva);

        EstadoReserva estadoActual = reserva.getEstado();
        if (estadoActual == EstadoReserva.SENTADA) {
            mesaAnterior.setEstado(Mesa.ESTADO_DISPONIBLE);
            mesaNueva.setEstado(Mesa.ESTADO_OCUPADA);
        }
        reserva.setMesa(mesaNueva);
        registrarHistorial(
                reserva,
                estadoActual,
                estadoActual,
                motivo,
                actor,
                TipoEventoReserva.REASIGNACION,
                mesaAnterior,
                mesaNueva);
        return toResponse(reserva);
    }

    private ReservaResponse crearInterno(Usuario actor,
                                          Cliente cliente,
                                          LocalDate fecha,
                                          Long turnoId,
                                          Long mesaId,
                                          Integer cantidadPersonas,
                                          String observaciones) {
        if (!cliente.isActivo()) {
            throw new BusinessRuleException("El cliente seleccionado no está activo");
        }

        Turno turno = turnoRepository.findByIdForUpdate(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el turno con id " + turnoId));
        Mesa mesa = mesaRepository.findByIdForUpdate(mesaId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la mesa con id " + mesaId));

        validarRecursosActivos(turno, mesa);
        if (cantidadPersonas > mesa.getCapacidad()) {
            throw new BusinessRuleException(
                    "La mesa " + mesa.getNumero() + " admite como máximo " + mesa.getCapacidad() + " personas");
        }

        long capacidadReservada = reservaRepository.sumPersonasActivas(
                turnoId, fecha, ReservaPolicy.ESTADOS_QUE_BLOQUEAN);
        if (capacidadReservada + cantidadPersonas > turno.getCapacidadMaxima()) {
            throw new BusinessRuleException("El turno no tiene capacidad suficiente para la reserva");
        }

        ReservaPolicy.Intervalo intervalo = ReservaPolicy.calcularIntervalo(fecha, turno);
        if (mesaAbiertaRepository.existsSolapamiento(mesaId, intervalo.inicio(), intervalo.fin())) {
            throw new BusinessRuleException("La mesa tiene una atención sin reserva en ese intervalo");
        }
        if (reservaRepository.existsSolapamiento(
                mesaId,
                intervalo.inicio(),
                intervalo.fin(),
                ReservaPolicy.ESTADOS_QUE_BLOQUEAN)) {
            throw new BusinessRuleException("La mesa ya tiene una reserva que se solapa con el turno solicitado");
        }

        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setTurno(turno);
        reserva.setMesa(mesa);
        reserva.setFecha(fecha);
        reserva.setInicio(intervalo.inicio());
        reserva.setFin(intervalo.fin());
        reserva.setCantidadPersonas(cantidadPersonas);
        reserva.setEstado(EstadoReserva.SOLICITADA);
        reserva.setObservaciones(normalizarTexto(observaciones));
        reserva.setCreadoPor(actor);
        reservaRepository.save(reserva);
        registrarHistorial(
                reserva,
                null,
                EstadoReserva.SOLICITADA,
                "Reserva creada",
                actor,
                TipoEventoReserva.CREACION,
                null,
                mesa);
        return toResponse(reserva);
    }

    private void validarRecursosActivos(Turno turno, Mesa mesa) {
        if (!turno.isActivo()) {
            throw new BusinessRuleException("El turno seleccionado no está activo");
        }
        if (!mesa.isActiva() || !mesa.getZona().isActiva()) {
            throw new BusinessRuleException("La mesa o su zona no están activas");
        }
    }

    private void cambiarEstado(Reserva reserva,
                                EstadoReserva nuevoEstado,
                                String motivo,
                                Usuario actor) {
        EstadoReserva anterior = reserva.getEstado();
        reserva.setEstado(nuevoEstado);
        registrarHistorial(
                reserva,
                anterior,
                nuevoEstado,
                motivo,
                actor,
                TipoEventoReserva.CAMBIO_ESTADO,
                null,
                null);
    }

    private void registrarHistorial(Reserva reserva,
                                     EstadoReserva anterior,
                                     EstadoReserva nuevo,
                                     String motivo,
                                     Usuario actor,
                                     TipoEventoReserva tipoEvento,
                                     Mesa mesaAnterior,
                                     Mesa mesaNueva) {
        HistorialReserva historial = new HistorialReserva();
        historial.setReserva(reserva);
        historial.setTipoEvento(tipoEvento);
        historial.setEstadoAnterior(anterior);
        historial.setEstadoNuevo(nuevo);
        historial.setMotivo(motivo);
        historial.setCambiadoPor(actor);
        historial.setMesaAnterior(mesaAnterior);
        historial.setMesaNueva(mesaNueva);
        historialRepository.save(historial);
    }

    private MesasBloqueadas bloquearMesas(Mesa mesaActual, Long mesaNuevaId) {
        Long mesaAnteriorId = mesaActual.getId();
        if (mesaAnteriorId.equals(mesaNuevaId)) {
            Mesa mesa = requireMesaForUpdate(mesaAnteriorId);
            return new MesasBloqueadas(mesa, mesa);
        }

        Mesa anterior;
        Mesa nueva;
        if (mesaAnteriorId < mesaNuevaId) {
            anterior = requireMesaForUpdate(mesaAnteriorId);
            nueva = requireMesaForUpdate(mesaNuevaId);
        } else {
            nueva = requireMesaForUpdate(mesaNuevaId);
            anterior = requireMesaForUpdate(mesaAnteriorId);
        }
        return new MesasBloqueadas(anterior, nueva);
    }

    private Mesa requireMesaForUpdate(Long id) {
        return mesaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la mesa con id " + id));
    }

    private void validarMesaDestino(Reserva reserva, Mesa mesa) {
        if (mesaAbiertaRepository.existsSolapamiento(mesa.getId(), reserva.getInicio(), reserva.getFin())) {
            throw new BusinessRuleException("La mesa nueva tiene una atención sin reserva en ese intervalo");
        }
        if (!mesa.isActiva() || !mesa.getZona().isActiva()) {
            throw new BusinessRuleException("La mesa nueva o su zona no están activas");
        }
        if (reserva.getCantidadPersonas() > mesa.getCapacidad()) {
            throw new BusinessRuleException(
                    "La mesa " + mesa.getNumero() + " admite como máximo "
                            + mesa.getCapacidad() + " personas");
        }
        if (!Mesa.ESTADO_DISPONIBLE.equals(mesa.getEstado())) {
            throw new BusinessRuleException("La mesa nueva no está disponible operativamente");
        }
        if (reservaRepository.existsSolapamientoExcluyendoReserva(
                mesa.getId(),
                reserva.getId(),
                reserva.getInicio(),
                reserva.getFin(),
                ReservaPolicy.ESTADOS_QUE_BLOQUEAN)) {
            throw new BusinessRuleException(
                    "La mesa nueva tiene una reserva que se solapa con el intervalo solicitado");
        }
    }

    private Specification<Reserva> crearFiltros(LocalDate fechaDesde,
                                                  LocalDate fechaHasta,
                                                  EstadoReserva estado,
                                                  Long clienteId,
                                                  Long turnoId,
                                                  Long mesaId) {
        Specification<Reserva> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();
        if (fechaDesde != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
        }
        if (fechaHasta != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
        }
        if (estado != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("estado"), estado));
        }
        if (clienteId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("cliente").get("id"), clienteId));
        }
        if (turnoId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("turno").get("id"), turnoId));
        }
        if (mesaId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("mesa").get("id"), mesaId));
        }
        return specification;
    }

    private void validarRangoFechas(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new BusinessRuleException("La fecha inicial no puede ser posterior a la fecha final");
        }
    }

    private Reserva requireReserva(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva con id " + id));
    }

    private Reserva requireReservaForUpdate(Long id) {
        return reservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva con id " + id));
    }

    private Cliente requireClienteActual(String userEmail) {
        return clienteRepository.findByUsuarioEmailIgnoreCase(normalizarEmail(userEmail))
                .filter(Cliente::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario no tiene un perfil de cliente activo"));
    }

    private Usuario requireUsuario(String userEmail) {
        return usuarioRepository.findByEmailIgnoreCase(normalizarEmail(userEmail))
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario autenticado activo"));
    }

    private ReservaResponse toResponse(Reserva reserva) {
        Cliente cliente = reserva.getCliente();
        Turno turno = reserva.getTurno();
        Mesa mesa = reserva.getMesa();
        return new ReservaResponse(
                reserva.getId(),
                new ClienteReservaSummaryResponse(
                        cliente.getId(), cliente.getNombre(), cliente.getEmail(), cliente.getTelefono()),
                new TurnoReservaSummaryResponse(
                        turno.getId(), turno.getNombre(), turno.getHoraInicio(), turno.getHoraFin(),
                        turno.getCapacidadMaxima()),
                new MesaReservaSummaryResponse(
                        mesa.getId(),
                        mesa.getNumero(),
                        mesa.getCapacidad(),
                        new ZonaSummaryResponse(
                                mesa.getZona().getId(), mesa.getZona().getNombre(), mesa.getZona().isActiva())),
                reserva.getFecha(),
                reserva.getInicio(),
                reserva.getFin(),
                reserva.getCantidadPersonas(),
                reserva.getEstado(),
                reserva.getObservaciones(),
                reserva.getCreadoPor().getId(),
                reserva.getCreadoEn(),
                reserva.getActualizadoEn(),
                reserva.getVersion()
        );
    }

    private HistorialReservaResponse toHistoryResponse(HistorialReserva historial) {
        return new HistorialReservaResponse(
                historial.getId(),
                historial.getTipoEvento(),
                historial.getEstadoAnterior(),
                historial.getEstadoNuevo(),
                historial.getMotivo(),
                toMesaSummary(historial.getMesaAnterior()),
                toMesaSummary(historial.getMesaNueva()),
                historial.getCambiadoPor().getId(),
                historial.getCambiadoPor().getNombre(),
                historial.getCreadoEn(),
                historial.getClienteAnterior() == null ? null : historial.getClienteAnterior().getId(),
                historial.getClienteNuevo() == null ? null : historial.getClienteNuevo().getId(),
                historial.getObservacionesAnteriores(),
                historial.getObservacionesNuevas()
        );
    }

    private MesaReservaSummaryResponse toMesaSummary(Mesa mesa) {
        if (mesa == null) {
            return null;
        }
        return new MesaReservaSummaryResponse(
                mesa.getId(),
                mesa.getNumero(),
                mesa.getCapacidad(),
                new ZonaSummaryResponse(
                        mesa.getZona().getId(), mesa.getZona().getNombre(), mesa.getZona().isActiva()));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarTexto(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private record MesasBloqueadas(Mesa anterior, Mesa nueva) {
    }
}
