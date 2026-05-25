package ayd2.p2b.conference_service_api.unit.feature.congress;

import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

    private Congress buildCongress(UUID institutionId) {
        return Congress.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .name("Test Congress")
                .description("Desc")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .location("City")
                .price(new BigDecimal("100.00"))
                .createdBy(UUID.randomUUID())
                .build();
    }

    private CongressEntity buildEntity(String institutionName) {
        InstitutionEntity institution = new InstitutionEntity();
        institution.setName(institutionName);

        CongressEntity entity = new CongressEntity();
        entity.setId(UUID.randomUUID());
        entity.setInstitution(institution);
        return entity;
    }

    @Test
    void save_delegates_to_repository_and_mapper() {
        UUID institutionId = UUID.randomUUID();
        Congress congress = buildCongress(institutionId);
        CongressEntity entity = new CongressEntity();
        CongressEntity saved = new CongressEntity();
        given(congressMapper.toEntity(congress)).willReturn(entity);
        given(congressRepository.save(entity)).willReturn(saved);
        given(congressMapper.toDomain(saved)).willReturn(congress);

        Congress result = adapter.save(congress);

        assertThat(result).isEqualTo(congress);
        verify(congressRepository).save(entity);
    }

    @Test
    void findById_returns_mapped_when_present() {
        UUID congressId = UUID.randomUUID();
        CongressEntity entity = new CongressEntity();
        Congress expected = buildCongress(UUID.randomUUID());
        given(congressRepository.findById(congressId)).willReturn(Optional.of(entity));
        given(congressMapper.toDomain(entity)).willReturn(expected);

        Optional<Congress> result = adapter.findById(congressId);

        assertThat(result).isPresent().contains(expected);
    }

    @Test
    void findById_returns_empty_when_not_found() {
        UUID congressId = UUID.randomUUID();
        given(congressRepository.findById(congressId)).willReturn(Optional.empty());

        Optional<Congress> result = adapter.findById(congressId);

        assertThat(result).isEmpty();
    }

    @Test
    void findPublicById_returns_view_with_institution_name_when_present() {
        UUID congressId = UUID.randomUUID();
        CongressEntity entity = buildEntity("Universidad Nacional");
        Congress domain = buildCongress(UUID.randomUUID());
        given(congressRepository.findPublicById(congressId)).willReturn(Optional.of(entity));
        given(congressMapper.toDomain(entity)).willReturn(domain);

        Optional<CongressView> result = adapter.findPublicById(congressId);

        assertThat(result).isPresent();
        assertThat(result.get().getInstitutionName()).isEqualTo("Universidad Nacional");
        assertThat(result.get().getCongress()).isEqualTo(domain);
    }

    @Test
    void findPublicById_returns_empty_when_not_found() {
        UUID congressId = UUID.randomUUID();
        given(congressRepository.findPublicById(congressId)).willReturn(Optional.empty());

        Optional<CongressView> result = adapter.findPublicById(congressId);

        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findPublicByCriteria_returns_mapped_page_of_views() {
        CongressSearchCriteria criteria = CongressSearchCriteria.builder().build();
        Pageable pageable = Pageable.unpaged();
        CongressEntity entity = buildEntity("Test Institution");
        Congress domain = buildCongress(UUID.randomUUID());
        given(congressRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(entity)));
        given(congressMapper.toDomain(entity)).willReturn(domain);

        Page<CongressView> result = adapter.findPublicByCriteria(criteria, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getInstitutionName()).isEqualTo("Test Institution");
        assertThat(result.getContent().get(0).getCongress()).isEqualTo(domain);
    }

    @Test
    void deleteById_delegates_to_repository() {
        UUID congressId = UUID.randomUUID();

        adapter.deleteById(congressId);

        verify(congressRepository).deleteById(congressId);
    }
}
