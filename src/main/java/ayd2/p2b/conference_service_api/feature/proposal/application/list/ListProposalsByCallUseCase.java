package ayd2.p2b.conference_service_api.feature.proposal.application.list;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.proposal.application.exception.ProposalExceptions;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalCallPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalCommitteeMembershipPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalRepositoryPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.support.ProposalAccessPolicy;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.Proposal;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalCallSummary;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalRequesterContext;
import ayd2.p2b.conference_service_api.feature.proposal.dto.response.ProposalResponse;
import ayd2.p2b.conference_service_api.feature.proposal.mapper.ProposalMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class ListProposalsByCallUseCase {

    private final ProposalRepositoryPort proposalRepositoryPort;
    private final ProposalCallPort proposalCallPort;
    private final ProposalCommitteeMembershipPort proposalCommitteeMembershipPort;
    private final IamUserLookupPort iamUserLookupPort;
    private final ProposalMapper proposalMapper;

    public ListProposalsByCallUseCase(
            ProposalRepositoryPort proposalRepositoryPort,
            ProposalCallPort proposalCallPort,
            ProposalCommitteeMembershipPort proposalCommitteeMembershipPort,
            IamUserLookupPort iamUserLookupPort,
            ProposalMapper proposalMapper
    ) {
        this.proposalRepositoryPort = proposalRepositoryPort;
        this.proposalCallPort = proposalCallPort;
        this.proposalCommitteeMembershipPort = proposalCommitteeMembershipPort;
        this.iamUserLookupPort = iamUserLookupPort;
        this.proposalMapper = proposalMapper;
    }

    public PageResponse<ProposalResponse> execute(UUID callId, Pageable pageable, ProposalRequesterContext requester) {
        ProposalAccessPolicy.ensureAuthenticated(requester);

        ProposalCallSummary callSummary = proposalCallPort.findVisibleCallById(callId)
                .orElseThrow(() -> ProposalExceptions.callNotFound(callId));

        authorizeReadAccess(callSummary, requester);

        Page<ProposalResponse> responsePage = proposalRepositoryPort.findByCallId(callId, pageable)
                .map(proposalMapper::toResponse);

        return PageResponse.<ProposalResponse>builder()
                .items(responsePage.getContent())
                .page(responsePage.getNumber())
                .size(responsePage.getSize())
                .totalItems(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .build();
    }

    private void authorizeReadAccess(ProposalCallSummary callSummary, ProposalRequesterContext requester) {
        if (proposalCommitteeMembershipPort.existsByCongressIdAndUserId(callSummary.getCongressId(), requester.getUserId())) {
            return;
        }

        if (!ProposalAccessPolicy.canUseScopedCongressAdminPath(requester)) {
            throw ProposalExceptions.forbidden("Requester is not authorized to list proposals for this call");
        }

        if (requester.getUserId().equals(callSummary.getCongressCreatedBy())) {
            return;
        }

        boolean linkedToInstitution = iamUserLookupPort.isCongressAdminLinkedToInstitution(
                requester.getUserId(),
                callSummary.getInstitutionId(),
                requester.getAccessToken()
        );
        if (!linkedToInstitution) {
            throw ProposalExceptions.forbidden("Requester is not owner and not linked to congress institution");
        }
    }
}
