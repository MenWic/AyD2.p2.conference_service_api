package ayd2.p2b.conference_service_api.feature.room.application;

import ayd2.p2b.conference_service_api.feature.room.application.exception.RoomExceptions;

public final class RoomInputValidator {

    private RoomInputValidator() {
    }

    public static String requiredTrimmed(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw RoomExceptions.validationFailed(fieldName + " is required");
        }
        return value.trim();
    }

    public static String optionalTrimmedNonBlank(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw RoomExceptions.validationFailed(fieldName + " cannot be blank");
        }
        return trimmed;
    }

    public static String optionalTrimmedToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static Integer optionalPositiveCapacity(Integer capacity) {
        if (capacity == null) {
            return null;
        }
        if (capacity <= 0) {
            throw RoomExceptions.validationFailed("capacity must be > 0");
        }
        return capacity;
    }
}
