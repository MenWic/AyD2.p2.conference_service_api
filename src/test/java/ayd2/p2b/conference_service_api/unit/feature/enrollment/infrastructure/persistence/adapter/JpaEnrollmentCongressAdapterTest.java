package ayd2.p2b.conference_service_api.unit.feature.enrollment.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.CongressEnrollmentSummary;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.adapter.JpaEnrollmentCongressAdapter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaEnrollmentCongressAdapterTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private JpaEnrollmentCongressAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaEnrollmentCongressAdapter(entityManager);
    }

    @Test
    void shouldReturnPublicEnrollmentCongressSummaryWhenFound() {
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        BigDecimal price = new BigDecimal("75.00");

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                congressId, institutionId, createdBy, "Congreso A", "Institucion A", price
        }));

        Optional<CongressEnrollmentSummary> result = adapter.findPublicEnrollmentCongressById(congressId);

        assertThat(result).isPresent();
        CongressEnrollmentSummary summary = result.orElseThrow();
        assertThat(summary.getCongressId()).isEqualTo(congressId);
        assertThat(summary.getInstitutionId()).isEqualTo(institutionId);
        assertThat(summary.getCreatedBy()).isEqualTo(createdBy);
        assertThat(summary.getCongressName()).isEqualTo("Congreso A");
        assertThat(summary.getInstitutionName()).isEqualTo("Institucion A");
        assertThat(summary.getPrice()).isEqualTo(price);
        verify(query).setParameter("congressId", congressId);
    }

    @Test
    void shouldReturnEmptyWhenPublicEnrollmentCongressIsMissing() {
        UUID congressId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        assertThat(adapter.findPublicEnrollmentCongressById(congressId)).isEmpty();
        verify(query).setParameter("congressId", congressId);
    }

    @Test
    void shouldReturnManageableCongressSummaryWhenFound() {
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        BigDecimal price = new BigDecimal("120.00");

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{
                congressId, institutionId, createdBy, "Congreso B", "Institucion B", price
        }));

        Optional<CongressEnrollmentSummary> result = adapter.findManageableCongressById(congressId);

        assertThat(result).isPresent();
        CongressEnrollmentSummary summary = result.orElseThrow();
        assertThat(summary.getCongressId()).isEqualTo(congressId);
        assertThat(summary.getInstitutionId()).isEqualTo(institutionId);
        assertThat(summary.getCreatedBy()).isEqualTo(createdBy);
        assertThat(summary.getCongressName()).isEqualTo("Congreso B");
        assertThat(summary.getInstitutionName()).isEqualTo("Institucion B");
        assertThat(summary.getPrice()).isEqualTo(price);
        verify(query).setParameter("congressId", congressId);
    }
}
