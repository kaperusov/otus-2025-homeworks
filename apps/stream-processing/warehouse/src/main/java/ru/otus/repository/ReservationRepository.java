package ru.otus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.models.Reservation;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    boolean existsByOrderId(UUID orderId);

    Optional<Reservation> findByIdAndAndStatus(UUID reservationId, String status);
}