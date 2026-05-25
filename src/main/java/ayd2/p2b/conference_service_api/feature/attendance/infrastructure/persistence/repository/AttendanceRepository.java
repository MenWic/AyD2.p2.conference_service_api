package ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.entity.AttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<AttendanceEntity, UUID>, JpaSpecificationExecutor<AttendanceEntity> {

    boolean existsByActivityIdAndUserId(UUID activityId, UUID userId);

    List<AttendanceEntity> findByUserIdOrderByRegisteredAtDesc(UUID userId);
}
