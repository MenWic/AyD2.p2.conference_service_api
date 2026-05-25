package ayd2.p2b.conference_service_api.feature.reservation.application.port;

import java.util.UUID;

public interface ReservationEnrollmentPort {

    boolean existsEnrollment(UUID congressId, UUID userId);
}
