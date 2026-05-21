package ayd2.p2b.conference_service_api.feature.room.application.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class RoomExceptions {

    private RoomExceptions() {
    }

    public static ApiException roomNotFound(UUID roomId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Room not found: " + roomId
        );
    }

    public static ApiException congressNotFound(UUID congressId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Congress not found: " + congressId
        );
    }

    public static ApiException roomNameConflict(UUID congressId, String name) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "Room name already exists in congress " + congressId + ": " + name
        );
    }

    public static ApiException hasActivities(UUID roomId) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "Room has dependent activities: " + roomId
        );
    }

    public static ApiException forbidden(String message) {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "auth.forbidden",
                message
        );
    }

    public static ApiException validationFailed(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation.failed",
                message
        );
    }
}
