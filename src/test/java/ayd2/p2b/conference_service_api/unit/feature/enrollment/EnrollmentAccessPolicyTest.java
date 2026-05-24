package ayd2.p2b.conference_service_api.unit.feature.enrollment;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.enrollment.application.support.EnrollmentAccessPolicy;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.EnrollmentRequesterContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class EnrollmentAccessPolicyTest {

  private static final UUID USER_ID = UUID.randomUUID();

  // ──────────────────────────────────────────────────────────────────
  // ensureParticipant
  // ──────────────────────────────────────────────────────────────────

  @Test
  void ensure_participant_passes_when_role_is_present() {
    EnrollmentRequesterContext requester = requesterWithRoles(USER_ID, Role.PARTICIPANT);

    assertThatCode(() -> EnrollmentAccessPolicy.ensureParticipant(requester))
        .doesNotThrowAnyException();
  }

  @Test
  void ensure_participant_passes_when_multiple_roles_include_participant() {
    EnrollmentRequesterContext requester = requesterWithRoles(USER_ID, Role.PARTICIPANT, Role.CONGRESS_ADMIN);

    assertThatCode(() -> EnrollmentAccessPolicy.ensureParticipant(requester))
        .doesNotThrowAnyException();
  }

  @Test
  void ensure_participant_throws_403_when_role_is_missing() {
    EnrollmentRequesterContext requester = requesterWithRoles(USER_ID, Role.CONGRESS_ADMIN);

    assertThatThrownBy(() -> EnrollmentAccessPolicy.ensureParticipant(requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assert apiEx.getStatus() == HttpStatus.FORBIDDEN;
          assert "auth.forbidden".equals(apiEx.getCode());
        });
  }

  @Test
  void ensure_participant_throws_403_when_roles_are_null() {
    EnrollmentRequesterContext requester = EnrollmentRequesterContext.builder()
        .userId(USER_ID)
        .roles(null)
        .accessToken("token")
        .build();

    assertThatThrownBy(() -> EnrollmentAccessPolicy.ensureParticipant(requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assert apiEx.getStatus() == HttpStatus.FORBIDDEN;
        });
  }

  @Test
  void ensure_participant_throws_403_when_roles_are_empty() {
    EnrollmentRequesterContext requester = requesterWithRoles(USER_ID);

    assertThatThrownBy(() -> EnrollmentAccessPolicy.ensureParticipant(requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assert apiEx.getStatus() == HttpStatus.FORBIDDEN;
        });
  }

  // ──────────────────────────────────────────────────────────────────
  // ensureSelfAccess
  // ──────────────────────────────────────────────────────────────────

  @Test
  void ensure_self_access_passes_when_user_id_matches() {
    EnrollmentRequesterContext requester = requesterWithRoles(USER_ID, Role.PARTICIPANT);

    assertThatCode(() -> EnrollmentAccessPolicy.ensureSelfAccess(USER_ID, requester))
        .doesNotThrowAnyException();
  }

  @Test
  void ensure_self_access_throws_403_when_user_id_differs() {
    UUID otherUser = UUID.randomUUID();
    EnrollmentRequesterContext requester = requesterWithRoles(USER_ID, Role.PARTICIPANT);

    assertThatThrownBy(() -> EnrollmentAccessPolicy.ensureSelfAccess(otherUser, requester))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> {
          ApiException apiEx = (ApiException) ex;
          assert apiEx.getStatus() == HttpStatus.FORBIDDEN;
          assert "auth.forbidden".equals(apiEx.getCode());
        });
  }

  // ──────────────────────────────────────────────────────────────────
  // helpers
  // ──────────────────────────────────────────────────────────────────

  private EnrollmentRequesterContext requesterWithRoles(UUID userId, Role... roles) {
    return EnrollmentRequesterContext.builder()
        .userId(userId)
        .roles(Set.of(roles))
        .accessToken("token")
        .build();
  }
}
