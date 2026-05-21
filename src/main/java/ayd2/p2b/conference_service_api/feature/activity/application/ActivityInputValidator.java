package ayd2.p2b.conference_service_api.feature.activity.application;

import ayd2.p2b.conference_service_api.feature.activity.application.exception.ActivityExceptions;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ActivityInputValidator {

    private ActivityInputValidator() {
    }

    public static String requiredTrimmed(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw ActivityExceptions.validationFailed(fieldName + " is required");
        }
        return value.trim();
    }

    public static String optionalTrimmedNonBlank(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw ActivityExceptions.validationFailed(fieldName + " cannot be blank");
        }
        return trimmed;
    }

    public static Set<UUID> normalizeLeaders(List<UUID> leaders) {
        if (leaders == null) {
            return Set.of();
        }
        if (leaders.stream().anyMatch(id -> id == null)) {
            throw ActivityExceptions.validationFailed("leaders cannot contain null values");
        }

        Set<UUID> normalized = leaders.stream()
                .collect(Collectors.toSet());
        if (normalized.size() != leaders.size()) {
            throw ActivityExceptions.validationFailed("leaders cannot contain duplicates");
        }
        return normalized;
    }
}
