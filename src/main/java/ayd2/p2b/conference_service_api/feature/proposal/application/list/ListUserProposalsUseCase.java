package ayd2.p2b.conference_service_api.feature.proposal.application.list;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.proposal.application.port.ProposalRepositoryPort;
import ayd2.p2b.conference_service_api.feature.proposal.application.support.ProposalAccessPolicy;
import ayd2.p2b.conference_service_api.feature.proposal.dto.internal.ProposalRequesterContext;
import ayd2.p2b.conference_service_api.feature.proposal.dto.response.ProposalResponse;
import ayd2.p2b.conference_service_api.feature.proposal.mapper.ProposalMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class ListUserProposalsUseCase {

    private final ProposalRepositoryPort proposalRepositoryPort;
    private final ProposalMapper proposalMapper;

    public ListUserProposalsUseCase(
            ProposalRepositoryPort proposalRepositoryPort,
            ProposalMapper proposalMapper
    ) {
        this.proposalRepositoryPort = proposalRepositoryPort;
        this.proposalMapper = proposalMapper;
    }

    public PageResponse<ProposalResponse> execute(UUID userId, Pageable pageable, ProposalRequesterContext requester) {
        ProposalAccessPolicy.ensureSelf(requester, userId);
        ProposalAccessPolicy.ensureParticipant(requester);

        Page<ProposalResponse> responsePage = proposalRepositoryPort.findByAuthorUserId(userId, pageable)
                .map(proposalMapper::toResponse);

        return PageResponse.<ProposalResponse>builder()
                .items(responsePage.getContent())
                .page(responsePage.getNumber())
                .size(responsePage.getSize())
                .totalItems(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .build();
    }
}
