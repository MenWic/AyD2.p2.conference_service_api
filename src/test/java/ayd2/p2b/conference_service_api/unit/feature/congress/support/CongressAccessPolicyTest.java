package ayd2.p2b.conference_service_api.unit.feature.congress.support;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.congress.application.support.CongressAccessPolicy;
import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressRequesterContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CongressAccessPolicyTest {

    @Test
    void shouldRejectNullRequester() {
        assertThatThrownBy(() -> CongressAccessPolicy.ensureCongressAdminWrite(null))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertForbidden((ApiException) ex));
    }

    @Test
    void shouldRejectRequesterWithoutCongressAdminRole() {
        CongressRequesterContext requester = requester(Set.of(Role.PARTICIPANT));

        assertThatThrownBy(() -> CongressAccessPolicy.ensureCongressAdminWrite(requester))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertForbidden((ApiException) ex));
    }

    @Test
    void shouldRejectRequesterWithSystemAdminRole() {
        CongressRequesterContext requester = requester(Set.of(Role.CONGRESS_ADMIN, Role.SYSTEM_ADMIN));

        assertThatThrownBy(() -> CongressAccessPolicy.ensureCongressAdminWrite(requester))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertForbidden((ApiException) ex));
    }

    @Test
    void shouldAllowValidCongressAdminRequester() {
        CongressRequesterContext requester = requester(Set.of(Role.CONGRESS_ADMIN, Role.PARTICIPANT));

        assertThatCode(() -> CongressAccessPolicy.ensureCongressAdminWrite(requester))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowOwnerToModifyEvenIfNotLinked() {
        UUID ownerId = UUID.randomUUID();
        Congress congress = sampleCongress(ownerId);
        CongressRequesterContext requester = requester(ownerId, Set.of(Role.CONGRESS_ADMIN));

        assertThatCode(() -> CongressAccessPolicy.ensureCanModifyExisting(requester, congress, false))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowLinkedAdminEvenIfNotOwner() {
        Congress congress = sampleCongress(UUID.randomUUID());
        CongressRequesterContext requester = requester(UUID.randomUUID(), Set.of(Role.CONGRESS_ADMIN));

        assertThatCode(() -> CongressAccessPolicy.ensureCanModifyExisting(requester, congress, true))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectRequesterWhenNotOwnerAndNotLinked() {
        Congress congress = sampleCongress(UUID.randomUUID());
        CongressRequesterContext requester = requester(UUID.randomUUID(), Set.of(Role.CONGRESS_ADMIN));

        assertThatThrownBy(() -> CongressAccessPolicy.ensureCanModifyExisting(requester, congress, false))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertForbidden((ApiException) ex));
    }

    private CongressRequesterContext requester(Set<Role> roles) {
        return requester(UUID.randomUUID(), roles);
    }

    private CongressRequesterContext requester(UUID userId, Set<Role> roles) {
        return CongressRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }

    private Congress sampleCongress(UUID ownerId) {
        return Congress.builder()
                .id(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .name("Congreso")
                .description("Descripcion")
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 2))
                .location("Guatemala")
                .price(new BigDecimal("50.00"))
                .createdBy(ownerId)
                .build();
    }

    private void assertForbidden(ApiException ex) {
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getCode()).isEqualTo("auth.forbidden");
    }
}
