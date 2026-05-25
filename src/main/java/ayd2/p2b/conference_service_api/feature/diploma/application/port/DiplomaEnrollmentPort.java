package ayd2.p2b.conference_service_api.feature.diploma.application.port;

import java.util.UUID;

public interface DiplomaEnrollmentPort {
    boolean existsEnrollment(UUID congressId, UUID userId);
}
