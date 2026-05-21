package ayd2.p2b.conference_service_api.unit.feature.room.support;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.room.application.support.RoomAccessPolicy;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomCongressSummary;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomRequesterContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomAccessPolicyTest {

    @Test
    void shouldRejectSystemAdminForRoomWrites() {
        RoomRequesterContext requester = requester(Set.of(Role.CONGRESS_ADMIN, Role.SYSTEM_ADMIN));

        assertThatThrownBy(() -> RoomAccessPolicy.ensureCongressAdminWrite(requester))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertForbidden((ApiException) ex));
    }

    @Test
    void shouldRejectParticipantForRoomWrites() {
        RoomRequesterContext requester = requester(Set.of(Role.PARTICIPANT));

        assertThatThrownBy(() -> RoomAccessPolicy.ensureCongressAdminWrite(requester))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertForbidden((ApiException) ex));
    }

    @Test
    void shouldAllowCongressAdminForRoomWrites() {
        RoomRequesterContext requester = requester(Set.of(Role.CONGRESS_ADMIN, Role.PARTICIPANT));

        assertThatCode(() -> RoomAccessPolicy.ensureCongressAdminWrite(requester))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowOwnerToManageCongress() {
        UUID userId = UUID.randomUUID();
        RoomRequesterContext requester = requester(userId, Set.of(Role.CONGRESS_ADMIN));
        RoomCongressSummary congress = congress(userId);

        assertThatCode(() -> RoomAccessPolicy.ensureCanManageCongress(requester, congress, false))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowLinkedCongressAdminToManageCongress() {
        RoomRequesterContext requester = requester(Set.of(Role.CONGRESS_ADMIN));
        RoomCongressSummary congress = congress(UUID.randomUUID());

        assertThatCode(() -> RoomAccessPolicy.ensureCanManageCongress(requester, congress, true))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectCongressAdminWhenNotOwnerAndNotLinked() {
        RoomRequesterContext requester = requester(Set.of(Role.CONGRESS_ADMIN));
        RoomCongressSummary congress = congress(UUID.randomUUID());

        assertThatThrownBy(() -> RoomAccessPolicy.ensureCanManageCongress(requester, congress, false))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertForbidden((ApiException) ex));
    }

    private RoomRequesterContext requester(Set<Role> roles) {
        return requester(UUID.randomUUID(), roles);
    }

    private RoomRequesterContext requester(UUID userId, Set<Role> roles) {
        return RoomRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }

    private RoomCongressSummary congress(UUID createdBy) {
        return RoomCongressSummary.builder()
                .id(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .createdBy(createdBy)
                .build();
    }

    private void assertForbidden(ApiException ex) {
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getCode()).isEqualTo("auth.forbidden");
    }
}
