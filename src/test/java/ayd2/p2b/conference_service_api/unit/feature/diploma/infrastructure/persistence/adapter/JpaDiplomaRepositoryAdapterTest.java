package ayd2.p2b.conference_service_api.unit.feature.diploma.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.diploma.domain.model.Diploma;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.adapter.JpaDiplomaRepositoryAdapter;
import ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.entity.DiplomaEntity;
import ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.repository.DiplomaRepository;
import ayd2.p2b.conference_service_api.feature.diploma.mapper.DiplomaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaDiplomaRepositoryAdapterTest {

    @Mock
    private DiplomaRepository diplomaRepository;
    @Mock
    private DiplomaMapper diplomaMapper;

    private JpaDiplomaRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaDiplomaRepositoryAdapter(diplomaRepository, diplomaMapper);
    }

    @Test
    void shouldSaveDiplomaUsingSaveAndFlushAndMapBackToDomain() {
        Diploma diploma = sampleDiploma();
        DiplomaEntity entity = new DiplomaEntity();
        DiplomaEntity persisted = new DiplomaEntity();
        persisted.setId(diploma.getId());

        when(diplomaMapper.toEntity(diploma)).thenReturn(entity);
        when(diplomaRepository.saveAndFlush(entity)).thenReturn(persisted);
        when(diplomaMapper.toDomain(persisted)).thenReturn(diploma);

        Diploma result = adapter.save(diploma);

        assertThat(result).isEqualTo(diploma);
        verify(diplomaRepository).saveAndFlush(entity);
        verify(diplomaRepository, never()).save(entity);
    }

    private Diploma sampleDiploma() {
        return Diploma.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .congressId(UUID.randomUUID())
                .type(DiplomaType.PARTICIPATION)
                .issuedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .createdBy(UUID.randomUUID())
                .createdAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .build();
    }
}
