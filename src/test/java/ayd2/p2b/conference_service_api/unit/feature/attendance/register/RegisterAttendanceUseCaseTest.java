package ayd2.p2b.conference_service_api.unit.feature.attendance.register;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceActivityPort;
import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceEnrollmentPort;
import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceRepositoryPort;
import ayd2.p2b.conference_service_api.feature.attendance.application.port.AttendanceReservationPort;
import ayd2.p2b.conference_service_api.feature.attendance.application.register.RegisterAttendanceUseCase;
import ayd2.p2b.conference_service_api.feature.attendance.domain.model.Attendance;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceActivitySummary;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceRequesterContext;
import ayd2.p2b.conference_service_api.feature.attendance.dto.request.RegisterAttendanceRequest;
import ayd2.p2b.conference_service_api.feature.attendance.dto.response.AttendanceResponse;
import ayd2.p2b.conference_service_api.feature.attendance.mapper.AttendanceMapper;
import ayd2.p2b.conference_service_api.integration.dto.IamPersonalIdUserSummary;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterAttendanceUseCaseTest {

    @Mock
    private AttendanceRepositoryPort attendanceRepositoryPort;
    @Mock
    private AttendanceActivityPort attendanceActivityPort;
    @Mock
    private AttendanceEnrollmentPort attendanceEnrollmentPort;
    @Mock
    private AttendanceReservationPort attendanceReservationPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private AttendanceMapper attendanceMapper;

    private RegisterAttendanceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterAttendanceUseCase(
                attendanceRepositoryPort,
                attendanceActivityPort,
                attendanceEnrollmentPort,
                attendanceReservationPort,
                iamUserLookupPort,
                attendanceMapper
        );
    }

    @Test
    void shouldRegisterAttendanceForPonenciaWithEnrollment() {
        UUID activityId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        AttendanceActivitySummary activity = activitySummary(activityId, requesterId, ActivityType.PONENCIA);
        RegisterAttendanceRequest request = request(activityId, "PID123");
        Attendance saved = savedAttendance(activityId, participantId, requesterId, "PID123");
        AttendanceResponse response = response(saved);

        when(iamUserLookupPort.findUserByPersonalId("PID123", "token"))
                .thenReturn(Optional.of(IamPersonalIdUserSummary.builder().userId(participantId).personalId("PID123").build()));
        when(attendanceActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(attendanceEnrollmentPort.existsEnrollment(activity.getCongressId(), participantId)).thenReturn(true);
        when(attendanceRepositoryPort.existsByActivityIdAndUserId(activityId, participantId)).thenReturn(false);
        when(attendanceRepositoryPort.save(any())).thenReturn(saved);
        when(attendanceMapper.toResponse(saved)).thenReturn(response);

        AttendanceResponse result = useCase.execute(request, requester(requesterId));

        assertThat(result.getActivityId()).isEqualTo(activityId);
        assertThat(result.getPersonalId()).isEqualTo("PID123");
    }

    @Test
    void shouldRegisterAttendanceForWorkshopWithReservation() {
        UUID activityId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        AttendanceActivitySummary activity = activitySummary(activityId, requesterId, ActivityType.TALLER);
        RegisterAttendanceRequest request = request(activityId, "PID777");
        Attendance saved = savedAttendance(activityId, participantId, requesterId, "PID777");

        when(iamUserLookupPort.findUserByPersonalId("PID777", "token"))
                .thenReturn(Optional.of(IamPersonalIdUserSummary.builder().userId(participantId).personalId("PID777").build()));
        when(attendanceActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(attendanceEnrollmentPort.existsEnrollment(activity.getCongressId(), participantId)).thenReturn(true);
        when(attendanceReservationPort.existsReservation(activityId, participantId)).thenReturn(true);
        when(attendanceRepositoryPort.existsByActivityIdAndUserId(activityId, participantId)).thenReturn(false);
        when(attendanceRepositoryPort.save(any())).thenReturn(saved);
        when(attendanceMapper.toResponse(saved)).thenReturn(response(saved));

        AttendanceResponse result = useCase.execute(request, requester(requesterId));

        assertThat(result.getPersonalId()).isEqualTo("PID777");
    }

    @Test
    void shouldRejectWorkshopAttendanceWithoutReservation() {
        UUID activityId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        AttendanceActivitySummary activity = activitySummary(activityId, requesterId, ActivityType.TALLER);

        when(iamUserLookupPort.findUserByPersonalId("PID001", "token"))
                .thenReturn(Optional.of(IamPersonalIdUserSummary.builder().userId(participantId).personalId("PID001").build()));
        when(attendanceActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(attendanceEnrollmentPort.existsEnrollment(activity.getCongressId(), participantId)).thenReturn(true);
        when(attendanceReservationPort.existsReservation(activityId, participantId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(request(activityId, "PID001"), requester(requesterId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(apiException.getCode()).isEqualTo("domain.invariant_violated");
                });
    }

    @Test
    void shouldRejectAttendanceWhenParticipantIsNotEnrolled() {
        UUID activityId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        AttendanceActivitySummary activity = activitySummary(activityId, requesterId, ActivityType.PONENCIA);

        when(iamUserLookupPort.findUserByPersonalId("PID002", "token"))
                .thenReturn(Optional.of(IamPersonalIdUserSummary.builder().userId(participantId).personalId("PID002").build()));
        when(attendanceActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(attendanceEnrollmentPort.existsEnrollment(activity.getCongressId(), participantId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(request(activityId, "PID002"), requester(requesterId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(apiException.getCode()).isEqualTo("domain.invariant_violated");
                });
    }

    @Test
    void shouldRejectDuplicateAttendance() {
        UUID activityId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        AttendanceActivitySummary activity = activitySummary(activityId, requesterId, ActivityType.PONENCIA);

        when(iamUserLookupPort.findUserByPersonalId("PID003", "token"))
                .thenReturn(Optional.of(IamPersonalIdUserSummary.builder().userId(participantId).personalId("PID003").build()));
        when(attendanceActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(attendanceEnrollmentPort.existsEnrollment(activity.getCongressId(), participantId)).thenReturn(true);
        when(attendanceRepositoryPort.existsByActivityIdAndUserId(activityId, participantId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request(activityId, "PID003"), requester(requesterId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    @Test
    void shouldRejectUnknownPersonalId() {
        UUID activityId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(iamUserLookupPort.findUserByPersonalId("UNKNOWN", "token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(request(activityId, "UNKNOWN"), requester(requesterId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getCode()).isEqualTo("resource.not_found");
                });
    }

    @Test
    void shouldRejectNonScopedCongressAdmin() {
        UUID activityId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        AttendanceActivitySummary activity = activitySummary(activityId, UUID.randomUUID(), ActivityType.PONENCIA);

        when(iamUserLookupPort.findUserByPersonalId("PID009", "token"))
                .thenReturn(Optional.of(IamPersonalIdUserSummary.builder().userId(participantId).personalId("PID009").build()));
        when(attendanceActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requesterId, activity.getInstitutionId(), "token")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(request(activityId, "PID009"), requester(requesterId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void shouldReturnPersonalIdFromSnapshotInResponse() {
        UUID activityId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        AttendanceActivitySummary activity = activitySummary(activityId, requesterId, ActivityType.PONENCIA);
        Attendance saved = savedAttendance(activityId, participantId, requesterId, "PI-D-SNAPSHOT");
        AttendanceResponse response = response(saved);

        when(iamUserLookupPort.findUserByPersonalId("PI-D-SNAPSHOT", "token"))
                .thenReturn(Optional.of(IamPersonalIdUserSummary.builder()
                        .userId(participantId)
                        .personalId("PI-D-SNAPSHOT")
                        .build()));
        when(attendanceActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(attendanceEnrollmentPort.existsEnrollment(activity.getCongressId(), participantId)).thenReturn(true);
        when(attendanceRepositoryPort.existsByActivityIdAndUserId(activityId, participantId)).thenReturn(false);
        when(attendanceRepositoryPort.save(any())).thenReturn(saved);
        when(attendanceMapper.toResponse(saved)).thenReturn(response);

        AttendanceResponse result = useCase.execute(request(activityId, "PI-D-SNAPSHOT"), requester(requesterId));

        assertThat(result.getPersonalId()).isEqualTo("PI-D-SNAPSHOT");
    }

    private AttendanceRequesterContext requester(UUID userId) {
        return AttendanceRequesterContext.builder()
                .userId(userId)
                .roles(Set.of(Role.CONGRESS_ADMIN, Role.PARTICIPANT))
                .accessToken("token")
                .build();
    }

    private RegisterAttendanceRequest request(UUID activityId, String personalId) {
        return RegisterAttendanceRequest.builder()
                .activityId(activityId)
                .personalId(personalId)
                .build();
    }

    private AttendanceActivitySummary activitySummary(UUID activityId, UUID ownerId, ActivityType type) {
        return AttendanceActivitySummary.builder()
                .activityId(activityId)
                .congressId(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .congressCreatedBy(ownerId)
                .roomId(UUID.randomUUID())
                .type(type)
                .build();
    }

    private Attendance savedAttendance(UUID activityId, UUID participantId, UUID registeredBy, String personalId) {
        return Attendance.builder()
                .id(UUID.randomUUID())
                .activityId(activityId)
                .userId(participantId)
                .personalIdSnapshot(personalId)
                .registeredBy(registeredBy)
                .registeredAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .createdBy(registeredBy)
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
