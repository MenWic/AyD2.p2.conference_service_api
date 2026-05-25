package ayd2.p2b.conference_service_api.feature.diploma.application.port;

import ayd2.p2b.conference_service_api.feature.diploma.dto.internal.DiplomaEligibilityCandidate;

import java.util.List;
import java.util.UUID;

public interface DiplomaEligibilityPort {
    List<DiplomaEligibilityCandidate> findParticipationCandidates(UUID userId);

    List<DiplomaEligibilityCandidate> findLeadershipCandidates(UUID userId);
}
