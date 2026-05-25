package ayd2.p2b.conference_service_api.feature.reservation.application.cancel;

import ayd2.p2b.conference_service_api.feature.reservation.application.exception.ReservationExceptions;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationAttendancePort;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationRepositoryPort;
import ayd2.p2b.conference_service_api.feature.reservation.application.support.ReservationAccessPolicy;
import ayd2.p2b.conference_service_api.feature.reservation.domain.model.Reservation;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationRequesterContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional
public class CancelReservationUseCase {

    private final ReservationRepositoryPort reservationRepositoryPort;
    private final ReservationAttendancePort reservationAttendancePort;

    public CancelReservationUseCase(
            ReservationRepositoryPort reservationRepositoryPort,
            ReservationAttendancePort reservationAttendancePort
    ) {
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.reservationAttendancePort = reservationAttendancePort;
    }

    public void execute(UUID reservationId, ReservationRequesterContext requester) {
        ReservationAccessPolicy.ensureAuthenticated(requester);

        Reservation reservation = reservationRepositoryPort.findById(reservationId)
                .orElseThrow(() -> ReservationExceptions.reservationNotFound(reservationId));

        if (!reservation.getUserId().equals(requester.getUserId())) {
            throw ReservationExceptions.forbidden("Requester can only cancel own reservations");
        }

        if (reservationAttendancePort.existsAttendance(reservation.getActivityId(), reservation.getUserId())) {
            throw ReservationExceptions.cannotCancelWithAttendance(reservationId);
        }

        reservationRepositoryPort.deleteById(reservationId);
    }
}
