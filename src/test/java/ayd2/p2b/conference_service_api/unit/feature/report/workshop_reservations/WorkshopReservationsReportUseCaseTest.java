package ayd2.p2b.conference_service_api.unit.feature.report.workshop_reservations;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsCongressScopePort;
import ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations.WorkshopReservationsReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations.port.WorkshopReservationsQueryPort;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.CongressInstitutionSummary;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationsReportResponse;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopReservationsReportUseCaseTest {

    @Mock
    private ParticipantsCongressScopePort congressScopePort;
    @Mock
    private WorkshopReservationsQueryPort queryPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;

    private WorkshopReservationsReportUseCase useCase;

    private static final UUID CONGRESS_ID = UUID.randomUUID();
    private static final UUID INSTITUTION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new WorkshopReservationsReportUseCase(congressScopePort, queryPort, iamUserLookupPort);
    }

    @Test
    void non_congress_admin_throws_403() {
        ReportRequesterContext ctx = context(Set.of(Role.PARTICIPANT));
        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void null_congress_id_throws_400() {
        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        assertThatThrownBy(() -> useCase.execute(null, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void unknown_congress_throws_404() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.empty());
        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void unlinked_congress_admin_throws_403() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(USER_ID, INSTITUTION_ID, "token"))
                .thenReturn(false);
        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void capacity_math_returned_correctly() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(USER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);

        UUID activityId = UUID.randomUUID();
        WorkshopReservationItem item = WorkshopReservationItem.builder()
                .activityId(activityId)
                .activityName("Java Workshop")
                .workshopCapacity(30)
                .reservationCount(12)
                .availableSeats(18)
                .roster(List.of())
                .build();
        when(queryPort.query(any(), any())).thenReturn(List.of(item));

        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        WorkshopReservationsReportResponse response = useCase.execute(CONGRESS_ID, null, ctx);

        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getWorkshopCapacity()).isEqualTo(30);
        assertThat(response.getItems().get(0).getReservationCount()).isEqualTo(12);
        assertThat(response.getItems().get(0).getAvailableSeats()).isEqualTo(18);
    }

    @Test
    void empty_result_returns_empty_response() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(USER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(queryPort.query(any(), any())).thenReturn(List.of());

        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        WorkshopReservationsReportResponse response = useCase.execute(CONGRESS_ID, null, ctx);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalItems()).isZero();
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
