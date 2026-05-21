package ayd2.p2b.conference_service_api.feature.activity.application.get;

import ayd2.p2b.conference_service_api.feature.activity.application.exception.ActivityExceptions;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityLeaderRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.dto.response.ActivityResponse;
import ayd2.p2b.conference_service_api.feature.activity.mapper.ActivityMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class GetActivityUseCase {

    private final ActivityRepositoryPort activityRepositoryPort;
    private final ActivityLeaderRepositoryPort activityLeaderRepositoryPort;
    private final ActivityMapper activityMapper;

    public GetActivityUseCase(
            ActivityRepositoryPort activityRepositoryPort,
            ActivityLeaderRepositoryPort activityLeaderRepositoryPort,
            ActivityMapper activityMapper
    ) {
        this.activityRepositoryPort = activityRepositoryPort;
        this.activityLeaderRepositoryPort = activityLeaderRepositoryPort;
        this.activityMapper = activityMapper;
    }

    public ActivityResponse execute(UUID activityId) {
        ActivityResponse response = activityRepositoryPort.findPublicById(activityId)
                .map(activityMapper::toResponse)
                .orElseThrow(() -> ActivityExceptions.activityNotFound(activityId));
        response.setLeaders(activityLeaderRepositoryPort.findByActivityId(activityId).stream()
                .map(ActivityLeader::getUserId)
                .sorted(Comparator.comparing(UUID::toString))
                .toList());
        return response;
    }
}
