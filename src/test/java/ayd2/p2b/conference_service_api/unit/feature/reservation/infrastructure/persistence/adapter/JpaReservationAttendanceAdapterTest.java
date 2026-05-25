package ayd2.p2b.conference_service_api.unit.feature.reservation.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.adapter.JpaReservationAttendanceAdapter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaReservationAttendanceAdapterTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private JpaReservationAttendanceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaReservationAttendanceAdapter(entityManager);
    }

    @Test
    void shouldReturnTrueWhenAttendanceExists() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("activityId", activityId)).thenReturn(query);
        when(query.setParameter("userId", userId)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(Boolean.TRUE);

        boolean result = adapter.existsAttendance(activityId, userId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sqlCaptor.capture());
        verify(query).setParameter("activityId", activityId);
        verify(query).setParameter("userId", userId);
        String normalizedSql = sqlCaptor.getValue().toLowerCase();
        assertThat(normalizedSql).contains("from attendances");
        assertThat(normalizedSql).contains("activity_id");
        assertThat(normalizedSql).contains("user_id");
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenAttendanceDoesNotExist() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("activityId", activityId)).thenReturn(query);
        when(query.setParameter("userId", userId)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(Boolean.FALSE);

        boolean result = adapter.existsAttendance(activityId, userId);

        assertThat(result).isFalse();
    }
}
