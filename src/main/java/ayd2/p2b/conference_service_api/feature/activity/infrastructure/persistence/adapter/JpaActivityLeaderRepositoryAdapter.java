package ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityLeaderRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityLeaderEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityLeaderRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class JpaActivityLeaderRepositoryAdapter implements ActivityLeaderRepositoryPort {

    private final ActivityLeaderRepository activityLeaderRepository;

    public JpaActivityLeaderRepositoryAdapter(ActivityLeaderRepository activityLeaderRepository) {
        this.activityLeaderRepository = activityLeaderRepository;
    }

    @Override
    public void replaceLeaders(UUID activityId, List<ActivityLeader> leaders) {
        activityLeaderRepository.deleteByActivityId(activityId);
        if (leaders == null || leaders.isEmpty()) {
            return;
        }

        List<ActivityLeaderEntity> entities = leaders.stream()
                .map(leader -> toEntity(activityId, leader))
                .toList();
        activityLeaderRepository.saveAll(entities);
    }

    @Override
    public List<ActivityLeader> findByActivityId(UUID activityId) {
        return activityLeaderRepository.findByActivityIdOrderByUserIdAsc(activityId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Map<UUID, List<ActivityLeader>> findByActivityIds(Set<UUID> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<ActivityLeader>> grouped = new LinkedHashMap<>();
        for (ActivityLeaderEntity entity : activityLeaderRepository.findByActivityIdInOrderByActivityIdAscUserIdAsc(activityIds)) {
            grouped.computeIfAbsent(entity.getActivityId(), ignored -> new ArrayList<>())
                    .add(toDomain(entity));
        }
        return grouped;
    }

    private ActivityLeaderEntity toEntity(UUID activityId, ActivityLeader leader) {
        ActivityLeaderEntity entity = new ActivityLeaderEntity();
        entity.setActivityId(activityId);
        entity.setUserId(leader.getUserId());
        entity.setLeaderType(leader.getLeaderType());
        return entity;
    }

    private ActivityLeader toDomain(ActivityLeaderEntity entity) {
        return ActivityLeader.builder()
                .userId(entity.getUserId())
                .leaderType(entity.getLeaderType())
                .build();
    }
}
