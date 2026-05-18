package ayd2.p2b.conference_service_api.feature.congress.application;

import ayd2.p2b.conference_service_api.feature.congress.domain.exception.CongressExceptions;

public final class CongressInputValidator {

    private CongressInputValidator() {
    }

    public static String requiredTrimmed(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw CongressExceptions.validationFailed(fieldName + " is required");
        }
        return value.trim();
    }

    public static String optionalTrimmed(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw CongressExceptions.validationFailed(fieldName + " cannot be blank");
        }
        return trimmed;
    }
}
