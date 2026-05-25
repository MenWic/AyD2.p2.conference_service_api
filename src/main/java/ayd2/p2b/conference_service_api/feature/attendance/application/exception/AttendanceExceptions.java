package ayd2.p2b.conference_service_api.feature.attendance.application.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class AttendanceExceptions {

    private AttendanceExceptions() {
    }

    public static ApiException activityNotFound(UUID activityId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Activity not found: " + activityId
        );
    }

    public static ApiException participantNotFoundByPersonalId(String personalId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Participant not found for personalId " + personalId
        );
    }

    public static ApiException duplicateAttendance(UUID activityId, UUID userId) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "Attendance already exists for activity " + activityId + " and user " + userId
        );
    }

    public static ApiException enrollmentRequired(UUID activityId, UUID userId) {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "domain.invariant_violated",
                "User " + userId + " must be enrolled in activity congress before attendance registration for activity " + activityId
        );
    }

    public static ApiException reservationRequiredForWorkshop(UUID activityId, UUID userId) {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "domain.invariant_violated",
                "Workshop attendance requires reservation for activity " + activityId + " and user " + userId
        );
    }

    public static ApiException forbidden(String detail) {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "auth.forbidden",
                detail
        );
    }

    public static ApiException validationFailed(String detail) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation.failed",
                detail
        );
    }
}
