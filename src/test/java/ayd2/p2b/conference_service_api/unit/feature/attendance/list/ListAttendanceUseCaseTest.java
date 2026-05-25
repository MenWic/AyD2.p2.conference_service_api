package ayd2.p2b.conference_service_api.unit.feature.attendance.list;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.attendance.application.list.ListAttendanceUseCase;
import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceRepositoryPort;
import ayd2.p2b.conference_service_api.feature.attendance.domain.model.Attendance;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceRequesterContext;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceSearchCriteria;
import ayd2.p2b.conference_service_api.feature.attendance.dto.response.AttendanceResponse;
import ayd2.p2b.conference_service_api.feature.attendance.mapper.AttendanceMapper;
import ayd2.p2b.conference_service_api.integration.dto.IamUserSummary;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAttendanceUseCaseTest {

    @Mock
    private AttendanceRepositoryPort attendanceRepositoryPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private AttendanceMapper attendanceMapper;

    private ListAttendanceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListAttendanceUseCase(
                attendanceRepositoryPort,
                iamUserLookupPort,
                attendanceMapper
        );
    }

    @Test
    void shouldAllowOwnerCongressAdminToListAttendance() {
        UUID requesterId = UUID.randomUUID();
        Attendance attendance = attendance();
        AttendanceResponse response = response(attendance);
        PageRequest pageable = PageRequest.of(0, 20);
        AttendanceSearchCriteria criteria = AttendanceSearchCriteria.builder().build();

        when(iamUserLookupPort.getUsersSummary(Set.of(requesterId), "token"))
                .thenReturn(Map.of(requesterId, IamUserSummary.builder()
                        .id(requesterId)
                        .active(true)
                        .roles(Set.of("CONGRESS_ADMIN"))
                        .linkedInstitutions(Set.of())
                        .build()));
        when(attendanceRepositoryPort.findByCriteria(criteria, pageable, requesterId, Set.of()))
                .thenReturn(new PageImpl<>(List.of(attendance), pageable, 1));
        when(attendanceMapper.toResponse(attendance)).thenReturn(response);

        PageResponse<AttendanceResponse> page = useCase.execute(criteria, pageable, requester(requesterId, Set.of(Role.CONGRESS_ADMIN)));

        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().getFirst().getId()).isEqualTo(attendance.getId());
    }

    @Test
    void shouldAllowScopedCongressAdminWithLinkedInstitutions() {
        UUID requesterId = UUID.randomUUID();
        UUID linkedInstitution = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        AttendanceSearchCriteria criteria = AttendanceSearchCriteria.builder().build();

        when(iamUserLookupPort.getUsersSummary(Set.of(requesterId), "token"))
                .thenReturn(Map.of(requesterId, IamUserSummary.builder()
                        .id(requesterId)
                        .active(true)
                        .roles(Set.of("CONGRESS_ADMIN"))
                        .linkedInstitutions(Set.of(linkedInstitution))
                        .build()));
        when(attendanceRepositoryPort.findByCriteria(criteria, pageable, requesterId, Set.of(linkedInstitution)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<AttendanceResponse> page = useCase.execute(criteria, pageable, requester(requesterId, Set.of(Role.CONGRESS_ADMIN)));

        assertThat(page.getTotalItems()).isZero();
    }

    @Test
    void shouldRejectParticipantRoleWhenListingAttendance() {
        UUID requesterId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(
                AttendanceSearchCriteria.builder().build(),
                PageRequest.of(0, 20),
                requester(requesterId, Set.of(Role.PARTICIPANT))
        )).isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void shouldFailWithIntegrationErrorWhenRequesterSummaryIsUnavailable() {
        UUID requesterId = UUID.randomUUID();
        when(iamUserLookupPort.getUsersSummary(Set.of(requesterId), "token"))
                .thenReturn(Map.of());

        assertThatThrownBy(() -> useCase.execute(
                AttendanceSearchCriteria.builder().build(),
                PageRequest.of(0, 20),
                requester(requesterId, Set.of(Role.CONGRESS_ADMIN))
        )).isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    private AttendanceRequesterContext requester(UUID userId, Set<Role> roles) {
        return AttendanceRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }

    private Attendance attendance() {
        return Attendance.builder()
                .id(UUID.randomUUID())
                .activityId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .personalIdSnapshot("PID123")
                .registeredBy(UUID.randomUUID())
                .registeredAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .createdBy(UUID.randomUUID())
                .createdAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .build();
    }

    private AttendanceResponse response(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .activityId(attendance.getActivityId())
                .personalId(attendance.getPersonalIdSnapshot())
                .registeredBy(attendance.getRegisteredBy())
                .registeredAt(attendance.getRegisteredAt())
                .build();
    }
}
