package ayd2.p2b.conference_service_api.unit.feature.activity.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivitySearchCriteria;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.adapter.JpaActivityRepositoryAdapter;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.activity.mapper.ActivityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaActivityRepositoryAdapterTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private ActivityMapper activityMapper;

    private JpaActivityRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaActivityRepositoryAdapter(activityRepository, activityMapper);
    }

    @Test
    void shouldSaveActivityThroughRepositoryAndMapper() {
        Activity activity = sampleActivity();
        ActivityEntity entity = new ActivityEntity();
        ActivityEntity savedEntity = new ActivityEntity();
        savedEntity.setId(activity.getId());

        when(activityMapper.toEntity(activity)).thenReturn(entity);
        when(activityRepository.save(entity)).thenReturn(savedEntity);
        when(activityMapper.toDomain(savedEntity)).thenReturn(activity);

        Activity result = adapter.save(activity);

        assertThat(result).isEqualTo(activity);
    }

    @Test
    void shouldFindById() {
        UUID activityId = UUID.randomUUID();
        ActivityEntity entity = new ActivityEntity();
        Activity domain = sampleActivity().toBuilder().id(activityId).build();

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(entity));
        when(activityMapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById(activityId)).contains(domain);
    }

    @Test
    void shouldFindPublicById() {
        UUID activityId = UUID.randomUUID();
        ActivityEntity entity = new ActivityEntity();
        Activity domain = sampleActivity().toBuilder().id(activityId).build();

        when(activityRepository.findPublicById(activityId)).thenReturn(Optional.of(entity));
        when(activityMapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findPublicById(activityId)).contains(domain);
    }

    @Test
    void shouldFindPublicByCongressIdWithCriteria() {
        UUID congressId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        ActivitySearchCriteria criteria = ActivitySearchCriteria.builder()
                .roomId(UUID.randomUUID())
                .type(ActivityType.PONENCIA)
                .build();
        ActivityEntity entity = new ActivityEntity();
        Activity activity = sampleActivity();

        when(activityRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(activityMapper.toDomain(entity)).thenReturn(activity);

        Page<Activity> result = adapter.findPublicByCongressId(congressId, criteria, pageable);

        assertThat(result.getContent()).containsExactly(activity);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldDelegateOverlapAndDelete() {
        UUID roomId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-07-01T11:00:00Z");

        when(activityRepository.existsRoomTimeOverlap(roomId, start, end, activityId)).thenReturn(true);

        assertThat(adapter.existsRoomTimeOverlap(roomId, start, end, activityId)).isTrue();
        adapter.deleteById(activityId);

        verify(activityRepository).existsRoomTimeOverlap(roomId, start, end, activityId);
        verify(activityRepository).deleteById(activityId);
    }

    private Activity sampleActivity() {
        return Activity.builder()
                .id(UUID.randomUUID())
                .congressId(UUID.randomUUID())
                .roomId(UUID.randomUUID())
                .name("Opening Talk")
                .description("Keynote")
                .type(ActivityType.PONENCIA)
                .startTime(OffsetDateTime.parse("2026-08-01T09:00:00Z"))
                .endTime(OffsetDateTime.parse("2026-08-01T10:00:00Z"))
                .build();
    }
}
