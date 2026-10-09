package com.example.gastroreservabackend1.service;

import com.example.gastroreservabackend1.dto.feedback.FeedbackDtos.*;
import com.example.gastroreservabackend1.exception.*;
import com.example.gastroreservabackend1.model.*;
import com.example.gastroreservabackend1.repository.FeedbackRepository;
import com.example.gastroreservabackend1.repository.ReservaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import static com.example.gastroreservabackend1.model.RolUsuario.*;

@Service
@Validated
@Transactional(readOnly = true)
public class FeedbackService {
    private final FeedbackRepository feedback;
    private final ReservaRepository reservas;
    private final ActorService actores;
    private final Clock clock;
    public FeedbackService(FeedbackRepository feedback, ReservaRepository reservas, ActorService actores, Clock clock) {
        this.feedback = feedback; this.reservas = reservas; this.actores = actores; this.clock = clock;
    }

    @Transactional
    public Respuesta crear(String email, @NotNull @Valid Crear request) {
        Usuario actor = actores.exigir(email, CLIENTE);
        Reserva reserva = reservas.findByIdForUpdate(request.reservaId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe una reserva propia con ese id"));
        if (reserva.getCliente().getUsuario() == null || !reserva.getCliente().getUsuario().getId().equals(actor.getId()))
            throw new ResourceNotFoundException("No existe una reserva propia con ese id");
        if (reserva.getEstado() != EstadoReserva.FINALIZADA)
            throw new BusinessRuleException("Sólo se puede opinar después de finalizar la visita");
        if (feedback.existsByReservaId(reserva.getId())) throw new ResourceConflictException("Esta visita ya tiene una valoración");
        Feedback opinion = new Feedback();
        opinion.setReserva(reserva); opinion.setAutor(actor); opinion.setPuntuacion(request.puntuacion());
        opinion.setComentario(request.comentario().trim());
        return respuesta(feedback.saveAndFlush(opinion));
    }

    public Respuesta buscar(Long id, String email) {
        Usuario actor = actores.exigir(email, ADMINISTRADOR, CLIENTE);
        Feedback opinion = feedback.findById(id).orElseThrow(() -> new ResourceNotFoundException("No existe la valoración"));
        if (actor.getRol() != ADMINISTRADOR && !opinion.getAutor().getId().equals(actor.getId()))
            throw new ResourceNotFoundException("No existe la valoración");
        return respuesta(opinion);
    }

    public List<Respuesta> listar(String email, boolean mios, Integer puntuacion, LocalDate desde, LocalDate hasta) {
        Usuario actor = mios ? actores.exigir(email, CLIENTE) : actores.exigir(email, ADMINISTRADOR);
        if (desde != null && hasta != null && desde.isAfter(hasta)) throw new BusinessRuleException("El rango de fechas es inválido");
        Specification<Feedback> filtro = (r, q, cb) -> cb.conjunction();
        if (mios) filtro = filtro.and((r, q, cb) -> cb.equal(r.get("autor").get("id"), actor.getId()));
        if (puntuacion != null) filtro = filtro.and((r, q, cb) -> cb.equal(r.get("puntuacion"), puntuacion));
        if (desde != null) filtro = filtro.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("creadoEn"), desde.atStartOfDay(clock.getZone()).toInstant()));
        if (hasta != null) filtro = filtro.and((r, q, cb) -> cb.lessThan(r.get("creadoEn"), hasta.plusDays(1).atStartOfDay(clock.getZone()).toInstant()));
        return feedback.findAll(filtro, Sort.by(Sort.Direction.DESC, "id")).stream().map(this::respuesta).toList();
    }

    private Respuesta respuesta(Feedback f) {
        return new Respuesta(f.getId(), f.getReserva().getId(), f.getAutor().getId(), f.getAutor().getNombre(),
                f.getPuntuacion(), f.getComentario(), f.getCreadoEn());
    }
}
