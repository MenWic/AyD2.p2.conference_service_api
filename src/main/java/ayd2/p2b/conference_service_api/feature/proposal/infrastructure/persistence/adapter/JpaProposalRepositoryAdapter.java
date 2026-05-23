package ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalRepositoryPort;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.Proposal;
import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.repository.ProposalRepository;
import ayd2.p2b.conference_service_api.feature.proposal.mapper.ProposalMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaProposalRepositoryAdapter implements ProposalRepositoryPort {

    private final ProposalRepository proposalRepository;
    private final ProposalMapper proposalMapper;

    public JpaProposalRepositoryAdapter(
            ProposalRepository proposalRepository,
            ProposalMapper proposalMapper
    ) {
        this.proposalRepository = proposalRepository;
        this.proposalMapper = proposalMapper;
    }

    @Override
    public Proposal save(Proposal proposal) {
        return proposalMapper.toDomain(proposalRepository.save(proposalMapper.toEntity(proposal)));
    }

    @Override
    public Optional<Proposal> findById(UUID proposalId) {
        return proposalRepository.findById(proposalId).map(proposalMapper::toDomain);
    }

    @Override
    public Page<Proposal> findByCallId(UUID callId, Pageable pageable) {
        return proposalRepository.findByCallId(callId, pageable)
                .map(proposalMapper::toDomain);
    }

    @Override
    public Page<Proposal> findByAuthorUserId(UUID authorUserId, Pageable pageable) {
        return proposalRepository.findByAuthorUserId(authorUserId, pageable)
                .map(proposalMapper::toDomain);
    }
}
