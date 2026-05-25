package ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.entity.ReservationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {

    boolean existsByActivityIdAndUserId(UUID activityId, UUID userId);

    long countByActivityId(UUID activityId);

    Page<ReservationEntity> findByActivityId(UUID activityId, Pageable pageable);

    Page<ReservationEntity> findByUserId(UUID userId, Pageable pageable);
}
