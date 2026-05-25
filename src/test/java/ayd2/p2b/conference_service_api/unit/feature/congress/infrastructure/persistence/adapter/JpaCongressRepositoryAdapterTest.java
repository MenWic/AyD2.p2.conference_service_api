package ayd2.p2b.conference_service_api.unit.feature.congress.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressSearchCriteria;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressView;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.adapter.JpaCongressRepositoryAdapter;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.congress.mapper.CongressMapper;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaCongressRepositoryAdapterTest {

    @Mock
    private CongressRepository congressRepository;
    @Mock
    private CongressMapper congressMapper;

    private JpaCongressRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaCongressRepositoryAdapter(congressRepository, congressMapper);
    }

    @Test
    void shouldSaveCongressThroughRepositoryAndMapper() {
        Congress congress = sampleCongress();
        CongressEntity entity = new CongressEntity();
        CongressEntity savedEntity = new CongressEntity();
        savedEntity.setId(congress.getId());

        when(congressMapper.toEntity(congress)).thenReturn(entity);
        when(congressRepository.save(entity)).thenReturn(savedEntity);
        when(congressMapper.toDomain(savedEntity)).thenReturn(congress);

        Congress result = adapter.save(congress);

        assertThat(result).isEqualTo(congress);
    }

    @Test
    void shouldFindByIdWhenPresent() {
        UUID congressId = UUID.randomUUID();
        CongressEntity entity = new CongressEntity();
        Congress congress = sampleCongress();

        when(congressRepository.findById(congressId)).thenReturn(Optional.of(entity));
        when(congressMapper.toDomain(entity)).thenReturn(congress);

        assertThat(adapter.findById(congressId)).contains(congress);
    }

    @Test
    void shouldReturnEmptyWhenFindByIdIsMissing() {
        UUID congressId = UUID.randomUUID();
        when(congressRepository.findById(congressId)).thenReturn(Optional.empty());

        assertThat(adapter.findById(congressId)).isEmpty();
    }

    @Test
    void shouldMapPublicByIdToCongressViewIncludingInstitutionName() {
        UUID congressId = UUID.randomUUID();
        CongressEntity entity = new CongressEntity();
        InstitutionEntity institution = new InstitutionEntity();
        institution.setName("USAC");
        entity.setInstitution(institution);
        Congress congress = sampleCongress();

        when(congressRepository.findPublicById(congressId)).thenReturn(Optional.of(entity));
        when(congressMapper.toDomain(entity)).thenReturn(congress);

        Optional<CongressView> result = adapter.findPublicById(congressId);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getCongress()).isEqualTo(congress);
        assertThat(result.orElseThrow().getInstitutionName()).isEqualTo("USAC");
    }

    @Test
    void shouldFindPublicByCriteriaAndMapPageContent() {
        Pageable pageable = PageRequest.of(0, 1);
        CongressSearchCriteria criteria = CongressSearchCriteria.builder()
                .search("tech")
                .build();
        CongressEntity entity = new CongressEntity();
        InstitutionEntity institution = new InstitutionEntity();
        institution.setName("UNI");
        entity.setInstitution(institution);
        Congress congress = sampleCongress();

        when(congressRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 6));
        when(congressMapper.toDomain(entity)).thenReturn(congress);

        Page<CongressView> result = adapter.findPublicByCriteria(criteria, pageable);

        ArgumentCaptor<Specification<CongressEntity>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(congressRepository).findAll(captor.capture(), eq(pageable));
        assertThat(captor.getValue()).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getCongress()).isEqualTo(congress);
        assertThat(result.getContent().getFirst().getInstitutionName()).isEqualTo("UNI");
        assertThat(result.getTotalElements()).isEqualTo(6);
    }

    @Test
    void shouldDeleteById() {
        UUID congressId = UUID.randomUUID();

        adapter.deleteById(congressId);

        verify(congressRepository).deleteById(congressId);
    }

    private Congress sampleCongress() {
        return Congress.builder()
                .id(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .name("Congress")
                .description("Description")
                .startDate(LocalDate.of(2026, 10, 10))
                .endDate(LocalDate.of(2026, 10, 12))
                .location("Guatemala")
                .price(new BigDecimal("50.00"))
                .createdBy(UUID.randomUUID())
                .build();
    }
}
