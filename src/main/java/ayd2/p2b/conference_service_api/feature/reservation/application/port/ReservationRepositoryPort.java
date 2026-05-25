package ayd2.p2b.conference_service_api.feature.reservation.application.port;

import ayd2.p2b.conference_service_api.feature.reservation.domain.model.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepositoryPort {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(UUID reservationId);

    boolean existsByActivityIdAndUserId(UUID activityId, UUID userId);

    long countByActivityId(UUID activityId);

    Page<Reservation> findByActivityId(UUID activityId, Pageable pageable);

    Page<Reservation> findByUserId(UUID userId, Pageable pageable);

    void deleteById(UUID reservationId);
}
