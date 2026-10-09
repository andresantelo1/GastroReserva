package com.example.gastroreservabackend1.dto.pedido;

import com.example.gastroreservabackend1.model.EstadoPedido;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public final class PedidoDtos {
    private PedidoDtos() {}

    public record Crear(@Positive Long reservaId, @Positive Long mesaAbiertaId) {
        @AssertTrue(message = "Indique una reserva o una mesa abierta, no ambas")
        public boolean isOrigenUnico() { return (reservaId == null) != (mesaAbiertaId == null); }
    }
    public record AgregarItem(@NotNull @Positive Long productoId, @NotNull @Min(1) @Max(999) Integer cantidad) {}
    public record CambiarCantidad(@NotNull @Min(1) @Max(999) Integer cantidad) {}
    public record CambiarEstado(@NotNull EstadoPedido estado, @Size(max = 500) String motivo) {}
    public record AsignarMesero(@NotNull @Positive Long usuarioId) {}
    public record AbrirMesa(@NotNull @Positive Long mesaId, @NotNull @Positive Integer cantidadPersonas,
                            @NotNull LocalDateTime finPrevisto) {}
    public record Item(Long id, Long productoId, String nombreProducto, Integer cantidad,
                       BigDecimal precioUnitarioHistorico, BigDecimal subtotal) {}
    public record Respuesta(Long id, Long reservaId, Long mesaAbiertaId, Long mesaId, Integer numeroMesa,
                            Long responsableId, String responsableNombre, EstadoPedido estado,
                            List<Item> items, BigDecimal totalOperativo, Instant creadoEn, Instant actualizadoEn) {}
    public record Historial(Long id, EstadoPedido estadoAnterior, EstadoPedido estadoNuevo, String detalle,
                            Long cambiadoPorId, String cambiadoPorNombre, Instant creadoEn) {}
    public record MesaAbiertaRespuesta(Long id, Long mesaId, Integer numeroMesa, Integer cantidadPersonas,
                                      LocalDateTime inicio, LocalDateTime finPrevisto, boolean abierta,
                                      Long creadaPorId, Instant creadaEn, Long cerradaPorId, Instant cerradaEn) {}
}
