package ayd2.p2b.conference_service_api.feature.proposal.application.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class ProposalExceptions {

    private ProposalExceptions() {
    }

    public static ApiException callNotFound(UUID callId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Call not found: " + callId
        );
    }

    public static ApiException proposalNotFound(UUID proposalId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "resource.not_found",
                "Proposal not found: " + proposalId
        );
    }

    public static ApiException submitRequiresOpenCall(UUID callId) {
        return new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "domain.invariant_violated",
                "Proposals can only be submitted while call is OPEN: " + callId
        );
    }

    public static ApiException reviewConflict(UUID proposalId, ProposalStatus status) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "resource.conflict",
                "Proposal " + proposalId + " is already in terminal status: " + status
        );
    }

    public static ApiException forbidden(String detail) {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "auth.forbidden",
                detail
        );
    }

    public static ApiException validationFailed(String detail) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation.failed",
                detail
        );
    }
}
