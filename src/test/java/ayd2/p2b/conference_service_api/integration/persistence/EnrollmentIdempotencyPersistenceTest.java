package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyStatus;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentEntity;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentIdempotencyRecordEntity;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentIdempotencyJpaRepository;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentJpaRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EnrollmentIdempotencyPersistenceTest {

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
  private EnrollmentIdempotencyJpaRepository idempotencyRepository;
  @Autowired
  private EnrollmentJpaRepository enrollmentJpaRepository;
  @Autowired
  private CongressRepository congressRepository;
  @Autowired
  private InstitutionRepository institutionRepository;
  @Autowired
  private EntityManager entityManager;

  @Test
  void processingAllowsPaymentIdNullOrPresentAndRequiresSnapshotFields() {
    CongressEntity congress = persistedCongress("USAC");
    UUID userId = UUID.randomUUID();

    idempotencyRepository.saveAndFlush(newRecord("key-processing-null", congress, userId, EnrollmentIdempotencyStatus.PROCESSING, null, null, "100.00"));
    idempotencyRepository.saveAndFlush(newRecord("key-processing-payment", congress, UUID.randomUUID(), EnrollmentIdempotencyStatus.PROCESSING, null, UUID.randomUUID(), "100.00"));

    EnrollmentIdempotencyRecordEntity missingAmount = newRecord(
        "key-missing-amount",
        congress,
        UUID.randomUUID(),
        EnrollmentIdempotencyStatus.PROCESSING,
        null,
        null,
        null);
    assertThatThrownBy(() -> idempotencyRepository.saveAndFlush(missingAmount))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void succeededRequiresEnrollmentIdAndPaymentId() {
    CongressEntity congress = persistedCongress("URL");
    UUID userId = UUID.randomUUID();

    EnrollmentEntity enrollment = newEnrollment(congress.getId(), userId, UUID.randomUUID());
    enrollment = enrollmentJpaRepository.saveAndFlush(enrollment);

    idempotencyRepository.saveAndFlush(newRecord(
        "key-succeeded-valid",
        congress,
        userId,
        EnrollmentIdempotencyStatus.SUCCEEDED,
        enrollment.getId(),
        enrollment.getPaymentId(),
        "110.00"));

    assertThatThrownBy(() -> idempotencyRepository.saveAndFlush(newRecord(
        "key-succeeded-missing-enrollment",
        congress,
        UUID.randomUUID(),
        EnrollmentIdempotencyStatus.SUCCEEDED,
        null,
        UUID.randomUUID(),
        "110.00")))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThatThrownBy(() -> idempotencyRepository.saveAndFlush(newRecord(
        "key-succeeded-missing-payment",
        congress,
        UUID.randomUUID(),
        EnrollmentIdempotencyStatus.SUCCEEDED,
        UUID.randomUUID(),
        null,
        "110.00")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void failedAllowsNullEnrollmentAndPaymentAndMultipleKeysForSameCongressUser() {
    CongressEntity congress = persistedCongress("DelValle");
    UUID userId = UUID.randomUUID();

    idempotencyRepository.saveAndFlush(newRecord("key-failed-1", congress, userId, EnrollmentIdempotencyStatus.FAILED, null, null, "120.00"));
    idempotencyRepository.saveAndFlush(newRecord("key-failed-2", congress, userId, EnrollmentIdempotencyStatus.FAILED, null, null, "120.00"));
  }

  @Test
  void activeProcessingOrSucceededMustBeUniqueByCongressAndUser() {
    CongressEntity congress = persistedCongress("Mariano");
    UUID userId = UUID.randomUUID();
    idempotencyRepository.saveAndFlush(newRecord("key-active-processing", congress, userId, EnrollmentIdempotencyStatus.PROCESSING, null, null, "130.00"));

    assertThatThrownBy(() -> idempotencyRepository.saveAndFlush(
        newRecord("key-active-succeeded", congress, userId, EnrollmentIdempotencyStatus.SUCCEEDED, UUID.randomUUID(), UUID.randomUUID(), "130.00")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void invalidStatusAndLongIdempotencyKeyAreRejected() {
    CongressEntity congress = persistedCongress("Landivar");

    assertThatThrownBy(() -> {
      entityManager.createNativeQuery("""
              insert into enrollment_idempotency_records (
                idempotency_key, congress_id, user_id, payment_date, request_hash, status,
                institution_id, congress_name_snapshot, institution_name_snapshot, amount
              ) values (
                :key, :congressId, :userId, :paymentDate, :requestHash, :status,
                :institutionId, :congressNameSnapshot, :institutionNameSnapshot, :amount
              )
              """)
          .setParameter("key", "k".repeat(121))
          .setParameter("congressId", congress.getId())
          .setParameter("userId", UUID.randomUUID())
          .setParameter("paymentDate", LocalDate.of(2026, 6, 15))
          .setParameter("requestHash", "h".repeat(64))
          .setParameter("status", "PROCESSING")
          .setParameter("institutionId", congress.getInstitutionId())
          .setParameter("congressNameSnapshot", congress.getName())
          .setParameter("institutionNameSnapshot", "Institution")
          .setParameter("amount", new BigDecimal("140.00"))
          .executeUpdate();
      entityManager.flush();
    }).isInstanceOfAny(DataIntegrityViolationException.class, jakarta.persistence.PersistenceException.class);

    assertThatThrownBy(() -> {
      entityManager.createNativeQuery("""
              insert into enrollment_idempotency_records (
                idempotency_key, congress_id, user_id, payment_date, request_hash, status,
                institution_id, congress_name_snapshot, institution_name_snapshot, amount
              ) values (
                :key, :congressId, :userId, :paymentDate, :requestHash, :status,
                :institutionId, :congressNameSnapshot, :institutionNameSnapshot, :amount
              )
              """)
          .setParameter("key", "key-invalid-status")
          .setParameter("congressId", congress.getId())
          .setParameter("userId", UUID.randomUUID())
          .setParameter("paymentDate", LocalDate.of(2026, 6, 15))
          .setParameter("requestHash", "h".repeat(64))
          .setParameter("status", "INVALID")
          .setParameter("institutionId", congress.getInstitutionId())
          .setParameter("congressNameSnapshot", congress.getName())
          .setParameter("institutionNameSnapshot", "Institution")
          .setParameter("amount", new BigDecimal("140.00"))
          .executeUpdate();
      entityManager.flush();
    }).isInstanceOfAny(DataIntegrityViolationException.class, jakarta.persistence.PersistenceException.class);
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

  private EnrollmentIdempotencyRecordEntity newRecord(
      String key,
      CongressEntity congress,
      UUID userId,
      EnrollmentIdempotencyStatus status,
      UUID enrollmentId,
      UUID paymentId,
      String amount
  ) {
    EnrollmentIdempotencyRecordEntity entity = new EnrollmentIdempotencyRecordEntity();
    entity.setIdempotencyKey(key);
    entity.setCongressId(congress.getId());
    entity.setUserId(userId);
    entity.setPaymentDate(LocalDate.of(2026, 6, 15));
    entity.setRequestHash("a".repeat(64));
    entity.setStatus(status.name());
    entity.setEnrollmentId(enrollmentId);
    entity.setPaymentId(paymentId);
    entity.setInstitutionId(congress.getInstitutionId());
    entity.setCongressNameSnapshot(congress.getName());
    entity.setInstitutionNameSnapshot("Institution");
    entity.setAmount(amount == null ? null : new BigDecimal(amount));
    return entity;
  }
}
