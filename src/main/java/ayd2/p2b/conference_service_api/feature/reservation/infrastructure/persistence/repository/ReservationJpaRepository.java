package ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> {
    long countByActivityId(UUID activityId);

    List<ReservationEntity> findByActivityId(UUID activityId);
}
