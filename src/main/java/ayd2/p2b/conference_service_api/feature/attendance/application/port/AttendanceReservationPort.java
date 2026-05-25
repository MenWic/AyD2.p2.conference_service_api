package ayd2.p2b.conference_service_api.feature.attendance.application.port;

import java.util.UUID;

public interface AttendanceReservationPort {

    boolean existsReservation(UUID activityId, UUID userId);
}
