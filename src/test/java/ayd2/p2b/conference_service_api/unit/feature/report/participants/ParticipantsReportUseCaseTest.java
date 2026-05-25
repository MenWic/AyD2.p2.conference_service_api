package ayd2.p2b.conference_service_api.unit.feature.report.participants;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.report.application.participants.ParticipantsReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsCongressScopePort;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsReportQueryPort;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.CongressInstitutionSummary;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ParticipantRow;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipantsReportResponse;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipationTypeEnum;
import ayd2.p2b.conference_service_api.integration.dto.IamUserDetailSummary;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipantsReportUseCaseTest {

    @Mock
    private ParticipantsCongressScopePort congressScopePort;
    @Mock
    private ParticipantsReportQueryPort queryPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;

    private ParticipantsReportUseCase useCase;

    private static final UUID CONGRESS_ID = UUID.randomUUID();
    private static final UUID INSTITUTION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PARTICIPANT_ID_1 = UUID.randomUUID();
    private static final UUID PARTICIPANT_ID_2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ParticipantsReportUseCase(congressScopePort, queryPort, iamUserLookupPort);
    }

    @Test
    void non_congress_admin_throws_403() {
        ReportRequesterContext ctx = context(Set.of(Role.PARTICIPANT));
        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiEx.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void null_congress_id_throws_400() {
        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        assertThatThrownBy(() -> useCase.execute(null, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiEx.getCode()).isEqualTo("validation.failed");
                });
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
    void empty_participants_returns_empty_response() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(USER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(queryPort.findParticipants(CONGRESS_ID)).thenReturn(List.of());

        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        ParticipantsReportResponse response = useCase.execute(CONGRESS_ID, null, ctx);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalItems()).isZero();
    }

    @Test
    void participants_returned_with_iam_details_merged() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(USER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(queryPort.findParticipants(CONGRESS_ID)).thenReturn(List.of(
                ParticipantRow.builder()
                        .userId(PARTICIPANT_ID_1)
                        .participationTypes(EnumSet.of(ParticipationTypeEnum.ENROLLED))
                        .build()
        ));
        when(iamUserLookupPort.getUserDetailsSummary(Set.of(PARTICIPANT_ID_1), "token"))
                .thenReturn(Map.of(PARTICIPANT_ID_1, iamUserDetail(PARTICIPANT_ID_1, "Alice", "alice@test.com")));

        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        ParticipantsReportResponse response = useCase.execute(CONGRESS_ID, null, ctx);

        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getFullName()).isEqualTo("Alice");
        assertThat(response.getItems().get(0).getEmail()).isEqualTo("alice@test.com");
        assertThat(response.getItems().get(0).getParticipationTypes()).contains(ParticipationTypeEnum.ENROLLED);
    }

    @Test
    void iam_getUserDetailsSummary_called_exactly_once() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(USER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(queryPort.findParticipants(CONGRESS_ID)).thenReturn(List.of(
                ParticipantRow.builder().userId(PARTICIPANT_ID_1)
                        .participationTypes(EnumSet.of(ParticipationTypeEnum.ENROLLED)).build(),
                ParticipantRow.builder().userId(PARTICIPANT_ID_2)
                        .participationTypes(EnumSet.of(ParticipationTypeEnum.SPEAKER)).build()
        ));
        when(iamUserLookupPort.getUserDetailsSummary(any(), anyString())).thenReturn(Map.of(
                PARTICIPANT_ID_1, iamUserDetail(PARTICIPANT_ID_1, "Alice", "alice@test.com"),
                PARTICIPANT_ID_2, iamUserDetail(PARTICIPANT_ID_2, "Bob", "bob@test.com")
        ));

        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        useCase.execute(CONGRESS_ID, null, ctx);

        verify(iamUserLookupPort).getUserDetailsSummary(Set.of(PARTICIPANT_ID_1, PARTICIPANT_ID_2), "token");
    }

    @Test
    void type_filter_narrows_results() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(USER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(queryPort.findParticipants(CONGRESS_ID)).thenReturn(List.of(
                ParticipantRow.builder().userId(PARTICIPANT_ID_1)
                        .participationTypes(EnumSet.of(ParticipationTypeEnum.ENROLLED)).build(),
                ParticipantRow.builder().userId(PARTICIPANT_ID_2)
                        .participationTypes(EnumSet.of(ParticipationTypeEnum.SPEAKER)).build()
        ));
        when(iamUserLookupPort.getUserDetailsSummary(any(), anyString())).thenReturn(Map.of(
                PARTICIPANT_ID_1, iamUserDetail(PARTICIPANT_ID_1, "Alice", "alice@test.com")
        ));

        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        ParticipantsReportResponse response = useCase.execute(CONGRESS_ID, ParticipationTypeEnum.ENROLLED, ctx);

        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getFullName()).isEqualTo("Alice");
    }

    @Test
    void iam_getUserDetailsSummary_not_called_when_no_participants() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(USER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(queryPort.findParticipants(CONGRESS_ID)).thenReturn(List.of());

        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        useCase.execute(CONGRESS_ID, null, ctx);

        verify(iamUserLookupPort, never()).getUserDetailsSummary(any(), anyString());
    }

    @Test
    void null_participation_types_in_row_yields_empty_list_in_item() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(USER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        // Row with null participationTypes — exercises the null-guard branch in buildItem()
        when(queryPort.findParticipants(CONGRESS_ID)).thenReturn(List.of(
                ParticipantRow.builder().userId(PARTICIPANT_ID_1).participationTypes(null).build()
        ));
        when(iamUserLookupPort.getUserDetailsSummary(Set.of(PARTICIPANT_ID_1), "token"))
                .thenReturn(Map.of(PARTICIPANT_ID_1, iamUserDetail(PARTICIPANT_ID_1, "Alice", "alice@test.com")));

        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        ParticipantsReportResponse response = useCase.execute(CONGRESS_ID, null, ctx);

        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getItems().get(0).getParticipationTypes()).isEmpty();
    }

    private CongressInstitutionSummary congressSummary() {
        return CongressInstitutionSummary.builder()
                .congressId(CONGRESS_ID)
                .institutionId(INSTITUTION_ID)
                .congressName("Test Congress")
                .build();
    }

    private IamUserDetailSummary iamUserDetail(UUID id, String fullName, String email) {
        return IamUserDetailSummary.builder()
                .id(id)
                .fullName(fullName)
                .email(email)
                .active(true)
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
