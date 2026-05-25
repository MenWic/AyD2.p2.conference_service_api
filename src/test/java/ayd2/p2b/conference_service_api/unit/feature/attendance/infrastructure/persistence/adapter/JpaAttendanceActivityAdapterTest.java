package ayd2.p2b.conference_service_api.unit.feature.attendance.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceActivitySummary;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.adapter.JpaAttendanceActivityAdapter;
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
class JpaAttendanceActivityAdapterTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private JpaAttendanceActivityAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaAttendanceActivityAdapter(entityManager);
    }

    @Test
    void shouldReturnActivitySummaryWhenFound() {
        UUID activityId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID congressCreatedBy = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("activityId", activityId)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                activityId, congressId, institutionId, congressCreatedBy, roomId, "PONENCIA"
        }));

        Optional<AttendanceActivitySummary> result = adapter.findActivityById(activityId);

        assertThat(result).isPresent();
        AttendanceActivitySummary summary = result.orElseThrow();
        assertThat(summary.getActivityId()).isEqualTo(activityId);
        assertThat(summary.getCongressId()).isEqualTo(congressId);
        assertThat(summary.getInstitutionId()).isEqualTo(institutionId);
        assertThat(summary.getCongressCreatedBy()).isEqualTo(congressCreatedBy);
        assertThat(summary.getRoomId()).isEqualTo(roomId);
        assertThat(summary.getType()).isEqualTo(ActivityType.PONENCIA);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sqlCaptor.capture());
        verify(query).setParameter("activityId", activityId);
        String normalizedSql = sqlCaptor.getValue().toLowerCase();
        assertThat(normalizedSql).contains("from activities a");
        assertThat(normalizedSql).contains("join congresses c");
        assertThat(normalizedSql).contains("join institutions i");
        assertThat(normalizedSql).contains("i.active = true");
    }

    @Test
    void shouldReturnEmptyWhenActivityIsMissing() {
        UUID activityId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("activityId", activityId)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        Optional<AttendanceActivitySummary> result = adapter.findActivityById(activityId);

        assertThat(result).isEmpty();
    }
}
