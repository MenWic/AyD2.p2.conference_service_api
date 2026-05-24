package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InstitutionPersistenceTest {

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
    private InstitutionRepository institutionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldEnforceUniqueInstitutionNameConstraint() {
        InstitutionEntity first = newInstitution("USAC", true);
        InstitutionEntity second = newInstitution("USAC", true);
        institutionRepository.saveAndFlush(first);

        assertThatThrownBy(() -> institutionRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldDefaultInstitutionAsActiveAndPersistAuditFields() {
        InstitutionEntity entity = newInstitution("URL", true);
        entity.setActive(true);

        InstitutionEntity saved = institutionRepository.saveAndFlush(entity);

        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isNotNull();
    }

    @Test
    void shouldPersistSoftDeleteAsActiveFalse() {
        InstitutionEntity saved = institutionRepository.saveAndFlush(newInstitution("Mariano", true));
        saved.setActive(false);
        saved.setUpdatedBy(UUID.randomUUID());
        institutionRepository.saveAndFlush(saved);

        InstitutionEntity reloaded = institutionRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.isActive()).isFalse();
    }

    @Test
    void shouldDetectCongressDependencyByInstitutionId() {
        InstitutionEntity institution = institutionRepository.saveAndFlush(newInstitution("Del Valle", true));
        jdbcTemplate.update(
                """
                INSERT INTO congresses (
                    id, institution_id, name, description, start_date, end_date, location, price, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                institution.getId(),
                "Congreso de Prueba",
                "Descripcion",
                Date.valueOf("2026-06-10"),
                Date.valueOf("2026-06-12"),
                "Guatemala",
                new BigDecimal("35.00"),
                UUID.randomUUID()
        );

        boolean hasDependency = institutionRepository.existsCongressesByInstitutionId(institution.getId());

        assertThat(hasDependency).isTrue();
    }

    @Test
    void shouldReturnOnlyActiveInstitutionsInActiveQuery() {
        InstitutionEntity active = institutionRepository.saveAndFlush(newInstitution("Galileo", true));
        InstitutionEntity inactive = institutionRepository.saveAndFlush(newInstitution("Landivar", false));
        assertThat(active.isActive()).isTrue();
        assertThat(inactive.isActive()).isFalse();

        var page = institutionRepository.findAllByActiveTrue(PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(InstitutionEntity::isActive).containsOnly(true);
        assertThat(page.getContent()).extracting(InstitutionEntity::getName).contains("Galileo");
        assertThat(page.getContent()).extracting(InstitutionEntity::getName).doesNotContain("Landivar");
    }

    private InstitutionEntity newInstitution(String name, boolean active) {
        InstitutionEntity entity = new InstitutionEntity();
        entity.setName(name);
        entity.setDescription("Institution " + name);
        entity.setContactEmail(name.toLowerCase() + "@example.com");
        entity.setActive(active);
        entity.setCreatedBy(UUID.randomUUID());
        return entity;
    }
}
