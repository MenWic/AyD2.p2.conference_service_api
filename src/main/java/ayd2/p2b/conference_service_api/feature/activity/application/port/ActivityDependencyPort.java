package ayd2.p2b.conference_service_api.feature.activity.application.port;

import java.util.List;
import java.util.UUID;

public interface ActivityDependencyPort {
    List<String> findBlockingDependencies(UUID activityId);
}
