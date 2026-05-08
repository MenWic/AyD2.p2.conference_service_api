package ayd2.p2b.conference_service_api.common.util;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String trimRequired(String value, String fieldName) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public static String trimOptional(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static String lowerTrimRequired(String value, String fieldName) {
        return trimRequired(value, fieldName).toLowerCase();
    }
}
