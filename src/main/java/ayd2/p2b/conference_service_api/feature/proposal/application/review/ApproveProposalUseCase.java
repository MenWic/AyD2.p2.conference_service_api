package ayd2.p2b.conference_service_api.feature.proposal.application.review;

import ayd2.p2b.conference_service_api.feature.proposal.application.exception.ProposalExceptions;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalCallPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalCommitteeMembershipPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalRepositoryPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.support.ProposalAccessPolicy;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.Proposal;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallSummary;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalRequesterContext;
import ayd2.p2b.conference_service_api.feature.proposal.dto.response.ProposalResponse;
import ayd2.p2b.conference_service_api.feature.proposal.mapper.ProposalMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@Transactional
public class ApproveProposalUseCase {

    private final ProposalRepositoryPort proposalRepositoryPort;
    private final ProposalCallPort proposalCallPort;
    private final ProposalCommitteeMembershipPort proposalCommitteeMembershipPort;
    private final ProposalMapper proposalMapper;

    public ApproveProposalUseCase(
            ProposalRepositoryPort proposalRepositoryPort,
            ProposalCallPort proposalCallPort,
            ProposalCommitteeMembershipPort proposalCommitteeMembershipPort,
            ProposalMapper proposalMapper
    ) {
        this.proposalRepositoryPort = proposalRepositoryPort;
        this.proposalCallPort = proposalCallPort;
        this.proposalCommitteeMembershipPort = proposalCommitteeMembershipPort;
        this.proposalMapper = proposalMapper;
    }

    public ProposalResponse execute(UUID proposalId, ProposalRequesterContext requester) {
        ProposalAccessPolicy.ensureAuthenticated(requester);

        Proposal proposal = proposalRepositoryPort.findById(proposalId)
                .orElseThrow(() -> ProposalExceptions.proposalNotFound(proposalId));
        ProposalCallSummary callSummary = proposalCallPort.findVisibleCallById(proposal.getCallId())
                .orElseThrow(() -> ProposalExceptions.callNotFound(proposal.getCallId()));

        if (!proposalCommitteeMembershipPort.existsByCongressIdAndUserId(callSummary.getCongressId(), requester.getUserId())) {
            throw ProposalExceptions.forbidden("Requester is not committee member for this congress");
        }
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw ProposalExceptions.reviewConflict(proposalId, proposal.getStatus());
        }

        Proposal approved = proposal.approve(requester.getUserId(), OffsetDateTime.now());
        approved.validateInvariants();
        Proposal saved = proposalRepositoryPort.save(approved);
        return proposalMapper.toResponse(saved);
    }
}
