package ayd2.p2b.conference_service_api.unit.feature.institution.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.adapter.JpaInstitutionDependencyAdapter;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaInstitutionDependencyAdapterTest {

    @Mock
    private InstitutionRepository institutionRepository;

    private JpaInstitutionDependencyAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaInstitutionDependencyAdapter(institutionRepository);
    }

    @Test
    void shouldDelegateCongressDependencyLookup() {
        UUID institutionId = UUID.randomUUID();
        when(institutionRepository.existsCongressesByInstitutionId(institutionId)).thenReturn(true);

        assertThat(adapter.hasCongressesByInstitutionId(institutionId)).isTrue();
    }
}
