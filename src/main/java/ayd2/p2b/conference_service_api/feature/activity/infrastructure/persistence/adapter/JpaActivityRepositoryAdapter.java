package ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivitySearchCriteria;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.specification.ActivitySpecification;
import ayd2.p2b.conference_service_api.feature.activity.mapper.ActivityMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaActivityRepositoryAdapter implements ActivityRepositoryPort {

    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;

    public JpaActivityRepositoryAdapter(ActivityRepository activityRepository, ActivityMapper activityMapper) {
        this.activityRepository = activityRepository;
        this.activityMapper = activityMapper;
    }

    @Override
    public Activity save(Activity activity) {
        return activityMapper.toDomain(activityRepository.save(activityMapper.toEntity(activity)));
    }

    @Override
    public Optional<Activity> findById(UUID activityId) {
        return activityRepository.findById(activityId)
                .map(activityMapper::toDomain);
    }

    @Override
    public Optional<Activity> findPublicById(UUID activityId) {
        return activityRepository.findPublicById(activityId)
                .map(activityMapper::toDomain);
    }

    @Override
    public Page<Activity> findPublicByCongressId(UUID congressId, ActivitySearchCriteria criteria, Pageable pageable) {
        return activityRepository.findAll(ActivitySpecification.publicByCongressAndFilters(congressId, criteria), pageable)
                .map(activityMapper::toDomain);
    }

    @Override
    public boolean existsRoomTimeOverlap(UUID roomId, OffsetDateTime startTime, OffsetDateTime endTime, UUID excludedActivityId) {
        return activityRepository.existsRoomTimeOverlap(roomId, startTime, endTime, excludedActivityId);
    }

    @Override
    public void deleteById(UUID activityId) {
        activityRepository.deleteById(activityId);
    }
}
