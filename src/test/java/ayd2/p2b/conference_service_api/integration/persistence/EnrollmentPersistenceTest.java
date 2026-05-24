package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentEntity;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentJpaRepository;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EnrollmentPersistenceTest {

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
  private EnrollmentJpaRepository enrollmentJpaRepository;
  @Autowired
  private CongressRepository congressRepository;
  @Autowired
  private InstitutionRepository institutionRepository;

  @Test
  void shouldEnforceUniqueCongressUserEnrollment() {
    CongressEntity congress = persistedCongress("USAC");
    UUID userId = UUID.randomUUID();

    enrollmentJpaRepository.saveAndFlush(newEnrollment(congress.getId(), userId, UUID.randomUUID()));

    assertThatThrownBy(() -> enrollmentJpaRepository.saveAndFlush(
        newEnrollment(congress.getId(), userId, UUID.randomUUID())))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldRequirePaymentId() {
    CongressEntity congress = persistedCongress("URL");
    EnrollmentEntity enrollment = newEnrollment(congress.getId(), UUID.randomUUID(), null);

    assertThatThrownBy(() -> enrollmentJpaRepository.saveAndFlush(enrollment))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private CongressEntity persistedCongress(String institutionName) {
    InstitutionEntity institution = new InstitutionEntity();
    institution.setName(institutionName);
    institution.setDescription("Institution " + institutionName);
    institution.setContactEmail(institutionName.toLowerCase() + "@example.com");
    institution.setActive(true);
    institution.setCreatedBy(UUID.randomUUID());
    institutionRepository.saveAndFlush(institution);

    CongressEntity congress = new CongressEntity();
    congress.setInstitutionId(institution.getId());
    congress.setName("Congreso " + institutionName);
    congress.setDescription("Description");
    congress.setStartDate(LocalDate.of(2026, 10, 10));
    congress.setEndDate(LocalDate.of(2026, 10, 12));
    congress.setLocation("Guatemala");
    congress.setPrice(new BigDecimal("45.00"));
    congress.setCreatedBy(UUID.randomUUID());
    return congressRepository.saveAndFlush(congress);
  }

  private EnrollmentEntity newEnrollment(UUID congressId, UUID userId, UUID paymentId) {
    EnrollmentEntity enrollment = new EnrollmentEntity();
    enrollment.setCongressId(congressId);
    enrollment.setUserId(userId);
    enrollment.setPaymentId(paymentId);
    enrollment.setPaymentDate(LocalDate.of(2026, 6, 15));
    enrollment.setCreatedBy(userId);
    return enrollment;
  }
}
