package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.pedido.PedidoDtos.*;
import com.example.gastroreservabackend1.exception.BusinessRuleException;
import com.example.gastroreservabackend1.exception.ResourceConflictException;
import com.example.gastroreservabackend1.exception.ResourceNotFoundException;
import com.example.gastroreservabackend1.model.*;
import com.example.gastroreservabackend1.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.example.gastroreservabackend1.model.RolUsuario.*;

@Service
@Validated
@Transactional(readOnly = true)
public class PedidoService {
    public static final List<EstadoPedido> ESTADOS_PENDIENTES =
            List.of(EstadoPedido.ABIERTO, EstadoPedido.EN_PREPARACION, EstadoPedido.SERVIDO);

    private final PedidoRepository pedidos;
    private final ItemPedidoRepository items;
    private final HistorialPedidoRepository historial;
    private final ReservaRepository reservas;
    private final MesaAbiertaRepository aperturas;
    private final ProductoMenuRepository productos;
    private final UsuarioRepository usuarios;
    private final ActorService actores;

    public PedidoService(PedidoRepository pedidos, ItemPedidoRepository items, HistorialPedidoRepository historial,
                         ReservaRepository reservas, MesaAbiertaRepository aperturas, ProductoMenuRepository productos,
                         UsuarioRepository usuarios, ActorService actores) {
        this.pedidos = pedidos; this.items = items; this.historial = historial;
        this.reservas = reservas; this.aperturas = aperturas; this.productos = productos;
        this.usuarios = usuarios; this.actores = actores;
    }

    public List<Respuesta> listar(String email, EstadoPedido estado, Long mesaId, Long reservaId, boolean mios) {
        Usuario actor = actores.exigir(email, ADMINISTRADOR, HOST, MESERO);
        Specification<Pedido> filtro = (root, q, cb) -> cb.conjunction();
        if (estado != null) filtro = filtro.and((root, q, cb) -> cb.equal(root.get("estado"), estado));
        if (reservaId != null) filtro = filtro.and((root, q, cb) -> cb.equal(root.get("reserva").get("id"), reservaId));
        if (mesaId != null) filtro = filtro.and((root, q, cb) -> cb.or(
                cb.equal(root.join("reserva", jakarta.persistence.criteria.JoinType.LEFT).get("mesa").get("id"), mesaId),
                cb.equal(root.join("mesaAbierta", jakarta.persistence.criteria.JoinType.LEFT).get("mesa").get("id"), mesaId)));
        if (mios) filtro = filtro.and((root, q, cb) -> cb.equal(root.get("responsable").get("id"), actor.getId()));
        return pedidos.findAll(filtro, Sort.by(Sort.Direction.DESC, "id")).stream().map(this::respuesta).toList();
    }

    public Respuesta buscar(Long id, String email) {
        actores.exigir(email, ADMINISTRADOR, HOST, MESERO);
        return respuesta(requirePedido(id));
    }

    public List<Historial> historial(Long id, String email) {
        actores.exigir(email, ADMINISTRADOR, HOST, MESERO);
        requirePedido(id);
        return historial.findByPedidoIdOrderByIdAsc(id).stream().map(h -> new Historial(
                h.getId(), h.getEstadoAnterior(), h.getEstadoNuevo(), h.getDetalle(),
                h.getCambiadoPor().getId(), h.getCambiadoPor().getNombre(), h.getCreadoEn())).toList();
    }

