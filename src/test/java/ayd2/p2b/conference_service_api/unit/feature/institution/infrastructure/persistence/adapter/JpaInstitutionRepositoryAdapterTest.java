package ayd2.p2b.conference_service_api.unit.feature.institution.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.adapter.JpaInstitutionRepositoryAdapter;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import ayd2.p2b.conference_service_api.feature.institution.mapper.InstitutionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaInstitutionRepositoryAdapterTest {

    @Mock
    private InstitutionRepository institutionRepository;
    @Mock
    private InstitutionMapper institutionMapper;

    private JpaInstitutionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaInstitutionRepositoryAdapter(institutionRepository, institutionMapper);
    }

    @Test
    void shouldDelegateNameExistenceQueries() {
        UUID id = UUID.randomUUID();
        when(institutionRepository.existsByName("USAC")).thenReturn(true);
        when(institutionRepository.existsByNameAndIdNot("USAC", id)).thenReturn(false);

        assertThat(adapter.existsByName("USAC")).isTrue();
        assertThat(adapter.existsByNameAndIdNot("USAC", id)).isFalse();
    }

    @Test
    void shouldMapAndPersistInstitution() {
        Institution domain = Institution.builder().id(UUID.randomUUID()).name("USAC").build();
        InstitutionEntity entity = new InstitutionEntity();
        entity.setId(domain.getId());
        entity.setName("USAC");

        when(institutionMapper.toEntity(domain)).thenReturn(entity);
        when(institutionRepository.save(entity)).thenReturn(entity);
        when(institutionMapper.toDomain(entity)).thenReturn(domain);

        Institution saved = adapter.save(domain);

        assertThat(saved).isEqualTo(domain);
        verify(institutionRepository).save(entity);
    }

    @Test
    void shouldMapFindByIdAndActiveQueries() {
        UUID id = UUID.randomUUID();
        InstitutionEntity entity = new InstitutionEntity();
        entity.setId(id);
        Institution domain = Institution.builder().id(id).name("USAC").build();

        when(institutionRepository.findById(id)).thenReturn(Optional.of(entity));
        when(institutionRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(entity));
        when(institutionMapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById(id)).contains(domain);
        assertThat(adapter.findActiveById(id)).contains(domain);
    }

    @Test
    void shouldMapActiveInstitutionPage() {
        UUID id = UUID.randomUUID();
        InstitutionEntity entity = new InstitutionEntity();
        entity.setId(id);
        Institution domain = Institution.builder().id(id).name("USAC").build();
        PageRequest pageable = PageRequest.of(0, 20);

        when(institutionRepository.findAllByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(institutionMapper.toDomain(any(InstitutionEntity.class))).thenReturn(domain);

        var page = adapter.findAllActive(pageable);

        assertThat(page.getContent()).containsExactly(domain);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}
