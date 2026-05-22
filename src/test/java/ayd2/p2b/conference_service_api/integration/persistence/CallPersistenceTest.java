package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.call.domain.model.CallStatus;
import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.entity.CallEntity;
import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.repository.CallRepository;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CallPersistenceTest {

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
    private CallRepository callRepository;

    @Autowired
    private CongressRepository congressRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldEnforceSingleOpenCallByPartialUniqueIndex() {
        CongressEntity congress = persistedCongress("USAC");
        callRepository.saveAndFlush(newCall(congress.getId(), CallStatus.OPEN, null));

        assertThatThrownBy(() -> callRepository.saveAndFlush(newCall(congress.getId(), CallStatus.OPEN, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowClosedCallAfterOpenCall() {
        CongressEntity congress = persistedCongress("URL");
        callRepository.saveAndFlush(newCall(congress.getId(), CallStatus.OPEN, null));

        CallEntity closed = callRepository.saveAndFlush(
                newCall(congress.getId(), CallStatus.CLOSED, OffsetDateTime.parse("2026-10-10T16:00:00Z"))
        );

        assertThat(closed.getId()).isNotNull();
        assertThat(callRepository.findAll()).hasSize(2);
    }

    @Test
    void shouldEnforceClosedAtCheckConstraint() {
        CongressEntity congress = persistedCongress("Del Valle");

        assertThatThrownBy(() -> callRepository.saveAndFlush(
                newCall(congress.getId(), CallStatus.OPEN, OffsetDateTime.parse("2026-10-10T16:00:00Z"))
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> callRepository.saveAndFlush(
                newCall(congress.getId(), CallStatus.CLOSED, null)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceCallStatusCheckConstraint() {
        CongressEntity congress = persistedCongress("Mariano");

        assertThatThrownBy(() -> {
            entityManager.createNativeQuery("""
                            insert into calls (id, congress_id, status, opened_at, created_by)
                            values (:id, :congressId, :status, :openedAt, :createdBy)
                            """)
                    .setParameter("id", UUID.randomUUID())
                    .setParameter("congressId", congress.getId())
                    .setParameter("status", "INVALID")
                    .setParameter("openedAt", OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                    .setParameter("createdBy", UUID.randomUUID())
                    .executeUpdate();
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, jakarta.persistence.PersistenceException.class);
    }

    private CongressEntity persistedCongress(String institutionName) {
        InstitutionEntity institution = new InstitutionEntity();
        institution.setName(institutionName);
        institution.setDescription("Institution " + institutionName);
        institution.setContactEmail(institutionName.toLowerCase().replace(" ", "") + "@example.com");
        institution.setActive(true);
        institution.setCreatedBy(UUID.randomUUID());
        institutionRepository.saveAndFlush(institution);

        CongressEntity congress = new CongressEntity();
        congress.setInstitutionId(institution.getId());
        congress.setName("Congreso " + institutionName);
        congress.setDescription("Descripcion " + institutionName);
        congress.setStartDate(LocalDate.of(2026, 10, 10));
        congress.setEndDate(LocalDate.of(2026, 10, 12));
        congress.setLocation("Guatemala");
        congress.setPrice(new BigDecimal("45.00"));
        congress.setCreatedBy(UUID.randomUUID());
        return congressRepository.saveAndFlush(congress);
    }

    private CallEntity newCall(UUID congressId, CallStatus status, OffsetDateTime closedAt) {
        CallEntity call = new CallEntity();
        call.setCongressId(congressId);
        call.setStatus(status);
        call.setOpenedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"));
        call.setClosedAt(closedAt);
        call.setCreatedBy(UUID.randomUUID());
        return call;
    }
}
