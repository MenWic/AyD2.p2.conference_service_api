package ayd2.p2b.conference_service_api.feature.activity.application.create;

import ayd2.p2b.conference_service_api.feature.activity.application.exception.ActivityExceptions;
import ayd2.p2b.conference_service_api.feature.activity.application.leader.ActivityLeaderResolver;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityCongressRoomPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityLeaderRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.schedule.ActivitySchedulePolicy;
import ayd2.p2b.conference_service_api.feature.activity.application.support.ActivityAccessPolicy;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityCongressRoomSummary;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityRequesterContext;
import ayd2.p2b.conference_service_api.feature.activity.dto.request.CreateActivityRequest;
import ayd2.p2b.conference_service_api.feature.activity.dto.response.ActivityResponse;
import ayd2.p2b.conference_service_api.feature.activity.mapper.ActivityMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static ayd2.p2b.conference_service_api.feature.activity.application.ActivityInputValidator.requiredTrimmed;

@Component
@Transactional
public class CreateActivityUseCase {

    private final ActivityRepositoryPort activityRepositoryPort;
    private final ActivityLeaderRepositoryPort activityLeaderRepositoryPort;
    private final ActivityLeaderResolver activityLeaderResolver;
    private final ActivityCongressRoomPort activityCongressRoomPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final ActivityMapper activityMapper;

    public CreateActivityUseCase(
            ActivityRepositoryPort activityRepositoryPort,
            ActivityLeaderRepositoryPort activityLeaderRepositoryPort,
            ActivityLeaderResolver activityLeaderResolver,
            ActivityCongressRoomPort activityCongressRoomPort,
            IamUserLookupPort iamUserLookupPort,
            ActivityMapper activityMapper
    ) {
        this.activityRepositoryPort = activityRepositoryPort;
        this.activityLeaderRepositoryPort = activityLeaderRepositoryPort;
        this.activityLeaderResolver = activityLeaderResolver;
        this.activityCongressRoomPort = activityCongressRoomPort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.activityMapper = activityMapper;
    }

    public ActivityResponse execute(UUID congressId, CreateActivityRequest request, ActivityRequesterContext requester) {
        ActivityAccessPolicy.ensureCongressAdminWrite(requester);

        ActivityCongressRoomSummary congress = activityCongressRoomPort.findManageableCongressById(congressId)
                .orElseThrow(() -> ActivityExceptions.congressNotFound(congressId));
        authorizeCongressAccess(requester, congress);

        ActivityCongressRoomSummary roomSummary = activityCongressRoomPort.findManageableRoomById(request.getRoomId())
                .orElseThrow(() -> ActivityExceptions.roomNotFound(request.getRoomId()));
        if (!congressId.equals(roomSummary.getCongressId())) {
            throw ActivityExceptions.validationFailed("roomId must belong to congressId from path");
        }

        Activity activityToSave = Activity.builder()
                .congressId(congressId)
                .roomId(roomSummary.getRoomId())
                .name(requiredTrimmed(request.getName(), "name"))
                .description(requiredTrimmed(request.getDescription(), "description"))
                .type(request.getType())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .workshopCapacity(request.getWorkshopCapacity())
                .createdBy(requester.getUserId())
                .build();
        activityToSave.validateInvariants();

        ActivitySchedulePolicy.ensureNoRoomOverlap(
                activityRepositoryPort,
                activityToSave.getRoomId(),
                activityToSave.getStartTime(),
                activityToSave.getEndTime(),
                null
        );

        Activity saved = activityRepositoryPort.save(activityToSave);
        List<ActivityLeader> resolvedLeaders = activityLeaderResolver.resolve(
                saved.getType(),
                request.getLeaders(),
                requester.getAccessToken()
        );
        activityLeaderRepositoryPort.replaceLeaders(saved.getId(), resolvedLeaders);
        return toResponse(saved, resolvedLeaders);
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

    private ActivityResponse toResponse(Activity activity, List<ActivityLeader> leaders) {
        ActivityResponse response = activityMapper.toResponse(activity);
        response.setLeaders(leaders.stream()
                .map(ActivityLeader::getUserId)
                .sorted(Comparator.comparing(UUID::toString))
                .toList());
        return response;
    }
}
