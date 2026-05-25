package ayd2.p2b.conference_service_api.integration.port;

import ayd2.p2b.conference_service_api.integration.dto.IamCommitteeCandidateSummary;
import ayd2.p2b.conference_service_api.integration.dto.IamPersonalIdUserSummary;
import ayd2.p2b.conference_service_api.integration.dto.IamUserSummary;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IamUserLookupPort {
    boolean isCongressAdminLinkedToInstitution(UUID userId, UUID institutionId, String accessToken);

    IamCommitteeCandidateSummary getCommitteeCandidateSummary(UUID userId, String accessToken);

    Map<UUID, IamUserSummary> getUsersSummary(Set<UUID> userIds, String accessToken);

    Optional<IamPersonalIdUserSummary> findUserByPersonalId(String personalId, String accessToken);
}
