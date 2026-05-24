package ayd2.p2b.conference_service_api.feature.enrollment.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class EnrollmentIdempotencyRecord {

  String idempotencyKey;
  UUID congressId;
  UUID userId;
  LocalDate paymentDate;
  String requestHash;
  EnrollmentIdempotencyStatus status;
  UUID enrollmentId;
  UUID paymentId;
  Instant createdAt;
  Instant updatedAt;
}
