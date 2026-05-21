package ayd2.p2b.conference_service_api.unit.feature.activity.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityCongressRoomSummary;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.adapter.JpaActivityCongressRoomAdapter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaActivityCongressRoomAdapterTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private JpaActivityCongressRoomAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaActivityCongressRoomAdapter(entityManager);
    }

    @Test
    void shouldReturnManageableCongressSummaryWhenFound() {
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{congressId, institutionId, createdBy}));

        Optional<ActivityCongressRoomSummary> result = adapter.findManageableCongressById(congressId);

        assertThat(result).isPresent();
        ActivityCongressRoomSummary summary = result.orElseThrow();
        assertThat(summary.getCongressId()).isEqualTo(congressId);
        assertThat(summary.getInstitutionId()).isEqualTo(institutionId);
        assertThat(summary.getCreatedBy()).isEqualTo(createdBy);
        assertThat(summary.getRoomId()).isNull();
    }

    @Test
    void shouldReturnEmptyWhenManageableCongressIsMissing() {
        UUID congressId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        assertThat(adapter.findManageableCongressById(congressId)).isEmpty();
    }

    @Test
    void shouldReturnManageableRoomSummaryWhenFound() {
        UUID roomId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("roomId", roomId)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{roomId, congressId, institutionId, createdBy}));

        Optional<ActivityCongressRoomSummary> result = adapter.findManageableRoomById(roomId);

        assertThat(result).isPresent();
        ActivityCongressRoomSummary summary = result.orElseThrow();
        assertThat(summary.getRoomId()).isEqualTo(roomId);
        assertThat(summary.getCongressId()).isEqualTo(congressId);
        assertThat(summary.getInstitutionId()).isEqualTo(institutionId);
        assertThat(summary.getCreatedBy()).isEqualTo(createdBy);
    }

    @Test
    void shouldReturnEmptyWhenManageableRoomIsMissing() {
        UUID roomId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("roomId", roomId)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        assertThat(adapter.findManageableRoomById(roomId)).isEmpty();
    }

    @Test
    void shouldReturnPublicCongressExistsFlag() {
        UUID congressId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(Boolean.TRUE);

        assertThat(adapter.existsPublicCongressById(congressId)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenPublicCongressDoesNotExist() {
        UUID congressId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(Boolean.FALSE);

        assertThat(adapter.existsPublicCongressById(congressId)).isFalse();
    }
}
