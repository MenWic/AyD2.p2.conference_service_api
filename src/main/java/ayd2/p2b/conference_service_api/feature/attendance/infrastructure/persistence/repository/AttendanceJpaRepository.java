package ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.entity.AttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttendanceJpaRepository extends JpaRepository<AttendanceEntity, UUID> {
    long countByActivityId(UUID activityId);
}
