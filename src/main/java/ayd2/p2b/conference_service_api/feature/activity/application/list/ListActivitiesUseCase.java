package ayd2.p2b.conference_service_api.feature.activity.application.list;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.activity.application.exception.ActivityExceptions;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityCongressRoomPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityLeaderRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivitySearchCriteria;
import ayd2.p2b.conference_service_api.feature.activity.dto.response.ActivityResponse;
import ayd2.p2b.conference_service_api.feature.activity.mapper.ActivityMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class ListActivitiesUseCase {

    private final ActivityRepositoryPort activityRepositoryPort;
    private final ActivityLeaderRepositoryPort activityLeaderRepositoryPort;
    private final ActivityCongressRoomPort activityCongressRoomPort;
    private final ActivityMapper activityMapper;

    public ListActivitiesUseCase(
            ActivityRepositoryPort activityRepositoryPort,
            ActivityLeaderRepositoryPort activityLeaderRepositoryPort,
            ActivityCongressRoomPort activityCongressRoomPort,
            ActivityMapper activityMapper
    ) {
        this.activityRepositoryPort = activityRepositoryPort;
        this.activityLeaderRepositoryPort = activityLeaderRepositoryPort;
        this.activityCongressRoomPort = activityCongressRoomPort;
        this.activityMapper = activityMapper;
    }

    public PageResponse<ActivityResponse> execute(UUID congressId, ActivitySearchCriteria criteria, Pageable pageable) {
        if (!activityCongressRoomPort.existsPublicCongressById(congressId)) {
            throw ActivityExceptions.congressNotFound(congressId);
        }

        Page<Activity> page = activityRepositoryPort.findPublicByCongressId(congressId, criteria, pageable);
        Set<UUID> activityIds = page.getContent().stream().map(Activity::getId).collect(Collectors.toSet());
        Map<UUID, List<ActivityLeader>> leadersByActivityId = activityLeaderRepositoryPort.findByActivityIds(activityIds);

        List<ActivityResponse> items = page.getContent().stream()
                .map(activity -> {
                    ActivityResponse response = activityMapper.toResponse(activity);
                    response.setLeaders(leadersByActivityId.getOrDefault(activity.getId(), List.of()).stream()
                            .map(ActivityLeader::getUserId)
                            .sorted(Comparator.comparing(UUID::toString))
                            .toList());
                    return response;
                })
                .toList();

        return PageResponse.<ActivityResponse>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
