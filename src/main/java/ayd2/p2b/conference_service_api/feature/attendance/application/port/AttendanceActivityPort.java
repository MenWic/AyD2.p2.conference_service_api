package ayd2.p2b.conference_service_api.feature.attendance.application.port;

import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceActivitySummary;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceActivityPort {

    Optional<AttendanceActivitySummary> findActivityById(UUID activityId);
}
