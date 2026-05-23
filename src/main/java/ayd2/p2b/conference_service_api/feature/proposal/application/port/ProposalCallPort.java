package ayd2.p2b.conference_service_api.feature.proposal.application.port;

import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallSummary;

import java.util.Optional;
import java.util.UUID;

public interface ProposalCallPort {
    Optional<ProposalCallSummary> findVisibleCallById(UUID callId);
}
