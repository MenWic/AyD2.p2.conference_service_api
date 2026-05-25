package ayd2.p2b.conference_service_api.feature.report.application.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.UUID;

public final class ReportExceptions {

    private ReportExceptions() {
    }

    public static ApiException invalidFormat(String value) {
        return new ApiException(HttpStatus.BAD_REQUEST, "validation.failed",
                "Invalid report format: '" + value + "'. Accepted values: json, html");
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden", message);
    }

    public static ApiException congressNotFound(UUID congressId) {
        return new ApiException(HttpStatus.NOT_FOUND, "resource.not_found",
                "Congress not found: " + congressId);
    }

    public static ApiException congressAccessDenied(UUID congressId) {
        return new ApiException(HttpStatus.FORBIDDEN, "auth.forbidden",
                "Access denied to congress: " + congressId);
    }

    public static ApiException missingCongressId() {
        return new ApiException(HttpStatus.BAD_REQUEST, "validation.failed",
                "congressId parameter is required");
    }

    public static ApiException invalidDateRange(LocalDate dateFrom, LocalDate dateTo) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation.failed",
                "dateFrom must be less than or equal to dateTo. dateFrom=" + dateFrom + ", dateTo=" + dateTo
        );
    }
}
