package ayd2.p2b.conference_service_api.feature.activity.application.port;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ActivityLeaderRepositoryPort {
    void replaceLeaders(UUID activityId, List<ActivityLeader> leaders);

    List<ActivityLeader> findByActivityId(UUID activityId);

    Map<UUID, List<ActivityLeader>> findByActivityIds(Set<UUID> activityIds);
}
