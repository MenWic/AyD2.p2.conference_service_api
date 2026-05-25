package ayd2.p2b.conference_service_api.unit.feature.activity.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.adapter.JpaActivityDependencyAdapter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaActivityDependencyAdapterTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private JpaActivityDependencyAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaActivityDependencyAdapter(entityManager);
    }

    @Test
    void shouldReturnBlockingDependenciesInExpectedOrder() {
        UUID activityId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("activityId", activityId)).thenReturn(query);
        when(query.getSingleResult())
                .thenReturn(Boolean.TRUE)
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.TRUE)
                .thenReturn(Boolean.FALSE);

        List<String> result = adapter.findBlockingDependencies(activityId);

        assertThat(result).containsExactly("reservations", "diplomas");
    }

    @Test
    void shouldIncludeReservationsAndAttendancesAsBlockingDependencies() {
        UUID activityId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("activityId", activityId)).thenReturn(query);
        when(query.getSingleResult())
                .thenReturn(Boolean.TRUE)
                .thenReturn(Boolean.TRUE)
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.FALSE);

        List<String> result = adapter.findBlockingDependencies(activityId);

        assertThat(result).containsExactly("reservations", "attendances");
    }

    @Test
    void shouldReturnEmptyWhenThereAreNoBlockingDependencies() {
        UUID activityId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("activityId", activityId)).thenReturn(query);
        when(query.getSingleResult())
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.FALSE);

        List<String> result = adapter.findBlockingDependencies(activityId);

        assertThat(result).isEmpty();
    }
}
