package ayd2.p2b.conference_service_api.feature.enrollment.application.register;

import ayd2.p2b.conference_service_api.feature.enrollment.application.exception.EnrollmentExceptions;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentIdempotencyRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.application.port.EnrollmentRepositoryPort;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.Enrollment;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyRecord;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class EnrollmentRegistrationTransactions {

  private final EnrollmentIdempotencyRepositoryPort idempotencyRepositoryPort;
  private final EnrollmentRepositoryPort enrollmentRepositoryPort;

  public EnrollmentRegistrationTransactions(
      EnrollmentIdempotencyRepositoryPort idempotencyRepositoryPort,
      EnrollmentRepositoryPort enrollmentRepositoryPort) {
    this.idempotencyRepositoryPort = idempotencyRepositoryPort;
    this.enrollmentRepositoryPort = enrollmentRepositoryPort;
  }

  @Transactional
  public EnrollmentIdempotencyRecord createProcessingRecord(EnrollmentIdempotencyRecord record) {
    return idempotencyRepositoryPort.insert(record);
  }

  @Transactional
  public EnrollmentIdempotencyRecord recordWalletPayment(EnrollmentIdempotencyRecord record, UUID paymentId) {
    EnrollmentIdempotencyRecord updated = record.toBuilder()
        .paymentId(paymentId)
        .updatedAt(Instant.now())
        .build();
    return idempotencyRepositoryPort.update(updated);
  }

  @Transactional
  public EnrollmentIdempotencyRecord markFailed(EnrollmentIdempotencyRecord record) {
    EnrollmentIdempotencyRecord failed = record.toBuilder()
        .status(EnrollmentIdempotencyStatus.FAILED)
        .updatedAt(Instant.now())
        .build();
    return idempotencyRepositoryPort.update(failed);
  }

  @Transactional
  public Enrollment completeSuccessfulEnrollment(
      EnrollmentIdempotencyRecord record,
      Enrollment enrollmentDraft,
      UUID paymentId) {

    Enrollment finalEnrollment = enrollmentRepositoryPort
        .findByCongressIdAndUserId(record.getCongressId(), record.getUserId())
        .map(existing -> {
          if (!paymentId.equals(existing.getPaymentId())) {
            throw EnrollmentExceptions.idempotencyConflict(record.getIdempotencyKey());
          }
          return existing;
        })
        .orElseGet(() -> saveEnrollmentWithConflictRecovery(record, enrollmentDraft, paymentId));

    EnrollmentIdempotencyRecord succeeded = record.toBuilder()
        .status(EnrollmentIdempotencyStatus.SUCCEEDED)
        .enrollmentId(finalEnrollment.getId())
        .paymentId(paymentId)
        .updatedAt(Instant.now())
        .build();
    idempotencyRepositoryPort.update(succeeded);

    return finalEnrollment;
  }

  private Enrollment saveEnrollmentWithConflictRecovery(
      EnrollmentIdempotencyRecord record,
      Enrollment enrollmentDraft,
      UUID paymentId) {
    try {
      return enrollmentRepositoryPort.save(enrollmentDraft);
    } catch (DataIntegrityViolationException ex) {
      Enrollment existing = enrollmentRepositoryPort
          .findByCongressIdAndUserId(record.getCongressId(), record.getUserId())
          .orElseThrow(() -> EnrollmentExceptions.idempotencyConflict(record.getIdempotencyKey()));
      if (!paymentId.equals(existing.getPaymentId())) {
        throw EnrollmentExceptions.idempotencyConflict(record.getIdempotencyKey());
      }
      return existing;
    }
  }
}
