package ayd2.p2b.conference_service_api.feature.activity.application.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

public final class ActivityExceptions {

    private ActivityExceptions() {
    }

    public static ApiException activityNotFound(UUID activityId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Activity not found: " + activityId
        );
    }

    public static ApiException congressNotFound(UUID congressId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Congress not found: " + congressId
        );
    }

    public static ApiException roomNotFound(UUID roomId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Room not found: " + roomId
        );
    }

    public static ApiException activityLeaderNotFound(UUID userId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Activity leader user not found or inactive: " + userId
        );
    }

    public static ApiException forbidden(String message) {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "auth.forbidden",
                message
        );
    }

    public static ApiException dependencyConflict(UUID activityId, List<String> dependencies) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "Activity has dependent records (" + activityId + "): " + String.join(", ", dependencies)
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
