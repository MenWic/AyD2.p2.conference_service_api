package ayd2.p2b.conference_service_api.unit.feature.proposal.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallStatus;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallSummary;
import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.adapter.JpaProposalCallAdapter;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaProposalCallAdapterTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private JpaProposalCallAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaProposalCallAdapter(entityManager);
    }

    @Test
    void shouldReturnVisibleCallSummaryWhenFound() {
        UUID callId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID congressCreatedBy = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("callId", callId)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                callId, congressId, institutionId, congressCreatedBy, "OPEN"
        }));

        Optional<ProposalCallSummary> result = adapter.findVisibleCallById(callId);

        assertThat(result).isPresent();
        ProposalCallSummary summary = result.orElseThrow();
        assertThat(summary.getCallId()).isEqualTo(callId);
        assertThat(summary.getCongressId()).isEqualTo(congressId);
        assertThat(summary.getInstitutionId()).isEqualTo(institutionId);
        assertThat(summary.getCongressCreatedBy()).isEqualTo(congressCreatedBy);
        assertThat(summary.getStatus()).isEqualTo(ProposalCallStatus.OPEN);
        verify(query).setParameter("callId", callId);
    }

    @Test
    void shouldReturnEmptyWhenVisibleCallIsMissing() {
        UUID callId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("callId", callId)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        assertThat(adapter.findVisibleCallById(callId)).isEmpty();
        verify(query).setParameter("callId", callId);
    }

    @Test
    void shouldHandleNullStatusAsNull() {
        UUID callId = UUID.randomUUID();
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID congressCreatedBy = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("callId", callId)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                callId, congressId, institutionId, congressCreatedBy, null
        }));

        Optional<ProposalCallSummary> result = adapter.findVisibleCallById(callId);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getStatus()).isNull();
    }
}
