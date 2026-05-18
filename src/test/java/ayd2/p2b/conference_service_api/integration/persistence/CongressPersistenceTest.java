package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressSearchCriteria;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.specification.CongressSpecification;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CongressPersistenceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private CongressRepository congressRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Test
    void shouldEnforceCongressPriceCheckConstraint() {
        InstitutionEntity institution = institutionRepository.saveAndFlush(newInstitution("USAC", true));
        CongressEntity congress = newCongress(institution.getId(), "Congreso Precio", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12));
        congress.setPrice(new BigDecimal("34.99"));

        assertThatThrownBy(() -> congressRepository.saveAndFlush(congress))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceCongressDateCheckConstraint() {
        InstitutionEntity institution = institutionRepository.saveAndFlush(newInstitution("URL", true));
        CongressEntity congress = newCongress(institution.getId(), "Congreso Fechas", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 10));

        assertThatThrownBy(() -> congressRepository.saveAndFlush(congress))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFilterByInstitutionDateAndSearchAndExcludeInactiveInstitutions() {
        InstitutionEntity activeInstitution = institutionRepository.saveAndFlush(newInstitution("Del Valle", true));
        InstitutionEntity inactiveInstitution = institutionRepository.saveAndFlush(newInstitution("Mariano", false));

        congressRepository.saveAndFlush(newCongress(
                activeInstitution.getId(),
                "Congreso IA",
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 12)
        ));
        congressRepository.saveAndFlush(newCongress(
                inactiveInstitution.getId(),
                "Congreso Inactivo",
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 12)
        ));

        CongressSearchCriteria criteria = CongressSearchCriteria.builder()
                .institutionId(activeInstitution.getId())
                .startDateFrom(LocalDate.of(2026, 10, 1))
                .startDateTo(LocalDate.of(2026, 10, 31))
                .search("ia")
                .build();

        var page = congressRepository.findAll(CongressSpecification.publicCriteria(criteria), PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(CongressEntity::getName).containsExactly("Congreso IA");
    }

    private InstitutionEntity newInstitution(String name, boolean active) {
        InstitutionEntity institution = new InstitutionEntity();
        institution.setName(name);
        institution.setDescription("Institution " + name);
        institution.setContactEmail(name.toLowerCase().replace(" ", "") + "@example.com");
        institution.setActive(active);
        institution.setCreatedBy(UUID.randomUUID());
        return institution;
    }

    private CongressEntity newCongress(UUID institutionId, String name, LocalDate startDate, LocalDate endDate) {
        CongressEntity congress = new CongressEntity();
        congress.setInstitutionId(institutionId);
        congress.setName(name);
        congress.setDescription("Descripcion " + name);
        congress.setStartDate(startDate);
        congress.setEndDate(endDate);
        congress.setLocation("Guatemala");
        congress.setPrice(new BigDecimal("45.00"));
        congress.setCreatedBy(UUID.randomUUID());
        return congress;
    }
}
