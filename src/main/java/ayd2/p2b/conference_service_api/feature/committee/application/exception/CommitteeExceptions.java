package ayd2.p2b.conference_service_api.feature.committee.application.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class CommitteeExceptions {

    private CommitteeExceptions() {
    }

    public static ApiException congressNotFound(UUID congressId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Congress not found: " + congressId
        );
    }

    public static ApiException memberAlreadyExists(UUID congressId, UUID userId) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "User " + userId + " is already a committee member in congress " + congressId
        );
    }

    public static ApiException memberNotFound(UUID congressId, UUID userId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Committee member not found for congress " + congressId + " and user " + userId
        );
    }

    public static ApiException candidateNotEligible(UUID userId) {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "domain.invariant_violated",
                "User is not eligible to be a committee member: " + userId
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
