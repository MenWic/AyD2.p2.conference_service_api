package ayd2.p2b.conference_service_api.feature.proposal.application.port;

import java.util.UUID;

public interface ProposalCommitteeMembershipPort {
    boolean existsByCongressIdAndUserId(UUID congressId, UUID userId);
}
