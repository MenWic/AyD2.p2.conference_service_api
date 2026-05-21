package ayd2.p2b.conference_service_api.unit.feature.activity.support;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.activity.application.support.ActivityAccessPolicy;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityCongressRoomSummary;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivityRequesterContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityAccessPolicyTest {

    @Test
    void shouldRejectSystemAdmin() {
        assertThatThrownBy(() -> ActivityAccessPolicy.ensureCongressAdminWrite(requester(Set.of(Role.CONGRESS_ADMIN, Role.SYSTEM_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertForbidden((ApiException) ex));
    }

    @Test
    void shouldRejectParticipant() {
        assertThatThrownBy(() -> ActivityAccessPolicy.ensureCongressAdminWrite(requester(Set.of(Role.PARTICIPANT))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertForbidden((ApiException) ex));
    }

    @Test
    void shouldAllowCongressAdmin() {
        assertThatCode(() -> ActivityAccessPolicy.ensureCongressAdminWrite(requester(Set.of(Role.CONGRESS_ADMIN))))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowOwner() {
        UUID ownerId = UUID.randomUUID();
        assertThatCode(() -> ActivityAccessPolicy.ensureCanManageActivity(
                requester(ownerId, Set.of(Role.CONGRESS_ADMIN)),
                summary(ownerId),
                false
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowLinkedCongressAdmin() {
        assertThatCode(() -> ActivityAccessPolicy.ensureCanManageActivity(
                requester(Set.of(Role.CONGRESS_ADMIN)),
                summary(UUID.randomUUID()),
                true
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWhenNotOwnerAndNotLinked() {
        assertThatThrownBy(() -> ActivityAccessPolicy.ensureCanManageActivity(
                requester(Set.of(Role.CONGRESS_ADMIN)),
                summary(UUID.randomUUID()),
                false
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertForbidden((ApiException) ex));
    }

    private ActivityRequesterContext requester(Set<Role> roles) {
        return requester(UUID.randomUUID(), roles);
    }

    private ActivityRequesterContext requester(UUID userId, Set<Role> roles) {
        return ActivityRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }

    private ActivityCongressRoomSummary summary(UUID ownerId) {
        return ActivityCongressRoomSummary.builder()
                .congressId(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .roomId(UUID.randomUUID())
                .createdBy(ownerId)
                .build();
    }

    private void assertForbidden(ApiException ex) {
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getCode()).isEqualTo("auth.forbidden");
    }
}
