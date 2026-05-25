package ayd2.p2b.conference_service_api.feature.attendance.application.port;

import ayd2.p2b.conference_service_api.feature.attendance.domain.model.Attendance;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

public interface AttendanceRepositoryPort {

    Attendance save(Attendance attendance);

    boolean existsByActivityIdAndUserId(UUID activityId, UUID userId);

    Page<Attendance> findByCriteria(
            AttendanceSearchCriteria criteria,
            Pageable pageable,
            UUID requesterUserId,
            Set<UUID> linkedInstitutionIds
    );
}
