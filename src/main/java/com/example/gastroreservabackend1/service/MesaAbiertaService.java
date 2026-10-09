package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.pedido.PedidoDtos.*;
import com.example.gastroreservabackend1.exception.BusinessRuleException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.model.Mesa;
import com.example.gastroreservabackend1.model.MesaAbierta;
import com.example.gastroreservabackend1.model.Usuario;
import com.example.gastroreservabackend1.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import static com.example.gastroreservabackend1.model.RolUsuario.*;

@Service
@Validated
@Transactional(readOnly = true)
public class MesaAbiertaService {
    private final MesaAbiertaRepository aperturas;
    private final MesaRepository mesas;
    private final ReservaRepository reservas;
    private final PedidoRepository pedidos;
    private final ActorService actores;
    private final Clock clock;

    public MesaAbiertaService(MesaAbiertaRepository aperturas, MesaRepository mesas, ReservaRepository reservas,
                             PedidoRepository pedidos, ActorService actores, Clock clock) {
        this.aperturas = aperturas; this.mesas = mesas; this.reservas = reservas;
        this.pedidos = pedidos; this.actores = actores; this.clock = clock;
    }

    public List<MesaAbiertaRespuesta> listar(String email, Boolean abierta, Long mesaId) {
        actores.exigir(email, ADMINISTRADOR, HOST, MESERO);
        Specification<MesaAbierta> filtro = (root, q, cb) -> cb.conjunction();
        if (abierta != null) filtro = filtro.and((root, q, cb) -> abierta
                ? cb.isNotNull(root.get("mesaActivaId")) : cb.isNull(root.get("mesaActivaId")));
        if (mesaId != null) filtro = filtro.and((root, q, cb) -> cb.equal(root.get("mesa").get("id"), mesaId));
        return aperturas.findAll(filtro, Sort.by(Sort.Direction.DESC, "id")).stream().map(this::respuesta).toList();
    }

    public MesaAbiertaRespuesta buscar(Long id, String email) {
        actores.exigir(email, ADMINISTRADOR, HOST, MESERO);
        return respuesta(aperturas.findById(id).orElseThrow(() -> new ResourceNotFoundException("No existe la atención")));
    }

    @Transactional
    public MesaAbiertaRespuesta abrir(String email, @NotNull @Valid AbrirMesa request) {
        Usuario actor = actores.exigir(email, ADMINISTRADOR, HOST, MESERO);
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!request.finPrevisto().isAfter(ahora)) throw new BusinessRuleException("El fin previsto debe ser posterior a la apertura");
        Mesa mesa = mesas.findByIdForUpdate(request.mesaId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe la mesa"));
        if (!mesa.isActiva() || !mesa.getZona().isActiva() || !Mesa.ESTADO_DISPONIBLE.equals(mesa.getEstado()))
            throw new BusinessRuleException("La mesa debe estar activa y disponible");
        if (request.cantidadPersonas() > mesa.getCapacidad()) throw new BusinessRuleException("La mesa no tiene capacidad suficiente");
        if (reservas.existsSolapamiento(mesa.getId(), ahora, request.finPrevisto(), ReservaPolicy.ESTADOS_QUE_BLOQUEAN))
            throw new BusinessRuleException("La atención se solapa con una reserva existente");
        MesaAbierta apertura = new MesaAbierta();
        apertura.setMesa(mesa); apertura.setMesaActivaId(mesa.getId());
        apertura.setCantidadPersonas(request.cantidadPersonas()); apertura.setInicio(ahora);
        apertura.setFinPrevisto(request.finPrevisto()); apertura.setCreadaPor(actor);
        mesa.setEstado(Mesa.ESTADO_OCUPADA);
        return respuesta(aperturas.saveAndFlush(apertura));
    }

    @Transactional
    public MesaAbiertaRespuesta finalizar(Long id, String email) {
        Usuario actor = actores.exigir(email, ADMINISTRADOR, HOST, MESERO);
        MesaAbierta apertura = aperturas.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la atención"));
        if (!apertura.isAbierta()) throw new BusinessRuleException("La atención ya está finalizada");
        if (pedidos.existsByMesaAbiertaIdAndEstadoIn(id, PedidoService.ESTADOS_PENDIENTES))
            throw new BusinessRuleException("Primero debe cerrar o cancelar el pedido pendiente");
        Mesa mesa = mesas.findByIdForUpdate(apertura.getMesa().getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe la mesa"));
        apertura.setMesaActivaId(null); apertura.setCerradaPor(actor); apertura.setCerradaEn(Instant.now(clock));
        mesa.setEstado(Mesa.ESTADO_DISPONIBLE);
        return respuesta(aperturas.saveAndFlush(apertura));
    }

    private MesaAbiertaRespuesta respuesta(MesaAbierta a) {
        return new MesaAbiertaRespuesta(a.getId(), a.getMesa().getId(), a.getMesa().getNumero(),
                a.getCantidadPersonas(), a.getInicio(), a.getFinPrevisto(), a.isAbierta(),
                a.getCreadaPor().getId(), a.getCreadaEn(), a.getCerradaPor() == null ? null : a.getCerradaPor().getId(), a.getCerradaEn());
    }
}
