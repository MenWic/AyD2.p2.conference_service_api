package ayd2.p2b.conference_service_api.feature.institution.application;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

public final class InstitutionInputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private InstitutionInputValidator() {
    }

    public static String requiredTrimmed(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw validationFailed(fieldName + " is required");
        }
        return value.trim();
    }

    public static String optionalTrimmed(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw validationFailed(fieldName + " cannot be blank");
        }
        return trimmed;
    }

    public static String validateEmailRequired(String value) {
        String email = requiredTrimmed(value, "contactEmail");
        validateEmailFormat(email);
        return email;
    }

    public static String validateEmailOptional(String value) {
        if (value == null) {
            return null;
        }
        String email = optionalTrimmed(value, "contactEmail");
        validateEmailFormat(email);
        return email;
    }

    private static void validateEmailFormat(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw validationFailed("contactEmail must be a valid email");
        }
    }

    private static ApiException validationFailed(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "validation.failed", message);
    }
}
