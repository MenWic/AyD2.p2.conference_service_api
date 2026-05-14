package ayd2.p2b.conference_service_api.feature.institution.domain.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class InstitutionExceptions {

    private InstitutionExceptions() {
    }

    public static ApiException notFound(UUID institutionId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Institution not found: " + institutionId
        );
    }

    public static ApiException nameConflict(String name) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "Institution name already exists: " + name
        );
    }

    public static ApiException hasCongresses(UUID institutionId) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "Institution has dependent congresses: " + institutionId
        );
    }

    public static ApiException forbidden() {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "auth.forbidden",
                "System admin role is required"
        );
    }
}
