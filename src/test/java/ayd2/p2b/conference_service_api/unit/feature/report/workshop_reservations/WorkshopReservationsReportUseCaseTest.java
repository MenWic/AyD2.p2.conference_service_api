package ayd2.p2b.conference_service_api.unit.feature.report.workshop_reservations;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsCongressScopePort;
import ayd2.p2b.conference_service_api.feature.report.application.participants.port.ParticipantsReportQueryPort;
import ayd2.p2b.conference_service_api.feature.report.application.support.ReportParticipationTypeResolver;
import ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations.WorkshopReservationsReportUseCase;
import ayd2.p2b.conference_service_api.feature.report.application.workshop_reservations.port.WorkshopReservationsQueryPort;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.CongressInstitutionSummary;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ParticipantRow;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ReportRequesterContext;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.WorkshopReservationRow;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipationTypeEnum;
import ayd2.p2b.conference_service_api.feature.report.dto.response.RosterEntry;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationsReportResponse;
import ayd2.p2b.conference_service_api.integration.dto.IamUserDetailSummary;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopReservationsReportUseCaseTest {

    @Mock
    private ParticipantsCongressScopePort congressScopePort;
    @Mock
    private WorkshopReservationsQueryPort workshopReservationsQueryPort;
    @Mock
    private ParticipantsReportQueryPort participantsReportQueryPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;

    private WorkshopReservationsReportUseCase useCase;

    private static final UUID CONGRESS_ID = UUID.randomUUID();
    private static final UUID INSTITUTION_ID = UUID.randomUUID();
    private static final UUID REQUESTER_ID = UUID.randomUUID();
    private static final UUID WORKSHOP_ID = UUID.randomUUID();
    private static final UUID USER_ID_1 = UUID.randomUUID();
    private static final UUID USER_ID_2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new WorkshopReservationsReportUseCase(
                congressScopePort,
                workshopReservationsQueryPort,
                participantsReportQueryPort,
                new ReportParticipationTypeResolver(),
                iamUserLookupPort
        );
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
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(REQUESTER_ID, INSTITUTION_ID, "token"))
                .thenReturn(false);
        ReportRequesterContext ctx = context(Set.of(Role.CONGRESS_ADMIN));
        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, ctx))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void empty_result_returns_empty_response_without_iam_lookup() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(REQUESTER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(workshopReservationsQueryPort.query(CONGRESS_ID, null)).thenReturn(List.of());

        WorkshopReservationsReportResponse response = useCase.execute(CONGRESS_ID, null, context(Set.of(Role.CONGRESS_ADMIN)));

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalItems()).isZero();
        verify(iamUserLookupPort, never()).getUserDetailsSummary(any(), any());
    }

    @Test
    void roster_is_enriched_with_iam_profile_and_participation_type() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(REQUESTER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(workshopReservationsQueryPort.query(CONGRESS_ID, null))
                .thenReturn(List.of(WorkshopReservationRow.builder()
                        .activityId(WORKSHOP_ID)
                        .activityName("Java Workshop")
                        .workshopCapacity(2)
                        .reservedUserIds(List.of(USER_ID_1, USER_ID_2))
                        .build()));
        when(iamUserLookupPort.getUserDetailsSummary(Set.of(USER_ID_1, USER_ID_2), "token"))
                .thenReturn(Map.of(
                        USER_ID_1, iamUser(USER_ID_1, "PID-001", "Ana", "ana@test.com"),
                        USER_ID_2, iamUser(USER_ID_2, "PID-002", "Luis", "luis@test.com")
                ));
        when(participantsReportQueryPort.findParticipants(CONGRESS_ID))
                .thenReturn(List.of(
                        ParticipantRow.builder()
                                .userId(USER_ID_1)
                                .participationTypes(EnumSet.of(ParticipationTypeEnum.ENROLLED, ParticipationTypeEnum.SPEAKER))
                                .build(),
                        ParticipantRow.builder()
                                .userId(USER_ID_2)
                                .participationTypes(EnumSet.of(ParticipationTypeEnum.PROPOSAL_AUTHOR, ParticipationTypeEnum.ENROLLED))
                                .build()
                ));

        WorkshopReservationsReportResponse response = useCase.execute(CONGRESS_ID, null, context(Set.of(Role.CONGRESS_ADMIN)));

        assertThat(response.getTotalItems()).isEqualTo(1);
        WorkshopReservationItem item = response.getItems().get(0);
        assertThat(item.getReservationCount()).isEqualTo(2);
        assertThat(item.getAvailableSeats()).isEqualTo(0);
        assertThat(item.getRoster()).hasSize(2);
        assertThat(item.getRoster()).extracting(RosterEntry::getPersonalId).containsExactly("PID-001", "PID-002");
        assertThat(item.getRoster()).extracting(RosterEntry::getFullName).containsExactly("Ana", "Luis");
        assertThat(item.getRoster()).extracting(RosterEntry::getEmail).containsExactly("ana@test.com", "luis@test.com");
        assertThat(item.getRoster()).extracting(RosterEntry::getParticipationType)
                .containsExactly(ParticipationTypeEnum.SPEAKER, ParticipationTypeEnum.PROPOSAL_AUTHOR);
    }

    @Test
    void iam_lookup_is_batched_once_with_all_reserved_users() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(REQUESTER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(workshopReservationsQueryPort.query(CONGRESS_ID, null))
                .thenReturn(List.of(
                        WorkshopReservationRow.builder()
                                .activityId(WORKSHOP_ID)
                                .activityName("Workshop A")
                                .workshopCapacity(10)
                                .reservedUserIds(List.of(USER_ID_1))
                                .build(),
                        WorkshopReservationRow.builder()
                                .activityId(UUID.randomUUID())
                                .activityName("Workshop B")
                                .workshopCapacity(10)
                                .reservedUserIds(List.of(USER_ID_2))
                                .build()
                ));
        when(iamUserLookupPort.getUserDetailsSummary(any(), any()))
                .thenReturn(Map.of(
                        USER_ID_1, iamUser(USER_ID_1, "P1", "Ana", "ana@test.com"),
                        USER_ID_2, iamUser(USER_ID_2, "P2", "Luis", "luis@test.com")
                ));
        when(participantsReportQueryPort.findParticipants(CONGRESS_ID))
                .thenReturn(List.of(
                        ParticipantRow.builder().userId(USER_ID_1).participationTypes(EnumSet.of(ParticipationTypeEnum.ENROLLED)).build(),
                        ParticipantRow.builder().userId(USER_ID_2).participationTypes(EnumSet.of(ParticipationTypeEnum.ENROLLED)).build()
                ));

        useCase.execute(CONGRESS_ID, null, context(Set.of(Role.CONGRESS_ADMIN)));

        ArgumentCaptor<Set<UUID>> userIdsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(iamUserLookupPort).getUserDetailsSummary(userIdsCaptor.capture(), any());
        assertThat(userIdsCaptor.getValue()).containsExactlyInAnyOrder(USER_ID_1, USER_ID_2);
    }

    @Test
    void missing_iam_profile_fields_throws_503_integration_iam_unavailable() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(REQUESTER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(workshopReservationsQueryPort.query(CONGRESS_ID, null))
                .thenReturn(List.of(WorkshopReservationRow.builder()
                        .activityId(WORKSHOP_ID)
                        .activityName("Workshop")
                        .workshopCapacity(5)
                        .reservedUserIds(List.of(USER_ID_1))
                        .build()));
        when(iamUserLookupPort.getUserDetailsSummary(Set.of(USER_ID_1), "token"))
                .thenReturn(Map.of(USER_ID_1, iamUser(USER_ID_1, "", "Ana", "ana@test.com")));
        when(participantsReportQueryPort.findParticipants(CONGRESS_ID))
                .thenReturn(List.of(
                        ParticipantRow.builder().userId(USER_ID_1).participationTypes(EnumSet.of(ParticipationTypeEnum.ENROLLED)).build()
                ));

        assertThatThrownBy(() -> useCase.execute(CONGRESS_ID, null, context(Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    @Test
    void available_seats_never_negative() {
        when(congressScopePort.findCongressSummary(CONGRESS_ID)).thenReturn(Optional.of(congressSummary()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(REQUESTER_ID, INSTITUTION_ID, "token"))
                .thenReturn(true);
        when(workshopReservationsQueryPort.query(CONGRESS_ID, null))
                .thenReturn(List.of(WorkshopReservationRow.builder()
                        .activityId(WORKSHOP_ID)
                        .activityName("Workshop")
                        .workshopCapacity(1)
                        .reservedUserIds(List.of(USER_ID_1, USER_ID_2))
                        .build()));
        when(iamUserLookupPort.getUserDetailsSummary(Set.of(USER_ID_1, USER_ID_2), "token"))
                .thenReturn(Map.of(
                        USER_ID_1, iamUser(USER_ID_1, "P1", "Ana", "ana@test.com"),
                        USER_ID_2, iamUser(USER_ID_2, "P2", "Luis", "luis@test.com")
                ));
        when(participantsReportQueryPort.findParticipants(CONGRESS_ID))
                .thenReturn(List.of(
                        ParticipantRow.builder().userId(USER_ID_1).participationTypes(EnumSet.of(ParticipationTypeEnum.ENROLLED)).build(),
                        ParticipantRow.builder().userId(USER_ID_2).participationTypes(EnumSet.of(ParticipationTypeEnum.ENROLLED)).build()
                ));

        WorkshopReservationsReportResponse response = useCase.execute(CONGRESS_ID, null, context(Set.of(Role.CONGRESS_ADMIN)));

        assertThat(response.getItems().get(0).getAvailableSeats()).isZero();
    }

    private CongressInstitutionSummary congressSummary() {
        return CongressInstitutionSummary.builder()
                .congressId(CONGRESS_ID)
                .institutionId(INSTITUTION_ID)
                .congressName("Test Congress")
                .build();
    }

    private IamUserDetailSummary iamUser(UUID id, String personalId, String fullName, String email) {
        return IamUserDetailSummary.builder()
                .id(id)
                .personalId(personalId)
                .fullName(fullName)
                .email(email)
                .active(true)
                .build();
    }

    private ReportRequesterContext context(Set<Role> roles) {
        return ReportRequesterContext.builder()
                .userId(REQUESTER_ID)
                .roles(roles)
                .accessToken("token")
                .build();
    }
}
