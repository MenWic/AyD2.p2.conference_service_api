package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.entity.CommitteeMemberEntity;
import ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.repository.CommitteeMemberRepository;
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
class CommitteePersistenceTest {

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
    private CommitteeMemberRepository committeeMemberRepository;

    @Autowired
    private CongressRepository congressRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldEnforceCompositePrimaryKeyPerCongressAndUser() {
        CongressEntity congress = persistedCongress("USAC");
        UUID userId = UUID.randomUUID();
        committeeMemberRepository.saveAndFlush(newMember(congress.getId(), userId));

        assertThatThrownBy(() -> {
            entityManager.createNativeQuery("""
                            insert into committee_members (congress_id, user_id, added_at, added_by)
                            values (:congressId, :userId, :addedAt, :addedBy)
                            """)
                    .setParameter("congressId", congress.getId())
                    .setParameter("userId", userId)
                    .setParameter("addedAt", OffsetDateTime.parse("2026-10-10T12:00:00Z"))
                    .setParameter("addedBy", UUID.randomUUID())
                    .executeUpdate();
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, jakarta.persistence.PersistenceException.class);
    }

    @Test
    void shouldDeleteOnlyMembershipWithoutDeletingCongress() {
        CongressEntity congress = persistedCongress("URL");
        UUID userId = UUID.randomUUID();
        committeeMemberRepository.saveAndFlush(newMember(congress.getId(), userId));

        committeeMemberRepository.deleteByCongressIdAndUserId(congress.getId(), userId);
        committeeMemberRepository.flush();

        assertThat(committeeMemberRepository.existsByCongressIdAndUserId(congress.getId(), userId)).isFalse();
        assertThat(congressRepository.existsById(congress.getId())).isTrue();
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

    private CommitteeMemberEntity newMember(UUID congressId, UUID userId) {
        CommitteeMemberEntity member = new CommitteeMemberEntity();
        member.setCongressId(congressId);
        member.setUserId(userId);
        member.setAddedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"));
        member.setAddedBy(UUID.randomUUID());
        return member;
    }
}
