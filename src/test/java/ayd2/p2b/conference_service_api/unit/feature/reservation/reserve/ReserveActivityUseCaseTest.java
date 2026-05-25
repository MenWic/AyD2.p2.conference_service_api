package ayd2.p2b.conference_service_api.unit.feature.reservation.reserve;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationActivityPort;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationEnrollmentPort;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationRepositoryPort;
import ayd2.p2b.conference_service_api.feature.reservation.application.reserve.ReserveActivityUseCase;
import ayd2.p2b.conference_service_api.feature.reservation.domain.model.Reservation;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationActivitySummary;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationRequesterContext;
import ayd2.p2b.conference_service_api.feature.reservation.dto.response.ReservationResponse;
import ayd2.p2b.conference_service_api.feature.reservation.mapper.ReservationMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReserveActivityUseCaseTest {

    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;
    @Mock
    private ReservationActivityPort reservationActivityPort;
    @Mock
    private ReservationEnrollmentPort reservationEnrollmentPort;
    @Mock
    private ReservationMapper reservationMapper;

    private ReserveActivityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReserveActivityUseCase(
                reservationRepositoryPort,
                reservationActivityPort,
                reservationEnrollmentPort,
                reservationMapper
        );
    }

    @Test
    void shouldReserveWorkshopForEnrolledParticipant() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ReservationActivitySummary activity = activitySummary(activityId, ActivityType.TALLER, 2);
        Reservation savedReservation = Reservation.builder()
                .id(UUID.randomUUID())
                .activityId(activityId)
                .userId(userId)
                .createdBy(userId)
                .reservedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .build();
        ReservationResponse response = ReservationResponse.builder()
                .id(savedReservation.getId())
                .activityId(activityId)
                .userId(userId)
                .reservedAt(savedReservation.getReservedAt())
                .build();

        when(reservationActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(reservationEnrollmentPort.existsEnrollment(activity.getCongressId(), userId)).thenReturn(true);
        when(reservationRepositoryPort.existsByActivityIdAndUserId(activityId, userId)).thenReturn(false);
        when(reservationRepositoryPort.countByActivityId(activityId)).thenReturn(1L);
        when(reservationRepositoryPort.save(any())).thenReturn(savedReservation);
        when(reservationMapper.toResponse(savedReservation)).thenReturn(response);

        ReservationResponse result = useCase.execute(activityId, participantRequester(userId));

        assertThat(result.getId()).isEqualTo(savedReservation.getId());
        assertThat(result.getActivityId()).isEqualTo(activityId);
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldRejectReservationForPonenciaActivity() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(reservationActivityPort.findActivityById(activityId))
                .thenReturn(Optional.of(activitySummary(activityId, ActivityType.PONENCIA, null)));

        assertThatThrownBy(() -> useCase.execute(activityId, participantRequester(userId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(apiException.getCode()).isEqualTo("domain.invariant_violated");
                });

        verify(reservationRepositoryPort, never()).save(any());
    }

    @Test
    void shouldRejectReservationWithoutEnrollment() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ReservationActivitySummary activity = activitySummary(activityId, ActivityType.TALLER, 10);
        when(reservationActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(reservationEnrollmentPort.existsEnrollment(activity.getCongressId(), userId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(activityId, participantRequester(userId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(apiException.getCode()).isEqualTo("domain.invariant_violated");
                });
    }

    @Test
    void shouldRejectDuplicateReservation() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ReservationActivitySummary activity = activitySummary(activityId, ActivityType.TALLER, 10);
        when(reservationActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(reservationEnrollmentPort.existsEnrollment(activity.getCongressId(), userId)).thenReturn(true);
        when(reservationRepositoryPort.existsByActivityIdAndUserId(activityId, userId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(activityId, participantRequester(userId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    @Test
    void shouldRejectWhenWorkshopIsFull() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ReservationActivitySummary activity = activitySummary(activityId, ActivityType.TALLER, 2);
        when(reservationActivityPort.findActivityById(activityId)).thenReturn(Optional.of(activity));
        when(reservationEnrollmentPort.existsEnrollment(activity.getCongressId(), userId)).thenReturn(true);
        when(reservationRepositoryPort.existsByActivityIdAndUserId(activityId, userId)).thenReturn(false);
        when(reservationRepositoryPort.countByActivityId(activityId)).thenReturn(2L);

        assertThatThrownBy(() -> useCase.execute(activityId, participantRequester(userId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });

        verify(reservationRepositoryPort, never()).save(any());
    }

    private ReservationRequesterContext participantRequester(UUID userId) {
        return ReservationRequesterContext.builder()
                .userId(userId)
                .roles(Set.of(Role.PARTICIPANT))
                .accessToken("token")
                .build();
    }

    private ReservationActivitySummary activitySummary(UUID activityId, ActivityType type, Integer capacity) {
        return ReservationActivitySummary.builder()
                .activityId(activityId)
                .congressId(UUID.randomUUID())
                .institutionId(UUID.randomUUID())
                .congressCreatedBy(UUID.randomUUID())
                .type(type)
                .workshopCapacity(capacity)
                .build();
    }
}
