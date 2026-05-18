package ayd2.p2b.conference_service_api.feature.congress.application.port;

import java.util.List;
import java.util.UUID;

public interface CongressDeletionGuardPort {
    List<String> findBlockingDependencies(UUID congressId);
}
