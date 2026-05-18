package ayd2.p2b.conference_service_api.unit.feature.congress.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.congress.dto.internal.InstitutionSummary;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.adapter.JpaCongressInstitutionAdapter;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaCongressInstitutionAdapterTest {

    @Mock
    private InstitutionRepository institutionRepository;

    private JpaCongressInstitutionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaCongressInstitutionAdapter(institutionRepository);
    }

    @Test
    void shouldReturnInstitutionSummaryForActiveInstitution() {
        UUID institutionId = UUID.randomUUID();
        InstitutionEntity entity = new InstitutionEntity();
        entity.setId(institutionId);
        entity.setName("USAC");

        when(institutionRepository.findByIdAndActiveTrue(institutionId)).thenReturn(Optional.of(entity));

        Optional<InstitutionSummary> result = adapter.findActiveInstitutionById(institutionId);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(institutionId);
        assertThat(result.orElseThrow().getName()).isEqualTo("USAC");
    }

    @Test
    void shouldReturnEmptyWhenInstitutionIsMissingOrInactive() {
        UUID institutionId = UUID.randomUUID();
        when(institutionRepository.findByIdAndActiveTrue(institutionId)).thenReturn(Optional.empty());

        assertThat(adapter.findActiveInstitutionById(institutionId)).isEmpty();
    }
}
