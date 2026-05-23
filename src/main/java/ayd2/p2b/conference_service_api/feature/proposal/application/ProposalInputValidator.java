package ayd2.p2b.conference_service_api.feature.proposal.application;

import ayd2.p2b.conference_service_api.feature.proposal.application.exception.ProposalExceptions;
import ayd2.p2b.conference_service_api.feature.proposal.dto.request.CreateProposalRequest;

public final class ProposalInputValidator {

    private ProposalInputValidator() {
    }

    public static CreateProposalRequest requireValidCreateRequest(CreateProposalRequest request) {
        if (request == null) {
            throw ProposalExceptions.validationFailed("request body is required");
        }

        String normalizedTitle = normalizeText(request.getTitle());
        if (normalizedTitle == null) {
            throw ProposalExceptions.validationFailed("title is required");
        }
        if (normalizedTitle.length() > 500) {
            throw ProposalExceptions.validationFailed("title must not exceed 500 characters");
        }

        String normalizedDescription = normalizeText(request.getDescription());
        if (normalizedDescription == null) {
            throw ProposalExceptions.validationFailed("description is required");
        }

        if (request.getType() == null) {
            throw ProposalExceptions.validationFailed("type is required");
        }

        return CreateProposalRequest.builder()
                .title(normalizedTitle)
                .description(normalizedDescription)
                .type(request.getType())
                .build();
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