    @Transactional
    public Respuesta crear(String email, @NotNull @Valid Crear request) {
        Usuario actor = actores.exigir(email, ADMINISTRADOR, MESERO);
        Pedido pedido = new Pedido();
        if (request.reservaId() != null) {
            Reserva reserva = bloquearReserva(request.reservaId());
            if (reserva.getEstado() != EstadoReserva.SENTADA) throw regla("La reserva debe estar SENTADA para recibir pedidos");
            if (pedidos.existsByReservaId(reserva.getId())) throw new ResourceConflictException("La reserva ya tiene un pedido");
            pedido.setReserva(reserva);
        } else {
            MesaAbierta apertura = bloquearApertura(request.mesaAbiertaId());
            if (!apertura.isAbierta()) throw regla("La mesa debe tener una atención abierta");
            if (pedidos.existsByMesaAbiertaId(apertura.getId())) throw new ResourceConflictException("La atención ya tiene un pedido");
            pedido.setMesaAbierta(apertura);
        }
        pedido.setResponsable(actor);
        pedido.setEstado(EstadoPedido.ABIERTO);
        pedidos.saveAndFlush(pedido);
        registrar(pedido, null, "Pedido creado", actor);
        return respuesta(pedido);
    }

    @Transactional
    public Respuesta agregar(Long id, String email, @NotNull @Valid AgregarItem request) {
        Usuario actor = actores.exigir(email, ADMINISTRADOR, MESERO);
        Pedido pedido = bloquearPedido(id);
        exigirResponsable(pedido, actor);
        exigirEditable(pedido);
        ProductoMenu producto = productos.findById(request.productoId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto"));
        if (!producto.isDisponible()) throw regla("El producto no está disponible");
        ItemPedido item = new ItemPedido();
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setNombreProducto(producto.getNombre());
        item.setCantidad(request.cantidad());
        item.setPrecioUnitarioHistorico(producto.getPrecio());
        items.saveAndFlush(item);
        registrar(pedido, pedido.getEstado(), "Agregado ítem " + item.getId() + ": "
                + request.cantidad() + " x " + producto.getNombre() + " a " + producto.getPrecio(), actor);
        return respuesta(pedido);
    }

    @Transactional
    public Respuesta cantidad(Long id, Long itemId, String email, @NotNull @Valid CambiarCantidad request) {
        Usuario actor = actores.exigir(email, ADMINISTRADOR, MESERO);
        Pedido pedido = bloquearPedido(id);
        exigirResponsable(pedido, actor);
        exigirEditable(pedido);
        ItemPedido item = requireItem(id, itemId);
        int anterior = item.getCantidad();
        item.setCantidad(request.cantidad());
        registrar(pedido, pedido.getEstado(), "Cantidad de ítem " + itemId + ": " + anterior + " → " + request.cantidad(), actor);
        return respuesta(pedido);
    }

    @Transactional
    public Respuesta quitar(Long id, Long itemId, String email) {
        Usuario actor = actores.exigir(email, ADMINISTRADOR, MESERO);
        Pedido pedido = bloquearPedido(id);
        exigirResponsable(pedido, actor);
        exigirEditable(pedido);
        ItemPedido item = requireItem(id, itemId);
        registrar(pedido, pedido.getEstado(), "Retirado ítem " + itemId + ": " + item.getCantidad()
                + " x " + item.getNombreProducto() + " a " + item.getPrecioUnitarioHistorico(), actor);
        items.delete(item);
        items.flush();
        return respuesta(pedido);
    }

    @Transactional
    public Respuesta cambiarEstado(Long id, String email, @NotNull @Valid CambiarEstado request) {
        Usuario actor = actores.exigir(email, ADMINISTRADOR, MESERO);
        Pedido pedido = bloquearPedido(id);
        exigirResponsable(pedido, actor);
        if (!pedido.getEstado().permite(request.estado())) throw regla("Transición de pedido no permitida");
        if (request.estado() == EstadoPedido.EN_PREPARACION && !items.existsByPedidoId(id))
            throw regla("No se puede preparar un pedido vacío");
        if (request.estado() == EstadoPedido.CANCELADO && !StringUtils.hasText(request.motivo()))
            throw regla("La cancelación requiere un motivo");
        EstadoPedido anterior = pedido.getEstado();
        pedido.setEstado(request.estado());
        registrar(pedido, anterior, StringUtils.hasText(request.motivo()) ? request.motivo().trim() : "Cambio de estado", actor);
        return respuesta(pedido);
    }

    @Transactional
    public Respuesta asignar(Long id, String email, @NotNull @Valid AsignarMesero request) {
        Usuario actor = actores.exigir(email, ADMINISTRADOR);
        Pedido pedido = bloquearPedido(id);
        if (pedido.getEstado().esFinal()) throw regla("Un pedido finalizado no admite reasignación");
        Usuario mesero = usuarios.findById(request.usuarioId()).filter(Usuario::isActivo)
                .filter(u -> u.getRol() == MESERO)
                .orElseThrow(() -> regla("El responsable debe ser un mesero activo"));
        Long anterior = pedido.getResponsable().getId();
        pedido.setResponsable(mesero);
        registrar(pedido, pedido.getEstado(), "Responsable: " + anterior + " → " + mesero.getId(), actor);
        return respuesta(pedido);
    }

    private Pedido bloquearPedido(Long id) {
        PedidoRepository.Origen origen = pedidos.findOrigen(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el pedido"));
        // Orden de bloqueo único: origen (reserva/atención), luego pedido.
        if (origen.getReservaId() != null) {
            if (bloquearReserva(origen.getReservaId()).getEstado() != EstadoReserva.SENTADA)
                throw regla("La visita ya no está en atención");
        } else if (!bloquearApertura(origen.getMesaAbiertaId()).isAbierta()) {
            throw regla("La atención de mesa ya finalizó");
        }
        return pedidos.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("No existe el pedido"));
    }

    private Reserva bloquearReserva(Long id) {
        return reservas.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("No existe la reserva"));
    }

