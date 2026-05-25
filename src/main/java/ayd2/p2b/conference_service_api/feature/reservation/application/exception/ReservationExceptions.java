package ayd2.p2b.conference_service_api.feature.reservation.application.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class ReservationExceptions {

    private ReservationExceptions() {
    }

    public static ApiException activityNotFound(UUID activityId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Activity not found: " + activityId
        );
    }

    public static ApiException reservationNotFound(UUID reservationId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Reservation not found: " + reservationId
        );
    }

    public static ApiException duplicateReservation(UUID activityId, UUID userId) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "User " + userId + " already has a reservation for activity " + activityId
        );
    }

    public static ApiException workshopFull(UUID activityId) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "Workshop is full for activity " + activityId
        );
    }

    public static ApiException reservationRequiresWorkshop(UUID activityId) {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "domain.invariant_violated",
                "Reservations are only allowed for workshop activities: " + activityId
        );
    }

    public static ApiException enrollmentRequired(UUID activityId, UUID userId) {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "domain.invariant_violated",
                "User " + userId + " must be enrolled in activity congress before reserving activity " + activityId
        );
    }

    public static ApiException cannotCancelWithAttendance(UUID reservationId) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "Reservation " + reservationId + " cannot be cancelled after attendance was registered"
        );
    }

    public static ApiException forbidden(String detail) {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "auth.forbidden",
                detail
        );
    }
}
