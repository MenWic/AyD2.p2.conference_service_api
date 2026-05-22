package ayd2.p2b.conference_service_api.feature.call.application.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class CallExceptions {

    private CallExceptions() {
    }

    public static ApiException callNotFound(UUID callId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Call not found: " + callId
        );
    }

    public static ApiException congressNotFound(UUID congressId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Congress not found: " + congressId
        );
    }

    public static ApiException openCallAlreadyExists(UUID congressId) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "An OPEN call already exists for congress: " + congressId
        );
    }

    public static ApiException callAlreadyClosed(UUID callId) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "Call is already closed: " + callId
        );
    }

    public static ApiException forbidden(String message) {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "auth.forbidden",
                message
        );
    }
}
