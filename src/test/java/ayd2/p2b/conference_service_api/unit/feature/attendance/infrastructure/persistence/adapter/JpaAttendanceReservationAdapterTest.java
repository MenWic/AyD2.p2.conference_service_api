package ayd2.p2b.conference_service_api.unit.feature.attendance.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.adapter.JpaAttendanceReservationAdapter;
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
class JpaAttendanceReservationAdapterTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private JpaAttendanceReservationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaAttendanceReservationAdapter(entityManager);
    }

    @Test
    void shouldReturnTrueWhenReservationExists() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("activityId", activityId)).thenReturn(query);
        when(query.setParameter("userId", userId)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(Boolean.TRUE);

        boolean result = adapter.existsReservation(activityId, userId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sqlCaptor.capture());
        verify(query).setParameter("activityId", activityId);
        verify(query).setParameter("userId", userId);
        String normalizedSql = sqlCaptor.getValue().toLowerCase();
        assertThat(normalizedSql).contains("from reservations");
        assertThat(normalizedSql).contains("activity_id");
        assertThat(normalizedSql).contains("user_id");
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenReservationDoesNotExist() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("activityId", activityId)).thenReturn(query);
        when(query.setParameter("userId", userId)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(Boolean.FALSE);

        boolean result = adapter.existsReservation(activityId, userId);

        assertThat(result).isFalse();
    }
}
