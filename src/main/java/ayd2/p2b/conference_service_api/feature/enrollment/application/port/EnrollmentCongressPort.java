package ayd2.p2b.conference_service_api.feature.enrollment.application.port;

import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.CongressEnrollmentSummary;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentCongressPort {

  Optional<CongressEnrollmentSummary> findPublicEnrollmentCongressById(UUID congressId);

  Optional<CongressEnrollmentSummary> findManageableCongressById(UUID congressId);
}
