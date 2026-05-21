package ayd2.p2b.conference_service_api.feature.activity.application.port;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivitySearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepositoryPort {
    Activity save(Activity activity);

    Optional<Activity> findById(UUID activityId);

    Optional<Activity> findPublicById(UUID activityId);

    Page<Activity> findPublicByCongressId(UUID congressId, ActivitySearchCriteria criteria, Pageable pageable);

    boolean existsRoomTimeOverlap(
            UUID roomId,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            UUID excludedActivityId
    );

    void deleteById(UUID activityId);
}
