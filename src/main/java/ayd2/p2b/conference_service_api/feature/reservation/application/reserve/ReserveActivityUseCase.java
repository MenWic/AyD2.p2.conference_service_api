package ayd2.p2b.conference_service_api.feature.reservation.application.reserve;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.reservation.application.exception.ReservationExceptions;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationActivityPort;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationEnrollmentPort;
import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationRepositoryPort;
import ayd2.p2b.conference_service_api.feature.reservation.application.support.ReservationAccessPolicy;
import ayd2.p2b.conference_service_api.feature.reservation.domain.model.Reservation;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationActivitySummary;
import ayd2.p2b.conference_service_api.feature.reservation.dto.internal.ReservationRequesterContext;
import ayd2.p2b.conference_service_api.feature.reservation.dto.response.ReservationResponse;
import ayd2.p2b.conference_service_api.feature.reservation.mapper.ReservationMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional
public class ReserveActivityUseCase {

    private final ReservationRepositoryPort reservationRepositoryPort;
    private final ReservationActivityPort reservationActivityPort;
    private final ReservationEnrollmentPort reservationEnrollmentPort;
    private final ReservationMapper reservationMapper;

    public ReserveActivityUseCase(
            ReservationRepositoryPort reservationRepositoryPort,
            ReservationActivityPort reservationActivityPort,
            ReservationEnrollmentPort reservationEnrollmentPort,
            ReservationMapper reservationMapper
    ) {
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.reservationActivityPort = reservationActivityPort;
        this.reservationEnrollmentPort = reservationEnrollmentPort;
        this.reservationMapper = reservationMapper;
    }

    public ReservationResponse execute(UUID activityId, ReservationRequesterContext requester) {
        ReservationAccessPolicy.ensureParticipant(requester);

        ReservationActivitySummary activity = reservationActivityPort.findActivityByIdForReservationUpdate(activityId)
                .orElseThrow(() -> ReservationExceptions.activityNotFound(activityId));
        if (activity.getType() != ActivityType.TALLER) {
            throw ReservationExceptions.reservationRequiresWorkshop(activityId);
        }

        if (!reservationEnrollmentPort.existsEnrollment(activity.getCongressId(), requester.getUserId())) {
            throw ReservationExceptions.enrollmentRequired(activityId, requester.getUserId());
        }

        if (reservationRepositoryPort.existsByActivityIdAndUserId(activityId, requester.getUserId())) {
            throw ReservationExceptions.duplicateReservation(activityId, requester.getUserId());
        }

        if (isWorkshopFull(activity)) {
            throw ReservationExceptions.workshopFull(activityId);
        }

        Reservation reservation = Reservation.builder()
                .activityId(activityId)
                .userId(requester.getUserId())
                .createdBy(requester.getUserId())
                .build();
        reservation.validateInvariants();

        try {
            return reservationMapper.toResponse(reservationRepositoryPort.save(reservation));
        } catch (DataIntegrityViolationException ex) {
            throw ReservationExceptions.duplicateReservation(activityId, requester.getUserId());
        }
    }

    private boolean isWorkshopFull(ReservationActivitySummary activity) {
        Integer capacity = activity.getWorkshopCapacity();
        if (capacity == null || capacity <= 0) {
            throw ReservationExceptions.reservationRequiresWorkshop(activity.getActivityId());
        }
        return reservationRepositoryPort.countByActivityId(activity.getActivityId()) >= capacity;
    }
}