    private MesaAbierta bloquearApertura(Long id) {
        return aperturas.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("No existe la atención de mesa"));
    }

    private Pedido requirePedido(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new ResourceNotFoundException("No existe el pedido"));
    }

    private ItemPedido requireItem(Long id, Long itemId) {
        return items.findByIdAndPedidoId(itemId, id).orElseThrow(() -> new ResourceNotFoundException("No existe el ítem en este pedido"));
    }

    private void exigirResponsable(Pedido pedido, Usuario actor) {
        if (actor.getRol() != ADMINISTRADOR && !pedido.getResponsable().getId().equals(actor.getId()))
            throw new AccessDeniedException("Sólo el mesero responsable o el administrador puede modificar el pedido");
    }

    private void exigirEditable(Pedido pedido) {
        if (pedido.getEstado() != EstadoPedido.ABIERTO) throw regla("Sólo se modifican ítems de pedidos ABIERTOS");
    }

    private void registrar(Pedido pedido, EstadoPedido anterior, String detalle, Usuario actor) {
        pedido.setActualizadoEn(Instant.now());
        HistorialPedido evento = new HistorialPedido();
        evento.setPedido(pedido); evento.setEstadoAnterior(anterior); evento.setEstadoNuevo(pedido.getEstado());
        evento.setDetalle(detalle); evento.setCambiadoPor(actor);
        historial.saveAndFlush(evento);
    }

    private Respuesta respuesta(Pedido pedido) {
        List<Item> detalle = items.findByPedidoIdOrderByIdAsc(pedido.getId()).stream().map(i -> new Item(
                i.getId(), i.getProducto().getId(), i.getNombreProducto(), i.getCantidad(),
                i.getPrecioUnitarioHistorico(), i.getPrecioUnitarioHistorico().multiply(BigDecimal.valueOf(i.getCantidad())))).toList();
        BigDecimal total = detalle.stream().map(Item::subtotal).reduce(new BigDecimal("0.00"), BigDecimal::add);
        return new Respuesta(pedido.getId(), pedido.getReserva() == null ? null : pedido.getReserva().getId(),
                pedido.getMesaAbierta() == null ? null : pedido.getMesaAbierta().getId(),
                pedido.getMesa().getId(), pedido.getMesa().getNumero(), pedido.getResponsable().getId(),
                pedido.getResponsable().getNombre(), pedido.getEstado(), detalle, total,
                pedido.getCreadoEn(), pedido.getActualizadoEn());
    }

    private BusinessRuleException regla(String mensaje) { return new BusinessRuleException(mensaje); }
}
