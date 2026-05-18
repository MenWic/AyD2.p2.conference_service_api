package ayd2.p2b.conference_service_api.integration.port;

import java.util.UUID;

public interface IamUserLookupPort {
    boolean isCongressAdminLinkedToInstitution(UUID userId, UUID institutionId, String accessToken);
}
