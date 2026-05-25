package ayd2.p2b.conference_service_api.feature.report.application.participants.port;

import ayd2.p2b.conference_service_api.feature.report.dto.internal.CongressInstitutionSummary;

import java.util.Optional;
import java.util.UUID;

public interface ParticipantsCongressScopePort {
    Optional<CongressInstitutionSummary> findCongressSummary(UUID congressId);
}
