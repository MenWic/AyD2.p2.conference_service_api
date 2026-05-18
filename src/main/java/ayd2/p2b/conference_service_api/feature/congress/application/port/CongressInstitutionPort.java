package ayd2.p2b.conference_service_api.feature.congress.application.port;

import ayd2.p2b.conference_service_api.feature.congress.dto.internal.InstitutionSummary;

import java.util.Optional;
import java.util.UUID;

public interface CongressInstitutionPort {
    Optional<InstitutionSummary> findActiveInstitutionById(UUID institutionId);
}
