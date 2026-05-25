package ayd2.p2b.conference_service_api.unit.feature.reservation.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.adapter.JpaReservationActivityAdapter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaReservationActivityAdapterTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private JpaReservationActivityAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaReservationActivityAdapter(entityManager);
    }

    @Test
    void shouldReturnActivitySummaryUsingLockingLookup() {
        UUID activityId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("activityId", activityId)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                activityId, congressId, institutionId, createdBy, "TALLER", 25
        }));

        var result = adapter.findActivityByIdForReservationUpdate(activityId);

        assertThat(result).isPresent();
        assertThat(result.get().getActivityId()).isEqualTo(activityId);
        assertThat(result.get().getCongressId()).isEqualTo(congressId);
        assertThat(result.get().getInstitutionId()).isEqualTo(institutionId);
        assertThat(result.get().getCongressCreatedBy()).isEqualTo(createdBy);
        assertThat(result.get().getType()).isEqualTo(ActivityType.TALLER);
        assertThat(result.get().getWorkshopCapacity()).isEqualTo(25);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sqlCaptor.capture());
        assertThat(sqlCaptor.getValue().toLowerCase()).contains("for update of a");
    }

    @Test
    void shouldReturnEmptyWhenActivityDoesNotExistForLockingLookup() {
        UUID activityId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("activityId", activityId)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        Optional<?> result = adapter.findActivityByIdForReservationUpdate(activityId);

        assertThat(result).isEmpty();
    }
}
