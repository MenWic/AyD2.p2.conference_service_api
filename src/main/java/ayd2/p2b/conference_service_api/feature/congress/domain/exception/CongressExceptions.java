package ayd2.p2b.conference_service_api.feature.congress.domain.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CongressExceptions {

    private CongressExceptions() {
    }

    public static ApiException notFound(UUID congressId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Congress not found: " + congressId
        );
    }

    public static ApiException institutionNotFound(UUID institutionId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Active institution not found: " + institutionId
        );
    }

    public static ApiException forbidden(String message) {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "auth.forbidden",
                message
        );
    }

    public static ApiException dependencyConflict(UUID congressId, List<String> dependencies) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "Congress has dependent records (" + congressId + "): " + String.join(", ", dependencies)
        );
    }

    public static ApiException validationFailed(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation.failed",
                message
        );
    }

    public static ApiException invariantViolation(String message) {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "domain.invariant_violated",
                message
        );
    }

    public static ApiException priceTooLow(BigDecimal minPrice) {
        return invariantViolation("price must be >= " + minPrice);
    }

    public static ApiException invalidDateRange(LocalDate startDate, LocalDate endDate) {
        return invariantViolation("startDate must be <= endDate. startDate=" + startDate + ", endDate=" + endDate);
    }
}
