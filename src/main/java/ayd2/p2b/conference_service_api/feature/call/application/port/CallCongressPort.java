package ayd2.p2b.conference_service_api.feature.call.application.port;

import ayd2.p2b.conference_service_api.feature.call.dto.internal.CallCongressSummary;

import java.util.Optional;
import java.util.UUID;

public interface CallCongressPort {
    Optional<CallCongressSummary> findManageableCongressById(UUID congressId);

    boolean existsPublicCongressById(UUID congressId);
}
