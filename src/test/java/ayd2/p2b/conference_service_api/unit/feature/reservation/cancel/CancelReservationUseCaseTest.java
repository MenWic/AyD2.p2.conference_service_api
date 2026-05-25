package ayd2.p2b.conference_service_api.unit.feature.reservation.cancel;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.reservation.application.cancel.CancelReservationUseCase;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationAttendancePort;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationRepositoryPort;
import ayd2.p2b.conference_service_api.feature.reservation.domain.model.Reservation;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationRequesterContext;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelReservationUseCaseTest {

    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;
    @Mock
    private ReservationAttendancePort reservationAttendancePort;

    private CancelReservationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelReservationUseCase(reservationRepositoryPort, reservationAttendancePort);
    }

    @Test
    void shouldCancelOwnReservationWhenAttendanceDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Reservation reservation = reservation(reservationId, activityId, userId);

        when(reservationRepositoryPort.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationAttendancePort.existsAttendance(activityId, userId)).thenReturn(false);

        useCase.execute(reservationId, requester(userId));

        verify(reservationRepositoryPort).deleteById(reservationId);
    }

    @Test
    void shouldRejectCancelWhenAttendanceAlreadyExists() {
        UUID reservationId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Reservation reservation = reservation(reservationId, activityId, userId);

        when(reservationRepositoryPort.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationAttendancePort.existsAttendance(activityId, userId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(reservationId, requester(userId)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });

        verify(reservationRepositoryPort, never()).deleteById(reservationId);
    }

    @Test
    void shouldRejectCancelByNonOwner() {
        UUID reservationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Reservation reservation = reservation(reservationId, UUID.randomUUID(), ownerId);

        when(reservationRepositoryPort.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> useCase.execute(reservationId, requester(UUID.randomUUID())))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    private ReservationRequesterContext requester(UUID userId) {
        return ReservationRequesterContext.builder()
                .userId(userId)
                .roles(Set.of(Role.PARTICIPANT))
                .accessToken("token")
                .build();
    }

    private Reservation reservation(UUID reservationId, UUID activityId, UUID userId) {
        return Reservation.builder()
                .id(reservationId)
                .activityId(activityId)
                .userId(userId)
                .reservedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .createdBy(userId)
                .build();
    }
}
