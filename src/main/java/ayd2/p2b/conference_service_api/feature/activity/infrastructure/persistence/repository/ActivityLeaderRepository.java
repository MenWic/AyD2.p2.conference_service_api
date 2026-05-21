package ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository;

import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityLeaderEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityLeaderEntityId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ActivityLeaderRepository extends JpaRepository<ActivityLeaderEntity, ActivityLeaderEntityId> {
    void deleteByActivityId(UUID activityId);

    List<ActivityLeaderEntity> findByActivityIdOrderByUserIdAsc(UUID activityId);

    List<ActivityLeaderEntity> findByActivityIdInOrderByActivityIdAscUserIdAsc(Collection<UUID> activityIds);
}
