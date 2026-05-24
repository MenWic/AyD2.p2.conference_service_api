package ayd2.p2b.conference_service_api.feature.enrollment.application.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class EnrollmentExceptions {

  private EnrollmentExceptions() {
  }

  public static ApiException notFound(UUID enrollmentId) {
    return new ApiException(HttpStatus.NOT_FOUND, "resource.not_found",
        "Enrollment not found: " + enrollmentId);
  }

  public static ApiException congressNotFound(UUID congressId) {
    return new ApiException(HttpStatus.NOT_FOUND, "resource.not_found",
        "Congress not found or not accessible: " + congressId);
  }

  public static ApiException forbidden(String message) {
    return new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden", message);
  }

  public static ApiException alreadyEnrolled(UUID congressId, UUID userId) {
    return new ApiException(HttpStatus.CONFLICT, "resource.conflict",
        "User " + userId + " is already enrolled in congress " + congressId);
  }

  public static ApiException idempotencyConflict(String idempotencyKey) {
    return new ApiException(HttpStatus.CONFLICT, "resource.conflict",
        "Idempotency key conflict: different request body for key " + idempotencyKey);
  }

  public static ApiException failedKeyNotReusable(String idempotencyKey) {
    return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency.failed_key_not_reusable",
        "Idempotency key was used in a failed request and cannot be reused: " + idempotencyKey);
  }

  public static ApiException validationFailed(String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, "validation.failed", message);
  }
}
