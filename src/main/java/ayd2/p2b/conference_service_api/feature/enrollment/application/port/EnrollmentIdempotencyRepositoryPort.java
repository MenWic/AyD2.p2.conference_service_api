package ayd2.p2b.conference_service_api.feature.enrollment.application.port;

import ayd2.p2b.conference_service_api.feature.enrollment.domain.model.EnrollmentIdempotencyRecord;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentIdempotencyRepositoryPort {

  /**
   * Inserts a new record with status PROCESSING.
   * Throws a conflict exception if an active (PROCESSING/SUCCEEDED) record
   * already exists
   * for the same (congressId, userId) pair.
   */
  EnrollmentIdempotencyRecord insert(EnrollmentIdempotencyRecord record);

  Optional<EnrollmentIdempotencyRecord> findByKey(String idempotencyKey);

  Optional<EnrollmentIdempotencyRecord> findActiveByCongressIdAndUserId(UUID congressId, UUID userId);

  EnrollmentIdempotencyRecord update(EnrollmentIdempotencyRecord record);
}
