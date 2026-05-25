package ayd2.p2b.conference_service_api.unit.feature.committee.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.committee.dto.internal.CommitteeCongressSummary;
import ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.adapter.JpaCommitteeCongressAdapter;
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
class JpaCommitteeCongressAdapterTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private JpaCommitteeCongressAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaCommitteeCongressAdapter(entityManager);
    }

    @Test
    void shouldReturnManageableCongressSummaryWhenFound() {
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{congressId, institutionId, createdBy}));

        Optional<CommitteeCongressSummary> result = adapter.findManageableCongressById(congressId);

        assertThat(result).isPresent();
        CommitteeCongressSummary summary = result.orElseThrow();
        assertThat(summary.getCongressId()).isEqualTo(congressId);
        assertThat(summary.getInstitutionId()).isEqualTo(institutionId);
        assertThat(summary.getCreatedBy()).isEqualTo(createdBy);
        verify(query).setParameter("congressId", congressId);
    }

    @Test
    void shouldReturnEmptyWhenManageableCongressIsMissing() {
        UUID congressId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        assertThat(adapter.findManageableCongressById(congressId)).isEmpty();
        verify(query).setParameter("congressId", congressId);
    }
}
