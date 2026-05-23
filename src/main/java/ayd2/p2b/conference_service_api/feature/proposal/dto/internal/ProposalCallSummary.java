package ayd2.p2b.conference_service_api.feature.proposal.dto.internal;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class ProposalCallSummary {
    UUID callId;
    UUID congressId;
    UUID institutionId;
    UUID congressCreatedBy;
    ProposalCallStatus status;
}
