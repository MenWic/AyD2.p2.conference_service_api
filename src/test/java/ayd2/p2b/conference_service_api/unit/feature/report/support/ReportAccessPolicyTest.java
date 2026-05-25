package ayd2.p2b.conference_service_api.unit.feature.report.support;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.report.application.support.ReportAccessPolicy;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportAccessPolicyTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void ensureCongressAdmin_with_congress_admin_role_passes() {
        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        assertThatCode(() -> ReportAccessPolicy.ensureCongressAdmin(ctx)).doesNotThrowAnyException();
    }

    @Test
    void ensureCongressAdmin_with_participant_role_throws_403() {
        ReportRequesterContext ctx = context(Set.of(Role.PARTICIPANT));
        assertThatThrownBy(() -> ReportAccessPolicy.ensureCongressAdmin(ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    org.assertj.core.api.Assertions.assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    org.assertj.core.api.Assertions.assertThat(apiEx.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void ensureCongressAdmin_with_system_admin_role_throws_403() {
        ReportRequesterContext ctx = context(Set.of(Role.SYSTEM_ADMIN));
        assertThatThrownBy(() -> ReportAccessPolicy.ensureCongressAdmin(ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void ensureCongressAdmin_with_empty_roles_throws_403() {
        ReportRequesterContext ctx = context(Set.of());
        assertThatThrownBy(() -> ReportAccessPolicy.ensureCongressAdmin(ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void ensureSystemAdmin_with_system_admin_role_passes() {
        ReportRequesterContext ctx = context(Set.of(Role.SYSTEM_ADMIN));
        assertThatCode(() -> ReportAccessPolicy.ensureSystemAdmin(ctx)).doesNotThrowAnyException();
    }

    @Test
    void ensureSystemAdmin_with_congress_admin_role_throws_403() {
        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        assertThatThrownBy(() -> ReportAccessPolicy.ensureSystemAdmin(ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    org.assertj.core.api.Assertions.assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    org.assertj.core.api.Assertions.assertThat(apiEx.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void ensureSystemAdmin_with_participant_role_throws_403() {
        ReportRequesterContext ctx = context(Set.of(Role.PARTICIPANT));
        assertThatThrownBy(() -> ReportAccessPolicy.ensureSystemAdmin(ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void ensureSystemAdmin_with_both_system_admin_and_participant_passes() {
        ReportRequesterContext ctx = context(Set.of(Role.SYSTEM_ADMIN, Role.PARTICIPANT));
        assertThatCode(() -> ReportAccessPolicy.ensureSystemAdmin(ctx)).doesNotThrowAnyException();
    }

    private ReportRequesterContext context(Set<Role> roles) {
        return ReportRequesterContext.builder()
                .userId(USER_ID)
                .roles(roles)
                .accessToken("token")
                .build();
    }
}
