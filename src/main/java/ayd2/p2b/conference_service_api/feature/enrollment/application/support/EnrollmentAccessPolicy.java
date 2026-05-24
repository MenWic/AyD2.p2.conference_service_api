package ayd2.p2b.conference_service_api.feature.enrollment.application.support;

import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.enrollment.application.exception.EnrollmentExceptions;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.EnrollmentRequesterContext;

import java.util.UUID;

public final class EnrollmentAccessPolicy {

  private EnrollmentAccessPolicy() {
  }

  public static void ensureParticipant(EnrollmentRequesterContext requester) {
    if (requester.getRoles() == null || !requester.getRoles().contains(Role.PARTICIPANT)) {
      throw EnrollmentExceptions.forbidden("Only participants can enroll in a congress");
    }
  }

  public static void ensureSelfAccess(UUID requestedUserId, EnrollmentRequesterContext requester) {
    if (!requester.getUserId().equals(requestedUserId)) {
      throw EnrollmentExceptions.forbidden("Access denied: cannot view another user's enrollments");
    }
  }
}
