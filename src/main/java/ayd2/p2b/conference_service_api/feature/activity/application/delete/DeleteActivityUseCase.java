package ayd2.p2b.conference_service_api.feature.activity.application.delete;

import ayd2.p2b.conference_service_api.feature.activity.application.exception.ActivityExceptions;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityCongressRoomPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityDependencyPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityLeaderRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.support.ActivityAccessPolicy;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityCongressRoomSummary;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityRequesterContext;
import ayd2.p2b.conference_service_api.feature.activity.dto.response.ActivityResponse;
import ayd2.p2b.conference_service_api.feature.activity.mapper.ActivityMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Transactional
public class DeleteActivityUseCase {

    private final ActivityRepositoryPort activityRepositoryPort;
    private final ActivityLeaderRepositoryPort activityLeaderRepositoryPort;
    private final ActivityCongressRoomPort activityCongressRoomPort;
    private final ActivityDependencyPort activityDependencyPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final ActivityMapper activityMapper;

    public DeleteActivityUseCase(
            ActivityRepositoryPort activityRepositoryPort,
            ActivityLeaderRepositoryPort activityLeaderRepositoryPort,
            ActivityCongressRoomPort activityCongressRoomPort,
            ActivityDependencyPort activityDependencyPort,
            IamUserLookupPort iamUserLookupPort,
            ActivityMapper activityMapper
    ) {
        this.activityRepositoryPort = activityRepositoryPort;
        this.activityLeaderRepositoryPort = activityLeaderRepositoryPort;
        this.activityCongressRoomPort = activityCongressRoomPort;
        this.activityDependencyPort = activityDependencyPort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.activityMapper = activityMapper;
    }

    public ActivityResponse execute(UUID activityId, ActivityRequesterContext requester) {
        ActivityAccessPolicy.ensureCongressAdminWrite(requester);

        Activity current = activityRepositoryPort.findById(activityId)
                .orElseThrow(() -> ActivityExceptions.activityNotFound(activityId));

        ActivityCongressRoomSummary congressSummary = activityCongressRoomPort.findManageableCongressById(current.getCongressId())
                .orElseThrow(() -> ActivityExceptions.congressNotFound(current.getCongressId()));
        authorizeCongressAccess(requester, congressSummary);

        List<String> dependencies = activityDependencyPort.findBlockingDependencies(activityId);
        if (!dependencies.isEmpty()) {
            throw ActivityExceptions.dependencyConflict(activityId, dependencies);
        }

        List<ActivityLeader> leaders = activityLeaderRepositoryPort.findByActivityId(activityId);
        activityRepositoryPort.deleteById(activityId);
        Activity deleted = current.toBuilder()
                .updatedBy(requester.getUserId())
                .updatedAt(OffsetDateTime.now())
                .build();

        ActivityResponse response = activityMapper.toResponse(deleted);
        response.setLeaders(leaders.stream()
                .map(ActivityLeader::getUserId)
                .sorted(Comparator.comparing(UUID::toString))
                .toList());
        return response;
    }

    private void authorizeCongressAccess(ActivityRequesterContext requester, ActivityCongressRoomSummary congressSummary) {
        if (requester.getUserId().equals(congressSummary.getCreatedBy())) {
            ActivityAccessPolicy.ensureCanManageActivity(requester, congressSummary, false);
            return;
        }
        boolean linkedToInstitution = iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(),
                congressSummary.getInstitutionId(),
                requester.getAccessToken()
        );
        ActivityAccessPolicy.ensureCanManageActivity(requester, congressSummary, linkedToInstitution);
    }
}
