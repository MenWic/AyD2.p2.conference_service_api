package ayd2.p2b.conference_service_api.feature.enrollment.domain.model;

import ayd2.p2b.conference_service_api.feature.enrollment.domain.exception.EnrollmentDomainException;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class Enrollment {

  UUID id;
  UUID congressId;
  UUID userId;
  UUID paymentId;
  Instant enrolledAt;
  LocalDate paymentDate;
  UUID createdBy;

  public void validateInvariants() {
    if (congressId == null) {
      throw new EnrollmentDomainException("congressId is required");
    }
    if (userId == null) {
      throw new EnrollmentDomainException("userId is required");
    }
    if (paymentId == null) {
      throw new EnrollmentDomainException("paymentId is required");
    }
    if (paymentDate == null) {
      throw new EnrollmentDomainException("paymentDate is required");
    }
    if (createdBy == null) {
      throw new EnrollmentDomainException("createdBy is required");
    }
  }
}
