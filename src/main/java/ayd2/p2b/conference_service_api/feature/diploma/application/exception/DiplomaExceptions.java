package ayd2.p2b.conference_service_api.feature.diploma.application.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class DiplomaExceptions {

    private DiplomaExceptions() {
    }

    public static ApiException diplomaNotFound(UUID diplomaId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Diploma not found: " + diplomaId
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
