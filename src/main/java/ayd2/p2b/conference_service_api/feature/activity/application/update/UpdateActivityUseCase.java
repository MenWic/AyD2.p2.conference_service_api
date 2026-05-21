package ayd2.p2b.conference_service_api.feature.activity.application.update;

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
import ayd2.p2b.conference_service_api.feature.activity.dto.request.UpdateActivityRequest;
import ayd2.p2b.conference_service_api.feature.activity.dto.response.ActivityResponse;
import ayd2.p2b.conference_service_api.feature.activity.mapper.ActivityMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static ayd2.p2b.conference_service_api.feature.activity.application.ActivityInputValidator.optionalTrimmedNonBlank;

@Component
@Transactional
public class UpdateActivityUseCase {

    private final ActivityRepositoryPort activityRepositoryPort;
    private final ActivityLeaderRepositoryPort activityLeaderRepositoryPort;
    private final ActivityLeaderResolver activityLeaderResolver;
    private final ActivityCongressRoomPort activityCongressRoomPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final ActivityMapper activityMapper;

    public UpdateActivityUseCase(
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

    public ActivityResponse execute(UUID activityId, UpdateActivityRequest request, ActivityRequesterContext requester) {
        ActivityAccessPolicy.ensureCongressAdminWrite(requester);

        Activity current = activityRepositoryPort.findById(activityId)
                .orElseThrow(() -> ActivityExceptions.activityNotFound(activityId));

        ActivityCongressRoomSummary congressSummary = activityCongressRoomPort.findManageableCongressById(current.getCongressId())
                .orElseThrow(() -> ActivityExceptions.congressNotFound(current.getCongressId()));
        authorizeCongressAccess(requester, congressSummary);

        if (request.getType() != null && request.getType() != current.getType()) {
            throw ActivityExceptions.validationFailed("type cannot be changed after creation");
        }

        UUID nextRoomId = resolveRoomId(current, request.getRoomId());
        String nextName = optionalTrimmedNonBlank(request.getName(), "name");
        String nextDescription = optionalTrimmedNonBlank(request.getDescription(), "description");
        OffsetDateTime nextStartTime = request.getStartTime() != null ? request.getStartTime() : current.getStartTime();
        OffsetDateTime nextEndTime = request.getEndTime() != null ? request.getEndTime() : current.getEndTime();
        Integer nextWorkshopCapacity = request.getWorkshopCapacity() != null
                ? request.getWorkshopCapacity()
                : current.getWorkshopCapacity();

        Activity updated = current.toBuilder()
                .roomId(nextRoomId)
                .name(nextName != null ? nextName : current.getName())
                .description(nextDescription != null ? nextDescription : current.getDescription())
                .startTime(nextStartTime)
                .endTime(nextEndTime)
                .workshopCapacity(nextWorkshopCapacity)
                .updatedBy(requester.getUserId())
                .updatedAt(OffsetDateTime.now())
                .build();
        updated.validateInvariants();

        ActivitySchedulePolicy.ensureNoRoomOverlap(
                activityRepositoryPort,
                updated.getRoomId(),
                updated.getStartTime(),
                updated.getEndTime(),
                current.getId()
        );

        Activity saved = activityRepositoryPort.save(updated);
        if (request.getLeaders() != null) {
            List<ActivityLeader> replacementLeaders = activityLeaderResolver.resolve(
                    saved.getType(),
                    request.getLeaders(),
                    requester.getAccessToken()
            );
            activityLeaderRepositoryPort.replaceLeaders(saved.getId(), replacementLeaders);
        }

        List<ActivityLeader> currentLeaders = activityLeaderRepositoryPort.findByActivityId(saved.getId());
        ActivityResponse response = activityMapper.toResponse(saved);
        response.setLeaders(currentLeaders.stream()
                .map(ActivityLeader::getUserId)
                .sorted(Comparator.comparing(UUID::toString))
                .toList());
        return response;
    }

    private UUID resolveRoomId(Activity current, UUID requestedRoomId) {
        if (requestedRoomId == null) {
            return current.getRoomId();
        }

        ActivityCongressRoomSummary roomSummary = activityCongressRoomPort.findManageableRoomById(requestedRoomId)
                .orElseThrow(() -> ActivityExceptions.roomNotFound(requestedRoomId));

        if (!current.getCongressId().equals(roomSummary.getCongressId())) {
            throw ActivityExceptions.validationFailed("roomId must belong to the same congress");
        }
        return requestedRoomId;
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
