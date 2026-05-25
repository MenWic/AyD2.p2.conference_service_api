package ayd2.p2b.conference_service_api.unit.feature.report.attendance_summary;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.report.application.attendance_summary.AttendanceByActivityReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.attendance_summary.port.AttendanceByActivityQueryPort;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsCongressScopePort;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.CongressInstitutionSummary;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceActivityItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceByActivityReportResponse;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceByActivityReportUseCaseTest {

    @Mock
    private ParticipantsCongressScopePort congressScopePort;
    @Mock
    private AttendanceByActivityQueryPort queryPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;

    private AttendanceByActivityReportUseCase useCase;

    private static final UUID CONGRESS_ID = UUID.randomUUID();
    private static final UUID INSTITUTION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new AttendanceByActivityReportUseCase(congressScopePort, queryPort, iamUserLookupPort);
    }

    @Test
    void non_congress_admin_throws_403() {
        ReportRequesterContext ctx = context(Set.of(Role.PARTICIPANT));
        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, null, null, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void null_congress_id_throws_400() {
        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        assertThatThrownBy(() -> useCase.execute(null, null, null, null, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(((ApiException) ex).getCode()).isEqualTo("validation.failed");
                });
    }

    @Test
    void unknown_congress_throws_404() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.empty());
        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, null, null, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void unlinked_congress_admin_throws_403() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(USER_ID, INSTITUTION_ID, "token"))
                .thenReturn(false);
        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, null, null, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void empty_result_returns_empty_response() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(USER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(queryPort.query(any(), any(), any(), any(), any())).thenReturn(List.of());

        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        AttendanceByActivityReportResponse response = useCase.execute(CONGRESS_ID, null, null, null, null, ctx);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalItems()).isZero();
    }

    @Test
    void results_wrapped_in_response() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(USER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);

        UUID activityId = UUID.randomUUID();
        AttendanceActivityItem item = AttendanceActivityItem.builder()
                .activityId(activityId)
                .activityName("Talk on AI")
                .roomName("Room A")
                .startTime(OffsetDateTime.now())
                .endTime(OffsetDateTime.now().plusHours(1))
                .attendanceCount(15L)
                .build();
        when(queryPort.query(any(), any(), any(), any(), any())).thenReturn(List.of(item));

        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        AttendanceByActivityReportResponse response = useCase.execute(CONGRESS_ID, null, null, null, null, ctx);

        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getActivityName()).isEqualTo("Talk on AI");
        assertThat(response.getItems().get(0).getAttendanceCount()).isEqualTo(15L);
    }

    private CongressInstitutionSummary congressSummary() {
        return CongressInstitutionSummary.builder()
                .congressId(CONGRESS_ID)
                .institutionId(INSTITUTION_ID)
                .congressName("Test Congress")
                .build();
    }

    private ReportRequesterContext context(Set<Role> roles) {
        return ReportRequesterContext.builder()
                .userId(USER_ID)
                .roles(roles)
                .accessToken("token")
                .build();
    }
}
