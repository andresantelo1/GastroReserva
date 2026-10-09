package com.example.gastroreservabackend1.repository;

import com.example.gastroreservabackend1.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FeedbackRepository extends JpaRepository<Feedback, Long>, JpaSpecificationExecutor<Feedback> {
    boolean existsByReservaId(Long reservaId);
}
