package com.example.gastroreservabackend1.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "mesas_abiertas")
public class MesaAbierta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mesa_id", nullable = false)
    private Mesa mesa;

    @Column(name = "mesa_activa_id", unique = true)
    private Long mesaActivaId;

    @Column(name = "cantidad_personas", nullable = false)
    private Integer cantidadPersonas;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(name = "fin_previsto", nullable = false)
    private LocalDateTime finPrevisto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creada_por_id", nullable = false)
    private Usuario creadaPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cerrada_por_id")
    private Usuario cerradaPor;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    @Column(name = "cerrada_en")
    private Instant cerradaEn;

    @PrePersist
    void onCreate() { creadaEn = Instant.now(); }

    public boolean isAbierta() { return mesaActivaId != null; }

    public Long getId() { return id; }

    public Mesa getMesa() { return mesa; }
    public void setMesa(Mesa mesa) { this.mesa = mesa; }

    public Long getMesaActivaId() { return mesaActivaId; }
    public void setMesaActivaId(Long mesaActivaId) { this.mesaActivaId = mesaActivaId; }

    public Integer getCantidadPersonas() { return cantidadPersonas; }
    public void setCantidadPersonas(Integer cantidadPersonas) { this.cantidadPersonas = cantidadPersonas; }

    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }

    public LocalDateTime getFinPrevisto() { return finPrevisto; }
    public void setFinPrevisto(LocalDateTime finPrevisto) { this.finPrevisto = finPrevisto; }

    public Usuario getCreadaPor() { return creadaPor; }
    public void setCreadaPor(Usuario creadaPor) { this.creadaPor = creadaPor; }

    public Usuario getCerradaPor() { return cerradaPor; }
    public void setCerradaPor(Usuario cerradaPor) { this.cerradaPor = cerradaPor; }

    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant creadaEn) { this.creadaEn = creadaEn; }

    public Instant getCerradaEn() { return cerradaEn; }
    public void setCerradaEn(Instant cerradaEn) { this.cerradaEn = cerradaEn; }

}
