package ayd2.p2b.conference_service_api.feature.report.dto.internal;

import ayd2.p2b.conference_service_api.feature.report.application.exception.ReportExceptions;

public enum ReportFormat {
    JSON,
    HTML;

    public static ReportFormat parse(String value) {
        if (value == null || value.isBlank()) {
            return JSON;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "json" -> JSON;
            case "html" -> HTML;
            default -> throw ReportExceptions.invalidFormat(value);
        };
    }
}
