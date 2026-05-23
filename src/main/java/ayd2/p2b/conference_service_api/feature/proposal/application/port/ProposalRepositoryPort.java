package ayd2.p2b.conference_service_api.feature.proposal.application.port;

import ayd2.p2b.conference_service_api.feature.proposal.domain.model.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProposalRepositoryPort {
    Proposal save(Proposal proposal);

    Optional<Proposal> findById(UUID proposalId);

    Page<Proposal> findByCallId(UUID callId, Pageable pageable);

    Page<Proposal> findByAuthorUserId(UUID authorUserId, Pageable pageable);
}
