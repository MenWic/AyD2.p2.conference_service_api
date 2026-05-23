package ayd2.p2b.conference_service_api.feature.proposal.domain.model;

import ayd2.p2b.conference_service_api.feature.proposal.domain.exception.ProposalDomainException;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class Proposal {
    UUID id;
    UUID callId;
    UUID authorUserId;
    String title;
    String description;
    ProposalType type;
    ProposalStatus status;
    UUID reviewedBy;
    OffsetDateTime reviewedAt;
    UUID createdActivityId;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    UUID createdBy;
    UUID updatedBy;

    public void validateInvariants() {
        if (callId == null) {
            throw new ProposalDomainException("callId is required");
        }
        if (authorUserId == null) {
            throw new ProposalDomainException("authorUserId is required");
        }
        if (title == null || title.isBlank()) {
            throw new ProposalDomainException("title is required");
        }
        if (title.length() > 500) {
            throw new ProposalDomainException("title must not exceed 500 characters");
        }
        if (description == null || description.isBlank()) {
            throw new ProposalDomainException("description is required");
        }
        if (type == null) {
            throw new ProposalDomainException("type is required");
        }
        if (status == null) {
            throw new ProposalDomainException("status is required");
        }
        if (createdBy == null) {
            throw new ProposalDomainException("createdBy is required");
        }

        if (status == ProposalStatus.PENDING) {
            if (reviewedBy != null || reviewedAt != null) {
                throw new ProposalDomainException("reviewedBy and reviewedAt must be null while status is PENDING");
            }
        } else if (reviewedBy == null || reviewedAt == null) {
            throw new ProposalDomainException("reviewedBy and reviewedAt are required for reviewed proposals");
        }

        if (createdActivityId != null && status != ProposalStatus.APPROVED) {
            throw new ProposalDomainException("createdActivityId is only allowed for APPROVED proposals");
        }
    }

    public Proposal approve(UUID reviewerUserId, OffsetDateTime reviewedAtValue) {
        if (status != ProposalStatus.PENDING) {
            throw new ProposalDomainException("Only PENDING proposals can be approved");
        }
        if (reviewerUserId == null) {
            throw new ProposalDomainException("reviewerUserId is required");
        }
        if (reviewedAtValue == null) {
            throw new ProposalDomainException("reviewedAt is required");
        }
        return toBuilder()
                .status(ProposalStatus.APPROVED)
                .reviewedBy(reviewerUserId)
                .reviewedAt(reviewedAtValue)
                .updatedBy(reviewerUserId)
                .build();
    }

    public Proposal reject(UUID reviewerUserId, OffsetDateTime reviewedAtValue) {
        if (status != ProposalStatus.PENDING) {
            throw new ProposalDomainException("Only PENDING proposals can be rejected");
        }
        if (reviewerUserId == null) {
            throw new ProposalDomainException("reviewerUserId is required");
        }
        if (reviewedAtValue == null) {
            throw new ProposalDomainException("reviewedAt is required");
        }
        return toBuilder()
                .status(ProposalStatus.REJECTED)
                .reviewedBy(reviewerUserId)
                .reviewedAt(reviewedAtValue)
                .updatedBy(reviewerUserId)
                .build();
    }
}
