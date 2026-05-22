package ayd2.p2b.conference_service_api.feature.committee.application.port;

import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeCongressSummary;

import java.util.Optional;
import java.util.UUID;

public interface CommitteeCongressPort {
    Optional<CommitteeCongressSummary> findManageableCongressById(UUID congressId);
}
