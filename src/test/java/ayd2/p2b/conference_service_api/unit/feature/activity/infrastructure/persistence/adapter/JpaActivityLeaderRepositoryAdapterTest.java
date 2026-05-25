package ayd2.p2b.conference_service_api.unit.feature.activity.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.adapter.JpaActivityLeaderRepositoryAdapter;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityLeaderEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityLeaderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaActivityLeaderRepositoryAdapterTest {

    @Mock
    private ActivityLeaderRepository activityLeaderRepository;

    private JpaActivityLeaderRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaActivityLeaderRepositoryAdapter(activityLeaderRepository);
    }

    @Test
    void shouldReplaceLeadersDeletingFirstThenSavingMappedEntities() {
        UUID activityId = UUID.randomUUID();
        ActivityLeader firstLeader = ActivityLeader.builder()
                .userId(UUID.randomUUID())
                .leaderType(ActivityLeaderType.SPEAKER)
                .build();
        ActivityLeader secondLeader = ActivityLeader.builder()
                .userId(UUID.randomUUID())
                .leaderType(ActivityLeaderType.GUEST_SPEAKER)
                .build();

        adapter.replaceLeaders(activityId, List.of(firstLeader, secondLeader));

        InOrder inOrder = inOrder(activityLeaderRepository);
        inOrder.verify(activityLeaderRepository).deleteByActivityId(activityId);
        ArgumentCaptor<List<ActivityLeaderEntity>> captor = ArgumentCaptor.forClass(List.class);
        inOrder.verify(activityLeaderRepository).saveAll(captor.capture());

        List<ActivityLeaderEntity> entities = captor.getValue();
        assertThat(entities).hasSize(2);
        assertThat(entities.getFirst().getActivityId()).isEqualTo(activityId);
        assertThat(entities.getFirst().getUserId()).isEqualTo(firstLeader.getUserId());
        assertThat(entities.getFirst().getLeaderType()).isEqualTo(firstLeader.getLeaderType());
        assertThat(entities.get(1).getActivityId()).isEqualTo(activityId);
        assertThat(entities.get(1).getUserId()).isEqualTo(secondLeader.getUserId());
        assertThat(entities.get(1).getLeaderType()).isEqualTo(secondLeader.getLeaderType());
    }

    @Test
    void shouldDeleteAndSkipSaveWhenReplacingWithNullLeaders() {
        UUID activityId = UUID.randomUUID();

        adapter.replaceLeaders(activityId, null);

        verify(activityLeaderRepository).deleteByActivityId(activityId);
        verify(activityLeaderRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldDeleteAndSkipSaveWhenReplacingWithEmptyLeaders() {
        UUID activityId = UUID.randomUUID();

        adapter.replaceLeaders(activityId, List.of());

        verify(activityLeaderRepository).deleteByActivityId(activityId);
        verify(activityLeaderRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldFindByActivityIdAndMapOrderedLeaders() {
        UUID activityId = UUID.randomUUID();
        ActivityLeaderEntity firstEntity = new ActivityLeaderEntity();
        firstEntity.setActivityId(activityId);
        firstEntity.setUserId(UUID.randomUUID());
        firstEntity.setLeaderType(ActivityLeaderType.SPEAKER);
        ActivityLeaderEntity secondEntity = new ActivityLeaderEntity();
        secondEntity.setActivityId(activityId);
        secondEntity.setUserId(UUID.randomUUID());
        secondEntity.setLeaderType(ActivityLeaderType.WORKSHOP_LEADER);

        when(activityLeaderRepository.findByActivityIdOrderByUserIdAsc(activityId))
                .thenReturn(List.of(firstEntity, secondEntity));

        List<ActivityLeader> result = adapter.findByActivityId(activityId);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getUserId()).isEqualTo(firstEntity.getUserId());
        assertThat(result.getFirst().getLeaderType()).isEqualTo(firstEntity.getLeaderType());
        assertThat(result.get(1).getUserId()).isEqualTo(secondEntity.getUserId());
        assertThat(result.get(1).getLeaderType()).isEqualTo(secondEntity.getLeaderType());
    }

    @Test
    void shouldReturnEmptyMapForNullActivityIds() {
        assertThat(adapter.findByActivityIds(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptyMapForEmptyActivityIds() {
        assertThat(adapter.findByActivityIds(Set.of())).isEmpty();
    }

    @Test
    void shouldGroupLeadersByActivityIdWhenFindingByActivityIds() {
        UUID activityA = UUID.randomUUID();
        UUID activityB = UUID.randomUUID();

        ActivityLeaderEntity entityA1 = new ActivityLeaderEntity();
        entityA1.setActivityId(activityA);
        entityA1.setUserId(UUID.randomUUID());
        entityA1.setLeaderType(ActivityLeaderType.SPEAKER);
        ActivityLeaderEntity entityA2 = new ActivityLeaderEntity();
        entityA2.setActivityId(activityA);
        entityA2.setUserId(UUID.randomUUID());
        entityA2.setLeaderType(ActivityLeaderType.GUEST_SPEAKER);
        ActivityLeaderEntity entityB1 = new ActivityLeaderEntity();
        entityB1.setActivityId(activityB);
        entityB1.setUserId(UUID.randomUUID());
        entityB1.setLeaderType(ActivityLeaderType.WORKSHOP_LEADER);

        when(activityLeaderRepository.findByActivityIdInOrderByActivityIdAscUserIdAsc(Set.of(activityA, activityB)))
                .thenReturn(List.of(entityA1, entityA2, entityB1));

        Map<UUID, List<ActivityLeader>> result = adapter.findByActivityIds(Set.of(activityA, activityB));

        assertThat(result).hasSize(2);
        assertThat(result.get(activityA)).hasSize(2);
        assertThat(result.get(activityA).getFirst().getUserId()).isEqualTo(entityA1.getUserId());
        assertThat(result.get(activityA).getFirst().getLeaderType()).isEqualTo(entityA1.getLeaderType());
        assertThat(result.get(activityA).get(1).getUserId()).isEqualTo(entityA2.getUserId());
        assertThat(result.get(activityB)).hasSize(1);
        assertThat(result.get(activityB).getFirst().getUserId()).isEqualTo(entityB1.getUserId());
        assertThat(result.get(activityB).getFirst().getLeaderType()).isEqualTo(entityB1.getLeaderType());
    }
}
