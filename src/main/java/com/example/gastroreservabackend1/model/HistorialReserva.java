package com.example.gastroreservabackend1.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "historial_reservas")
public class HistorialReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEventoReserva tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoReserva estadoNuevo;

    @Column(length = 500)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cambiado_por_usuario_id", nullable = false)
    private Usuario cambiadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_anterior_id")
    private Mesa mesaAnterior;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_nueva_id")
    private Mesa mesaNueva;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    public HistorialReserva() {
    }

    @PrePersist
    void onCreate() {
        creadoEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public TipoEventoReserva getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEventoReserva tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public EstadoReserva getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoReserva estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoReserva getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoReserva estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Usuario getCambiadoPor() {
        return cambiadoPor;
    }

    public void setCambiadoPor(Usuario cambiadoPor) {
        this.cambiadoPor = cambiadoPor;
    }

    public Mesa getMesaAnterior() {
        return mesaAnterior;
    }

    public void setMesaAnterior(Mesa mesaAnterior) {
        this.mesaAnterior = mesaAnterior;
    }

    public Mesa getMesaNueva() {
        return mesaNueva;
    }

    public void setMesaNueva(Mesa mesaNueva) {
        this.mesaNueva = mesaNueva;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
