package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import ayd2.p2b.conference_service_api.feature.report.dto.response.CongressByInstitutionItem;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query.JpaCongressesByInstitutionQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaCongressesByInstitutionQuery.class)
class CongressesByInstitutionQueryTest {

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
    private JpaCongressesByInstitutionQuery query;

    @Autowired
    private CongressRepository congressRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Test
    void no_date_filter_returns_all_congresses() {
        InstitutionEntity inst1 = persistInstitution("Alfa");
        InstitutionEntity inst2 = persistInstitution("Beta");
        persistCongress(inst1.getId(), "C1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5));
        persistCongress(inst2.getId(), "C2", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3));
        persistCongress(inst1.getId(), "C3", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5));

        List<CongressByInstitutionItem> result = query.query(null, null);

        assertThat(result).hasSize(3);
    }

    @Test
    void dateFrom_filter_excludes_earlier_congresses() {
        InstitutionEntity inst = persistInstitution("Gamma");
        persistCongress(inst.getId(), "Early", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3));
        persistCongress(inst.getId(), "Late", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3));

        List<CongressByInstitutionItem> result = query.query(LocalDate.of(2026, 1, 1), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCongressName()).isEqualTo("Late");
    }

    @Test
    void dateTo_filter_excludes_later_congresses() {
        InstitutionEntity inst = persistInstitution("Delta");
        persistCongress(inst.getId(), "Early", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 3));
        persistCongress(inst.getId(), "Late", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 5));

        List<CongressByInstitutionItem> result = query.query(null, LocalDate.of(2025, 12, 31));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCongressName()).isEqualTo("Early");
    }

    @Test
    void both_filters_return_intersection() {
        InstitutionEntity inst = persistInstitution("Epsilon");
        persistCongress(inst.getId(), "Before", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3));
        persistCongress(inst.getId(), "Within", LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 3));
        persistCongress(inst.getId(), "After", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 5));

        List<CongressByInstitutionItem> result = query.query(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCongressName()).isEqualTo("Within");
    }

    @Test
    void results_sorted_by_institution_name_then_start_date() {
        InstitutionEntity instA = persistInstitution("Alpha");
        InstitutionEntity instZ = persistInstitution("Zeta");
        persistCongress(instZ.getId(), "ZetaConf1", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5));
        persistCongress(instA.getId(), "AlphaConf2", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5));
        persistCongress(instA.getId(), "AlphaConf1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5));

        List<CongressByInstitutionItem> result = query.query(null, null);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getInstitutionName()).isEqualTo("Alpha");
        assertThat(result.get(0).getCongressName()).isEqualTo("AlphaConf1");
        assertThat(result.get(1).getCongressName()).isEqualTo("AlphaConf2");
        assertThat(result.get(2).getInstitutionName()).isEqualTo("Zeta");
    }

    private InstitutionEntity persistInstitution(String name) {
        InstitutionEntity inst = new InstitutionEntity();
        inst.setName(name);
        inst.setDescription("Desc " + name);
        inst.setContactEmail(name.toLowerCase() + "@test.com");
        inst.setActive(true);
        inst.setCreatedBy(UUID.randomUUID());
        return institutionRepository.saveAndFlush(inst);
    }

    private CongressEntity persistCongress(UUID institutionId, String name, LocalDate startDate, LocalDate endDate) {
        CongressEntity congress = new CongressEntity();
        congress.setInstitutionId(institutionId);
        congress.setName(name);
        congress.setDescription("Desc " + name);
        congress.setStartDate(startDate);
        congress.setEndDate(endDate);
        congress.setLocation("Guatemala");
        congress.setPrice(new BigDecimal("50.00"));
        congress.setCreatedBy(UUID.randomUUID());
        return congressRepository.saveAndFlush(congress);
    }
}
